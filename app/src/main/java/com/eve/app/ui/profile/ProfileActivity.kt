package com.eve.app.ui.profile

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.ui.about.AboutActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.isHardcodedAdmin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val adminRepo = AdminRepository()
    private var progressDialog: AlertDialog? = null
    private var pendingPhotoUri: Uri? = null

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                previewSelectedPhoto(uri)
            }
        }

    private val reauthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)
                        ?.addOnSuccessListener {
                            executeAccountDeletion()
                        }
                        ?.addOnFailureListener { e ->
                            dismissProgress()
                            Toast.makeText(this, "Re-authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    dismissProgress()
                    Toast.makeText(this, "Could not get authentication token", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                dismissProgress()
                Toast.makeText(this, "Re-authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            finish()
            return
        }

        binding.btnBack.setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (pendingPhotoUri != null) {
                    cancelPhotoPreview()
                } else {
                    finish()
                }
            }
        })

        binding.tvName.text = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
        binding.tvEmail.text = user.email ?: ""

        loadProfilePhoto()
        binding.btnChangePhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }
        binding.ivProfilePhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }

        binding.btnCancelPhoto.setOnClickListener {
            cancelPhotoPreview()
        }

        binding.btnSavePhoto.setOnClickListener {
            savePendingPhoto()
        }

        setupDobPicker()
        loadUserProfile(user.uid)

        binding.btnSaveProfile.setOnClickListener {
            saveUserProfile()
        }

        val setupAdminUi = {
            binding.chipAdmin.visibility = View.VISIBLE
            binding.btnEditAbout.visibility = View.VISIBLE
            binding.btnEditAbout.setOnClickListener {
                startActivity(Intent(this, com.eve.app.ui.admin.EditAboutActivity::class.java))
            }
        }

        if (isHardcodedAdmin(user.email)) {
            setupAdminUi()
        } else {
            lifecycleScope.launch {
                if (adminRepo.isAdmin(user.email)) setupAdminUi()
            }
        }

        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.switchReminder.isChecked = ReminderScheduler.isEnabled(this)
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            ReminderScheduler.setEnabled(this, isChecked)
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun setupDobPicker() {
        val openPicker = {
            val calendar = Calendar.getInstance()
            val existingDob = binding.etDob.text?.toString()?.trim() ?: ""
            if (existingDob.isNotEmpty()) {
                val parts = existingDob.split("/")
                if (parts.size == 3) {
                    val d = parts[0].toIntOrNull()
                    val m = parts[1].toIntOrNull()
                    val y = parts[2].toIntOrNull()
                    if (d != null && m != null && y != null) {
                        calendar.set(y, m - 1, d)
                    }
                }
            } else {
                calendar.add(Calendar.YEAR, -18)
            }

            val picker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val formatted = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    binding.etDob.setText(formatted)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.maxDate = System.currentTimeMillis()
            picker.show()
        }

        binding.etDob.setOnClickListener { openPicker() }
        binding.tilDob.setEndIconOnClickListener { openPicker() }
    }

    private fun loadUserProfile(userId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val currentName = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
        binding.tvName.text = currentName
        binding.etName.setText(if (currentName != "Student") currentName else "")

        lifecycleScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val name = snapshot.getString("displayName")
                    val dob = snapshot.getString("dob")
                    val category = snapshot.getString("category")

                    if (!name.isNullOrBlank()) {
                        binding.tvName.text = name
                        binding.etName.setText(name)
                    }
                    if (!dob.isNullOrBlank()) {
                        binding.etDob.setText(dob)
                    }
                    if (!category.isNullOrBlank()) {
                        setCategoryChip(category)
                    }
                }
            } catch (e: Exception) {
                // Non-fatal: Safe fallback, never crash if offline or document missing
                android.util.Log.w("ProfileActivity", "Error loading profile: ${e.message}")
            }
        }
    }

    private fun setCategoryChip(category: String) {
        when (category.trim().uppercase(Locale.US)) {
            "OBC" -> binding.chipObc.isChecked = true
            "SC" -> binding.chipSc.isChecked = true
            "ST" -> binding.chipSt.isChecked = true
            else -> binding.chipGeneral.isChecked = true
        }
    }

    private fun getSelectedCategory(): String {
        return when {
            binding.chipObc.isChecked -> "OBC"
            binding.chipSc.isChecked -> "SC"
            binding.chipSt.isChecked -> "ST"
            else -> "General"
        }
    }

    private fun saveUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val dob = binding.etDob.text?.toString()?.trim() ?: ""
        val category = getSelectedCategory()

        if (name.isBlank()) {
            binding.tilName.error = "Name cannot be empty"
            return
        }
        binding.tilName.error = null

        showProgress("Saving profile...")

        lifecycleScope.launch {
            try {
                val data = hashMapOf<String, Any>(
                    "displayName" to name,
                    "dob" to dob,
                    "category" to category,
                    "email" to (user.email ?: ""),
                    "lastUpdated" to System.currentTimeMillis()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .set(data, SetOptions.merge())
                    .await()

                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (_: Exception) {
                    // Non-fatal
                }

                binding.tvName.text = name
                dismissProgress()
                Toast.makeText(this@ProfileActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                dismissProgress()
                Toast.makeText(this@ProfileActivity, "Failed to save profile: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDeleteAccountConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Account")
            .setMessage("Warning: Deleting your account is permanent. All your data (profile, test attempts, results, history, leaderboard entries, pinned tests and notifications) will be erased and cannot be recovered. Do you want to continue?")
            .setPositiveButton("Delete") { _, _ ->
                executeAccountDeletion()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showProgress(msg: String) {
        dismissProgress()
        val builder = AlertDialog.Builder(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(48, 48, 48, 48)
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(ProgressBar(this@ProfileActivity))
            addView(TextView(this@ProfileActivity).apply {
                text = msg
                setPadding(32, 0, 0, 0)
                textSize = 15f
            })
        }
        builder.setView(layout)
        builder.setCancelable(false)
        progressDialog = builder.create().apply { show() }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun executeAccountDeletion() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        showProgress("Deleting account and data...")

        lifecycleScope.launch {
            try {
                // Call Cloud Function Admin SDK cleanup
                try {
                    FirebaseFunctions.getInstance()
                        .getHttpsCallable("deleteUserAccount")
                        .call()
                        .await()
                } catch (fnErr: Exception) {
                    android.util.Log.w("ProfileActivity", "Backend delete callable note: ${fnErr.message}")
                }

                // Delete client auth user
                try {
                    user.delete().await()
                } catch (authErr: Exception) {
                    if (authErr is FirebaseAuthRecentLoginRequiredException) {
                        dismissProgress()
                        promptReauthentication()
                        return@launch
                    } else {
                        android.util.Log.w("ProfileActivity", "Client auth delete note: ${authErr.message}")
                    }
                }

                // Local cleanup
                ProfilePhotoManager.removePhoto(this@ProfileActivity)
                getSharedPreferences("eve_prefs", MODE_PRIVATE).edit().clear().apply()

                FirebaseAuth.getInstance().signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(this@ProfileActivity, gso).signOut()

                dismissProgress()
                Toast.makeText(this@ProfileActivity, "Account deleted successfully", Toast.LENGTH_LONG).show()

                val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                dismissProgress()
                MaterialAlertDialogBuilder(this@ProfileActivity)
                    .setTitle("Account Deletion Failed")
                    .setMessage(e.localizedMessage ?: "Could not delete account. Please try again.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun promptReauthentication() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Re-authentication Required")
            .setMessage("For security, please sign in with Google again to confirm account deletion.")
            .setPositiveButton("Sign In") { _, _ ->
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(this, gso)
                client.signOut().addOnCompleteListener {
                    reauthLauncher.launch(client.signInIntent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadProfilePhoto() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        ProfilePhotoManager.applyTo(
            this,
            binding.ivProfilePhoto,
            user.photoUrl?.toString(),
            R.drawable.bg_circle_primary
        )
    }

    private fun previewSelectedPhoto(uri: Uri) {
        pendingPhotoUri = uri
        binding.ivProfilePhoto.background = null
        binding.ivProfilePhoto.setPadding(0, 0, 0, 0)
        binding.ivProfilePhoto.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.ivProfilePhoto.load(uri) {
            crossfade(true)
            placeholder(R.drawable.ic_person)
            error(R.drawable.ic_person)
        }
        binding.layoutPhotoActions.visibility = View.VISIBLE
    }

    private fun cancelPhotoPreview() {
        pendingPhotoUri = null
        binding.layoutPhotoActions.visibility = View.GONE
        loadProfilePhoto()
    }

    private fun savePendingPhoto() {
        val uri = pendingPhotoUri ?: return
        if (ProfilePhotoManager.savePhoto(this, uri)) {
            Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
        }
        pendingPhotoUri = null
        binding.layoutPhotoActions.visibility = View.GONE
        loadProfilePhoto()
    }

    override fun onDestroy() {
        dismissProgress()
        super.onDestroy()
    }
}

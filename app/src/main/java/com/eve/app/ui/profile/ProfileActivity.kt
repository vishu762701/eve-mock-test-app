package com.eve.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val adminRepo = AdminRepository()
    private var progressDialog: AlertDialog? = null

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && ProfilePhotoManager.savePhoto(this, uri)) {
                loadProfilePhoto()
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

        binding.tvName.text = user.displayName ?: "Student"
        binding.tvEmail.text = user.email ?: ""

        loadProfilePhoto()
        binding.btnChangePhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }
        binding.ivProfilePhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }

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
            com.eve.app.R.drawable.bg_circle_primary
        )
    }

    override fun onDestroy() {
        dismissProgress()
        super.onDestroy()
    }
}

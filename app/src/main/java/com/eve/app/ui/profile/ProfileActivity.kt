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
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.isHardcodedAdmin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.eve.app.data.remote.ApiClient
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
                val res = ApiClient.apiService.getProfile()
                if (res.success && res.data != null) {
                    val profile = res.data
                    if (profile.displayName.isNotBlank()) {
                        binding.tvName.text = profile.displayName
                        binding.etName.setText(profile.displayName)
                    }
                    if (profile.dob.isNotBlank()) {
                        binding.etDob.setText(profile.dob)
                    }
                    if (profile.category.isNotBlank()) {
                        setCategoryChip(profile.category)
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
                val data = mapOf<String, Any>(
                    "displayName" to name,
                    "dob" to dob,
                    "category" to category
                )

                val res = ApiClient.apiService.updateProfile(data)
                if (!res.success) {
                    throw Exception(res.error ?: "Failed to update profile")
                }

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

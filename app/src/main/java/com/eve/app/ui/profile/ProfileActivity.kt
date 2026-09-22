package com.eve.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.ui.login.LoginActivity
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ThemeManager
import com.eve.app.util.isHardcodedAdmin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val adminRepo = AdminRepository()

    private val pickPhotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null && ProfilePhotoManager.savePhoto(this, uri)) {
                loadProfilePhoto()
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
        ThemeManager.setupToggleButton(this, binding.btnThemeToggle)

        binding.tvName.text = user.displayName ?: "Student"
        binding.tvEmail.text = user.email ?: ""

        loadProfilePhoto()
        binding.btnChangePhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }
        binding.ivProfilePhoto.setOnClickListener { pickPhotoLauncher.launch("image/*") }

        if (isHardcodedAdmin(user.email)) {
            binding.chipAdmin.visibility = View.VISIBLE
        } else {
            lifecycleScope.launch {
                if (adminRepo.isAdmin(user.email)) binding.chipAdmin.visibility = View.VISIBLE
            }
        }

        binding.btnLogout.setOnClickListener { logout() }
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

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            // Poora back stack clear karke Login par bhejo (Home/Profile dono khatam)
            val intent = Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}

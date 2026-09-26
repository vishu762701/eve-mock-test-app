package com.eve.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivitySettingsBinding
import com.eve.app.ui.login.LoginActivity
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.ThemeManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var progressDialog: AlertDialog? = null

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
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupThemeSetting()
        setupReminderSetting()
        setupDeleteAccountSetting()
    }

    private fun setupThemeSetting() {
        val isDark = ThemeManager.isDarkMode(this)
        binding.switchDarkMode.isChecked = isDark
        binding.ivThemeIcon.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)

        val performToggle = {
            if (!ThemeManager.isTransitioning) {
                ThemeManager.toggleWithCircularReveal(this, binding.switchDarkMode.let {
                    val loc = IntArray(2)
                    it.getLocationOnScreen(loc)
                    loc[0] + it.width / 2
                }, binding.switchDarkMode.let {
                    val loc = IntArray(2)
                    it.getLocationOnScreen(loc)
                    loc[1] + it.height / 2
                })
            }
        }

        binding.switchDarkMode.setOnClickListener {
            performToggle()
        }

        binding.rowDarkMode.setOnClickListener {
            performToggle()
        }
    }

    private fun setupReminderSetting() {
        binding.switchReminder.isChecked = ReminderScheduler.isEnabled(this)
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            ReminderScheduler.setEnabled(this, isChecked)
        }
        binding.rowReminder.setOnClickListener {
            binding.switchReminder.toggle()
        }
    }

    private fun setupDeleteAccountSetting() {
        binding.rowDeleteAccount.setOnClickListener {
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
            addView(ProgressBar(this@SettingsActivity))
            addView(TextView(this@SettingsActivity).apply {
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
                // Call Cloudflare Worker account deletion
                try {
                    ApiClient.apiService.deleteAccount()
                } catch (fnErr: Exception) {
                    android.util.Log.w("SettingsActivity", "Backend delete callable note: ${fnErr.message}")
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
                        android.util.Log.w("SettingsActivity", "Client auth delete note: ${authErr.message}")
                    }
                }

                // Local cleanup
                ProfilePhotoManager.removePhoto(this@SettingsActivity)
                getSharedPreferences("eve_prefs", MODE_PRIVATE).edit().clear().apply()

                FirebaseAuth.getInstance().signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(this@SettingsActivity, gso).signOut()

                dismissProgress()
                Toast.makeText(this@SettingsActivity, "Account deleted successfully", Toast.LENGTH_LONG).show()

                val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                dismissProgress()
                MaterialAlertDialogBuilder(this@SettingsActivity)
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

    override fun onDestroy() {
        dismissProgress()
        super.onDestroy()
    }
}

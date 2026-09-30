package com.eve.app.ui.settings

import com.eve.app.ui.common.EveBaseActivity

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import com.eve.app.util.AppBulletin
import android.view.View
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.eve.app.ui.home.TargetExamsBottomSheet
import com.eve.app.util.StreakHelper

class SettingsActivity : EveBaseActivity() {

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
                            AppBulletin.showError(this, "Re-authentication failed: ${e.message}")
                        }
                } else {
                    dismissProgress()
                    AppBulletin.showError(this, "Could not get authentication token")
                }
            } catch (e: Exception) {
                dismissProgress()
                AppBulletin.showError(this, "Re-authentication failed: ${e.message}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupFontSetting()
        setupReminderSetting()
        setupDailyGoalSetting()
        setupTargetExamsSetting()
        setupLanguageSetting()
        setupDeleteAccountSetting()
    }

    private fun setupFontSetting() {
        fun refreshFontText() {
            val choice = com.eve.app.util.FontManager.getFontChoice(this)
            binding.tvFontValue.text = if (choice == com.eve.app.util.FontManager.FONT_DEVICE) "Device font" else "Eve default"
        }
        refreshFontText()

        binding.rowFont.setOnClickListener {
            showFontPickerBottomSheet {
                refreshFontText()
            }
        }
    }

    private fun showFontPickerBottomSheet(onChanged: () -> Unit) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val sheetBinding = com.eve.app.databinding.BottomSheetFontPickerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val currentChoice = com.eve.app.util.FontManager.getFontChoice(this)
        sheetBinding.radioEveDefault.isChecked = (currentChoice == com.eve.app.util.FontManager.FONT_EVE_DEFAULT)
        sheetBinding.radioDeviceFont.isChecked = (currentChoice == com.eve.app.util.FontManager.FONT_DEVICE)

        // Live preview typography for sample lines
        sheetBinding.tvEveDefaultTitle.typeface = com.eve.app.util.FontManager.typeface(this, bold = true)
        sheetBinding.tvEveDefaultSample.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.source_serif_4_regular)
        sheetBinding.tvDeviceFontTitle.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        sheetBinding.tvDeviceFontSample.typeface = android.graphics.Typeface.SANS_SERIF

        fun select(choice: String) {
            if (currentChoice != choice) {
                com.eve.app.util.FontManager.setFontChoice(this, choice)
                dialog.dismiss()
                onChanged()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                recreate()
            } else {
                dialog.dismiss()
            }
        }

        sheetBinding.rowEveDefault.setOnClickListener { select(com.eve.app.util.FontManager.FONT_EVE_DEFAULT) }
        sheetBinding.rowDeviceFont.setOnClickListener { select(com.eve.app.util.FontManager.FONT_DEVICE) }

        dialog.show()
    }

    private fun setupDailyGoalSetting() {
        val prefs = getSharedPreferences(StreakHelper.PREFS_NAME, MODE_PRIVATE)
        fun refreshText() {
            val goal = prefs.getInt(StreakHelper.KEY_DAILY_GOAL, StreakHelper.DEFAULT_DAILY_GOAL)
            binding.tvDailyGoalValue.text = "$goal tests per day"
        }
        refreshText()
        binding.rowDailyGoal.setOnClickListener {
            val options = arrayOf("1 test per day", "2 tests per day", "3 tests per day", "5 tests per day")
            val values = intArrayOf(1, 2, 3, 5)
            val currentGoal = prefs.getInt(StreakHelper.KEY_DAILY_GOAL, StreakHelper.DEFAULT_DAILY_GOAL)
            val selectedIdx = values.indexOf(currentGoal).coerceAtLeast(0)

            MaterialAlertDialogBuilder(this)
                .setTitle("Select Daily Goal")
                .setSingleChoiceItems(options, selectedIdx) { dialog, which ->
                    prefs.edit().putInt(StreakHelper.KEY_DAILY_GOAL, values[which]).apply()
                    refreshText()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupTargetExamsSetting() {
        binding.rowTargetExams.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val exams = com.eve.app.data.repository.ExamRepository().getExams()
                    TargetExamsBottomSheet.show(this@SettingsActivity, exams)
                } catch (e: Exception) {
                    AppBulletin.showError(this@SettingsActivity, "Could not load exams: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun setupLanguageSetting() {
        fun refreshText() {
            val current = AppCompatDelegate.getApplicationLocales()
            val text = when {
                current.isEmpty -> "System default"
                current.toLanguageTags().startsWith("hi") -> "हिन्दी"
                else -> "English"
            }
            binding.tvLanguageValue.text = text
        }
        refreshText()

        binding.rowLanguage.setOnClickListener {
            val options = arrayOf("System default", "English", "हिन्दी")
            val current = AppCompatDelegate.getApplicationLocales()
            val selectedIdx = when {
                current.isEmpty -> 0
                current.toLanguageTags().startsWith("hi") -> 2
                else -> 1
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Select App Language")
                .setSingleChoiceItems(options, selectedIdx) { dialog, which ->
                    val locales = when (which) {
                        1 -> LocaleListCompat.forLanguageTags("en")
                        2 -> LocaleListCompat.forLanguageTags("hi")
                        else -> LocaleListCompat.getEmptyLocaleList()
                    }
                    AppCompatDelegate.setApplicationLocales(locales)
                    refreshText()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
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
                AppBulletin.showSuccess(this@SettingsActivity, "Account deleted successfully")

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

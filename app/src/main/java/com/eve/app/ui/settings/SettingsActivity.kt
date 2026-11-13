package com.eve.app.ui.settings

import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.designsystem.UiArcMotion
import com.eve.app.designsystem.UiArcRadioCardsGroup

import com.eve.app.ui.common.EveBaseActivity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import com.eve.app.designsystem.TelegramRadialProgressView
import android.widget.TextView
import com.eve.app.util.AppBulletin
import com.eve.app.util.EveNavigationHelper
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.databinding.ActivitySettingsBinding
import com.eve.app.ui.about.AboutActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import androidx.appcompat.app.AppCompatDelegate
import com.eve.app.ui.home.TargetExamsBottomSheet
import com.eve.app.util.StreakHelper
import com.eve.app.util.SubmissionRetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

class SettingsActivity : EveBaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var progressDialog: TelegramNativeAlertDialog? = null
    private var passwordReauthenticationDialog: TelegramNativeAlertDialog? = null
    private var deleteConfirmationDialog: TelegramNativeAlertDialog? = null
    private var targetExamsDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    private var deleteConfirmArmedAt: Long = 0L
    private var deleteTimeoutJob: kotlinx.coroutines.Job? = null
    private val settingsAuth = FirebaseAuth.getInstance()
    private var settingsOwnerUid: String? = null
    private var logoutJob: Job? = null
    private var accountDeletionJob: Job? = null
    private var deletingFirebaseCredential = false

    private fun hasSettingsOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(settingsOwnerUid, settingsAuth.currentUser?.uid)

    private val settingsAccountListener = FirebaseAuth.AuthStateListener { firebase ->
        if (settingsOwnerUid != null && !hasSettingsOwner() &&
            !(deletingFirebaseCredential && firebase.currentUser == null)) {
            logoutJob?.cancel()
            accountDeletionJob?.cancel()
            dismissProgress()
            finish()
        }
    }

    private val reauthLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (!hasSettingsOwner() || isFinishing || isDestroyed) { finish(); return@registerForActivityResult }
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)
                        ?.addOnSuccessListener {
                            if (hasSettingsOwner() && !isFinishing && !isDestroyed) executeAccountDeletion()
                            else finish()
                        }
                        ?.addOnFailureListener { e ->
                            if (hasSettingsOwner() && !isFinishing && !isDestroyed) {
                                dismissProgress()
                                AppBulletin.showError(this, "Re-authentication failed: ${e.message}")
                            }
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
        settingsOwnerUid = if (savedInstanceState?.containsKey("settings_owner_uid") == true) {
            savedInstanceState.getString("settings_owner_uid")
        } else settingsAuth.currentUser?.uid
        if (!hasSettingsOwner()) { finish(); return }
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.designsystem.DesignPress.bind(binding.btnBack) { finish() }
        listOf(
            binding.rowAppearance, binding.rowAbout, binding.rowLogout,
            binding.rowDailyGoal, binding.rowTargetExams, binding.rowLanguage,
            binding.rowReminder, binding.rowDeleteAccount
        ).forEach { com.eve.app.designsystem.DesignPress.bind(it) }

        binding.rowAppearance.setOnClickListener {
            startActivity(Intent(this, AppearanceActivity::class.java))
        }

        binding.rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        setupReminderSetting()
        setupDailyGoalSetting()
        setupTargetExamsSetting()
        setupLanguageSetting()
        setupLogoutSetting()
        setupDeleteAccountSetting()
    }

    override fun onStart() {
        super.onStart()
        if (!hasSettingsOwner() && !deletingFirebaseCredential) { finish(); return }
        settingsAuth.addAuthStateListener(settingsAccountListener)
    }

    override fun onStop() {
        settingsAuth.removeAuthStateListener(settingsAccountListener)
        dismissProgress()
        deleteConfirmationDialog?.dismiss()
        deleteConfirmationDialog = null
        passwordReauthenticationDialog?.dismiss()
        passwordReauthenticationDialog = null
        targetExamsDialog?.dismiss()
        targetExamsDialog = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasSettingsOwner() && !deletingFirebaseCredential) finish()
        refreshLanguageText()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("settings_owner_uid", settingsOwnerUid)
    }

    private fun setupLogoutSetting() {
        binding.rowLogout.setOnClickListener {
            if (isFinishing || isDestroyed) return@setOnClickListener
            com.eve.app.designsystem.TelegramNativeAlertDialog.Builder(this)
                .setTitle("Log out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log out") { _, _ ->
                    executeLogout()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun executeLogout() {
        if (!hasSettingsOwner()) { finish(); return }
        if (!binding.rowLogout.isEnabled || logoutJob?.isActive == true) return
        binding.rowLogout.isEnabled = false
        logoutJob = lifecycleScope.launch {
            try {
                if (!hasSettingsOwner()) { finish(); return@launch }
                com.eve.app.util.ProfilePresenceClient.revokeBeforeLogout()
                if (!hasSettingsOwner()) { finish(); return@launch }
                performLocalLogout()
            } finally {
                if (hasSettingsOwner() && ::binding.isInitialized) binding.rowLogout.isEnabled = true
            }
        }
    }

    private fun performLocalLogout() {
        if (!hasSettingsOwner()) { finish(); return }
        com.eve.app.util.SessionManager.clear()
        val uid = requireNotNull(settingsOwnerUid)
        com.eve.app.data.repository.PremiumRepository.clearCacheForLogout(uid, this)
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        GoogleSignIn.getClient(this, gso).signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finishAffinity()
    }

    private fun setupDailyGoalSetting() {
        val prefs = getSharedPreferences(StreakHelper.PREFS_NAME, MODE_PRIVATE)
        fun refreshText() {
            val goal = prefs.getInt(StreakHelper.KEY_DAILY_GOAL, StreakHelper.DEFAULT_DAILY_GOAL)
            binding.tvDailyGoalValue.text = "$goal tests per day"
        }
        refreshText()
        binding.rowDailyGoal.setOnClickListener {
            val currentGoal = prefs.getInt(StreakHelper.KEY_DAILY_GOAL, StreakHelper.DEFAULT_DAILY_GOAL)
            val density = resources.displayMetrics.density

            val container = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                val padH = Math.round(16f * density)
                val padT = Math.round(12f * density)
                val padB = Math.round(16f * density)
                setPadding(padH, padT, padH, padB)
                clipChildren = false
                clipToPadding = false

                val tvDesc = TextView(this@SettingsActivity).apply {
                    setTextAppearance(R.style.TextAppearance_Eve_Token_Secondary)
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = Math.round(8f * density)
                    }
                    layoutParams = lp
                    text = "Choose your target number of practice mock tests to maintain your streak."
                }
                addView(tvDesc)
            }

            val radioGroup = UiArcRadioCardsGroup(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(radioGroup)

            val dialogView = android.widget.ScrollView(this).apply {
                clipChildren = false
                clipToPadding = false
                addView(container)
            }

            val options = listOf(
                UiArcRadioCardsGroup.RadioCardOption(
                    value = "1",
                    title = "1 test per day",
                    description = "Light daily practice to keep momentum"
                ),
                UiArcRadioCardsGroup.RadioCardOption(
                    value = "2",
                    title = "2 tests per day",
                    description = "Steady rhythm to improve consistency"
                ),
                UiArcRadioCardsGroup.RadioCardOption(
                    value = "3",
                    title = "3 tests per day",
                    description = "Accelerate syllabus coverage"
                ),
                UiArcRadioCardsGroup.RadioCardOption(
                    value = "5",
                    title = "5 tests per day",
                    description = "Intensive focus for upcoming exams"
                )
            )

            var dialog: com.eve.app.designsystem.TelegramNativeAlertDialog? = null

            radioGroup.setCardOptions(options, currentGoal.toString()) { selectedValue ->
                val chosenGoal = selectedValue.toIntOrNull() ?: 1
                prefs.edit().putInt(StreakHelper.KEY_DAILY_GOAL, chosenGoal).apply()
                refreshText()
                if (UiArcMotion.isMotionEnabled(this)) {
                    radioGroup.postDelayed({ dialog?.dismiss() }, 200L)
                } else {
                    dialog?.dismiss()
                }
            }

            dialog = com.eve.app.designsystem.TelegramNativeAlertDialog.Builder(this)
                .setTitle("Select Daily Goal")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupTargetExamsSetting() {
        binding.rowTargetExams.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val exams = com.eve.app.data.repository.ExamRepository().getExams()
                    if (isFinishing || isDestroyed) return@launch
                    targetExamsDialog?.dismiss()
                    targetExamsDialog = TargetExamsBottomSheet.show(this@SettingsActivity, exams).also { dlg ->
                        dlg.setOnDismissListener {
                            if (targetExamsDialog === dlg) targetExamsDialog = null
                        }
                    }
                } catch (e: Exception) {
                    AppBulletin.showError(this@SettingsActivity, "Could not load exams: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun refreshLanguageText() {
        val current = AppCompatDelegate.getApplicationLocales()
        val text = when {
            current.isEmpty -> "System default"
            current.toLanguageTags().startsWith("hi") -> "हिन्दी"
            else -> "English"
        }
        binding.tvLanguageValue.text = text
    }

    private fun setupLanguageSetting() {
        refreshLanguageText()

        binding.rowLanguage.setOnClickListener {
            EveNavigationHelper.navigate(
                this,
                Intent(this, LanguageSelectActivity::class.java)
            )
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
        if (isFinishing || isDestroyed || deleteConfirmationDialog?.isShowing == true) return
        if (!hasSettingsOwner() || accountDeletionJob?.isActive == true) { finish(); return }
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Warning: Deleting your account is permanent. All your data (profile, test attempts, results, history, leaderboard entries, pinned tests and notifications) will be erased and cannot be recovered. Do you want to continue?")
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel") { d, _ ->
                deleteTimeoutJob?.cancel()
                d.dismiss()
            }
            .makeRed(DialogInterface.BUTTON_POSITIVE)
            .show()
        deleteConfirmationDialog = dialog

        deleteConfirmArmedAt = android.os.SystemClock.elapsedRealtime()
        requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).setOnClickListener {
            if (!dialog.isShowing) return@setOnClickListener
            if (android.os.SystemClock.elapsedRealtime() - deleteConfirmArmedAt < 280L) {
                return@setOnClickListener
            }
            deleteTimeoutJob?.cancel()
            dialog.dismiss()
            executeAccountDeletion()
        }

        deleteTimeoutJob?.cancel()
        deleteTimeoutJob = lifecycleScope.launch {
            kotlinx.coroutines.delay(6000L)
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            deleteTimeoutJob?.cancel()
            if (deleteConfirmationDialog === dialog) deleteConfirmationDialog = null
        }
    }

    private fun showProgress(msg: String) {
        dismissProgress()
        val builder = TelegramNativeAlertDialog.Builder(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            val space = resources.getDimensionPixelSize(R.dimen.design_card_padding)
            setPadding(space, space, space, space)
            gravity = android.view.Gravity.CENTER_VERTICAL
            addView(TelegramRadialProgressView(this@SettingsActivity), android.widget.LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.design_ring_size),
                resources.getDimensionPixelSize(R.dimen.design_ring_size)))
            addView(TextView(this@SettingsActivity).apply {
                text = msg
                setPadding(resources.getDimensionPixelSize(R.dimen.design_section_gap), 0, 0, 0)
                androidx.core.widget.TextViewCompat.setTextAppearance(this, com.eve.app.R.style.TextAppearance_Eve_Token_Body)
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
        if (!hasSettingsOwner() || accountDeletionJob?.isActive == true) {
            if (!hasSettingsOwner()) finish()
            return
        }
        val user = settingsAuth.currentUser ?: return
        val ownerUid = requireNotNull(settingsOwnerUid)
        if (user.uid != ownerUid) { finish(); return }
        showProgress("Deleting account and data...")

        accountDeletionJob = lifecycleScope.launch {
            try {
                if (!hasSettingsOwner()) { finish(); return@launch }
                val response = ApiClient.apiService.deleteAccount()
                if (!hasSettingsOwner()) { finish(); return@launch }
                if (!response.success) {
                    throw IllegalStateException(response.error ?: "Backend account deletion was not confirmed")
                }

                deletingFirebaseCredential = true
                try {
                    user.delete().await()
                } catch (authErr: Exception) {
                    deletingFirebaseCredential = false
                    if (authErr is FirebaseAuthRecentLoginRequiredException) {
                        dismissProgress()
                        if (hasSettingsOwner()) promptReauthentication()
                        else finish()
                        return@launch
                    } else {
                        throw authErr
                    }
                }

                com.eve.app.util.NotificationStore.clearDeletedUser(this@SettingsActivity, user.uid)
                com.eve.app.data.local.TestSessionStore(this@SettingsActivity).clearAllForUser(user.uid)
                com.eve.app.data.local.PendingSubmissionStore(this@SettingsActivity).clearAllForUser(user.uid)
                com.eve.app.util.AttemptLimitManager.clearLocalForUser(this@SettingsActivity, user.uid)
                ProfilePhotoManager.removePhoto(this@SettingsActivity, user.uid)
                com.eve.app.ui.mistakes.MistakesPreferenceScope.clearDeletedUser(
                    this@SettingsActivity, user.uid
                )
                com.eve.app.ui.home.HomeRecentExamStore.clearDeletedUser(
                    this@SettingsActivity, user.uid
                )
                com.eve.app.ui.home.HomeSearchHistory.clear(
                    this@SettingsActivity, user.uid
                )
                com.eve.app.data.repository.PremiumRepository.clearCacheForLogout(user.uid, this@SettingsActivity)
                val survivingUid = settingsAuth.currentUser?.uid
                if (!SettingsDeletionBoundary.mayClearGlobalSession(ownerUid, survivingUid)) {
                    dismissProgress()
                    finish()
                    return@launch
                }
                com.eve.app.util.SessionManager.clear()
                settingsAuth.signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(this@SettingsActivity, gso).signOut()

                dismissProgress()
                AppBulletin.showSuccess(this@SettingsActivity, "Account deleted successfully")

                val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                dismissProgress()
                if (isFinishing || isDestroyed) return@launch
                if (!hasSettingsOwner()) { finish(); return@launch }
                TelegramNativeAlertDialog.Builder(this@SettingsActivity)
                    .setTitle("Account Deletion Failed")
                    .setMessage(e.localizedMessage ?: "Could not delete account. Please try again.")
                    .setPositiveButton("OK", null)
                    .show()
            } finally {
                deletingFirebaseCredential = false
            }
        }
    }

    private fun promptReauthentication() {
        if (isFinishing || isDestroyed || passwordReauthenticationDialog?.isShowing == true) return
        if (!hasSettingsOwner()) { finish(); return }
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (user.providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID }) {
            val email = user.email?.takeIf { it.isNotBlank() } ?: return
            val field = android.widget.EditText(this).apply {
                hint = "Current account password"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                isSingleLine = true
                contentDescription = "Current account password"
            }
            val container = android.widget.FrameLayout(this).apply {
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, 0, pad, 0)
                addView(field, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }
            val dialog = TelegramNativeAlertDialog.Builder(this)
                .setTitle("Confirm Account Identity")
                .setMessage("Enter your current password to complete account deletion.")
                .setView(container)
                .setPositiveButton("Verify", null)
                .setNegativeButton("Cancel", null)
                .create()
            dialog.setOnShowListener {
                requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).setOnClickListener {
                    if (!dialog.isShowing) return@setOnClickListener
                    val password = field.text?.toString().orEmpty()
                    if (password.isBlank()) {
                        field.error = "Password required"
                        return@setOnClickListener
                    }
                    requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).isEnabled = false
                    lifecycleScope.launch {
                        try {
                            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
                            field.text?.clear()
                            dialog.dismiss()
                            if (hasSettingsOwner() && !isFinishing && !isDestroyed) executeAccountDeletion()
                            else finish()
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            field.text?.clear()
                            field.error = "Verification failed. Check your password."
                            requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).isEnabled = true
                        }
                    }
                }
            }
            dialog.setOnDismissListener {
                field.text?.clear()
                if (passwordReauthenticationDialog === dialog) passwordReauthenticationDialog = null
            }
            dialog.show()
            passwordReauthenticationDialog = dialog
            return
        }
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Re-authentication Required")
            .setMessage("Sign in with your linked Google account to finish deletion.")
            .setPositiveButton("Sign In") { _, _ ->
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(this, gso)
                client.signOut().addOnCompleteListener {
                    if (!isFinishing && !isDestroyed) {
                        if (hasSettingsOwner()) reauthLauncher.launch(client.signInIntent)
                        else finish()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        logoutJob?.cancel()
        accountDeletionJob?.cancel()
        deleteTimeoutJob?.cancel()
        deleteConfirmationDialog?.dismiss()
        deleteConfirmationDialog = null
        passwordReauthenticationDialog?.dismiss()
        passwordReauthenticationDialog = null
        targetExamsDialog?.dismiss()
        targetExamsDialog = null
        dismissProgress()
        super.onDestroy()
    }

    override fun onApplyThemePalette(palette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette) {
        super.onApplyThemePalette(palette)
        if (::binding.isInitialized) {
            val radius = 8 * resources.displayMetrics.density
            val rows = listOf(
                binding.rowAppearance,
                binding.rowReminder,
                binding.rowDailyGoal,
                binding.rowTargetExams,
                binding.rowLanguage,
                binding.rowAbout,
                binding.rowLogout,
                binding.rowDeleteAccount
            )
            rows.forEach { row ->
                row.background = com.eve.app.designsystem.TelegramThemeSelectors.getRoundRectSelectorDrawable(
                    radius,
                    palette.actionSelector
                )
            }
        }
    }
}

package com.eve.app.ui.settings

import com.eve.app.designsystem.RadioCell
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.telegram.animation.TelegramAnimationRuntime

import com.eve.app.ui.common.EveBaseActivity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import com.eve.app.designsystem.TelegramRadialProgressView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.eve.app.util.AppBulletin
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
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
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.IconBackgroundColors

/**
 * Main Settings Hub powered by real ported Telegram UI components:
 * ActionBar (with QR menu item), HeaderCell, TextCell with IconBackgroundColors chips,
 * TextCheckCell, ShadowSectionCell, and TextInfoPrivacyCell.
 */
class SettingsActivity : EveBaseActivity() {

    private var progressDialog: TelegramNativeAlertDialog? = null
    private var passwordReauthenticationDialog: TelegramNativeAlertDialog? = null
    private var deleteConfirmationDialog: TelegramNativeAlertDialog? = null
    private var targetExamsDialog: android.app.Dialog? = null
    private var deleteConfirmArmedAt: Long = 0L
    private var deleteTimeoutJob: kotlinx.coroutines.Job? = null
    private val settingsAuth = FirebaseAuth.getInstance()
    private var settingsOwnerUid: String? = null
    private var logoutJob: Job? = null
    private var accountDeletionJob: Job? = null
    private var deletingFirebaseCredential = false
    private var uiInitialized = false

    private lateinit var actionBar: ActionBar
    private lateinit var rowReminder: TextCheckCell
    private lateinit var rowDailyGoal: TextCell
    private lateinit var rowTargetExams: TextCell
    private lateinit var rowAppearance: TextCell
    private lateinit var rowPasscode: TextCell
    private lateinit var rowStorage: TextCell
    private lateinit var rowLanguage: TextCell
    private lateinit var rowLanguageScreen: TextCell
    private lateinit var rowAbout: TextCell
    private lateinit var rowLogout: TextCell
    private lateinit var rowDeleteAccount: TextCell

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

        Theme.syncFromEvePalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        // 1. Real Telegram ActionBar with QR button
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Settings")
            val menu = createMenu()
            menu.addItem(MENU_QR, R.drawable.msg_qrcode)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> finish()
                        MENU_QR -> openQrShare()
                    }
                }
            })
        }
        root.addView(actionBar, matchWrap())

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        scrollView.addView(list, matchWrap())
        root.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Section 1: Study Preferences
        list.addView(sectionHeader("Study Preferences"), matchWrap())
        rowReminder = TextCheckCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
        }
        list.addView(rowReminder, matchWrap())

        rowDailyGoal = settingsCell()
        list.addView(rowDailyGoal, matchWrap())

        rowTargetExams = settingsCell().apply {
            setTextAndColorfulIcon(
                "Target Exams",
                R.drawable.msg_settings,
                IconBackgroundColors.BLUE_DEEP.bottom,
                false
            )
        }
        list.addView(rowTargetExams, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // Section 2: App Settings
        list.addView(sectionHeader("App Settings"), matchWrap())

        rowAppearance = settingsCell().apply {
            setTextAndColorfulIcon(
                "Appearance",
                R.drawable.msg_theme,
                IconBackgroundColors.CYAN.bottom,
                true
            )
        }
        list.addView(rowAppearance, matchWrap())

        rowPasscode = settingsCell()
        list.addView(rowPasscode, matchWrap())

        rowStorage = settingsCell()
        list.addView(rowStorage, matchWrap())

        rowLanguage = settingsCell()
        list.addView(rowLanguage, matchWrap())

        rowLanguageScreen = settingsCell().apply {
            setTextAndColorfulIcon(
                "Language Picker",
                R.drawable.msg_language,
                IconBackgroundColors.PURPLE.bottom,
                false
            )
        }
        list.addView(rowLanguageScreen, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // Section 3: Support & Info
        list.addView(sectionHeader("Information"), matchWrap())
        rowAbout = settingsCell().apply {
            setTextAndColorfulIcon(
                "About Eve",
                R.drawable.msg_info,
                IconBackgroundColors.GRAY.bottom,
                false
            )
        }
        list.addView(rowAbout, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // Section 4: Account
        list.addView(sectionHeader("Account"), matchWrap())
        rowLogout = settingsCell().apply {
            setText("Log Out", true)
            setTextColor(Theme.getColor(Theme.key_text_RedRegular))
        }
        list.addView(rowLogout, matchWrap())

        rowDeleteAccount = settingsCell().apply {
            setTextAndColorfulIcon(
                "Delete Account",
                R.drawable.msg_block,
                IconBackgroundColors.RED.bottom,
                false
            )
            setTextColor(Theme.getColor(Theme.key_text_RedRegular))
        }
        list.addView(rowDeleteAccount, matchWrap())

        val footer = TextInfoPrivacyCell(this).apply {
            setText("Manage your study reminders, appearance, passcode lock, storage cache, and account preferences.")
        }
        list.addView(footer, matchWrap())

        uiInitialized = true

        rowAppearance.setOnClickListener {
            startActivity(Intent(this, ThemeActivity::class.java))
        }
        rowPasscode.setOnClickListener {
            startActivity(Intent(this, PasscodeActivity::class.java))
        }
        rowStorage.setOnClickListener {
            startActivity(Intent(this, CacheControlActivity::class.java))
        }
        rowLanguageScreen.setOnClickListener {
            startActivity(Intent(this, LanguageSelectActivity::class.java))
        }
        rowAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        setupReminderSetting()
        setupDailyGoalSetting()
        setupTargetExamsSetting()
        setupLanguageSetting()
        setupLogoutSetting()
        setupDeleteAccountSetting()
        refreshDynamicValues()
    }

    private fun openQrShare() {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid ?: "student"
        val username = (user?.displayName ?: "").replace(" ", "_").lowercase()
        val normalized = com.eve.app.ui.profile.ProfileQrPayload.normalize(username)
        val payload = if (normalized != null) {
            com.eve.app.ui.profile.ProfileQrPayload.encode(normalized)
        } else {
            "eve-mock-test://user/$uid"
        }
        startActivity(Intent(this, com.eve.app.ui.profile.QrShareActivity::class.java).setData(android.net.Uri.parse(payload)))
    }

    private fun refreshDynamicValues() {
        if (!uiInitialized) return
        val passcodeVal = if (com.eve.app.util.PasscodeManager.isPasscodeEnabled(this)) "On" else "Off"
        rowPasscode.setTextAndValueAndColorfulIcon(
            "Passcode Lock",
            passcodeVal,
            false,
            R.drawable.ic_lock_header,
            IconBackgroundColors.GREEN.top,
            IconBackgroundColors.GREEN.bottom,
            true
        )
        val storageVal = com.eve.app.util.CacheManager.formatFileSize(com.eve.app.util.CacheManager.getTotalCacheSize(this))
        rowStorage.setTextAndValueAndColorfulIcon(
            "Data and Storage",
            storageVal,
            false,
            R.drawable.msg_filled_storageusage,
            IconBackgroundColors.BLUE.top,
            IconBackgroundColors.BLUE.bottom,
            true
        )
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
        if (!hasSettingsOwner() && !deletingFirebaseCredential) { finish(); return }
        if (!uiInitialized) return
        refreshLanguageText()
        refreshDynamicValues()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("settings_owner_uid", settingsOwnerUid)
    }

    private fun setupLogoutSetting() {
        rowLogout.setOnClickListener {
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
        if (!rowLogout.isEnabled || logoutJob?.isActive == true) return
        rowLogout.isEnabled = false
        logoutJob = lifecycleScope.launch {
            try {
                if (!hasSettingsOwner()) { finish(); return@launch }
                com.eve.app.util.ProfilePresenceClient.revokeBeforeLogout()
                if (!hasSettingsOwner()) { finish(); return@launch }
                performLocalLogout()
            } finally {
                if (hasSettingsOwner() && uiInitialized) rowLogout.isEnabled = true
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
            rowDailyGoal.setTextAndValueAndColorfulIcon(
                "Daily Goal",
                "$goal tests per day",
                false,
                R.drawable.msg_settings,
                IconBackgroundColors.ORANGE.top,
                IconBackgroundColors.ORANGE.bottom,
                true
            )
        }
        refreshText()
        rowDailyGoal.setOnClickListener {
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

            val options = listOf(
                "1" to "1 test per day — Light daily practice",
                "2" to "2 tests per day — Steady rhythm to improve consistency",
                "3" to "3 tests per day — Accelerate syllabus coverage",
                "5" to "5 tests per day — Intensive focus for upcoming exams"
            )

            val radioCells = mutableListOf<RadioCell>()
            var dialog: TelegramNativeAlertDialog? = null

            for ((value, label) in options) {
                val cell = RadioCell(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setText(label, value == currentGoal.toString(), true)
                    setOnClickListener {
                        val chosenGoal = value.toIntOrNull() ?: 1
                        prefs.edit().putInt(StreakHelper.KEY_DAILY_GOAL, chosenGoal).apply()
                        refreshText()
                        radioCells.forEach { it.setChecked(it === this, true) }
                        if (TelegramAnimationRuntime.isMotionEnabled(this@SettingsActivity)) {
                            postDelayed({ dialog?.dismiss() }, 200L)
                        } else {
                            dialog?.dismiss()
                        }
                    }
                }
                radioCells.add(cell)
                container.addView(cell)
            }

            val dialogView = android.widget.ScrollView(this).apply {
                clipChildren = false
                clipToPadding = false
                addView(container)
            }

            dialog = TelegramNativeAlertDialog.Builder(this)
                .setTitle("Select Daily Goal")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupTargetExamsSetting() {
        rowTargetExams.setOnClickListener {
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
        if (!uiInitialized) return
        val current = AppCompatDelegate.getApplicationLocales()
        val text = when {
            current.isEmpty -> "System default"
            current.toLanguageTags().startsWith("hi") -> "हिन्दी"
            else -> "English"
        }
        rowLanguage.setTextAndValueAndColorfulIcon(
            "Language",
            text,
            false,
            R.drawable.msg_language,
            IconBackgroundColors.PURPLE.top,
            IconBackgroundColors.PURPLE.bottom,
            true
        )
    }

    private fun setupLanguageSetting() {
        refreshLanguageText()

        rowLanguage.setOnClickListener {
            val options = arrayOf("System default", "English", "हिन्दी")
            val current = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            val selectedIdx = when {
                current.isEmpty -> 0
                current.toLanguageTags().startsWith("hi") -> 2
                else -> 1
            }

            com.eve.app.designsystem.TelegramNativeAlertDialog.Builder(this)
                .setTitle("Select App Language")
                .setSingleChoiceItems(options, selectedIdx) { dialog, which ->
                    val locales = when (which) {
                        1 -> androidx.core.os.LocaleListCompat.forLanguageTags("en")
                        2 -> androidx.core.os.LocaleListCompat.forLanguageTags("hi")
                        else -> androidx.core.os.LocaleListCompat.getEmptyLocaleList()
                    }
                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
                    refreshLanguageText()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupReminderSetting() {
        var enabled = ReminderScheduler.isEnabled(this)
        rowReminder.setTextAndCheck("Daily Study Reminder", enabled, true)
        rowReminder.setOnClickListener {
            enabled = !enabled
            ReminderScheduler.setEnabled(this, enabled)
            rowReminder.setTextAndCheck("Daily Study Reminder", enabled, true)
        }
    }

    private fun setupDeleteAccountSetting() {
        rowDeleteAccount.setOnClickListener {
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

    private fun sectionHeader(title: String): HeaderCell = HeaderCell(this).apply {
        setText(title)
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
    }

    private fun settingsCell(): TextCell = TextCell(this).apply {
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        foreground = Theme.getSelectorDrawable(false)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    companion object {
        private const val MENU_QR = 1
    }
}

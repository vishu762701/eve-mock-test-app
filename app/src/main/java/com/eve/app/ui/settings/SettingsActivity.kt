package com.eve.app.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.AppContentRepository
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.designsystem.TelegramRadialProgressView
import com.eve.app.ui.about.AboutActivity
import com.eve.app.ui.about.ContentDisplayActivity
import com.eve.app.ui.common.TelegramHostActivity
import com.eve.app.ui.home.TargetExamsBottomSheet
import com.eve.app.ui.login.LoginActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.SubmissionRetryPolicy
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.telegram.ui.SettingsActivity as TelegramSettingsActivity

/**
 * Host Activity for Telegram Settings port.
 * Extends TelegramHostActivity (INavigationLayout) and delegates visible rendering
 * directly to org.telegram.ui.SettingsActivity (Telegram BaseFragment + UniversalRecyclerView).
 * Implements TelegramSettingsDelegate to provide EVE user profile info and handle actions.
 */
class SettingsActivity : TelegramHostActivity<TelegramSettingsActivity>(), TelegramSettingsDelegate {

    private var progressDialog: TelegramNativeAlertDialog? = null
    private var passwordReauthenticationDialog: TelegramNativeAlertDialog? = null
    private var logoutConfirmationDialog: TelegramNativeAlertDialog? = null
    private var deleteConfirmationDialog: TelegramNativeAlertDialog? = null
    private var targetExamsDialog: android.app.Dialog? = null
    private var targetExamsJob: Job? = null
    private var deleteConfirmArmedAt: Long = 0L
    private var deleteTimeoutJob: Job? = null
    private val settingsAuth = FirebaseAuth.getInstance()
    private var settingsOwnerUid: String? = null
    private var logoutJob: Job? = null
    private var accountDeletionJob: Job? = null
    private var deletingFirebaseCredential = false

    private fun resolveCurrentUid(): String? {
        val testUid = com.eve.app.ui.profile.ProfileEditorAccountGuard.testOverrideUid
            ?: if (intent?.getBooleanExtra("is_test_intent", false) == true) {
                intent?.getStringExtra(com.eve.app.ui.profile.ProfileEditorAccountGuard.EXTRA_OWNER_UID)
            } else null
        return testUid ?: settingsAuth.currentUser?.uid
    }

    private fun hasSettingsOwner(): Boolean =
        SubmissionRetryPolicy.ownsSubmission(settingsOwnerUid, resolveCurrentUid())

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

    override fun onBeforeCreateFragment(savedInstanceState: Bundle?): Boolean {
        settingsOwnerUid = if (savedInstanceState?.containsKey("settings_owner_uid") == true) {
            savedInstanceState.getString("settings_owner_uid")
        } else resolveCurrentUid()
        if (!hasSettingsOwner()) {
            finish()
            return false
        }
        return true
    }

    override fun createTelegramFragment(savedInstanceState: Bundle?): TelegramSettingsActivity? {
        return TelegramSettingsActivity().apply {
            setEveDelegate(this@SettingsActivity)
        }
    }

    // --- TelegramSettingsDelegate Implementation ---

    override fun getUserDisplayName(): String {
        val user = settingsAuth.currentUser
        val name = user?.displayName
        return if (!name.isNullOrBlank()) name else (user?.email?.substringBefore("@") ?: "Student")
    }

    override fun getUserSubtitle(): String {
        val user = settingsAuth.currentUser
        val phone = user?.phoneNumber
        val email = user?.email
        return when {
            !phone.isNullOrBlank() -> phone
            !email.isNullOrBlank() -> email
            else -> "@eve_student"
        }
    }

    override fun getUserUidHash(): Long {
        val uid = resolveCurrentUid() ?: return 1L
        return uid.hashCode().toLong()
    }

    private var cachedPhotoUid: String? = null
    private var cachedPhotoDrawable: Drawable? = null

    override fun getUserPhoto(): Drawable? {
        val uid = resolveCurrentUid() ?: return null
        if (cachedPhotoUid == uid && cachedPhotoDrawable != null) {
            return cachedPhotoDrawable
        }
        val bitmap = ProfilePhotoManager.loadBitmap(this, uid) ?: return null
        val drawable = RoundedBitmapDrawableFactory.create(resources, bitmap).apply {
            isCircular = true
        }
        cachedPhotoUid = uid
        cachedPhotoDrawable = drawable
        return drawable
    }

    override fun getVersionString(): String {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "1.0"
        }
        return "EVE Android v$versionName"
    }

    override fun onRowClicked(id: Int) {
        if (isFinishing || isDestroyed) return
        when (id) {
            TelegramSettingsActivity.ROW_ACCOUNT -> {
                startActivity(Intent(this, com.eve.app.ui.profile.ProfileActivity::class.java))
            }
            TelegramSettingsActivity.ROW_CHAT -> {
                startActivity(Intent(this, ThemeActivity::class.java))
            }
            TelegramSettingsActivity.ROW_PRIVACY -> {
                startActivity(Intent(this, PasscodeActivity::class.java))
            }
            TelegramSettingsActivity.ROW_NOTIFICATIONS -> {
                startActivity(Intent(this, com.eve.app.ui.notifications.NotificationsActivity::class.java))
            }
            TelegramSettingsActivity.ROW_DATA -> {
                startActivity(Intent(this, CacheControlActivity::class.java))
            }
            TelegramSettingsActivity.ROW_FOLDERS -> {
                startActivity(Intent(this, CacheControlActivity::class.java))
            }
            TelegramSettingsActivity.ROW_DEVICES -> {
                startActivity(Intent(this, PasscodeActivity::class.java))
            }
            TelegramSettingsActivity.ROW_POWER -> {
                startActivity(Intent(this, ThemeActivity::class.java))
            }
            TelegramSettingsActivity.ROW_LANGUAGE -> {
                startActivity(Intent(this, LanguageSelectActivity::class.java))
            }
            TelegramSettingsActivity.ROW_TARGET_EXAMS -> {
                showTargetExams()
            }
            TelegramSettingsActivity.ROW_ASK -> {
                startActivity(Intent(this, com.eve.app.ui.feedback.FeedbackActivity::class.java))
            }
            TelegramSettingsActivity.ROW_FAQ -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
            TelegramSettingsActivity.ROW_FEATURES -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
            TelegramSettingsActivity.ROW_POLICY -> {
                val intent = Intent(this, ContentDisplayActivity::class.java).apply {
                    putExtra(ContentDisplayActivity.EXTRA_CONTENT_TYPE, AppContentRepository.TYPE_PRIVACY)
                }
                startActivity(intent)
            }
        }
    }

    override fun onQrClicked() {
        openQrShare()
    }

    override fun onSearchClicked() {
    }

    override fun onLogOutClicked() {
        showLogoutConfirmation()
    }

    override fun onDeleteAccountClicked() {
        showDeleteAccountConfirmation()
    }

    override fun onAvatarClicked() {
        startActivity(Intent(this, com.eve.app.ui.profile.ProfileActivity::class.java))
    }

    // --- Actions & Helpers ---

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

    private fun showTargetExams() {
        if (targetExamsDialog?.isShowing == true || targetExamsJob?.isActive == true) return
        targetExamsJob = lifecycleScope.launch {
            try {
                val exams = com.eve.app.data.repository.ExamRepository().getExams()
                if (isFinishing || isDestroyed) return@launch
                targetExamsDialog?.dismiss()
                targetExamsDialog = TargetExamsBottomSheet.show(this@SettingsActivity, exams).also { dlg ->
                    dlg.setOnDismissListener {
                        if (targetExamsDialog === dlg) targetExamsDialog = null
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (!isFinishing && !isDestroyed) {
                    AppBulletin.showError(this@SettingsActivity, "Could not load exams: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showLogoutConfirmation() {
        if (isFinishing || isDestroyed || logoutConfirmationDialog?.isShowing == true) return
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle(getString(R.string.LogOut))
            .setMessage(getString(R.string.home_logout_confirmation))
            .setPositiveButton(getString(R.string.LogOut)) { _, _ ->
                executeLogout()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
        logoutConfirmationDialog = dialog
    }

    private fun executeLogout() {
        if (!hasSettingsOwner()) { finish(); return }
        if (logoutJob?.isActive == true) return
        logoutJob = lifecycleScope.launch {
            if (!hasSettingsOwner()) { finish(); return@launch }
            try {
                com.eve.app.util.ProfilePresenceClient.revokeBeforeLogout()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Best-effort presence revocation; never block offline local logout.
            }
            if (!hasSettingsOwner()) { finish(); return@launch }
            performLocalLogout()
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

    override fun onStart() {
        super.onStart()
        if (!hasSettingsOwner() && !deletingFirebaseCredential) { finish(); return }
        settingsAuth.addAuthStateListener(settingsAccountListener)
    }

    override fun onStop() {
        settingsAuth.removeAuthStateListener(settingsAccountListener)
        dismissProgress()
        deleteTimeoutJob?.cancel()
        logoutConfirmationDialog?.dismiss()
        logoutConfirmationDialog = null
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
        cachedPhotoDrawable = null
        if (!hasSettingsOwner() && !deletingFirebaseCredential) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("settings_owner_uid", settingsOwnerUid)
    }

    override fun onDestroy() {
        logoutJob?.cancel()
        accountDeletionJob?.cancel()
        targetExamsJob?.cancel()
        deleteTimeoutJob?.cancel()
        logoutConfirmationDialog?.dismiss()
        logoutConfirmationDialog = null
        deleteConfirmationDialog?.dismiss()
        deleteConfirmationDialog = null
        passwordReauthenticationDialog?.dismiss()
        passwordReauthenticationDialog = null
        targetExamsDialog?.dismiss()
        targetExamsDialog = null
        dismissProgress()
        super.onDestroy()
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showDeleteAccountConfirmation() {
        if (!hasSettingsOwner()) { finish(); return }
        // Official Telegram AndroidUtilities.isSafeToShow():6255-6267.
        // Never stack destructive windows or show one during Activity teardown.
        if (isFinishing || isDestroyed || deleteConfirmationDialog?.isShowing == true) return
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle(getString(R.string.DeleteAccount))
            .setMessage("Warning: Deleting your account is permanent. All your data (profile, test attempts, results, history, leaderboard entries, pinned tests and notifications) will be erased and cannot be recovered. Do you want to continue?")
            .setPositiveButton(getString(R.string.Delete), null)
            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                deleteTimeoutJob?.cancel()
                d.dismiss()
            }
            .makeRed(DialogInterface.BUTTON_POSITIVE)
            .show()
        deleteConfirmationDialog = dialog
        dialog.setOnDismissListener {
            deleteTimeoutJob?.cancel()
            if (deleteConfirmationDialog === dialog) deleteConfirmationDialog = null
        }

        deleteConfirmArmedAt = android.os.SystemClock.elapsedRealtime()
        requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).setOnClickListener {
            if (!dialog.isShowing) return@setOnClickListener
            // Enforce UIArc duplicate-action guard (280ms)
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
            if (deleteConfirmationDialog?.isShowing == true) {
                deleteConfirmationDialog?.dismiss()
            }
        }
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
                try {
                    val response = ApiClient.apiService.deleteAccount()
                    if (!response.success) {
                        val err = response.error.orEmpty()
                        val isAlreadyDeleted = err.contains("not found", ignoreCase = true) ||
                            err.contains("already deleted", ignoreCase = true) ||
                            err.contains("404", ignoreCase = true)
                        if (!isAlreadyDeleted) {
                            throw IllegalStateException(response.error ?: "Backend account deletion was not confirmed")
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    val isHttp404 = (e as? retrofit2.HttpException)?.code() == 404
                    val msg = e.localizedMessage.orEmpty()
                    val isAlreadyDeleted = isHttp404 ||
                        msg.contains("not found", ignoreCase = true) ||
                        msg.contains("already deleted", ignoreCase = true)
                    if (!isAlreadyDeleted) {
                        throw e
                    }
                }
                if (!hasSettingsOwner()) { finish(); return@launch }

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
                            if (!dialog.isShowing || isFinishing || isDestroyed) return@launch
                            field.text?.clear()
                            field.error = "Verification failed. Check your password."
                            dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled = true
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
}

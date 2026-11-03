package com.eve.app.ui.settings

import com.eve.app.util.SubmissionRetryPolicy
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prevent account-switch callbacks from signing out or deleting a second user. */
class SettingsAccountIsolationTest {
    private val src = File("src/main/java/com/eve/app/ui/settings/SettingsActivity.kt").readText()

    @Test fun postDeletionSignoutOnlyTargetsTheDeletedOrAutomaticallySignedOutUser() {
        assertTrue(SettingsDeletionBoundary.mayClearGlobalSession("student-a", null))
        assertTrue(SettingsDeletionBoundary.mayClearGlobalSession("student-a", "student-a"))
        assertFalse(SettingsDeletionBoundary.mayClearGlobalSession("student-a", "student-b"))
        assertFalse(SettingsDeletionBoundary.mayClearGlobalSession("", null))
    }

    @Test fun settingsOwnerIsBoundToOpeningOrRestoredSession() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("student-a", "student-a"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("student-a", "student-b"))
        assertTrue(src.contains("settingsOwnerUid = if (savedInstanceState?.containsKey(\"settings_owner_uid\") == true)"))
        assertTrue(src.contains("outState.putString(\"settings_owner_uid\", settingsOwnerUid)"))
        assertTrue(src.contains("settingsAuth.addAuthStateListener(settingsAccountListener)"))
        assertTrue(src.contains("settingsAuth.removeAuthStateListener(settingsAccountListener)"))
        assertTrue(src.contains("logoutJob?.cancel()"))
        assertTrue(src.contains("accountDeletionJob?.cancel()"))
    }

    @Test fun delayedLogoutRechecksIdentityAfterPresenceRevocation() {
        val logout = src.substringAfter("private fun executeLogout()").substringBefore("private fun setupDailyGoalSetting()")
        val call = logout.indexOf("ProfilePresenceClient.revokeBeforeLogout()")
        val after = logout.indexOf("if (!hasSettingsOwner()) { finish(); return@launch }", call)
        val local = logout.indexOf("performLocalLogout()", after)
        assertTrue(call >= 0 && after > call && local > after)
        assertTrue(logout.contains("private fun performLocalLogout() {\n        if (!hasSettingsOwner())"))
    }

    @Test fun deletionRechecksOwnerAfterNetworkAndBeforeCredentialRemoval() {
        val delete = src.substringAfter("private fun executeAccountDeletion()")
            .substringBefore("private fun promptReauthentication()")
        val net = delete.indexOf("ApiClient.apiService.deleteAccount()")
        val guard = delete.indexOf("if (!hasSettingsOwner()) { finish(); return@launch }", net)
        val credential = delete.indexOf("user.delete().await()", guard)
        assertTrue(net >= 0 && guard > net && credential > guard)
        assertTrue(delete.contains("deletingFirebaseCredential = true"))
        assertTrue(delete.contains("SettingsDeletionBoundary.mayClearGlobalSession(ownerUid, survivingUid)"))
        val globalClear = delete.indexOf("SessionManager.clear()")
        val surviving = delete.indexOf("SettingsDeletionBoundary.mayClearGlobalSession")
        assertTrue(globalClear > surviving)
        assertTrue(delete.contains("catch (cancel: CancellationException)"))
    }

    @Test fun homeLogoutAndResumeCannotAdoptAnotherAccount() {
        val home = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val resume = home.substringAfter("override fun onResume()")
            .substringBefore("override fun onPause()")
        assertTrue(resume.contains("if (!searchOwnerCurrent) { finish(); return }"))
        val logout = home.substringAfter("private fun logout()")
            .substringBefore("private fun prepareDrawerSurface()")
        val revoke = logout.indexOf("ProfilePresenceClient.revokeBeforeLogout()")
        val guard = logout.indexOf("if (!searchOwnerCurrent) { finish(); return@launch }", revoke)
        val local = logout.indexOf("performLocalLogout()", guard)
        assertTrue(revoke >= 0 && guard > revoke && local > guard)
        assertTrue(logout.contains("private fun performLocalLogout() {\n        if (!searchOwnerCurrent)"))
    }

    @Test fun reauthenticationDoesNotContinueForAnUnrelatedUser() {
        val google = src.substringAfter("private val reauthLauncher =")
            .substringBefore("override fun onCreate(")
        assertTrue(google.contains("if (!hasSettingsOwner() || isFinishing || isDestroyed)"))
        assertTrue(google.contains("if (hasSettingsOwner() && !isFinishing && !isDestroyed) executeAccountDeletion()"))
        val prompt = src.substringAfter("private fun promptReauthentication()").substringBefore("override fun onDestroy()")
        assertTrue(prompt.contains("if (!hasSettingsOwner()) { finish(); return }"))
        assertTrue(prompt.contains("if (hasSettingsOwner() && !isFinishing && !isDestroyed) executeAccountDeletion()"))
    }
}

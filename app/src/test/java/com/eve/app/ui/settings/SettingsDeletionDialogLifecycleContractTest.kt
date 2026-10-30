package com.eve.app.ui.settings

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Prevent duplicate destructive UI without weakening the account deletion flow. */
class SettingsDeletionDialogLifecycleContractTest {
    private val settings = File("src/main/java/com/eve/app/ui/settings/SettingsActivity.kt").readText()

    @Test fun oneSourceBackedDeletionDialogPerActivityAndSafeLifecycle() {
        val open = settings.substringAfter("private fun showDeleteAccountConfirmation()")
            .substringBefore("private fun showProgress(")
        assertTrue(settings.contains("private var deleteConfirmationDialog: TelegramNativeAlertDialog? = null"))
        assertTrue(open.contains(
            "if (isFinishing || isDestroyed || deleteConfirmationDialog?.isShowing == true) return"))
        assertTrue(open.contains("val dialog = TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(open.contains(".makeRed(DialogInterface.BUTTON_POSITIVE)"))
        assertTrue(open.contains("deleteConfirmationDialog = dialog"))
        assertTrue(open.contains("if (deleteConfirmationDialog === dialog) deleteConfirmationDialog = null"))
        val destroy = settings.substringAfter("override fun onDestroy()")
        assertTrue(destroy.contains("deleteConfirmationDialog?.dismiss()"))
        assertTrue(destroy.contains("deleteConfirmationDialog = null"))
    }

    @Test fun existingDoubleTapDelayTimeoutAndAuthorizedDeletionAreUnchanged() {
        val open = settings.substringAfter("private fun showDeleteAccountConfirmation()")
            .substringBefore("private fun showProgress(")
        assertTrue(open.contains("deleteConfirmArmedAt = android.os.SystemClock.elapsedRealtime()"))
        assertTrue(open.contains("deleteConfirmArmedAt < 280L"))
        assertTrue(open.contains("kotlinx.coroutines.delay(6000L)"))
        assertTrue(open.contains("deleteTimeoutJob?.cancel()"))
        assertTrue(open.contains("executeAccountDeletion()"))
        assertTrue(open.contains("requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).setOnClickListener"))
        assertTrue(open.contains("if (!dialog.isShowing) return@setOnClickListener"))
        assertTrue(settings.contains("promptReauthentication()"))
        assertTrue(settings.contains("user.reauthenticate("))
    }
}

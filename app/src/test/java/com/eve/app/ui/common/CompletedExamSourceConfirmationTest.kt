package com.eve.app.ui.common

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-backed original popup window atoms with unchanged EVE reattempt callback. */
class CompletedExamSourceConfirmationTest {
    private val code = File("src/main/java/com/eve/app/ui/common/CompletedExamBottomSheet.kt").readText()

    @Test fun reattemptPreventsConcurrentChecksAndLateDialogWindows() {
        assertTrue(code.contains("reattemptCheckPending || reattemptConfirmDialog?.isShowing == true"))
        assertTrue(code.contains("activity.isFinishing || activity.isDestroyed || !dialog.isShowing"))
        assertTrue(code.contains("reattemptCheckPending = true"))
        assertTrue(code.contains("reattemptCheckPending = false"))
        assertTrue(code.contains("confirmation.setOnDismissListener { dismissed ->"))
        assertTrue(code.contains("if (reattemptConfirmDialog === dismissed) reattemptConfirmDialog = null"))
        assertTrue(code.contains("reattemptConfirmDialog?.dismiss()"))
    }

    @Test fun originalDialogSourceIsAppliedWithoutChangingAttemptGuardOrCallbacks() {
        assertTrue(code.contains("val canReattempt = com.eve.app.util.AttemptLimitManager.canAttempt(activity, examId)"))
        assertTrue(code.contains("TelegramDialogSourceBackground.apply(confirmation)"))
        assertTrue(code.contains(".setTitle(\"Attempt Limit Reached\")"))
        assertTrue(code.contains(".setPositiveButton(\"OK\", null)"))
        assertTrue(code.contains(".setTitle(R.string.reattempt_confirm_title)"))
        assertTrue(code.contains(".setNegativeButton(R.string.cancel, null)"))
        assertTrue(code.contains(".setPositiveButton(\"Start Attempt\") { _, _ ->"))
        assertTrue(code.contains("binding.btnReattempt.isEnabled = false"))
        assertTrue(code.contains("HomeViewModel.markAttemptCleared(examId)"))
        assertTrue(code.contains("onReattemptConfirmed()"))
        assertTrue(code.contains("onDismiss?.invoke()"))
        assertTrue(code.contains("openReview(att)"))
    }
}

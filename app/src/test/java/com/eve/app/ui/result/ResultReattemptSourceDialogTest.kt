package com.eve.app.ui.result

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-backed reattempt dialog contract in ResultActivity with Telegram chrome and state guards. */
class ResultReattemptSourceDialogTest {
    private val code = File("src/main/java/com/eve/app/ui/result/ResultActivity.kt").readText()

    @Test fun reattemptDialogPreventsReentrancyAndGuardsLifecycle() {
        assertTrue(code.contains("reattemptConfirmDialog?.isShowing == true || reattemptLimitDialog?.isShowing == true"))
        assertTrue(code.contains("if (isFinishing || isDestroyed) return@launch"))
        assertTrue(code.contains("reattemptLimitDialog?.dismiss()"))
        assertTrue(code.contains("reattemptConfirmDialog?.dismiss()"))
    }

    @Test fun reattemptUsesIsolatedTelegramHostAndPreservesDestructiveAction() {
        assertTrue(code.contains("TelegramNativeAlertDialog.Builder(this@ResultActivity)"))
        assertTrue(code.contains("requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE))"))
        assertTrue(code.contains("confirmBtn.setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.system_red_text))"))
        assertTrue(!code.contains("TelegramDialogSourceBackground.apply(limitDialog)"))
        assertTrue(code.contains("AttemptLimitManager.canAttempt"))
        assertTrue(code.contains("Attempt Limit Reached"))
        assertTrue(code.contains("Reattempt this test?"))
    }

    @Test fun reattemptFlowMaintainsFullResetAndNavigation() {
        assertTrue(code.contains("resetAttemptPost(mapOf(\"examId\" to examId))"))
        assertTrue(code.contains("clearSession(examId)"))
        assertTrue(code.contains("ResultDataHolder.clear()"))
        assertTrue(code.contains("HomeViewModel.markAttemptCleared(examId)"))
        assertTrue(code.contains("Intent(this@ResultActivity, TestActivity::class.java)"))
    }
}

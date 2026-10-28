package com.eve.app.ui.test

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-backed exam confirmation presentation; exam logic remains authoritative. */
class TelegramTestConfirmationSourceContractTest {
    private val activity = File("src/main/java/com/eve/app/ui/test/TestActivity.kt").readText()

    @Test fun backConfirmationUsesSourceWithSingleInstanceAndOriginalPauseFlow() {
        val back = activity.substringAfter("onBackPressedDispatcher.addCallback")
            .substringBefore("lifecycleScope.launch {\n            repeatOnLifecycle")
        assertTrue(back.contains("leaveConfirmDialog?.isShowing == true"))
        assertTrue(back.contains("isFinishing || isDestroyed || isLeavingTest"))
        assertTrue(back.contains("isLeavingTest = true"))
        assertTrue(back.contains("viewModel.stopTimer()"))
        assertTrue(back.contains("viewModel.pauseSession(examId, currentPos)"))
        assertTrue(back.contains("finally { finish() }"))
        assertTrue(back.contains("setOnDismissListener { leaveConfirmDialog = null }"))
        assertTrue(back.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(back.contains(".setNegativeButton(\"Continue\", null)"))
    }

    @Test fun submitConfirmationRetainsCountsAndExactlyOneSubmissionCallback() {
        val confirm = activity.substringAfter("private fun confirmSubmit()")
            .substringBefore("private fun submit()")
        assertTrue(confirm.contains("submitConfirmDialog?.isShowing == true"))
        assertTrue(confirm.contains("isFinishing || isDestroyed"))
        assertTrue(confirm.contains("QuestionPaletteAdapter.calculateSubmitDialogCounts("))
        for (field in listOf("counts.answered", "counts.notAnswered",
            "counts.markedForReview", "counts.notVisited")) {
            assertTrue("No lost submission summary: $field", confirm.contains(field))
        }
        assertTrue(confirm.contains(".setPositiveButton(\"Submit\") { _, _ -> submit() }"))
        assertTrue(confirm.contains(".setNegativeButton(\"Cancel\", null)"))
        assertTrue(confirm.contains("DesignSurfaces.dialog(dialog)"))
        assertTrue(confirm.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(confirm.contains("setOnDismissListener { submitConfirmDialog = null }"))
        val submit = activity.substringAfter("private fun submit()")
            .substringBefore("private fun showSubmissionError(")
        assertTrue(submit.contains("if (submitted) return"))
        assertTrue(submit.contains("viewModel.saveCurrentSession("))
        assertTrue(submit.contains("viewModel.buildAnswerItems()"))
        assertTrue(submit.contains("viewModel.clientAttemptId"))
    }

    @Test fun examNoticesReuseSourceHostWithoutStackingOrChangingRecoveryActions() {
        val notices = activity.substringAfter("private fun showExamNotice(")
            .substringBefore("private fun sessionTitle()")
        assertTrue("Never show a notice on destroyed Activity",
            notices.contains("if (isFinishing || isDestroyed) return null"))
        assertTrue("Only one exam status window may remain visible",
            notices.contains("statusDialog?.takeIf { it.isShowing }?.dismiss()"))
        assertTrue(notices.contains("if (statusDialog === dismissed) statusDialog = null"))
        assertTrue(notices.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue("Retry must remain wired to original submit operation",
            notices.contains(".setPositiveButton(\"Retry\") { _, _ ->") &&
            notices.contains("submit()"))
        assertTrue("Offline Save must still delegate to original queue logic",
            notices.contains(".setNegativeButton(\"Save Offline\")") &&
            notices.contains("handleOfflineSubmit(attemptName, items)"))
        assertTrue("Cancel and offline acknowledgements are unchanged",
            notices.contains(".setNeutralButton(\"Cancel\", null)") &&
            notices.contains(".setTitle(\"Submission Saved Offline\")"))
        assertTrue("Both recovery dialogs use source host",
            notices.split(".let(::showExamNotice)").size >= 3)
        val attemptLimit = activity.substringAfter("if (!canAttempt)")
            .substringBefore("viewModel.start(")
        assertTrue(attemptLimit.contains(".let(::showExamNotice)"))
        assertTrue(attemptLimit.contains(".setCancelable(false)"))
    }

    @Test fun dialogCleanupRetainsTimerAndTypographySessionCleanup() {
        val destroy = activity.substringAfter("override fun onDestroy()")
        assertTrue(destroy.contains("leaveConfirmDialog?.dismiss()"))
        assertTrue(destroy.contains("submitConfirmDialog?.dismiss()"))
        assertTrue(destroy.contains("statusDialog?.dismiss()"))
        assertTrue(destroy.contains("setTestSessionActive(false)"))
        assertTrue(activity.contains("launch { viewModel.timeUp.collect { if (it) submit() } }"))
    }
}

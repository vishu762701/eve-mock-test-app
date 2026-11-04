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

    @Test fun fallbackHistoryReadIsOwnerBoundAndErrorsAreHandledWithoutCrossAccountUi() {
        val block = code.substringAfter("binding.btnViewResult.setOnClickListener {")
            .substringBefore("var reattemptCheckPending")
        val fetch = block.indexOf("HistoryRepository().getAttempts(ownerUid)")
        val guard = block.indexOf("FirebaseAuth.getInstance().currentUser?.uid != ownerUid", fetch)
        val use = block.indexOf("attempts.firstOrNull", guard)
        assertTrue("History may not be fetched for a replacement account", fetch >= 0)
        assertTrue("Read completion must validate original identity before showing a result",
            guard > fetch && use > guard)
        assertTrue(block.contains("it.userId == ownerUid && it.examId == examId"))
        assertTrue(block.contains("reviewPending = true"))
        assertTrue(block.contains("reviewPending = false"))
        assertTrue(block.contains("!dialog.isShowing"))
        assertTrue(block.contains("catch (cancel: kotlinx.coroutines.CancellationException)"))
        assertTrue(block.contains("catch (_: Exception)"))
        assertTrue(block.contains("Could not load attempt details. Please retry."))
        assertTrue(!block.contains("getAttempts(uid)"))
        // The hosting TestActivity finishes in onDismiss. Navigation must win that race.
        assertTrue(block.contains("if (openReview(attempt)) dialog.dismiss()"))
        assertTrue(block.contains("if (openReview(found)) dialog.dismiss()"))
    }

    @Test fun originalDialogSourceIsAppliedWithoutChangingAttemptGuardOrCallbacks() {
        assertTrue(code.contains("val canReattempt = com.eve.app.util.AttemptLimitManager.canAttempt(activity, examId)"))
        assertTrue(code.contains("TelegramNativeAlertDialog.Builder(activity)"))
        assertTrue(!code.contains("TelegramDialogSourceBackground.apply(confirmation)"))
        assertTrue(code.contains("TelegramBottomSheetSourceBackdrop.install(dialog, activity)"))
        assertTrue(code.contains(".setTitle(\"Attempt Limit Reached\")"))
        assertTrue(code.contains(".setPositiveButton(\"OK\", null)"))
        assertTrue(code.contains(".setTitle(activity.getString(R.string.reattempt_confirm_title))"))
        assertTrue(code.contains(".setNegativeButton(activity.getString(R.string.cancel), null)"))
        assertTrue(code.contains(".setPositiveButton(\"Start Attempt\") { _, _ ->"))
        assertTrue(code.contains("binding.btnReattempt.isEnabled = false"))
        assertTrue(code.contains("HomeViewModel.markAttemptCleared(examId, ownerUid)"))
        assertTrue(code.contains("onReattemptConfirmed()"))
        assertTrue(code.contains("onDismiss?.invoke()"))
        assertTrue("Review helper must preserve original saved answer payload",
            code.contains("ResultDataHolder.setAnswers(att.answers, ownerUid, att.examId)"))
        assertTrue("Direct completed attempt still opens review",
            code.contains("openReview(attempt)"))
        assertTrue("Fetched completed attempt still opens review",
            code.contains("openReview(found)"))
    }
}

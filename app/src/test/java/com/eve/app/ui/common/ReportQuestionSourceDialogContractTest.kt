package com.eve.app.ui.common

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Report dialog retains its validation and server workflow with source-backed chrome. */
class ReportQuestionSourceDialogContractTest {
    @Test fun originalTelegramDialogAtomsApplyAfterShowWithoutReplacingReportForm() {
        val source = File("src/main/java/com/eve/app/ui/common/ReportQuestionDialog.kt").readText()
        val show = source.substringAfter("val dialog = MaterialAlertDialogBuilder(activity)")
            .substringBefore("val btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE)")
        assertTrue(show.contains(".setView(dialogView)"))
        assertTrue(show.contains(".setPositiveButton(\"Submit Report\", null)"))
        assertTrue(show.contains(".setNegativeButton(\"Cancel\", null)"))
        assertTrue(show.contains(".create()"))
        assertTrue(show.contains("dialog.show()"))
        assertTrue(show.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(show.indexOf("dialog.show()") < show.indexOf("TelegramDialogSourceBackground.apply(dialog)"))
    }

    @Test fun originalSecureReportPayloadAndSingleSubmissionGuardRemainUntouched() {
        val source = File("src/main/java/com/eve/app/ui/common/ReportQuestionDialog.kt").readText()
        assertTrue(source.contains("if (activity.isFinishing || activity.isDestroyed) return"))
        assertTrue(source.contains("btnSubmit.setOnClickListener"))
        assertTrue(source.contains("btnSubmit.isEnabled = false"))
        assertTrue(source.contains("flaggedRepo.flagQuestionResult("))
        for (field in listOf("questionId = questionId", "examId = examId",
                "examName = examName", "questionText = questionText",
                "reason = selectedReason", "comment = comment")) {
            assertTrue("Server report field must remain unchanged: $field", source.contains(field))
        }
        assertTrue(source.contains("if (result.isSuccess)"))
        assertTrue(source.contains("dialog.dismiss()"))
        assertTrue(source.contains("btnSubmit.isEnabled = true"))
        assertTrue(source.contains("onSubmitted?.invoke(true)"))
        assertTrue(source.contains("onSubmitted?.invoke(false)"))
    }
}

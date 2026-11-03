package com.eve.app.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Admin exam editor dialogs share original Telegram chrome without changing mutations. */
class EditExamTelegramDialogContractTest {
    @Test fun allFourEditorModalsUseTheSourceIsolatedNativeDialog() {
        val source = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertEquals("Import preview, Add Question, Edit Question and Delete Question",
            4, source.split("TelegramNativeAlertDialog.Builder(").size - 1 +
                source.split("createQuestionEditorDialog(").size - 1)
        assertEquals("One native window owner shows all four modal callers",1,source.split(".show()").size - 1)
        assertEquals(4,Regex("""^\s+showOwnedQuestionDialog\(dialog(?:,|\))""",RegexOption.MULTILINE).findAll(source).count())
        assertTrue(source.contains("questionDialog?.dismiss()"))
        assertTrue("Delete Question must remain explicitly destructive red",
            source.contains(".makeRed(DialogInterface.BUTTON_POSITIVE).create()"))
        assertTrue(!source.contains(".showTelegramSourceDialog()"))
        val native = File("src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt").readText()
        assertTrue(native.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(native.contains("R.color.design_destructive_text"))
    }

    @Test fun allThreeOriginalEditAndImportWorkflowsKeepInputsAndAdminMutations() {
        val source = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        val canonical = File("src/main/java/com/eve/app/ui/admin/ManageQuestionsActivity.kt").readText()
            .substringAfter("internal fun createQuestionEditorDialog").substringBefore("internal fun createRenameExamDialog")
        assertTrue("EditExam must use the one canonical active Add presentation", source.contains("createQuestionEditorDialog(this, exam"))
        for (required in listOf("Bulk Question Import Preview", "Import (",
                "importQuestions(result.validQuestions)", "Add New Question", "Edit Question",
                "examRepo.addQuestion(question)", "examRepo.updateQuestion(updatedQ)",
                "examRepo.deleteQuestion(q.id)", "AppUndoBar.show(")) {
            assertTrue("Required original exam flow missing: $required", source.contains(required))
        }
        for (required in listOf("questionText = qText", "optionA = a", "optionD = d")) {
            assertTrue("Canonical form must keep original input mapping: $required",canonical.contains(required))
        }
        assertEquals("All three local modal cancel actions preserved",3,
            source.split(".setNegativeButton(\"Cancel\", null)").size - 1)
        assertTrue("Canonical Add cancel explicitly dismisses only outside pending save",
            canonical.contains(".setNegativeButton(\"Cancel\") { owner, _ -> if (!submitting) owner.dismiss() }"))
    }
}

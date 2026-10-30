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
            4, source.split("TelegramNativeAlertDialog.Builder(").size - 1)
        assertEquals(4, source.split(".show()").size - 1)
        assertTrue("Delete Question must remain explicitly destructive red",
            source.contains(".makeRed(DialogInterface.BUTTON_POSITIVE).show()"))
        assertTrue(!source.contains(".showTelegramSourceDialog()"))
        val native = File("src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt").readText()
        assertTrue(native.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(native.contains("R.color.design_destructive_text"))
    }

    @Test fun allThreeOriginalEditAndImportWorkflowsKeepInputsAndAdminMutations() {
        val source = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        for (required in listOf("Bulk Question Import Preview", "Import (",
                "importQuestions(result.validQuestions)", "Add New Question", "Edit Question",
                "questionText = qText", "optionA = a", "optionD = d",
                "examRepo.addQuestion(newQuestion)", "examRepo.updateQuestion(updatedQ)",
                "examRepo.deleteQuestion(q.id)", "AppUndoBar.show(")) {
            assertTrue("Required original exam flow missing: $required", source.contains(required))
        }
        assertTrue("Custom form cancel buttons preserved",
            source.split(".setNegativeButton(\"Cancel\", null)").size >= 3)
    }
}

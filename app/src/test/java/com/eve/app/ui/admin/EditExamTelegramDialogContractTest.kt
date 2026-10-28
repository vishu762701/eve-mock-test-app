package com.eve.app.ui.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Admin exam editor dialogs share original Telegram chrome without changing mutations. */
class EditExamTelegramDialogContractTest {
    @Test fun onlyThreeAppOwnedExamEditorModalsUseVerifiedSourceDialogAdapter() {
        val source = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        assertTrue(source.contains("import com.eve.app.designsystem.showTelegramSourceDialog"))
        assertEquals("Import preview, Add Question and Edit Question are the only plain modals",
            3, source.split(".showTelegramSourceDialog()").size - 1)
        assertEquals("All original direct show() calls should use the source adapter",
            0, source.split(".show()").size - 1)
        assertTrue("Destructive Delete Question must still use protected destructive style",
            source.contains(".showDestructive()"))
        val extension = File("src/main/java/com/eve/app/designsystem/DesignDialogs.kt").readText()
        assertTrue(extension.contains("fun AlertDialog.Builder.showTelegramSourceDialog(): AlertDialog"))
        assertTrue(extension.contains("TelegramDialogSourceBackground.apply(dialog)"))
    }

    @Test fun allThreeOriginalEditAndImportWorkflowsKeepInputsAndAdminMutations() {
        val source = File("src/main/java/com/eve/app/ui/admin/EditExamActivity.kt").readText()
        for (required in listOf("Bulk Question Import Preview", "Import (" + ',
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
 + "validCount questions)",
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

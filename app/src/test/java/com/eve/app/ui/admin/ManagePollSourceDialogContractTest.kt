package com.eve.app.ui.admin

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManagePollSourceDialogContractTest {
    @Test fun ownerPollDialogUsesOriginalSourceChromeAndSingleWindowGuard() {
        val code = File("src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt").readText()
        val body = code.substringAfter("private fun showCreatePollDialog()")
            .substringBefore("private fun pickCustomDateTime(")
        assertTrue(body.contains("if (isFinishing || isDestroyed || createPollDialog?.isShowing == true) return"))
        assertTrue(body.contains("AlertDialog.Builder(this)"))
        assertTrue(body.contains(".setView(dialogBinding.root)"))
        assertTrue(body.contains(".create()"))
        assertTrue(body.contains("dialog.setOnDismissListener"))
        assertTrue(body.contains("dialog.show()"))
        assertTrue(body.contains("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(body.indexOf("dialog.show()") < body.indexOf("TelegramDialogSourceBackground.apply(dialog)"))
        assertTrue(body.contains("if (createPollDialog === dialog) createPollDialog = null"))
        assertTrue(code.contains("createPollDialog?.dismiss()"))
    }

    @Test fun serverMutationValidationAndSystemDatePickerMustNotChange() {
        val code = File("src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt").readText()
        assertTrue(code.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true)"))
        assertTrue(code.contains("if (question.isBlank())"))
        assertTrue(code.contains("if (options.size < 2)"))
        assertTrue(code.contains("dialogBinding.btnSubmitPoll.isEnabled = false"))
        assertTrue(code.contains("pollRepository.createPoll("))
        for (field in listOf("question = question", "options = options",
                "endsAt = calculatedEndsAt", "active = active", "createdBy = userEmail"))
            assertTrue("Poll payload changed: $field", code.contains(field))
        assertTrue(code.contains("DatePickerDialog(") && code.contains("TimePickerDialog("))
        assertTrue("Preserve admin deletion flow",
            code.contains("deleteFlow.confirmRemoval("))
    }
}

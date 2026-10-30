package com.eve.app.ui.admin

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SystemMonitorTelegramDialogContractTest {
    @Test fun retryAndFailureDialogsUseVerifiedSourceBackdropAndNeverStack() {
        val file = File("src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt").readText()
        assertTrue(file.contains("import com.eve.app.designsystem.TelegramNativeAlertDialog"))
        assertTrue(file.contains("private var retryConfirmationDialog: TelegramNativeAlertDialog? = null"))
        val retry = file.substringAfter("private fun confirmRetry(id: String)")
            .substringBefore("private fun JsonObject.string")
        assertTrue(retry.contains("if (loading || isFinishing || isDestroyed || retryConfirmationDialog?.isShowing == true) return"))
        assertTrue(retry.contains("val dialog = TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(retry.contains(".show()"))
        assertTrue(retry.contains("dialog.setOnDismissListener"))
        assertTrue(retry.contains("retryConfirmationDialog?.dismiss()"))
        assertTrue(retry.contains("if (!isFinishing && !isDestroyed)"))
    }

    @Test fun retryAuthorizationAndServerPayloadRemainUnchanged() {
        val file = File("src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt").readText()
        assertTrue(file.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true)"))
        assertTrue(file.contains("if (loading) return@setPositiveButton"))
        assertTrue(file.contains("checked { ApiClient.api.retryGenerationJob(id) }"))
        assertTrue(file.contains("catch (e: CancellationException) { throw e }"))
        assertTrue(file.contains("finally { loading = false; offset = 0; load() }"))
        assertTrue(file.contains(".setNegativeButton(\"Cancel\", null)"))
        assertTrue(file.contains(".setPositiveButton(\"Retry\")"))
    }
}

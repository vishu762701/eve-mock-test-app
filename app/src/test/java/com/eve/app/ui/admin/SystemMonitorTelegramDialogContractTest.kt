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
        assertTrue(retry.contains("if (!canRetryGeneration || loading || !canPublish() || retryPending"))
        assertTrue(retry.contains("retryConfirmationDialog?.isShowing == true) return"))
        assertTrue(file.contains("!isFinishing && !isDestroyed"))
        assertTrue(retry.contains("val dialog = TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(retry.contains(".show()"))
        assertTrue(retry.contains("dialog.setOnDismissListener"))
        assertTrue(retry.contains("retryConfirmationDialog?.dismiss()"))
        assertTrue(retry.contains("if (epoch == stopEpoch && canPublish())"))
    }

    @Test fun retryAuthorizationAndServerPayloadRemainUnchanged() {
        val file = File("src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt").readText()
        assertTrue(file.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = \"view_analytics_logs\")"))
        assertTrue(file.contains("if (loading || retryPending || !canPublish() || id in confirmedRetries) return@setPositiveButton"))
        val action = file.substringAfter("retryJob = lifecycleScope.launch")
        assertTrue(action.indexOf("requireFreshRetryRight(epoch)") < action.indexOf("ApiClient.api.retryGenerationJob(id)"))
        assertTrue(action.contains("check(confirmedGenerationRetry(response))"))
        assertTrue(file.contains("initiatingEmail, \"manage_generated_tests\""))
        assertTrue(file.contains("catch (e: CancellationException) { throw e }"))
        assertTrue(file.contains("if (confirmed) { offset = 0; load() }"))
        val stop = file.substringAfter("override fun onStop()").substringBefore("override fun onCreate")
        assertTrue(stop.contains("++stopEpoch"))
        assertTrue(stop.contains("++loadGeneration"))
        assertTrue(stop.contains("readJob?.cancel()"))
        assertTrue(stop.contains("retryJob?.cancel()"))
        assertTrue(file.contains(".setNegativeButton(\"Cancel\", null)"))
        assertTrue(file.contains(".setPositiveButton(\"Retry\")"))
    }
}

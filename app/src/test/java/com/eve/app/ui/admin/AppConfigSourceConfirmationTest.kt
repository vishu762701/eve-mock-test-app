package com.eve.app.ui.admin

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-backed privileged confirmation, without modifying the server save gate. */
class AppConfigSourceConfirmationTest {
    private val source = File("src/main/java/com/eve/app/ui/admin/AppConfigActivity.kt").readText()

    @Test fun maintenanceUsesOfficialDialogAndRejectsDuplicatePrivilegedActions() {
        val block = source.substringAfter("private fun validateAndSaveConfig()")
            .substringBefore("private fun saveConfig(config: AppConfig)")
        assertTrue(block.contains("if (maintenanceMode)"))
        assertTrue(block.contains(
            "if (isFinishing || isDestroyed || maintenanceConfirmationDialog?.isShowing == true) return"))
        assertTrue(block.contains(".setTitle(\"Enable Maintenance Mode?\")"))
        assertTrue(block.contains(".setPositiveButton(\"Enable Maintenance\")"))
        assertTrue(block.contains("saveConfig(configToSave)"))
        assertTrue(block.contains(".setNegativeButton(\"Cancel\", null)"))
        assertTrue(block.contains("TelegramNativeAlertDialog.Builder(this)"))
        assertTrue(block.contains(".show()"))
        assertTrue(block.contains("maintenanceConfirmationDialog = dialog"))
        assertTrue(block.contains("if (maintenanceConfirmationDialog === dialog) maintenanceConfirmationDialog = null"))
        assertTrue("Disabling maintenance must still save immediately",
            block.contains("} else {") && block.contains("saveConfig(configToSave)"))
    }

    @Test fun adminAuthorizationAndOwnerWindowCleanupArePreserved() {
        assertTrue(source.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true)"))
        assertTrue(source.contains("adminRepo.updateAppConfig(config)"))
        assertTrue(source.contains("auditLogRepo.recordLog("))
        assertTrue(source.contains("AdminAuditLog.ACTION_MAINTENANCE_TOGGLED"))
        val destroy = source.substringAfter("override fun onDestroy()")
        assertTrue(destroy.contains("maintenanceConfirmationDialog?.dismiss()"))
        assertTrue(destroy.contains("maintenanceConfirmationDialog = null"))
        val host = File("src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt").readText()
        assertTrue(host.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(host.contains("fun setOnDismissListener(listener: DialogInterface.OnDismissListener?)"))
        assertTrue(source.contains("private var maintenanceConfirmationDialog: TelegramNativeAlertDialog? = null"))
    }
}

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
        assertTrue("Only a deliberate off-to-on save needs enable confirmation",
            block.contains("if (maintenanceMode && confirmedConfig?.maintenance_mode != true)"))
        assertTrue("Save must reject stale account and overlapping writes",
            block.contains("if (!canPublish() || configBusy || healthBusy) return"))
        assertTrue(block.contains("if (confirmedConfig == null) { loadConfig(); return }"))
        assertTrue("Every dismissal restores the confirmed server switch state",
            block.contains("binding.switchMaintenanceMode.setCheckedSilently(confirmedConfig?.maintenance_mode ?: false)"))
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

    @Test fun accountAndRightBoundariesProtectEverySuspendedConfigAction() {
        assertTrue(source.contains("AdminIdentityGuard.allowed(true, configOwnerUid, configOwnerEmail"))
        assertTrue(source.contains("requireFreshRight(\"manage_config_maintenance\")"))
        assertTrue(source.contains("requireFreshRight(\"manage_questions\")"))
        assertTrue("AI endpoint requires a distinct role; never use config role for AI health",
            source.contains("AdminPermissionSnapshot.allows(permission, configOwnerEmail, \"manage_questions\")"))
        assertTrue(source.contains("val request = ++configReadGeneration"))
        assertTrue(source.contains("request != configReadGeneration || !canPublish()"))
        assertTrue(source.contains("override fun onStop()"))
        assertTrue(source.contains("reloadAfterStop = true"))
        assertTrue(source.contains("if (!canPublish()) return@launch"))
        assertTrue(source.contains("if (!res.success || res.data == null)"))
        assertTrue(source.contains("if (!canPublish() || healthBusy || configBusy) return"))
    }

    @Test fun adminAuthorizationAndOwnerWindowCleanupArePreserved() {
        assertTrue(source.contains("AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = \"manage_config_maintenance\")"))
        assertTrue(source.contains("adminRepo.updateAppConfig(config, confirmedConfig ?: throw IllegalStateException(\"Reload configuration\"))"))
        val repository = File("src/main/java/com/eve/app/data/repository/AdminRepository.kt").readText()
        assertTrue("Editor sends only deliberately changed fields", repository.contains("val changes = body.filter { (key, value) -> old[key] != value }"))
        assertTrue("Success requires server readback", repository.contains("if (confirmed != config) throw Exception"))
        val destroy = source.substringAfter("override fun onDestroy()")
        assertTrue(destroy.contains("maintenanceConfirmationDialog?.dismiss()"))
        assertTrue(destroy.contains("maintenanceConfirmationDialog = null"))
        val host = File("src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt").readText()
        assertTrue(host.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(host.contains("fun setOnDismissListener(listener: DialogInterface.OnDismissListener?)"))
        assertTrue(source.contains("private var maintenanceConfirmationDialog: TelegramNativeAlertDialog? = null"))
    }
}

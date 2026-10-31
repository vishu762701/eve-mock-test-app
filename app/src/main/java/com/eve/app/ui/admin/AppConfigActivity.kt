package com.eve.app.ui.admin

import com.eve.app.ui.common.EveBaseActivity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eve.app.BuildConfig
import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.model.AppConfig
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.AuditLogRepository
import com.eve.app.databinding.ActivityAppConfigBinding
import com.eve.app.util.AppBulletin
import com.eve.app.designsystem.TelegramNativeAlertDialog
import kotlinx.coroutines.launch

class AppConfigActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAppConfigBinding
    private val adminRepo = AdminRepository()
    private val auditLogRepo = AuditLogRepository()
    private var confirmedConfig: AppConfig? = null
    private var configBusy = false
    private var maintenanceConfirmationDialog: TelegramNativeAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {

        binding.btnBack.setOnClickListener { finish() }

        binding.tvCurrentVersionNotice.text =
            "Current installed app version code: ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})"

        binding.btnSaveConfig.setOnClickListener {
            validateAndSaveConfig()
        }

        binding.btnTestAiHealth.setOnClickListener {
            testAiHealth()
        }

        loadConfig()
        }
    }

    private fun loadConfig() {
        if (configBusy) return
        configBusy = true
        binding.btnSaveConfig.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val config = adminRepo.getAppConfig()
                confirmedConfig = config
                binding.progressBar.visibility = View.GONE
                binding.switchMaintenanceMode.isChecked = config.maintenance_mode
                binding.etMaintenanceMessage.setText(config.maintenance_message)
                binding.etMinVersionCode.setText(config.minimum_supported_version_code.toString())
                if (config.default_publish_mode.equals("live", ignoreCase = true)) {
                    binding.rbPublishLive.isChecked = true
                } else {
                    binding.rbPublishPaused.isChecked = true
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@AppConfigActivity, "Failed to load config: ${e.toUserFriendlyMessage()}. Tap Save to retry loading.")
            } finally {
                configBusy = false
                binding.btnSaveConfig.isEnabled = true
            }
        }
    }

    private fun validateAndSaveConfig() {
        if (configBusy) return
        if (confirmedConfig == null) { loadConfig(); return }
        val maintenanceMode = binding.switchMaintenanceMode.isChecked
        val message = binding.etMaintenanceMessage.text?.toString()?.trim().orEmpty()
        val minVersionStr = binding.etMinVersionCode.text?.toString()?.trim().orEmpty()
        val minVersion = minVersionStr.toIntOrNull()
        if (minVersion == null || minVersion < 0 || message.length > 1000) {
            AppBulletin.showError(this, "Enter a non-negative version and a message up to 1000 characters")
            return
        }
        val defaultPublishMode = if (binding.rbPublishLive.isChecked) "live" else "paused"

        val configToSave = AppConfig(
            minimum_supported_version_code = minVersion,
            maintenance_mode = maintenanceMode,
            maintenance_message = message,
            default_publish_mode = defaultPublishMode
        )

        if (maintenanceMode && confirmedConfig?.maintenance_mode != true) {
            // Original AndroidUtilities.isSafeToShow() and one active
            // privileged window: don't stack server-impacting confirmations.
            if (isFinishing || isDestroyed || maintenanceConfirmationDialog?.isShowing == true) return
            val dialog = TelegramNativeAlertDialog.Builder(this)
                .setTitle("Enable Maintenance Mode?")
                .setMessage("Are you sure? When maintenance mode is active, students opening the app will be blocked by a full-screen maintenance notice.")
                .setPositiveButton("Enable Maintenance") { _, _ ->
                    saveConfig(configToSave)
                }
                .setNegativeButton("Cancel", null)
                .show()
            maintenanceConfirmationDialog = dialog
            dialog.setOnDismissListener {
                if (maintenanceConfirmationDialog === dialog) maintenanceConfirmationDialog = null
                binding.switchMaintenanceMode.setCheckedSilently(confirmedConfig?.maintenance_mode ?: false)
            }
        } else {
            saveConfig(configToSave)
        }
    }

    private fun saveConfig(config: AppConfig) {
        if (configBusy) return
        configBusy = true
        binding.btnSaveConfig.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val success = adminRepo.updateAppConfig(config, confirmedConfig ?: throw IllegalStateException("Reload configuration"))
                binding.progressBar.visibility = View.GONE
                if (success) {
                    confirmedConfig = config
                    binding.switchMaintenanceMode.setCheckedSilently(config.maintenance_mode)
                    AppBulletin.showSuccess(this@AppConfigActivity, "Configuration saved successfully!")
                } else {
                    AppBulletin.showError(this@AppConfigActivity, "Failed to save configuration")
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@AppConfigActivity, "Save unconfirmed: ${e.toUserFriendlyMessage()}. Reload before retry.")
                confirmedConfig = null
            } finally {
                configBusy = false
                binding.btnSaveConfig.isEnabled = true
            }
        }
    }

    private fun testAiHealth() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnTestAiHealth.isEnabled = false
        lifecycleScope.launch {
            try {
                val res = ApiClient.apiService.checkAiHealth()
                binding.progressBar.visibility = View.GONE
                if (!res.success) {
                    val errMsg = res.error ?: "Failed to perform AI health check"
                    TelegramNativeAlertDialog.Builder(this@AppConfigActivity)
                        .setTitle("AI Health Check Failed")
                        .setMessage(errMsg)
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val data = res.data
                    val workingModel = data?.workingModel ?: "None"
                    val primaryModel = data?.primaryModel ?: "gemini-3.5-flash-lite"
                    val slots = data?.slots.orEmpty()

                    val message = buildString {
                        appendLine("Primary Model: $primaryModel")
                        appendLine("Working Model: $workingModel")
                        appendLine()
                        if (slots.isEmpty()) {
                            appendLine("No Gemini API keys configured on backend.")
                        } else {
                            slots.forEach { slot ->
                                val statusIcon = if (slot.ok) "✔" else "✖"
                                appendLine("$statusIcon ${slot.slot}: ${if (slot.ok) "OK" else "FAILED"} (${slot.latencyMs}ms)")
                                appendLine("   Status: ${slot.providerStatus.ifBlank { "UNKNOWN" }}")
                                if (slot.message.isNotBlank()) {
                                    appendLine("   Message: ${slot.message}")
                                }
                                appendLine()
                            }
                        }
                    }.trim()

                    TelegramNativeAlertDialog.Builder(this@AppConfigActivity)
                        .setTitle("AI Connection Health")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                val errMsg = e.toUserFriendlyMessage()
                TelegramNativeAlertDialog.Builder(this@AppConfigActivity)
                    .setTitle("AI Health Check Failed")
                    .setMessage(errMsg)
                    .setPositiveButton("OK", null)
                    .show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnTestAiHealth.isEnabled = true
            }
        }
    }
    override fun onDestroy() {
        maintenanceConfirmationDialog?.dismiss()
        maintenanceConfirmationDialog = null
        super.onDestroy()
    }

}

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AppConfigActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAppConfigBinding
    private val adminRepo = AdminRepository()
    private val auditLogRepo = AuditLogRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    private fun loadConfig() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val config = adminRepo.getAppConfig()
                binding.progressBar.visibility = View.GONE
                binding.switchMaintenanceMode.isChecked = config.maintenance_mode
                binding.etMaintenanceMessage.setText(config.maintenance_message)
                binding.etMinVersionCode.setText(config.minimum_supported_version_code.toString())
                if (config.default_publish_mode.equals("live", ignoreCase = true)) {
                    binding.rbPublishLive.isChecked = true
                } else {
                    binding.rbPublishPaused.isChecked = true
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@AppConfigActivity, "Failed to load config: ${e.toUserFriendlyMessage()}")
            }
        }
    }

    private fun validateAndSaveConfig() {
        val maintenanceMode = binding.switchMaintenanceMode.isChecked
        val message = binding.etMaintenanceMessage.text?.toString()?.trim().orEmpty()
        val minVersionStr = binding.etMinVersionCode.text?.toString()?.trim().orEmpty()
        val minVersion = minVersionStr.toIntOrNull() ?: 1
        val defaultPublishMode = if (binding.rbPublishLive.isChecked) "live" else "paused"

        val configToSave = AppConfig(
            minimum_supported_version_code = minVersion,
            maintenance_mode = maintenanceMode,
            maintenance_message = message,
            default_publish_mode = defaultPublishMode
        )

        if (maintenanceMode) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Enable Maintenance Mode?")
                .setMessage("Are you sure? When maintenance mode is active, students opening the app will be blocked by a full-screen maintenance notice.")
                .setPositiveButton("Enable Maintenance") { _, _ ->
                    saveConfig(configToSave)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            saveConfig(configToSave)
        }
    }

    private fun saveConfig(config: AppConfig) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val success = adminRepo.updateAppConfig(config)
                binding.progressBar.visibility = View.GONE
                if (success) {
                    auditLogRepo.recordLog(
                        AdminAuditLog.ACTION_MAINTENANCE_TOGGLED,
                        if (config.maintenance_mode) "Enabled maintenance mode: ${config.maintenance_message}" else "Disabled maintenance mode"
                    )
                    AppBulletin.showSuccess(this@AppConfigActivity, "Configuration saved successfully!")
                } else {
                    AppBulletin.showError(this@AppConfigActivity, "Failed to save configuration")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                AppBulletin.showError(this@AppConfigActivity, "Error saving: ${e.toUserFriendlyMessage()}")
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
                    MaterialAlertDialogBuilder(this@AppConfigActivity)
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

                    MaterialAlertDialogBuilder(this@AppConfigActivity)
                        .setTitle("AI Connection Health")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                val errMsg = e.toUserFriendlyMessage()
                MaterialAlertDialogBuilder(this@AppConfigActivity)
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
}

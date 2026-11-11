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
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.util.AdminIdentityGuard
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import com.eve.app.designsystem.TelegramNativeAlertDialog
import kotlinx.coroutines.launch

class AppConfigActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAppConfigBinding
    private val adminRepo = AdminRepository()
    private val auditLogRepo = AuditLogRepository()
    private var confirmedConfig: AppConfig? = null
    private var configBusy = false
    private var maintenanceConfirmationDialog: TelegramNativeAlertDialog? = null
    private var healthDialog: TelegramNativeAlertDialog? = null
    private var configOwnerUid = ""
    private var configOwnerEmail = ""
    private var adminAuthorized = false
    private var configReadGeneration = 0L
    private var reloadAfterStop = false
    private var reauthorizeOnResume = false
    private var healthBusy = false

    private fun ownerCurrent(): Boolean {
        val active = FirebaseAuth.getInstance().currentUser
        return adminAuthorized && !isFinishing && !isDestroyed &&
            AdminIdentityGuard.allowed(true, configOwnerUid, configOwnerEmail, active?.uid, active?.email)
    }

    private fun canPublish(): Boolean =
        ownerCurrent() && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private suspend fun requireFreshRight(right: String) {
        if (!canPublish()) throw CancellationException("Admin account changed")
        val granted = AdminPermissionSnapshot.load(
            configOwnerEmail, right,
            membership = { adminRepo.isAdminFresh(configOwnerUid, configOwnerEmail) },
            permissions = { adminRepo.getAdminMe() }
        )
        if (!canPublish()) throw CancellationException("Admin account changed")
        if (granted == null) throw SecurityException("Admin permission is no longer available")
    }

    override fun onStop() {
        ++configReadGeneration
        reloadAfterStop = true
        reauthorizeOnResume = true
        maintenanceConfirmationDialog?.dismiss()
        maintenanceConfirmationDialog = null
        healthDialog?.dismiss()
        healthDialog = null
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (adminAuthorized && !ownerCurrent()) {
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        if (reauthorizeOnResume && ::binding.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != configOwnerUid || live.email != configOwnerEmail) {
                finish()
                return
            }
            val hasData = confirmedConfig != null
            if (!hasData) {
                binding.progressBar.visibility = View.VISIBLE
                binding.scrollConfigContent.visibility = View.GONE
            }
            com.eve.app.util.AdminAccessGate.requireAdmin(
                this,
                binding.root,
                requireFreshServer = true,
                requiredRight = "manage_config_maintenance"
            ) { permission ->
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                configOwnerUid = live.uid
                configOwnerEmail = live.email.orEmpty()
                binding.progressBar.visibility = View.GONE
                binding.scrollConfigContent.visibility = View.VISIBLE
                binding.btnTestAiHealth.isEnabled =
                    AdminPermissionSnapshot.allows(permission, configOwnerEmail, "manage_questions")
                if (reloadAfterStop && !configBusy) {
                    reloadAfterStop = false
                    loadConfig()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.progressBar.visibility = View.VISIBLE
        binding.scrollConfigContent.visibility = View.GONE

        com.eve.app.util.AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true, requiredRight = "manage_config_maintenance") { permission ->
        val principal = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
        configOwnerUid = principal.uid
        configOwnerEmail = principal.email.orEmpty()
        adminAuthorized = true
        if (!ownerCurrent()) return@requireAdmin
        binding.progressBar.visibility = View.GONE
        binding.scrollConfigContent.visibility = View.VISIBLE
        binding.btnTestAiHealth.isEnabled =
            AdminPermissionSnapshot.allows(permission, configOwnerEmail, "manage_questions")

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
        if (configBusy || !ownerCurrent()) return
        val request = ++configReadGeneration
        configBusy = true
        binding.btnSaveConfig.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val config = adminRepo.getAppConfig()
                if (request != configReadGeneration || !canPublish()) return@launch
                confirmedConfig = config
                binding.switchMaintenanceMode.isChecked = config.maintenance_mode
                binding.etMaintenanceMessage.setText(config.maintenance_message)
                binding.etMinVersionCode.setText(config.minimum_supported_version_code.toString())
                if (config.default_publish_mode.equals("live", ignoreCase = true)) {
                    binding.rbPublishLive.isChecked = true
                } else {
                    binding.rbPublishPaused.isChecked = true
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (request == configReadGeneration && canPublish()) {
                    AppBulletin.showError(this@AppConfigActivity,
                        "Failed to load config: ${e.toUserFriendlyMessage()}. Tap Save to retry loading.")
                }
            } finally {
                configBusy = false
                if (canPublish()) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveConfig.isEnabled = true
                    if (reloadAfterStop) {
                        reloadAfterStop = false
                        loadConfig()
                    }
                }
            }
        }
    }

    private fun validateAndSaveConfig() {
        if (!canPublish() || configBusy || healthBusy) return
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
        if (configBusy || healthBusy || !canPublish()) return
        configBusy = true
        binding.btnSaveConfig.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                // The editor authorization must still be fresh at mutation time.
                // Existing Worker authorization and server readback remain authoritative.
                requireFreshRight("manage_config_maintenance")
                if (!canPublish()) return@launch
                val success = adminRepo.updateAppConfig(config, confirmedConfig ?: throw IllegalStateException("Reload configuration"))
                if (!canPublish()) return@launch
                if (success) {
                    confirmedConfig = config
                    binding.switchMaintenanceMode.setCheckedSilently(config.maintenance_mode)
                    AppBulletin.showSuccess(this@AppConfigActivity, "Configuration saved successfully!")
                } else {
                    AppBulletin.showError(this@AppConfigActivity, "Failed to save configuration")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (canPublish()) {
                    AppBulletin.showError(this@AppConfigActivity,
                        "Save unconfirmed: ${e.toUserFriendlyMessage()}. Reload before retry.")
                    confirmedConfig = null
                }
            } finally {
                configBusy = false
                if (canPublish()) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveConfig.isEnabled = true
                    if (reloadAfterStop) {
                        reloadAfterStop = false
                        loadConfig()
                    }
                }
            }
        }
    }

    private fun testAiHealth() {
        if (!canPublish() || healthBusy || configBusy) return
        healthBusy = true
        binding.btnSaveConfig.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.btnTestAiHealth.isEnabled = false
        lifecycleScope.launch {
            try {
                // The Worker AI health endpoint requires manage_questions, not the
                // manage_config_maintenance right used to enter this screen.
                requireFreshRight("manage_questions")
                if (!canPublish()) return@launch
                val res = ApiClient.apiService.checkAiHealth()
                if (!canPublish()) return@launch
                binding.progressBar.visibility = View.GONE
                if (!res.success || res.data == null) {
                    val errMsg = res.error ?: "Failed to perform AI health check"
                    healthDialog?.dismiss()
                    healthDialog = TelegramNativeAlertDialog.Builder(this@AppConfigActivity)
                        .setTitle("AI Health Check Failed")
                        .setMessage(errMsg)
                        .setPositiveButton("OK", null)
                        .show().also { d ->
                            d.setOnDismissListener { if (healthDialog === d) healthDialog = null }
                        }
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

                    healthDialog?.dismiss()
                    healthDialog = TelegramNativeAlertDialog.Builder(this@AppConfigActivity)
                        .setTitle("AI Connection Health")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show().also { d ->
                            d.setOnDismissListener { if (healthDialog === d) healthDialog = null }
                        }
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (!canPublish()) return@launch
                binding.progressBar.visibility = View.GONE
                val errMsg = e.toUserFriendlyMessage()
                healthDialog?.dismiss()
                healthDialog = TelegramNativeAlertDialog.Builder(this@AppConfigActivity)
                    .setTitle("AI Health Check Failed")
                    .setMessage(errMsg)
                    .setPositiveButton("OK", null)
                    .show().also { d ->
                        d.setOnDismissListener { if (healthDialog === d) healthDialog = null }
                    }
            } finally {
                healthBusy = false
                if (canPublish()) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSaveConfig.isEnabled = true
                    binding.btnTestAiHealth.isEnabled = true
                }
            }
        }
    }
    override fun onDestroy() {
        maintenanceConfirmationDialog?.dismiss()
        maintenanceConfirmationDialog = null
        healthDialog?.dismiss()
        healthDialog = null
        super.onDestroy()
    }

}

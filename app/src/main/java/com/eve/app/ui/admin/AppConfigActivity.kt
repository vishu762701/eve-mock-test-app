package com.eve.app.ui.admin

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.eve.app.BuildConfig
import com.eve.app.R
import com.eve.app.data.model.AppConfig
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.toUserFriendlyMessage
import com.eve.app.data.repository.AdminRepository
import org.telegram.ui.ActionBar.AlertDialog
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AdminIdentityGuard
import com.eve.app.util.AdminPermissionSnapshot
import com.eve.app.util.AppBulletin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Cells.TextRadioCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Admin App Config & Maintenance screen powered by real ported Telegram UI components:
 * ActionBar, HeaderCell, TextCheckCell, TextRadioCell, EditTextBoldCursor,
 * ButtonWithCounterView, ShadowSectionCell, TextInfoPrivacyCell, and EmptyTextProgressView.
 */
class AppConfigActivity : EveBaseActivity() {

    private val adminRepo = AdminRepository()
    private var confirmedConfig: AppConfig? = null
    private var configBusy = false
    private var maintenanceConfirmationDialog: AlertDialog? = null
    private var healthDialog: AlertDialog? = null
    private var configOwnerUid = ""
    private var configOwnerEmail = ""
    private var adminAuthorized = false
    private var configReadGeneration = 0L
    private var reloadAfterStop = false
    private var reauthorizeOnResume = false
    private var healthBusy = false

    private lateinit var rootView: LinearLayout
    private lateinit var scrollConfigContent: ScrollView
    private lateinit var loadingView: EmptyTextProgressView
    private lateinit var rowMaintenanceMode: TextCheckCell
    private lateinit var etMaintenanceMessage: EditTextBoldCursor
    private lateinit var etMinVersionCode: EditTextBoldCursor
    private lateinit var rowPublishLive: TextRadioCell
    private lateinit var rowPublishPaused: TextRadioCell
    private lateinit var btnSaveConfig: ButtonWithCounterView
    private lateinit var btnTestAiHealth: ButtonWithCounterView

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
        if (reauthorizeOnResume && ::rootView.isInitialized) {
            reauthorizeOnResume = false
            val live = FirebaseAuth.getInstance().currentUser
            if (live == null || live.uid != configOwnerUid || live.email != configOwnerEmail) {
                finish()
                return
            }
            val hasData = confirmedConfig != null
            if (!hasData) {
                loadingView.visibility = View.VISIBLE
                scrollConfigContent.visibility = View.GONE
            }
            AdminAccessGate.requireAdmin(
                this,
                rootView,
                requireFreshServer = true,
                requiredRight = "manage_config_maintenance"
            ) { permission ->
                if (!ownerCurrent()) {
                    finish()
                    return@requireAdmin
                }
                configOwnerUid = live.uid
                configOwnerEmail = live.email.orEmpty()
                loadingView.visibility = View.GONE
                scrollConfigContent.visibility = View.VISIBLE
                btnTestAiHealth.isEnabled =
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

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(rootView)

        val actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("App Config & Maintenance")
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    if (id == -1) finish()
                }
            })
        }
        rootView.addView(actionBar, matchWrap())

        val contentFrame = FrameLayout(this)
        rootView.addView(
            contentFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        scrollConfigContent = ScrollView(this).apply {
            isFillViewport = true
            visibility = View.GONE
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }

        // 1. Maintenance Mode
        list.addView(HeaderCell(this).apply {
            setText("Maintenance Mode")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        rowMaintenanceMode = TextCheckCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndCheck("Enable Maintenance Mode", false, true)
            setOnClickListener { setChecked(!isChecked) }
        }
        list.addView(rowMaintenanceMode, matchWrap())

        val msgWrap = inputWrap()
        etMaintenanceMessage = styledEditText("Maintenance notice message", singleLine = false)
        msgWrap.addView(etMaintenanceMessage, matchWrap())
        list.addView(msgWrap, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // 2. Version & Publish Defaults
        list.addView(HeaderCell(this).apply {
            setText("Minimum Supported Version")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        val verWrap = inputWrap()
        etMinVersionCode = styledEditText("Minimum version code (e.g. 1)", singleLine = true).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        verWrap.addView(etMinVersionCode, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f)))
        list.addView(verWrap, matchWrap())
        list.addView(TextInfoPrivacyCell(this).apply {
            setText("Current installed app version code: ${BuildConfig.VERSION_CODE} (${BuildConfig.VERSION_NAME})")
        }, matchWrap())

        list.addView(HeaderCell(this).apply {
            setText("Default Generated Test Publish Mode")
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        }, matchWrap())

        rowPublishLive = TextRadioCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndCheck("Live immediately", true, true)
            setOnClickListener {
                setChecked(true)
                rowPublishPaused.setChecked(false)
            }
        }
        list.addView(rowPublishLive, matchWrap())

        rowPublishPaused = TextRadioCell(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            foreground = Theme.getSelectorDrawable(false)
            setTextAndCheck("Paused (review before publishing)", false, false)
            setOnClickListener {
                setChecked(true)
                rowPublishLive.setChecked(false)
            }
        }
        list.addView(rowPublishPaused, matchWrap())
        list.addView(ShadowSectionCell(this), matchWrap())

        // 3. Actions
        val btnWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
            val padH = AndroidUtilities.dp(16f)
            val padV = AndroidUtilities.dp(12f)
            setPadding(padH, padV, padH, padV)
        }
        btnSaveConfig = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Save Configuration", false)
            setOnClickListener { validateAndSaveConfig() }
        }
        btnWrap.addView(btnSaveConfig, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48f)))

        btnTestAiHealth = ButtonWithCounterView(this, false, null).apply {
            setRound()
            setNeutral()
            setText("Test AI Connection Health", false)
            setOnClickListener { testAiHealth() }
        }
        btnWrap.addView(
            btnTestAiHealth,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(44f)).apply {
                topMargin = AndroidUtilities.dp(10f)
            }
        )
        list.addView(btnWrap, matchWrap())

        scrollConfigContent.addView(list, matchWrap())
        contentFrame.addView(
            scrollConfigContent,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        loadingView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            showProgress()
        }
        contentFrame.addView(
            loadingView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        AdminAccessGate.requireAdmin(this, rootView, requireFreshServer = true, requiredRight = "manage_config_maintenance") { permission ->
            val principal = FirebaseAuth.getInstance().currentUser ?: return@requireAdmin
            configOwnerUid = principal.uid
            configOwnerEmail = principal.email.orEmpty()
            adminAuthorized = true
            if (!ownerCurrent()) return@requireAdmin
            loadingView.visibility = View.GONE
            scrollConfigContent.visibility = View.VISIBLE
            btnTestAiHealth.isEnabled =
                AdminPermissionSnapshot.allows(permission, configOwnerEmail, "manage_questions")
            loadConfig()
        }
    }

    private fun loadConfig() {
        if (configBusy || !ownerCurrent()) return
        val request = ++configReadGeneration
        configBusy = true
        btnSaveConfig.isEnabled = false
        btnSaveConfig.setLoading(true)
        lifecycleScope.launch {
            try {
                val config = adminRepo.getAppConfig()
                if (request != configReadGeneration || !canPublish()) return@launch
                confirmedConfig = config
                rowMaintenanceMode.setChecked(config.maintenance_mode)
                etMaintenanceMessage.setText(config.maintenance_message)
                etMinVersionCode.setText(config.minimum_supported_version_code.toString())
                val isLive = config.default_publish_mode.equals("live", ignoreCase = true)
                rowPublishLive.setChecked(isLive)
                rowPublishPaused.setChecked(!isLive)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (request == configReadGeneration && canPublish()) {
                    AppBulletin.showError(
                        this@AppConfigActivity,
                        "Failed to load config: ${e.toUserFriendlyMessage()}. Tap Save to retry loading."
                    )
                }
            } finally {
                configBusy = false
                if (canPublish()) {
                    btnSaveConfig.setLoading(false)
                    btnSaveConfig.isEnabled = true
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
        val maintenanceMode = rowMaintenanceMode.isChecked
        val message = etMaintenanceMessage.text?.toString()?.trim().orEmpty()
        val minVersion = etMinVersionCode.text?.toString()?.trim().orEmpty().toIntOrNull()
        if (minVersion == null || minVersion < 0 || message.length > 1000) {
            AppBulletin.showError(this, "Enter a non-negative version and a message up to 1000 characters")
            return
        }
        val defaultPublishMode = if (rowPublishLive.isChecked) "live" else "paused"

        val configToSave = AppConfig(
            minimum_supported_version_code = minVersion,
            maintenance_mode = maintenanceMode,
            maintenance_message = message,
            default_publish_mode = defaultPublishMode
        )

        if (maintenanceMode && confirmedConfig?.maintenance_mode != true) {
            if (isFinishing || isDestroyed || maintenanceConfirmationDialog?.isShowing == true) return
            val dialog = AlertDialog.Builder(this)
                .setTitle("Enable Maintenance Mode?")
                .setMessage("Are you sure? When maintenance mode is active, students opening the app will be blocked by a full-screen maintenance notice.")
                .setPositiveButton("Enable Maintenance") { _, _ -> saveConfig(configToSave) }
                .setNegativeButton("Cancel", null)
                .show()
            maintenanceConfirmationDialog = dialog
            dialog.setOnDismissListener {
                if (maintenanceConfirmationDialog === dialog) maintenanceConfirmationDialog = null
                rowMaintenanceMode.setChecked(confirmedConfig?.maintenance_mode ?: false)
            }
        } else {
            saveConfig(configToSave)
        }
    }

    private fun saveConfig(config: AppConfig) {
        if (configBusy || healthBusy || !canPublish()) return
        configBusy = true
        btnSaveConfig.isEnabled = false
        btnSaveConfig.setLoading(true)
        lifecycleScope.launch {
            try {
                requireFreshRight("manage_config_maintenance")
                if (!canPublish()) return@launch
                val success = adminRepo.updateAppConfig(
                    config,
                    confirmedConfig ?: throw IllegalStateException("Reload configuration")
                )
                if (!canPublish()) return@launch
                if (success) {
                    confirmedConfig = config
                    rowMaintenanceMode.setChecked(config.maintenance_mode)
                    AppBulletin.showSuccess(this@AppConfigActivity, "Configuration saved successfully!")
                } else {
                    AppBulletin.showError(this@AppConfigActivity, "Failed to save configuration")
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (canPublish()) {
                    AppBulletin.showError(
                        this@AppConfigActivity,
                        "Save unconfirmed: ${e.toUserFriendlyMessage()}. Reload before retry."
                    )
                    confirmedConfig = null
                }
            } finally {
                configBusy = false
                if (canPublish()) {
                    btnSaveConfig.setLoading(false)
                    btnSaveConfig.isEnabled = true
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
        btnSaveConfig.isEnabled = false
        btnTestAiHealth.isEnabled = false
        btnTestAiHealth.setLoading(true)
        lifecycleScope.launch {
            try {
                requireFreshRight("manage_questions")
                if (!canPublish()) return@launch
                val res = ApiClient.apiService.checkAiHealth()
                if (!canPublish()) return@launch
                if (!res.success || res.data == null) {
                    val errMsg = res.error ?: "Failed to perform AI health check"
                    showHealthDialog("AI Health Check Failed", errMsg)
                } else {
                    val data = res.data
                    val workingModel = data.workingModel ?: "None"
                    val primaryModel = data.primaryModel ?: "gemini-3.5-flash-lite"
                    val slots = data.slots.orEmpty()
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
                                if (slot.message.isNotBlank()) appendLine("   Message: ${slot.message}")
                                appendLine()
                            }
                        }
                    }.trim()
                    showHealthDialog("AI Connection Health", message)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (e: Exception) {
                if (!canPublish()) return@launch
                showHealthDialog("AI Health Check Failed", e.toUserFriendlyMessage())
            } finally {
                healthBusy = false
                if (canPublish()) {
                    btnTestAiHealth.setLoading(false)
                    btnSaveConfig.isEnabled = true
                    btnTestAiHealth.isEnabled = true
                }
            }
        }
    }

    private fun showHealthDialog(title: String, message: String) {
        healthDialog?.dismiss()
        healthDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show().also { d ->
                d.setOnDismissListener { if (healthDialog === d) healthDialog = null }
            }
    }

    override fun onDestroy() {
        maintenanceConfirmationDialog?.dismiss()
        maintenanceConfirmationDialog = null
        healthDialog?.dismiss()
        healthDialog = null
        super.onDestroy()
    }

    private fun inputWrap() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
        val padH = AndroidUtilities.dp(21f)
        val padV = AndroidUtilities.dp(8f)
        setPadding(padH, padV, padH, padV)
    }

    private fun styledEditText(hintText: String, singleLine: Boolean) = EditTextBoldCursor(this).apply {
        hint = hintText
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText))
        setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
        setCursorSize(AndroidUtilities.dp(20f))
        setCursorWidth(1.5f)
        isSingleLine = singleLine
        if (!singleLine) minLines = 3
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
}

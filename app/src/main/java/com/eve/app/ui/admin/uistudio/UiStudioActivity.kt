package com.eve.app.ui.admin.uistudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.eve.app.R
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.repository.UiStudioRepository
import com.eve.app.databinding.ActivityUiStudioBinding
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import com.eve.app.util.UiStudioEngine
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class UiStudioActivity : EveBaseActivity() {

    private lateinit var binding: ActivityUiStudioBinding
    private val repo = UiStudioRepository.getInstance()

    private var activeConfig: UiStudioConfig = repo.getDefaultTemplate()
    private var isUpdatingFields = false

    private val supportedScreens = listOf(
        "home" to "Home Screen",
        "test" to "Test Screen",
        "result" to "Result Screen"
    )

    private val screenComponentsMap = mapOf(
        "home" to listOf(
            "hero_banner" to "Hero Card / Daily Challenge",
            "streak_pill" to "Streak Pill",
            "find_test_panel" to "Search & Filter Card",
            "active_exams_header" to "Exams Section Header"
        ),
        "test" to listOf(
            "timer_pill" to "Timer Pill",
            "question_card" to "Question Card",
            "question_text" to "Question Text",
            "option_item" to "Option Card",
            "action_buttons" to "Action Navigation Buttons"
        ),
        "result" to listOf(
            "score_card" to "Score Summary Card",
            "analytics_summary" to "Analytics Stat Pills"
        )
    )

    private var currentScreenKey = "home"
    private var currentCompKey = "hero_banner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUiStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSpinners()
        setupFieldListeners()
        setupBottomActions()

        loadDraftFromBackend()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnVersionHistory.setOnClickListener { showVersionHistoryDialog() }
        binding.btnExportImport.setOnClickListener { showExportImportDialog() }
        binding.btnResetDefaults.setOnClickListener { confirmResetToDefaults() }
    }

    private fun setupSpinners() {
        // 1. Screen Spinner
        val screenNames = supportedScreens.map { it.second }
        val screenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, screenNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spScreenSelector.adapter = screenAdapter

        binding.spScreenSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentScreenKey = supportedScreens[position].first
                updateComponentSpinner()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Component Spinner
        binding.spComponentSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val components = screenComponentsMap[currentScreenKey] ?: emptyList()
                if (position in components.indices) {
                    currentCompKey = components[position].first
                    populateFieldsForCurrentComponent()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateComponentSpinner() {
        val components = screenComponentsMap[currentScreenKey] ?: emptyList()
        val compNames = components.map { it.second }
        val compAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, compNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spComponentSelector.adapter = compAdapter
        if (components.isNotEmpty()) {
            currentCompKey = components[0].first
            populateFieldsForCurrentComponent()
        }
    }

    private fun loadDraftFromBackend() {
        lifecycleScope.launch {
            binding.tvStudioStatus.text = "Loading draft..."
            val result = repo.getDraft()
            result.onSuccess { draft ->
                activeConfig = if (draft.screens.isNotEmpty()) draft else repo.getDefaultTemplate()
                binding.tvStudioStatus.text = if (activeConfig.version > 0) "Draft v${activeConfig.version}" else "Unpublished Draft"
                populateFieldsForCurrentComponent()
            }.onFailure {
                activeConfig = repo.loadCachedConfig() ?: repo.getDefaultTemplate()
                binding.tvStudioStatus.text = "Working Offline Draft"
                populateFieldsForCurrentComponent()
            }
        }
    }

    private fun getCurrentComponentConfig(): ComponentConfig {
        val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
        return screen.components[currentCompKey] ?: ComponentConfig(id = currentCompKey)
    }

    private fun updateCurrentComponentConfig(mutator: (ComponentConfig) -> ComponentConfig) {
        val currentComp = getCurrentComponentConfig()
        val updatedComp = mutator(currentComp)

        val screen = activeConfig.screens[currentScreenKey] ?: ScreenConfig(id = currentScreenKey)
        val updatedComponents = screen.components.toMutableMap().apply {
            put(currentCompKey, updatedComp)
        }
        val updatedScreen = screen.copy(components = updatedComponents)
        val updatedScreens = activeConfig.screens.toMutableMap().apply {
            put(currentScreenKey, updatedScreen)
        }
        activeConfig = activeConfig.copy(screens = updatedScreens)

        updateLivePreview(updatedComp)
    }

    private fun populateFieldsForCurrentComponent() {
        isUpdatingFields = true
        val comp = getCurrentComponentConfig()

        // Appearance
        binding.etBackgroundColor.setText(comp.appearance.backgroundColor ?: "")
        binding.etCornerRadius.setText(comp.appearance.cornerRadius?.toString() ?: "")
        binding.etStrokeWidth.setText(comp.appearance.strokeWidth?.toString() ?: "")
        binding.etStrokeColor.setText(comp.appearance.strokeColor ?: "")
        binding.etElevation.setText(comp.appearance.elevation?.toString() ?: "")
        binding.etOpacity.setText(comp.appearance.opacity?.toString() ?: "")

        // Layout
        binding.etMarginTop.setText(comp.layout.marginTop?.toString() ?: "")
        binding.etMarginBottom.setText(comp.layout.marginBottom?.toString() ?: "")
        binding.etMarginStart.setText(comp.layout.marginStart?.toString() ?: "")
        binding.etMarginEnd.setText(comp.layout.marginEnd?.toString() ?: "")

        binding.etPaddingTop.setText(comp.layout.paddingTop?.toString() ?: "")
        binding.etPaddingBottom.setText(comp.layout.paddingBottom?.toString() ?: "")
        binding.etPaddingStart.setText(comp.layout.paddingStart?.toString() ?: "")
        binding.etPaddingEnd.setText(comp.layout.paddingEnd?.toString() ?: "")

        // Typography
        binding.etTextColor.setText(comp.typography.textColor ?: "")
        binding.etTextSize.setText(comp.typography.textSize?.toString() ?: "")
        binding.etTextStyle.setText(comp.typography.textStyle ?: "")
        binding.etTextAlign.setText(comp.typography.textAlign ?: "")

        // Visibility & Content
        binding.switchVisible.isChecked = comp.visible
        binding.etContentTitle.setText(comp.content.title ?: "")

        isUpdatingFields = false
        updateLivePreview(comp)
    }

    private fun setupFieldListeners() {
        fun addSimpleWatcher(editText: EditText, action: (String) -> Unit) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (!isUpdatingFields) {
                        action(s?.toString()?.trim() ?: "")
                    }
                }
            })
        }

        // Appearance Listeners
        addSimpleWatcher(binding.etBackgroundColor) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(backgroundColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etCornerRadius) { v ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(cornerRadius = v.toIntOrNull()))
            }
        }
        addSimpleWatcher(binding.etStrokeWidth) { v ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(strokeWidth = v.toIntOrNull()))
            }
        }
        addSimpleWatcher(binding.etStrokeColor) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(strokeColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etElevation) { v ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(elevation = v.toIntOrNull()))
            }
        }
        addSimpleWatcher(binding.etOpacity) { v ->
            updateCurrentComponentConfig { comp ->
                comp.copy(appearance = comp.appearance.copy(opacity = v.toFloatOrNull()))
            }
        }

        // Layout Margins
        addSimpleWatcher(binding.etMarginTop) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginTop = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etMarginBottom) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginBottom = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etMarginStart) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginStart = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etMarginEnd) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(marginEnd = v.toIntOrNull())) }
        }

        // Layout Paddings
        addSimpleWatcher(binding.etPaddingTop) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingTop = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etPaddingBottom) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingBottom = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etPaddingStart) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingStart = v.toIntOrNull())) }
        }
        addSimpleWatcher(binding.etPaddingEnd) { v ->
            updateCurrentComponentConfig { comp -> comp.copy(layout = comp.layout.copy(paddingEnd = v.toIntOrNull())) }
        }

        // Typography
        addSimpleWatcher(binding.etTextColor) { hex ->
            updateCurrentComponentConfig { comp ->
                comp.copy(typography = comp.typography.copy(textColor = if (hex.isBlank()) null else hex))
            }
        }
        addSimpleWatcher(binding.etTextSize) { v ->
            updateCurrentComponentConfig { comp ->
                comp.copy(typography = comp.typography.copy(textSize = v.toIntOrNull()))
            }
        }
        addSimpleWatcher(binding.etTextStyle) { style ->
            updateCurrentComponentConfig { comp ->
                comp.copy(typography = comp.typography.copy(textStyle = if (style.isBlank()) null else style))
            }
        }
        addSimpleWatcher(binding.etTextAlign) { align ->
            updateCurrentComponentConfig { comp ->
                comp.copy(typography = comp.typography.copy(textAlign = if (align.isBlank()) null else align))
            }
        }

        // Visibility & Content
        binding.switchVisible.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFields) {
                updateCurrentComponentConfig { comp -> comp.copy(visible = isChecked) }
            }
        }
        addSimpleWatcher(binding.etContentTitle) { title ->
            updateCurrentComponentConfig { comp ->
                comp.copy(content = comp.content.copy(title = if (title.isBlank()) null else title))
            }
        }
    }

    private fun updateLivePreview(comp: ComponentConfig) {
        // Swatch background indicator
        val colorInt = UiStudioEngine.parseColorSafe(comp.appearance.backgroundColor)
        val swatchDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorInt ?: Color.DKGRAY)
        }
        binding.swatchBgColor.background = swatchDrawable

        // Apply appearance & layout to preview card
        UiStudioEngine.applyToView(binding.previewCard, comp)

        // Apply typography to preview title
        UiStudioEngine.applyTypography(binding.previewTitle, comp)

        val componentLabel = screenComponentsMap[currentScreenKey]?.find { it.first == currentCompKey }?.second ?: currentCompKey
        binding.previewTitle.text = comp.content.title?.takeIf { it.isNotBlank() } ?: componentLabel
        binding.previewSubtitle.text = if (comp.visible) "Live styling active" else "Component hidden (GONE)"
    }

    private fun setupBottomActions() {
        binding.btnSaveDraft.setOnClickListener { saveDraft() }
        binding.btnValidate.setOnClickListener { validateCurrentDraft() }
        binding.btnPublish.setOnClickListener { promptPublish() }
    }

    private fun saveDraft() {
        val validation = repo.validateConfig(activeConfig)
        if (!validation.first) {
            AppBulletin.showError(this, "Validation issue: ${validation.second.firstOrNull()}")
            return
        }

        lifecycleScope.launch {
            binding.btnSaveDraft.isEnabled = false
            val result = repo.saveDraft(activeConfig)
            binding.btnSaveDraft.isEnabled = true
            result.onSuccess {
                AppBulletin.showSuccess(this@UiStudioActivity, "Draft saved successfully")
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Failed to save draft: ${e.localizedMessage}")
            }
        }
    }

    private fun validateCurrentDraft() {
        val validation = repo.validateConfig(activeConfig)
        if (validation.first) {
            AppBulletin.showSuccess(this, "Validation passed: Configuration schema is 100% valid")
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Validation Errors")
                .setMessage(validation.second.joinToString("\n• ", prefix = "• "))
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun promptPublish() {
        val validation = repo.validateConfig(activeConfig)
        if (!validation.first) {
            AppBulletin.showError(this, "Cannot publish: Fix schema errors first")
            return
        }

        val input = EditText(this).apply {
            hint = "Changelog notes (e.g. Updated card corner radii)"
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Publish UI Studio Configuration")
            .setMessage("This will push your changes live to all student devices without an app update.")
            .setView(input)
            .setPositiveButton("Publish Live") { _, _ ->
                val notes = input.text.toString().trim()
                executePublish(notes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun executePublish(notes: String) {
        lifecycleScope.launch {
            binding.btnPublish.isEnabled = false
            val result = repo.publish(notes, activeConfig)
            binding.btnPublish.isEnabled = true
            result.onSuccess { version ->
                activeConfig = activeConfig.copy(version = version)
                binding.tvStudioStatus.text = "Published v$version"
                AppBulletin.showSuccess(this@UiStudioActivity, "Published v$version successfully to all devices!")
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Publish failed: ${e.localizedMessage}")
            }
        }
    }

    private fun showVersionHistoryDialog() {
        lifecycleScope.launch {
            val result = repo.getVersions()
            result.onSuccess { versions ->
                if (versions.isEmpty()) {
                    AppBulletin.show(this@UiStudioActivity, "No previous published versions found")
                    return@onSuccess
                }

                val items = versions.map { v ->
                    "v${v.version} — ${v.notes ?: "No notes"}\nBy: ${v.createdBy}"
                }.toTypedArray()

                MaterialAlertDialogBuilder(this@UiStudioActivity)
                    .setTitle("Version History & Rollback")
                    .setItems(items) { _, which ->
                        val selected = versions[which]
                        promptRestoreVersion(selected)
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Failed to load versions: ${e.localizedMessage}")
            }
        }
    }

    private fun promptRestoreVersion(versionItem: UiStudioVersionItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Restore v${versionItem.version}")
            .setMessage("Choose how to restore this snapshot:\n• 'To Draft': Loads into studio for further editing\n• 'Directly Live': Immediately publishes as the new active version")
            .setPositiveButton("Restore to Draft") { _, _ ->
                restoreVersion(versionItem.version, "draft")
            }
            .setNeutralButton("Publish Directly") { _, _ ->
                restoreVersion(versionItem.version, "publish")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreVersion(versionId: Int, target: String) {
        lifecycleScope.launch {
            val result = repo.restoreVersion(versionId, target)
            result.onSuccess {
                AppBulletin.showSuccess(this@UiStudioActivity, "Restored v$versionId to $target")
                loadDraftFromBackend()
            }.onFailure { e ->
                AppBulletin.showError(this@UiStudioActivity, "Restore failed: ${e.localizedMessage}")
            }
        }
    }

    private fun confirmResetToDefaults() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset to Native UI Defaults?")
            .setMessage("This will clear custom UI Studio styling and return all components cleanly to the app's native layout and XML styles.")
            .setPositiveButton("Reset All") { _, _ ->
                lifecycleScope.launch {
                    val result = repo.reset("all")
                    result.onSuccess {
                        activeConfig = repo.getDefaultTemplate()
                        populateFieldsForCurrentComponent()
                        binding.tvStudioStatus.text = "Reset to Native Defaults"
                        AppBulletin.showSuccess(this@UiStudioActivity, "Reset completed. Native defaults active.")
                    }.onFailure { e ->
                        AppBulletin.showError(this@UiStudioActivity, "Reset failed: ${e.localizedMessage}")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportImportDialog() {
        val options = arrayOf("Export Configuration as JSON", "Import Configuration from JSON")
        MaterialAlertDialogBuilder(this)
            .setTitle("Import / Export Configuration")
            .setItems(options) { _, which ->
                if (which == 0) {
                    showExportJsonDialog()
                } else {
                    showImportJsonDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportJsonDialog() {
        val json = repo.exportToJson(activeConfig)
        val textView = TextView(this).apply {
            text = json
            setTextIsSelectable(true)
            setPadding(32, 24, 32, 24)
            textSize = 12f
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Exported JSON Configuration")
            .setView(textView)
            .setPositiveButton("Copy to Clipboard") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("UI Studio Config", json))
                AppBulletin.showSuccess(this, "Copied JSON configuration to clipboard")
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showImportJsonDialog() {
        val input = EditText(this).apply {
            hint = "Paste UI Studio JSON here..."
            setPadding(32, 24, 32, 24)
            textSize = 12f
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Import JSON Configuration")
            .setView(input)
            .setPositiveButton("Import & Apply") { _, _ ->
                val pastedJson = input.text.toString().trim()
                if (pastedJson.isBlank()) {
                    AppBulletin.showError(this, "Input JSON is empty")
                    return@setPositiveButton
                }
                val result = repo.importFromJson(pastedJson)
                result.onSuccess { imported ->
                    activeConfig = imported
                    populateFieldsForCurrentComponent()
                    AppBulletin.showSuccess(this, "Imported configuration successfully!")
                }.onFailure { e ->
                    AppBulletin.showError(this, "Import failed: ${e.localizedMessage}")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

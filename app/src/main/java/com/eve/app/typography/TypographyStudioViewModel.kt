package com.eve.app.typography

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eve.app.util.FontManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing state, validation, draft editing, and publishing for EVE Typography Studio.
 */
class TypographyStudioViewModel(
    private val repository: TypographyRepository = TypographyRepository.getInstance()
) : ViewModel() {

    private val _draftConfig = MutableStateFlow(repository.loadCachedDraft() ?: repository.getActiveConfig().copy(status = "draft"))
    val draftConfig: StateFlow<TypographyConfig> = _draftConfig.asStateFlow()

    private val _publishedConfig = MutableStateFlow(repository.getActiveConfig())
    val publishedConfig: StateFlow<TypographyConfig> = _publishedConfig.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Font Library, 1: Global Styles, 2: Screen Editor, 3: Live Preview, 4: Versions & Restore
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedScreenKey = MutableStateFlow("home")
    val selectedScreenKey: StateFlow<String> = _selectedScreenKey.asStateFlow()

    private val _selectedElementKey = MutableStateFlow("home_welcome_title")
    val selectedElementKey: StateFlow<String> = _selectedElementKey.asStateFlow()

    private val _previewDarkMode = MutableStateFlow(false)
    val previewDarkMode: StateFlow<Boolean> = _previewDarkMode.asStateFlow()

    private val _previewWidthDp = MutableStateFlow(360)
    val previewWidthDp: StateFlow<Int> = _previewWidthDp.asStateFlow()

    private val _previewFontScale = MutableStateFlow(1.0f)
    val previewFontScale: StateFlow<Float> = _previewFontScale.asStateFlow()

    private val _beforeAfterMode = MutableStateFlow(false)
    val beforeAfterMode: StateFlow<Boolean> = _beforeAfterMode.asStateFlow()

    private val _statusMessage = MutableStateFlow("Local draft ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _validationIssues = MutableStateFlow<List<TypographyResolver.ValidationIssue>>(emptyList())
    val validationIssues: StateFlow<List<TypographyResolver.ValidationIssue>> = _validationIssues.asStateFlow()

    init {
        revalidate()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab.coerceIn(0, 4)
    }

    fun selectScreen(screenKey: String) {
        _selectedScreenKey.value = screenKey
        val screen = TypographyRegistry.findScreen(screenKey)
        val firstElement = screen?.elements?.firstOrNull()?.key ?: ""
        _selectedElementKey.value = firstElement
    }

    fun selectElement(elementKey: String) {
        _selectedElementKey.value = elementKey
    }

    fun toggleDarkMode() {
        _previewDarkMode.value = !_previewDarkMode.value
    }

    fun setPreviewWidth(widthDp: Int) {
        _previewWidthDp.value = widthDp
    }

    fun setPreviewFontScale(scale: Float) {
        _previewFontScale.value = scale.coerceIn(0.8f, 2.0f)
    }

    fun toggleBeforeAfterMode() {
        _beforeAfterMode.value = !_beforeAfterMode.value
    }

    fun selectFontFamily(family: FontManager.FontFamily) {
        val updated = _draftConfig.value.copy(
            fontFamily = family.id,
            updatedAt = System.currentTimeMillis()
        )
        updateDraft(updated)
        _statusMessage.value = "Selected font: ${family.displayName}"
    }

    fun applyPreset(preset: TypographyPreset) {
        val updated = _draftConfig.value.copy(
            presetId = preset.id,
            fontFamily = preset.defaultFontFamily,
            globalStyles = preset.rules,
            updatedAt = System.currentTimeMillis()
        )
        updateDraft(updated)
        _statusMessage.value = "Applied preset: ${preset.name}"
    }

    fun updateGlobalRole(role: String, rule: StyleRule) {
        val currentStyles = _draftConfig.value.globalStyles.toMutableMap()
        currentStyles[role] = rule
        val updated = _draftConfig.value.copy(
            globalStyles = currentStyles,
            updatedAt = System.currentTimeMillis()
        )
        updateDraft(updated)
    }

    fun updateElementOverride(screenKey: String, elementKey: String, rule: StyleRule) {
        val currentScreenOverrides = _draftConfig.value.screenOverrides.toMutableMap()
        val screenConfig = currentScreenOverrides[screenKey] ?: ScreenTypographyConfig(screenKey)
        val elementOverrides = screenConfig.elementOverrides.toMutableMap()
        elementOverrides[elementKey] = rule

        currentScreenOverrides[screenKey] = screenConfig.copy(elementOverrides = elementOverrides)
        val updated = _draftConfig.value.copy(
            screenOverrides = currentScreenOverrides,
            updatedAt = System.currentTimeMillis()
        )
        updateDraft(updated)
        _statusMessage.value = "Updated element '$elementKey'"
    }

    fun resetElementOverride(screenKey: String, elementKey: String) {
        val currentScreenOverrides = _draftConfig.value.screenOverrides.toMutableMap()
        val screenConfig = currentScreenOverrides[screenKey] ?: return
        val elementOverrides = screenConfig.elementOverrides.toMutableMap()
        elementOverrides.remove(elementKey)

        currentScreenOverrides[screenKey] = screenConfig.copy(elementOverrides = elementOverrides)
        val updated = _draftConfig.value.copy(
            screenOverrides = currentScreenOverrides,
            updatedAt = System.currentTimeMillis()
        )
        updateDraft(updated)
        _statusMessage.value = "Reset element '$elementKey' to inherited value"
    }

    fun saveDraft(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.saveDraft(_draftConfig.value)
            if (result.isSuccess) {
                _statusMessage.value = "Draft saved locally and on server"
                onComplete?.invoke(true)
            } else {
                _statusMessage.value = "Saved draft locally"
                onComplete?.invoke(true)
            }
        }
    }

    fun discardDraft() {
        val restored = _publishedConfig.value.copy(status = "draft")
        repository.clearCachedDraft()
        _draftConfig.value = restored
        revalidate()
        _statusMessage.value = "Draft discarded · restored from active published version"
    }

    fun publish(notes: String?, onComplete: ((Boolean, String) -> Unit)? = null) {
        val issues = _validationIssues.value.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
        if (issues.isNotEmpty()) {
            val msg = "Cannot publish: ${issues.first().message}"
            _statusMessage.value = msg
            onComplete?.invoke(false, msg)
            return
        }

        viewModelScope.launch {
            val result = repository.publishDraft(notes)
            if (result.isSuccess) {
                val published = result.getOrThrow()
                _publishedConfig.value = published
                _draftConfig.value = published.copy(status = "draft")
                val msg = "Published version ${published.version} successfully"
                _statusMessage.value = msg
                onComplete?.invoke(true, msg)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Publishing failed"
                _statusMessage.value = msg
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun rollback(version: Int, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.rollback(version)
            if (result.isSuccess) {
                val published = result.getOrThrow()
                _publishedConfig.value = published
                _draftConfig.value = published.copy(status = "draft")
                val msg = "Rolled back to version ${published.version}"
                _statusMessage.value = msg
                onComplete?.invoke(true, msg)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Rollback failed"
                _statusMessage.value = msg
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun resetToFactoryDefaults(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.resetToFactoryDefaults()
            val resetConfig = result.getOrNull() ?: TypographyRepository.createDefaultConfig()
            _publishedConfig.value = resetConfig
            _draftConfig.value = resetConfig.copy(status = "draft")
            revalidate()
            _statusMessage.value = "Reset to factory default (iOS 26 Balanced)"
            onComplete?.invoke(true)
        }
    }

    fun exportJson(): String {
        return repository.exportJson(_draftConfig.value)
    }

    fun importJson(json: String): Boolean {
        val result = repository.importJson(json)
        return if (result.isSuccess) {
            val imported = result.getOrThrow()
            updateDraft(imported)
            _statusMessage.value = "Imported valid configuration"
            true
        } else {
            _statusMessage.value = "Import rejected: ${result.exceptionOrNull()?.message}"
            false
        }
    }

    private fun updateDraft(config: TypographyConfig) {
        _draftConfig.value = config
        repository.saveCachedDraft(config)
        revalidate()
    }

    private fun revalidate() {
        _validationIssues.value = TypographyResolver.validate(_draftConfig.value)
    }
}

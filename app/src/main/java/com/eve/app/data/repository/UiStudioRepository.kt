package com.eve.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.eve.app.EveApplication
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import com.eve.app.uistudio.UiStudioRegistry
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository managing UI Studio configurations, local persistent disk caches,
 * offline fallbacks, and admin editing operations.
 */
class UiStudioRepository(
    private val api: EveApiService = ApiClient.apiService,
    private val context: Context? = try { EveApplication.instance } catch (_: Throwable) { null },
    private val gson: Gson = Gson()
) {

    companion object {
        private const val TAG = "UiStudioRepository"
        private const val PREFS_NAME = "eve_ui_studio_prefs"
        private const val KEY_CACHED_CONFIG = "cached_published_config"
        private const val KEY_CACHED_VERSION = "cached_published_version"
        private const val KEY_CACHED_DRAFT = "cached_ui_studio_draft"
        private const val KEY_CACHED_DRAFT_VERSION = "cached_ui_studio_draft_version"
        private const val KEY_SESSION_STATE = "cached_ui_studio_session_state"

        @Volatile
        private var INSTANCE: UiStudioRepository? = null

        fun getInstance(): UiStudioRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UiStudioRepository().also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences? by lazy {
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (_: Throwable) {
            null
        }
    }

    // Style Clipboard for Copy / Paste Style
    private var copiedStyleAppearance: AppearanceProperties? = null
    private var copiedStyleMaterial: MaterialProperties? = null

    fun copyStyle(comp: ComponentConfig) {
        copiedStyleAppearance = comp.appearance.copy()
        copiedStyleMaterial = comp.material.copy()
    }

    fun hasCopiedStyle(): Boolean = copiedStyleAppearance != null

    fun pasteStyle(target: ComponentConfig): ComponentConfig {
        val app = copiedStyleAppearance ?: target.appearance
        val mat = copiedStyleMaterial ?: target.material
        return target.copy(appearance = app, material = mat)
    }

    fun saveSessionState(state: UiStudioSessionState) {
        try {
            prefs?.edit()?.putString(KEY_SESSION_STATE, gson.toJson(state))?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save UI Studio session state", e)
        }
    }

    fun loadSessionState(): UiStudioSessionState? {
        val json = prefs?.getString(KEY_SESSION_STATE, null) ?: return null
        return try {
            gson.fromJson(json, UiStudioSessionState::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private val _activeConfigFlow = MutableStateFlow<UiStudioConfig>(loadCachedConfig() ?: UiStudioConfig())
    val activeConfigFlow: StateFlow<UiStudioConfig> = _activeConfigFlow.asStateFlow()

    val currentConfig: UiStudioConfig
        get() = _activeConfigFlow.value

    /**
     * Loads published configuration from local SharedPreferences cache.
     */
    fun loadCachedConfig(): UiStudioConfig? {
        val json = prefs?.getString(KEY_CACHED_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, UiStudioConfig::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached UI Studio published config", e)
            null
        }
    }

    /**
     * Loads working draft configuration from local SharedPreferences cache.
     */
    fun loadCachedDraft(): UiStudioConfig? {
        val json = prefs?.getString(KEY_CACHED_DRAFT, null) ?: return null
        return try {
            gson.fromJson(json, UiStudioConfig::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached UI Studio draft config", e)
            null
        }
    }

    fun saveDraftToLocalCache(config: UiStudioConfig) {
        try {
            val json = gson.toJson(config)
            prefs?.edit()
                ?.putString(KEY_CACHED_DRAFT, json)
                ?.putInt(KEY_CACHED_DRAFT_VERSION, config.version)
                ?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save UI Studio draft to local cache", e)
        }
    }

    private fun saveToLocalCache(config: UiStudioConfig) {
        try {
            val json = gson.toJson(config)
            prefs?.edit()
                ?.putString(KEY_CACHED_CONFIG, json)
                ?.putInt(KEY_CACHED_VERSION, config.version)
                ?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save UI Studio config to local cache", e)
        }
    }

    private fun clearLocalCache() {
        prefs?.edit()
            ?.remove(KEY_CACHED_CONFIG)
            ?.remove(KEY_CACHED_VERSION)
            ?.remove(KEY_CACHED_DRAFT)
            ?.remove(KEY_CACHED_DRAFT_VERSION)
            ?.apply()
    }

    /**
     * Fetches published configuration from backend with offline & error resilience.
     * Guaranteed never to crash or blank the UI.
     */
    suspend fun fetchPublishedConfig(forceRefresh: Boolean = false): UiStudioConfig = withContext(Dispatchers.IO) {
        if (!forceRefresh && _activeConfigFlow.value.version > 0) {
            return@withContext _activeConfigFlow.value
        }

        try {
            val response = api.getPublishedUiStudioConfig()
            if (response.success && response.data != null) {
                val publishedData = response.data
                val config = publishedData.config
                if (config != null && config.screens.isNotEmpty()) {
                    saveToLocalCache(config)
                    _activeConfigFlow.value = config
                    return@withContext config
                } else {
                    val emptyConfig = UiStudioConfig()
                    _activeConfigFlow.value = emptyConfig
                    return@withContext emptyConfig
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch published UI Studio config from network: ${e.message}")
        }

        val cached = loadCachedConfig() ?: UiStudioConfig()
        _activeConfigFlow.value = cached
        cached
    }

    // ========================================================================
    // Admin Operations: Draft, Publish, Versions, Rollback, Reset
    // ========================================================================

    /**
     * Retrieves active draft configuration following the mandatory priority:
     * 1. Newest valid local draft
     * 2. Newest server-side working draft
     * 3. Latest published configuration
     * 4. Built-in defaults ONLY if genuinely no saved configuration exists.
     */
    suspend fun getDraft(): Result<UiStudioConfig> = withContext(Dispatchers.IO) {
        // Priority 1: Check local cached draft first
        val localDraft = loadCachedDraft()
        if (localDraft != null && localDraft.screens.isNotEmpty()) {
            return@withContext Result.success(localDraft)
        }

        // Priority 2: Check server-side working draft
        try {
            val response = api.getAdminUiStudioDraft()
            if (response.success && response.data?.config != null && response.data.config.screens.isNotEmpty()) {
                val config = response.data.config
                saveDraftToLocalCache(config)
                return@withContext Result.success(config)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network fetch for draft failed, fallback to local storage: ${e.message}")
        }

        // Priority 3: Check cached or live published config
        val published = loadCachedConfig()
        if (published != null && published.screens.isNotEmpty()) {
            saveDraftToLocalCache(published)
            return@withContext Result.success(published)
        }

        try {
            val livePublished = fetchPublishedConfig()
            if (livePublished.screens.isNotEmpty()) {
                saveDraftToLocalCache(livePublished)
                return@withContext Result.success(livePublished)
            }
        } catch (_: Exception) {}

        // Priority 4: Fresh installation baseline template
        val defaultTemplate = getDefaultTemplate()
        saveDraftToLocalCache(defaultTemplate)
        try {
            api.saveAdminUiStudioDraft(SaveDraftRequest(defaultTemplate))
        } catch (_: Exception) {}

        Result.success(defaultTemplate)
    }

    /**
     * Truthful draft save that distinguishes between server confirmation
     * and offline local caching.
     */
    suspend fun saveDraftDetailed(config: UiStudioConfig): SaveDraftResult = withContext(Dispatchers.IO) {
        saveDraftToLocalCache(config)
        try {
            val response = api.saveAdminUiStudioDraft(SaveDraftRequest(config))
            if (response.success && response.data?.config != null) {
                SaveDraftResult.ServerSuccess(response.data.config)
            } else {
                SaveDraftResult.LocalOfflineSuccess(config, response.error ?: "Server returned error")
            }
        } catch (e: Exception) {
            SaveDraftResult.LocalOfflineSuccess(config, e.localizedMessage ?: "Network unavailable")
        }
    }

    suspend fun saveDraft(config: UiStudioConfig): Result<UiStudioConfig> = withContext(Dispatchers.IO) {
        saveDraftToLocalCache(config)
        try {
            val response = api.saveAdminUiStudioDraft(SaveDraftRequest(config))
            if (response.success && response.data?.config != null) {
                Result.success(response.data.config)
            } else {
                Result.success(config)
            }
        } catch (e: Exception) {
            Result.success(config)
        }
    }

    /**
     * Verified publish flow:
     * 1. Saves working draft
     * 2. Calls backend publish
     * 3. Fetches live published config
     * 4. Confirms active version matches published version.
     */
    suspend fun publishVerified(notes: String, config: UiStudioConfig? = null): PublishResult = withContext(Dispatchers.IO) {
        val configToPublish = config ?: loadCachedDraft() ?: currentConfig
        saveDraftToLocalCache(configToPublish)
        try {
            val response = api.publishUiStudioConfig(PublishStudioRequest(notes = notes, config = configToPublish))
            if (!response.success) {
                return@withContext PublishResult.NetworkFailure(response.error ?: "Publish failed on server")
            }
            val expectedVersion = (response.data?.get("version") as? Number)?.toInt() ?: 1
            val publishedAt = (response.data?.get("publishedAt") as? Number)?.toLong() ?: System.currentTimeMillis()

            val liveConfig = fetchPublishedConfig(forceRefresh = true)
            if (liveConfig.version == expectedVersion || liveConfig.screens.isNotEmpty()) {
                PublishResult.VerifiedSuccess(expectedVersion, publishedAt, liveConfig)
            } else {
                PublishResult.VerificationFailed(expectedVersion, "Version mismatch: expected $expectedVersion but found ${liveConfig.version}")
            }
        } catch (e: Exception) {
            PublishResult.NetworkFailure(e.localizedMessage ?: "Network error during publish")
        }
    }

    suspend fun publish(notes: String, config: UiStudioConfig? = null): Result<Int> = withContext(Dispatchers.IO) {
        config?.let { saveDraftToLocalCache(it) }
        try {
            val response = api.publishUiStudioConfig(PublishStudioRequest(notes = notes, config = config))
            if (response.success) {
                fetchPublishedConfig(forceRefresh = true)
                val ver = (response.data?.get("version") as? Number)?.toInt() ?: 1
                Result.success(ver)
            } else {
                Result.failure(Exception(response.error ?: "Publish failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVersions(): Result<List<UiStudioVersionItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUiStudioVersions()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to load versions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreVersion(versionId: Int, target: String = "draft"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.restoreUiStudioVersion(versionId, RestoreStudioRequest(target = target))
            if (response.success) {
                if (target == "publish") {
                    fetchPublishedConfig(forceRefresh = true)
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "Failed to restore version"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reset(target: String = "all"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.resetUiStudioConfig(ResetStudioRequest(target = target))
            if (response.success) {
                if (target == "published" || target == "all") {
                    clearLocalCache()
                    _activeConfigFlow.value = UiStudioConfig()
                }
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error ?: "Reset failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAuditLogs(): Result<List<UiStudioAuditItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getUiStudioAuditLogs()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to load audit logs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // Import / Export JSON
    // ========================================================================

    fun exportToJson(config: UiStudioConfig): String {
        return gson.toJson(config)
    }

    fun importFromJson(jsonString: String): Result<UiStudioConfig> {
        return try {
            val config = gson.fromJson(jsonString, UiStudioConfig::class.java)
            val validation = validateConfig(config)
            if (!validation.first) {
                Result.failure(IllegalArgumentException("Invalid configuration: ${validation.second.joinToString(", ")}"))
            } else {
                Result.success(config)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Validates configuration schema.
     */
    fun validateConfig(config: UiStudioConfig?): Pair<Boolean, List<String>> {
        val errors = mutableListOf<String>()
        if (config == null) {
            return Pair(false, listOf("Configuration is null"))
        }

        val colorRegex = Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

        // Branding validation
        val brand = config.branding
        if (brand.brandColor.isNotBlank() && !brand.brandColor.matches(colorRegex)) {
            errors.add("Invalid branding.brandColor '${brand.brandColor}'")
        }
        if (brand.globalBackgroundColor.isNotBlank() && !brand.globalBackgroundColor.matches(colorRegex)) {
            errors.add("Invalid branding.globalBackgroundColor '${brand.globalBackgroundColor}'")
        }

        // Design System validation
        val ds = config.designSystem
        val dsColors = listOf(
            "appBackground" to ds.appBackground,
            "surfaceBackground" to ds.surfaceBackground,
            "textPrimary" to ds.textPrimary,
            "textSecondary" to ds.textSecondary,
            "accentColor" to ds.accentColor,
            "successColor" to ds.successColor,
            "warningColor" to ds.warningColor,
            "errorColor" to ds.errorColor,
            "borderColor" to ds.borderColor,
            "dividerColor" to ds.dividerColor
        )
        dsColors.forEach { (name, hex) ->
            if (hex.isNotBlank() && !hex.matches(colorRegex)) {
                errors.add("Invalid designSystem.$name '$hex'")
            }
        }

        config.screens.forEach { (screenKey, screen) ->
            screen.backgroundColor?.let {
                if (it.isNotBlank() && !it.matches(colorRegex)) {
                    errors.add("Invalid screen.backgroundColor '$it' in $screenKey")
                }
            }
            screen.components.forEach { (compKey, comp) ->
                val app = comp.appearance
                app.backgroundColor?.let {
                    if (!it.matches(colorRegex)) errors.add("Invalid backgroundColor '$it' in $screenKey.$compKey")
                }
                app.strokeColor?.let {
                    if (!it.matches(colorRegex)) errors.add("Invalid strokeColor '$it' in $screenKey.$compKey")
                }
                app.cornerRadius?.let {
                    if (it !in 0..120) errors.add("Invalid cornerRadius ($it) in $screenKey.$compKey")
                }
                app.strokeWidth?.let {
                    if (it !in 0..30) errors.add("Invalid strokeWidth ($it) in $screenKey.$compKey")
                }
                app.elevation?.let {
                    if (it !in 0..40) errors.add("Invalid elevation ($it) in $screenKey.$compKey")
                }
                app.opacity?.let {
                    if (it !in 0.0f..1.0f) errors.add("Invalid opacity ($it) in $screenKey.$compKey")
                }

                // Material / Glass Validation
                val mat = comp.material
                mat.blurRadius?.let {
                    if (it !in 0..50) errors.add("Invalid blurRadius ($it) in $screenKey.$compKey")
                }
                mat.materialOpacity?.let {
                    if (it !in 0.0f..1.0f) errors.add("Invalid materialOpacity ($it) in $screenKey.$compKey")
                }
                mat.tintColor?.let {
                    if (!it.matches(colorRegex)) errors.add("Invalid tintColor '$it' in $screenKey.$compKey")
                }
                mat.tintOpacity?.let {
                    if (it !in 0.0f..1.0f) errors.add("Invalid tintOpacity ($it) in $screenKey.$compKey")
                }

                // Animation Validation
                val anim = comp.animation
                anim.durationMs.let {
                    if (it !in 0L..10000L) errors.add("Invalid animation duration ($it ms) in $screenKey.$compKey")
                }
                anim.delayMs.let {
                    if (it !in 0L..10000L) errors.add("Invalid animation delay ($it ms) in $screenKey.$compKey")
                }

                val lay = comp.layout
                val dimList = listOf(
                    "marginTop" to lay.marginTop,
                    "marginBottom" to lay.marginBottom,
                    "marginStart" to lay.marginStart,
                    "marginEnd" to lay.marginEnd,
                    "paddingTop" to lay.paddingTop,
                    "paddingBottom" to lay.paddingBottom,
                    "paddingStart" to lay.paddingStart,
                    "paddingEnd" to lay.paddingEnd
                )
                dimList.forEach { (name, value) ->
                    value?.let {
                        if (it !in 0..300) errors.add("Invalid $name ($it) in $screenKey.$compKey")
                    }
                }

                val typo = comp.typography
                typo.textColor?.let {
                    if (!it.matches(colorRegex)) errors.add("Invalid textColor '$it' in $screenKey.$compKey")
                }
                typo.textSize?.let {
                    if (it !in 6..96) errors.add("Invalid textSize ($it) in $screenKey.$compKey")
                }
            }
        }

        return Pair(errors.isEmpty(), errors)
    }

    /**
     * Default template matching native app styling for all registered screens and components.
     */
    fun getDefaultTemplate(): UiStudioConfig {
        return UiStudioRegistry.getCompleteDefaultTemplate()
    }
}

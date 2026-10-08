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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException
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

    // Serialize network readback and publication: a pre-publish read must finish before publication.
    private val publishedMutex = Mutex()
    var lastServerSavedDraft: UiStudioConfig? = null
        private set
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
            gson.fromJson(json, UiStudioConfig::class.java)?.takeIf { validateConfig(it, allowLegacyBlur=true).first }
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

    /**
     * Fetches published configuration from backend with offline & error resilience.
     * Guaranteed never to crash or blank the UI.
     */
    suspend fun fetchPublishedConfig(forceRefresh: Boolean = false): UiStudioConfig = withContext(Dispatchers.IO) {
        publishedMutex.withLock {
            if (!forceRefresh && currentConfig.version > 0) return@withLock currentConfig
            try {
                val response = api.getPublishedUiStudioConfig()
                val data = response.data
                if (response.success && data != null) {
                    val config = data.config
                    if (config != null && config.version == data.version &&
                        config.version >= currentConfig.version && validateConfig(config, allowLegacyBlur=true).first) {
                        saveToLocalCache(config)
                        _activeConfigFlow.value = config
                    } else if (config == null && data.version == 0) {
                        // An authoritative server reset, never an error/malformed envelope.
                        val emptyConfig = UiStudioConfig()
                        saveToLocalCache(emptyConfig)
                        _activeConfigFlow.value = emptyConfig
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Published refresh failed; retaining last known good memory/disk state: ${e.message}")
            }
            // Disk initializes memory once. A failing refresh must never roll memory backwards.
            currentConfig
        }
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
        if (localDraft != null) {
            return@withContext Result.success(localDraft)
        }

        // Priority 2: Check server-side working draft
        try {
            val response = api.getAdminUiStudioDraft()
            if (response.success && response.data?.config != null && validateConfig(response.data.config).first) {
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

        // Native layout is the baseline; never save fixture text/styles as a new draft.
        val defaultTemplate = UiStudioConfig()
        saveDraftToLocalCache(defaultTemplate)

        Result.success(defaultTemplate)
    }

    /**
     * Truthful draft save that distinguishes between server confirmation,
     * offline local caching, and optimistic concurrency conflicts.
     */
    private fun sameVisualFields(a: UiStudioConfig,b: UiStudioConfig): Boolean =
        a.screens==b.screens && a.branding==b.branding && a.designSystem==b.designSystem &&
            a.schemaVersion==b.schemaVersion && a.configVersion==b.configVersion

    suspend fun saveDraftDetailed(config: UiStudioConfig, force: Boolean = false): SaveDraftResult = withContext(Dispatchers.IO) {
        saveDraftToLocalCache(config)
        val validation = validateConfig(config)
        if (!validation.first) return@withContext SaveDraftResult.Failure(validation.second.joinToString("; "))
        try {
            val response = api.saveAdminUiStudioDraft(
                SaveDraftRequest(
                    config = config,
                    baseRevision = config.revision,
                    force = force
                )
            )
            if (response.success && response.data?.config != null) {
                val savedConfig = response.data.config.copy(
                    revision = response.data.revision ?: response.data.config.revision
                )
                if (!sameVisualFields(savedConfig,config) || !validateConfig(savedConfig).first) {
                    return@withContext SaveDraftResult.Failure("Server draft fields differ from the edited configuration; local work retained")
                }
                lastServerSavedDraft = savedConfig
                saveDraftToLocalCache(savedConfig)
                SaveDraftResult.ServerSuccess(savedConfig)
            } else if (response.error == "NEWER_DRAFT_EXISTS") {
                SaveDraftResult.Conflict(
                    serverDraft = response.data?.serverDraft,
                    message = response.message ?: "A newer draft exists on the server."
                )
            } else {
                SaveDraftResult.Failure(response.message ?: response.error ?: "Server rejected draft; local copy retained")
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 409) {
                SaveDraftResult.Conflict(
                    serverDraft = null,
                    message = "A newer draft exists on the server (409 Conflict)."
                )
            } else {
                SaveDraftResult.Failure("HTTP ${e.code()}: server rejected draft; local copy retained")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveDraftResult.LocalOfflineSuccess(config, e.localizedMessage ?: "Network unavailable")
        }
    }

    suspend fun saveDraft(config: UiStudioConfig): Result<UiStudioConfig> = withContext(Dispatchers.IO) {
        saveDraftToLocalCache(config)
        try {
            val response = api.saveAdminUiStudioDraft(SaveDraftRequest(config = config, baseRevision = config.revision))
            if (response.success && response.data?.config != null) {
                Result.success(response.data.config)
            } else {
                Result.failure(Exception(response.error ?: response.message ?: "Failed to save draft to server"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
        publishedMutex.withLock {
            val source = config ?: loadCachedDraft() ?: currentConfig
            val configToPublish = when (val saved = saveDraftDetailed(source)) {
                is SaveDraftResult.ServerSuccess -> saved.config
                is SaveDraftResult.Conflict -> return@withLock PublishResult.NetworkFailure("Revision conflict: load the server draft before publishing")
                is SaveDraftResult.LocalOfflineSuccess -> return@withLock PublishResult.NetworkFailure("Draft is local only: ${saved.error}")
                is SaveDraftResult.Failure -> return@withLock PublishResult.NetworkFailure(saved.error)
            }
            try {
                val response = api.publishUiStudioConfig(PublishStudioRequest(notes = notes, config = configToPublish))
                if (!response.success) {
                    return@withLock PublishResult.NetworkFailure(response.error ?: "Publish failed on server")
                }
                val expectedVersion = (response.data?.get("version") as? Number)?.toInt()
                    ?: return@withLock PublishResult.VerificationFailed(0, "Publish response omitted its version")
                val publishedAt = (response.data?.get("publishedAt") as? Number)?.toLong() ?: System.currentTimeMillis()

                // No offline/cache fallback is permitted for publication verification.
                val readback = try { api.getPublishedUiStudioConfig() } catch (e: CancellationException) { throw e } catch (e: Exception) {
                    return@withLock PublishResult.VerificationFailed(expectedVersion, "Fresh readback unavailable: ${e.message}")
                }
                val liveConfig = readback.data?.config
                if (!readback.success || liveConfig == null) {
                    return@withLock PublishResult.VerificationFailed(expectedVersion, "Fresh readback returned no configuration")
                }
                val sameFields = validateConfig(liveConfig).first && sameVisualFields(liveConfig,source) &&
                    liveConfig.revision == configToPublish.revision
                if (expectedVersion>currentConfig.version && liveConfig.version == expectedVersion &&
                    readback.data.version == expectedVersion && liveConfig.status=="published" && sameFields) {
                    saveToLocalCache(liveConfig)
                    _activeConfigFlow.value = liveConfig
                    PublishResult.VerifiedSuccess(expectedVersion, publishedAt, liveConfig)
                } else {
                    PublishResult.VerificationFailed(expectedVersion, "Published version, revision or edited fields differ from this draft")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PublishResult.NetworkFailure(e.localizedMessage ?: "Network error during publish")
            }
        }
    }

    suspend fun publish(notes: String, config: UiStudioConfig? = null): Result<Int> = when(val result=publishVerified(notes,config)) {
        is PublishResult.VerifiedSuccess -> Result.success(result.version)
        is PublishResult.VerificationFailed -> Result.failure(Exception(result.reason))
        is PublishResult.NetworkFailure -> Result.failure(Exception(result.error))
    }

    suspend fun getServerDraft(): Result<UiStudioConfig> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getAdminUiStudioDraft()
            val draft = response.data?.config ?: error(response.error ?: "No server draft")
            check(response.success && validateConfig(draft).first) { "Invalid or rejected server draft" }
            draft
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
        publishedMutex.withLock {
            try {
                val response = api.restoreUiStudioVersion(versionId, RestoreStudioRequest(target = target,
                    baseRevision = loadCachedDraft()?.revision, publishedVersion = currentConfig.version))
                check(response.success) { response.message ?: response.error ?: "Failed to restore version" }
                if (target == "publish") {
                    val expected=(response.data?.get("version") as? Number)?.toInt() ?: error("Restore omitted its published version")
                    val fresh=api.getPublishedUiStudioConfig()
                    val restored=fresh.data?.config
                    check(fresh.success && restored!=null && fresh.data.version==expected &&
                        restored.version==expected && expected>currentConfig.version && validateConfig(restored).first) {
                        "Restore not verified by fresh published readback; last good styling retained"
                    }
                    saveToLocalCache(restored)
                    _activeConfigFlow.value=restored
                } else {
                    val fresh=api.getAdminUiStudioDraft()
                    val restored=fresh.data?.config
                    check(fresh.success && restored!=null && validateConfig(restored).first) { "Restored draft readback failed" }
                    saveDraftToLocalCache(restored)
                }
                Result.success(Unit)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
        }
    }

    suspend fun reset(target: String = "all"): Result<Unit> = withContext(Dispatchers.IO) {
        publishedMutex.withLock {
            try {
                val response = api.resetUiStudioConfig(ResetStudioRequest(target = target,
                    baseRevision = loadCachedDraft()?.revision, publishedVersion = currentConfig.version))
                check(response.success) { response.message ?: response.error ?: "Reset failed" }
                if (target == "published" || target == "all") {
                    val fresh=api.getPublishedUiStudioConfig()
                    check(fresh.success && fresh.data?.version==0 && fresh.data.config==null) {
                        "Reset not verified by fresh published readback; last good styling retained"
                    }
                    val empty=UiStudioConfig()
                    saveToLocalCache(empty)
                    _activeConfigFlow.value=empty
                }
                if (target == "draft" || target == "all") {
                    // Remove only the reset draft. A published-only reset must retain local editing work.
                    prefs?.edit()?.remove(KEY_CACHED_DRAFT)?.remove(KEY_CACHED_DRAFT_VERSION)?.apply()
                    lastServerSavedDraft=null
                }
                Result.success(Unit)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
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
    fun validateConfig(config: UiStudioConfig?, allowLegacyBlur: Boolean = false): Pair<Boolean, List<String>> = try {
        validateConfigFields(config, allowLegacyBlur)
    } catch (_: Exception) { false to listOf("Malformed configuration: required object is null or invalid") }

    private fun validateConfigFields(config: UiStudioConfig?, allowLegacyBlur: Boolean): Pair<Boolean, List<String>> {
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
                    if (it !in 0..(if(allowLegacyBlur)50 else 25)) errors.add("Invalid blurRadius ($it) in $screenKey.$compKey")
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

        errors += com.eve.app.uistudio.StudioPolicy.validate(config, allowLegacyBlur)
        return Pair(errors.isEmpty(), errors)
    }

    /**
     * Default template matching native app styling for all registered screens and components.
     */
    fun getDefaultTemplate(): UiStudioConfig {
        return UiStudioRegistry.getCompleteDefaultTemplate()
    }
}

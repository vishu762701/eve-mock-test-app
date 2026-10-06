package com.eve.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.eve.app.EveApplication
import com.eve.app.data.model.uistudio.*
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository managing UI Studio configurations, local persistent disk cache,
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

    private val _activeConfigFlow = MutableStateFlow<UiStudioConfig>(loadCachedConfig() ?: UiStudioConfig())
    val activeConfigFlow: StateFlow<UiStudioConfig> = _activeConfigFlow.asStateFlow()

    val currentConfig: UiStudioConfig
        get() = _activeConfigFlow.value

    /**
     * Loads configuration from local SharedPreferences cache.
     * Returns null if no cached configuration exists or if corrupt.
     */
    fun loadCachedConfig(): UiStudioConfig? {
        val json = prefs?.getString(KEY_CACHED_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, UiStudioConfig::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse cached UI Studio config, fallback to null", e)
            null
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
                if (config != null) {
                    saveToLocalCache(config)
                    _activeConfigFlow.value = config
                    return@withContext config
                } else {
                    // Backend has no published studio config -> clear cache and return empty config
                    clearLocalCache()
                    val emptyConfig = UiStudioConfig()
                    _activeConfigFlow.value = emptyConfig
                    return@withContext emptyConfig
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch published UI Studio config from network: ${e.message}")
        }

        // Fallback: return cached or empty config
        val cached = loadCachedConfig() ?: UiStudioConfig()
        _activeConfigFlow.value = cached
        cached
    }

    // ========================================================================
    // Admin Operations
    // ========================================================================

    suspend fun getDraft(): Result<UiStudioConfig> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAdminUiStudioDraft()
            if (response.success && response.data?.config != null) {
                Result.success(response.data.config)
            } else {
                Result.success(currentConfig)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDraft(config: UiStudioConfig): Result<UiStudioConfig> = withContext(Dispatchers.IO) {
        try {
            val response = api.saveAdminUiStudioDraft(SaveDraftRequest(config))
            if (response.success && response.data?.config != null) {
                Result.success(response.data.config)
            } else {
                Result.failure(Exception(response.error ?: "Failed to save draft"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publish(notes: String, config: UiStudioConfig? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = api.publishUiStudioConfig(PublishStudioRequest(notes = notes, config = config))
            if (response.success) {
                // Refresh published configuration locally
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

        config.screens.forEach { (screenKey, screen) ->
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
     * Default template matching native app styling for supported screens and components.
     */
    fun getDefaultTemplate(): UiStudioConfig {
        return UiStudioConfig(
            version = 1,
            notes = "Default Native Template",
            screens = mapOf(
                "home" to ScreenConfig(
                    id = "home",
                    name = "Home Screen",
                    components = mapOf(
                        "hero_banner" to ComponentConfig(
                            id = "hero_banner",
                            name = "Hero Card / Daily Challenge",
                            visible = true,
                            order = 1,
                            layout = LayoutProperties(
                                marginTop = 12,
                                marginBottom = 12,
                                marginStart = 16,
                                marginEnd = 16,
                                paddingTop = 16,
                                paddingBottom = 16,
                                paddingStart = 16,
                                paddingEnd = 16
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#1E293B",
                                cornerRadius = 16,
                                strokeWidth = 1,
                                strokeColor = "#334155",
                                opacity = 1.0f,
                                elevation = 2
                            ),
                            typography = TypographyProperties(
                                textColor = "#FFFFFF",
                                textSize = 18,
                                textStyle = "bold"
                            )
                        ),
                        "streak_pill" to ComponentConfig(
                            id = "streak_pill",
                            name = "Streak Pill",
                            visible = true,
                            order = 2,
                            layout = LayoutProperties(
                                paddingTop = 6,
                                paddingBottom = 6,
                                paddingStart = 12,
                                paddingEnd = 12
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#2A1F10",
                                cornerRadius = 20,
                                strokeWidth = 1,
                                strokeColor = "#E69B00"
                            ),
                            typography = TypographyProperties(
                                textColor = "#F59E0B",
                                textSize = 13,
                                textStyle = "bold"
                            )
                        ),
                        "find_test_panel" to ComponentConfig(
                            id = "find_test_panel",
                            name = "Search & Filter Card",
                            visible = true,
                            order = 3,
                            layout = LayoutProperties(
                                marginTop = 8,
                                marginBottom = 8,
                                marginStart = 16,
                                marginEnd = 16,
                                paddingTop = 14,
                                paddingBottom = 14,
                                paddingStart = 16,
                                paddingEnd = 16
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#1E293B",
                                cornerRadius = 14,
                                strokeWidth = 1,
                                strokeColor = "#334155"
                            ),
                            typography = TypographyProperties(
                                textColor = "#F8FAFC",
                                textSize = 15
                            )
                        ),
                        "active_exams_header" to ComponentConfig(
                            id = "active_exams_header",
                            name = "Exams Section Header",
                            visible = true,
                            order = 4,
                            layout = LayoutProperties(
                                marginStart = 16,
                                marginEnd = 16,
                                marginTop = 16,
                                marginBottom = 8
                            ),
                            typography = TypographyProperties(
                                textColor = "#94A3B8",
                                textSize = 13,
                                textStyle = "bold"
                            )
                        )
                    )
                ),
                "test" to ScreenConfig(
                    id = "test",
                    name = "Test Screen",
                    components = mapOf(
                        "timer_pill" to ComponentConfig(
                            id = "timer_pill",
                            name = "Timer Pill",
                            visible = true,
                            order = 1,
                            layout = LayoutProperties(
                                paddingTop = 6,
                                paddingBottom = 6,
                                paddingStart = 12,
                                paddingEnd = 12
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#1E293B",
                                cornerRadius = 16,
                                strokeWidth = 1,
                                strokeColor = "#334155"
                            ),
                            typography = TypographyProperties(
                                textColor = "#38BDF8",
                                textSize = 14,
                                textStyle = "bold"
                            )
                        ),
                        "question_card" to ComponentConfig(
                            id = "question_card",
                            name = "Question Card",
                            visible = true,
                            order = 2,
                            layout = LayoutProperties(
                                marginTop = 8,
                                marginBottom = 12,
                                marginStart = 16,
                                marginEnd = 16,
                                paddingTop = 16,
                                paddingBottom = 16,
                                paddingStart = 16,
                                paddingEnd = 16
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#1E293B",
                                cornerRadius = 16,
                                strokeWidth = 1,
                                strokeColor = "#334155",
                                elevation = 1
                            )
                        ),
                        "question_text" to ComponentConfig(
                            id = "question_text",
                            name = "Question Text",
                            visible = true,
                            order = 3,
                            typography = TypographyProperties(
                                textColor = "#F8FAFC",
                                textSize = 16,
                                textStyle = "normal"
                            )
                        ),
                        "option_item" to ComponentConfig(
                            id = "option_item",
                            name = "Option Item Card",
                            visible = true,
                            order = 4,
                            layout = LayoutProperties(
                                marginTop = 6,
                                marginBottom = 6,
                                paddingTop = 12,
                                paddingBottom = 12,
                                paddingStart = 14,
                                paddingEnd = 14
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#0F172A",
                                cornerRadius = 12,
                                strokeWidth = 1,
                                strokeColor = "#334155"
                            ),
                            typography = TypographyProperties(
                                textColor = "#E2E8F0",
                                textSize = 14
                            )
                        )
                    )
                ),
                "result" to ScreenConfig(
                    id = "result",
                    name = "Result Screen",
                    components = mapOf(
                        "score_card" to ComponentConfig(
                            id = "score_card",
                            name = "Score Card",
                            visible = true,
                            order = 1,
                            layout = LayoutProperties(
                                marginTop = 16,
                                marginBottom = 16,
                                marginStart = 16,
                                marginEnd = 16,
                                paddingTop = 20,
                                paddingBottom = 20,
                                paddingStart = 16,
                                paddingEnd = 16
                            ),
                            appearance = AppearanceProperties(
                                backgroundColor = "#1E293B",
                                cornerRadius = 18,
                                strokeWidth = 1,
                                strokeColor = "#334155"
                            ),
                            typography = TypographyProperties(
                                textColor = "#FFFFFF",
                                textSize = 24,
                                textStyle = "bold"
                            )
                        ),
                        "analytics_summary" to ComponentConfig(
                            id = "analytics_summary",
                            name = "Analytics Stat Pills",
                            visible = true,
                            order = 2,
                            appearance = AppearanceProperties(
                                backgroundColor = "#0F172A",
                                cornerRadius = 12,
                                strokeWidth = 1,
                                strokeColor = "#334155"
                            )
                        )
                    )
                )
            )
        )
    }
}

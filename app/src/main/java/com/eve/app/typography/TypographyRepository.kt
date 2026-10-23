package com.eve.app.typography

import android.content.Context
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import com.eve.app.util.FontManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Authoritative central repository for typography configuration:
 * - Local disk caching for offline delivery without blocking startup
 * - Legacy published configuration compatibility
 * - Active test session protection
 * - Schema validation before caching or applying
 */
class TypographyRepository(
    private val context: Context? = null,
    private val api: EveApiService = ApiClient.apiService
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Volatile
    private var activePublishedConfig: TypographyConfig = createDefaultConfig()

    @Volatile
    private var isTestActive: Boolean = false

    companion object {
        private const val PUBLISHED_CACHE_FILE = "typography_published_cache.json"

        @Volatile
        private var INSTANCE: TypographyRepository? = null

        fun getInstance(context: Context? = null): TypographyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TypographyRepository(context?.applicationContext).also { INSTANCE = it }
            }
        }

        fun createDefaultConfig(): TypographyConfig {
            val preset = TypographyPreset.TELEGRAM_NATIVE
            return TypographyConfig(
                schemaVersion = 1,
                profileId = "default",
                presetId = preset.id,
                fontFamily = preset.defaultFontFamily,
                globalStyles = preset.rules,
                version = 1,
                status = "published",
                notes = "Factory default Telegram-aligned Android typography configuration"
            )
        }
    }

    init {
        loadCachedPublished()
    }

    fun isTestSessionActive(): Boolean = isTestActive

    fun setTestSessionActive(active: Boolean) {
        isTestActive = active
    }

    fun getActiveConfig(): TypographyConfig = activePublishedConfig

    fun loadCachedPublished(): TypographyConfig {
        val file = getFile(PUBLISHED_CACHE_FILE) ?: return activePublishedConfig
        return try {
            if (file.exists()) {
                val json = file.readText()
                val parsed = gson.fromJson(json, TypographyConfig::class.java)
                if (parsed != null) {
                    val issues = TypographyResolver.validate(parsed)
                    val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
                    if (errors.isEmpty()) {
                        activePublishedConfig = parsed
                        FontManager.activeFontFamily = if (parsed.presetId == "telegram_native")
                            FontManager.FontFamily.fromId(parsed.fontFamily) else FontManager.FontFamily.ROBOTO
                        parsed
                    } else activePublishedConfig
                } else activePublishedConfig
            } else activePublishedConfig
        } catch (_: Exception) {
            activePublishedConfig
        }
    }

    fun saveCachedPublished(config: TypographyConfig) {
        activePublishedConfig = config
        FontManager.activeFontFamily = if (config.presetId == "telegram_native")
            FontManager.FontFamily.fromId(config.fontFamily) else FontManager.FontFamily.ROBOTO
        val file = getFile(PUBLISHED_CACHE_FILE) ?: return
        try {
            file.writeText(gson.toJson(config))
        } catch (_: Exception) {}
    }

    suspend fun fetchPublishedConfig(): Result<TypographyConfig> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPublishedTypography()
            if (response.success && response.data != null) {
                val issues = TypographyResolver.validate(response.data)
                val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
                if (errors.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Published configuration failed validation: ${errors.first().message}"))
                }
                // If a test session is currently active, do not mutate live typography mid-test
                if (!isTestActive) {
                    saveCachedPublished(response.data)
                }
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch typography configuration"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFile(name: String): File? {
        val baseDir = context?.filesDir ?: File("build/tmp").also { it.mkdirs() }
        return File(baseDir, name)
    }
}

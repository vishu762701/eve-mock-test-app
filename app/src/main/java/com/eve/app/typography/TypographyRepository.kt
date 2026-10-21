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
 * - Draft & publish isolation
 * - Active test session protection
 * - Schema validation before caching or applying
 * - JSON import & export with validation
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
        private const val DRAFT_CACHE_FILE = "typography_draft_cache.json"

        @Volatile
        private var INSTANCE: TypographyRepository? = null

        fun getInstance(context: Context? = null): TypographyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TypographyRepository(context?.applicationContext).also { INSTANCE = it }
            }
        }

        fun createDefaultConfig(): TypographyConfig {
            val preset = TypographyPreset.IOS26_BALANCED
            return TypographyConfig(
                schemaVersion = 1,
                profileId = "default",
                presetId = preset.id,
                fontFamily = preset.defaultFontFamily,
                globalStyles = preset.rules,
                version = 1,
                status = "published",
                notes = "Factory default iOS 26 Balanced configuration"
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
                        FontManager.activeFontFamily = FontManager.FontFamily.fromId(parsed.fontFamily)
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
        FontManager.activeFontFamily = FontManager.FontFamily.fromId(config.fontFamily)
        val file = getFile(PUBLISHED_CACHE_FILE) ?: return
        try {
            file.writeText(gson.toJson(config))
        } catch (_: Exception) {}
    }

    fun loadCachedDraft(): TypographyConfig? {
        val file = getFile(DRAFT_CACHE_FILE) ?: return null
        return try {
            if (file.exists()) {
                val json = file.readText()
                gson.fromJson(json, TypographyConfig::class.java)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedDraft(config: TypographyConfig) {
        val file = getFile(DRAFT_CACHE_FILE) ?: return
        try {
            file.writeText(gson.toJson(config.copy(status = "draft", updatedAt = System.currentTimeMillis())))
        } catch (_: Exception) {}
    }

    fun clearCachedDraft() {
        val file = getFile(DRAFT_CACHE_FILE) ?: return
        try {
            if (file.exists()) file.delete()
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

    suspend fun fetchDraftConfig(): Result<TypographyConfig> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAdminTypographyDraft()
            if (response.success && response.data != null) {
                saveCachedDraft(response.data)
                Result.success(response.data)
            } else {
                // Fallback to local cached draft or active config
                val cached = loadCachedDraft() ?: activePublishedConfig.copy(status = "draft")
                Result.success(cached)
            }
        } catch (_: Exception) {
            val cached = loadCachedDraft() ?: activePublishedConfig.copy(status = "draft")
            Result.success(cached)
        }
    }

    suspend fun saveDraft(config: TypographyConfig): Result<TypographyConfig> = withContext(Dispatchers.IO) {
        saveCachedDraft(config)
        try {
            val response = api.saveAdminTypographyDraft(config)
            if (response.success && response.data != null) {
                saveCachedDraft(response.data)
                Result.success(response.data)
            } else {
                Result.success(config)
            }
        } catch (_: Exception) {
            Result.success(config)
        }
    }

    suspend fun publishDraft(notes: String?): Result<TypographyConfig> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf("notes" to (notes ?: "Published via Typography Studio"))
            val response = api.publishTypography(body)
            if (response.success && response.data != null) {
                val issues = TypographyResolver.validate(response.data)
                val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
                if (errors.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Published response failed validation: ${errors.first().message}"))
                }
                saveCachedPublished(response.data)
                clearCachedDraft()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to publish typography"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rollback(version: Int): Result<TypographyConfig> = withContext(Dispatchers.IO) {
        try {
            val body: Map<String, Any> = mapOf("version" to version, "confirmed" to true)
            val response = api.rollbackTypography(body)
            if (response.success && response.data != null) {
                val issues = TypographyResolver.validate(response.data)
                val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
                if (errors.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Rollback configuration failed validation: ${errors.first().message}"))
                }
                saveCachedPublished(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Rollback failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetToFactoryDefaults(): Result<TypographyConfig> = withContext(Dispatchers.IO) {
        try {
            val response = api.resetTypography(mapOf("confirmed" to true))
            if (response.success && response.data != null) {
                val issues = TypographyResolver.validate(response.data)
                val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
                if (errors.isNotEmpty()) {
                    return@withContext Result.failure(Exception("Reset configuration failed validation: ${errors.first().message}"))
                }
                saveCachedPublished(response.data)
                clearCachedDraft()
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to reset typography on backend"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportJson(config: TypographyConfig): String {
        return gson.toJson(config)
    }

    fun importJson(json: String): Result<TypographyConfig> {
        return try {
            val parsed = gson.fromJson(json, TypographyConfig::class.java)
                ?: return Result.failure(IllegalArgumentException("Invalid JSON structure"))
            val issues = TypographyResolver.validate(parsed)
            val errors = issues.filter { it.level == TypographyResolver.ValidationIssue.Level.ERROR }
            if (errors.isNotEmpty()) {
                Result.failure(IllegalArgumentException("Validation failed: ${errors.first().message}"))
            } else {
                Result.success(parsed)
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

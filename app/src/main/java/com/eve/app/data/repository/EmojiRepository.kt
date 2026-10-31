package com.eve.app.data.repository

import android.util.Log
import com.eve.app.data.model.*
import com.eve.app.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmojiRepository(
    private val api: EveApiService = ApiClient.api
) {
    suspend fun getPacks(): Result<EmojiPacksResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEmojiPacks()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch emoji packs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getItems(packId: String? = null, q: String? = null, offset: Int = 0, limit: Int = 50): Result<List<EmojiItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEmojiItems(packId, q, offset, limit)
            if (response.success && response.data != null) {
                val list = response.data.map { dto ->
                    EmojiItem(
                        id = dto.id,
                        packId = dto.packId,
                        packTitle = dto.packTitle,
                        packSlug = dto.packSlug,
                        altEmoji = dto.altEmoji,
                        kind = dto.kind,
                        url = dto.url,
                        storageKey = dto.storageKey,
                        byteSize = dto.byteSize,
                        sha256 = dto.sha256,
                        enabled = dto.enabled,
                        premiumOrigin = dto.premiumOrigin,
                        telegramCustomEmojiId = dto.telegramCustomEmojiId,
                        source = dto.source,
                        createdAt = dto.createdAt
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch emoji items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePackStatus(id: String, enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.toggleEmojiPackStatus(id, mapOf("enabled" to enabled))
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.error ?: "Failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleGlobalTelegramStatus(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.toggleGlobalTelegramEmojiStatus(mapOf("enabled" to enabled))
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.error ?: "Failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePack(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteEmojiPack(id)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.error ?: "Failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteEmojiItem(id)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.error ?: "Failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importTelegramPack(packName: String, offset: Int = 0, limit: Int = 5): Result<ImportTelegramEmojiResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.importTelegramEmoji(ImportTelegramEmojiRequest(packName = packName, offset = offset, limit = limit))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to import Telegram pack"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importNotoEmoji(category: String = "popular"): Result<ImportNotoEmojiResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.importNotoEmoji(ImportNotoEmojiRequest(category = category))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to import Noto emoji"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlacements(): Result<List<EmojiPlacementAdmin>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getEmojiPlacements()
            if (response.success && response.data != null) {
                val list = response.data.map { dto ->
                    EmojiPlacementAdmin(
                        id = dto.id,
                        slotType = dto.slotType,
                        targetId = dto.targetId,
                        itemId = dto.itemId,
                        altEmoji = dto.altEmoji,
                        kind = dto.kind,
                        url = dto.url,
                        packTitle = dto.packTitle,
                        updatedBy = dto.updatedBy ?: "",
                        updatedAt = dto.updatedAt
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch placements"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setPlacement(slotType: String, targetId: String, itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.setEmojiPlacement(SetEmojiPlacementRequest(slotType, targetId, itemId))
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.error ?: "Failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearPlacement(slotType: String, targetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.clearEmojiPlacement(slotType, targetId)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.error ?: "Failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPublicPlacements(): Result<PublicEmojiDataDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getPublicEmojiPlacements()
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch public placements"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

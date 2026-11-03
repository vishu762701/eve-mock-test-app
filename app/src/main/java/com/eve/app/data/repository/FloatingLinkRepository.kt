package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.CancellationException
import java.net.URI

data class FloatingLinkData(
    val url: String,
    val x: Float? = null,
    val y: Float? = null
)

/**
 * Repository for managing the floating community link.
 * Caches the link and coordinates for 10 minutes on the client and strictly validates schemes and format.
 */
class FloatingLinkRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
        @Volatile private var cachedUrl: String? = null
        @Volatile private var cachedData: FloatingLinkData? = null
        @Volatile private var lastFetchTimestamp: Long = 0L

        /**
         * Validates that the URL uses HTTPS, does not contain forbidden schemes
         * (such as javascript:, intent:, file:), has no embedded credentials,
         * and does not exceed 500 characters.
         */
        fun isValidHttpsUrl(rawUrl: String): Boolean {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty() || trimmed.length > 500) return false

            val lower = trimmed.lowercase()
            if (lower.startsWith("javascript:") ||
                lower.startsWith("intent:") ||
                lower.startsWith("file:") ||
                lower.startsWith("http:")
            ) {
                return false
            }

            if (!lower.startsWith("https://")) {
                return false
            }

            return try {
                val uri = URI(trimmed)
                if (!uri.scheme.equals("https", ignoreCase = true)) return false
                if (!uri.userInfo.isNullOrEmpty()) return false // No embedded credentials
                if (uri.host.isNullOrBlank()) return false
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Retrieves the floating link data (url + normalized safe viewport coordinates).
     */
    suspend fun getFloatingLinkData(forceRefresh: Boolean = false): FloatingLinkData? {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedData != null && (now - lastFetchTimestamp < CACHE_TTL_MS)) {
            return cachedData
        }

        return try {
            val response = api.getFloatingLink()
            if (response.success && response.data != null) {
                val remoteUrl = response.data.url?.trim() ?: ""
                val validUrl = if (remoteUrl.isNotEmpty() && isValidHttpsUrl(remoteUrl)) remoteUrl else null
                val data = if (validUrl != null) {
                    FloatingLinkData(
                        url = validUrl,
                        x = response.data.x?.takeIf { it in 0f..1f },
                        y = response.data.y?.takeIf { it in 0f..1f }
                    )
                } else null
                cachedData = data
                cachedUrl = validUrl
                lastFetchTimestamp = now
            }
            cachedData
        } catch (_: Exception) {
            cachedData
        }
    }

    /**
     * Retrieves the floating link URL. Uses 10-minute in-memory cache.
     * On network/server failure, keeps the last cached value.
     */
    suspend fun getFloatingLink(forceRefresh: Boolean = false): String? {
        return getFloatingLinkData(forceRefresh)?.url ?: cachedUrl
    }

    /**
     * Admin: fetch current link directly from admin endpoint.
     */
    /** Keep errors/cancellation distinct: the Admin dialog must never announce an unconfirmed write. */
    private suspend fun <T> confirmed(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        Result.failure(error)
    }

    suspend fun getAdminFloatingLink(): Result<String> = confirmed {
        val res = api.getAdminFloatingLink()
        val url = res.data?.url
        if (!res.success || url == null || (url.isNotEmpty() && !isValidHttpsUrl(url))) {
            throw IllegalStateException(res.error ?: "Unable to confirm current floating link")
        }
        url.trim()
    }

    suspend fun updateFloatingLink(url: String): Result<String> = confirmed {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty() &&
            !cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)) {
            cleanUrl = "https://$cleanUrl"
        }
        if (cleanUrl.isNotEmpty() && !isValidHttpsUrl(cleanUrl)) {
            throw IllegalArgumentException("Invalid URL. Must be valid HTTPS up to 500 characters.")
        }
        val response = api.updateAdminFloatingLink(mapOf("url" to cleanUrl))
        val savedUrl = response.data?.url?.trim()
        if (!response.success || savedUrl == null ||
            (savedUrl.isNotEmpty() && !isValidHttpsUrl(savedUrl)) ||
            (cleanUrl.isNotEmpty() && savedUrl.isEmpty()) ||
            (cleanUrl.isEmpty() && savedUrl.isNotEmpty())) {
            throw IllegalStateException(response.error ?: "Floating link update was not confirmed")
        }
        cachedUrl = savedUrl.ifBlank { null }
        cachedData = savedUrl.takeIf { it.isNotEmpty() }?.let {
            FloatingLinkData(
                url = it,
                x = response.data?.x?.takeIf { coord -> coord in 0f..1f },
                y = response.data?.y?.takeIf { coord -> coord in 0f..1f }
            )
        }
        lastFetchTimestamp = System.currentTimeMillis()
        savedUrl
    }

    suspend fun updateFloatingLinkPosition(x: Float, y: Float): Result<FloatingLinkData> = confirmed {
        val clampedX = x.coerceIn(0f, 1f)
        val clampedY = y.coerceIn(0f, 1f)
        val response = api.updateAdminFloatingLink(mapOf("x" to clampedX, "y" to clampedY))
        val data = response.data
        if (!response.success || data == null) {
            throw IllegalStateException(response.error ?: "Floating link position was not confirmed")
        }
        val validX = data.x?.takeIf { it in 0f..1f }
        val validY = data.y?.takeIf { it in 0f..1f }
        if (validX == null || validY == null) {
            throw IllegalStateException("Floating link coordinates were not confirmed")
        }
        val returnedUrl = data.url?.trim().orEmpty()
        if (returnedUrl.isNotEmpty() && !isValidHttpsUrl(returnedUrl)) {
            throw IllegalStateException("Floating link response URL is invalid")
        }
        val result = FloatingLinkData(returnedUrl, validX, validY)
        cachedUrl = returnedUrl.ifBlank { null }
        cachedData = returnedUrl.takeIf { it.isNotEmpty() }?.let { result }
        lastFetchTimestamp = System.currentTimeMillis()
        result
    }

    suspend fun clearFloatingLink(): Result<Unit> = confirmed {
        val response = api.updateAdminFloatingLink(mapOf("url" to ""))
        if (!response.success || response.data?.url?.isNotEmpty() != false) {
            throw IllegalStateException(response.error ?: "Clearing the floating link was not confirmed")
        }
        cachedUrl = null
        cachedData = null
        lastFetchTimestamp = System.currentTimeMillis()
    }

}

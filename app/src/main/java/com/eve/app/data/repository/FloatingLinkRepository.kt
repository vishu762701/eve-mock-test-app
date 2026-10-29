package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
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
    suspend fun getAdminFloatingLink(): Result<String> = runCatching {
        val response = api.getAdminFloatingLink()
        if (response.success && response.data != null) {
            response.data.url?.trim() ?: ""
        } else {
            throw IllegalStateException(response.error ?: "Failed to fetch floating link")
        }
    }

    /**
     * Admin: updates or sets the floating community link URL and optionally coordinates.
     */
    suspend fun updateFloatingLink(url: String): Result<String> = runCatching {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotEmpty()) {
            if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
                !cleanUrl.startsWith("https://", ignoreCase = true)
            ) {
                cleanUrl = "https://$cleanUrl"
            }
            if (!isValidHttpsUrl(cleanUrl)) {
                throw IllegalArgumentException("Invalid URL. Must be a valid HTTPS URL up to 500 characters.")
            }
        }

        val response = api.updateAdminFloatingLink(mapOf("url" to cleanUrl))
        if (response.success) {
            val savedUrl = response.data?.url?.trim() ?: cleanUrl
            val validUrl = if (savedUrl.isNotEmpty() && isValidHttpsUrl(savedUrl)) savedUrl else null
            cachedUrl = validUrl
            cachedData = if (validUrl != null) {
                FloatingLinkData(
                    url = validUrl,
                    x = response.data?.x?.takeIf { it in 0f..1f } ?: cachedData?.x,
                    y = response.data?.y?.takeIf { it in 0f..1f } ?: cachedData?.y
                )
            } else null
            lastFetchTimestamp = System.currentTimeMillis()
            savedUrl
        } else {
            throw IllegalStateException(response.error ?: "Failed to update floating link")
        }
    }

    /**
     * Admin: updates only the normalized coordinates for the floating link.
     */
    suspend fun updateFloatingLinkPosition(x: Float, y: Float): Result<FloatingLinkData> = runCatching {
        val clampedX = x.coerceIn(0f, 1f)
        val clampedY = y.coerceIn(0f, 1f)
        val payload: Map<String, Any> = mapOf("x" to clampedX, "y" to clampedY)
        val response = api.updateAdminFloatingLink(payload)
        if (response.success) {
            val currentUrl = response.data?.url?.trim()?.takeIf { isValidHttpsUrl(it) } ?: cachedUrl ?: ""
            val updated = FloatingLinkData(url = currentUrl, x = clampedX, y = clampedY)
            cachedData = updated
            lastFetchTimestamp = System.currentTimeMillis()
            updated
        } else {
            throw IllegalStateException(response.error ?: "Failed to update floating link position")
        }
    }

    /**
     * Admin: clears the floating community link.
     */
    suspend fun clearFloatingLink(): Result<Unit> = runCatching {
        val response = api.updateAdminFloatingLink(mapOf("url" to ""))
        if (response.success) {
            cachedUrl = null
            cachedData = null
            lastFetchTimestamp = System.currentTimeMillis()
        } else {
            throw IllegalStateException(response.error ?: "Failed to clear floating link")
        }
    }
}

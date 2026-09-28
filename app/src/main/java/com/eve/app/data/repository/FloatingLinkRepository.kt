package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import java.net.URI

/**
 * Repository for managing the floating community link.
 * Caches the link for 10 minutes on the client and strictly validates schemes and format.
 */
class FloatingLinkRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
        @Volatile private var cachedUrl: String? = null
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
     * Retrieves the floating link. Uses 10-minute in-memory cache.
     * On network/server failure, keeps the last cached value.
     */
    suspend fun getFloatingLink(forceRefresh: Boolean = false): String? {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedUrl != null && (now - lastFetchTimestamp < CACHE_TTL_MS)) {
            return cachedUrl
        }

        return try {
            val response = api.getFloatingLink()
            if (response.success && response.data != null) {
                val remoteUrl = response.data.url?.trim() ?: ""
                cachedUrl = if (remoteUrl.isNotEmpty() && isValidHttpsUrl(remoteUrl)) remoteUrl else null
                lastFetchTimestamp = now
            }
            cachedUrl
        } catch (_: Exception) {
            // Keep last cached value on failure
            cachedUrl
        }
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
     * Admin: updates or sets the floating community link.
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
            cachedUrl = if (savedUrl.isNotEmpty() && isValidHttpsUrl(savedUrl)) savedUrl else null
            lastFetchTimestamp = System.currentTimeMillis()
            savedUrl
        } else {
            throw IllegalStateException(response.error ?: "Failed to update floating link")
        }
    }

    /**
     * Admin: clears the floating community link.
     */
    suspend fun clearFloatingLink(): Result<Unit> = runCatching {
        val response = api.updateAdminFloatingLink(mapOf("url" to ""))
        if (response.success) {
            cachedUrl = null
            lastFetchTimestamp = System.currentTimeMillis()
        } else {
            throw IllegalStateException(response.error ?: "Failed to clear floating link")
        }
    }
}

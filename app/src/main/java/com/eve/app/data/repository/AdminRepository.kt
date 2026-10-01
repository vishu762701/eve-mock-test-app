package com.eve.app.data.repository

import com.eve.app.EveApplication
import com.eve.app.data.remote.ApiClient
import com.eve.app.util.Constants
import com.eve.app.util.NetworkUtil
import com.eve.app.util.SessionManager

class AdminRepository {

    private val api = ApiClient.api

    /**
     * Determines whether the user is an admin.
     * Primary source: Cloudflare Worker backend (/api/auth/me).
     * Responses are cached with a short TTL in SessionManager.
     * Constants.ADMIN_EMAILS is consulted ONLY as an offline emergency fallback
     * when there is no network connection AND no cached admin status exists.
     */
    suspend fun isAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val cleanEmail = email.trim()

        // 1. Fresh cache check
        val freshCached = SessionManager.getCachedAdminStatus(cleanEmail, allowStale = false)
        if (freshCached != null) {
            return freshCached
        }

        // 2. Authoritative backend check if online
        val isOnline = NetworkUtil.isOnline(EveApplication.instance)
        if (isOnline) {
            try {
                val response = api.getMe()
                if (response.success && response.data != null) {
                    val isAdmin = response.data.isAdmin
                    SessionManager.setCachedAdminStatus(cleanEmail, isAdmin)
                    return isAdmin
                }
            } catch (_: Exception) {
                // Network or API failure: fall through to cache / emergency fallback
            }
        }

        // 3. Fallback to stale cached status if available
        val staleCached = SessionManager.getCachedAdminStatus(cleanEmail, allowStale = true)
        if (staleCached != null) {
            return staleCached
        }

        // 4. Emergency offline fallback: ONLY when there is no network AND no cached admin status
        return isOfflineEmergencyAdmin(cleanEmail)
    }

    companion object {
        /**
         * Emergency offline fallback ONLY when there is no network AND no cached admin status.
         */
        fun isOfflineEmergencyAdmin(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            return Constants.ADMIN_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }
        }
    }

    /** Dynamic admins from D1 */
    suspend fun getDynamicAdmins(): List<String> {
        val response = api.getDynamicAdmins()
        if (!response.success) {
            throw Exception(response.error ?: "Failed to fetch dynamic admins")
        }
        return response.data ?: emptyList()
    }

    suspend fun addAdmin(email: String) {
        val clean = email.trim()
        if (clean.isBlank()) return
        val response = api.addAdmin(mapOf("email" to clean))
        if (!response.success) {
            throw Exception(response.error ?: "Failed to add admin")
        }
    }

    suspend fun removeAdmin(email: String) {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return
        val response = api.removeAdmin(clean)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to remove admin")
        }
    }

    suspend fun getUsers(query: String? = null): List<com.eve.app.data.model.AdminUser> {
        val response = api.getUsers(query)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to fetch users")
        }
        return response.data ?: emptyList()
    }

    suspend fun toggleUserStatus(userId: String, disabled: Boolean): Boolean {
        val response = api.toggleUserStatus(userId, mapOf("disabled" to disabled))
        if (!response.success) {
            throw Exception(response.error ?: "Failed to toggle user status")
        }
        return response.success
    }

    suspend fun getUserAttempts(userId: String): List<com.eve.app.data.model.TestAttempt> {
        val response = api.getUserAttempts(userId)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to fetch user attempts")
        }
        return response.data ?: emptyList()
    }

    suspend fun getAppConfig(): com.eve.app.data.model.AppConfig {
        val response = api.getAppConfig()
        if (!response.success || response.data == null) {
            throw Exception(response.error ?: "Failed to fetch app configuration")
        }
        return response.data
    }

    suspend fun updateAppConfig(config: com.eve.app.data.model.AppConfig): Boolean {
        val body = mapOf(
            "minimum_supported_version_code" to config.minimum_supported_version_code,
            "maintenance_mode" to config.maintenance_mode,
            "maintenance_message" to config.maintenance_message,
            "default_publish_mode" to config.default_publish_mode
        )
        val response = api.updateAppConfig(body)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to update app configuration")
        }
        return response.success
    }
}

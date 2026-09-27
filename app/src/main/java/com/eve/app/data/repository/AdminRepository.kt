package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import com.eve.app.util.isHardcodedAdmin

class AdminRepository {

    private val api = ApiClient.api

    /** Hardcoded + Worker/D1 dynamic check */
    suspend fun isAdmin(email: String?): Boolean {
        if (email == null) return false
        if (isHardcodedAdmin(email)) return true
        return try {
            val response = api.getMe()
            response.data?.isAdmin == true
        } catch (_: Exception) {
            false
        }
    }

    /** Dynamic admins from D1 */
    suspend fun getDynamicAdmins(): List<String> = try {
        val response = api.getDynamicAdmins()
        response.data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun addAdmin(email: String) {
        val clean = email.trim()
        if (clean.isBlank()) return
        api.addAdmin(mapOf("email" to clean))
    }

    suspend fun removeAdmin(email: String) {
        val clean = email.trim().lowercase()
        if (clean.isBlank()) return
        api.removeAdmin(clean)
    }

    suspend fun getUsers(query: String? = null): List<com.eve.app.data.model.AdminUser> = try {
        val response = api.getUsers(query)
        response.data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun toggleUserStatus(userId: String, disabled: Boolean): Boolean = try {
        val response = api.toggleUserStatus(userId, mapOf("disabled" to disabled))
        response.success
    } catch (_: Exception) {
        false
    }

    suspend fun getUserAttempts(userId: String): List<com.eve.app.data.model.TestAttempt> = try {
        val response = api.getUserAttempts(userId)
        response.data ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun getAppConfig(): com.eve.app.data.model.AppConfig = try {
        val response = api.getAppConfig()
        response.data ?: com.eve.app.data.model.AppConfig()
    } catch (_: Exception) {
        com.eve.app.data.model.AppConfig()
    }

    suspend fun updateAppConfig(config: com.eve.app.data.model.AppConfig): Boolean = try {
        val body = mapOf(
            "minimum_supported_version_code" to config.minimum_supported_version_code,
            "maintenance_mode" to config.maintenance_mode,
            "maintenance_message" to config.maintenance_message
        )
        val response = api.updateAppConfig(body)
        response.success
    } catch (_: Exception) {
        false
    }
}

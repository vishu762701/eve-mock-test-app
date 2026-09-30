package com.eve.app.data.repository

import com.eve.app.data.remote.ApiClient
import com.eve.app.util.isHardcodedAdmin

class AdminRepository {

    private val api = ApiClient.api

    /** Hardcoded + Worker/D1 dynamic check */
    suspend fun isAdmin(email: String?): Boolean {
        if (email == null) return false
        if (isHardcodedAdmin(email)) return true
        val response = api.getMe()
        if (!response.success) {
            throw Exception(response.error ?: "Failed to verify admin status")
        }
        return response.data?.isAdmin == true
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
            "maintenance_message" to config.maintenance_message
        )
        val response = api.updateAppConfig(body)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to update app configuration")
        }
        return response.success
    }
}

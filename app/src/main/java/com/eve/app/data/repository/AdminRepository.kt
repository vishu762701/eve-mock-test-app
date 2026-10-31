package com.eve.app.data.repository

import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.DynamicAdminItem
import com.eve.app.data.remote.UpdateAdminRightsRequest
import com.eve.app.util.SessionManager

class AdminRepository(
    private val api: com.eve.app.data.remote.EveApiService = ApiClient.api
) {
    /** Network failure or a mismatched identity never grants admin access. */
    suspend fun isAdmin(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return false
        if (!user.email.equals(email, ignoreCase = true)) return false
        return try {
            val authorized = isAdminFresh(user.uid, email)
            val current = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            val sameIdentity = current?.uid == user.uid && current?.email.equals(email, ignoreCase = true)
            SessionManager.setCachedAdminStatus(email, authorized && sameIdentity)
            authorized && sameIdentity
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (_: Exception) {
            SessionManager.setCachedAdminStatus(email, false)
            false
        }
    }

    suspend fun isAdminFresh(uid: String, email: String): Boolean {
        val response = api.getMe()
        val profile = response.data ?: return false
        return response.success && profile.isAdmin && uid.isNotBlank() &&
            profile.uid == uid && profile.email.equals(email, ignoreCase = true)
    }

    /** Current caller admin permissions from /api/admin/me */
    suspend fun getAdminMe(): AdminMeData {
        val response = api.getAdminMe()
        if (!response.success) {
            throw Exception(response.error ?: "Failed to fetch admin permissions")
        }
        return response.data ?: AdminMeData()
    }

    /** Dynamic admins from D1 */
    suspend fun getDynamicAdmins(): List<DynamicAdminItem> {
        val response = api.getDynamicAdmins()
        if (!response.success) {
            throw Exception(response.error ?: "Failed to fetch dynamic admins")
        }
        return response.data ?: emptyList()
    }

    suspend fun addAdmin(email: String, rights: List<String> = emptyList(), customTitle: String? = null) {
        val clean = email.trim()
        if (clean.isBlank()) return
        val body = mutableMapOf<String, Any>("email" to clean)
        if (rights.isNotEmpty()) {
            body["rights"] = rights
        }
        if (!customTitle.isNullOrBlank()) {
            body["customTitle"] = customTitle.trim()
        }
        val response = api.addAdmin(body)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to add admin")
        }
    }

    suspend fun updateAdminRights(email: String, rights: List<String>, customTitle: String?): DynamicAdminItem {
        val clean = email.trim().lowercase()
        val request = UpdateAdminRightsRequest(rights = rights, customTitle = customTitle?.trim())
        val response = api.updateAdminRights(clean, request)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to update admin permissions")
        }
        return response.data ?: DynamicAdminItem(email = clean, rights = rights, customTitle = customTitle)
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

    suspend fun getUsersPage(
        query: String,
        page: Int,
        pageSize: Int = 50
    ): com.eve.app.data.model.AdminUsersPage {
        require(page > 0 && pageSize in 1..200)
        val response = api.getUsers(query.trim().ifEmpty { null }, page, pageSize)
        if (!response.success) throw Exception(response.error ?: "Failed to fetch users")
        // A deployed older Worker cannot silently masquerade as a complete page.
        val pagination = response.pagination
            ?: throw IllegalStateException("Server pagination is unavailable; please update the backend")
        return com.eve.app.data.model.AdminUsersPage(response.data ?: emptyList(), pagination)
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

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
        return response.data ?: throw IllegalStateException("Admin permissions response is incomplete")
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
        val title = customTitle?.trim()
        require(clean.isNotBlank() && "@" in clean) { "Invalid administrator email" }
        require(title == null || title.length <= 64) { "Administrator title exceeds 64 characters" }
        require(rights.distinct().size == rights.size && rights.none { it.isBlank() }) {
            "Invalid or duplicate administrator rights"
        }
        val request = UpdateAdminRightsRequest(rights = rights, customTitle = title)
        val response = api.updateAdminRights(clean, request)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to update admin permissions")
        }
        val confirmed = response.data
            ?: throw IllegalStateException("Admin rights update was not acknowledged by the server")
        // Kotlin DTO non-null types do not protect against Gson nulls.
        val returnedEmail: String? = confirmed.email
        val returnedRights: List<*>? = confirmed.rights
        val returnedTitle: String? = confirmed.customTitle
        if (returnedEmail.isNullOrBlank() || !returnedEmail.equals(clean, ignoreCase = true) ||
            returnedRights == null || returnedRights.any { it !is String || it.isBlank() } ||
            returnedRights.toSet() != rights.toSet() ||
            (title != null && returnedTitle != title)) {
            throw IllegalStateException("Server returned a mismatched administrator rights acknowledgment")
        }
        return confirmed
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
        val users = response.data
            ?: throw IllegalStateException("Server users list is missing; retry before trusting an empty page")
        val totalPages = (pagination.total.toLong() + pageSize - 1) / pageSize
        check(pagination.page == page && pagination.pageSize == pageSize &&
            pagination.total >= 0 && totalPages == pagination.totalPages.toLong() &&
            pagination.hasNext == (page < pagination.totalPages) &&
            pagination.hasPrevious == (page > 1 && pagination.totalPages > 0) &&
            users.size <= pageSize && users.size <= pagination.total &&
            (page.toLong() <= totalPages || users.isEmpty())) {
            "Server users pagination was malformed"
        }
        val seenIds = HashSet<String>()
        for (user in users) {
            // Gson may decode explicit null into nominally non-null Kotlin DTO fields.
            val value: com.eve.app.data.model.AdminUser? = user
            val id: String? = value?.id
            val email: String? = value?.email
            val name: String? = value?.displayName
            val category: String? = value?.category
            check(!id.isNullOrBlank() && email != null && name != null &&
                category != null && seenIds.add(id)) {
                "Server users list contained an incomplete or duplicate identity"
            }
        }
        return com.eve.app.data.model.AdminUsersPage(users, pagination)
    }

    suspend fun toggleUserStatus(userId: String, disabled: Boolean): Boolean {
        val response = api.toggleUserStatus(userId, mapOf("disabled" to disabled))
        if (!response.success) {
            throw Exception(response.error ?: "Failed to toggle user status")
        }
        // Do not display a success bulletin for a malformed positive response.
        val acknowledgedId = response.data?.get("id") as? String
        val acknowledgedDisabled = response.data?.get("disabled") as? Boolean
        check(acknowledgedId == userId && acknowledgedDisabled == disabled) {
            "User status update was not acknowledged by the server"
        }
        return true
    }

    suspend fun getUserAttempts(userId: String): List<com.eve.app.data.model.TestAttempt> {
        val response = api.getUserAttempts(userId)
        if (!response.success) {
            throw Exception(response.error ?: "Failed to fetch user attempts")
        }
        return response.data ?: throw IllegalStateException("Server user attempts response was incomplete")
    }

    suspend fun getAppConfig(): com.eve.app.data.model.AppConfig {
        val response = api.getAppConfig()
        if (!response.success || response.data == null) {
            throw Exception(response.error ?: "Failed to fetch app configuration")
        }
        return response.data
    }

    suspend fun updateMaintenanceMode(enabled: Boolean): com.eve.app.data.model.AppConfig {
        val response = api.updateAppConfig(mapOf("maintenance_mode" to enabled))
        if (!response.success || response.data?.maintenance_mode != enabled) {
            throw Exception(response.error ?: "Maintenance update was not acknowledged")
        }
        val confirmed = getAppConfig()
        if (confirmed.maintenance_mode != enabled) throw Exception("Maintenance readback differs; reload before retry")
        return confirmed
    }

    suspend fun updateAppConfig(config: com.eve.app.data.model.AppConfig, previous: com.eve.app.data.model.AppConfig): Boolean {
        val body = mapOf(
            "minimum_supported_version_code" to config.minimum_supported_version_code,
            "maintenance_mode" to config.maintenance_mode,
            "maintenance_message" to config.maintenance_message,
            "default_publish_mode" to config.default_publish_mode
        )
        val old = mapOf(
            "minimum_supported_version_code" to previous.minimum_supported_version_code,
            "maintenance_mode" to previous.maintenance_mode,
            "maintenance_message" to previous.maintenance_message,
            "default_publish_mode" to previous.default_publish_mode
        )
        val changes = body.filter { (key, value) -> old[key] != value }
        if (changes.isNotEmpty()) {
            val response = api.updateAppConfig(changes)
            if (!response.success) {
                throw Exception(response.error ?: "Failed to update app configuration")
            }
        }
        val confirmed = getAppConfig()
        if (confirmed != config) throw Exception("Configuration readback differs; reload before retry")
        return true
    }
}

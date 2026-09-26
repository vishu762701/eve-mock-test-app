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
}

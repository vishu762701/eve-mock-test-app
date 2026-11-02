package com.eve.app.util

import com.eve.app.data.remote.AdminMeData

/** UI-only defense in depth. Worker authentication and requirePermission remain authoritative. */
internal object AdminPermissionSnapshot {
    fun allows(data: AdminMeData?, expectedEmail: String, requiredRight: String? = null): Boolean {
        if (data == null || expectedEmail.isBlank()) return false
        // Gson can return null for Kotlin non-null fields. Reject malformed snapshots,
        // including owner snapshots, before exposing any protected UI/callback.
        val email: String? = data.email
        val rights: List<*>? = data.rights
        if (email.isNullOrBlank() || !email.equals(expectedEmail, ignoreCase = true) ||
            rights == null || rights.any { it !is String || it.isBlank() }) return false
        return requiredRight == null || data.isOwner || rights.contains(requiredRight)
    }

    suspend fun load(
        expectedEmail: String,
        requiredRight: String?,
        membership: suspend () -> Boolean,
        permissions: suspend () -> AdminMeData
    ): AdminMeData? {
        if (!membership()) return null
        return permissions().takeIf { allows(it, expectedEmail, requiredRight) }
    }
}

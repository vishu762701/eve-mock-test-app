package com.eve.app.ui.admin

import com.eve.app.data.repository.AuditLogTimeRange

/**
 * Immutable in-flight audit read identity. Only the Activity may publish results;
 * FirebaseAuth and AdminAccessGate remain the authoritative access owners.
 */
internal data class AuditReadWindow(
    val ownerUid: String,
    val ownerEmail: String,
    val epoch: Long,
    val range: AuditLogTimeRange,
    val adminEmail: String?,
    val actionType: String?,
    val searchQuery: String?,
    val before: Long?
) {
    fun matches(
        currentUid: String?,
        currentEmail: String?,
        currentEpoch: Long,
        currentRange: AuditLogTimeRange,
        currentAdminEmail: String?,
        currentActionType: String?,
        currentSearchQuery: String?
    ): Boolean =
        ownerUid.isNotBlank() && ownerEmail.isNotBlank() &&
            ownerUid == currentUid &&
            ownerEmail.equals(currentEmail, ignoreCase = true) &&
            epoch == currentEpoch && range == currentRange &&
            adminEmail == currentAdminEmail && actionType == currentActionType &&
            searchQuery == currentSearchQuery
}

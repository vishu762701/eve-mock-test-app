package com.eve.app.ui.admin

/**
 * Stateless check for whether an in-flight user-details read still belongs to
 * the selected row and the original authenticated directory window.
 * AdminAccessGate and FirebaseAuth remain the authority for current permission.
 */
internal data class UserDetailsSelection(
    val ownerUid: String,
    val userId: String,
    val page: Int,
    val query: String,
    val epoch: Long
) {
    fun matches(
        liveOwnerUid: String?,
        liveUserId: String?,
        livePage: Int,
        liveQuery: String,
        liveEpoch: Long
    ): Boolean =
        ownerUid.isNotBlank() && ownerUid == liveOwnerUid &&
            userId.isNotBlank() && userId == liveUserId &&
            page == livePage && query == liveQuery && epoch == liveEpoch
}

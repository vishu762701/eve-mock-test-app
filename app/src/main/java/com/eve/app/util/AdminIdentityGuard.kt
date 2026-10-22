package com.eve.app.util

/** Pure identity check shared by the privileged screen guard. */
internal object AdminIdentityGuard {
    fun allowed(
        authorized: Boolean,
        initialUid: String,
        initialEmail: String,
        currentUid: String?,
        currentEmail: String?
    ): Boolean =
        authorized && initialUid.isNotBlank() &&
            currentUid == initialUid &&
            currentEmail.equals(initialEmail, ignoreCase = true)
}

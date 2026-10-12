package com.eve.app.util

/**
 * Retain answers on every failure; replay only recoverable responses for the original owner.
 * Safely distinguishes:
 * 1. Eligible owner (matching UID)
 * 2. Temporarily unavailable auth / transient retryable conditions (HTTP 401, token refresh timeout)
 * 3. Signed-out state (currentUid is null/blank while ownerUid is non-blank)
 * 4. Mismatched user (currentUid != ownerUid; different authenticated account)
 * 5. Unowned legacy payload (ownerUid is null/blank)
 */
object SubmissionRetryPolicy {

    enum class AuthState {
        ELIGIBLE,
        SIGNED_OUT,
        MISMATCHED_USER,
        UNOWNED_LEGACY
    }

    fun shouldRetryHttp(status: Int): Boolean = status == 401 || status == 408 || status == 429 || status in 500..599

    fun ownsSubmission(ownerUid: String?, currentUid: String?): Boolean = !ownerUid.isNullOrBlank() && ownerUid == currentUid

    fun evaluateAuthState(ownerUid: String?, currentUid: String?): AuthState = when {
        ownerUid.isNullOrBlank() -> AuthState.UNOWNED_LEGACY
        currentUid.isNullOrBlank() -> AuthState.SIGNED_OUT
        ownerUid == currentUid -> AuthState.ELIGIBLE
        else -> AuthState.MISMATCHED_USER
    }

    fun canSubmitNow(ownerUid: String?, currentUid: String?): Boolean =
        evaluateAuthState(ownerUid, currentUid) == AuthState.ELIGIBLE
}

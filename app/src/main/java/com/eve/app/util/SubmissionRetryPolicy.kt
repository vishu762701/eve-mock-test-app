package com.eve.app.util

import androidx.work.ExistingWorkPolicy

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

    /**
     * Scheduling policy contract:
     * ExistingWorkPolicy.KEEP atomically preserves in-flight active work (ENQUEUED, RUNNING, BLOCKED)
     * without interrupting network uploads, while permitting new work when no unfinished work exists
     * (including after FAILED or CANCELLED terminal states).
     */
    val UNIQUE_WORK_POLICY: ExistingWorkPolicy = ExistingWorkPolicy.KEEP

    /**
     * Formats deterministic unique work name for an attempt.
     */
    fun uniqueWorkName(clientAttemptId: String): String = "submit_$clientAttemptId"

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

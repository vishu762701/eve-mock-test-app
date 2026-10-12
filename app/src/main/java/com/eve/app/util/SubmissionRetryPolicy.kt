package com.eve.app.util

import androidx.work.WorkInfo

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

    enum class WorkSchedulingDecision {
        DO_NOTHING,       // Work is active (ENQUEUED, RUNNING, BLOCKED) or already SUCCEEDED. Preserve existing state.
        RECOVER_FAILED,   // Prior work has terminated in FAILED or CANCELLED. Replace with fresh request to recover.
        ENQUEUE_NEW       // No existing work found. Enqueue fresh request.
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

    /**
     * Determines the exact WorkManager scheduling action across all lifecycle states:
     * - ENQUEUED, RUNNING, BLOCKED: Active work in progress -> DO_NOTHING (preserve active work).
     * - SUCCEEDED: Attempt already accepted by backend -> DO_NOTHING (never reschedule completed work).
     * - FAILED, CANCELLED: Completed failed attempt -> RECOVER_FAILED (replace with fresh request).
     * - Empty: No prior work -> ENQUEUE_NEW (enqueue fresh request).
     */
    fun decideWorkScheduling(states: List<WorkInfo.State>): WorkSchedulingDecision {
        if (states.isEmpty()) return WorkSchedulingDecision.ENQUEUE_NEW
        if (states.any { state ->
            state == WorkInfo.State.ENQUEUED ||
            state == WorkInfo.State.RUNNING ||
            state == WorkInfo.State.BLOCKED ||
            state == WorkInfo.State.SUCCEEDED
        }) {
            return WorkSchedulingDecision.DO_NOTHING
        }
        return WorkSchedulingDecision.RECOVER_FAILED
    }

    /**
     * Legacy boolean helper for backwards compatibility.
     * Active work (ENQUEUED, RUNNING, BLOCKED) is not finished and must be preserved.
     */
    fun shouldPreserveActiveWork(isFinishedStates: List<Boolean>): Boolean =
        isFinishedStates.any { isFinished -> !isFinished }
}

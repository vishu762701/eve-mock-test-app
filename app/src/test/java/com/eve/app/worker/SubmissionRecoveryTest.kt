package com.eve.app.worker

import com.eve.app.data.local.PendingAnswer
import com.eve.app.data.local.PendingSubmission
import com.eve.app.util.SubmissionRetryPolicy
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class SubmissionRecoveryTest {
    @Test fun authenticationTimeoutRateLimitAndBackendFailuresRemainRetryable() {
        listOf(401, 408, 429, 500, 502, 503, 504).forEach { assertTrue("HTTP $it should retry", SubmissionRetryPolicy.shouldRetryHttp(it)) }
        listOf(200, 400, 403, 404, 409, 422).forEach { assertFalse("HTTP $it needs confirmation or correction", SubmissionRetryPolicy.shouldRetryHttp(it)) }
    }

    @Test fun logoutAccountSwitchAndLegacyFilesCannotReplayAsAnotherStudent() {
        assertTrue(SubmissionRetryPolicy.ownsSubmission("original", "original"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("original", "other"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("original", null))
        assertFalse(SubmissionRetryPolicy.ownsSubmission(null, "other"))
        assertFalse(SubmissionRetryPolicy.ownsSubmission("", "other"))
    }

    @Test fun queuedPayloadPreservesIdentityClearedAnswersAndTimingAcrossRestart() {
        val queued = PendingSubmission("attempt-key", "exam__test", "Exam", "General", listOf(PendingAnswer("test_0", 1, "B", true, 7), PendingAnswer("test_1", 2, "", false, 3)), userId = "original")
        val restored = Gson().fromJson(Gson().toJson(queued), PendingSubmission::class.java)
        assertEquals(queued, restored)
        assertEquals("attempt-key", restored.clientAttemptId)
        assertEquals("", restored.answers[1].selected)
        assertEquals(7L, restored.answers[0].timeTakenSeconds)
    }

    @Test fun legacyJsonWithNoOwnerIsRetainedButCannotAutomaticallySubmit() {
        val legacy = Gson().fromJson("""{"clientAttemptId":"old","examId":"exam","answers":[]}""", PendingSubmission::class.java)
        assertFalse(SubmissionRetryPolicy.ownsSubmission(legacy.userId, "new-login"))
        assertFalse(SubmissionRetryPolicy.canSubmitNow(legacy.userId, "new-login"))
        assertEquals(SubmissionRetryPolicy.AuthState.UNOWNED_LEGACY, SubmissionRetryPolicy.evaluateAuthState(legacy.userId, "new-login"))
    }

    @Test fun evaluateAuthStateDistinguishesAllLifecycleStates() {
        // 1. Eligible owner: original student matches current Firebase session
        assertEquals(SubmissionRetryPolicy.AuthState.ELIGIBLE, SubmissionRetryPolicy.evaluateAuthState("student_1", "student_1"))
        assertTrue(SubmissionRetryPolicy.canSubmitNow("student_1", "student_1"))

        // 2. Signed-out state: currentUid is null or blank while ownerUid is valid
        assertEquals(SubmissionRetryPolicy.AuthState.SIGNED_OUT, SubmissionRetryPolicy.evaluateAuthState("student_1", null))
        assertEquals(SubmissionRetryPolicy.AuthState.SIGNED_OUT, SubmissionRetryPolicy.evaluateAuthState("student_1", ""))
        assertFalse(SubmissionRetryPolicy.canSubmitNow("student_1", null))
        assertFalse(SubmissionRetryPolicy.canSubmitNow("student_1", ""))

        // 3. Different authenticated user: student B is active, attempt belongs to student A
        assertEquals(SubmissionRetryPolicy.AuthState.MISMATCHED_USER, SubmissionRetryPolicy.evaluateAuthState("student_A", "student_B"))
        assertFalse(SubmissionRetryPolicy.canSubmitNow("student_A", "student_B"))

        // 4. Legacy unowned payload: ownerUid is null or blank
        assertEquals(SubmissionRetryPolicy.AuthState.UNOWNED_LEGACY, SubmissionRetryPolicy.evaluateAuthState(null, "student_1"))
        assertEquals(SubmissionRetryPolicy.AuthState.UNOWNED_LEGACY, SubmissionRetryPolicy.evaluateAuthState("", "student_1"))
        assertEquals(SubmissionRetryPolicy.AuthState.UNOWNED_LEGACY, SubmissionRetryPolicy.evaluateAuthState(null, null))
        assertFalse(SubmissionRetryPolicy.canSubmitNow(null, "student_1"))
        assertFalse(SubmissionRetryPolicy.canSubmitNow("", "student_1"))
    }

    @Test fun accountSwitchPreservesPendingAnswersWithoutSubmittingUnderWrongAccount() {
        val studentAPayload = PendingSubmission(
            clientAttemptId = "client-uuid-101",
            examId = "upsc-prelims-gs1",
            examName = "UPSC Prelims GS 1",
            category = "Civil Services",
            answers = listOf(PendingAnswer("q1", 1, "C", false, 45)),
            userId = "student_A"
        )
        // When Student B is logged in, Student A's payload must NOT be submitted
        assertFalse(SubmissionRetryPolicy.canSubmitNow(studentAPayload.userId, "student_B"))
        assertEquals(SubmissionRetryPolicy.AuthState.MISMATCHED_USER, SubmissionRetryPolicy.evaluateAuthState(studentAPayload.userId, "student_B"))

        // When Student A logs back in, it becomes immediately ELIGIBLE for submission
        assertTrue(SubmissionRetryPolicy.canSubmitNow(studentAPayload.userId, "student_A"))
        assertEquals(SubmissionRetryPolicy.AuthState.ELIGIBLE, SubmissionRetryPolicy.evaluateAuthState(studentAPayload.userId, "student_A"))
    }

    @Test fun activeWorkPreservationProtectsRunningAndEnqueuedTasks() {
        // Active states (isFinished == false) must be preserved
        assertTrue("RUNNING work must be preserved", SubmissionRetryPolicy.shouldPreserveActiveWork(listOf(false)))
        assertTrue("ENQUEUED work must be preserved", SubmissionRetryPolicy.shouldPreserveActiveWork(listOf(false)))
        assertTrue("Mix of active and finished must be preserved", SubmissionRetryPolicy.shouldPreserveActiveWork(listOf(true, false)))

        // Finished states (isFinished == true) should NOT be preserved; allows recovery with REPLACE
        assertFalse("FAILED work should not block replacement", SubmissionRetryPolicy.shouldPreserveActiveWork(listOf(true)))
        assertFalse("CANCELLED work should not block replacement", SubmissionRetryPolicy.shouldPreserveActiveWork(listOf(true)))
        assertFalse("Multiple finished works should not block replacement", SubmissionRetryPolicy.shouldPreserveActiveWork(listOf(true, true)))
        assertFalse("Empty work list should not block replacement", SubmissionRetryPolicy.shouldPreserveActiveWork(emptyList()))
    }

    @Test fun decideWorkSchedulingDistinguishesAllWorkStates() {
        // 1. Active states (ENQUEUED, RUNNING, BLOCKED) must be preserved (DO_NOTHING)
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.ENQUEUED))
        )
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.RUNNING))
        )
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.BLOCKED))
        )

        // 2. Completed successful state (SUCCEEDED) must NOT be rescheduled (DO_NOTHING)
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.SUCCEEDED))
        )

        // 3. Completed failed/cancelled states (FAILED, CANCELLED) must recover (RECOVER_FAILED)
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.RECOVER_FAILED,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.FAILED))
        )
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.RECOVER_FAILED,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.CANCELLED))
        )
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.RECOVER_FAILED,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.FAILED, androidx.work.WorkInfo.State.CANCELLED))
        )

        // 4. Empty list (no prior work) must enqueue fresh work (ENQUEUE_NEW)
        assertEquals(
            SubmissionRetryPolicy.WorkSchedulingDecision.ENQUEUE_NEW,
            SubmissionRetryPolicy.decideWorkScheduling(emptyList())
        )

        // 5. Mixed states: Active or Succeeded always takes precedence over Failed
        assertEquals(
            "Active RUNNING work takes precedence over FAILED",
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.FAILED, androidx.work.WorkInfo.State.RUNNING))
        )
        assertEquals(
            "SUCCEEDED work takes precedence over FAILED",
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.FAILED, androidx.work.WorkInfo.State.SUCCEEDED))
        )
        assertEquals(
            "ENQUEUED work takes precedence over FAILED",
            SubmissionRetryPolicy.WorkSchedulingDecision.DO_NOTHING,
            SubmissionRetryPolicy.decideWorkScheduling(listOf(androidx.work.WorkInfo.State.FAILED, androidx.work.WorkInfo.State.ENQUEUED))
        )
    }
}

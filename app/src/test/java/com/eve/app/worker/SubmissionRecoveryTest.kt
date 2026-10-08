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
    }
}

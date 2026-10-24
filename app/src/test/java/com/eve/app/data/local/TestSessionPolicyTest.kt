package com.eve.app.data.local

import org.junit.Assert.*
import org.junit.Test

class TestSessionPolicyTest {
    private val saved = TestSession("exam__test", "attempt-1", "owner", mapOf("question" to "B"))
    @Test fun resumeRequiresSameOwnerExamAndAttempt() {
        assertTrue(TestSessionPolicy.canRestore(saved, "owner", "exam__test", "attempt-1"))
        assertFalse(TestSessionPolicy.canRestore(saved, "other", "exam__test", "attempt-1"))
        assertFalse(TestSessionPolicy.canRestore(saved, "owner", "other__test", "attempt-1"))
        assertFalse(TestSessionPolicy.canRestore(saved, "owner", "exam__test", "attempt-2"))
    }
    @Test fun freshAndReattemptNeverImportPreviousAnswers() {
        assertFalse(TestSessionPolicy.canRestore(null, "owner", "exam__test", "new"))
        assertFalse(TestSessionPolicy.canRestore(saved, "owner", "exam__test", "new"))
        assertFalse(TestSessionPolicy.canRestore(saved.copy(clientAttemptId = ""), "owner", "exam__test", "new"))
        assertFalse(TestSessionPolicy.canRestore(saved.copy(userId = ""), "owner", "exam__test", "attempt-1"))
    }
    @Test fun clearedSessionRemainsAnEmptyValidResume() {
        val cleared = saved.copy(answers = emptyMap())
        assertTrue(TestSessionPolicy.canRestore(cleared, "owner", "exam__test", "attempt-1"))
        assertTrue(cleared.answers.isEmpty())
    }
}

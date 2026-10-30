package com.eve.app.ui.test

import com.eve.app.data.local.TestSession
import com.eve.app.data.local.TestSessionPolicy
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ProgressiveQuestionStripTest {
    @Test fun freshTestHasOnlyOneNumberAndNeverShowsFutureQuestions() {
        assertEquals(0, ProgressiveQuestionStrip.visibleCount(0, 0))
        assertEquals(1, ProgressiveQuestionStrip.visibleCount(8, 0))
        assertEquals(2, ProgressiveQuestionStrip.visibleCount(8, 1))
        assertEquals(3, ProgressiveQuestionStrip.visibleCount(8, 2))
        assertEquals(8, ProgressiveQuestionStrip.visibleCount(8, 100))
        assertEquals(1, ProgressiveQuestionStrip.visibleCount(1, 100))
    }

    @Test fun accountScopedSavedSessionRestoresRevealIndependentlyOfAnswerOrVisited() {
        val saved = TestSession(attemptKey = "math", clientAttemptId = "attemptA",
            userId = "studentA", currentQuestionIndex = 1, furthestRevealedIndex = 5,
            answers = mapOf("q0" to "B"), visitedQuestions = setOf("q0","q1"))
        val restored = Gson().fromJson(Gson().toJson(saved), TestSession::class.java)
        assertEquals(5, restored.furthestRevealedIndex)
        assertEquals(6, ProgressiveQuestionStrip.visibleCount(10, restored.furthestRevealedIndex))
        assertEquals(2, restored.visitedQuestions.size)
        assertEquals(1, restored.answers.size)
        assertTrue(TestSessionPolicy.canRestore(restored, "studentA", "math", "attemptA"))
        assertFalse(TestSessionPolicy.canRestore(restored, "studentB", "math", "attemptA"))
        assertFalse(TestSessionPolicy.canRestore(restored, "studentA", "math", "newAttempt"))
    }

    @Test fun previousVersionSessionsDefaultToInitialReveal() {
        val legacy = """{"attemptKey":"math","clientAttemptId":"old","userId":"studentA"}"""
        val restored = Gson().fromJson(legacy, TestSession::class.java)
        assertEquals(0, restored.furthestRevealedIndex)
    }
}

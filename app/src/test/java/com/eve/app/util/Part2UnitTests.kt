package com.eve.app.util

import com.eve.app.data.model.AnswerItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import org.junit.Assert.*
import org.junit.Test

class Part2UnitTests {

    // ------------------------------------------------------------
    // 1. PaletteState mapping tests
    // ------------------------------------------------------------
    @Test
    fun testPaletteStateMapping() {
        // Answered + Visited + Marked -> ANSWERED_MARKED
        assertEquals(
            PaletteState.ANSWERED_MARKED,
            QuestionPaletteAdapter.mapPaletteState(answered = true, visited = true, marked = true)
        )

        // Answered + Visited + Not Marked -> ANSWERED
        assertEquals(
            PaletteState.ANSWERED,
            QuestionPaletteAdapter.mapPaletteState(answered = true, visited = true, marked = false)
        )

        // Unanswered + Visited + Marked -> MARKED
        assertEquals(
            PaletteState.MARKED,
            QuestionPaletteAdapter.mapPaletteState(answered = false, visited = true, marked = true)
        )

        // Unanswered + Visited + Not Marked -> VISITED
        assertEquals(
            PaletteState.VISITED,
            QuestionPaletteAdapter.mapPaletteState(answered = false, visited = true, marked = false)
        )

        // Unanswered + Not Visited + Not Marked -> UNATTEMPTED
        assertEquals(
            PaletteState.UNATTEMPTED,
            QuestionPaletteAdapter.mapPaletteState(answered = false, visited = false, marked = false)
        )

        // Unanswered + Not Visited + Marked -> MARKED
        assertEquals(
            PaletteState.MARKED,
            QuestionPaletteAdapter.mapPaletteState(answered = false, visited = false, marked = true)
        )
    }

    // ------------------------------------------------------------
    // 2. Submit Dialog 4-line counts tests
    // ------------------------------------------------------------
    @Test
    fun testSubmitDialogCounts() {
        val total = 10
        val answeredIndices = setOf(0, 1, 6)
        val visitedIndices = setOf(0, 1, 2, 3, 6, 7, 8)
        val markedIndices = setOf(1, 2)

        val counts = QuestionPaletteAdapter.calculateSubmitDialogCounts(
            totalQuestions = total,
            answeredIndices = answeredIndices,
            visitedIndices = visitedIndices,
            markedIndices = markedIndices
        )

        assertEquals(3, counts.answered)
        assertEquals(7, counts.notAnswered)
        assertEquals(2, counts.markedForReview)
        assertEquals(3, counts.notVisited)

        // Ensure answered + notAnswered = total
        assertEquals(total, counts.answered + counts.notAnswered)
    }

    // ------------------------------------------------------------
    // 3. Topic accuracy aggregation and weakest-first order tests
    // ------------------------------------------------------------
    @Test
    fun testTopicAccuracyAggregationWeakestFirst() {
        val items = listOf(
            AnswerItem(number = 1, questionText = "Q1", selected = "A", selectedText = "A", correct = "A", correctText = "A", topic = "Physics"),
            AnswerItem(number = 2, questionText = "Q2", selected = "B", selectedText = "B", correct = "A", correctText = "A", topic = "Physics"),
            AnswerItem(number = 3, questionText = "Q3", selected = "A", selectedText = "A", correct = "A", correctText = "A", topic = "Chemistry"),
            AnswerItem(number = 4, questionText = "Q4", selected = "A", selectedText = "A", correct = "A", correctText = "A", topic = "Chemistry"),
            AnswerItem(number = 5, questionText = "Q5", selected = "B", selectedText = "B", correct = "A", correctText = "A", topic = "Math"),
            AnswerItem(number = 6, questionText = "Q6", selected = "C", selectedText = "C", correct = "A", correctText = "A", topic = "Math"),
            AnswerItem(number = 7, questionText = "Q7", selected = "A", selectedText = "A", correct = "A", correctText = "A", topic = "") // Should default to "General"
        )

        val result = TopicAccuracyHelper.aggregate(items)

        // Math: 0/2 = 0.0%
        // Physics: 1/2 = 50.0%
        // Chemistry: 2/2 = 100.0%
        // General: 1/1 = 100.0%
        assertEquals(4, result.size)

        // Weakest first: Math should be at index 0 (0% accuracy)
        assertEquals("Math", result[0].topic)
        assertEquals(0.0, result[0].accuracy, 0.01)
        assertEquals(0, result[0].correct)
        assertEquals(2, result[0].total)

        // Next weakest: Physics (50% accuracy)
        assertEquals("Physics", result[1].topic)
        assertEquals(50.0, result[1].accuracy, 0.01)
        assertEquals(1, result[1].correct)
        assertEquals(2, result[1].total)

        // General (1/1) vs Chemistry (2/2): General has lower total count (1 vs 2), sorted by total
        assertEquals(100.0, result[2].accuracy, 0.01)
        assertEquals(100.0, result[3].accuracy, 0.01)
    }

    // ------------------------------------------------------------
    // 4. availableFrom schedule lock and clock skew logic tests
    // ------------------------------------------------------------
    @Test
    fun testScheduleHelperLockAndSkew() {
        val serverNowMs = 1700000000000L
        val localNowMs = 1700000005000L // Local is 5 seconds ahead
        val skewMs = TestScheduleHelper.calculateSkew(serverNow = serverNowMs, localNow = localNowMs)
        assertEquals(-5000L, skewMs)

        // Case 1: Test scheduled in the future (availableFrom is serverNow + 60s)
        val futureAvailableFromMs = serverNowMs + 60000L
        val isLockedFuture = TestScheduleHelper.isLocked(
            availableFrom = futureAvailableFromMs,
            serverNow = serverNowMs,
            localNow = localNowMs
        )
        assertTrue("Test scheduled in future must be locked", isLockedFuture)

        // Case 2: Test scheduled in the past (availableFrom is serverNow - 10s)
        val pastAvailableFromMs = serverNowMs - 10000L
        val isLockedPast = TestScheduleHelper.isLocked(
            availableFrom = pastAvailableFromMs,
            serverNow = serverNowMs,
            localNow = localNowMs
        )
        assertFalse("Test scheduled in past must not be locked", isLockedPast)

        // Case 3: No availableFrom set (0L)
        assertFalse(TestScheduleHelper.isLocked(0L, serverNowMs, localNowMs))

        // Formatting test
        val opensAtText = TestScheduleHelper.formatOpensAt(availableFrom = futureAvailableFromMs)
        assertTrue(opensAtText.startsWith("Opens "))
    }

    // ------------------------------------------------------------
    // 5. Streak text formatting tests
    // ------------------------------------------------------------
    @Test
    fun testStreakTextFormatting() {
        val text1 = StreakHelper.formatStreakText(currentStreak = 0, todayCount = 0, goal = 2)
        assertEquals("🔥 0-day streak • Today: 0/2 tests", text1)

        val text2 = StreakHelper.formatStreakText(currentStreak = 3, todayCount = 2, goal = 2)
        assertEquals("🔥 3-day streak • Today: 2/2 tests", text2)
    }
}

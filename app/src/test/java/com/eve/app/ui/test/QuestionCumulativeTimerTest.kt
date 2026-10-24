package com.eve.app.ui.test

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test for cumulative per-question timer behavior in TestViewModel:
 * Scenario: Q1 for 10s, Q2 for 5s, back to Q1 for 7s -> Q1 = 17s, Q2 = 5s.
 */
class QuestionCumulativeTimerTest {

    @Test
    fun testCumulativeQuestionTimer_sequenceAccumulation() {
        val viewModel = TestViewModel()

        // Q1 (position 0) for 10s
        viewModel.recordQuestionTime(0, 10L)
        assertEquals(10L, viewModel.getQuestionTime(0))

        // Q2 (position 1) for 5s
        viewModel.recordQuestionTime(1, 5L)
        assertEquals(5L, viewModel.getQuestionTime(1))

        // Back to Q1 (position 0) for 7s
        viewModel.recordQuestionTime(0, 7L)

        // Verify cumulative totals: Q1 = 17s, Q2 = 5s
        assertEquals(17L, viewModel.getQuestionTime(0))
        assertEquals(5L, viewModel.getQuestionTime(1))
    }
}

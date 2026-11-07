package com.eve.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PollNormalizationTest {

    @Test
    fun zeroVotesReturnZeroPercentages() {
        val poll = Poll(
            id = "test_1",
            question = "Which subject?",
            options = listOf("Math", "Physics", "Chemistry"),
            voteCounts = emptyMap()
        )
        assertEquals(listOf(0, 0, 0), poll.getNormalizedPercentages())
        assertEquals(0, poll.getPercentageForOption(0))
        assertEquals(0, poll.getPercentageForOption(1))
        assertEquals(0, poll.getPercentageForOption(2))
    }

    @Test
    fun equalDistributionKeepsEqualPercentagesWithoutArbitraryRounding() {
        val poll = Poll(
            id = "test_2",
            question = "Pick one",
            options = listOf("A", "B", "C"),
            voteCounts = mapOf("0" to 1L, "1" to 1L, "2" to 1L)
        )
        // 1/3 each = 33% each without arbitrary bias
        assertEquals(listOf(33, 33, 33), poll.getNormalizedPercentages())
    }

    @Test
    fun unequalDistributionNormalizesToHundredPercent() {
        val poll = Poll(
            id = "test_3",
            question = "Pick one",
            options = listOf("A", "B", "C"),
            voteCounts = mapOf("0" to 10L, "1" to 8L, "2" to 7L) // total = 25
        )
        // 10/25 = 40%, 8/25 = 32%, 7/25 = 28% -> sum = 100%
        val percentages = poll.getNormalizedPercentages()
        assertEquals(listOf(40, 32, 28), percentages)
        assertEquals(100, percentages.sum())
    }

    @Test
    fun roundingRemainderAllocatedToHighestFraction() {
        // Total = 7 votes.
        // A: 3 votes (3/7 = 42.857%)
        // B: 2 votes (2/7 = 28.571%)
        // C: 2 votes (2/7 = 28.571%)
        // Floor percents: 42%, 28%, 28% -> sum = 98% (rest = 2)
        // Highest fractions: A (.857) and B/C (.571)
        val poll = Poll(
            id = "test_4",
            question = "Pick one",
            options = listOf("A", "B", "C"),
            voteCounts = mapOf("0" to 3L, "1" to 2L, "2" to 2L)
        )
        val percentages = poll.getNormalizedPercentages()
        assertEquals(100, percentages.sum())
        assertEquals(43, percentages[0])
    }
}

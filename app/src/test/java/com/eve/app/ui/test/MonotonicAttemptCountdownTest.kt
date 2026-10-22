package com.eve.app.ui.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonotonicAttemptCountdownTest {
    @Test fun countdownNeverExtendsWhenSubmissionFails() {
        val clock = MonotonicAttemptCountdown()
        assertFalse(clock.isInitialized)
        clock.start(90, 10_000)
        assertTrue(clock.isInitialized)
        assertEquals(90L, clock.remainingSeconds(10_000))
        // A 12.5-second in-flight submission must be charged to the same attempt.
        assertEquals(78L, clock.remainingSeconds(22_500))
        assertEquals(60L, clock.remainingSeconds(40_000))
        assertEquals(0L, clock.remainingSeconds(100_000))
        assertEquals(0L, clock.remainingSeconds(103_000))
    }

    @Test fun monotonicMillisDoNotUseWallClockOrPauseObservation() {
        val clock = MonotonicAttemptCountdown()
        clock.start(30, 5_000)
        assertEquals(29L, clock.remainingSeconds(6_001))
        assertEquals(1L, clock.remainingSeconds(34_999))
        assertEquals(0L, clock.remainingSeconds(35_000))
    }

    @Test fun newServerAuthorizedAttemptCanResetItsOwnDeadline() {
        val clock = MonotonicAttemptCountdown()
        clock.start(10, 1_000)
        assertEquals(0L, clock.remainingSeconds(11_000))
        clock.start(80, 12_000)
        assertEquals(80L, clock.remainingSeconds(12_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeRemainingTimeIsRejected() {
        MonotonicAttemptCountdown().start(-1, 0)
    }
}

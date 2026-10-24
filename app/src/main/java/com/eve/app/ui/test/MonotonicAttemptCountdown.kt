package com.eve.app.ui.test

/**
 * Pure monotonic clock arithmetic for the client-side display countdown.
 * Server deadline and attempt authorization remain authoritative.
 * Cancelling UI observation never changes [deadlineMs].
 */
internal class MonotonicAttemptCountdown {
    private var deadlineMs: Long? = null

    val isInitialized: Boolean get() = deadlineMs != null

    fun start(remainingSeconds: Long, nowElapsedMs: Long) {
        require(remainingSeconds >= 0L) { "Remaining time cannot be negative" }
        require(nowElapsedMs >= 0L) { "Monotonic uptime cannot be negative" }
        val boundedSeconds = remainingSeconds.coerceAtMost((Long.MAX_VALUE - nowElapsedMs) / 1000L)
        deadlineMs = nowElapsedMs + boundedSeconds * 1000L
    }

    fun remainingSeconds(nowElapsedMs: Long): Long {
        val deadline = deadlineMs ?: return 0L
        val difference = deadline - nowElapsedMs
        if (difference <= 0L) return 0L
        return difference / 1000L + if (difference % 1000L == 0L) 0L else 1L
    }
}

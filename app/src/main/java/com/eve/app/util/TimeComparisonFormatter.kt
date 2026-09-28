package com.eve.app.util

import com.eve.app.R

/**
 * Shared formatter for question time vs community average comparison in Review screen.
 */
object TimeComparisonFormatter {

    data class ComparisonResult(
        val message: String,
        val statusColorRes: Int,
        val emoji: String,
        val isFaster: Boolean
    )

    fun format(timeSpent: Long, avgTime: Long, isCorrect: Boolean): ComparisonResult {
        val isFaster = timeSpent <= avgTime
        val emoji = when {
            isFaster && isCorrect -> "🎉"
            !isCorrect -> "❌"
            isFaster -> "⚡"
            else -> "⏱️"
        }

        val message = when {
            isFaster && isCorrect -> "$emoji Yay! Faster than average ($timeSpent s vs $avgTime s) and answered correctly."
            !isCorrect -> "$emoji You took ${timeSpent}s (avg: ${avgTime}s). Double-check concept accuracy."
            isFaster -> "$emoji Fast pace ($timeSpent s vs $avgTime s). Make sure to maintain accuracy."
            else -> "$emoji You took longer than average (${timeSpent}s vs ${avgTime}s). Pace yourself!"
        }

        val colorRes = when {
            !isCorrect -> R.color.eve_status_error
            isFaster -> R.color.eve_status_success
            else -> R.color.eve_status_warning
        }

        return ComparisonResult(
            message = message,
            statusColorRes = colorRes,
            emoji = emoji,
            isFaster = isFaster
        )
    }
}

package com.eve.app.data.model

/**
 * Running aggregated stats for a question in Firestore collection: question_stats
 */
data class QuestionStat(
    val questionId: String = "",
    val examId: String = "",
    val totalAttempts: Long = 0L,
    val correctAttempts: Long = 0L,
    val totalTimeSeconds: Long = 0L,
    val avgTimeSeconds: Double = 0.0
) {
    val correctPercentage: Int
        get() = if (totalAttempts <= 0L) 0 else ((correctAttempts * 100.0) / totalAttempts).toInt()

    val calculatedAvgSeconds: Int
        get() = if (avgTimeSeconds > 0.0) avgTimeSeconds.toInt() else if (totalAttempts > 0L) (totalTimeSeconds / totalAttempts).toInt() else 45
}

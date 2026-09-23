package com.eve.app.data.model

data class ExamAnalytics(
    val examId: String = "",
    val examName: String = "",
    val category: String = "",
    val attemptCount: Long = 0L,
    val uniqueUsers: Long = 0L,
    val lastAttemptAt: Long = 0L
) {
    val participantText: String get() = "$uniqueUsers students"
}

data class QuestionAnalytics(
    val id: String = "",
    val examId: String = "",
    val examName: String = "",
    val questionId: String = "",
    val questionNumber: Int = 0,
    val questionText: String = "",
    val topic: String = "",
    val attempts: Long = 0L,
    val correct: Long = 0L,
    val wrong: Long = 0L,
    val unattempted: Long = 0L
) {
    val wrongRate: Double
        get() = if (attempts <= 0L) 0.0 else wrong * 100.0 / attempts

    val accuracy: Double
        get() = if (attempts <= 0L) 0.0 else correct * 100.0 / attempts
}

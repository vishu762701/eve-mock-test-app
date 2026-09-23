package com.eve.app.data.model

data class GeneratedQuestion(
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctAnswer: String = "A",
    val explanation: String = ""
)

data class GeneratedTest(
    val id: String = "",
    val examId: String = "",
    val examName: String = "",
    val generatedAt: Long = 0L,
    val status: String = "paused", // "paused" | "live" | "rejected"
    val questionCount: Int = 0,
    val questions: List<GeneratedQuestion> = emptyList()
) {
    val isLive: Boolean get() = status.equals("live", ignoreCase = true)
}

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
    val testNumber: String = "Test 1",
    val title: String = "",
    val generatedAt: Long = 0L,
    val status: String = "paused", // "paused" | "live" | "rejected"
    val questionCount: Int = 0,
    val questions: List<GeneratedQuestion> = emptyList()
) {
    val isLive: Boolean get() = status.equals("live", ignoreCase = true)
    val displayTitle: String get() = when {
        title.isNotBlank() -> title
        testNumber.isNotBlank() -> "$examName - $testNumber"
        else -> examName.ifBlank { "Generated Test" }
    }
}

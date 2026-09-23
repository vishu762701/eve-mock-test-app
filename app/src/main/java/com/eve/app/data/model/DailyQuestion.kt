package com.eve.app.data.model

/**
 * Phase 20: Firestore collection `daily_questions`.
 * Document fields exam questions jaisi hain + `date` (yyyy-MM-dd, IST).
 */
data class DailyQuestion(
    val id: String = "",
    val date: String = "",
    val questionText: String = "",
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val topic: String = "",
    val questionTextHi: String = "",
    val optionAHi: String = "",
    val optionBHi: String = "",
    val optionCHi: String = "",
    val optionDHi: String = "",
    val explanationHi: String = ""
) {
    fun toQuestion(): Question = Question(
        id = id,
        examId = "daily-gk",
        questionText = questionText,
        optionA = optionA,
        optionB = optionB,
        optionC = optionC,
        optionD = optionD,
        correctAnswer = correctAnswer,
        explanation = explanation,
        topic = topic.ifBlank { "Current Affairs" },
        questionTextHi = questionTextHi,
        optionAHi = optionAHi,
        optionBHi = optionBHi,
        optionCHi = optionCHi,
        optionDHi = optionDHi,
        explanationHi = explanationHi
    )
}

/** Ek calendar day ke quiz ka summary — Home / archive list ke liye. */
data class DailyQuizDay(
    val date: String,
    val questionCount: Int
) {
    val timeLimitMinutes: Int get() = questionCount.coerceAtLeast(1)
}

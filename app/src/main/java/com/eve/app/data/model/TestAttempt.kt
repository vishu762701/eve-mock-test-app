package com.eve.app.data.model

/**
 * Firestore collection: attempts. Ek document = ek user ka ek test submit karna.
 * Manually parse hota hai (AnswerItem ke fields me defaults nahi hain isliye Firestore
 * ka automatic toObject() nested list ke liye kaam nahi karega) — dekho HistoryRepository.
 */
data class TestAttempt(
    val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val category: String = "",
    val score: Double = 0.0,
    val total: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val unattempted: Int = 0,
    val timestamp: Long = 0L,
    val answers: List<AnswerItem> = emptyList()
) {
    val scoreText: String get() = if (score % 1.0 == 0.0) score.toInt().toString() else String.format("%.2f", score)
}

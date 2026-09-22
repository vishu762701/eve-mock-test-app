package com.eve.app.data.model

/**
 * Phase 19: ek exam ke andar ek year (+ optional paper/shift) ke PYQ questions ka group.
 * Alag Firestore collection nahi — questions collection ke isPyq/pyqYear/pyqPaper fields
 * se client-side group hota hai.
 */
data class PyqSet(
    val examId: String,
    val examName: String,
    val category: String,
    val year: Int,
    val paper: String,
    val questionCount: Int,
    val timeLimitMinutes: Int
) {
    val title: String
        get() = if (paper.isBlank()) "$year" else "$year • $paper"

    val subtitle: String
        get() = if (questionCount == 1) "1 question" else "$questionCount questions"
}

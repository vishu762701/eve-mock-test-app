package com.eve.app.data.model

/** Firestore collection: exams */
data class Exam(
    val id: String = "",
    val examName: String = "",
    val timeLimitMinutes: Int = 30
)

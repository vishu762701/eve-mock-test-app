package com.eve.app.data.model

/** Firestore collection: exams. category = "SSC" | "UPSC" | "Banking" | ... (Constants.CATEGORIES) */
data class Exam(
    val id: String = "",
    val examName: String = "",
    val timeLimitMinutes: Int = 30,
    val category: String = ""
) {
    /** Purane exams jinme category field nahi hai unke liye fallback */
    val categoryOrOther: String get() = category.ifBlank { "Other" }
}

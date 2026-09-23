package com.eve.app.data.model

/** Firestore collection: exams. category = "SSC" | "UPSC" | "Banking" | ... (Constants.CATEGORIES) */
data class Exam(
    val id: String = "",
    val examName: String = "",
    val timeLimitMinutes: Int = 30,
    val category: String = "",
    val syllabus: String = "",
    val questionCount: Int = 20,
    val customPromptNotes: String = "",
    val autoGenerationEnabled: Boolean = false
) {
    /** Fallback for older exams without category */
    val categoryOrOther: String get() = category.ifBlank { "Other" }
}

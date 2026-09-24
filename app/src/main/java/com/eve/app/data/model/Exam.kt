package com.eve.app.data.model

/**
 * Firestore collection: exams.
 * Per-exam fields: examName, testNumber, questionCount, autoGenEnabled, autoGenTime, timezone,
 * syllabusUrl, syllabusFileName, generationPrompt, lastGeneratedDate, lastGenerationStatus, lastGenerationError.
 * Existing exams default autoGenEnabled=true, autoGenTime="00:00" without destructive migration.
 */
data class Exam(
    val id: String = "",
    val examName: String = "",
    val timeLimitMinutes: Int = 30,
    val category: String = "",
    val syllabus: String = "",
    val questionCount: Int = 20,
    val customPromptNotes: String = "",
    val autoGenerationEnabled: Boolean = true,
    val autoGenTime: String = "00:00",
    val timezone: String = "Asia/Kolkata",
    val testNumber: String = "Test 1",
    val syllabusUrl: String = "",
    val syllabusFileName: String = "",
    val generationPrompt: String = "",
    val lastGeneratedDate: String = "",
    val lastGenerationStatus: String = "",
    val lastGenerationError: String = "",
    val lastGenerationTime: Long = 0L
) {
    val categoryOrOther: String get() = category.ifBlank { "Other" }
    val autoGenEnabled: Boolean get() = autoGenerationEnabled
}

package com.eve.app.data.model

data class FlaggedQuestion(
    val id: String = "",
    val questionId: String = "",
    val examId: String = "",
    val examName: String = "",
    val questionText: String = "",
    val reason: String = "",
    val comment: String = "",
    val studentId: String = "",
    val studentEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DISMISSED = "dismissed"

        val REASONS = listOf(
            "Wrong answer marked correct",
            "Confusing wording",
            "Typo/formatting issue",
            "Out of syllabus",
            "Other"
        )
    }
}

/** Aggregated view model for Admin Inbox showing flag count and student comments */
data class AggregatedFlaggedQuestion(
    val questionId: String,
    val examId: String,
    val examName: String,
    val questionText: String,
    val flagCount: Int,
    val reasons: List<String>,
    val comments: List<String>,
    val flagIds: List<String>,
    val latestTimestamp: Long
)

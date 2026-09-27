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
    val status: String = STATUS_PENDING,
    val reportType: String = TYPE_CONTENT
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_DISMISSED = "dismissed"

        const val TYPE_CONTENT = "content"
        const val TYPE_TECHNICAL = "technical"

        val CONTENT_ISSUES = listOf(
            "Wrong Question",
            "No Solution",
            "Wrong Translation",
            "Out of Syllabus"
        )

        val TECHNICAL_ISSUES = listOf(
            "Question and Options not visible",
            "Blinking Screen Issue",
            "Formatting Issues",
            "Scroll Not Working",
            "Dark Mode Issue",
            "Question not visible but Options visible"
        )

        const val OTHER = "Other"

        val REASONS = CONTENT_ISSUES + TECHNICAL_ISSUES + listOf(OTHER)

        fun isContentIssue(category: String): Boolean {
            return category in CONTENT_ISSUES || category == OTHER || category.startsWith("Wrong")
        }

        fun isCommentValid(comment: String): Boolean {
            val trimmed = comment.trim()
            if (trimmed.length >= 40) return true
            val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
            return words.size >= 7
        }
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
    val latestTimestamp: Long,
    val reportType: String = FlaggedQuestion.TYPE_CONTENT
)

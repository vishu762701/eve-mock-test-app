package com.eve.app.data.model

/**
 * Task B: Data model for admin-published feedback posts displayed on the Home screen.
 */
data class FeedbackPost(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorId: String = "",
    val authorEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

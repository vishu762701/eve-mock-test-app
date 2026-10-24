package com.eve.app.data.model

/**
 * Task B: Data model for student replies to admin feedback posts.
 * Stored in subcollection: feedback_posts/{postId}/replies/{replyId}
 */
data class FeedbackPostReply(
    val id: String = "",
    val postId: String = "",
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

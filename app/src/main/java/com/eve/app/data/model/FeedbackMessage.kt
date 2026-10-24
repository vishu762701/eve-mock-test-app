package com.eve.app.data.model

/**
 * Task B: Data model for student feedback messages.
 */
data class FeedbackMessage(
    val id: String = "",
    val message: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val postId: String? = null,
    val postTitle: String? = null
)

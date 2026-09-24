package com.eve.app.data.model

/**
 * Task 2: Data model for student feedback messages.
 */
data class FeedbackMessage(
    val id: String = "",
    val message: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)

package com.eve.app.data.repository

import com.eve.app.data.model.FeedbackMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Task 2: Firestore repository for sending and managing feedback messages.
 */
class FeedbackRepository {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("feedback_messages")

    suspend fun sendFeedback(
        userId: String,
        userName: String,
        userEmail: String,
        message: String
    ): Result<Unit> = runCatching {
        val trimmed = message.trim()
        require(trimmed.isNotEmpty()) { "Message cannot be empty" }
        require(trimmed.length <= 1000) { "Message cannot exceed 1000 characters" }

        val docRef = collection.document()
        val data = hashMapOf(
            "id" to docRef.id,
            "message" to trimmed,
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )
        docRef.set(data).await()
    }

    suspend fun getFeedbackMessages(): List<FeedbackMessage> {
        val snap = collection.orderBy("timestamp", Query.Direction.DESCENDING).get().await()
        return snap.documents.mapNotNull { doc ->
            val id = doc.id
            val message = doc.getString("message") ?: return@mapNotNull null
            val userId = doc.getString("userId") ?: ""
            val userName = doc.getString("userName") ?: "Student"
            val userEmail = doc.getString("userEmail") ?: ""
            val timestamp = doc.getLong("timestamp") ?: 0L
            val read = doc.getBoolean("read") ?: false
            FeedbackMessage(id, message, userId, userName, userEmail, timestamp, read)
        }
    }

    suspend fun markAsRead(id: String): Result<Unit> = runCatching {
        collection.document(id).update("read", true).await()
    }

    suspend fun deleteFeedbackMessage(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }
}

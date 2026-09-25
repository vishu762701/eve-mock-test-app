package com.eve.app.data.repository

import com.eve.app.data.model.FeedbackMessage
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.model.FeedbackPostReply
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Task B: Firestore repository for student feedback messages, admin feedback posts,
 * and per-post replies.
 */
class FeedbackRepository {
    private val db = FirebaseFirestore.getInstance()
    private val messagesCollection = db.collection("feedback_messages")
    private val postsCollection = db.collection("feedback_posts")

    // --- Student Feedback Messages ---

    suspend fun sendFeedback(
        userId: String,
        userName: String,
        userEmail: String,
        message: String,
        postId: String? = null,
        postTitle: String? = null
    ): Result<Unit> = runCatching {
        val trimmed = message.trim()
        require(trimmed.isNotEmpty()) { "Message cannot be empty" }
        require(trimmed.length <= 1000) { "Message cannot exceed 1000 characters" }

        val docRef = messagesCollection.document()
        val data = hashMapOf<String, Any>(
            "id" to docRef.id,
            "message" to trimmed,
            "userId" to userId,
            "userName" to userName,
            "userEmail" to userEmail,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )
        if (!postId.isNullOrEmpty()) {
            data["postId"] = postId
        }
        if (!postTitle.isNullOrEmpty()) {
            data["postTitle"] = postTitle
        }
        docRef.set(data).await()
    }

    suspend fun getFeedbackMessages(): List<FeedbackMessage> {
        val snap = messagesCollection.orderBy("timestamp", Query.Direction.DESCENDING).get().await()
        return snap.documents.mapNotNull { doc ->
            val id = doc.id
            val message = doc.getString("message") ?: return@mapNotNull null
            val userId = doc.getString("userId") ?: ""
            val userName = doc.getString("userName") ?: "Student"
            val userEmail = doc.getString("userEmail") ?: ""
            val timestamp = doc.getLong("timestamp") ?: 0L
            val read = doc.getBoolean("read") ?: false
            val postId = doc.getString("postId")
            val postTitle = doc.getString("postTitle")
            FeedbackMessage(id, message, userId, userName, userEmail, timestamp, read, postId, postTitle)
        }
    }

    suspend fun markAsRead(id: String): Result<Unit> = runCatching {
        messagesCollection.document(id).update("read", true).await()
    }

    suspend fun deleteFeedbackMessage(id: String): Result<Unit> = runCatching {
        messagesCollection.document(id).delete().await()
    }

    // --- Admin Feedback Posts ---

    suspend fun createFeedbackPost(
        title: String,
        message: String,
        authorId: String,
        authorEmail: String
    ): Result<String> = runCatching {
        val trimmedTitle = title.trim()
        val trimmedMessage = message.trim()
        require(trimmedTitle.isNotEmpty()) { "Title cannot be empty" }
        require(trimmedMessage.isNotEmpty()) { "Message cannot be empty" }

        val docRef = postsCollection.document()
        val data = hashMapOf(
            "id" to docRef.id,
            "title" to trimmedTitle,
            "message" to trimmedMessage,
            "authorId" to authorId,
            "authorEmail" to authorEmail,
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(data).await()
        docRef.id
    }

    suspend fun getFeedbackPosts(): List<FeedbackPost> {
        return try {
            val snap = postsCollection.orderBy("timestamp", Query.Direction.DESCENDING).get().await()
            snap.documents.mapNotNull { doc ->
                val id = doc.id
                val title = doc.getString("title") ?: return@mapNotNull null
                val message = doc.getString("message") ?: ""
                val authorId = doc.getString("authorId") ?: ""
                val authorEmail = doc.getString("authorEmail") ?: ""
                val timestamp = doc.getLong("timestamp") ?: 0L
                FeedbackPost(id, title, message, authorId, authorEmail, timestamp)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun observeFeedbackPosts(): Flow<List<FeedbackPost>> = callbackFlow {
        val registration = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val message = doc.getString("message") ?: ""
                    val authorId = doc.getString("authorId") ?: ""
                    val authorEmail = doc.getString("authorEmail") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    FeedbackPost(id, title, message, authorId, authorEmail, timestamp)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun deleteFeedbackPost(postId: String): Result<Unit> = runCatching {
        // Delete all replies under this post first, then delete the post document atomically
        val repliesSnap = postsCollection.document(postId).collection("replies").get().await()
        val batch = db.batch()
        for (doc in repliesSnap.documents) {
            batch.delete(doc.reference)
        }
        batch.delete(postsCollection.document(postId))
        batch.commit().await()
    }

    // --- Subcollection: feedback_posts/{postId}/replies ---

    suspend fun submitPostReply(
        postId: String,
        uid: String,
        name: String,
        email: String,
        text: String
    ): Result<Unit> = runCatching {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Reply cannot be empty" }
        require(trimmed.length <= 1000) { "Reply cannot exceed 1000 characters" }

        val repliesCol = postsCollection.document(postId).collection("replies")
        val docRef = repliesCol.document()
        val data = hashMapOf(
            "id" to docRef.id,
            "postId" to postId,
            "uid" to uid,
            "name" to name,
            "email" to email,
            "text" to trimmed,
            "timestamp" to System.currentTimeMillis(),
            "read" to false
        )
        docRef.set(data).await()
    }

    suspend fun getRepliesForPost(postId: String): List<FeedbackPostReply> {
        return try {
            val snap = postsCollection.document(postId)
                .collection("replies")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                val id = doc.id
                val text = doc.getString("text") ?: return@mapNotNull null
                val uid = doc.getString("uid") ?: ""
                val name = doc.getString("name") ?: "Student"
                val email = doc.getString("email") ?: ""
                val timestamp = doc.getLong("timestamp") ?: 0L
                val read = doc.getBoolean("read") ?: false
                FeedbackPostReply(id, postId, uid, name, email, text, timestamp, read)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun markReplyAsRead(postId: String, replyId: String): Result<Unit> = runCatching {
        postsCollection.document(postId).collection("replies").document(replyId)
            .update("read", true).await()
    }

    suspend fun deleteSingleReply(postId: String, replyId: String): Result<Unit> = runCatching {
        postsCollection.document(postId).collection("replies").document(replyId)
            .delete().await()
    }
}

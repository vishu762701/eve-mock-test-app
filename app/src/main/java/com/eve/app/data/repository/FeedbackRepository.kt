package com.eve.app.data.repository

import com.eve.app.data.model.FeedbackMessage
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.model.FeedbackPostReply
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for student feedback messages, admin feedback posts,
 * and per-post replies via Cloudflare Worker API.
 */
class FeedbackRepository(
    private val api: EveApiService = ApiClient.apiService
) {

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

        val body = mutableMapOf<String, Any>(
            "message" to trimmed,
            "userName" to userName,
            "userEmail" to userEmail
        )
        if (!postId.isNullOrEmpty()) body["postId"] = postId
        if (!postTitle.isNullOrEmpty()) body["postTitle"] = postTitle

        val res = api.sendFeedbackMessage(body)
        if (!res.success) throw Exception(res.error ?: "Failed to send feedback")
    }

    suspend fun getFeedbackMessages(): List<FeedbackMessage> {
        val res = api.getFeedbackMessages()
        return res.data ?: emptyList()
    }

    suspend fun markAsRead(id: String): Result<Unit> = runCatching {
        val res = api.markFeedbackRead(id)
        if (!res.success) throw Exception(res.error ?: "Failed to mark feedback as read")
    }

    suspend fun deleteFeedbackMessage(id: String): Result<Unit> = runCatching {
        val res = api.deleteFeedbackMessage(id)
        if (!res.success) throw Exception(res.error ?: "Failed to delete feedback message")
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

        val body = mapOf(
            "title" to trimmedTitle,
            "message" to trimmedMessage
        )
        val res = api.createFeedbackPost(body)
        if (!res.success || res.data == null) {
            throw Exception(res.error ?: "Failed to create feedback post")
        }
        res.data["id"] ?: ""
    }

    suspend fun getFeedbackPosts(): List<FeedbackPost> {
        return try {
            val res = api.getFeedbackPosts()
            res.data ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun observeFeedbackPosts(): Flow<List<FeedbackPost>> = flow {
        try {
            val res = api.getFeedbackPosts()
            if (res.success && res.data != null) {
                emit(res.data)
            } else {
                emit(emptyList())
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    suspend fun deleteFeedbackPost(postId: String): Result<Unit> = runCatching {
        val res = api.deleteFeedbackPost(postId)
        if (!res.success) throw Exception(res.error ?: "Failed to delete feedback post")
    }

    // --- Subcollection / child entity: feedback_posts/{postId}/replies ---

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

        val body = mapOf(
            "text" to trimmed,
            "name" to name,
            "email" to email
        )
        val res = api.submitPostReply(postId, body)
        if (!res.success) throw Exception(res.error ?: "Failed to submit reply")
    }

    suspend fun getRepliesForPost(postId: String): List<FeedbackPostReply> {
        return try {
            val res = api.getPostReplies(postId)
            res.data ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun markReplyAsRead(postId: String, replyId: String): Result<Unit> = runCatching {
        val res = api.markReplyRead(postId, replyId)
        if (!res.success) throw Exception(res.error ?: "Failed to mark reply as read")
    }

    suspend fun deleteSingleReply(postId: String, replyId: String): Result<Unit> = runCatching {
        val res = api.deleteReply(postId, replyId)
        if (!res.success) throw Exception(res.error ?: "Failed to delete reply")
    }
}

package com.eve.app.data.repository

import com.eve.app.data.model.FeedbackMessage
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.model.FeedbackPostReply
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.CancellationException
import com.eve.app.data.remote.toApiError
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
    ): Result<Unit> = requestResult {
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
        check(res.success && res.data != null) { res.error ?: "Unable to load feedback messages." }
        return res.data
    }

    suspend fun markAsRead(id: String): Result<Unit> = requestResult {
        val res = api.markFeedbackRead(id)
        if (!res.success) throw Exception(res.error ?: "Failed to mark feedback as read")
    }

    suspend fun deleteFeedbackMessage(id: String): Result<Unit> = requestResult {
        val res = api.deleteFeedbackMessage(id)
        if (!res.success) throw Exception(res.error ?: "Failed to delete feedback message")
    }

    suspend fun replyFeedbackMessage(id: String, replyText: String): Result<Unit> = requestResult {
        val trimmed = replyText.trim()
        require(trimmed.isNotEmpty()) { "Reply cannot be empty" }
        require(trimmed.length <= 1000) { "Reply cannot exceed 1000 characters" }
        val res = api.replyFeedbackMessage(id, mapOf("reply" to trimmed))
        if (!res.success) throw Exception(res.error ?: "Failed to send reply")
    }

    // --- Admin Feedback Posts ---

    suspend fun createFeedbackPost(
        title: String,
        message: String,
        authorId: String,
        authorEmail: String
    ): Result<String> = requestResult {
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
        val res = api.getFeedbackPosts()
        check(res.success && res.data != null) { res.error ?: "Unable to load feedback posts." }
        return res.data
    }

    fun observeFeedbackPosts(): Flow<List<FeedbackPost>> = flow { emit(getFeedbackPosts()) }

    suspend fun updateFeedbackPost(postId: String, title: String, message: String): Result<Unit> = requestResult {
        val trimmedTitle = title.trim()
        val trimmedMessage = message.trim()
        require(trimmedTitle.isNotEmpty()) { "Title cannot be empty" }
        require(trimmedMessage.isNotEmpty()) { "Message cannot be empty" }

        val body = mapOf(
            "title" to trimmedTitle,
            "message" to trimmedMessage
        )
        val res = api.updateFeedbackPost(postId, body)
        if (!res.success) throw Exception(res.error ?: "Failed to update feedback post")
    }

    suspend fun deleteFeedbackPost(postId: String): Result<Unit> = requestResult {
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
    ): Result<Unit> = requestResult {
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
        val res = api.getPostReplies(postId)
        check(res.success && res.data != null) { res.error ?: "Unable to load replies." }
        return res.data
    }

    suspend fun markReplyAsRead(postId: String, replyId: String): Result<Unit> = requestResult {
        val res = api.markReplyRead(postId, replyId)
        if (!res.success) throw Exception(res.error ?: "Failed to mark reply as read")
    }

    suspend fun deleteSingleReply(postId: String, replyId: String): Result<Unit> = requestResult {
        val res = api.deleteReply(postId, replyId)
        if (!res.success) throw Exception(res.error ?: "Failed to delete reply")
    }
    private suspend fun <T> requestResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { Result.failure(e.toApiError()) }
}

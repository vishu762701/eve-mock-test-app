package com.eve.app.data.repository

import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import com.eve.app.util.SubmissionRetryPolicy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Repository to manage bookmarked questions per user via Cloudflare Worker API.
 */
class BookmarkRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    // This repository instance owns one original Firebase session. Its StateFlows
    // cannot be adopted by another signed-in account after an Activity transition.
    private val initialOwnerUid = FirebaseAuth.getInstance().currentUser?.uid
    private fun accepts(userId: String): Boolean =
        userId == initialOwnerUid && SubmissionRetryPolicy.ownsSubmission(
            userId, FirebaseAuth.getInstance().currentUser?.uid)

    private val _bookmarkIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val _bookmarksFlow = MutableStateFlow<List<BookmarkedQuestion>>(emptyList())

    fun getStableId(question: Question, questionNumber: Int = 1): String {
        if (question.id.isNotBlank()) return question.id
        val safeExam = question.examId.ifBlank { "general" }
        val safeText = question.questionText.trim().take(30).replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "${safeExam}_q${questionNumber}_${safeText}".trim('_')
    }

    fun observeBookmarkIds(userId: String): Flow<Set<String>> = flow {
        if (!accepts(userId)) {
            emit(emptySet())
            return@flow
        }
        try {
            val res = api.getBookmarkIds()
            if (!accepts(userId)) return@flow
            if (res.success && res.data != null) {
                _bookmarkIdsFlow.value = res.data.toSet()
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        }
        if (accepts(userId)) emitAll(_bookmarkIdsFlow)
    }

    fun observeBookmarkedQuestions(userId: String): Flow<List<BookmarkedQuestion>> = flow {
        if (!accepts(userId)) {
            emit(emptyList())
            return@flow
        }
        try {
            val res = api.getBookmarks()
            if (!accepts(userId)) return@flow
            if (res.success && res.data != null) {
                _bookmarksFlow.value = res.data
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {
        }
        if (accepts(userId)) emitAll(_bookmarksFlow)
    }

    suspend fun bookmarkQuestion(
        userId: String,
        question: Question,
        examName: String,
        questionNumber: Int
    ): Result<Unit> {
        if (!accepts(userId)) return Result.failure(IllegalStateException("Bookmark account changed"))
        return try {
        val qId = getStableId(question, questionNumber)
        val body = mapOf(
            "questionId" to qId,
            "examId" to question.examId,
            "examName" to examName,
            "questionNumber" to questionNumber,
            "questionText" to question.questionText,
            "questionTextHi" to question.questionTextHi,
            "optionA" to question.optionA,
            "optionB" to question.optionB,
            "optionC" to question.optionC,
            "optionD" to question.optionD,
            "optionAHi" to question.optionAHi,
            "optionBHi" to question.optionBHi,
            "optionCHi" to question.optionCHi,
            "optionDHi" to question.optionDHi,
            "correctAnswer" to question.correctAnswer,
            "explanation" to question.explanation,
            "explanationHi" to question.explanationHi,
            "topic" to question.topic,
            "isPyq" to question.isPyq,
            "pyqYear" to question.pyqYear,
            "pyqPaper" to question.pyqPaper
        )

        val res = api.addBookmark(body)
        if (!accepts(userId)) throw IllegalStateException("Bookmark account changed")
        if (!res.success) throw Exception(res.error ?: "Failed to bookmark")
        _bookmarkIdsFlow.value = _bookmarkIdsFlow.value + qId
        try {
            val updated = api.getBookmarks()
            if (accepts(userId) && updated.success && updated.data != null) {
                _bookmarksFlow.value = updated.data
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (_: Exception) {}
        Result.success(Unit)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unbookmarkQuestion(userId: String, questionId: String): Result<Unit> {
        if (!accepts(userId)) return Result.failure(IllegalStateException("Bookmark account changed"))
        if (questionId.isBlank()) return Result.success(Unit)
        return try {
            val res = api.deleteBookmark(questionId)
            if (!accepts(userId)) throw IllegalStateException("Bookmark account changed")
            if (!res.success) throw Exception(res.error ?: "Failed to unbookmark")
            _bookmarkIdsFlow.value = _bookmarkIdsFlow.value - questionId
            _bookmarksFlow.value = _bookmarksFlow.value.filter { it.questionId != questionId }
            Result.success(Unit)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleBookmark(
        userId: String,
        question: Question,
        examName: String,
        questionNumber: Int,
        currentlyBookmarked: Boolean
    ): Result<Unit> {
        val qId = getStableId(question, questionNumber)
        return if (currentlyBookmarked) {
            unbookmarkQuestion(userId, qId)
        } else {
            bookmarkQuestion(userId, question, examName, questionNumber)
        }
    }
}

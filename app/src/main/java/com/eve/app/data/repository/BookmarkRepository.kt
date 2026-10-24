package com.eve.app.data.repository

import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.data.model.Question
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
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

    private val _bookmarkIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    private val _bookmarksFlow = MutableStateFlow<List<BookmarkedQuestion>>(emptyList())

    fun getStableId(question: Question, questionNumber: Int = 1): String {
        if (question.id.isNotBlank()) return question.id
        val safeExam = question.examId.ifBlank { "general" }
        val safeText = question.questionText.trim().take(30).replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "${safeExam}_q${questionNumber}_${safeText}".trim('_')
    }

    fun observeBookmarkIds(userId: String): Flow<Set<String>> = flow {
        if (userId.isBlank()) {
            emit(emptySet())
            return@flow
        }
        try {
            val res = api.getBookmarkIds()
            if (res.success && res.data != null) {
                _bookmarkIdsFlow.value = res.data.toSet()
            }
        } catch (_: Exception) {
        }
        emitAll(_bookmarkIdsFlow)
    }

    fun observeBookmarkedQuestions(userId: String): Flow<List<BookmarkedQuestion>> = flow {
        if (userId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        try {
            val res = api.getBookmarks()
            if (res.success && res.data != null) {
                _bookmarksFlow.value = res.data
            }
        } catch (_: Exception) {
        }
        emitAll(_bookmarksFlow)
    }

    suspend fun bookmarkQuestion(
        userId: String,
        question: Question,
        examName: String,
        questionNumber: Int
    ): Result<Unit> = runCatching {
        if (userId.isBlank()) return@runCatching
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
        if (!res.success) throw Exception(res.error ?: "Failed to bookmark")
        _bookmarkIdsFlow.value = _bookmarkIdsFlow.value + qId
        try {
            val updated = api.getBookmarks()
            if (updated.success && updated.data != null) {
                _bookmarksFlow.value = updated.data
            }
        } catch (_: Exception) {}
    }

    suspend fun unbookmarkQuestion(userId: String, questionId: String): Result<Unit> = runCatching {
        if (userId.isBlank() || questionId.isBlank()) return@runCatching
        val res = api.deleteBookmark(questionId)
        if (!res.success) throw Exception(res.error ?: "Failed to unbookmark")
        _bookmarkIdsFlow.value = _bookmarkIdsFlow.value - questionId
        _bookmarksFlow.value = _bookmarksFlow.value.filter { it.questionId != questionId }
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

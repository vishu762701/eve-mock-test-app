package com.eve.app.data.repository

import com.eve.app.data.model.BookmarkedQuestion
import com.eve.app.data.model.Question
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository to manage bookmarked questions per user in Firestore.
 * Path: users/{userId}/bookmarks/{questionId}
 */
class BookmarkRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getStableId(question: Question, questionNumber: Int = 1): String {
        if (question.id.isNotBlank()) return question.id
        val safeExam = question.examId.ifBlank { "general" }
        val safeText = question.questionText.trim().take(30).replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "${safeExam}_q${questionNumber}_${safeText}".trim('_')
    }

    fun observeBookmarkIds(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userId)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptySet())
                    return@addSnapshotListener
                }
                val set = snapshot?.documents?.map { it.id }?.toSet().orEmpty()
                trySend(set)
            }

        awaitClose { listener.remove() }
    }

    fun observeBookmarkedQuestions(userId: String): Flow<List<BookmarkedQuestion>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("users")
            .document(userId)
            .collection("bookmarks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BookmarkedQuestion::class.java)?.copy(questionId = doc.id)
                }?.sortedByDescending { it.bookmarkedAt }.orEmpty()
                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    suspend fun bookmarkQuestion(
        userId: String,
        question: Question,
        examName: String,
        questionNumber: Int
    ): Result<Unit> = runCatching {
        if (userId.isBlank()) return@runCatching
        val qId = getStableId(question, questionNumber)
        val bookmark = BookmarkedQuestion(
            questionId = qId,
            examId = question.examId,
            examName = examName,
            questionNumber = questionNumber,
            questionText = question.questionText,
            questionTextHi = question.questionTextHi,
            optionA = question.optionA,
            optionB = question.optionB,
            optionC = question.optionC,
            optionD = question.optionD,
            optionAHi = question.optionAHi,
            optionBHi = question.optionBHi,
            optionCHi = question.optionCHi,
            optionDHi = question.optionDHi,
            correctAnswer = question.correctAnswer,
            explanation = question.explanation,
            explanationHi = question.explanationHi,
            topic = question.topic,
            isPyq = question.isPyq,
            pyqYear = question.pyqYear,
            pyqPaper = question.pyqPaper,
            bookmarkedAt = System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("bookmarks")
            .document(qId)
            .set(bookmark)
            .await()
    }

    suspend fun unbookmarkQuestion(userId: String, questionId: String): Result<Unit> = runCatching {
        if (userId.isBlank() || questionId.isBlank()) return@runCatching
        db.collection("users")
            .document(userId)
            .collection("bookmarks")
            .document(questionId)
            .delete()
            .await()
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

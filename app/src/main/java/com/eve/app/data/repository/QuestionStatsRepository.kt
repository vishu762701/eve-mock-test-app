package com.eve.app.data.repository

import com.eve.app.data.model.QuestionStat
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class QuestionStatsRepository {

    private val firestore get() = FirebaseFirestore.getInstance()

    suspend fun getQuestionStat(questionId: String): QuestionStat? {
        if (questionId.isBlank()) return null
        return try {
            val doc = firestore.collection("question_stats").document(questionId).get().await()
            if (!doc.exists()) return null
            val qId = doc.getString("questionId") ?: questionId
            val eId = doc.getString("examId").orEmpty()
            val total = doc.getLong("totalAttempts") ?: 0L
            val correct = doc.getLong("correctAttempts") ?: 0L
            val time = doc.getLong("totalTimeSeconds") ?: 0L
            val avg = doc.getDouble("avgTimeSeconds") ?: (if (total > 0) time.toDouble() / total else 0.0)
            QuestionStat(qId, eId, total, correct, time, avg)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getQuestionStatsForExam(examId: String): Map<String, QuestionStat> {
        if (examId.isBlank()) return emptyMap()
        return try {
            val snap = firestore.collection("question_stats")
                .whereEqualTo("examId", examId)
                .get()
                .await()
            val map = mutableMapOf<String, QuestionStat>()
            for (doc in snap.documents) {
                val qId = doc.getString("questionId") ?: doc.id
                val eId = doc.getString("examId").orEmpty()
                val total = doc.getLong("totalAttempts") ?: 0L
                val correct = doc.getLong("correctAttempts") ?: 0L
                val time = doc.getLong("totalTimeSeconds") ?: 0L
                val avg = doc.getDouble("avgTimeSeconds") ?: (if (total > 0) time.toDouble() / total else 0.0)
                map[qId] = QuestionStat(qId, eId, total, correct, time, avg)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun getQuestionStats(questionIds: List<String>): Map<String, QuestionStat> {
        val nonBlank = questionIds.filter { it.isNotBlank() }.distinct()
        if (nonBlank.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, QuestionStat>()
        try {
            // Chunk by 30 for whereIn if needed, or query documents directly
            val chunks = nonBlank.chunked(30)
            for (chunk in chunks) {
                val snap = firestore.collection("question_stats")
                    .whereIn("questionId", chunk)
                    .get()
                    .await()
                for (doc in snap.documents) {
                    val qId = doc.getString("questionId") ?: doc.id
                    val eId = doc.getString("examId").orEmpty()
                    val total = doc.getLong("totalAttempts") ?: 0L
                    val correct = doc.getLong("correctAttempts") ?: 0L
                    val time = doc.getLong("totalTimeSeconds") ?: 0L
                    val avg = doc.getDouble("avgTimeSeconds") ?: (if (total > 0) time.toDouble() / total else 0.0)
                    result[qId] = QuestionStat(qId, eId, total, correct, time, avg)
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    fun recordQuestionAttempt(questionId: String, examId: String, isCorrect: Boolean, timeSeconds: Long) {
        if (questionId.isBlank()) return
        try {
            val docRef = firestore.collection("question_stats").document(questionId)
            val updates = hashMapOf<String, Any>(
                "questionId" to questionId,
                "examId" to examId,
                "totalAttempts" to FieldValue.increment(1),
                "correctAttempts" to FieldValue.increment(if (isCorrect) 1 else 0),
                "totalTimeSeconds" to FieldValue.increment(timeSeconds),
                "lastUpdatedAt" to System.currentTimeMillis()
            )
            docRef.set(updates, SetOptions.merge())
        } catch (_: Exception) {}
    }
}

package com.eve.app.data.repository

import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.model.FlaggedQuestion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FlaggedQuestionRepository {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()

    /** Student action: Flag / report a questionable question */
    suspend fun flagQuestion(
        questionId: String,
        examId: String,
        examName: String,
        questionText: String,
        reason: String,
        comment: String
    ): Boolean {
        return try {
            val user = auth.currentUser
            val doc = hashMapOf(
                "questionId" to questionId,
                "examId" to examId,
                "examName" to examName,
                "questionText" to questionText,
                "reason" to reason,
                "comment" to comment,
                "studentId" to (user?.uid ?: "anonymous"),
                "studentEmail" to (user?.email ?: "Anonymous"),
                "timestamp" to System.currentTimeMillis(),
                "status" to FlaggedQuestion.STATUS_PENDING
            )
            firestore.collection("flagged_questions").add(doc).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Admin action: Fetch pending flagged questions grouped by questionId */
    suspend fun getPendingFlaggedQuestions(): List<AggregatedFlaggedQuestion> {
        return try {
            val snap = firestore.collection("flagged_questions")
                .whereEqualTo("status", FlaggedQuestion.STATUS_PENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val rawList = snap.documents.mapNotNull { doc ->
                val id = doc.id
                val qId = doc.getString("questionId").orEmpty()
                val eId = doc.getString("examId").orEmpty()
                val eName = doc.getString("examName").orEmpty()
                val qText = doc.getString("questionText").orEmpty()
                val reason = doc.getString("reason").orEmpty()
                val comment = doc.getString("comment").orEmpty()
                val sId = doc.getString("studentId").orEmpty()
                val sEmail = doc.getString("studentEmail").orEmpty()
                val time = doc.getLong("timestamp") ?: 0L
                val status = doc.getString("status") ?: FlaggedQuestion.STATUS_PENDING

                FlaggedQuestion(
                    id = id,
                    questionId = qId,
                    examId = eId,
                    examName = eName,
                    questionText = qText,
                    reason = reason,
                    comment = comment,
                    studentId = sId,
                    studentEmail = sEmail,
                    timestamp = time,
                    status = status
                )
            }

            // Group by questionId or questionText
            val grouped = rawList.groupBy { if (it.questionId.isNotBlank()) it.questionId else it.questionText }
            grouped.map { (_, flags) ->
                val first = flags.first()
                val allReasons = flags.map { it.reason }.filter { it.isNotBlank() }.distinct()
                val allComments = flags.map { it.comment }.filter { it.isNotBlank() }
                val allFlagIds = flags.map { it.id }
                val maxTime = flags.maxOf { it.timestamp }

                AggregatedFlaggedQuestion(
                    questionId = first.questionId,
                    examId = first.examId,
                    examName = first.examName,
                    questionText = first.questionText,
                    flagCount = flags.size,
                    reasons = allReasons,
                    comments = allComments,
                    flagIds = allFlagIds,
                    latestTimestamp = maxTime
                )
            }.sortedByDescending { it.flagCount }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** Admin action: Dismiss flags for this question */
    suspend fun dismissFlags(flagIds: List<String>): Boolean {
        return try {
            val batch = firestore.batch()
            for (id in flagIds) {
                val ref = firestore.collection("flagged_questions").document(id)
                batch.update(ref, "status", FlaggedQuestion.STATUS_DISMISSED)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

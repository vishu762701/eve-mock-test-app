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

    /** Student action: Flag / report a questionable question or technical bug */
    suspend fun flagQuestion(
        questionId: String,
        examId: String,
        examName: String,
        questionText: String,
        reason: String,
        comment: String
    ): Boolean = flagQuestionResult(questionId, examId, examName, questionText, reason, comment).isSuccess

    suspend fun flagQuestionResult(
        questionId: String,
        examId: String,
        examName: String,
        questionText: String,
        reason: String,
        comment: String
    ): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!FlaggedQuestion.isCommentValid(comment)) {
            return@withContext Result.failure(IllegalArgumentException("Please enter at least 7 words or 40 characters explaining the issue."))
        }
        try {
            var user = auth.currentUser
            if (user == null) {
                try {
                    user = auth.signInAnonymously().await().user
                } catch (_: Exception) {
                    // Fall back to writing anonymously
                }
            }
            val isContent = FlaggedQuestion.isContentIssue(reason)
            val collectionName = if (isContent) "flagged_questions" else "reported_bugs"
            val reportType = if (isContent) FlaggedQuestion.TYPE_CONTENT else FlaggedQuestion.TYPE_TECHNICAL

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
                "status" to FlaggedQuestion.STATUS_PENDING,
                "reportType" to reportType
            )
            firestore.collection(collectionName).add(doc).await()
            android.util.Log.d("FlaggedRepo", "Successfully submitted report to $collectionName for q=$questionId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FlaggedRepo", "Failed to submit report for questionId=$questionId", e)
            Result.failure(e)
        }
    }

    /** Admin action: Fetch pending reports grouped by questionId, separated by tab */
    suspend fun getPendingFlaggedQuestions(isTechnical: Boolean = false): List<AggregatedFlaggedQuestion> {
        return try {
            val collectionName = if (isTechnical) "reported_bugs" else "flagged_questions"
            val snap = firestore.collection(collectionName)
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
                val reportType = doc.getString("reportType") ?: if (isTechnical) FlaggedQuestion.TYPE_TECHNICAL else FlaggedQuestion.TYPE_CONTENT

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
                    status = status,
                    reportType = reportType
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
                    latestTimestamp = maxTime,
                    reportType = first.reportType
                )
            }.sortedByDescending { it.flagCount }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** Admin action: Dismiss flags for this question or bug */
    suspend fun dismissFlags(flagIds: List<String>, isTechnical: Boolean = false): Boolean {
        return try {
            val collectionName = if (isTechnical) "reported_bugs" else "flagged_questions"
            val batch = firestore.batch()
            for (id in flagIds) {
                val ref = firestore.collection(collectionName).document(id)
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

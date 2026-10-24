package com.eve.app.data.repository

import android.util.Log
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.model.FlaggedQuestion
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.remote.SubmitReportRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlaggedQuestionRepository(
    private val api: EveApiService = ApiClient.api
) {

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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.submitReport(
                SubmitReportRequest(
                    questionId = questionId,
                    examId = examId,
                    examName = examName,
                    questionText = questionText,
                    reason = reason,
                    comment = comment
                )
            )
            if (response.success) {
                Log.d("FlaggedRepo", "Successfully submitted report for q=$questionId")
                Result.success(Unit)
            } else {
                val err = response.error ?: "Failed to submit report"
                Log.e("FlaggedRepo", "Failed to submit report for q=$questionId: $err")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e("FlaggedRepo", "Exception submitting report for questionId=$questionId", e)
            Result.failure(e)
        }
    }

    /** Admin action: Fetch pending reports grouped by questionId, separated by tab */
    suspend fun getPendingFlaggedQuestions(isTechnical: Boolean = false): List<AggregatedFlaggedQuestion> = withContext(Dispatchers.IO) {
        try {
            val typeParam = if (isTechnical) "technical" else "content"
            val response = api.getAdminReports(type = typeParam)
            if (!response.success || response.data == null) {
                return@withContext emptyList()
            }

            val rawList = response.data.map { dto ->
                FlaggedQuestion(
                    id = dto.id,
                    questionId = dto.questionId,
                    examId = dto.examId,
                    examName = dto.examName,
                    questionText = dto.questionText,
                    reason = dto.reason,
                    comment = dto.comment,
                    studentId = dto.studentId,
                    studentEmail = dto.studentEmail,
                    timestamp = dto.timestamp,
                    status = dto.status,
                    reportType = dto.reportType
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
            Log.e("FlaggedRepo", "Error fetching pending flagged questions", e)
            emptyList()
        }
    }

    /** Admin action: Dismiss flags for this question or bug */
    suspend fun dismissFlags(flagIds: List<String>, isTechnical: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (flagIds.isEmpty()) return@withContext true
        try {
            val response = api.dismissReportBatch(mapOf("ids" to flagIds))
            response.success
        } catch (e: Exception) {
            Log.e("FlaggedRepo", "Error dismissing flags: $flagIds", e)
            false
        }
    }

    /** Admin action: Delete a single report */
    suspend fun deleteReport(reportId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteReport(reportId)
            response.success
        } catch (e: Exception) {
            Log.e("FlaggedRepo", "Error deleting report: $reportId", e)
            false
        }
    }
}

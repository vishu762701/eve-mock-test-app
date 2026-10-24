package com.eve.app.data.repository

import android.util.Log
import com.eve.app.data.model.AnswerItem
import com.eve.app.data.model.TestAttempt
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HistoryRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    private val submitScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun submitAttemptSync(
        examId: String,
        examName: String,
        category: String,
        displayName: String,
        items: List<AnswerItem>,
        clientAttemptId: String = java.util.UUID.randomUUID().toString(),
        ownerUid: String = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    ): Boolean {
        check(com.eve.app.util.SubmissionRetryPolicy.ownsSubmission(ownerUid, com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid)) { "Sign in with the account that started this test" }
        val payload = mapOf(
            "examId" to examId,
            "examName" to examName,
            "category" to category,
            "displayName" to displayName,
            "clientAttemptId" to clientAttemptId,
            "expectedUid" to ownerUid,
            "answers" to items.map { a ->
                mapOf(
                    "questionId" to a.questionId,
                    "number" to a.number,
                    "selected" to a.selected,
                    "isBookmarked" to a.isBookmarked
                )
            }
        )
        val res = api.submitAttempt(payload)
        if (res.success && res.data != null) {
            try {
                ApiUsageRepository().incrementTestSubmissions()
            } catch (_: Exception) {
                // optional: failure is fine
            }
            return true
        } else {
            throw Exception(res.error ?: "Submission rejected by server")
        }
    }

    fun saveAttempt(
        examId: String,
        examName: String,
        category: String,
        displayName: String,
        items: List<AnswerItem>
    ) {
        val clientAttemptId = java.util.UUID.randomUUID().toString()
        val ownerUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        submitScope.launch {
            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                try {
                    val ok = submitAttemptSync(examId, examName, category, displayName, items, clientAttemptId, ownerUid)
                    if (ok) return@launch
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // optional: retry on failure
                }
                if (attempt < maxAttempts) delay(2000L * attempt)
            }
        }
    }

    /** Backward-compatible attempt check via Cloudflare Worker lock endpoint. */
    suspend fun hasAttempted(userId: String, examId: String): Boolean {
        if (examId.isBlank()) return false
        val res = api.checkAttemptLock(examId)
        if (!res.success) {
            throw Exception(res.error ?: "Failed to check attempt lock")
        }
        return res.data?.hasLock == true
    }

    /** List attempts in reverse chronological order. */
    suspend fun getAttempts(userId: String): List<TestAttempt> {
        val res = api.getAttempts()
        if (!res.success) {
            throw Exception(res.error ?: "Failed to fetch attempts")
        }
        return (res.data ?: emptyList()).sortedByDescending { it.timestamp }
    }

    /** Reset attempt and lock for an exam so user can reattempt. */
    suspend fun resetAttempt(examId: String): Boolean {
        if (examId.isBlank()) return false
        val res = api.resetAttempt(examId)
        if (!res.success) {
            throw Exception(res.error ?: "Failed to reset attempt")
        }
        return res.success
    }
}

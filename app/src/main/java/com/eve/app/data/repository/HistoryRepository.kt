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

    fun saveAttempt(
        examId: String,
        examName: String,
        category: String,
        displayName: String,
        items: List<AnswerItem>
    ) {
        val payload = mapOf(
            "examId" to examId,
            "examName" to examName,
            "category" to category,
            "displayName" to displayName,
            "answers" to items.map { a ->
                mapOf(
                    "questionId" to a.questionId,
                    "number" to a.number,
                    "selected" to a.selected,
                    "isBookmarked" to a.isBookmarked
                )
            }
        )

        submitScope.launch {
            val maxAttempts = 3
            for (attempt in 1..maxAttempts) {
                try {
                    val res = api.submitAttempt(payload)
                    if (res.success) return@launch
                    Log.w("HistoryRepository", "submitAttempt try $attempt/$maxAttempts failed: ${res.error}")
                } catch (e: Exception) {
                    Log.w("HistoryRepository", "submitAttempt try $attempt/$maxAttempts exception", e)
                }
                if (attempt < maxAttempts) delay(2000L * attempt)
            }
        }
    }

    /** Backward-compatible attempt check via Cloudflare Worker lock endpoint. */
    suspend fun hasAttempted(userId: String, examId: String): Boolean {
        if (examId.isBlank()) return false
        return try {
            val res = api.checkAttemptLock(examId)
            res.data?.hasLock == true
        } catch (_: Exception) {
            false
        }
    }

    /** List attempts in reverse chronological order. */
    suspend fun getAttempts(userId: String): List<TestAttempt> {
        return try {
            val res = api.getAttempts()
            (res.data ?: emptyList()).sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

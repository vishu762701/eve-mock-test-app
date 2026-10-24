package com.eve.app.data.repository

import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.data.model.RankInfo
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.remote.EveApiService

class LeaderboardRepository(
    private val api: EveApiService = ApiClient.apiService
) {

    /** Top scorers ek exam ke liye, ya overall across all exams. */
    suspend fun getTopScorers(examId: String, limit: Long = 50): List<LeaderboardEntry> {
        val targetExamId = if (examId.isBlank()) "overall" else examId
        return try {
            val res = api.getLeaderboard(targetExamId, limit.toInt())
            res.data ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Current user ka apna rank + kitne total participants hain.
     */
    suspend fun getUserRank(examId: String, userId: String): RankInfo? {
        val targetExamId = if (examId.isBlank()) "overall" else examId
        return try {
            val res = api.getUserRank(targetExamId)
            res.data
        } catch (_: Exception) {
            null
        }
    }
}

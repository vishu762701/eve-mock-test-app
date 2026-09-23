package com.eve.app.data.repository

import com.eve.app.data.model.LeaderboardEntry
import com.eve.app.data.model.RankInfo
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun collection() = db.collection("leaderboard")
    private fun overallCollection() = db.collection("overall_leaderboard")

    /** Top scorers ek exam ke liye, ya overall across all exams. */
    suspend fun getTopScorers(examId: String, limit: Long = 50): List<LeaderboardEntry> {
        return if (examId.isBlank() || examId == "overall") {
            getOverallTopScorers(limit)
        } else {
            collection()
                .whereEqualTo("examId", examId)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
                .documents.mapNotNull { parseEntry(it) }
        }
    }

    private suspend fun getOverallTopScorers(limit: Long): List<LeaderboardEntry> {
        // 1. Check overall_leaderboard collection maintained by Cloud Function
        try {
            val docs = overallCollection()
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()
                .documents
            if (docs.isNotEmpty()) {
                return docs.mapNotNull { doc ->
                    val userId = doc.getString("userId") ?: doc.id
                    val score = doc.getDouble("score") ?: doc.getDouble("totalScore") ?: 0.0
                    val total = doc.getLong("total")?.toInt() ?: 0
                    val displayName = doc.getString("displayName")?.ifBlank { "Student" } ?: "Student"
                    LeaderboardEntry(
                        userId = userId,
                        examId = "overall",
                        examName = "Overall",
                        displayName = displayName,
                        score = score,
                        total = total,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback: Aggregate from leaderboard collection if overall_leaderboard is not yet populated
        return try {
            val allLeaderboardDocs = collection().limit(150).get().await().documents
            val userEntries = mutableMapOf<String, LeaderboardEntry>()
            for (doc in allLeaderboardDocs) {
                val entry = parseEntry(doc) ?: continue
                val existing = userEntries[entry.userId]
                if (existing == null) {
                    userEntries[entry.userId] = entry.copy(examId = "overall", examName = "Overall")
                } else {
                    userEntries[entry.userId] = existing.copy(
                        score = existing.score + entry.score,
                        total = existing.total + entry.total,
                        timestamp = maxOf(existing.timestamp, entry.timestamp)
                    )
                }
            }
            userEntries.values.sortedByDescending { it.score }.take(limit.toInt())
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Current user ka apna rank + kitne total participants hain.
     */
    suspend fun getUserRank(examId: String, userId: String): RankInfo? {
        return if (examId.isBlank() || examId == "overall") {
            getOverallUserRank(userId)
        } else {
            val myDoc = collection().document("${examId}_$userId").get().await()
            if (!myDoc.exists()) return null
            val myScore = myDoc.getDouble("score") ?: return null
            val myTotal = (myDoc.getLong("total") ?: 0L).toInt()

            val higherCount = collection()
                .whereEqualTo("examId", examId)
                .whereGreaterThan("score", myScore)
                .count().get(AggregateSource.SERVER).await().count

            val totalCount = collection()
                .whereEqualTo("examId", examId)
                .count().get(AggregateSource.SERVER).await().count

            RankInfo(
                rank = higherCount.toInt() + 1,
                totalParticipants = totalCount.toInt(),
                score = myScore,
                total = myTotal
            )
        }
    }

    private suspend fun getOverallUserRank(userId: String): RankInfo? {
        try {
            val myDoc = overallCollection().document(userId).get().await()
            if (myDoc.exists()) {
                val myScore = myDoc.getDouble("score") ?: myDoc.getDouble("totalScore") ?: return null
                val myTotal = (myDoc.getLong("total") ?: 0L).toInt()

                val higherCount = overallCollection()
                    .whereGreaterThan("score", myScore)
                    .count().get(AggregateSource.SERVER).await().count

                val totalCount = overallCollection()
                    .count().get(AggregateSource.SERVER).await().count

                return RankInfo(
                    rank = higherCount.toInt() + 1,
                    totalParticipants = totalCount.toInt(),
                    score = myScore,
                    total = myTotal
                )
            }
        } catch (_: Exception) {}

        // Fallback calculation from top list
        val topList = getOverallTopScorers(100)
        val myIndex = topList.indexOfFirst { it.userId == userId }
        if (myIndex >= 0) {
            val item = topList[myIndex]
            return RankInfo(
                rank = myIndex + 1,
                totalParticipants = topList.size,
                score = item.score,
                total = item.total
            )
        }
        return null
    }

    private fun parseEntry(doc: DocumentSnapshot): LeaderboardEntry? {
        val examId = doc.getString("examId") ?: return null
        return LeaderboardEntry(
            userId = doc.getString("userId") ?: "",
            examId = examId,
            examName = doc.getString("examName") ?: "",
            displayName = doc.getString("displayName")?.ifBlank { "Student" } ?: "Student",
            score = doc.getDouble("score") ?: 0.0,
            total = (doc.getLong("total") ?: 0L).toInt(),
            timestamp = doc.getLong("timestamp") ?: 0L
        )
    }
}

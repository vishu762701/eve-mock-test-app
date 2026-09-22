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

    /** Top scorers ek exam ke liye, sabse zyada score sabse upar. */
    suspend fun getTopScorers(examId: String, limit: Long = 50): List<LeaderboardEntry> =
        collection()
            .whereEqualTo("examId", examId)
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
            .documents.mapNotNull { parseEntry(it) }

    /**
     * Current user ka apna rank + kitne total participants hain — poori list download
     * kiye bina, seedha server par count() aggregation query se (fast + cheap, chahe
     * hazaaron log attempt kar chuke hon).
     * Agar user ne abhi tak is exam ka attempt hi nahi diya (ya Cloud Function abhi tak
     * chala nahi — usually 1-2 second lagta hai), to null return hota hai.
     */
    suspend fun getUserRank(examId: String, userId: String): RankInfo? {
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

        return RankInfo(
            rank = higherCount.toInt() + 1,
            totalParticipants = totalCount.toInt(),
            score = myScore,
            total = myTotal
        )
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

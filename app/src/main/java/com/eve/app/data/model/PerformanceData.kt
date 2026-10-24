package com.eve.app.data.model

import kotlin.math.roundToInt

/** Ek topic ki accuracy — Test History ke saare attempts ke answers se aggregate hoti hai. */
data class TopicStat(
    val topic: String,
    val correct: Int,
    val total: Int
) {
    val accuracy: Int get() = if (total == 0) 0 else ((correct * 100.0) / total).roundToInt()
}

/** Score trend graph ka ek point — ek attempt ki accuracy %, chronological order me. */
data class ScorePoint(
    val timestamp: Long,
    val examName: String,
    val percent: Int
)

/** PerformanceActivity (Phase 17) ke liye poora computed snapshot — HistoryRepository ke
 * attempts se client-side nikalta hai, koi naya Firestore collection nahi chahiye. */
data class PerformanceData(
    val totalAttempts: Int,
    val totalQuestions: Int,
    val overallAccuracy: Int,
    /** Oldest se newest, purane attempts drop kiye ja sakte hain (last N) agar bahut zyada ho. */
    val scoreTrend: List<ScorePoint>,
    /** Sabse kam accuracy wale topic sabse upar. */
    val topicStats: List<TopicStat>
) {
    /** Kam se kam 2 questions wale topics ko priority — 1 question wale topic noisy hote hain.
     * Koi bhi topic 2+ questions wala na ho to fallback pure list par hi kar dete hain. */
    val weakTopics: List<TopicStat>
        get() {
            val reliable = topicStats.filter { it.total >= 2 }
            return (reliable.ifEmpty { topicStats }).sortedBy { it.accuracy }.take(3)
        }

    companion object {
        val EMPTY = PerformanceData(0, 0, 0, emptyList(), emptyList())
    }
}

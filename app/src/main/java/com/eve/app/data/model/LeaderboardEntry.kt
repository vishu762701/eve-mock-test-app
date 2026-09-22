package com.eve.app.data.model

/**
 * Firestore collection: leaderboard. Ek document = ek user ka ek exam ka BEST score
 * (doc ID = "{examId}_{userId}" isliye ek user ka ek exam me sirf ek hi entry rehti hai).
 *
 * Yeh collection client se seedha kabhi likha nahi jaata — jab bhi "attempts" me naya
 * document create hota hai, ek Cloud Function (functions/index.js -> updateLeaderboard)
 * automatically yahan best-score entry upsert kar deta hai. Isse do cheezein solve hoti
 * hain: (1) har user sirf apni khud ki full attempt (saare answers) read kar sakta hai
 * (Phase 13 rules), par leaderboard ki summary sabko dikhti hai; (2) koi client-side cheating
 * (khud ka fake score likh dena) possible nahi hai kyunki likhne wala sirf server-side
 * Cloud Function hai.
 */
data class LeaderboardEntry(
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val displayName: String = "",
    val score: Double = 0.0,
    val total: Int = 0,
    val timestamp: Long = 0L
) {
    val scoreText: String get() = if (score % 1.0 == 0.0) score.toInt().toString() else String.format("%.2f", score)

    val initial: String get() = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

/**
 * Current user ka rank ek exam ke andar — poori list download kiye bina nikala jaata hai,
 * Firestore ki count() aggregation query se (dekho LeaderboardRepository.getUserRank).
 */
data class RankInfo(
    val rank: Int,
    val totalParticipants: Int,
    val score: Double,
    val total: Int
) {
    val scoreText: String get() = if (score % 1.0 == 0.0) score.toInt().toString() else String.format("%.2f", score)

    /** "Tum top X% me ho" wala number — jitna kam utna behtar (top 1% = best). */
    val topPercent: Int get() {
        if (totalParticipants <= 1) return 1
        val percent = (rank * 100.0) / totalParticipants
        return percent.toInt().coerceIn(1, 100)
    }
}

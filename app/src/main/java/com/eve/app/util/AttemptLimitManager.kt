package com.eve.app.util

import android.content.Context
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.withTimeoutOrNull

object AttemptLimitManager {

    const val MAX_ATTEMPTS = 3
    private const val PREFS_NAME = "eve_attempt_limits"
    private const val KEY_PREFIX = "attempt_count_"

    /**
     * Checks if given user email is an admin (hardcoded or dynamic).
     */
    suspend fun isAdmin(email: String?, context: Context? = null): Boolean {
        if (email.isNullOrBlank()) return false
        if (Constants.ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) }) return true
        if (context != null && !NetworkUtil.isOnline(context)) return false
        return try {
            withTimeoutOrNull(2000L) {
                AdminRepository().isAdmin(email)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun isCurrentUserAdmin(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        return Constants.ADMIN_EMAILS.any { it.equals(user.email, ignoreCase = true) }
    }

    fun getLocalAttemptCount(context: Context, examId: String, uid: String): Int {
        if (examId.isBlank() || uid.isBlank()) return 0
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt("${KEY_PREFIX}${uid}_$examId", 0)
    }

    fun setLocalAttemptCount(context: Context, examId: String, uid: String, count: Int) {
        if (examId.isBlank() || uid.isBlank()) return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putInt("${KEY_PREFIX}${uid}_$examId", count).apply()
    }

    /**
     * Fetches current attempt count for user on the given exam.
     * Combines local preferences and existing attempts in history.
     * Guaranteed non-blocking and instant when offline.
     */
    suspend fun getAttemptCount(context: Context, examId: String): Int {
        val user = FirebaseAuth.getInstance().currentUser
        val uid = user?.uid.orEmpty()
        val localCount = getLocalAttemptCount(context, examId, uid)
        if (uid.isBlank() || examId.isBlank()) return localCount

        if (!NetworkUtil.isOnline(context)) {
            return localCount
        }

        var historyCount = 0
        try {
            withTimeoutOrNull(2000L) {
                val attempts = HistoryRepository().getAttempts(uid).filter { it.examId == examId }
                historyCount = attempts.size
            }
        } catch (_: Exception) {
            // optional: failure is fine
        }

        val highest = maxOf(localCount, historyCount)
        if (highest != localCount) {
            setLocalAttemptCount(context, examId, uid, highest)
        }
        return highest
    }

    /**
     * Checks if current user can start or reattempt the given exam.
     * Admins have unlimited attempts. Normal users are capped at MAX_ATTEMPTS.
     */
    suspend fun canAttempt(context: Context, examId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return true
        if (isAdmin(user.email, context)) return true
        if (com.eve.app.data.repository.PremiumRepository.isCurrentUserPremium(context)) return true
        val count = getAttemptCount(context, examId)
        return count < MAX_ATTEMPTS
    }

    fun canAttemptLocal(context: Context, examId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return true
        if (isCurrentUserAdmin()) return true
        if (com.eve.app.data.repository.PremiumRepository.isCurrentUserPremium(context)) return true
        val count = getLocalAttemptCount(context, examId, user.uid)
        return count < MAX_ATTEMPTS
    }

    /**
     * Increments the attempt count for this exam in local preferences as an offline hint.
     */
    suspend fun recordAttempt(context: Context, examId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        if (examId.isBlank() || uid.isBlank()) return

        val currentCount = getLocalAttemptCount(context, examId, uid)
        val newCount = currentCount + 1
        setLocalAttemptCount(context, examId, uid, newCount)
    }
}

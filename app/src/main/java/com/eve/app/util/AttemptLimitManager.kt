package com.eve.app.util

import android.content.Context
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.HistoryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
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
     * Combines local preferences, Firestore user doc, and existing attempts in history.
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

        var remoteCount = 0
        try {
            withTimeoutOrNull(2000L) {
                val userDoc = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()
                val counts = userDoc.get("attemptCounts") as? Map<*, *>
                remoteCount = (counts?.get(examId) as? Number)?.toInt() ?: 0
            }
        } catch (_: Exception) {}

        var historyCount = 0
        try {
            withTimeoutOrNull(2000L) {
                val attempts = HistoryRepository().getAttempts(uid).filter { it.examId == examId }
                historyCount = attempts.size
            }
        } catch (_: Exception) {}

        val highest = maxOf(localCount, remoteCount, historyCount)
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
        val count = getAttemptCount(context, examId)
        return count < MAX_ATTEMPTS
    }

    fun canAttemptLocal(context: Context, examId: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return true
        if (isCurrentUserAdmin()) return true
        val count = getLocalAttemptCount(context, examId, user.uid)
        return count < MAX_ATTEMPTS
    }

    /**
     * Increments the attempt count for this exam.
     */
    suspend fun recordAttempt(context: Context, examId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        if (examId.isBlank() || uid.isBlank()) return

        val currentCount = getLocalAttemptCount(context, examId, uid)
        val newCount = currentCount + 1
        setLocalAttemptCount(context, examId, uid, newCount)

        if (NetworkUtil.isOnline(context)) {
            try {
                withTimeoutOrNull(2500L) {
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    userRef.set(
                        mapOf("attemptCounts" to mapOf(examId to FieldValue.increment(1))),
                        SetOptions.merge()
                    ).await()
                }
            } catch (_: Exception) {}
        }
    }
}

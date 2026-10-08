package com.eve.app.data.local

import android.content.Context
import com.google.gson.Gson

data class TestSession(
    val attemptKey: String,
    val clientAttemptId: String = "",
    val userId: String = "",
    val answers: Map<String, String> = emptyMap(), // questionId -> selected ("A"/"B"/"C"/"D")
    val questionTimes: Map<String, Long> = emptyMap(), // questionId -> seconds spent
    val visitedQuestions: Set<String> = emptySet(),
    val markedQuestions: Set<String> = emptySet(),
    val elapsedSeconds: Long = 0L,
    val timeLimitSeconds: Long = 0L,
    val startedAt: Long = 0L,
    val currentQuestionIndex: Int = 0,
    val lastSavedAt: Long = System.currentTimeMillis()
)

class TestSessionStore(private val context: Context) {

    private val gson = Gson()
    private val prefs = context.getSharedPreferences("test_session_store_v1", Context.MODE_PRIVATE)

    fun saveSession(session: TestSession) {
        val json = gson.toJson(session)
        require(session.userId.isNotBlank()) { "Sign in before saving this test" }
        prefs.edit().putString("${session.userId}:${session.attemptKey}", json).apply()
    }

    fun getSession(attemptKey: String): TestSession? {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val json = prefs.getString("$uid:$attemptKey", null) ?: prefs.getString(attemptKey, null) ?: return null
        return try {
            val session = gson.fromJson(json, TestSession::class.java)
            session.takeIf { it.userId.isNotBlank() && it.userId == uid }
        } catch (_: Exception) {
            null
        }
    }

    fun hasSession(attemptKey: String): Boolean {
        return getSession(attemptKey) != null
    }

    fun clearSession(attemptKey: String, ownerUid: String? = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid) {
        val uid = ownerUid ?: return
        prefs.edit().remove("$uid:$attemptKey").apply()
    }
}

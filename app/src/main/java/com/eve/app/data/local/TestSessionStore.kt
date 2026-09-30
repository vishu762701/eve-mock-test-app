package com.eve.app.data.local

import android.content.Context
import com.google.gson.Gson

data class TestSession(
    val attemptKey: String,
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
        prefs.edit().putString(session.attemptKey, json).apply()
    }

    fun getSession(attemptKey: String): TestSession? {
        val json = prefs.getString(attemptKey, null) ?: return null
        return try {
            gson.fromJson(json, TestSession::class.java)
        } catch (_: Exception) {
            null
        }
    }

    fun hasSession(attemptKey: String): Boolean {
        return prefs.contains(attemptKey)
    }

    fun clearSession(attemptKey: String) {
        prefs.edit().remove(attemptKey).apply()
    }
}

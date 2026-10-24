package com.eve.app.util

import android.content.Context

/**
 * Helper for Daily Streak formatting and persistent state cache.
 * Ensures the streak summary survives activity recreation and theme switches without flickering or resetting to GONE.
 */
object StreakHelper {
    const val PREFS_NAME = "eve_streak_prefs"
    const val KEY_DAILY_GOAL = "daily_goal"
    const val DEFAULT_DAILY_GOAL = 2

    const val KEY_HAS_VALID_STREAK = "has_valid_streak"
    const val KEY_CURRENT_STREAK = "current_streak"
    const val KEY_TODAY_COUNT = "today_count"
    const val KEY_LAST_FETCH_TIME = "last_fetch_timestamp"

    data class CachedStreak(
        val currentStreak: Int,
        val todayCount: Int,
        val goal: Int,
        val timestamp: Long
    )

    fun formatStreakText(currentStreak: Int, todayCount: Int, goal: Int): String {
        return "$currentStreak-day streak • Today: $todayCount/$goal tests"
    }

    fun saveStreak(context: Context, currentStreak: Int, todayCount: Int, goal: Int = DEFAULT_DAILY_GOAL) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_HAS_VALID_STREAK, true)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_TODAY_COUNT, todayCount)
            .putInt(KEY_DAILY_GOAL, goal)
            .putLong(KEY_LAST_FETCH_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getCachedStreak(context: Context): CachedStreak? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_HAS_VALID_STREAK, false)) {
            return null
        }
        val streak = prefs.getInt(KEY_CURRENT_STREAK, 0)
        val today = prefs.getInt(KEY_TODAY_COUNT, 0)
        val goal = prefs.getInt(KEY_DAILY_GOAL, DEFAULT_DAILY_GOAL)
        val ts = prefs.getLong(KEY_LAST_FETCH_TIME, 0L)
        return CachedStreak(
            currentStreak = streak,
            todayCount = today,
            goal = goal,
            timestamp = ts
        )
    }
}

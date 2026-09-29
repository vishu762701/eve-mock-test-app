package com.eve.app.util

object StreakHelper {
    const val PREFS_NAME = "eve_streak_prefs"
    const val KEY_DAILY_GOAL = "daily_goal"
    const val DEFAULT_DAILY_GOAL = 2

    fun formatStreakText(currentStreak: Int, todayCount: Int, goal: Int): String {
        return "🔥 $currentStreak-day streak • Today: $todayCount/$goal tests"
    }
}

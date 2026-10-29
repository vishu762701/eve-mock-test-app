package com.eve.app.util

/**
 * Shared preferences contract for daily practice goal settings and reminders.
 * Preserves user settings across app sessions without maintaining dead Home streak cache state.
 */
object StreakHelper {
    const val PREFS_NAME = "eve_streak_prefs"
    const val KEY_DAILY_GOAL = "daily_goal"
    const val DEFAULT_DAILY_GOAL = 2
}

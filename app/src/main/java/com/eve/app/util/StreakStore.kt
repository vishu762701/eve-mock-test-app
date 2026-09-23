package com.eve.app.util

import android.content.Context

/**
 * Phase 20: Daily GK streak — locally persist kiya jaata hai.
 * Submit ke baad [recordAttempt] call karo; Home card yahi se streak padhta hai.
 */
object StreakStore {

    private const val PREFS = "eve_daily_gk"
    private const val KEY_STREAK = "streak"
    private const val KEY_LAST_DATE = "last_date"
    private const val KEY_LAST_SCORE = "last_score"
    private const val KEY_LAST_TOTAL = "last_total"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun streak(context: Context): Int = prefs(context).getInt(KEY_STREAK, 0)

    fun lastDate(context: Context): String = prefs(context).getString(KEY_LAST_DATE, "") ?: ""

    fun lastScore(context: Context): Int = prefs(context).getInt(KEY_LAST_SCORE, -1)

    fun lastTotal(context: Context): Int = prefs(context).getInt(KEY_LAST_TOTAL, 0)

    fun attemptedToday(context: Context): Boolean = lastDate(context) == DateUtil.todayIso()

    fun recordAttempt(context: Context, isoDate: String, score: Int, total: Int) {
        val p = prefs(context)
        val last = p.getString(KEY_LAST_DATE, "") ?: ""
        if (last == isoDate) {
            // Same day retake — streak same, latest score update
            p.edit().putInt(KEY_LAST_SCORE, score).putInt(KEY_LAST_TOTAL, total).apply()
            return
        }
        val nextStreak = when {
            last.isBlank() -> 1
            last == DateUtil.yesterdayIso() && isoDate == DateUtil.todayIso() -> p.getInt(KEY_STREAK, 0) + 1
            isoDate == DateUtil.todayIso() -> 1
            else -> 1
        }
        p.edit()
            .putInt(KEY_STREAK, nextStreak)
            .putString(KEY_LAST_DATE, isoDate)
            .putInt(KEY_LAST_SCORE, score)
            .putInt(KEY_LAST_TOTAL, total)
            .apply()
    }
}

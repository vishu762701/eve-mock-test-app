package com.eve.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eve.app.data.remote.ApiClient
import com.eve.app.util.NotificationHelper
import com.eve.app.util.StreakHelper
import com.google.firebase.auth.FirebaseAuth
import java.util.TimeZone

class DailyReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val prefs = applicationContext.getSharedPreferences(StreakHelper.PREFS_NAME, Context.MODE_PRIVATE)
        val goal = prefs.getInt(StreakHelper.KEY_DAILY_GOAL, StreakHelper.DEFAULT_DAILY_GOAL)

        var todayCount = 0
        var currentStreak = 0

        try {
            val tzOffset = -TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
            val res = ApiClient.api.getStreak(tzOffset)
            if (res.success && res.data != null) {
                todayCount = res.data.todayCount
                currentStreak = res.data.currentStreak
            }
        } catch (_: Exception) {}

        // If todayCount >= goal, user already achieved their goal today -> show nothing!
        if (todayCount >= goal) {
            return Result.success()
        }

        val (title, body) = if (currentStreak > 0 && todayCount == 0) {
            "Don't lose your $currentStreak-day streak!" to "Complete a mock test today to keep your streak alive."
        } else {
            val messages = listOf(
                "Time for your daily practice!" to "A quick mock test will help you hit your goal of $goal tests today.",
                "Haven't reached today's goal yet?" to "You've completed $todayCount/$goal tests today. Keep it up!",
                "Consistency is key!" to "Keep your daily study momentum going with a test today!"
            )
            messages.random()
        }

        NotificationHelper.showDailyReminder(applicationContext, title, body)
        return Result.success()
    }
}

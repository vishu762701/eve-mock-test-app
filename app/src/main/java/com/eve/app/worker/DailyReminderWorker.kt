package com.eve.app.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.eve.app.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth

/**
 * Phase 12: har roz ek local notification jo student ko practice karne ke liye yaad dilaata
 * hai. Yeh purely on-device hai (koi server ki zaroorat nahi) — [com.eve.app.util.ReminderScheduler]
 * isko WorkManager ke through daily schedule karta hai.
 */
class DailyReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // Logout ho chuka ho to reminder mat dikhao
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()

        val messages = listOf(
            "Today's Daily GK quiz is ready" to "Take 5 minutes to attempt today's quiz and protect your streak.",
            "Haven't taken today's mock test yet?" to "Take 5 minutes and complete a test.",
            "Practice makes perfect!" to "Taking a daily mock test improves both speed and accuracy.",
            "Preparing for your exam?" to "Attempt today's Daily GK and a mock test to track your progress.",
            "Consistency is key!" to "Keep your daily study momentum going — maintain your streak!"
        )
        val (title, body) = messages.random()
        NotificationHelper.showDailyReminder(applicationContext, title, body)
        return Result.success()
    }
}

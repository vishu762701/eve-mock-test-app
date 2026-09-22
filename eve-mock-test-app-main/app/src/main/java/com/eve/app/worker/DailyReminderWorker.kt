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
            "Aaj ka mock test abhi tak nahi diya?" to "5 minute nikaalo aur ek test complete karo.",
            "Practice makes perfect!" to "Rozana ek mock test se apni speed aur accuracy dono improve hongi.",
            "Exam ki taiyari chal rahi hai?" to "Aaj ka test attempt karke apni progress check karo.",
            "Consistency is the key" to "Ek test aaj bhi ho jaaye — apna streak mat todo!"
        )
        val (title, body) = messages.random()
        NotificationHelper.showDailyReminder(applicationContext, title, body)
        return Result.success()
    }
}

package com.eve.app.util

import android.content.Context
import android.widget.ImageButton
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.eve.app.R
import com.eve.app.worker.DailyReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Daily reminder ON/OFF state ThemeManager jaisi hi SharedPreferences me save hoti hai, taaki
 * choice app dobara khulne par bhi yaad rahe. Default = ON (pehli baar app khulte hi reminder
 * schedule ho jaata hai; user chahe to Home screen ke bell icon se band kar sakta hai).
 */
object ReminderScheduler {

    private const val PREFS = "eve_prefs"
    private const val KEY_REMINDER_ENABLED = "key_reminder_enabled"
    private const val WORK_NAME = "eve_daily_reminder"
    private const val REMINDER_HOUR = 19 // Reminder roz shaam 7 baje

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_REMINDER_ENABLED, true)

    /** App start / login ke baad call karo — saved preference ke hisaab se schedule kar dega ya cancel. */
    fun applySavedState(context: Context) {
        if (isEnabled(context)) scheduleDailyReminder(context) else cancelDailyReminder(context)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .apply()
        if (enabled) scheduleDailyReminder(context) else cancelDailyReminder(context)
    }

    private fun scheduleDailyReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextReminder(), TimeUnit.MILLISECONDS)
            .build()

        // KEEP: agar pehle se schedule hai to usi ko rakho (naya delay recalc karke dobara
        // enqueue karne se har app-open par time thoda shift ho jaata, isliye sirf tab replace
        // karo jab user ne explicitly toggle kiya ho — setEnabled() alag se REPLACE bhejta hai)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }

    private fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Home screen ke bell icon ko current state ke hisaab se set karta hai (ThemeManager wale toggle jaisa hi pattern). */
    fun setupToggleButton(context: Context, button: ImageButton) {
        button.setImageResource(if (isEnabled(context)) R.drawable.ic_bell else R.drawable.ic_bell_off)
        button.contentDescription = context.getString(
            if (isEnabled(context)) R.string.reminder_toggle_off else R.string.reminder_toggle_on
        )
        button.setOnClickListener {
            setEnabled(context, !isEnabled(context))
            setupToggleButton(context, button)
        }
    }

    private fun millisUntilNextReminder(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return target.timeInMillis - now.timeInMillis
    }
}

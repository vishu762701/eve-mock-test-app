package com.eve.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eve.app.R

/**
 * Phase 12: dono tarah ke notifications (naya exam alert + daily reminder) ke channels
 * aur ek common "safely show karo" function yahan hai — taaki permission check har jagah
 * copy-paste na karna pade.
 */
object NotificationHelper {

    const val CHANNEL_NEW_EXAM = "new_exam_channel_v2"
    const val CHANNEL_DAILY_REMINDER = "daily_reminder_channel"

    /** FCM isi topic par subscribe karke sabhi users ko naya exam ka alert bhejega */
    const val TOPIC_NEW_EXAMS = "new_exams"

    private const val ID_NEW_EXAM = 1001
    private const val ID_DAILY_REMINDER = 1002

    private val VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250)

    /** App start hote hi (Android 8+ ke liye channels banana zaroori hai, warna notification dikhegi hi nahi) */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEW_EXAM,
                "New Exam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a new mock test is added"
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                "Daily Practice Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily mock test practice reminder"
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
            }
        )
    }

    /** Android 13+ par POST_NOTIFICATIONS permission diya gaya hai ya nahi */
    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showNewExamNotification(context: Context, title: String, body: String) {
        show(context, CHANNEL_NEW_EXAM, ID_NEW_EXAM, title, body)
    }

    fun showDailyReminder(context: Context, title: String, body: String) {
        show(context, CHANNEL_DAILY_REMINDER, ID_DAILY_REMINDER, title, body)
    }

    private fun show(context: Context, channelId: String, notificationId: Int, title: String, body: String) {
        // Bug fix: har notification ab local history me bhi save hoti hai (bell icon list ke liye)
        NotificationStore.add(context, title, body)

        if (!hasPermission(context)) return
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(VIBRATION_PATTERN)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

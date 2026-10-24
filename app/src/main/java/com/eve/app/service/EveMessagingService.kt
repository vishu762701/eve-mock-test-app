package com.eve.app.service

import com.eve.app.data.remote.ApiClient
import com.eve.app.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EveMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.apiService.updateFcmToken(mapOf("fcmToken" to token))
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New Exam Available!"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: "A new mock test has been added. Try it now!"

        com.eve.app.util.VibrationHelper.vibrateNotification(applicationContext)
        NotificationHelper.showNewExamNotification(applicationContext, title, body)
    }
}

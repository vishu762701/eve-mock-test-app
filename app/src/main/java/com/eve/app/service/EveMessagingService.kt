package com.eve.app.service

import com.eve.app.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Phase 12: naye exam ka push notification.
 *
 * Sabhi devices "new_exams" topic subscribe karte hain (MainActivity me). Jab admin Firestore
 * me naya exam add karta hai, notification bhejne ka kaam ek Cloud Function karta hai (dekho
 * README.md ke "Phase 12" section me `functions/index.js` — wahi is topic par FCM message
 * bhejta hai). Yeh client sirf us message ko receive karke dikhata hai; client se seedha
 * doosre devices ko push bhejna possible nahi hai (koi bhi client apni FCM server key expose
 * nahi kar sakta), isliye Cloud Function zaroori hai — usko deploy kiye bina yeh service kaam
 * to karega (test message Firebase Console se bhejke dekha ja sakta hai) par asli automatic
 * alert exam add hote hi tabhi aayega jab function deploy ho.
 */
class EveMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Future me kisi specific user ko target karke bhejna ho (jaise sirf unhi students ko
        // jinhone ek exam attempt kiya hai) to token ko Firestore me save karke rakha ja sakta
        // hai. Abhi ke liye sabhi ek hi topic ("new_exams") use karte hain isliye yeh optional hai.
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(hashMapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // App foreground me ho tab bhi notification payload wale messages system khud dikha
        // deta hai — is override ka kaam sirf DATA-only messages (jaise Cloud Function se
        // bheja gaya) handle karna hai taaki foreground me bhi notification dikhe.
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New Exam Available!"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: "A new mock test has been added. Try it now!"

        NotificationHelper.showNewExamNotification(applicationContext, title, body)
    }
}

package com.eve.app

import android.app.Application
import com.eve.app.util.ThemeManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class EveApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Saved dark/light choice sabse pehle apply karo, koi Activity dikhne se pehle
        // (warna ek split-second ke liye galat theme flash ho sakti hai).
        ThemeManager.applySavedMode(this)

        // Firestore offline persistence: exams/questions ek baar load hone ke baad
        // device par cache ho jaate hain, taaki weak/no network me bhi purana data
        // turant dikh sake. Firestore SDK me yeh by default bhi on hota hai, par yahan
        // explicitly (unlimited cache size ke saath) set kar rahe hain taaki behaviour
        // guaranteed rahe, chahe future me SDK ka default kabhi badal jaye.
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}

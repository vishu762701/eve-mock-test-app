package com.eve.app.data.repository

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Admin Dashboard ke liye: total signed-up users, aur abhi "online" (recently active) users
 * ka count. `users/{uid}` doc LoginActivity (har login par) aur MainActivity.onResume (jab tak
 * app foreground mein hai) se likha jaata hai — dekho unke comments.
 *
 * "Online" yahan ek live presence system (jaisa Realtime Database se hota hai) nahi hai — bas
 * "lastActive pichle ONLINE_WINDOW_MINUTES ke andar update hui" ka approximate signal hai.
 * Is app ke scale ke liye yeh kaafi hai aur extra infra (RTDB, Cloud Functions) nahi maangta.
 */
class UserStatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val ONLINE_WINDOW_MINUTES = 5
    }

    private fun collection() = db.collection("users")

    suspend fun getTotalUserCount(): Long =
        collection().count().get(AggregateSource.SERVER).await().count

    suspend fun getOnlineUserCount(): Long {
        val cutoff = System.currentTimeMillis() - ONLINE_WINDOW_MINUTES * 60_000L
        return collection()
            .whereGreaterThan("lastActive", cutoff)
            .count().get(AggregateSource.SERVER).await().count
    }
}

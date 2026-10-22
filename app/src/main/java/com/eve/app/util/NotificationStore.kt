package com.eve.app.util

import android.content.Context
import com.eve.app.data.model.BroadcastMessage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bug fix: pehle Home screen ka bell icon sirf daily-reminder ON/OFF toggle tha, notification
 * dikhata hi nahi tha (woh toggle ab Profile screen me shift kar diya hai). Ab bell tap karne par
 * ek Notifications list khulti hai — yeh class us list ko device par hi (SharedPreferences me
 * JSON array ke roop me) save/read karti hai. Koi extra backend/collection nahi chahiye.
 */
data class StoredNotification(
    val title: String,
    val body: String,
    val timestampMillis: Long,
    val read: Boolean
)

object NotificationStore {

    private const val PREFS = "eve_prefs"
    private const val KEY_NOTIFICATIONS = "key_notifications_json"
    private const val MAX_STORED = 50
    private const val KEY_LAST_BROADCAST_TS = "key_last_broadcast_delivery_ts"

    fun add(context: Context, title: String, body: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = readArray(prefs)

        val entry = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("timestamp", System.currentTimeMillis())
            put("read", false)
        }

        // Naya notification sabse upar (index 0)
        val newArray = JSONArray()
        newArray.put(entry)
        for (i in 0 until array.length()) newArray.put(array.get(i))

        // Sirf latest MAX_STORED hi rakho, warna SharedPreferences dheere dheere bhaari ho jayegi
        val trimmed = JSONArray()
        for (i in 0 until minOf(newArray.length(), MAX_STORED)) trimmed.put(newArray.get(i))

        prefs.edit().putString(KEY_NOTIFICATIONS, trimmed.toString()).apply()
        try {
            context.sendBroadcast(android.content.Intent("com.eve.app.NOTIFICATION_RECEIVED").setPackage(context.packageName))
        } catch (_: Exception) { }
    }

    /**
     * Import a bounded, server-authoritative broadcast page without replacing local FCM
     * and reminder notifications. Synchronize delivery by stable broadcast ID plus the
     * existing last-delivery timestamp; this does NOT acknowledge anything as read.
     * Caller owns visible badge/motion updates so opening Home never vibrates for old posts.
     */
    @Synchronized
    fun importBroadcasts(context: Context, broadcasts: List<BroadcastMessage>): Int {
        if (broadcasts.isEmpty()) return 0
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val old = readArray(prefs)
        val lastCursor = prefs.getLong(KEY_LAST_BROADCAST_TS, 0L)
        val alreadyStoredIds = mutableSetOf<String>()
        for (i in 0 until old.length()) {
            old.optJSONObject(i)?.optString("sourceId")?.takeIf { it.isNotBlank() }
                ?.let { alreadyStoredIds.add(it) }
        }
        val dedupedIds = mutableSetOf<String>()
        val incoming = broadcasts.asSequence()
            .filter { it.id.isNotBlank() && it.sentAt > 0L && it.sentAt >= lastCursor }
            .filter { it.id !in alreadyStoredIds && dedupedIds.add(it.id) }
            .sortedWith(compareBy<BroadcastMessage> { it.sentAt }.thenBy { it.id })
            .toList().takeLast(MAX_STORED)
        val maxTimestamp = maxOf(lastCursor, broadcasts.maxOfOrNull { it.sentAt } ?: 0L)
        if (incoming.isEmpty()) {
            if (maxTimestamp > lastCursor) {
                prefs.edit().putLong(KEY_LAST_BROADCAST_TS, maxTimestamp).apply()
            }
            return 0
        }
        val merged = JSONArray()
        for (item in incoming.asReversed()) {
            merged.put(JSONObject().apply {
                put("title", item.title)
                put("body", item.message)
                put("timestamp", item.sentAt)
                put("sourceId", item.id)
                put("read", false)
            })
        }
        for (i in 0 until minOf(old.length(), MAX_STORED - incoming.size)) {
            merged.put(old.get(i))
        }
        prefs.edit()
            .putString(KEY_NOTIFICATIONS, merged.toString())
            .putLong(KEY_LAST_BROADCAST_TS, maxTimestamp)
            .apply()
        return incoming.size
    }

    fun getAll(context: Context): List<StoredNotification> {
        val array = readArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            StoredNotification(
                title = obj.getString("title"),
                body = obj.getString("body"),
                timestampMillis = obj.getLong("timestamp"),
                read = obj.optBoolean("read", false)
            )
        }
    }

    private const val KEY_LAST_SEEN_TIMESTAMP = "key_last_seen_notif_ts"

    fun getLastSeenTimestamp(context: Context): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SEEN_TIMESTAMP, 0L)
    }

    fun setLastSeenTimestamp(context: Context, timestamp: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_SEEN_TIMESTAMP, timestamp)
            .apply()
    }

    fun getLatestNotificationTimestamp(context: Context): Long {
        val all = getAll(context)
        return all.firstOrNull()?.timestampMillis ?: 0L
    }

    fun hasNewUnseen(context: Context): Boolean {
        val latest = getLatestNotificationTimestamp(context)
        val lastSeen = getLastSeenTimestamp(context)
        return latest > lastSeen && hasUnread(context)
    }

    fun hasUnread(context: Context): Boolean = getAll(context).any { !it.read }

    /** Notifications screen khulte hi sab ko "read" mark kar do taaki bell ka red dot hat jaye. */
    fun markAllRead(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = readArray(prefs)
        var maxTs = 0L
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            obj.put("read", true)
            val ts = obj.optLong("timestamp", 0L)
            if (ts > maxTs) maxTs = ts
        }
        val editor = prefs.edit().putString(KEY_NOTIFICATIONS, array.toString())
        if (maxTs > 0) {
            editor.putLong(KEY_LAST_SEEN_TIMESTAMP, maxTs)
        } else {
            editor.putLong(KEY_LAST_SEEN_TIMESTAMP, System.currentTimeMillis())
        }
        editor.apply()
    }

    private fun readArray(prefs: android.content.SharedPreferences): JSONArray {
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}

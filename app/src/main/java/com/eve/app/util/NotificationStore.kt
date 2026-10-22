package com.eve.app.util

import android.content.Context
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

    /** Deduplicate server broadcasts without conflating delivery with read acknowledgement. */
    @Synchronized
    fun addBroadcast(context: Context, title: String, body: String, sentAt: Long): Boolean {
        if (sentAt <= 0L) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (sentAt <= prefs.getLong(KEY_LAST_BROADCAST_TS, 0L)) return false
        val old = readArray(prefs)
        val entry = JSONObject().apply {
            put("title", title)
            put("body", body)
            put("timestamp", sentAt)
            put("read", false)
        }
        val merged = JSONArray().apply {
            put(entry)
            for (i in 0 until minOf(old.length(), MAX_STORED - 1)) put(old.get(i))
        }
        prefs.edit()
            .putString(KEY_NOTIFICATIONS, merged.toString())
            .putLong(KEY_LAST_BROADCAST_TS, sentAt)
            .apply()
        context.sendBroadcast(android.content.Intent("com.eve.app.NOTIFICATION_RECEIVED").setPackage(context.packageName))
        return true
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

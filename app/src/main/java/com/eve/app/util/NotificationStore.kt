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

    fun hasUnread(context: Context): Boolean = getAll(context).any { !it.read }

    /** Notifications screen khulte hi sab ko "read" mark kar do taaki bell ka red dot hat jaye. */
    fun markAllRead(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = readArray(prefs)
        for (i in 0 until array.length()) {
            array.getJSONObject(i).put("read", true)
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, array.toString()).apply()
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

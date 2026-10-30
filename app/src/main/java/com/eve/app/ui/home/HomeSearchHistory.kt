package com.eve.app.ui.home

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer
import java.util.Locale

/** EVE-only searches: bounded local phrases, partitioned by signed-in Firebase UID.
 * No keystroke recording; only explicit search submissions or suggestion selections. */
internal object HomeSearchHistory {
    private const val PREFS = "eve_home_search_history"
    private const val MAX_ITEMS = 8
    private const val MAX_CHARS = 80

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)
            .trim().replace(Regex("\\s+"), " ").take(MAX_CHARS)

    fun load(context: Context, uid: String): List<String> {
        if (uid.isBlank()) return emptyList()
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("recent_$uid", "[]") ?: "[]"
        return try {
            val array = JSONArray(saved)
            (0 until minOf(array.length(), MAX_ITEMS)).mapNotNull { idx ->
                normalize(array.optString(idx)).takeIf { it.isNotEmpty() }
            }.distinctBy { it.lowercase(Locale.ROOT) }
        } catch (_: org.json.JSONException) {
            emptyList()
        }
    }

    fun record(context: Context, uid: String, value: String) {
        val query = normalize(value)
        if (uid.isBlank() || query.isEmpty()) return
        val recent = (listOf(query) + load(context, uid).filterNot {
            it.equals(query, ignoreCase = true)
        }).take(MAX_ITEMS)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("recent_$uid", JSONArray(recent).toString()).apply()
    }

    fun clear(context: Context, uid: String) {
        if (uid.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("recent_$uid").apply()
    }
}

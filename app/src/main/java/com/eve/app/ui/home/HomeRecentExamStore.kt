package com.eve.app.ui.home

import android.content.Context

/** One account-bound owner for the Home Continue exam preference. */
internal object HomeRecentExamStore {
    private const val PREFS = "eve_app_prefs"
    private const val KEY_PREFIX = "last_exam_id_"

    private fun key(uid: String) = KEY_PREFIX + uid

    @Synchronized
    fun save(context: Context, uid: String, examId: String) {
        if (uid.isBlank() || examId.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(uid), examId).apply()
    }

    fun load(context: Context, uid: String?): String? {
        if (uid.isNullOrBlank()) return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key(uid), null)?.takeIf { it.isNotBlank() }
    }

    @Synchronized
    fun clearDeletedUser(context: Context, uid: String) {
        if (uid.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(key(uid)).apply()
    }
}

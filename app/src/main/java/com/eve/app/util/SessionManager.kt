package com.eve.app.util

import android.content.Context
import com.eve.app.EveApplication

/**
 * Manages user session state and caches backend-verified admin status with a short TTL.
 * Admin authorization is authoritatively determined by the Cloudflare Worker backend (/api/auth/me).
 */
object SessionManager {

    private const val PREFS_NAME = "eve_session_manager"
    private const val KEY_IS_ADMIN = "session_is_admin"
    private const val KEY_ADMIN_EMAIL = "session_admin_email"
    private const val KEY_TIMESTAMP = "session_admin_timestamp"

    /** Short TTL for admin status cache (5 minutes = 300,000 ms) */
    const val CACHE_TTL_MS = 5 * 60 * 1000L

    internal var prefsOverride: android.content.SharedPreferences? = null

    private val prefs: android.content.SharedPreferences
        get() = prefsOverride ?: EveApplication.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns cached admin status if email matches and cache is still within TTL.
     * If allowStale is true (e.g. offline fallback), returns cached value even if expired.
     */
    fun getCachedAdminStatus(email: String?, allowStale: Boolean = false): Boolean? {
        if (email.isNullOrBlank()) return null
        val cleanEmail = email.trim().lowercase()
        val cachedEmail = prefs.getString(KEY_ADMIN_EMAIL, null) ?: return null
        if (!cachedEmail.equals(cleanEmail, ignoreCase = true)) return null

        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        val isFresh = (now - timestamp) < CACHE_TTL_MS

        if (isFresh || allowStale) {
            return prefs.getBoolean(KEY_IS_ADMIN, false)
        }
        return null
    }

    /**
     * Caches the admin status returned authoritatively from the backend (/api/auth/me).
     */
    fun setCachedAdminStatus(email: String?, isAdmin: Boolean) {
        if (email.isNullOrBlank()) return
        val cleanEmail = email.trim().lowercase()
        prefs.edit()
            .putString(KEY_ADMIN_EMAIL, cleanEmail)
            .putBoolean(KEY_IS_ADMIN, isAdmin)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    /**
     * Clears all session cache (called on sign out).
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
}

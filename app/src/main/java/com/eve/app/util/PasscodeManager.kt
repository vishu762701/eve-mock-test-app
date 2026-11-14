/*
 * Derived from Telegram Android PasscodeActivity.java, SharedConfig.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.util

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Secure Passcode and App Lock Manager.
 * Upstream: SharedConfig.java:passcodeHash, passcodeSalt, autoLockIn
 */
object PasscodeManager {

    private const val PREFS_NAME = "passcode_prefs"
    private const val KEY_PASSCODE_HASH = "passcode_hash"
    private const val KEY_PASSCODE_SALT = "passcode_salt"
    private const val KEY_PASSCODE_ENABLED = "passcode_enabled"
    private const val KEY_FINGERPRINT_ENABLED = "fingerprint_enabled"
    private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
    private const val KEY_LAST_PAUSED_TIME = "last_paused_time"
    private const val KEY_IS_LOCKED = "is_locked"

    // Auto lock timeout values in seconds
    const val TIMEOUT_DISABLED = 0
    const val TIMEOUT_1_MINUTE = 60
    const val TIMEOUT_5_MINUTES = 300
    const val TIMEOUT_1_HOUR = 3600
    const val TIMEOUT_5_HOURS = 18000

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isPasscodeEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_PASSCODE_ENABLED, false) && hasPasscode(context)

    fun hasPasscode(context: Context): Boolean =
        getPrefs(context).getString(KEY_PASSCODE_HASH, null) != null

    fun setPasscode(context: Context, passcode: String) {
        val salt = generateSalt()
        val hash = hashPasscode(passcode, salt)
        getPrefs(context).edit()
            .putString(KEY_PASSCODE_HASH, hash)
            .putString(KEY_PASSCODE_SALT, salt)
            .putBoolean(KEY_PASSCODE_ENABLED, true)
            .apply()
    }

    fun checkPasscode(context: Context, passcode: String): Boolean {
        val prefs = getPrefs(context)
        val salt = prefs.getString(KEY_PASSCODE_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_PASSCODE_HASH, null) ?: return false
        val calculatedHash = hashPasscode(passcode, salt)
        return calculatedHash == expectedHash
    }

    fun disablePasscode(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_PASSCODE_HASH)
            .remove(KEY_PASSCODE_SALT)
            .putBoolean(KEY_PASSCODE_ENABLED, false)
            .putBoolean(KEY_IS_LOCKED, false)
            .apply()
    }

    fun setPasscodeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_PASSCODE_ENABLED, enabled)
            .apply()
    }

    fun isFingerprintEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_FINGERPRINT_ENABLED, true)

    fun setFingerprintEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_FINGERPRINT_ENABLED, enabled)
            .apply()
    }

    fun getAutoLockTimeout(context: Context): Int =
        getPrefs(context).getInt(KEY_AUTO_LOCK_TIMEOUT, TIMEOUT_5_MINUTES)

    fun setAutoLockTimeout(context: Context, seconds: Int) {
        getPrefs(context).edit()
            .putInt(KEY_AUTO_LOCK_TIMEOUT, seconds)
            .apply()
    }

    fun isLocked(context: Context): Boolean {
        if (!isPasscodeEnabled(context)) return false
        val prefs = getPrefs(context)
        if (prefs.getBoolean(KEY_IS_LOCKED, false)) return true

        val timeout = getAutoLockTimeout(context)
        if (timeout == TIMEOUT_DISABLED) return false

        val lastPaused = prefs.getLong(KEY_LAST_PAUSED_TIME, 0L)
        if (lastPaused == 0L) return false

        val elapsed = (SystemClock.elapsedRealtime() - lastPaused) / 1000L
        if (elapsed >= timeout) {
            lock(context)
            return true
        }
        return false
    }

    fun lock(context: Context) {
        if (!isPasscodeEnabled(context)) return
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LOCKED, true)
            .apply()
    }

    fun unlock(context: Context) {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putLong(KEY_LAST_PAUSED_TIME, SystemClock.elapsedRealtime())
            .apply()
    }

    fun onAppPaused(context: Context) {
        if (isPasscodeEnabled(context)) {
            getPrefs(context).edit()
                .putLong(KEY_LAST_PAUSED_TIME, SystemClock.elapsedRealtime())
                .apply()
        }
    }

    fun onAppResumed(context: Context) {
        // Triggers auto-lock check
        if (isPasscodeEnabled(context)) {
            val timeout = getAutoLockTimeout(context)
            if (timeout != TIMEOUT_DISABLED) {
                val lastPaused = getPrefs(context).getLong(KEY_LAST_PAUSED_TIME, 0L)
                if (lastPaused > 0L) {
                    val elapsed = (SystemClock.elapsedRealtime() - lastPaused) / 1000L
                    if (elapsed >= timeout) {
                        lock(context)
                    }
                }
            }
        }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPasscode(passcode: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$passcode"
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

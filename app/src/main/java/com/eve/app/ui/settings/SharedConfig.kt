package com.eve.app.ui.settings

import android.content.Context
import android.os.SystemClock
import androidx.annotation.IntDef
import com.eve.app.util.PasscodeManager
import org.telegram.messenger.ApplicationLoader

/**
 * Eve CUT-INTERFACE bridge for [org.telegram.ui.PasscodeActivity].
 * Mirrors the 15 `SharedConfig` passcode fields/methods read/written by Telegram's
 * PasscodeActivity and persists every change through [PasscodeManager].
 */
object SharedConfig {
    const val PASSCODE_TYPE_PIN = 0
    const val PASSCODE_TYPE_PASSWORD = 1

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(PASSCODE_TYPE_PIN, PASSCODE_TYPE_PASSWORD)
    annotation class PasscodeType

    private const val BRIDGE_PREFS = "eve_passcode_bridge"
    private const val KEY_TYPE = "passcode_type"
    private const val KEY_ALLOW_CAPTURE = "allow_screen_capture"

    @JvmField var passcodeHash: String = ""
    @JvmField var passcodeSalt: ByteArray = ByteArray(0)
    @JvmField var passcodeType: Int = PASSCODE_TYPE_PIN
    @JvmField var appLocked: Boolean = false
    @JvmField var autoLockIn: Int = PasscodeManager.TIMEOUT_5_MINUTES
    @JvmField var useFingerprintLock: Boolean = true
    @JvmField var allowScreenCapture: Boolean = false
    @JvmField var badPasscodeTries: Int = 0
    @JvmField var passcodeRetryInMs: Long = 0L
    @JvmField var lastUptimeMillis: Long = 0L

    private var pendingPlainPasscode: String? = null

    private fun appContext(): Context? = ApplicationLoader.applicationContext

    @JvmStatic
    fun loadFromEve(context: Context) {
        val prefs = context.getSharedPreferences(BRIDGE_PREFS, Context.MODE_PRIVATE)
        passcodeHash = if (PasscodeManager.isPasscodeEnabled(context)) "eve_enabled" else ""
        passcodeSalt = ByteArray(0)
        passcodeType = prefs.getInt(KEY_TYPE, PASSCODE_TYPE_PIN)
        appLocked = PasscodeManager.isLocked(context)
        autoLockIn = PasscodeManager.getAutoLockTimeout(context)
        useFingerprintLock = PasscodeManager.isFingerprintEnabled(context)
        allowScreenCapture = prefs.getBoolean(KEY_ALLOW_CAPTURE, false)
        badPasscodeTries = 0
        passcodeRetryInMs = 0L
        pendingPlainPasscode = null
    }

    @JvmStatic
    fun setPlainPasscodeForSave(plain: String) {
        pendingPlainPasscode = plain
    }

    @JvmStatic
    fun checkPasscode(passcode: String): Boolean {
        val ctx = appContext() ?: return false
        return PasscodeManager.checkPasscode(ctx, passcode)
    }

    @JvmStatic
    fun increaseBadPasscodeTries() {
        badPasscodeTries++
        if (badPasscodeTries >= 3) {
            passcodeRetryInMs = when (badPasscodeTries) {
                3 -> 5000L
                4 -> 10000L
                5 -> 15000L
                6 -> 20000L
                7 -> 25000L
                else -> 30000L
            }
            lastUptimeMillis = SystemClock.elapsedRealtime()
        }
    }

    @JvmStatic
    fun saveConfig() {
        val ctx = appContext() ?: return
        if (passcodeHash.isEmpty()) {
            PasscodeManager.disablePasscode(ctx)
        } else {
            val plain = pendingPlainPasscode
            if (plain != null) {
                PasscodeManager.setPasscode(ctx, plain)
                PasscodeManager.unlock(ctx)
                pendingPlainPasscode = null
            }
        }
        PasscodeManager.setFingerprintEnabled(ctx, useFingerprintLock)
        PasscodeManager.setAutoLockTimeout(ctx, autoLockIn)
        ctx.getSharedPreferences(BRIDGE_PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_TYPE, passcodeType)
            .putBoolean(KEY_ALLOW_CAPTURE, allowScreenCapture)
            .apply()
    }
}

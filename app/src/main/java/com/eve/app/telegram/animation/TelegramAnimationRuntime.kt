package com.eve.app.telegram.animation

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings

/** Loads only Telegram's isolated QR animation JNI, never Telegram account/network code. */
object TelegramAnimationRuntime {
    init {
        try {
            System.loadLibrary("eve_qr_animation")
        } catch (_: UnsatisfiedLinkError) {
            // Ignored in unit test runtime
        }
    }

    fun ensureLoaded() = Unit

    fun isMotionEnabled(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 26) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

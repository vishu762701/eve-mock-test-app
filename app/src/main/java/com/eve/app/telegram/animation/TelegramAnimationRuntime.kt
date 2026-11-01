package com.eve.app.telegram.animation

/** Loads only Telegram's isolated QR animation JNI, never Telegram account/network code. */
internal object TelegramAnimationRuntime {
    init { System.loadLibrary("eve_qr_animation") }
    fun ensureLoaded() = Unit
}

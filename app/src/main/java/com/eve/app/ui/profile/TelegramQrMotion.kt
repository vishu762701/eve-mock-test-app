package com.eve.app.ui.profile

import kotlin.math.exp
import kotlin.math.tanh

/** Source-traced motion parameters from Telegram 13.1.1 QR; EVE rendering is ADAPTED.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareActivity.java:819-832; QrShareView.java:208-212,2785-2787,3631-3665.
 */
internal object TelegramQrMotion {
    const val OPEN_MS = com.eve.app.telegram.qr.QrSceneReveal.OPEN_MS
    const val CLOSE_MS = com.eve.app.telegram.qr.QrSceneReveal.CLOSE_MS
    const val QR_BUILD_MS = 380L
    const val WAVE_MS = 600L
    const val GLIDE_SECONDS = .35f
    const val MAX_FLING_DP_PER_SEC = 2600f

    fun reveal(fraction: Float, opening: Boolean): Float =
        com.eve.app.telegram.qr.QrSceneReveal.revealProgress(fraction.coerceIn(0f, 1f), opening)

    fun rubber(displacement: Float, length: Float): Float {
        val l = length.coerceAtLeast(1f)
        return .32f * l * tanh(displacement / l * 2.5f)
    }

    fun glide(velocity: Float, seconds: Float): Float =
        velocity * GLIDE_SECONDS *
            (1f - exp((-seconds.coerceAtLeast(0f) / GLIDE_SECONDS).toDouble()).toFloat())
}

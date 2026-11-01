package com.eve.app.ui.profile

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/** Source-traced motion parameters from Telegram 13.1.1 QR; EVE rendering is ADAPTED.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareActivity.java:819-832; QrShareView.java:208-212,2785-2787,3631-3665.
 */
internal object TelegramQrMotion {
    const val OPEN_MS = 860L
    const val CLOSE_MS = 200L
    const val QR_BUILD_MS = 380L
    const val WAVE_MS = 600L
    const val GLIDE_SECONDS = .35f
    const val MAX_FLING_DP_PER_SEC = 2600f

    fun reveal(fraction: Float, opening: Boolean): Float {
        val v = fraction.coerceIn(0f, 1f)
        if (!opening) return 1f - (1f - v) * (1f - v)
        val z = .9
        val t = v * .86
        val w0 = 2.0 * PI / .62
        val wd = w0 * sqrt(1.0 - z * z)
        val spring = 1.0 - exp(-z * w0 * t) *
            (cos(wd * t) + z / sqrt(1.0 - z * z) * sin(wd * t))
        val u = ((v - .85f) / .15f).coerceIn(0f, 1f)
        val blend = u * u * (3f - 2f * u)
        return (spring + (1.0 - spring) * blend).toFloat()
    }

    fun rubber(displacement: Float, length: Float): Float {
        val l = length.coerceAtLeast(1f)
        return .32f * l * tanh(displacement / l * 2.5f)
    }

    fun glide(velocity: Float, seconds: Float): Float =
        velocity * GLIDE_SECONDS *
            (1f - exp((-seconds.coerceAtLeast(0f) / GLIDE_SECONDS).toDouble()).toFloat())
}

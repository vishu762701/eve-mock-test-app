package com.eve.app.ui.profile

import kotlin.math.floor

/**
 * EVE identity and emoji assets; upstream QR selection and placement properties.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareView.java:175,972-992,3171-3202,3206-3233,3369-3407.
 * ADAPTED: the original Telegram document and account identifiers are excluded.
 */
internal data class QrEmojiChoice(
    val key: String,
    val alt: String,
    val source: String = ""
)

internal object QrEmojiPattern {
    const val MAX_SELECTED = 6
    const val COLUMN_DP = 86f
    const val ROW_DP = 70f
    const val APPEAR_MS = 380f
    const val SPREAD_MS = 300f

    fun standard(value: String) = QrEmojiChoice("s:" + value.replace("\uFE0F", ""), value)
    fun custom(id: String, alt: String, source: String) =
        QrEmojiChoice("c:" + id, alt, source)

    /** Original QR toggle contract: one per key, drop oldest after six. */
    fun toggle(current: List<QrEmojiChoice>, picked: QrEmojiChoice): List<QrEmojiChoice> {
        val old = current.filterNot { it.key == picked.key }
        if (old.size != current.size) return old
        return (old + picked).takeLast(MAX_SELECTED)
    }

    /** Source-authentic deterministic cell distribution (not a screen random on redraw). */
    fun hash(row: Int, column: Int): Double {
        var h = row.toLong() * 0x9E3779B97F4A7C15UL.toLong() xor
            (column.toLong() * 0xC2B2AE3D27D4EB4FUL.toLong())
        h = h xor (h ushr 31)
        h *= 0xBF58476D1CE4E5B9UL.toLong()
        h = h xor (h ushr 27)
        h *= 0x94D049BB133111EBUL.toLong()
        h = h xor (h ushr 31)
        return (h ushr 11).toDouble() / 9007199254740992.0
    }

    fun fraction(x: Double): Float = (x - floor(x)).toFloat()

    /** Original overshoot used for each asynchronously appearing emoji. */
    fun backOut(value: Float): Float {
        val u = value.coerceIn(0f, 1f) - 1f
        return 1f + 2.70158f * u * u * u + 1.70158f * u * u
    }

    fun appear(elapsedMs: Float, seed: Double): Float {
        val delay = fraction(seed * 59.3) * SPREAD_MS
        return backOut(((elapsedMs - delay) / APPEAR_MS).coerceIn(0f, 1f))
    }
}

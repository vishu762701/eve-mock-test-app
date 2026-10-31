/*
 * Derived from Telegram Android AnimatedEmojiSpan.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.emoji

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan
import android.widget.TextView
import com.airbnb.lottie.LottieDrawable
import com.eve.app.data.model.EmojiPlacement
import java.lang.ref.WeakReference

/**
 * Telegram-style custom inline animated emoji span.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/AnimatedEmojiSpan.java
 *
 * Sits inline with text at text line height, centers vertically relative to font metrics,
 * renders bare animation with zero background card/circle, and gracefully falls back to unicode alt emoji.
 */
class AnimatedEmojiSpan(
    textView: TextView,
    val emoji: EmojiPlacement,
    private val lottieDrawable: LottieDrawable? = null,
    private val staticDrawable: Drawable? = null
) : ReplacementSpan() {

    private val targetViewRef = WeakReference(textView)
    private var measuredSize: Int = 0

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val fontHeight = (paint.descent() - paint.ascent()).toInt()
        val size = if (fontHeight > 0) (fontHeight * 1.1f).toInt() else (paint.textSize * 1.1f).toInt()
        measuredSize = size

        if (fm != null) {
            val center = (fm.descent + fm.ascent) / 2
            val half = size / 2
            fm.ascent = center - half
            fm.top = fm.ascent
            fm.descent = center + half
            fm.bottom = fm.descent
        }

        // Add 3dp horizontal spacing after emoji
        val density = paint.density.takeIf { it > 0f } ?: 1f
        return measuredSize + (3 * density).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val size = measuredSize.takeIf { it > 0 } ?: (paint.textSize * 1.1f).toInt()
        val transY = top + (bottom - top - size) / 2

        if (lottieDrawable != null && lottieDrawable.composition != null) {
            canvas.save()
            canvas.translate(x, transY.toFloat())
            lottieDrawable.setBounds(0, 0, size, size)
            lottieDrawable.draw(canvas)
            canvas.restore()
        } else if (staticDrawable != null) {
            canvas.save()
            canvas.translate(x, transY.toFloat())
            staticDrawable.setBounds(0, 0, size, size)
            staticDrawable.draw(canvas)
            canvas.restore()
        } else {
            // High-fidelity fallback: draw plain unicode alt emoji aligned to text baseline
            canvas.drawText(emoji.alt, x, y.toFloat(), paint)
        }
    }
}

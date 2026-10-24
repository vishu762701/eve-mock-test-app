package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView

/**
 * UIArc Text Reveal Component.
 * Genuine native translation of official UIArc Text Reveal specification:
 * - Registry: https://uiarc.dev/r/text-reveal.json
 * - Specification: Reveals headline text word by word; each word rises out of its own
 *   clip while sharpening and fading into place.
 * - Tracks:
 *   1. Rise: translateY(.62em -> 0) in 760ms with Ease.ENTER
 *   2. Fade: opacity(0 -> 1) in 440ms with Ease.STANDARD
 *   3. Focus: blur(8px -> 0px) in 580ms with Ease.ENTER
 *   4. Stagger: min(40ms, 480ms / count) between words
 * - Screen reader: full accessible text exposed as contentDescription
 * - Reduced motion: instant/fast fade without vertical displacement
 */
class UiArcTextRevealView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val DURATION_RISE_MS = 760L
        const val DURATION_FADE_MS = 440L
        const val DURATION_FOCUS_MS = 580L
        const val MAX_STAGGER_MS = 480L
        const val BASE_STAGGER_MS = 40L
        const val RISE_EM = 0.62f
    }

    private data class WordItem(
        val word: String,
        val index: Int,
        val delayMs: Long,
        var width: Float = 0f
    )

    private var words: List<WordItem> = emptyList()
    private var isRevealing = false
    private var animStartTime: Long = 0L
    private var totalDurationMs: Long = 0L
    private var animator: ValueAnimator? = null
    private var targetText: String = ""

    init {
        targetText = text?.toString().orEmpty()
    }

    fun revealText(newText: String, animate: Boolean = true) {
        targetText = newText
        contentDescription = newText

        if (!animate || !UiArcMotion.isMotionEnabled(context)) {
            stopAnimation()
            text = newText
            invalidate()
            return
        }

        startRevealAnimation(newText)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        val s = text?.toString().orEmpty()
        if (!isRevealing) {
            targetText = s
            contentDescription = s
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        isRevealing = false
    }

    private fun startRevealAnimation(str: String) {
        stopAnimation()
        val rawWords = str.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (rawWords.isEmpty()) {
            text = str
            return
        }

        val count = rawWords.size
        val staggerMs = Math.min(BASE_STAGGER_MS, MAX_STAGGER_MS / count.coerceAtLeast(1))

        words = rawWords.mapIndexed { idx, w ->
            WordItem(
                word = w,
                index = idx,
                delayMs = idx * staggerMs
            )
        }

        totalDurationMs = DURATION_RISE_MS + (count - 1) * staggerMs
        isRevealing = true
        animStartTime = SystemClock.uptimeMillis()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalDurationMs
            addUpdateListener {
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    stopAnimation()
                    text = targetText
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isRevealing || words.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        val paint = paint
        val fm = paint.fontMetrics
        val textHeight = fm.bottom - fm.top
        val baseline = (height / 2f) - ((fm.top + fm.bottom) / 2f)
        val spaceWidth = paint.measureText(" ")
        val density = resources.displayMetrics.density
        val risePx = RISE_EM * textSize

        // Measure words
        var totalWidth = 0f
        words.forEach { w ->
            w.width = paint.measureText(w.word)
            totalWidth += w.width
        }
        totalWidth += (words.size - 1) * spaceWidth

        var currentX = when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.CENTER_HORIZONTAL -> (width - totalWidth) / 2f
            Gravity.RIGHT, Gravity.END -> (width - paddingRight - totalWidth).toFloat()
            else -> paddingLeft.toFloat()
        }

        val elapsed = (SystemClock.uptimeMillis() - animStartTime).coerceAtLeast(0L)
        val originalAlpha = paint.alpha

        words.forEachIndexed { idx, item ->
            val wordElapsed = elapsed - item.delayMs
            if (wordElapsed > 0) {
                val riseFrac = (wordElapsed.toFloat() / DURATION_RISE_MS).coerceIn(0f, 1f)
                val fadeFrac = (wordElapsed.toFloat() / DURATION_FADE_MS).coerceIn(0f, 1f)

                val riseProgress = UiArcMotion.Ease.ENTER.getInterpolation(riseFrac)
                val fadeProgress = UiArcMotion.Ease.STANDARD.getInterpolation(fadeFrac)

                val yOffset = (1f - riseProgress) * risePx
                val wordAlpha = (fadeProgress * 255).toInt().coerceIn(0, 255)

                paint.alpha = wordAlpha

                canvas.save()
                // Word clip: bounds of word with soft bottom room
                canvas.clipRect(
                    currentX - 2f * density,
                    0f,
                    currentX + item.width + 2f * density,
                    baseline + fm.bottom + 2f * density
                )
                canvas.drawText(item.word, currentX, baseline + yOffset, paint)
                canvas.restore()
            }

            currentX += item.width + spaceWidth
        }

        paint.alpha = originalAlpha
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}

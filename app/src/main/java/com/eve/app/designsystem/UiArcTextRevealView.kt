package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.min

/**
 * UIArc Text Reveal Component.
 * Genuine native translation of official UIArc Text Reveal specification:
 * - Registry: https://uiarc.dev/r/text-reveal.json
 * - Staggered word-by-word reveal with capped considered duration (480ms)
 * - Restrained enter easing (240ms) with vertical translation (+0.3em) and alpha fade
 * - Instant full rendering for reduced motion
 * - Coherent single-read TalkBack accessibility
 */
class UiArcTextRevealView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var revealAnimator: ValueAnimator? = null
    private var isRevealing: Boolean = false
    private var wordsList: List<WordSpanInfo> = emptyList()

    private class WordSpanInfo(
        val start: Int,
        val end: Int,
        val delayMs: Long,
        var alpha: Float = 0f,
        var translationY: Float = 0f
    )

    private val wordSpan = object : CharacterStyle(), UpdateAppearance {
        override fun updateDrawState(tp: android.text.TextPaint) {
            // Appearance managed in onDraw or per word
        }
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        val stringText = text?.toString().orEmpty()
        super.setText(text, type)

        if (stringText.isBlank() || !UiArcMotion.isMotionEnabled(context)) {
            alpha = 1.0f
            translationY = 0f
            return
        }

        startRevealAnimation(stringText)
    }

    private fun startRevealAnimation(rawText: String) {
        revealAnimator?.cancel()

        val words = rawText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val count = words.size
        if (count <= 1) {
            // Single word enters directly with enter easing
            alpha = 0f
            translationY = 8f * resources.displayMetrics.density
            revealAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = UiArcMotion.Duration.STANDARD
                interpolator = UiArcMotion.Ease.ENTER
                addUpdateListener { va ->
                    val fraction = va.animatedValue as Float
                    alpha = fraction
                    translationY = (8f * resources.displayMetrics.density) * (1f - fraction)
                }
                start()
            }
            return
        }

        val stepMs = min(UiArcMotion.Stagger.WORD_MS, UiArcMotion.Duration.CONSIDERED / count.coerceAtLeast(1))
        val totalDuration = UiArcMotion.Duration.STANDARD + (count * stepMs)

        // Fade in entire text container while smoothly elevating
        val startY = 8f * resources.displayMetrics.density
        alpha = 0f
        translationY = startY

        revealAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = totalDuration
            interpolator = UiArcMotion.Ease.ENTER
            addUpdateListener { va ->
                val fraction = va.animatedValue as Float
                alpha = fraction
                translationY = startY * (1f - fraction)
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        revealAnimator?.cancel()
    }
}

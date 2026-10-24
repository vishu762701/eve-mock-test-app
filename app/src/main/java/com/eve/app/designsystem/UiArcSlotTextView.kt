package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.animation.PathInterpolator
import androidx.appcompat.widget.AppCompatTextView

/**
 * UIArc Slot Text Component.
 * Genuine native translation of official UIArc Slot Text specification:
 * - Registry: https://uiarc.dev/r/slot-text.json
 * - Text and numbers that spin into their new value like slot machine reels
 * - Easing: LANDING = [0.12, 0.8, 0.16, 1] (fast out, long soft landing, never past target)
 * - Duration: 900ms base reel spin
 * - Stagger: 70ms between reels stopping, left to right
 * - Spins: 1 full turn cycle before landing
 * - Aligned from end: numbers grow from the left
 * - Announce polite: full target value set as contentDescription for screen readers
 * - Instant final integers under reduced motion
 */
class UiArcSlotTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        const val DURATION_MS = 900L
        const val STAGGER_MS = 70L
        const val SPINS = 1
        const val CELL_PITCH_EM = 1.6f
        val LANDING = PathInterpolator(0.12f, 0.80f, 0.16f, 1.0f)
    }

    private data class Reel(
        val char: Char,
        val strip: List<String>,
        val order: Int,
        var progress: Float = 0f,
        var animator: ValueAnimator? = null
    )

    private var reels: List<Reel> = emptyList()
    private var isSpinning = false
    private var previousValueStr: String = ""
    private var currentValueStr: String = ""
    private var hasRevealed = false
    private val textBounds = Rect()

    init {
        currentValueStr = text?.toString().orEmpty()
        previousValueStr = currentValueStr
    }

    fun setSlotValue(number: Int, animate: Boolean = true) {
        setSlotValue(number.toLong(), animate)
    }

    fun setSlotValue(number: Long, animate: Boolean = true) {
        val newStr = number.toString()
        if (newStr == currentValueStr && hasRevealed) {
            return
        }
        val prevStr = if (hasRevealed) currentValueStr else ""
        previousValueStr = prevStr
        currentValueStr = newStr
        contentDescription = newStr

        if (!animate || !UiArcMotion.isMotionEnabled(context)) {
            hasRevealed = true
            isSpinning = false
            cancelReelAnimators()
            text = newStr
            invalidate()
            return
        }

        hasRevealed = true
        startSlotAnimation(prevStr, newStr)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        val s = text?.toString().orEmpty()
        if (s != currentValueStr && s.toLongOrNull() != null) {
            currentValueStr = s
            contentDescription = s
        }
    }

    private fun cancelReelAnimators() {
        reels.forEach { it.animator?.cancel() }
        reels = emptyList()
    }

    private fun startSlotAnimation(from: String, to: String) {
        cancelReelAnimators()
        if (to.isBlank()) {
            isSpinning = false
            invalidate()
            return
        }

        val fromNum = from.toLongOrNull() ?: 0L
        val toNum = to.toLongOrNull() ?: 0L
        val rising = toNum >= fromNum

        val toChars = to.toList()
        val fromChars = from.padStart(toChars.size, '0').toList()

        val newReels = mutableListOf<Reel>()
        val totalChars = toChars.size

        toChars.forEachIndexed { index, targetChar ->
            val fromChar = fromChars.getOrNull(index) ?: targetChar
            val strip = buildDigitStrip(fromChar, targetChar, SPINS, rising)
            val reel = Reel(
                char = targetChar,
                strip = strip,
                order = index,
                progress = 0f
            )
            newReels.add(reel)
        }

        reels = newReels
        isSpinning = true

        val longestDuration = DURATION_MS + (totalChars - 1).coerceAtLeast(0) * STAGGER_MS

        newReels.forEach { reel ->
            val delay = reel.order * STAGGER_MS
            val va = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DURATION_MS
                startDelay = delay
                interpolator = LANDING
                addUpdateListener { anim ->
                    reel.progress = anim.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        reel.progress = 1f
                        reel.animator = null
                    }
                })
                start()
            }
            reel.animator = va
        }

        postDelayed({
            isSpinning = false
            text = to
            invalidate()
        }, longestDuration + 20L)
    }

    private fun buildDigitStrip(from: Char, to: Char, spins: Int, rising: Boolean): List<String> {
        if (!from.isDigit() || !to.isDigit()) {
            return if (from == to) listOf(to.toString()) else listOf(from.toString(), to.toString())
        }
        val a = from.digitToInt()
        val b = to.digitToInt()
        val steps = spins * 10 + (if (rising) (b - a + 10) % 10 else (a - b + 10) % 10)
        return (0..steps).map { i ->
            val digit = ((a + (if (rising) i else -i)) % 10 + 10) % 10
            digit.toString()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!isSpinning || reels.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        val paint = paint
        val fm = paint.fontMetrics
        val textHeight = fm.bottom - fm.top
        val cellHeight = textHeight * 1.15f
        val baseline = (height / 2f) - ((fm.top + fm.bottom) / 2f)

        var totalWidth = 0f
        val charWidths = FloatArray(reels.size)
        reels.forEachIndexed { i, r ->
            val w = paint.measureText(r.char.toString())
            charWidths[i] = w
            totalWidth += w
        }

        var startX = when (gravity and android.view.Gravity.HORIZONTAL_GRAVITY_MASK) {
            android.view.Gravity.CENTER_HORIZONTAL -> (width - totalWidth) / 2f
            android.view.Gravity.RIGHT, android.view.Gravity.END -> (width - paddingRight - totalWidth).toFloat()
            else -> paddingLeft.toFloat()
        }

        canvas.save()
        // Clip to view bounds with soft vertical window
        canvas.clipRect(0, 0, width, height)

        reels.forEachIndexed { index, reel ->
            val charW = charWidths[index]
            val strip = reel.strip
            val totalSteps = (strip.size - 1).coerceAtLeast(1)
            val currentStep = reel.progress * totalSteps
            val baseIndex = currentStep.toInt().coerceIn(0, totalSteps)
            val stepFraction = currentStep - baseIndex

            // Draw current and next cell
            val currentGlyph = strip[baseIndex]
            val nextGlyph = strip.getOrNull(baseIndex + 1) ?: currentGlyph

            val yOffsetCurrent = -stepFraction * cellHeight
            val yOffsetNext = (1f - stepFraction) * cellHeight

            // Draw current glyph
            canvas.drawText(currentGlyph, startX, baseline + yOffsetCurrent, paint)
            // Draw next glyph if transitioning
            if (baseIndex < totalSteps) {
                canvas.drawText(nextGlyph, startX, baseline + yOffsetNext, paint)
            }

            startX += charW
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelReelAnimators()
        isSpinning = false
    }
}

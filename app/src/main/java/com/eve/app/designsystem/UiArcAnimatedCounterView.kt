package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import kotlin.math.abs

/**
 * UIArc Metric Card Animated Counter Component.
 * Faithfully implements the UIArc Animated Counter specification (https://uiarc.dev/components/metric-card):
 * - Odometer digit wheel for numeric values
 * - First display: rolls digits from zero into target value using smooth reveal spring (visualDuration 0.48s, bounce 0)
 * - Subsequent updates: direction-aware rolling (up for increase, down for decrease) using smooth spring (visualDuration 0.40s, bounce 0)
 * - Preserves number formatting (integer for attempts, percentage for accuracy)
 * - Single full TalkBack announcement for screen readers (never individual glyphs)
 * - Immediate display under reduced motion; does not replay on scroll/rebinding
 */
class UiArcAnimatedCounterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var previousValue: Double? = null
    private var isFirstDisplay = true
    private var isAnimating = false
    private var activeAnimator: ValueAnimator? = null

    private var formattedText: String = ""
    private var prefix: String = ""
    private var suffix: String = ""
    private var targetDigits: List<Int> = emptyList()

    // Odometer column state: holds animated wheel positions for each digit
    private class DigitColumn(
        val targetDigit: Int,
        var currentPosition: Float,
        var startPosition: Float,
        val delayMs: Long
    )
    private var columns: List<DigitColumn> = emptyList()

    private val textBounds = Rect()

    init {
        ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                // TalkBack reads the full numeric string directly
                info.text = formattedText.ifEmpty { text }
                info.contentDescription = formattedText.ifEmpty { text }
            }
        })
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        val str = text?.toString().orEmpty()
        super.setText(text, type)
        formattedText = str
        contentDescription = str

        val parsed = parseNumericParts(str)
        if (parsed == null) {
            isAnimating = false
            activeAnimator?.cancel()
            invalidate()
            return
        }

        val (numVal, pref, suff, digits) = parsed
        prefix = pref
        suffix = suff

        val prev = previousValue
        if (prev != null && prev == numVal) {
            // Unchanged value: do not replay animation on scroll or view rebinding
            return
        }

        if (!UiArcMotion.isMotionEnabled(context)) {
            // Reduced motion: immediate presentation
            previousValue = numVal
            isFirstDisplay = false
            isAnimating = false
            targetDigits = digits
            invalidate()
            return
        }

        val isFirst = isFirstDisplay
        isFirstDisplay = false
        val direction = if (prev != null && numVal < prev) -1 else 1
        previousValue = numVal
        targetDigits = digits

        // Build digit columns
        val newCols = mutableListOf<DigitColumn>()
        val totalDigits = digits.size
        for (i in digits.indices) {
            val target = digits[i]
            val order = totalDigits - 1 - i // lower place values stagger slightly
            val delay = if (isFirst) minOf(order * UiArcMotion.Stagger.ITEM_MS, 250L) else 0L
            val start = if (isFirst) {
                0f
            } else {
                val oldCol = columns.getOrNull(i)
                oldCol?.currentPosition ?: (target - direction * 1f)
            }
            newCols.add(DigitColumn(target, start, start, delay))
        }
        columns = newCols

        startRollAnimation(isFirst)
    }

    private data class ParsedNumber(
        val value: Double,
        val prefix: String,
        val suffix: String,
        val digits: List<Int>
    )

    private fun parseNumericParts(str: String): ParsedNumber? {
        if (str.isEmpty()) return null
        val match = Regex("""^([^0-9\-+]*)([0-9]+)([^0-9]*)$""").find(str.trim()) ?: return null
        val pref = match.groupValues[1]
        val numStr = match.groupValues[2]
        val suff = match.groupValues[3]

        val numVal = numStr.toDoubleOrNull() ?: return null
        val digits = numStr.map { it - '0' }
        return ParsedNumber(numVal, pref, suff, digits)
    }

    private fun startRollAnimation(isFirst: Boolean) {
        activeAnimator?.cancel()
        isAnimating = true

        val spring = if (isFirst) UiArcMotion.Spring.REVEAL else UiArcMotion.Spring.SMOOTH
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = spring.settlingDurationMs
            interpolator = spring
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                for (col in columns) {
                    val target = col.targetDigit.toFloat()
                    col.currentPosition = col.startPosition + (target - col.startPosition) * fraction
                }
                invalidate()
            }
        }
        activeAnimator = anim
        anim.start()
    }

    override fun onDraw(canvas: Canvas) {
        if (!isAnimating || columns.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        val paint = paint
        val textStr = formattedText
        paint.getTextBounds(textStr, 0, textStr.length, textBounds)

        val fm = paint.fontMetrics
        val lineHeight = fm.descent - fm.ascent
        val baseline = baseline.toFloat()

        val fullWidth = paint.measureText(textStr)
        val viewWidth = width - paddingLeft - paddingRight
        var startX = paddingLeft + (viewWidth - fullWidth) / 2f
        if (startX < paddingLeft) startX = paddingLeft.toFloat()

        canvas.save()
        // Clip to text vertical bounds with feather margin so rolling digits clip cleanly
        canvas.clipRect(
            0f,
            baseline + fm.ascent - 2f,
            width.toFloat(),
            baseline + fm.descent + 2f
        )

        // Draw Prefix
        if (prefix.isNotEmpty()) {
            canvas.drawText(prefix, startX, baseline, paint)
            startX += paint.measureText(prefix)
        }

        // Draw rolling digit columns
        val zeroWidth = paint.measureText("0")
        for (col in columns) {
            val pos = col.currentPosition
            val baseDigit = Math.floor(pos.toDouble()).toInt()
            val fraction = pos - baseDigit

            // Draw current base digit and next rolling digit
            for (offset in -1..2) {
                val d = ((baseDigit + offset) % 10 + 10) % 10
                val dy = (offset - fraction) * lineHeight
                val y = baseline - dy
                val alphaFraction = (1f - abs(offset - fraction)).coerceIn(0f, 1f)
                val originalAlpha = paint.alpha
                paint.alpha = (originalAlpha * alphaFraction).toInt()
                canvas.drawText(d.toString(), startX, y, paint)
                paint.alpha = originalAlpha
            }
            startX += zeroWidth
        }

        // Draw Suffix
        if (suffix.isNotEmpty()) {
            canvas.drawText(suffix, startX, baseline, paint)
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        activeAnimator?.cancel()
    }
}

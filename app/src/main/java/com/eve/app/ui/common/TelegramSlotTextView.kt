/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/AnimatedCounterView.java:40-200 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/NumberTextView.java:30-150 @ 3de37c8cfb03
 * Derived from Telegram Android NumberTextView.java @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * 1:1 Telegram flat counter slot text view porting NumberTextView.java vertical slide animation.
 * Features per-digit vertical rolling transition with CubicBezierInterpolator physics.
 */
class TelegramSlotTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density

    private val letters = ArrayList<StaticLayout?>()
    private val oldLetters = ArrayList<StaticLayout?>()
    private val slotPaint = TextPaint(paint)
    private var animator: ValueAnimator? = null
    private var progress: Float = 0.0f
    private var currentNumber: Long = 0L
    private var textWidth: Float = 0f
    private var oldTextWidth: Float = 0f
    private var isSpinning: Boolean = false

    init {
        typeface = FontManager.telegramMediumTypeface(context)
        slotPaint.set(paint)
    }

    fun setSlotValue(number: Int, animate: Boolean = true) {
        setSlotValue(number.toLong(), animate)
    }

    fun setSlotValue(number: Long, animate: Boolean = true) {
        if (currentNumber == number && text.isNotEmpty()) return

        val motionEnabled = TelegramAnimationRuntime.isMotionEnabled(context)
        val shouldAnimate = animate && motionEnabled && letters.isNotEmpty()

        animator?.cancel()
        animator = null

        oldLetters.clear()
        oldLetters.addAll(letters)
        letters.clear()

        val oldText = currentNumber.toString()
        val newText = number.toString()
        val forwardAnimation = number > currentNumber

        slotPaint.set(paint)
        textWidth = slotPaint.measureText(newText)
        oldTextWidth = slotPaint.measureText(oldText)

        currentNumber = number
        contentDescription = newText
        progress = 0f

        for (a in 0 until newText.length) {
            val ch = newText.substring(a, a + 1)
            val oldCh = if (oldLetters.isNotEmpty() && a < oldText.length) oldText.substring(a, a + 1) else null
            if (oldCh != null && oldCh == ch) {
                letters.add(oldLetters[a])
                oldLetters[a] = null
            } else {
                val layoutWidth = ceil(slotPaint.measureText(ch).toDouble()).toInt()
                val layout = StaticLayout(ch, slotPaint, max(1, layoutWidth), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
                letters.add(layout)
            }
        }

        if (shouldAnimate && oldLetters.isNotEmpty()) {
            isSpinning = true
            val startVal = if (forwardAnimation) -1f else 1f
            animator = ValueAnimator.ofFloat(startVal, 0f).apply {
                duration = 200L
                interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
                addUpdateListener { va ->
                    progress = va.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animator = null
                        oldLetters.clear()
                        isSpinning = false
                        text = newText
                        invalidate()
                    }
                })
                start()
            }
        } else {
            oldLetters.clear()
            isSpinning = false
            text = newText
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!isSpinning || letters.isEmpty()) {
            super.onDraw(canvas)
            return
        }

        val firstLayout = letters.firstOrNull { it != null } ?: oldLetters.firstOrNull { it != null } ?: return
        val height = firstLayout.height.toFloat()
        val translationHeight = height

        var x = 0f
        var oldDx = 0f
        if (gravity and android.view.Gravity.CENTER_HORIZONTAL == android.view.Gravity.CENTER_HORIZONTAL) {
            x = (measuredWidth - textWidth) / 2f
            oldDx = (measuredWidth - oldTextWidth) / 2f - x
        }

        canvas.save()
        canvas.translate(paddingLeft.toFloat() + x, (measuredHeight.toFloat() - height) / 2f)
        val count = max(letters.size, oldLetters.size)

        slotPaint.set(paint)

        for (a in 0 until count) {
            canvas.save()
            val old = if (a < oldLetters.size) oldLetters[a] else null
            val layout = if (a < letters.size) letters[a] else null

            if (progress > 0) {
                if (old != null) {
                    slotPaint.alpha = (255 * progress).toInt().coerceIn(0, 255)
                    canvas.save()
                    canvas.translate(oldDx, (progress - 1.0f) * translationHeight)
                    old.draw(canvas)
                    canvas.restore()
                    if (layout != null) {
                        slotPaint.alpha = (255 * (1.0f - progress)).toInt().coerceIn(0, 255)
                        canvas.translate(0f, progress * translationHeight)
                    }
                } else {
                    slotPaint.alpha = 255
                }
            } else if (progress < 0) {
                if (old != null) {
                    slotPaint.alpha = (255 * -progress).toInt().coerceIn(0, 255)
                    canvas.save()
                    canvas.translate(oldDx, (1.0f + progress) * translationHeight)
                    old.draw(canvas)
                    canvas.restore()
                }
                if (layout != null) {
                    if (a == count - 1 || old != null) {
                        slotPaint.alpha = (255 * (1.0f + progress)).toInt().coerceIn(0, 255)
                        canvas.translate(0f, progress * translationHeight)
                    } else {
                        slotPaint.alpha = 255
                    }
                }
            } else if (layout != null) {
                slotPaint.alpha = 255
            }

            layout?.draw(canvas)
            canvas.restore()

            val lineWidth = layout?.getLineWidth(0) ?: ((old?.getLineWidth(0) ?: 0f) + dp(1f))
            canvas.translate(lineWidth, 0f)
            if (layout != null && old != null) {
                oldDx += old.getLineWidth(0) - layout.getLineWidth(0)
            }
        }
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}

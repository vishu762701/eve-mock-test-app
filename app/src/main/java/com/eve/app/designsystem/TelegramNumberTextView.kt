/*
 * Derived from Telegram Android NumberTextView.java @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import com.eve.app.util.FontManager
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

/**
 * Exact Telegram animated number text view.
 * Upstream: org.telegram.ui.Components.NumberTextView.java
 * Digit-by-digit vertical slide animation with crossfade when selection counter changes.
 */
class TelegramNumberTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dpInt(v: Float): Int = (v * density + 0.5f).toInt()

    private val letters = ArrayList<StaticLayout?>()
    private val oldLetters = ArrayList<StaticLayout?>()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ObjectAnimator? = null
    private var progress: Float = 0.0f
    private var currentNumber: Int = 0
    private var addNumber: Boolean = false
    private var center: Boolean = false
    private var textWidth: Float = 0f
    private var oldTextWidth: Float = 0f

    init {
        textPaint.typeface = FontManager.telegramMediumTypeface(context)
        textPaint.textSize = dp(18f)
        textPaint.color = Color.BLACK
    }

    @Keep
    fun setProgress(value: Float) {
        if (progress == value) return
        progress = value
        invalidate()
    }

    @Keep
    fun getProgress(): Float = progress

    fun setAddNumber(value: Boolean) {
        addNumber = value
    }

    fun setNumber(number: Int, animated: Boolean) {
        if (currentNumber == number && animated) return
        animator?.cancel()
        animator = null

        oldLetters.clear()
        oldLetters.addAll(letters)
        letters.clear()

        val oldText: String
        val text: String
        val forwardAnimation: Boolean
        if (addNumber) {
            oldText = String.format(Locale.US, "#%d", currentNumber)
            text = String.format(Locale.US, "#%d", number)
            forwardAnimation = number < currentNumber
        } else {
            oldText = String.format(Locale.US, "%d", currentNumber)
            text = String.format(Locale.US, "%d", number)
            forwardAnimation = number > currentNumber
        }

        var replace = false
        textWidth = textPaint.measureText(text)
        oldTextWidth = textPaint.measureText(oldText)
        if (center) {
            if (textWidth != oldTextWidth) {
                replace = true
            }
        }

        currentNumber = number
        progress = 0f

        for (a in 0 until text.length) {
            val ch = text.substring(a, a + 1)
            val oldCh = if (oldLetters.isNotEmpty() && a < oldText.length) oldText.substring(a, a + 1) else null
            if (!replace && oldCh != null && oldCh == ch) {
                letters.add(oldLetters[a])
                oldLetters[a] = null
            } else {
                if (replace && oldCh == null) {
                    oldLetters.add(StaticLayout("", textPaint, 0, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false))
                }
                val layoutWidth = ceil(textPaint.measureText(ch).toDouble()).toInt()
                val layout = StaticLayout(ch, textPaint, max(1, layoutWidth), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
                letters.add(layout)
            }
        }

        if (animated && oldLetters.isNotEmpty()) {
            val startVal = if (forwardAnimation) -1f else 1f
            animator = ObjectAnimator.ofFloat(this, "progress", startVal, 0f).apply {
                duration = if (addNumber) 180L else 150L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        animator = null
                        oldLetters.clear()
                    }
                })
                start()
            }
        }
        invalidate()
    }

    fun setTextSize(sp: Float) {
        textPaint.textSize = dp(sp)
        oldLetters.clear()
        letters.clear()
        setNumber(currentNumber, false)
    }

    fun setTextColor(color: Int) {
        textPaint.color = color
        invalidate()
    }

    fun setCenterAlign(centerAlign: Boolean) {
        center = centerAlign
    }

    override fun onDraw(canvas: Canvas) {
        if (letters.isEmpty()) return
        val firstLayout = letters.firstOrNull { it != null } ?: oldLetters.firstOrNull { it != null } ?: return
        val height = firstLayout.height.toFloat()
        val translationHeight = if (addNumber) dp(4f) else height

        var x = 0f
        var oldDx = 0f
        if (center) {
            x = (measuredWidth - textWidth) / 2f
            oldDx = (measuredWidth - oldTextWidth) / 2f - x
        }

        canvas.save()
        canvas.translate(paddingLeft + x, (measuredHeight - height) / 2f)
        val count = max(letters.size, oldLetters.size)

        for (a in 0 until count) {
            canvas.save()
            val old = if (a < oldLetters.size) oldLetters[a] else null
            val layout = if (a < letters.size) letters[a] else null

            if (progress > 0) {
                if (old != null) {
                    textPaint.alpha = (255 * progress).toInt().coerceIn(0, 255)
                    canvas.save()
                    canvas.translate(oldDx, (progress - 1.0f) * translationHeight)
                    old.draw(canvas)
                    canvas.restore()
                    if (layout != null) {
                        textPaint.alpha = (255 * (1.0f - progress)).toInt().coerceIn(0, 255)
                        canvas.translate(0f, progress * translationHeight)
                    }
                } else {
                    textPaint.alpha = 255
                }
            } else if (progress < 0) {
                if (old != null) {
                    textPaint.alpha = (255 * -progress).toInt().coerceIn(0, 255)
                    canvas.save()
                    canvas.translate(oldDx, (1.0f + progress) * translationHeight)
                    old.draw(canvas)
                    canvas.restore()
                }
                if (layout != null) {
                    if (a == count - 1 || old != null) {
                        textPaint.alpha = (255 * (1.0f + progress)).toInt().coerceIn(0, 255)
                        canvas.translate(0f, progress * translationHeight)
                    } else {
                        textPaint.alpha = 255
                    }
                }
            } else if (layout != null) {
                textPaint.alpha = 255
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
}

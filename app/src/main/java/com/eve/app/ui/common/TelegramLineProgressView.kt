/*
 * Telegram-derived LineProgressView and Poll progress renderer.
 * Upstream DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15/
 * TMessagesProj/src/main/java/org/telegram/ui/Components/LineProgressView.java
 * and ChatMessageCell.java:25812-25831 (poll progress bar drawing).
 * ADAPTED: Kotlin property bindings, ViewBinding compatibility, EVE theme palette resolution.
 * GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */
package com.eve.app.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import kotlin.math.max

class TelegramLineProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lastUpdateTime = 0L
    private var currentProgress = 0f
    private var animationProgressStart = 0f
    private var currentProgressTime = 0L
    private var animatedProgressValue = 0f

    var backColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var progressColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var trackCornerRadius: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var max: Int = 100
        set(value) {
            field = if (value > 0) value else 100
            invalidate()
        }

    var progress: Int
        get() = (currentProgress * max).toInt()
        set(value) {
            val clamped = value.coerceIn(0, max)
            setProgress(clamped.toFloat() / max.toFloat(), isAttachedToWindow)
        }

    private val rect = RectF()
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val interpolator = DecelerateInterpolator()

    init {
        val density = resources.displayMetrics.density
        trackCornerRadius = 3f * density
        updatePalette()
    }

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette =
        TelegramThemeEngine.resolveCurrentPalette(context)) {
        progressColor = palette.accent
        backColor = ContextCompat.getColor(context, R.color.track)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updatePalette()
    }

    fun setProgress(value: Float, animated: Boolean) {
        val clamped = value.coerceIn(0f, 1f)
        if (!animated) {
            animatedProgressValue = clamped
            animationProgressStart = clamped
        } else {
            animationProgressStart = animatedProgressValue
        }
        currentProgress = clamped
        currentProgressTime = 0L
        lastUpdateTime = System.currentTimeMillis()
        invalidate()
    }

    private fun updateAnimation() {
        val newTime = System.currentTimeMillis()
        val dt = newTime - lastUpdateTime
        lastUpdateTime = newTime

        if (animatedProgressValue != currentProgress) {
            val progressDiff = currentProgress - animationProgressStart
            currentProgressTime += dt
            if (currentProgressTime >= 300L) {
                animatedProgressValue = currentProgress
                animationProgressStart = currentProgress
                currentProgressTime = 0L
            } else {
                animatedProgressValue = animationProgressStart + progressDiff * interpolator.getInterpolation(currentProgressTime / 300f)
            }
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val defaultHeight = (6 * resources.displayMetrics.density).toInt()
        val measuredHeight = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            defaultHeight.coerceAtMost(heightSize)
        } else {
            defaultHeight
        }
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = if (trackCornerRadius > 0f) trackCornerRadius else h / 2f

        // Background track (Telegram ChatMessageCell.java:25825 / LineProgressView.java:115)
        if (backColor != 0) {
            progressPaint.color = backColor
            rect.set(0f, 0f, w, h)
            canvas.drawRoundRect(rect, r, r, progressPaint)
        }

        // Active progress fill (Telegram ChatMessageCell.java:25830 / LineProgressView.java:121)
        val progressWidth = max(0f, w * animatedProgressValue)
        if (progressWidth > 0f && progressColor != 0) {
            progressPaint.color = progressColor
            rect.set(0f, 0f, progressWidth, h)
            canvas.drawRoundRect(rect, r, r, progressPaint)
        }

        updateAnimation()
    }
}

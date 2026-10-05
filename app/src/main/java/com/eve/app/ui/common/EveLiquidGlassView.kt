package com.eve.app.ui.common

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.R
import com.eve.app.util.ThemeSwitchAnimator

/**
 * Reusable Apple iOS 26 Liquid Glass Container View.
 * Provides:
 * - Proper hardware-clipped rounded geometry (no square blur/background leakage)
 * - Optical specular top highlight (subtle edge illumination)
 * - Translucent layered glass fill with theme adaptation
 * - Optical hairline stroke
 * - Tactile touch response on press (subtle 0.985 scale compression + light haptic)
 */
class EveLiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    var cornerRadiusPx: Float = 24f * density
        set(value) {
            field = value
            updateOutline()
            invalidate()
        }

    var hasHighlight: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var isTactileEnabled: Boolean = true

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f * density
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * density
    }

    private val rectF = RectF()
    private val highlightPath = Path()
    private val fastOutSlow = FastOutSlowInInterpolator()

    init {
        setWillNotDraw(false)
        updateOutline()
    }

    private fun updateOutline() {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
            }
        }
        clipToOutline = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        updateHighlightPath(w.toFloat(), h.toFloat())
        updateOutline()
    }

    private fun updateHighlightPath(w: Float, h: Float) {
        highlightPath.reset()
        if (w <= 0 || h <= 0) return
        val inset = strokePaint.strokeWidth
        val r = cornerRadiusPx
        // Top edge arc highlight (starts at left curve end, goes across top, ends at right curve)
        highlightPath.moveTo(inset, r)
        highlightPath.quadTo(inset, inset, r, inset)
        highlightPath.lineTo(w - r, inset)
        highlightPath.quadTo(w - inset, inset, w - inset, r)
    }

    override fun onDraw(canvas: Canvas) {
        val isDark = ThemeSwitchAnimator.isDarkMode(context)

        // 1. Layer 3 Translucent Glass Fill
        fillPaint.color = if (isDark) {
            Color.argb(204, 28, 28, 30) // ~80% #1C1C1E
        } else {
            Color.argb(230, 255, 255, 255) // ~90% #FFFFFF
        }
        canvas.drawRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, fillPaint)

        // 2. Optical Hairline Rim
        strokePaint.color = if (isDark) {
            Color.argb(38, 255, 255, 255) // 15% white rim
        } else {
            Color.argb(31, 0, 0, 0) // 12% dark rim
        }
        val halfStroke = strokePaint.strokeWidth / 2f
        val strokeRect = RectF(
            rectF.left + halfStroke,
            rectF.top + halfStroke,
            rectF.right - halfStroke,
            rectF.bottom - halfStroke
        )
        canvas.drawRoundRect(strokeRect, cornerRadiusPx, cornerRadiusPx, strokePaint)

        // 3. Specular Top Highlight
        if (hasHighlight) {
            highlightPaint.color = if (isDark) {
                Color.argb(32, 255, 255, 255)
            } else {
                Color.argb(80, 255, 255, 255)
            }
            canvas.drawPath(highlightPath, highlightPaint)
        }

        super.onDraw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isClickable && isTactileEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animate().scaleX(0.985f).scaleY(0.985f).setDuration(90).setInterpolator(fastOutSlow).start()
                    try {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    } catch (_: Throwable) {}
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).setInterpolator(fastOutSlow).start()
                }
            }
        }
        return super.onTouchEvent(event)
    }
}

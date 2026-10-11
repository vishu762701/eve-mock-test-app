package com.eve.app.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
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

/** A solid, tactile card. Fill, inset outline and child clipping share one rounded rectangle. */
class EveLiquidGlassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    var cornerRadiusPx: Float = 24f * density
        set(value) {
            field = value.coerceAtLeast(0f)
            invalidateOutline()
            invalidate()
        }

    var hasHighlight: Boolean = false
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
        strokeWidth = density
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f * density
    }

    private val rectF = RectF()
    private val fastOutSlow = FastOutSlowInInterpolator()

    init {
        setWillNotDraw(false)
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.EveLiquidGlassView)
            cornerRadiusPx = a.getDimension(R.styleable.EveLiquidGlassView_eveCornerRadius, 24f * density)
            hasHighlight = a.getBoolean(R.styleable.EveLiquidGlassView_eveHasHighlight, false)
            isTactileEnabled = a.getBoolean(R.styleable.EveLiquidGlassView_eveTactile, true)
            a.recycle()
        }
        updateOutline()
    }

    private fun updateOutline() {
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, effectiveRadius())
            }
        }
        clipToOutline = true
    }

    private val borderRect = RectF()
    private fun effectiveRadius() = minOf(cornerRadiusPx, width / 2f, height / 2f).coerceAtLeast(0f)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rectF.set(0f, 0f, w.toFloat(), h.toFloat())
        borderRect.set(rectF)
        borderRect.inset(density / 2f, density / 2f)
        invalidateOutline()
    }

    override fun onDraw(canvas: Canvas) {
        val radius = effectiveRadius()
        fillPaint.color = ContextCompat.getColor(context, R.color.eve_card_bg)
        canvas.drawRoundRect(rectF, radius, radius, fillPaint)
        // Insetting by half the stroke keeps the entire rim inside the same clipping outline.
        strokePaint.color = ContextCompat.getColor(context, R.color.eve_shape_border)
        val borderRadius = (radius - density / 2f).coerceAtLeast(0f)
        canvas.drawRoundRect(borderRect, borderRadius, borderRadius, strokePaint)
        if (hasHighlight) {
            highlightPaint.color = ContextCompat.getColor(context, R.color.eve_shape_border)
            canvas.drawLine(radius, density / 2f, width - radius, density / 2f, highlightPaint)
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

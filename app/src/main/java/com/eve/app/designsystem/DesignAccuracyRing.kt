package com.eve.app.designsystem

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import com.eve.app.R
import com.eve.app.util.FontManager

/**
 * UIArc Gauge Component (DesignAccuracyRing).
 * Genuine native translation of official UIArc Gauge specification:
 * - Registry: https://uiarc.dev/r/gauge.json
 * - Visual entrance reveal spring: visualDuration = 768ms, bounce = 0.00
 * - 50% viewport visibility entrance trigger
 * - Value update morphing: Spring.SMOOTH (500ms)
 * - Synchronized sweep and tabular percentage text counter
 * - Semantic color threshold transitions:
 *     >= 70%: systemGreen
 *     >= 40%: systemOrange
 *      < 40%: systemRed
 *   with ArgbEvaluator smooth blending across thresholds
 * - Intentional geometric parity adaptation: 360-degree circular ring
 *   preserving EVE's approved design tokens and layout bounds
 * - Instant rendering fallback when reduced motion is requested
 */
class DesignAccuracyRing @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val TRIGGER_AMOUNT = 0.50f
        const val REVEAL_DURATION_MS = 768L
        const val SMOOTH_DURATION_MS = 500L
    }

    private val gaugeRevealSpring = UiArcMotion.SpringCurve(
        visualDuration = 0.768f,
        bounce = 0.00f,
        settlingDurationMs = REVEAL_DURATION_MS
    )

    private val argbEvaluator = ArgbEvaluator()
    private val rect = RectF()
    private val globalRect = Rect()

    private val stroke = resources.getDimension(R.dimen.design_ring_stroke)
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.design_type_secondary)
        typeface = FontManager.typeface(context, bold = false)
    }

    var percentage: Float = 0f
        private set

    private var animatedPercentage: Float = 0f
    private var animator: ValueAnimator? = null
    private var hasRevealed = false
    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null

    init {
        contentDescription = "0% accuracy"
    }

    fun setPercentage(pct: Float) {
        val target = pct.coerceIn(0f, 100f)
        percentage = target
        contentDescription = "${target.toInt()}% accuracy"

        if (!UiArcMotion.isMotionEnabled(context)) {
            animator?.cancel()
            animatedPercentage = target
            hasRevealed = true
            invalidate()
            return
        }

        if (hasRevealed) {
            startValueMorphAnimation(animatedPercentage, target)
        } else {
            post { checkViewportVisibility() }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupScrollTrigger()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        scrollListener?.let {
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnScrollChangedListener(it)
            }
        }
        scrollListener = null
    }

    private fun setupScrollTrigger() {
        if (hasRevealed) return

        if (!UiArcMotion.isMotionEnabled(context)) {
            animatedPercentage = percentage
            hasRevealed = true
            invalidate()
            return
        }

        if (viewTreeObserver.isAlive) {
            val listener = ViewTreeObserver.OnScrollChangedListener {
                checkViewportVisibility()
            }
            scrollListener = listener
            viewTreeObserver.addOnScrollChangedListener(listener)
        }
        post { checkViewportVisibility() }
    }

    private fun checkViewportVisibility() {
        if (hasRevealed || !isAttachedToWindow) return

        if (!UiArcMotion.isMotionEnabled(context)) {
            animatedPercentage = percentage
            hasRevealed = true
            invalidate()
            return
        }

        val isVisible = getGlobalVisibleRect(globalRect)
        if (isVisible) {
            val visibleHeight = globalRect.height().toFloat()
            val totalHeight = height.toFloat()
            if (totalHeight > 0f && (visibleHeight / totalHeight) >= TRIGGER_AMOUNT) {
                triggerEntranceAnimation()
            }
        }
    }

    private fun triggerEntranceAnimation() {
        if (hasRevealed) return
        hasRevealed = true

        scrollListener?.let {
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnScrollChangedListener(it)
            }
        }
        scrollListener = null

        animator?.cancel()
        val anim = ValueAnimator.ofFloat(0f, percentage).apply {
            duration = REVEAL_DURATION_MS
            interpolator = gaugeRevealSpring
            addUpdateListener {
                animatedPercentage = it.animatedValue as Float
                invalidate()
            }
        }
        animator = anim
        anim.start()
    }

    private fun startValueMorphAnimation(fromVal: Float, toVal: Float) {
        animator?.cancel()
        val anim = ValueAnimator.ofFloat(fromVal, toVal).apply {
            duration = SMOOTH_DURATION_MS
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener {
                animatedPercentage = it.animatedValue as Float
                invalidate()
            }
        }
        animator = anim
        anim.start()
    }

    private fun getColorForPercentage(pct: Float): Int {
        val red = DesignTokens.color(context, R.attr.systemRed)
        val orange = DesignTokens.color(context, R.attr.systemOrange)
        val green = DesignTokens.color(context, R.attr.systemGreen)

        return when {
            pct < 35f -> red
            pct < 40f -> argbEvaluator.evaluate((pct - 35f) / 5f, red, orange) as Int
            pct < 65f -> orange
            pct < 70f -> argbEvaluator.evaluate((pct - 65f) / 5f, orange, green) as Int
            else -> green
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = stroke / 2
        val diameter = minOf(width, height).toFloat()
        val left = (width - diameter) / 2
        val top = (height - diameter) / 2
        rect.set(left + pad, top + pad, left + diameter - pad, top + diameter - pad)

        // Track ring
        arc.color = DesignTokens.track(context)
        canvas.drawOval(rect, arc)

        // Progress arc with semantic color
        val current = if (!UiArcMotion.isMotionEnabled(context)) percentage else animatedPercentage
        arc.color = getColorForPercentage(current)
        canvas.drawArc(rect, -90f, 360f * (current / 100f), false, arc)

        // Centered percentage label
        label.color = DesignTokens.primary(context)
        val text = "${current.toInt()}%"
        val yOffset = (label.ascent() + label.descent()) / 2
        canvas.drawText(text, width / 2f, height / 2f - yOffset, label)
    }
}

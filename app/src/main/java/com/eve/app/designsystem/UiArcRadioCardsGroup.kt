package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.eve.app.R

/**
 * UIArc Radio Cards Component.
 * Genuine native translation of official UIArc Radio Cards specification:
 * - Registry: https://uiarc.dev/r/radio-cards.json
 * - Selection ring: Single sliding indicator ring gliding between cards on GLIDE spring (550ms, visualDuration 0.42s, bounce 0.00)
 * - Initial placement: Fast opacity fade-in (160ms, Ease.STANDARD)
 * - Native RadioGroup keyboard navigation and mutual exclusion preserved
 * - Instant snap with reduced motion
 */
class UiArcRadioCardsGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RadioGroup(context, attrs) {

    companion object {
        const val GLIDE_DURATION_MS = 550L
        const val FADE_IN_DURATION_MS = 160L
    }

    private val density = resources.displayMetrics.density
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = ContextCompat.getColor(context, R.color.system_blue)
    }

    private val currentRect = RectF()
    private val targetRect = RectF()
    private var ringAlpha = 0f
    private var isRingPlaced = false
    private var ringAnimator: ValueAnimator? = null
    private var alphaAnimator: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        setOnCheckedChangeListener { group, checkedId ->
            updateSelectedCard(checkedId, animate = true)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (checkedRadioButtonId != View.NO_ID) {
            updateSelectedCard(checkedRadioButtonId, animate = false)
        }
    }

    private fun updateSelectedCard(checkedId: Int, animate: Boolean) {
        val selectedView = findViewById<View>(checkedId) ?: return
        val targetL = selectedView.left.toFloat()
        val targetT = selectedView.top.toFloat()
        val targetR = selectedView.right.toFloat()
        val targetB = selectedView.bottom.toFloat()

        targetRect.set(targetL, targetT, targetR, targetB)

        if (!isRingPlaced || !animate || !UiArcMotion.isMotionEnabled(context)) {
            currentRect.set(targetRect)
            ringAlpha = 1f
            isRingPlaced = true
            invalidate()
            return
        }

        // Animate selection ring with GLIDE spring
        val fromL = currentRect.left
        val fromT = currentRect.top
        val fromR = currentRect.right
        val fromB = currentRect.bottom

        ringAnimator?.cancel()
        ringAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = GLIDE_DURATION_MS
            interpolator = UiArcMotion.Spring.CARD_GLIDE
            addUpdateListener { va ->
                val fraction = va.animatedValue as Float
                currentRect.left = fromL + (targetL - fromL) * fraction
                currentRect.top = fromT + (targetT - fromT) * fraction
                currentRect.right = fromR + (targetR - fromR) * fraction
                currentRect.bottom = fromB + (targetB - fromB) * fraction
                invalidate()
            }
            start()
        }

        if (ringAlpha < 1f) {
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(ringAlpha, 1f).apply {
                duration = FADE_IN_DURATION_MS
                interpolator = UiArcMotion.Ease.STANDARD
                addUpdateListener { va ->
                    ringAlpha = va.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isRingPlaced && ringAlpha > 0f && !currentRect.isEmpty) {
            ringPaint.alpha = (ringAlpha * 255).toInt()
            val radius = 14f * density
            canvas.drawRoundRect(currentRect, radius, radius, ringPaint)
        }
    }
}

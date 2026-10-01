package com.eve.app.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.eve.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight, zero-allocation sparkle burst for the bookmark star animation.
 * Emits 6 tiny accent dots outward (~300ms) and fades out smoothly.
 * No Lottie, no ring/circle behind the star.
 */
class SparkleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var progress = 0f
    private var animator: ValueAnimator? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 6 particles spaced equally around 360 degrees
    private val angles = floatArrayOf(30f, 90f, 150f, 210f, 270f, 330f)
    private val minRadius = 10f * density
    private val maxRadius = 24f * density
    private val baseDotRadius = 2.2f * density

    fun startSparkle() {
        reset()
        val accentColor = ContextCompat.getColor(context, R.color.eve_star_fill)
        paint.color = accentColor

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                progress = va.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    reset()
                }
                override fun onAnimationCancel(animation: Animator) {
                    reset()
                }
            })
            start()
        }
    }

    fun reset() {
        animator?.cancel()
        animator = null
        progress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (progress <= 0f || progress >= 1f) return

        val cx = width / 2f
        val cy = height / 2f
        val currentDist = minRadius + progress * (maxRadius - minRadius)
        val currentAlpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
        val currentDotRadius = baseDotRadius * (1f - progress * 0.4f)

        paint.alpha = currentAlpha

        for (angleDeg in angles) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val px = cx + (currentDist * cos(rad)).toFloat()
            val py = cy + (currentDist * sin(rad)).toFloat()
            canvas.drawCircle(px, py, currentDotRadius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        reset()
    }
}

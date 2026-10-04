package com.eve.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import com.eve.app.R

/**
 * Lightweight, continuous, subtle looping vector flame animation.
 * Replaces the static ic_flame_line with a calm, premium breathing and swaying flame.
 *
 * Requirements:
 * - Subtle, premium, calmly looping without childish distraction.
 * - Works in both light and dark themes (uses eve_text token color).
 * - Pauses when backgrounded, resumes when visible.
 * - Respects system animation scale / reduced-motion.
 * - Zero external runtime dependencies.
 */
class FlameAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.8f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val outerFlamePath = Path()
    private val innerFlamePath = Path()
    private val transformedOuterPath = Path()
    private val transformedInnerPath = Path()
    private val transformMatrix = Matrix()

    private var animProgress = 0f
    private var animator: ValueAnimator? = null
    private var isManuallyPaused = false

    init {
        // Base paths matching ic_flame_line (viewport 24x24)
        val outerData = "M12,2.5C12,2.5 15,6 15,9C15,10.6 14.2,11.8 13,12.4C16,12.9 18.5,15.2 18.5,18C18.5,20.8 15.6,22.5 12,22.5C8.4,22.5 5.5,20.8 5.5,18C5.5,14.2 9,12 10.2,9C11,7 11.2,4.8 12,2.5Z"
        val innerData = "M12,14.5C10.8,14.5 10,15.5 10,16.8C10,18 10.9,19 12,19C13.1,19 14,18 14,16.8C14,15.5 13.2,14.5 12,14.5Z"

        try {
            PathParser.createPathFromPathData(outerData).let { outerFlamePath.set(it) }
            PathParser.createPathFromPathData(innerData).let { innerFlamePath.set(it) }
        } catch (_: Exception) {}
    }

    private fun setupAnimator() {
        val animScale = try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (_: Exception) {
            1f
        }

        if (animScale <= 0f) {
            animProgress = 0.5f
            invalidate()
            return
        }

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                animProgress = it.animatedValue as Float
                invalidate()
            }
            if (!isManuallyPaused && isAttachedToWindow && visibility == VISIBLE) {
                start()
            }
        }
    }

    fun pauseAnimation() {
        isManuallyPaused = true
        animator?.pause()
    }

    fun resumeAnimation() {
        isManuallyPaused = false
        if (animator?.isPaused == true) {
            animator?.resume()
        } else if (animator?.isRunning != true) {
            setupAnimator()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isManuallyPaused) {
            setupAnimator()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && !isManuallyPaused) {
            if (animator?.isRunning != true) {
                setupAnimator()
            }
        } else {
            animator?.pause()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Resolve theme-aware color
        val color = ContextCompat.getColor(context, R.color.eve_text)
        flamePaint.color = color

        // Subtly scale and sway (0.95x to 1.05x, gentle vertical breathing)
        val scale = 0.96f + 0.08f * animProgress
        val scaleFactor = (w / 24f) * scale
        val dx = (w - 24f * scaleFactor) / 2f
        val dy = (h - 24f * scaleFactor) / 2f - (0.5f * density * animProgress)

        transformMatrix.reset()
        transformMatrix.setScale(scaleFactor, scaleFactor)
        transformMatrix.postTranslate(dx, dy)

        transformedOuterPath.reset()
        outerFlamePath.transform(transformMatrix, transformedOuterPath)

        transformedInnerPath.reset()
        innerFlamePath.transform(transformMatrix, transformedInnerPath)

        canvas.drawPath(transformedOuterPath, flamePaint)
        canvas.drawPath(transformedInnerPath, flamePaint)
    }
}

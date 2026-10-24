package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.eve.app.R

/**
 * UIArc Text Shimmer — Loading Status Label Component.
 * Faithfully implements the UIArc Text Shimmer specification (https://uiarc.dev/components/text-shimmer):
 * - Bell-shaped light highlight band moving linearly across a muted base text color
 * - 1.8s sweep duration with 576ms (1.8s * 0.32) pause between passes
 * - Linear gradient with pre-allocated Matrix and zero onDraw allocations
 * - Runs only while UiState.Loading is active and the view is visible
 * - Pauses/resumes on visibility and window changes; cleans up on detach
 * - Reduced-motion fallback renders standard solid text with no active animation
 */
class UiArcTextShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var isShimmerActive = false
    private var sweepAnimator: ValueAnimator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val nextPassRunnable = Runnable { runNextPass() }

    private var shader: LinearGradient? = null
    private val shaderMatrix = Matrix()

    private var sweepProgress = 0f
    private var bandWidthPx = 0f
    private var textWidthPx = 0f

    init {
        includeFontPadding = false
    }

    /** Starts the linear shimmer sweep. */
    fun startShimmer() {
        isShimmerActive = true
        if (isShown && UiArcMotion.isMotionEnabled(context)) {
            runNextPass()
        }
    }

    /** Stops the shimmer immediately and resolves to solid text. */
    fun stopShimmer() {
        isShimmerActive = false
        sweepAnimator?.cancel()
        handler.removeCallbacks(nextPassRunnable)
        paint.shader = null
        invalidate()
    }

    private fun runNextPass() {
        if (!isShimmerActive || !isShown || !UiArcMotion.isMotionEnabled(context)) {
            paint.shader = null
            invalidate()
            return
        }

        sweepAnimator?.cancel()
        handler.removeCallbacks(nextPassRunnable)

        sweepAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = UiArcMotion.Duration.SHIMMER_SWEEP
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                sweepProgress = anim.animatedValue as Float
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isShimmerActive && isShown) {
                        handler.postDelayed(nextPassRunnable, UiArcMotion.Duration.SHIMMER_PAUSE)
                    }
                }
            })
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildShader()
    }

    private fun rebuildShader() {
        val textStr = text?.toString().orEmpty()
        textWidthPx = paint.measureText(textStr).coerceAtLeast(1f)
        bandWidthPx = 2.2f * textSize // 2.2em band width from UIArc specification

        val baseColor = ContextCompat.getColor(context, R.color.text_secondary)
        val highlightColor = ContextCompat.getColor(context, R.color.text_primary)

        // Interpolate bell-shaped falloff stops: [0, 0.5, 0.9, 1.0, 0.9, 0.5, 0]
        val stops = floatArrayOf(0.0f, 0.15f, 0.35f, 0.5f, 0.65f, 0.85f, 1.0f)
        val strengths = floatArrayOf(0.0f, 0.5f, 0.9f, 1.0f, 0.9f, 0.5f, 0.0f)
        val colors = IntArray(stops.size) { i ->
            val s = strengths[i]
            mixColor(baseColor, highlightColor, s)
        }

        shader = LinearGradient(
            0f, 0f, bandWidthPx, 0f,
            colors, stops,
            Shader.TileMode.CLAMP
        )
    }

    private fun mixColor(base: Int, highlight: Int, factor: Float): Int {
        val a = Color.alpha(base)
        val r = (Color.red(base) * (1f - factor) + Color.red(highlight) * factor).toInt()
        val g = (Color.green(base) * (1f - factor) + Color.green(highlight) * factor).toInt()
        val b = (Color.blue(base) * (1f - factor) + Color.blue(highlight) * factor).toInt()
        return Color.argb(a, r, g, b)
    }

    override fun onDraw(canvas: Canvas) {
        val active = isShimmerActive && isShown && UiArcMotion.isMotionEnabled(context)
        if (active && shader != null) {
            val totalTravel = textWidthPx + bandWidthPx * 2f
            val currentX = -bandWidthPx + totalTravel * sweepProgress
            shaderMatrix.setTranslate(currentX, 0f)
            shader?.setLocalMatrix(shaderMatrix)
            paint.shader = shader
        } else {
            paint.shader = null
        }
        super.onDraw(canvas)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isShimmerActive) {
            startShimmer()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible && isShimmerActive) {
            runNextPass()
        } else {
            sweepAnimator?.cancel()
            handler.removeCallbacks(nextPassRunnable)
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE && isShimmerActive) {
            runNextPass()
        } else {
            sweepAnimator?.cancel()
            handler.removeCallbacks(nextPassRunnable)
        }
    }
}

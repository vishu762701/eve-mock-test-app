package com.eve.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.util.ThemeSwitchAnimator

/**
 * Telegram-style Shimmer/Skeleton Loading Placeholder View (matching LoadingDrawable.java pattern).
 * Renders gray rounded-rectangle placeholder outlines matching real content shapes,
 * animated with a continuous diagonal light-gradient sweep left-to-right.
 */
class ShimmerSkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val TYPE_EXAM_CARDS = 0
        const val TYPE_QUESTION = 1
        const val TYPE_LIST_ITEMS = 2
    }

    var skeletonType: Int = TYPE_EXAM_CARDS
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderMatrix = Matrix()
    private val rectF = RectF()

    private var animator: ValueAnimator? = null
    private var sweepProgress: Float = 0f

    init {
        startAnimation()
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { va ->
                sweepProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader(w.toFloat(), h.toFloat())
    }

    private fun updateShader(width: Float, height: Float) {
        if (width <= 0 || height <= 0) return
        val baseColor = ContextCompat.getColor(context, R.color.eve_shimmer_base)
        val highlightColor = ContextCompat.getColor(context, R.color.eve_shimmer_highlight)

        val gradientWidth = width * 0.7f
        val shader = LinearGradient(
            0f, 0f, gradientWidth, gradientWidth * 0.5f,
            intArrayOf(baseColor, highlightColor, baseColor),
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        paint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        if (paint.shader == null) {
            updateShader(w, h)
        }

        // Translate the diagonal sweep across the view
        val totalTravel = w * 2f
        val transX = -w * 0.5f + totalTravel * sweepProgress
        shaderMatrix.reset()
        shaderMatrix.setTranslate(transX, 0f)
        paint.shader?.setLocalMatrix(shaderMatrix)

        when (skeletonType) {
            TYPE_QUESTION -> drawQuestionSkeleton(canvas, w, h)
            TYPE_LIST_ITEMS -> drawListItemsSkeleton(canvas, w, h)
            else -> drawExamCardsSkeleton(canvas, w, h)
        }
    }

    private fun drawExamCardsSkeleton(canvas: Canvas, w: Float, h: Float) {
        val pad = 16f * density
        val cardW = w - (pad * 2f)
        val cardH = 96f * density
        val cardRadius = 16f * density
        val spacing = 12f * density

        var top = 8f * density
        for (i in 0 until 4) {
            if (top + cardH > h && i > 0) break
            rectF.set(pad, top, pad + cardW, top + cardH)
            canvas.drawRoundRect(rectF, cardRadius, cardRadius, paint)
            top += cardH + spacing
        }
    }

    private fun drawQuestionSkeleton(canvas: Canvas, w: Float, h: Float) {
        val pad = 16f * density
        val lineRadius = 6f * density

        // Question header line
        rectF.set(pad, 16f * density, pad + 120f * density, 32f * density)
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, paint)

        // Question text lines
        rectF.set(pad, 44f * density, w - pad, 62f * density)
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, paint)

        rectF.set(pad, 70f * density, w - pad - 60f * density, 88f * density)
        canvas.drawRoundRect(rectF, lineRadius, lineRadius, paint)

        // 4 Options (cards with radio circles)
        var top = 116f * density
        val optH = 50f * density
        val spacing = 10f * density
        for (i in 0 until 4) {
            rectF.set(pad, top, w - pad, top + optH)
            canvas.drawRoundRect(rectF, 12f * density, 12f * density, paint)
            top += optH + spacing
        }
    }

    private fun drawListItemsSkeleton(canvas: Canvas, w: Float, h: Float) {
        val pad = 16f * density
        val itemH = 68f * density
        val spacing = 8f * density

        var top = 8f * density
        for (i in 0 until 6) {
            if (top + itemH > h && i > 0) break
            rectF.set(pad, top, w - pad, top + itemH)
            canvas.drawRoundRect(rectF, 14f * density, 14f * density, paint)
            top += itemH + spacing
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animator?.isStarted != true) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}

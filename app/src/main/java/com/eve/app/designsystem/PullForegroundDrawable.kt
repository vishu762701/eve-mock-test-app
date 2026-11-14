package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import com.eve.app.telegram.components.CubicBezierInterpolator
import com.eve.app.theme.TelegramThemeEngine

/**
 * Kotlin port of Telegram Android's PullForegroundDrawable.java.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/PullForegroundDrawable.java
 * Reference: DrKLO/Telegram at 0aae959806c57be8ad457980f2a5c6277ff18635.
 */
class PullForegroundDrawable(
    private val pullTooltipText: CharSequence = "Pull down to refresh",
    private val releaseTooltipText: CharSequence = "Release to refresh"
) {
    companion object {
        const val SNAP_HEIGHT = 0.85f
        const val startPullParallax = 0.45f
        const val endPullParallax = 0.25f
        const val startPullOverScroll = 0.2f
        const val minPullingTime = 200L

        fun getMaxOverscroll(density: Float): Int = (72 * density).toInt()
    }

    private val paintSecondary = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintWhite = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private val tooltipTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val arrowDrawable = ArrowDrawable()

    private var textSwappingProgress = 1f
    private var arrowRotateProgress = 1f
    private var animateToEndText = false
    private var arrowAnimateTo = false
    private var textSwipingAnimator: ValueAnimator? = null
    private var arrowRotateAnimator: ValueAnimator? = null

    private var hostView: View? = null
    var pullProgress: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                updateTextProgress(value)
                hostView?.invalidate()
            }
        }

    private var pullTooltipLayout: StaticLayout? = null
    private var releaseTooltipLayout: StaticLayout? = null
    private var lastWidth = 0

    init {
        tooltipTextPaint.typeface = Typeface.DEFAULT_BOLD
        tooltipTextPaint.color = Color.WHITE
        paintWhite.color = Color.WHITE
        paintSecondary.color = ColorUtils.setAlphaComponent(Color.WHITE, 100)
    }

    fun setHostView(view: View) {
        hostView = view
        val density = view.resources.displayMetrics.density
        tooltipTextPaint.textSize = 14f * density
        updateColors()
    }

    fun updateColors() {
        val view = hostView ?: return
        val palette = TelegramThemeEngine.resolveCurrentPalette(view.context)
        backgroundPaint.color = palette.windowBackground
        tooltipTextPaint.color = palette.textPrimary
        paintWhite.color = palette.surface
        arrowDrawable.setColor(palette.accent)
    }

    private fun checkTextLayouts(width: Int) {
        if (width <= 0 || width == lastWidth) return
        lastWidth = width
        val density = hostView?.resources?.displayMetrics?.density ?: 2f
        tooltipTextPaint.textSize = 14f * density

        pullTooltipLayout = StaticLayout.Builder.obtain(
            pullTooltipText, 0, pullTooltipText.length, tooltipTextPaint, width
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()

        releaseTooltipLayout = StaticLayout.Builder.obtain(
            releaseTooltipText, 0, releaseTooltipText.length, tooltipTextPaint, width
        ).setAlignment(Layout.Alignment.ALIGN_CENTER).build()
    }

    private fun updateTextProgress(progress: Float) {
        val endText = progress >= SNAP_HEIGHT
        if (animateToEndText != endText) {
            animateToEndText = endText
            textSwipingAnimator?.cancel()
            textSwipingAnimator = ValueAnimator.ofFloat(textSwappingProgress, if (endText) 0f else 1f).apply {
                duration = 170
                interpolator = LinearInterpolator()
                addUpdateListener {
                    textSwappingProgress = it.animatedValue as Float
                    hostView?.invalidate()
                }
                start()
            }
        }

        if (endText != arrowAnimateTo) {
            arrowAnimateTo = endText
            arrowRotateAnimator?.cancel()
            arrowRotateAnimator = ValueAnimator.ofFloat(arrowRotateProgress, if (arrowAnimateTo) 0f else 1f).apply {
                duration = 250
                interpolator = CubicBezierInterpolator.EASE_BOTH
                addUpdateListener {
                    arrowRotateProgress = it.animatedValue as Float
                    hostView?.invalidate()
                }
                start()
            }
        }
    }

    fun reset() {
        pullProgress = 0f
        animateToEndText = false
        arrowAnimateTo = false
        textSwappingProgress = 1f
        arrowRotateProgress = 1f
        textSwipingAnimator?.cancel()
        arrowRotateAnimator?.cancel()
    }

    fun draw(canvas: Canvas, width: Int, pullHeight: Float) {
        if (pullHeight <= 0f) return
        val density = hostView?.resources?.displayMetrics?.density ?: 2f
        val smallMargin = (8 * density).toInt()
        val radius = (12 * density).toInt()

        checkTextLayouts(width - (32 * density).toInt())

        // Top curved/flat background
        rectF.set(0f, 0f, width.toFloat(), pullHeight)
        canvas.drawRect(rectF, backgroundPaint)

        // Arrow circle & icon
        val cX = width / 2f
        val cY = pullHeight - radius - smallMargin
        if (cY > radius) {
            canvas.drawCircle(cX, cY, radius.toFloat(), paintWhite)

            val ih = (18 * density).toInt()
            val iw = ih
            arrowDrawable.setBounds(
                (cX - iw / 2).toInt(),
                (cY - ih / 2).toInt(),
                (cX + iw / 2).toInt(),
                (cY + ih / 2).toInt()
            )

            val rotateAngle = 180f * (1f - arrowRotateProgress)
            canvas.save()
            canvas.rotate(rotateAngle, cX, cY)
            arrowDrawable.draw(canvas)
            canvas.restore()
        }

        // Tooltip text
        val textY = cY - radius - (16 * density)
        if (textY > 0) {
            if (textSwappingProgress > 0f && pullTooltipLayout != null) {
                canvas.save()
                tooltipTextPaint.alpha = (255 * textSwappingProgress).toInt()
                canvas.translate(cX - (pullTooltipLayout!!.width / 2f), textY)
                pullTooltipLayout!!.draw(canvas)
                canvas.restore()
            }
            if (textSwappingProgress < 1f && releaseTooltipLayout != null) {
                canvas.save()
                tooltipTextPaint.alpha = (255 * (1f - textSwappingProgress)).toInt()
                canvas.translate(cX - (releaseTooltipLayout!!.width / 2f), textY)
                releaseTooltipLayout!!.draw(canvas)
                canvas.restore()
            }
        }
    }

    private inner class ArrowDrawable : Drawable() {
        private val path = Path()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeJoin = Paint.Join.ROUND
        }

        init {
            updatePath(2f)
        }

        fun updatePath(density: Float) {
            val h = 18f * density
            path.reset()
            path.moveTo(h / 2f, 4.98f * density)
            path.lineTo(4.95f * density, 9f * density)
            path.lineTo(h - 4.95f * density, 9f * density)
            path.lineTo(h / 2f, 4.98f * density)
            paint.strokeWidth = 1f * density
        }

        fun setColor(color: Int) {
            paint.color = color
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val density = (b.width() / 18f).coerceAtLeast(1f)
            updatePath(density)
            canvas.save()
            canvas.translate(b.left.toFloat(), b.top.toFloat())
            canvas.drawPath(path, paint)
            val h = 18f * density
            canvas.drawRect(7.56f * density, 8f * density, h - 7.56f * density, 11.1f * density, paint)
            canvas.restore()
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}

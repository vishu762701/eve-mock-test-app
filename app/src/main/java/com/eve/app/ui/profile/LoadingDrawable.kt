package com.eve.app.ui.profile

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.SystemClock
import org.telegram.ui.Components.CubicBezierInterpolator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Authentic Telegram LoadingDrawable ported from:
 * org.telegram.ui.Components.LoadingDrawable.java:28-405@3de37c8cfb039452be7398909461da6288babf15
 * License: GPL v2 or later.
 * Provides shimmering gradient sweep, customizable stroke outline, and authentic appear/disappear gradient transitions.
 */
class LoadingDrawable(val density: Float = 1f) : Drawable() {

    companion object {
        private const val APPEAR_DURATION = 550f
        private const val DISAPPEAR_DURATION = 320f
    }

    private fun dp(v: Float): Float = v * density
    private fun dp(v: Int): Int = (v * density).roundToInt()

    var color1: Int? = null
    var color2: Int? = null
    var strokeColor1: Int? = null
    var strokeColor2: Int? = null
    var stroke: Boolean = false

    var backgroundPaint: Paint? = null
    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val strokePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = if (density > 2f) 2f else 1f
    }

    private var usePath: Path? = null
    private val path = Path()
    private var lastBounds: Rect? = null
    private val radii = FloatArray(8)
    private val rectF = RectF()

    private var start: Long = -1L
    private var disappearStart: Long = -1L

    private var gradient: LinearGradient? = null
    private var strokeGradient: LinearGradient? = null
    private val matrix = Matrix()
    private val strokeMatrix = Matrix()
    private var gradientColor1: Int = 0
    private var gradientColor2: Int = 0
    private var gradientStrokeColor1: Int = 0
    private var gradientStrokeColor2: Int = 0
    private var gradientWidth: Int = 0
    private var gradientWidthScale: Float = 1f
    private var speed: Float = 1f
    private var singleCycleDuration: Long = 0L

    private var appearByGradient: Boolean = false
    private var appearGradientWidth: Int = 0
    private var appearPaint: Paint? = null
    private var appearGradient: LinearGradient? = null
    private var appearMatrix: Matrix? = null

    private var disappearGradientWidth: Int = 0
    private var disappearPaint: Paint? = null
    private var disappearGradient: LinearGradient? = null
    private var disappearMatrix: Matrix? = null

    fun setColors(color1: Int, color2: Int, strokeColor1: Int, strokeColor2: Int) {
        this.color1 = color1
        this.color2 = color2
        this.stroke = true
        this.strokeColor1 = strokeColor1
        this.strokeColor2 = strokeColor2
    }

    fun setColors(color1: Int, color2: Int) {
        this.color1 = color1
        this.color2 = color2
        this.stroke = false
    }

    fun setAppearByGradient(enabled: Boolean) {
        appearByGradient = enabled
    }

    fun setSpeed(value: Float) {
        speed = value
    }

    fun setGradientScale(scale: Float) {
        gradientWidthScale = scale
    }

    fun setRadii(all: Float) {
        if (usePath != null) {
            paint.pathEffect = CornerPathEffect(all)
            strokePaint.pathEffect = CornerPathEffect(all)
        } else {
            setRadii(all, all, all, all)
        }
    }

    fun setRadii(topLeft: Float, topRight: Float, bottomRight: Float, bottomLeft: Float) {
        val changed = radii[0] != topLeft || radii[2] != topRight || radii[4] != bottomRight || radii[6] != bottomLeft
        radii[0] = topLeft
        radii[1] = topLeft
        radii[2] = topRight
        radii[3] = topRight
        radii[4] = bottomRight
        radii[5] = bottomRight
        radii[6] = bottomLeft
        radii[7] = bottomLeft

        if (lastBounds != null && changed) {
            path.rewind()
            rectF.set(lastBounds!!)
            path.addRoundRect(rectF, radii, Path.Direction.CW)
        }
    }

    fun isDisappearing(): Boolean {
        return disappearStart > 0 && (SystemClock.elapsedRealtime() - disappearStart) < DISAPPEAR_DURATION
    }

    fun isDisappeared(): Boolean {
        return disappearStart > 0 && (SystemClock.elapsedRealtime() - disappearStart) >= DISAPPEAR_DURATION
    }

    val isRunning: Boolean
        get() = start > 0 && !isDisappeared()

    fun reset() {
        start = -1L
    }

    fun disappear() {
        if (!isDisappeared() && !isDisappearing()) {
            disappearStart = SystemClock.elapsedRealtime()
        }
    }

    fun resetDisappear() {
        disappearStart = -1L
    }

    override fun draw(canvas: Canvas) {
        if (isDisappeared()) return

        val bounds = bounds
        if (paint.alpha <= 0) return

        var width = bounds.width()
        if (width <= 0) {
            width = dp(200)
        }
        val gwidth = (min(dp(400), width) * gradientWidthScale).roundToInt()
        val c1 = color1 ?: 0x1AFFFFFF
        val c2 = color2 ?: 0x4DFFFFFF
        val sc1 = strokeColor1 ?: c1
        val sc2 = strokeColor2 ?: c2

        if (gradient == null || gwidth != gradientWidth || c1 != gradientColor1 || c2 != gradientColor2 ||
            sc1 != gradientStrokeColor1 || sc2 != gradientStrokeColor2) {
            gradientWidth = gwidth
            gradientColor1 = c1
            gradientColor2 = c2
            gradient = LinearGradient(0f, 0f, gradientWidth.toFloat(), 0f,
                intArrayOf(gradientColor1, gradientColor2, gradientColor1),
                floatArrayOf(0f, 0.67f, 1f),
                Shader.TileMode.REPEAT
            )
            gradient?.setLocalMatrix(matrix)
            paint.shader = gradient

            gradientStrokeColor1 = sc1
            gradientStrokeColor2 = sc2
            strokeGradient = LinearGradient(0f, 0f, gradientWidth.toFloat(), 0f,
                intArrayOf(gradientStrokeColor1, gradientStrokeColor1, gradientStrokeColor2, gradientStrokeColor1),
                floatArrayOf(0f, 0.4f, 0.67f, 1f),
                Shader.TileMode.REPEAT
            )
            strokeGradient?.setLocalMatrix(strokeMatrix)
            strokePaint.shader = strokeGradient
        }

        val now = SystemClock.elapsedRealtime()
        if (start < 0) {
            start = now
        }
        val offset = if (singleCycleDuration > 0) {
            min(1f, (now - start) / singleCycleDuration.toFloat()) * gradientWidth
        } else {
            var t = (now - start) / 2000f
            t = (t * speed / 4f).toDouble().pow(0.85).toFloat() * 4f
            ((t * density * gradientWidth) % gradientWidth)
        }
        val appearT = (now - start) / APPEAR_DURATION
        val disappearT = if (disappearStart > 0) {
            1f - CubicBezierInterpolator.EASE_OUT.getInterpolation(min(1f, (now - disappearStart) / DISAPPEAR_DURATION))
        } else 0f

        var disappearRestore = false
        if (isDisappearing()) {
            val disappearGradientWidthNow = max(dp(200), bounds.width() / 3)
            if (disappearT < 1f) {
                if (disappearPaint == null) {
                    disappearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    disappearGradientWidth = disappearGradientWidthNow
                    disappearGradient = LinearGradient(0f, 0f, disappearGradientWidth.toFloat(), 0f,
                        intArrayOf(-0x1, 0x00FFFFFF), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                    disappearMatrix = Matrix()
                    disappearGradient?.setLocalMatrix(disappearMatrix)
                    disappearPaint?.shader = disappearGradient
                    disappearPaint?.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                } else if (disappearGradientWidth != disappearGradientWidthNow) {
                    disappearGradientWidth = disappearGradientWidthNow
                    disappearGradient = LinearGradient(0f, 0f, disappearGradientWidth.toFloat(), 0f,
                        intArrayOf(-0x1, 0x00FFFFFF), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                    disappearGradient?.setLocalMatrix(disappearMatrix)
                    disappearPaint?.shader = disappearGradient
                }
                rectF.set(bounds)
                rectF.inset(-strokePaint.strokeWidth, -strokePaint.strokeWidth)
                canvas.saveLayerAlpha(rectF, 255, Canvas.ALL_SAVE_FLAG)
                disappearRestore = true
            }
        }

        var appearRestore = false
        if (appearByGradient) {
            val appearGradientWidthNow = max(dp(200), bounds.width() / 3)
            if (appearT < 1f) {
                if (appearPaint == null) {
                    appearPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    appearGradientWidth = appearGradientWidthNow
                    appearGradient = LinearGradient(0f, 0f, appearGradientWidth.toFloat(), 0f,
                        intArrayOf(0x00FFFFFF, -0x1), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                    appearMatrix = Matrix()
                    appearGradient?.setLocalMatrix(appearMatrix)
                    appearPaint?.shader = appearGradient
                    appearPaint?.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                } else if (appearGradientWidth != appearGradientWidthNow) {
                    appearGradientWidth = appearGradientWidthNow
                    appearGradient = LinearGradient(0f, 0f, appearGradientWidth.toFloat(), 0f,
                        intArrayOf(0x00FFFFFF, -0x1), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                    appearGradient?.setLocalMatrix(appearMatrix)
                    appearPaint?.shader = appearGradient
                }
                rectF.set(bounds)
                rectF.inset(-strokePaint.strokeWidth, -strokePaint.strokeWidth)
                canvas.saveLayerAlpha(rectF, 255, Canvas.ALL_SAVE_FLAG)
                appearRestore = true
            }
        }

        matrix.setTranslate(offset, 0f)
        gradient?.setLocalMatrix(matrix)

        strokeMatrix.setTranslate(offset, 0f)
        strokeGradient?.setLocalMatrix(strokeMatrix)

        val drawPath: Path = usePath ?: run {
            if (lastBounds == null || lastBounds != bounds) {
                path.rewind()
                rectF.set(bounds)
                lastBounds = Rect(bounds)
                path.addRoundRect(rectF, radii, Path.Direction.CW)
            }
            path
        }

        backgroundPaint?.let { canvas.drawPath(drawPath, it) }
        canvas.drawPath(drawPath, paint)
        if (stroke) {
            canvas.drawPath(drawPath, strokePaint)
        }

        if (appearRestore) {
            canvas.save()
            val appearOffset = appearT * (appearGradientWidth + bounds.width() + appearGradientWidth) - appearGradientWidth
            appearMatrix?.setTranslate(bounds.left + appearOffset, 0f)
            appearGradient?.setLocalMatrix(appearMatrix)
            val inset = strokePaint.strokeWidth
            appearPaint?.let {
                canvas.drawRect(bounds.left - inset, bounds.top - inset, bounds.right + inset, bounds.bottom + inset, it)
            }
            canvas.restore()
            canvas.restore()
        }

        if (disappearRestore) {
            canvas.save()
            val appearOffset = disappearT * (disappearGradientWidth + bounds.width() + disappearGradientWidth) - disappearGradientWidth
            disappearMatrix?.setTranslate(bounds.right - appearOffset, 0f)
            disappearGradient?.setLocalMatrix(disappearMatrix)
            val inset = strokePaint.strokeWidth
            disappearPaint?.let {
                canvas.drawRect(bounds.left - inset, bounds.top - inset, bounds.right + inset, bounds.bottom + inset, it)
            }
            canvas.restore()
            canvas.restore()
        }

        if (!isDisappeared()) {
            invalidateSelf()
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        strokePaint.alpha = alpha
        if (alpha > 0) {
            invalidateSelf()
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT
}

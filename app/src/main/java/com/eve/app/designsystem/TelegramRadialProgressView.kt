package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.eve.app.theme.TelegramThemeEngine

/**
 * Kotlin adaptation of Telegram Android's RadialProgressView.java.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v. 2 or later (upstream file header).
 * Reference: DrKLO/Telegram at 0aae959806c57be8ad457980f2a5c6277ff18635.
 *
 * Modified for EVE: Java-to-Kotlin translation, palette and density accessors,
 * XML constructors, and drawAt() Drawable integration. This is NOT verbatim
 * source reuse or independently verified exact runtime parity.
 * Distribution still requires dependency compatibility and corresponding-source
 * review; see THIRD_PARTY_NOTICES.md and docs/telegram-release-compliance-gate.md.
 */
/**
 * Typealias for Telegram RadialProgressView canonical naming.
 */
typealias RadialProgressView = TelegramRadialProgressView

class TelegramRadialProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var lastUpdateTime = 0L
    private var radOffset = 0f
    private var currentCircleLength = 0f
    private var risingCircleLength = false
    private var currentProgressTime = 0f
    private val circleRect = RectF()
    private var useSelfAlpha = false
    private var drawingCircleLength = 0f

    private val decelerateInterpolator = DecelerateInterpolator()
    private val accelerateInterpolator = AccelerateInterpolator()
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(3f)
        color = TelegramThemeEngine.resolveCurrentPalette(context).accent
    }
    private var size = dp(40f).toInt()
    private var currentProgress = 0f
    private var progressAnimationStart = 0f
    private var progressTime = 0f
    private var animatedProgress = 0f
    private var toCircle = false
    private var toCircleProgress = 0f
    private var noProgress = true
    // Test-only capture opt-in. Real attached loading spinners retain upstream arc motion.
    private var frozenForStaticSnapshot = false

    init {
        if (attrs != null) {
            val ns = "http://schemas.android.com/apk/res/android"
            val isIndet = attrs.getAttributeBooleanValue(ns, "indeterminate", true)
            val hasExplicitIndet = attrs.getAttributeValue(ns, "indeterminate") != null
            val max = attrs.getAttributeIntValue(ns, "max", 100).coerceAtLeast(1)
            val prog = attrs.getAttributeIntValue(ns, "progress", 0)
            val hasExplicitProgress = attrs.getAttributeValue(ns, "progress") != null
            val styleAttr = attrs.getAttributeValue(null, "style")
            val isRingStyle = styleAttr != null && styleAttr.contains("Ring")
            if ((hasExplicitIndet && !isIndet) || (!hasExplicitIndet && hasExplicitProgress) || isRingStyle) {
                noProgress = false
                currentProgress = (prog.toFloat() / max).coerceIn(0f, 1f)
                animatedProgress = currentProgress
                progressAnimationStart = currentProgress
                currentCircleLength = maxOf(4f, 360f * animatedProgress)
            }
        }
    }

    fun freezeForStaticSnapshot() {
        if (frozenForStaticSnapshot) return
        // The source's 85-ms advance yields a visible arc at the first software capture.
        updateAnimation(17L * 5L)
        frozenForStaticSnapshot = true
    }

    fun resumeFromStaticSnapshot() {
        frozenForStaticSnapshot = false
        lastUpdateTime = System.currentTimeMillis()
        invalidate()
    }

    fun isFrozenForStaticSnapshot(): Boolean = frozenForStaticSnapshot

    fun setUseSelfAlpha(value: Boolean) { useSelfAlpha = value }

    override fun setAlpha(alpha: Float) {
        super.setAlpha(alpha)
        if (useSelfAlpha) {
            background?.alpha = (alpha * 255).toInt()
            progressPaint.alpha = (alpha * 255).toInt()
        }
    }

    fun setNoProgress(value: Boolean) {
        if (noProgress == value) return
        noProgress = value
        invalidate()
    }

    fun setProgress(value: Float) {
        noProgress = false
        val next = value.coerceIn(0f, 1f)
        if (currentProgress == next && animatedProgress == next) return
        currentProgress = next
        if (animatedProgress > next) {
            animatedProgress = next
        }
        progressAnimationStart = animatedProgress
        progressTime = 0f
        invalidate()
    }

    fun setPercentage(percentage: Float) {
        setProgress(percentage / 100f)
    }

    var progress: Int
        get() = (currentProgress * 100).toInt()
        set(value) {
            setProgress(value / 100f)
        }

    val progressDrawable: android.graphics.drawable.Drawable? = null

    fun setIndicatorColor(color: Int) {
        setProgressColor(color)
    }
    fun sync(from: TelegramRadialProgressView) {
        lastUpdateTime = from.lastUpdateTime
        radOffset = from.radOffset
        toCircle = from.toCircle
        toCircleProgress = from.toCircleProgress
        noProgress = from.noProgress
        currentCircleLength = from.currentCircleLength
        drawingCircleLength = from.drawingCircleLength
        currentProgressTime = from.currentProgressTime
        currentProgress = from.currentProgress
        progressTime = from.progressTime
        animatedProgress = from.animatedProgress
        risingCircleLength = from.risingCircleLength
        progressAnimationStart = from.progressAnimationStart
        updateAnimation(17L * 5L)
    }

    private fun updateAnimation() {
        val now = System.currentTimeMillis()
        val dt = (now - lastUpdateTime).coerceAtMost(17L)
        lastUpdateTime = now
        updateAnimation(dt)
    }

    private fun updateAnimation(dt: Long) {
        if (toCircle && toCircleProgress != 1f) {
            toCircleProgress += 16f / 220f
            if (toCircleProgress > 1f) toCircleProgress = 1f
        } else if (!toCircle && toCircleProgress != 0f) {
            toCircleProgress -= 16f / 400f
            if (toCircleProgress < 0f) toCircleProgress = 0f
        }

        if (noProgress) {
            radOffset += 360f * dt / ROTATION_TIME
            val count = (radOffset / 360f).toInt()
            radOffset -= count * 360f

            if (toCircleProgress == 0f) {
                currentProgressTime += dt
                if (currentProgressTime >= RISING_TIME) currentProgressTime = RISING_TIME
                if (risingCircleLength) {
                    currentCircleLength = 4f + 266f * accelerateInterpolator.getInterpolation(currentProgressTime / RISING_TIME)
                } else {
                    currentCircleLength = 4f - 270f * (1f - decelerateInterpolator.getInterpolation(currentProgressTime / RISING_TIME))
                }
                if (currentProgressTime == RISING_TIME) {
                    if (risingCircleLength) {
                        radOffset += 270f
                        currentCircleLength = -266f
                    }
                    risingCircleLength = !risingCircleLength
                    currentProgressTime = 0f
                }
            } else {
                if (risingCircleLength) {
                    val old = currentCircleLength
                    currentCircleLength = 4f + 266f * accelerateInterpolator.getInterpolation(currentProgressTime / RISING_TIME)
                    currentCircleLength += 360f * toCircleProgress
                    val dx = old - currentCircleLength
                    if (dx > 0f) radOffset += old - currentCircleLength
                } else {
                    val old = currentCircleLength
                    currentCircleLength = 4f - 270f * (1f - decelerateInterpolator.getInterpolation(currentProgressTime / RISING_TIME))
                    currentCircleLength -= 364f * toCircleProgress
                    val dx = old - currentCircleLength
                    if (dx > 0f) radOffset += old - currentCircleLength
                }
            }
            if (!frozenForStaticSnapshot && isShown && isAttachedToWindow) invalidate()
        } else {
            radOffset = -90f
            val progressDiff = currentProgress - progressAnimationStart
            var isAnimating = false
            if (progressDiff > 0f) {
                progressTime += dt
                if (progressTime >= 200f) {
                    animatedProgress = currentProgress
                    progressAnimationStart = currentProgress
                    progressTime = 0f
                } else {
                    animatedProgress = progressAnimationStart + progressDiff *
                        decelerateInterpolator.getInterpolation(progressTime / 200f)
                    isAnimating = true
                }
            }
            currentCircleLength = maxOf(4f, 360f * animatedProgress)
            if (isAnimating && !frozenForStaticSnapshot) {
                invalidate()
            }
        }
    }

    fun setSize(value: Int) { size = value; invalidate() }
    fun setStrokeWidth(value: Float) { progressPaint.strokeWidth = dp(value) }
    fun setProgressColor(color: Int) { progressPaint.color = color; invalidate() }
    fun toCircle(value: Boolean, animated: Boolean) {
        toCircle = value
        if (!animated) toCircleProgress = if (value) 1f else 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val x = (measuredWidth - size) / 2f
        val y = (measuredHeight - size) / 2f
        circleRect.set(x, y, x + size, y + size)
        drawingCircleLength = currentCircleLength
        canvas.drawArc(circleRect, radOffset, drawingCircleLength, false, progressPaint)
        if (!frozenForStaticSnapshot) {
            val isSpinning = noProgress && isShown && isAttachedToWindow
            val isProgressAnimating = !noProgress && (animatedProgress < currentProgress || (toCircleProgress > 0f && toCircleProgress < 1f))
            if (isSpinning || isProgressAnimating) {
                updateAnimation()
            }
        }
    }

    fun drawAt(canvas: Canvas, cx: Float, cy: Float) {
        circleRect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        drawingCircleLength = currentCircleLength
        canvas.drawArc(circleRect, radOffset, drawingCircleLength, false, progressPaint)
        if (!frozenForStaticSnapshot) {
            val isSpinning = noProgress && isShown && isAttachedToWindow
            val isProgressAnimating = !noProgress && (animatedProgress < currentProgress || (toCircleProgress > 0f && toCircleProgress < 1f))
            if (isSpinning || isProgressAnimating) {
                updateAnimation()
            }
        }
    }

    fun isCircle(): Boolean = kotlin.math.abs(drawingCircleLength) >= 360f
    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val ROTATION_TIME = 2000f
        private const val RISING_TIME = 500f
    }
}

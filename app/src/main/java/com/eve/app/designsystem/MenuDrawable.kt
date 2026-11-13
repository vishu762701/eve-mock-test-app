/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/MenuDrawable.java:1-260 @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

class MenuDrawable(private var type: Int = TYPE_DEFAULT) : Drawable() {

    companion object {
        const val TYPE_DEFAULT = 0
        const val TYPE_UDPATE_AVAILABLE = 1
        const val TYPE_UDPATE_DOWNLOADING = 2
    }

    private val density = Resources.getSystem().displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dp(v: Int): Float = v * density

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2f)
    }
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = density * 1.66f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private var reverseAngle = false
    private var lastFrameTime = 0L
    private var finalRotation = 0f
    private var currentRotation = 0f
    private var currentAnimationTime = 0
    private var rotateToBack = true
    private val interpolator = DecelerateInterpolator()
    private var iconColor = Color.WHITE
    private var backColor = 0
    private var roundCap = false

    private var previousType = TYPE_DEFAULT
    private var typeAnimationProgress = 1.0f
    private var alpha = 255

    init {
        previousType = TYPE_DEFAULT
        typeAnimationProgress = 1.0f
    }

    fun setRotateToBack(value: Boolean) {
        rotateToBack = value
    }

    fun getCurrentRotation(): Float = currentRotation

    fun setIconColor(color: Int) {
        iconColor = color
        invalidateSelf()
    }

    fun setBackColor(color: Int) {
        backColor = color
        invalidateSelf()
    }

    fun setRotation(rotation: Float, animated: Boolean) {
        lastFrameTime = 0L
        if (currentRotation == 1f) {
            reverseAngle = true
        } else if (currentRotation == 0f) {
            reverseAngle = false
        }
        if (animated) {
            currentAnimationTime = if (currentRotation < rotation) {
                (currentRotation * 200).toInt()
            } else {
                ((1.0f - currentRotation) * 200).toInt()
            }
            lastFrameTime = SystemClock.uptimeMillis()
            finalRotation = rotation
        } else {
            finalRotation = rotation
            currentRotation = rotation
        }
        invalidateSelf()
    }

    fun setType(value: Int, animated: Boolean) {
        if (type == value) return
        previousType = type
        type = value
        typeAnimationProgress = if (animated) 0.0f else 1.0f
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val newTime = SystemClock.uptimeMillis()
        val dt = newTime - lastFrameTime
        if (currentRotation != finalRotation) {
            if (lastFrameTime != 0L) {
                currentAnimationTime += dt.toInt()
                if (currentAnimationTime >= 200) {
                    currentRotation = finalRotation
                } else {
                    currentRotation = if (currentRotation < finalRotation) {
                        interpolator.getInterpolation(currentAnimationTime / 200.0f) * finalRotation
                    } else {
                        1.0f - interpolator.getInterpolation(currentAnimationTime / 200.0f)
                    }
                }
            }
            invalidateSelf()
        }
        if (typeAnimationProgress < 1.0f) {
            typeAnimationProgress += dt / 200.0f
            if (typeAnimationProgress > 1.0f) {
                typeAnimationProgress = 1.0f
            }
            invalidateSelf()
        }
        lastFrameTime = newTime

        canvas.save()
        canvas.translate(intrinsicWidth / 2f - dp(9) - dp(1) * currentRotation, intrinsicHeight / 2f)

        var endYDiff: Float
        var endXDiff: Float
        var startYDiff: Float
        var startXDiff: Float

        val color1 = iconColor

        var diffUp = 0f
        var diffMiddle = 0f
        if (type == TYPE_DEFAULT) {
            if (previousType != TYPE_DEFAULT) {
                diffUp = dp(9) * (1.0f - typeAnimationProgress)
                diffMiddle = dp(7) * (1.0f - typeAnimationProgress)
            }
        } else {
            if (previousType == TYPE_DEFAULT) {
                diffUp = dp(9) * typeAnimationProgress * (1.0f - currentRotation)
                diffMiddle = dp(7) * typeAnimationProgress * (1.0f - currentRotation)
            } else {
                diffUp = dp(9) * (1.0f - currentRotation)
                diffMiddle = dp(7) * (1.0f - currentRotation)
            }
        }

        if (rotateToBack) {
            canvas.rotate(currentRotation * (if (reverseAngle) -180f else 180f), dp(9), 0f)
            paint.color = color1
            paint.alpha = alpha
            canvas.drawLine(
                if (roundCap) dp(0.5f) * currentRotation + (paint.strokeWidth / 2f) * (1f - currentRotation) else 0f,
                0f,
                dp(18) - dp(3.0f) * currentRotation - diffMiddle - (if (roundCap) (paint.strokeWidth / 2f) * (1f - currentRotation) else 0f),
                0f,
                paint
            )
            endYDiff = dp(5) * (1f - abs(currentRotation)) - dp(0.5f) * abs(currentRotation)
            endXDiff = dp(18) - dp(2.5f) * abs(currentRotation)
            startYDiff = dp(5) + dp(2.0f) * abs(currentRotation)
            startXDiff = dp(7.5f) * abs(currentRotation)
            if (roundCap) {
                startXDiff += (paint.strokeWidth / 2f) * (1f - currentRotation)
                endYDiff += dp(0.5f) * currentRotation
                endXDiff -= dp(0.5f) * currentRotation + (paint.strokeWidth / 2f) * (1f - currentRotation)
                startYDiff -= dp(0.25f) * currentRotation
                endYDiff += dp(0.25f) * currentRotation
            }
        } else {
            canvas.rotate(currentRotation * (if (reverseAngle) -225f else 135f), dp(9), 0f)
            paint.color = color1
            paint.alpha = alpha
            canvas.drawLine(dp(1) * currentRotation, 0f, dp(18) - dp(1) * currentRotation - diffMiddle, 0f, paint)
            endYDiff = dp(5) * (1f - abs(currentRotation)) - dp(0.5f) * abs(currentRotation)
            endXDiff = dp(18) - dp(9) * abs(currentRotation)
            startYDiff = dp(5) + dp(3) * abs(currentRotation)
            startXDiff = dp(9) * abs(currentRotation)
        }

        canvas.drawLine(startXDiff, -startYDiff, endXDiff - diffUp, -endYDiff, paint)
        canvas.drawLine(startXDiff, startYDiff, endXDiff, endYDiff, paint)

        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha
        paint.alpha = alpha
        backPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
        backPaint.colorFilter = cf
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    override fun getIntrinsicWidth(): Int = dp(24f).toInt()

    override fun getIntrinsicHeight(): Int = dp(24f).toInt()
}

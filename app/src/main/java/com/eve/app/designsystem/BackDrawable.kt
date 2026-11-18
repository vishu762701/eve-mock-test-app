/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BackDrawable.java:1-180 @ 3de37c8cfb039452be7398909461da6288babf15
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
import androidx.core.graphics.ColorUtils

class BackDrawable(private val alwaysClose: Boolean = false) : Drawable() {

    private val density = Resources.getSystem().displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(TelegramParity.BACK_DRAWABLE_STROKE_WIDTH_DP.toFloat())
        strokeCap = Paint.Cap.ROUND
    }

    private var reverseAngle = false
    private var lastFrameTime = 0L
    private var finalRotation = 0f
    private var currentRotation = 0f
    private var currentAnimationTime = 0
    private val interpolator = DecelerateInterpolator()
    private var color = Color.WHITE
    private var rotatedColor = 0xff757575.toInt()
    private var animationTime = TelegramParity.BACK_DRAWABLE_ANIMATION_TIME_MS.toFloat()
    private var rotated = true
    private var arrowRotation = 0

    fun getRotation(): Float = finalRotation

    fun setColor(value: Int) {
        color = value
        invalidateSelf()
    }

    fun setRotatedColor(value: Int) {
        rotatedColor = value
        invalidateSelf()
    }

    fun setArrowRotation(angle: Int) {
        arrowRotation = angle
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
                (currentRotation * animationTime).toInt()
            } else {
                ((1.0f - currentRotation) * animationTime).toInt()
            }
            lastFrameTime = SystemClock.uptimeMillis()
            finalRotation = rotation
        } else {
            finalRotation = rotation
            currentRotation = rotation
        }
        invalidateSelf()
    }

    fun setAnimationTime(value: Float) {
        animationTime = value
    }

    fun setRotated(value: Boolean) {
        rotated = value
    }

    override fun draw(canvas: Canvas) {
        if (currentRotation != finalRotation) {
            if (lastFrameTime != 0L) {
                val dt = SystemClock.uptimeMillis() - lastFrameTime
                currentAnimationTime += dt.toInt()
                if (currentAnimationTime >= animationTime) {
                    currentRotation = finalRotation
                } else {
                    currentRotation = if (currentRotation < finalRotation) {
                        interpolator.getInterpolation(currentAnimationTime / animationTime) * finalRotation
                    } else {
                        1.0f - interpolator.getInterpolation(currentAnimationTime / animationTime)
                    }
                }
            }
            lastFrameTime = SystemClock.uptimeMillis()
            invalidateSelf()
        }

        paint.color = ColorUtils.blendARGB(color, rotatedColor, currentRotation)

        canvas.save()
        canvas.translate(intrinsicWidth / 2f, intrinsicHeight / 2f)
        if (arrowRotation != 0) {
            canvas.rotate(arrowRotation.toFloat())
        }
        var rotation = currentRotation
        canvas.translate(-dp(0.66f), 0f)
        if (!alwaysClose) {
            canvas.rotate(currentRotation * (if (reverseAngle) -225f else 135f))
        } else {
            canvas.rotate(135f + currentRotation * (if (reverseAngle) -180f else 180f))
            rotation = 1.0f
        }
        canvas.drawLine(
            dp(lerp(-6.75f, -8f, rotation)),
            0f,
            dp(8f) - (paint.strokeWidth / 2f) * (1f - rotation),
            0f,
            paint
        )
        val startYDiff = dp(-0.25f)
        val endYDiff = dp(lerp(7f, 8f, rotation)) - (paint.strokeWidth / 4f) * (1f - rotation)
        val startXDiff = dp(lerp(-7f - 0.25f, 0f, rotation))
        val endXDiff = 0f
        canvas.drawLine(startXDiff, -startYDiff, endXDiff, -endYDiff, paint)
        canvas.drawLine(startXDiff, startYDiff, endXDiff, endYDiff, paint)
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        paint.colorFilter = cf
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    override fun getIntrinsicWidth(): Int = dp(24f).toInt()

    override fun getIntrinsicHeight(): Int = dp(24f).toInt()
}

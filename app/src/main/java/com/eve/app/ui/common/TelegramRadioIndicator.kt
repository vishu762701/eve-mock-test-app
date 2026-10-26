/*
 * Derived from Telegram Android RadioButton.java, Copyright Nikolai Kudashov,
 * 2013–2018. GNU GPL v2 or later. See assets/licenses/Telegram-GPL2.txt.
 * Verified upstream revision and compatibility changes: docs/telegram-radio-indicator.md.
 */
package com.eve.app.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable

/** Per-option paints and progress; native CompoundButton owns checked/accessibility state. */
internal class TelegramRadioIndicator(
    private val density: Float,
    private val extent: Int,
    private val motionEnabled: () -> Boolean
) : Drawable() {
    companion object { const val TRANSITION_MS = 200L }
    private fun dp(value: Int) = kotlin.math.ceil(value * density).toInt()
    private val size = dp(20) // RadioButtonCell's explicit size; preserve EVE's existing outer slot.
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dp(2).toFloat()
        style = Paint.Style.STROKE
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val eraser = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    internal var progress = 0f
        private set
    internal var uncheckedColor = Color.GRAY
        private set
    internal var checkedColor = Color.WHITE
        private set
    internal var animator: ValueAnimator? = null
        private set
    private var checked = false

    fun setColors(normal: Int, selected: Int) {
        uncheckedColor = normal
        checkedColor = selected
        invalidateSelf()
    }

    fun setChecked(value: Boolean, attached: Boolean) {
        if (value == checked) {
            if (!attached || !motionEnabled()) jumpToCurrentState()
            return
        }
        checked = value
        cancelAnimation()
        if (!attached || !motionEnabled()) {
            jumpToCurrentState()
            return
        }
        // ValueAnimator uses the same default accelerate/decelerate interpolation as ObjectAnimator.
        animator = ValueAnimator.ofFloat(progress, if (checked) 1f else 0f).apply {
            duration = TRANSITION_MS
            addUpdateListener { progress = it.animatedValue as Float; invalidateSelf() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animator === animation) animator = null
                }
            })
            start()
        }
    }

    private fun cancelAnimation() {
        val previous = animator
        animator = null
        previous?.cancel()
    }

    override fun jumpToCurrentState() {
        cancelAnimation()
        progress = if (checked) 1f else 0f
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (animator != null && !motionEnabled()) jumpToCurrentState()
        val circleProgress: Float
        val color: Int
        if (progress <= 0.5f) {
            circleProgress = progress / 0.5f
            color = uncheckedColor
        } else {
            circleProgress = 2f - progress / 0.5f
            val r = Color.red(uncheckedColor)
            val g = Color.green(uncheckedColor)
            val b = Color.blue(uncheckedColor)
            color = Color.rgb(
                r + ((Color.red(checkedColor) - r) * (1f - circleProgress)).toInt(),
                g + ((Color.green(checkedColor) - g) * (1f - circleProgress)).toInt(),
                b + ((Color.blue(checkedColor) - b) * (1f - circleProgress)).toInt()
            )
        }
        ring.color = color
        fill.color = color
        // CLEAR must erase only the indicator layer, never the answer tile or software canvas.
        val checkpoint = canvas.saveLayer(bounds.left.toFloat(), bounds.top.toFloat(),
            bounds.right.toFloat(), bounds.bottom.toFloat(), null)
        val x = bounds.exactCenterX()
        val y = bounds.exactCenterY()
        val radius = size / 2 - (1f + circleProgress) * density
        canvas.drawCircle(x, y, radius, ring)
        if (progress <= 0.5f) {
            canvas.drawCircle(x, y, radius - dp(1), fill)
            canvas.drawCircle(x, y, (radius - dp(1)) * (1f - circleProgress), eraser)
        } else {
            canvas.drawCircle(x, y, size / 4 + (radius - dp(1) - size / 4) * circleProgress, fill)
        }
        canvas.restoreToCount(checkpoint)
    }

    override fun getIntrinsicWidth() = extent
    override fun getIntrinsicHeight() = extent
    override fun setAlpha(alpha: Int) { ring.alpha = alpha; fill.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { ring.colorFilter = colorFilter; fill.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable contract")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.eve.app.R

/** Native dragging, checked state, animation scale and accessibility; exact iOS geometry. */
open class DesignSwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialSwitchStyle
) : SwitchCompat(context, attrs, defStyleAttr) {
    init {
        val width = resources.getDimensionPixelSize(R.dimen.design_switch_width)
        val height = resources.getDimensionPixelSize(R.dimen.design_switch_height)
        val thumb = resources.getDimension(R.dimen.design_switch_thumb)
        // Native SwitchCompat enforces width >= 2*thumbIntrinsicWidth + track padding.
        // Its thumb travel must be W-H, while its painted diameter stays D=27dp.
        val travel = width - height
        thumbTintList = null
        trackTintList = null
        thumbDrawable = SwitchPaint(context, travel, thumb.toInt(), thumb, false)
        trackDrawable = SwitchPaint(context, width, height, thumb, true)
        switchMinWidth = width
        minimumWidth = width
        minimumHeight = resources.getDimensionPixelSize(R.dimen.design_touch_min)
        switchPadding = resources.getDimensionPixelSize(R.dimen.design_chip_gap)
        showText = false
        splitTrack = false
        gravity = (gravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or Gravity.CENTER_VERTICAL
        background = null
        setPadding(0, 0, 0, 0)
        refreshDrawableState()
    }

    private class SwitchPaint(
        val context: Context,
        val intrinsicW: Int,
        val intrinsicH: Int,
        val diameter: Float,
        val track: Boolean
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var drawableAlpha = 255
        private var filter: ColorFilter? = null
        override fun getIntrinsicWidth(): Int = intrinsicW
        override fun getIntrinsicHeight(): Int = intrinsicH
        override fun isStateful(): Boolean = track
        override fun onStateChange(state: IntArray): Boolean {
            invalidateSelf()
            return true
        }
        override fun getPadding(padding: Rect): Boolean {
            if (!track) { padding.setEmpty(); return false }
            val total = 2 * intrinsicH - intrinsicW
            padding.set(total / 2, 0, total - total / 2, 0)
            return true
        }
        override fun draw(canvas: Canvas) {
            val checked = state.contains(android.R.attr.state_checked)
            paint.color = ContextCompat.getColor(context, if (track) {
                if (checked) R.color.system_green else R.color.track
            } else R.color.solid_white)
            paint.alpha = (android.graphics.Color.alpha(paint.color) * drawableAlpha / 255)
            paint.colorFilter = filter
            if (track) canvas.drawRoundRect(bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.bottom.toFloat(), bounds.height() / 2f, bounds.height() / 2f, paint)
            else canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), diameter / 2f, paint)
        }
        override fun setAlpha(alpha: Int) { drawableAlpha = alpha; invalidateSelf() }
        override fun setColorFilter(colorFilter: ColorFilter?) { filter = colorFilter; invalidateSelf() }
        @Deprecated("Deprecated by Android")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}

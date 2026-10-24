package com.eve.app.designsystem

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.SystemClock

/**
 * Drawable bridge around source-faithful EVE RadialProgressView.
 * No new arc maths: delegate to TelegramRadialProgressView.drawAt().
 * For MaterialButton.icon, request invalidations at the original ~16ms cadence.
 */
class TelegramRadialProgressIcon(context: Context) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val iconSize = (20f * density).toInt()
    private val radial = TelegramRadialProgressView(context).apply {
        setSize(iconSize)
        setStrokeWidth(3f)
    }
    private val frame = Runnable { if (running) invalidateSelf() }
    private var running = false

    fun setProgressColor(color: Int) {
        radial.setProgressColor(color)
        invalidateSelf()
    }

    fun start() {
        if (running) return
        running = true
        invalidateSelf()
    }

    fun stop() {
        running = false
        unscheduleSelf(frame)
    }

    override fun draw(canvas: Canvas) {
        if (!running) return
        radial.drawAt(canvas, bounds.exactCenterX(), bounds.exactCenterY())
        unscheduleSelf(frame)
        scheduleSelf(frame, SystemClock.uptimeMillis() + 16L)
    }

    override fun getIntrinsicWidth(): Int = iconSize
    override fun getIntrinsicHeight(): Int = iconSize

    override fun setAlpha(alpha: Int) {
        radial.alpha = alpha / 255f
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // No tint filter: color is set explicitly to the owning button's text color.
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

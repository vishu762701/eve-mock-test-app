package com.eve.app.telegram.animation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import org.telegram.ui.Components.RLottieNative

/** EVE Drawable/lifecycle adapter around the unchanged Telegram native frame renderer. */
internal class NativeQrDrawable private constructor(
    private val animation: RLottieNative,
    size: Int
) : Drawable(), Animatable, AutoCloseable {
    private val frame = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var startedAt = 0L
    private var running = false
    private var closed = false
    private var renderedFrame = -1
    private val tick = Runnable { invalidateSelf() }

    override fun draw(canvas: Canvas) {
        if (closed) return
        val index = if (running) {
            (((SystemClock.uptimeMillis() - startedAt) * animation.fps / 1000.0).toInt()) %
                animation.frameCount.coerceAtLeast(1)
        } else renderedFrame.coerceAtLeast(0)
        if (index != renderedFrame) {
            animation.getFrame(index, frame, true)
            renderedFrame = index
        }
        canvas.drawBitmap(frame, null, bounds, paint)
        if (running) {
            unscheduleSelf(tick)
            scheduleSelf(tick, SystemClock.uptimeMillis() + (1000 / animation.fps.coerceAtLeast(1)))
        }
    }

    override fun start() {
        if (closed || running) return
        startedAt = SystemClock.uptimeMillis()
        running = true
        invalidateSelf()
    }
    override fun stop() { running = false; unscheduleSelf(tick) }
    override fun isRunning() = running
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable opacity is not used")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
    override fun close() {
        if (closed) return
        stop()
        closed = true
        animation.recycle()
        frame.recycle()
        callback = null
    }

    companion object {
        fun fromJson(json: String, size: Int): NativeQrDrawable {
            require(size in 1..1024)
            TelegramAnimationRuntime.ensureLoaded()
            val native = requireNotNull(RLottieNative.createFromRawJson(json)) { "Invalid QR animation" }
            return try { NativeQrDrawable(native, size) } catch (failure: Throwable) {
                native.recycle()
                throw failure
            }
        }
    }
}

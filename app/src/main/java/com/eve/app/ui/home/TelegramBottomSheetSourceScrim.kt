/*
 * UI-only source adaptation from official Telegram Android BottomSheet.java:135-192.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later.
 *
 * Original Paint/PorterDuff SRC/bounds/insets/alpha logic is retained.
 * Telegram's global blur-capture flag is deliberately not imported; EVE has
 * its own scoped render/blur pipeline. This is NOT the complete BottomSheet.
 */
package com.eve.app.ui.home

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.Drawable

internal class TelegramBottomSheetSourceScrim : Drawable() {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        color = Color.BLACK
    }
    val insets = Rect()
    private val boundsWithInsets = Rect()

    fun setBackgroundInsets(left: Int, top: Int, right: Int, bottom: Int) {
        if (insets.left != left || insets.top != top ||
            insets.right != right || insets.bottom != bottom) {
            insets.set(left, top, right, bottom)
            onBoundsChange(bounds)
            invalidateSelf()
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        boundsWithInsets.set(bounds)
        boundsWithInsets.left += maxOf(0, insets.left)
        boundsWithInsets.top += maxOf(0, insets.top)
        boundsWithInsets.right -= maxOf(0, insets.right)
        boundsWithInsets.bottom -= maxOf(0, insets.bottom)
    }

    override fun draw(canvas: Canvas) {
        if (boundsWithInsets.isEmpty || alpha == 0) return
        canvas.drawRect(boundsWithInsets, bgPaint)
    }

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun getAlpha(): Int = bgPaint.alpha

    override fun setColorFilter(filter: ColorFilter?) {
        bgPaint.colorFilter = filter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.UNKNOWN
}

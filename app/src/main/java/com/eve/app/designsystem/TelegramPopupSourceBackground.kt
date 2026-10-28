package com.eve.app.designsystem

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

/**
 * Isolates the no-gap/no-swipe original Telegram popup background-only growth.
 *
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * ActionBarPopupWindow.java:467-539 (GPL-2-or-later).
 * Unmodified upstream NinePatch drawable bytes are supplied by the caller.
 *
 * Original ActionBarPopupWindowLayout (no gaps, shownFromBottom=false, swipeBack=null):
 * rectTmp2.set(0, 0, width, (int) (height * backScaleY));
 * backgroundDrawable.setAlpha(backAlpha);
 * backgroundDrawable.setBounds(rectTmp2); backgroundDrawable.draw(canvas).
 *
 * EVE does not port Telegram's gap, reactions, swipe-back or service dependencies.
 */
class TelegramPopupSourceBackground(val sourceDrawable: Drawable) : Drawable() {
    var backScaleY: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidateSelf()
        }

    var backAlpha: Int = 255
        set(value) {
            field = value.coerceIn(0, 255)
            invalidateSelf()
        }

    fun visibleHeight(totalHeight: Int): Int =
        (totalHeight * backScaleY).toInt().coerceIn(0, totalHeight)

    override fun draw(canvas: Canvas) {
        val box = bounds
        val y = visibleHeight(box.height())
        if (y == 0) return
        sourceDrawable.alpha = backAlpha
        sourceDrawable.setBounds(box.left, box.top, box.right, box.top + y)
        sourceDrawable.draw(canvas)
    }

    override fun getPadding(padding: Rect): Boolean = sourceDrawable.getPadding(padding)
    override fun setAlpha(alpha: Int) { backAlpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) {
        sourceDrawable.colorFilter = colorFilter
        invalidateSelf()
    }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

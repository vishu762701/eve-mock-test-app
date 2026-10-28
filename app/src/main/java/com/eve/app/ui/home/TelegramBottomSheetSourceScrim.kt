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

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.WindowManager
import com.eve.app.designsystem.UiArcMotion
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.telegram.ui.Components.CubicBezierInterpolator
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

/**
 * EVE Material host adaptation of original Telegram BottomSheet.java:135-192,
 * 213-222,1766-1778; not a claim of source-exact gesture/detent physics.
 */
internal object TelegramBottomSheetSourceBackdrop {
    fun install(dialog: BottomSheetDialog, context: Context) {
        val host = dialog.findViewById<View>(com.google.android.material.R.id.coordinator) ?: return
        val scrim = TelegramBottomSheetSourceScrim().apply { alpha = 0 }
        host.background = scrim
        dialog.window?.let { window ->
            window.setDimAmount(0f)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        if (!UiArcMotion.isMotionEnabled(context)) {
            scrim.alpha = 51
        } else {
            val fade = ValueAnimator.ofInt(0, 51).apply {
                duration = 400L
                startDelay = 20L
                interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
                addUpdateListener { scrim.alpha = it.animatedValue as Int }
            }
            host.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    fade.cancel()
                    v.removeOnAttachStateChangeListener(this)
                }
            })
            fade.start()
        }
    }
}

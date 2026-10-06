package com.eve.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.ViewGroup
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur

/**
 * Unified Apple-style Real-time Frosted Glass Blur Helper (Section 2c).
 * Single source of truth for:
 * - View-level RenderEffect blur on Android 12+ (API 31+) with CLAMP tile mode.
 * - Hardware WindowManager blur-behind for dialogs, popups, and bottom sheets.
 * - BlurView setup with hardware RenderEffect engine and legacy fallback.
 * - Fast StackBlur bitmap fallback for custom drawer & surface rendering.
 *
 * Rule: NEVER blur content (text, icons, Lotties, photos), ONLY chrome surfaces.
 */
object EveBlurHelper {

    const val DEFAULT_BLUR_RADIUS_DP = 16f
    const val WINDOW_BLUR_RADIUS = GlassmorphismHelper.DEFAULT_BLUR_RADIUS

    /**
     * Applies RenderEffect blur directly to chrome background surface on API 31+.
     */
    fun applyRenderEffect(view: View, radiusPx: Float = 24f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (radiusPx > 0f) {
                    val effect = RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
                    view.setRenderEffect(effect)
                } else {
                    view.setRenderEffect(null)
                }
            } catch (_: Throwable) {
                // Safe graceful fallback
            }
        }
    }

    /**
     * Clears RenderEffect on API 31+.
     */
    fun clearRenderEffect(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(null)
            } catch (_: Throwable) { }
        }
    }

    /**
     * Configures a real-time BlurView surface.
     */
    fun setupBlurView(
        blurView: BlurView,
        rootView: ViewGroup,
        radiusDp: Float = DEFAULT_BLUR_RADIUS_DP,
        overlayColor: Int? = null
    ) {
        try {
            val facade = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                blurView.setupWith(rootView, RenderEffectBlur())
            } else {
                blurView.setupWith(rootView)
            }

            facade.setFrameClearDrawable(blurView.background)
                .setBlurRadius(radiusDp.coerceIn(1f, 25f))
                .setBlurAutoUpdate(true)

            if (overlayColor != null) {
                facade.setOverlayColor(overlayColor)
            }
        } catch (t: Throwable) {
            android.util.Log.w("EveBlurHelper", "BlurView setup skipped: ${t.message}")
        }
    }

    /**
     * WindowManager blur-behind for popups, dialogs, and bottom sheets.
     */
    fun applyWindowBlur(view: View, blurRadius: Int = WINDOW_BLUR_RADIUS, animate: Boolean = true) {
        GlassmorphismHelper.applyWindowBlur(view, blurRadius, animate)
    }

    fun removeWindowBlur(view: View, animate: Boolean = true) {
        GlassmorphismHelper.removeWindowBlur(view, animate)
    }

    /**
     * Bitmap fallback blur for offscreen/drawer surfaces.
     */
    fun blurBitmap(sentBitmap: Bitmap, radius: Int = 16): Bitmap? {
        return FastBlurHelper.blur(sentBitmap, radius)
    }
}

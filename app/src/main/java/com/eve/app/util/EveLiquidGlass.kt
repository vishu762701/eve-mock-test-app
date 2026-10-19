package com.eve.app.util

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import com.eve.app.R

/**
 * Unified Apple-Inspired Liquid Glass Regular Material (EVE_REGULAR_GLASS).
 *
 * Implements one adaptive semantic material across Light and Dark modes:
 * - Light Mode: 90% luminous neutral tint (#E6FFFFFF) with 1dp subtle rim (#1A000000).
 * - Dark Mode: 90% deep neutral tint (#E61C1C1E) with 1dp subtle rim (#26FFFFFF).
 * - Geometry: Shared modal radius (34dp) and capsule radius (50%).
 * - Hardware Blur: SurfaceFlinger cross-window background blur (API 31+).
 * - Foreground: Sharp primary labels and controls with zero blur and >= 4.5:1 contrast.
 * - Fallback: Opaque secondarySystemBackground (#F2F2F7 light / #1C1C1E dark) on API < 31.
 */
object EveLiquidGlass {

    const val MATERIAL_ID = "apple-regular"
    const val VARIANT = "Liquid Glass .regular"

    /**
     * Checks if real-time hardware cross-window blur is supported and enabled by the device compositor.
     */
    fun isCrossWindowBlurSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return false
        return try {
            wm.isCrossWindowBlurEnabled
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Applies the EVE_REGULAR_GLASS specification to a floating Dialog Window.
     * On API 31+, applies window background blur and the translucent regular material.
     * On API < 31, gracefully retains the opaque fallback.
     */
    fun applyToWindow(window: Window, @Px blurRadiusPx: Int = 80) {
        val context = window.context
        window.setBackgroundDrawableResource(R.drawable.bg_dialog_liquid_glass)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (isCrossWindowBlurSupported(context)) {
                    window.setBackgroundBlurRadius(blurRadiusPx)
                }
            } catch (_: Throwable) {
                // Graceful fallback: window background drawable renders cleanly without blur
            }
        }
    }

    /**
     * Applies the EVE_REGULAR_GLASS specification to a Dialog instance.
     */
    fun applyToDialog(dialog: Dialog, @Px blurRadiusPx: Int = 80) {
        dialog.window?.let { applyToWindow(it, blurRadiusPx) }
    }

    /**
     * Applies the EVE_REGULAR_GLASS specification to the Mock Test submission confirmation dialog.
     * On API 31+, registers a lifecycle-safe CrossWindowBlurEnabledListener to adaptively switch between
     * hardware window blur and an opaque semantic surface if transparency is disabled or toggled at runtime.
     * On dismissal, all listeners are released to prevent memory leaks.
     */
    fun applySubmitConfirmationGlass(dialog: Dialog, @Px blurRadiusPx: Int? = null) {
        val window = dialog.window ?: return
        val context = dialog.context
        val density = context.resources.displayMetrics.density
        val radiusPx = blurRadiusPx ?: Math.round(16f * density)

        window.setBackgroundDrawableResource(R.drawable.bg_dialog_submit_confirmation)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val mainExecutor = ContextCompat.getMainExecutor(context)

            fun updateSurface(blurEnabled: Boolean) {
                try {
                    if (blurEnabled) {
                        window.setBackgroundBlurRadius(radiusPx)
                        window.setBackgroundDrawableResource(R.drawable.bg_dialog_submit_confirmation)
                    } else {
                        window.setBackgroundBlurRadius(0)
                        val drawable = ContextCompat.getDrawable(context, R.drawable.bg_dialog_submit_confirmation)?.mutate()
                        if (drawable is android.graphics.drawable.InsetDrawable) {
                            val inner = drawable.drawable as? android.graphics.drawable.GradientDrawable
                            inner?.setColor(ContextCompat.getColor(context, R.color.surface_inset))
                        }
                        window.setBackgroundDrawable(drawable)
                    }
                } catch (_: Throwable) {
                    // Graceful fallback
                }
            }

            val isSupported = isCrossWindowBlurSupported(context)
            updateSurface(isSupported)

            if (wm != null) {
                val blurListener = java.util.function.Consumer<Boolean> { enabled ->
                    updateSurface(enabled)
                }
                try {
                    wm.addCrossWindowBlurEnabledListener(mainExecutor, blurListener)
                    dialog.setOnDismissListener {
                        try {
                            wm.removeCrossWindowBlurEnabledListener(blurListener)
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    /**
     * Returns the appropriate Liquid Glass Regular tint for the current theme context.
     */
    @ColorInt
    fun getRegularTint(context: Context): Int {
        return ContextCompat.getColor(context, R.color.glass_fallback)
    }

    /**
     * Returns the rim stroke border color for the current theme context.
     */
    @ColorInt
    fun getRimBorderColor(context: Context): Int {
        return ContextCompat.getColor(context, R.color.hairline)
    }
}

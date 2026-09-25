package com.eve.app.util

import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager

/**
 * Utility to apply Glassmorphism / Frosted Glass UI on Android.
 * Supports Window Blur Behind on Android 12+ (API 31+) and safe fallbacks.
 */
object GlassmorphismHelper {

    fun applyWindowBlur(view: View, blurRadius: Int = 28) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.post {
                try {
                    val root = view.rootView
                    val lp = root.layoutParams as? WindowManager.LayoutParams
                    if (lp != null) {
                        lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                        lp.blurBehindRadius = blurRadius
                        val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                        wm?.updateViewLayout(root, lp)
                    }
                } catch (_: Throwable) {
                    // Safe fallback if window manager does not permit layout updates
                }
            }
        }
    }
}

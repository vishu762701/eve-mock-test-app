package com.eve.app.util

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Task C: Premium Glassmorphism / Frosted Glass Effect utility.
 * Applies hardware-accelerated WindowManager blur-behind on Android 12+ (API 31+)
 * with smooth animated blur-in on open and blur-out on close.
 * Provides graceful fallback for API < 31.
 */
object GlassmorphismHelper {

    const val DEFAULT_BLUR_RADIUS = 32

    fun applyWindowBlur(view: View, blurRadius: Int = DEFAULT_BLUR_RADIUS, animate: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.post {
                try {
                    val root = view.rootView
                    val lp = root.layoutParams as? WindowManager.LayoutParams ?: return@post
                    val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

                    lp.flags = (lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND) and
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                    lp.dimAmount = 0.0f

                    if (animate) {
                        lp.blurBehindRadius = 1
                        wm.updateViewLayout(root, lp)

                        ValueAnimator.ofInt(1, blurRadius).apply {
                            duration = 200L
                            interpolator = FastOutSlowInInterpolator()
                            addUpdateListener { va ->
                                try {
                                    lp.blurBehindRadius = va.animatedValue as Int
                                    wm.updateViewLayout(root, lp)
                                } catch (_: Throwable) { }
                            }
                            start()
                        }
                    } else {
                        lp.blurBehindRadius = blurRadius
                        wm.updateViewLayout(root, lp)
                    }
                } catch (_: Throwable) {
                    // Safe fallback
                }
            }
        }
    }

    fun removeWindowBlur(view: View, animate: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val root = view.rootView
                val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
                val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
                val currentRadius = lp.blurBehindRadius

                if (animate && currentRadius > 0) {
                    ValueAnimator.ofInt(currentRadius, 0).apply {
                        duration = 150L
                        interpolator = FastOutSlowInInterpolator()
                        addUpdateListener { va ->
                            try {
                                lp.blurBehindRadius = va.animatedValue as Int
                                wm.updateViewLayout(root, lp)
                            } catch (_: Throwable) { }
                        }
                        start()
                    }
                } else {
                    lp.blurBehindRadius = 0
                    wm.updateViewLayout(root, lp)
                }
            } catch (_: Throwable) {
                // Safe fallback
            }
        }
    }
}

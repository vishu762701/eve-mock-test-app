package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Global system bar / top inset handler.
 * Fixes edge-to-edge / status bar overlap at root cause across all activities:
 * - Content starts directly below the system status bar.
 * - Prevents headers, titles, profile avatars, tabs, and action buttons from being clipped or overlapping status bar icons.
 * - Dynamically adapts to notch, hole-punch cutouts, rotation, and Android 15 edge-to-edge enforcement.
 */
object SystemBarHelper {

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applySystemBarInsets(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                applySystemBarInsets(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                applySystemBarInsets(activity)
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun applySystemBarInsets(activity: Activity) {
        val window = activity.window ?: return
        val contentView = activity.findViewById<View>(android.R.id.content)
        if (contentView != null) {
            setupInsetsListener(contentView)
        } else {
            window.decorView.post {
                val postedContent = activity.findViewById<View>(android.R.id.content) ?: return@post
                setupInsetsListener(postedContent)
            }
        }
    }

    private fun setupInsetsListener(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            if (v.paddingTop != statusBars.top) {
                v.setPadding(0, statusBars.top, 0, 0)
            }
            insets
        }
        ViewCompat.requestApplyInsets(view)
    }
}

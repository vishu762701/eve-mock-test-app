package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.eve.app.R

/**
 * Global system bar / top inset and appearance handler.
 * Centralizes status bar and navigation bar insets, icon appearance, and theme synchronization:
 * - Content starts directly below the system status bar.
 * - Prevents headers, titles, profile avatars, tabs, and action buttons from being clipped or overlapping status bar icons.
 * - Dynamically adapts to notch, hole-punch cutouts, rotation, and Android 15 edge-to-edge enforcement.
 * - Synchronizes status bar / navigation bar appearance during theme switches with zero flash or glitch.
 */
object SystemBarHelper {

    fun init(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                applySystemBarInsets(activity)
                syncSystemBars(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                applySystemBarInsets(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                applySystemBarInsets(activity)
                syncSystemBars(activity)
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

    /**
     * Centralized system-bar appearance updater.
     * Keeps status bar background and icon appearance synchronized with the visible theme,
     * preventing any flash, wrong background, or transient icon glitch during transitions.
     */
    fun syncSystemBars(activity: Activity) {
        val window = activity.window ?: return
        val decorView = window.decorView

        val activeState = ThemeSwitchAnimator.getActiveTransitionState()

        // Match the target appearance immediately so the status bar and notch never linger in the old theme
        val isDarkAppearance = activeState?.isDarkModeTarget ?: ThemeSwitchAnimator.isDarkMode(activity)

        val controller = WindowInsetsControllerCompat(window, decorView)
        controller.isAppearanceLightStatusBars = !isDarkAppearance
        controller.isAppearanceLightNavigationBars = !isDarkAppearance

        try {
            val statusBarColor = ContextCompat.getColor(
                activity,
                if (isDarkAppearance) R.color.eve_header_ink else R.color.eve_bg
            )
            val navBarColor = ContextCompat.getColor(
                activity,
                if (isDarkAppearance) R.color.eve_canvas else R.color.eve_canvas
            )
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
        } catch (_: Exception) {}
    }
}

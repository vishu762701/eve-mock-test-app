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
 * - Matches system-bar appearance to each activity’s visible theme.
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

    internal fun setupInsetsListener(view: View) {
        if (view.getTag(R.id.eve_system_insets_installed) == true) return
        view.setTag(R.id.eve_system_insets_installed, true)
        val left = view.paddingLeft
        val top = view.paddingTop
        val right = view.paddingRight
        val bottom = view.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            // The content container handled these edges. Keep child listeners,
            // but don't make them add the same navigation padding a second time.
            insets.inset(bars.left, bars.top, bars.right, bars.bottom)
        }
        ViewCompat.requestApplyInsets(view)
    }

    /**
     * Centralized system-bar appearance updater.
     * Keeps status bar background and icon appearance synchronized with the visible theme,
     * without resolving target-theme colors from old activity resources.
     */
    fun syncSystemBars(activity: Activity) {
        val window = activity.window ?: return
        val decorView = window.decorView

        val isDarkAppearance = activity.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val background = ContextCompat.getColor(activity, R.color.eve_bg)
        if (android.os.Build.VERSION.SDK_INT >= 35) {
            // Android 15 derives forced navigation appearance from the window
            // background under enforced edge-to-edge. The regular light-icon
            // flag alone cannot correct a stale light window background.
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(background))
            // PhoneWindow's public setter also clears Android 15's forced-light
            // navigation appearance. Use transparent for enforced edge-to-edge;
            // setting an already-forced opaque color can return without clearing it.
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        } else {
            window.statusBarColor = background
            window.navigationBarColor = ContextCompat.getColor(activity, R.color.eve_canvas)
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowInsetsControllerCompat(window, decorView)
        controller.isAppearanceLightStatusBars = !isDarkAppearance
        controller.isAppearanceLightNavigationBars = !isDarkAppearance
    }
}

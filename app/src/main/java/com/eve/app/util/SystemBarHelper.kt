package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.eve.app.R
import com.eve.app.ui.about.ContentDisplayActivity
import com.eve.app.ui.home.MainActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.test.TestActivity

/**
 * Global system bar / top inset and appearance handler.
 * Centralizes status bar and navigation bar insets, icon appearance, and theme synchronization:
 * - Content starts directly below the system status bar.
 * - Prevents headers, titles, profile avatars, tabs, and action buttons from being clipped or overlapping status bar icons.
 * - Dynamically adapts to notch, hole-punch cutouts, rotation, and Android 15 edge-to-edge enforcement.
 * - Matches system-bar appearance and canvas backing to each screen's semantic role from one source of truth.
 */
object SystemBarHelper {

    enum class ScreenCanvasRole(@ColorRes val colorRes: Int) {
        PLAIN(R.color.bg_app),
        GROUPED(R.color.bg_app)
    }

    /**
     * Authoritative set of plain-canvas screens (Home, Login, active Test, plain Content Display).
     * All other production screens (Settings, Profile, Result, Full Leaderboard, Notifications,
     * About, student grouped lists/dashboards, and all Admin screens) use the grouped canvas.
     */
    val PLAIN_CANVAS_ACTIVITY_NAMES: Set<String> = setOf(
        MainActivity::class.java.name,
        LoginActivity::class.java.name,
        TestActivity::class.java.name,
        ContentDisplayActivity::class.java.name
    )

    val PLAIN_CANVAS_LAYOUT_NAMES: Set<String> = setOf(
        "activity_main.xml",
        "activity_login.xml",
        "activity_test.xml",
        "activity_content_display.xml"
    )

    fun resolveCanvasRole(activity: Activity): ScreenCanvasRole {
        if (activity is MainActivity ||
            activity is LoginActivity ||
            activity is TestActivity ||
            activity is ContentDisplayActivity ||
            activity.javaClass.name in PLAIN_CANVAS_ACTIVITY_NAMES
        ) {
            return ScreenCanvasRole.PLAIN
        }
        // For debug/instrumentation host activities that dynamically swap layouts via setContentView:
        if (activity.javaClass.simpleName == "RepairVerificationActivity") {
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val root = content?.getChildAt(0) ?: return ScreenCanvasRole.PLAIN
            if (root.findViewById<View>(R.id.drawerLayout) != null ||
                root.findViewById<View>(R.id.mainContentContainer) != null ||
                root.findViewById<View>(R.id.cardLogin) != null ||
                root.findViewById<View>(R.id.circularTimerView) != null ||
                root.findViewById<View>(R.id.tvHeaderTitle) != null
            ) {
                return ScreenCanvasRole.PLAIN
            }
            if (root.findViewById<View>(R.id.tabLayoutResult) != null ||
                root.findViewById<View>(R.id.ivProfilePhoto) != null ||
                root.findViewById<View>(R.id.switchReminder) != null ||
                root.findViewById<View>(R.id.rvLeaderboard) != null ||
                root.findViewById<View>(R.id.rvNotifications) != null ||
                root.findViewById<View>(R.id.tabLayoutAdmin) != null
            ) {
                return ScreenCanvasRole.GROUPED
            }
            val lightGrouped = (root.background as? android.graphics.drawable.ColorDrawable)?.color
            if (lightGrouped != null && lightGrouped == ContextCompat.getColor(activity, R.color.bg_app) &&
                lightGrouped != ContextCompat.getColor(activity, R.color.bg_app)
            ) {
                return ScreenCanvasRole.GROUPED
            }
            return ScreenCanvasRole.PLAIN
        }
        return ScreenCanvasRole.GROUPED
    }

    fun resolveCanvasRoleForNames(activityName: String, layoutName: String? = null): ScreenCanvasRole {
        val cleanActivity = activityName.substringAfterLast('.')
        val cleanLayout = layoutName?.let { if (it.endsWith(".xml")) it else "$it.xml" }
        if (cleanActivity in setOf("MainActivity", "LoginActivity", "TestActivity", "ContentDisplayActivity") ||
            activityName in PLAIN_CANVAS_ACTIVITY_NAMES ||
            (cleanLayout != null && cleanLayout in PLAIN_CANVAS_LAYOUT_NAMES)
        ) {
            return ScreenCanvasRole.PLAIN
        }
        return ScreenCanvasRole.GROUPED
    }

    fun isGroupedScreen(activity: Activity): Boolean =
        resolveCanvasRole(activity) == ScreenCanvasRole.GROUPED

    @ColorRes
    fun canvasColorRes(activity: Activity): Int = resolveCanvasRole(activity).colorRes

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
    fun syncSystemBars(activity: Activity, force: Boolean = false) {
        val window = activity.window ?: return
        val decorView = window.decorView

        val isDarkAppearance = activity.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val background = ContextCompat.getColor(activity, R.color.bg_app)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(background))
        activity.findViewById<View>(android.R.id.content)?.setBackgroundColor(background)
        window.statusBarColor = ContextCompat.getColor(activity, R.color.transparent)
        // API 24–25 has no dark navigation-button icons. Owner-approved fallback.
        window.navigationBarColor = ContextCompat.getColor(activity,
            if (android.os.Build.VERSION.SDK_INT < 26 && !isDarkAppearance)
                R.color.navigation_legacy_background else R.color.transparent)
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        val controller = WindowInsetsControllerCompat(window, decorView)
        val activeTargetIsDark = ThemeManager.activeTargetIsDark
        val iconDark = if (ThemeManager.isTransitioning && !force && activeTargetIsDark != null) {
            // While pre_reveal_overlay is showing the old screen snapshot, keep icons matching that appearance
            !activeTargetIsDark
        } else {
            isDarkAppearance
        }
        controller.isAppearanceLightStatusBars = !iconDark
        controller.isAppearanceLightNavigationBars = !iconDark
    }
}

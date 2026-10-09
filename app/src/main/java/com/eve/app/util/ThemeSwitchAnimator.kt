package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import kotlin.math.hypot
import kotlin.math.max

/**
 * ThemeSwitchAnimator: Coordinates Day/Night theme transitions with Eve's
 * Telegram-style circular reveal animation hosted by ThemeManager.
 */
object ThemeSwitchAnimator {

    const val TAG = "ThemeSwitchAnimator"
    const val PREFS = "eve_prefs"
    const val KEY_DARK_MODE = "key_dark_mode"
    const val ANIMATION_DURATION = 400L

    private var inTransitionForTest = false

    val isTransitioning: Boolean
        get() = inTransitionForTest || ThemeManager.isTransitioning

    fun isDarkMode(context: Context): Boolean {
        var current: Context? = context
        while (current is android.content.ContextWrapper) {
            if (current is Activity) {
                return current.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            }
            current = current.baseContext
        }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK_MODE)) {
            prefs.getBoolean(KEY_DARK_MODE, false)
        } else {
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun ensureLifecycleRegistered(app: Application) {
        ThemeManager.ensureLifecycleRegistered(app)
    }

    fun calculateIconCenter(view: View): Pair<Int, Int> {
        val pos = IntArray(2)
        view.getLocationOnScreen(pos)
        val w = if (view.measuredWidth > 0) view.measuredWidth else view.width
        val h = if (view.measuredHeight > 0) view.measuredHeight else view.height
        return (pos[0] + w / 2) to (pos[1] + h / 2)
    }

    fun animate(
        activity: Activity,
        clickView: View,
        isDarkModeTarget: Boolean,
        staticIconView: View? = null
    ) {
        val (cx, cy) = calculateIconCenter(clickView)
        animateAt(activity, cx, cy, isDarkModeTarget, staticIconView = staticIconView)
    }

    fun animateAt(
        activity: Activity,
        clickScreenX: Int,
        clickScreenY: Int,
        isDarkModeTarget: Boolean,
        clickWidth: Int = 48,
        clickHeight: Int = 48,
        staticIconView: View? = null
    ) {
        ThemeManager.toggleWithCircularReveal(activity, clickScreenX, clickScreenY, isDarkModeTarget) {
            switch(activity, isDarkModeTarget)
        }
    }

    fun toggle(activity: Activity, view: View) {
        animate(activity, view, !isDarkMode(activity))
    }

    fun toggleAt(activity: Activity, cx: Int, cy: Int) {
        animateAt(activity, cx, cy, !isDarkMode(activity))
    }

    fun switch(activity: Activity, target: Boolean) {
        if (activity.isFinishing || activity.isDestroyed) return
        ensureLifecycleRegistered(activity.application)
        persistThemePreference(activity, target)
        val mode = if (target) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        activity.overridePendingTransition(0, 0)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun persistThemePreference(context: Context, isDarkMode: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, isDarkMode)
            .apply()
    }

    fun calculateMaxRadius(cx: Float, cy: Float, w: Float, h: Float): Float {
        val d1 = hypot(cx.toDouble(), cy.toDouble())
        val d2 = hypot((w - cx).toDouble(), cy.toDouble())
        val d3 = hypot(cx.toDouble(), (h - cy).toDouble())
        val d4 = hypot((w - cx).toDouble(), (h - cy).toDouble())
        return max(max(d1, d2), max(d3, d4)).toFloat()
    }

    fun getRevealRadii(isDarkModeTarget: Boolean, maxRadius: Float): Pair<Float, Float> {
        return if (isDarkModeTarget) {
            0f to maxRadius
        } else {
            maxRadius to 0f
        }
    }

    fun resetForTesting() {
        inTransitionForTest = false
        ThemeManager.cleanupPending("resetForTesting")
    }

    fun setInTransitionForTesting(value: Boolean) {
        inTransitionForTest = value
    }
}

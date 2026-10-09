package com.eve.app.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import kotlin.math.hypot
import kotlin.math.max

/** A single AppCompat recreation, without captured system bars, bitmap overlays,
 * hidden controls or touch locks. System bar appearance follows the visible
 * activity resources, rather than switching icons ahead of the visible content. */
object ThemeSwitchAnimator {
    const val PREFS = "eve_prefs"
    const val KEY_DARK_MODE = "key_dark_mode"
    private var inTransition = false
    private var registered = false
    private var sourceId = 0
    private var sourceClass = ""
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val release = Runnable { inTransition = false }
    val isTransitioning get() = inTransition

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false)
        else context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    fun ensureLifecycleRegistered(app: Application) {
        if (registered) return
        registered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (inTransition && activity.javaClass.name == sourceClass && System.identityHashCode(activity) != sourceId) {
                    handler.removeCallbacks(release)
                    inTransition = false
                }
                SystemBarHelper.syncSystemBars(activity)
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun calculateIconCenter(view: View): Pair<Int, Int> {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return (location[0] + view.width / 2) to (location[1] + view.height / 2)
    }

    @Suppress("UNUSED_PARAMETER")
    fun animate(activity: Activity, clickView: View, isDarkModeTarget: Boolean, staticIconView: View? = null) = switch(activity, isDarkModeTarget)

    @Suppress("UNUSED_PARAMETER")
    fun animateAt(activity: Activity, clickScreenX: Int, clickScreenY: Int, isDarkModeTarget: Boolean,
                  clickWidth: Int = 48, clickHeight: Int = 48, staticIconView: View? = null) = switch(activity, isDarkModeTarget)

    fun toggle(activity: Activity, view: View) = switch(activity, !isDarkMode(activity))
    fun toggleAt(activity: Activity, cx: Int, cy: Int) = switch(activity, !isDarkMode(activity))

    private fun switch(activity: Activity, target: Boolean) {
        if (inTransition || activity.isFinishing || activity.isDestroyed) return
        ensureLifecycleRegistered(activity.application)
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK_MODE, target).apply()
        val mode = if (target) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        inTransition = true
        sourceId = System.identityHashCode(activity)
        sourceClass = activity.javaClass.name
        handler.postDelayed(release, 1500L) // Guard release only; there is no touch-blocking view.
        activity.overridePendingTransition(0, 0)
        AppCompatDelegate.setDefaultNightMode(mode) // AppCompat owns exactly one recreation.
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

    fun resetForTesting() { inTransition = false }
    fun setInTransitionForTesting(value: Boolean) { inTransition = value }
}

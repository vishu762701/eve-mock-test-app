package com.eve.app.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import android.view.animation.PathInterpolator
import com.eve.app.R
import java.lang.ref.WeakReference
import kotlin.math.hypot
import kotlin.math.max

/**
 * ThemeSwitchAnimator: Robust, standalone, production animator implementing Telegram's exact
 * Day/Night theme-switch mechanism.
 *
 * Fully compatible with minSdk 24:
 * 1. SCREENSHOT CAPTURE:
 *    - Instantaneous, high-fidelity synchronous Canvas draw fallback combined with PixelCopy on API 26+.
 * 2. CIRCULAR REVEAL:
 *    - Uses ViewAnimationUtils.createCircularReveal(targetView, x, y, startRadius, endRadius).
 *    - Switching to dark: circle expands outward from button (0 -> maxRadius).
 *    - Switching to light: screenshot overlay shrinks inward into button (maxRadius -> 0).
 *    - Duration: 400ms, Interpolator: Telegram easeInOutQuad (PathInterpolator(0.455f, 0.03f, 0.515f, 0.955f)).
 * 3. PERSISTENCE & LIFECYCLE:
 *    - Persists selection synchronously to SharedPreferences (eve_prefs, key_dark_mode).
 *    - Pre-measures overlays in onActivityCreated for zero-flicker activity recreation synchronization.
 *    - Full cleanup of overlays, touch locks, animators, and bitmaps on completion.
 */
object ThemeSwitchAnimator {

    const val TAG = "ThemeSwitchAnimator"
    const val PREFS = "eve_prefs"
    const val KEY_DARK_MODE = "key_dark_mode"
    const val ANIMATION_DURATION = 400L

    data class TransitionState(
        val bitmap: Bitmap,
        val clickScreenX: Int,
        val clickScreenY: Int,
        val clickWidth: Int,
        val clickHeight: Int,
        val isDarkModeTarget: Boolean,
        val oldActivityId: Int,
        val activityClassName: String,
        val timestamp: Long,
        val iconIdsToHide: List<Int> = emptyList()
    )

    private var activeState: TransitionState? = null
    private var sourceActivityRef: WeakReference<Activity>? = null
    private var targetActivityRef: WeakReference<Activity>? = null
    private val hiddenViewsRef = mutableListOf<WeakReference<View>>()
    private var isLifecycleRegistered = false
    private var inTransition = false

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val safetyTimeoutRunnable = Runnable {
        Log.w(TAG, "[ThemeSwitchAnimator] Safety timeout reached -> forcing cleanup")
        cleanup("safety_timeout")
    }

    val isTransitioning: Boolean
        get() = inTransition

    fun getActiveTransitionState(): TransitionState? = activeState

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_DARK_MODE)) {
            return prefs.getBoolean(KEY_DARK_MODE, false)
        }
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun ensureLifecycleRegistered(app: Application) {
        if (isLifecycleRegistered) return
        isLifecycleRegistered = true

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val state = activeState ?: return
                if (activity.javaClass.name == state.activityClassName &&
                    System.identityHashCode(activity) != state.oldActivityId
                ) {
                    Log.i(TAG, "[ThemeSwitchAnimator] New activity created (${activity.javaClass.simpleName}) -> attaching pre-overlay")
                    targetActivityRef = WeakReference(activity)
                    activity.overridePendingTransition(0, 0)
                    attachInitialOverlayToNewActivity(activity, state)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                val state = activeState ?: return
                if (activity.javaClass.name == state.activityClassName &&
                    System.identityHashCode(activity) != state.oldActivityId
                ) {
                    Log.i(TAG, "[ThemeSwitchAnimator] New activity resumed -> waiting for pre-draw")
                    targetActivityRef = WeakReference(activity)
                    activity.overridePendingTransition(0, 0)
                    waitForNewThemeAndStartReveal(activity, state)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                val state = activeState ?: return
                if (System.identityHashCode(activity) == state.oldActivityId) {
                    Log.d(TAG, "[ThemeSwitchAnimator] Old activity stopped during recreate (expected)")
                    return
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                val state = activeState ?: return
                if (System.identityHashCode(activity) == state.oldActivityId) {
                    Log.d(TAG, "[ThemeSwitchAnimator] Old activity destroyed during recreate (expected)")
                    removeOverlays(activity)
                    return
                }
                if (activity.javaClass.name == state.activityClassName) {
                    Log.i(TAG, "[ThemeSwitchAnimator] Target activity destroyed -> cleaning up")
                    cleanup("activity_destroyed")
                }
            }
        })
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
        val pos = IntArray(2)
        clickView.getLocationOnScreen(pos)
        val w = if (clickView.measuredWidth > 0) clickView.measuredWidth else clickView.width
        val h = if (clickView.measuredHeight > 0) clickView.measuredHeight else clickView.height
        val clickScreenX = pos[0] + w / 2
        val clickScreenY = pos[1] + h / 2
        val clickWidth = max(w, 24)
        val clickHeight = max(h, 24)

        Log.i(TAG, "[ThemeSwitchAnimator] animate called for View '${clickView.javaClass.simpleName}' at icon center ($clickScreenX, $clickScreenY), targetDark=$isDarkModeTarget")
        val viewsToHide = mutableListOf<View>()
        viewsToHide.add(clickView)
        if (staticIconView != null && staticIconView !== clickView) {
            viewsToHide.add(staticIconView)
        }
        animateInternal(activity, clickScreenX, clickScreenY, clickWidth, clickHeight, isDarkModeTarget, viewsToHide)
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
        Log.i(TAG, "[ThemeSwitchAnimator] animateAt called at ($clickScreenX, $clickScreenY), size=${clickWidth}x${clickHeight}, targetDark=$isDarkModeTarget")
        val viewsToHide = if (staticIconView != null) listOf(staticIconView) else emptyList()
        animateInternal(activity, clickScreenX, clickScreenY, clickWidth, clickHeight, isDarkModeTarget, viewsToHide)
    }

    fun toggle(activity: Activity, clickView: View) {
        animate(activity, clickView, !isDarkMode(activity))
    }

    fun toggleAt(activity: Activity, cx: Int, cy: Int) {
        animateAt(activity, cx, cy, !isDarkMode(activity))
    }

    private fun animateInternal(
        activity: Activity,
        clickScreenX: Int,
        clickScreenY: Int,
        clickWidth: Int,
        clickHeight: Int,
        isDarkModeTarget: Boolean,
        viewsToHide: List<View> = emptyList()
    ) {
        if (inTransition) {
            Log.w(TAG, "[ThemeSwitchAnimator] Theme transition already in flight. Ignoring tap.")
            return
        }

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            Log.w(TAG, "[ThemeSwitchAnimator] DecorView not found -> applying theme directly")
            applyThemeDirectly(activity, isDarkModeTarget)
            return
        }

        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) {
            Log.w(TAG, "[ThemeSwitchAnimator] DecorView dimensions 0 -> applying theme directly")
            applyThemeDirectly(activity, isDarkModeTarget)
            return
        }

        inTransition = true
        sourceActivityRef = WeakReference(activity)
        ensureLifecycleRegistered(activity.application)

        // FIX 1: Hide static icons immediately before capture so screen capture has clean background without duplicate icon
        hiddenViewsRef.clear()
        viewsToHide.forEach {
            hiddenViewsRef.add(WeakReference(it))
            it.visibility = View.INVISIBLE
        }
        val iconIdsToHide = viewsToHide.mapNotNull { v ->
            if (v.id != View.NO_ID && v.id != 0) v.id else null
        }.distinct()

        // 1. CAPTURE SCREENSHOT (Synchronous Canvas draw with PixelCopy support)
        captureScreenBitmap(activity, decorView, width, height) { bitmap ->
            if (bitmap == null || bitmap.isRecycled) {
                Log.w(TAG, "[ThemeSwitchAnimator] Screen capture failed -> applying theme directly without animation")
                hiddenViewsRef.forEach { it.get()?.visibility = View.VISIBLE }
                hiddenViewsRef.clear()
                inTransition = false
                applyThemeDirectly(activity, isDarkModeTarget)
                return@captureScreenBitmap
            }

            Log.i(TAG, "[ThemeSwitchAnimator] Screen capture succeeded (${bitmap.width}x${bitmap.height})")

            activeState = TransitionState(
                bitmap = bitmap,
                clickScreenX = clickScreenX,
                clickScreenY = clickScreenY,
                clickWidth = clickWidth,
                clickHeight = clickHeight,
                isDarkModeTarget = isDarkModeTarget,
                oldActivityId = System.identityHashCode(activity),
                activityClassName = activity.javaClass.name,
                timestamp = SystemClock.uptimeMillis(),
                iconIdsToHide = iconIdsToHide
            )

            // Freeze the old activity screen immediately with static overlay
            freezeCurrentActivityScreen(activity, bitmap)

            mainHandler.postDelayed(safetyTimeoutRunnable, 3500)

            // 2. PERSIST THEME (Commit synchronously so SharedPreferences is immediately consistent)
            persistThemePreference(activity, isDarkModeTarget)

            // 3. APPLY NIGHT MODE & RECREATE
            val newNightMode = if (isDarkModeTarget) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newNightMode)

            activity.overridePendingTransition(0, 0)
            activity.recreate()
        }
    }

    fun persistThemePreference(context: Context, isDarkMode: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, isDarkMode)
            .commit()
        Log.i(TAG, "[ThemeSwitchAnimator] Theme persisted to SharedPreferences: key_dark_mode=$isDarkMode")
    }

    private fun applyThemeDirectly(
        activity: Activity,
        isDarkModeTarget: Boolean
    ) {
        persistThemePreference(activity, isDarkModeTarget)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeTarget) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        activity.overridePendingTransition(0, 0)
        activity.recreate()
    }

    private fun captureScreenBitmap(
        activity: Activity,
        decorView: ViewGroup,
        width: Int,
        height: Int,
        onCaptured: (Bitmap?) -> Unit
    ) {
        // Fast synchronous Canvas draw fallback
        val canvasFallback = {
            try {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                decorView.draw(canvas)
                bmp
            } catch (t: Throwable) {
                Log.e(TAG, "[ThemeSwitchAnimator] Canvas capture failed: ${t.message}")
                null
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                var handled = false

                val timeout = Runnable {
                    if (!handled) {
                        handled = true
                        Log.d(TAG, "[ThemeSwitchAnimator] PixelCopy timed out -> using Canvas fallback")
                        onCaptured(canvasFallback())
                    }
                }
                mainHandler.postDelayed(timeout, 100)

                PixelCopy.request(
                    activity.window,
                    Rect(0, 0, width, height),
                    bmp,
                    { result ->
                        mainHandler.removeCallbacks(timeout)
                        if (!handled) {
                            handled = true
                            if (result == PixelCopy.SUCCESS) {
                                onCaptured(bmp)
                            } else {
                                Log.d(TAG, "[ThemeSwitchAnimator] PixelCopy code $result -> using Canvas fallback")
                                onCaptured(canvasFallback())
                            }
                        }
                    },
                    mainHandler
                )
                return
            } catch (t: Throwable) {
                Log.w(TAG, "[ThemeSwitchAnimator] PixelCopy error: ${t.message} -> Canvas fallback")
            }
        }

        onCaptured(canvasFallback())
    }

    private fun freezeCurrentActivityScreen(activity: Activity, bitmap: Bitmap) {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        if (bitmap.isRecycled) return

        val overlay = ImageView(activity).apply {
            tag = "theme_switch_freeze_overlay"
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            fitsSystemWindows = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        decorView.addView(overlay)
    }

    private fun attachInitialOverlayToNewActivity(activity: Activity, state: TransitionState) {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        if (state.bitmap.isRecycled) return

        removeOverlays(activity)
        SystemBarHelper.syncSystemBars(activity)

        // FIX 1: Ensure static icons at entry points remain INVISIBLE in the new activity during the reveal
        state.iconIdsToHide.forEach { id ->
            activity.findViewById<View>(id)?.visibility = View.INVISIBLE
        }

        val overlay = ImageView(activity).apply {
            tag = "theme_switch_animating_overlay"
            setImageBitmap(state.bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            fitsSystemWindows = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val contentRoot = if (decorView.childCount > 0) decorView.getChildAt(0) else null

        if (state.isDarkModeTarget && contentRoot != null) {
            // Switching to dark: insert overlay at index 0 (behind contentRoot)
            decorView.addView(overlay, 0)
        } else {
            // Switching to light: add overlay on top
            decorView.addView(overlay)
        }
    }

    private fun waitForNewThemeAndStartReveal(activity: Activity, state: TransitionState) {
        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanup("no_decor_on_new_activity")
            return
        }

        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                }

                if (activeState == null || state.bitmap.isRecycled || activity.isFinishing || activity.isDestroyed) {
                    cleanup("predraw_cancelled_or_recycled")
                    return true
                }

                executeCircularReveal(activity, decorView, state)
                return true
            }
        })
        decorView.invalidate()
    }

    private fun executeCircularReveal(
        activity: Activity,
        decorView: ViewGroup,
        state: TransitionState
    ) {
        // Prevent touch input during animation
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        val overlay = decorView.findViewWithTag<ImageView>("theme_switch_animating_overlay")
        val contentRoot = decorView.findViewById<View>(android.R.id.content) ?: decorView.getChildAt(0)

        val targetView = if (state.isDarkModeTarget && contentRoot != null) contentRoot else overlay

        if (targetView == null || !targetView.isAttachedToWindow) {
            Log.w(TAG, "[ThemeSwitchAnimator] targetView is null or unattached -> direct cleanup")
            cleanup("target_view_null_or_unattached")
            return
        }

        // Android coordinate system fix: circular reveal center must be relative to targetView
        val targetLoc = IntArray(2)
        targetView.getLocationOnScreen(targetLoc)
        val revealCx = state.clickScreenX - targetLoc[0]
        val revealCy = state.clickScreenY - targetLoc[1]

        val targetW = targetView.width.toFloat()
        val targetH = targetView.height.toFloat()
        val maxRadius = calculateMaxRadius(revealCx.toFloat(), revealCy.toFloat(), targetW, targetH)
        val (startRadius, endRadius) = getRevealRadii(state.isDarkModeTarget, maxRadius)

        Log.i(TAG, "[ThemeSwitchAnimator] Starting reveal: target=${targetView.javaClass.simpleName}, revealCenter=($revealCx, $revealCy), r=$startRadius->$endRadius, maxRadius=$maxRadius")

        ViewAnimationUtils.createCircularReveal(
            targetView,
            revealCx,
            revealCy,
            startRadius,
            endRadius
        ).apply {
            duration = ANIMATION_DURATION
            interpolator = PathInterpolator(0.455f, 0.03f, 0.515f, 0.955f)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    Log.i(TAG, "[ThemeSwitchAnimator] Animation finished cleanly")
                    cleanup("animation_complete")
                }
            })
            start()
        }
    }

    private fun removeOverlays(activity: Activity?) {
        if (activity == null) return
        val decorView = activity.window?.decorView as? ViewGroup ?: return

        val tags = listOf(
            "theme_switch_freeze_overlay",
            "theme_switch_animating_overlay"
        )

        for (tag in tags) {
            var view: View?
            do {
                view = decorView.findViewWithTag<View>(tag)
                if (view != null) {
                    if (view is ImageView) view.setImageDrawable(null)
                    decorView.removeView(view)
                }
            } while (view != null)
        }
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
        inTransition = false
        activeState = null
        sourceActivityRef = null
        targetActivityRef = null
        hiddenViewsRef.clear()
    }

    fun setInTransitionForTesting(transitioning: Boolean) {
        inTransition = transitioning
    }

    fun cleanup(reason: String) {
        Log.i(TAG, "[ThemeSwitchAnimator] cleanup triggered (reason: $reason)")
        mainHandler.removeCallbacks(safetyTimeoutRunnable)

        val src = sourceActivityRef?.get()
        val tgt = targetActivityRef?.get()

        removeOverlays(src)
        removeOverlays(tgt)

        // FIX 1: Restore visibility of static icons on completion
        val state = activeState
        state?.iconIdsToHide?.forEach { id ->
            tgt?.findViewById<View>(id)?.visibility = View.VISIBLE
            src?.findViewById<View>(id)?.visibility = View.VISIBLE
        }
        hiddenViewsRef.forEach { it.get()?.visibility = View.VISIBLE }
        hiddenViewsRef.clear()

        src?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        tgt?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        sourceActivityRef = null
        targetActivityRef = null

        val finalActivity = tgt ?: src

        val bmp = activeState?.bitmap
        activeState = null
        inTransition = false

        if (finalActivity != null) {
            SystemBarHelper.syncSystemBars(finalActivity)
        }

        if (bmp != null && !bmp.isRecycled) {
            try {
                bmp.recycle()
            } catch (t: Throwable) {
                Log.w(TAG, "[ThemeSwitchAnimator] Bitmap recycle note: ${t.message}")
            }
        }
    }
}

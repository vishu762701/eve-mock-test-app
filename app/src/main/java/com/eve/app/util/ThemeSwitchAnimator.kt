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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.airbnb.lottie.LottieAnimationView
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
 *    - Duration: 400ms, Interpolator: smooth cubic ease-in-out (FastOutSlowInInterpolator).
 * 3. LOTTIE SUN/MOON MORPH:
 *    - Floating LottieAnimationView (sun_to_moon.json) positioned directly at tap location.
 *    - Plays forward (0 -> 1) when switching to dark, reverse (1 -> 0) when switching to light.
 * 4. PERSISTENCE & LIFECYCLE:
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
        val timestamp: Long
    )

    private var activeState: TransitionState? = null
    private var sourceActivityRef: WeakReference<Activity>? = null
    private var targetActivityRef: WeakReference<Activity>? = null
    private var isLifecycleRegistered = false
    private var inTransition = false

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val safetyTimeoutRunnable = Runnable {
        Log.w(TAG, "[ThemeSwitchAnimator] Safety timeout reached -> forcing cleanup")
        cleanup("safety_timeout")
    }

    val isTransitioning: Boolean
        get() = inTransition

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
                    Log.i(TAG, "[ThemeSwitchAnimator] New activity created (${activity.javaClass.simpleName}) -> attaching pre-overlay and Lottie icon")
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
        view.getLocationInWindow(pos)
        val w = if (view.measuredWidth > 0) view.measuredWidth else view.width
        val h = if (view.measuredHeight > 0) view.measuredHeight else view.height
        return (pos[0] + w / 2) to (pos[1] + h / 2)
    }

    fun animate(
        activity: Activity,
        clickView: View,
        isDarkModeTarget: Boolean
    ) {
        val pos = IntArray(2)
        clickView.getLocationInWindow(pos)
        val w = if (clickView.measuredWidth > 0) clickView.measuredWidth else clickView.width
        val h = if (clickView.measuredHeight > 0) clickView.measuredHeight else clickView.height
        val clickScreenX = pos[0] + w / 2
        val clickScreenY = pos[1] + h / 2
        val clickWidth = max(w, 24)
        val clickHeight = max(h, 24)

        Log.i(TAG, "[ThemeSwitchAnimator] animate called for View '${clickView.javaClass.simpleName}' at icon center ($clickScreenX, $clickScreenY), targetDark=$isDarkModeTarget")
        animateInternal(activity, clickScreenX, clickScreenY, clickWidth, clickHeight, isDarkModeTarget)
    }

    fun animateAt(
        activity: Activity,
        clickScreenX: Int,
        clickScreenY: Int,
        isDarkModeTarget: Boolean,
        clickWidth: Int = 48,
        clickHeight: Int = 48
    ) {
        Log.i(TAG, "[ThemeSwitchAnimator] animateAt called at ($clickScreenX, $clickScreenY), size=${clickWidth}x${clickHeight}, targetDark=$isDarkModeTarget")
        animateInternal(activity, clickScreenX, clickScreenY, clickWidth, clickHeight, isDarkModeTarget)
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
        isDarkModeTarget: Boolean
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

        // 1. CAPTURE SCREENSHOT (Synchronous Canvas draw with PixelCopy support)
        captureScreenBitmap(activity, decorView, width, height) { bitmap ->
            if (bitmap == null || bitmap.isRecycled) {
                Log.w(TAG, "[ThemeSwitchAnimator] Screen capture failed -> applying theme directly without animation")
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
                timestamp = SystemClock.uptimeMillis()
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

        // Add Lottie Sun/Moon Morph icon on top at tap location
        val decorLoc = IntArray(2)
        decorView.getLocationInWindow(decorLoc)
        val cx = state.clickScreenX - decorLoc[0]
        val cy = state.clickScreenY - decorLoc[1]

        val lottieView = LottieAnimationView(activity).apply {
            tag = "theme_switch_lottie_icon"
            setAnimation(R.raw.sun_to_moon)
            // Initial frame: 0.0 (Sun) if going dark, 1.0 (Moon) if going light
            progress = if (state.isDarkModeTarget) 0f else 1f
            elevation = 2000f
        }
        val btnLeft = cx - (state.clickWidth / 2)
        val btnTop = cy - (state.clickHeight / 2)
        val lottieLp = FrameLayout.LayoutParams(state.clickWidth, state.clickHeight).apply {
            leftMargin = btnLeft
            topMargin = btnTop
        }
        decorView.addView(lottieView, lottieLp)
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

                executeCircularRevealAndLottieSync(activity, decorView, state)
                return true
            }
        })
        decorView.invalidate()
    }

    private fun executeCircularRevealAndLottieSync(
        activity: Activity,
        decorView: ViewGroup,
        state: TransitionState
    ) {
        // Prevent touch input during animation
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        val decorLoc = IntArray(2)
        decorView.getLocationInWindow(decorLoc)
        val cx = state.clickScreenX - decorLoc[0]
        val cy = state.clickScreenY - decorLoc[1]

        val w = decorView.width.toFloat()
        val h = decorView.height.toFloat()

        val maxRadius = calculateMaxRadius(cx.toFloat(), cy.toFloat(), w, h)

        val overlay = decorView.findViewWithTag<ImageView>("theme_switch_animating_overlay")
        val lottieView = decorView.findViewWithTag<LottieAnimationView>("theme_switch_lottie_icon")
        val contentRoot = decorView.findViewById<View>(android.R.id.content) ?: decorView.getChildAt(0)

        // Ensure floating Lottie icon is placed with exact precision at (cx, cy)
        val btnLeft = cx - (state.clickWidth / 2)
        val btnTop = cy - (state.clickHeight / 2)
        lottieView?.let { lv ->
            (lv.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                if (leftMargin != btnLeft || topMargin != btnTop) {
                    leftMargin = btnLeft
                    topMargin = btnTop
                    lv.layoutParams = this
                }
            }
        }

        val targetView = if (state.isDarkModeTarget && contentRoot != null) contentRoot else overlay
        val (startRadius, endRadius) = getRevealRadii(state.isDarkModeTarget, maxRadius)

        Log.i(TAG, "[ThemeSwitchAnimator] Starting reveal: target=${targetView?.javaClass?.simpleName}, cx=$cx, cy=$cy, r=$startRadius->$endRadius, maxRadius=$maxRadius")

        if (targetView == null || !targetView.isAttachedToWindow) {
            Log.w(TAG, "[ThemeSwitchAnimator] targetView is null or unattached -> direct cleanup")
            cleanup("target_view_null_or_unattached")
            return
        }

        val revealAnimator = ViewAnimationUtils.createCircularReveal(
            targetView,
            cx,
            cy,
            startRadius,
            endRadius
        ).apply {
            duration = ANIMATION_DURATION
            interpolator = FastOutSlowInInterpolator()
        }

        val (lottieStart, lottieEnd) = getLottieProgressRange(state.isDarkModeTarget)
        val lottieAnimator = ValueAnimator.ofFloat(lottieStart, lottieEnd).apply {
            duration = ANIMATION_DURATION
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { va ->
                lottieView?.progress = va.animatedValue as Float
            }
        }

        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(revealAnimator, lottieAnimator)
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
            "theme_switch_animating_overlay",
            "theme_switch_lottie_icon"
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

    fun getLottieProgressRange(isDarkModeTarget: Boolean): Pair<Float, Float> {
        return if (isDarkModeTarget) {
            0f to 1f
        } else {
            1f to 0f
        }
    }

    fun resetForTesting() {
        inTransition = false
        activeState = null
        sourceActivityRef = null
        targetActivityRef = null
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

        src?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        tgt?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

        sourceActivityRef = null
        targetActivityRef = null

        val bmp = activeState?.bitmap
        activeState = null
        inTransition = false

        if (bmp != null && !bmp.isRecycled) {
            try {
                bmp.recycle()
            } catch (t: Throwable) {
                Log.w(TAG, "[ThemeSwitchAnimator] Bitmap recycle note: ${t.message}")
            }
        }
    }
}

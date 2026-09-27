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
 * ThemeSwitchAnimator: Standalone, reusable animator implementing Telegram's exact
 * Day/Night theme-switch mechanism.
 *
 * Fully compatible with minSdk 24:
 * 1. SCREENSHOT CAPTURE:
 *    - PixelCopy on API 26+ (hardware accelerated surface capture).
 *    - Synchronous Canvas draw fallback for API 24-25 and PixelCopy timeouts.
 * 2. CIRCULAR REVEAL:
 *    - Uses ViewAnimationUtils.createCircularReveal(targetView, x, y, startRadius, endRadius).
 *    - Switching to dark: circle expands outward from button (0 -> maxRadius).
 *    - Switching to light: screenshot overlay shrinks inward into button (maxRadius -> 0).
 *    - Duration: 400ms, Interpolator: smooth cubic ease-in-out (FastOutSlowInInterpolator).
 * 3. LOTTIE SUN/MOON MORPH:
 *    - Floating LottieAnimationView (sun_to_moon.json) positioned directly at tap location.
 *    - Plays forward (0 -> 1) when switching to dark, reverse (1 -> 0) when switching to light.
 * 4. PERSISTENCE & INTEGRATION:
 *    - Persists selection to SharedPreferences (eve_prefs, key_dark_mode).
 *    - Zero-flicker activity recreation lifecycle synchronization.
 *    - Complete cleanup of overlays, animators, and bitmaps on completion.
 */
object ThemeSwitchAnimator {

    private const val TAG = "ThemeSwitchAnimator"
    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"
    private const val ANIMATION_DURATION = 400L

    private data class TransitionState(
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

    private val mainHandler = Handler(Looper.getMainLooper())
    private val safetyTimeoutRunnable = Runnable {
        Log.d(TAG, "Safety timeout reached -> cleaning up transition state")
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
                    targetActivityRef = WeakReference(activity)
                    activity.overridePendingTransition(0, 0)
                    waitForNewThemeAndStartReveal(activity, state)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                val state = activeState ?: return
                if (activity.javaClass.name == state.activityClassName &&
                    System.identityHashCode(activity) != state.oldActivityId
                ) {
                    cleanup("activity_stopped")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                val state = activeState ?: return
                if (activity.javaClass.name == state.activityClassName &&
                    System.identityHashCode(activity) != state.oldActivityId
                ) {
                    cleanup("activity_destroyed")
                }
            }
        })
    }

    /**
     * Primary entry point: Triggers Telegram's exact Day/Night theme animation from a tapped View.
     *
     * @param activity The host activity triggering the theme change.
     * @param clickView The button or view that was tapped (used for tap location and size).
     * @param isDarkModeTarget True to transition to Dark Mode, False for Light Mode.
     * @param onThemeApplied Optional custom callback to apply theme changes.
     */
    fun animate(
        activity: Activity,
        clickView: View,
        isDarkModeTarget: Boolean,
        onThemeApplied: (() -> Unit)? = null
    ) {
        val loc = IntArray(2)
        clickView.getLocationOnScreen(loc)
        val clickScreenX = loc[0] + clickView.width / 2
        val clickScreenY = loc[1] + clickView.height / 2
        val clickWidth = max(clickView.width, 24)
        val clickHeight = max(clickView.height, 24)

        animateInternal(activity, clickScreenX, clickScreenY, clickWidth, clickHeight, isDarkModeTarget, onThemeApplied)
    }

    /**
     * Triggers the theme animation from explicit screen coordinates (e.g. from popup or menu touch).
     */
    fun animateAt(
        activity: Activity,
        clickScreenX: Int,
        clickScreenY: Int,
        isDarkModeTarget: Boolean,
        clickWidth: Int = 48,
        clickHeight: Int = 48,
        onThemeApplied: (() -> Unit)? = null
    ) {
        animateInternal(activity, clickScreenX, clickScreenY, clickWidth, clickHeight, isDarkModeTarget, onThemeApplied)
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
        onThemeApplied: (() -> Unit)?
    ) {
        if (inTransition) {
            Log.d(TAG, "Theme transition already in flight. Ignoring tap.")
            return
        }

        val decorView = activity.window.decorView as? ViewGroup ?: return
        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) {
            applyThemeDirectly(activity, isDarkModeTarget, onThemeApplied)
            return
        }

        inTransition = true
        sourceActivityRef = WeakReference(activity)
        ensureLifecycleRegistered(activity.application)

        // SCREENSHOT CAPTURE (PixelCopy on API 26+ with Canvas fallback)
        captureScreenBitmap(activity, decorView, width, height) { bitmap ->
            if (bitmap == null || bitmap.isRecycled) {
                Log.w(TAG, "Screen capture failed -> applying theme directly without animation")
                inTransition = false
                applyThemeDirectly(activity, isDarkModeTarget, onThemeApplied)
                return@captureScreenBitmap
            }

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

            // Freeze the old activity screen immediately with a static overlay
            freezeCurrentActivityScreen(activity, bitmap)

            mainHandler.postDelayed(safetyTimeoutRunnable, 3500)

            // PERSIST THEME & APPLY
            persistThemePreference(activity, isDarkModeTarget)
            activity.overridePendingTransition(0, 0)

            if (onThemeApplied != null) {
                onThemeApplied.invoke()
            } else {
                applyAppCompatNightMode(isDarkModeTarget)
            }
        }
    }

    private fun persistThemePreference(context: Context, isDarkMode: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, isDarkMode)
            .apply()
    }

    private fun applyAppCompatNightMode(isDarkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun applyThemeDirectly(
        activity: Activity,
        isDarkModeTarget: Boolean,
        onThemeApplied: (() -> Unit)?
    ) {
        persistThemePreference(activity, isDarkModeTarget)
        if (onThemeApplied != null) {
            onThemeApplied.invoke()
        } else {
            applyAppCompatNightMode(isDarkModeTarget)
        }
    }

    /**
     * Captures a screenshot of the window.
     * Uses PixelCopy on API 26+, falling back to Canvas draw for older versions or timeouts.
     */
    private fun captureScreenBitmap(
        activity: Activity,
        decorView: ViewGroup,
        width: Int,
        height: Int,
        onCaptured: (Bitmap?) -> Unit
    ) {
        val canvasFallback = {
            try {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                decorView.draw(canvas)
                bmp
            } catch (t: Throwable) {
                Log.e(TAG, "Canvas capture failed: ${t.message}")
                null
            }
        }

        // On API 26+, use PixelCopy for hardware-accelerated surface fidelity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                var handled = false

                val timeout = Runnable {
                    if (!handled) {
                        handled = true
                        Log.d(TAG, "PixelCopy timed out. Using Canvas fallback.")
                        onCaptured(canvasFallback())
                    }
                }
                mainHandler.postDelayed(timeout, 120)

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
                                Log.d(TAG, "PixelCopy failed with code $result. Using Canvas fallback.")
                                onCaptured(canvasFallback())
                            }
                        }
                    },
                    mainHandler
                )
                return
            } catch (t: Throwable) {
                Log.w(TAG, "PixelCopy error: ${t.message}. Falling back to Canvas.")
            }
        }

        // API 24-25 fallback
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
            tag = "theme_switch_freeze_overlay"
            setImageBitmap(state.bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            fitsSystemWindows = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        decorView.addView(overlay)
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

    /**
     * Executes the circular reveal and Lottie sun/moon morph animation in exact synchronization.
     */
    private fun executeCircularRevealAndLottieSync(
        activity: Activity,
        decorView: ViewGroup,
        state: TransitionState
    ) {
        // 1. Prevent touch input during animation
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        // 2. Map coordinates relative to decorView
        val decorLoc = IntArray(2)
        decorView.getLocationOnScreen(decorLoc)
        val cx = state.clickScreenX - decorLoc[0]
        val cy = state.clickScreenY - decorLoc[1]

        val w = decorView.width.toFloat()
        val h = decorView.height.toFloat()

        // Calculate end radius: max distance from (cx, cy) to the 4 screen corners
        val d1 = hypot(cx.toDouble(), cy.toDouble())
        val d2 = hypot((w - cx).toDouble(), cy.toDouble())
        val d3 = hypot(cx.toDouble(), (h - cy).toDouble())
        val d4 = hypot((w - cx).toDouble(), (h - cy).toDouble())
        val maxRadius = max(max(d1, d2), max(d3, d4)).toFloat()

        // Clean up previous freeze overlay
        removeOverlays(activity)

        // 3. Create the Screenshot Overlay View
        val screenshotOverlay = ImageView(activity).apply {
            tag = "theme_switch_animating_overlay"
            setImageBitmap(state.bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            fitsSystemWindows = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 4. Determine target view and radius bounds:
        // Switching to dark: circle expands outward from the button (0 -> maxRadius)
        // Switching to light: screenshot overlay shrinks inward into the button (maxRadius -> 0)
        val contentRoot = if (decorView.childCount > 0) decorView.getChildAt(0) else null

        val (targetView, startRadius, endRadius) = if (state.isDarkModeTarget && contentRoot != null) {
            decorView.addView(screenshotOverlay, 0)
            Triple(contentRoot, 0f, maxRadius)
        } else {
            decorView.addView(screenshotOverlay)
            Triple(screenshotOverlay, maxRadius, 0f)
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

        // 5. Create and Position Floating Lottie Sun/Moon Morph Icon
        val lottieView = LottieAnimationView(activity).apply {
            tag = "theme_switch_lottie_icon"
            setAnimation(R.raw.sun_to_moon)
            progress = if (state.isDarkModeTarget) 0f else 1f
        }

        val btnLeft = cx - (state.clickWidth / 2)
        val btnTop = cy - (state.clickHeight / 2)
        val lottieLp = FrameLayout.LayoutParams(state.clickWidth, state.clickHeight).apply {
            leftMargin = btnLeft
            topMargin = btnTop
        }
        lottieView.layoutParams = lottieLp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            lottieView.elevation = 2000f
        }
        decorView.addView(lottieView)

        // 6. Build Lottie Morph Animator (in exact sync)
        val lottieAnimator = ValueAnimator.ofFloat(
            if (state.isDarkModeTarget) 0f else 1f,
            if (state.isDarkModeTarget) 1f else 0f
        ).apply {
            duration = ANIMATION_DURATION
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { va ->
                lottieView.progress = va.animatedValue as Float
            }
        }

        // 7. Play circular reveal and Lottie morph together
        val animatorSet = android.animation.AnimatorSet().apply {
            playTogether(revealAnimator, lottieAnimator)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    screenshotOverlay.visibility = View.GONE
                    lottieView.visibility = View.GONE
                    removeOverlays(activity)
                    cleanup("animation_complete")
                }
            })
        }

        decorView.post {
            if (activity.isFinishing || activity.isDestroyed) {
                cleanup("activity_destroyed_before_start")
                return@post
            }
            animatorSet.start()
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

    private fun cleanup(reason: String) {
        Log.d(TAG, "cleanup triggered (reason: $reason)")
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
                Log.w(TAG, "Bitmap recycle note: ${t.message}")
            }
        }
    }
}

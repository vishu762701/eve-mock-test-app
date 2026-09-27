package com.eve.app.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.eve.app.BuildConfig
import com.eve.app.R
import java.lang.ref.WeakReference
import kotlin.math.hypot
import kotlin.math.max

/**
 * ThemeManager: Controls Light <-> Dark theme switching with Telegram-style circular reveal.
 * Solves all known flicker causes:
 * 1. Synchronous/PixelCopy bitmap capture complete BEFORE theme change begins.
 * 2. Static full-screen overlay attached to DecorView during recreation (covers status & nav bar).
 * 3. Frame synchronization via OnPreDrawListener waits until new theme renders underneath.
 * 4. Expanding circular reveal (0 -> maxRadius) centered at exact tap/button coordinates.
 * 5. Full lifecycle cleanup and rapid-tap protection.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"
    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private data class SnapshotHolder(
        val bitmap: Bitmap,
        val originX: Float,
        val originY: Float,
        val oldActivityId: Int,
        val activityClassName: String,
        val captureTimestamp: Long
    )

    private var pendingSnapshot: SnapshotHolder? = null
    private var sourceActivityRef: WeakReference<Activity>? = null
    private var targetActivityRef: WeakReference<Activity>? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var themeSwitchOverlay: ThemeSwitchOverlayView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        logDebug("Timeout reached, cleaning up pending transition")
        cleanupPending("timeout")
    }

    val isTransitioning: Boolean
        get() = transitioning

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[${SystemClock.uptimeMillis()}] $message")
        }
    }

    fun applySavedMode(context: Context) {
        val app = (context as? Application) ?: (context.applicationContext as? Application)
        app?.let { ensureLifecycleRegistered(it) }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val mode = if (prefs.contains(KEY_DARK_MODE)) {
            if (prefs.getBoolean(KEY_DARK_MODE, false)) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun isDarkMode(context: Context): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun toggle(context: Context) {
        val goingDark = !isDarkMode(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, goingDark)
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (goingDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun areAnimationsEnabled(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale > 0f
        } catch (_: Throwable) {
            true
        }
    }

    private fun findActivity(context: Context): AppCompatActivity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is AppCompatActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    fun ensureLifecycleRegistered(app: Application) {
        if (isLifecycleRegistered) return
        isLifecycleRegistered = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val holder = pendingSnapshot ?: return
                if (activity.javaClass.name == holder.activityClassName &&
                    System.identityHashCode(activity) != holder.oldActivityId
                ) {
                    targetActivityRef = WeakReference(activity)
                    logDebug("New Activity created under theme change -> Attaching full-screen pre-overlay")
                    activity.overridePendingTransition(0, 0)
                    attachStaticOverlayImmediately(activity, holder.bitmap)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                val holder = pendingSnapshot ?: return
                if (activity.javaClass.name == holder.activityClassName &&
                    System.identityHashCode(activity) != holder.oldActivityId
                ) {
                    targetActivityRef = WeakReference(activity)
                    logDebug("New Activity resumed -> Waiting for first draw before triggering reveal")
                    activity.overridePendingTransition(0, 0)
                    waitForNewThemeRenderAndReveal(activity, holder)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                val holder = pendingSnapshot ?: return
                if (System.identityHashCode(activity) == holder.oldActivityId) {
                    logDebug("Old Activity stopped during recreate (expected)")
                    return
                }
                if (activity.javaClass.name == holder.activityClassName) {
                    logDebug("Activity stopped during transition (${activity.javaClass.simpleName}) -> cleaning up safely")
                    detachAllOverlays(activity)
                    cleanupPending("activity_stopped")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                val holder = pendingSnapshot ?: return
                if (System.identityHashCode(activity) == holder.oldActivityId) {
                    logDebug("Old Activity destroyed during recreate (expected)")
                    detachAllOverlays(activity)
                    return
                }
                if (activity.javaClass.name == holder.activityClassName) {
                    logDebug("Activity destroyed -> cleaning up overlay")
                    detachAllOverlays(activity)
                    cleanupPending("activity_destroyed")
                }
            }
        })
    }

    /**
     * Safely detaches and clears all overlay views associated with the theme transition
     * from the specified Activity's decorView.
     */
    private fun detachAllOverlays(activity: Activity?) {
        if (activity == null) return
        try {
            val decorView = activity.window?.decorView as? ViewGroup ?: return

            // 1. Find and remove ALL views tagged "pre_reveal_overlay"
            var preOverlay: View?
            do {
                preOverlay = decorView.findViewWithTag<View>("pre_reveal_overlay")
                if (preOverlay != null) {
                    (preOverlay as? ImageView)?.setImageDrawable(null)
                    decorView.removeView(preOverlay)
                    logDebug("Removed pre_reveal_overlay from ${activity.javaClass.simpleName}")
                }
            } while (preOverlay != null)

            // 2. Remove and cancel themeSwitchOverlay if attached
            themeSwitchOverlay?.let { overlay ->
                if (overlay.parent == decorView || overlay.parent != null) {
                    overlay.cancelAnimation()
                    (overlay.parent as? ViewGroup)?.removeView(overlay)
                    logDebug("Removed themeSwitchOverlay from ${activity.javaClass.simpleName}")
                }
            }

            // 3. Clear window touch-blocking flag to prevent UI lockup
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        } catch (t: Throwable) {
            logDebug("Error during detachAllOverlays: ${t.message}")
        }
    }

    private fun attachStaticOverlayImmediately(activity: Activity, bitmap: Bitmap) {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        if (bitmap.isRecycled) return
        // First ensure any previous overlay is detached
        detachAllOverlays(activity)
        val overlay = ImageView(activity).apply {
            tag = "pre_reveal_overlay"
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

    private fun waitForNewThemeRenderAndReveal(activity: Activity, holder: SnapshotHolder) {
        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanupPending("waitForNewThemeRenderAndReveal_no_decor")
            return
        }

        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                }
                if (pendingSnapshot == null || holder.bitmap.isRecycled || activity.isFinishing || activity.isDestroyed) {
                    logDebug("Aborting reveal: transition was cancelled, bitmap recycled, or activity finishing")
                    cleanupPending("preDraw_cancelled_or_recycled")
                    return true
                }
                logDebug("New theme preDraw confirmed -> Launching circular reveal animation")
                triggerCircularReveal(activity, holder)
                return true
            }
        })
        decorView.invalidate()
    }

    private fun cleanupPending(reason: String = "unknown") {
        logDebug("cleanupPending triggered (reason: $reason)")
        mainHandler.removeCallbacks(timeoutRunnable)

        // Step 1: Detach and clear all overlays from both source and target activities before bitmap recycle
        val source = sourceActivityRef?.get()
        val target = targetActivityRef?.get()
        detachAllOverlays(source)
        detachAllOverlays(target)
        sourceActivityRef = null
        targetActivityRef = null

        // Step 2: Ensure themeSwitchOverlay animation is cancelled and references cleared
        themeSwitchOverlay?.let { overlay ->
            overlay.cancelAnimation()
            (overlay.parent as? ViewGroup)?.removeView(overlay)
            themeSwitchOverlay = null
        }

        // Step 3: Now that all views referencing the bitmap have been removed from the
        // view hierarchy and their ImageDrawables cleared, it is safe to recycle the bitmap.
        val bmp = pendingSnapshot?.bitmap
        pendingSnapshot = null
        transitioning = false

        if (bmp != null && !bmp.isRecycled) {
            try {
                bmp.recycle()
                logDebug("Bitmap successfully and safely recycled")
            } catch (t: Throwable) {
                logDebug("Error while recycling bitmap: ${t.message}")
            }
        }
    }

    /**
     * Triggers theme toggle with a Telegram-style circular reveal originating
     * from the exact interaction coordinates (originX, originY).
     * Standard Android SDK only, 400ms duration, AccelerateDecelerateInterpolator.
     */
    fun toggleWithCircularReveal(activity: Activity, originX: Int, originY: Int, applyAction: Runnable? = null) {
        if (transitioning) {
            logDebug("Rapid tap blocked: transition already in progress")
            return
        }

        if (!areAnimationsEnabled(activity)) {
            logDebug("Animations disabled in accessibility -> instant toggle")
            if (applyAction != null) {
                applyAction.run()
            } else {
                toggle(activity)
            }
            return
        }

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            if (applyAction != null) {
                applyAction.run()
            } else {
                toggle(activity)
            }
            return
        }

        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) {
            if (applyAction != null) {
                applyAction.run()
            } else {
                toggle(activity)
            }
            return
        }

        logDebug("Theme toggle initiated at ($originX, $originY). Capturing bitmap...")
        ensureLifecycleRegistered(activity.application)

        // Capture static snapshot: PixelCopy on API 26+ with synchronous Canvas fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val window = activity.window
                var pixelCopyDone = false
                val pixelCopyTimeout = Runnable {
                    if (!pixelCopyDone) {
                        pixelCopyDone = true
                        logDebug("PixelCopy timed out -> fallback to Canvas draw")
                        fallbackSynchronousCaptureAndToggle(activity, decorView, originX.toFloat(), originY.toFloat(), applyAction)
                    }
                }
                mainHandler.postDelayed(pixelCopyTimeout, 120)

                PixelCopy.request(
                    window,
                    Rect(0, 0, width, height),
                    bitmap,
                    { copyResult ->
                        mainHandler.removeCallbacks(pixelCopyTimeout)
                        if (!pixelCopyDone) {
                            pixelCopyDone = true
                            if (copyResult == PixelCopy.SUCCESS) {
                                logDebug("PixelCopy capture SUCCESS")
                                commitRevealTransition(activity, bitmap, originX.toFloat(), originY.toFloat(), applyAction)
                            } else {
                                logDebug("PixelCopy failed with code $copyResult -> fallback to Canvas draw")
                                fallbackSynchronousCaptureAndToggle(activity, decorView, originX.toFloat(), originY.toFloat(), applyAction)
                            }
                        }
                    },
                    mainHandler
                )
                return
            } catch (t: Throwable) {
                logDebug("PixelCopy exception: ${t.message} -> fallback to Canvas draw")
            }
        }

        fallbackSynchronousCaptureAndToggle(activity, decorView, originX.toFloat(), originY.toFloat(), applyAction)
    }

    private fun fallbackSynchronousCaptureAndToggle(
        activity: Activity,
        decorView: ViewGroup,
        originX: Float,
        originY: Float,
        applyAction: Runnable? = null
    ) {
        val bitmap = try {
            val bmp = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            decorView.draw(canvas)
            logDebug("Synchronous Canvas draw capture SUCCESS")
            bmp
        } catch (t: Throwable) {
            logDebug("Canvas capture failed: ${t.message}")
            null
        }

        if (bitmap == null) {
            if (applyAction != null) {
                applyAction.run()
            } else {
                toggle(activity)
            }
            return
        }

        commitRevealTransition(activity, bitmap, originX, originY, applyAction)
    }

    private fun commitRevealTransition(
        activity: Activity,
        bitmap: Bitmap,
        originX: Float,
        originY: Float,
        applyAction: Runnable? = null
    ) {
        transitioning = true
        sourceActivityRef = WeakReference(activity)
        pendingSnapshot = SnapshotHolder(
            bitmap = bitmap,
            originX = originX,
            originY = originY,
            oldActivityId = System.identityHashCode(activity),
            activityClassName = activity.javaClass.name,
            captureTimestamp = SystemClock.uptimeMillis()
        )

        // Add pre-overlay on old screen immediately to prevent any 1-frame gap
        val decorView = activity.window.decorView as? ViewGroup
        if (decorView != null && !bitmap.isRecycled) {
            val preOverlay = ImageView(activity).apply {
                tag = "pre_reveal_overlay"
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_XY
                fitsSystemWindows = false
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            decorView.addView(preOverlay)
        }

        activity.overridePendingTransition(0, 0)
        mainHandler.postDelayed(timeoutRunnable, 3500)

        logDebug("Applying new theme mode via AppCompatDelegate...")
        if (applyAction != null) {
            applyAction.run()
        } else {
            toggle(activity)
        }
    }

    fun toggleWithCircularReveal(anchorView: View) {
        if (transitioning) return
        val loc = IntArray(2)
        anchorView.getLocationOnScreen(loc)
        val cx = loc[0] + anchorView.width / 2
        val cy = loc[1] + anchorView.height / 2
        val activity = findActivity(anchorView.context) ?: return
        toggleWithCircularReveal(activity, cx, cy)
    }

    fun toggleWithReveal(context: Context, anchorView: View) {
        val activity = (context as? Activity) ?: findActivity(anchorView.context)
        if (activity != null) {
            val loc = IntArray(2)
            anchorView.getLocationOnScreen(loc)
            val cx = loc[0] + anchorView.width / 2
            val cy = loc[1] + anchorView.height / 2
            toggleWithCircularReveal(activity, cx, cy)
        } else {
            toggleWithCircularReveal(anchorView)
        }
    }

    fun animateThemeChange(
        rootView: View,
        touchX: Int,
        touchY: Int,
        applyNewThemeAction: Runnable? = null
    ) {
        val activity = findActivity(rootView.context)
        if (activity != null) {
            toggleWithCircularReveal(activity, touchX, touchY, applyNewThemeAction)
        } else {
            applyNewThemeAction?.run()
        }
    }

    private fun triggerCircularReveal(activity: Activity, holder: SnapshotHolder) {
        val bitmap = holder.bitmap

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanupPending("triggerCircularReveal_no_decor")
            return
        }

        if (bitmap.isRecycled) {
            logDebug("Bitmap already recycled before reveal started -> aborting")
            cleanupPending("triggerCircularReveal_recycled_bitmap")
            return
        }

        // Disable touches while animation is in flight
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        val decorLoc = IntArray(2)
        decorView.getLocationOnScreen(decorLoc)
        val cx = holder.originX - decorLoc[0]
        val cy = holder.originY - decorLoc[1]

        val w = decorView.width.toFloat()
        val h = decorView.height.toFloat()

        // Dynamic radius calculation: distance from origin to farthest of 4 corners
        val d1 = hypot(cx.toDouble(), cy.toDouble())
        val d2 = hypot((w - cx).toDouble(), cy.toDouble())
        val d3 = hypot(cx.toDouble(), (h - cy).toDouble())
        val d4 = hypot((w - cx).toDouble(), (h - cy).toDouble())
        val maxRadius = max(max(d1, d2), max(d3, d4)).toFloat()

        val overlay = ThemeSwitchOverlayView(activity).apply {
            setSnapshot(bitmap)
            visibility = View.VISIBLE
        }
        themeSwitchOverlay = overlay

        // Clean up any stale overlay parent to prevent IllegalStateException
        (overlay.parent as? ViewGroup)?.removeView(overlay)
        decorView.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Remove the temporary pre-overlay now that ThemeSwitchOverlayView is in place
        var preOverlay: View?
        do {
            preOverlay = decorView.findViewWithTag<View>("pre_reveal_overlay")
            if (preOverlay != null) {
                (preOverlay as? ImageView)?.setImageDrawable(null)
                decorView.removeView(preOverlay)
            }
        } while (preOverlay != null)

        overlay.post {
            if (activity.isFinishing || activity.isDestroyed) {
                cleanupPending("activity_finishing_before_anim_start")
                return@post
            }
            overlay.startReveal(cx, cy, maxRadius) {
                logDebug("Reveal animation completed -> cleaning up pending transition")
                overlay.visibility = View.GONE
                (overlay.parent as? ViewGroup)?.removeView(overlay)
                cleanupPending("animation_complete")
            }
        }
    }

    fun setupToggleButton(context: Context, button: ImageButton) {
        val activity = findActivity(button.context) ?: findActivity(context)
        activity?.application?.let { ensureLifecycleRegistered(it) }

        val isDark = isDarkMode(context)
        button.setImageResource(if (isDark) R.drawable.ic_moon else R.drawable.ic_sun)
        button.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.eve_text))
        button.contentDescription = context.getString(
            if (isDark) R.string.theme_toggle_to_light else R.string.theme_toggle_to_dark
        )
        button.setOnClickListener {
            if (transitioning) return@setOnClickListener

            // Theme toggle icon switches as part of this reveal directly without rotating animation
            button.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
            toggleWithCircularReveal(button)
        }
    }

    /**
     * Telegram exact Day/Night theme-switch overlay.
     * Displays captured snapshot of old theme and punches an expanding anti-aliased circular
     * hole centered at the exact tap/switch origin (originX, originY) from 0 to maxRadius,
     * revealing the new theme underneath using hardware-layer PorterDuff.Mode.CLEAR masking
     * and cubic bezier easing (FastOutSlowInInterpolator, 400ms).
     */
    private class ThemeSwitchOverlayView(context: Context) : View(context) {

        private var snapshotBitmap: Bitmap? = null
        private var originX: Float = 0f
        private var originY: Float = 0f
        private var currentRadius: Float = 0f
        private var animator: ValueAnimator? = null
        private val dstRect = RectF()
        private var onCompleteCallback: (() -> Unit)? = null

        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        init {
            fitsSystemWindows = false
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        fun setSnapshot(bitmap: Bitmap) {
            snapshotBitmap = bitmap
            invalidate()
        }

        fun startReveal(cx: Float, cy: Float, maxRadius: Float, onComplete: () -> Unit) {
            originX = cx
            originY = cy
            currentRadius = 0f
            onCompleteCallback = onComplete

            animator = ValueAnimator.ofFloat(0f, maxRadius).apply {
                duration = 400L // 400ms duration per specification
                interpolator = FastOutSlowInInterpolator() // Telegram-exact cubic bezier curve
                addUpdateListener { va ->
                    currentRadius = va.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onCompleteCallback?.invoke()
                        onCompleteCallback = null
                    }
                })
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            val bmp = snapshotBitmap ?: return
            if (bmp.isRecycled) return

            dstRect.set(0f, 0f, width.toFloat(), height.toFloat())

            if (currentRadius <= 0f) {
                // Initial static frame: display old theme snapshot
                canvas.drawBitmap(bmp, null, dstRect, bitmapPaint)
            } else {
                // Telegram exact technique: off-screen layer with PorterDuff.Mode.CLEAR
                // Produces perfectly anti-aliased sub-pixel circular hole revealing new theme
                val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
                canvas.drawBitmap(bmp, null, dstRect, bitmapPaint)
                canvas.drawCircle(originX, originY, currentRadius, clearPaint)
                canvas.restoreToCount(saveCount)
            }
        }

        fun cancelAnimation() {
            animator?.cancel()
            animator = null
            snapshotBitmap = null
            onCompleteCallback = null
        }
    }
}

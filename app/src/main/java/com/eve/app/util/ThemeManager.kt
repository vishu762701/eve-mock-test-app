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
import android.graphics.Rect
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.BuildConfig
import com.eve.app.R
import kotlin.math.hypot
import kotlin.math.max

/**
 * ThemeManager: Controls Light <-> Dark theme switching with Telegram-style circular reveal.
 * Solves all known flicker causes:
 * 1. Synchronous/PixelCopy bitmap capture complete BEFORE theme change begins.
 * 2. Static full-screen overlay attached to DecorView during recreation (covers status & nav bar).
 * 3. Frame synchronization via OnPreDrawListener waits until new theme renders underneath.
 * 4. Expanding hole reveal (0 -> maxRadius) in both directions centered at the exact 3-dot icon.
 * 5. Full lifecycle cleanup and rapid-tap protection.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"
    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private data class SnapshotHolder(
        val bitmap: Bitmap,
        val originX: Int,
        val originY: Int,
        val oldActivityId: Int,
        val activityClassName: String,
        val captureTimestamp: Long
    )

    private var pendingSnapshot: SnapshotHolder? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var activeOverlay: CircularRevealOverlayView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        logDebug("Timeout reached, cleaning up pending transition")
        cleanupPending()
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
                    logDebug("New Activity resumed -> Waiting for first draw before triggering reveal")
                    activity.overridePendingTransition(0, 0)
                    waitForNewThemeRenderAndReveal(activity, holder)
                }
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                val holder = pendingSnapshot ?: return
                if (System.identityHashCode(activity) == holder.oldActivityId) {
                    logDebug("Old Activity destroyed during recreate (expected)")
                    return
                }
                if (activity.javaClass.name == holder.activityClassName) {
                    logDebug("Activity destroyed -> cleaning up overlay")
                    activeOverlay?.cancelAnimation()
                    (activity.window.decorView as? ViewGroup)?.removeView(activeOverlay)
                    activeOverlay = null
                    cleanupPending()
                }
            }
        })
    }

    private fun attachStaticOverlayImmediately(activity: Activity, bitmap: Bitmap) {
        val decorView = activity.window.decorView as? ViewGroup ?: return
        if (bitmap.isRecycled) return
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
            cleanupPending()
            return
        }

        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                decorView.viewTreeObserver.removeOnPreDrawListener(this)
                logDebug("New theme preDraw confirmed -> Launching reveal animation")
                triggerCircularReveal(activity, holder)
                return true
            }
        })
        decorView.invalidate()
    }

    private fun cleanupPending() {
        mainHandler.removeCallbacks(timeoutRunnable)
        pendingSnapshot?.bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
                logDebug("Bitmap successfully recycled")
            }
        }
        pendingSnapshot = null
        transitioning = false
    }

    /**
     * Triggers theme toggle with a full-screen Telegram-style circular reveal expanding
     * from the specified on-screen coordinates (originX, originY).
     */
    fun toggleWithCircularReveal(activity: Activity, originX: Int, originY: Int) {
        if (transitioning) {
            logDebug("Rapid tap blocked: transition already in progress")
            return
        }

        if (!areAnimationsEnabled(activity)) {
            logDebug("Animations disabled in accessibility -> instant toggle")
            toggle(activity)
            return
        }

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            toggle(activity)
            return
        }

        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) {
            toggle(activity)
            return
        }

        logDebug("Theme toggle initiated from anchor ($originX, $originY). Capturing bitmap...")
        ensureLifecycleRegistered(activity.application)

        // Capture static snapshot: PixelCopy on API 26+ with synchronous Canvas fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val window = activity.window
                PixelCopy.request(
                    window,
                    Rect(0, 0, width, height),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            logDebug("PixelCopy capture SUCCESS")
                            commitRevealTransition(activity, bitmap, originX, originY)
                        } else {
                            logDebug("PixelCopy failed with code $copyResult -> fallback to Canvas draw")
                            fallbackSynchronousCaptureAndToggle(activity, decorView, originX, originY)
                        }
                    },
                    mainHandler
                )
                return
            } catch (t: Throwable) {
                logDebug("PixelCopy exception: ${t.message} -> fallback to Canvas draw")
            }
        }

        fallbackSynchronousCaptureAndToggle(activity, decorView, originX, originY)
    }

    private fun fallbackSynchronousCaptureAndToggle(
        activity: Activity,
        decorView: ViewGroup,
        originX: Int,
        originY: Int
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
            toggle(activity)
            return
        }

        commitRevealTransition(activity, bitmap, originX, originY)
    }

    private fun commitRevealTransition(
        activity: Activity,
        bitmap: Bitmap,
        originX: Int,
        originY: Int
    ) {
        transitioning = true
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
        toggle(activity)
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

    private fun triggerCircularReveal(activity: Activity, holder: SnapshotHolder) {
        val bitmap = holder.bitmap
        val cx = holder.originX
        val cy = holder.originY

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanupPending()
            return
        }

        // Disable touches while animation is in flight
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        val overlayView = CircularRevealOverlayView(activity, bitmap, cx, cy) {
            logDebug("Reveal animation completed -> Removing overlay & restoring touch")
            decorView.removeView(activeOverlay)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            activeOverlay = null
            cleanupPending()
        }
        activeOverlay = overlayView
        decorView.addView(overlayView)

        // Remove the temporary pre-overlay now that CircularRevealOverlayView is in place
        val preOverlay = decorView.findViewWithTag<View>("pre_reveal_overlay")
        if (preOverlay != null) {
            decorView.removeView(preOverlay)
        }

        overlayView.post {
            if (activity.isFinishing || activity.isDestroyed) {
                cleanupPending()
                return@post
            }
            overlayView.startAnimation()
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

            button.animate()
                .scaleX(0.85f)
                .scaleY(0.85f)
                .alpha(0.6f)
                .setDuration(180)
                .setInterpolator(FastOutSlowInInterpolator())
                .withEndAction {
                    button.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
                    button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(200)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                .start()

            toggleWithCircularReveal(button)
        }
    }

    /**
     * Full-screen overlay that displays the captured snapshot of the old theme.
     * Cuts an expanding circular hole centered at (cx, cy) from radius 0 to maxRadius,
     * cleanly revealing the new theme underneath in both Light->Dark and Dark->Light.
     */
    private class CircularRevealOverlayView(
        context: Context,
        private val bitmap: Bitmap,
        private val cx: Int,
        private val cy: Int,
        private val onComplete: () -> Unit
    ) : View(context) {

        private var currentRadius: Float = 0f
        private val clipPath = Path()
        private var animator: ValueAnimator? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        init {
            fitsSystemWindows = false
            setLayerType(LAYER_TYPE_HARDWARE, null)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        fun startAnimation() {
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0f || h <= 0f) {
                post { startAnimation() }
                return
            }

            // Calculate exact distance to farthest of the four screen corners
            val d1 = hypot(cx.toDouble(), cy.toDouble())
            val d2 = hypot((w - cx).toDouble(), cy.toDouble())
            val d3 = hypot(cx.toDouble(), (h - cy).toDouble())
            val d4 = hypot((w - cx).toDouble(), (h - cy).toDouble())
            val maxRadius = max(max(d1, d2), max(d3, d4)).toFloat()

            logDebug("Starting reveal animation from ($cx, $cy) to radius $maxRadius (duration 400ms)")

            animator = ValueAnimator.ofFloat(0f, maxRadius).apply {
                duration = 400L // 350-450ms specification
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { va ->
                    currentRadius = va.animatedValue as Float
                    invalidate()
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onComplete()
                    }
                })
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (bitmap.isRecycled) return

            if (currentRadius <= 0f) {
                // Entire screen covered by old snapshot
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } else {
                // Expanding hole reveal: inside circle is new theme underneath, outside is old snapshot
                clipPath.reset()
                clipPath.fillType = Path.FillType.EVEN_ODD
                clipPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
                clipPath.addCircle(cx.toFloat(), cy.toFloat(), currentRadius, Path.Direction.CW)

                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                canvas.restore()
            }
        }

        fun cancelAnimation() {
            animator?.cancel()
            animator = null
        }
    }
}

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
import java.lang.ref.WeakReference
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
    private const val KEY_LAST_REVEAL_END_ANCHOR = "key_last_reveal_end_anchor"
    private const val ANCHOR_TOP_RIGHT = "top_right"
    private const val ANCHOR_BOTTOM_LEFT = "bottom_left"

    private data class TravelingCoords(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val targetAnchor: String
    )

    private data class SnapshotHolder(
        val bitmap: Bitmap,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val targetAnchor: String,
        val oldActivityId: Int,
        val activityClassName: String,
        val captureTimestamp: Long
    )

    private var pendingSnapshot: SnapshotHolder? = null
    private var sourceActivityRef: WeakReference<Activity>? = null
    private var targetActivityRef: WeakReference<Activity>? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var activeOverlay: CircularRevealOverlayView? = null
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
                // Check if user navigated away while transition was in progress
                if (activity.javaClass.name == holder.activityClassName) {
                    logDebug("Activity stopped during transition (${activity.javaClass.simpleName}) -> cleaning up safely")
                    detachAllOverlays(activity)
                    cleanupPending("activity_stopped")
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                detachAllOverlays(activity)
                val holder = pendingSnapshot ?: return
                if (System.identityHashCode(activity) == holder.oldActivityId) {
                    logDebug("Old Activity destroyed during recreate (expected)")
                    return
                }
                if (activity.javaClass.name == holder.activityClassName) {
                    logDebug("Activity destroyed -> cleaning up overlay")
                    cleanupPending("activity_destroyed")
                }
            }
        })
    }

    /**
     * Safely detaches and clears all overlay views associated with the theme transition
     * from the specified Activity's decorView.
     * Crucial: nulls out image references and removes views from the hierarchy BEFORE
     * any bitmap is recycled to prevent "Canvas: trying to use a recycled bitmap".
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

            // 2. Remove and cancel activeOverlay if attached
            activeOverlay?.let { overlay ->
                if (overlay.parent == decorView || overlay.parent != null) {
                    overlay.cancelAnimation()
                    (overlay.parent as? ViewGroup)?.removeView(overlay)
                    logDebug("Removed activeOverlay from ${activity.javaClass.simpleName}")
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
                logDebug("New theme preDraw confirmed -> Launching reveal animation")
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

        // Step 2: Ensure activeOverlay animation is cancelled and bitmap reference cleared
        activeOverlay?.let { overlay ->
            overlay.cancelAnimation()
            (overlay.parent as? ViewGroup)?.removeView(overlay)
            activeOverlay = null
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
     * Triggers theme toggle with a traveling corner-to-corner circular reveal.
     * Alternates between:
     * - Top-Right anchor (live center of 3-dot overflow button)
     * - Bottom-Left anchor (fixed 24dp inset from bottom-left screen edge)
     * Persists last ended anchor in SharedPreferences across app restarts.
     * center(t) = start + (end - start) * t
     * radius(t) = t * R_final
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

        val insetPx = (24 * activity.resources.displayMetrics.density).toInt()
        val trX = originX.toFloat()
        val trY = originY.toFloat()
        val blX = insetPx.toFloat()
        val blY = (height - insetPx).toFloat()

        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastEndedAt = prefs.getString(KEY_LAST_REVEAL_END_ANCHOR, null)

        val coords = if (lastEndedAt == ANCHOR_BOTTOM_LEFT) {
            // Previous toggle ended at bottom-left -> this toggle starts at bottom-left and travels to top-right
            TravelingCoords(
                startX = blX,
                startY = blY,
                endX = trX,
                endY = trY,
                targetAnchor = ANCHOR_TOP_RIGHT
            )
        } else {
            // Initial toggle (null) or previous ended at top-right -> this toggle starts at top-right and travels to bottom-left
            TravelingCoords(
                startX = trX,
                startY = trY,
                endX = blX,
                endY = blY,
                targetAnchor = ANCHOR_BOTTOM_LEFT
            )
        }

        logDebug("Theme toggle initiated: lastEndedAt=$lastEndedAt -> traveling from (${coords.startX}, ${coords.startY}) to (${coords.endX}, ${coords.endY}), target=${coords.targetAnchor}. Capturing bitmap...")
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
                            commitRevealTransition(activity, bitmap, coords)
                        } else {
                            logDebug("PixelCopy failed with code $copyResult -> fallback to Canvas draw")
                            fallbackSynchronousCaptureAndToggle(activity, decorView, coords)
                        }
                    },
                    mainHandler
                )
                return
            } catch (t: Throwable) {
                logDebug("PixelCopy exception: ${t.message} -> fallback to Canvas draw")
            }
        }

        fallbackSynchronousCaptureAndToggle(activity, decorView, coords)
    }

    private fun fallbackSynchronousCaptureAndToggle(
        activity: Activity,
        decorView: ViewGroup,
        coords: TravelingCoords
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

        commitRevealTransition(activity, bitmap, coords)
    }

    private fun commitRevealTransition(
        activity: Activity,
        bitmap: Bitmap,
        coords: TravelingCoords
    ) {
        // Persist target anchor so next toggle starts from where this one ends
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_REVEAL_END_ANCHOR, coords.targetAnchor)
            .apply()

        transitioning = true
        sourceActivityRef = WeakReference(activity)
        pendingSnapshot = SnapshotHolder(
            bitmap = bitmap,
            startX = coords.startX,
            startY = coords.startY,
            endX = coords.endX,
            endY = coords.endY,
            targetAnchor = coords.targetAnchor,
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

        // Dynamically verify end anchor on new activity to ensure exact landing precision
        val insetPx = (24 * activity.resources.displayMetrics.density).toInt()
        val liveEndX: Float
        val liveEndY: Float

        if (holder.targetAnchor == ANCHOR_TOP_RIGHT) {
            val overflowBtn = activity.findViewById<View>(R.id.btnOverflow)
            if (overflowBtn != null && overflowBtn.isAttachedToWindow && overflowBtn.width > 0) {
                val loc = IntArray(2)
                overflowBtn.getLocationOnScreen(loc)
                liveEndX = (loc[0] + overflowBtn.width / 2).toFloat()
                liveEndY = (loc[1] + overflowBtn.height / 2).toFloat()
            } else {
                liveEndX = holder.endX
                liveEndY = holder.endY
            }
        } else {
            liveEndX = insetPx.toFloat()
            liveEndY = (decorView.height - insetPx).toFloat()
        }

        val overlayView = CircularRevealOverlayView(
            activity,
            bitmap,
            holder.startX,
            holder.startY,
            liveEndX,
            liveEndY
        ) {
            logDebug("Reveal animation completed -> cleaning up pending transition")
            cleanupPending("animation_complete")
        }
        activeOverlay = overlayView

        // Clean up any stale activeOverlay parent to prevent IllegalStateException
        (overlayView.parent as? ViewGroup)?.removeView(overlayView)
        decorView.addView(overlayView)

        // Remove the temporary pre-overlay now that CircularRevealOverlayView is in place
        var preOverlay: View?
        do {
            preOverlay = decorView.findViewWithTag<View>("pre_reveal_overlay")
            if (preOverlay != null) {
                (preOverlay as? ImageView)?.setImageDrawable(null)
                decorView.removeView(preOverlay)
            }
        } while (preOverlay != null)

        overlayView.post {
            if (activity.isFinishing || activity.isDestroyed) {
                cleanupPending("activity_finishing_before_anim_start")
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
     * Cuts an expanding circular hole whose center travels linearly from (startX, startY)
     * to (endX, endY) and whose radius expands as t * R_final, cleanly revealing the new theme underneath.
     */
    private class CircularRevealOverlayView(
        context: Context,
        private var bitmap: Bitmap?,
        private val startX: Float,
        private val startY: Float,
        private val endX: Float,
        private val endY: Float,
        private val onComplete: () -> Unit
    ) : View(context) {

        private var currentX: Float = startX
        private var currentY: Float = startY
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

            // Calculate exact distance from the END anchor to all four screen corners
            val d1 = hypot(endX.toDouble(), endY.toDouble())
            val d2 = hypot((w - endX).toDouble(), endY.toDouble())
            val d3 = hypot(endX.toDouble(), (h - endY).toDouble())
            val d4 = hypot((w - endX).toDouble(), (h - endY).toDouble())
            val rFinal = max(max(d1, d2), max(d3, d4)).toFloat()

            logDebug("Starting traveling reveal: ($startX, $startY) -> ($endX, $endY), rFinal=$rFinal (duration 550ms)")

            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 550L
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { va ->
                    val t = va.animatedValue as Float
                    currentX = startX + (endX - startX) * t
                    currentY = startY + (endY - startY) * t
                    currentRadius = t * rFinal
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
            val bmp = bitmap ?: return
            if (bmp.isRecycled) return

            if (currentRadius <= 0f) {
                // Entire screen covered by old snapshot
                canvas.drawBitmap(bmp, 0f, 0f, paint)
            } else {
                // Expanding hole reveal: inside circle is new theme underneath, outside is old snapshot
                clipPath.reset()
                clipPath.fillType = Path.FillType.EVEN_ODD
                clipPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
                clipPath.addCircle(currentX, currentY, currentRadius, Path.Direction.CW)

                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(bmp, 0f, 0f, paint)
                canvas.restore()
            }
        }

        fun cancelAnimation() {
            animator?.cancel()
            animator = null
            bitmap = null
        }
    }
}

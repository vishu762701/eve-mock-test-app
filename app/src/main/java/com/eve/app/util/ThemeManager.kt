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
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
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
 * Faithful port of Telegram's Day/Night theme switch:
 * - Upstream: org.telegram.ui.LaunchActivity
 * - Day -> Night: circular reveal animates new live dark content expanding outward over old light screenshot.
 * - Night -> Day: circular reveal animates old dark screenshot shrinking inward over new live light content.
 * - ViewAnimationUtils.createCircularReveal native implementation with TelegramThemeEasing (400ms).
 * - Multi-transaction identity, asynchronous PixelCopy safety, and transition-aware system bar sync.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"
    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private data class SnapshotHolder(
        val transitionId: Long,
        val bitmap: Bitmap,
        val originX: Float,
        val originY: Float,
        val isDarkModeTarget: Boolean,
        val oldActivityId: Int,
        val activityClassName: String,
        val captureTimestamp: Long
    )

    private var currentTransitionId = 0L
    private var pendingSnapshot: SnapshotHolder? = null
    private var sourceActivityRef: WeakReference<Activity>? = null
    private var targetActivityRef: WeakReference<Activity>? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var currentThemeSwitchOverlay: ImageView? = null
    private var currentAnimator: Animator? = null
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
        app?.let { 
            ensureLifecycleRegistered(it)
        }

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
        return ThemeSwitchAnimator.isDarkMode(context)
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
                    SystemBarHelper.syncSystemBars(activity)
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
                SystemBarHelper.syncSystemBars(activity)
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

            // 1. Find and remove ALL views tagged "pre_reveal_overlay" or "theme_switch_freeze_overlay"
            val tags = listOf("pre_reveal_overlay", "theme_switch_freeze_overlay")
            for (tag in tags) {
                var overlay: View?
                do {
                    overlay = decorView.findViewWithTag<View>(tag)
                    if (overlay != null) {
                        (overlay as? ImageView)?.setImageDrawable(null)
                        decorView.removeView(overlay)
                        logDebug("Removed $tag from ${activity.javaClass.simpleName}")
                    }
                } while (overlay != null)
            }

            // 2. Remove currentThemeSwitchOverlay ONLY if attached to THIS activity's decorView
            currentThemeSwitchOverlay?.let { overlay ->
                if (overlay.parent === decorView) {
                    overlay.setImageDrawable(null)
                    decorView.removeView(overlay)
                    logDebug("Removed currentThemeSwitchOverlay from ${activity.javaClass.simpleName}")
                }
            }

            // 3. Cancel animator safely without re-entry
            currentAnimator?.let { anim ->
                currentAnimator = null
                anim.removeAllListeners()
                anim.cancel()
            }

            // 4. Clear window touch-blocking flag to prevent UI lockup
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

        val expectedTransitionId = holder.transitionId
        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                }
                if (pendingSnapshot?.transitionId != expectedTransitionId ||
                    currentTransitionId != expectedTransitionId ||
                    holder.bitmap.isRecycled ||
                    activity.isFinishing ||
                    activity.isDestroyed
                ) {
                    logDebug("Aborting reveal: transition was cancelled, ID mismatch, bitmap recycled, or activity finishing")
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

    fun cleanupPending(reason: String = "unknown") {
        logDebug("cleanupPending triggered (reason: $reason)")
        mainHandler.removeCallbacks(timeoutRunnable)

        currentAnimator?.let { anim ->
            currentAnimator = null
            anim.removeAllListeners()
            anim.cancel()
        }

        // Step 1: Detach and clear all overlays from both source and target activities before bitmap recycle
        val source = sourceActivityRef?.get()
        val target = targetActivityRef?.get()
        detachAllOverlays(source)
        detachAllOverlays(target)
        sourceActivityRef = null
        targetActivityRef = null

        currentThemeSwitchOverlay = null

        // Step 2: Now that all views referencing the bitmap have been removed from the
        // view hierarchy and their ImageDrawables cleared, it is safe to recycle the bitmap.
        val bmp = pendingSnapshot?.bitmap
        pendingSnapshot = null
        transitioning = false

        val finalActivity = target ?: source
        if (finalActivity != null) {
            finalActivity.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            SystemBarHelper.syncSystemBars(finalActivity, force = true)
        }

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
    fun toggleWithCircularReveal(
        activity: Activity,
        originX: Int,
        originY: Int,
        isDarkModeTarget: Boolean = !isDarkMode(activity),
        applyAction: Runnable? = null
    ) {
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

        // Lock transitions IMMEDIATELY so rapid taps cannot start concurrent captures
        transitioning = true
        val transitionId = ++currentTransitionId

        logDebug("Theme toggle initiated at ($originX, $originY) [transitionId: $transitionId, targetDark: $isDarkModeTarget]. Capturing bitmap...")
        ensureLifecycleRegistered(activity.application)

        // Capture static snapshot: PixelCopy on API 26+ with synchronous Canvas fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var bitmap: Bitmap? = null
            var timeoutRunnable: Runnable? = null
            try {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val window = activity.window
                var pixelCopyDone = false
                val scheduledTimeout = Runnable {
                    if (!pixelCopyDone && currentTransitionId == transitionId) {
                        pixelCopyDone = true
                        logDebug("PixelCopy timed out -> fallback to Canvas draw")
                        if (!bitmap.isRecycled) {
                            try { bitmap.recycle() } catch (_: Throwable) {}
                        }
                        fallbackSynchronousCaptureAndToggle(transitionId, activity, decorView, originX.toFloat(), originY.toFloat(), isDarkModeTarget, applyAction)
                    }
                }
                timeoutRunnable = scheduledTimeout
                mainHandler.postDelayed(scheduledTimeout, 120)

                PixelCopy.request(
                    window,
                    Rect(0, 0, width, height),
                    bitmap,
                    { copyResult ->
                        mainHandler.removeCallbacks(scheduledTimeout)
                        if (!pixelCopyDone && currentTransitionId == transitionId) {
                            pixelCopyDone = true
                            if (copyResult == PixelCopy.SUCCESS) {
                                logDebug("PixelCopy capture SUCCESS")
                                commitRevealTransition(transitionId, activity, bitmap, originX.toFloat(), originY.toFloat(), isDarkModeTarget, applyAction)
                            } else {
                                logDebug("PixelCopy failed with code $copyResult -> fallback to Canvas draw")
                                if (!bitmap.isRecycled) {
                                    try { bitmap.recycle() } catch (_: Throwable) {}
                                }
                                fallbackSynchronousCaptureAndToggle(transitionId, activity, decorView, originX.toFloat(), originY.toFloat(), isDarkModeTarget, applyAction)
                            }
                        } else {
                            if (!bitmap.isRecycled) {
                                try { bitmap.recycle() } catch (_: Throwable) {}
                            }
                        }
                    },
                    mainHandler
                )
                return
            } catch (t: Throwable) {
                logDebug("PixelCopy exception: ${t.message} -> fallback to Canvas draw")
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                if (bitmap != null && !bitmap.isRecycled) {
                    try { bitmap.recycle() } catch (_: Throwable) {}
                }
            }
        }

        fallbackSynchronousCaptureAndToggle(transitionId, activity, decorView, originX.toFloat(), originY.toFloat(), isDarkModeTarget, applyAction)
    }

    private fun fallbackSynchronousCaptureAndToggle(
        transitionId: Long,
        activity: Activity,
        decorView: ViewGroup,
        originX: Float,
        originY: Float,
        isDarkModeTarget: Boolean,
        applyAction: Runnable? = null
    ) {
        if (currentTransitionId != transitionId) {
            logDebug("Fallback capture ignored: transition ID mismatch")
            return
        }

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
            transitioning = false
            if (applyAction != null) {
                applyAction.run()
            } else {
                toggle(activity)
            }
            return
        }

        commitRevealTransition(transitionId, activity, bitmap, originX, originY, isDarkModeTarget, applyAction)
    }

    private fun commitRevealTransition(
        transitionId: Long,
        activity: Activity,
        bitmap: Bitmap,
        originX: Float,
        originY: Float,
        isDarkModeTarget: Boolean,
        applyAction: Runnable? = null
    ) {
        if (currentTransitionId != transitionId) {
            if (!bitmap.isRecycled) {
                try { bitmap.recycle() } catch (_: Throwable) {}
            }
            return
        }

        sourceActivityRef = WeakReference(activity)
        pendingSnapshot = SnapshotHolder(
            transitionId = transitionId,
            bitmap = bitmap,
            originX = originX,
            originY = originY,
            isDarkModeTarget = isDarkModeTarget,
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
        mainHandler.postDelayed(timeoutRunnable, 2000L)

        logDebug("Applying new theme mode via AppCompatDelegate...")
        if (applyAction != null) {
            applyAction.run()
        } else {
            toggle(activity)
        }
    }

    fun toggleWithCircularReveal(anchorView: View) {
        if (isTransitioning) return
        val activity = findActivity(anchorView.context) ?: return
        ThemeSwitchAnimator.animate(activity, anchorView, !ThemeSwitchAnimator.isDarkMode(activity))
    }

    fun toggleWithReveal(context: Context, anchorView: View) {
        val activity = (context as? Activity) ?: findActivity(anchorView.context)
        if (activity != null) {
            ThemeSwitchAnimator.animate(activity, anchorView, !ThemeSwitchAnimator.isDarkMode(activity))
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
            val goingDark = !ThemeSwitchAnimator.isDarkMode(activity)
            toggleWithCircularReveal(
                activity = activity,
                originX = touchX,
                originY = touchY,
                isDarkModeTarget = goingDark,
                applyAction = applyNewThemeAction ?: Runnable {
                    ThemeSwitchAnimator.switch(activity, goingDark)
                }
            )
        } else {
            applyNewThemeAction?.run()
        }
    }

    /**
     * Exact Telegram circular reveal implementation.
     * Upstream reference:
     * - TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java
     *
     * Layer placement & animation:
     * DAY -> NIGHT (toDark = true):
     * - Old light screenshot underneath (themeSwitchImageView added at index 0 of decorView).
     * - New live dark content above it (at index 1).
     * - ViewAnimationUtils.createCircularReveal animates the NEW LIVE CONTENT (0 -> finalRadius).
     * - Outside the circle remains the old light screenshot.
     *
     * NIGHT -> DAY (toDark = false):
     * - New live light content underneath (at index 0).
     * - Old dark screenshot above it (themeSwitchImageView added on top).
     * - ViewAnimationUtils.createCircularReveal animates the OLD SCREENSHOT (finalRadius -> 0).
     * - Inside the shrinking circle remains the old dark screenshot.
     *
     * Easing: TelegramThemeEasing (400ms duration).
     */
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

        // Remove any temporary pre-overlay from this decorView
        var preOverlay: View?
        do {
            preOverlay = decorView.findViewWithTag<View>("pre_reveal_overlay")
            if (preOverlay != null) {
                (preOverlay as? ImageView)?.setImageDrawable(null)
                decorView.removeView(preOverlay)
            }
        } while (preOverlay != null)

        val toDark = holder.isDarkModeTarget
        val themeSwitchImageView = ImageView(activity).apply {
            tag = "theme_switch_freeze_overlay"
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            fitsSystemWindows = false
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        currentThemeSwitchOverlay = themeSwitchImageView

        // Telegram layer placement:
        // if (toDark) frameLayout.addView(themeSwitchImageView, 0)
        // else frameLayout.addView(themeSwitchImageView, 1)
        if (toDark) {
            decorView.addView(themeSwitchImageView, 0)
        } else {
            decorView.addView(themeSwitchImageView)
        }

        // Target view to animate with ViewAnimationUtils.createCircularReveal:
        // DAY -> NIGHT: live content is animated from 0 to finalRadius
        // NIGHT -> DAY: old screenshot (themeSwitchImageView) is animated from finalRadius to 0
        val targetView: View = if (toDark) {
            if (decorView.childCount > 1) decorView.getChildAt(1)
            else decorView.findViewById<View>(android.R.id.content) ?: themeSwitchImageView
        } else {
            themeSwitchImageView
        }

        targetView.post {
            if (activity.isFinishing || activity.isDestroyed || currentTransitionId != holder.transitionId) {
                cleanupPending("activity_finishing_before_anim_start")
                return@post
            }

            val targetLoc = IntArray(2)
            targetView.getLocationOnScreen(targetLoc)
            val revealCx = (holder.originX - targetLoc[0]).toInt()
            val revealCy = (holder.originY - targetLoc[1]).toInt()

            val w = targetView.width.toFloat()
            val h = targetView.height.toFloat()
            val finalRadius = ThemeSwitchAnimator.calculateMaxRadius(
                revealCx.toFloat(),
                revealCy.toFloat(),
                w,
                h
            )

            val startRadius = if (toDark) 0f else finalRadius
            val endRadius = if (toDark) finalRadius else 0f

            val anim = try {
                ViewAnimationUtils.createCircularReveal(
                    targetView,
                    revealCx,
                    revealCy,
                    startRadius,
                    endRadius
                )
            } catch (t: Throwable) {
                logDebug("createCircularReveal error: ${t.message} -> fallback cleanup")
                cleanupPending("reveal_creation_error")
                return@post
            }

            anim.duration = 400L
            anim.interpolator = TelegramThemeEasing

            var completed = false
            val finishAction = Runnable {
                if (!completed) {
                    completed = true
                    logDebug("Circular reveal completed -> cleaning up")
                    if (themeSwitchImageView.parent === decorView) {
                        themeSwitchImageView.setImageDrawable(null)
                        decorView.removeView(themeSwitchImageView)
                    }
                    if (currentThemeSwitchOverlay === themeSwitchImageView) {
                        currentThemeSwitchOverlay = null
                    }
                    currentAnimator = null
                    cleanupPending("animation_complete")
                }
            }

            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishAction.run()
                }

                override fun onAnimationCancel(animation: Animator) {
                    finishAction.run()
                }
            })

            currentAnimator = anim
            anim.start()
        }
    }

    fun setupToggleButton(context: Context, button: ImageButton) {
        val activity = findActivity(button.context) ?: findActivity(context)
        activity?.application?.let {
            ensureLifecycleRegistered(it)
            ThemeSwitchAnimator.ensureLifecycleRegistered(it)
        }

        val isDark = isDarkMode(context)
        button.setImageResource(if (isDark) R.drawable.ic_theme_sun else R.drawable.ic_theme_moon)
        button.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.eve_text))
        button.contentDescription = context.getString(
            if (isDark) R.string.theme_toggle_to_light else R.string.theme_toggle_to_dark
        )
        button.setOnClickListener {
            if (isTransitioning) return@setOnClickListener
            val goingDark = !isDarkMode(context)
            if (activity != null) {
                ThemeSwitchAnimator.animate(activity, button, goingDark)
            } else {
                toggleWithCircularReveal(button)
            }
        }
    }
}

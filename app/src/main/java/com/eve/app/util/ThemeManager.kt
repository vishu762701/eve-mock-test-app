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
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.eve.app.BuildConfig
import com.eve.app.R
import java.lang.ref.WeakReference

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
    const val PREFS = "eve_prefs"
    const val KEY_DARK_MODE = "key_dark_mode"

    private data class SnapshotHolder(
        val transitionId: Long,
        val bitmap: Bitmap,
        val originX: Float,
        val originY: Float,
        val isDarkModeTarget: Boolean,
        val oldActivityId: Int,
        val activityClassName: String,
        val captureTimestamp: Long,
        val capturedWidth: Int,
        val capturedHeight: Int,
        val orientation: Int
    )

    private var currentTransitionId = 0L
    private data class RecreationTicket(val id: Long, val sourceId: Int, val className: String, val targetDark: Boolean)
    private val themeRecreationActivities = java.util.WeakHashMap<Activity, Long>()
    private var recreationTicket: RecreationTicket? = null
    private var themeApplicationStarted = false
    private var pendingSnapshot: SnapshotHolder? = null
    private var sourceActivityRef: WeakReference<Activity>? = null
    private var targetActivityRef: WeakReference<Activity>? = null
    private var currentAnimatorActivityRef: WeakReference<Activity>? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var popupHandoff: Any? = null
    private val handoffTimeout = Runnable { popupHandoff = null }

    fun reservePopupHandoff(): Any? {
        if (isTransitioning) return null
        return Any().also { popupHandoff = it; mainHandler.postDelayed(handoffTimeout, 2000L) }
    }

    fun ownsPopupHandoff(owner: Any): Boolean = popupHandoff === owner

    fun releasePopupHandoff(owner: Any): Boolean {
        if (popupHandoff !== owner) return false
        popupHandoff = null
        mainHandler.removeCallbacks(handoffTimeout)
        return true
    }
    private var activeTransitionTargetDark: Boolean? = null
    private var touchLockInstalled = false
    private var currentThemeSwitchOverlay: ImageView? = null
    private var currentAnimator: Animator? = null
    private val overlayOwners = java.util.WeakHashMap<View, Long>()
    private var animatorTransitionId = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var captureInterceptorForTest: ((commitAction: Runnable) -> Unit)? = null

    fun setCaptureInterceptorForTest(interceptor: ((commitAction: Runnable) -> Unit)?) {
        captureInterceptorForTest = interceptor
    }

    @Volatile private var preparationInterceptorForTest: ((Runnable) -> Unit)? = null
    fun setPreparationInterceptorForTest(interceptor: ((Runnable) -> Unit)?) {
        preparationInterceptorForTest = if (BuildConfig.DEBUG) interceptor else null
    }

    private val timeoutRunnable = Runnable {
        val target = targetActivityRef?.get()
        val source = sourceActivityRef?.get()
        val act = target ?: source
        val targetDark = pendingSnapshot?.isDarkModeTarget ?: activeTransitionTargetDark
        val actualUiModeDark = act?.let {
            (it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        val savedPref = act?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.let {
            if (it.contains(KEY_DARK_MODE)) it.getBoolean(KEY_DARK_MODE, false) else null
        }
        val delegateMode = AppCompatDelegate.getDefaultNightMode()
        val actState = when {
            act == null -> "null"
            act.isDestroyed -> "destroyed"
            act.isFinishing -> "finishing"
            else -> "alive"
        }
        logDebug("Timeout reached [transitionId: $currentTransitionId, requestedTarget: $targetDark, actualUiModeDark: $actualUiModeDark, savedPref: $savedPref, delegateMode: $delegateMode, transitioning: $transitioning, activityState: $actState]. Cleaning up pending transition.")
        cleanupPending("timeout")
    }

    val isTransitioning: Boolean
        get() = transitioning || popupHandoff != null

    val activeTransitionId: Long
        get() = currentTransitionId

    val activeTargetIsDark: Boolean?
        get() = activeTransitionTargetDark ?: pendingSnapshot?.isDarkModeTarget

    @Volatile private var eventObserverForTest: ((String, Long, View?) -> Unit)? = null
    fun setEventObserverForTest(observer: ((String, Long, View?) -> Unit)?) {
        eventObserverForTest = if (BuildConfig.DEBUG) observer else null
    }

    private fun logDebug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[${SystemClock.uptimeMillis()}] $message")
        }
    }

    fun trace(stage: String, view: View? = null, id: Long = currentTransitionId) {
        if (BuildConfig.DEBUG) eventObserverForTest?.invoke(stage, id, view)
        val activity = view?.let { findActivity(it.context) }
        logDebug("event=$stage id=$id bounds=${view?.width}x${view?.height} position=${view?.left},${view?.top} attached=${view?.isAttachedToWindow} laidOut=${view?.isLaidOut} finishing=${activity?.isFinishing} destroyed=${activity?.isDestroyed} focus=${view?.hasWindowFocus()}")
    }

    fun isThemeRecreation(activity: Activity, savedState: Bundle?): Boolean {
        val id = savedState?.getLong("eve_theme_recreation", -1L) ?: return false
        if (themeRecreationActivities[activity] == id) return true
        val ticket = recreationTicket ?: return false
        return id == ticket.id && activity.javaClass.name == ticket.className &&
            System.identityHashCode(activity) != ticket.sourceId &&
            ((activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES) == ticket.targetDark
    }

    private fun applyForRecreation(activity: Activity, targetDark: Boolean, action: Runnable?) {
        ensureLifecycleRegistered(activity.application)
        val actualDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val targetMode = if (targetDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        recreationTicket = if (actualDark != targetDark && AppCompatDelegate.getDefaultNightMode() != targetMode) {
            RecreationTicket(currentTransitionId, System.identityHashCode(activity), activity.javaClass.name, targetDark)
        } else null
        themeApplicationStarted = true
        if (action != null) action.run() else applyThemeTarget(activity, targetDark)
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
        applyThemeTarget(context, !isDarkMode(context))
    }

    fun applyThemeTarget(context: Context, isDarkModeTarget: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DARK_MODE, isDarkModeTarget)
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkModeTarget) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
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
                if (isThemeRecreation(activity, savedInstanceState)) {
                    recreationTicket?.let { themeRecreationActivities[activity] = it.id }
                    recreationTicket = null
                }
                val holder = pendingSnapshot ?: return
                if (activity.javaClass.name == holder.activityClassName &&
                    System.identityHashCode(activity) != holder.oldActivityId
                ) {
                    targetActivityRef = WeakReference(activity)
                    trace("target_created", activity.window.decorView, holder.transitionId)
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
                    trace("target_resumed", activity.window.decorView, holder.transitionId)
                    logDebug("New Activity resumed -> Waiting for first draw before triggering reveal")
                    activity.overridePendingTransition(0, 0)
                    waitForNewThemeRenderAndReveal(activity, holder)
                }
                SystemBarHelper.syncSystemBars(activity)
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                if (sourceActivityRef?.get() === activity && transitioning && !themeApplicationStarted) {
                    cleanupPending("source_stopped_during_preparation")
                    return
                }
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

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                recreationTicket?.let { ticket ->
                    if (System.identityHashCode(activity) == ticket.sourceId) {
                        outState.putLong("eve_theme_recreation", ticket.id)
                    }
                }
            }

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
    private fun detachAllOverlays(activity: Activity?, ownerId: Long = currentTransitionId) {
        if (activity == null) return
        try {
            val decorView = activity.window?.decorView as? ViewGroup ?: return

            overlayOwners.entries.toList().filter { it.value == ownerId && it.key.parent === decorView }.forEach { (overlay, _) ->
                (overlay as? ImageView)?.setImageDrawable(null)
                decorView.removeView(overlay)
                overlayOwners.remove(overlay)
            }
            if (currentAnimatorActivityRef?.get() === activity && animatorTransitionId == ownerId) {
                currentAnimator?.let { anim ->
                    currentAnimator = null
                    anim.removeAllListeners()
                    anim.cancel()
                }
                currentAnimatorActivityRef = null
            }

            // 4. Clear window touch-blocking flag if installed by transition
            if (touchLockInstalled && ownerId == currentTransitionId) {
                activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
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
        overlayOwners[overlay] = currentTransitionId
        decorView.addView(overlay)
        overlay.bringToFront()
    }

    private fun waitForNewThemeRenderAndReveal(activity: Activity, holder: SnapshotHolder) {
        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanupPending("waitForNewThemeRenderAndReveal_no_decor")
            return
        }

        decorView.findViewWithTag<View>("pre_reveal_overlay")?.bringToFront()

        val expectedTransitionId = holder.transitionId
        decorView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (decorView.viewTreeObserver.isAlive) {
                    decorView.viewTreeObserver.removeOnPreDrawListener(this)
                }
                val prepare = Runnable {
                    if (pendingSnapshot?.transitionId != expectedTransitionId ||
                        currentTransitionId != expectedTransitionId
                    ) {
                        logDebug("Stale preDraw callback detected (ID mismatch) -> ignoring without calling global cleanup")
                        detachAllOverlays(activity, expectedTransitionId)
                        return@Runnable
                    }
                    if (holder.bitmap.isRecycled || activity.isFinishing || activity.isDestroyed) {
                        logDebug("Aborting reveal: bitmap recycled or activity finishing")
                        detachAllOverlays(activity)
                        cleanupPending("preDraw_cancelled_or_recycled")
                        return@Runnable
                    }
                    if (activity.resources.configuration.orientation != holder.orientation ||
                        decorView.width != holder.capturedWidth ||
                        decorView.height != holder.capturedHeight
                    ) {
                        logDebug("Aborting reveal: geometry or orientation changed during recreate")
                        detachAllOverlays(activity)
                        cleanupPending("preDraw_geometry_mismatch")
                        return@Runnable
                    }
                    logDebug("New theme preDraw confirmed -> Launching circular reveal animation")
                    triggerCircularReveal(activity, holder)
                }
                val interceptor = preparationInterceptorForTest
                if (interceptor != null) interceptor(prepare) else prepare.run()
                return true
            }
        })
        decorView.invalidate()
    }

    fun cleanupPending(reason: String = "unknown") {
        trace("cleanup:$reason", (targetActivityRef?.get() ?: sourceActivityRef?.get())?.window?.decorView)
        logDebug("cleanupPending triggered (reason: $reason)")
        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.removeCallbacks(handoffTimeout)
        popupHandoff = null

        // Invalidate and retire transition ID to reject late callbacks
        val retiringId = currentTransitionId
        ++currentTransitionId

        currentAnimator?.let { anim ->
            currentAnimator = null
            anim.removeAllListeners()
            anim.cancel()
        }
        currentAnimatorActivityRef = null

        // Step 1: Detach and clear all overlays from both source and target activities before bitmap recycle
        val source = sourceActivityRef?.get()
        val target = targetActivityRef?.get()
        detachAllOverlays(source, retiringId)
        detachAllOverlays(target, retiringId)
        sourceActivityRef = null
        targetActivityRef = null

        currentThemeSwitchOverlay = null

        // Step 2: Now that all views referencing the bitmap have been removed from the
        // view hierarchy and their ImageDrawables cleared, it is safe to recycle the bitmap.
        val bmp = pendingSnapshot?.bitmap
        pendingSnapshot = null
        transitioning = false
        themeApplicationStarted = false
        activeTransitionTargetDark = null

        if (Build.VERSION.SDK_INT >= 34) {
            source?.clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN)
            source?.clearOverrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE)
        }
        val finalActivity = target ?: source
        if (finalActivity != null) {
            if (touchLockInstalled) {
                finalActivity.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                touchLockInstalled = false
            }
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
     * Standard Android SDK only, 400ms duration, TelegramThemeEasing.
     */
    fun toggleWithCircularReveal(
        activity: Activity,
        originX: Int,
        originY: Int,
        isDarkModeTarget: Boolean = !isDarkMode(activity),
        applyAction: Runnable? = null
    ) {
        // 1. REJECT if transition is active FIRST.
        // No same-target check, no preference mutations, no delegate calls, no callback execution.
        if (isTransitioning) {
            logDebug("Rapid tap blocked: transition already in progress")
            return
        }

        // 2. SAME-TARGET CHECK: Check if Activity and delegate are ALREADY settled in target theme
        val targetDelegateMode = if (isDarkModeTarget) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        val currentDelegateMode = AppCompatDelegate.getDefaultNightMode()
        val currentUiDark = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedPref = if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null

        // Settled ONLY when the Activity uiMode matches target, the delegate matches target,
        // and any saved preference matches (or is null).
        // If the system uiMode happens to match due to an external command (e.g. cmd uimode),
        // but the app delegate is still in the opposing mode, the app is NOT settled in target.
        val isAlreadySettled = currentUiDark == isDarkModeTarget &&
                currentDelegateMode == targetDelegateMode &&
                (savedPref == null || savedPref == isDarkModeTarget)

        if (isAlreadySettled) {
            logDebug("Same-target toggle requested: Activity already settled in target theme ($isDarkModeTarget)")
            if (savedPref != isDarkModeTarget) {
                prefs.edit().putBoolean(KEY_DARK_MODE, isDarkModeTarget).apply()
            }
            if (applyAction != null) {
                applyAction.run()
            }
            return
        }

        if (!areAnimationsEnabled(activity)) {
            logDebug("Animations disabled in accessibility -> instant toggle")
            applyForRecreation(activity, isDarkModeTarget, applyAction)
            return
        }

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            applyForRecreation(activity, isDarkModeTarget, applyAction)
            return
        }

        val width = decorView.width
        val height = decorView.height
        if (width <= 0 || height <= 0) {
            applyForRecreation(activity, isDarkModeTarget, applyAction)
            return
        }

        // Lock transitions IMMEDIATELY so rapid taps cannot start concurrent captures
        transitioning = true
        activeTransitionTargetDark = isDarkModeTarget
        themeApplicationStarted = false
        sourceActivityRef = WeakReference(activity)
        val transitionId = ++currentTransitionId

        logDebug("Theme toggle initiated at ($originX, $originY) [transitionId: $transitionId, targetDark: $isDarkModeTarget]. Capturing bitmap...")
        ensureLifecycleRegistered(activity.application)

        trace("capture_requested", decorView, transitionId)
        // Capture static snapshot: PixelCopy on API 26+ with synchronous Canvas fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var bitmap: Bitmap? = null
            var timeoutRunnable: Runnable? = null
            try {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val window = activity.window
                var pixelCopyDone = false
                var pixelCopyTimedOut = false
                val scheduledTimeout = Runnable {
                    if (!pixelCopyDone && currentTransitionId == transitionId) {
                        pixelCopyTimedOut = true
                        logDebug("PixelCopy timed out -> fallback to Canvas draw without premature recycling")
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
                        if (!pixelCopyTimedOut && !pixelCopyDone && currentTransitionId == transitionId) {
                            pixelCopyDone = true
                            if (activity.isFinishing || activity.isDestroyed) {
                                logDebug("Activity finishing during PixelCopy completion -> aborting")
                                try { bitmap.recycle() } catch (_: Throwable) {}
                                cleanupPending("activity_finishing_during_capture")
                                return@request
                            }
                            if (copyResult == PixelCopy.SUCCESS) {
                                logDebug("PixelCopy capture SUCCESS")
                                commitRevealTransition(transitionId, activity, bitmap, originX.toFloat(), originY.toFloat(), isDarkModeTarget, applyAction)
                            } else {
                                logDebug("PixelCopy failed with code $copyResult -> fallback to Canvas draw")
                                try { bitmap.recycle() } catch (_: Throwable) {}
                                fallbackSynchronousCaptureAndToggle(transitionId, activity, decorView, originX.toFloat(), originY.toFloat(), isDarkModeTarget, applyAction)
                            }
                        } else {
                            // Native write completed after timeout or cancellation; now safe to recycle bitmap
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

        if (activity.isFinishing || activity.isDestroyed) {
            logDebug("Activity finishing or destroyed during fallback capture -> cleaning up transition")
            cleanupPending("fallback_activity_finishing")
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
            logDebug("Fallback capture produced null bitmap -> instant toggle fallback")
            cleanupPending("fallback_null_bitmap")
            applyForRecreation(activity, isDarkModeTarget, applyAction)
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
        val commitAction = Runnable {
            commitRevealTransitionInternal(
                transitionId = transitionId,
                activity = activity,
                bitmap = bitmap,
                originX = originX,
                originY = originY,
                isDarkModeTarget = isDarkModeTarget,
                applyAction = applyAction
            )
        }
        val interceptor = captureInterceptorForTest
        if (interceptor != null) {
            interceptor.invoke(commitAction)
        } else {
            commitAction.run()
        }
    }

    private fun commitRevealTransitionInternal(
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

        if (activity.isFinishing || activity.isDestroyed) {
            logDebug("Activity finishing or destroyed during commitRevealTransition -> cleaning up")
            if (!bitmap.isRecycled) {
                try { bitmap.recycle() } catch (_: Throwable) {}
            }
            cleanupPending("commit_activity_finishing")
            return
        }

        val orientation = activity.resources.configuration.orientation
        val width = bitmap.width
        val height = bitmap.height

        sourceActivityRef = WeakReference(activity)
        pendingSnapshot = SnapshotHolder(
            transitionId = transitionId,
            bitmap = bitmap,
            originX = originX,
            originY = originY,
            isDarkModeTarget = isDarkModeTarget,
            oldActivityId = System.identityHashCode(activity),
            activityClassName = activity.javaClass.name,
            captureTimestamp = SystemClock.uptimeMillis(),
            capturedWidth = width,
            capturedHeight = height,
            orientation = orientation
        )

        trace("capture_completed", activity.window.decorView, transitionId)
        // Keep the source protected through an actual submitted frame before recreation.
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
            overlayOwners[preOverlay] = transitionId
            decorView.addView(preOverlay)
        }

        mainHandler.postDelayed(timeoutRunnable, 2000L)
        val sourceDecor = decorView ?: run { cleanupPending("source_no_decor"); return }
        ThemeFrameCoordinator.afterFrame(sourceDecor, ready = {
            sourceDecor.findViewWithTag<View>("pre_reveal_overlay")?.let {
                it.width == width && it.height == height && it.isAttachedToWindow
            } == true
        }, valid = { currentTransitionId == transitionId }) {
            if (currentTransitionId != transitionId) return@afterFrame
            if (activity.isDestroyed || activity.isFinishing || !activity.hasWindowFocus() ||
                sourceDecor.width != width || sourceDecor.height != height ||
                activity.resources.configuration.orientation != orientation) {
                cleanupPending("source_not_visible"); return@afterFrame
            }
            trace("source_cover_ready", sourceDecor, transitionId)
            activity.overridePendingTransition(0, 0)
            if (Build.VERSION.SDK_INT >= 34) {
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
            }
            trace("theme_application", sourceDecor, transitionId)
            applyForRecreation(activity, isDarkModeTarget, applyAction)
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
        touchLockInstalled = true

        val targetBg = ContextCompat.getColor(activity, R.color.eve_bg)
        val origStatusBarColor = activity.window.statusBarColor
        val origNavBarColor = activity.window.navigationBarColor

        // Make window background and system bars transparent so the content root's background
        // spans behind the status and navigation bar areas throughout the circular reveal
        activity.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(targetBg))
        activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
        activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val contentRoot: View = decorView.findViewById<View>(androidx.appcompat.R.id.action_bar_root)
            ?: activity.findViewById<View>(android.R.id.content) ?: run {
                cleanupPending("missing_content_root"); return
            }
        contentRoot.setBackgroundColor(targetBg)

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
        overlayOwners[themeSwitchImageView] = holder.transitionId
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
        // DAY -> NIGHT: live contentRoot is animated from 0 to finalRadius (revealing dark status, content, and nav bar)
        // NIGHT -> DAY: old screenshot (themeSwitchImageView) is animated from finalRadius to 0 (shrinking over light content)
        val targetView: View = if (toDark) {
            contentRoot
        } else {
            themeSwitchImageView
        }

        val protection = decorView.findViewWithTag<View>("pre_reveal_overlay") ?: run {
            cleanupPending("missing_target_cover"); return
        }
        protection.bringToFront()
        val observer = decorView.viewTreeObserver
        observer.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (currentTransitionId != holder.transitionId) {
                    if (observer.isAlive) observer.removeOnPreDrawListener(this)
                    detachAllOverlays(activity, holder.transitionId)
                    return true
                }
                if (activity.isFinishing || activity.isDestroyed || holder.bitmap.isRecycled ||
                    activity.resources.configuration.orientation != holder.orientation ||
                    decorView.width != holder.capturedWidth || decorView.height != holder.capturedHeight) {
                    if (observer.isAlive) observer.removeOnPreDrawListener(this)
                    cleanupPending("prepared_geometry_or_lifecycle_changed")
                    return true
                }
                if (!protection.isLaidOut || protection.width != holder.capturedWidth ||
                    protection.height != holder.capturedHeight || protection.left != 0 || protection.top != 0 ||
                    !themeSwitchImageView.isLaidOut || themeSwitchImageView.width != holder.capturedWidth ||
                    themeSwitchImageView.height != holder.capturedHeight ||
                    themeSwitchImageView.left != 0 || themeSwitchImageView.top != 0 || !targetView.isLaidOut ||
                    targetView.width <= 0 || targetView.height <= 0) return true
                if (observer.isAlive) observer.removeOnPreDrawListener(this)
                trace("target_layers_ready", themeSwitchImageView, holder.transitionId)
                startPreparedReveal(activity, holder, decorView, targetView, themeSwitchImageView,
                    protection, origStatusBarColor, origNavBarColor)
                return true
            }
        })
        decorView.requestLayout()
    }

    private fun startPreparedReveal(activity: Activity, holder: SnapshotHolder, decorView: ViewGroup,
        targetView: View, themeSwitchImageView: ImageView, protection: View,
        origStatusBarColor: Int, origNavBarColor: Int) {
        val toDark = holder.isDarkModeTarget
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
            activity.window.statusBarColor = origStatusBarColor
            activity.window.navigationBarColor = origNavBarColor
            cleanupPending("reveal_creation_error")
            return
        }

        anim.duration = 400L
        anim.interpolator = TelegramThemeEasing

        var completed = false
        val finishAction = Runnable {
            if (!completed && currentTransitionId == holder.transitionId) {
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
                currentAnimatorActivityRef = null
                activity.window.statusBarColor = origStatusBarColor
                activity.window.navigationBarColor = origNavBarColor
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
        currentAnimatorActivityRef = WeakReference(activity)
        animatorTransitionId = holder.transitionId
        trace("native_reveal_start", targetView, holder.transitionId)
        anim.start()
        // The protection stays above both layers until a hardware frame containing the
        // native reveal has been submitted. start() alone is not a render fence.
        ThemeFrameCoordinator.afterFrame(decorView, valid = { currentTransitionId == holder.transitionId }) {
            if (currentTransitionId != holder.transitionId || currentAnimator !== anim) return@afterFrame
            trace("protection_cover_removal", protection, holder.transitionId)
            (protection as? ImageView)?.setImageDrawable(null)
            decorView.removeView(protection)
            overlayOwners.remove(protection)
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

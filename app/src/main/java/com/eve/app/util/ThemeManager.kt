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
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.R

/**
 * ThemeManager: Controls Light <-> Dark theme switching with Telegram-style circular reveal.
 * Captures a full-window snapshot of the current theme before recreation, then seamlessly
 * expands a circular mask centered on the toggle button across the recreated Activity
 * to reveal the new theme underneath with zero flicker or flash.
 */
object ThemeManager {

    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private data class SnapshotHolder(
        val bitmap: Bitmap,
        val originX: Int,
        val originY: Int,
        val oldActivityId: Int,
        val activityClassName: String
    )

    private var pendingSnapshot: SnapshotHolder? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var activeOverlay: CircularRevealOverlayView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { cleanupPending() }

    val isTransitioning: Boolean
        get() = transitioning

    /** App start hote hi (Application.onCreate me) call karo, kisi Activity dikhne se pehle. */
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

    fun isNight(context: Context): Boolean = isDarkMode(context)

    /**
     * Standard theme toggle without animation.
     */
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
                    activity.overridePendingTransition(0, 0)
                    attachStaticOverlayImmediately(activity, holder.bitmap)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                val holder = pendingSnapshot ?: return
                if (activity.javaClass.name == holder.activityClassName &&
                    System.identityHashCode(activity) != holder.oldActivityId
                ) {
                    activity.overridePendingTransition(0, 0)
                    triggerCircularReveal(activity, holder)
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                val holder = pendingSnapshot ?: return
                // Critical root-cause fix: Do NOT clean up when the OLD activity is destroyed during recreation!
                if (System.identityHashCode(activity) == holder.oldActivityId) {
                    return
                }
                if (activity.javaClass.name == holder.activityClassName) {
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
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        decorView.addView(overlay)
    }

    private fun cleanupPending() {
        mainHandler.removeCallbacks(timeoutRunnable)
        pendingSnapshot?.bitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        pendingSnapshot = null
        transitioning = false
    }

    /**
     * Triggers theme toggle with a full-screen Telegram-style circular reveal expanding
     * from the specified on-screen coordinates (originX, originY).
     */
    fun toggleWithCircularReveal(activity: Activity, originX: Int, originY: Int) {
        if (transitioning) return

        if (!areAnimationsEnabled(activity)) {
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

        ensureLifecycleRegistered(activity.application)

        // Capture static snapshot: Use PixelCopy on API 26+ if possible, else synchronous Canvas draw
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
                            commitRevealTransition(activity, bitmap, originX, originY)
                        } else {
                            fallbackSynchronousCaptureAndToggle(activity, decorView, originX, originY)
                        }
                    },
                    mainHandler
                )
                return
            } catch (_: Throwable) {
                // Fallback to synchronous Canvas draw
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
            bmp
        } catch (_: Throwable) {
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
            activityClassName = activity.javaClass.name
        )

        // Add pre-overlay on old screen to prevent any flash before activity recreation begins
        val decorView = activity.window.decorView as? ViewGroup
        if (decorView != null && !bitmap.isRecycled) {
            val preOverlay = ImageView(activity).apply {
                setImageBitmap(bitmap)
                scaleType = ImageView.ScaleType.FIT_XY
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            decorView.addView(preOverlay)
        }

        activity.overridePendingTransition(0, 0)

        // Safety timeout to avoid leak if recreation is cancelled or delayed
        mainHandler.postDelayed(timeoutRunnable, 3000)

        // Commit and apply new theme
        toggle(activity)
    }

    /**
     * Helper to trigger circular reveal from an anchor view.
     */
    fun toggleWithCircularReveal(anchorView: View) {
        if (transitioning) return
        val loc = IntArray(2)
        anchorView.getLocationInWindow(loc)
        val cx = loc[0] + anchorView.width / 2
        val cy = loc[1] + anchorView.height / 2
        val activity = findActivity(anchorView.context) ?: return
        toggleWithCircularReveal(activity, cx, cy)
    }

    /** Backward compatibility alias */
    fun toggleWithReveal(context: Context, anchorView: View) {
        toggleWithCircularReveal(anchorView)
    }

    private fun triggerCircularReveal(activity: Activity, holder: SnapshotHolder) {
        val bitmap = holder.bitmap
        val cx = holder.originX
        val cy = holder.originY

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanupPending()
            return
        }

        // Remove the temporary pre-overlay if attached during onCreate
        val preOverlay = decorView.findViewWithTag<View>("pre_reveal_overlay")
        if (preOverlay != null) {
            decorView.removeView(preOverlay)
        }

        // Disable touches while reveal animation is running
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        val overlayView = CircularRevealOverlayView(activity, bitmap, cx, cy) {
            decorView.removeView(activeOverlay)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            activeOverlay = null
            cleanupPending()
        }
        activeOverlay = overlayView
        decorView.addView(overlayView)

        decorView.post {
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

            // Synced icon cross-fade with rotate + scale
            button.animate()
                .rotationBy(if (isDark) 90f else -90f)
                .scaleX(0.7f)
                .scaleY(0.7f)
                .alpha(0.5f)
                .setDuration(330)
                .withEndAction {
                    button.setImageResource(if (isDark) R.drawable.ic_sun else R.drawable.ic_moon)
                    button.animate()
                        .rotation(0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(350)
                        .start()
                }
                .start()

            toggleWithCircularReveal(button)
        }
    }

    /**
     * Overlay view that displays the old theme snapshot and cuts a smooth circular hole
     * expanding from (cx, cy) to reveal the new theme directly underneath.
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

        init {
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

            // Dynamically calculate distance to the farthest of the 4 screen corners
            val maxRadius = Math.hypot(
                Math.max(cx.toDouble(), (w - cx).toDouble()),
                Math.max(cy.toDouble(), (h - cy).toDouble())
            ).toFloat().coerceAtLeast(1f)

            animator = ValueAnimator.ofFloat(0f, maxRadius).apply {
                duration = 680
                interpolator = FastOutSlowInInterpolator()
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
                // Circle has not expanded yet; draw full old snapshot
                canvas.drawBitmap(bitmap, 0f, 0f, null)
            } else {
                // Cut expanding circular hole using EVEN_ODD fill
                clipPath.reset()
                clipPath.fillType = Path.FillType.EVEN_ODD
                clipPath.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
                clipPath.addCircle(cx.toFloat(), cy.toFloat(), currentRadius, Path.Direction.CW)

                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                canvas.restore()
            }
        }

        fun cancelAnimation() {
            animator?.cancel()
            animator = null
        }
    }
}

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
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.eve.app.R

/**
 * ThemeManager: Controls Light <-> Dark theme switching with Telegram-style circular reveal.
 * Choice SharedPreferences me save hoti hai isliye app dobara khulne par bhi wahi mode yaad rehta hai.
 */
object ThemeManager {

    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private var pendingBitmap: Bitmap? = null
    private var pendingOriginX: Int = 0
    private var pendingOriginY: Int = 0
    private var pendingActivityClass: Class<*>? = null
    private var isLifecycleRegistered = false
    private var transitioning = false
    private var activeOverlay: CircularRevealOverlayView? = null

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
                if (activity.javaClass == pendingActivityClass && pendingBitmap != null) {
                    activity.overridePendingTransition(0, 0)
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (activity.javaClass == pendingActivityClass && pendingBitmap != null) {
                    activity.overridePendingTransition(0, 0)
                    triggerCircularReveal(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity.javaClass == pendingActivityClass) {
                    activeOverlay?.cancelAnimation()
                    (activity.window.decorView as? ViewGroup)?.removeView(activeOverlay)
                    activeOverlay = null
                    cleanupPending()
                }
            }
        })
    }

    private fun cleanupPending() {
        pendingBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        pendingBitmap = null
        pendingActivityClass = null
        transitioning = false
    }

    /**
     * Triggers theme toggle with a full-screen Telegram-style circular reveal expanding
     * from the specified on-screen coordinates (originX, originY).
     */
    fun toggleWithCircularReveal(activity: Activity, originX: Int, originY: Int) {
        if (transitioning) return

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            toggle(activity)
            return
        }

        if (decorView.width <= 0 || decorView.height <= 0) {
            toggle(activity)
            return
        }

        ensureLifecycleRegistered(activity.application)

        // Capture static snapshot of the entire screen in the current theme
        val bitmap = try {
            val bmp = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            decorView.draw(canvas)
            bmp
        } catch (e: Throwable) {
            null
        }

        if (bitmap == null) {
            toggle(activity)
            return
        }

        transitioning = true
        pendingBitmap = bitmap
        pendingOriginX = originX
        pendingOriginY = originY
        pendingActivityClass = activity.javaClass

        // Add static overlay on the current screen to eliminate any flash before recreation
        val preOverlay = ImageView(activity).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        decorView.addView(preOverlay)

        // Suppress default activity recreate crossfade
        activity.overridePendingTransition(0, 0)

        // Persist and commit the new theme
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

    private fun triggerCircularReveal(activity: Activity) {
        val bitmap = pendingBitmap ?: return
        val cx = pendingOriginX
        val cy = pendingOriginY

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            cleanupPending()
            return
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

        button.setImageResource(if (isDarkMode(context)) R.drawable.ic_moon else R.drawable.ic_sun)
        button.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.eve_text))
        button.contentDescription = context.getString(
            if (isDarkMode(context)) R.string.theme_toggle_to_light else R.string.theme_toggle_to_dark
        )
        button.setOnClickListener {
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
                duration = 400
                interpolator = DecelerateInterpolator()
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
                // Cut expanding hole using EVEN_ODD fill
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

package com.eve.app.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.eve.app.R

/**
 * Ek single toggle: Light <-> Dark. Sun icon = "abhi light hai, dark karne ke liye dabao",
 * Moon icon = "abhi dark hai, light karne ke liye dabao". Choice SharedPreferences me save
 * hoti hai isliye app dobara khulne par bhi wahi mode yaad rehta hai. Pehli baar (koi saved
 * choice nahi) system ka dark/light setting follow hoti hai.
 *
 * FEATURE 1: Telegram-style circular theme reveal animation across AppCompatDelegate.setDefaultNightMode()
 * Activity recreation.
 */
object ThemeManager {

    private const val PREFS = "eve_prefs"
    private const val KEY_DARK_MODE = "key_dark_mode"

    private var pendingBitmap: Bitmap? = null
    private var pendingOriginX: Int = 0
    private var pendingOriginY: Int = 0
    private var pendingActivityClass: Class<*>? = null
    private var isLifecycleRegistered = false
    private var isTransitioning = false

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
     * Mode flip karta hai aur save karta hai. AppCompatDelegate.setDefaultNightMode()
     * chalte hi saari running AppCompatActivity apne aap recreate ho jaati hain, isliye
     * icon/colors sab jagah turant update ho jaate hain — manual recreate() ki zaroorat nahi.
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

    private fun ensureLifecycleRegistered(app: Application) {
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
                    triggerRevealAnimation(activity)
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity.javaClass == pendingActivityClass) {
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
        isTransitioning = false
    }

    private fun triggerRevealAnimation(activity: Activity) {
        val bitmap = pendingBitmap ?: return
        val cx = pendingOriginX
        val cy = pendingOriginY
        // Consume state so it only triggers once
        pendingBitmap = null
        pendingActivityClass = null

        val decorView = activity.window.decorView as? ViewGroup ?: run {
            if (!bitmap.isRecycled) bitmap.recycle()
            isTransitioning = false
            return
        }

        // Ignore taps during animation
        activity.window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        // Add overlay behind the new content so circular reveal displays new theme expanding outwards
        val overlay = ImageView(activity).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val rootContent = decorView.findViewById<View>(android.R.id.content) ?: decorView.getChildAt(0) ?: decorView
        decorView.addView(overlay, 0)
        rootContent.bringToFront()

        decorView.post {
            if (activity.isFinishing || activity.isDestroyed) {
                decorView.removeView(overlay)
                if (!bitmap.isRecycled) bitmap.recycle()
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                isTransitioning = false
                return@post
            }

            val w = decorView.width.toFloat()
            val h = decorView.height.toFloat()
            val maxRadius = Math.hypot(
                Math.max(cx.toFloat(), w - cx.toFloat()).toDouble(),
                Math.max(cy.toFloat(), h - cy.toFloat()).toDouble()
            ).toFloat().coerceAtLeast(1f)

            try {
                // Reveal circle starts at toggle point in both directions (light->dark & dark->light)
                val anim = ViewAnimationUtils.createCircularReveal(rootContent, cx, cy, 0f, maxRadius)
                anim.duration = 400
                anim.interpolator = AccelerateDecelerateInterpolator()
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        decorView.removeView(overlay)
                        if (!bitmap.isRecycled) bitmap.recycle()
                        activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        isTransitioning = false
                    }
                })
                anim.start()
            } catch (e: Exception) {
                decorView.removeView(overlay)
                if (!bitmap.isRecycled) bitmap.recycle()
                activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                isTransitioning = false
            }
        }
    }

    /**
     * Ek chhota icon-only toggle button ko current mode ke hisaab se sun/moon icon set
     * karta hai aur click par mode switch karta hai. Telegram-style circular theme reveal
     * transition centrally yahin implement kiya gaya hai taaki har screen ko automatically
     * smooth circular reveal transition mil jaye.
     */
    fun setupToggleButton(context: Context, button: ImageButton) {
        val activity = findActivity(button.context) ?: findActivity(context)
        activity?.application?.let { ensureLifecycleRegistered(it) }

        button.setImageResource(if (isDarkMode(context)) R.drawable.ic_moon else R.drawable.ic_sun)
        button.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.eve_text))
        button.contentDescription = context.getString(
            if (isDarkMode(context)) R.string.theme_toggle_to_light else R.string.theme_toggle_to_dark
        )
        button.setOnClickListener {
            // Animate sun/moon rotation and scale
            button.animate()
                .rotationBy(360f)
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(180)
                .withEndAction {
                    button.scaleX = 1f
                    button.scaleY = 1f
                    toggleWithReveal(context, button)
                }
                .start()
        }
    }

    /**
     * Triggers theme toggle with a circular reveal expanding/collapsing from the specified
     * anchor View (e.g. toggle button or three-dot menu button).
     */
    fun toggleWithReveal(context: Context, anchorView: View) {
        if (isTransitioning) return

        val currentActivity = findActivity(anchorView.context) ?: findActivity(context)
        if (currentActivity == null) {
            toggle(context)
            return
        }

        val decorView = currentActivity.window.decorView as? ViewGroup
        if (decorView == null || decorView.width <= 0 || decorView.height <= 0) {
            toggle(context)
            return
        }

        // Capture exact anchor center relative to decorView
        val anchorLoc = IntArray(2)
        anchorView.getLocationInWindow(anchorLoc)
        val decorLoc = IntArray(2)
        decorView.getLocationInWindow(decorLoc)
        val cx = (anchorLoc[0] - decorLoc[0]) + anchorView.width / 2
        val cy = (anchorLoc[1] - decorLoc[1]) + anchorView.height / 2

        // Capture current screen as Bitmap screenshot
        val bitmap = try {
            val bmp = Bitmap.createBitmap(decorView.width, decorView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            decorView.draw(canvas)
            bmp
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }

        if (bitmap == null) {
            toggle(context)
            return
        }

        isTransitioning = true
        pendingBitmap = bitmap
        pendingOriginX = cx
        pendingOriginY = cy
        pendingActivityClass = currentActivity.javaClass

        // Add overlay to current screen immediately so no jump/glitch occurs before recreate
        val oldOverlay = ImageView(currentActivity).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        decorView.addView(oldOverlay)

        // Suppress default activity recreate crossfade
        currentActivity.overridePendingTransition(0, 0)

        // Call existing toggle() function
        toggle(context)
    }
}

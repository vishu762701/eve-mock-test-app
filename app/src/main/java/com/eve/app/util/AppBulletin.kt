package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.eve.app.R

/**
 * Telegram-style Bulletin Bar component (matching Bulletin.java).
 * Slides up from the bottom with spring/overshoot easing, stays for a configured
 * duration, then smoothly slides down and fades out.
 */
class AppBulletin private constructor(
    private val activity: Activity,
    private val message: CharSequence,
    private val durationMs: Long = DURATION_SHORT,
    private val iconRes: Int = R.drawable.ic_check_circle,
    private val actionText: String? = null,
    private val onAction: (() -> Unit)? = null
) {

    companion object {
        const val DURATION_SHORT = 1500L
        const val DURATION_LONG = 2750L
        const val DURATION_PROLONG = 5000L

        private val mainHandler = Handler(Looper.getMainLooper())
        private var activeBulletin: AppBulletin? = null

        fun findActivity(context: Context?): Activity? {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx
                ctx = ctx.baseContext
            }
            return null
        }

        fun make(
            activity: Activity,
            message: CharSequence,
            durationMs: Long = DURATION_SHORT,
            iconRes: Int = R.drawable.ic_check_circle,
            actionText: String? = null,
            onAction: (() -> Unit)? = null
        ): AppBulletin {
            return AppBulletin(activity, message, durationMs, iconRes, actionText, onAction)
        }

        /**
         * Replaces Toast across the app. Displays a Telegram-style bulletin bar
         * if an Activity is available, or gracefully falls back to Toast.
         */
        fun show(
            context: Context,
            message: CharSequence,
            durationMs: Long = if (message.length > 35) DURATION_LONG else DURATION_SHORT,
            iconRes: Int = R.drawable.ic_check_circle,
            actionText: String? = null,
            onAction: (() -> Unit)? = null
        ) {
            val act = findActivity(context)
            if (act != null && !act.isFinishing && !act.isDestroyed) {
                make(act, message, durationMs, iconRes, actionText, onAction).show()
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        fun showSuccess(context: Context, message: CharSequence) {
            show(context, message, DURATION_SHORT, R.drawable.ic_check_circle)
        }

        fun showError(context: Context, message: CharSequence) {
            show(context, message, DURATION_LONG, R.drawable.ic_info)
        }
    }

    private var bulletinView: View? = null
    private val dismissRunnable = Runnable { dismiss() }

    fun show() {
        if (activity.isFinishing || activity.isDestroyed) return

        // Dismiss existing active bulletin immediately so they don't stack awkwardly
        activeBulletin?.dismissInternal(immediate = true)
        activeBulletin = this

        val decorView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.layout_bulletin, decorView, false)
        bulletinView = view

        val tvMessage = view.findViewById<TextView>(R.id.tvBulletinMessage)
        val ivIcon = view.findViewById<ImageView>(R.id.ivBulletinIcon)
        val btnAction = view.findViewById<TextView>(R.id.btnBulletinAction)
        val card = view.findViewById<View>(R.id.cardBulletin)

        tvMessage.text = message
        ivIcon.setImageResource(iconRes)

        if (!actionText.isNullOrBlank() && onAction != null) {
            btnAction.visibility = View.VISIBLE
            btnAction.text = actionText
            btnAction.setOnClickListener {
                dismiss()
                onAction.invoke()
            }
        } else {
            btnAction.visibility = View.GONE
        }

        decorView.addView(view)

        // Slide up with spring/overshoot easing
        val targetTranslationY = 0f
        val startOffset = 120 * activity.resources.displayMetrics.density
        card.translationY = startOffset
        card.alpha = 0f

        card.animate()
            .translationY(targetTranslationY)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()

        mainHandler.postDelayed(dismissRunnable, durationMs)
    }

    fun dismiss() {
        dismissInternal(immediate = false)
    }

    private fun dismissInternal(immediate: Boolean) {
        mainHandler.removeCallbacks(dismissRunnable)
        val view = bulletinView ?: return
        val card = view.findViewById<View>(R.id.cardBulletin)

        if (activeBulletin === this) {
            activeBulletin = null
        }

        if (immediate || card == null) {
            (view.parent as? ViewGroup)?.removeView(view)
            bulletinView = null
            return
        }

        val exitOffset = 100 * activity.resources.displayMetrics.density
        card.animate()
            .translationY(exitOffset)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                (view.parent as? ViewGroup)?.removeView(view)
                bulletinView = null
            }
            .start()
    }
}

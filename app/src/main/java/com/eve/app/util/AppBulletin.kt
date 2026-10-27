package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.designsystem.DesignMotion
import com.eve.app.designsystem.UiArcMotion
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * UIArc Toast Stack & Bulletin System.
 * Genuine native translation of official UIArc Toast Stack specification:
 * - Source: https://uiarc.dev/r/toast-stack.json
 * - Implicit message duration: 1500ms below 20 characters, otherwise 2750ms
 * - Errors retain the previous 8000ms for readability until the host is replaced
 * - Maximum queue: 12; maximum visible stacked: 3
 * - Stacked peek: 14dp; scale step: 0.05 per tucked level
 * - Entering bulletins slide up from bottom edge with smooth spring
 * - Front card dismiss reflows subsequent toasts forward with spring.smooth
 * - Touch down pauses dismissal timer; touch release resumes
 * - Horizontal swipe-to-dismiss gesture support
 * - Full accessibility with TalkBack announcements and reduced-motion support
 */
class AppBulletin private constructor(
    private val activity: Activity,
    private val message: CharSequence,
    private val durationMs: Long = DURATION_DEFAULT,
    private val iconRes: Int = R.drawable.ic_check_circle,
    private val actionText: String? = null,
    private val onAction: (() -> Unit)? = null
) {

    companion object {
        const val DURATION_SHORT = 1500L
        const val DURATION_LONG = 2750L
        const val DURATION_PROLONG = 5000L

        // UIArc Toast Stack authoritative timing and sizing tokens
        const val DURATION_DEFAULT = 5000L
        const val DURATION_ERROR = 8000L // 5000ms * 1.6
        const val MAX_QUEUE = 12
        const val MAX_VISIBLE = 3
        const val PEEK_DP = 14f
        const val SCALE_STEP = 0.05f

        /** Owner-supplied Telegram BulletinFactory duration contract. */
        fun durationForMessage(message: CharSequence): Long =
            if (message.length < 20) DURATION_SHORT else DURATION_LONG

        private val mainHandler = Handler(Looper.getMainLooper())
        private val activityStacks = WeakHashMap<Activity, MutableList<AppBulletin>>()

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
            durationMs: Long = durationForMessage(message),
            iconRes: Int = R.drawable.ic_check_circle,
            actionText: String? = null,
            onAction: (() -> Unit)? = null
        ): AppBulletin {
            return AppBulletin(activity, message, durationMs, iconRes, actionText, onAction)
        }

        /**
         * Replaces Toast across the app. Displays a UIArc Toast Stack bulletin
         * if an Activity is available, or gracefully falls back to Toast.
         */
        fun show(
            context: Context,
            message: CharSequence,
            durationMs: Long = durationForMessage(message),
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
            show(context, message, durationForMessage(message), R.drawable.ic_check_circle)
        }

        fun showError(context: Context, message: CharSequence) {
            show(context, message, DURATION_ERROR, R.drawable.ic_info)
        }
    }

    private var bulletinView: View? = null
    private var isDismissed = false
    private var isPaused = false
    private var remainingDurationMs = durationMs

    private val dismissRunnable = Runnable { dismiss() }

    fun show() {
        if (activity.isFinishing || activity.isDestroyed) return

        val stack = activityStacks.getOrPut(activity) { mutableListOf() }

        // Enforce max queue of 12 items
        while (stack.size >= MAX_QUEUE) {
            val oldest = stack.removeAt(0)
            oldest.dismissInternal(immediate = true)
        }

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

        // Add to active stack (newest is at end of list, representing the front-most visible toast)
        stack.add(this)

        decorView.addView(view)
        card.announceForAccessibility(message)

        // Setup touch to pause timer & swipe-to-dismiss
        setupTouchInteractions(card)

        // Reflow entire stack according to UIArc Toast Stack physics
        reflowStack(activity, animateNew = true)

        startDismissTimer()
    }

    private fun startDismissTimer() {
        mainHandler.removeCallbacks(dismissRunnable)
        if (!isPaused && remainingDurationMs > 0) {
            mainHandler.postDelayed(dismissRunnable, remainingDurationMs)
        }
    }

    private fun pauseDismissTimer() {
        if (!isPaused) {
            isPaused = true
            mainHandler.removeCallbacks(dismissRunnable)
        }
    }

    private fun resumeDismissTimer() {
        if (isPaused) {
            isPaused = false
            startDismissTimer()
        }
    }

    private fun setupTouchInteractions(card: View) {
        var startX = 0f
        var isDragging = false

        card.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    isDragging = false
                    pauseStackTimers(activity)
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - startX
                    if (deltaX > 20f) {
                        isDragging = true
                        v.translationX = deltaX
                        v.alpha = (1f - (deltaX / (v.width * 0.8f))).coerceIn(0.2f, 1f)
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resumeStackTimers(activity)
                    if (isDragging) {
                        val deltaX = event.rawX - startX
                        if (deltaX > v.width * 0.35f) {
                            // Swiped far enough to dismiss
                            dismissWithSwipe(v)
                        } else {
                            // Snap back to 0
                            val motionEnabled = UiArcMotion.isMotionEnabled(activity)
                            v.animate()
                                .translationX(0f)
                                .alpha(1f)
                                .setDuration(if (motionEnabled) UiArcMotion.Duration.FAST else 0L)
                                .setInterpolator(UiArcMotion.Spring.SNAPPY)
                                .start()
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun dismissWithSwipe(card: View) {
        if (isDismissed) return
        isDismissed = true
        mainHandler.removeCallbacks(dismissRunnable)

        val stack = activityStacks[activity]
        stack?.remove(this)

        val motionEnabled = UiArcMotion.isMotionEnabled(activity)
        val exitTarget = card.width.toFloat() + 60f

        card.animate()
            .translationX(exitTarget)
            .alpha(0f)
            .setDuration(if (motionEnabled) UiArcMotion.Duration.EXIT else 0L)
            .setInterpolator(UiArcMotion.Ease.EXIT)
            .withEndAction {
                cleanupView()
                reflowStack(activity, animateNew = false)
            }
            .start()
    }

    fun dismiss() {
        dismissInternal(immediate = false)
    }

    private fun dismissInternal(immediate: Boolean) {
        if (isDismissed) return
        isDismissed = true
        mainHandler.removeCallbacks(dismissRunnable)

        val stack = activityStacks[activity]
        stack?.remove(this)

        val view = bulletinView
        val card = view?.findViewById<View>(R.id.cardBulletin)

        if (immediate || card == null || view == null) {
            cleanupView()
            reflowStack(activity, animateNew = false)
            return
        }

        val motionEnabled = UiArcMotion.isMotionEnabled(activity)
        val exitOffset = activity.resources.getDimension(R.dimen.design_section_gap)

        card.animate()
            .translationY(exitOffset)
            .alpha(0f)
            .setDuration(if (motionEnabled) UiArcMotion.Duration.EXIT else 0L)
            .setInterpolator(UiArcMotion.Ease.EXIT)
            .withEndAction {
                cleanupView()
                reflowStack(activity, animateNew = false)
            }
            .start()
    }

    private fun cleanupView() {
        bulletinView?.let { v ->
            (v.parent as? ViewGroup)?.removeView(v)
        }
        bulletinView = null
    }

    private fun reflowStack(act: Activity, animateNew: Boolean) {
        val stack = activityStacks[act] ?: return
        if (act.isFinishing || act.isDestroyed) return

        val density = act.resources.displayMetrics.density
        val peekPx = PEEK_DP * density
        val motionEnabled = UiArcMotion.isMotionEnabled(act)

        // Stack order: stack.last() is depth 0 (front-most toast)
        val total = stack.size
        for (i in 0 until total) {
            val bulletin = stack[i]
            val depth = (total - 1 - i) // 0 for the newest/front toast, 1 for previous, etc.
            val card = bulletin.bulletinView?.findViewById<View>(R.id.cardBulletin) ?: continue

            if (depth >= MAX_VISIBLE) {
                // Older toasts beyond top 3 are hidden in background
                card.animate().cancel()
                card.visibility = View.GONE
                card.alpha = 0f
            } else {
                card.visibility = View.VISIBLE
                val targetTranslationY = -depth * peekPx
                val targetScale = (1.0f - depth * SCALE_STEP).coerceAtLeast(0.7f)
                val targetAlpha = (1.0f - depth * 0.08f).coerceIn(0.5f, 1.0f)

                if (bulletin === this && animateNew) {
                    // Newly entering toast: slide up from bottom edge
                    val startOffset = act.resources.getDimension(R.dimen.design_section_gap)
                    card.translationY = startOffset
                    card.translationX = 0f
                    card.scaleX = 1f
                    card.scaleY = 1f
                    card.alpha = 0f

                    card.animate()
                        .translationY(targetTranslationY)
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .alpha(targetAlpha)
                        .setDuration(if (motionEnabled) UiArcMotion.Spring.SMOOTH.settlingDurationMs else 0L)
                        .setInterpolator(UiArcMotion.Spring.SMOOTH)
                        .start()
                } else {
                    // Reflowing existing toast in stack
                    card.animate()
                        .translationY(targetTranslationY)
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .alpha(targetAlpha)
                        .setDuration(if (motionEnabled) UiArcMotion.Spring.SMOOTH.settlingDurationMs else 0L)
                        .setInterpolator(UiArcMotion.Spring.SMOOTH)
                        .start()
                }
            }
        }
    }

    private fun pauseStackTimers(act: Activity) {
        val stack = activityStacks[act] ?: return
        for (b in stack) {
            b.pauseDismissTimer()
        }
    }

    private fun resumeStackTimers(act: Activity) {
        val stack = activityStacks[act] ?: return
        for (b in stack) {
            b.resumeDismissTimer()
        }
    }
}

package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.eve.app.R
import com.eve.app.ui.common.CircularCountdownView

/**
 * Telegram-style Undo Bar for delete actions (matching UndoView.java pattern).
 * Displays a depleting circular countdown ring icon, message, and "UNDO" action button.
 * Existing show() callers retain immediate execution; showDeferred() owns an explicit Undo deadline.
 */
class AppUndoBar private constructor(
    private val activity: Activity,
    private val message: CharSequence,
    private val timeLeftMs: Long,
    private val onUndo: () -> Unit,
    private val deferredDelete: (() -> Unit)? = null
) {

    companion object {
        const val TIME_LIGHT = 3000L      // For lighter actions (e.g. removing a bookmark)
        const val TIME_IMPORTANT = 5000L  // For important actions (e.g. deleting an exam, poll, post)

        private var activeUndoBar: AppUndoBar? = null

        fun findActivity(context: Context?): Activity? {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is Activity) return ctx
                ctx = ctx.baseContext
            }
            return null
        }

        fun show(
            context: Context,
            message: CharSequence,
            timeLeftMs: Long = TIME_IMPORTANT,
            onUndo: () -> Unit,
            onExecuteDelete: (() -> Unit)? = null
        ) {
            // Architectural requirement: Deletion executes IMMEDIATELY from data source
            // before the Undo window opens. The 5-second countdown is an undo opportunity, not a deferred deletion delay.
            onExecuteDelete?.invoke()

            val act = findActivity(context) ?: return
            if (act.isFinishing || act.isDestroyed) {
                return
            }

            // Dismiss existing bar without reverting the previous deletion
            activeUndoBar?.resolveReplacement()

            val bar = AppUndoBar(act, message, timeLeftMs, onUndo)
            activeUndoBar = bar
            bar.showInternal()
        }

        /** Explicitly authorized deferred caller; existing show() behavior stays compatible. */
        fun showDeferred(context: Context, message: CharSequence, timeLeftMs: Long = TIME_IMPORTANT,
            onUndo: () -> Unit, onExecuteDelete: () -> Unit) {
            val act = findActivity(context)
            if (act == null || act.isFinishing || act.isDestroyed ||
                act.findViewById<ViewGroup>(android.R.id.content) == null) {
                onUndo()
                return
            }
            activeUndoBar?.resolveReplacement()
            val bar = AppUndoBar(act, message, timeLeftMs, onUndo, onExecuteDelete)
            activeUndoBar = bar
            bar.showInternal()
        }

        fun cancelDeferredForHost(context: Context) {
            val bar = activeUndoBar ?: return
            if (bar.activity === findActivity(context) && bar.deferredDelete != null) {
                bar.undoAndDismiss(immediate = true)
            }
        }
    }

    private var barView: View? = null
    private var isResolved = false
    private val deadlineHandler = Handler(Looper.getMainLooper())
    private val deadlineAction = Runnable {
        if (!isResolved) {
            isResolved = true
            barView?.findViewById<CircularCountdownView>(R.id.countdownView)?.cancel()
            dismissInternal(immediate = false)
            deferredDelete?.invoke()
        }
    }

    private fun undoAndDismiss(immediate: Boolean) {
        if (isResolved) return
        isResolved = true
        deadlineHandler.removeCallbacks(deadlineAction)
        barView?.findViewById<CircularCountdownView>(R.id.countdownView)?.cancel()
        try { onUndo() } finally { dismissInternal(immediate) }
    }

    private fun resolveReplacement() {
        // Replacing an explicit deferred bar cancels it; never commit before its deadline.
        if (deferredDelete != null) undoAndDismiss(immediate = true)
        else {
            isResolved = true
            barView?.findViewById<CircularCountdownView>(R.id.countdownView)?.cancel()
            dismissInternal(immediate = true)
        }
    }

    private fun showInternal() {
        val decorView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        val inflater = LayoutInflater.from(activity)
        val view = inflater.inflate(R.layout.layout_undo_bar, decorView, false)
        barView = view

        val tvMessage = view.findViewById<TextView>(R.id.tvUndoMessage)
        val btnUndo = view.findViewById<TextView>(R.id.btnUndoAction)
        val countdown = view.findViewById<CircularCountdownView>(R.id.countdownView)
        val card = view.findViewById<View>(R.id.cardUndo)

        tvMessage.text = message

        btnUndo.setOnClickListener { undoAndDismiss(immediate = false) }
        if (deferredDelete != null) {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    if (!isResolved) undoAndDismiss(immediate = true)
                }
            })
        }

        decorView.addView(view)

        // Shared token motion; message lifetime and action callbacks stay unchanged.
        val startOffset = activity.resources.getDimension(R.dimen.design_section_gap)
        card.translationY = startOffset
        card.alpha = 0f

        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(if (com.eve.app.designsystem.DesignMotion.enabled(activity)) activity.resources.getInteger(R.integer.design_transition_duration).toLong() else 0L)
            .setInterpolator(com.eve.app.designsystem.DesignMotion.interpolator(activity))
            .start()

        // The deferred mutation deadline is independent of ValueAnimator/system motion scale.
        if (deferredDelete != null) deadlineHandler.postAtTime(deadlineAction, SystemClock.uptimeMillis() + timeLeftMs)
        countdown.startCountdown(timeLeftMs) {
            if (deferredDelete == null && !isResolved) {
                isResolved = true
                dismissInternal(immediate = false)
            }
        }
    }

    private fun dismissInternal(immediate: Boolean) {
        if (activeUndoBar === this) {
            activeUndoBar = null
        }
        val view = barView ?: return
        val card = view.findViewById<View>(R.id.cardUndo)

        if (immediate || card == null) {
            (view.parent as? ViewGroup)?.removeView(view)
            barView = null
            return
        }

        val exitOffset = activity.resources.getDimension(R.dimen.design_section_gap)
        card.animate()
            .translationY(exitOffset)
            .alpha(0f)
            .setDuration(if (com.eve.app.designsystem.DesignMotion.enabled(activity)) activity.resources.getInteger(R.integer.design_transition_duration).toLong() else 0L)
            .setInterpolator(com.eve.app.designsystem.DesignMotion.interpolator(activity))
            .withEndAction {
                (view.parent as? ViewGroup)?.removeView(view)
                barView = null
            }
            .start()
    }
}

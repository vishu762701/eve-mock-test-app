package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import com.eve.app.R
import com.eve.app.ui.common.CircularCountdownView

/**
 * Telegram-style Undo Bar for delete actions (matching UndoView.java pattern).
 * Displays a depleting circular countdown ring icon, message, and "UNDO" action button.
 * The actual delete execution is deferred until the countdown completes without UNDO.
 */
class AppUndoBar private constructor(
    private val activity: Activity,
    private val message: CharSequence,
    private val timeLeftMs: Long,
    private val onUndo: () -> Unit
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
            activeUndoBar?.dismissInternal(immediate = true)

            val bar = AppUndoBar(act, message, timeLeftMs, onUndo)
            activeUndoBar = bar
            bar.showInternal()
        }
    }

    private var barView: View? = null
    private var isResolved = false

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

        btnUndo.setOnClickListener {
            if (isResolved) return@setOnClickListener
            isResolved = true
            countdown.cancel()
            onUndo.invoke()
            dismissInternal(immediate = false)
        }

        decorView.addView(view)

        // Slide up with spring/overshoot easing
        val startOffset = 120 * activity.resources.displayMetrics.density
        card.translationY = startOffset
        card.alpha = 0f

        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()

        // Start smooth depleting countdown; on expiration, simply dismiss since item is already deleted
        countdown.startCountdown(timeLeftMs) {
            if (!isResolved) {
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

        val exitOffset = 100 * activity.resources.displayMetrics.density
        card.animate()
            .translationY(exitOffset)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                (view.parent as? ViewGroup)?.removeView(view)
                barView = null
            }
            .start()
    }
}

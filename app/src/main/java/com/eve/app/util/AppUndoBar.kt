package com.eve.app.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.eve.app.R
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.ui.common.CircularCountdownView
import com.google.android.material.card.MaterialCardView
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Telegram-style Undo Bar for delete actions (matching UndoView.java:258-333).
 * Displays a 10dp-radius dark floating bar (undo_background), depleting circular
 * countdown ring with seconds counter (undo_infoColor), message, and "UNDO" button (undo_cancelColor).
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

        private const val UNDO_BACKGROUND = 0xFF262A2D.toInt() // Theme.key_undo_background
        private const val UNDO_INFO_COLOR = Color.WHITE         // Theme.key_undo_infoColor
        private const val UNDO_CANCEL_COLOR = 0xFF6BB7FF.toInt() // Theme.key_undo_cancelColor

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
            onExecuteDelete?.invoke()

            val act = findActivity(context) ?: return
            if (act.isFinishing || act.isDestroyed) {
                return
            }

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
        val density = activity.resources.displayMetrics.density

        // UndoView.java:258-333: 10dp round rect undo_background, white info text, light blue cancel button
        (card as? MaterialCardView)?.apply {
            setCardBackgroundColor(UNDO_BACKGROUND)
            radius = 10f * density
            strokeWidth = 0
        }
        tvMessage.setTextColor(UNDO_INFO_COLOR)
        btnUndo.setTextColor(UNDO_CANCEL_COLOR)
        (btnUndo as? com.google.android.material.button.MaterialButton)?.apply {
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            rippleColor = ColorStateList.valueOf(UNDO_CANCEL_COLOR and 0x22FFFFFF)
        } ?: run {
            btnUndo.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                2f * density, UNDO_CANCEL_COLOR and 0x22FFFFFF
            )
        }
        countdown.setInfoColor(UNDO_INFO_COLOR)

        tvMessage.text = message

        btnUndo.setOnClickListener { undoAndDismiss(immediate = false) }

        var downX = 0f
        var downY = 0f
        var dragging = false
        val slop = android.view.ViewConfiguration.get(activity).scaledTouchSlop
        card.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    dragging = false
                    false
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    if (!dragging && kotlin.math.abs(dx) > slop && kotlin.math.abs(dx) > kotlin.math.abs(event.rawY - downY)) {
                        dragging = true
                    }
                    if (dragging) {
                        card.translationX = dx
                    }
                    dragging
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - downX
                    if (event.actionMasked == android.view.MotionEvent.ACTION_UP && dragging && kotlin.math.abs(dx) > card.width / 3f) {
                        dismissInternal(immediate = false)
                    } else if (dragging) {
                        card.animate().translationX(0f).setDuration(if (com.eve.app.designsystem.DesignMotion.enabled(activity)) 200L else 0L).start()
                    }
                    val wasDragging = dragging
                    dragging = false
                    wasDragging
                }
                else -> false
            }
        }

        if (deferredDelete != null) {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit
                override fun onViewDetachedFromWindow(v: View) {
                    if (!isResolved) undoAndDismiss(immediate = true)
                }
            })
        }

        decorView.addView(view)

        val startOffset = activity.resources.getDimension(R.dimen.design_section_gap)
        card.translationY = startOffset
        card.alpha = 0f

        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(if (com.eve.app.designsystem.DesignMotion.enabled(activity)) 220L else 0L)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .start()

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
            .setDuration(if (com.eve.app.designsystem.DesignMotion.enabled(activity)) 220L else 0L)
            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
            .withEndAction {
                (view.parent as? ViewGroup)?.removeView(view)
                barView = null
            }
            .start()
    }
}

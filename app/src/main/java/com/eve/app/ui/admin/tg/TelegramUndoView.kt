/*
 * Derived from Telegram Android UndoView.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Official Telegram UndoView banner ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/UndoView.java:120-200
 * - Background: 0xEA272F38 (dark rounded card, radius 10dp)
 * - Info text: 14sp white
 * - Undo button: 14sp bold cyan (0xFF85CAFF)
 * - Auto-dismiss after 5 seconds
 */
class TelegramUndoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val infoTextView = TextView(context)
    private val undoTextView = TextView(context)
    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null

    init {
        visibility = View.GONE
        elevation = dp(6f).toFloat()

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(10f).toFloat()
            setColor(0xEA272F38.toInt())
        }
        background = bg
        minimumHeight = dp(48f)

        infoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        infoTextView.setTextColor(Color.WHITE)
        infoTextView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        val infoLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = dp(16f)
            marginEnd = dp(72f)
        }
        addView(infoTextView, infoLp)

        undoTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        undoTextView.setTextColor(0xFF85CAFF.toInt())
        undoTextView.typeface = android.graphics.Typeface.DEFAULT_BOLD
        undoTextView.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        undoTextView.text = "Undo"
        undoTextView.isClickable = true
        undoTextView.isFocusable = true
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
        undoTextView.setBackgroundResource(outValue.resourceId)
        val undoLp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(16f)
        }
        addView(undoTextView, undoLp)
    }

    fun show(info: CharSequence, onUndo: () -> Unit) {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        infoTextView.text = info

        undoTextView.setOnClickListener {
            hide()
            onUndo()
        }

        visibility = View.VISIBLE
        alpha = 0f
        translationY = dp(16f).toFloat()
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(180L)
            .setListener(null)
            .start()

        val r = Runnable { hide() }
        dismissRunnable = r
        handler.postDelayed(r, 5000L)
    }

    fun hide() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        animate()
            .alpha(0f)
            .translationY(dp(16f).toFloat())
            .setDuration(150L)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
            .start()
    }
}

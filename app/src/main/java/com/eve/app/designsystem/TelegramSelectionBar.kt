/*
 * Derived from Telegram Android ActionBar.java (ActionMode) @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.VibrationHelper
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Authentic Telegram ActionMode selection bar.
 * Upstream: ActionBar.java:700-890
 * Replaces normal toolbar during multi-selection mode with counter animation and action buttons.
 */
class TelegramSelectionBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)
    private val numberTextView: TelegramNumberTextView
    private val btnClose: ImageButton
    private val actionsLayout: LinearLayout

    var onCloseClicked: (() -> Unit)? = null

    init {
        visibility = View.GONE
        alpha = 0f
        setBackgroundColor(palette.surface)
        minimumHeight = dp(56f)

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        btnClose = ImageButton(context).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(palette.textPrimary)
            background = TelegramThemeSelectors.getCircleSelectorDrawable(palette.actionSelector)
            contentDescription = "Cancel selection"
            layoutParams = LinearLayout.LayoutParams(dp(54f), dp(56f))
            setOnClickListener {
                VibrationHelper.vibrateLightHaptic(context)
                onCloseClicked?.invoke()
            }
        }
        row.addView(btnClose)

        numberTextView = TelegramNumberTextView(context).apply {
            setTextColor(palette.textPrimary)
            setTextSize(18f)
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                marginStart = dp(16f)
            }
        }
        row.addView(numberTextView)

        actionsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT
            )
        }
        row.addView(actionsLayout)

        addView(row)
    }

    fun setSelectedCount(count: Int, animated: Boolean = true) {
        numberTextView.setNumber(count, animated)
    }

    fun clearActions() {
        actionsLayout.removeAllViews()
    }

    fun addAction(
        id: Int,
        @DrawableRes iconRes: Int,
        contentDescription: String,
        isDestructive: Boolean = false,
        onClick: () -> Unit
    ): ImageButton {
        val btn = ImageButton(context).apply {
            tag = id
            setImageResource(iconRes)
            val tint = if (isDestructive) {
                ContextCompat.getColor(context, R.color.design_destructive_text)
            } else {
                palette.textPrimary
            }
            setColorFilter(tint)
            background = TelegramThemeSelectors.getCircleSelectorDrawable(palette.actionSelector)
            this.contentDescription = contentDescription
            layoutParams = LinearLayout.LayoutParams(dp(48f), dp(56f))
            setOnClickListener {
                VibrationHelper.vibrateLightHaptic(context)
                onClick()
            }
        }
        actionsLayout.addView(btn)
        return btn
    }

    fun show(animated: Boolean = true, onShowFinished: (() -> Unit)? = null) {
        if (visibility == View.VISIBLE && alpha == 1f) return
        visibility = View.VISIBLE
        if (animated) {
            animate()
                .alpha(1f)
                .setDuration(200L)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onShowFinished?.invoke()
                    }
                })
                .start()
        } else {
            alpha = 1f
            onShowFinished?.invoke()
        }
    }

    fun hide(animated: Boolean = true, onHideFinished: (() -> Unit)? = null) {
        if (visibility == View.GONE) return
        if (animated) {
            animate()
                .alpha(0f)
                .setDuration(160L)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                        onHideFinished?.invoke()
                    }
                })
                .start()
        } else {
            alpha = 0f
            visibility = View.GONE
            onHideFinished?.invoke()
        }
    }
}

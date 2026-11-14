/*
 * Derived from Telegram Android EmptyTextProgressView.java @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RawRes
import androidx.appcompat.widget.AppCompatButton
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import com.eve.app.util.VibrationHelper
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Typealias for Telegram canonical naming.
 */
typealias EmptyTextProgressView = TelegramEmptyTextProgressView

/**
 * Authentic Telegram EmptyTextProgressView.
 * Upstream: org.telegram.ui.Components.EmptyTextProgressView.java
 * Combines radial progress view, animated sticker / icon, title and description text
 * with animated transitions.
 */
class TelegramEmptyTextProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)
    private val progressView: TelegramRadialProgressView
    private val contentLayout: LinearLayout
    private val lottieView: LottieAnimationView
    private val tvTitle: TextView
    private val tvSubtitle: TextView
    private val btnAction: AppCompatButton

    init {
        // Center radial progress spinner
        progressView = TelegramRadialProgressView(context).apply {
            layoutParams = LayoutParams(dp(44f), dp(44f), Gravity.CENTER)
            visibility = View.GONE
        }
        addView(progressView)

        // Center content layout: Sticker + Title + Subtitle + Action
        contentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24f), 0, dp(24f), 0)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            clipChildren = false
            clipToPadding = false
        }

        lottieView = LottieAnimationView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(140f), dp(140f)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12f)
            }
            visibility = View.GONE
        }
        contentLayout.addView(lottieView)

        tvTitle = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            typeface = FontManager.boldTypeface(context)
            setTextColor(palette.textPrimary)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        contentLayout.addView(tvTitle)

        tvSubtitle = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = FontManager.telegramRegularTypeface(context)
            setTextColor(palette.textSecondary)
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(6f)
            }
            visibility = View.GONE
        }
        contentLayout.addView(tvSubtitle)

        btnAction = AppCompatButton(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = FontManager.telegramMediumTypeface(context)
            setTextColor(palette.accent)
            background = null
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(44f)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(12f)
            }
            visibility = View.GONE
        }
        contentLayout.addView(btnAction)

        addView(contentLayout)
    }

    fun showProgress(animated: Boolean = true) {
        if (animated) {
            contentLayout.animate().alpha(0f).setDuration(150L).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    contentLayout.visibility = View.GONE
                }
            }).start()
            progressView.visibility = View.VISIBLE
            progressView.alpha = 0f
            progressView.animate().alpha(1f).setDuration(200L).setListener(null).start()
        } else {
            contentLayout.visibility = View.GONE
            contentLayout.alpha = 0f
            progressView.visibility = View.VISIBLE
            progressView.alpha = 1f
        }
    }

    fun showTextView(animated: Boolean = true) {
        if (animated) {
            progressView.animate().alpha(0f).setDuration(150L).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progressView.visibility = View.GONE
                }
            }).start()
            contentLayout.visibility = View.VISIBLE
            contentLayout.alpha = 0f
            contentLayout.animate().alpha(1f).setDuration(200L).setListener(null).start()
        } else {
            progressView.visibility = View.GONE
            progressView.alpha = 0f
            contentLayout.visibility = View.VISIBLE
            contentLayout.alpha = 1f
        }
    }

    fun setText(title: CharSequence, subtitle: CharSequence? = null) {
        tvTitle.text = title
        if (subtitle.isNullOrBlank()) {
            tvSubtitle.visibility = View.GONE
        } else {
            tvSubtitle.text = subtitle
            tvSubtitle.visibility = View.VISIBLE
        }
    }

    fun setSticker(@RawRes rawRes: Int, autoPlay: Boolean = true) {
        lottieView.visibility = View.VISIBLE
        lottieView.setAnimation(rawRes)
        if (autoPlay) {
            lottieView.playAnimation()
        }
    }

    fun setActionButton(text: String, onClick: () -> Unit) {
        btnAction.text = text
        btnAction.visibility = View.VISIBLE
        btnAction.setOnClickListener {
            VibrationHelper.vibrateLightHaptic(context)
            onClick()
        }
    }
}

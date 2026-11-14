/*
 * Derived from Telegram Android EditTextBoldCursor.java @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.animation.CycleInterpolator
import androidx.appcompat.widget.AppCompatEditText
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import com.eve.app.util.VibrationHelper

/**
 * Typealias for Telegram canonical naming.
 */
typealias EditTextBoldCursor = TelegramEditText

/**
 * Authentic Telegram EditText with bold cursor and error shake animation.
 * Upstream: org.telegram.ui.Components.EditTextBoldCursor.java
 */
class TelegramEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)

    init {
        typeface = FontManager.telegramRegularTypeface(context)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTextColor(palette.textPrimary)
        setHintTextColor(palette.textSecondary)

        // Custom accent-colored cursor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cursorDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(palette.accent)
                setSize(dp(2f), dp(20f))
                cornerRadius = dp(1f).toFloat()
            }
            textCursorDrawable = cursorDrawable
        }
        background = null
        includeFontPadding = false
    }

    /**
     * Authentic error shake animation (AndroidUtilities.shakeView).
     * Shakes horizontally with cycle interpolator and error vibration.
     */
    fun shakeError() {
        VibrationHelper.vibrateAppError(context)
        val animator = ObjectAnimator.ofFloat(this, "translationX", 0f, dp(8f).toFloat(), -dp(8f).toFloat(), dp(4f).toFloat(), -dp(4f).toFloat(), 0f).apply {
            duration = 350L
            interpolator = CycleInterpolator(1f)
        }
        animator.start()
    }
}

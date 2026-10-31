/*
 * Telegram-derived profile single-choice row.
 * Upstream DrKLO/Telegram@3de37c8.../RadioCell.java:55-85,96-108,132-137.
 * Indicator reuses EVE's GPL-noticed port of Telegram RadioButton.java.
 * ADAPTED: independent EVE view hierarchy, theme role, input/checked API and density handling.
 * GPL v2 or later. See THIRD_PARTY_NOTICES.md and docs/EVE_PROFILE_CATEGORY_PROVENANCE.md.
 */
package com.eve.app.ui.profile

import android.animation.ValueAnimator
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.common.TelegramRadioIndicator
import kotlin.math.ceil

class TelegramProfileRadioCell @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private fun dp(value: Int) = ceil(value * resources.displayMetrics.density).toInt()
    private fun motionEnabled() = Settings.Global.getFloat(
        context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    ) > 0f && (android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled())

    private val radioDrawable = TelegramRadioIndicator(resources.displayMetrics.density, dp(22), ::motionEnabled)
    private val labelView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL or Gravity.START
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val radioView = View(context).apply {
        background = radioDrawable
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    var isChecked: Boolean = false
        set(value) {
            field = value
            radioDrawable.setChecked(value, isAttachedToWindow)
            refreshDrawableState()
        }

    init {
        val a = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
        labelView.text = a.getText(0) ?: ""
        a.recycle()
        contentDescription = labelView.text
        isClickable = true
        isFocusable = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        minimumHeight = dp(50)
        val selector = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        background = selector.getDrawable(0)
        selector.recycle()
        addView(labelView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            marginStart = dp(20)
            marginEnd = dp(54)
        })
        addView(radioView, LayoutParams(dp(22), dp(22), Gravity.END or Gravity.CENTER_VERTICAL).apply {
            marginEnd = dp(20)
        })
        applyPalette()
    }

    private fun applyPalette() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        labelView.setTextColor(palette.textPrimary)
        radioDrawable.setColors(palette.textSecondary, palette.accent)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyPalette()
        radioDrawable.setChecked(isChecked, false)
    }

    override fun onDetachedFromWindow() {
        radioDrawable.jumpToCurrentState()
        super.onDetachedFromWindow()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.RadioButton"
        info.isCheckable = true
        info.isChecked = isChecked
    }
}

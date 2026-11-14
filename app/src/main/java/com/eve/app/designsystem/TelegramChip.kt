/*
 * Derived from Telegram Android FiltersView.java, FilterTabsView.java, ProfileActionsView.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.google.android.material.chip.Chip
import com.google.android.material.shape.ShapeAppearanceModel
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Exact Telegram filter chip and pill control.
 *
 * Upstream sources:
 * - Adapters/FiltersView.java:697-760 (FilterView round rect pill: 28dp/32dp height, 14dp/16dp radius)
 * - Components/FilterTabsView.java:176-178 (pill radius 14dp)
 * - Components/ProfileActionsView.java (pill action geometry)
 */
open class TelegramChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.chipStyle
) : Chip(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private var badgeCount: Int? = null
    private var baseText: CharSequence? = ""

    private var scaleProgress: Float = 1.0f
    private var pressAnimator: ValueAnimator? = null

    init {
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        includeFontPadding = false

        // Telegram pill geometry: 32dp height, 16dp radius, minimum 48dp touch slot
        chipMinHeight = dp(32f).toFloat()
        chipCornerRadius = dp(16f).toFloat()
        chipStrokeWidth = 0f
        isCheckedIconVisible = false
        isCloseIconVisible = false
        setEnsureMinTouchTargetSize(true)

        val hPad = dp(14f).toFloat()
        chipStartPadding = hPad
        chipEndPadding = hPad
        textStartPadding = 0f
        textEndPadding = 0f

        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(dp(16f).toFloat())
            .build()

        isFocusable = true
        isClickable = true
        isCheckable = true

        val initialText = text
        if (!initialText.isNullOrEmpty()) {
            baseText = initialText
        }

        updateAppearance()
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        val current = baseText
        if ((current == null || current.isEmpty()) && !text.isNullOrEmpty()) {
            baseText = text
        }
    }

    fun setBadgeCount(count: Int?) {
        badgeCount = count
        val current = baseText ?: text ?: ""
        if (count != null && count > 0) {
            super.setText("$current ($count)")
        } else {
            super.setText(current)
        }
    }

    fun setChipIconDrawable(drawable: Drawable?) {
        chipIcon = drawable
        isChipIconVisible = drawable != null
        chipIconSize = dp(18f).toFloat()
    }

    fun setChipIconRes(@DrawableRes resId: Int) {
        if (resId != 0) {
            setChipIconDrawable(ContextCompat.getDrawable(context, resId))
        } else {
            setChipIconDrawable(null)
        }
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        updateAppearance()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> animateScale(0.96f)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateScale(1.0f)
        }
        return super.onTouchEvent(event)
    }

    private fun animateScale(target: Float) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(scaleProgress, target).apply {
            duration = 140L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                scaleProgress = it.animatedValue as Float
                scaleX = scaleProgress
                scaleY = scaleProgress
            }
            start()
        }
    }

    fun updateAppearance() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        if (isChecked) {
            // Authentic Telegram checked pill
            chipBackgroundColor = ColorStateList.valueOf(palette.accent)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            chipStrokeWidth = 0f
            rippleColor = ColorStateList.valueOf(0x33FFFFFF)
            chipIconTint = ColorStateList.valueOf(Color.WHITE)
        } else {
            // Authentic Telegram unchecked pill
            val bgColor = if (palette.isDark) 0x24FFFFFF else 0x14000000
            chipBackgroundColor = ColorStateList.valueOf(bgColor)
            setTextColor(palette.textPrimary)
            typeface = Typeface.DEFAULT
            chipStrokeColor = ColorStateList.valueOf(palette.hairline)
            chipStrokeWidth = dp(1f).toFloat()
            rippleColor = ColorStateList.valueOf(palette.actionSelector)
            chipIconTint = ColorStateList.valueOf(palette.textSecondary)
        }
        invalidate()
    }
}

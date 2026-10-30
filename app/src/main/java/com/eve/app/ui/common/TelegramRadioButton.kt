package com.eve.app.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.designsystem.DesignType

/** Shared answer tile. Native RadioGroup selection and accessibility semantics are preserved. */
class TelegramRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.radioButtonStyle
) : AppCompatRadioButton(context, attrs, defStyleAttr) {
    internal var indicator: TelegramRadioIndicator? = null
        private set
    private var palette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette? = null
    init { applyChoiceStyle() }

    private fun motionEnabled(): Boolean =
        android.provider.Settings.Global.getFloat(context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f &&
            (android.os.Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled())

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        indicator?.setChecked(isChecked, isAttachedToWindow)
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        indicator?.jumpToCurrentState()
    }

    override fun onDetachedFromWindow() {
        indicator?.jumpToCurrentState()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updatePalette(com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(context))
        indicator?.setChecked(isChecked, false)
    }

    fun updatePalette(value: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette) {
        palette = value
        applyChoiceColors()
    }

    private fun applyChoiceColors() {
        val p = palette
        fun color(id: Int) = ContextCompat.getColor(context, id)
        // Owner's R07 selected-answer state is not a correctness judgment.
        // Telegram ThemeColors.java:94-95 defines green text roles; this
        // exam-specific green selection surface is an ADAPTED EVE decision.
        // Use theme-qualified system green so Day/Night both remain legible.
        val selectedGreen = color(R.color.system_green)
        val onSelectedGreen = color(R.color.solid_black)
        val inset = p?.surfaceInset ?: color(R.color.surface_inset)
        val primary = p?.textPrimary ?: color(R.color.text_primary)
        val secondary = p?.textSecondary ?: color(R.color.text_secondary)

        fun surface(color: Int) = GradientDrawable().apply {
            cornerRadius = resources.getDimension(R.dimen.design_radius_inner)
            setColor(color)
        }
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), surface(selectedGreen))
            addState(intArrayOf(), surface(inset))
        }
        setTextColor(ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(onSelectedGreen, primary)
        ))
        indicator?.setColors(secondary, onSelectedGreen)
    }

    fun applyChoiceStyle() {
        indicator?.jumpToCurrentState()
        indicator = TelegramRadioIndicator(resources.displayMetrics.density,
            resources.getDimensionPixelSize(R.dimen.design_icon_size), ::motionEnabled)
        // API 24+ AppCompat inflation installs buttonTint through the public framework
        // channel. Clear it so the source drawing interpolates its own two colors.
        androidx.core.widget.CompoundButtonCompat.setButtonTintList(this, null)
        val iconInset = resources.getDimensionPixelSize(R.dimen.design_card_padding)
        buttonDrawable = android.graphics.drawable.InsetDrawable(indicator, iconInset, 0, iconInset, 0)
        indicator?.setChecked(isChecked, false)
        DesignType.apply(this, DesignType.Role.BODY)
        applyChoiceColors()
        minHeight = resources.getDimensionPixelSize(R.dimen.design_touch_min)
        val pad = resources.getDimensionPixelSize(R.dimen.design_card_padding)
        setPaddingRelative(0, pad, pad, pad)
        compoundDrawablePadding = pad
        stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(context, R.animator.design_press_scale)
    }
}

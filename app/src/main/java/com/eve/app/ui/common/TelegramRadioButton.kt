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
    init { applyChoiceStyle() }

    fun applyChoiceStyle() {
        fun surface(color: Int) = GradientDrawable().apply {
            cornerRadius = resources.getDimension(R.dimen.design_radius_inner)
            setColor(ContextCompat.getColor(context, color))
        }
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), surface(R.color.accent))
            addState(intArrayOf(), surface(R.color.surface_inset))
        }
        DesignType.apply(this, DesignType.Role.BODY)
        setTextColor(ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(context, R.color.on_accent), ContextCompat.getColor(context, R.color.text_primary))
        ))
        val iconInset = resources.getDimensionPixelSize(R.dimen.design_card_padding)
        buttonDrawable = android.graphics.drawable.InsetDrawable(ContextCompat.getDrawable(context, R.drawable.design_choice_indicator), iconInset, 0, iconInset, 0)
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(context, R.color.on_accent), ContextCompat.getColor(context, R.color.text_secondary))
        )
        minHeight = resources.getDimensionPixelSize(R.dimen.design_touch_min)
        val pad = resources.getDimensionPixelSize(R.dimen.design_card_padding)
        setPaddingRelative(0, pad, pad, pad)
        compoundDrawablePadding = pad
        stateListAnimator = android.animation.AnimatorInflater.loadStateListAnimator(context, R.animator.design_press_scale)
    }
}

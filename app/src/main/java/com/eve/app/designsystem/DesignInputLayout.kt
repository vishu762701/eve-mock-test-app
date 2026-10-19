package com.eve.app.designsystem

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.eve.app.R
import com.eve.app.util.FontManager
import com.google.android.material.textfield.TextInputLayout

/** Native validation/dropdown/password behavior with one tokenized field surface. */
class DesignInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        editText?.let { TouchTargets.ensure(it) }
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        styleHelpers(this)
    }
    private fun styleHelpers(group: ViewGroup) {
        for (i in 0 until group.childCount) {
            val view = group.getChildAt(i)
            if (view is TextView && view !is EditText && view.getTag(R.id.design_input_helper_styled) == null) {
                view.setTag(R.id.design_input_helper_styled, true)
                view.includeFontPadding = false
                TextViewCompat.setLineHeight(view, resources.getDimensionPixelSize(R.dimen.design_line_secondary))
            }
            if (view is ViewGroup) styleHelpers(view)
        }
    }
    init {
        addOnEditTextAttachedListener { layout ->
            val field = requireNotNull(layout.editText)
            val placeholder = layout.hint ?: field.hint
            layout.isHintEnabled = false
            field.hint = placeholder
            val multiline = field.minLines > 1 ||
                (field.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            val padding = resources.getDimensionPixelSize(R.dimen.design_input_padding)
            val height = resources.getDimensionPixelSize(R.dimen.design_input_height)
            val radius = resources.getDimension(if (multiline) R.dimen.design_radius_multiline else R.dimen.design_input_height) /
                if (multiline) 1f else 2f
            layout.setBoxCornerRadii(radius, radius, radius, radius)
            field.background = null
            field.includeFontPadding = false
            field.typeface = FontManager.typeface(context, bold = false)
            field.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.design_type_body))
            TextViewCompat.setLineHeight(field, resources.getDimensionPixelSize(R.dimen.design_line_body))
            field.setTextColor(DesignTokens.primary(context))
            field.setHintTextColor(DesignTokens.secondary(context))
            field.setPaddingRelative(padding, if (multiline) padding else 0, padding, if (multiline) padding else 0)
            field.minimumHeight = height
            if (!multiline) {
                field.layoutParams = field.layoutParams.apply { this.height = height }
                field.gravity = (field.gravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or Gravity.CENTER_VERTICAL
            } else if (field.layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                field.layoutParams = field.layoutParams.apply { this.height = ViewGroup.LayoutParams.WRAP_CONTENT }
            }
            layout.boxStrokeWidth = resources.getDimensionPixelSize(R.dimen.design_focus_stroke)
            layout.boxStrokeWidthFocused = layout.boxStrokeWidth
            layout.setBoxStrokeColorStateList(requireNotNull(ContextCompat.getColorStateList(context, R.color.design_input_stroke)))
            layout.boxStrokeErrorColor = ContextCompat.getColorStateList(context, R.color.system_red)
            layout.setErrorTextAppearance(R.style.TextAppearance_Eve_Token_Error)
            layout.setErrorTextColor(requireNotNull(ContextCompat.getColorStateList(context, R.color.system_red)))
            if (field is AutoCompleteTextView) layout.endIconMode = END_ICON_DROPDOWN_MENU
        }
    }
}

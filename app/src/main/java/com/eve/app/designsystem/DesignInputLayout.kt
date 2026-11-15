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
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import com.eve.app.util.TelegramShake
import com.eve.app.util.VibrationHelper
import com.google.android.material.textfield.TextInputLayout

/**
 * Native validation/dropdown/password behavior with Telegram OutlineTextContainerView geometry.
 * 10dp corner radius, 1dp -> 1.8dp focused stroke width, TelegramShake.shakeViewSpring error shake,
 * and TelegramThemeEngine palette colors.
 */
class DesignInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Float = v * density
    private fun dpInt(v: Float): Int = (v * density + 0.5f).toInt()

    private fun updateTokenStroke() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val field = editText ?: return
        val background = field.background
        val box = when (background) {
            is com.google.android.material.shape.MaterialShapeDrawable -> background
            is android.graphics.drawable.LayerDrawable -> if (background.numberOfLayers > 0)
                background.getDrawable(background.numberOfLayers - 1) as? com.google.android.material.shape.MaterialShapeDrawable else null
            else -> null
        } ?: return

        val hasError = !error.isNullOrEmpty() || (isCounterEnabled && counterMaxLength > 0 && field.length() > counterMaxLength)
        val color = when {
            !isEnabled -> ContextCompat.getColor(context, R.color.transparent)
            hasError -> 0xFFE53935.toInt()
            field.hasFocus() -> palette.accent
            else -> palette.hairline
        }
        val strokeWidth = if (field.hasFocus()) dp(1.8f) else dp(1.0f)
        box.setStroke(strokeWidth, color)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        updateTokenStroke()
    }

    override fun setError(errorText: CharSequence?) {
        super.setError(errorText)
        updateTokenStroke()
        if (!errorText.isNullOrEmpty()) {
            VibrationHelper.vibrateAppError(context)
            TelegramShake.shakeViewSpring(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        editText?.let { TouchTargets.ensure(it) }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        styleHelpers(this)
        updateTokenStroke()
    }

    private fun styleHelpers(group: ViewGroup) {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        for (i in 0 until group.childCount) {
            val view = group.getChildAt(i)
            if (view is TextView && view !is EditText && view.getTag(R.id.design_input_helper_styled) == null) {
                view.setTag(R.id.design_input_helper_styled, true)
                view.includeFontPadding = false
                view.setTextColor(palette.textSecondary)
                TextViewCompat.setLineHeight(view, resources.getDimensionPixelSize(R.dimen.design_line_secondary))
            }
            if (view is ViewGroup) styleHelpers(view)
        }
    }

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val radius = dp(10f)
        setBoxCornerRadii(radius, radius, radius, radius)
        boxStrokeWidth = dpInt(1.0f)
        boxStrokeWidthFocused = dpInt(1.8f)

        addOnEditTextAttachedListener { layout ->
            val field = requireNotNull(layout.editText)
            val placeholder = layout.hint ?: field.hint
            layout.isHintEnabled = false
            field.hint = placeholder
            val multiline = field.minLines > 1 ||
                (field.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            val padding = dpInt(14f)
            val height = dpInt(54f)

            layout.setBoxCornerRadii(radius, radius, radius, radius)
            field.background = null
            field.includeFontPadding = false
            field.typeface = FontManager.telegramRegularTypeface(context)
            field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            field.setTextColor(palette.textPrimary)
            field.setHintTextColor(palette.textSecondary)
            field.setPaddingRelative(padding, if (multiline) dpInt(12f) else 0, padding, if (multiline) dpInt(12f) else 0)
            field.minimumHeight = height

            if (!multiline) {
                field.layoutParams = field.layoutParams.apply { this.height = height }
                field.gravity = (field.gravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or Gravity.CENTER_VERTICAL
            } else if (field.layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT) {
                field.layoutParams = field.layoutParams.apply { this.height = ViewGroup.LayoutParams.WRAP_CONTENT }
            }

            layout.boxStrokeWidth = dpInt(1.0f)
            layout.boxStrokeWidthFocused = dpInt(1.8f)
            layout.boxStrokeErrorColor = ContextCompat.getColorStateList(context, R.color.system_red)
            layout.setErrorTextAppearance(R.style.TextAppearance_Eve_Token_Error)
            layout.setErrorTextColor(requireNotNull(ContextCompat.getColorStateList(context, R.color.system_red_text)))

            if (field is AutoCompleteTextView) {
                layout.endIconMode = END_ICON_DROPDOWN_MENU
                layout.setEndIconDrawable(R.drawable.msg_arrowright)
            } else if (layout.endIconMode == END_ICON_PASSWORD_TOGGLE) {
                layout.setEndIconDrawable(R.drawable.design_password_toggle)
            }
            updateTokenStroke()
            field.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(text: android.text.Editable?) { updateTokenStroke() }
            })
        }
    }
}

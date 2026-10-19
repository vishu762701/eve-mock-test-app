package com.eve.app.designsystem

import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.eve.app.R
import com.eve.app.util.FontManager

/** The same type roles for programmatic rows, framework widgets and XML-inflated text. */
object DesignType {
    enum class Role { TITLE, HEADING, BODY, SECONDARY, CHIP, BUTTON }
    fun apply(view: TextView, role: Role) {
        val style: Int
        val line: Int
        when (role) {
            Role.TITLE -> { style = R.style.TextAppearance_Eve_Token_Title; line = R.dimen.design_line_title }
            Role.HEADING -> { style = R.style.TextAppearance_Eve_Token_Heading; line = R.dimen.design_line_heading }
            Role.BODY -> { style = R.style.TextAppearance_Eve_Token_Body; line = R.dimen.design_line_body }
            Role.SECONDARY -> { style = R.style.TextAppearance_Eve_Token_Secondary; line = R.dimen.design_line_secondary }
            Role.CHIP -> { style = R.style.TextAppearance_Eve_Token_Chip; line = R.dimen.design_line_chip }
            Role.BUTTON -> { style = R.style.TextAppearance_Eve_Token_Button; line = R.dimen.design_line_button }
        }
        TextViewCompat.setTextAppearance(view, style)
        view.includeFontPadding = false
        view.typeface = if (role == Role.CHIP) FontManager.mediumTypeface(view.context)
            else FontManager.typeface(view.context, role in listOf(Role.TITLE, Role.HEADING, Role.BUTTON))
        TextViewCompat.setLineHeight(view, view.resources.getDimensionPixelSize(line))
    }
}

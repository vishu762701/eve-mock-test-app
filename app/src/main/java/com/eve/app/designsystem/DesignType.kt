package com.eve.app.designsystem

import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.eve.app.R
import com.eve.app.typography.TypographyRepository
import com.eve.app.util.FontManager

/**
 * Authoritative central typography coordinator for the official iOS 26 dynamic typography scale
 * and component adaptations across programmatic rows, framework widgets, and XML views.
 */
object DesignType {
    enum class Role {
        // Canonical Apple iOS 26 text styles
        LARGE_TITLE,
        TITLE_1,
        TITLE_2,
        TITLE_3,
        HEADLINE,
        BODY,
        CALLOUT,
        SUBHEADLINE,
        FOOTNOTE,
        CAPTION_1,
        CAPTION_2,

        // Semantic component adaptations & backward-compatible aliases
        TITLE,       // Maps to Title 1 Emphasized (28sp bold)
        HEADING,     // Maps to Headline (17sp semibold)
        SECONDARY,   // Maps to Subheadline (15sp regular)
        TAB,         // Maps to 14/15sp tab label
        CHIP,        // Maps to Footnote Medium/Emphasized (14sp medium)
        BUTTON       // Maps to Callout Emphasized (16sp semibold)
    }

    fun apply(view: TextView, role: Role, emphasized: Boolean = false) {
        val style: Int
        val line: Int
        when (role) {
            Role.LARGE_TITLE -> {
                style = if (emphasized) R.style.TextAppearance_Eve_LargeTitle_Emphasized else R.style.TextAppearance_Eve_LargeTitle
                line = R.dimen.design_line_large_title
            }
            Role.TITLE_1, Role.TITLE -> {
                style = if (emphasized || role == Role.TITLE) R.style.TextAppearance_Eve_Token_Title else R.style.TextAppearance_Eve_Title1
                line = R.dimen.design_line_title1
            }
            Role.TITLE_2 -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Title2_Emphasized else R.style.TextAppearance_Eve_Title2
                line = R.dimen.design_line_title2
            }
            Role.TITLE_3 -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Title3_Emphasized else R.style.TextAppearance_Eve_Title3
                line = R.dimen.design_line_title3
            }
            Role.HEADLINE, Role.HEADING -> {
                style = if (role == Role.HEADING) R.style.TextAppearance_Eve_Token_Heading else R.style.TextAppearance_Eve_Headline
                line = R.dimen.design_line_headline
            }
            Role.BODY -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Body_Emphasized else R.style.TextAppearance_Eve_Token_Body
                line = R.dimen.design_line_body
            }
            Role.CALLOUT -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Callout_Emphasized else R.style.TextAppearance_Eve_Callout
                line = R.dimen.design_line_callout
            }
            Role.SUBHEADLINE, Role.SECONDARY -> {
                style = if (role == Role.SECONDARY) R.style.TextAppearance_Eve_Token_Secondary
                else if (emphasized) R.style.TextAppearance_Eve_Subheadline_Emphasized
                else R.style.TextAppearance_Eve_Subheadline
                line = R.dimen.design_line_subheadline
            }
            Role.FOOTNOTE -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Footnote_Emphasized else R.style.TextAppearance_Eve_Footnote
                line = R.dimen.design_line_footnote
            }
            Role.CAPTION_1 -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Caption1_Emphasized else R.style.TextAppearance_Eve_Caption1
                line = R.dimen.design_line_caption1
            }
            Role.CAPTION_2 -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Caption2_Emphasized else R.style.TextAppearance_Eve_Caption2
                line = R.dimen.design_line_caption2
            }
            Role.TAB -> {
                style = if (emphasized) R.style.TextAppearance_Eve_Token_Tab_Selected else R.style.TextAppearance_Eve_Token_Tab
                line = R.dimen.design_line_tab
            }
            Role.CHIP -> {
                style = R.style.TextAppearance_Eve_Token_Chip
                line = R.dimen.design_line_chip
            }
            Role.BUTTON -> {
                style = R.style.TextAppearance_Eve_Token_Button
                line = R.dimen.design_line_button
            }
        }
        TextViewCompat.setTextAppearance(view, style)
        view.includeFontPadding = false
        view.paint.isFakeBoldText = false

        val config = try {
            TypographyRepository.getInstance(view.context).getActiveConfig()
        } catch (_: Exception) {
            null
        }
        val family = FontManager.FontFamily.fromId(config?.fontFamily)

        view.typeface = when {
            role in listOf(Role.LARGE_TITLE, Role.TITLE_1, Role.TITLE_2, Role.TITLE) && (emphasized || role == Role.TITLE) -> FontManager.getTypeface(view.context, family, FontManager.FontWeight.BOLD)
            role in listOf(Role.TITLE_3, Role.HEADLINE, Role.HEADING, Role.BUTTON) -> FontManager.getTypeface(view.context, family, FontManager.FontWeight.SEMIBOLD)
            role in listOf(Role.CHIP) -> FontManager.getTypeface(view.context, family, FontManager.FontWeight.MEDIUM)
            emphasized -> FontManager.getTypeface(view.context, family, FontManager.FontWeight.SEMIBOLD)
            else -> FontManager.getTypeface(view.context, family, FontManager.FontWeight.REGULAR)
        }
        TextViewCompat.setLineHeight(view, view.resources.getDimensionPixelSize(line))

        // Apply explicit component override if defined by admin
        if (config != null) {
            val roleKey = when (role) {
                Role.LARGE_TITLE -> "large_title"
                Role.TITLE_1, Role.TITLE -> "title_1"
                Role.TITLE_2 -> "title_2"
                Role.TITLE_3 -> "title_3"
                Role.HEADLINE, Role.HEADING -> "headline"
                Role.BODY -> "body"
                Role.CALLOUT -> "callout"
                Role.SUBHEADLINE, Role.SECONDARY -> "subheadline"
                Role.FOOTNOTE -> "footnote"
                Role.CAPTION_1 -> "caption_1"
                Role.CAPTION_2 -> "caption_2"
                Role.BUTTON -> "button"
                Role.TAB -> "tab"
                Role.CHIP -> "chip"
            }
            val compOverride = config.componentOverrides[roleKey]
            if (compOverride != null) {
                compOverride.textSizeSp?.let { view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, it) }
                compOverride.lineHeightSp?.let {
                    val density = view.resources.displayMetrics.scaledDensity
                    val px = (it * density).toInt()
                    if (px > 0) TextViewCompat.setLineHeight(view, px)
                }
                compOverride.letterSpacing?.let { view.letterSpacing = it }
            }
        }
    }
}

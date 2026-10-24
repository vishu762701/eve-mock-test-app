package com.eve.app.theme

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.eve.app.R
import com.eve.app.designsystem.DesignGlassLayout
import com.eve.app.designsystem.DesignSegmentedTabs
import com.eve.app.ui.navigation.EveBottomPillBar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

/**
 * App-wide runtime theme applier.
 * Dynamically binds views (backgrounds, surfaces, cards, text, icons, buttons, local tabs, chips, toolbars)
 * to the single authoritative TelegramThemeEngine runtime palette.
 *
 * Guarantees zero reinflation: active exam questions, answers, timers, scroll offsets,
 * and user text inputs are strictly preserved.
 */
object TelegramThemeApplier {

    fun applyToActivity(activity: Activity, palette: TelegramThemeEngine.ResolvedPalette) {
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        root.setBackgroundColor(palette.windowBackground)
        activity.window.decorView.setBackgroundColor(palette.windowBackground)
        applyToViewTree(root, palette)
    }

    fun applyToViewTree(view: View, palette: TelegramThemeEngine.ResolvedPalette) {
        if (view.tag == "no_theme" || view.tag == "status_color" || view.tag == "semantic_status") {
            return
        }

        when (view) {
            is EveBottomPillBar -> {
                view.updateThemePalette(palette)
            }
            is DesignSegmentedTabs -> {
                view.updatePalette(palette)
            }
            is DesignGlassLayout -> {
                view.updatePalette(palette)
            }
            is MaterialCardView -> {
                if (view.tag != "solid_color_card") {
                    view.setCardBackgroundColor(palette.surface)
                }
                if (view.strokeWidth > 0) {
                    view.strokeColor = palette.hairline
                }
            }
            is MaterialButton -> {
                val tagStr = view.tag?.toString() ?: ""
                val isAccentButton = tagStr.contains("accent_button") ||
                        view.id == R.id.btnApply ||
                        view.id == R.id.btnRetry ||
                        view.backgroundTintList?.defaultColor == palette.accent

                if (isAccentButton) {
                    view.backgroundTintList = ColorStateList.valueOf(palette.accent)
                    view.setTextColor(palette.onAccent)
                } else if (view.strokeWidth > 0) {
                    view.strokeColor = ColorStateList.valueOf(palette.hairline)
                    view.setTextColor(palette.textPrimary)
                }
            }
            is Chip -> {
                if (view.isChecked) {
                    view.chipBackgroundColor = ColorStateList.valueOf(palette.accent)
                    view.setTextColor(palette.onAccent)
                } else {
                    view.chipBackgroundColor = ColorStateList.valueOf(palette.surfaceInset)
                    view.setTextColor(palette.textPrimary)
                }
            }
            is CheckBox -> {
                view.buttonTintList = ColorStateList.valueOf(palette.accent)
                view.setTextColor(palette.textPrimary)
            }
            is RadioButton -> {
                view.buttonTintList = ColorStateList.valueOf(palette.accent)
                view.setTextColor(palette.textPrimary)
            }
            is EditText -> {
                view.setTextColor(palette.textPrimary)
                view.setHintTextColor(palette.textSecondary)
            }
            is Toolbar -> {
                view.setTitleTextColor(palette.textPrimary)
                view.setSubtitleTextColor(palette.textSecondary)
            }
            is TextView -> {
                applyTextViewColors(view, palette)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToViewTree(view.getChildAt(i), palette)
            }
        }
    }

    private fun applyTextViewColors(tv: TextView, palette: TelegramThemeEngine.ResolvedPalette) {
        val tagStr = tv.tag?.toString() ?: ""
        if (tagStr.contains("semantic_") ||
            tagStr == "status_correct" ||
            tagStr == "status_wrong" ||
            tagStr == "status_unattempted" ||
            tagStr == "timer_countdown"
        ) {
            return
        }
        val current = tv.currentTextColor
        if (current == Color.BLACK || current == Color.WHITE || current == 0xFF000000.toInt() || current == 0xFFFFFFFF.toInt()) {
            tv.setTextColor(palette.textPrimary)
        } else if (current == 0xFF6C6C70.toInt() || current == 0xFFA1A1A6.toInt()) {
            tv.setTextColor(palette.textSecondary)
        }
    }
}

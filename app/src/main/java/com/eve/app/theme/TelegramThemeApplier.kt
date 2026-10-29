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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.util.WeakHashMap

/**
 * App-wide runtime theme applier.
 * Dynamically binds views (backgrounds, surfaces, cards, text, icons, buttons, local tabs, chips, toolbars)
 * to the single authoritative TelegramThemeEngine runtime palette.
 *
 * Guarantees zero reinflation: active exam questions, answers, timers, scroll offsets,
 * and user text inputs are strictly preserved.
 */
object TelegramThemeApplier {

    private enum class TextColorRole { PRIMARY, SECONDARY, ON_ACCENT }

    private data class AppliedTextColor(val role: TextColorRole, val color: Int)

    // Retain semantic roles across palette changes without retaining detached views.
    private val appliedTextColors = WeakHashMap<TextView, AppliedTextColor>()
    private val appliedImageColors = WeakHashMap<ImageView, Pair<Int, Int>>()

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

        applyBackgroundRole(view, palette)
        when (view) {
            is com.eve.app.designsystem.TelegramFlickerSkeletonView -> {
                view.updateTheme(palette)
                return
            }
            is DesignSegmentedTabs -> {
                // TabLayout owns its selected/unselected label colors. Recursing into
                // its child TextViews would classify onAccent BLACK/WHITE as a generic
                // primary text role and overwrite it with textPrimary.
                view.updatePalette(palette)
                return
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
                } else if (view.id == R.id.btnAdmin || tagStr.contains("admin_button")) {
                    com.eve.app.designsystem.EveDesignSystem.Components.applyAdminButtonPalette(view, palette)
                } else if (view.strokeWidth > 0) {
                    view.strokeColor = ColorStateList.valueOf(palette.hairline)
                    view.setTextColor(palette.textPrimary)
                } else {
                    view.setTextColor(palette.textPrimary)
                }
            }
            is Chip -> {
                val unselectedBg = if (palette.isDark) 0x1AFFFFFF else palette.surface
                view.chipBackgroundColor = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        palette.accent,
                        unselectedBg
                    )
                )
                view.setTextColor(
                    ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf(-android.R.attr.state_checked)
                        ),
                        intArrayOf(
                            palette.onAccent,
                            palette.textPrimary
                        )
                    )
                )
                view.checkedIconTint = ColorStateList.valueOf(palette.onAccent)
            }
            is CheckBox -> {
                view.buttonTintList = ColorStateList.valueOf(palette.accent)
                view.setTextColor(palette.textPrimary)
            }
            is com.eve.app.ui.common.TelegramRadioButton -> {
                view.updatePalette(palette)
            }
            is RadioButton -> {
                view.buttonTintList = ColorStateList.valueOf(palette.accent)
                view.setTextColor(palette.textPrimary)
            }
            is ImageView -> {
                val old = androidx.core.widget.ImageViewCompat.getImageTintList(view)
                if (old != null && !old.isStateful) {
                    val prior = appliedImageColors[view]
                    val current = old.defaultColor
                    val drawerColor = androidx.core.content.ContextCompat.getColor(view.context, R.color.telegram_drawer_icon)
                    val telegramPrimaryColor = androidx.core.content.ContextCompat.getColor(view.context, R.color.telegram_type_primary)
                    val role = if (prior?.second == current) prior.first else when (current) {
                        androidx.core.content.ContextCompat.getColor(view.context, R.color.text_primary),
                        telegramPrimaryColor -> 0
                        androidx.core.content.ContextCompat.getColor(view.context, R.color.text_secondary) -> 1
                        androidx.core.content.ContextCompat.getColor(view.context, R.color.accent),
                        androidx.core.content.ContextCompat.getColor(view.context, R.color.accent_icon) -> 2
                        drawerColor -> 3
                        else -> -1
                    }
                    val color = when (role) {
                        0 -> palette.textPrimary
                        1 -> palette.textSecondary
                        2 -> TelegramColorRoles.accessibleForeground(palette.accent, listOf(palette.windowBackground, palette.surface, palette.dialogSurface), 3.0)
                        3 -> if (palette.isDark) 0xFFFFFFFF.toInt() else palette.drawerIcon
                        else -> current
                    }
                    if (role >= 0) {
                        androidx.core.widget.ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color))
                        appliedImageColors[view] = role to color
                    }
                }
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

    private enum class BackgroundRole { CANVAS, SURFACE, DIALOG }
    private data class AppliedBackground(val role: BackgroundRole, val color: Int)
    private val backgroundRoles = WeakHashMap<View, AppliedBackground>()

    private fun solidColor(view: View): Int? = when (val background = view.background) {
        is android.graphics.drawable.ColorDrawable -> background.color
        is android.graphics.drawable.GradientDrawable -> background.color?.defaultColor
        else -> null
    }

    /** Preserve shape/padding and semantic caller-owned fills while rebinding late/live roots. */
    private fun applyBackgroundRole(view: View, palette: TelegramThemeEngine.ResolvedPalette) {
        if (view is com.eve.app.designsystem.TelegramFlickerSkeletonView) return
        val color = solidColor(view) ?: return
        val previous = backgroundRoles[view]
        val role = if (previous?.color == color) previous.role else when (color) {
            androidx.core.content.ContextCompat.getColor(view.context, R.color.bg_app) -> BackgroundRole.CANVAS
            androidx.core.content.ContextCompat.getColor(view.context, R.color.surface) -> BackgroundRole.SURFACE
            androidx.core.content.ContextCompat.getColor(view.context, R.color.dialog_surface) -> BackgroundRole.DIALOG
            else -> return
        }
        val resolved = when (role) {
            BackgroundRole.CANVAS -> palette.windowBackground
            BackgroundRole.SURFACE -> palette.surface
            BackgroundRole.DIALOG -> palette.dialogSurface
        }
        when (val background = view.background) {
            is android.graphics.drawable.ColorDrawable -> if (color != resolved) background.color = resolved
            is android.graphics.drawable.GradientDrawable -> if (color != resolved) (background.mutate() as android.graphics.drawable.GradientDrawable).setColor(resolved)
        }
        backgroundRoles[view] = AppliedBackground(role, resolved)
    }

    private fun applyTextViewColors(tv: TextView, palette: TelegramThemeEngine.ResolvedPalette) {
        val tagStr = tv.tag?.toString() ?: ""
        if (tagStr.contains("semantic_") ||
            tagStr == "status_correct" ||
            tagStr == "status_wrong" ||
            tagStr == "status_unattempted" ||
            tagStr == "timer_countdown"
        ) {
            appliedTextColors.remove(tv)
            return
        }

        val current = tv.currentTextColor
        val previouslyApplied = appliedTextColors[tv]
        val role = if (previouslyApplied != null && current == previouslyApplied.color) {
            previouslyApplied.role
        } else {
            // Preserve caller-owned custom colors; reclassify only known defaults.
            when {
                solidColor(tv) == palette.accent && current == palette.onAccent -> TextColorRole.ON_ACCENT
                current == Color.BLACK || current == Color.WHITE || current == 0xFF1A1D21.toInt() -> TextColorRole.PRIMARY
                current == 0xFF6C6C70.toInt() || current == 0xFFA1A1A6.toInt() || current == 0xFF6C6E6F.toInt() || current == 0x75FFFFFF -> TextColorRole.SECONDARY
                else -> null
            }
        }

        if (role == null) {
            appliedTextColors.remove(tv)
            return
        }
        val resolved = when (role) {
            TextColorRole.PRIMARY -> palette.textPrimary
            TextColorRole.SECONDARY -> palette.textSecondary
            TextColorRole.ON_ACCENT -> palette.onAccent
        }
        if (current != resolved) tv.setTextColor(resolved)
        appliedTextColors[tv] = AppliedTextColor(role, resolved)
    }
}

package com.eve.app.typography

import android.app.Activity
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.eve.app.util.FontManager

/**
 * Applies resolved typography to real Android View instances cleanly,
 * ensuring no synthetic double-bolding, authentic line heights, and zero layout flicker.
 */
object TypographyApplier {

    fun apply(
        view: TextView,
        style: TypographyResolver.ResolvedStyle,
        applySize: Boolean = false
    ) {
        val context = view.context
        val typeface = FontManager.getTypeface(context, style.fontFamily, style.weight)

        view.typeface = typeface
        view.paint.isFakeBoldText = false
        view.includeFontPadding = false

        if (applySize) {
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.textSizeSp)
            val density = context.resources.displayMetrics.scaledDensity
            val lineHeightPx = (style.lineHeightSp * density).toInt()
            if (lineHeightPx > 0) {
                TextViewCompat.setLineHeight(view, lineHeightPx)
            }
            view.letterSpacing = style.letterSpacing
        }
    }

    fun apply(
        view: TextView,
        config: TypographyConfig,
        screenKey: String?,
        elementKey: String?,
        componentRole: String? = null
    ): TypographyResolver.ResolvedStyle {
        val resolved = TypographyResolver.resolve(config, screenKey, elementKey, componentRole)
        apply(view, resolved, applySize = true)
        return resolved
    }

    /**
     * Applies the active published typography across eligible TextViews in an Activity.
     * Guaranteed to respect active exam session isolation and internal test harnesses.
     */
    fun applyToActivity(activity: Activity) {
        val repo = TypographyRepository.getInstance(activity)
        if (repo.isTestSessionActive()) return

        // Keep native screenshot/test harness typography independent of production rendering.
        val name = activity::class.java.simpleName
        if (name == "RepairVerificationActivity") return

        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val config = repo.getActiveConfig()
        val screenKey = TypographyRegistry.findScreenForActivity(activity)
        applyToHierarchy(root, config, screenKey)
    }

    fun applyToHierarchy(view: View, config: TypographyConfig, screenKey: String?) {
        if (view is TextView) {
            applyToView(view, config, screenKey)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToHierarchy(view.getChildAt(i), config, screenKey)
            }
        }
    }

    fun applyToView(view: TextView, config: TypographyConfig, screenKey: String?) {
        // Retired remote typography must not overwrite audited Telegram role views.
        if (view.tag == "eve_telegram_type") return
        val idName = try {
            if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else null
        } catch (_: Exception) {
            null
        }

        // Check if there is an explicit element override
        val elementOverride = if (idName != null && screenKey != null) {
            config.screenOverrides[screenKey]?.elementOverrides?.get(idName)
                ?: config.elementOverrides[idName]
        } else if (idName != null) {
            config.elementOverrides[idName]
        } else null

        val elementDesc = idName?.let { TypographyRegistry.findElement(it) }
        val role = elementDesc?.defaultRole

        val screenRoleOverride = if (role != null && screenKey != null) {
            config.screenOverrides[screenKey]?.roleOverrides?.get(role)
        } else null

        val compOverride = if (role != null) {
            config.componentOverrides[role]
        } else null

        val hasExplicitOverride = (elementOverride != null && hasAnyRule(elementOverride)) ||
                (screenRoleOverride != null && hasAnyRule(screenRoleOverride)) ||
                (compOverride != null && hasAnyRule(compOverride))

        if (hasExplicitOverride) {
            val resolved = TypographyResolver.resolve(
                config = config,
                screenKey = screenKey,
                elementKey = idName,
                componentRole = role
            )
            apply(view, resolved, applySize = true)
        } else {
            // No explicit element/component size override:
            // Apply authentic typeface for active font family if different from default,
            // while strictly preserving the approved XML baseline dimensions!
            val family = FontManager.FontFamily.fromId(config.fontFamily)
            if (family != FontManager.FontFamily.INTER) {
                val currentTf = view.typeface
                val weight = when {
                    currentTf?.isBold == true -> FontManager.FontWeight.BOLD
                    else -> FontManager.FontWeight.REGULAR
                }
                view.typeface = FontManager.getTypeface(view.context, family, weight)
                view.paint.isFakeBoldText = false
                view.includeFontPadding = false
            }
        }
    }

    private fun hasAnyRule(rule: StyleRule): Boolean {
        return rule.textSizeSp != null || rule.lineHeightSp != null ||
                rule.weight != null || rule.fontFamily != null || rule.letterSpacing != null
    }
}

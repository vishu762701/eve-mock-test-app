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
        style: TypographyResolver.ResolvedStyle
    ) {
        val context = view.context
        val typeface = FontManager.getTypeface(context, style.fontFamily, style.weight)

        view.typeface = typeface
        view.paint.isFakeBoldText = false
        view.includeFontPadding = false
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.textSizeSp)

        val density = context.resources.displayMetrics.scaledDensity
        val lineHeightPx = (style.lineHeightSp * density).toInt()
        if (lineHeightPx > 0) {
            TextViewCompat.setLineHeight(view, lineHeightPx)
        }

        view.letterSpacing = style.letterSpacing
    }

    fun apply(
        view: TextView,
        config: TypographyConfig,
        screenKey: String?,
        elementKey: String?,
        componentRole: String? = null
    ): TypographyResolver.ResolvedStyle {
        val resolved = TypographyResolver.resolve(config, screenKey, elementKey, componentRole)
        apply(view, resolved)
        return resolved
    }

    /**
     * Applies the active published typography across all eligible TextViews in an Activity.
     * Guaranteed to respect active exam session safety.
     */
    fun applyToActivity(activity: Activity) {
        val repo = TypographyRepository.getInstance(activity)
        if (repo.isTestSessionActive()) return

        // TypographyStudioActivity manages its own live preview instances
        if (activity::class.java.simpleName == "TypographyStudioActivity") return

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
        val idName = try {
            if (view.id != View.NO_ID) view.resources.getResourceEntryName(view.id) else null
        } catch (_: Exception) {
            null
        }

        val elementDesc = idName?.let { TypographyRegistry.findElement(it) }
        val role = elementDesc?.defaultRole ?: inferRoleFromView(view, idName)

        val resolved = TypographyResolver.resolve(
            config = config,
            screenKey = screenKey,
            elementKey = idName,
            componentRole = role
        )

        apply(view, resolved)
    }

    private fun inferRoleFromView(view: TextView, idName: String?): String {
        if (idName != null) {
            val lower = idName.lowercase()
            return when {
                lower.contains("largetitle") || lower.contains("bigscore") -> "large_title"
                lower.contains("title1") || lower.contains("tagline") -> "title_1"
                lower.contains("title") || lower.contains("heading1") -> "title_2"
                lower.contains("sectionheader") || lower.contains("title3") -> "title_3"
                lower.contains("headline") || lower.contains("heading") -> "headline"
                lower.contains("callout") || lower.contains("status") -> "callout"
                lower.contains("subheadline") || lower.contains("subtitle") || lower.contains("secondary") || lower.contains("desc") -> "subheadline"
                lower.contains("footnote") || lower.contains("note") -> "footnote"
                lower.contains("caption1") || lower.contains("timer") || lower.contains("counter") -> "caption_1"
                lower.contains("caption") -> "caption_2"
                lower.contains("button") || lower.startsWith("btn") -> "button"
                lower.contains("tab") -> "tab"
                lower.contains("chip") || lower.contains("badge") || lower.contains("pill") -> "chip"
                else -> "body"
            }
        }
        return if (view is Button) "button" else "body"
    }
}

package com.eve.app.typography

import android.util.TypedValue
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
}

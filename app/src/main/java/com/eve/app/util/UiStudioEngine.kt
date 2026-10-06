package com.eve.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.eve.app.data.model.uistudio.ComponentConfig
import com.google.android.material.card.MaterialCardView

/**
 * Robust runtime application engine for EVE UI Studio.
 * Applies schema-driven visual properties (Layout, Appearance, Typography, Visibility)
 * to native Android views safely with zero chance of crashes or blank screens.
 */
object UiStudioEngine {

    private const val TAG = "UiStudioEngine"

    /**
     * Safely parse a hex color string (#RRGGBB or #AARRGGBB).
     * Returns null if string is null, blank, or invalid hex.
     */
    fun parseColorSafe(hex: String?, fallback: Int? = null): Int? {
        if (hex.isNullOrBlank()) return fallback
        return try {
            val clean = hex.trim().removePrefix("#")
            when (clean.length) {
                6 -> {
                    val rgb = java.lang.Long.parseLong(clean, 16).toInt()
                    (0xFF000000.toInt()) or rgb
                }
                8 -> {
                    java.lang.Long.parseLong(clean, 16).toInt()
                }
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Converts DP to PX.
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Converts SP to PX.
     */
    fun spToPx(context: Context, sp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp.toFloat(),
            context.resources.displayMetrics
        )
    }

    /**
     * Applies general component configuration (Visibility, Layout, Appearance) to any View.
     */
    fun applyToView(view: View?, config: ComponentConfig?) {
        if (view == null || config == null) return

        try {
            // 1. Visibility
            if (!config.visible) {
                view.visibility = View.GONE
                return
            } else {
                view.visibility = View.VISIBLE
            }

            val context = view.context
            val density = context.resources.displayMetrics.density

            // 2. Layout Margins
            val params = view.layoutParams
            if (params is ViewGroup.MarginLayoutParams) {
                var marginsChanged = false
                config.layout.marginTop?.let {
                    params.topMargin = (it * density).toInt()
                    marginsChanged = true
                }
                config.layout.marginBottom?.let {
                    params.bottomMargin = (it * density).toInt()
                    marginsChanged = true
                }
                config.layout.marginStart?.let {
                    params.marginStart = (it * density).toInt()
                    params.leftMargin = params.marginStart
                    marginsChanged = true
                }
                config.layout.marginEnd?.let {
                    params.marginEnd = (it * density).toInt()
                    params.rightMargin = params.marginEnd
                    marginsChanged = true
                }
                if (marginsChanged) {
                    view.layoutParams = params
                }
            }

            // 3. Layout Padding
            val lay = config.layout
            if (lay.paddingTop != null || lay.paddingBottom != null || lay.paddingStart != null || lay.paddingEnd != null) {
                val pStart = lay.paddingStart?.let { (it * density).toInt() } ?: view.paddingStart
                val pTop = lay.paddingTop?.let { (it * density).toInt() } ?: view.paddingTop
                val pEnd = lay.paddingEnd?.let { (it * density).toInt() } ?: view.paddingEnd
                val pBottom = lay.paddingBottom?.let { (it * density).toInt() } ?: view.paddingBottom
                view.setPaddingRelative(pStart, pTop, pEnd, pBottom)
            }

            // 4. Appearance Opacity
            config.appearance.opacity?.let {
                view.alpha = it.coerceIn(0.0f, 1.0f)
            }

            // 5. Appearance Elevation
            config.appearance.elevation?.let {
                view.elevation = (it * density)
            }

            // 6. Card / Background Appearance
            val app = config.appearance
            if (view is MaterialCardView) {
                app.backgroundColor?.let { hex ->
                    parseColorSafe(hex)?.let { color ->
                        view.setCardBackgroundColor(color)
                    }
                }
                app.cornerRadius?.let { radiusDp ->
                    view.radius = radiusDp * density
                }
                app.strokeWidth?.let { widthDp ->
                    view.strokeWidth = (widthDp * density).toInt()
                }
                app.strokeColor?.let { strokeHex ->
                    parseColorSafe(strokeHex)?.let { color ->
                        view.strokeColor = color
                    }
                }
            } else if (app.backgroundColor != null || app.cornerRadius != null || app.strokeColor != null) {
                val drawable = (view.background as? GradientDrawable) ?: GradientDrawable()
                app.backgroundColor?.let { hex ->
                    parseColorSafe(hex)?.let { drawable.setColor(it) }
                }
                app.cornerRadius?.let { radiusDp ->
                    drawable.cornerRadius = radiusDp * density
                }
                val strokeWidthPx = (app.strokeWidth?.let { it * density } ?: 0f).toInt()
                val strokeColorVal = parseColorSafe(app.strokeColor) ?: Color.TRANSPARENT
                if (strokeWidthPx > 0 && strokeColorVal != Color.TRANSPARENT) {
                    drawable.setStroke(strokeWidthPx, strokeColorVal)
                }
                view.background = drawable
            }

            // 7. Typography (if view is TextView)
            if (view is TextView) {
                applyTypography(view, config)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed applying UI Studio config to view: ${config.id}", e)
        }
    }

    /**
     * Applies typography and text styling to a TextView.
     */
    fun applyTypography(textView: TextView?, config: ComponentConfig?) {
        if (textView == null || config == null) return

        try {
            val typo = config.typography
            typo.textColor?.let { hex ->
                parseColorSafe(hex)?.let { textView.setTextColor(it) }
            }
            typo.textSize?.let { sizeSp ->
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat())
            }
            typo.textStyle?.let { styleStr ->
                val style = when (styleStr.lowercase()) {
                    "bold" -> Typeface.BOLD
                    "italic" -> Typeface.ITALIC
                    "bold_italic" -> Typeface.BOLD_ITALIC
                    else -> Typeface.NORMAL
                }
                textView.setTypeface(textView.typeface, style)
            }
            typo.textAlign?.let { alignStr ->
                when (alignStr.lowercase()) {
                    "center" -> textView.gravity = (textView.gravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.CENTER_HORIZONTAL
                    "end", "right" -> textView.gravity = (textView.gravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.END
                    "start", "left" -> textView.gravity = (textView.gravity and Gravity.VERTICAL_GRAVITY_MASK) or Gravity.START
                }
            }

            // Title text override if specified
            config.content.title?.let { customText ->
                if (customText.isNotBlank()) {
                    textView.text = customText
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed applying typography: ${config.id}", e)
        }
    }

    /**
     * High-level binder for Home Screen Hero Card / Daily Challenge component.
     */
    fun applyHeroBanner(
        card: MaterialCardView?,
        titleView: TextView? = null,
        subtitleView: TextView? = null,
        config: ComponentConfig?
    ) {
        if (card == null || config == null) return
        applyToView(card, config)
        if (titleView != null) {
            applyTypography(titleView, config)
        }
        if (subtitleView != null && !config.content.subtitle.isNullOrBlank()) {
            subtitleView.text = config.content.subtitle
        }
    }

    /**
     * High-level binder for Streak Pill.
     */
    fun applyStreakPill(pillView: View?, textView: TextView? = null, config: ComponentConfig?) {
        if (pillView == null || config == null) return
        applyToView(pillView, config)
        if (textView != null) {
            applyTypography(textView, config)
        }
    }
}

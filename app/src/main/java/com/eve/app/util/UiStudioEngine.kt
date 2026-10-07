package com.eve.app.util

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import com.eve.app.data.model.uistudio.AnimationProperties
import com.eve.app.data.model.uistudio.ComponentConfig
import com.google.android.material.card.MaterialCardView

/**
 * Robust runtime application engine for EVE UI Studio.
 * Applies schema-driven visual properties (Layout, Appearance, Material/Glass Blur,
 * Typography, Visibility, Animations) to native Android views safely with zero chance
 * of crashes or blank screens.
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
     * Parses dimension strings ("match_parent", "wrap_content", or exact dp number) to px.
     */
    fun parseDimensionPx(context: Context, dimStr: String?, defaultVal: Int): Int {
        if (dimStr.isNullOrBlank()) return defaultVal
        return when (dimStr.trim().lowercase()) {
            "match_parent", "match" -> ViewGroup.LayoutParams.MATCH_PARENT
            "wrap_content", "wrap" -> ViewGroup.LayoutParams.WRAP_CONTENT
            else -> {
                val num = dimStr.trim().removeSuffix("dp").removeSuffix("px").toIntOrNull()
                if (num != null) dpToPx(context, num) else defaultVal
            }
        }
    }

    /**
     * Applies general component configuration (Visibility, Layout, Appearance, Material, Typography)
     * to any Android View.
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

            // 2. Enabled state
            view.isEnabled = config.enabled

            val context = view.context
            val density = context.resources.displayMetrics.density

            // 3. Layout Dimensions & Margins
            val params = view.layoutParams
            if (params != null) {
                var paramsChanged = false
                if (!config.layout.width.isNullOrBlank()) {
                    params.width = parseDimensionPx(context, config.layout.width, params.width)
                    paramsChanged = true
                }
                if (!config.layout.height.isNullOrBlank()) {
                    params.height = parseDimensionPx(context, config.layout.height, params.height)
                    paramsChanged = true
                }

                if (params is ViewGroup.MarginLayoutParams) {
                    config.layout.marginTop?.let {
                        params.topMargin = (it * density).toInt()
                        paramsChanged = true
                    }
                    config.layout.marginBottom?.let {
                        params.bottomMargin = (it * density).toInt()
                        paramsChanged = true
                    }
                    config.layout.marginStart?.let {
                        params.marginStart = (it * density).toInt()
                        params.leftMargin = params.marginStart
                        paramsChanged = true
                    }
                    config.layout.marginEnd?.let {
                        params.marginEnd = (it * density).toInt()
                        params.rightMargin = params.marginEnd
                        paramsChanged = true
                    }
                }

                if (paramsChanged) {
                    view.layoutParams = params
                }
            }

            // 4. Layout Padding
            val lay = config.layout
            if (lay.paddingTop != null || lay.paddingBottom != null || lay.paddingStart != null || lay.paddingEnd != null) {
                val pStart = lay.paddingStart?.let { (it * density).toInt() } ?: view.paddingStart
                val pTop = lay.paddingTop?.let { (it * density).toInt() } ?: view.paddingTop
                val pEnd = lay.paddingEnd?.let { (it * density).toInt() } ?: view.paddingEnd
                val pBottom = lay.paddingBottom?.let { (it * density).toInt() } ?: view.paddingBottom
                view.setPaddingRelative(pStart, pTop, pEnd, pBottom)
            }

            // 5. Appearance Opacity
            config.appearance.opacity?.let {
                view.alpha = it.coerceIn(0.0f, 1.0f)
            }

            // 6. Appearance Elevation
            config.appearance.elevation?.let {
                view.elevation = (it * density)
            }

            // 7. Card / Background Appearance & Material Tint
            val app = config.appearance
            val mat = config.material

            if (view is MaterialCardView) {
                // If material tint is provided, prefer tint color; else background color
                val effectiveColor = parseColorSafe(mat.tintColor) ?: parseColorSafe(app.backgroundColor)
                effectiveColor?.let { color ->
                    val finalColor = if (mat.tintOpacity != null) {
                        val alpha = (mat.tintOpacity * 255).toInt().coerceIn(0, 255)
                        (color and 0x00FFFFFF) or (alpha shl 24)
                    } else color
                    view.setCardBackgroundColor(finalColor)
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
            } else if (app.backgroundColor != null || app.cornerRadius != null || app.strokeColor != null || mat.tintColor != null) {
                val drawable = (view.background as? GradientDrawable) ?: GradientDrawable()
                val effectiveColor = parseColorSafe(mat.tintColor) ?: parseColorSafe(app.backgroundColor)
                effectiveColor?.let { color ->
                    val finalColor = if (mat.tintOpacity != null) {
                        val alpha = (mat.tintOpacity * 255).toInt().coerceIn(0, 255)
                        (color and 0x00FFFFFF) or (alpha shl 24)
                    } else color
                    drawable.setColor(finalColor)
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

            // 8. Typography (if view is TextView)
            if (view is TextView) {
                applyTypography(view, config)
            }

            // 9. Content (if view is ImageView)
            if (view is ImageView) {
                config.appearance.strokeColor?.let { hex ->
                    parseColorSafe(hex)?.let { view.setColorFilter(it) }
                }
            }

            // 10. Animation if configured
            if (config.animation.enabled) {
                playEntranceAnimation(view, config.animation)
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
            typo.maxLines?.let { lines ->
                if (lines > 0) textView.maxLines = lines
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
     * Executes safe entrance animation for a view based on animation schema.
     */
    fun playEntranceAnimation(view: View?, animConfig: AnimationProperties?) {
        if (view == null || animConfig == null || !animConfig.enabled) return

        val duration = animConfig.durationMs.coerceIn(50L, 2000L)
        val delay = animConfig.delayMs.coerceIn(0L, 2000L)
        val interpolator: TimeInterpolator = when (animConfig.interpolator.lowercase()) {
            "accelerate" -> AccelerateInterpolator()
            "decelerate" -> DecelerateInterpolator()
            "overshoot", "spring" -> OvershootInterpolator(1.2f)
            else -> AccelerateDecelerateInterpolator()
        }

        when (animConfig.type.lowercase()) {
            "fade" -> {
                view.alpha = 0f
                view.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator)
                    .start()
            }
            "scale" -> {
                view.scaleX = 0.85f
                view.scaleY = 0.85f
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator)
                    .start()
            }
            "fade_scale", "pop" -> {
                view.alpha = 0f
                view.scaleX = 0.88f
                view.scaleY = 0.88f
                view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator)
                    .start()
            }
            "slide" -> {
                view.translationY = 40f
                view.alpha = 0f
                view.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .setInterpolator(interpolator)
                    .start()
            }
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

    /**
     * High-level binder for Screen Background Color.
     * Precedence: Screen background > Global Design System appBackground.
     */
    fun applyScreenBackground(
        view: View?,
        screenBgHex: String?,
        designSystem: com.eve.app.data.model.uistudio.DesignSystemConfig? = null
    ) {
        if (view == null) return
        val color = parseColorSafe(screenBgHex)
            ?: parseColorSafe(designSystem?.appBackground)
            ?: return
        view.setBackgroundColor(color)
    }

    /**
     * High-level binder for the visible Test Timer capsule host and CircularTimerView.
     */
    fun applyTimerHost(
        hostView: View?,
        timerView: com.eve.app.ui.common.CircularTimerView?,
        config: ComponentConfig?
    ) {
        if (config == null) return
        hostView?.let { applyToView(it, config) }
        timerView?.let {
            it.setTimerColors(
                normalColorHex = config.appearance.strokeColor ?: config.typography.textColor,
                warningColorHex = config.states.disabledBackgroundColor,
                trackColorHex = config.appearance.strokeColor,
                textColorHex = config.typography.textColor
            )
        }
    }

    /**
     * High-level binder for Test Action Pills (Clear, Mark Review, Prev, Next).
     */
    fun applyTestActionPills(
        btnClear: View?,
        btnMarkReview: View?,
        btnPrev: View?,
        btnNext: View?,
        config: ComponentConfig?
    ) {
        if (config == null) return
        btnClear?.let { applyToView(it, config) }
        btnMarkReview?.let { applyToView(it, config) }
        btnPrev?.let { applyToView(it, config) }
        btnNext?.let { applyToView(it, config) }
    }

    /**
     * High-level binder for Question Options (A, B, C, D).
     */
    fun applyQuestionOptions(
        options: List<View>,
        config: ComponentConfig?
    ) {
        if (config == null) return
        for (opt in options) {
            applyToView(opt, config)
        }
    }

    // --- Formal Typed Component Adapters ---

    object TextAdapter {
        fun apply(textView: TextView?, config: ComponentConfig?) {
            if (textView == null || config == null) return
            applyToView(textView, config)
            applyTypography(textView, config)
        }
    }

    object ButtonAdapter {
        fun apply(view: View?, config: ComponentConfig?) {
            if (view == null || config == null) return
            applyToView(view, config)
            if (view is TextView) {
                applyTypography(view, config)
            }
        }
    }

    object TimerAdapter {
        fun apply(hostView: View?, timerView: com.eve.app.ui.common.CircularTimerView?, config: ComponentConfig?) {
            applyTimerHost(hostView, timerView, config)
        }
    }

    object QuestionOptionAdapter {
        fun apply(options: List<View>, config: ComponentConfig?, selectedIndex: Int = -1) {
            if (config == null) return
            for ((idx, opt) in options.withIndex()) {
                applyToView(opt, config)
                if (idx == selectedIndex) {
                    val selBg = parseColorSafe(config.states.selectedBackgroundColor)
                        ?: parseColorSafe("#16A34A") // EVE semantic green
                    opt.background = GradientDrawable().apply {
                        cornerRadius = dpToPx(opt.context, config.appearance.cornerRadius ?: 12).toFloat()
                        setColor(selBg ?: Color.parseColor("#16A34A"))
                        val strokeW = config.appearance.strokeWidth ?: 1
                        val strokeC = parseColorSafe(config.appearance.strokeColor) ?: Color.TRANSPARENT
                        setStroke(dpToPx(opt.context, strokeW), strokeC)
                    }
                    if (opt is TextView) {
                        config.states.selectedTextColor?.let { col ->
                            parseColorSafe(col)?.let { opt.setTextColor(it) }
                        }
                    }
                }
            }
        }
    }

    object TestActionAdapter {
        fun apply(pills: List<View>, config: ComponentConfig?) {
            if (config == null) return
            for (pill in pills) {
                applyToView(pill, config)
            }
        }
    }

    object ResultStatAdapter {
        fun apply(tile: View?, labelView: TextView? = null, valueView: TextView? = null, config: ComponentConfig?) {
            if (tile == null || config == null) return
            applyToView(tile, config)
            labelView?.let { applyTypography(it, config) }
            valueView?.let { applyTypography(it, config) }
        }
    }

    object NavigationAdapter {
        fun apply(navView: View?, config: ComponentConfig?) {
            if (navView == null || config == null) return
            applyToView(navView, config)
        }
    }

    object MaterialSurfaceAdapter {
        fun apply(view: View?, config: ComponentConfig?) {
            if (view == null || config == null) return
            applyToView(view, config)
        }
    }

    object ImageAdapter {
        fun apply(imageView: ImageView?, config: ComponentConfig?) {
            if (imageView == null || config == null) return
            applyToView(imageView, config)
            config.appearance.strokeColor?.let { hex ->
                parseColorSafe(hex)?.let { imageView.setColorFilter(it) }
            }
        }
    }

    object IconAdapter {
        fun apply(iconView: ImageView?, config: ComponentConfig?) {
            if (iconView == null || config == null) return
            applyToView(iconView, config)
            config.appearance.strokeColor?.let { hex ->
                parseColorSafe(hex)?.let { iconView.setColorFilter(it) }
            }
        }
    }

    object SwitchAdapter {
        fun apply(switchView: View?, config: ComponentConfig?) {
            if (switchView == null || config == null) return
            applyToView(switchView, config)
        }
    }

    object SliderAdapter {
        fun apply(sliderView: View?, config: ComponentConfig?) {
            if (sliderView == null || config == null) return
            applyToView(sliderView, config)
        }
    }

    object RowAdapter {
        fun apply(rowView: View?, titleView: TextView? = null, subtitleView: TextView? = null, config: ComponentConfig?) {
            if (rowView == null || config == null) return
            applyToView(rowView, config)
            titleView?.let { applyTypography(it, config) }
            subtitleView?.let {
                if (!config.content.subtitle.isNullOrBlank()) {
                    it.text = config.content.subtitle
                }
            }
        }
    }
}

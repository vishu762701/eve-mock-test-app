package com.eve.app.designsystem

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.Window
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.Px
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.eve.app.R
import com.eve.app.util.Constants
import com.eve.app.util.EveLiquidGlass
import com.eve.app.util.EveMotionHelper
import com.eve.app.util.EveShapes
import com.eve.app.util.EveTouchHelper
import com.eve.app.util.ExamCategoryTintHelper
import com.eve.app.util.SquircleHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

/**
 * Eve Design System Foundation.
 *
 * Authoritative single entry point and architectural contract for all Eve
 * design tokens, component geometry, typography hierarchy, semantic colors,
 * spatial grid, elevation, motion, and tactile feedback.
 */
object EveDesignSystem {

    /**
     * Semantic and adaptive color tokens across Light and Dark themes.
     */
    object Colors {

        fun isDark(context: Context): Boolean {
            val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return uiMode == Configuration.UI_MODE_NIGHT_YES
        }

        fun resolveThemeColor(context: Context, @AttrRes attrResId: Int): Int {
            val typedValue = TypedValue()
            return if (context.theme.resolveAttribute(attrResId, typedValue, true)) {
                if (typedValue.resourceId != 0) {
                    ContextCompat.getColor(context, typedValue.resourceId)
                } else {
                    typedValue.data
                }
            } else {
                Color.TRANSPARENT
            }
        }

        // Screen Canvases (ADR-007, ADR-009, ADR-013)
        @ColorInt
        fun systemBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_system_background)

        @ColorInt
        fun systemGroupedBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_system_grouped_background)

        // Semantic Surface Hierarchy (ADR-008, ADR-009)
        @ColorInt
        fun secondarySystemBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_secondary_system_background)

        @ColorInt
        fun tertiarySystemBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tertiary_system_background)

        @ColorInt
        fun secondarySystemGroupedBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_secondary_system_grouped_background)

        @ColorInt
        fun tertiarySystemGroupedBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tertiary_system_grouped_background)

        @ColorInt
        fun cardBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_card_bg)

        @ColorInt
        fun contentCardBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_secondary_background)

        // Label Typography Roles (ADR-008)
        @ColorInt
        fun labelPrimary(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_label_primary)

        @ColorInt
        fun labelSecondary(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_label_secondary)

        @ColorInt
        fun labelTertiary(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_label_tertiary)

        @ColorInt
        fun labelQuaternary(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_label_quaternary)

        @ColorInt
        fun placeholderText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_placeholder_text)

        @ColorInt
        fun headerInkText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_header_ink_text)

        // Authoritative Locked Color Hex Constants (ADR-014)
        const val HEX_LIGHT_CANVAS = "#FFFFFF"
        const val HEX_DARK_CANVAS = "#000000"
        const val HEX_LIGHT_CARDS = "#F2F2F7"
        const val HEX_DARK_CARDS = "#1C1C1E"
        const val HEX_LIGHT_ACCENT = "#0088FF"
        const val HEX_DARK_ACCENT = "#0091FF"
        const val HEX_LIGHT_SUCCESS = "#34C759"
        const val HEX_DARK_SUCCESS = "#30D158"
        const val HEX_LIGHT_ERROR = "#FF383C"
        const val HEX_DARK_ERROR = "#FF4245"
        const val HEX_LIGHT_WARNING = "#FF8D28"
        const val HEX_DARK_WARNING = "#FF9230"
        const val HEX_LIGHT_SECONDARY_TEXT = "#50505A"
        const val HEX_DARK_SECONDARY_TEXT = "#C5C5CC"

        // Accent & Interactive Blue (ADR-008, ADR-009)
        @ColorInt
        fun systemBlue(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_system_blue)

        @ColorInt
        fun accentText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_accent_text)

        @ColorInt
        fun accentContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_accent_container)

        @ColorInt
        fun onAccentContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_on_accent_container)

        // Authoritative System Status Colors (ADR-014)
        @ColorInt
        fun systemGreen(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_system_green)

        @ColorInt
        fun systemRed(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_system_red)

        @ColorInt
        fun systemOrange(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_system_orange)

        @ColorInt
        fun statusSuccess(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_status_success)

        @ColorInt
        fun statusError(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_status_error)

        @ColorInt
        fun statusWarning(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_status_warning)

        // Protected Test State & Semantic Feedback Colors (Invariants)
        @ColorInt
        fun statusRightFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tile_right_fill)

        @ColorInt
        fun statusRightText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tile_right_text)

        @ColorInt
        fun statusWrongFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tile_wrong_fill)

        @ColorInt
        fun statusWrongText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tile_wrong_text)

        @ColorInt
        fun statusReviewFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tile_medium_fill)

        @ColorInt
        fun statusReviewText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_tile_medium_text)

        @ColorInt
        fun destructiveText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_destructive_text)

        @ColorInt
        fun disabledFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_disabled_fill)

        @ColorInt
        fun disabledText(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_disabled_text)

        // Strokes, Borders & Separators
        @ColorInt
        fun shapeBorder(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_shape_border)

        @ColorInt
        fun separator(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_separator)

        @ColorInt
        fun opaqueSeparator(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_opaque_separator)

        // Material Tints
        @ColorInt
        fun materialRegular(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_material_regular)

        @ColorInt
        fun materialThin(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_material_thin)

        @ColorInt
        fun materialUltraThin(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_material_ultra_thin)

        @ColorInt
        fun materialThick(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_material_thick)

        @ColorInt
        fun drawerScrim(context: Context): Int =
            ContextCompat.getColor(context, R.color.eve_drawer_scrim)
    }

    /**
     * Canonical Apple iOS 26 dynamic typography tokens and helpers.
     */
    object Typography {
        @StyleRes val LargeTitle: Int = R.style.TextAppearance_Eve_LargeTitle
        @StyleRes val Title1: Int = R.style.TextAppearance_Eve_Title1
        @StyleRes val Title2: Int = R.style.TextAppearance_Eve_Title2
        @StyleRes val Title3: Int = R.style.TextAppearance_Eve_Title3
        @StyleRes val Headline: Int = R.style.TextAppearance_Eve_Headline
        @StyleRes val Body: Int = R.style.TextAppearance_Eve_Body
        @StyleRes val Callout: Int = R.style.TextAppearance_Eve_Callout
        @StyleRes val Subheadline: Int = R.style.TextAppearance_Eve_Subheadline
        @StyleRes val SubheadlineMedium: Int = R.style.TextAppearance_Eve_Subheadline_Medium
        @StyleRes val Footnote: Int = R.style.TextAppearance_Eve_Footnote
        @StyleRes val FootnoteMedium: Int = R.style.TextAppearance_Eve_Footnote_Medium
        @StyleRes val Caption1: Int = R.style.TextAppearance_Eve_Caption1
        @StyleRes val Caption1Medium: Int = R.style.TextAppearance_Eve_Caption1_Medium
        @StyleRes val Caption2: Int = R.style.TextAppearance_Eve_Caption2
        @StyleRes val Eyebrow: Int = R.style.TextAppearance_Eve_Eyebrow
        @StyleRes val Search: Int = R.style.TextAppearance_Eve_Search

        // Emphasized & Variant Styles
        @StyleRes val Title1Bold: Int = R.style.TextAppearance_Eve_Title1_Bold
        @StyleRes val Title2Bold: Int = R.style.TextAppearance_Eve_Title2_Bold
        @StyleRes val Title3Semibold: Int = R.style.TextAppearance_Eve_Title3_Semibold

        // Authoritative Locked Text Hierarchy Roles (ADR-014)
        @StyleRes val Title: Int = Title1Bold           // 28sp bold -> Title 1
        @StyleRes val Heading: Int = Title3Semibold     // 20sp semibold -> Title 3
        @StyleRes val Secondary: Int = Subheadline      // 15sp regular -> Subheadline
        @StyleRes val Caption: Int = Footnote           // 13sp regular -> Footnote

        fun applyStyle(textView: TextView, @StyleRes styleResId: Int) {
            TextViewCompat.setTextAppearance(textView, styleResId)
        }

        fun sansSerif(): Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        fun sansSerifMedium(): Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    /**
     * Geometry, corner radii, and outline shapes (ADR-009).
     */
    object Shapes {
        const val RADIUS_CAPSULE_PERCENT = 50f
        const val RADIUS_PILL_DP = 999f
        const val RADIUS_CARD_DP = 26f
        const val RADIUS_SHEET_DP = 34f
        const val RADIUS_CONCENTRIC_CARD_DP = 8f
        const val RADIUS_MIN_DP = 4f

        fun cardRadius(resources: Resources): Float =
            resources.getDimension(R.dimen.eve_radius_card)

        fun sheetRadius(resources: Resources): Float =
            resources.getDimension(R.dimen.eve_radius_bottom_sheet)

        fun concentricRadius(parentDp: Float, paddingDp: Float): Float =
            EveShapes.concentric(parentDp, paddingDp)

        fun concentricRadius(resources: Resources, @DimenRes parent: Int, @DimenRes padding: Int): Float =
            EveShapes.concentric(resources, parent, padding)

        fun capsuleRadius(heightPx: Float): Float =
            EveShapes.capsule(heightPx)

        fun applyCardSquircle(view: View) {
            val radius = cardRadius(view.resources)
            SquircleHelper.applySquircleOutline(view, radius)
        }

        fun applyCapsuleOutline(view: View) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    val r = v.height / 2f
                    outline.setRoundRect(0, 0, v.width, v.height, r)
                }
            }
            view.clipToOutline = true
        }

        fun applyConcentricCardOutline(view: View, parentPaddingDp: Float = 18f) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    val density = v.resources.displayMetrics.density
                    val radiusPx = concentricRadius(RADIUS_CARD_DP, parentPaddingDp) * density
                    outline.setRoundRect(0, 0, v.width, v.height, radiusPx)
                }
            }
            view.clipToOutline = true
        }
    }

    /**
     * 4dp spatial grid and component constraint dimensions.
     */
    object Spacing {
        const val SPACE_4_DP = 4
        const val SPACE_8_DP = 8
        const val SPACE_12_DP = 12
        const val SPACE_16_DP = 16
        const val SPACE_20_DP = 20
        const val SPACE_24_DP = 24
        const val SPACE_32_DP = 32

        const val MIN_TOUCH_TARGET_DP = 48
        const val MIN_LIST_ROW_HEIGHT_DP = 52
        const val MIN_SEGMENTED_HEIGHT_DP = 56

        fun dpToPx(resources: Resources, dp: Int): Int =
            Math.round(dp * resources.displayMetrics.density)

        fun pxToDp(resources: Resources, px: Int): Float =
            px / resources.displayMetrics.density
    }

    /**
     * Elevation specification in Light and Dark modes.
     */
    object Elevation {
        fun cardElevation(context: Context): Float {
            val isDark = Colors.isDark(context)
            val resId = if (isDark) R.dimen.eve_card_elevation_dark else R.dimen.eve_card_elevation_light
            return context.resources.getDimension(resId)
        }

        fun homeCardElevation(context: Context): Float =
            context.resources.getDimension(R.dimen.eve_home_card_elevation)
    }

    /**
     * Motion durations, interpolators, spring physics, and tactile feedback.
     */
    object Motion {
        const val DURATION_QUICK_MS = 150L
        const val DURATION_DISMISS_MS = 220L
        const val DURATION_STANDARD_MS = EveMotionHelper.DURATION_STANDARD_MS
        const val DURATION_SPRING_MS = EveMotionHelper.DURATION_SPRING_MS

        // Authoritative Spring Physics & Tactile Constraints (ADR-016)
        const val DAMPING_RATIO_SPRING = 0.8f
        const val SCALE_PRESS_FEEDBACK = 0.97f

        val standardInterpolator: PathInterpolator = EveMotionHelper.standardInterpolator
        val springInterpolator: OvershootInterpolator = EveMotionHelper.springInterpolator

        fun areAnimationsEnabled(context: Context): Boolean =
            EveMotionHelper.areAnimationsEnabled(context)

        fun attachTactileFeedback(
            view: View,
            pressScale: Float = EveTouchHelper.DEFAULT_PRESS_SCALE,
            pressAlpha: Float = EveTouchHelper.DEFAULT_PRESS_ALPHA,
            enableHaptic: Boolean = true,
            onClick: (() -> Unit)? = null
        ) {
            EveTouchHelper.attachTactileFeedback(view, pressScale, pressAlpha, enableHaptic, onClick)
        }

        fun animateSpringEntrance(view: View, startScale: Float = 0.85f, delayMs: Long = 0L) {
            EveMotionHelper.animateSpringEntrance(view, startScale, delayMs)
        }

        fun animateDismiss(view: View, onEnd: () -> Unit) {
            EveMotionHelper.animateDismiss(view, onEnd)
        }
    }

    /**
     * Unified Liquid Glass and backdrop material specifications (ADR-012).
     */
    object Materials {
        const val SPEC_REGULAR_GLASS = EveLiquidGlass.MATERIAL_ID

        fun isCrossWindowBlurSupported(context: Context): Boolean =
            EveLiquidGlass.isCrossWindowBlurSupported(context)

        fun applyRegularGlassToWindow(window: Window, @Px blurRadiusPx: Int = 80) {
            EveLiquidGlass.applyToWindow(window, blurRadiusPx)
        }
    }

    /**
     * Component builders and styling helpers ensuring strict design system fidelity.
     */
    object Components {

        /**
         * Applies the canonical Eve Card foundation to a MaterialCardView.
         */
        fun applyCard(card: MaterialCardView, isGrouped: Boolean = false) {
            val ctx = card.context
            val radius = Shapes.cardRadius(card.resources)
            card.radius = radius
            card.strokeColor = Colors.shapeBorder(ctx)
            card.strokeWidth = 0
            val bgRes = if (isGrouped) {
                R.color.eve_secondary_system_grouped_background
            } else {
                R.color.eve_secondary_background
            }
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, bgRes))
            card.cardElevation = Elevation.cardElevation(ctx)
        }

        /**
         * Applies concentric corner geometry to a nested icon container tile.
         */
        fun applyConcentricIconContainer(
            container: MaterialCardView,
            categoryTint: ExamCategoryTintHelper.TintColors? = null,
            parentPaddingDp: Float = 18f
        ) {
            val ctx = container.context
            val concentricDp = Shapes.concentricRadius(Shapes.RADIUS_CARD_DP, parentPaddingDp)
            val density = container.resources.displayMetrics.density
            container.radius = concentricDp * density
            container.strokeColor = Colors.shapeBorder(ctx)
            container.strokeWidth = 0
            container.cardElevation = 0f
            if (categoryTint != null) {
                container.setCardBackgroundColor(ContextCompat.getColor(ctx, categoryTint.bgRes))
            }
        }

        /**
         * Builds a canonical Apple iOS 26 category filter chip.
         * Enforces 40dp min visual height, 48dp touch delegation, capsule geometry,
         * Footnote.Medium (13sp) typography, and semantic color selectors.
         */
        fun createCategoryChip(
            context: Context,
            category: String,
            isSelected: Boolean,
            onSelect: () -> Unit
        ): Chip = Chip(context).apply {
            text = category
            isCheckable = true
            isChecked = isSelected
            val density = resources.displayMetrics.density
            chipStrokeWidth = 0f
            chipStrokeColor = ContextCompat.getColorStateList(context, R.color.eve_shape_border)
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.selector_home_category_chip_bg)
            setTextColor(ContextCompat.getColorStateList(context, R.color.selector_home_category_chip_text))
            isCheckedIconVisible = false
            setEnsureMinTouchTargetSize(false) // Parent container delegates >= 48dp touch region
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typography.sansSerifMedium()
            chipStartPadding = 12f * density
            chipEndPadding = 12f * density
            textStartPadding = 0f
            textEndPadding = 0f
            setPaddingRelative(paddingStart, Math.round(8f * density), paddingEnd, Math.round(8f * density))
            chipMinHeight = maxOf(40f * density, paint.fontMetrics.run { bottom - top } + 16f * density)
            chipCornerRadius = chipMinHeight / 2f
            minWidth = Math.round(64f * density)
            minHeight = Math.round(chipMinHeight)
            maxWidth = Math.round(maxOf(64, resources.configuration.screenWidthDp - 32) * density)
            ellipsize = android.text.TextUtils.TruncateAt.END
            contentDescription = category
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setOnClickListener { onSelect() }
        }

        /**
         * Styles the Home streak summary pill.
         */
        fun applyStreakPill(pillContainer: View, textView: TextView, summaryText: CharSequence) {
            val ctx = pillContainer.context
            textView.text = summaryText
            Typography.applyStyle(textView, Typography.FootnoteMedium)
            textView.setTextColor(Colors.labelPrimary(ctx))
        }

        /**
         * Styles the Admin Dashboard button on the Home screen.
         */
        fun applyAdminButton(button: MaterialButton) {
            val ctx = button.context
            button.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.selector_home_admin_surface)
            button.strokeColor = ContextCompat.getColorStateList(ctx, R.color.eve_shape_border)
            button.strokeWidth = Spacing.dpToPx(button.resources, 1)
            button.minHeight = Spacing.dpToPx(button.resources, 52)
        }

        /**
         * Styles and clips the Home banner surface to the shared 26dp card squircle.
         */
        fun applyHomeBannerSurface(bannerContainer: View, matteView: View) {
            Shapes.applyCardSquircle(bannerContainer)
            matteView.setBackgroundColor(Colors.secondarySystemBackground(bannerContainer.context))
        }

        /**
         * Tints the bare Home notification bell Lottie animation with primary label ink.
         */
        fun tintNotificationBell(view: com.airbnb.lottie.LottieAnimationView) {
            val ink = Colors.labelPrimary(view.context)
            fun applyInk() {
                view.addValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.COLOR,
                    com.airbnb.lottie.value.LottieValueCallback(ink)
                )
                view.addValueCallback(
                    com.airbnb.lottie.model.KeyPath("**"),
                    com.airbnb.lottie.LottieProperty.STROKE_COLOR,
                    com.airbnb.lottie.value.LottieValueCallback(ink)
                )
            }
            view.addLottieOnCompositionLoadedListener { applyInk() }
            applyInk()
        }

        /**
         * Applies Home top bar depth with adaptive cross-window blur support (ADR-016).
         */
        fun applyHomeTopBarDepth(topBarContainer: View, searchTopBar: View? = null) {
            val context = topBarContainer.context
            val hasBlur = Materials.isCrossWindowBlurSupported(context)
            val topBarBg = ContextCompat.getColor(
                context,
                if (hasBlur) R.color.eve_home_blur_tint else R.color.eve_home_solid_tint
            )
            topBarContainer.setBackgroundColor(topBarBg)
            searchTopBar?.setBackgroundColor(topBarBg)
        }
    }
}

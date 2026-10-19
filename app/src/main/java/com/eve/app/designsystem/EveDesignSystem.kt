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
            ContextCompat.getColor(context, R.color.bg_app)

        @ColorInt
        fun systemGroupedBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.bg_app)

        // Semantic Surface Hierarchy (ADR-008, ADR-009)
        @ColorInt
        fun secondarySystemBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface_inset)

        @ColorInt
        fun tertiarySystemBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface_inset)

        @ColorInt
        fun secondarySystemGroupedBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface_inset)

        @ColorInt
        fun tertiarySystemGroupedBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface_inset)

        @ColorInt
        fun cardBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface)

        @ColorInt
        fun contentCardBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface_inset)

        // Label Typography Roles (ADR-008)
        @ColorInt
        fun labelPrimary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

        @ColorInt
        fun labelSecondary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        @ColorInt
        fun labelTertiary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        @ColorInt
        fun labelQuaternary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        @ColorInt
        fun placeholderText(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        @ColorInt
        fun headerInkText(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

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

        // Authoritative Home Locked Color Hex Constants
        const val HEX_HOME_LIGHT_BG = "#F5F5F5"
        const val HEX_HOME_DARK_BG = "#000000"
        const val HEX_HOME_LIGHT_CARD = "#FFFFFF"
        const val HEX_HOME_DARK_CARD = "#1C1C1E"
        const val HEX_HOME_LIGHT_TEXT_PRIMARY = "#000000"
        const val HEX_HOME_DARK_TEXT_PRIMARY = "#FFFFFF"
        const val HEX_HOME_LIGHT_TEXT_SECONDARY = "#6C6C70"
        const val HEX_HOME_DARK_TEXT_SECONDARY = "#A1A1A6"
        const val HEX_HOME_LIGHT_HAIRLINE = "#1A000000"
        const val HEX_HOME_DARK_HAIRLINE = "#24FFFFFF"
        const val HEX_HOME_LIGHT_TAB_TRACK = "#29787880"
        const val HEX_HOME_DARK_TAB_TRACK = "#52787880"
        const val HEX_HOME_LIGHT_ACCENT = "#000000"
        const val HEX_HOME_DARK_ACCENT = "#FFFFFF"
        const val HEX_HOME_LIGHT_ON_ACCENT = "#FFFFFF"
        const val HEX_HOME_DARK_ON_ACCENT = "#000000"
        const val HEX_HOME_LIGHT_SUCCESS = "#34C759"
        const val HEX_HOME_DARK_SUCCESS = "#30D158"
        const val HEX_HOME_LIGHT_ERROR = "#FF383C"
        const val HEX_HOME_DARK_ERROR = "#FF4245"
        const val HEX_HOME_LIGHT_WARNING = "#FF8D28"
        const val HEX_HOME_DARK_WARNING = "#FF9230"
        const val HEX_HOME_LIGHT_FLOATING_BAR_BG = "#FFFFFF"
        const val HEX_HOME_DARK_FLOATING_BAR_BG = "#1C1C1E"

        // Authoritative App-Wide Locked Color Hex Constants (ADR-020)
        const val HEX_APP_LIGHT_BG = "#F5F5F5"
        const val HEX_APP_DARK_BG = "#000000"
        const val HEX_APP_LIGHT_CARD = "#FFFFFF"
        const val HEX_APP_DARK_CARD = "#1C1C1E"
        const val HEX_APP_LIGHT_TEXT_PRIMARY = "#000000"
        const val HEX_APP_DARK_TEXT_PRIMARY = "#FFFFFF"
        const val HEX_APP_LIGHT_TEXT_SECONDARY = "#6C6C70"
        const val HEX_APP_DARK_TEXT_SECONDARY = "#A1A1A6"
        const val HEX_APP_LIGHT_HAIRLINE = "#1A000000"
        const val HEX_APP_DARK_HAIRLINE = "#24FFFFFF"
        const val HEX_APP_LIGHT_TAB_TRACK = "#29787880"
        const val HEX_APP_DARK_TAB_TRACK = "#52787880"
        const val HEX_APP_LIGHT_ACCENT = "#000000"
        const val HEX_APP_DARK_ACCENT = "#FFFFFF"
        const val HEX_APP_LIGHT_ON_ACCENT = "#FFFFFF"
        const val HEX_APP_DARK_ON_ACCENT = "#000000"
        const val HEX_APP_LIGHT_SUCCESS = "#34C759"
        const val HEX_APP_DARK_SUCCESS = "#30D158"
        const val HEX_APP_LIGHT_ERROR = "#FF383C"
        const val HEX_APP_DARK_ERROR = "#FF4245"
        const val HEX_APP_LIGHT_WARNING = "#FF8D28"
        const val HEX_APP_DARK_WARNING = "#FF9230"
        const val HEX_APP_LIGHT_FLOATING_BAR_BG = "#FFFFFF"
        const val HEX_APP_DARK_FLOATING_BAR_BG = "#1C1C1E"
        const val HEX_APP_LIGHT_CARD_SHADOW = "#1A000000"
        const val HEX_APP_DARK_CARD_SHADOW = "#00000000"

        // Home Locked Color Accessors
        @ColorInt
        fun homeBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.bg_app)

        @ColorInt
        fun homeCard(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface)

        @ColorInt
        fun homeTextPrimary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

        @ColorInt
        fun homeTextSecondary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        @ColorInt
        fun homeHairline(context: Context): Int =
            ContextCompat.getColor(context, R.color.hairline)

        @ColorInt
        fun homeTabTrack(context: Context): Int =
            ContextCompat.getColor(context, R.color.track)

        @ColorInt
        fun homeAccent(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

        @ColorInt
        fun homeOnAccent(context: Context): Int =
            ContextCompat.getColor(context, R.color.on_accent)

        @ColorInt
        fun homeSuccess(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun homeError(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun homeWarning(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        @ColorInt
        fun homeFloatingBarBg(context: Context): Int =
            ContextCompat.getColor(context, R.color.glass_fallback)

        @ColorInt
        fun homeCardShadow(context: Context): Int =
            ContextCompat.getColor(context, R.color.card_shadow)

        // App-Wide Locked Color Accessors (ADR-019)
        @ColorInt
        fun appBackground(context: Context): Int =
            ContextCompat.getColor(context, R.color.bg_app)

        @ColorInt
        fun appCard(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface)

        @ColorInt
        fun appTextPrimary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

        @ColorInt
        fun appTextSecondary(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        @ColorInt
        fun appHairline(context: Context): Int =
            ContextCompat.getColor(context, R.color.hairline)

        @ColorInt
        fun appTabTrack(context: Context): Int =
            ContextCompat.getColor(context, R.color.track)

        @ColorInt
        fun appAccent(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

        @ColorInt
        fun appOnAccent(context: Context): Int =
            ContextCompat.getColor(context, R.color.on_accent)

        @ColorInt
        fun appSuccess(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun appError(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun appWarning(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        @ColorInt
        fun appFloatingBarBg(context: Context): Int =
            ContextCompat.getColor(context, R.color.glass_fallback)

        @ColorInt
        fun appCardShadow(context: Context): Int =
            ContextCompat.getColor(context, R.color.design_role_color_card_shadow)

        // Authoritative App-Wide Color Tokens (ADR-020)
        @ColorInt
        fun bgApp(context: Context): Int =
            ContextCompat.getColor(context, R.color.bg_app)

        @ColorInt
        fun surface(context: Context): Int =
            ContextCompat.getColor(context, R.color.surface)

        @ColorInt
        fun systemRedToken(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun systemGreenToken(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun systemBlueToken(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_blue)

        @ColorInt
        fun systemIndigo(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_indigo)

        @ColorInt
        fun systemOrangeToken(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        @ColorInt
        fun systemPurple(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_purple)

        @ColorInt
        fun systemYellow(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_yellow)

        @ColorInt
        fun systemRedContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red_container)

        @ColorInt
        fun systemGreenContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green_container)

        @ColorInt
        fun systemBlueContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_blue_container)

        @ColorInt
        fun systemIndigoContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_indigo_container)

        @ColorInt
        fun systemOrangeContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange_container)

        @ColorInt
        fun systemPurpleContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_purple_container)

        @ColorInt
        fun systemYellowContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_yellow_container)

        // Accent & Interactive Blue (ADR-008, ADR-009)
        @ColorInt
        fun systemBlue(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_blue)

        @ColorInt
        fun accentText(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_blue)

        @ColorInt
        fun accentContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_blue_container)

        @ColorInt
        fun onAccentContainer(context: Context): Int =
            ContextCompat.getColor(context, R.color.on_accent)

        // Authoritative System Status Colors (ADR-014)
        @ColorInt
        fun systemGreen(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun systemRed(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun systemOrange(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        @ColorInt
        fun statusSuccess(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun statusError(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun statusWarning(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        // Protected Test State & Semantic Feedback Colors (Invariants)
        @ColorInt
        fun statusRightFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun statusRightText(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_green)

        @ColorInt
        fun statusWrongFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun statusWrongText(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun statusReviewFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        @ColorInt
        fun statusReviewText(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_orange)

        @ColorInt
        fun destructiveText(context: Context): Int =
            ContextCompat.getColor(context, R.color.system_red)

        @ColorInt
        fun disabledFill(context: Context): Int =
            ContextCompat.getColor(context, R.color.track)

        @ColorInt
        fun disabledText(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_secondary)

        // Strokes, Borders & Separators
        @ColorInt
        fun shapeBorder(context: Context): Int =
            ContextCompat.getColor(context, R.color.hairline)

        @ColorInt
        fun separator(context: Context): Int =
            ContextCompat.getColor(context, R.color.hairline)

        @ColorInt
        fun opaqueSeparator(context: Context): Int =
            ContextCompat.getColor(context, R.color.hairline)

        // Material Tints
        @ColorInt
        fun materialRegular(context: Context): Int =
            ContextCompat.getColor(context, R.color.glass_fallback)

        @ColorInt
        fun materialThin(context: Context): Int =
            ContextCompat.getColor(context, R.color.glass_fallback)

        @ColorInt
        fun materialUltraThin(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)

        @ColorInt
        fun materialThick(context: Context): Int =
            ContextCompat.getColor(context, R.color.glass_fallback)

        @ColorInt
        fun drawerScrim(context: Context): Int =
            ContextCompat.getColor(context, R.color.text_primary)
    }

    /**
     * Canonical Apple iOS 26 dynamic typography tokens and helpers.
     */
    object Typography {
        @StyleRes val LargeTitle: Int = R.style.TextAppearance_Eve_Token_Title
        @StyleRes val Title1: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val Title2: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val Title3: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val Headline: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val Body: Int = R.style.TextAppearance_Eve_Token_Body
        @StyleRes val Callout: Int = R.style.TextAppearance_Eve_Token_Body
        @StyleRes val Subheadline: Int = R.style.TextAppearance_Eve_Token_Body
        @StyleRes val SubheadlineMedium: Int = R.style.TextAppearance_Eve_Token_Body
        @StyleRes val Footnote: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val FootnoteMedium: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val Caption1: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val Caption1Medium: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val Caption2: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val Eyebrow: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val Search: Int = R.style.TextAppearance_Eve_Token_Body

        // Emphasized & Variant Styles
        @StyleRes val Title1Bold: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val Title2Bold: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val Title3Semibold: Int = R.style.TextAppearance_Eve_Token_Heading

        // Authoritative Locked Text Hierarchy Roles (ADR-014)
        @StyleRes val Title: Int = Title1Bold           // 28sp bold -> Title 1
        @StyleRes val Heading: Int = Title3Semibold     // 20sp semibold -> Title 3
        @StyleRes val Secondary: Int = Subheadline      // 15sp regular -> Subheadline
        @StyleRes val Caption: Int = Footnote           // 13sp regular -> Footnote

        // Eve Design System Locked Foundation Typography (Home Screen)
        @StyleRes val HomePageTitle: Int = R.style.TextAppearance_Eve_Token_Title
        @StyleRes val HomeCardTitle: Int = R.style.TextAppearance_Eve_Token_Heading
        @StyleRes val HomeCardBody: Int = R.style.TextAppearance_Eve_Token_Body
        @StyleRes val HomeSecondary: Int = R.style.TextAppearance_Eve_Token_Secondary
        @StyleRes val HomeTabLabel: Int = R.style.TextAppearance_Eve_Token_Body
        @StyleRes val HomePillLabel: Int = R.style.TextAppearance_Eve_Token_Secondary

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

        // Eve Design System Locked Foundation Shapes & Spacing (Home Screen)
        const val HOME_CARD_RADIUS_DP = 24f
        const val HOME_CARD_PADDING_DP = 12f
        const val HOME_CARD_GAP_DP = 8f
        const val HOME_SCREEN_MARGIN_DP = 16f
        const val HOME_SECTION_GAP_DP = 16f
        const val HOME_TAB_HEIGHT_DP = 40f
        const val HOME_TAB_INNER_PADDING_DP = 3f
        const val HOME_PILL_HEIGHT_DP = 32f
        const val HOME_PILL_PADDING_HORIZONTAL_DP = 12f
        const val HOME_PILL_BORDER_WIDTH_DP = 1f
        const val HOME_BOTTOM_BAR_MARGIN_HORIZONTAL_DP = 12f
        const val HOME_BOTTOM_BAR_MARGIN_BOTTOM_DP = 10f
        const val HOME_BOTTOM_BAR_HEIGHT_DP = 54f
        const val HOME_BOTTOM_BAR_ICON_SIZE_DP = 22f
        const val HOME_HAIRLINE_STROKE_DP = 0.5f
        const val HOME_MIN_TOUCH_TARGET_DP = 48f

        fun homeCardRadius(resources: Resources): Float =
            resources.getDimension(R.dimen.design_radius_card)

        fun cardRadius(resources: Resources): Float =
            resources.getDimension(R.dimen.design_radius_card)

        fun sheetRadius(resources: Resources): Float =
            resources.getDimension(R.dimen.design_radius_card)

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
        fun cardElevation(context: Context): Float = context.resources.getDimension(R.dimen.design_card_elevation)
        fun homeCardElevation(context: Context): Float = cardElevation(context)

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
            applyHomeCard(card)
            if (isGrouped) {
                card.radius = card.resources.getDimension(R.dimen.design_radius_inner)
                card.setCardBackgroundColor(ContextCompat.getColor(card.context, R.color.surface_inset))
                card.cardElevation = 0f
            }
        }

        /**
         * Applies concentric corner geometry to a nested icon container tile.
         */

        /**
         * Builds an Eve Design System category outline pill.
         * Enforces 32dp height, 12dp horizontal padding, fully rounded (16dp radius),
         * 1dp accent border, 14sp Medium DM Sans text, and at least 48dp touch target.
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
            DesignType.apply(this, DesignType.Role.CHIP)
            chipStrokeWidth = 0f
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.accent)
            setTextColor(ContextCompat.getColor(context, R.color.on_accent))
            setCheckedIconResource(R.drawable.ic_target_exam_checked)
            checkedIconTint = ContextCompat.getColorStateList(context, R.color.on_accent)
            isCheckedIconVisible = true
            setEnsureMinTouchTargetSize(true)
            chipMinHeight = resources.getDimension(R.dimen.design_pill_height)
            chipStartPadding = resources.getDimension(R.dimen.design_pill_padding)
            chipEndPadding = chipStartPadding
            textStartPadding = 0f
            textEndPadding = 0f
            shapeAppearanceModel = shapeAppearanceModel.withCornerSize(com.google.android.material.shape.RelativeCornerSize(.5f))
            maxWidth = resources.displayMetrics.widthPixels - 2 * resources.getDimensionPixelSize(R.dimen.design_screen_margin)
            DesignPress.bind(this)
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            contentDescription = category
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setOnClickListener { onSelect() }
        }

        /**
         * Applies the locked Eve Design System Card styling to a Home card.
         * Enforces 24dp radius, elevation (2dp light / 0dp dark), #FFFFFF / #161B24 card background,
         * 0dp stroke, and soft 10% black shadow in light mode.
         */
        fun applyHomeCard(card: MaterialCardView) {
            val ctx = card.context
            val res = card.resources
            card.radius = res.getDimension(R.dimen.design_radius_card)
            card.cardElevation = Elevation.homeCardElevation(ctx)
            card.maxCardElevation = Elevation.homeCardElevation(ctx)
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
            card.strokeWidth = 0
            card.strokeColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val shadowColor = ContextCompat.getColor(ctx, R.color.card_shadow)
                card.outlineAmbientShadowColor = shadowColor
                card.outlineSpotShadowColor = shadowColor
            }
        }

        /**
         * Styles the Home streak summary pill.
         */
        fun applyStreakPill(pillContainer: View, textView: TextView, summaryText: CharSequence) {
            val ctx = pillContainer.context
            textView.text = summaryText
            DesignType.apply(textView, DesignType.Role.CHIP)
            textView.setTextColor(Colors.homeOnAccent(ctx))
        }

        /**
         * Styles the Admin Dashboard button on the Home screen.
         */
        fun applyAdminButton(button: MaterialButton) {
            button.backgroundTintList = ContextCompat.getColorStateList(button.context, R.color.track)
            button.setTextColor(ContextCompat.getColorStateList(button.context, R.color.design_secondary_text))
            button.strokeWidth = 0
            button.minHeight = button.resources.getDimensionPixelSize(R.dimen.design_touch_min)
            DesignType.apply(button, DesignType.Role.BUTTON)
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
            topBarContainer.setBackgroundColor(Color.TRANSPARENT)
            searchTopBar?.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}

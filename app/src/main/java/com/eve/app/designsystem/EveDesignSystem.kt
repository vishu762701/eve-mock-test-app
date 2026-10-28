package com.eve.app.designsystem

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

/** Shared Home presentation; all values resolve from the root token resources. */
object EveDesignSystem {
    object Colors {
        fun surface(context: Context): Int = ContextCompat.getColor(context, R.color.surface)
        fun homeTextPrimary(context: Context): Int = ContextCompat.getColor(context, R.color.text_primary)
        fun homeTextSecondary(context: Context): Int = ContextCompat.getColor(context, R.color.text_secondary)
        fun homeOnAccent(context: Context): Int = ContextCompat.getColor(context, R.color.on_accent)
        fun secondarySystemBackground(context: Context): Int = ContextCompat.getColor(context, R.color.surface_inset)
        fun labelPrimary(context: Context): Int = ContextCompat.getColor(context, R.color.text_primary)
        fun statusRightText(context: Context): Int = ContextCompat.getColor(context, R.color.system_green_text)
        fun shapeBorder(context: Context): Int = ContextCompat.getColor(context, R.color.hairline)
        fun drawerScrim(context: Context): Int = ContextCompat.getColor(context, R.color.modal_scrim)
    }
    object Shapes {
        fun cardRadius(resources: Resources): Float = resources.getDimension(R.dimen.design_radius_card)
        fun applyCardSquircle(view: View) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, outline: Outline) {
                    outline.setRoundRect(0, 0, v.width, v.height, cardRadius(v.resources))
                }
            }
            view.clipToOutline = true
        }
    }
    object Elevation {
        fun homeCardElevation(context: Context): Float = context.resources.getDimension(R.dimen.design_card_elevation)
    }
    object Components {
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
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(context)
            // FilterTabsView.java:915-918,1448-1514 draws one shared 28dp
            // low-alpha tab indicator behind the title; tabs are not solid pills.
            // This Chip remains an EVE accessibility/selection host only.
            chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
            rippleColor = android.content.res.ColorStateList.valueOf(palette.filterTabSelector)
            setTextColor(
                android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        palette.filterTabActiveText,
                        palette.filterTabUnactiveText
                    )
                )
            )
            // Telegram FilterTabsView.java:388-400,649-659 measures title/counter only;
            // no selected checkmark consumes width. Only Home's category chip follows
            // this verified source property; TargetExamsBottomSheet keeps its checkmark.
            isCheckedIconVisible = false
            setEnsureMinTouchTargetSize(true)
            chipMinHeight = resources.getDimension(R.dimen.design_pill_height)
            chipStartPadding = resources.getDimension(R.dimen.design_pill_padding)
            chipEndPadding = chipStartPadding
            textStartPadding = 0f
            textEndPadding = 0f
            shapeAppearanceModel = shapeAppearanceModel.withCornerSize(com.google.android.material.shape.RelativeCornerSize(.5f))
            maxWidth = kotlin.math.round(resources.configuration.screenWidthDp * resources.displayMetrics.density).toInt() -
                2 * resources.getDimensionPixelSize(R.dimen.design_screen_margin)
            DesignPress.bind(this)
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            contentDescription = category
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setOnClickListener { onSelect() }
        }

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

        fun applyStreakPill(pillContainer: View, textView: TextView, summaryText: CharSequence) {
            val ctx = pillContainer.context
            textView.text = summaryText
            DesignType.apply(textView, DesignType.Role.CHIP)
            textView.setTextColor(Colors.homeOnAccent(ctx))
        }

        fun applyAdminButton(button: MaterialButton) {
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(button.context)
            applyAdminButtonPalette(button, palette)
        }

        fun applyAdminButtonPalette(button: MaterialButton, palette: com.eve.app.theme.TelegramThemeEngine.ResolvedPalette) {
            DesignType.apply(button, DesignType.Role.BUTTON)
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(palette.actionSelector)
            button.setTextColor(
                android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf(android.R.attr.state_enabled)
                    ),
                    intArrayOf(
                        palette.textSecondary,
                        palette.textPrimary
                    )
                )
            )
            button.strokeWidth = 0
            button.minHeight = button.resources.getDimensionPixelSize(R.dimen.design_touch_min)
        }

        fun applyHomeBannerSurface(bannerContainer: View, matteView: View) {
            Shapes.applyCardSquircle(bannerContainer)
            matteView.setBackgroundColor(Colors.secondarySystemBackground(bannerContainer.context))
        }

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

        fun applyHomeTopBarDepth(topBarContainer: View, searchTopBar: View? = null) {
            topBarContainer.setBackgroundColor(Color.TRANSPARENT)
            searchTopBar?.setBackgroundColor(Color.TRANSPARENT)
        }
    }
}

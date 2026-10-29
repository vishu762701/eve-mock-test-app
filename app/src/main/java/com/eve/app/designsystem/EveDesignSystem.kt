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
            // Owner override R05: Monochrome category filter pills
            // NIGHT selected: EXACT #FFFFFF bg, #000000 text/icons
            // DAY selected: EXACT #000000 bg, #FFFFFF text/icons
            // Unselected: quiet theme-neutral surfaces with WCAG AA readable text
            val selectedBg = ContextCompat.getColor(context, R.color.category_pill_selected_bg)
            val unselectedBg = ContextCompat.getColor(context, R.color.category_pill_unselected_bg)
            val selectedFg = ContextCompat.getColor(context, R.color.category_pill_selected_fg)
            val unselectedFg = ContextCompat.getColor(context, R.color.category_pill_unselected_fg)

            chipBackgroundColor = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(selectedBg, unselectedBg)
            )
            val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(context)
            rippleColor = android.content.res.ColorStateList.valueOf(
                if (selectedBg == Color.BLACK) 0x1A000000 else 0x26FFFFFF
            )
            setTextColor(
                android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(selectedFg, unselectedFg)
                )
            )
            chipIconTint = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(selectedFg, unselectedFg)
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
            card.radius = res.getDimension(R.dimen.telegram_list_card_radius)
            card.cardElevation = 0f
            card.maxCardElevation = 0f
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
            card.strokeWidth = 0
            card.strokeColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                card.outlineAmbientShadowColor = Color.TRANSPARENT
                card.outlineSpotShadowColor = Color.TRANSPARENT
            }
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

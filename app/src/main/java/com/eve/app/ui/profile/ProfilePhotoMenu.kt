/*
 * Derived from Telegram Android ImageUpdater.java and BottomSheet.java,
 * Copyright Nikolai Kudashov / Telegram contributors.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.profile

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import com.eve.app.R
import com.eve.app.designsystem.DesignSurfaces
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Source composition of ImageUpdater.openMenu()
 * (`ImageUpdater.java:253-332`): "Take photo", "Upload from gallery" and,
 * only when an avatar exists, destructive "Remove photo". The original
 * BottomSheet title is `Choose photo` for a self profile (`type == 0`).
 */
internal object ProfilePhotoMenuModel {
    enum class Action { CAMERA, GALLERY, REMOVE }

    data class Item(
        val action: Action,
        @DrawableRes val iconRes: Int,
        @StringRes val labelRes: Int,
        val destructive: Boolean = false
    )

    fun items(hasAvatar: Boolean): List<Item> = buildList {
        add(Item(Action.CAMERA, R.drawable.ic_camera, R.string.profile_take_photo))
        add(Item(Action.GALLERY, R.drawable.msg_photos, R.string.profile_upload_from_gallery))
        if (hasAvatar) {
            add(Item(Action.REMOVE, R.drawable.ic_delete, R.string.profile_remove_photo, destructive = true))
        }
    }
}

/**
 * ADAPTED BottomSheet host for the source item menu. Telegram's BottomSheet
 * title is 48dp high (20sp bold, 21dp side padding) and each cell is a 48dp
 * row with a 56dp-wide icon column, text starting at 72dp and 16sp type
 * (`BottomSheet.java:1061-1145,1400-1475`). The host uses EVE's existing
 * themed sheet surface/scrim while retaining those source dimensions.
 */
internal object ProfilePhotoMenu {

    fun show(
        activity: Activity,
        hasAvatar: Boolean,
        onAction: (ProfilePhotoMenuModel.Item) -> Unit
    ): BottomSheetDialog {
        val palette = TelegramThemeEngine.resolveCurrentPalette(activity)
        val density = activity.resources.displayMetrics.density
        fun dp(value: Float) = (value * density + 0.5f).toInt()

        val title = TextView(activity).apply {
            setText(R.string.profile_choose_photo)
            setTextColor(palette.submenuText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            setPadding(dp(21f), dp(6f), dp(21f), dp(8f))
        }

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f)))
        }

        val dialog = BottomSheetDialog(activity, R.style.Theme_Eve_BottomSheetDialog)
        for (item in ProfilePhotoMenuModel.items(hasAvatar)) {
            val label = activity.getString(item.labelRes)
            val ink = if (item.destructive) {
                ContextCompat.getColor(activity, R.color.telegram_text_red_bold)
            } else {
                palette.submenuText
            }
            val iconInk = if (item.destructive) {
                ContextCompat.getColor(activity, R.color.telegram_text_red_regular)
            } else {
                palette.submenuIcon
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                minimumHeight = dp(48f)
                contentDescription = label
                background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
            }
            val icon = ImageView(activity).apply {
                setImageResource(item.iconRes)
                scaleType = ImageView.ScaleType.CENTER
                // BottomSheet.java:1132-1138 gives the icon a 5dp leading inset
                // inside the 56dp column for a big-title sheet.
                setPaddingRelative(dp(5f), 0, 0, 0)
                ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(iconInk))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            row.addView(icon, LinearLayout.LayoutParams(dp(56f), dp(48f)))
            val text = TextView(activity).apply {
                setText(item.labelRes)
                setTextColor(ink)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                gravity = Gravity.CENTER_VERTICAL
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.END
                // 56dp icon column + 16dp = source 72dp text start; big-title
                // cells end at 21dp (BottomSheet.java:1133-1138).
                setPaddingRelative(dp(16f), 0, dp(21f), 0)
            }
            row.addView(text, LinearLayout.LayoutParams(0, dp(48f), 1f))
            row.setOnClickListener {
                dialog.dismiss()
                onAction(item)
            }
            content.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f)))
        }

        dialog.setContentView(content)
        DesignSurfaces.dialog(dialog)
        DesignSurfaces.sheet(content)
        com.eve.app.ui.home.TelegramBottomSheetSourceBackdrop.install(dialog, activity)

        // Source BottomSheet is a fixed item sheet; it never rests half-open.
        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.maxWidth = dp(640f)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            view.setPadding(
                0,
                0,
                0,
                insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(content)
        dialog.show()
        return dialog
    }
}

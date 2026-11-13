/*
 * Derived from Telegram Android ImageUpdater.java and BottomSheet.java,
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later.
 * See THIRD_PARTY_NOTICES.md and docs/PROFILE_PHOTO_MENU_AUDIT.md.
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
import androidx.core.widget.ImageViewCompat
import com.eve.app.R
import com.eve.app.designsystem.TelegramBottomSheet
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine

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
        @StringRes val labelRes: Int,
        @DrawableRes val iconRes: Int,
        val destructive: Boolean = false
    )

    fun items(hasAvatar: Boolean): List<Item> {
        val base = mutableListOf(
            Item(Action.CAMERA, R.string.profile_take_photo, R.drawable.ic_camera),
            Item(Action.GALLERY, R.string.profile_upload_from_gallery, R.drawable.msg_photos)
        )
        if (hasAvatar) {
            base.add(Item(Action.REMOVE, R.string.profile_remove_photo, R.drawable.ic_delete, destructive = true))
        }
        return base
    }
}

/**
 * Authentic Telegram BottomSheet host for the profile photo menu.
 */
internal object ProfilePhotoMenu {

    fun show(
        activity: Activity,
        hasAvatar: Boolean,
        onAction: (ProfilePhotoMenuModel.Item) -> Unit
    ): TelegramBottomSheet {
        val palette = TelegramThemeEngine.resolveCurrentPalette(activity)
        val density = activity.resources.displayMetrics.density
        fun dp(value: Float) = (value * density + 0.5f).toInt()

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        var dialogRef: TelegramBottomSheet? = null
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
                setPaddingRelative(dp(16f), 0, dp(21f), 0)
            }
            row.addView(text, LinearLayout.LayoutParams(0, dp(48f), 1f))
            row.setOnClickListener {
                dialogRef?.dismiss()
                onAction(item)
            }
            content.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f)))
        }

        val dialog = TelegramBottomSheet.Builder(activity)
            .setTitle(activity.getString(R.string.profile_choose_photo), isBig = true)
            .setCustomView(content)
            .create()
        dialogRef = dialog
        dialog.show()
        return dialog
    }
}

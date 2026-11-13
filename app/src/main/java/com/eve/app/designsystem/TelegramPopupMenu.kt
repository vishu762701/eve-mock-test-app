/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java & ActionBarMenuSubItem.java @ 3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager

/**
 * Source-accurate Telegram popup menu.
 * Upstream:
 * - ActionBarPopupWindow.java: 196dp min width, 8dp padding, 6dp gap, popup_fixed_alert4 9-patch
 * - ActionBarMenuSubItem.java: 48dp row height, 16sp text, 18dp padding, ripple feedback
 * - Animation: R.style.PopupContextAnimation (@anim/popup_context_in)
 */
class TelegramPopupMenu(private val context: Context) : PopupWindow(context) {

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density + 0.5f).toInt()

    private val palette = TelegramThemeEngine.resolveCurrentPalette(context)
    private val contentLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
        minimumWidth = dp(196f)
    }

    private val items = mutableListOf<View>()

    init {
        contentView = contentLayout
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isFocusable = true
        isOutsideTouchable = true
        elevation = 0f // 9-patch supplies authentic shadow

        // Telegram ActionBarPopupWindow.java:1394 PopupContextAnimation
        animationStyle = R.style.PopupContextAnimation
        inputMethodMode = INPUT_METHOD_NOT_NEEDED
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED

        // Authentic 9-patch background tinted to dialogSurface
        val bgDrawable = AppCompatResources.getDrawable(context, R.drawable.telegram_dialog_popup_fixed_alert4)?.mutate()
        val surfaceColor = if (palette.isDark) 0xFF262626.toInt() else palette.dialogSurface
        bgDrawable?.colorFilter = PorterDuffColorFilter(surfaceColor, PorterDuff.Mode.MULTIPLY)
        setBackgroundDrawable(bgDrawable ?: ColorDrawable(Color.TRANSPARENT))

        contentLayout.isFocusableInTouchMode = true
        contentLayout.setOnKeyListener { _, keyCode, event ->
            if ((keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK) &&
                event.repeatCount == 0 && event.action == KeyEvent.ACTION_UP && isShowing
            ) {
                dismiss()
                true
            } else {
                false
            }
        }
    }

    /**
     * Adds a 48dp item row following ActionBarMenuSubItem.java:28-60.
     */
    fun addItem(
        id: Int,
        text: CharSequence,
        @DrawableRes iconRes: Int = 0,
        isDestructive: Boolean = false,
        onClick: () -> Unit
    ): TelegramPopupMenu {
        val row = FrameLayout(context).apply {
            tag = id
            isClickable = true
            isFocusable = true
            minimumHeight = dp(48f)
            minimumWidth = dp(196f)
            setPadding(dp(18f), 0, dp(18f), 0)
            background = TelegramThemeSelectors.getRoundRectSelectorDrawable(dp(8f).toFloat(), palette.actionSelector)

            val textInk = if (isDestructive) {
                ContextCompat.getColor(context, R.color.design_destructive_text)
            } else {
                if (palette.isDark) Color.WHITE else palette.textPrimary
            }

            val iconInk = if (isDestructive) {
                ContextCompat.getColor(context, R.color.design_destructive_text)
            } else {
                palette.textSecondary
            }

            val textView = TextView(context).apply {
                this.text = text
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
                typeface = FontManager.telegramRegularTypeface(context)
                setTextColor(textInk)
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }

            if (iconRes != 0) {
                val imageView = ImageView(context).apply {
                    setImageResource(iconRes)
                    setColorFilter(PorterDuffColorFilter(iconInk, PorterDuff.Mode.SRC_IN))
                    scaleType = ImageView.ScaleType.CENTER
                }
                addView(imageView, FrameLayout.LayoutParams(dp(24f), dp(24f), Gravity.START or Gravity.CENTER_VERTICAL))
                addView(textView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.START or Gravity.CENTER_VERTICAL
                ).apply {
                    marginStart = dp(38f) // 24dp icon + 14dp gap
                })
            } else {
                addView(textView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.START or Gravity.CENTER_VERTICAL
                ))
            }

            setOnClickListener {
                dismiss()
                onClick()
            }
        }

        items.add(row)
        contentLayout.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48f)
        ))
        return this
    }

    /**
     * Adds a 6dp group gap following Telegram ActionBarPopupWindow.java:215.
     */
    fun addGap(): TelegramPopupMenu {
        val gap = View(context).apply {
            minimumHeight = dp(6f)
        }
        contentLayout.addView(gap, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(6f)
        ))
        return this
    }

    fun show(anchor: View, xOffset: Int = 0, yOffset: Int = 0) {
        showAsDropDown(anchor, xOffset, yOffset)
    }
}

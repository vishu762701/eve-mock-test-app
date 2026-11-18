/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java & ItemOptions.java @ 21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
        elevation = dp(8f).toFloat()

        animationStyle = R.style.PopupContextAnimation
        inputMethodMode = INPUT_METHOD_NOT_NEEDED
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED

        val bgDrawable = AppCompatResources.getDrawable(context, R.drawable.telegram_dialog_popup_fixed_alert4)?.mutate()
        val surfaceColor = if (palette.isDark) 0xFF262626.toInt() else Color.WHITE
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
                if (palette.isDark) Color.WHITE else palette.textPrimary
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
                    marginStart = dp(38f)
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
        try {
            anchor.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } catch (_: Exception) {}
        val origBg = anchor.background
        val origZ = anchor.translationZ
        val highlight = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12f).toFloat()
            setColor(if (palette.isDark) 0xFF2C2C2E.toInt() else Color.WHITE)
        }
        anchor.background = highlight
        anchor.translationZ = dp(6f).toFloat()
        setOnDismissListener {
            anchor.background = origBg
            anchor.translationZ = origZ
        }
        showAsDropDown(anchor, xOffset + dp(14f), yOffset)
        try {
            val container = contentView.rootView
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val lp = container.layoutParams as? WindowManager.LayoutParams
            if (wm != null && lp != null) {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                lp.dimAmount = if (palette.isDark) 0.45f else 0.30f
                wm.updateViewLayout(container, lp)
            }
        } catch (_: Exception) {}
    }
}

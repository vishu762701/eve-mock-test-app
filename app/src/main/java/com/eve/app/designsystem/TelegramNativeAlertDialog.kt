/*
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15:
 * ActionBar/AlertDialog.java:302-318,489-504,809-821,920,1137-1215.
 * Copyright Nikolai Kudashov, 2013-2018. GPL-2.0-or-later.
 *
 * Dependency-isolated ADAPTED port of the Telegram message-confirmation path.
 * NOT the complete renderer: custom views, choices, progress, rich emoji and
 * animations have not yet been ported. No Telegram account/service dependencies.
 */
package com.eve.app.designsystem

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager

/** Message/title/two-button dialog only; not for custom/edit/choice/progress dialogs. */
class TelegramNativeAlertDialog private constructor(
    context: Context,
    private val titleText: CharSequence?,
    private val messageText: CharSequence?,
    private val positiveText: CharSequence?,
    private val onPositive: DialogInterface.OnClickListener?,
    private val negativeText: CharSequence?,
    private val onNegative: DialogInterface.OnClickListener?
) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar) {

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
    private val actionButtons = mutableMapOf<Int, TextView>()
    fun getActionButton(which: Int): TextView? = actionButtons[which]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val textInk = if (palette.isDark) Color.WHITE else palette.textPrimary
        val actionInk = if (palette.isDark) Color.WHITE else palette.accent
        val backgroundInk = if (palette.isDark) 0xFF262626.toInt() else palette.dialogSurface
        val background = requireNotNull(AppCompatResources.getDrawable(
            context, R.drawable.telegram_dialog_popup_fixed_alert4
        )).mutate()
        background.colorFilter = PorterDuffColorFilter(backgroundInk, PorterDuff.Mode.MULTIPLY)
        window?.setBackgroundDrawable(background)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setDimAmount(0.5f)

        val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titleText?.let { label ->
            root.addView(TextView(context).apply {
                tag = "telegram_native_alert_title"
                text = label
                setTextColor(textInk)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = FontManager.telegramMediumTypeface(context)
                gravity = Gravity.START
                setPadding(dp(24), dp(19), dp(24), dp(10))
            }, LinearLayout.LayoutParams(-1, -2))
        }
        messageText?.let { message ->
            val scroll = ScrollView(context).apply {
                isVerticalScrollBarEnabled = false
                addView(TextView(context).apply {
                    tag = "telegram_native_alert_message"
                    text = message
                    setTextColor(textInk)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    gravity = Gravity.START
                    setPadding(dp(24), if (titleText == null) dp(19) else 0, dp(24), dp(10))
                }, android.widget.FrameLayout.LayoutParams(-1, -2))
            }
            root.addView(scroll, LinearLayout.LayoutParams(-1, -2))
        }
        if (positiveText != null || negativeText != null) {
            val actions = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), 0)
            }
            fun addAction(which: Int, label: CharSequence, listener: DialogInterface.OnClickListener?) {
                val mask = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.WHITE)
                }
                val action = TextView(context).apply {
                    tag = which
                    text = label
                    setTextColor(actionInk)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = FontManager.telegramMediumTypeface(context)
                    gravity = Gravity.CENTER
                    minWidth = dp(64)
                    minHeight = dp(48)
                    setPadding(dp(12), 0, dp(12), 0)
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    setBackground(RippleDrawable(
                        ColorStateList.valueOf((actionInk and 0x00FFFFFF) or 0x19000000), null, mask
                    ))
                    setOnClickListener {
                        if (!isShowing) return@setOnClickListener
                        try {
                            listener?.onClick(this@TelegramNativeAlertDialog, which)
                        } finally {
                            if (isShowing) {
                                if (which == DialogInterface.BUTTON_NEGATIVE) cancel() else dismiss()
                            }
                        }
                    }
                }
                actionButtons[which] = action
                val lp = LinearLayout.LayoutParams(-2, dp(48))
                if (actions.childCount != 0) lp.marginStart = dp(8)
                actions.addView(action, lp)
            }
            negativeText?.let { addAction(DialogInterface.BUTTON_NEGATIVE, it, onNegative) }
            positiveText?.let { addAction(DialogInterface.BUTTON_POSITIVE, it, onPositive) }
            // Upstream buttonsLayout is 52dp high; EVE retains >=48dp targets.
            root.addView(actions, LinearLayout.LayoutParams(-1, dp(52)))
        }
        setContentView(root, android.view.ViewGroup.LayoutParams(-1, -2))
    }

    override fun show() {
        super.show()
        // Upstream AlertDialog.java:489-504 width constraints and 9-patch padding.
        val metrics = context.resources.displayMetrics
        val size = context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        val isTablet = size >= Configuration.SCREENLAYOUT_SIZE_LARGE
        val minSideDp = minOf(metrics.widthPixels, metrics.heightPixels) / metrics.density
        val maxWidthDp = if (!isTablet) 356 else if (minSideDp <= 690f) 446 else 496
        val insets = Rect()
        window?.decorView?.background?.getPadding(insets)
        val width = minOf(metrics.widthPixels - dp(56), dp(maxWidthDp)) + insets.left + insets.right
        window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    class Builder(private val context: Context) {
        private var title: CharSequence? = null
        private var message: CharSequence? = null
        private var positive: CharSequence? = null
        private var negative: CharSequence? = null
        private var onPositive: DialogInterface.OnClickListener? = null
        private var onNegative: DialogInterface.OnClickListener? = null
        fun setTitle(value: CharSequence) = apply { title = value }
        fun setMessage(value: CharSequence) = apply { message = value }
        fun setPositiveButton(value: CharSequence, listener: DialogInterface.OnClickListener?) = apply {
            positive = value; onPositive = listener
        }
        fun setNegativeButton(value: CharSequence, listener: DialogInterface.OnClickListener?) = apply {
            negative = value; onNegative = listener
        }
        fun create() = TelegramNativeAlertDialog(context, title, message, positive, onPositive, negative, onNegative)
        fun show() = create().also { it.show() }
    }
}

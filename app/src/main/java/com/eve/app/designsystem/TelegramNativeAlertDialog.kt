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

import android.app.Activity
import android.app.Dialog
import android.content.ContextWrapper
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
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
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
    private val onNegative: DialogInterface.OnClickListener?,
    private val rowItems: Array<out CharSequence>?,
    private val selectedItemIndex: Int,
    private val onItemClick: DialogInterface.OnClickListener?,
    private val customContent: View?,
    private val neutralText: CharSequence?,
    private val onNeutral: DialogInterface.OnClickListener?,
    private val redButtons: Set<Int>
) : Dialog(context, R.style.Theme_Eve_TelegramNativeDialog) {

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
        if (messageText != null || rowItems != null || customContent != null) {
            // Telegram AlertDialog.java:850-871,920,932-949: a single scroll host
            // for message + 50dp list cells, independent of the action row.
            val scrollContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            messageText?.let { message ->
                scrollContainer.addView(TextView(context).apply {
                    tag = "telegram_native_alert_message"
                    text = message
                    setTextColor(textInk)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    gravity = Gravity.START
                    setPadding(dp(24), if (titleText == null) dp(19) else 0, dp(24), dp(10))
                }, LinearLayout.LayoutParams(-1, -2))
            }
            rowItems?.forEachIndexed { index, label ->
                val row = LinearLayout(context).apply {
                    tag = "telegram_native_alert_item_" + index
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    minimumHeight = dp(50)
                    setPadding(dp(24), 0, dp(24), 0)
                    isClickable = true
                    isFocusable = true
                    val ripple = android.util.TypedValue()
                    if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
                        setBackgroundResource(ripple.resourceId)
                    }
                    setOnClickListener {
                        if (!isShowing) return@setOnClickListener
                        try {
                            onItemClick?.onClick(this@TelegramNativeAlertDialog, index)
                        } finally {
                            if (isShowing) dismiss()
                        }
                    }
                }
                row.addView(TextView(context).apply {
                    text = label
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    setTextColor(textInk)
                    gravity = Gravity.CENTER_VERTICAL
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(0, -2, 1f))
                if (selectedItemIndex >= 0) {
                    // EVE radio-choice behavior is ADAPTED: Telegram's upstream
                    // AlertDialog only defines setItems(), not setSingleChoiceItems().
                    row.addView(RadioButton(context).apply {
                        isChecked = index == selectedItemIndex
                        buttonTintList = ColorStateList.valueOf(palette.accent)
                        isClickable = false
                        isFocusable = false
                        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, LinearLayout.LayoutParams(dp(40), dp(48)))
                    row.isSelected = index == selectedItemIndex
                }
                scrollContainer.addView(row, LinearLayout.LayoutParams(-1, dp(50)))
            }
            customContent?.let { child ->
                // Telegram AlertDialog.java:950-956 detaches an existing parent before
                // mounting the custom View into the dialog's scrollable content.
                (child.parent as? ViewGroup)?.removeView(child)
                scrollContainer.addView(child, LinearLayout.LayoutParams(-1, -2))
                // Native EVE inputs use the standard IME resize path, no Telegram services.
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            root.addView(ScrollView(context).apply {
                isVerticalScrollBarEnabled = false
                addView(scrollContainer, android.widget.FrameLayout.LayoutParams(-1, -2))
            }, LinearLayout.LayoutParams(-1, -2))
        }
        if (positiveText != null || negativeText != null || neutralText != null) {
            // Original Telegram AlertDialog.java:957-987 measures action labels
            // and switches to a vertical row when a narrow screen cannot fit them.
            val actionLabels = listOfNotNull(neutralText, negativeText, positiveText)
            val measurePaint = TextPaint().apply {
                textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP, 16f, context.resources.displayMetrics
                )
                typeface = FontManager.telegramMediumTypeface(context)
            }
            val estimatedWidth = actionLabels.sumOf {
                measurePaint.measureText(it.toString()).toDouble()
            }.toFloat() + dp(24 * actionLabels.size + 8 * (actionLabels.size - 1))
            val screenWidth = context.resources.displayMetrics.widthPixels
            val availableActionWidth = minOf(screenWidth - dp(56), dp(356)) - dp(16)
            val verticalActions = estimatedWidth > availableActionWidth
            val actions = LinearLayout(context).apply {
                tag = "telegram_native_alert_actions"
                orientation = if (verticalActions) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setPadding(dp(8), 0, dp(8), 0)
            }
            fun addAction(which: Int, label: CharSequence, listener: DialogInterface.OnClickListener?) {
                val mask = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.WHITE)
                }
                // Original AlertDialog.Builder.makeRed():1991-2008.
                // EVE destructive-red override is ADAPTED to its owner token.
                val buttonInk = if (which in redButtons) {
                    ContextCompat.getColor(context, R.color.design_destructive_text)
                } else actionInk
                val action = TextView(context).apply {
                    tag = which
                    text = label
                    setTextColor(buttonInk)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    typeface = FontManager.telegramMediumTypeface(context)
                    gravity = Gravity.CENTER
                    minWidth = dp(64)
                    minHeight = dp(48)
                    setPadding(dp(12), 0, dp(12), 0)
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    setBackground(RippleDrawable(
                        ColorStateList.valueOf((buttonInk and 0x00FFFFFF) or 0x19000000), null, mask
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
                val lp = LinearLayout.LayoutParams(
                    if (verticalActions) -1 else -2, dp(48)
                )
                if (actions.childCount != 0 && !verticalActions) lp.marginStart = dp(8)
                actions.addView(action, lp)
            }
            neutralText?.let { addAction(DialogInterface.BUTTON_NEUTRAL, it, onNeutral) }
            negativeText?.let { addAction(DialogInterface.BUTTON_NEGATIVE, it, onNegative) }
            positiveText?.let { addAction(DialogInterface.BUTTON_POSITIVE, it, onPositive) }
            // Telegram 52dp horizontal bar; EVE adapts vertical bar for 48dp taps.
            val rowHeight = if (verticalActions) dp(48 * actionLabels.size + 16) else dp(52)
            root.addView(actions, LinearLayout.LayoutParams(-1, rowHeight))
        }
        setContentView(root, android.view.ViewGroup.LayoutParams(-1, -2))
    }

    /** Upstream AndroidUtilities.isSafeToShow():6255-6267, AlertDialog.java:325-328. */
    private fun hostActivityIsAlive(): Boolean {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return !current.isFinishing && !current.isDestroyed
            current = current.baseContext
        }
        return true
    }

    override fun show() {
        if (!hostActivityIsAlive()) return
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
        private var items: Array<out CharSequence>? = null
        private var selectedItemIndex: Int = -1
        private var onItemClick: DialogInterface.OnClickListener? = null
        private var customView: View? = null
        private var neutral: CharSequence? = null
        private var onNeutral: DialogInterface.OnClickListener? = null
        private val redButtons = mutableSetOf<Int>()
        fun setTitle(value: CharSequence) = apply { title = value }
        fun setMessage(value: CharSequence) = apply { message = value }
        fun setPositiveButton(value: CharSequence, listener: DialogInterface.OnClickListener?) = apply {
            positive = value; onPositive = listener
        }
        fun setNegativeButton(value: CharSequence, listener: DialogInterface.OnClickListener?) = apply {
            negative = value; onNegative = listener
        }
        /** Telegram AlertDialog.Builder.setView():1845-1853, mount at 950-956. */
        fun setView(view: View) = apply { customView = view }
        /** Original Telegram AlertDialog.Builder.setItems():1827-1842. */
        fun setItems(values: Array<out CharSequence>, listener: DialogInterface.OnClickListener?) = apply {
            items = values.copyOf(); selectedItemIndex = -1; onItemClick = listener
        }
        /** EVE-only ADAPTED single-radio-choice behavior on upstream 50dp rows. */
        fun setSingleChoiceItems(
            values: Array<out CharSequence>,
            checkedIndex: Int,
            listener: DialogInterface.OnClickListener?
        ) = apply {
            items = values.copyOf(); selectedItemIndex = checkedIndex; onItemClick = listener
        }
        fun setNeutralButton(value: CharSequence, listener: DialogInterface.OnClickListener?) = apply {
            neutral = value; onNeutral = listener
        }
        fun makeRed(which: Int) = apply {
            require(which in setOf(
                DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE, DialogInterface.BUTTON_NEUTRAL
            )) { "Unsupported destructive action" }
            redButtons += which
        }
        fun create() = TelegramNativeAlertDialog(
            context, title, message, positive, onPositive, negative, onNegative,
            items, selectedItemIndex, onItemClick, customView, neutral, onNeutral, redButtons.toSet()
        )
        fun show() = create().also { it.show() }
    }
}

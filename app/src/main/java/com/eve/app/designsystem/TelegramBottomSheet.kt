/*
 * Derived from Telegram Android BottomSheet.java.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL v2 or later.
 */
package com.eve.app.designsystem

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.theme.TelegramThemeApplier
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.ui.home.TelegramBottomSheetSourceBackdrop
import com.eve.app.util.FontManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Telegram BottomSheet.java + BottomSheet.Builder port for Eve.
 * Preserves 20dp top corner radius (sheet_shadow_round), 51-alpha source scrim
 * (TelegramBottomSheetSourceBackdrop), 50dp title header, and 48dp BottomSheetCell rows.
 */
class TelegramBottomSheet private constructor(
    context: Context,
    private val titleText: CharSequence?,
    private val items: Array<out CharSequence>?,
    private val itemIcons: IntArray?,
    private val onItemClick: DialogInterface.OnClickListener?,
    private val customView: View?,
    private val applyTopPadding: Boolean,
    private val applyBottomPadding: Boolean
) : BottomSheetDialog(context, R.style.Theme_Eve_BottomSheetDialog) {

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

    private val itemViews = mutableListOf<BottomSheetCell>()
    private var dismissListener: DialogInterface.OnDismissListener? = null

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val topPad = if (applyTopPadding && titleText == null) dp(8) else 0
            val bottomPad = if (applyBottomPadding) dp(8) else 0
            setPadding(0, topPad, 0, bottomPad)
        }

        titleText?.let { title ->
            root.addView(TextView(context).apply {
                tag = "telegram_bottom_sheet_title"
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = FontManager.telegramMediumTypeface(context)
                setTextColor(palette.textSecondary)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(dp(21), dp(8), dp(21), dp(8))
                isSingleLine = true
                ellipsize = TextUtils.TruncateAt.END
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        }

        if (items != null && items.isNotEmpty()) {
            val scrollContent = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }
            items.forEachIndexed { index, label ->
                val iconRes = if (itemIcons != null && index < itemIcons.size) itemIcons[index] else 0
                val cell = BottomSheetCell(context, palette).apply {
                    tag = "telegram_bottom_sheet_item_$index"
                    setTextAndIcon(label, iconRes)
                    setOnClickListener {
                        if (!isShowing) return@setOnClickListener
                        try {
                            onItemClick?.onClick(this@TelegramBottomSheet, index)
                        } finally {
                            if (isShowing) dismiss()
                        }
                    }
                }
                itemViews.add(cell)
                scrollContent.addView(
                    cell,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
                )
            }
            root.addView(ScrollView(context).apply {
                isVerticalScrollBarEnabled = false
                addView(scrollContent, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        customView?.let { child ->
            (child.parent as? ViewGroup)?.removeView(child)
            TelegramThemeApplier.applyToViewTree(child, palette)
            root.addView(child, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        DesignSurfaces.sheet(root)
        setContentView(root)
        DesignSurfaces.dialog(this)
        TelegramBottomSheetSourceBackdrop.install(this, context)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    fun setItemText(index: Int, text: CharSequence) {
        if (index in itemViews.indices) {
            itemViews[index].setText(text)
        }
    }

    fun setItemColor(index: Int, color: Int) {
        if (index in itemViews.indices) {
            itemViews[index].setTextColor(color)
            itemViews[index].setIconColor(color)
        }
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        dismissListener = listener
        super.setOnDismissListener(listener)
    }

    /** BottomSheet.BottomSheetCell (BottomSheet.java:363-440): 48dp height, 24dp icon + 16sp text. */
    class BottomSheetCell(
        context: Context,
        palette: TelegramThemeEngine.ResolvedPalette
    ) : FrameLayout(context) {

        private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density + 0.5f).toInt()

        private val textView = TextView(context).apply {
            setLines(1)
            isSingleLine = true
            gravity = Gravity.CENTER_VERTICAL
            ellipsize = TextUtils.TruncateAt.END
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(if (palette.isDark) Color.WHITE else palette.textPrimary)
        }

        private val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER
            colorFilter = PorterDuffColorFilter(palette.textSecondary, PorterDuff.Mode.MULTIPLY)
        }

        init {
            background = TelegramThemeSelectors.getSelectorDrawable(palette.actionSelector)
            setPadding(dp(16), 0, dp(16), 0)
            isClickable = true
            isFocusable = true

            addView(imageView, LayoutParams(dp(24), dp(24), Gravity.START or Gravity.CENTER_VERTICAL))
            addView(textView, LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            ))
        }

        fun setTextAndIcon(text: CharSequence?, @DrawableRes iconRes: Int) {
            textView.text = text
            val lp = textView.layoutParams as LayoutParams
            if (iconRes != 0) {
                imageView.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
                imageView.visibility = VISIBLE
                lp.marginStart = dp(56)
            } else {
                imageView.visibility = INVISIBLE
                lp.marginStart = 0
            }
            textView.layoutParams = lp
        }

        fun setText(text: CharSequence?) {
            textView.text = text
        }

        fun setTextColor(color: Int) {
            textView.setTextColor(color)
        }

        fun setIconColor(color: Int) {
            imageView.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        }
    }

    class Builder(private val context: Context) {
        private var title: CharSequence? = null
        private var items: Array<out CharSequence>? = null
        private var icons: IntArray? = null
        private var onClickListener: DialogInterface.OnClickListener? = null
        private var customView: View? = null
        private var applyTopPadding: Boolean = true
        private var applyBottomPadding: Boolean = true
        private var cancelable: Boolean = true
        private var dismissListener: DialogInterface.OnDismissListener? = null
        private var cancelListener: DialogInterface.OnCancelListener? = null

        fun setTitle(title: CharSequence?) = apply { this.title = title }

        fun setItems(items: Array<out CharSequence>, listener: DialogInterface.OnClickListener?) = apply {
            this.items = items.copyOf()
            this.icons = null
            this.onClickListener = listener
        }

        fun setItems(
            items: Array<out CharSequence>,
            icons: IntArray,
            listener: DialogInterface.OnClickListener?
        ) = apply {
            this.items = items.copyOf()
            this.icons = icons.copyOf()
            this.onClickListener = listener
        }

        fun setCustomView(view: View) = apply { this.customView = view }
        fun setApplyTopPadding(value: Boolean) = apply { this.applyTopPadding = value }
        fun setApplyBottomPadding(value: Boolean) = apply { this.applyBottomPadding = value }
        fun setCancelable(value: Boolean) = apply { this.cancelable = value }
        fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) = apply { this.dismissListener = listener }
        fun setOnCancelListener(listener: DialogInterface.OnCancelListener?) = apply { this.cancelListener = listener }

        fun create(): TelegramBottomSheet = TelegramBottomSheet(
            context = context,
            titleText = title,
            items = items,
            itemIcons = icons,
            onItemClick = onClickListener,
            customView = customView,
            applyTopPadding = applyTopPadding,
            applyBottomPadding = applyBottomPadding
        ).also { sheet ->
            sheet.setCancelable(cancelable)
            sheet.setCanceledOnTouchOutside(cancelable)
            sheet.setOnDismissListener(dismissListener)
            sheet.setOnCancelListener(cancelListener)
        }

        fun show(): TelegramBottomSheet = create().also { it.show() }
    }
}

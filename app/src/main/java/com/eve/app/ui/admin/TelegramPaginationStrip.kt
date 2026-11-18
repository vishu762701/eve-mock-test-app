/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/ScrollSlidingTabStrip.java:35-220 @ 3de37c8cfb03
 * Derived from Telegram Android FilterTabsView.java and ScrollSlidingTabStrip.java
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * 1:1 Telegram pagination strip view.
 * Features a gliding selection pill with CubicBezierInterpolator.EASE_OUT_QUINT,
 * Telegram theme palette, and accessible page controls.
 */
open class TelegramPaginationStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private val slot = dp(48f)
    private val mark = View(context)
    private val cells = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private var lastStart = -1
    private var lastPage = -1
    private var markAnimator: ValueAnimator? = null

    init {
        clipChildren = false
        clipToPadding = false

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        mark.background = GradientDrawable().apply {
            cornerRadius = dp(12f).toFloat()
            setColor(palette.accent)
        }

        addView(mark, LayoutParams(slot, slot, Gravity.START or Gravity.TOP))
        addView(cells, LayoutParams(LayoutParams.WRAP_CONTENT, slot, Gravity.START or Gravity.TOP))

        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "User list pagination"
    }

    fun bind(page: Int, pageCount: Int, onSelect: (Int) -> Unit) {
        val window = AdminPageWindow.pages(page, pageCount).toList()
        val previousStart = lastStart
        val shift = if (previousStart < 0 || window.isEmpty()) 0 else window.first() - previousStart
        lastStart = window.firstOrNull() ?: -1
        val selected = page.coerceIn(1, pageCount.coerceAtLeast(1))

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val onAccent = ContextCompat.getColor(context, R.color.on_accent)

        while (cells.childCount > window.size) cells.removeViewAt(cells.childCount - 1)
        while (cells.childCount < window.size) {
            val button = TextView(context).apply {
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                minWidth = slot
                minHeight = slot
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = FontManager.telegramMediumTypeface(context)
                background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                    dp(12f).toFloat(),
                    palette.actionSelector
                )
            }
            cells.addView(button, LinearLayout.LayoutParams(slot, slot))
        }

        window.forEachIndexed { index, number ->
            val button = cells.getChildAt(index) as TextView
            button.animate().cancel()
            button.text = number.toString()
            button.isSelected = number == selected
            button.contentDescription = if (number == selected)
                "Page $number, current page" else "Page $number"
            button.setTextColor(if (number == selected) onAccent else palette.textPrimary)

            button.setOnClickListener {
                if (number != page) onSelect(number)
            }
        }

        val targetIndex = window.indexOf(selected).coerceAtLeast(0)
        val targetX = (targetIndex * slot).toFloat()

        val motionEnabled = TelegramAnimationRuntime.isMotionEnabled(context)

        if (lastPage < 0 || !motionEnabled) {
            markAnimator?.cancel()
            markAnimator = null
            mark.translationX = targetX
        } else {
            val currentStartX = if (shift != 0) {
                (targetIndex + shift) * slot.toFloat()
            } else {
                mark.translationX
            }

            markAnimator?.cancel()
            val startX = currentStartX
            markAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 280L
                interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
                addUpdateListener { va ->
                    val f = va.animatedFraction
                    mark.translationX = startX + (targetX - startX) * f
                }
                start()
            }
        }
        lastPage = selected
    }

    override fun onDetachedFromWindow() {
        markAnimator?.cancel()
        markAnimator = null
        super.onDetachedFromWindow()
    }
}

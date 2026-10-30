/*
 * Derived from Telegram Android FilterTabsView.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official Telegram Segmented Pill Tabs ported for EVE Admin Hub.
 * Upstream: TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java:80-160
 * - Rounded pill indicator with 16dp corner radius
 * - Smooth DecelerateInterpolator animation between tabs
 * - Selected text: white/onAccent or accent, unselected: textSecondary
 */
class TelegramSegmentedTabs @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val tabContainer = LinearLayout(context)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorRect = RectF()

    private var selectedIndex = 0
    private var currentIndicatorX = 0f
    private var currentIndicatorWidth = 0f
    private var targetIndicatorX = 0f
    private var targetIndicatorWidth = 0f
    private var animator: ValueAnimator? = null

    private val tabs = mutableListOf<String>()
    private val tabViews = mutableListOf<TextView>()

    var onTabSelectedListener: ((position: Int) -> Unit)? = null

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        indicatorPaint.color = palette.accent
        setWillNotDraw(false)

        tabContainer.orientation = LinearLayout.HORIZONTAL
        tabContainer.gravity = Gravity.CENTER_VERTICAL
        addView(tabContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setTabs(titles: List<String>, defaultIndex: Int = 0) {
        tabs.clear()
        tabs.addAll(titles)
        tabViews.clear()
        tabContainer.removeAllViews()

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)

        for ((index, title) in titles.withIndex()) {
            val tv = TextView(context).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                gravity = Gravity.CENTER
                includeFontPadding = false
                isClickable = true
                isFocusable = true
                setBackgroundResource(outValue.resourceId)
                setTextColor(if (index == defaultIndex) 0xFFFFFFFF.toInt() else palette.textSecondary)
                setOnClickListener { selectTab(index, animated = true) }
            }
            tabViews.add(tv)
            val lp = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            tabContainer.addView(tv, lp)
        }

        selectedIndex = defaultIndex.coerceIn(0, (titles.size - 1).coerceAtLeast(0))
        post {
            updateIndicatorBounds(selectedIndex, animated = false)
        }
    }

    fun selectTab(index: Int, animated: Boolean = true) {
        if (index !in tabs.indices || (index == selectedIndex && currentIndicatorWidth > 0)) return
        selectedIndex = index

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        for ((i, tv) in tabViews.withIndex()) {
            tv.setTextColor(if (i == selectedIndex) 0xFFFFFFFF.toInt() else palette.textSecondary)
        }

        updateIndicatorBounds(selectedIndex, animated)
        onTabSelectedListener?.invoke(selectedIndex)
    }

    private fun updateIndicatorBounds(index: Int, animated: Boolean) {
        if (index !in tabViews.indices) return
        val tv = tabViews[index]
        targetIndicatorX = tv.left.toFloat() + dp(4f)
        targetIndicatorWidth = (tv.width - dp(8f)).coerceAtLeast(0).toFloat()

        if (!animated || currentIndicatorWidth == 0f) {
            currentIndicatorX = targetIndicatorX
            currentIndicatorWidth = targetIndicatorWidth
            invalidate()
            return
        }

        val startX = currentIndicatorX
        val startW = currentIndicatorWidth
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                currentIndicatorX = startX + (targetIndicatorX - startX) * fraction
                currentIndicatorWidth = startW + (targetIndicatorWidth - startW) * fraction
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (currentIndicatorWidth > 0) {
            val top = dp(4f).toFloat()
            val bottom = (height - dp(4f)).toFloat()
            indicatorRect.set(currentIndicatorX, top, currentIndicatorX + currentIndicatorWidth, bottom)
            val radius = (bottom - top) / 2f
            canvas.drawRoundRect(indicatorRect, radius, radius, indicatorPaint)
        }
        super.onDraw(canvas)
    }
}

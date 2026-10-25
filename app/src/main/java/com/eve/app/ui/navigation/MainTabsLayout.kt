package com.eve.app.ui.navigation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.eve.app.theme.TelegramThemeEngine

/**
 * Custom container layout for Telegram bottom navigation tabs.
 * Direct adaptation of Telegram's org.telegram.ui.MainTabsLayout.
 * Preserves content-fit text size/padding passes, max width (344dp), and gesture coordination.
 */
class MainTabsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface TabSelectionListener {
        fun onTabSelected(position: Int)
        fun onTabReselected(position: Int)
    }

    private var tabSelectionListener: TabSelectionListener? = null
    private var maxWidthPx: Int = 0
    private var selectedTabPosition: Int = 0

    companion object {
        private val PASS_TEXT_SIZES_DP = floatArrayOf(12f, 12f, 10f)
        private val PASS_PADDINGS_DP = intArrayOf(16, 8, 4)
    }

    init {
        orientation = HORIZONTAL
        clipChildren = false
        clipToPadding = false
        // Keep Telegram's 12dp horizontal inset, but do not subtract it vertically:
        // the parent pill is only 56dp tall, so vertical 12dp insets would shrink
        // each tab's measured/tappable height to 32dp and clip its label.
        val pad = dp(12)
        setPadding(pad, 0, pad, 0)
        // 328 + 2 * 8 = 344dp max width
        maxWidthPx = dp(344)
    }

    fun setTabSelectionListener(listener: TabSelectionListener) {
        this.tabSelectionListener = listener
    }

    fun setMaxWidthPx(widthPx: Int) {
        if (maxWidthPx != widthPx) {
            maxWidthPx = widthPx
            requestLayout()
        }
    }

    fun selectTab(position: Int, animated: Boolean = true, notify: Boolean = true) {
        val count = childCount
        if (position < 0 || position >= count) return

        val reselected = (selectedTabPosition == position)
        selectedTabPosition = position
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child is GlassTabView) {
                child.setSelectedTab(i == position, animated)
            }
        }
        if (notify) {
            if (reselected) {
                tabSelectionListener?.onTabReselected(position)
            } else {
                tabSelectionListener?.onTabSelected(position)
            }
        }
    }

    fun getSelectedTabPosition(): Int = selectedTabPosition

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is GlassTabView) {
                child.setPalette(palette)
            }
        }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        val pos = indexOfChild(child)
        if (child is GlassTabView) {
            child.setSelectedTab(pos == selectedTabPosition, false)
            child.setOnClickListener {
                val currentPos = indexOfChild(child)
                if (currentPos == selectedTabPosition) {
                    tabSelectionListener?.onTabReselected(currentPos)
                } else {
                    selectTab(currentPos, true)
                }
            }
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val offeredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val width = if (maxWidthPx > 0) minOf(offeredWidth, maxWidthPx) else offeredWidth
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val available = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        // Telegram MainTabsLayout.java:68-69. On compact screens, never
        // demand a 320dp tab strip wider than the space actually available.
        val minimum = minOf(dp(320), available)
        val visible = (0 until childCount).filter { getChildAt(it).visibility != GONE }
        val count = visible.size
        if (count == 0) {
            setMeasuredDimension(paddingLeft + paddingRight, height)
            return
        }

        // Exact upstream text-size and horizontal-padding passes:
        // MainTabsLayout.java:46-47, 71-90.
        var chosenPass = PASS_TEXT_SIZES_DP.lastIndex
        val measuredTextWidths = FloatArray(childCount)
        for (pass in PASS_TEXT_SIZES_DP.indices) {
            val textSize = PASS_TEXT_SIZES_DP[pass]
            val horizontalPadding = dp(PASS_PADDINGS_DP[pass])
            var total = 0f
            for (index in visible) {
                val child = getChildAt(index)
                if (child is GlassTabView) {
                    child.setTextSizeDp(textSize)
                    measuredTextWidths[index] = child.measureTextWidth()
                } else {
                    measuredTextWidths[index] = 0f
                }
                total += measuredTextWidths[index] + 2f * horizontalPadding
            }
            if (total <= available || pass == PASS_TEXT_SIZES_DP.lastIndex) {
                chosenPass = pass
                break
            }
        }

        val tabPadding = dp(PASS_PADDINGS_DP[chosenPass])
        val equalTextSpace = available / count - tabPadding * 2
        val widths = FloatArray(childCount)
        val weights = IntArray(childCount)
        var totalWidth = 0f
        var totalWeight = 0
        for (index in visible) {
            val child = getChildAt(index)
            if (child is GlassTabView) child.setTextSizeDp(PASS_TEXT_SIZES_DP[chosenPass])
            widths[index] = measuredTextWidths[index] + tabPadding * 2f
            weights[index] = if (widths[index] > equalTextSpace + tabPadding * 2) 0 else 1
            totalWeight += weights[index]
            totalWidth += widths[index]
        }
        if (totalWeight == 0) {
            for (index in visible) weights[index] = 1
            totalWeight = count
        }
        // Telegram MainTabsLayout.java:94-151: shrink to fit or distribute
        // additional width only to tabs that meet its text-fit weight rule.
        when {
            totalWidth > available && totalWidth > 0f -> {
                val scale = available / totalWidth
                for (index in visible) widths[index] *= scale
            }
            totalWidth < minimum -> {
                val grow = (minimum - totalWidth) / totalWeight
                for (index in visible) widths[index] += grow * weights[index]
            }
        }
        var measuredWidth = paddingLeft + paddingRight
        val heightForTabs = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        for (index in visible) {
            val child = getChildAt(index)
            val childWidth = Math.round(widths[index]).coerceAtLeast(1)
            child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(heightForTabs, MeasureSpec.EXACTLY))
            if (child is GlassTabView) child.setVisualWidth(childWidth.toFloat())
            measuredWidth += childWidth
        }
        for (index in 0 until childCount) {
            if (getChildAt(index).visibility == GONE) {
                getChildAt(index).measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY))
            }
        }
        setMeasuredDimension(minOf(width, measuredWidth), height)
    }

    private fun dp(value: Int): Int {
        return kotlin.math.ceil((value * resources.displayMetrics.density).toDouble()).toInt()
    }
}

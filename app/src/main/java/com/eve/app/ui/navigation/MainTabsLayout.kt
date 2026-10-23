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

    fun selectTab(position: Int, animated: Boolean = true) {
        val count = childCount
        if (position < 0 || position >= count) return

        if (selectedTabPosition == position) {
            tabSelectionListener?.onTabReselected(position)
            return
        }

        selectedTabPosition = position
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child is GlassTabView) {
                child.setSelectedTab(i == position, animated)
            }
        }
        tabSelectionListener?.onTabSelected(position)
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
        var width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (maxWidthPx > 0 && width > maxWidthPx) {
            width = maxWidthPx
        }

        val maxTotalWidthForTabs = width - paddingLeft - paddingRight
        val visibleChildCount = childCount.coerceAtLeast(1)

        // Content-fit text size pass from Telegram MainTabsLayout.java:73-90
        var chosenPass = PASS_TEXT_SIZES_DP.size - 1
        for (pass in PASS_TEXT_SIZES_DP.indices) {
            val textSize = PASS_TEXT_SIZES_DP[pass]
            val padding = dp(PASS_PADDINGS_DP[pass])
            var total = 0f
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child is GlassTabView && child.visibility != GONE) {
                    child.setTextSizeDp(textSize)
                    total += child.measureTextWidth() + padding * 2
                }
            }
            if (total <= maxTotalWidthForTabs || pass == PASS_TEXT_SIZES_DP.size - 1) {
                chosenPass = pass
                break
            }
        }

        val finalPassTextSize = PASS_TEXT_SIZES_DP[chosenPass]
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is GlassTabView) {
                child.setTextSizeDp(finalPassTextSize)
            }
        }

        // Divide available space equally among tabs
        val tabWidth = maxTotalWidthForTabs / visibleChildCount
        val childWidthSpec = MeasureSpec.makeMeasureSpec(tabWidth, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(height - paddingTop - paddingBottom, MeasureSpec.EXACTLY)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.measure(childWidthSpec, childHeightSpec)
                if (child is GlassTabView) {
                    child.setVisualWidth(tabWidth.toFloat())
                }
            }
        }

        setMeasuredDimension(width, height)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

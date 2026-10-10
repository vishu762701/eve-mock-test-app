package com.eve.app.ui.result

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout

/** Fixed 170dp metric cards wrap before they overflow; enlarged text may grow rows. */
class ResultStatisticsGrid @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GridLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = Math.round(value * density)
        val available = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val tileWidth = dp(170)
        val gap = dp(12)
        val columns = ((available + gap) / (tileWidth + gap)).coerceIn(1, childCount.coerceAtLeast(1))
        // Clear old specs before changing columnCount on a responsive remeasure.
        for (i in 0 until childCount) (getChildAt(i).layoutParams as LayoutParams).columnSpec = spec(UNDEFINED)
        columnCount = columns
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.minimumHeight = dp(116)
            val p = child.layoutParams as LayoutParams
            p.columnSpec = spec(i % columns)
            p.rowSpec = spec(i / columns, FILL)
            p.width = tileWidth
            p.height = LayoutParams.WRAP_CONTENT
            p.marginStart = 0
            p.marginEnd = if (i % columns == columns - 1) 0 else gap
            p.topMargin = 0
            p.bottomMargin = if (i / columns < (childCount - 1) / columns) gap else 0
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

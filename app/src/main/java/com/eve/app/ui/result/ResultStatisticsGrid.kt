package com.eve.app.ui.result

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout

/** Equal-width metrics fill the content area. Enlarged text gets fewer, wider columns. */
class ResultStatisticsGrid @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GridLayout(context, attrs) {
    private var configuredColumns = UNDEFINED

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = Math.round(value * density)
        val available = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val gap = dp(12)
        val minimumWidth = Math.round(124f * density * resources.configuration.fontScale.coerceAtLeast(1f))
        val columns = when {
            childCount >= 4 && available >= minimumWidth * 4 + gap * 3 -> 4
            childCount >= 2 && available >= minimumWidth * 2 + gap -> 2
            else -> 1
        }
        val tileWidth = ((available - gap * (columns - 1)) / columns).coerceAtLeast(0)
        val remainder = (available - gap * (columns - 1) - tileWidth * columns).coerceAtLeast(0)
        if (configuredColumns != columns) {
            // setColumnCount validates the cached max index before invalidating it.
            // Clear explicit specs and reset the axis before a narrower remeasure.
            for (i in 0 until childCount) (getChildAt(i).layoutParams as LayoutParams).columnSpec = spec(UNDEFINED)
            columnCount = UNDEFINED
            columnCount = columns
            configuredColumns = columns
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.minimumHeight != dp(96)) child.minimumHeight = dp(96)
            val p = child.layoutParams as LayoutParams
            p.columnSpec = spec(i % columns)
            p.rowSpec = spec(i / columns, FILL)
            p.width = tileWidth + if (i % columns < remainder) 1 else 0
            p.height = LayoutParams.WRAP_CONTENT
            p.marginStart = 0
            p.marginEnd = if (i % columns == columns - 1) 0 else gap
            p.topMargin = 0
            p.bottomMargin = if (i / columns < (childCount - 1) / columns) gap else 0
            p.resolveLayoutDirection(layoutDirection)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

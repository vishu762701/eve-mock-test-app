package com.eve.app.ui.result

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout

/** Two readable columns on phones; four on wider screens. No text shrinking. */
class ResultStatisticsGrid @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GridLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val available = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val fourColThreshold = (116 * 4 + 36) * density * resources.configuration.fontScale
        val columns = if (available >= fourColThreshold) 4 else 2
        for (i in 0 until childCount) (getChildAt(i).layoutParams as LayoutParams).columnSpec = spec(UNDEFINED)
        columnCount = columns
        val tileWidthPx = (116 * density + 0.5f).toInt()
        val tileHeightPx = (96 * density + 0.5f).toInt()
        val gap12Px = (12 * density + 0.5f).toInt()
        val canFitFixed = available >= (tileWidthPx * columns + gap12Px * (columns - 1))

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val p = child.layoutParams as LayoutParams
            p.columnSpec = if (canFitFixed) spec(i % columns) else spec(i % columns, FILL, 1f)
            p.rowSpec = spec(i / columns, FILL)
            p.width = if (canFitFixed) tileWidthPx else 0
            p.height = tileHeightPx
            p.marginEnd = if (i % columns == columns - 1) 0 else gap12Px
            p.bottomMargin = if (i / columns < (childCount - 1) / columns) gap12Px else 0
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

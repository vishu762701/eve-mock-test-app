package com.eve.app.ui.result

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridLayout

/** Two readable columns on phones; four on wider screens. No text shrinking. */
class ResultStatisticsGrid @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : GridLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val columns = if (available >= 440 * resources.displayMetrics.density * resources.configuration.fontScale) 4 else 2
        // Clear old specs before changing column count after rotation/resizing.
        for (i in 0 until childCount) (getChildAt(i).layoutParams as LayoutParams).columnSpec = spec(UNDEFINED)
        columnCount = columns
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val p = child.layoutParams as LayoutParams
            p.columnSpec = spec(i % columns, FILL, 1f)
            p.rowSpec = spec(i / columns, FILL)
            p.width = 0
            p.height = LayoutParams.WRAP_CONTENT
            p.marginEnd = if (i % columns == columns - 1) 0 else (8 * resources.displayMetrics.density).toInt()
            p.bottomMargin = if (i / columns < (childCount - 1) / columns) (8 * resources.displayMetrics.density).toInt() else 0
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

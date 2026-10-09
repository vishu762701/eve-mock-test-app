package com.eve.app.ui.result

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

/** Preserve existing card styling. Stack competing columns when their text
 * would be squeezed, including when the user increases the system font size. */
class ResponsiveResultRow @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private data class Original(val width: Int, val weight: Float, val start: Int, val end: Int, val bottom: Int)
    private val originals = mutableListOf<Original>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (originals.isEmpty()) for (i in 0 until childCount) {
            val p = getChildAt(i).layoutParams as LayoutParams
            originals += Original(p.width, p.weight, p.marginStart, p.marginEnd, p.bottomMargin)
        }
        val available = View.MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val stacked = available < (if (childCount == 3) 260 else 340) * resources.displayMetrics.density * resources.configuration.fontScale
        orientation = if (stacked) VERTICAL else HORIZONTAL
        for (i in 0 until childCount) {
            val p = getChildAt(i).layoutParams as LayoutParams
            val original = originals[i]
            p.width = if (stacked && original.weight > 0) LayoutParams.MATCH_PARENT else original.width
            p.weight = if (stacked) 0f else original.weight
            p.marginStart = if (stacked) 0 else original.start
            p.marginEnd = if (stacked) 0 else original.end
            p.bottomMargin = if (stacked && i < childCount - 1) (8 * resources.displayMetrics.density).toInt() else original.bottom
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}

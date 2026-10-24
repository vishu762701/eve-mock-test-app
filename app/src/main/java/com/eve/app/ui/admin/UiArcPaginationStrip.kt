package com.eve.app.ui.admin

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
import com.eve.app.designsystem.DesignTokens
import com.eve.app.designsystem.UiArcMotion

/** Five page slots with a single traveling highlight, no copy of the web React runtime. */
class UiArcPaginationStrip @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private val slot = (48f * resources.displayMetrics.density).toInt()
    // Arc's documented morph spring is visualDuration=0.42, bounce=0.
    private val glide = UiArcMotion.SpringCurve(0.42f, 0f, 550L)
    private val mark = View(context)
    private val cells = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private var lastStart = -1
    private var lastPage = -1

    init {
        clipChildren = false
        clipToPadding = false
        mark.background = GradientDrawable().apply {
            cornerRadius = 12f * resources.displayMetrics.density
            setColor(DesignTokens.accent(context))
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
        while (cells.childCount > window.size) cells.removeViewAt(cells.childCount - 1)
        while (cells.childCount < window.size) {
            val button = TextView(context).apply {
                gravity = Gravity.CENTER
                isFocusable = true
                isClickable = true
                minWidth = slot
                minHeight = slot
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = com.eve.app.util.FontManager.regularTypeface(context)
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
            button.setTextColor(
                if (number == selected) ContextCompat.getColor(context, R.color.on_accent)
                else DesignTokens.primary(context)
            )
            button.setOnClickListener { onSelect(number) }
            if (shift != 0 && UiArcMotion.isMotionEnabled(context)) {
                button.translationX = (shift.coerceIn(-5, 5) * slot / 2).toFloat()
                button.alpha = 0.55f
                button.animate().translationX(0f).alpha(1f)
                    .setDuration(UiArcMotion.Duration.STANDARD)
                    .setInterpolator(UiArcMotion.Ease.ENTER).start()
            } else { button.translationX = 0f; button.alpha = 1f }
        }
        mark.animate().cancel()
        if (window.isEmpty()) { mark.visibility = View.GONE; lastPage = -1; return }
        mark.visibility = View.VISIBLE
        // FrameLayout.Gravity.START anchors to the right on RTL layouts.
        // Child indices remain logical; translation sign changes, not their IDs.
        val direction = if (layoutDirection == View.LAYOUT_DIRECTION_RTL) -1f else 1f
        val destination = window.indexOf(selected) * slot * direction
        if (lastPage > 0 && UiArcMotion.isMotionEnabled(context)) {
            mark.animate().translationX(destination)
                .setDuration(glide.settlingDurationMs)
                .setInterpolator(glide).start()
        } else mark.translationX = destination
        lastPage = selected
    }

    override fun onDetachedFromWindow() {
        mark.animate().cancel()
        repeat(cells.childCount) { cells.getChildAt(it).animate().cancel() }
        super.onDetachedFromWindow()
    }
}

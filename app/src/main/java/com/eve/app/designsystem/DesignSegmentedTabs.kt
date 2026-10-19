package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import com.eve.app.util.FontManager
import androidx.core.content.ContextCompat
import android.view.View
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.eve.app.R
import com.google.android.material.tabs.TabLayout

/** Native tabs/pager selection callbacks, fixed token strip and explicit selected weight. */
class DesignSegmentedTabs @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.tabStyle
) : TabLayout(context, attrs, defStyleAttr) {
    init {
        clipChildren = false
        clipToPadding = false
        addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: Tab?) = applyTypography()
            override fun onTabUnselected(tab: Tab?) = Unit
            override fun onTabReselected(tab: Tab?) = Unit
        })
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = resolveSize(resources.getDimensionPixelSize(R.dimen.design_tab_height), heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        val strip = getChildAt(0) as? ViewGroup ?: return
        val available = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0)
        val innerWidth = if (tabMode == MODE_FIXED) available else maxOf(available, strip.measuredWidth)
        strip.measure(MeasureSpec.makeMeasureSpec(innerWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec((measuredHeight - paddingTop - paddingBottom).coerceAtLeast(0), MeasureSpec.EXACTLY))
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTypography()
    }
    private fun applyTypography() {
        val strip = getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until strip.childCount) {
            val segment = strip.getChildAt(i)
            val tab = getTabAt(i) ?: continue
            if (tab.customView == null) {
                tab.customView = AppCompatTextView(context).apply {
                    id = android.R.id.text1
                    text = tab.text
                    gravity = Gravity.CENTER
                    DesignType.apply(this, DesignType.Role.TAB)
                }
            }
            // Material measures its stock label with its own size/maxLines. Use the native
            // custom-label slot so the shared type role cannot fight that measurement.
            applyText(requireNotNull(tab.customView), i == selectedTabPosition)
            if (segment.stateListAnimator == null) DesignPress.bind(segment)
            if (isAttachedToWindow) TouchTargets.ensure(segment)
        }
    }
    private fun applyText(view: View, selected: Boolean) {
        if (view is TextView) {
            if (view.typeface?.isBold != selected) view.typeface = FontManager.typeface(context, selected)
            val size = resources.getDimension(R.dimen.design_type_tab)
            if (view.textSize != size) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            val color = ContextCompat.getColor(context, if (selected) R.color.on_accent else R.color.text_primary)
            if (view.currentTextColor != color) view.setTextColor(color)
            view.isSelected = selected
            val line = resources.getDimensionPixelSize(R.dimen.design_line_tab)
            if (view.lineHeight != line) TextViewCompat.setLineHeight(view, line)
            val glyphHeight = kotlin.math.ceil((view.paint.fontMetrics.descent - view.paint.fontMetrics.ascent).toDouble()).toInt()
            val labelHeight = maxOf(line, glyphHeight)
            if (view.layoutParams.height != labelHeight) view.layoutParams = view.layoutParams.apply { height = labelHeight }
            ShadowHost.releaseClipping(view)
            view.includeFontPadding = false
            if (view.maxLines != 1) view.maxLines = 1
            view.ellipsize = android.text.TextUtils.TruncateAt.END
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) applyText(view.getChildAt(i), selected)
    }
}

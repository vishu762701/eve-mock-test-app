package com.eve.app.designsystem

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import com.eve.app.util.FontManager
import androidx.core.content.ContextCompat
import android.view.View
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
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        applyTypography()
    }
    private fun applyTypography() {
        val strip = getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until strip.childCount) {
            val segment = strip.getChildAt(i)
            applyText(segment, i == selectedTabPosition)
            if (isAttachedToWindow) TouchTargets.ensure(segment)
        }
    }
    private fun applyText(view: View, selected: Boolean) {
        if (view is TextView) {
            if (view.typeface?.isBold != selected) view.typeface = FontManager.typeface(context, selected)
            val size = resources.getDimensionPixelSize(R.dimen.design_type_tab).toFloat()
            if (view.textSize != size) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
            val color = ContextCompat.getColor(context, if (selected) R.color.on_accent else R.color.text_primary)
            if (view.currentTextColor != color) view.setTextColor(color)
            val line = resources.getDimensionPixelSize(R.dimen.design_line_tab)
            if (view.lineHeight != line) TextViewCompat.setLineHeight(view, line)
            if (view.layoutParams.height != line) view.layoutParams = view.layoutParams.apply { height = line }
            ShadowHost.releaseClipping(view)
            view.includeFontPadding = false
            if (view.maxLines != 1) view.maxLines = 1
            view.ellipsize = android.text.TextUtils.TruncateAt.END
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) applyText(view.getChildAt(i), selected)
    }
}

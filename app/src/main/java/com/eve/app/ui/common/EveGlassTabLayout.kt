package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayout
import com.eve.app.R

/**
 * Apple iOS 26 Liquid Glass Segmented TabLayout.
 * Bridges Material TabLayout interface with native Apple Liquid Glass segmented styling:
 * - Rounded track clipping
 * - Tactile compression (scale 0.97) on tab items
 * - Clock-tick haptic feedback on tab selection
 */
class EveGlassTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {

    init {
        if (background == null) setBackgroundResource(R.drawable.bg_tab_segmented_track)
        tabRippleColor = null
        clipToOutline = true
        addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: Tab?) {
                try {
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                } catch (_: Throwable) {}
            }
            override fun onTabUnselected(tab: Tab?) {}
            override fun onTabReselected(tab: Tab?) {}
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (tabMode == MODE_FIXED && childCount == 1) {
            // Material 1.12 forces the fixed strip to the outer width, ignoring
            // horizontal padding. Keep the last capsule inside the Result track.
            val strip = getChildAt(0)
            val contentWidth = (measuredWidth - paddingLeft - paddingRight).coerceAtLeast(0)
            strip.measure(View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(strip.measuredHeight, View.MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val slidingTabStrip = getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until slidingTabStrip.childCount) {
            val tabView = slidingTabStrip.getChildAt(i)
            tabView.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
                    }
                }
                false
            }
        }
    }
}

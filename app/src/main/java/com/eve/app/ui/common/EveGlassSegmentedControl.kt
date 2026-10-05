package com.eve.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.eve.app.R
import com.eve.app.util.ThemeSwitchAnimator

/**
 * Apple iOS 26 Liquid Glass Segmented Control.
 * Features:
 * - Fluid sliding indicator pill that physically moves/morphs between segments
 * - Subtle spring-like overshoot response (180-260ms duration) with squash & stretch during transition
 * - Proper rounded clipping to track bounds
 * - Theme-aware text color transitions and tactile haptic feedback
 */
class EveGlassSegmentedControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private var selectedIndex = 0
    private var tabTitles = listOf<String>()
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    private val tabsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    private val indicatorView = View(context).apply {
        setBackgroundResource(R.drawable.bg_tab_segmented_indicator)
    }

    private var currentAnimator: ValueAnimator? = null
    private val overshootInterpolator = OvershootInterpolator(0.6f)
    private val fastOutSlow = FastOutSlowInInterpolator()

    init {
        setBackgroundResource(R.drawable.bg_tab_segmented_track)
        val pad = (3f * density).toInt()
        setPadding(pad, pad, pad, pad)

        addView(indicatorView, LayoutParams(0, LayoutParams.MATCH_PARENT))
        addView(tabsContainer)
    }

    fun setTabs(titles: List<String>, initialIndex: Int = 0) {
        tabTitles = titles
        tabsContainer.removeAllViews()

        for (i in titles.indices) {
            val tv = TextView(context).apply {
                text = titles[i]
                textSize = 14f
                gravity = Gravity.CENTER
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener {
                    if (selectedIndex != i) {
                        selectTab(i, animate = true)
                    }
                }
            }
            tabsContainer.addView(tv)
        }

        selectedIndex = initialIndex.coerceIn(0, (titles.size - 1).coerceAtLeast(0))
        updateTextColors()
        post { layoutIndicator(animate = false) }
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun selectTab(index: Int, animate: Boolean = true) {
        if (index < 0 || index >= tabTitles.size) return
        if (selectedIndex == index && indicatorView.width > 0) return

        selectedIndex = index
        updateTextColors()
        layoutIndicator(animate = animate)

        try {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Throwable) {}

        onTabSelectedListener?.invoke(selectedIndex)
    }

    fun getSelectedIndex(): Int = selectedIndex

    private fun updateTextColors() {
        val selectedColor = ContextCompat.getColor(context, R.color.eve_tab_selected_text)
        val unselectedColor = ContextCompat.getColor(context, R.color.eve_tab_unselected_text)

        for (i in 0 until tabsContainer.childCount) {
            val tv = tabsContainer.getChildAt(i) as? TextView ?: continue
            tv.setTextColor(if (i == selectedIndex) selectedColor else unselectedColor)
        }
    }

    private fun layoutIndicator(animate: Boolean) {
        val count = tabTitles.size
        if (count == 0 || width <= 0) return

        val innerWidth = width - paddingLeft - paddingRight
        val innerHeight = height - paddingTop - paddingBottom
        val tabWidth = innerWidth.toFloat() / count
        val targetX = selectedIndex * tabWidth

        val lp = indicatorView.layoutParams as LayoutParams
        lp.width = tabWidth.toInt()
        lp.height = innerHeight
        indicatorView.layoutParams = lp

        if (!animate) {
            currentAnimator?.cancel()
            indicatorView.translationX = targetX
            indicatorView.scaleX = 1.0f
            return
        }

        currentAnimator?.cancel()
        val startX = indicatorView.translationX
        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240L
            interpolator = overshootInterpolator
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                // Morphing / Squash & stretch: scale indicator width by up to 1.04 during mid-flight
                val squashStretch = 1.0f + 0.04f * kotlin.math.sin(fraction * Math.PI).toFloat()
                indicatorView.scaleX = squashStretch
                indicatorView.translationX = startX + (targetX - startX) * (va.animatedValue as Float)
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { layoutIndicator(animate = false) }
    }
}

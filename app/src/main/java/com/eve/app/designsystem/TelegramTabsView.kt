/*
 * Derived from Telegram Android FilterTabsView.java, PagerSlidingTabStrip.java
 * Copyright Nikolai Kudashov, 2013-2023. GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.viewpager2.widget.ViewPager2
import com.eve.app.theme.TelegramThemeEngine
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Reusable Telegram tab strip matching FilterTabsView.java and PagerSlidingTabStrip.java.
 *
 * - Height: 44dp / 48dp
 * - Indicator: bottom rounded line (height 3dp, corner radius 1.5dp) animating with EASE_OUT_QUINT
 * - Scroll-to-center on select
 * - Badge counters support
 * - Day / night authentic Telegram colors
 */
class TelegramTabsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val tabsContainer = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val indicatorRect = RectF()
    private var indicatorAnimator: ValueAnimator? = null
    private var indicatorProgress = 1.0f

    private var currentPosition: Int = 0
    private var fromIndicatorLeft = 0f
    private var fromIndicatorRight = 0f
    private var toIndicatorLeft = 0f
    private var toIndicatorRight = 0f

    var onTabSelectedListener: ((position: Int) -> Unit)? = null

    init {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        setWillNotDraw(false)

        addView(tabsContainer)
    }

    fun addTab(title: String, count: Int = 0) {
        val position = tabsContainer.childCount
        val tabView = createTabView(title, count, position)
        tabsContainer.addView(tabView)
        if (position == 0) {
            selectTab(0, notify = false, animated = false)
        }
    }

    fun setTabs(tabs: List<Pair<String, Int>>) {
        tabsContainer.removeAllViews()
        for (i in tabs.indices) {
            val (title, count) = tabs[i]
            val tabView = createTabView(title, count, i)
            tabsContainer.addView(tabView)
        }
        if (tabs.isNotEmpty()) {
            selectTab(0.coerceAtMost(currentPosition), notify = false, animated = false)
        }
    }

    private fun createTabView(title: String, count: Int, position: Int): View {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val tabLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(16f), 0, dp(16f), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            val mask = GradientDrawable().apply {
                cornerRadius = dp(6f).toFloat()
                setColor(Color.WHITE)
            }
            background = RippleDrawable(ColorStateList.valueOf(palette.actionSelector), null, mask)

            isFocusable = true
            isClickable = true
            setOnClickListener {
                selectTab(position, notify = true, animated = true)
            }
        }

        val tvTitle = TextView(context).apply {
            id = android.R.id.text1
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            includeFontPadding = false
            setTextColor(if (position == currentPosition) palette.accent else palette.textSecondary)
            typeface = if (position == currentPosition) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
        tabLayout.addView(tvTitle)

        if (count > 0) {
            val tvCount = TextView(context).apply {
                id = android.R.id.text2
                text = count.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                includeFontPadding = false
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(6f), dp(2f), dp(6f), dp(2f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(9f).toFloat()
                    setColor(if (position == currentPosition) palette.accent else palette.textSecondary)
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(6f)
                }
                layoutParams = lp
            }
            tabLayout.addView(tvCount)
        }

        return tabLayout
    }

    fun selectTab(position: Int, notify: Boolean = true, animated: Boolean = true) {
        if (position < 0 || position >= tabsContainer.childCount) return
        val prevPosition = currentPosition
        currentPosition = position

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        indicatorPaint.color = palette.accent

        // Update tab views typography and colors
        for (i in 0 until tabsContainer.childCount) {
            val container = tabsContainer.getChildAt(i) as? LinearLayout ?: continue
            val titleView = container.findViewById<TextView>(android.R.id.text1)
            val countView = container.findViewById<TextView>(android.R.id.text2)
            val isSelected = (i == position)

            titleView?.setTextColor(if (isSelected) palette.accent else palette.textSecondary)
            titleView?.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            if (countView != null) {
                (countView.background as? GradientDrawable)?.setColor(
                    if (isSelected) palette.accent else palette.textSecondary
                )
            }
        }

        post {
            animateIndicator(prevPosition, position, animated)
            scrollToCenter(position)
        }

        if (notify) {
            onTabSelectedListener?.invoke(position)
        }
    }

    private fun scrollToCenter(position: Int) {
        val tab = tabsContainer.getChildAt(position) ?: return
        val targetX = tab.left + tab.width / 2 - width / 2
        smoothScrollTo(targetX.coerceAtLeast(0), 0)
    }

    private fun animateIndicator(fromPos: Int, toPos: Int, animated: Boolean) {
        val fromTab = tabsContainer.getChildAt(fromPos)
        val toTab = tabsContainer.getChildAt(toPos) ?: return

        fromIndicatorLeft = fromTab?.let { (it.left + dp(12f)).toFloat() } ?: (toTab.left + dp(12f)).toFloat()
        fromIndicatorRight = fromTab?.let { (it.right - dp(12f)).toFloat() } ?: (toTab.right - dp(12f)).toFloat()
        toIndicatorLeft = (toTab.left + dp(12f)).toFloat()
        toIndicatorRight = (toTab.right - dp(12f)).toFloat()

        if (!animated) {
            indicatorRect.set(
                toIndicatorLeft,
                (height - dp(3f)).toFloat(),
                toIndicatorRight,
                height.toFloat()
            )
            invalidate()
            return
        }

        indicatorAnimator?.cancel()
        indicatorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                val p = it.animatedValue as Float
                val left = fromIndicatorLeft + (toIndicatorLeft - fromIndicatorLeft) * p
                val right = fromIndicatorRight + (toIndicatorRight - fromIndicatorRight) * p
                indicatorRect.set(
                    left,
                    (height - dp(3f)).toFloat(),
                    right,
                    height.toFloat()
                )
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!indicatorRect.isEmpty) {
            canvas.drawRoundRect(indicatorRect, dp(1.5f).toFloat(), dp(1.5f).toFloat(), indicatorPaint)
        }
    }

    fun bindViewPager2(viewPager: ViewPager2) {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                selectTab(position, notify = false, animated = true)
            }
        })
        onTabSelectedListener = { position ->
            viewPager.setCurrentItem(position, true)
        }
    }
}

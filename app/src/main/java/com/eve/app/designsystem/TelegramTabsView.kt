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
import android.graphics.drawable.Drawable
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
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.viewpager2.widget.ViewPager2
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * Reusable Telegram tab strip matching FilterTabsView.java and PagerSlidingTabStrip.java.
 *
 * - Height: 44dp / 48dp
 * - Indicator: bottom rounded line (height 3dp, corner radius 1.5dp) or stretch segmented pill animating with EASE_OUT_QUINT
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

    val selectedTabPosition: Int
        get() = currentPosition

    val tabCount: Int
        get() = tabsContainer.childCount

    var tabMode: Int = com.google.android.material.tabs.TabLayout.MODE_FIXED
        set(value) {
            field = value
            updateTabsLayout()
        }

    var tabGravity: Int = com.google.android.material.tabs.TabLayout.GRAVITY_FILL
        set(value) {
            field = value
            updateTabsLayout()
        }

    var tabIndicatorGravity: Int = com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH
    var tabSelectedIndicator: Drawable? = null
    var tabTextColors: ColorStateList? = null

    class Tab(val position: Int, val text: String, private val tabsView: TelegramTabsView) {
        val customView: View?
            get() = (tabsView.tabsContainer.getChildAt(position) as? ViewGroup)?.findViewById<TextView>(android.R.id.text1)
                ?: tabsView.tabsContainer.getChildAt(position)

        fun select() {
            tabsView.selectTab(position, notify = true, animated = true)
        }
    }

    fun getTabAt(index: Int): Tab? {
        if (index < 0 || index >= tabsContainer.childCount) return null
        val tv = (tabsContainer.getChildAt(index) as? ViewGroup)?.findViewById<TextView>(android.R.id.text1)
        return Tab(index, tv?.text?.toString().orEmpty(), this)
    }

    interface OnTabSelectedListener {
        fun onTabSelected(tab: Tab?)
        fun onTabUnselected(tab: Tab?) {}
        fun onTabReselected(tab: Tab?) {}
    }

    private val tabListeners = mutableListOf<OnTabSelectedListener>()
    private val materialTabListeners = mutableListOf<com.google.android.material.tabs.TabLayout.OnTabSelectedListener>()

    fun addOnTabSelectedListener(listener: OnTabSelectedListener) {
        tabListeners.add(listener)
    }

    fun addOnTabSelectedListener(listener: com.google.android.material.tabs.TabLayout.OnTabSelectedListener) {
        materialTabListeners.add(listener)
    }

    init {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        setWillNotDraw(false)

        addView(tabsContainer)

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        indicatorPaint.color = palette.accent
        tabSelectedIndicator = GradientDrawable().apply {
            cornerRadius = dp(34f).toFloat()
            setColor(palette.accent)
        }
        tabTextColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(
                palette.onAccent,
                ContextCompat.getColor(context, R.color.text_primary)
            )
        )

        attrs?.let {
            val a = context.obtainStyledAttributes(it, com.google.android.material.R.styleable.TabLayout, defStyleAttr, 0)
            if (a.hasValue(com.google.android.material.R.styleable.TabLayout_tabMode)) {
                tabMode = a.getInt(com.google.android.material.R.styleable.TabLayout_tabMode, com.google.android.material.tabs.TabLayout.MODE_FIXED)
            }
            if (a.hasValue(com.google.android.material.R.styleable.TabLayout_tabGravity)) {
                tabGravity = a.getInt(com.google.android.material.R.styleable.TabLayout_tabGravity, com.google.android.material.tabs.TabLayout.GRAVITY_FILL)
            }
            if (a.hasValue(com.google.android.material.R.styleable.TabLayout_tabIndicatorGravity)) {
                tabIndicatorGravity = a.getInt(com.google.android.material.R.styleable.TabLayout_tabIndicatorGravity, com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH)
            }
            a.recycle()
        }
    }

    private fun updateTabsLayout() {
        val isFixed = (tabMode == com.google.android.material.tabs.TabLayout.MODE_FIXED || tabGravity == com.google.android.material.tabs.TabLayout.GRAVITY_FILL)
        for (i in 0 until tabsContainer.childCount) {
            val child = tabsContainer.getChildAt(i)
            val lp = (child.layoutParams as? LinearLayout.LayoutParams) ?: LinearLayout.LayoutParams(
                if (isFixed) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            lp.width = if (isFixed) 0 else LinearLayout.LayoutParams.WRAP_CONTENT
            lp.weight = if (isFixed) 1f else 0f
            child.layoutParams = lp
        }
        tabsContainer.layoutParams = LayoutParams(
            if (isFixed) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT,
            LayoutParams.MATCH_PARENT
        )
    }

    fun addTab(title: String, count: Int = 0) {
        val position = tabsContainer.childCount
        val tabView = createTabView(title, count, position)
        val isFixed = (tabMode == com.google.android.material.tabs.TabLayout.MODE_FIXED || tabGravity == com.google.android.material.tabs.TabLayout.GRAVITY_FILL)
        tabView.layoutParams = LinearLayout.LayoutParams(
            if (isFixed) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (isFixed) 1f else 0f
        )
        tabsContainer.addView(tabView)
        if (position == 0) {
            selectTab(0, notify = false, animated = false)
        }
    }

    fun setTabs(tabs: List<Pair<String, Int>>) {
        tabsContainer.removeAllViews()
        val isFixed = (tabMode == com.google.android.material.tabs.TabLayout.MODE_FIXED || tabGravity == com.google.android.material.tabs.TabLayout.GRAVITY_FILL)
        for (i in tabs.indices) {
            val (title, count) = tabs[i]
            val tabView = createTabView(title, count, i)
            tabView.layoutParams = LinearLayout.LayoutParams(
                if (isFixed) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                if (isFixed) 1f else 0f
            )
            tabsContainer.addView(tabView)
        }
        if (tabs.isNotEmpty()) {
            selectTab(0.coerceAtMost(currentPosition), notify = false, animated = false)
        }
    }

    private fun createTabView(title: String, count: Int, position: Int): View {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val isFixed = (tabMode == com.google.android.material.tabs.TabLayout.MODE_FIXED || tabGravity == com.google.android.material.tabs.TabLayout.GRAVITY_FILL)
        val tabLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val hPad = if (isFixed) 0 else dp(16f)
            setPadding(hPad, 0, hPad, 0)
            layoutParams = LinearLayout.LayoutParams(
                if (isFixed) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                if (isFixed) 1f else 0f
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

        val isSelected = (position == currentPosition)
        val tvTitle = TextView(context).apply {
            id = android.R.id.text1
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            includeFontPadding = false
            setTextColor(if (isSelected) palette.onAccent else ContextCompat.getColor(context, R.color.text_primary))
            typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
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
                    setColor(if (isSelected) palette.accent else palette.textSecondary)
                }
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
        (tabSelectedIndicator as? GradientDrawable)?.setColor(palette.accent)

        // Update tab views typography and colors
        for (i in 0 until tabsContainer.childCount) {
            val container = tabsContainer.getChildAt(i) as? LinearLayout ?: continue
            val titleView = container.findViewById<TextView>(android.R.id.text1)
            val countView = container.findViewById<TextView>(android.R.id.text2)
            val isSelected = (i == position)

            titleView?.setTextColor(if (isSelected) palette.onAccent else ContextCompat.getColor(context, R.color.text_primary))
            titleView?.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

            if (countView != null) {
                (countView.background as? GradientDrawable)?.setColor(
                    if (isSelected) palette.accent else palette.textSecondary
                )
            }
        }

        val tab = tabsContainer.getChildAt(position)
        if (tab != null && tab.width > 0) {
            val isStretch = (tabIndicatorGravity == com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH)
            val inset = if (isStretch) 0f else dp(12f).toFloat()
            val left = tab.left.toFloat() + inset
            val right = tab.right.toFloat() - inset
            val top = if (isStretch) paddingTop.toFloat() else (height - dp(3f)).toFloat()
            val bottom = if (isStretch) (height - paddingBottom).toFloat() else height.toFloat()
            indicatorRect.set(left, top, right, bottom)
            invalidate()
            if (animated) {
                animateIndicator(prevPosition, position, true)
            }
        } else {
            post {
                animateIndicator(prevPosition, position, animated)
                scrollToCenter(position)
            }
        }

        if (notify) {
            onTabSelectedListener?.invoke(position)
            val tabObj = getTabAt(position)
            tabListeners.forEach { it.onTabSelected(tabObj) }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val tab = tabsContainer.getChildAt(currentPosition)
        if (tab != null && tab.width > 0) {
            val isStretch = (tabIndicatorGravity == com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH)
            val inset = if (isStretch) 0f else dp(12f).toFloat()
            val left = tab.left.toFloat() + inset
            val right = tab.right.toFloat() - inset
            val top = if (isStretch) paddingTop.toFloat() else (height - dp(3f)).toFloat()
            val bottom = if (isStretch) (height - paddingBottom).toFloat() else height.toFloat()
            indicatorRect.set(left, top, right, bottom)
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

        val isStretch = (tabIndicatorGravity == com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH)
        val inset = if (isStretch) 0f else dp(12f).toFloat()

        fromIndicatorLeft = (fromTab?.left?.toFloat() ?: toTab.left.toFloat()) + inset
        fromIndicatorRight = (fromTab?.right?.toFloat() ?: toTab.right.toFloat()) - inset
        toIndicatorLeft = toTab.left.toFloat() + inset
        toIndicatorRight = toTab.right.toFloat() - inset

        val top = if (isStretch) paddingTop.toFloat() else (height - dp(3f)).toFloat()
        val bottom = if (isStretch) (height - paddingBottom).toFloat() else height.toFloat()

        if (!animated) {
            indicatorRect.set(
                toIndicatorLeft,
                top,
                toIndicatorRight,
                bottom
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
                    top,
                    right,
                    bottom
                )
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!indicatorRect.isEmpty) {
            val isStretch = (tabIndicatorGravity == com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH)
            val radius = if (isStretch) dp(34f).toFloat() else dp(1.5f).toFloat()
            canvas.drawRoundRect(indicatorRect, radius, radius, indicatorPaint)
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

    fun updatePalette(palette: TelegramThemeEngine.ResolvedPalette) {
        indicatorPaint.color = palette.accent
        (tabSelectedIndicator as? GradientDrawable)?.setColor(palette.accent)
        tabTextColors = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(
                palette.onAccent,
                ContextCompat.getColor(context, R.color.text_primary)
            )
        )
        for (i in 0 until tabsContainer.childCount) {
            val tabView = tabsContainer.getChildAt(i) as? LinearLayout ?: continue
            val mask = GradientDrawable().apply {
                cornerRadius = dp(6f).toFloat()
                setColor(Color.WHITE)
            }
            tabView.background = RippleDrawable(ColorStateList.valueOf(palette.actionSelector), null, mask)
            val tvTitle = tabView.findViewById<TextView>(android.R.id.text1)
            tvTitle?.setTextColor(if (i == currentPosition) palette.onAccent else ContextCompat.getColor(context, R.color.text_primary))
            val tvCount = tabView.findViewById<TextView>(android.R.id.text2)
            (tvCount?.background as? GradientDrawable)?.setColor(if (i == currentPosition) palette.accent else palette.textSecondary)
        }
        invalidate()
    }
}

/*
 * Derived from Telegram Android FilterTabsView.java, Copyright Nikolai Kudashov, 2013-2018.
 * GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.ui.admin.tg

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.PathInterpolator
import android.graphics.Typeface
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.theme.TelegramThemeEngine

/**
 * ADAPTED EVE tabs based on the original Telegram FilterTabsView indicator.
 * Verified archive: Telegram-master.zip SHA-256 0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3
 * FilterTabsView.java:915-918,1509-1512 -> indicator 14dp radius, 28dp tall, alpha 31.
 * FilterTabsView.java:837,855-880 -> 320ms EASE_OUT_QUINT.
 * CubicBezierInterpolator.java:13 -> (.23, 1, .32, 1).
 * Tab layout remains EVE's equal-width LinearLayout (ADAPTED, not Telegram's RecyclerListView).
 */
class TelegramSegmentedTabs @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density).toInt()

    private val tabContainer = LinearLayout(context)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorRect = RectF()

    private var selectedIndex = 0
    private var currentIndicatorX = 0f
    private var currentIndicatorWidth = 0f
    private var targetIndicatorX = 0f
    private var targetIndicatorWidth = 0f
    private var animator: ValueAnimator? = null

    private val tabs = mutableListOf<String>()
    private val tabViews = mutableListOf<TextView>()

    var onTabSelectedListener: ((position: Int) -> Unit)? = null

    init {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        indicatorPaint.color = palette.accent
        indicatorPaint.alpha = 31
        setWillNotDraw(false)

        tabContainer.orientation = LinearLayout.HORIZONTAL
        tabContainer.gravity = Gravity.CENTER_VERTICAL
        addView(tabContainer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        tabContainer.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (animator?.isRunning != true) updateIndicatorBounds(selectedIndex, animated = false)
        }
    }

    fun setTabs(titles: List<String>, defaultIndex: Int = 0) {
        animator?.cancel()
        animator = null
        currentIndicatorWidth = 0f
        tabs.clear()
        tabs.addAll(titles)
        tabViews.clear()
        tabContainer.removeAllViews()
        selectedIndex = defaultIndex.coerceIn(0, (titles.size - 1).coerceAtLeast(0))

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)

        for ((index, title) in titles.withIndex()) {
            val tv = TextView(context).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
                isClickable = true
                isFocusable = true
                setBackgroundResource(outValue.resourceId)
                setTextColor(if (index == selectedIndex) palette.accent else palette.textSecondary)
                isSelected = index == selectedIndex
                setOnClickListener { selectTab(index, animated = true) }
            }
            tabViews.add(tv)
            val lp = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            tabContainer.addView(tv, lp)
        }

        post {
            updateIndicatorBounds(selectedIndex, animated = false)
        }
    }

    fun selectTab(index: Int, animated: Boolean = true) {
        if (index !in tabs.indices || index == selectedIndex) return
        selectedIndex = index

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        for ((i, tv) in tabViews.withIndex()) {
            tv.setTextColor(if (i == selectedIndex) palette.accent else palette.textSecondary)
            tv.isSelected = i == selectedIndex
        }

        updateIndicatorBounds(selectedIndex, animated)
        onTabSelectedListener?.invoke(selectedIndex)
    }

    private fun updateIndicatorBounds(index: Int, animated: Boolean) {
        if (index !in tabViews.indices) return
        val tv = tabViews[index]
        // The Telegram selector follows the label plus 12.5dp internal padding on both sides.
        // Equal-width EVE tabs remain ADAPTED; text-driven geometry is source-derived.
        val maxWidth = (tv.width - dp(8f)).coerceAtLeast(0).toFloat()
        targetIndicatorWidth = (tv.paint.measureText(tv.text.toString()) + dp(25f))
            .coerceAtLeast(dp(28f).toFloat()).coerceAtMost(maxWidth)
        targetIndicatorX = tv.left.toFloat() + (tv.width - targetIndicatorWidth) / 2f

        if (!animated || currentIndicatorWidth == 0f) {
            currentIndicatorX = targetIndicatorX
            currentIndicatorWidth = targetIndicatorWidth
            invalidate()
            return
        }

        val startX = currentIndicatorX
        val startW = currentIndicatorWidth
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320L
            interpolator = PathInterpolator(0.23f, 1f, 0.32f, 1f)
            addUpdateListener { va ->
                val fraction = va.animatedFraction
                currentIndicatorX = startX + (targetIndicatorX - startX) * fraction
                currentIndicatorWidth = startW + (targetIndicatorWidth - startW) * fraction
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (currentIndicatorWidth > 0) {
            val center = height / 2f
            val radius = dp(14f).toFloat()
            indicatorRect.set(currentIndicatorX, center - radius, currentIndicatorX + currentIndicatorWidth, center + radius)
            canvas.drawRoundRect(indicatorRect, radius, radius, indicatorPaint)
        }
        super.onDraw(canvas)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }
}

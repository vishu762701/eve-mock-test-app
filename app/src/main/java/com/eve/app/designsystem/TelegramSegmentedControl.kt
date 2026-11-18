/*
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/SlideSegmentsView.java:25-180 @ 3de37c8cfb03
 * Telegram: TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java:50-220 @ 3de37c8cfb03
 * Derived from Telegram Android FilledTabsView.java and FilterTabsView.java
 * DrKLO/Telegram@21f7d109da6ab3506c9e9577f98c6a38771ab335
 * Copyright Nikolai Kudashov, 2013-2018; GNU GPL v2 or later. See THIRD_PARTY_NOTICES.md.
 */

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.eve.app.R
import com.eve.app.telegram.animation.TelegramAnimationRuntime
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.FontManager
import org.telegram.ui.Components.CubicBezierInterpolator

/**
 * 1:1 Telegram segmented control based on FilterTabsView and FilledTabsView indicator physics.
 * Sliding selection pill with CubicBezierInterpolator.EASE_OUT_QUINT (320ms) and Telegram theme styling.
 */
open class TelegramSegmentedControl @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private var checkedChangeListener: ((group: View, checkedIds: List<Int>) -> Unit)? = null

    private val scrollView: HorizontalScrollView
    private val trackLayout: FrameLayout
    private val optionsRow: LinearLayout

    private val optionViews = mutableListOf<TextView>()
    private var selectedIndex = 2 // Default: All time (index 2)

    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillRect = RectF()
    private var currentPillLeft = 0f
    private var currentPillWidth = 0f
    private var targetPillLeft = 0f
    private var targetPillWidth = 0f
    private var pillAnimator: ValueAnimator? = null

    val checkedChipIds: List<Int>
        get() = if (selectedIndex in optionViews.indices) listOf(optionViews[selectedIndex].id) else emptyList()

    val checkedChipId: Int
        get() = if (selectedIndex in optionViews.indices) optionViews[selectedIndex].id else View.NO_ID

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        clipChildren = false
        clipToPadding = false

        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        pillPaint.color = palette.accent
        pillPaint.alpha = 35

        scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = OVER_SCROLL_IF_CONTENT_SCROLLS
            clipChildren = false
            clipToPadding = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        trackLayout = object : FrameLayout(context) {
            override fun dispatchDraw(canvas: Canvas) {
                if (currentPillWidth > 0f) {
                    val r = dp(14f).toFloat()
                    val top = (height - dp(32f)) / 2f
                    val bottom = top + dp(32f)
                    pillRect.set(currentPillLeft, top, currentPillLeft + currentPillWidth, bottom)
                    canvas.drawRoundRect(pillRect, r, r, pillPaint)
                }
                super.dispatchDraw(canvas)
            }
        }.apply {
            clipChildren = false
            clipToPadding = false
            minimumHeight = dp(40f)
            val p = dp(4f)
            setPadding(p, p, p, p)
            background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                dp(16f).toFloat(),
                palette.surfaceInset
            )
        }

        optionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        trackLayout.addView(optionsRow)
        scrollView.addView(trackLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(scrollView)

        createOption(R.id.chipLast7Days, "Last 7 days", 0)
        createOption(R.id.chipLast30Days, "Last 30 days", 1)
        createOption(R.id.chipAllTime, "All time", 2)

        post {
            updatePillPosition(animated = false)
            updateTextStates()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findViewById<View>(R.id.chipLast7Days)?.let { if (it !== optionViews.getOrNull(0)) removeView(it) }
        findViewById<View>(R.id.chipLast30Days)?.let { if (it !== optionViews.getOrNull(1)) removeView(it) }
        findViewById<View>(R.id.chipAllTime)?.let { if (it !== optionViews.getOrNull(2)) removeView(it) }
    }

    private fun createOption(viewId: Int, label: String, index: Int) {
        val segmentHeight = dp(36f)
        val pillPadding = dp(12f)
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)

        val option = TextView(context).apply {
            id = viewId
            text = label
            gravity = Gravity.CENTER
            typeface = FontManager.telegramMediumTypeface(context)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            includeFontPadding = false
            isSingleLine = true
            setPadding(pillPadding, 0, pillPadding, 0)
            isClickable = true
            isFocusable = true
            background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                dp(14f).toFloat(),
                palette.actionSelector
            )

            layoutParams = LinearLayout.LayoutParams(0, segmentHeight, 1f).apply {
                if (index > 0) marginStart = dp(2f)
            }

            setOnClickListener {
                selectIndex(index, animate = true, fromUser = true)
            }

            ViewCompat.setAccessibilityDelegate(this, object : androidx.core.view.AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = "android.widget.RadioButton"
                    info.isCheckable = true
                    info.isChecked = index == selectedIndex
                }
            })
        }

        optionViews.add(option)
        optionsRow.addView(option)
    }

    fun check(viewId: Int) {
        val idx = optionViews.indexOfFirst { it.id == viewId }
        if (idx in optionViews.indices && idx != selectedIndex) {
            selectIndex(idx, animate = true, fromUser = false)
        }
    }

    fun setOnCheckedStateChangeListener(listener: (group: View, checkedIds: List<Int>) -> Unit) {
        this.checkedChangeListener = listener
    }

    private fun selectIndex(index: Int, animate: Boolean, fromUser: Boolean) {
        if (index !in optionViews.indices || index == selectedIndex) return
        selectedIndex = index
        updatePillPosition(animate)
        updateTextStates()
        if (fromUser) {
            checkedChangeListener?.invoke(this, checkedChipIds)
        }
    }

    private fun updatePillPosition(animated: Boolean) {
        if (selectedIndex !in optionViews.indices) return
        val target = optionViews[selectedIndex]
        if (target.width == 0) {
            post { updatePillPosition(animated = false) }
            return
        }

        targetPillLeft = target.left.toFloat()
        targetPillWidth = target.width.toFloat()

        if (!animated || !TelegramAnimationRuntime.isMotionEnabled(context) || currentPillWidth == 0f) {
            pillAnimator?.cancel()
            pillAnimator = null
            currentPillLeft = targetPillLeft
            currentPillWidth = targetPillWidth
            trackLayout.invalidate()
            return
        }

        val startLeft = currentPillLeft
        val startWidth = currentPillWidth

        pillAnimator?.cancel()
        pillAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320L
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener { va ->
                val f = va.animatedFraction
                currentPillLeft = startLeft + (targetPillLeft - startLeft) * f
                currentPillWidth = startWidth + (targetPillWidth - startWidth) * f
                trackLayout.invalidate()
            }
            start()
        }
    }

    private fun updateTextStates() {
        val palette = TelegramThemeEngine.resolveCurrentPalette(context)
        for ((idx, view) in optionViews.withIndex()) {
            val isSelected = idx == selectedIndex
            view.setTextColor(if (isSelected) palette.accent else palette.textSecondary)
            view.isSelected = isSelected
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (selectedIndex > 0) {
                    selectIndex(selectedIndex - 1, animate = true, fromUser = true)
                    optionViews[selectedIndex].requestFocus()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (selectedIndex < optionViews.size - 1) {
                    selectIndex(selectedIndex + 1, animate = true, fromUser = true)
                    optionViews[selectedIndex].requestFocus()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDetachedFromWindow() {
        pillAnimator?.cancel()
        pillAnimator = null
        super.onDetachedFromWindow()
    }
}

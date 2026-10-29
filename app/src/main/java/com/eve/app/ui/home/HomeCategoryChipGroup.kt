package com.eve.app.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.drawable.GradientDrawable
import android.text.Layout
import kotlin.math.ceil
import android.util.AttributeSet
import android.os.SystemClock
import com.eve.app.designsystem.UiArcMotion
import org.telegram.ui.Components.CubicBezierInterpolator
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.eve.app.theme.TelegramThemeEngine

/**
 * Preserves EVE category touch/selection while drawing only the original Telegram
 * FilterTabsView selector atom. Source:
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15,
 * FilterTabsView.java:176-178,904-918,1448-1514, GPL v2-or-later.
 * Host/text measurement/selection remain EVE and are explicitly NOT a full port.
 */
class HomeCategoryChipGroup @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ChipGroup(context, attrs) {
    private fun dp(value: Float) = value * resources.displayMetrics.density
    private val selectorDrawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, null).apply {
        val radius = dp(14f)
        cornerRadii = floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
    }
    private var selectorColor: Int? = null

    private val originalInterpolator = CubicBezierInterpolator.EASE_OUT_QUINT
    private var lastRenderedBounds: RectF? = null
    private var fromBounds: RectF? = null
    private var toBounds: RectF? = null
    private var animatingIndicator = false
    private var animationTime = 0f
    private var lastAnimationTime = 0L

    // Source: FilterTabsView.java:855-881. Original frame delta cap=17ms,
    // accumulated time divided by 320ms, EASE_OUT_QUINT interpolation.
    private val indicatorAnimation = object : Runnable {
        override fun run() {
            if (!animatingIndicator) return
            val newTime = SystemClock.elapsedRealtime()
            val dt = (newTime - lastAnimationTime).coerceIn(0L, 17L)
            lastAnimationTime = newTime
            animationTime += dt / 320.0f
            val progress = originalInterpolator.getInterpolation(animationTime.coerceIn(0f, 1f))
            val from = fromBounds
            val to = toBounds
            if (from != null && to != null) {
                lastRenderedBounds = RectF(
                    lerp(from.left, to.left, progress),
                    lerp(from.top, to.top, progress),
                    lerp(from.right, to.right, progress),
                    lerp(from.bottom, to.bottom, progress)
                )
            }
            invalidate()
            if (animationTime < 1f) {
                postOnAnimation(this)
            } else {
                animatingIndicator = false
                lastRenderedBounds = toBounds?.let(::RectF)
                fromBounds = null
                toBounds = null
            }
        }
    }

    private fun lerp(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress

    private fun selectedIndicatorBounds(): RectF? {
        val selected = (0 until childCount).asSequence()
            .mapNotNull { getChildAt(it) as? Chip }
            .firstOrNull { it.isChecked && it.visibility == View.VISIBLE }
            ?: return null
        if (selected.width <= 0) return null
        val desiredWidth = ceil(Layout.getDesiredWidth(selected.text, selected.paint).toDouble()).toFloat()
        val indicatorWidth = maxOf(dp(16f), desiredWidth)
        val x = selected.x + (selected.width - indicatorWidth) / 2f
        val y = height / 2f - dp(14f)
        val padding = dp(12.5f)
        return RectF(x - padding, y, x + indicatorWidth + padding, y + dp(28f))
    }

    private fun selectedCategoryChanged() {
        removeCallbacks(indicatorAnimation)
        val target = selectedIndicatorBounds()
        val previous = lastRenderedBounds
        if (target == null || previous == null ||
            !UiArcMotion.isMotionEnabled(context) || !isAttachedToWindow ||
            (previous.left == target.left && previous.right == target.right)) {
            animatingIndicator = false
            lastRenderedBounds = target
            fromBounds = null
            toBounds = null
            invalidate()
            return
        }
        fromBounds = RectF(previous)
        toBounds = RectF(target)
        animationTime = 0f
        lastAnimationTime = SystemClock.elapsedRealtime()
        animatingIndicator = true
        postOnAnimation(indicatorAnimation)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(indicatorAnimation)
        animatingIndicator = false
        fromBounds = null
        toBounds = null
        lastRenderedBounds = null
        super.onDetachedFromWindow()
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Telegram FilterTabsView.java:1448-1514, 28dp/14dp/alpha31 selector.
        // Home uses existing single-line ChipGroup rather than Telegram RecyclerListView.
        val target = selectedIndicatorBounds()
        val shown = if (animatingIndicator) lastRenderedBounds ?: target else target
        if (!animatingIndicator) lastRenderedBounds = target?.let(::RectF)
        if (shown != null) {
            // Owner override R05: Monochrome category filter pills draw their own solid surfaces.
            // Transparent selector prevents any bright blue Telegram tab line from rendering.
            val color = Color.TRANSPARENT
            if (selectorColor != color) {
                selectorColor = color
                selectorDrawable.setColor(color)
            }
            selectorDrawable.setBounds(
                shown.left.toInt(), shown.top.toInt(),
                shown.right.toInt(), shown.bottom.toInt()
            )
            selectorDrawable.alpha = 31
            selectorDrawable.draw(canvas)
        }
        super.dispatchDraw(canvas)
    }

    init {
        val inset = Math.round(8f * resources.displayMetrics.density)
        setPaddingRelative(paddingStart, inset, paddingEnd, inset)
        minimumHeight = Math.round(48f * resources.displayMetrics.density)
        setOnCheckedStateChangeListener { _, _ -> selectedCategoryChanged() }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateTouchDelegates() }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        // Rebuild after category replacement even when the group's bounds stay unchanged.
        child.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTouchDelegates()
            invalidate()
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        removeCallbacks(indicatorAnimation)
        animatingIndicator = false
        lastRenderedBounds = null
        fromBounds = null
        toBounds = null
        updateTouchDelegates()
    }

    private fun updateTouchDelegates() {
        val regions = linkedMapOf<Region, View>()
        val delegates = (0 until childCount).map { index ->
            val child = getChildAt(index)
            val bounds = Rect().also { child.getHitRect(it) }
            val extra = ((Math.round(48f * resources.displayMetrics.density) - child.height).coerceAtLeast(0) + 1) / 2
            bounds.inset(0, -extra)
            regions[Region(bounds)] = child
            bounds to TouchDelegate(bounds, child)
        }
        if (delegates.isEmpty()) {
            touchDelegate = null
            return
        }
        touchDelegate = object : TouchDelegate(Rect(), this) {
            // TouchDelegate mutates MotionEvent coordinates. Capture one owner on DOWN and
            // route every later event in that gesture to that owner, never all siblings.
            private var gestureOwner: TouchDelegate? = null

            private fun forward(delegate: TouchDelegate, event: MotionEvent): Boolean {
                val copy = MotionEvent.obtain(event)
                return try { delegate.onTouchEvent(copy) } finally { copy.recycle() }
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureOwner = null
                        val x = event.x.toInt()
                        val y = event.y.toInt()
                        val selected = delegates.firstOrNull { (bounds, _) -> bounds.contains(x, y) }
                            ?: return false
                        val accepted = forward(selected.second, event)
                        if (accepted) gestureOwner = selected.second
                        return accepted
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val owner = gestureOwner ?: return false
                        gestureOwner = null
                        return forward(owner, event)
                    }
                    else -> return gestureOwner?.let { forward(it, event) } ?: false
                }
            }
            @androidx.annotation.RequiresApi(29)
            override fun getTouchDelegateInfo(): AccessibilityNodeInfo.TouchDelegateInfo =
                AccessibilityNodeInfo.TouchDelegateInfo(regions)
        }
    }
}

/*
 * EVE input adapter derived from Telegram ProfileActivity.java:4190-4237,
 * 4320-4340,8465-8520, DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Nikolai Kudashov / Telegram contributors. GPL v2 or later.
 */
package com.eve.app.ui.profile

import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
import android.view.animation.PathInterpolator
import androidx.core.widget.NestedScrollView
import com.eve.app.designsystem.DesignMotion
import kotlin.math.abs

/** Source-derived resistance, 0.33 expansion latch and motion; EVE NestedScrollView host. */
class ProfilePullScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : NestedScrollView(context, attrs) {
    var canExpand: () -> Boolean = { false }
    var onExpansionChanged: (Float) -> Unit = {}
    var motionEnabled: () -> Boolean = { DesignMotion.enabled(context) }
    var maximumPull: Float = 0f
        set(value) {
            val next = value.coerceAtLeast(0f)
            if (field == next) return
            field = next
            if (isExpanded) updatePull(field)
            restorePendingExpansionIfReady()
        }
    var expansionFraction: Float = 0f
        private set
    var isExpanded = false
        private set
    private var pull = 0f
    private var downY = 0f
    private var downX = 0f
    private var lastDragY = 0f
    private var dragging = false
    private var latched = false
    private var tracker: VelocityTracker? = null
    private var animator: ValueAnimator? = null
    private var pendingRestore: Boolean? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private fun updatePull(value: Float) {
        pull = value.coerceIn(0f, maximumPull)
        expansionFraction = if (maximumPull > 0) pull / maximumPull else 0f
        onExpansionChanged(expansionFraction)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                animator?.cancel(); animator = null
                downY = event.y; downX = event.x; lastDragY = event.y
                dragging = false; latched = isExpanded
            }
            MotionEvent.ACTION_MOVE -> if (beginPull(event)) return true
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (pull > 0 && !dragging) settle(isExpanded, 0f)
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    private fun beginPull(event: MotionEvent): Boolean {
        if (dragging) return true
        val accessibility = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val dy = event.y - downY
        if (event.pointerCount != 1 || !canExpand() || maximumPull <= 0 || accessibility.isTouchExplorationEnabled ||
            canScrollVertically(-1) || abs(dy) <= slop || abs(dy) <= abs(event.x - downX) || (dy <= 0 && pull <= 0)) return false
        dragging = true
        parent?.requestDisallowInterceptTouchEvent(true)
        tracker = VelocityTracker.obtain().also { it.addMovement(event) }
        val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
        super.onTouchEvent(cancel); cancel.recycle()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!dragging && (event.actionMasked != MotionEvent.ACTION_MOVE || !beginPull(event))) return super.onTouchEvent(event)
        tracker?.addMovement(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount != 1) { finishDrag(cancelled = true); return true }
                // Telegram halves drag travel before the expansion latch opens.
                val resistance = if (latched) 1f else .5f
                updatePull(pull + (event.y - lastDragY) * resistance)
                lastDragY = event.y
                if (expansionFraction >= .33f) latched = true
                if (pull <= 0f) {
                    latched = false
                    // Hand an upward continuation back to real NestedScrollView scrolling.
                    isExpanded = false; dragging = false
                    tracker?.recycle(); tracker = null
                    val down = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_DOWN }
                    super.onTouchEvent(down); down.recycle()
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> finishDrag(cancelled = true)
            MotionEvent.ACTION_UP -> finishDrag(cancelled = false)
        }
        return true
    }

    private fun finishDrag(cancelled: Boolean) {
        tracker?.computeCurrentVelocity(1000)
        val velocity = tracker?.yVelocity ?: 0f
        tracker?.recycle(); tracker = null; dragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
        settle(if (cancelled) isExpanded else latched, velocity)
    }

    fun setExpanded(expanded: Boolean, animated: Boolean = true) {
        if (expanded && !canExpand()) return
        scrollTo(0, 0)
        settle(expanded, 0f, animated)
    }

    private fun settle(expanded: Boolean, velocity: Float, animated: Boolean = true) {
        animator?.cancel(); animator = null
        isExpanded = expanded
        val target = if (expanded) maximumPull else 0f
        if (!animated || !motionEnabled() || maximumPull <= 0 || target == pull) { updatePull(target); return }
        val density = resources.displayMetrics.density
        val factor = abs(velocity).coerceIn(1100f * density, 2000f * density) / (1100f * density)
        animator = ValueAnimator.ofFloat(pull, target).apply {
            duration = (250f * abs(target - pull) / maximumPull / factor).toLong().coerceAtLeast(1)
            interpolator = PathInterpolator(.42f, 0f, .58f, 1f)
            addUpdateListener { updatePull(it.animatedValue as Float) }
            start()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        restorePendingExpansionIfReady()
    }

    private fun restorePendingExpansionIfReady() {
        val expanded = pendingRestore ?: return
        if (maximumPull <= 0) return
        pendingRestore = null
        setExpanded(expanded, animated = false)
    }

    override fun onSaveInstanceState(): Parcelable = PullState(super.onSaveInstanceState()).also { it.expanded = isExpanded }
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is PullState) { super.onRestoreInstanceState(state.superState); pendingRestore = state.expanded; restorePendingExpansionIfReady(); requestLayout() }
        else super.onRestoreInstanceState(state)
    }
    override fun onDetachedFromWindow() {
        animator?.cancel(); animator = null
        tracker?.recycle(); tracker = null; dragging = false
        updatePull(if (isExpanded) maximumPull else 0f)
        super.onDetachedFromWindow()
    }
    class PullState : BaseSavedState {
        var expanded = false
        constructor(parent: Parcelable?) : super(parent)
        constructor(parcel: Parcel) : super(parcel) { expanded = parcel.readInt() != 0 }
        override fun writeToParcel(out: Parcel, flags: Int) { super.writeToParcel(out, flags); out.writeInt(if (expanded) 1 else 0) }
        companion object {
            @JvmField val CREATOR = object : Parcelable.Creator<PullState> {
                override fun createFromParcel(parcel: Parcel) = PullState(parcel)
                override fun newArray(size: Int): Array<PullState?> = arrayOfNulls(size)
            }
        }
    }
}

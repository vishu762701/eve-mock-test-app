package com.eve.app.ui.home

import android.content.Context
import android.graphics.Rect
import android.graphics.Region
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import com.google.android.material.chip.ChipGroup

/** Keeps the 40dp visible pills while delegating a full 48dp vertical touch region. */
class HomeCategoryChipGroup @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ChipGroup(context, attrs) {
    init {
        val inset = Math.round(8f * resources.displayMetrics.density)
        setPaddingRelative(paddingStart, inset, paddingEnd, inset)
        minimumHeight = Math.round(48f * resources.displayMetrics.density)
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateTouchDelegates() }
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        // Rebuild after category replacement even when the group's bounds stay unchanged.
        child.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateTouchDelegates() }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
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

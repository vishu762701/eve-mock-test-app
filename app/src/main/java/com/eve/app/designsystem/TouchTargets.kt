package com.eve.app.designsystem

import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import androidx.core.view.doOnLayout
import com.eve.app.R

/** Expand hit regions without changing drawn bounds or replacing touch/click listeners. */
object TouchTargets {
    fun ensure(view: View) {
        view.doOnLayout { target ->
            if (target.getTag(R.id.design_touch_target) != null) return@doOnLayout
            val minimum = target.resources.getDimensionPixelSize(R.dimen.design_touch_min)
            if (target.width >= minimum && target.height >= minimum) return@doOnLayout
            var ancestor = target.parent
            while (ancestor is ViewGroup && (ancestor.width < minimum || ancestor.height < minimum)) ancestor = ancestor.parent
            val owner = ancestor as? ViewGroup ?: return@doOnLayout
            val group = owner.getTag(R.id.design_touch_group) as? Group ?: run {
                val old = owner.touchDelegate
                val created = if (Build.VERSION.SDK_INT >= 29) AccessibleGroup(owner, old) else Group(owner, old)
                owner.touchDelegate = created
                owner.setTag(R.id.design_touch_group, created)
                created
            }
            val binding = Binding(owner, target, group, minimum)
            target.setTag(R.id.design_touch_target, binding)
            binding.attach()
        }
    }

    private class Binding(val owner: ViewGroup, val target: View, val group: Group, val minimum: Int) :
        View.OnLayoutChangeListener, View.OnAttachStateChangeListener {
        fun attach() {
            target.addOnLayoutChangeListener(this)
            target.addOnAttachStateChangeListener(this)
            update()
        }
        private fun update() {
            val bounds = Rect(0, 0, target.width, target.height)
            owner.offsetDescendantRectToMyCoords(target, bounds)
            val extraX = (minimum - bounds.width()).coerceAtLeast(0)
            val extraY = (minimum - bounds.height()).coerceAtLeast(0)
            bounds.left -= extraX / 2
            bounds.right += extraX - extraX / 2
            bounds.top -= extraY / 2
            bounds.bottom += extraY - extraY / 2
            group.entries[target] = Entry(bounds, target, minimum)
        }
        override fun onLayoutChange(v: View, l: Int, t: Int, r: Int, b: Int, oldL: Int, oldT: Int, oldR: Int, oldB: Int) = update()
        override fun onViewAttachedToWindow(v: View) = update()
        override fun onViewDetachedFromWindow(v: View) {
            target.removeOnLayoutChangeListener(this)
            target.removeOnAttachStateChangeListener(this)
            target.setTag(R.id.design_touch_target, null)
            group.entries.remove(target)
            if (group.entries.isEmpty()) {
                if (owner.touchDelegate === group) owner.touchDelegate = group.previous
                owner.setTag(R.id.design_touch_group, null)
                group.dispose()
            }
        }
    }
    private class Entry(val bounds: Rect, val view: View, val minimum: Int) {
        val delegate = TouchDelegate(bounds, view)
        fun distance(x: Float, y: Float): Float =
            (x - bounds.exactCenterX()) * (x - bounds.exactCenterX()) +
                (y - bounds.exactCenterY()) * (y - bounds.exactCenterY())
    }
    private open class Group(val owner: ViewGroup, val previous: TouchDelegate?) : TouchDelegate(Rect(), owner), ViewTreeObserver.OnGlobalLayoutListener {
        private val observer = owner.viewTreeObserver
        init { observer.addOnGlobalLayoutListener(this) }
        fun dispose() { if (observer.isAlive) observer.removeOnGlobalLayoutListener(this) }
        override fun onGlobalLayout() {
            for ((view, entry) in entries.toMap()) {
                val bounds = Rect(0, 0, view.width, view.height)
                owner.offsetDescendantRectToMyCoords(view, bounds)
                val extraX = (entry.minimum - bounds.width()).coerceAtLeast(0)
                val extraY = (entry.minimum - bounds.height()).coerceAtLeast(0)
                bounds.left -= extraX / 2
                bounds.right += extraX - extraX / 2
                bounds.top -= extraY / 2
                bounds.bottom += extraY - extraY / 2
                entries[view] = Entry(bounds, view, entry.minimum)
            }
        }
        val entries = LinkedHashMap<View, Entry>()
        private var active: Entry? = null
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                active = entries.values.filter {
                    it.view.isShown && it.view.isEnabled && it.bounds.contains(event.x.toInt(), event.y.toInt())
                }.minByOrNull { it.distance(event.x, event.y) }
            }
            val handled = active?.delegate?.onTouchEvent(event) ?: previous?.onTouchEvent(event) ?: false
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) active = null
            return handled
        }
    }
    @RequiresApi(29)
    private class AccessibleGroup(owner: ViewGroup, previous: TouchDelegate?) : Group(owner, previous) {
        override fun getTouchDelegateInfo(): AccessibilityNodeInfo.TouchDelegateInfo {
            val regions = LinkedHashMap<Region, View>()
            for ((view, entry) in entries) regions[Region(entry.bounds)] = view
            if (regions.isEmpty()) regions[Region(0, 0, owner.width, owner.height)] = owner
            return AccessibilityNodeInfo.TouchDelegateInfo(regions)
        }
    }
}

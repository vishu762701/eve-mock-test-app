package com.eve.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AbsListView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.designsystem.DesignGlassLayout

/** Pinned transparent chrome at scroll top; shared Medium backdrop when content scrolls. */
class EveGlassToolbar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : DesignGlassLayout(context, attrs), ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnPreDrawListener {
    private var observer: ViewTreeObserver? = null
    private val scrollHosts = ArrayList<View>()
    init { useSquareCorners(); glassEnabled = false }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observer = rootView.viewTreeObserver.also {
            it.addOnGlobalLayoutListener(this)
            it.addOnPreDrawListener(this)
        }
        onGlobalLayout()
    }
    override fun onDetachedFromWindow() {
        observer?.takeIf { it.isAlive }?.let {
            it.removeOnGlobalLayoutListener(this)
            it.removeOnPreDrawListener(this)
        }
        observer = null
        scrollHosts.clear()
        super.onDetachedFromWindow()
    }
    override fun onGlobalLayout() {
        scrollHosts.clear()
        fun visit(view: View) {
            if (view === this) return
            if (view is RecyclerView || view is ScrollView || view is NestedScrollView || view is AbsListView)
                scrollHosts.add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(rootView)
    }
    override fun onPreDraw(): Boolean {
        // Ignore elastic, sub-pixel edge movement that otherwise toggles blur on
        // successive frames near scrollY=0. Geometry and backdrop tokens are unchanged.
        val glassNeeded = scrollHosts.any { host ->
            if (!host.isShown || !host.canScrollVertically(-1)) return@any false
            when (host) {
                is RecyclerView -> host.computeVerticalScrollOffset() > 2
                is ScrollView -> host.scrollY > 2
                is NestedScrollView -> host.scrollY > 2
                else -> true
            }
        }
        if (glassEnabled != glassNeeded) glassEnabled = glassNeeded
        return true
    }
}

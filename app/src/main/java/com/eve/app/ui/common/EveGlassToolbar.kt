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
    /** Contrasting scroll backdrop is disabled by default app-wide so upward scrolling
     * on Profile, Home, or any other screen does not reveal a contrasting top strip or seam.
     */
    var scrollBackdropEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                scrollHosts.clear()
                glassEnabled = false
            }
            invalidate()
        }

    init {
        useSquareCorners()
        drawHairline = false
        glassEnabled = false
    }
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
        if (!scrollBackdropEnabled) return
        fun visit(view: View) {
            if (view === this) return
            if (view is RecyclerView || view is ScrollView || view is NestedScrollView || view is AbsListView)
                scrollHosts.add(view)
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(rootView)
    }
    override fun onPreDraw(): Boolean {
        if (!scrollBackdropEnabled) {
            if (glassEnabled) glassEnabled = false
            return true
        }
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

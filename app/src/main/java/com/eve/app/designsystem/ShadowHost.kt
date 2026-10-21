package com.eve.app.designsystem

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R

/** Give native elevation room through scroll/pager/recycler ancestors. */
object ShadowHost {
    fun releaseClipping(view: View) {
        // Elevation only needs room inside the immediate holder/list container.
        // Mutating every ancestor up to android.R.id.content allowed shadows to
        // escape unrelated overlays and even the system-bar backing.
        val parent = view.parent as? ViewGroup ?: return
        if (parent.id == android.R.id.content) return
        parent.clipChildren = false
        parent.clipToPadding = false
        if (parent !is RecyclerView) {
            val list = parent.parent as? RecyclerView
            if (list != null) {
                list.clipChildren = false
                list.clipToPadding = false
            }
        }
    }

    /** For a strip overlaid on its scrolling content, reserve exactly its drawn height. */
    fun reserveTabStrip(strip: View, content: ViewGroup) {
        content.clipChildren = false
        content.clipToPadding = false
        val top = if (strip.visibility == View.GONE) 0 else
            strip.resources.getDimensionPixelSize(R.dimen.design_tab_height)
        content.setPadding(content.paddingLeft, top, content.paddingRight, content.paddingBottom)
        strip.bringToFront()
        releaseClipping(content)
    }
}

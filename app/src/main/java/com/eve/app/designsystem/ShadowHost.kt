package com.eve.app.designsystem

import android.view.View
import android.view.ViewGroup
import com.eve.app.R

/** Give native elevation room through scroll/pager/recycler ancestors. */
object ShadowHost {
    fun releaseClipping(view: View) {
        var ancestor = view.parent
        while (ancestor is ViewGroup) {
            ancestor.clipChildren = false
            ancestor.clipToPadding = false
            if (ancestor.id == android.R.id.content) break
            ancestor = ancestor.parent
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

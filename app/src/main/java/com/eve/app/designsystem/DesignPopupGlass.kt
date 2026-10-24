package com.eve.app.designsystem

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inspector.WindowInspector
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ListView
import androidx.annotation.RequiresApi
import com.eve.app.R

/** Decorate native popup chrome without replacing adapters, filtering, selection or listeners. */
object DesignPopupGlass {
    fun around(anchor: View, showNative: () -> Unit) {
        if (Build.VERSION.SDK_INT < 31) { showNative(); return }
        val before = windows().toSet()
        showNative()
        anchor.post {
            if (anchor.isAttachedToWindow) {
                windows().filter { it !in before }.forEach { root -> install(root, anchor.rootView as? ViewGroup) }
            }
        }
    }
    @RequiresApi(31)
    private fun windows(): List<View> = WindowInspector.getGlobalWindowViews()

    private fun install(root: View, source: ViewGroup?) {
        if (source == null || root === source || root.getTag(R.id.design_popup_glass_installed) == true) return
        val frame = root as? FrameLayout ?: return // Unknown hosts retain the exact 94% native fallback.
        val type = (root.layoutParams as? WindowManager.LayoutParams)?.type ?: return
        if (type !in listOf(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL)) return
        fun hasList(view: View): Boolean = view is AbsListView ||
            (view is ViewGroup && (0 until view.childCount).any { hasList(view.getChildAt(it)) })
        if (!hasList(frame) || frame.childCount == 0) return
        root.setTag(R.id.design_popup_glass_installed, true)
        val glass = DesignGlassLayout(root.context).apply {
            backdropRoot = source
            val padding = resources.getDimensionPixelSize(R.dimen.design_card_padding)
            setPadding(padding, padding, padding, padding)
        }
        fun clearContainerPaint(view: View) {
            if (view is ListView) {
                view.background = null
                view.divider = null
                view.dividerHeight = 0
                return // Preserve item/selector/checkable state drawing.
            }
            if (view is ViewGroup) {
                view.background = null
                view.setPadding(0, 0, 0, 0)
                view.clipChildren = false
                view.clipToPadding = false
                for (i in 0 until view.childCount) clearContainerPaint(view.getChildAt(i))
            }
        }
        clearContainerPaint(frame)
        while (frame.childCount > 0) {
            val child = frame.getChildAt(0)
            val previous = child.layoutParams
            frame.removeViewAt(0)
            glass.addView(child, FrameLayout.LayoutParams(previous))
        }
        frame.addView(glass, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        frame.outlineProvider = glass.outlineProvider
        frame.clipToOutline = true
        val sourceObserver = source.viewTreeObserver
        // Separate popup surfaces need a frame when the host backdrop changes.
        val update = ViewTreeObserver.OnPreDrawListener { if (root.isAttachedToWindow) root.postInvalidateOnAnimation(); true }
        sourceObserver.addOnPreDrawListener(update)
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = Unit
            override fun onViewDetachedFromWindow(view: View) {
                if (sourceObserver.isAlive) sourceObserver.removeOnPreDrawListener(update)
                view.removeOnAttachStateChangeListener(this)
            }
        })
    }
}

package com.eve.app.util

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver

/** Draw boundaries, not timing delays. Each callback validates its transaction owner. */
internal object ThemeFrameCoordinator {
    fun afterFrame(view: View, ready: () -> Boolean = { true }, valid: () -> Boolean = { true }, action: () -> Unit) {
        val observer = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
            private fun remove() {
                if (observer.isAlive) observer.removeOnPreDrawListener(this)
                view.removeOnAttachStateChangeListener(this)
            }
            override fun onPreDraw(): Boolean {
                if (!valid()) { remove(); return true }
                if (!view.isAttachedToWindow || !view.isLaidOut || !ready()) return true
                remove()
                afterCurrentFrame(view, valid, action)
                return true
            }
            override fun onViewDetachedFromWindow(v: View) { remove() }
            override fun onViewAttachedToWindow(v: View) {}
        }
        observer.addOnPreDrawListener(listener)
        view.addOnAttachStateChangeListener(listener)
        view.invalidate()
    }

    /** Called from an already validated pre-draw. A native reveal can run entirely
     * on RenderThread without another UI traversal, so do not wait for another pre-draw. */
    fun afterCurrentFrame(view: View, valid: () -> Boolean, action: () -> Unit) {
        var finished = false
        fun complete(stage: String) {
            if (!finished && view.isAttachedToWindow && valid()) {
                finished = true
                ThemeManager.trace(stage, view)
                action()
            }
        }
        if (Build.VERSION.SDK_INT >= 29 && view.isHardwareAccelerated) {
            view.viewTreeObserver.registerFrameCommitCallback {
                view.post { complete("hardware_frame_committed") }
            }
        } else {
            // Older platforms expose draw completion, not a hardware commit fence.
            val observer = view.viewTreeObserver
            observer.addOnDrawListener(object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    view.post {
                        if (observer.isAlive) observer.removeOnDrawListener(this)
                        complete("draw_completed_fallback")
                    }
                }
            })
        }
    }
}

package com.eve.app.util

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver

/** A draw boundary, not a delay. Callers must still validate transaction/lifecycle ownership. */
internal object ThemeFrameCoordinator {
    fun afterFrame(view: View, ready: () -> Boolean = { true }, valid: () -> Boolean = { true }, action: () -> Unit) {
        val observer = view.viewTreeObserver
        val listener = object : ViewTreeObserver.OnPreDrawListener, View.OnAttachStateChangeListener {
            private var finished = false
            private fun remove() {
                if (observer.isAlive) observer.removeOnPreDrawListener(this)
                view.removeOnAttachStateChangeListener(this)
            }
            override fun onPreDraw(): Boolean {
                if (!valid()) { finished = true; remove(); return true }
                if (!view.isAttachedToWindow || !view.isLaidOut || !ready()) return true
                remove()
                if (Build.VERSION.SDK_INT >= 29 && view.isHardwareAccelerated) {
                    view.viewTreeObserver.registerFrameCommitCallback {
                        view.post {
                            if (!finished && view.isAttachedToWindow && valid()) {
                                finished = true
                                ThemeManager.trace("hardware_frame_committed", view)
                                action()
                            }
                        }
                    }
                } else {
                    // Older platforms expose draw completion, not a hardware commit fence.
                    val drawObserver = view.viewTreeObserver
                    val drawListener = object : ViewTreeObserver.OnDrawListener {
                        override fun onDraw() {
                            view.post {
                                if (drawObserver.isAlive) drawObserver.removeOnDrawListener(this)
                                if (!finished && view.isAttachedToWindow && valid()) {
                                    finished = true
                                    ThemeManager.trace("draw_completed_fallback", view)
                                    action()
                                }
                            }
                        }
                    }
                    drawObserver.addOnDrawListener(drawListener)
                }
                return true
            }
            override fun onViewDetachedFromWindow(v: View) { finished = true; remove() }
            override fun onViewAttachedToWindow(v: View) {}
        }
        observer.addOnPreDrawListener(listener)
        view.addOnAttachStateChangeListener(listener)
        view.invalidate()
    }
}

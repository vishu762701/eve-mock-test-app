package com.eve.app.util

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/** Window-local ownership prevents an obsolete entrance from restoring dismissed blur. */
object GlassmorphismHelper {
    const val DEFAULT_BLUR_RADIUS = 80
    private class Work : View.OnAttachStateChangeListener {
        var setup: Runnable? = null
        var animator: ValueAnimator? = null
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) { cancel(v) }
    }
    private val work = java.util.WeakHashMap<View, Work>()

    private fun cancel(view: View) {
        work.remove(view)?.let {
            it.setup?.let(view::removeCallbacks)
            it.animator?.removeAllUpdateListeners()
            it.animator?.cancel()
            view.removeOnAttachStateChangeListener(it)
        }
    }

    private fun update(view: View, owner: Work, radius: Int) {
        if (Build.VERSION.SDK_INT < 31 || work[view] !== owner || !view.isAttachedToWindow) return
        try {
            val root = view.rootView
            val lp = root.layoutParams as? WindowManager.LayoutParams ?: return
            val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            lp.flags = (lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND) and
                WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            lp.dimAmount = 0f
            lp.blurBehindRadius = radius
            wm.updateViewLayout(root, lp)
            ThemeManager.trace("popup_blur_update:$radius", view)
        } catch (_: Exception) { /* Window may have detached between validation and update. */ }
    }

    private fun own(view: View): Work {
        cancel(view)
        return Work().also { work[view] = it; view.addOnAttachStateChangeListener(it) }
    }

    fun applyWindowBlur(view: View, blurRadius: Int = DEFAULT_BLUR_RADIUS, animate: Boolean = true) {
        if (Build.VERSION.SDK_INT < 31) return
        val owner = own(view)
        owner.setup = Runnable {
            if (work[view] !== owner || !view.isAttachedToWindow) return@Runnable
            owner.setup = null
            if (animate) {
                update(view, owner, 1)
                owner.animator = ValueAnimator.ofInt(1, blurRadius).apply {
                    duration = 200L
                    interpolator = FastOutSlowInInterpolator()
                    addUpdateListener { update(view, owner, it.animatedValue as Int) }
                    start()
                }
            } else update(view, owner, blurRadius)
        }.also { view.post(it) }
    }

    fun removeWindowBlur(view: View, animate: Boolean = true) {
        if (Build.VERSION.SDK_INT < 31) return
        val owner = own(view) // Cancel both posted setup and any running entrance first.
        val radius = (view.rootView.layoutParams as? WindowManager.LayoutParams)?.blurBehindRadius ?: 0
        if (animate && radius > 0 && view.isAttachedToWindow) {
            owner.animator = ValueAnimator.ofInt(radius, 0).apply {
                duration = 150L
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { update(view, owner, it.animatedValue as Int) }
                start()
            }
        } else {
            update(view, owner, 0)
            cancel(view)
        }
    }
}

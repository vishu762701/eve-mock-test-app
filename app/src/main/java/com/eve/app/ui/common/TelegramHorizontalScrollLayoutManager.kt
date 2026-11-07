package com.eve.app.ui.common

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.sqrt

/**
 * Source-backed Telegram horizontal scroll physics for item strips and question palettes.
 * Upstream provenance: FilterTabsView.java:1060-1090 & RecyclerListView.java:2159-2185.
 *
 * Features:
 * 1. Adaptive overscroll mode: OVER_SCROLL_ALWAYS during active user drag, OVER_SCROLL_NEVER during idle/programmatic scrolls.
 * 2. Telegram LinearSmoothScroller with 60dp lookahead offset and DecelerateInterpolator (min 180ms).
 */
class TelegramHorizontalScrollLayoutManager(
    context: Context
) : LinearLayoutManager(context, HORIZONTAL, false) {

    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()
    private val decelerateInterpolator = DecelerateInterpolator()

    override fun supportsPredictiveItemAnimations(): Boolean = true

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        if (position < 0 || position >= state.itemCount) return
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                var dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
                val lookahead = dp(60f)
                val edgeBuffer = dp(21f)
                val measuredWidth = recyclerView.measuredWidth

                if (dx > 0 || (dx == 0 && targetView.left - edgeBuffer < 0)) {
                    dx += lookahead
                } else if (dx < 0 || (dx == 0 && targetView.right + edgeBuffer > measuredWidth)) {
                    dx -= lookahead
                }

                val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toInt()
                val time = maxOf(180, calculateTimeForDeceleration(distance))
                if (time > 0) {
                    action.update(-dx, -dy, time, decelerateInterpolator)
                }
            }
        }
        scroller.targetPosition = position
        startSmoothScroll(scroller)
    }

    companion object {
        /**
         * Installs Telegram adaptive overscroll and horizontal layout manager on target RecyclerView.
         */
        fun install(recyclerView: RecyclerView): TelegramHorizontalScrollLayoutManager {
            val lm = TelegramHorizontalScrollLayoutManager(recyclerView.context)
            recyclerView.layoutManager = lm
            recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

            var activeTouches = 0
            recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            if (activeTouches == 0) {
                                rv.overScrollMode = View.OVER_SCROLL_ALWAYS
                            }
                            activeTouches++
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            activeTouches = maxOf(0, activeTouches - 1)
                            if (activeTouches == 0) {
                                rv.overScrollMode = View.OVER_SCROLL_NEVER
                            }
                        }
                    }
                    return false
                }

                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                    if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) {
                        activeTouches = 0
                        rv.overScrollMode = View.OVER_SCROLL_NEVER
                    }
                }

                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    if (disallowIntercept) {
                        activeTouches = 0
                    }
                }
            })
            return lm
        }

        /**
         * Installs Telegram adaptive overscroll physics on a HorizontalScrollView or View.
         * Upstream: RecyclerListView.java:2159-2185 & FilterTabsView.java:1060-1090.
         */
        fun install(view: View) {
            view.overScrollMode = View.OVER_SCROLL_NEVER
            var activeTouches = 0
            view.setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (activeTouches == 0) {
                            v.overScrollMode = View.OVER_SCROLL_ALWAYS
                        }
                        activeTouches++
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        activeTouches = maxOf(0, activeTouches - 1)
                        if (activeTouches == 0) {
                            v.overScrollMode = View.OVER_SCROLL_NEVER
                        }
                    }
                }
                false
            }
        }
    }
}

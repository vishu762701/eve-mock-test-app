package com.eve.app.ui.common

import android.graphics.Rect
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet

/**
 * Reusable helper for collapsible cards on the Result Overview screen.
 * - Animates height in place with ChangeBounds + Fade (~280ms).
 * - Rotates chevron by 180°.
 * - Scrolls card into view if bottom edge is off-screen upon expansion.
 * - State survives rotation and tab switches.
 * - TalkBack announces expanded/collapsed.
 * - Honors ANIMATOR_DURATION_SCALE == 0.
 */
class ExpandableCardHelper(
    private val cardView: View,
    private val headerView: View,
    private val chevronView: ImageView,
    private val hintView: TextView?,
    private val collapsedSummaryView: View?,
    private val expandedContentView: View,
    private val scrollView: NestedScrollView?,
    private val cardTitle: String,
    private val stateKey: String,
    private val stateStore: MutableMap<String, Boolean>? = null,
    private val onToggleListener: ((Boolean) -> Unit)? = null
) {
    var isExpanded: Boolean = stateStore?.get(stateKey) ?: false
        private set

    init {
        applyState(animate = false)
        val clickListener = View.OnClickListener {
            toggle()
        }
        headerView.setOnClickListener(clickListener)
        chevronView.setOnClickListener(clickListener)
    }

    fun toggle() {
        setExpanded(!isExpanded, animate = true)
    }

    fun setExpanded(expanded: Boolean, animate: Boolean = true) {
        if (isExpanded == expanded && animate) return
        isExpanded = expanded
        stateStore?.put(stateKey, expanded)
        applyState(animate = animate)
        onToggleListener?.invoke(expanded)
    }

    private fun applyState(animate: Boolean) {
        val context = cardView.context
        val animScale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        } catch (_: Exception) {
            1f
        }
        val shouldAnimate = animate && animScale > 0f

        if (shouldAnimate) {
            val transition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                duration = 280L
                interpolator = FastOutSlowInInterpolator()
                addTransition(ChangeBounds())
                addTransition(Fade())
            }
            val parent = cardView.parent as? ViewGroup ?: (cardView as? ViewGroup)
            if (parent != null) {
                TransitionManager.beginDelayedTransition(parent, transition)
            }
        }

        expandedContentView.visibility = if (isExpanded) View.VISIBLE else View.GONE
        collapsedSummaryView?.visibility = if (isExpanded) View.GONE else View.VISIBLE

        if (hintView != null) {
            hintView.text = if (isExpanded) "Tap to collapse" else "Tap for details"
        }

        val targetRotation = if (isExpanded) 180f else 0f
        if (shouldAnimate) {
            chevronView.animate()
                .rotation(targetRotation)
                .setDuration(280L)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        } else {
            chevronView.animate().cancel()
            chevronView.rotation = targetRotation
        }

        val statusText = if (isExpanded) "Expanded" else "Collapsed"
        headerView.contentDescription = "$cardTitle, $statusText. Double tap to ${if (isExpanded) "collapse" else "expand"}."
        if (animate) {
            cardView.announceForAccessibility("$cardTitle $statusText")
        }

        if (isExpanded && scrollView != null) {
            cardView.post {
                val scrollBounds = Rect()
                scrollView.getHitRect(scrollBounds)
                val cardBounds = Rect()
                cardView.getDrawingRect(cardBounds)
                scrollView.offsetDescendantRectToMyCoords(cardView, cardBounds)

                if (cardBounds.bottom > scrollBounds.bottom) {
                    val delta = cardBounds.bottom - scrollBounds.bottom + 24
                    scrollView.smoothScrollBy(0, delta)
                }
            }
        }
    }
}

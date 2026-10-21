package com.eve.app.designsystem

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.eve.app.R
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * UIArc Swipe Actions Component.
 * Genuine native translation of official UIArc Swipe Actions specification:
 * - Registry: https://uiarc.dev/r/swipe-actions.json
 * - Action width: 76dp (ACTION = 76)
 * - Movement slop: 8dp before horizontal swipe locks (SLOP = 8)
 * - Full swipe threshold: max(openActionWidth + 48px, 56% of rowWidth)
 * - Elastic rubber-band resistance for overswiping
 * - Velocity projection: 0.1s (PROJECTION = 0.1)
 * - Flick velocity: 900 px/s (FLICK = 900)
 * - Settle physics: Spring.SMOOTH (500ms) to 0; Spring.SNAPPY (360ms) to resting open
 * - Commit animation: Spring.SMOOTH to direction * (width + 2), followed by height collapse
 * - Single open row coordination
 * - RTL layout direction support
 * - Non-gesture accessible button fallback
 * - Instant jump with reduced motion
 */
class UiArcSwipeRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        const val ACTION_WIDTH_DP = 76f
        const val SLOP_DP = 8f
        const val PROJECTION_SECONDS = 0.1f
        const val FLICK_VELOCITY_PX_S = 900f
        const val FULL_SWIPE_EXTRA_DP = 48f
        const val FULL_SWIPE_FRACTION = 0.56f

        private var currentlyOpenRow: UiArcSwipeRow? = null

        fun closeActiveRow() {
            currentlyOpenRow?.settle(0f)
            currentlyOpenRow = null
        }
    }

    interface OnSwipeActionListener {
        fun onActionTriggered(row: UiArcSwipeRow)
    }

    private var actionListener: OnSwipeActionListener? = null

    private val density = resources.displayMetrics.density
    private val actionWidthPx = Math.round(ACTION_WIDTH_DP * density)
    private val extra48Px = Math.round(FULL_SWIPE_EXTRA_DP * density)
    private val slopPx = Math.max(ViewConfiguration.get(context).scaledTouchSlop.toFloat(), SLOP_DP * density)

    private val actionLayer: FrameLayout
    private val actionContent: LinearLayout
    private val actionIcon: ImageView
    private val actionLabel: TextView
    private var contentView: View? = null

    private var velocityTracker: VelocityTracker? = null
    private var startX = 0f
    private var startY = 0f
    private var originX = 0f
    private var isDragging = false
    private var isLockedY = false
    private var swallowNextClick = false
    private var isArmed = false
    private var isLeaving = false
    private var isOpen = false

    private var travelAnimator: ValueAnimator? = null
    private var collapseAnimator: ValueAnimator? = null

    init {
        // Underlying revealed action layer
        actionLayer = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setBackgroundColor(ContextCompat.getColor(context, R.color.system_red))
            isClickable = true
            isFocusable = false
        }

        actionContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(actionWidthPx, LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
        }

        actionIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                Math.round(24f * density),
                Math.round(24f * density)
            )
            setImageResource(R.drawable.ic_delete)
            setColorFilter(Color.WHITE)
        }

        actionLabel = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Math.round(4f * density)
            }
            text = "Remove"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        actionContent.addView(actionIcon)
        actionContent.addView(actionLabel)
        actionLayer.addView(actionContent)
        super.addView(actionLayer, 0)

        actionLayer.setOnClickListener {
            if (!isLeaving) {
                commit()
            }
        }
    }

    private fun isRtl(): Boolean = layoutDirection == View.LAYOUT_DIRECTION_RTL

    // Swipe direction: in LTR, swiping left is trailing (direction = -1). In RTL, swiping right is trailing (direction = 1).
    private fun trailingDirection(): Float = if (isRtl()) 1f else -1f

    fun setOnSwipeActionListener(listener: OnSwipeActionListener?) {
        this.actionListener = listener
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        if (child != null && child !== actionLayer) {
            contentView = child
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isLeaving) return true
        val front = contentView ?: return super.onInterceptTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                travelAnimator?.cancel()
                startX = ev.x
                startY = ev.y
                originX = front.translationX
                isDragging = false
                isLockedY = false
                swallowNextClick = false
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)

                if (currentlyOpenRow != null && currentlyOpenRow !== this) {
                    currentlyOpenRow?.settle(0f)
                    currentlyOpenRow = null
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isLockedY) return false
                val dx = ev.x - startX
                val dy = ev.y - startY
                if (!isDragging) {
                    if (hypot(dx.toDouble(), dy.toDouble()) >= slopPx) {
                        if (abs(dy) > abs(dx)) {
                            // Vertical scroll: do not intercept, pass to RecyclerView
                            isLockedY = true
                            return false
                        } else {
                            // Horizontal drag: intercept
                            isDragging = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                            velocityTracker?.addMovement(ev)
                            return true
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    return true
                }
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (isLeaving) return true
        val front = contentView ?: return super.onTouchEvent(ev)
        velocityTracker?.addMovement(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - startX
                val raw = originX + dx
                val clamped = constrain(raw)
                front.translationX = clamped
                updateActionLayer(clamped)
                checkArmed(clamped)
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val vx = velocityTracker?.xVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                if (isDragging) {
                    swallowNextClick = true
                    isDragging = false
                    release(vx)
                    return true
                } else if (isOpen && abs(originX) > 0.5f) {
                    swallowNextClick = true
                    settle(0f)
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                if (isDragging) {
                    isDragging = false
                    settle(0f)
                    return true
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun constrain(raw: Float): Float {
        val totalWidth = width.toFloat().coerceAtLeast(320f * density)
        val dir = trailingDirection()
        val isTrailing = (dir < 0f && raw <= 0f) || (dir > 0f && raw >= 0f)

        if (!isTrailing) {
            // Overshooting opposite side: elastic rubber band resistance
            return rubberBand(abs(raw), totalWidth) * (if (raw > 0f) 1f else -1f)
        }

        val distance = abs(raw)
        val limit = totalWidth
        return if (distance <= limit) {
            raw
        } else {
            val overshoot = distance - limit
            val banded = limit + rubberBand(overshoot, totalWidth)
            banded * dir
        }
    }

    private fun rubberBand(overshoot: Float, dimension: Float): Float {
        return (1f - 1f / (overshoot * 0.55f / dimension + 1f)) * dimension
    }

    private fun threshold(): Float {
        val w = width.toFloat().coerceAtLeast(320f * density)
        return maxOf((actionWidthPx + extra48Px).toFloat(), w * FULL_SWIPE_FRACTION)
    }

    private fun checkArmed(translationX: Float) {
        val distance = abs(translationX)
        val th = threshold()
        val dir = trailingDirection()
        val inDirection = (dir < 0f && translationX < 0f) || (dir > 0f && translationX > 0f)
        val nextArmed = inDirection && distance > (if (isArmed) th - 20f * density else th)
        if (nextArmed != isArmed) {
            isArmed = nextArmed
            if (isArmed) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    private fun updateActionLayer(translationX: Float) {
        val distance = abs(translationX)
        val share = distance
        val scale = (0.6f + 0.4f * (share / actionWidthPx).coerceIn(0f, 1f)).coerceIn(0.6f, 1.2f)
        actionIcon.scaleX = scale
        actionIcon.scaleY = scale
    }

    private fun release(velocity: Float) {
        val front = contentView ?: return
        val currentX = front.translationX
        val distance = abs(currentX)
        val dir = trailingDirection()
        val outward = velocity * dir // positive when traveling in commit direction
        val projected = distance + outward * PROJECTION_SECONDS
        val flung = distance > actionWidthPx && outward > FLICK_VELOCITY_PX_S && projected > threshold()

        if ((isArmed && outward > -FLICK_VELOCITY_PX_S) || flung) {
            commit(velocity)
            return
        }

        if (projected > actionWidthPx / 2f) {
            settle(dir * actionWidthPx.toFloat(), velocity)
        } else {
            settle(0f, velocity)
        }
    }

    fun settle(target: Float, velocity: Float = 0f) {
        val front = contentView ?: return
        travelAnimator?.cancel()
        isArmed = false

        if (!UiArcMotion.isMotionEnabled(context)) {
            front.translationX = target
            isOpen = target != 0f
            if (isOpen) currentlyOpenRow = this else if (currentlyOpenRow === this) currentlyOpenRow = null
            return
        }

        val start = front.translationX
        val interpolator = if (target == 0f) UiArcMotion.Spring.SMOOTH else UiArcMotion.Spring.SNAPPY
        val duration = if (target == 0f) UiArcMotion.Spring.SMOOTH.settlingDurationMs else UiArcMotion.Spring.SNAPPY.settlingDurationMs

        travelAnimator = ValueAnimator.ofFloat(start, target).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener { va ->
                val v = va.animatedValue as Float
                front.translationX = v
                updateActionLayer(v)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isOpen = target != 0f
                    if (isOpen) {
                        currentlyOpenRow = this@UiArcSwipeRow
                    } else if (currentlyOpenRow === this@UiArcSwipeRow) {
                        currentlyOpenRow = null
                    }
                }
            })
            start()
        }
    }

    fun commit(velocity: Float = 0f) {
        if (isLeaving) return
        val front = contentView ?: return
        isLeaving = true
        travelAnimator?.cancel()
        if (currentlyOpenRow === this) currentlyOpenRow = null

        val dir = trailingDirection()
        val endX = dir * (width.toFloat() + 10f * density)

        if (!UiArcMotion.isMotionEnabled(context)) {
            front.translationX = endX
            actionListener?.onActionTriggered(this)
            return
        }

        travelAnimator = ValueAnimator.ofFloat(front.translationX, endX).apply {
            duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
            interpolator = UiArcMotion.Spring.SMOOTH
            addUpdateListener { va ->
                front.translationX = va.animatedValue as Float
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    collapseAndTrigger()
                }
            })
            start()
        }
    }

    private fun collapseAndTrigger() {
        val originalHeight = height
        collapseAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
            interpolator = UiArcMotion.Spring.SMOOTH
            startDelay = 120L
            addUpdateListener { va ->
                val fraction = va.animatedValue as Float
                layoutParams.height = (originalHeight * fraction).toInt()
                alpha = fraction
                requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    actionListener?.onActionTriggered(this@UiArcSwipeRow)
                }
            })
            start()
        }
    }

    /** Reset state when row is recycled or restored via Undo. */
    fun resetRow() {
        travelAnimator?.cancel()
        collapseAnimator?.cancel()
        isLeaving = false
        isOpen = false
        isArmed = false
        contentView?.translationX = 0f
        layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        alpha = 1f
        requestLayout()
    }
}

package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.eve.app.telegram.components.CubicBezierInterpolator
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.VibrationHelper

/**
 * Authentic Telegram pull-to-refresh layout.
 * Replaces Material SwipeRefreshLayout with Telegram's curved pull foreground,
 * rotating arrow, snap height 0.85, radial progress spinner, and light haptics.
 * Reference: DrKLO/Telegram at 0aae959806c57be8ad457980f2a5c6277ff18635
 * (DialogsActivity.java, PullForegroundDrawable.java, RadialProgressView.java)
 */
class TelegramPullRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    fun interface OnRefreshListener {
        fun onRefresh()
    }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val density = resources.displayMetrics.density
    private val snapHeight = (64f * density)
    private val maxPull = snapHeight * 2f

    private val pullForegroundDrawable = PullForegroundDrawable().apply {
        setHostView(this@TelegramPullRefreshLayout)
    }

    private val progressView = TelegramRadialProgressView(context).apply {
        visibility = View.GONE
    }

    private var onRefreshListener: OnRefreshListener? = null
    private var onRefreshLambda: (() -> Unit)? = null

    private var isDragging = false
    private var startY = 0f
    private var lastY = 0f
    private var currentPull = 0f
    private var pullStartTime = 0L
    private var hasTriggeredHaptic = false
    private var settleAnimator: ValueAnimator? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var isRefreshing: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    animateToOffset(snapHeight) {
                        progressView.visibility = View.VISIBLE
                    }
                } else {
                    progressView.visibility = View.GONE
                    animateToOffset(0f) {
                        pullForegroundDrawable.reset()
                    }
                }
            }
        }

    fun setRefreshing(refreshing: Boolean) {
        this.isRefreshing = refreshing
    }

    fun setOnRefreshListener(listener: OnRefreshListener?) {
        this.onRefreshListener = listener
    }

    fun setOnRefreshListener(listener: () -> Unit) {
        this.onRefreshLambda = listener
    }

    init {
        setWillNotDraw(false)
        val progressSize = (36 * density).toInt()
        val lp = LayoutParams(progressSize, progressSize).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL or android.view.Gravity.TOP
            topMargin = (14 * density).toInt()
        }
        addView(progressView, lp)
    }

    private fun getTargetView(): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== progressView) return child
        }
        return null
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || isRefreshing) return super.onInterceptTouchEvent(ev)
        val target = getTargetView() ?: return super.onInterceptTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y
                lastY = ev.y
                isDragging = false
                hasTriggeredHaptic = false
                settleAnimator?.cancel()
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.y - startY
                if (!isDragging && dy > touchSlop && !target.canScrollVertically(-1)) {
                    isDragging = true
                    pullStartTime = System.currentTimeMillis()
                    lastY = ev.y
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }
        return isDragging
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isEnabled || isRefreshing) return super.onTouchEvent(ev)
        val target = getTargetView() ?: return super.onTouchEvent(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startY = ev.y
                lastY = ev.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val y = ev.y
                val dy = y - lastY
                lastY = y

                if (!isDragging && dy > touchSlop && !target.canScrollVertically(-1)) {
                    isDragging = true
                    pullStartTime = System.currentTimeMillis()
                }

                if (isDragging) {
                    val k = (currentPull / maxPull).coerceIn(0f, 1f)
                    val damping = (PullForegroundDrawable.startPullParallax - PullForegroundDrawable.endPullParallax * k).coerceAtLeast(0.15f)
                    currentPull = (currentPull + dy * damping).coerceAtLeast(0f)

                    val progress = (currentPull / snapHeight).coerceIn(0f, 1.5f)
                    pullForegroundDrawable.pullProgress = progress

                    if (progress >= PullForegroundDrawable.SNAP_HEIGHT && !hasTriggeredHaptic) {
                        VibrationHelper.vibrateLightHaptic(context)
                        hasTriggeredHaptic = true
                    } else if (progress < PullForegroundDrawable.SNAP_HEIGHT && hasTriggeredHaptic) {
                        hasTriggeredHaptic = false
                    }

                    target.translationY = currentPull
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    val pullingTime = System.currentTimeMillis() - pullStartTime
                    val progress = currentPull / snapHeight

                    if (progress >= PullForegroundDrawable.SNAP_HEIGHT && pullingTime >= PullForegroundDrawable.minPullingTime) {
                        isRefreshing = true
                        VibrationHelper.vibrateLightHaptic(context)
                        animateToOffset(snapHeight) {
                            progressView.visibility = View.VISIBLE
                            onRefreshListener?.onRefresh()
                            onRefreshLambda?.invoke()
                        }
                    } else {
                        animateToOffset(0f) {
                            pullForegroundDrawable.reset()
                        }
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    private fun animateToOffset(targetOffset: Float, onEnd: (() -> Unit)? = null) {
        val target = getTargetView()
        settleAnimator?.cancel()
        val startOffset = currentPull
        settleAnimator = ValueAnimator.ofFloat(startOffset, targetOffset).apply {
            duration = 250
            interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
            addUpdateListener {
                currentPull = it.animatedValue as Float
                target?.translationY = currentPull
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    currentPull = targetOffset
                    target?.translationY = targetOffset
                    invalidate()
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (currentPull > 0f) {
            val palette = TelegramThemeEngine.resolveCurrentPalette(context)
            backgroundPaint.color = palette.backgroundSecondary
            canvas.drawRect(0f, 0f, width.toFloat(), currentPull, backgroundPaint)

            if (!isRefreshing) {
                pullForegroundDrawable.draw(canvas, width, currentPull)
            }
        }
        super.dispatchDraw(canvas)
    }
}

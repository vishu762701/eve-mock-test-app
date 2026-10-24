package com.eve.app.ui.performance

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import com.eve.app.R
import com.eve.app.data.model.ScorePoint
import com.eve.app.designsystem.DesignTokens
import com.eve.app.designsystem.UiArcMotion
import com.eve.app.util.FontManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * UIArc Line Chart Component.
 * Genuine native translation of official UIArc Line Chart specification:
 * - Registry: https://uiarc.dev/r/line-chart.json
 * - Left-to-right path entrance reveal (840ms, Ease.IN_OUT)
 * - 45% viewport visibility trigger
 * - Smooth path morphing on data updates (Spring.SMOOTH)
 * - Gliding crosshair and floating tooltip card (Spring.SNAPPY)
 * - Touch scrubbing with ScrollView gesture arbitration
 * - Instant rendering for reduced motion
 */
class ScoreTrendChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val DRAW_DURATION = 840L // duration.considered * 1.75 = 480 * 1.75
        const val TRIGGER_AMOUNT = 0.45f
        const val TOOLTIP_GAP_DP = 14f
        const val TOOLTIP_RADIUS_DP = 12f
    }

    private val density = resources.displayMetrics.density
    private var points: List<ScorePoint> = emptyList()

    // Animation state
    private var drawProgress = 0f
    private var drawAnimator: ValueAnimator? = null
    private var morphAnimator: ValueAnimator? = null
    private var hasRevealed = false
    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null

    // Touch scrubbing & crosshair state
    private var isScrubbing = false
    private var activeIndex = -1
    private var targetCrosshairX = 0f
    private var animatedCrosshairX = 0f
    private var crosshairAlpha = 0f
    private var crosshairFadeAnimator: ValueAnimator? = null
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDragging = false

    // Cached morph endpoints
    private var previousPercents: FloatArray = FloatArray(0)
    private var targetPercents: FloatArray = FloatArray(0)
    private var animatedPercents: FloatArray = FloatArray(0)

    // Drawing reusable caches
    private val linePath = Path()
    private val fillPath = Path()
    private val segmentPath = Path()
    private val pathMeasure = PathMeasure()
    private val tooltipRect = RectF()
    private val globalRect = Rect()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Paints
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.systemBlue)
        strokeWidth = 2f * density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.system_blue_alpha_26)
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.systemBlue)
        style = Paint.Style.FILL
    }

    private val activeDotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.designTextPrimary)
        strokeWidth = 0.5f * density
        alpha = 38 // 15% opacity crisp gridline
    }

    private val crosshairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.systemBlue)
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.designTextSecondary)
        textSize = resources.getDimension(R.dimen.design_type_secondary)
        typeface = FontManager.typeface(context, bold = false)
    }

    private val tooltipBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.surface)
        style = Paint.Style.FILL
    }

    private val tooltipBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.hairline)
        strokeWidth = 1f * density
        style = Paint.Style.STROKE
    }

    private val tooltipTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.designTextSecondary)
        textSize = 11f * density
        typeface = FontManager.typeface(context, bold = false)
    }

    private val tooltipValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DesignTokens.color(context, R.attr.designTextPrimary)
        textSize = 13f * density
        typeface = FontManager.typeface(context, bold = true)
    }

    init {
        setupVisibilityTrigger()
    }

    private fun setupVisibilityTrigger() {
        scrollListener = ViewTreeObserver.OnScrollChangedListener {
            checkVisibilityAndTrigger()
        }
        viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    private fun checkVisibilityAndTrigger() {
        if (hasRevealed || points.size < 2) return
        if (width <= 0 || height <= 0) return

        if (!getGlobalVisibleRect(globalRect)) return
        val visibleHeight = globalRect.height().toFloat()
        val totalHeight = height.toFloat()

        if (totalHeight > 0 && (visibleHeight / totalHeight) >= TRIGGER_AMOUNT) {
            hasRevealed = true
            viewTreeObserver.removeOnScrollChangedListener(scrollListener)
            scrollListener = null
            startEntranceAnimation()
        }
    }

    fun submit(newPoints: List<ScorePoint>) {
        val oldPoints = points
        points = newPoints

        if (!UiArcMotion.isMotionEnabled(context)) {
            drawProgress = 1f
            hasRevealed = true
            animatedPercents = FloatArray(newPoints.size) { newPoints[it].percent.toFloat() }
            updateContentDescription()
            invalidate()
            return
        }

        if (!hasRevealed) {
            drawProgress = 0f
            animatedPercents = FloatArray(newPoints.size) { newPoints[it].percent.toFloat() }
            post { checkVisibilityAndTrigger() }
        } else {
            // Path morph on data changes
            previousPercents = FloatArray(newPoints.size) { i ->
                if (i < oldPoints.size) oldPoints[i].percent.toFloat() else (oldPoints.lastOrNull()?.percent?.toFloat() ?: 0f)
            }
            targetPercents = FloatArray(newPoints.size) { newPoints[it].percent.toFloat() }
            animatedPercents = previousPercents.clone()

            morphAnimator?.cancel()
            morphAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = UiArcMotion.Spring.SMOOTH.settlingDurationMs
                interpolator = UiArcMotion.Spring.SMOOTH
                addUpdateListener { va ->
                    val fraction = va.animatedValue as Float
                    for (i in targetPercents.indices) {
                        val start = if (i < previousPercents.size) previousPercents[i] else 0f
                        animatedPercents[i] = start + (targetPercents[i] - start) * fraction
                    }
                    invalidate()
                }
                start()
            }
        }

        updateContentDescription()
        invalidate()
    }

    private fun startEntranceAnimation() {
        drawAnimator?.cancel()
        drawProgress = 0f

        drawAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DRAW_DURATION
            interpolator = UiArcMotion.Ease.IN_OUT
            addUpdateListener { va ->
                drawProgress = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun updateContentDescription() {
        contentDescription = when {
            points.isEmpty() -> "Score trend chart: no data available"
            points.size == 1 -> "Score trend chart: 1 attempt, ${points.first().percent}% accuracy"
            else -> "Score trend chart: ${points.size} attempts, latest ${points.last().percent}% accuracy"
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!hasRevealed && points.size >= 2) {
            post { checkVisibilityAndTrigger() }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (points.size < 2) return super.onTouchEvent(event)

        val paddingLeft = labelPaint.measureText("100%") + 12f * density
        val paddingRight = 8f * density
        val chartW = width - paddingLeft - paddingRight
        if (chartW <= 0f) return super.onTouchEvent(event)

        val stepX = chartW / (points.size - 1)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.x
                touchDownY = event.y
                isDragging = false
                val idx = ((event.x - paddingLeft + stepX / 2f) / stepX).toInt().coerceIn(0, points.size - 1)
                activeIndex = idx
                targetCrosshairX = paddingLeft + stepX * idx
                animatedCrosshairX = targetCrosshairX
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - touchDownX)
                val dy = abs(event.y - touchDownY)
                if (!isDragging && dx > 8f * density && dx > dy) {
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    isScrubbing = true
                    showCrosshair()
                }

                if (isDragging) {
                    val idx = ((event.x - paddingLeft + stepX / 2f) / stepX).toInt().coerceIn(0, points.size - 1)
                    if (idx != activeIndex) {
                        activeIndex = idx
                        targetCrosshairX = paddingLeft + stepX * idx
                        animateCrosshairTo(targetCrosshairX)
                        val p = points[idx]
                        contentDescription = "Attempt ${idx + 1} of ${points.size}: ${p.examName}, ${p.percent}% accuracy"
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    hideCrosshair()
                }
                isDragging = false
                isScrubbing = false
                updateContentDescription()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun showCrosshair() {
        crosshairFadeAnimator?.cancel()
        crosshairFadeAnimator = ValueAnimator.ofFloat(crosshairAlpha, 1f).apply {
            duration = UiArcMotion.Duration.FAST
            interpolator = UiArcMotion.Ease.STANDARD
            addUpdateListener { va ->
                crosshairAlpha = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun hideCrosshair() {
        crosshairFadeAnimator?.cancel()
        crosshairFadeAnimator = ValueAnimator.ofFloat(crosshairAlpha, 0f).apply {
            duration = UiArcMotion.Duration.INSTANT
            interpolator = UiArcMotion.Ease.STANDARD
            addUpdateListener { va ->
                crosshairAlpha = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateCrosshairTo(targetX: Float) {
        val startX = animatedCrosshairX
        ValueAnimator.ofFloat(startX, targetX).apply {
            duration = UiArcMotion.Spring.SNAPPY.settlingDurationMs
            interpolator = UiArcMotion.Spring.SNAPPY
            addUpdateListener { va ->
                animatedCrosshairX = va.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val paddingLeft = labelPaint.measureText("100%") + 12f * density
        val paddingRight = 8f * density
        val paddingTop = labelPaint.fontSpacing
        val paddingBottom = labelPaint.fontSpacing
        val chartW = w - paddingLeft - paddingRight
        val chartH = h - paddingTop - paddingBottom
        if (chartW <= 0f || chartH <= 0f) return

        // Grid lines + Y labels at 0, 25, 50, 75, 100%
        listOf(0, 25, 50, 75, 100).forEach { v ->
            val y = paddingTop + chartH * (1 - v / 100f)
            canvas.drawLine(paddingLeft, y, w - paddingRight, y, gridPaint)
            canvas.drawText("$v%", 0f, y - (labelPaint.ascent() + labelPaint.descent()) / 2f, labelPaint)
        }

        if (points.size < 2) {
            val msg = if (points.isEmpty()) "No data available yet"
            else "Complete another test to see your trend"
            labelPaint.textAlign = Paint.Align.CENTER
            val text = android.text.StaticLayout.Builder.obtain(
                msg, 0, msg.length,
                android.text.TextPaint(labelPaint).apply { textAlign = Paint.Align.LEFT },
                chartW.toInt()
            ).setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).build()

            canvas.save()
            canvas.translate(paddingLeft, paddingTop + (chartH - text.height) / 2)
            text.draw(canvas)
            canvas.restore()
            labelPaint.textAlign = Paint.Align.LEFT

            if (points.size == 1) {
                // Draw single point marker in center
                val cx = paddingLeft + chartW / 2f
                val cy = paddingTop + chartH * (1 - points[0].percent.coerceIn(0, 100) / 100f)
                canvas.drawCircle(cx, cy, 4f * density, dotPaint)
            }
            return
        }

        val n = points.size
        val stepX = chartW / (n - 1)

        fun xOf(i: Int) = paddingLeft + stepX * i
        fun yOf(pct: Float) = paddingTop + chartH * (1 - pct.coerceIn(0f, 100f) / 100f)

        // Build full target path
        linePath.reset()
        fillPath.reset()

        val activeValues = if (animatedPercents.size == n) animatedPercents else FloatArray(n) { points[it].percent.toFloat() }

        points.forEachIndexed { i, _ ->
            val x = xOf(i)
            val y = yOf(activeValues[i])
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, paddingTop + chartH)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            if (i == n - 1) {
                fillPath.lineTo(x, paddingTop + chartH)
                fillPath.close()
            }
        }

        val currentProgress = if (hasRevealed) 1f else drawProgress

        if (currentProgress >= 1f) {
            canvas.drawPath(fillPath, fillPaint)
            canvas.drawPath(linePath, linePaint)
            points.forEachIndexed { i, _ ->
                canvas.drawCircle(xOf(i), yOf(activeValues[i]), 3f * density, dotPaint)
            }
        } else if (currentProgress > 0f) {
            pathMeasure.setPath(linePath, false)
            val pathLength = pathMeasure.length
            segmentPath.reset()
            pathMeasure.getSegment(0f, pathLength * currentProgress, segmentPath, true)

            // Fill area up to current progress
            val pos = FloatArray(2)
            pathMeasure.getPosTan(pathLength * currentProgress, pos, null)
            val currentEndX = pos[0]

            val partialFill = Path(segmentPath)
            partialFill.lineTo(currentEndX, paddingTop + chartH)
            partialFill.lineTo(paddingLeft, paddingTop + chartH)
            partialFill.close()

            canvas.drawPath(partialFill, fillPaint)
            canvas.drawPath(segmentPath, linePaint)

            // Draw dots that have been reached by the sweep
            points.forEachIndexed { i, _ ->
                val px = xOf(i)
                if (px <= currentEndX) {
                    canvas.drawCircle(px, yOf(activeValues[i]), 3f * density, dotPaint)
                }
            }
        }

        // Draw crosshair and tooltip when scrubbing
        if (crosshairAlpha > 0f && activeIndex in 0 until n) {
            crosshairPaint.alpha = (255 * crosshairAlpha).roundToInt()
            val cx = animatedCrosshairX
            val cy = yOf(activeValues[activeIndex])

            // Vertical crosshair line
            canvas.drawLine(cx, paddingTop, cx, paddingTop + chartH, crosshairPaint)

            // Active dot highlight with outer ring
            activeDotRingPaint.alpha = (255 * crosshairAlpha).roundToInt()
            dotPaint.alpha = (255 * crosshairAlpha).roundToInt()
            canvas.drawCircle(cx, cy, 6f * density, activeDotRingPaint)
            canvas.drawCircle(cx, cy, 4.5f * density, dotPaint)
            dotPaint.alpha = 255

            // Floating Tooltip Card
            drawTooltip(canvas, cx, cy, activeIndex, paddingLeft, w - paddingRight, paddingTop, paddingTop + chartH)
        }
    }

    private fun drawTooltip(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        index: Int,
        minX: Float,
        maxX: Float,
        minY: Float,
        maxY: Float
    ) {
        val p = points[index]
        val examTitle = if (p.examName.length > 22) p.examName.take(20) + "…" else p.examName
        val dateStr = try {
            if (p.timestamp > 0) dateFormat.format(Date(p.timestamp)) else "Recent"
        } catch (_: Exception) {
            "Recent"
        }
        val valueStr = "${p.percent}% Accuracy"

        val titleW = tooltipTitlePaint.measureText(examTitle)
        val dateW = tooltipTitlePaint.measureText(dateStr)
        val valueW = tooltipValuePaint.measureText(valueStr)

        val cardW = maxOf(titleW, maxOf(dateW, valueW)) + 24f * density
        val cardH = 54f * density
        val gap = TOOLTIP_GAP_DP * density
        val radius = TOOLTIP_RADIUS_DP * density

        // Horizontally center tooltip over crosshair, clamp within plot bounds
        var cardLeft = cx - cardW / 2f
        cardLeft = cardLeft.coerceIn(minX, maxX - cardW)
        val cardRight = cardLeft + cardW

        // Vertically place above crosshair, or below if near top
        var cardTop = cy - gap - cardH
        if (cardTop < minY) {
            cardTop = cy + gap
        }
        val cardBottom = cardTop + cardH

        tooltipRect.set(cardLeft, cardTop, cardRight, cardBottom)

        tooltipBgPaint.alpha = (245 * crosshairAlpha).roundToInt()
        tooltipBorderPaint.alpha = (180 * crosshairAlpha).roundToInt()
        tooltipTitlePaint.alpha = (200 * crosshairAlpha).roundToInt()
        tooltipValuePaint.alpha = (255 * crosshairAlpha).roundToInt()

        canvas.drawRoundRect(tooltipRect, radius, radius, tooltipBgPaint)
        canvas.drawRoundRect(tooltipRect, radius, radius, tooltipBorderPaint)

        val textLeft = cardLeft + 12f * density
        canvas.drawText(examTitle, textLeft, cardTop + 16f * density, tooltipTitlePaint)
        canvas.drawText(dateStr, textLeft, cardTop + 30f * density, tooltipTitlePaint)
        canvas.drawText(valueStr, textLeft, cardTop + 46f * density, tooltipValuePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        drawAnimator?.cancel()
        morphAnimator?.cancel()
        crosshairFadeAnimator?.cancel()
        scrollListener?.let { viewTreeObserver.removeOnScrollChangedListener(it) }
        scrollListener = null
    }
}

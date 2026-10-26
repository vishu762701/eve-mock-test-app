package com.eve.app.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import com.eve.app.R
import com.eve.app.theme.TelegramThemeEngine

/**
 * Official-source-derived flicker loading skeleton view for EVE student lists.
 * Derived from Telegram FlickerLoadingView.java:
 * - LinearGradient stops: [0.0, 0.4, 0.6, 1.0]
 * - Colors: [color1, color0, color0, color1] (color0=base, color1=highlight)
 * - TileMode: CLAMP
 * - List gradient width: 600dp
 * - Motion: SystemClock.elapsedRealtime, delta clamped (>17ms -> 16ms, <4ms -> 0ms),
 *   progresses by deltaTime * height / 400f, wraps at 2x height to -2x gradientWidth.
 * - Colors: Light base #FFFFFFFF / highlight #0F000000; Dark base #FF282829 / highlight #0DFFFFFF.
 * - Placeholder geometry is derived from authentic EVE row layouts (item_exam, item_exam_test, item_history).
 */
class TelegramFlickerSkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class RowType {
        HOME_EXAM,
        EXAM_TEST,
        HISTORY
    }

    var rowType: RowType = RowType.HOME_EXAM
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    var itemsCount: Int = 5
        set(value) {
            field = value.coerceAtLeast(1)
            invalidate()
        }

    // Reusable drawing members — no allocations in onDraw
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val matrix = Matrix()
    private val cardRect = RectF()
    private val elemRect = RectF()

    private var gradient: LinearGradient? = null
    private var gradientWidth: Float = 0f
    private var color0: Int = 0
    private var color1: Int = 0
    private var innerCutoutColor: Int = 0

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var lastUpdateTime: Long = 0L

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    var totalTranslation: Float = 0f

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    var isReducedMotionForTest: Boolean? = null

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.TelegramFlickerSkeletonView, defStyleAttr, 0)
            val typeIndex = a.getInt(R.styleable.TelegramFlickerSkeletonView_flickerRowType, 0)
            rowType = when (typeIndex) {
                1 -> RowType.EXAM_TEST
                2 -> RowType.HISTORY
                else -> RowType.HOME_EXAM
            }
            itemsCount = a.getInt(R.styleable.TelegramFlickerSkeletonView_flickerItemsCount, 5)
            a.recycle()
        }

        updateColors(resolveIsDark())
    }

    private fun resolveIsDark(): Boolean {
        return try {
            TelegramThemeEngine.isDarkActive(context)
        } catch (_: Exception) {
            val nightMask = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightMask == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    fun updateTheme(palette: TelegramThemeEngine.ResolvedPalette) {
        updateColors(palette.isDark)
        invalidate()
    }

    fun updateColors(isDark: Boolean) {
        val c0 = if (isDark) 0xFF282829.toInt() else 0xFFFFFFFF.toInt()
        val c1 = if (isDark) 0x0DFFFFFF else 0x0F000000
        val cutout = if (isDark) 0x16FFFFFF else 0x0F000000

        if (c0 != color0 || c1 != color1 || gradient == null) {
            color0 = c0
            color1 = c1
            innerCutoutColor = cutout
            innerCutoutPaint.color = innerCutoutColor

            gradientWidth = dp(600f)
            gradient = LinearGradient(
                0f,
                0f,
                0f,
                gradientWidth,
                intArrayOf(color1, color0, color0, color1),
                floatArrayOf(0.0f, 0.4f, 0.6f, 1.0f),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
        }
    }

    fun isReducedMotion(): Boolean {
        isReducedMotionForTest?.let { return it }
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            if (scale == 0f) return true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!ValueAnimator.areAnimatorsEnabled()) return true
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Advance animation matrix by time delta. Clamped: >17ms -> 16ms, <4ms -> 0ms.
     * Advances by dt * height / 400f, wraps at 2x height to -2x gradientWidth.
     */
    fun updateGradient() {
        val now = SystemClock.elapsedRealtime()
        if (lastUpdateTime == 0L) {
            lastUpdateTime = now
        }
        var dt = Math.abs(now - lastUpdateTime)
        if (dt > 17) dt = 16
        if (dt < 4) dt = 0
        lastUpdateTime = now

        val h = if (measuredHeight > 0) measuredHeight else 1
        totalTranslation += dt * h / 400.0f
        if (totalTranslation >= h * 2) {
            totalTranslation = -gradientWidth * 2f
        }
        matrix.setTranslate(0f, totalTranslation)
        gradient?.setLocalMatrix(matrix)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastUpdateTime = SystemClock.elapsedRealtime()
        if (isShown && !isReducedMotion()) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isAttachedToWindow && !isReducedMotion()) {
            lastUpdateTime = SystemClock.elapsedRealtime()
            postInvalidateOnAnimation()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow && !isReducedMotion()) {
            lastUpdateTime = SystemClock.elapsedRealtime()
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        updateColors(resolveIsDark())

        val isRtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val padStart = if (isRtl) paddingRight else paddingLeft
        val padEnd = if (isRtl) paddingLeft else paddingRight

        val cardRadius = resources.getDimension(R.dimen.design_radius_card)
        val cardGap = resources.getDimension(R.dimen.design_card_gap)
        val cardPadding = resources.getDimension(R.dimen.design_card_padding)

        val rowHeight = when (rowType) {
            RowType.HOME_EXAM -> dp(80f)
            RowType.EXAM_TEST -> dp(72f)
            RowType.HISTORY -> dp(96f)
        }
        val totalRowSpan = rowHeight + cardGap
        val count = itemsCount.coerceAtLeast(1)

        var y = paddingTop.toFloat()
        var i = 0
        while (y < height && i < count) {
            cardRect.set(padStart.toFloat(), y, (width - padEnd).toFloat(), y + rowHeight)
            // 1. Draw card background with shimmer shader
            canvas.drawRoundRect(cardRect, cardRadius, cardRadius, paint)

            // 2. Draw row-specific inner elements with cutout paint
            val innerLeft = cardRect.left + cardPadding
            val innerRight = cardRect.right - cardPadding
            val innerWidth = (innerRight - innerLeft).coerceAtLeast(0f)

            when (rowType) {
                RowType.HOME_EXAM -> {
                    // Icon container: 56dp x 56dp
                    val iconSize = dp(56f)
                    val iconRadius = resources.getDimension(R.dimen.design_radius_inner)
                    val iconTop = y + (rowHeight - iconSize) / 2f
                    if (isRtl) {
                        elemRect.set(innerRight - iconSize, iconTop, innerRight, iconTop + iconSize)
                    } else {
                        elemRect.set(innerLeft, iconTop, innerLeft + iconSize, iconTop + iconSize)
                    }
                    canvas.drawRoundRect(elemRect, iconRadius, iconRadius, innerCutoutPaint)

                    // Text column
                    val sectionGap = resources.getDimension(R.dimen.design_section_gap)
                    val textStart = if (isRtl) innerLeft else innerLeft + iconSize + sectionGap
                    val textWidth = (innerWidth - iconSize - sectionGap).coerceAtLeast(0f)
                    val titleW = minOf(dp(180f), textWidth * 0.70f)
                    val subW = minOf(dp(120f), textWidth * 0.45f)
                    val titleH = dp(18f)
                    val subH = dp(14f)
                    val gap = resources.getDimension(R.dimen.design_compact_gap)
                    val textTop = y + (rowHeight - (titleH + gap + subH)) / 2f

                    // Title
                    if (isRtl) {
                        elemRect.set(innerRight - iconSize - sectionGap - titleW, textTop, innerRight - iconSize - sectionGap, textTop + titleH)
                    } else {
                        elemRect.set(textStart, textTop, textStart + titleW, textTop + titleH)
                    }
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)

                    // Subtitle
                    val subTop = textTop + titleH + gap
                    if (isRtl) {
                        elemRect.set(innerRight - iconSize - sectionGap - subW, subTop, innerRight - iconSize - sectionGap, subTop + subH)
                    } else {
                        elemRect.set(textStart, subTop, textStart + subW, subTop + subH)
                    }
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)
                }
                RowType.EXAM_TEST -> {
                    // Status chip: width 56dp, height 26dp, capsule
                    val chipW = dp(56f)
                    val chipH = dp(26f)
                    val chipRadius = chipH / 2f
                    val chipTop = y + (rowHeight - chipH) / 2f
                    if (isRtl) {
                        elemRect.set(innerLeft, chipTop, innerLeft + chipW, chipTop + chipH)
                    } else {
                        elemRect.set(innerRight - chipW, chipTop, innerRight, chipTop + chipH)
                    }
                    canvas.drawRoundRect(elemRect, chipRadius, chipRadius, innerCutoutPaint)

                    // Text column
                    val chipGap = resources.getDimension(R.dimen.design_card_gap)
                    val textWidth = (innerWidth - chipW - chipGap).coerceAtLeast(0f)
                    val titleW = minOf(dp(160f), textWidth * 0.60f)
                    val subW = minOf(dp(220f), textWidth * 0.85f)
                    val titleH = dp(18f)
                    val subH = dp(14f)
                    val gap = dp(4f)
                    val textTop = y + (rowHeight - (titleH + gap + subH)) / 2f

                    // Title
                    if (isRtl) {
                        elemRect.set(innerRight - titleW, textTop, innerRight, textTop + titleH)
                    } else {
                        elemRect.set(innerLeft, textTop, innerLeft + titleW, textTop + titleH)
                    }
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)

                    // Subtitle
                    val subTop = textTop + titleH + gap
                    if (isRtl) {
                        elemRect.set(innerRight - subW, subTop, innerRight, subTop + subH)
                    } else {
                        elemRect.set(innerLeft, subTop, innerLeft + subW, subTop + subH)
                    }
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)
                }
                RowType.HISTORY -> {
                    // Vertical column
                    val titleW = minOf(dp(200f), innerWidth * 0.65f)
                    val metaW = minOf(dp(130f), innerWidth * 0.45f)
                    val scoreW = minOf(dp(100f), innerWidth * 0.35f)
                    val breakW = minOf(dp(150f), innerWidth * 0.50f)
                    val titleH = dp(18f)
                    val metaH = dp(12f)
                    val scoreH = dp(14f)
                    val breakH = dp(12f)

                    var currY = y + cardPadding
                    // Title
                    if (isRtl) elemRect.set(innerRight - titleW, currY, innerRight, currY + titleH)
                    else elemRect.set(innerLeft, currY, innerLeft + titleW, currY + titleH)
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)

                    // Meta
                    currY += titleH + dp(4f)
                    if (isRtl) elemRect.set(innerRight - metaW, currY, innerRight, currY + metaH)
                    else elemRect.set(innerLeft, currY, innerLeft + metaW, currY + metaH)
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)

                    // Score
                    currY += metaH + resources.getDimension(R.dimen.design_card_gap)
                    if (isRtl) elemRect.set(innerRight - scoreW, currY, innerRight, currY + scoreH)
                    else elemRect.set(innerLeft, currY, innerLeft + scoreW, currY + scoreH)
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)

                    // Breakdown
                    currY += scoreH + dp(2f)
                    if (isRtl) elemRect.set(innerRight - breakW, currY, innerRight, currY + breakH)
                    else elemRect.set(innerLeft, currY, innerLeft + breakW, currY + breakH)
                    canvas.drawRoundRect(elemRect, dp(4f), dp(4f), innerCutoutPaint)
                }
            }

            y += totalRowSpan
            i++
        }

        if (isShown && isAttachedToWindow && !isReducedMotion()) {
            updateGradient()
            postInvalidateOnAnimation()
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getColor0ForTest(): Int = color0

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getColor1ForTest(): Int = color1
}

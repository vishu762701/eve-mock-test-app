package com.eve.app.ui.test

import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import com.eve.app.ui.common.TelegramRadioButton
import kotlin.math.max

object QuestionFitHelper {

    const val MAX_QUESTION_SP = 18f
    const val MIN_QUESTION_SP = 15f
    const val MAX_OPTION_SP = 16f
    const val MIN_OPTION_SP = 14f

    val CANDIDATE_QUESTION_SIZES = listOf(18f, 17f, 16f, 15f)
    val CANDIDATE_OPTION_SIZES = listOf(16f, 15f, 14f)

    /**
     * Line spacing multiplier contract:
     * 1.25 for text sizes >= 17sp, 1.15 for text sizes below 17sp.
     */
    fun getLineSpacingMultiplier(sp: Float): Float = if (sp >= 17f) 1.25f else 1.15f

    /**
     * Pure testable function to pick the largest question text size from [candidates]
     * that fits within [maxHeightPx] using the provided [measureHeight] callback.
     */
    fun pickQuestionTextSize(
        candidates: List<Float> = CANDIDATE_QUESTION_SIZES,
        maxHeightPx: Int,
        measureHeight: (sp: Float, lineSpacingMult: Float) -> Int
    ): Float {
        if (maxHeightPx <= 0) return MIN_QUESTION_SP
        for (sp in candidates) {
            val spacingMult = getLineSpacingMultiplier(sp)
            val height = measureHeight(sp, spacingMult)
            if (height <= maxHeightPx) {
                return sp
            }
        }
        return MIN_QUESTION_SP
    }

    /**
     * Pure function to determine if internal scrolling is required.
     */
    fun isScrollRequired(
        maxHeightPx: Int,
        measuredHeightAtMinSp: Int
    ): Boolean = maxHeightPx > 0 && measuredHeightAtMinSp > maxHeightPx

    /**
     * Pure testable function to pick option text size (15sp down to 14sp).
     */
    fun pickOptionTextSize(
        candidates: List<Float> = CANDIDATE_OPTION_SIZES,
        maxOptionsHeightPx: Int,
        measureOptionsHeight: (sp: Float) -> Int
    ): Float {
        if (maxOptionsHeightPx <= 0) return MIN_OPTION_SP
        for (sp in candidates) {
            val height = measureOptionsHeight(sp)
            if (height <= maxOptionsHeightPx) {
                return sp
            }
        }
        return MIN_OPTION_SP
    }

    /**
     * Measures text height in pixels for given parameters using StaticLayout.
     */
    fun measureTextHeight(
        text: CharSequence,
        basePaint: TextPaint,
        textSizeSp: Float,
        lineSpacingMult: Float,
        widthPx: Int,
        scaledDensity: Float
    ): Int {
        if (widthPx <= 0 || text.isEmpty()) return 0
        val paint = TextPaint(basePaint).apply {
            textSize = textSizeSp * scaledDensity
        }
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, lineSpacingMult)
                .setIncludePad(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                paint,
                widthPx,
                Layout.Alignment.ALIGN_NORMAL,
                lineSpacingMult,
                0f,
                true
            )
        }
        return layout.height
    }

    /**
     * Pure testable measurement of actual total rendered option height.
     * Evaluates real text length and wrapping across multiline Hindi and English options,
     * accounting for TelegramRadioButton indicator padding (34dp), right padding (12dp),
     * vertical padding (20dp), min touch-target height (48dp), and 6dp bottom margin.
     */
    fun measureTotalOptionsHeight(
        optionsText: List<CharSequence>,
        basePaint: TextPaint,
        optionSp: Float,
        availableWidthPx: Int,
        density: Float,
        scaledDensity: Float
    ): Int {
        val leftPad = (34f * density).toInt()
        val rightPad = (12f * density).toInt()
        val vertPad = (20f * density).toInt()
        val minHeight = (48f * density).toInt()
        val gap = (6f * density).toInt()

        val textWidth = (availableWidthPx - leftPad - rightPad).coerceAtLeast(1)
        var total = 0
        for ((idx, text) in optionsText.withIndex()) {
            val textH = if (text.isNotEmpty()) {
                measureTextHeight(
                    text = text,
                    basePaint = basePaint,
                    textSizeSp = optionSp,
                    lineSpacingMult = 1.15f,
                    widthPx = textWidth,
                    scaledDensity = scaledDensity
                )
            } else {
                0
            }
            val singleH = max(minHeight, textH + vertPad)
            total += singleH
            if (idx > 0) {
                total += gap
            }
        }
        return total
    }

    /**
     * Computes the maximum height the question container can expand to before
     * options start being squeezed or pushed off the bottom.
     *
     * Item layout budget:
     * - Top padding (12dp) + bottom padding (8dp) = 20dp
     * - Meta row: 40dp + 8dp marginBottom = 48dp
     * - Gap between question and options = 16dp
     * Total overhead = 84dp + optionsHeightPx.
     */
    fun computeMaxQuestionHeight(
        totalAvailableHeightPx: Int,
        optionsHeightPx: Int,
        density: Float
    ): Int {
        val overheadPx = (84f * density).toInt() + optionsHeightPx
        val available = totalAvailableHeightPx - overheadPx
        val minFloor = (72f * density).toInt()
        return available.coerceAtLeast(minFloor)
    }

    /**
     * Fits question text and options to the available viewport using exact measurement.
     * Computes without flicker or recursive layout thrashing when dimensions are known.
     */
    fun fitQuestionAndOptions(
        tvQuestion: TextView,
        svQuestion: ScrollView,
        ivScrollHint: View?,
        options: List<TelegramRadioButton>,
        questionText: CharSequence,
        questionContainer: View? = null,
        rgOptions: View? = null
    ) {
        val density = svQuestion.resources.displayMetrics.density
        val scaledDensity = svQuestion.resources.displayMetrics.scaledDensity

        val containerWidth = svQuestion.width.takeIf { it > 0 }
            ?: (svQuestion.parent as? View)?.width?.takeIf { it > 0 }
            ?: (svQuestion.resources.displayMetrics.widthPixels - (32 * density).toInt())

        val parentView = (questionContainer?.parent as? View) ?: (svQuestion.parent as? View)
        val availableHeight = parentView?.height?.takeIf { it > 0 }
            ?: svQuestion.height.takeIf { it > 0 }
            ?: 0

        if (availableHeight <= 0 || containerWidth <= 0 || questionText.isEmpty()) {
            // Container not measured yet: post once safely
            svQuestion.post {
                if (svQuestion.isAttachedToWindow) {
                    fitQuestionAndOptions(
                        tvQuestion,
                        svQuestion,
                        ivScrollHint,
                        options,
                        questionText,
                        questionContainer,
                        rgOptions
                    )
                }
            }
            return
        }

        // 1. Measure actual rendered options height dynamically
        val optionsText = options.map { it.text ?: "" }
        val samplePaint = if (options.isNotEmpty()) options.first().paint else tvQuestion.paint

        // Compute available budget for options after overhead (84dp) and minimum question floor (72dp)
        val maxOptionsBudget = availableHeight - (156f * density).toInt()

        val chosenOptionSp = pickOptionTextSize(
            candidates = CANDIDATE_OPTION_SIZES,
            maxOptionsHeightPx = maxOptionsBudget,
            measureOptionsHeight = { sp ->
                measureTotalOptionsHeight(
                    optionsText = optionsText,
                    basePaint = samplePaint,
                    optionSp = sp,
                    availableWidthPx = containerWidth,
                    density = density,
                    scaledDensity = scaledDensity
                )
            }
        )

        options.forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, chosenOptionSp)
        }

        val optionsHeight = if (rgOptions != null && rgOptions.height > 0) {
            rgOptions.height
        } else {
            measureTotalOptionsHeight(
                optionsText = optionsText,
                basePaint = samplePaint,
                optionSp = chosenOptionSp,
                availableWidthPx = containerWidth,
                density = density,
                scaledDensity = scaledDensity
            )
        }

        // 2. Compute budget for question container
        val maxAllowedHeight = computeMaxQuestionHeight(availableHeight, optionsHeight, density)

        // 3. Pick question text size
        val chosenQuestionSp = pickQuestionTextSize(
            candidates = CANDIDATE_QUESTION_SIZES,
            maxHeightPx = maxAllowedHeight,
            measureHeight = { sp, mult ->
                measureTextHeight(
                    text = questionText,
                    basePaint = tvQuestion.paint,
                    textSizeSp = sp,
                    lineSpacingMult = mult,
                    widthPx = containerWidth,
                    scaledDensity = scaledDensity
                )
            }
        )

        tvQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, chosenQuestionSp)
        tvQuestion.setLineSpacing(0f, getLineSpacingMultiplier(chosenQuestionSp))

        val minSpHeight = measureTextHeight(
            text = questionText,
            basePaint = tvQuestion.paint,
            textSizeSp = MIN_QUESTION_SP,
            lineSpacingMult = getLineSpacingMultiplier(MIN_QUESTION_SP),
            widthPx = containerWidth,
            scaledDensity = scaledDensity
        )

        val needsScroll = isScrollRequired(maxAllowedHeight, minSpHeight)
        if (questionContainer != null) {
            val targetContainerHeight = if (needsScroll) maxAllowedHeight else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            val targetSvHeight = if (needsScroll) android.view.ViewGroup.LayoutParams.MATCH_PARENT else android.view.ViewGroup.LayoutParams.WRAP_CONTENT

            val changed = (questionContainer.layoutParams.height != targetContainerHeight) ||
                          (svQuestion.layoutParams.height != targetSvHeight)

            if (changed) {
                questionContainer.layoutParams.height = targetContainerHeight
                svQuestion.layoutParams.height = targetSvHeight
                questionContainer.requestLayout()
            }
        }

        if (ivScrollHint != null) {
            ivScrollHint.visibility = if (needsScroll) View.VISIBLE else View.GONE
            if (needsScroll) {
                svQuestion.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    val maxScroll = tvQuestion.height - svQuestion.height
                    if (scrollY >= maxScroll - (8 * density).toInt()) {
                        ivScrollHint.visibility = View.GONE
                    } else {
                        ivScrollHint.visibility = View.VISIBLE
                    }
                }
            } else {
                svQuestion.setOnScrollChangeListener(null)
            }
        }
    }
}

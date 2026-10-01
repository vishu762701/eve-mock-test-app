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

object QuestionFitHelper {

    const val MAX_QUESTION_SP = 18f
    const val MIN_QUESTION_SP = 15f
    const val MAX_OPTION_SP = 15f
    const val MIN_OPTION_SP = 14f

    val CANDIDATE_QUESTION_SIZES = listOf(18f, 17f, 16f, 15f)
    val CANDIDATE_OPTION_SIZES = listOf(15f, 14f)

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
     * Fits question text and options to the available viewport.
     * Computes without flicker when dimensions are already known (e.g. ViewPager swipe).
     */
    fun fitQuestionAndOptions(
        tvQuestion: TextView,
        svQuestion: ScrollView,
        ivScrollHint: View?,
        options: List<TelegramRadioButton>,
        questionText: CharSequence
    ) {
        // Reset to baseline defaults on each bind / re-run
        tvQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, MAX_QUESTION_SP)
        tvQuestion.setLineSpacing(0f, getLineSpacingMultiplier(MAX_QUESTION_SP))
        options.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, MAX_OPTION_SP) }

        val containerWidth = svQuestion.width.takeIf { it > 0 }
            ?: (svQuestion.parent as? View)?.width?.takeIf { it > 0 }
            ?: (svQuestion.resources.displayMetrics.widthPixels - (32 * svQuestion.resources.displayMetrics.density).toInt())

        val containerHeight = svQuestion.height.takeIf { it > 0 }
            ?: (svQuestion.parent as? View)?.height?.takeIf { it > 0 }
            ?: 0

        val scaledDensity = svQuestion.resources.displayMetrics.scaledDensity

        if (containerHeight > 0 && containerWidth > 0 && questionText.isNotEmpty()) {
            val chosenSp = pickQuestionTextSize(
                candidates = CANDIDATE_QUESTION_SIZES,
                maxHeightPx = containerHeight,
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

            tvQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, chosenSp)
            val mult = getLineSpacingMultiplier(chosenSp)
            tvQuestion.setLineSpacing(0f, mult)

            val minSpHeight = measureTextHeight(
                text = questionText,
                basePaint = tvQuestion.paint,
                textSizeSp = MIN_QUESTION_SP,
                lineSpacingMult = getLineSpacingMultiplier(MIN_QUESTION_SP),
                widthPx = containerWidth,
                scaledDensity = scaledDensity
            )

            val needsScroll = isScrollRequired(containerHeight, minSpHeight)
            if (ivScrollHint != null) {
                ivScrollHint.visibility = if (needsScroll) View.VISIBLE else View.GONE
                if (needsScroll) {
                    svQuestion.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                        val maxScroll = tvQuestion.height - svQuestion.height
                        if (scrollY >= maxScroll - 8) {
                            ivScrollHint.visibility = View.GONE
                        } else {
                            ivScrollHint.visibility = View.VISIBLE
                        }
                    }
                } else {
                    svQuestion.setOnScrollChangeListener(null)
                }
            }
        } else {
            // Container not measured yet (first draw): wait for layout
            svQuestion.post {
                if (svQuestion.isAttachedToWindow) {
                    fitQuestionAndOptions(
                        tvQuestion,
                        svQuestion,
                        ivScrollHint,
                        options,
                        questionText
                    )
                }
            }
        }

        // If options are very long (multi-line options in Hindi or complex exams),
        // step down options from 15sp to 14sp so all 4 remain fully visible
        val hasVeryLongOption = options.any { (it.text?.length ?: 0) > 75 }
        if (hasVeryLongOption) {
            options.forEach { it.setTextSize(TypedValue.COMPLEX_UNIT_SP, MIN_OPTION_SP) }
        }
    }
}

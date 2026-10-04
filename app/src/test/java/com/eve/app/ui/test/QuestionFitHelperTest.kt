package com.eve.app.ui.test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionFitHelperTest {

    @Test
    fun testLineSpacingMultiplier_contract() {
        assertEquals(1.25f, QuestionFitHelper.getLineSpacingMultiplier(18f), 0.001f)
        assertEquals(1.25f, QuestionFitHelper.getLineSpacingMultiplier(17f), 0.001f)
        assertEquals(1.15f, QuestionFitHelper.getLineSpacingMultiplier(16f), 0.001f)
        assertEquals(1.15f, QuestionFitHelper.getLineSpacingMultiplier(15f), 0.001f)
        assertEquals(1.15f, QuestionFitHelper.getLineSpacingMultiplier(14f), 0.001f)
    }

    @Test
    fun testPickQuestionTextSize_shortQuestionFitsAt18sp() {
        // Height at each sp: 18sp -> 60px, 17sp -> 55px, 16sp -> 50px, 15sp -> 45px
        val maxHeight = 100
        val chosen = QuestionFitHelper.pickQuestionTextSize(
            maxHeightPx = maxHeight,
            measureHeight = { sp, _ -> (sp * 3.33f).toInt() }
        )
        assertEquals(18f, chosen, 0.001f)
    }

    @Test
    fun testPickQuestionTextSize_mediumQuestionStepsDownTo17sp() {
        val maxHeight = 110
        // At 18sp height is 120px (>110), at 17sp height is 105px (<=110)
        val chosen = QuestionFitHelper.pickQuestionTextSize(
            maxHeightPx = maxHeight,
            measureHeight = { sp, _ ->
                when (sp) {
                    18f -> 120
                    17f -> 105
                    16f -> 95
                    else -> 85
                }
            }
        )
        assertEquals(17f, chosen, 0.001f)
    }

    @Test
    fun testPickQuestionTextSize_longQuestionStepsDownTo16sp() {
        val maxHeight = 100
        // At 18sp -> 140, 17sp -> 120, 16sp -> 98, 15sp -> 80
        val chosen = QuestionFitHelper.pickQuestionTextSize(
            maxHeightPx = maxHeight,
            measureHeight = { sp, _ ->
                when (sp) {
                    18f -> 140
                    17f -> 120
                    16f -> 98
                    else -> 80
                }
            }
        )
        assertEquals(16f, chosen, 0.001f)
    }

    @Test
    fun testPickQuestionTextSize_veryLongQuestionStepsDownTo15sp() {
        val maxHeight = 100
        // At 18sp -> 160, 17sp -> 140, 16sp -> 120, 15sp -> 95
        val chosen = QuestionFitHelper.pickQuestionTextSize(
            maxHeightPx = maxHeight,
            measureHeight = { sp, _ ->
                when (sp) {
                    18f -> 160
                    17f -> 140
                    16f -> 120
                    15f -> 95
                    else -> 80
                }
            }
        )
        assertEquals(15f, chosen, 0.001f)
    }

    @Test
    fun testPickQuestionTextSize_extremeNineLineQuestionHitsFloor15spAndRequiresScroll() {
        val maxHeight = 120
        // Even at 15sp, height is 180px (>120)
        val chosen = QuestionFitHelper.pickQuestionTextSize(
            maxHeightPx = maxHeight,
            measureHeight = { sp, _ ->
                when (sp) {
                    18f -> 260
                    17f -> 230
                    16f -> 205
                    15f -> 180
                    else -> 160
                }
            }
        )
        assertEquals(15f, chosen, 0.001f)

        val scrollRequired = QuestionFitHelper.isScrollRequired(
            maxHeightPx = maxHeight,
            measuredHeightAtMinSp = 180
        )
        assertTrue("Internal scrolling must be enabled when text exceeds box at 15sp", scrollRequired)
    }

    @Test
    fun testIsScrollRequired_whenFitsAtMinSp() {
        val maxHeight = 150
        val measuredHeightAtMinSp = 120
        assertFalse(QuestionFitHelper.isScrollRequired(maxHeight, measuredHeightAtMinSp))
    }

    @Test
    fun testPickOptionTextSize_stepDown() {
        val maxOptionsHeight = 200
        // 15sp requires 220px, 14sp requires 190px
        val chosen = QuestionFitHelper.pickOptionTextSize(
            maxOptionsHeightPx = maxOptionsHeight,
            measureOptionsHeight = { sp -> if (sp >= 15f) 220 else 190 }
        )
        assertEquals(14f, chosen, 0.001f)
    }

    @Test
    fun testComputeMaxQuestionHeight_subtractsOverheadAndRespectsFloor() {
        val density = 2f
        // overhead = 84dp * 2 = 168px + optionsHeight (say 400px) = 568px
        // available = 1000px - 568px = 432px
        val computed = QuestionFitHelper.computeMaxQuestionHeight(
            totalAvailableHeightPx = 1000,
            optionsHeightPx = 400,
            density = density
        )
        assertEquals(432, computed)

        // Floor test: available = 600px - 568px = 32px (< floor 72 * 2 = 144px)
        val floorComputed = QuestionFitHelper.computeMaxQuestionHeight(
            totalAvailableHeightPx = 600,
            optionsHeightPx = 400,
            density = density
        )
        assertEquals(144, floorComputed)
    }
}

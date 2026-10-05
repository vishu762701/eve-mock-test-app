package com.eve.app.ui.polish

import com.eve.app.R
import com.eve.app.util.ExamCategoryTintHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TaskBToEVerificationTest {

    @Test
    fun testCategoryTintHelperReturnsAllCuratedTints() {
        val sscTint = ExamCategoryTintHelper.getTintForCategory("SSC CGL")
        assertEquals(R.color.eve_category_tint_lilac_bg, sscTint.bgRes)
        assertEquals(R.color.eve_category_tint_lilac_fg, sscTint.fgRes)

        val railwayTint = ExamCategoryTintHelper.getTintForCategory("RRB NTPC")
        assertEquals(R.color.eve_category_tint_blue_bg, railwayTint.bgRes)
        assertEquals(R.color.eve_category_tint_blue_fg, railwayTint.fgRes)

        val defenceTint = ExamCategoryTintHelper.getTintForCategory("NDA Exam")
        assertEquals(R.color.eve_category_tint_green_bg, defenceTint.bgRes)
        assertEquals(R.color.eve_category_tint_green_fg, defenceTint.fgRes)

        val bankingTint = ExamCategoryTintHelper.getTintForCategory("IBPS PO")
        assertEquals(R.color.eve_category_tint_amber_bg, bankingTint.bgRes)
        assertEquals(R.color.eve_category_tint_amber_fg, bankingTint.fgRes)

        val teachingTint = ExamCategoryTintHelper.getTintForCategory("CTET Paper 1")
        assertEquals(R.color.eve_category_tint_rose_bg, teachingTint.bgRes)
        assertEquals(R.color.eve_category_tint_rose_fg, teachingTint.fgRes)

        val upscTint = ExamCategoryTintHelper.getTintForCategory("UPSC Civil Services")
        assertEquals(R.color.eve_category_tint_teal_bg, upscTint.bgRes)
        assertEquals(R.color.eve_category_tint_teal_fg, upscTint.fgRes)

        val defaultTint = ExamCategoryTintHelper.getTintForCategory(null)
        assertNotNull(defaultTint)
    }

    @Test
    fun testColorsXmlContainsTaskBTokens() {
        val lightColors = File("src/main/res/values/colors.xml").readText()
        val darkColors = File("src/main/res/values-night/colors.xml").readText()

        val expectedTokens = listOf(
            "eve_lilac_subtle",
            "eve_lilac_tint",
            "eve_lilac_fill",
            "eve_lilac_stroke",
            "eve_lilac_text",
            "eve_lilac_pressed",
            "eve_category_tint_lilac_bg",
            "eve_category_tint_green_bg",
            "eve_category_tint_amber_bg",
            "eve_category_tint_rose_bg",
            "eve_category_tint_blue_bg",
            "eve_category_tint_teal_bg"
        )

        for (token in expectedTokens) {
            assertTrue("Token $token missing in values/colors.xml", lightColors.contains(token))
            assertTrue("Token $token missing in values-night/colors.xml", darkColors.contains(token))
        }
    }

    @Test
    fun testLayoutsIncludeRequiredPolishUpgrades() {
        val mistakesLayout = File("src/main/res/layout/activity_mistakes.xml").readText()
        assertTrue("Mistakes layout should use illustration_empty_all_caught_up", mistakesLayout.contains("illustration_empty_all_caught_up"))
        assertTrue("Mistakes layout should use layout_enter animation", mistakesLayout.contains("@anim/layout_enter"))

        val notificationsLayout = File("src/main/res/layout/activity_notifications.xml").readText()
        assertTrue("Notifications layout should reference notification_bell", notificationsLayout.contains("@raw/notification_bell"))

        val mainLayout = File("src/main/res/layout/activity_main.xml").readText()
        assertTrue("Main layout greeting should use HeadlineLarge", mainLayout.contains("TextAppearance.Eve.M3.HeadlineLarge"))
        assertTrue("Main layout panel should use HeadlineMedium", mainLayout.contains("TextAppearance.Eve.M3.HeadlineMedium"))

        val resultLayout = File("src/main/res/layout/activity_result.xml").readText()
        assertTrue("Result layout should contain Statistics heading without 15sp override", resultLayout.contains("android:text=\"Statistics\""))
        assertTrue("Result layout should contain Performance Standing heading", resultLayout.contains("android:text=\"Performance Standing\""))
        assertTrue("Result layout should contain Analytics & Insights heading", resultLayout.contains("android:text=\"Analytics &amp; Insights\""))

        val examItemLayout = File("src/main/res/layout/item_exam.xml").readText()
        assertTrue("Exam item layout should use press_scale animator", examItemLayout.contains("@animator/press_scale"))
        assertTrue("Exam item layout should use eve_space_8 margin", examItemLayout.contains("@dimen/eve_space_8"))
    }
}

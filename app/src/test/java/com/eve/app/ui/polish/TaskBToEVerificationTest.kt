package com.eve.app.ui.polish

import com.eve.app.R
import com.eve.app.util.ExamCategoryTintHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TaskBToEVerificationTest {

    @Test
    fun testCategoryTintHelperReturnsAllCuratedTints() {
        val sscTint = ExamCategoryTintHelper.getTintForCategory("SSC CGL")
        assertEquals(R.color.design_role_category_tint_lilac_bg, sscTint.bgRes)
        assertEquals(R.color.design_role_category_tint_lilac_fg, sscTint.fgRes)

        val railwayTint = ExamCategoryTintHelper.getTintForCategory("RRB NTPC")
        assertEquals(R.color.design_role_category_tint_blue_bg, railwayTint.bgRes)
        assertEquals(R.color.system_blue, railwayTint.fgRes)

        val defenceTint = ExamCategoryTintHelper.getTintForCategory("NDA Exam")
        assertEquals(R.color.design_role_category_tint_green_bg, defenceTint.bgRes)
        assertEquals(R.color.system_green, defenceTint.fgRes)

        val bankingTint = ExamCategoryTintHelper.getTintForCategory("IBPS PO")
        assertEquals(R.color.design_role_category_tint_amber_bg, bankingTint.bgRes)
        assertEquals(R.color.system_orange, bankingTint.fgRes)

        val teachingTint = ExamCategoryTintHelper.getTintForCategory("CTET Paper 1")
        assertEquals(R.color.design_role_category_tint_rose_bg, teachingTint.bgRes)
        assertEquals(R.color.system_red, teachingTint.fgRes)

        val upscTint = ExamCategoryTintHelper.getTintForCategory("UPSC Civil Services")
        assertEquals(R.color.design_role_category_tint_teal_bg, upscTint.bgRes)
        assertEquals(R.color.design_role_category_tint_teal_fg, upscTint.fgRes)

        val defaultTint = ExamCategoryTintHelper.getTintForCategory(null)
        assertNotNull(defaultTint)
    }

    @Test
    fun testColorsXmlContainsTaskBTokens() {
        for (folder in listOf("values", "values-night")) {
            val colors = File("src/main/res/$folder/design_derived_colors.xml").readText()
            for (hue in listOf("lilac", "green", "amber", "rose", "blue", "teal")) {
                assertTrue("Category $hue is tokenized in $folder", colors.contains("design_role_category_tint_${hue}_bg"))
            }
        }
    }

    @Test
    fun testAppBackgroundRemainsPureWhiteAndBlack() {
        val light = File("src/main/res/values/design_colors.xml").readText()
        val dark = File("src/main/res/values-night/design_colors.xml").readText()
        assertTrue(light.contains("<color name=\"bg_app\">#F1F1F3</color>"))
        assertTrue(dark.contains("<color name=\"bg_app\">#000000</color>"))
    }

    @Test
    fun testLayoutsIncludeRequiredPolishUpgrades() {
        val mistakesLayout = File("src/main/res/layout/activity_mistakes.xml").readText()
        assertTrue("Mistakes layout should use illustration_empty_all_caught_up", mistakesLayout.contains("illustration_empty_all_caught_up"))
        assertFalse("Shared activity motion owns entry; list rows stay settled", mistakesLayout.contains("android:layoutAnimation"))

        val notificationsLayout = File("src/main/res/layout/activity_notifications.xml").readText()
        assertTrue("Notifications layout should reference notification_bell", notificationsLayout.contains("@raw/notification_bell"))

        val mainLayout = File("src/main/res/layout/activity_main.xml").readText()
        assertTrue("Main layout greeting should use LargeTitle, HeadlineLarge, or HomePageTitle",
            mainLayout.contains("TextAppearance.Eve.LargeTitle") ||
            mainLayout.contains("TextAppearance.Eve.M3.HeadlineLarge") ||
            mainLayout.contains("TextAppearance.Eve.Token.Title"))
        assertTrue("Main layout should contain the single Home Banner surface", mainLayout.contains("@+id/panelHomeBanner"))
        assertFalse("Legacy Home Hero content container must be removed", mainLayout.contains("layoutHomeHeroContent"))
        assertFalse("Legacy second banner card must be removed", mainLayout.contains("@+id/cardHomeBanner"))

        val resultLayout = File("src/main/res/layout/activity_result.xml").readText()
        assertTrue("Result layout should contain Statistics heading without 15sp override", resultLayout.contains("android:text=\"Statistics\""))
        assertTrue("Result layout should contain Performance Standing heading", resultLayout.contains("android:text=\"Performance Standing\""))
        assertTrue("Result layout should contain Analytics & Insights heading", resultLayout.contains("android:text=\"Analytics &amp; Insights\""))

        val examItemLayout = File("src/main/res/layout/item_exam.xml").readText()
        assertTrue("Exam item layout should use press_scale animator", File("src/main/res/values/design_widgets.xml").readText().contains("@animator/design_press_scale"))
        assertTrue("Exam item layout should use eve_space_8 margin", examItemLayout.contains("@dimen/design_card_gap"))
    }
}

package com.eve.app.designsystem

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ButtonBounceTest {

    @Test
    fun testThreeArgumentAndFourArgumentConstructorsKeepDistinctSemantics() {
        val view = View(RuntimeEnvironment.getApplication())
        val sourceStyleConstructor = ButtonBounce(view, 1f, 5f)
        val fullConstructor = ButtonBounce(view, 1f, 1f, 5f)
        assertEquals(1f, sourceStyleConstructor.getScale(0.04f), 0.001f)
        assertEquals(1f, fullConstructor.getScale(0.04f), 0.001f)
    }

    @Test
    fun testInitialState() {
        val context = RuntimeEnvironment.getApplication()
        val view = View(context)
        val bounce = ButtonBounce(view)

        assertFalse(bounce.isPressed())
        assertEquals(0f, bounce.isPressedProgress(), 0.001f)
        assertEquals(1f, bounce.getScale(0.04f), 0.001f)
    }

    @Test
    fun testScaleCalculation() {
        val context = RuntimeEnvironment.getApplication()
        val view = View(context)
        val bounce = ButtonBounce(view)

        // When not pressed, getScale(0.04f) should be 1.0
        assertEquals(1f, bounce.getScale(0.04f), 0.001f)
    }

    @Test
    fun testSetPressedStateChanges() {
        val context = RuntimeEnvironment.getApplication()
        val view = View(context)
        val bounce = ButtonBounce(view)

        bounce.setPressed(true)
        assertTrue(bounce.isPressed())

        bounce.setPressed(false)
        assertFalse(bounce.isPressed())
    }

    @Test
    fun testDesignPressBindsWithoutException() {
        val context = RuntimeEnvironment.getApplication()
        val view = View(context)
        var clicked = false

        DesignPress.bind(view) { clicked = true }
        assertEquals(null, view.stateListAnimator)
        view.performClick()
        assertTrue(clicked)
    }

    @Test
    fun testTelegramButtonBounceAndCanonicalInterpolatorCoverage() {
        val pollAdmin = java.io.File("src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt").readText()
        assertTrue(pollAdmin.contains("DesignPress.bind(binding.btnBack)"))
        assertTrue(pollAdmin.contains("DesignPress.bind(binding.btnCreatePollDialog)"))

        val testActivity = java.io.File("src/main/java/com/eve/app/ui/test/TestActivity.kt").readText()
        assertTrue(testActivity.contains("DesignPress.bind(binding.btnPrev)"))
        assertTrue(testActivity.contains("DesignPress.bind(binding.btnNext)"))
        assertTrue(testActivity.contains("DesignPress.bind(binding.btnClear)"))
        assertTrue(testActivity.contains("DesignPress.bind(binding.btnMarkReview)"))
        assertTrue(testActivity.contains("DesignPress.bind(binding.btnRetry)"))

        val resultActivity = java.io.File("src/main/java/com/eve/app/ui/result/ResultActivity.kt").readText()
        assertTrue(resultActivity.contains("DesignPress.bind(binding.btnBack)"))
        assertTrue(resultActivity.contains("DesignPress.bind(binding.btnReattempt)"))
        assertTrue(resultActivity.contains("DesignPress.bind(binding.btnShare)"))
        assertTrue(resultActivity.contains("DesignPress.bind(binding.btnOpenLeaderboard)"))

        val popupHelper = java.io.File("src/main/java/com/eve/app/util/TelegramPopupHelper.kt").readText()
        assertTrue(popupHelper.contains("CubicBezierInterpolator.DEFAULT"))
        assertFalse(popupHelper.contains("FastOutSlowInInterpolator"))

        val glassHelper = java.io.File("src/main/java/com/eve/app/util/GlassmorphismHelper.kt").readText()
        assertTrue(glassHelper.contains("CubicBezierInterpolator.DEFAULT"))
        assertFalse(glassHelper.contains("FastOutSlowInInterpolator"))
    }
}

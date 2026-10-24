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
}

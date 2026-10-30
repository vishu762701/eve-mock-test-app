package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Requirement R04 Verification:
 * Greeting Name Tracking-Expand Intro (Typography Motion).
 *
 * Verifies:
 * 1. Safe letter-spacing detection for Latin scripts and graceful fallback for Devanagari (Hindi) and RTL (Arabic/Hebrew).
 * 2. Motion constants: subtle condensed start (-0.02f ems), natural end (0f ems), duration (350ms).
 * 3. Saved-state key and lifecycle contract in MainActivity to prevent replay on rotation, onResume, or list updates.
 * 4. Layout attributes of tvWelcome in activity_main.xml (wrap_content, single line, ellipsize).
 */
class HomeGreetingMotionContractTest {

    @Test
    fun testMotionConstants() {
        assertEquals(-0.02f, HomeGreetingMotion.CONDENSED_TRACKING_EMS, 0.0001f)
        assertEquals(0f, HomeGreetingMotion.NATURAL_TRACKING_EMS, 0.0001f)
        assertEquals(350L, HomeGreetingMotion.ANIMATION_DURATION_MS)
        assertEquals("key_has_animated_greeting", HomeGreetingMotion.KEY_HAS_ANIMATED_GREETING)
    }

    @Test
    fun testTrackingSafeForLatinAndPunctuation() {
        assertTrue("Null should be safe", HomeGreetingMotion.isTrackingSafeForText(null))
        assertTrue("Empty string should be safe", HomeGreetingMotion.isTrackingSafeForText(""))
        assertTrue("Latin greeting should be safe", HomeGreetingMotion.isTrackingSafeForText("Hi, Student"))
        assertTrue("Latin name with numbers should be safe", HomeGreetingMotion.isTrackingSafeForText("Hi, Alex 2026"))
        assertTrue("Latin punctuation should be safe", HomeGreetingMotion.isTrackingSafeForText("Hello, John!"))
    }

    @Test
    fun testTrackingUnsafeForDevanagariAndIndicScripts() {
        // Devanagari characters require conjunct ligature shaping that letterSpacing disrupts
        assertFalse("Hindi name Aarti must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("Hi, आरती"))
        assertFalse("Hindi greeting Namaste must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("नमस्ते"))
        assertFalse("Devanagari conjunct must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("विद्यार्थी"))
        assertFalse("Bengali script must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("স্বাগতম"))
        assertFalse("Tamil script must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("வணக்கம்"))
    }

    @Test
    fun testTrackingUnsafeForRtlScripts() {
        // Arabic and Hebrew scripts have cursive/contextual ligatures
        assertFalse("Arabic greeting must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("مرحبا"))
        assertFalse("Hebrew greeting must not use letterSpacing", HomeGreetingMotion.isTrackingSafeForText("שלום"))
    }

    @Test
    fun testMainActivitySavedStateContract() {
        val candidates = listOf(
            File("app/src/main/java/com/eve/app/ui/home/MainActivity.kt"),
            File("src/main/java/com/eve/app/ui/home/MainActivity.kt")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("MainActivity.kt not found")
        val content = file.readText()

        assertTrue(
            "MainActivity must declare hasAnimatedWelcome state",
            content.contains("hasAnimatedWelcome")
        )
        assertTrue(
            "MainActivity must restore greeting animation state in onCreate",
            content.contains("savedInstanceState?.getBoolean(HomeGreetingMotion.KEY_HAS_ANIMATED_GREETING")
        )
        assertTrue(
            "MainActivity must persist greeting animation state in onSaveInstanceState",
            content.contains("outState.putBoolean(HomeGreetingMotion.KEY_HAS_ANIMATED_GREETING, hasAnimatedWelcome)")
        )
        assertTrue(
            "MainActivity must trigger animateIntro only on first unplayed session",
            content.contains("HomeGreetingMotion.animateIntro(binding.tvWelcome)")
        )
        assertTrue(
            "MainActivity must settle greeting natural in onResume to prevent re-animation",
            content.contains("HomeGreetingMotion.settleNatural(binding.tvWelcome)")
        )
    }

    @Test
    fun initialOnStartMustNotInterruptTheOneTimeGreetingIntro() {
        val main = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val onStart = main.substringAfter("override fun onStart()")
            .substringBefore("override fun onStop()")
        val onStop = main.substringAfter("override fun onStop()")
            .substringBefore("private fun setupFloatingAirplane()")
        assertTrue("First onStart must distinguish return-from-stop",
            onStart.contains("val returningFromStop = isReturningFromStopped"))
        assertTrue("Only a real return may settle the greeting",
            onStart.contains("if (returningFromStop) {") &&
                onStart.contains("HomeGreetingMotion.settleNatural(binding.tvWelcome)"))
        assertTrue("A running greeting animator must cancel on backgrounding",
            onStop.contains("greetingIntroAnimator?.cancel()"))
        assertTrue("Indic-script fallback ViewPropertyAnimator must also cancel",
            onStop.contains("binding.tvWelcome.animate().cancel()"))
        assertTrue("Intro is still initiated only in onCreate",
            main.substringAfter("override fun onCreate(")
                .substringBefore("override fun onStart()")
                .contains("greetingIntroAnimator = HomeGreetingMotion.animateIntro(binding.tvWelcome)"))
    }

    @Test
    fun testWelcomeTextViewLayoutContract() {
        val candidates = listOf(
            File("app/src/main/res/layout/activity_main.xml"),
            File("src/main/res/layout/activity_main.xml")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("activity_main.xml not found")

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val textViews = doc.getElementsByTagName("TextView")
        var tvWelcomeFound = false

        for (i in 0 until textViews.length) {
            val tv = textViews.item(i) as org.w3c.dom.Element
            val id = tv.getAttribute("android:id")
            if (id == "@+id/tvWelcome" || id == "tvWelcome") {
                tvWelcomeFound = true
                assertEquals("wrap_content", tv.getAttribute("android:layout_width"))
                assertEquals("wrap_content", tv.getAttribute("android:layout_height"))
                assertEquals("1", tv.getAttribute("android:maxLines"))
                assertEquals("end", tv.getAttribute("android:ellipsize"))
                assertEquals("@style/TextAppearance.Eve.Token.Title", tv.getAttribute("style"))
                break
            }
        }
        assertTrue("tvWelcome must exist in activity_main.xml", tvWelcomeFound)
    }
}

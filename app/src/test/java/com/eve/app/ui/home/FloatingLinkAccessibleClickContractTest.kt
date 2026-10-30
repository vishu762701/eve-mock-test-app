package com.eve.app.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Owner-required ordinary-user tap + accessible Home floating-community action. */
class FloatingLinkAccessibleClickContractTest {
    @Test fun realClickPerformsNavigationButAdminDragDoesNotClick() {
        val src = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val interaction = src.substringAfter("private fun setupFloatingAirplaneInteraction()")
            .substringBefore("private fun openFloatingLink()")
        val click = interaction.substringAfter("card.setOnClickListener {")
            .substringBefore("card.addOnLayoutChangeListener")
        assertTrue("Screen reader ACTION_CLICK must be wired to the real URL opener",
            click.contains("openFloatingLink()"))
        assertTrue("Clicks for every user must retain duplicate-open debounce",
            click.contains("lastLinkLaunch"))
        assertTrue("Drag must be privileged", interaction.contains(
            "if (!isFloatingAirplaneAdmin) return@Runnable"))
        val up = interaction.substringAfter("MotionEvent.ACTION_UP -> {")
            .substringBefore("MotionEvent.ACTION_CANCEL -> {")
        assertTrue("A short touch must call the same accessible click",
            up.contains("if (distance <= slop) {") && up.contains("view.performClick()"))
        assertFalse("A drag release must never directly launch the link",
            up.contains("openFloatingLink()"))
        assertTrue("Click must be reachable only from the tap branch",
            up.indexOf("view.performClick()") > up.indexOf("} else {"))
    }

    @Test fun floatingIconIsOneLabeledAccessibilityTarget() {
        val file = File("src/main/res/layout/activity_main.xml")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file).getElementsByTagName("*")
        val elements = (0 until nodes.length).map { nodes.item(it) as Element }
        val card = elements.first { it.getAttribute("android:id") == "@+id/cardFloatingAirplane" }
        val artwork = elements.first { it.getAttribute("android:id") == "@+id/lottieFloatingAirplane" }
        assertTrue(card.getAttribute("android:contentDescription") == "Open community link")
        assertTrue(card.getAttribute("android:importantForAccessibility") == "yes")
        assertTrue(artwork.getAttribute("android:importantForAccessibility") == "no")
    }
}

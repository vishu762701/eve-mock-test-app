package com.eve.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LottieContainersNoDecorationTest {

    private fun loadXml(relativePath: String): Document {
        val fileCandidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = fileCandidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }

    private fun findElementById(node: Node, targetId: String): Element? {
        if (node is Element) {
            val id = node.getAttribute("android:id")
            if (id == targetId || id == "@+id/$targetId" || id.endsWith("/$targetId")) {
                return node
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            val found = findElementById(children.item(i), targetId)
            if (found != null) return found
        }
        return null
    }

    @Test
    fun testExamIconContainer_hasZeroStrokeAndTransparentBackground() {
        val doc = loadXml("src/main/res/layout/item_exam.xml")
        val container = findElementById(doc.documentElement, "examIconContainer")
        assertNotNull("examIconContainer must exist in item_exam.xml", container)

        val strokeWidth = container!!.getAttribute("app:strokeWidth")
        assertTrue(
            "examIconContainer strokeWidth must be 0dp or 0 (was '$strokeWidth')",
            strokeWidth == "0dp" || strokeWidth == "0"
        )

        val bgColor = container.getAttribute("app:cardBackgroundColor")
        assertEquals(
            "examIconContainer cardBackgroundColor must be @android:color/transparent",
            "@android:color/transparent",
            bgColor
        )

        val elevation = container.getAttribute("app:cardElevation")
        assertTrue(
            "examIconContainer cardElevation must be 0dp or empty (was '$elevation')",
            elevation.isEmpty() || elevation == "0dp" || elevation == "0"
        )

        val rippleColor = container.getAttribute("app:rippleColor")
        assertTrue(
            "examIconContainer rippleColor must be @android:color/transparent or @null",
            rippleColor == "@android:color/transparent" || rippleColor == "@null" || rippleColor.isEmpty()
        )
    }

    @Test
    fun testCardFloatingAirplane_hasZeroStrokeAndTransparentBackground() {
        val doc = loadXml("src/main/res/layout/activity_main.xml")
        val card = findElementById(doc.documentElement, "cardFloatingAirplane")
        assertNotNull("cardFloatingAirplane must exist in activity_main.xml", card)

        val strokeWidth = card!!.getAttribute("app:strokeWidth")
        assertTrue(
            "cardFloatingAirplane strokeWidth must be 0dp or 0 (was '$strokeWidth')",
            strokeWidth == "0dp" || strokeWidth == "0"
        )

        val bgColor = card.getAttribute("app:cardBackgroundColor")
        assertEquals(
            "cardFloatingAirplane cardBackgroundColor must be @android:color/transparent",
            "@android:color/transparent",
            bgColor
        )

        val elevation = card.getAttribute("app:cardElevation")
        assertTrue(
            "cardFloatingAirplane cardElevation must be 0dp or empty (was '$elevation')",
            elevation.isEmpty() || elevation == "0dp" || elevation == "0"
        )

        val rippleColor = card.getAttribute("app:rippleColor")
        assertTrue(
            "cardFloatingAirplane rippleColor must be @android:color/transparent or @null",
            rippleColor == "@android:color/transparent" || rippleColor == "@null" || rippleColor.isEmpty()
        )
    }

    @Test
    fun testDrawerPremiumStar_hasNoDecoration() {
        val doc = loadXml("src/main/res/layout/activity_main.xml")
        val star = findElementById(doc.documentElement, "lottieDrawerPremiumStar")
        assertNotNull("lottieDrawerPremiumStar must exist in activity_main.xml", star)
        val bg = star!!.getAttribute("android:background")
        assertTrue(
            "lottieDrawerPremiumStar background must be transparent or empty (was '$bg')",
            bg.isEmpty() || bg == "@android:color/transparent" || bg == "@null"
        )
    }
}

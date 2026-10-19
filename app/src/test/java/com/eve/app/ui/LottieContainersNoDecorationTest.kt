package com.eve.app.ui

import com.eve.app.designsystem.designAttribute
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

        val bgColor = container!!.designAttribute("app:cardBackgroundColor")
        assertEquals(
            "examIconContainer cardBackgroundColor must be @color/eve_exam_icon_tile_bg",
            "@color/surface_inset",
            bgColor
        )

        val cornerRadius = container.designAttribute("app:cardCornerRadius")
        assertEquals("@dimen/design_radius_inner", cornerRadius)
        val strokeWidth = container.designAttribute("app:strokeWidth")
        assertTrue(
            "Icon tile must not add a nested outline (was '$strokeWidth')",
            strokeWidth == "@dimen/design_zero" || strokeWidth == "0dp" || strokeWidth == "0" || strokeWidth == "@dimen/eve_space_0"
        )

        val elevation = container.designAttribute("app:cardElevation")
        assertTrue(
            "examIconContainer cardElevation must be 0dp or empty (was '$elevation')",
            elevation == "@dimen/design_zero" || elevation.isEmpty() || elevation == "0dp" || elevation == "0" || elevation == "@dimen/eve_home_card_elevation" || elevation == "@dimen/eve_space_0"
        )
    }

    @Test
    fun testCardFloatingAirplane_hasZeroStrokeAndTransparentBackground() {
        val doc = loadXml("src/main/res/layout/activity_main.xml")
        val card = findElementById(doc.documentElement, "cardFloatingAirplane")
        assertNotNull("cardFloatingAirplane must exist in activity_main.xml", card)

        val strokeWidth = card!!.designAttribute("app:strokeWidth")
        assertTrue(
            "cardFloatingAirplane strokeWidth must be 0dp or 0 (was '$strokeWidth')",
            strokeWidth == "@dimen/design_zero" || strokeWidth == "0dp" || strokeWidth == "0" || strokeWidth == "@dimen/eve_space_0"
        )

        val bgColor = card.designAttribute("app:cardBackgroundColor")
        assertEquals(
            "cardFloatingAirplane cardBackgroundColor must be @android:color/transparent",
            "@color/transparent",
            bgColor
        )

        val elevation = card.designAttribute("app:cardElevation")
        assertTrue(
            "cardFloatingAirplane cardElevation must be 0dp or empty (was '$elevation')",
            elevation == "@dimen/design_zero" || elevation.isEmpty() || elevation == "0dp" || elevation == "0" || elevation == "@dimen/eve_home_card_elevation" || elevation == "@dimen/eve_space_0"
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
        val star = findElementById(doc.documentElement, "lottieDrawerPremiumStarFallback")
        assertNotNull("lottieDrawerPremiumStar must exist in activity_main.xml", star)
        val bg = star!!.designAttribute("android:background")
        assertTrue(
            "lottieDrawerPremiumStar background must be transparent or empty (was '$bg')",
            bg.isEmpty() || bg == "@android:color/transparent" || bg == "@null"
        )
    }
}

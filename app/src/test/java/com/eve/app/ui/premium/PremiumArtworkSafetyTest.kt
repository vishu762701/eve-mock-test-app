package com.eve.app.ui.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PremiumArtworkSafetyTest {

    private fun findProjectFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")
    }

    private fun loadXml(relativePath: String): Document {
        val file = findProjectFile(relativePath)
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
    fun testDrawerLayoutContainsStarAndFallback() {
        val doc = loadXml("src/main/res/layout/activity_main.xml")
        val starView = findElementById(doc.documentElement, "lottieDrawerPremiumStar")
        val fallbackView = findElementById(doc.documentElement, "lottieDrawerPremiumStarFallback")

        assertEquals("OpenGL overlay is replaced by the retained Lottie icon", null, starView)
        assertNotNull("lottieDrawerPremiumStarFallback must exist in activity_main.xml", fallbackView)

        assertEquals("Fallback must be initially gone", "gone", fallbackView!!.getAttribute("android:visibility"))
    }

    @Test
    fun testPremiumLayoutContainsStarAndFallbackForBothStates() {
        val doc = loadXml("src/main/res/layout/activity_premium.xml")

        val notPremStar = findElementById(doc.documentElement, "starViewNotPremium")
        val notPremFallback = findElementById(doc.documentElement, "ivHeaderStarFallback")
        assertNotNull("starViewNotPremium must exist", notPremStar)
        assertEquals(null, notPremFallback)
        assertEquals("ImageView", notPremStar!!.tagName)
        assertEquals("@drawable/ic_star_outline", notPremStar.getAttribute("android:src"))

        val activePremStar = findElementById(doc.documentElement, "starViewPremiumActive")
        val activePremFallback = findElementById(doc.documentElement, "ivActiveStarFallback")
        assertNotNull("starViewPremiumActive must exist", activePremStar)
        assertEquals(null, activePremFallback)
        assertEquals("ImageView", activePremStar!!.tagName)
        assertEquals("@drawable/ic_star_outline", activePremStar.getAttribute("android:src"))
    }











    @Test
    fun testPremiumStarLottieResourcesExist() {
        val starDark = findProjectFile("src/main/res/raw/premium_star.json")
        val starLight = findProjectFile("src/main/res/raw/premium_star_light.json")
        assertTrue("premium_star.json must exist", starDark.exists() && starDark.length() > 0)
        assertTrue("premium_star_light.json must exist", starLight.exists() && starLight.length() > 0)
    }


}

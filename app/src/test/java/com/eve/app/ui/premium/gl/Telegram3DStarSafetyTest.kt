package com.eve.app.ui.premium.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import javax.xml.parsers.DocumentBuilderFactory

class Telegram3DStarSafetyTest {

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

        assertNotNull("lottieDrawerPremiumStar must exist in activity_main.xml", starView)
        assertNotNull("lottieDrawerPremiumStarFallback must exist in activity_main.xml", fallbackView)

        assertEquals("Fallback must be initially gone", "gone", fallbackView!!.getAttribute("android:visibility"))
    }

    @Test
    fun testPremiumLayoutContainsStarAndFallbackForBothStates() {
        val doc = loadXml("src/main/res/layout/activity_premium.xml")

        val notPremStar = findElementById(doc.documentElement, "starViewNotPremium")
        val notPremFallback = findElementById(doc.documentElement, "ivHeaderStarFallback")
        assertNotNull("starViewNotPremium must exist", notPremStar)
        assertNotNull("ivHeaderStarFallback must exist", notPremFallback)
        assertEquals("Fallback must be initially gone", "gone", notPremFallback!!.getAttribute("android:visibility"))

        val activePremStar = findElementById(doc.documentElement, "starViewPremiumActive")
        val activePremFallback = findElementById(doc.documentElement, "ivActiveStarFallback")
        assertNotNull("starViewPremiumActive must exist", activePremStar)
        assertNotNull("ivActiveStarFallback must exist", activePremFallback)
        assertEquals("Fallback must be initially gone", "gone", activePremFallback!!.getAttribute("android:visibility"))
    }

    @Test
    fun testShaderStrictGlslEs100Compliance() {
        val fragFile = findProjectFile("src/main/assets/shaders/fragment4.glsl")
        val fragContent = fragFile.readText()

        assertFalse(
            "Shader must not contain scalar subtraction '1.0 - texture2D' which violates GLSL ES 1.00",
            fragContent.contains("1.0 - texture2D")
        )
        assertTrue(
            "Shader must use vector subtraction 'vec3(1.0) - texture2D'",
            fragContent.contains("vec3(1.0) - texture2D")
        )

        val vertFile = findProjectFile("src/main/assets/shaders/vertex2.glsl")
        assertTrue("Vertex shader must exist and be non-empty", vertFile.length() > 0)
    }

    @Test
    fun testStarAssetsExistAndAreValid() {
        val binobjFile = findProjectFile("src/main/assets/models/star.binobj")
        assertTrue("star.binobj must exist", binobjFile.exists())
        assertTrue("star.binobj must be larger than 50KB", binobjFile.length() > 50000)

        // Verify star.binobj format integrity
        DataInputStream(FileInputStream(binobjFile)).use { dis ->
            val numVertices = dis.readInt()
            assertTrue("numVertices must be positive", numVertices > 0)
            dis.skipBytes(numVertices * 4)

            val numTextures = dis.readInt()
            assertTrue("numTextures must be positive", numTextures > 0)
            dis.skipBytes(numTextures * 4)

            val numNormals = dis.readInt()
            assertTrue("numNormals must be positive", numNormals > 0)
            dis.skipBytes(numNormals * 4)

            val numFaces = dis.readInt()
            assertTrue("numFaces must be positive", numFaces > 0)
        }

        val flecksFile = findProjectFile("src/main/assets/flecks.png")
        assertTrue("flecks.png must exist", flecksFile.exists())
        assertTrue("flecks.png must have non-zero size", flecksFile.length() > 0)
    }

    @Test
    fun testGLIconRendererConstants() {
        assertEquals("FRAGMENT_STYLE must be 0", 0, GLIconRenderer.FRAGMENT_STYLE)
        assertEquals("TYPE_STAR must be 0", 0, Icon3D.TYPE_STAR)
    }
}

package com.eve.app.ui

import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Audit actual outline attributes without rewriting shared semantic colors. */
class ManualBorderInventoryTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private fun elements(file: File): List<Element> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("*")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    @Test fun allDecorativeXmlShapeStrokesUseTheDedicatedStandard() {
        var count = 0
        res.walkTopDown().filter { it.extension == "xml" && it.parentFile.name.startsWith("drawable") }.forEach { file ->
            // Notification/status dot artwork has a contrast halo, not a UI container outline.
            if (file.name != "bg_dot_red.xml") for (node in elements(file).filter { it.tagName == "stroke" }) {
                assertEquals(file.path, "1dp", node.getAttribute("android:width"))
                assertEquals(file.path, "@color/eve_shape_border", node.getAttribute("android:color"))
                count++
            }
        }
        assertTrue("Inventory must include active shape outlines", count >= 27)
    }

    @Test fun explicitLayoutOutlinesPreserveZeroStrokeExceptions() {
        res.walkTopDown().filter { it.extension == "xml" && it.parentFile.name.startsWith("layout") }.forEach { file ->
            for (node in elements(file)) for (prefix in listOf("stroke", "chipStroke")) {
                val width = node.getAttribute("app:${prefix}Width")
                val color = node.getAttribute("app:${prefix}Color")
                if (width.isNotEmpty() && width != "0dp") assertEquals(file.path, "1dp", width)
                if (color.isNotEmpty() && width != "0dp") assertEquals(file.path, "@color/eve_shape_border", color)
            }
        }
    }

    @Test fun exactColorsAndIntentionalBorderlessSurfaces() {
        val switchOutline = elements(File(res, "drawable/eve_switch_track_outline.xml")).first { it.tagName == "path" }
        assertEquals("1", switchOutline.getAttribute("android:strokeWidth"))
        assertEquals("@color/eve_shape_border", switchOutline.getAttribute("android:strokeColor"))
        val switchStates = elements(File(res, "color/selector_switch_outline.xml")).filter { it.tagName == "item" }
        assertEquals("@android:color/transparent", switchStates.first().getAttribute("android:color"))
        assertEquals("true", switchStates.first().getAttribute("android:state_checked"))
        assertEquals("@color/eve_shape_border", switchStates.last().getAttribute("android:color"))
        for ((qualifier, expected) in listOf("values" to "#FF000000", "values-night" to "#FFFFFFFF")) {
            val color = elements(File(res, "$qualifier/manual_ui_colors.xml")).first { it.getAttribute("name") == "eve_shape_border" }
            assertEquals(expected, color.textContent)
        }
        for (name in listOf("bg_result_segmented_track.xml", "bg_result_segmented_indicator.xml")) {
            assertFalse(elements(File(res, "drawable/$name")).any { it.tagName == "stroke" })
        }
        val source = File(res.parentFile, "java/com/eve/app/ui/common/TelegramRadioButton.kt").readText()
        val approved = source.substringAfter("fun enableApprovedTestStyle()").substringBefore("private fun animateCheckProgress")
        assertFalse(approved.contains("setStroke"))
        assertTrue(approved.contains("cornerRadius = 16f * density"))
        val next = elements(File(res, "values/approved_ui_styles.xml")).first { it.tagName == "style" && it.getAttribute("name") == "Widget.Eve.ApprovedTestNext" }
        assertTrue(next.textContent.contains("0dp"))
    }
}

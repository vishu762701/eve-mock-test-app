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
        res.walkTopDown().filter { it.extension == "xml" && it.parentFile.name.startsWith("drawable") }.forEach { file ->
            // Notification/status dot artwork has a contrast halo, not a UI container outline.
            if (file.name != "bg_dot_red.xml") for (node in elements(file).filter { it.tagName == "stroke" }) {
                val width = node.getAttribute("android:width")
                if (file.name == "design_input_background.xml") {
                    assertEquals("Locked focused input ring", "@dimen/design_focus_stroke", width)
                    assertEquals("@color/accent", node.getAttribute("android:color"))
                    continue
                }
                assertTrue("${file.path}: stroke width must be 1dp or @dimen/eve_home_hairline_border (was $width)",
                    width == "1dp" || width == "@dimen/eve_home_hairline_border" || width == "@dimen/eve_home_hairline_stroke" || width == "@dimen/eve_home_pill_border_width")
                val expected = if (file.name == "bg_edit_text_rounded.xml") "@color/eve_control_outline" else "@color/eve_shape_border"
                val color = node.getAttribute("android:color")
                assertTrue("${file.path}: stroke color must be $expected or @color/eve_home_hairline_border (was $color)",
                    color == expected || color == "@color/eve_home_hairline_border" || color == "@color/eve_home_hairline" || color == "@color/eve_home_accent")
            }
        }
        for (name in listOf("bg_edit_text_rounded.xml", "bg_tile_right.xml", "bg_tile_wrong.xml", "bg_tile_medium.xml", "bg_tile_unattempted.xml")) {
            assertTrue("Functional outline must remain: $name", elements(File(res, "drawable/$name")).any { it.tagName == "stroke" })
        }
        for (name in listOf("bg_category_chip.xml", "bg_circle_badge.xml", "bg_circle_translucent.xml", "bg_sheet_bottom.xml", "bg_stat_mini.xml", "bg_streak_pill.xml", "bg_question_timer_chip.xml")) {
            assertFalse("Approved content surface must remain borderless: $name", elements(File(res, "drawable/$name")).any { it.tagName == "stroke" })
        }
    }

    @Test fun explicitLayoutOutlinesPreserveZeroStrokeExceptions() {
        res.walkTopDown().filter { it.extension == "xml" && it.parentFile.name.startsWith("layout") }.forEach { file ->
            for (node in elements(file)) for (prefix in listOf("stroke", "chipStroke")) {
                val width = node.getAttribute("app:${prefix}Width")
                val color = node.getAttribute("app:${prefix}Color")
                if (width.isNotEmpty() && width != "0dp" && width != "@dimen/eve_space_0") {
                    assertTrue("${file.path}: stroke width must be 1dp or @dimen/eve_home_hairline_border (was $width)",
                        width == "1dp" || width == "@dimen/eve_home_hairline_border" || width == "@dimen/eve_home_hairline_stroke")
                }
                if (color.isNotEmpty() && width != "0dp" && width != "@dimen/eve_space_0") {
                    assertTrue("${file.path}: stroke color must be @color/eve_shape_border or @color/eve_home_hairline_border (was $color)",
                        color == "@color/eve_shape_border" || color == "@color/eve_home_hairline_border" || color == "@color/eve_home_hairline")
                }
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
        assertEquals("@color/eve_control_outline", switchStates.last().getAttribute("android:color"))
        for ((qualifier, expected) in listOf("values" to "#D9DDE5", "values-night" to "#3B3F4A")) {
            val color = elements(File(res, "$qualifier/manual_ui_colors.xml")).first { it.getAttribute("name") == "eve_shape_border" }
            assertEquals(expected, color.textContent)
        }
        for (name in listOf("bg_result_segmented_track.xml", "bg_result_segmented_indicator.xml")) {
            assertFalse(elements(File(res, "drawable/$name")).any { it.tagName == "stroke" })
        }
        val source = File(res.parentFile, "java/com/eve/app/ui/common/TelegramRadioButton.kt").readText()
        val approved = source.substringAfter("fun enableApprovedTestStyle()").substringBefore("private fun animateCheckProgress")
        assertFalse(approved.contains("setStroke"))
        assertTrue(approved.contains("cornerRadius = com.eve.app.util.EveShapes.concentric"))
        val next = elements(File(res, "values/approved_ui_styles.xml")).first { it.tagName == "style" && it.getAttribute("name") == "Widget.Eve.ApprovedTestNext" }
        assertTrue(next.textContent.contains("0dp"))
    }
}

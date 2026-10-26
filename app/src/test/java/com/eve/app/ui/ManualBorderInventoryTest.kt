package com.eve.app.ui

import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Root-owned outlines: focus ring and glass hairline; solid cards have no extra border. */
class ManualBorderInventoryTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
    private fun elements(file: File): List<Element> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("*")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    @Test fun sharedShapeOutlinesUseOnlyTheirSpecifiedRoles() {
        File(res, "drawable").listFiles()!!.filter { it.name.startsWith("design_") && it.extension == "xml" }.forEach { file ->
            for (node in elements(file).filter { it.tagName == "stroke" }) {
                when (file.name) {
                    "design_menu_fallback.xml" -> {
                        assertEquals("@dimen/design_hairline_width", node.getAttribute("android:width"))
                        assertEquals("@color/hairline", node.getAttribute("android:color"))
                    }
                    "design_input_background.xml" -> assertEquals("@dimen/design_focus_stroke", node.getAttribute("android:width"))
                    else -> assertTrue("No extra outline in ${file.name}", node.getAttribute("android:width") in listOf("0dp", "@dimen/design_zero"))
                }
            }
        }
    }

    @Test fun layoutsDoNotAddLocalCardOrChipOutlines() {
        File(res, "layout").listFiles()!!.filter { it.extension == "xml" }.forEach { file ->
            for (node in elements(file)) for (attribute in listOf("app:strokeWidth", "app:chipStrokeWidth")) {
                val value = node.getAttribute(attribute)
                assertTrue("${file.name}/$attribute: $value", value in listOf("", "0dp", "@dimen/design_zero"))
            }
        }
    }

    @Test fun solidPanelsAndAnswerTilesKeepNoDecorativeStroke() {
        val panel = elements(File(res, "drawable/design_surface_panel.xml"))
        assertFalse(panel.any { it.tagName == "stroke" })
        assertEquals("@color/dialog_surface", panel.single { it.tagName == "solid" }.getAttribute("android:color"))
        val source = File(res.parentFile, "java/com/eve/app/ui/common/TelegramRadioButton.kt").readText()
        assertFalse(source.contains("setStroke"))
        assertTrue(source.contains("R.dimen.design_radius_inner"))
        val widgets = elements(File(res, "values/design_widgets.xml"))
        val button = widgets.single { it.tagName == "style" && it.getAttribute("name") == "Widget.Eve.Token.Button" }
        assertTrue(button.textContent.contains("@dimen/design_zero"))
    }
}

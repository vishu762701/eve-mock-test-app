package com.eve.app.ui

import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class SurfaceMaterialTest {

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

    private fun loadColors(relativePath: String): Map<String, String> {
        val doc = loadXml(relativePath)
        val colorNodes = doc.getElementsByTagName("color")
        val map = mutableMapOf<String, String>()
        for (i in 0 until colorNodes.length) {
            val node = colorNodes.item(i) as Element
            val name = node.getAttribute("name")
            val value = node.textContent?.trim()
            if (name.isNotEmpty() && value != null) {
                map[name] = value
            }
        }
        return map
    }

    private fun findItem(element: Element, itemName: String): String? {
        val items = element.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            if (item.getAttribute("name") == itemName) {
                return item.textContent?.trim()
            }
        }
        return null
    }

    private fun findStyle(doc: Document, styleName: String): Element? {
        val styles = doc.getElementsByTagName("style")
        for (i in 0 until styles.length) {
            val style = styles.item(i) as Element
            if (style.getAttribute("name") == styleName) {
                return style
            }
        }
        return null
    }





    @Test fun glassTokensUseMediumTintAndAvailabilityFallback() {
        val light = loadColors("src/main/res/values/design_colors.xml")
        val dark = loadColors("src/main/res/values-night/design_colors.xml")
        assertEquals("#C2FFFFFF", light["glass_tint"])
        assertEquals("#C2262627", dark["glass_tint"])
        assertEquals("#C2FFFFFF", light["glass_fallback"])
        assertEquals("#C2262627", dark["glass_fallback"])
    }
    @Test fun panelsHaveSolidSurfaceAndSharedRadiusWithoutOutline() {
        val panel = loadXml("src/main/res/drawable/design_surface_panel.xml")
        assertEquals("shape", panel.documentElement.tagName)
        assertEquals("@color/surface", (panel.getElementsByTagName("solid").item(0) as Element).getAttribute("android:color"))
        assertEquals("@dimen/design_radius_card", (panel.getElementsByTagName("corners").item(0) as Element).getAttribute("android:radius"))
        assertEquals(0, panel.getElementsByTagName("stroke").length)
    }

    @Test
    fun testDialogAndBottomSheetThemes_disableWindowBackgroundBlurOnApi31() {
        val doc = loadXml("src/main/res/values-v31/materials.xml")

        // Theme.Eve.Dialog
        val dialogStyle = findStyle(doc, "Theme.Eve.Dialog")
        assertNotNull("Theme.Eve.Dialog must exist in values-v31/materials.xml", dialogStyle)
        assertEquals("false", findItem(dialogStyle!!, "android:windowIsTranslucent"))
        assertEquals("@dimen/design_zero", findItem(dialogStyle, "android:windowBackgroundBlurRadius"))
        assertEquals("false", findItem(dialogStyle, "android:windowBlurBehindEnabled"))

        // Theme.Eve.BottomSheetDialog
        val sheetStyle = findStyle(doc, "Theme.Eve.BottomSheetDialog")
        assertNotNull("Theme.Eve.BottomSheetDialog must exist in values-v31/materials.xml", sheetStyle)
        assertEquals("false", findItem(sheetStyle!!, "android:windowIsTranslucent"))
        assertEquals("@dimen/design_zero", findItem(sheetStyle, "android:windowBackgroundBlurRadius"))
        assertEquals("@style/Widget.Eve.BottomSheet.Modal", findItem(sheetStyle, "bottomSheetStyle"))
    }





    @Test
    fun testContentCardsAndTestViewport_remainOpaqueWithoutBlur() {
        val testContent = File("src/main/res/layout/activity_test.xml").readText()
        val questionContent = File("src/main/res/layout/item_question.xml").readText()
        val examCard = File("src/main/res/layout/item_exam.xml").readText()

        assertFalse("activity_test.xml must not contain BlurView", testContent.contains("BlurView"))
        assertFalse("item_question.xml must not contain BlurView", questionContent.contains("BlurView"))
        assertFalse("item_exam.xml must not contain BlurView", examCard.contains("BlurView"))
    }

    @Test
    fun testSubmitConfirmationDialog_themeAndGeometry() {
        // Theme.Eve.Dialog.SubmitConfirmation in values/themes.xml
        val themesDoc = loadXml("src/main/res/values/themes.xml")
        val themeStyle = findStyle(themesDoc, "Theme.Eve.Dialog.SubmitConfirmation")
        assertNotNull("Theme.Eve.Dialog.SubmitConfirmation must exist in values/themes.xml", themeStyle)
        assertEquals("@drawable/design_dialog_surface_panel", findItem(themeStyle!!, "android:windowBackground"))
        assertEquals("true", findItem(themeStyle, "android:windowIsFloating"))

        // Theme.Eve.Dialog.SubmitConfirmation in values-v31/materials.xml
        val v31Doc = loadXml("src/main/res/values-v31/materials.xml")
        val v31Style = findStyle(v31Doc, "Theme.Eve.Dialog.SubmitConfirmation")
        assertNotNull("Theme.Eve.Dialog.SubmitConfirmation must exist in values-v31/materials.xml", v31Style)
        assertEquals("@drawable/design_dialog_surface_panel", findItem(v31Style!!, "android:windowBackground"))
        assertEquals("true", findItem(v31Style, "android:windowIsFloating"))
        assertEquals("false", findItem(v31Style, "android:windowIsTranslucent"))
        assertEquals("@dimen/design_zero", findItem(v31Style, "android:windowBackgroundBlurRadius"))
        assertEquals("false", findItem(v31Style, "android:windowBlurBehindEnabled"))
    }

    @Test
    fun testSubmitConfirmationDialog_sourceCodeAndInvariants() {
        val testActivitySource = File("src/main/java/com/eve/app/ui/test/TestActivity.kt").readText()

        assertTrue(
            "confirmSubmit() must use Theme_Eve_Dialog_SubmitConfirmation",
            testActivitySource.contains("R.style.Theme_Eve_Dialog_SubmitConfirmation")
        )
        assertTrue(
            "confirmSubmit() must set title 'Submit test?'",
            testActivitySource.contains(".setTitle(\"Submit test?\")")
        )
        assertTrue(
            "confirmSubmit() must have positive button 'Submit'",
            testActivitySource.contains(".setPositiveButton(\"Submit\")")
        )
        assertTrue(
            "confirmSubmit() must have negative button 'Cancel'",
            testActivitySource.contains(".setNegativeButton(\"Cancel\", null)")
        )
        assertTrue(
            "confirmSubmit() must apply the solid dialog surface",
            testActivitySource.contains("DesignSurfaces.dialog(dialog)")
        )
        assertTrue(
            "calculateSubmitDialogCounts must be preserved",
            testActivitySource.contains("QuestionPaletteAdapter.calculateSubmitDialogCounts")
        )
    }
}

package com.eve.app.ui

import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LiquidGlassRegularMaterialTest {

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

    @Test
    fun testUnifiedLiquidGlassTokens_resolveCorrectlyInLightAndDark() {
        val lightColors = loadColors("src/main/res/values/colors.xml")
        val darkColors = loadColors("src/main/res/values-night/colors.xml")

        // Official Liquid Glass .regular tokens: 90% white (#E6FFFFFF) / 90% #1C1C1E (#E61C1C1E)
        assertEquals("#E6FFFFFF", lightColors["eve_material_regular"])
        assertEquals("#E61C1C1E", darkColors["eve_material_regular"])

        // Pre-31 fallback: opaque secondary system background
        assertEquals("@color/eve_secondary_system_background", lightColors["eve_modal_material"])
        assertEquals("@color/eve_secondary_system_background", darkColors["eve_modal_material"])

        // API 31+ override: resolves to eve_material_regular
        val lightV31 = loadColors("src/main/res/values-v31/materials.xml")
        val darkV31 = loadColors("src/main/res/values-night-v31/materials.xml")
        assertEquals("@color/eve_material_regular", lightV31["eve_modal_material"])
        assertEquals("@color/eve_material_regular", darkV31["eve_modal_material"])
    }

    @Test
    fun testDialogLiquidGlassGeometry_andDrawables() {
        val doc = loadXml("src/main/res/drawable/bg_dialog_liquid_glass.xml")
        val inset = doc.documentElement
        assertEquals("inset", inset.tagName)
        assertEquals("16dp", inset.getAttribute("android:insetLeft"))
        assertEquals("16dp", inset.getAttribute("android:insetRight"))
        assertEquals("16dp", inset.getAttribute("android:insetTop"))
        assertEquals("16dp", inset.getAttribute("android:insetBottom"))

        val solid = doc.getElementsByTagName("solid").item(0) as Element
        assertEquals("@color/eve_modal_material", solid.getAttribute("android:color"))

        val stroke = doc.getElementsByTagName("stroke").item(0) as Element
        assertEquals("1dp", stroke.getAttribute("android:width"))
        assertEquals("@color/eve_shape_border", stroke.getAttribute("android:color"))

        val corners = doc.getElementsByTagName("corners").item(0) as Element
        assertEquals("@dimen/eve_radius_bottom_sheet", corners.getAttribute("android:radius"))
    }

    @Test
    fun testDialogAndBottomSheetThemes_enableWindowBackgroundBlurOnApi31() {
        val doc = loadXml("src/main/res/values-v31/materials.xml")

        // Theme.Eve.Dialog
        val dialogStyle = findStyle(doc, "Theme.Eve.Dialog")
        assertNotNull("Theme.Eve.Dialog must exist in values-v31/materials.xml", dialogStyle)
        assertEquals("true", findItem(dialogStyle!!, "android:windowIsTranslucent"))
        assertEquals("@dimen/eve_modal_blur_radius", findItem(dialogStyle, "android:windowBackgroundBlurRadius"))
        assertEquals("false", findItem(dialogStyle, "android:windowBlurBehindEnabled"))

        // Theme.Eve.BottomSheetDialog
        val sheetStyle = findStyle(doc, "Theme.Eve.BottomSheetDialog")
        assertNotNull("Theme.Eve.BottomSheetDialog must exist in values-v31/materials.xml", sheetStyle)
        assertEquals("true", findItem(sheetStyle!!, "android:windowIsTranslucent"))
        assertEquals("@dimen/eve_modal_blur_radius", findItem(sheetStyle, "android:windowBackgroundBlurRadius"))
        assertEquals("@style/Widget.Eve.BottomSheet.Modal", findItem(sheetStyle, "bottomSheetStyle"))
    }

    @Test
    fun testEveLiquidGlassHelper_specificationsAndConstants() {
        assertEquals("apple-regular", com.eve.app.util.EveLiquidGlass.MATERIAL_ID)
        assertEquals("Liquid Glass .regular", com.eve.app.util.EveLiquidGlass.VARIANT)
    }

    @Test
    fun testLiquidGlassContrastRatios_exceedWcagGate() {
        // WCAG relative luminance calculation
        fun sRgbToLinear(c: Double): Double {
            return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        fun luminance(r: Int, g: Int, b: Int): Double {
            val lr = sRgbToLinear(r / 255.0)
            val lg = sRgbToLinear(g / 255.0)
            val lb = sRgbToLinear(b / 255.0)
            return 0.2126 * lr + 0.7152 * lg + 0.0722 * lb
        }
        fun contrast(l1: Double, l2: Double): Double {
            val lighter = maxOf(l1, l2)
            val darker = minOf(l1, l2)
            return (lighter + 0.05) / (darker + 0.05)
        }

        // Light mode: #E6FFFFFF (90% alpha white) composited over black backdrop (#000000)
        // Composite color: 0.9 * 255 + 0.1 * 0 = 229.5 -> ~230
        val lightCompositeLum = luminance(230, 230, 230)
        val blackTextLum = luminance(0, 0, 0)
        val lightRatio = contrast(lightCompositeLum, blackTextLum)
        assertTrue("Light regular glass contrast must exceed 4.5:1 against black text (was $lightRatio)", lightRatio >= 4.5)
        assertTrue("Light regular glass contrast exceeds 10:1", lightRatio >= 10.0)

        // Dark mode: #E61C1C1E (90% alpha #1C1C1E) composited over white backdrop (#FFFFFF)
        // #1C = 28. Composite color: 0.9 * 28 + 0.1 * 255 = 25.2 + 25.5 = 50.7 -> ~51
        val darkCompositeLum = luminance(51, 51, 51)
        val whiteTextLum = luminance(255, 255, 255)
        val darkRatio = contrast(whiteTextLum, darkCompositeLum)
        assertTrue("Dark regular glass contrast must exceed 4.5:1 against white text (was $darkRatio)", darkRatio >= 4.5)
        assertTrue("Dark regular glass contrast exceeds 10:1", darkRatio >= 10.0)
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
        // bg_dialog_submit_confirmation.xml
        val doc = loadXml("src/main/res/drawable/bg_dialog_submit_confirmation.xml")
        val inset = doc.documentElement
        assertEquals("inset", inset.tagName)
        assertEquals("16dp", inset.getAttribute("android:insetLeft"))
        assertEquals("16dp", inset.getAttribute("android:insetRight"))
        assertEquals("16dp", inset.getAttribute("android:insetTop"))
        assertEquals("16dp", inset.getAttribute("android:insetBottom"))

        val solid = doc.getElementsByTagName("solid").item(0) as Element
        assertEquals("@color/eve_modal_material", solid.getAttribute("android:color"))

        val stroke = doc.getElementsByTagName("stroke").item(0) as Element
        assertEquals("1dp", stroke.getAttribute("android:width"))
        assertEquals("@color/eve_shape_border", stroke.getAttribute("android:color"))

        val corners = doc.getElementsByTagName("corners").item(0) as Element
        assertEquals("@dimen/eve_radius_bottom_sheet", corners.getAttribute("android:radius"))

        // Theme.Eve.Dialog.SubmitConfirmation in values/themes.xml
        val themesDoc = loadXml("src/main/res/values/themes.xml")
        val themeStyle = findStyle(themesDoc, "Theme.Eve.Dialog.SubmitConfirmation")
        assertNotNull("Theme.Eve.Dialog.SubmitConfirmation must exist in values/themes.xml", themeStyle)
        assertEquals("@drawable/bg_dialog_submit_confirmation", findItem(themeStyle!!, "android:windowBackground"))
        assertEquals("true", findItem(themeStyle, "android:windowIsFloating"))

        // Theme.Eve.Dialog.SubmitConfirmation in values-v31/materials.xml
        val v31Doc = loadXml("src/main/res/values-v31/materials.xml")
        val v31Style = findStyle(v31Doc, "Theme.Eve.Dialog.SubmitConfirmation")
        assertNotNull("Theme.Eve.Dialog.SubmitConfirmation must exist in values-v31/materials.xml", v31Style)
        assertEquals("@drawable/bg_dialog_submit_confirmation", findItem(v31Style!!, "android:windowBackground"))
        assertEquals("true", findItem(v31Style, "android:windowIsFloating"))
        assertEquals("true", findItem(v31Style, "android:windowIsTranslucent"))
        assertEquals("@dimen/eve_modal_blur_radius", findItem(v31Style, "android:windowBackgroundBlurRadius"))
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
            "confirmSubmit() must apply EveLiquidGlass.applySubmitConfirmationGlass",
            testActivitySource.contains("applySubmitConfirmationGlass(dialog)")
        )
        assertTrue(
            "calculateSubmitDialogCounts must be preserved",
            testActivitySource.contains("QuestionPaletteAdapter.calculateSubmitDialogCounts")
        )
    }
}

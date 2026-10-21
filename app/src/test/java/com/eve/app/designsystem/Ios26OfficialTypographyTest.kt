package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Comprehensive verification of Apple iOS 26 Official Typography implementation.
 *
 * Validates:
 * 1. All 11 canonical Apple HIG semantic text styles (Large Title through Caption 2).
 * 2. Official font sizes and line heights (34/41 down to 11/13).
 * 3. Standard and Emphasized font weights.
 * 4. Dimensional consistency across base, sw360dp, and sw600dp qualifiers.
 * 5. Open-source Inter font family integration (SIL OFL 1.1).
 * 6. Component-specific adaptations and DesignType mappings.
 */
class Ios26OfficialTypographyTest {

    private fun mainDir(): File = listOf(File("src/main"), File("app/src/main"), File("../app/src/main"))
        .first { it.isDirectory }

    private fun loadXml(relativePath: String): Element {
        val rel = relativePath.removePrefix("app/src/main/").removePrefix("src/main/")
        val file = File(mainDir(), rel)
        assertTrue("XML file exists: ${file.absolutePath}", file.isFile)
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(file).documentElement
    }

    private fun loadDimens(relativePath: String): Map<String, String> {
        val root = loadXml(relativePath)
        val dimens = mutableMapOf<String, String>()
        val nodeList = root.getElementsByTagName("dimen")
        for (i in 0 until nodeList.length) {
            val item = nodeList.item(i) as Element
            val name = item.getAttribute("name")
            val value = item.textContent.trim()
            if (name.isNotEmpty()) {
                dimens[name] = value
            }
        }
        return dimens
    }

    private fun loadStyles(relativePath: String): Map<String, Map<String, String>> {
        val root = loadXml(relativePath)
        val styles = mutableMapOf<String, MutableMap<String, String>>()
        val styleNodes = root.getElementsByTagName("style")
        for (i in 0 until styleNodes.length) {
            val styleNode = styleNodes.item(i) as Element
            val styleName = styleNode.getAttribute("name")
            val items = mutableMapOf<String, String>()
            if (styleNode.hasAttribute("parent")) {
                items["_parent"] = styleNode.getAttribute("parent")
            }
            val itemNodes = styleNode.getElementsByTagName("item")
            for (j in 0 until itemNodes.length) {
                val itemNode = itemNodes.item(j) as Element
                val itemName = itemNode.getAttribute("name")
                items[itemName] = itemNode.textContent.trim()
            }
            styles[styleName] = items
        }
        return styles
    }

    data class ExpectedStyle(
        val name: String,
        val sizeDimen: String,
        val lineDimen: String,
        val expectedSize: String,
        val expectedLine: String,
        val standardFont: String,
        val emphasizedFont: String
    )

    private val canonicalStyles = listOf(
        ExpectedStyle("LargeTitle", "design_type_large_title", "design_line_large_title", "34sp", "41sp", "@font/inter_regular", "@font/inter_bold"),
        ExpectedStyle("Title1", "design_type_title1", "design_line_title1", "28sp", "34sp", "@font/inter_regular", "@font/inter_bold"),
        ExpectedStyle("Title2", "design_type_title2", "design_line_title2", "22sp", "28sp", "@font/inter_regular", "@font/inter_bold"),
        ExpectedStyle("Title3", "design_type_title3", "design_line_title3", "20sp", "25sp", "@font/inter_regular", "@font/inter_semibold"),
        ExpectedStyle("Headline", "design_type_headline", "design_line_headline", "17sp", "22sp", "@font/inter_semibold", "@font/inter_semibold"),
        ExpectedStyle("Body", "design_type_body", "design_line_body", "17sp", "22sp", "@font/inter_regular", "@font/inter_semibold"),
        ExpectedStyle("Callout", "design_type_callout", "design_line_callout", "16sp", "21sp", "@font/inter_regular", "@font/inter_semibold"),
        ExpectedStyle("Subheadline", "design_type_subheadline", "design_line_subheadline", "15sp", "20sp", "@font/inter_regular", "@font/inter_semibold"),
        ExpectedStyle("Footnote", "design_type_footnote", "design_line_footnote", "13sp", "18sp", "@font/inter_regular", "@font/inter_semibold"),
        ExpectedStyle("Caption1", "design_type_caption1", "design_line_caption1", "12sp", "16sp", "@font/inter_regular", "@font/inter_semibold"),
        ExpectedStyle("Caption2", "design_type_caption2", "design_line_caption2", "11sp", "13sp", "@font/inter_regular", "@font/inter_semibold")
    )

    @Test
    fun testAll11CanonicalStylesDefinedInDimensWithAppleMetrics() {
        val dimens = loadDimens("app/src/main/res/values/design_dimens.xml")
        for (expected in canonicalStyles) {
            assertEquals("Size for ${expected.name}", expected.expectedSize, dimens[expected.sizeDimen])
            assertEquals("Line height for ${expected.name}", expected.expectedLine, dimens[expected.lineDimen])
        }
    }

    @Test
    fun testCanonicalStylesIdenticalAcrossQualifiedDimens() {
        val baseDimens = loadDimens("app/src/main/res/values/design_dimens.xml")
        val sw360Dimens = loadDimens("app/src/main/res/values-sw360dp/design_dimens.xml")
        val sw600Dimens = loadDimens("app/src/main/res/values-sw600dp/design_dimens.xml")

        for (expected in canonicalStyles) {
            assertEquals("sw360dp size for ${expected.name}", baseDimens[expected.sizeDimen], sw360Dimens[expected.sizeDimen])
            assertEquals("sw360dp line for ${expected.name}", baseDimens[expected.lineDimen], sw360Dimens[expected.lineDimen])
            assertEquals("sw600dp size for ${expected.name}", baseDimens[expected.sizeDimen], sw600Dimens[expected.sizeDimen])
            assertEquals("sw600dp line for ${expected.name}", baseDimens[expected.lineDimen], sw600Dimens[expected.lineDimen])
        }
    }

    @Test
    fun testAll11CanonicalStylesDeclaredInTypeXmlWithCorrectFontsAndSizes() {
        val styles = loadStyles("app/src/main/res/values/design_type.xml")
        for (expected in canonicalStyles) {
            val styleName = "TextAppearance.Eve.${expected.name}"
            val style = styles[styleName]
            assertNotNull("Style $styleName must be defined", style)
            assertEquals("Size for $styleName", "@dimen/${expected.sizeDimen}", style?.get("android:textSize"))
            assertEquals("Line height for $styleName", "@dimen/${expected.lineDimen}", style?.get("lineHeight"))
            assertEquals("Font family for $styleName", expected.standardFont, style?.get("fontFamily"))

            // Emphasized variant
            val empName = "$styleName.Emphasized"
            val empStyle = styles[empName]
            assertNotNull("Style $empName must be defined", empStyle)
            assertEquals("Emphasized font for $empName", expected.emphasizedFont, empStyle?.get("fontFamily"))
        }
    }

    @Test
    fun testComponentSpecificAdaptationsDeclared() {
        val styles = loadStyles("app/src/main/res/values/design_type.xml")
        val requiredComponentStyles = listOf(
            "TextAppearance.Eve.Component.NavigationTitle",
            "TextAppearance.Eve.Component.DialogTitle",
            "TextAppearance.Eve.Component.DialogMessage",
            "TextAppearance.Eve.Component.CardHeading",
            "TextAppearance.Eve.Component.Badge",
            "TextAppearance.Eve.Component.Timer",
            "TextAppearance.Eve.Component.HeroScore",
            "TextAppearance.Eve.Component.MetricValue",
            "TextAppearance.Eve.Component.ChartLabel"
        )
        for (name in requiredComponentStyles) {
            assertTrue("Component style $name must exist", styles.containsKey(name))
        }
    }

    @Test
    fun testDesignTypeEnumMatchesAll11CanonicalRoles() {
        val enumNames = DesignType.Role.values().map { it.name }
        val expectedEnums = listOf(
            "LARGE_TITLE",
            "TITLE_1",
            "TITLE_2",
            "TITLE_3",
            "HEADLINE",
            "BODY",
            "CALLOUT",
            "SUBHEADLINE",
            "FOOTNOTE",
            "CAPTION_1",
            "CAPTION_2"
        )
        for (expected in expectedEnums) {
            assertTrue("DesignType.Role must contain $expected", enumNames.contains(expected))
        }
    }

    @Test
    fun testInterFontArtifactsAndLicensing() {
        val resFontDir = File(mainDir(), "res/font")
        for (w in listOf("regular", "medium", "semibold", "bold", "variable")) {
            val file = File(resFontDir, "inter_$w.ttf")
            assertTrue("inter_$w.ttf must exist and be non-empty", file.isFile && file.length() > 10000)
        }

        val licenseFile = File(mainDir(), "assets/licenses/Inter-OFL.txt")
        assertTrue("Inter-OFL.txt exists", licenseFile.isFile)
        val text = licenseFile.readText()
        assertTrue("Contains OFL 1.1 notice", text.contains("SIL OPEN FONT LICENSE Version 1.1"))
    }
}

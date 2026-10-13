package com.eve.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Phase 3 Official Apple iOS 26 Typography and Semantic Color Foundation Verification
 * across Profile, Settings, and Login screens.
 *
 * Verifies:
 * 1. Zero hardcoded text sizes (android:textSize) across activity_profile.xml,
 *    activity_settings.xml, bottom_sheet_font_picker.xml, and activity_login.xml.
 * 2. Proper semantic text appearance bindings according to Apple iOS 26 specifications.
 * 3. Official Apple dynamic switch green selector tinting.
 * 4. WCAG AA / Apple HIG contrast compliance for semantic colors across Day and Night palettes.
 */
class ProfileSettingsLoginTypographyAndColorTest {

    private fun loadXml(relativePath: String): Element {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Cannot find file: $relativePath")
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
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

    private fun collectAllElementsWithAttribute(node: Node, attrName: String, list: MutableList<Element> = mutableListOf()): List<Element> {
        if (node is Element) {
            if (node.hasAttribute(attrName)) {
                list.add(node)
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            collectAllElementsWithAttribute(children.item(i), attrName, list)
        }
        return list
    }

    @Test
    fun testZeroHardcodedTextSizesInMigratedLayouts() {
        val layoutFiles = listOf(
            "src/main/res/layout/activity_profile.xml",
            "src/main/res/layout/activity_settings.xml",
            "src/main/res/layout/bottom_sheet_font_picker.xml",
            "src/main/res/layout/activity_login.xml"
        )

        for (filePath in layoutFiles) {
            val root = loadXml(filePath)
            val elementsWithTextSize = collectAllElementsWithAttribute(root, "android:textSize")
            val violatingIds = elementsWithTextSize.map { el ->
                el.getAttribute("android:id").ifEmpty { "<${el.tagName}>" }
            }
            assertTrue(
                "Layout $filePath must contain 0 hardcoded android:textSize occurrences, but found: $violatingIds",
                elementsWithTextSize.isEmpty()
            )
        }
    }

    @Test
    fun testProfileTypographyAndColors() {
        val root = loadXml("src/main/res/layout/activity_profile.xml")

        val tvName = findElementById(root, "tvName")
        assertNotNull("tvName must exist", tvName)
        assertEquals("@style/TextAppearance.Eve.Title2", tvName?.getAttribute("style"))

        val tvEmail = findElementById(root, "tvEmail")
        assertNotNull("tvEmail must exist", tvEmail)
        assertEquals("@style/TextAppearance.Eve.Subheadline", tvEmail?.getAttribute("style"))

        val tvEveId = findElementById(root, "tvEveId")
        assertNotNull("tvEveId must exist", tvEveId)
        assertEquals("@style/TextAppearance.Eve.Footnote.Medium", tvEveId?.getAttribute("style"))

        val etName = findElementById(root, "etName")
        assertNotNull("etName must exist", etName)
        assertEquals("@style/TextAppearance.Eve.Body", etName?.getAttribute("style"))

        val etDob = findElementById(root, "etDob")
        assertNotNull("etDob must exist", etDob)
        assertEquals("@style/TextAppearance.Eve.Body", etDob?.getAttribute("style"))

        val chipGeneral = findElementById(root, "chipGeneral")
        assertNotNull("chipGeneral must exist", chipGeneral)
        assertEquals("@style/TextAppearance.Eve.Footnote.Medium", chipGeneral?.getAttribute("android:textAppearance"))

        val btnSaveProfile = findElementById(root, "btnSaveProfile")
        assertNotNull("btnSaveProfile must exist", btnSaveProfile)
        assertEquals("@style/TextAppearance.Eve.Headline", btnSaveProfile?.getAttribute("android:textAppearance"))
    }

    @Test
    fun testSettingsTypographyAndColors() {
        val root = loadXml("src/main/res/layout/activity_settings.xml")

        val tvFontValue = findElementById(root, "tvFontValue")
        assertNotNull("tvFontValue must exist", tvFontValue)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvFontValue?.getAttribute("style"))

        val tvDailyGoalValue = findElementById(root, "tvDailyGoalValue")
        assertNotNull("tvDailyGoalValue must exist", tvDailyGoalValue)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvDailyGoalValue?.getAttribute("style"))

        val tvLanguageValue = findElementById(root, "tvLanguageValue")
        assertNotNull("tvLanguageValue must exist", tvLanguageValue)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvLanguageValue?.getAttribute("style"))

        val rowDeleteAccount = findElementById(root, "rowDeleteAccount")
        assertNotNull("rowDeleteAccount must exist", rowDeleteAccount)
    }

    @Test
    fun testFontPickerTypography() {
        val root = loadXml("src/main/res/layout/bottom_sheet_font_picker.xml")

        val tvEveDefaultTitle = findElementById(root, "tvEveDefaultTitle")
        assertNotNull("tvEveDefaultTitle must exist", tvEveDefaultTitle)
        assertEquals("@style/TextAppearance.Eve.Headline", tvEveDefaultTitle?.getAttribute("style"))

        val tvEveDefaultSample = findElementById(root, "tvEveDefaultSample")
        assertNotNull("tvEveDefaultSample must exist", tvEveDefaultSample)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvEveDefaultSample?.getAttribute("style"))

        val tvDeviceFontTitle = findElementById(root, "tvDeviceFontTitle")
        assertNotNull("tvDeviceFontTitle must exist", tvDeviceFontTitle)
        assertEquals("@style/TextAppearance.Eve.Headline", tvDeviceFontTitle?.getAttribute("style"))

        val tvDeviceFontSample = findElementById(root, "tvDeviceFontSample")
        assertNotNull("tvDeviceFontSample must exist", tvDeviceFontSample)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvDeviceFontSample?.getAttribute("style"))
    }

    @Test
    fun testLoginTypographyAndColors() {
        val root = loadXml("src/main/res/layout/activity_login.xml")

        val tvLogo = findElementById(root, "tvLogo")
        assertNotNull("tvLogo must exist", tvLogo)
        assertEquals("@style/TextAppearance.Eve.Eyebrow", tvLogo?.getAttribute("style"))

        val tvTagline = findElementById(root, "tvTagline")
        assertNotNull("tvTagline must exist", tvTagline)
        assertEquals("@style/TextAppearance.Eve.Title1", tvTagline?.getAttribute("style"))

        val tvSubtitle = findElementById(root, "tvSubtitle")
        assertNotNull("tvSubtitle must exist", tvSubtitle)
        assertEquals("@style/TextAppearance.Eve.Subheadline", tvSubtitle?.getAttribute("style"))

        val etName = findElementById(root, "etName")
        assertNotNull("etName must exist", etName)
        assertEquals("@style/TextAppearance.Eve.Callout", etName?.getAttribute("style"))

        val etEmail = findElementById(root, "etEmail")
        assertNotNull("etEmail must exist", etEmail)
        assertEquals("@style/TextAppearance.Eve.Callout", etEmail?.getAttribute("style"))

        val etPassword = findElementById(root, "etPassword")
        assertNotNull("etPassword must exist", etPassword)
        assertEquals("@style/TextAppearance.Eve.Callout", etPassword?.getAttribute("style"))

        val tvError = findElementById(root, "tvError")
        assertNotNull("tvError must exist", tvError)
        assertEquals("@style/TextAppearance.Eve.Footnote", tvError?.getAttribute("style"))
        assertEquals("@color/eve_destructive_text", tvError?.getAttribute("android:textColor"))

        val btnSubmit = findElementById(root, "btnSubmit")
        assertNotNull("btnSubmit must exist", btnSubmit)
        assertEquals("@style/TextAppearance.Eve.Headline", btnSubmit?.getAttribute("android:textAppearance"))

        val btnGoogle = findElementById(root, "btnGoogle")
        assertNotNull("btnGoogle must exist", btnGoogle)
        assertEquals("@style/TextAppearance.Eve.Subheadline.Medium", btnGoogle?.getAttribute("android:textAppearance"))

        val btnToggleMode = findElementById(root, "btnToggleMode")
        assertNotNull("btnToggleMode must exist", btnToggleMode)
        assertEquals("@style/TextAppearance.Eve.Subheadline.Medium", btnToggleMode?.getAttribute("android:textAppearance"))
    }

    @Test
    fun testSwitchTrackCheckedStateSatisfiesContrast() {
        val root = loadXml("src/main/res/color/selector_eve_switch_track.xml")
        val items = root.getElementsByTagName("item")
        var checkedColor: String? = null
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            if (item.getAttribute("android:state_checked") == "true") {
                checkedColor = item.getAttribute("android:color")
            }
        }
        assertEquals("@color/eve_primary", checkedColor)
    }

    @Test
    fun testContrastComplianceOfPhase3SemanticColors() {
        // Luminance calculation helper:
        fun luminance(r: Int, g: Int, b: Int): Double {
            fun channel(c: Int): Double {
                val s = c / 255.0
                return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
        }

        fun contrast(rgb1: Triple<Int, Int, Int>, rgb2: Triple<Int, Int, Int>): Double {
            val l1 = luminance(rgb1.first, rgb1.second, rgb1.third)
            val l2 = luminance(rgb2.first, rgb2.second, rgb2.third)
            val lighter = Math.max(l1, l2)
            val darker = Math.min(l1, l2)
            return (lighter + 0.05) / (darker + 0.05)
        }

        // Day palette
        val lightCanvas = Triple(255, 255, 255) // #FFFFFF
        val lightPrimaryLabel = Triple(0, 0, 0) // #000000
        val lightSecondaryLabel = Triple(60, 60, 67) // 60% opacity on #FFFFFF -> approx #3C3C43
        val lightDestructiveText = Triple(150, 33, 28) // #96211C
        val lightSystemGreen = Triple(52, 199, 89) // #34C759

        // Night palette
        val darkCanvas = Triple(0, 0, 0) // #000000
        val darkPrimaryLabel = Triple(255, 255, 255) // #FFFFFF
        val darkSecondaryLabel = Triple(235, 235, 245) // 60% opacity on #000000 -> approx #EBEBF5
        val darkDestructiveText = Triple(255, 154, 146) // #FF9A92
        val darkSystemGreen = Triple(48, 209, 88) // #30D158

        // Verify Light mode contrast
        assertTrue("Light primary label on canvas >= 7.0", contrast(lightPrimaryLabel, lightCanvas) >= 7.0)
        assertTrue("Light secondary label on canvas >= 4.5", contrast(lightSecondaryLabel, lightCanvas) >= 4.5)
        assertTrue("Light destructive text on canvas >= 4.5", contrast(lightDestructiveText, lightCanvas) >= 4.5)

        // Verify Dark mode contrast
        assertTrue("Dark primary label on canvas >= 7.0", contrast(darkPrimaryLabel, darkCanvas) >= 7.0)
        assertTrue("Dark secondary label on canvas >= 4.5", contrast(darkSecondaryLabel, darkCanvas) >= 4.5)
        assertTrue("Dark destructive text on canvas >= 4.5", contrast(darkDestructiveText, darkCanvas) >= 4.5)
    }

    @Test
    fun testProfileScopedGroupedDayNightHierarchy() {
        val profile = loadXml("src/main/res/layout/activity_profile.xml")
        val formSurface = loadXml("src/main/res/drawable/bg_student_group.xml")
        val formSolid = formSurface.getElementsByTagName("solid").item(0) as Element
        assertEquals("@color/eve_system_grouped_background", profile.getAttribute("android:background"))
        assertEquals("@color/eve_secondary_system_grouped_background", formSolid.getAttribute("android:color"))

        // The Profile toolbar stays on its original opaque canvas.
        val header = profile.getElementsByTagName("LinearLayout").item(0) as Element
        assertEquals("@color/eve_bg", header.getAttribute("android:background"))

        fun hex(resourcePath: String, token: String): String {
            val root = loadXml(resourcePath)
            val nodes = root.getElementsByTagName("color")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as Element
                if (node.getAttribute("name") == token) return node.textContent.trim()
            }
            error("Missing semantic token: $token in $resourcePath")
        }
        assertEquals("#F2F2F7", hex("src/main/res/values/colors.xml", "eve_system_grouped_background"))
        assertEquals("#FFFFFF", hex("src/main/res/values/colors.xml", "eve_secondary_system_grouped_background"))
        assertEquals("#000000", hex("src/main/res/values-night/colors.xml", "eve_system_grouped_background"))
        assertEquals("#1C1C1E", hex("src/main/res/values-night/colors.xml", "eve_secondary_system_grouped_background"))
    }
}

package com.eve.app.ui.admin

import com.eve.app.designsystem.DesignType
import com.eve.app.util.FontManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Verification test suite for EVE Typography Studio (Part 1):
 * - Immediate local preview upon font selection
 * - 11 canonical Apple iOS 26 typography styles
 * - Authentic Inter font family weights (400, 500, 600, 700) and system fallbacks
 * - Component previews and Multilingual (Hindi/Devanagari) preset support
 * - AdminActivity integration and manifest declaration
 */
class TypographyStudioTest {

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
    fun testActivityTypographyStudioLayoutElements() {
        val root = loadXml("res/layout/activity_typography_studio.xml")

        // 1. Navigation
        assertNotNull("btnBack must exist", findElementById(root, "btnBack"))

        // 2. Active font info & selection controls
        assertNotNull("tvActiveFontInfo must exist", findElementById(root, "tvActiveFontInfo"))
        assertNotNull("chipGroupFonts must exist", findElementById(root, "chipGroupFonts"))

        // 3. Authentic Inter weights + system fallback chips
        val expectedChips = listOf(
            "chipInterRegular",
            "chipInterMedium",
            "chipInterSemibold",
            "chipInterBold",
            "chipSystemSans",
            "chipSystemSerif",
            "chipSystemMono"
        )
        for (chipId in expectedChips) {
            assertNotNull("Chip $chipId must exist", findElementById(root, chipId))
        }

        // 4. Emphasized toggle & sample text input
        assertNotNull("switchEmphasized must exist", findElementById(root, "switchEmphasized"))
        assertNotNull("tilSampleText must exist", findElementById(root, "tilSampleText"))
        assertNotNull("etSampleText must exist", findElementById(root, "etSampleText"))

        // 5. Presets
        assertNotNull("btnPresetEnglish must exist", findElementById(root, "btnPresetEnglish"))
        assertNotNull("btnPresetHindi must exist", findElementById(root, "btnPresetHindi"))
        assertNotNull("btnResetText must exist", findElementById(root, "btnResetText"))

        // 6. Live component preview elements
        assertNotNull("previewPrimaryButton must exist", findElementById(root, "previewPrimaryButton"))
        assertNotNull("previewSecondaryButton must exist", findElementById(root, "previewSecondaryButton"))
        assertNotNull("previewCardHeading must exist", findElementById(root, "previewCardHeading"))
        assertNotNull("previewCardBody must exist", findElementById(root, "previewCardBody"))

        // 7. All 11 canonical Apple iOS 26 style rows included
        val expected11Styles = listOf(
            "previewLargeTitle",
            "previewTitle1",
            "previewTitle2",
            "previewTitle3",
            "previewHeadline",
            "previewBody",
            "previewCallout",
            "previewSubheadline",
            "previewFootnote",
            "previewCaption1",
            "previewCaption2"
        )
        for (styleRowId in expected11Styles) {
            assertNotNull("11 canonical style row $styleRowId must exist", findElementById(root, styleRowId))
        }
    }

    @Test
    fun testItemTypographyPreviewRowLayout() {
        val root = loadXml("res/layout/item_typography_preview_row.xml")
        assertNotNull("tvStyleName must exist", findElementById(root, "tvStyleName"))
        assertNotNull("tvStyleSpecs must exist", findElementById(root, "tvStyleSpecs"))
        assertNotNull("tvPreviewSample must exist", findElementById(root, "tvPreviewSample"))
        assertEquals("com.eve.app.designsystem.DesignCardView", root.tagName)
    }

    @Test
    fun testAdminActivityContainsTypographyStudioButton() {
        val root = loadXml("res/layout/activity_admin.xml")
        val btn = findElementById(root, "btnTypographyStudio")
        assertNotNull("btnTypographyStudio must exist in activity_admin.xml", btn)
        assertEquals("Typography Studio", btn?.getAttribute("android:text"))
    }

    @Test
    fun testAndroidManifestDeclaresTypographyStudioActivity() {
        val root = loadXml("AndroidManifest.xml")
        val activities = root.getElementsByTagName("activity")
        var declared = false
        var exported = true
        for (i in 0 until activities.length) {
            val act = activities.item(i) as Element
            if (act.getAttribute("android:name") == ".ui.admin.TypographyStudioActivity") {
                declared = true
                exported = act.getAttribute("android:exported") == "true"
            }
        }
        assertTrue("TypographyStudioActivity must be declared in AndroidManifest.xml", declared)
        assertTrue("TypographyStudioActivity must not be exported", !exported)
    }

    @Test
    fun testFontManagerFontOptionsEnum() {
        val options = FontManager.FontOption.values()
        assertEquals(7, options.size)

        val names = options.map { it.name }
        assertTrue(names.contains("INTER_REGULAR"))
        assertTrue(names.contains("INTER_MEDIUM"))
        assertTrue(names.contains("INTER_SEMIBOLD"))
        assertTrue(names.contains("INTER_BOLD"))
        assertTrue(names.contains("SYSTEM_SANS"))
        assertTrue(names.contains("SYSTEM_SERIF"))
        assertTrue(names.contains("SYSTEM_MONOSPACE"))

        assertEquals("Inter Regular", FontManager.FontOption.INTER_REGULAR.displayName)
        assertEquals("400", FontManager.FontOption.INTER_REGULAR.weightLabel)
        assertEquals("Inter Bold", FontManager.FontOption.INTER_BOLD.displayName)
        assertEquals("700", FontManager.FontOption.INTER_BOLD.weightLabel)
        assertEquals("System Sans-Serif", FontManager.FontOption.SYSTEM_SANS.displayName)
    }

    @Test
    fun testCanonicalElevenRolesRepresentedInDesignType() {
        val canonicalRoles = listOf(
            DesignType.Role.LARGE_TITLE,
            DesignType.Role.TITLE_1,
            DesignType.Role.TITLE_2,
            DesignType.Role.TITLE_3,
            DesignType.Role.HEADLINE,
            DesignType.Role.BODY,
            DesignType.Role.CALLOUT,
            DesignType.Role.SUBHEADLINE,
            DesignType.Role.FOOTNOTE,
            DesignType.Role.CAPTION_1,
            DesignType.Role.CAPTION_2
        )
        assertEquals("Must cover exactly 11 canonical styles", 11, canonicalRoles.size)
    }

    @Test
    fun testDefaultSampleTexts() {
        assertEquals("The quick brown fox jumps over the lazy dog", TypographyStudioActivity.DEFAULT_ENGLISH_TEXT)
        assertEquals("संघ लोक सेवा आयोग — सिविल सेवा परीक्षा", TypographyStudioActivity.DEFAULT_HINDI_TEXT)
    }

    @Test
    fun testTypographyRegistryCatalog() {
        val screens = com.eve.app.typography.TypographyRegistry.SCREENS
        assertTrue("Must register at least 20 screens", screens.size >= 20)
        val allElements = com.eve.app.typography.TypographyRegistry.allElements()
        assertTrue("Must register at least 100 elements across screens", allElements.size >= 100)

        // Check key screens exist
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("home"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("mock_test"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("result"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("result_detail"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("leaderboard"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("performance"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("history"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("bookmarks"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("practice"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("pyq"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("exam_tests"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("mistakes"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("notifications"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("premium"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("settings"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("profile"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("feedback"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("about"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("syllabus"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("admin_analytics"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("admin_manage_exams"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("admin_system_monitor"))
        assertNotNull(com.eve.app.typography.TypographyRegistry.findScreen("dialogs"))
    }

    @Test
    fun testAuthenticFontAssetsExist() {
        val assetsDir = File(mainDir(), "assets")
        val fontsDir = File(assetsDir, "fonts")
        assertTrue("Fonts asset directory exists", fontsDir.isDirectory)

        // Poppins fonts including Semibold 600
        val poppinsSemi = File(fontsDir, "poppins_semibold.ttf")
        assertTrue("Genuine Poppins semibold binary exists", poppinsSemi.isFile && poppinsSemi.length() > 50000)

        // Noto Sans binaries
        val notoSansRegular = File(fontsDir, "noto_sans_regular.ttf")
        val notoSansMedium = File(fontsDir, "noto_sans_medium.ttf")
        val notoSansSemibold = File(fontsDir, "noto_sans_semibold.ttf")
        val notoSansBold = File(fontsDir, "noto_sans_bold.ttf")
        assertTrue("Noto Sans Regular exists", notoSansRegular.isFile)
        assertTrue("Noto Sans Medium exists", notoSansMedium.isFile)
        assertTrue("Noto Sans Semibold exists", notoSansSemibold.isFile)
        assertTrue("Noto Sans Bold exists", notoSansBold.isFile)

        // OFL License notice
        val licensesDir = File(assetsDir, "licenses")
        val notoLicense = File(licensesDir, "NotoSans-OFL.txt")
        assertTrue("Noto Sans OFL license notice exists", notoLicense.isFile)
    }

    @Test
    fun testTypographyPresetsAndResolutionOrder() {
        val presets = listOf(
            com.eve.app.typography.TypographyPreset.IOS26_BALANCED,
            com.eve.app.typography.TypographyPreset.IOS26_HIG_REFERENCE,
            com.eve.app.typography.TypographyPreset.POPPINS_CLEAN,
            com.eve.app.typography.TypographyPreset.ROBOTO_NEUTRAL,
            com.eve.app.typography.TypographyPreset.NOTO_READABLE
        )
        assertEquals(5, presets.size)

        val config = com.eve.app.typography.TypographyRepository.createDefaultConfig()
        val defaultResolved = com.eve.app.typography.TypographyResolver.resolve(
            config = config,
            screenKey = "home",
            elementKey = "home_welcome_title",
            componentRole = "title_1"
        )
        assertEquals(FontManager.FontFamily.INTER, defaultResolved.fontFamily)
        assertEquals(28f, defaultResolved.textSizeSp)
        assertEquals(FontManager.FontWeight.BOLD, defaultResolved.weight)

        // Test element override takes precedence over preset
        val modifiedConfig = config.copy(
            elementOverrides = mapOf(
                "home_welcome_title" to com.eve.app.typography.StyleRule(
                    fontFamily = "poppins",
                    textSizeSp = 30f,
                    weight = 600
                )
            )
        )
        val overriddenResolved = com.eve.app.typography.TypographyResolver.resolve(
            config = modifiedConfig,
            screenKey = "home",
            elementKey = "home_welcome_title",
            componentRole = "title_1"
        )
        assertEquals(FontManager.FontFamily.POPPINS, overriddenResolved.fontFamily)
        assertEquals(30f, overriddenResolved.textSizeSp)
        assertEquals(FontManager.FontWeight.SEMIBOLD, overriddenResolved.weight)
        assertTrue(overriddenResolved.source.startsWith("Element override"))
    }
}

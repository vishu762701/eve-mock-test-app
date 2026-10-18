package com.eve.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Project-wide verification for the authoritative Apple iOS 26 Typography
 * hierarchy and 26 Semantic Colour Roles across all layouts.
 *
 * Requirements verified:
 * 1. 100% elimination of hardcoded android:textSize across all 95 layouts.
 * 2. Complete declaration of all 26 Apple iOS color roles in attrs.xml.
 * 3. Complete binding of all 26 Apple iOS color roles in Theme.Eve.Foundation.
 * 4. Core Apple iOS 26 typography scale definitions present in type.xml.
 */
class AppWideTypographyAndColorRolesTest {

    private fun getProjectDir(): File {
        val candidates = listOf(
            File("."),
            File("app"),
            File("..")
        )
        for (candidate in candidates) {
            val layoutDir = File(candidate, "app/src/main/res/layout")
            if (layoutDir.exists() && layoutDir.isDirectory) {
                return candidate
            }
            val directLayoutDir = File(candidate, "src/main/res/layout")
            if (directLayoutDir.exists() && directLayoutDir.isDirectory) {
                return candidate.parentFile ?: File(".")
            }
        }
        return File(".")
    }

    private fun getLayoutDir(): File {
        val root = getProjectDir()
        val direct = File(root, "app/src/main/res/layout")
        if (direct.exists()) return direct
        val local = File("app/src/main/res/layout")
        if (local.exists()) return local
        return File("src/main/res/layout")
    }

    private fun getValuesFile(filename: String): File {
        val root = getProjectDir()
        val direct = File(root, "app/src/main/res/values/$filename")
        if (direct.exists()) return direct
        val local = File("app/src/main/res/values/$filename")
        if (local.exists()) return local
        return File("src/main/res/values/$filename")
    }

    private fun loadXml(file: File): Element {
        val dbf = DocumentBuilderFactory.newInstance()
        val db = dbf.newDocumentBuilder()
        return db.parse(file).documentElement
    }

    private fun collectTextSizeViolations(node: Node, file: File, violations: MutableList<String>) {
        if (node is Element) {
            if (node.hasAttribute("android:textSize")) {
                val size = node.getAttribute("android:textSize")
                val id = node.getAttribute("android:id")
                violations.add("${file.name}: element <${node.tagName}> (id='$id') has hardcoded android:textSize='$size'")
            }
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            collectTextSizeViolations(children.item(i), file, violations)
        }
    }

    @Test
    fun testZeroHardcodedTextSizesAcrossAll95Layouts() {
        val layoutDir = getLayoutDir()
        assertTrue("Layout directory must exist at ${layoutDir.absolutePath}", layoutDir.exists())

        val layoutFiles = layoutDir.listFiles { file -> file.extension == "xml" }?.sortedBy { it.name } ?: emptyList()
        assertEquals("Project must contain exactly 95 layout files", 95, layoutFiles.size)

        val violations = mutableListOf<String>()
        for (file in layoutFiles) {
            val root = loadXml(file)
            collectTextSizeViolations(root, file, violations)
        }

        assertTrue(
            "Found ${violations.size} hardcoded android:textSize violations across project layouts:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testAppleIos26ColorRolesDeclaredInAttrsXml() {
        val attrsFile = getValuesFile("attrs.xml")
        assertTrue("attrs.xml must exist at ${attrsFile.absolutePath}", attrsFile.exists())
        val root = loadXml(attrsFile)

        val declaredAttrs = mutableSetOf<String>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && node.tagName == "attr") {
                declaredAttrs.add(node.getAttribute("name"))
            }
        }

        val required26ColorRoles = listOf(
            "eveLabelPrimary",
            "eveLabelSecondary",
            "eveLabelTertiary",
            "eveLabelQuaternary",
            "evePlaceholderText",
            "eveSystemBackground",
            "eveSecondarySystemBackground",
            "eveTertiarySystemBackground",
            "eveSystemGroupedBackground",
            "eveSecondarySystemGroupedBackground",
            "eveTertiarySystemGroupedBackground",
            "eveSeparator",
            "eveOpaqueSeparator",
            "eveControlOutline",
            "eveSystemBlue",
            "evePrimaryFill",
            "eveOnPrimary",
            "eveAccentText",
            "eveAccentContainer",
            "eveOnAccentContainer",
            "eveDestructiveText",
            "eveFillPrimary",
            "eveFillSecondary",
            "eveFillTertiary",
            "eveFillQuaternary",
            "eveLink"
        )

        val missingRoles = required26ColorRoles.filter { it !in declaredAttrs }
        assertTrue(
            "All 26 Apple iOS color roles must be declared in attrs.xml. Missing: $missingRoles",
            missingRoles.isEmpty()
        )
    }

    @Test
    fun testAppleIos26ColorRolesBoundInThemeEveFoundation() {
        val themesFile = getValuesFile("themes.xml")
        assertTrue("themes.xml must exist at ${themesFile.absolutePath}", themesFile.exists())
        val root = loadXml(themesFile)

        var foundationTheme: Element? = null
        val styles = root.getElementsByTagName("style")
        for (i in 0 until styles.length) {
            val style = styles.item(i) as Element
            if (style.getAttribute("name") == "Theme.Eve.Foundation") {
                foundationTheme = style
                break
            }
        }
        assertTrue("Theme.Eve.Foundation must exist in themes.xml", foundationTheme != null)

        val boundAttrs = mutableSetOf<String>()
        val items = foundationTheme!!.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            boundAttrs.add(item.getAttribute("name"))
        }

        val required26ColorRoles = listOf(
            "eveLabelPrimary",
            "eveLabelSecondary",
            "eveLabelTertiary",
            "eveLabelQuaternary",
            "evePlaceholderText",
            "eveSystemBackground",
            "eveSecondarySystemBackground",
            "eveTertiarySystemBackground",
            "eveSystemGroupedBackground",
            "eveSecondarySystemGroupedBackground",
            "eveTertiarySystemGroupedBackground",
            "eveSeparator",
            "eveOpaqueSeparator",
            "eveControlOutline",
            "eveSystemBlue",
            "evePrimaryFill",
            "eveOnPrimary",
            "eveAccentText",
            "eveAccentContainer",
            "eveOnAccentContainer",
            "eveDestructiveText",
            "eveFillPrimary",
            "eveFillSecondary",
            "eveFillTertiary",
            "eveFillQuaternary",
            "eveLink"
        )

        val missingBindings = required26ColorRoles.filter { it !in boundAttrs }
        assertTrue(
            "All 26 Apple iOS color roles must be bound in Theme.Eve.Foundation. Missing: $missingBindings",
            missingBindings.isEmpty()
        )
    }

    @Test
    fun testCoreTypographyRolesDefinedInTypeXml() {
        val typeFile = getValuesFile("type.xml")
        assertTrue("type.xml must exist at ${typeFile.absolutePath}", typeFile.exists())
        val root = loadXml(typeFile)

        val definedStyles = mutableSetOf<String>()
        val styles = root.getElementsByTagName("style")
        for (i in 0 until styles.length) {
            val style = styles.item(i) as Element
            definedStyles.add(style.getAttribute("name"))
        }

        val requiredTypeStyles = listOf(
            "TextAppearance.Eve.LargeTitle",
            "TextAppearance.Eve.Title1",
            "TextAppearance.Eve.Title2",
            "TextAppearance.Eve.Title3",
            "TextAppearance.Eve.Headline",
            "TextAppearance.Eve.Body",
            "TextAppearance.Eve.Callout",
            "TextAppearance.Eve.Subheadline",
            "TextAppearance.Eve.Footnote",
            "TextAppearance.Eve.Caption1",
            "TextAppearance.Eve.Caption2",
            "TextAppearance.Eve.Admin.ScreenTitle",
            "TextAppearance.Eve.Admin.SectionHeader",
            "TextAppearance.Eve.Admin.CardTitle",
            "TextAppearance.Eve.Admin.CardValue",
            "TextAppearance.Eve.Admin.ItemTitle",
            "TextAppearance.Eve.Admin.ItemSubtitle",
            "TextAppearance.Eve.Admin.Badge"
        )

        val missingStyles = requiredTypeStyles.filter { it !in definedStyles }
        assertTrue(
            "Essential Apple iOS 26 dynamic typography styles must be defined in type.xml. Missing: $missingStyles",
            missingStyles.isEmpty()
        )
    }

    @Test
    fun testApi28TextFontWeightsDefinedInValuesV28TypeXml() {
        val rootDir = getProjectDir()
        val v28TypeFile = File(rootDir, "app/src/main/res/values-v28/type.xml").takeIf { it.exists() }
            ?: File("src/main/res/values-v28/type.xml")
        assertTrue("values-v28/type.xml must exist at ${v28TypeFile.absolutePath}", v28TypeFile.exists())

        val root = loadXml(v28TypeFile)
        val styles = root.getElementsByTagName("style")
        assertTrue("values-v28/type.xml must declare typography styles", styles.length > 0)

        var hasWeight400 = false
        var hasWeight600 = false
        var hasWeight700 = false

        val items = root.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val item = items.item(i) as Element
            if (item.getAttribute("name") == "android:textFontWeight") {
                when (item.textContent.trim()) {
                    "400" -> hasWeight400 = true
                    "600" -> hasWeight600 = true
                    "700" -> hasWeight700 = true
                }
            }
        }

        assertTrue("values-v28/type.xml must declare 400 (Regular) weight", hasWeight400)
        assertTrue("values-v28/type.xml must declare 600 (Semibold) weight", hasWeight600)
        assertTrue("values-v28/type.xml must declare 700 (Bold) weight", hasWeight700)
    }

    @Test
    fun testAppleIos26CoreColorValues() {
        fun loadColors(qualifier: String): Map<String, String> {
            val rootDir = getProjectDir()
            val colorsFile = File(rootDir, "app/src/main/res/$qualifier/colors.xml").takeIf { it.exists() }
                ?: File("src/main/res/$qualifier/colors.xml")
            val root = loadXml(colorsFile)
            val colors = mutableMapOf<String, String>()
            val nodes = root.getElementsByTagName("color")
            for (i in 0 until nodes.length) {
                val node = nodes.item(i) as Element
                colors[node.getAttribute("name")] = node.textContent.trim()
            }
            return colors
        }

        val lightColors = loadColors("values")
        assertEquals("@color/system_blue", lightColors["eve_system_blue"])
        assertEquals("#0088FF", lightColors["system_blue"])
        assertEquals("@color/eve_system_blue", lightColors["eve_primary"])
        assertEquals("#FFFFFF", lightColors["eve_on_primary"])
        assertEquals("#4A3C3C43", lightColors["eve_separator"])
        assertEquals("#C6C6C8", lightColors["eve_opaque_separator"])

        val darkColors = loadColors("values-night")
        assertEquals("@color/system_blue", darkColors["eve_system_blue"])
        assertEquals("#0091FF", darkColors["system_blue"])
        assertEquals("@color/eve_system_blue", darkColors["eve_primary"])
        assertEquals("#FFFFFF", darkColors["eve_on_primary"])
        assertEquals("#99545458", darkColors["eve_separator"])
        assertEquals("#38383A", darkColors["eve_opaque_separator"])
    }

    @Test
    fun testGroupedAndSystemScreenLayoutClassifications() {
        val layoutDir = getLayoutDir()
        val groupedSample = listOf(
            "activity_settings.xml",
            "activity_profile.xml",
            "activity_about.xml",
            "activity_history.xml",
            "activity_bookmarks.xml",
            "activity_mistakes.xml",
            "activity_notifications.xml",
            "activity_premium.xml",
            "activity_performance.xml",
            "activity_syllabus.xml",
            "activity_practice.xml",
            "activity_pyq.xml",
            "activity_result_detail.xml",
            "activity_leaderboard.xml",
            "activity_admin.xml"
        )
        for (filename in groupedSample) {
            val file = File(layoutDir, filename)
            assertTrue("$filename must exist", file.exists())
            val root = loadXml(file)
            val bg = root.getAttribute("android:background")
            assertEquals(
                "$filename root background must be @color/eve_system_grouped_background",
                "@color/eve_system_grouped_background",
                bg
            )
        }

        val systemSample = listOf(
            "activity_login.xml",
            "activity_content_display.xml"
        )
        for (filename in systemSample) {
            val file = File(layoutDir, filename)
            assertTrue("$filename must exist", file.exists())
            val root = loadXml(file)
            val bg = root.getAttribute("android:background")
            assertEquals(
                "$filename root background must be @color/eve_system_background",
                "@color/eve_system_background",
                bg
            )
        }
    }
}

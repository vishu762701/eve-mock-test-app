package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-traced role guards for the migrated subset only. */
class TelegramTypeSourceRolesTest {
    private fun read(path: String): String = File("src/main/res/" + path).readText()

    @Test fun sizesAndFonts() {
        val s = read("values/telegram_typography.xml")
        val roles = mapOf("NavigationTitle" to 20, "SettingsLabel" to 16,
            "SettingsDetail" to 13, "SettingsSection" to 14, "MenuItem" to 16,
            "DialogTitle" to 20, "DialogMessage" to 16, "SearchInput" to 18,
            "NotificationTitle" to 17, "NotificationBody" to 16, "NotificationTime" to 12)
        for ((role, size) in roles) {
            val start = s.indexOf("<style name=\"TextAppearance.Eve.Telegram." + role + "\"")
            assertTrue("No role: " + role, start >= 0)
            val end = s.indexOf("</style>", start)
            assertTrue("Unclosed role: " + role, end > start)
            val body = s.substring(start, end)
            assertTrue("Wrong SP size: " + role, body.contains(">" + size + "sp</item>"))
            val bundledRoles = setOf("NavigationTitle", "SettingsSection", "DialogTitle", "NotificationTitle")
            val expectedFont = if (role in bundledRoles) "@font/telegram_roboto_medium" else "sans-serif"
            assertTrue("Invalid source-mapped font family: " + role, body.contains(expectedFont))
            assertTrue("Inter leaked: " + role, !body.contains("@font/inter"))
        }
    }
    @Test fun pairedColorsAndProtectedUsages() {
        for (role in listOf("primary", "secondary", "section", "dialog")) {
            for (qualifier in listOf("values", "values-night")) {
                assertTrue("Missing " + role + " in " + qualifier,
                    read(qualifier + "/telegram_colors.xml").contains("name=\"telegram_type_" + role + "\""))
            }
        }
        for (file in listOf("activity_settings.xml", "activity_notifications.xml",
            "activity_result.xml", "popup_telegram_menu.xml",
            "activity_about.xml", "item_notification.xml")) {
            val text = read("layout/" + file)
            val roles = "TextAppearance.Eve.Telegram.".toRegex().findAll(text).count()
            val marks = "android:tag=\"eve_telegram_type\"".toRegex().findAll(text).count()
            assertTrue("Wrong tag count for " + file, roles > 0 && roles == marks)
        }
    }

    /**
     * Guard against the native rendering failure observed on 360dp and 320dp:
     * contrast 3.36:1 for the previous Telegram gray against Eve's #F5F5F5 canvas.
     * Check BOTH Eve canvas and solid card, Day and Night, without changing test thresholds.
     */
    @Test fun textRolesMeetNormalTextContrastOnDayAndNightSurfaces() {
        val cases = listOf(
            Triple("values", "#F5F5F5", "#FFFFFF"),
            Triple("values-night", "#000000", "#1C1C1E")
        )
        for ((qualifier, canvas, surface) in cases) {
            val content = read(qualifier + "/telegram_colors.xml")
            for (role in listOf("primary", "secondary", "section", "dialog")) {
                val matcher = Regex("<color name=\"telegram_type_" + role + "\">(#?[0-9A-Fa-f]{6,8})</color>").find(content)
                assertTrue("Missing/invalid " + role + " " + qualifier, matcher != null)
                val foreground = matcher!!.groupValues[1]
                for (background in listOf(canvas, surface)) {
                    val contrast = contrast(foreground, background)
                    assertTrue("Inaccessible " + qualifier + " " + role + " on " + background + ": " + contrast,
                        contrast >= 4.5)
                }
            }
        }
        val styles = read("values/telegram_typography.xml")
        val start = styles.indexOf("<style name=\"TextAppearance.Eve.Telegram.SettingsSection\"")
        val end = styles.indexOf("</style>", start)
        assertTrue("Settings section must use dedicated accessible header theme role",
            start >= 0 && end > start && styles.substring(start, end).contains("@color/telegram_type_section"))
    }

    @Test fun homeSearchHasNativeInputStateAndTelegramText() {
        val style = read("values/design_widgets.xml")
        val start = style.indexOf("<style name=\"Widget.Eve.Token.Input.TelegramSearch\"")
        assertTrue("Search style is absent", start >= 0)
        val end = style.indexOf("</style>", start)
        val body = style.substring(start, end)
        assertTrue(body.contains("parent=\"Widget.Eve.Token.Input\""))
        assertTrue(body.contains("@style/TextAppearance.Eve.Telegram.SearchInput"))
        assertTrue(body.contains("18sp"))
        val home = read("layout/activity_main.xml")
        assertTrue(home.contains("id=\"@+id/etSearch\" style=\"@style/Widget.Eve.Token.Input.TelegramSearch\" android:tag=\"eve_telegram_type\""))
        val item = read("layout/item_notification.xml")
        for (role in listOf("NotificationTitle", "NotificationBody", "NotificationTime")) {
            assertTrue("Missing mapped notification role " + role,
                item.contains("style=\"@style/TextAppearance.Eve.Telegram." + role + "\" android:tag=\"eve_telegram_type\""))
        }
        // EVE notification bell remains functional until source-exact art can be licensed.
        assertTrue(item.contains("@drawable/ic_bell"))
    }

    @Test fun globalLegacyInterCannotOverrideNativeSourceTypography() {
        val theme = read("values/themes.xml")
        assertTrue(theme.contains("<item name=\"android:fontFamily\">sans-serif</item>"))
        val styles = read("values/design_type.xml")
        val widgets = read("values/design_widgets.xml")
        assertTrue("Migrated global styles must not force bundled Inter", !styles.contains("@font/inter"))
        assertTrue("Shared widgets must not force bundled Inter", !widgets.contains("@font/inter"))
        val dimens = read("values/design_dimens.xml")
        for ((key, expected) in mapOf("design_type_title" to "20sp",
            "design_type_body" to "16sp", "design_type_heading" to "17sp")) {
            assertTrue("Wrong $key", dimens.contains("<dimen name=\"" + key + "\">" + expected + "</dimen>"))
        }
    }

    @Test fun noUnlicensedIconClaimAndLegacyPresetBoundary() {
        val repo = File("src/main/java/com/eve/app/typography/TypographyRepository.kt").readText()
        assertTrue(repo.contains("parsed.presetId == \"telegram_native\""))
        assertTrue(repo.contains("config.presetId == \"telegram_native\""))
        val font = File("src/main/java/com/eve/app/util/FontManager.kt").readText()
        assertTrue(font.contains("var activeFontFamily: FontFamily = FontFamily.ROBOTO"))
        for (name in listOf("ic_back", "ic_more_vert", "ic_search", "ic_delete",
            "ic_bell", "ic_settings", "ic_bookmark", "ic_close", "ic_send", "ic_share")) {
            val xml = File("src/main/res/drawable/" + name + ".xml").readText()
            assertTrue("Preserve EVE icon view resource " + name, xml.contains("<vector"))
            assertTrue("Preserve EVE icon dimensions " + name, xml.contains("@dimen/design_icon_size"))
            assertTrue("Unlicensed Telegram asset in " + name,
                !xml.contains("msg_search.webp") && !xml.contains("msg_share.webp"))
        }
    }

    @Test fun sourceExactActionBarOrientationAndAlertDialogTextRoles() {
        val phoneLand = read("values-land/telegram_typography.xml")
        val tabletLand = read("values-sw600dp-land/telegram_typography.xml")
        assertTrue("ActionBar phone landscape title is 18sp", phoneLand.contains("18sp"))
        assertTrue("ActionBar tablet landscape title is 20sp", tabletLand.contains("20sp"))
        val dialogTitleFragment = """style="@style/TextAppearance.Eve.Telegram.DialogTitle" android:tag="eve_telegram_type""""
        for (file in listOf("dialog_create_feedback_post.xml", "dialog_reply_feedback.xml")) {
            val xml = read("layout/" + file)
            assertTrue("Dialog heading must use source title role",
                xml.contains(dialogTitleFragment))
        }
        val widgets = read("values/design_widgets.xml")
        assertTrue(widgets.contains("@style/TextAppearance.Eve.Telegram.DialogTitle"))
        assertTrue(widgets.contains("@style/TextAppearance.Eve.Telegram.DialogMessage"))
    }

    @Test fun googleRobotoMediumExactlyMatchesTelegramDefaultAndIsLicensed() {
        val font = File("src/main/res/font/telegram_roboto_medium.ttf")
        assertTrue("Telegram source-matched Roboto Medium missing", font.isFile && font.length() == 274864L)
        val bytes = font.readBytes()
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        org.junit.Assert.assertEquals(
            "c4a17ec40f13be4a229e34893e3e04f19dd190f48b4908e0649402cb2229e9cb",
            sha
        )
        val license = File("src/main/assets/licenses/Roboto-APACHE-2.0.txt")
        assertTrue("Full Roboto Apache license missing", license.readText().contains("Apache License"))
        val notice = File("src/main/assets/licenses/Roboto-Medium-NOTICE.txt")
        assertTrue("Google copyright notice missing", notice.readText().contains("Copyright 2011 Google Inc."))
    }

    private fun contrast(foreground: String, background: String): Double {
        fun luminance(hex: String): Double {
            val rgb = hex.removePrefix("#").takeLast(6)
            val channels = listOf(0, 2, 4).map { index ->
                val v = rgb.substring(index, index + 2).toInt(16) / 255.0
                if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
            }
            return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722
        }
        val a = luminance(foreground)
        val b = luminance(background)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}

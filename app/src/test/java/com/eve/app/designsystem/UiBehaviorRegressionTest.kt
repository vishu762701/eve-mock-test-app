package com.eve.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Behavior, accessibility and contrast guards retained from the former Telegram test files. */
class UiBehaviorRegressionTest {
    private fun actionIcons(file: File): List<Element> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        val doc = factory.newDocumentBuilder().parse(file)
        val found = mutableListOf<Element>()
        fun visit(node: Node) {
            if (node is Element) {
                val source = node.getAttribute("android:src")
                val navigation = node.getAttribute("app:navigationIcon")
                if (source == "@drawable/ic_back" || source == "@drawable/ic_more_vert" ||
                    navigation == "@drawable/ic_back") {
                    found.add(node)
                }
            }
            val children = node.childNodes
            for (index in 0 until children.length) visit(children.item(index))
        }
        visit(doc.documentElement)
        return found
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

    private fun read(path: String): String = File("src/main/res/" + path).readText()

    private fun source(path: String): String = File("src/main/java/com/eve/app/" + path).readText()

    @Test fun everyActivityNavigationActionUsesTelegramSourceIconColors() {
        val files = File("src/main/res/layout").listFiles()!!.filter {
            it.isFile && it.name.startsWith("activity_") && it.extension == "xml"
        }
        var affected = 0
        for (file in files) {
            for (icon in actionIcons(file)) {
                val toolbarNavigation = icon.getAttribute("app:navigationIcon") == "@drawable/ic_back"
                val tintAttr = if (toolbarNavigation) "app:navigationIconTint" else "app:tint"
                assertEquals("Incorrect ActionBar tint in ${file.name}",
                    "@color/telegram_type_primary", icon.getAttribute(tintAttr))
                if (!toolbarNavigation) {
                    assertTrue("Navigation icon lacks content description in ${file.name}",
                        icon.hasAttribute("android:contentDescription"))
                }
                affected++
            }
        }
        assertTrue("Incomplete source role coverage of ActionBar navigation", affected >= 40)
    }

    @Test fun sourceMappedToolbarActionsHavePairedThemeTintsAndPreservedTargets() {
        val day = File("src/main/res/values/telegram_colors.xml").readText()
        val night = File("src/main/res/values-night/telegram_colors.xml").readText()
        assertTrue(day.contains("<color name=\"telegram_type_primary\">#FF1A1D21</color>"))
        assertTrue(night.contains("<color name=\"telegram_type_primary\">#FFFFFFFF</color>"))
        val screens = listOf("activity_main", "activity_settings", "activity_notifications",
            "activity_result", "activity_profile", "activity_result_detail",
            "activity_about", "activity_admin", "activity_appearance")
        var actions = 0
        for (screen in screens) {
            val file = File("src/main/res/layout/$screen.xml")
            val icons = actionIcons(file).filter {
                it.getAttribute("android:src") == "@drawable/ic_back" ||
                    it.getAttribute("android:src") == "@drawable/ic_more_vert"
            }
            assertTrue("No corresponding ActionBar action in $screen", icons.isNotEmpty())
            for (icon in icons) {
                assertEquals("Tint is not Telegram Day/Night action ink in $screen",
                    "@color/telegram_type_primary", icon.getAttribute("app:tint"))
                assertEquals("Touch size missing $screen",
                    "@dimen/design_touch_min", icon.getAttribute("android:layout_width"))
                assertTrue("Content description missing $screen",
                    icon.hasAttribute("android:contentDescription"))
                actions++
            }
        }
        assertEquals(10, actions)
    }

    @Test fun variableLengthMetadataStillEllipsizesAndAdminActionsRemain() {
        val s = File("src/main/res/layout/item_syllabus.xml").readText()
        val user = File("src/main/res/layout/item_user_admin.xml").readText()
        val banner = File("src/main/res/layout/item_admin_banner.xml").readText()
        assertTrue(s.contains("android:ellipsize=\"middle\""))
        assertTrue(s.contains("android:singleLine=\"true\""))
        assertTrue(user.contains("android:ellipsize=\"end\""))
        assertTrue(user.contains("android:id=\"@+id/btnMoreUser\""))
        assertTrue(banner.contains("android:id=\"@+id/btnDeleteBanner\""))
        assertTrue(banner.contains("android:id=\"@+id/btnMoveUp\""))
        assertTrue(banner.contains("android:id=\"@+id/btnMoveDown\""))
    }

    @Test fun bookmarkHasTrueOnOffArtworkAndAccessibleNames() {
        val code = File("src/main/java/com/eve/app/designsystem/DesignIconStates.kt").readText()
        assertTrue("Selected bookmark needs distinguishable filled artwork",
            code.contains("if (checked) R.drawable.ic_star_filled else R.drawable.ic_star_outline"))
        assertTrue(code.contains("view.isSelected = checked"))
        assertTrue(code.contains("ImageViewCompat.setImageTintList"))
        assertTrue(code.contains("Remove bookmark"))
        assertTrue(code.contains("Bookmark question"))
        val question = File("src/main/java/com/eve/app/ui/test/QuestionAdapter.kt").readText()
        val saved = File("src/main/java/com/eve/app/ui/bookmarks/BookmarkAdapter.kt").readText()
        assertTrue(question.contains("DesignIconStates.bookmark(b.btnBookmark, nowBookmarked)"))
        assertTrue(saved.contains("DesignIconStates.bookmark(binding.btnBookmark, true)"))
        assertTrue(saved.contains("DesignIconStates.bookmark(binding.btnBookmark, false)"))
    }

    @Test fun iconActionsUseTelegramNative20DpSelectorNotInventedScale() {
        val style = File("src/main/res/values/design_widgets.xml").readText()
        val begin = style.indexOf("<style name=\"Widget.Eve.Token.IconButton\"")
        assertTrue(begin >= 0)
        val section = style.substring(begin, style.indexOf("</style>", begin))
        assertTrue(section.contains("<item name=\"tint\">@color/telegram_type_primary</item>"))
        assertTrue(section.contains("<item name=\"android:minWidth\">@dimen/design_touch_min</item>"))
        assertTrue(section.contains("<item name=\"android:minHeight\">@dimen/design_touch_min</item>"))
        assertTrue("Bare icon views must not paint a background", section.contains("<item name=\"android:background\">@null</item>"))
        assertTrue("Foreground ripple preserves Telegram 20dp press feedback",
            section.contains("<item name=\"android:foreground\">@drawable/telegram_action_icon_ripple</item>"))
        assertTrue(section.contains("<item name=\"android:stateListAnimator\">@null</item>"))
        val ripple = File("src/main/res/drawable/telegram_action_icon_ripple.xml").readText()
        assertTrue(ripple.contains("<ripple"))
        assertTrue(ripple.contains("android:radius=\"20dp\""))
        assertTrue(ripple.contains("android:color=\"@color/telegram_action_selector\""))
        assertTrue(File("src/main/res/values/telegram_colors.xml").readText()
            .contains("<color name=\"telegram_action_selector\">#121A1D21</color>"))
        assertTrue(File("src/main/res/values-night/telegram_colors.xml").readText()
            .contains("<color name=\"telegram_action_selector\">#16FFFFFF</color>"))
        // Preserve generic scale motion for EVE's non-icon card, tab and row controls.
        assertTrue(style.contains("<item name=\"android:stateListAnimator\">@animator/design_press_scale</item>"))
    }

    @Test fun factoryUsesTelegramNotRetiredIosProfile() {
        val m = source("typography/TypographyModels.kt")
        val r = source("typography/TypographyRepository.kt")
        val resolver = source("typography/TypographyResolver.kt")
        assertTrue(m.contains("val TELEGRAM_NATIVE = TypographyPreset("))
        assertTrue(m.contains("id = \"telegram_native\""))
        assertTrue(m.contains("val presetId: String = \"telegram_native\""))
        assertTrue(m.contains("val fontFamily: String = \"roboto\""))
        assertTrue(m.contains("?: TELEGRAM_NATIVE"))
        assertTrue(r.contains("val preset = TypographyPreset.TELEGRAM_NATIVE"))
        assertTrue(source("typography/TypographyApplier.kt").contains("if (config.screenOverrides.isEmpty() &&"))
        assertTrue(resolver.contains("val defaultPreset = TypographyPreset.TELEGRAM_NATIVE"))
        assertTrue(resolver.contains("fallbackFamily = \"roboto\""))
        assertTrue(r.contains("val parsed = gson.fromJson(json, TypographyConfig::class.java)"))
        assertTrue(r.contains("if (!isTestActive)"))
    }

    @Test fun existingTelegramXmlMediumNeverDowngradedByDefaultProfile() {
        val m = source("typography/TypographyApplier.kt")
        val f = source("util/FontManager.kt")
        assertTrue(m.contains("if (view.tag == \"eve_telegram_type\") return"))
        assertTrue(m.contains("if (hasExplicitOverride)"))
        assertTrue(m.contains("view.paint.isFakeBoldText = false"))
        assertTrue(!m.contains("view.typeface = FontManager.getTypeface(view.context, family, weight)"))
        assertTrue(f.contains("?: ROBOTO"))
        assertTrue(f.contains("R.font.telegram_roboto_medium"))
    }

    @Test fun homeToolbarUsesCoherentDayNightSourceInkWithoutHidingNotifications() {
        val xml = File("src/main/res/layout/activity_main.xml").readText()
        for (id in listOf("btnSearch", "ivNotificationIdle", "btnOverflow", "btnClearSearch")) {
            val line = xml.lineSequence().first { it.contains("android:id=\"@+id/" + id + "\"") }
            assertTrue("Wrong ActionBar tint on " + id, line.contains("app:tint=\"@color/telegram_type_primary\""))
        }
        assertTrue(xml.contains("android:id=\"@+id/notificationAction\""))
        assertTrue(xml.contains("android:src=\"@drawable/ic_bell\""))
        assertTrue(xml.contains("app:lottie_rawRes=\"@raw/notification_bell\""))
        val main = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        assertTrue(main.contains("binding.ivNotificationIdle.visibility = View.VISIBLE"))
        assertTrue(main.contains("binding.btnNotification.alpha = 0f"))
    }

    @Test fun menuIconsPreserveSemanticsAndMapTelegramInk() {
        val menu = File("src/main/res/layout/popup_telegram_menu.xml").readText()
        for (line in menu.lineSequence().filter { it.contains("<ImageView") && it.contains("android:src=\"@drawable/") }) {
            assertTrue("Mixed menu icon tint: " + line, line.contains("app:tint=\"@color/telegram_type_primary\""))
        }
        val mistakeLine = menu.lineSequence().first { it.contains("android:contentDescription=\"Mistake Notebook\"") }
        assertTrue("Mistake action must not be represented by Close", mistakeLine.contains("android:src=\"@drawable/telegram_popup_notes\""))
        assertTrue(menu.contains("android:id=\"@+id/menuRowMistakes\""))
    }


    @Test fun popupOfficialTelegramIconsKeepOriginalSourceBytes() {
        val menu = File("src/main/res/layout/popup_telegram_menu.xml").readText()
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        assertTrue(menu.contains("android:src=\"@drawable/telegram_popup_notes\""))
        assertTrue(menu.contains("android:src=\"@drawable/telegram_popup_topics\""))
        assertTrue(menu.contains("android:src=\"@drawable/telegram_popup_paper\""))
        assertTrue(popup.contains("R.drawable.telegram_menu_night_mode_24"))
        val mdpiToXxhdpi = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi")
        val expected = mapOf(
            "telegram_popup_topics" to listOf(
                "b97155cd49fb0504f0d5c0f61245fac95ce0646d74c4f3b0673aa1a85d0cec83",
                "3bc5c5743b3d41361ad77373ab506621a557c7945919f5339d958f7cf18688d5",
                "c89f25e558b3a8e0db32c29416e92a569d0881a0f85063356c1022ac2aeace84",
                "0d8826e7c835e0fc8edf2678b36ac955cf9ded826521512a854a9bdfa22c8812"),
            "telegram_popup_paper" to listOf(
                "023818805e20163fb85ba62486e1767a8a89741117b583510ff41f74f0474330",
                "462347aa66a287114dfb4586e2de45534b4f867347990c4225df364d2138e3cb",
                "4ac7fadf911592a184dbe8032457f87096d7c041c7635549cf1aeb12295120e2",
                "ec68b5f82706205d8a50d9673838365a8e0bd77f2c1a855957bdc58cc9a95282"),
            "telegram_popup_notes" to listOf(
                "8e6cdfa4fa8103e3d0a441e68023e457b4cfc3724653364a368231ff7ab48430",
                "2f4e571154cfc2a40e6652bd9004d76b383fc5d2fd1b67e19bd9e423ce57b48b",
                "7668ec03ef61274e970fbe2a78d70c46d7b10d77b73118e4b1aa9d296c2a21af",
                "b45b9d4c8f4c34b799344cfbcdcc58625adaf2fb979f40b3d7d6957249746cfd")
        )
        fun sha256(data: ByteArray): String =
            java.security.MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        for ((name, hashes) in expected) {
            for ((index, density) in mdpiToXxhdpi.withIndex()) {
                val resource = File("src/main/res/drawable-${density}/${name}.webp")
                assertTrue("Missing original ${resource.path}", resource.isFile)
                assertEquals("Changed original Telegram asset ${resource.path}", hashes[index], sha256(resource.readBytes()))
            }
        }
        val moon = File("src/main/res/drawable/telegram_menu_night_mode_24.xml")
        assertTrue(moon.isFile)
        assertEquals("Source Telegram menu_night_mode_24 vector must be unmodified",
            "66f143dbffd1c5abbc85dae15de22161a6a7d3c13ded1fc0168051b2af41d861", sha256(moon.readBytes()))
    }

    @Test fun adminStateAndExamControlsAreUntouched() {
        val base = listOf(File("src/main/res/layout"), File("app/src/main/res/layout")).first { it.isDirectory }
        val xml = File(base, "item_admin_transaction.xml").readText()
        assertTrue(xml.contains("android:id=\"@+id/tvOrderAmount\""))
        assertTrue(xml.contains("android:id=\"@+id/tvOrderStatus\""))
        val sent = File(base, "item_sent_broadcast.xml").readText()
        assertTrue(sent.contains("android:id=\"@+id/tvBroadcastTarget\""))
        val test = File(base, "item_exam_test.xml").readText()
        assertTrue(test.contains("android:id=\"@+id/tvStatus\""))
    }

    @Test fun historyResultUsesSourcePairedTitleAndSubtitle() {
        val view = File("src/main/res/layout/activity_result.xml").readText()
        val source = File("src/main/java/com/eve/app/ui/result/ResultActivity.kt").readText()
        val title = view.lineSequence().first { it.contains("android:id=\"@+id/tvResultTitle\"") }
        val subtitle = view.lineSequence().first { it.contains("android:id=\"@+id/tvResultSubtitle\"") }
        assertTrue(title.contains("@style/TextAppearance.Eve.Telegram.NavigationTitle"))
        assertTrue(subtitle.contains("style=\"@style/TextAppearance.Eve.Telegram.NavigationSubtitle\""))
        assertTrue(subtitle.contains("android:tag=\"eve_telegram_type\""))
        assertTrue(source.contains("if (fromHistory)"))
        assertTrue(source.contains("R.style.TextAppearance_Eve_Telegram_NavigationTitle_WithSubtitle"))
        assertTrue(source.contains("R.style.TextAppearance_Eve_Telegram_NavigationTitle"))
        assertTrue(source.contains("binding.tvResultSubtitle.visibility = View.GONE"))
        assertTrue(source.contains("Attempted on "))
    }

    @Test fun semanticStatusOverridesRemainExplicit() {
        val styles = File("src/main/res/values/design_semantic_type.xml").readText()
        for (role in listOf("Body", "Secondary")) {
            for (semantic in listOf("blue", "green", "orange", "red")) {
                assertTrue("Preserve " + role + " " + semantic,
                    styles.contains("<style name=\"TextAppearance.Eve.Token." + role + "." + semantic + "\""))
            }
        }
        val day = File("src/main/res/values/telegram_colors.xml").readText()
        val night = File("src/main/res/values-night/telegram_colors.xml").readText()
        assertTrue(day.contains("<color name=\"telegram_type_primary\">#FF1A1D21</color>"))
        assertTrue(night.contains("<color name=\"telegram_type_primary\">#FFFFFFFF</color>"))
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

    @Test fun textRolesMeetNormalTextContrastOnDayAndNightSurfaces() {
        val cases = listOf(
            Triple("values", "#F1F1F3", "#FFFFFF"),
            Triple("values-night", "#000000", "#181819")
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

    @Test fun dialogActionsUseActualTelegramDefaultFontWhileRetainingEveButtons() {
        val styles = read("values/design_widgets.xml")
        for (role in listOf("Primary", "Secondary")) {
            val start = styles.indexOf("<style name=\"Widget.Eve.Telegram.DialogAction." + role + "\"")
            assertTrue("Missing " + role + " dialog action", start >= 0)
            val end = styles.indexOf("</style>", start)
            val part = styles.substring(start, end)
            assertTrue("Unmapped Telegram button font " + role,
                part.contains("@font/telegram_roboto_medium"))
            assertTrue("Missing source-derived 16sp text " + role, part.contains(">16sp</item>"))
            assertTrue("Lost EVE button style inheritance " + role, part.contains("parent=\"Widget.Eve.Token.Button"))
        }
        val theme = read("values/themes.xml")
        assertTrue(theme.contains("buttonBarPositiveButtonStyle\">@style/Widget.Eve.Telegram.DialogAction.Primary"))
        assertTrue(theme.contains("buttonBarNegativeButtonStyle\">@style/Widget.Eve.Telegram.DialogAction.Secondary"))
        assertTrue(theme.contains("buttonBarNeutralButtonStyle\">@style/Widget.Eve.Telegram.DialogAction.Secondary"))
        val destructive = File("src/main/java/com/eve/app/designsystem/DesignDialogs.kt").readText()
        assertTrue(destructive.contains("FontManager.telegramMediumTypeface(button.context)"))
        assertTrue(destructive.contains("design_destructive_text"))
    }
}

package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Seven EVE actions remain reachable as 48dp+ standalone accessibility targets. */
class HomeMenuAccessibilityContractTest {
    private val rowIds = listOf(
        "cardTheme", "menuRowHistory", "menuRowBookmarks", "menuRowMistakes",
        "menuRowTopic", "menuRowPyq", "menuRowLogout"
    )

    @Test fun popupLabelGeometryMatchesOfficialTelegramSourceTextInset() {
        // ActionBarMenuSubItem.java:83-84/206-218. EVE linear-host mapping:
        // original 18dp outer + 43dp inner = EVE 18dp + 24dp icon + 19dp gap.
        val source = File("src/main/res/values/telegram_popup_dimens.xml").readText()
        assertTrue(source.contains("name=\"telegram_popup_label_gap\">19dp"))
        val layout = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/layout/popup_telegram_menu.xml"))
        val nodes = layout.getElementsByTagName("LinearLayout")
        for (id in rowIds) {
            val row = (0 until nodes.length).map { nodes.item(it) as Element }
                .first { it.getAttribute("android:id") == "@+id/$id" }
            val label = row.getElementsByTagName("TextView").item(0) as Element
            assertEquals("Original source requires 18dp outer inset: $id",
                "18dp", row.getAttribute("android:paddingStart"))
            assertEquals("Original Telegram 43dp title inset with 24dp icon: $id",
                "@dimen/telegram_popup_label_gap",
                label.getAttribute("android:layout_marginStart"))
        }
        assertEquals(61, 18 + 24 + 19)
    }

    @Test fun popupRetainsSevenFocusableActionsAtLeast48dpTall() {
        val layout = File("src/main/res/layout/popup_telegram_menu.xml")
        val dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(layout)
        for (id in rowIds) {
            val nodes = dom.getElementsByTagName("LinearLayout")
            val element = (0 until nodes.length).map { nodes.item(it) as Element }
                .firstOrNull { it.getAttribute("android:id") == "@+id/$id" }
            assertNotNull("Missing menu action $id", element)
            assertEquals("true", element!!.getAttribute("android:clickable"))
            assertEquals("true", element.getAttribute("android:focusable"))
            assertEquals("48dp", element.getAttribute("android:minHeight"))
            assertEquals("196dp", element.getAttribute("android:minWidth"))
            assertEquals("0dp", element.getAttribute("android:paddingTop"))
            assertEquals("0dp", element.getAttribute("android:paddingBottom"))
            assertEquals(1, element.getElementsByTagName("ImageView").length)
            assertEquals(1, element.getElementsByTagName("TextView").length)
        }
    }

    @Test fun originalTelegramRadialPopupMaskIsAppliedWithExactRowCorners() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val selector = File("src/main/java/com/eve/app/ui/home/TelegramPopupRadialSelector.java").readText()
        assertTrue(popup.contains("row.background = TelegramPopupRadialSelector.create("))
        assertTrue(popup.contains("if (index == 0) 6 else 0"))
        assertTrue(popup.contains("if (index == rows.lastIndex) 6 else 0"))
        assertTrue(popup.contains("row.foreground = null"))
        assertTrue(selector.contains("path.addRoundRect(rect, radii, Path.Direction.CW)"))
        assertTrue(selector.contains("new Paint(Paint.ANTI_ALIAS_FLAG)"))
        assertTrue(selector.contains("final int save = canvas.save()"))
        assertTrue(selector.contains("canvas.restoreToCount(save)"))
        assertTrue(selector.contains("3de37c8cfb039452be7398909461da6288babf15"))
        assertTrue(!selector.contains("org.telegram.messenger"))
    }

    @Test fun sourceBackedNinePatchOnlyChangesHomeOverflow() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val glass = File("src/main/java/com/eve/app/designsystem/DesignGlassLayout.kt").readText()
        assertTrue("Normal ActionBarMenuItem uses popup_fixed_alert4, not topView alert2",
            popup.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(!popup.contains("R.drawable.telegram_popup_fixed_alert2"))
        assertTrue(popup.contains("PorterDuff.Mode.MULTIPLY"))
        assertTrue(popup.contains("palette.submenuBackground"))
        assertTrue(popup.contains("binding.root.glassEnabled = false"))
        assertTrue(popup.contains("binding.root.originalPopupBackground = sourceBackground"))
        assertTrue(popup.contains("TelegramPopupSourceBackground(it)"))
        assertTrue(glass.contains("var originalPopupBackground: Drawable? = null"))
        assertTrue(glass.contains("if (source != null) background = source"))
    }

    @Test fun sourcePopupRetainsInputModeAndMenuKeyDismissContract() {
        val source = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val init = source.substringAfter("    init {").substringBefore("        setupThemeCard()")
        assertTrue(init.contains("inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED"))
        assertTrue(init.contains("softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED"))
        assertTrue(init.contains("binding.root.isFocusableInTouchMode = true"))
        assertTrue(init.contains("keyCode == KeyEvent.KEYCODE_MENU"))
        assertTrue(init.contains("event.repeatCount == 0"))
        assertTrue(init.contains("event.action == KeyEvent.ACTION_UP && isShowing"))
        assertTrue(init.contains("dismiss()"))
        assertTrue(source.contains("isOutsideTouchable = true"))
        assertTrue(source.contains("isClippingEnabled = true"))
    }

    @Test fun originalActionBarMenuItemDisablesPlatformPopupAnimationForCustomCascade() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val shown = popup.substringAfter("fun show(anchorView: View)")
            .substringBefore("private fun applyOriginalMenuRoles()")
        assertTrue("Original ActionBarMenuItem.java:841-845 disables duplicate platform animation",
            shown.contains("animationStyle = 0"))
        assertTrue("Custom exit must not trigger an extra platform transition",
            shown.contains("exitTransition = null"))
        assertTrue(shown.contains("TelegramPopupSourceMotion.entrance(binding.root, actionRows())"))
        assertTrue(!shown.contains("ordinaryAnimationStyle") && !shown.contains("ordinaryExitTransition"))
        assertTrue("Theme reveal handoff still dismisses immediately without a second animation",
            popup.contains("ThemeManager.reservePopupHandoff()") &&
                popup.contains("ThemeFrameCoordinator.afterFrame("))
    }

    @Test fun popupUsesOfficialCascadeAndDismissMotionWithoutWholePopupScale() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val motion = File("src/main/java/com/eve/app/ui/home/TelegramPopupSourceMotion.kt").readText()
        assertTrue(popup.contains("TelegramPopupSourceMotion.entrance(binding.root, actionRows())"))
        assertTrue(popup.contains("TelegramPopupSourceMotion.exit(binding.root, actionRows())"))
        assertTrue(popup.contains("TelegramPopupSourceMotion.cancel(binding.root, actionRows())"))
        assertTrue(!popup.contains("TelegramPopupHelper.animateEntrance"))
        assertTrue(!popup.contains("TelegramPopupHelper.animateExit"))
        assertTrue(motion.contains("duration = (150 + 16 * count).toLong()"))
        assertTrue("Telegram ActionBarPopupWindow.java:66,397 uses decelerateInterpolator",
            motion.contains("interpolator = DecelerateInterpolator()"))
        assertTrue("Telegram ActionBarPopupWindow.java:846 keeps root alpha fully opaque",
            motion.contains("root.alpha = 1f"))
        assertTrue("Source-based menu cannot stack an invented secondary root fade",
            !motion.contains("root.alpha = (0.01f") &&
                !motion.contains("root.alpha = 0.01f"))
        assertTrue("9-patch itself controls entrance opacity",
            motion.contains("source.backAlpha = (progress * 255f).toInt().coerceIn(0, 255)"))
        assertTrue(motion.contains("val rise = -6f * root.resources.displayMetrics.density"))
        assertTrue(motion.contains("val at = cascade(progress, position.toFloat(), count.toFloat(), 4f)"))
        assertTrue(motion.contains("duration = 150L"))
        assertTrue(motion.contains("val distance = -5f * root.resources.displayMetrics.density"))
        assertTrue(motion.contains("root.background?.alpha = (progress * 255f).toInt().coerceIn(0, 255)"))
        assertTrue(motion.contains("if (!UiArcMotion.isMotionEnabled(root.context))"))
        assertTrue(!motion.contains("root.scaleY =") && !motion.contains("View.SCALE_Y"))
        assertTrue(!motion.contains("UserConfig"))
    }

    @Test fun officialPopupNinePatchGrowsWithoutScalingEntireRowLayout() {
        val source = File("src/main/java/com/eve/app/designsystem/TelegramPopupSourceBackground.kt").readText()
        val glass = File("src/main/java/com/eve/app/designsystem/DesignGlassLayout.kt").readText()
        val motion = File("src/main/java/com/eve/app/ui/home/TelegramPopupSourceMotion.kt").readText()
        assertTrue(source.contains("fun visibleHeight(totalHeight: Int): Int"))
        assertTrue(source.contains("sourceDrawable.setBounds(box.left, box.top, box.right, box.top + y)"))
        assertTrue(source.contains("sourceDrawable.alpha = backAlpha"))
        assertTrue(glass.contains("canvas.clipRect(0, 0, width, source.visibleHeight(height))"))
        assertTrue(motion.contains("source.backScaleY = progress"))
        assertTrue(motion.contains("source.backAlpha = (progress * 255f).toInt().coerceIn(0, 255)"))
        assertTrue(!source.contains("UserConfig"))
        assertTrue(!motion.contains("root.scaleY =") && !motion.contains("View.SCALE_Y"))
    }

    @Test fun originalTelegramPopupTitlesAreSingleLineEndEllipsizedWithoutAccessibilityLoss() {
        val source = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val roles = source.substringAfter("private fun applyOriginalMenuRoles()")
            .substringBefore("private fun dismissWithAction(")
        assertTrue("Telegram ActionBarMenuSubItem.java:91-98 requires one visual line",
            roles.contains("child.setSingleLine(true)"))
        assertTrue("Telegram original visual menu titles end-ellipsize",
            roles.contains("child.ellipsize = android.text.TextUtils.TruncateAt.END"))
        assertTrue("Full untruncated title remains available to screen readers",
            roles.contains("row.contentDescription = child.text"))
        assertTrue(roles.contains("row.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES"))
    }

    @Test fun allRowsAreReadAsSingleDynamicLabelWithoutDecorativeDuplication() {
        val source = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        val roles = source.substringAfter("private fun applyOriginalMenuRoles()")
            .substringBefore("private fun dismissWithAction(")
        // Rows are now explicitly typed once in actionRows(), reused by animation
        // and accessibility. Verify all seven mappings and the applying loop.
        val mapping = source.substringAfter("private fun actionRows()")
            .substringBefore("    init {")
        for (id in rowIds) assertTrue("Accessibility row mapping missing $id",
            mapping.contains("binding.$id"))
        assertTrue("Accessibility roles must iterate the actual action rows",
            roles.contains("val rows = actionRows()"))
        assertTrue(roles.contains("row.contentDescription = child.text"))
        assertTrue(roles.contains("row.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES"))
        assertTrue(roles.contains("child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO"))
        assertTrue(roles.contains("binding.root.updatePalette(palette)"))
    }

    @Test fun popupUsesSource196dpMinWidthAndDynamicWrapContent() {
        val popup = File("src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt").readText()
        assertTrue("Original ActionBarMenuItem.java:2467 uses 196dp minWidth",
            popup.contains("val minRowWidth = (196f * context.resources.displayMetrics.density).toInt()"))
        assertTrue("Popup width expands from wrap_content with minPopupWidth",
            popup.contains("val targetWidth = maxOf(binding.root.measuredWidth, minPopupWidth).coerceAtMost(maxWidth)"))
        assertTrue(!popup.contains("targetWidth = minOf((250f"))
    }
}

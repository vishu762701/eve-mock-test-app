package com.eve.app.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class GoogleOnlyLoginPresentationTest {
    private val root by lazy {
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/layout/activity_login.xml"))
            .documentElement
    }
    private fun find(id: String): Element {
        val elements = root.getElementsByTagName("*")
        return (0 until elements.length).map { elements.item(it) as Element }
            .firstOrNull { it.getAttribute("android:id") == "@+id/$id" }
            ?: throw AssertionError("Missing login element $id")
    }
    @Test fun googleIsTheOnlyInteractiveSignInAction() {
        for (id in listOf("tilName","tilEmail","tilPassword","layoutBtnSubmit","dividerLayout","btnToggleMode")) {
            assertEquals("$id must stay hidden", "gone", find(id).getAttribute("android:visibility"))
        }
        val google=find("btnGoogle")
        assertEquals("Continue with Google", google.getAttribute("android:text"))
        assertEquals("@style/Widget.Eve.Token.Button", google.getAttribute("style"))
        assertEquals("56dp", google.getAttribute("android:layout_height"))
        assertEquals("4dp", google.getAttribute("android:insetTop"))
        assertEquals("4dp", google.getAttribute("android:insetBottom"))
        assertEquals("com.eve.app.designsystem.TelegramRadialProgressView", find("progressGoogle").tagName)
    }
    @Test fun telegramFirstPageTypographyUsesEveBranding() {
        assertEquals("@style/TextAppearance.Eve.Telegram.LoginTitle",find("tvTagline").getAttribute("style"))
        assertEquals("@style/TextAppearance.Eve.Telegram.LoginSubtitle",find("tvSubtitle").getAttribute("style"))
        assertEquals("center",find("tvTagline").getAttribute("android:gravity"))
        assertEquals("center",find("tvSubtitle").getAttribute("android:gravity"))
    }
    @Test fun firstPageSpacingAndTypefaceFollowPhoneViewSource() {
        assertEquals("0dp", find("tvTagline").getAttribute("android:layout_marginTop"))
        assertEquals("2dp", find("tvTagline").getAttribute("android:lineSpacingExtra"))
        assertEquals("32dp", find("tvTagline").getAttribute("android:layout_marginHorizontal"))
        assertEquals("32dp", find("tvSubtitle").getAttribute("android:layout_marginHorizontal"))
        assertEquals("", find("tvSubtitle").getAttribute("android:layout_marginBottom"))
        assertEquals("30dp", find("cardLogin").getAttribute("android:layout_marginTop"))
        assertEquals("0dp", find("scrollView").getAttribute("android:paddingBottom"))
        val styles = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/values/telegram_typography.xml")).getElementsByTagName("style")
        val title = (0 until styles.length).map { styles.item(it) as Element }
            .single { it.getAttribute("name") == "TextAppearance.Eve.Telegram.LoginTitle" }
        val items = title.getElementsByTagName("item")
        val textStyle = (0 until items.length).map { items.item(it) as Element }
            .single { it.getAttribute("name") == "android:textStyle" }
        assertEquals("Source bold() already supplies Medium; do not synthesize Bold", "normal", textStyle.textContent)
    }
}

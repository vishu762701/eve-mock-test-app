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
        assertEquals("com.eve.app.designsystem.TelegramRadialProgressView", find("progressGoogle").tagName)
    }
    @Test fun telegramFirstPageTypographyUsesEveBranding() {
        assertEquals("@style/TextAppearance.Eve.Telegram.LoginTitle",find("tvTagline").getAttribute("style"))
        assertEquals("@style/TextAppearance.Eve.Telegram.LoginSubtitle",find("tvSubtitle").getAttribute("style"))
        assertEquals("center",find("tvTagline").getAttribute("android:gravity"))
        assertEquals("center",find("tvSubtitle").getAttribute("android:gravity"))
    }
}

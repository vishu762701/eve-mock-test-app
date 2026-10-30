package com.eve.app.designsystem

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** UI-only source contract. Native device parity remains a separate gate. */
class TelegramNativeDialogThemeContractTest {
    @Test fun messagePortUsesOriginalFloatingDialogWindowStyle() {
        val theme = File("src/main/res/values/themes.xml").readText()
            .substringAfter("<style name=\"Theme.Eve.TelegramNativeDialog\"")
            .substringBefore("</style>")
        for (entry in listOf(
            "parent=\"@android:style/Theme.Dialog\"",
            "<item name=\"android:windowNoTitle\">true</item>",
            "<item name=\"android:windowIsFloating\">true</item>",
            "<item name=\"android:windowIsTranslucent\">true</item>",
            "<item name=\"android:windowFullscreen\">false</item>",
            "<item name=\"android:windowBackground\">@android:color/transparent</item>"
        )) assertTrue("Original source style missing $entry", theme.contains(entry))
        val port = File("src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt").readText()
        assertTrue(port.contains("Dialog(context, R.style.Theme_Eve_TelegramNativeDialog)"))
        assertTrue(port.contains("R.drawable.telegram_dialog_popup_fixed_alert4"))
        assertTrue(port.contains("window?.setDimAmount(0.5f)"))
        assertTrue(!port.contains("import org.telegram.messenger.UserConfig"))
        assertTrue(!port.contains("import org.telegram.messenger.ApplicationLoader"))
        // Port only the upstream Activity liveness gate; no Telegram application context.
        assertTrue(port.contains("if (!hostActivityIsAlive()) return"))
        assertTrue(port.contains("!current.isFinishing && !current.isDestroyed"))
    }
}

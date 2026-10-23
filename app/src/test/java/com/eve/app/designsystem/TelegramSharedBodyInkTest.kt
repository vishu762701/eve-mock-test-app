package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Shared semantic color mapping, preserving business-status override styling. */
class TelegramSharedBodyInkTest {
    @Test fun nativeBodyAndSecondaryInkPropagateToSharedStyles() {
        val xml = File("src/main/res/values/design_type.xml").readText()
        for ((styleName, color) in listOf(
            Pair("TextAppearance.Eve.Token.Body", "@color/telegram_type_primary"),
            Pair("TextAppearance.Eve.Token.Secondary", "@color/telegram_type_secondary")
        )) {
            val start = xml.indexOf("<style name=\"" + styleName + "\"")
            assertTrue("Missing shared style " + styleName, start >= 0)
            val end = xml.indexOf("</style>", start)
            val body = xml.substring(start, end)
            assertTrue("Missing Telegram ink for " + styleName,
                body.contains("<item name=\"android:textColor\">" + color + "</item>"))
            assertTrue("Roboto regular unexpectedly replaced in " + styleName,
                body.contains("<item name=\"android:fontFamily\">sans-serif</item>"))
        }
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
}

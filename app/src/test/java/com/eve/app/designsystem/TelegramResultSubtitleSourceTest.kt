package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramResultSubtitleSourceTest {
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
}

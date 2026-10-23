package com.eve.app.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TelegramBookmarkIconStateTest {
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
}

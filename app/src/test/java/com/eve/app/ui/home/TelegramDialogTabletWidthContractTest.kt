package com.eve.app.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Exact upstream window-width role, not full renderer parity. */
class TelegramDialogTabletWidthContractTest {
    @Test fun originalPhoneSmallTabletAndLargeTabletWidthsRemainExplicit() {
        val s = File("src/main/java/com/eve/app/ui/home/TelegramDialogSourceBackground.kt").readText()
        assertTrue(s.contains("minSideDp <= 690f -> 446"))
        assertTrue(s.contains("else -> 496"))
        assertTrue(s.contains("!isTablet -> 356"))
        assertTrue(s.contains("dm.widthPixels - (56 * dm.density).toInt()"))
        assertTrue(s.contains("backgroundPaddings.left + backgroundPaddings.right"))
        assertTrue(s.contains("remains ADAPTED"))
    }
}

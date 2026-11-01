package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source parameters and live popup wiring. Real capture exercised by native popup suite. */
class ProfilePopupBlurContractTest {
    @Test fun originalTelegramScrimAndMenuProviderParametersStayPinned() {
        assertEquals(15, ProfilePopupBlurBackdrop.SOURCE_DOWNSCALE)
        assertEquals(15, ProfilePopupBlurBackdrop.SOURCE_BLUR_RADIUS)
        assertEquals(12f, ProfilePopupBlurBackdrop.SOURCE_RADIUS_DP)
        assertEquals(8f, ProfilePopupBlurBackdrop.SOURCE_PADDING_DP)
        assertEquals(.825f, ProfilePopupBlurBackdrop.LIGHT_ALPHA)
        assertEquals(.85f, ProfilePopupBlurBackdrop.DARK_ALPHA)
    }

    @Test fun popupUsesRealBackgroundAndReleasesTransientCapturedProfileBitmap() {
        val popup = File("src/main/java/com/eve/app/ui/profile/ProfileOverflowPopup.kt").readText()
        assertTrue(popup.contains("ProfilePopupBlurBackdrop.capture(anchor, layout, palette)"))
        assertTrue(popup.contains("layout.setBackgroundDrawable(blurBackdrop)"))
        assertTrue(popup.contains("blurBackdrop?.release()"))
        val source = File("src/main/java/com/eve/app/ui/profile/ProfilePopupBlurBackdrop.kt").readText()
        assertTrue(source.contains("scene.draw(this)"))
        assertTrue(source.contains("ProfileStackBlur.blurBitmap(raw"))
        assertTrue(source.contains("snapshot?.recycle()"))
        assertTrue(source.contains("setSaturation(if (palette.isDark) 2f else 3f)"))
    }
}

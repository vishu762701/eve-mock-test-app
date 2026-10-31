package com.eve.app.ui.profile

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Source property contract; the platform motion path is exercised on an emulator
 * only by AndroidTest, not by FAST unit-test compilation.
 */
class ProfileActionBounceSourceTest {
    @Test fun telegramTimingCurvesAndCancellationRemainMapped() {
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileActionBounce.kt").readText()
        for (key in listOf(
            "PRESS_DURATION_MS = 60L",
            "RELEASE_DURATION_MS = 350L",
            "MAX_DRIFT_PX = 20f",
            "LONG_PRESS_HAPTIC_MS = 250L",
            "SCALE_DIFF = 0.04f",
            "OVERSHOOT = 5f",
            "PathInterpolator(0.25f, 0.1f, 0.25f, 1f)",
            "OvershootInterpolator(OVERSHOOT)",
            "animator?.cancel()",
            "v.performClick()",
            "MotionEvent.ACTION_CANCEL",
            "MotionEvent.ACTION_POINTER_DOWN",
            "onViewDetachedFromWindow"
        )) {
            assertTrue("Missing verified source property $key", source.contains(key))
        }
    }

    @Test fun eachExistingProfileActionUsesBounceWithoutChangingClickDestinations() {
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val setup = source.substringAfter("private fun setupProfileActionBounce()")
            .substringBefore("private fun setupCells()")
        for (id in listOf("btnSetPhoto", "btnEditInfo", "btnSettings")) {
            assertTrue("$id must receive Telegram ButtonBounce", setup.contains("ProfileActionBounce.install(binding.$id)"))
        }
        val onCreate = source.substringAfter("override fun onCreate(").substringBefore("private fun setupAvatarWindowHost()")
        assertTrue(onCreate.indexOf("setupActions()") < onCreate.indexOf("setupProfileActionBounce()"))
        val clickRegion = source.substringAfter("private fun setupActions()").substringBefore("private fun setupProfileActionBounce()")
        assertTrue(clickRegion.contains("binding.btnSetPhoto.setOnClickListener"))
        assertTrue(clickRegion.contains("binding.btnEditInfo.setOnClickListener"))
        assertTrue(clickRegion.contains("binding.btnSettings.setOnClickListener"))
        assertTrue(clickRegion.contains("pickPhotoLauncher.launch"))
        assertTrue(clickRegion.contains("SettingsActivity::class.java"))
        assertTrue(clickRegion.contains("ChangeNameActivity::class.java"))
    }
}

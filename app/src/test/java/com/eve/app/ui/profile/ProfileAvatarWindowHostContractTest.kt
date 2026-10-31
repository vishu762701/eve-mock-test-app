package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileAvatarWindowHostContractTest {
    @Test fun fullWindowGooeyIsBehindScrollableGestureTarget() {
        val xml = File("src/main/res/layout/activity_profile.xml").readText()
        val renderer = xml.indexOf("<com.eve.app.ui.profile.ProfileGooeyView")
        val scroll = xml.indexOf("<com.eve.app.ui.profile.ProfilePullScrollView")
        val toolbar = xml.indexOf("<com.eve.app.ui.common.EveGlassToolbar")
        assertTrue(renderer > 0 && scroll > renderer && toolbar > renderer)
        val overlay = xml.substring(renderer, scroll)
        assertTrue(overlay.contains("android:id=\"@+id/avatarGooey\""))
        assertTrue(overlay.contains("android:layout_height=\"match_parent\""))
        assertTrue(overlay.contains("android:importantForAccessibility=\"noHideDescendants\""))
        assertTrue(overlay.contains("android:id=\"@+id/avatarContainer\""))
        val placeholder = xml.substringAfter("android:id=\"@+id/avatarOuterContainer\"")
            .substringBefore("<!-- Photo actions")
        assertTrue(placeholder.contains("android:id=\"@+id/avatarTouchTarget\""))
        assertTrue(placeholder.contains("android:clickable=\"true\""))
        assertTrue(placeholder.contains("android:focusable=\"true\""))
        assertFalse(placeholder.contains("<com.eve.app.ui.profile.ProfileGooeyView"))
    }

    @Test fun sourceCameraTargetMotionIsWiredWithoutBreakingPhotoPicker() {
        val code = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        assertTrue(code.contains("binding.avatarTouchTarget.setOnClickListener"))
        assertTrue(code.contains("pickPhotoLauncher.launch(\"image/*\")"))
        assertTrue(code.contains("ProfileAvatarTrajectory.offsetToNotch("))
        assertTrue(code.contains("binding.avatarGooey.notchInfo"))
        assertTrue(code.contains("contentRoot?.clipToPadding = false"))
        assertTrue(code.contains("WindowInsetsCompat.Type.displayCutout()"))
    }

    @Test fun interpolationStartsAtRealAvatarCenterAndEndsAtNotchCenter() {
        assertEquals(0f, ProfileAvatarTrajectory.offsetToNotch(120f, 16f, 0f), 0.0001f)
        assertEquals(-52f, ProfileAvatarTrajectory.offsetToNotch(120f, 16f, 0.5f), 0.0001f)
        assertEquals(-104f, ProfileAvatarTrajectory.offsetToNotch(120f, 16f, 1f), 0.0001f)
    }
}

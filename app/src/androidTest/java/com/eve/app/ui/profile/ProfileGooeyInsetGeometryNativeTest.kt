package com.eve.app.ui.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileGooeyInsetGeometryNativeTest {
    @Test fun smallCircularCutoutUsesVerifiedTelegramScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = ProfileGooeyView(context)
        val d = context.resources.displayMetrics.density
        view.notchInfo = NotchInfoUtils.NotchInfo().apply {
            isLikelyCircle = true
            bounds.set(0f, 0f, 12f * d, 12f * d)
        }
        assertEquals(0.1f, view.getAvatarEndScale(), 0.001f)
        // ProfileGooeyView end scale controls mask paint; avatar motion uses
        // the separate ProfileActivity.java:8735-8740 half-notch formula.
        assertEquals(0.06f,
            ProfileAvatarTrajectory.sourceAvatarScale(1f, 12f * d, d), 0.001f)
        assertEquals(0.96f,
            ProfileAvatarTrajectory.sourceAvatarScale(0f, 12f * d, d), 0.001f)
    }

    @Test fun noCutoutOffsetIncludesRealPlatformStatusHeight() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = ProfileGooeyView(context)
        view.notchInfo = null
        val d = context.resources.displayMetrics.density
        assertEquals(-(16f + 100f) * d, view.getEndOffset(false, 0.8f), 1f)
        assertTrue(view.getEndOffset(true, 0.8f) < view.getEndOffset(false, 0.8f))
    }
}

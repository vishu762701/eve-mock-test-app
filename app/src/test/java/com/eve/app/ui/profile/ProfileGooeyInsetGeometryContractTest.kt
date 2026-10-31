package com.eve.app.ui.profile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileGooeyInsetGeometryContractTest {
    @Test fun noNotchOffsetUsesSystemStatusInsetInsteadOfFixed24dp() {
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileGooeyView.kt").readText()
        val offset = source.substringAfter("fun getEndOffset(").substringBefore("fun getAvatarEndScale(")
        assertTrue(offset.contains("statusBarHeightPx()"))
        assertFalse(offset.contains("if (occupyStatusBar) dp(24)"))
        val measurement = source.substringAfter("private fun statusBarHeightPx()")
            .substringBefore("private val blackPaint")
        assertTrue(measurement.contains("ViewCompat.getRootWindowInsets(this)"))
        assertTrue(measurement.contains("WindowInsetsCompat.Type.statusBars()"))
        assertTrue(measurement.contains("getDimensionPixelSize(id)"))
    }

    @Test fun smallCutoutsHaveNoForeignPointTwoFloor() {
        val source = File("src/main/java/com/eve/app/ui/profile/ProfileGooeyView.kt").readText()
        val scale = source.substringAfter("fun getAvatarEndScale(").substringBefore("fun hasNotchInfo(")
        assertTrue(scale.contains("min(0.8f, f)"))
        assertFalse(scale.contains("max(0.2f, f)"))
    }
}

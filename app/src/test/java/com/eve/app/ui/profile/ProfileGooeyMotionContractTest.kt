package com.eve.app.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileGooeyMotionContractTest {
    @Test fun originalTelegramLiquidBridgeRisesAndFallsInsteadOfStickingAtMaximum() {
        val peak = 21f
        assertEquals(0f, ProfileGooeyMotionMath.liquidBridgeHeight(0f, peak), 0.0001f)
        assertEquals(10.5f, ProfileGooeyMotionMath.liquidBridgeHeight(0.25f, peak), 0.0001f)
        assertEquals(21f, ProfileGooeyMotionMath.liquidBridgeHeight(0.5f, peak), 0.0001f)
        assertEquals(10.5f, ProfileGooeyMotionMath.liquidBridgeHeight(0.75f, peak), 0.0001f)
        assertEquals(0f, ProfileGooeyMotionMath.liquidBridgeHeight(1f, peak), 0.0001f)
    }

    @Test fun upstreamAvatarToNotchFadeRawAlphaIsReproduced() {
        assertEquals(510, ProfileGooeyMotionMath.imageAlphaUnclamped(0f))
        assertEquals(255, ProfileGooeyMotionMath.imageAlphaUnclamped(0.5f))
        assertEquals(0, ProfileGooeyMotionMath.imageAlphaUnclamped(1f))
    }

    @Test fun nativeGpuBranchIncludesRequiredNotchMaskAndFadeStages() {
        val code = File("src/main/java/com/eve/app/ui/profile/ProfileGooeyView.kt").readText()
        val gpu = code.substringAfter("private inner class GPUImpl")
        assertTrue(gpu.contains("ProfileGooeyMotionMath.liquidBridgeHeight("))
        assertTrue(gpu.contains("c.saveLayer(wholeOptimized, null)"))
        assertTrue(gpu.contains("c.saveLayerAlpha(wholeOptimized, imageAlpha)"))
        assertTrue(gpu.contains("if (info.isLikelyCircle)"))
        assertTrue(gpu.contains("else if (info.isAccurate)"))
        assertTrue(gpu.contains("c.drawRoundRect(temp, rad, rad, blackPaint)"))
        assertTrue(gpu.contains("c.drawRect(0f, 0f, rw.toFloat(), dp(BLACK_KING_BAR), blackPaint)"))
        assertTrue(gpu.contains("canvas.saveLayer(wholeOptimized, filter)"))
        assertTrue(gpu.contains("canvas.drawRenderNode(effectNotchNode)"))
        assertTrue(gpu.contains("canvas.drawRenderNode(effectNode)"))
        assertTrue(gpu.contains("canvas.drawRenderNode(blurNode)"))
        assertTrue(gpu.contains("canvas.drawRect(wholeOptimized, blackNodePaint)"))
        assertTrue(gpu.contains("canvas.saveLayerAlpha(wholeOptimized, blurImageAlpha)"))
        assertFalse(gpu.contains("MathUtils.clamp(pullProgress / 0.5f"))
    }
}

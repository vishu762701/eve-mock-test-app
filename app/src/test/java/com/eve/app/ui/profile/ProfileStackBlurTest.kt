package com.eve.app.ui.profile

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProfileStackBlurTest {
    @Test fun radiusOneUsesTelegramTriangularWeightsAndReplicatedEdges() {
        val pixels = intArrayOf(0xff000000.toInt(), 0xff808080.toInt(), 0xffffffff.toInt())
        ProfileStackBlur.blurArgb(pixels, 3, 1, 1)
        assertArrayEquals(
            intArrayOf(0xff202020.toInt(), 0xff7f7f7f.toInt(), 0xffdfdfdf.toInt()),
            pixels
        )
    }

    @Test fun constantColorIsUnchangedEvenAtLargerRadius() {
        val pixels = IntArray(16) { 0xff4488cc.toInt() }
        ProfileStackBlur.blurArgb(pixels, 4, 4, 3)
        assertArrayEquals(IntArray(16) { 0xff4488cc.toInt() }, pixels)
    }

    @Test fun allFourRgbaChannelsAreBlurredAndZeroRadiusIsNoOp() {
        val pixels = intArrayOf(0x00000000, 0xff000000.toInt(), 0x00000000)
        ProfileStackBlur.blurArgb(pixels, 3, 1, 1)
        assertArrayEquals(
            intArrayOf(0x3f000000, 0x7f000000, 0x3f000000), pixels
        )
        val unchanged = pixels.copyOf()
        ProfileStackBlur.blurArgb(pixels, 3, 1, 0)
        assertArrayEquals(unchanged, pixels)
    }

    @Test fun cpuGooeyActuallyUsesSoftwareMaskAndBlurStages() {
        val file = File("src/main/java/com/eve/app/ui/profile/ProfileGooeyView.kt").readText()
        val cpu = file.substringAfter("private inner class CPUImpl")
            .substringBefore("private inner class GPUImpl")
        assertTrue(cpu.contains("buffer.eraseColor(0)"))
        assertTrue(cpu.contains("offscreen.drawCircle("))
        assertTrue(cpu.contains("offscreen.drawPath(info.path, blackPaint)"))
        assertTrue(cpu.contains("offscreen.drawRoundRect("))
        assertTrue(cpu.contains("ProfileStackBlur.blurBitmap("))
        assertTrue(cpu.contains("canvas.drawBitmap(buffer, 0f, 0f, bitmapPaint)"))
        assertTrue(cpu.contains("canvas.drawBitmap(buffer, 0f, 0f, bitmapPaint2)"))
        assertTrue(cpu.contains("canvas.saveLayerAlpha("))
    }
}

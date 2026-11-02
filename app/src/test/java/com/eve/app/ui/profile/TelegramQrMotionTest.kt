package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test

/** Verified constants trace to Telegram QR 13.1.1; EVE host remains ADAPTED. */
class TelegramQrMotionTest {
    @Test fun revealHasStableEndpointsAndMidpoint() {
        assertEquals(0f, TelegramQrMotion.reveal(0f, true), .0001f)
        assertEquals(1f, TelegramQrMotion.reveal(1f, true), .0001f)
        assertEquals(0f, TelegramQrMotion.reveal(0f, false), .0001f)
        assertEquals(1f, TelegramQrMotion.reveal(1f, false), .0001f)
        assertTrue(TelegramQrMotion.reveal(.5f, true) in 0f..1.1f)
    }
    @Test fun originalCardBounceAndPulseSettleAndRespectReducedMotion() {
        val motion = com.eve.app.telegram.qr.QrCardMotion()
        motion.recolor(1000, true)
        assertEquals(1f, motion.scale(1000, true), .00001f)
        assertEquals(1.03f, motion.scale(1080, true), .00001f)
        assertEquals(.995f, motion.scale(1192, true), .00001f)
        motion.recolor(1080, true)
        assertEquals(1.03f, motion.scale(1080, true), .00001f)
        assertEquals(1f, motion.scale(1400, true), .00001f)
        assertFalse(motion.isAnimating(1400))
        motion.changeEmojis(2000, true)
        assertEquals(.9745f, motion.scale(2180, true), .00001f)
        assertEquals(1f, motion.scale(2360, true), .00001f)
        motion.recolor(3000, true)
        assertEquals(1f, motion.scale(3080, false), .00001f)
        assertFalse(motion.isAnimating(3080))
    }

    @Test fun sourceIntervalsAndGestureBoundsRemainStable() {
        assertEquals(860L, TelegramQrMotion.OPEN_MS)
        assertEquals(200L, TelegramQrMotion.CLOSE_MS)
        assertEquals(380L, TelegramQrMotion.QR_BUILD_MS)
        assertEquals(600L, TelegramQrMotion.WAVE_MS)
        assertEquals(0f, TelegramQrMotion.rubber(0f, 300f), .00001f)
        assertTrue(TelegramQrMotion.rubber(1000f, 300f) < 300f)
        assertTrue(TelegramQrMotion.glide(1000f, 1f) > 0f)
    }
}

package com.eve.app.ui.profile

import org.junit.Assert.*
import org.junit.Test

class QrEmojiPatternTest {
    @Test fun originalSixSelectionToggleDropsOldestAndRemovesExisting() {
        val picks = (1..6).map { QrEmojiPattern.custom(it.toString(), "✨", "res:raw/premium_star") }
        val seventh = QrEmojiPattern.custom("7", "🔥", "res:raw/premium_star")
        val chosen = QrEmojiPattern.toggle(picks, seventh)
        assertEquals(6, chosen.size)
        assertEquals(picks.drop(1) + seventh, chosen)
        assertEquals(picks.drop(1), QrEmojiPattern.toggle(chosen, seventh))
    }

    @Test fun standardEmojiVariationSelectorNormalizesKey() {
        assertEquals(QrEmojiPattern.standard("❤️").key, QrEmojiPattern.standard("❤").key)
    }

    @Test fun originalLatticeConstantsAreStableAndSeedsDeterministic() {
        assertEquals(86f, QrEmojiPattern.COLUMN_DP, 0f)
        assertEquals(70f, QrEmojiPattern.ROW_DP, 0f)
        assertEquals(380f, QrEmojiPattern.APPEAR_MS, 0f)
        assertEquals(300f, QrEmojiPattern.SPREAD_MS, 0f)
        val h = QrEmojiPattern.hash(-5, 12)
        assertTrue(h >= 0 && h < 1)
        assertEquals(h, QrEmojiPattern.hash(-5, 12), 0.0)
        assertEquals(0f, QrEmojiPattern.appear(0f, h), .00001f)
        assertEquals(1f, QrEmojiPattern.appear(2000f, h), .00001f)
        assertTrue(QrEmojiPattern.backOut(.75f) > 1f)
    }
}

package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import org.junit.Test;

/** Source Java old/new emoji-slot compositor on production math, no Android services. */
public class QrPatternSwapTest {
    @Test public void sourceShrinkAndDelayedPopFollowOriginalCurve() {
        assertEquals(1f, QrPatternSwap.slotScale(-.01f, true, false), 0f);
        assertEquals(.5f, QrPatternSwap.slotScale(.09f, true, false), 0.00001f);
        assertEquals(0f, QrPatternSwap.slotScale(.18f, true, false), 0f);
        assertEquals(0f, QrPatternSwap.slotScale(.13f, false, false), 0f);
        assertTrue(QrPatternSwap.slotScale(.135f + .23f, false, false) > 1f);
        assertEquals(1f, QrPatternSwap.slotScale(.5f, false, false), 0f);
    }

    @Test public void transitionFromEmptyDoesNotWaitForOldSlot() {
        assertEquals(0f, QrPatternSwap.slotScale(0f, false, true), 0f);
        assertTrue(QrPatternSwap.slotScale(.16f, false, true) > 0f);
        assertEquals(1f, QrPatternSwap.slotScale(.315f, false, true), 0f);
    }

    @Test public void negativeOddRowsUseTheActualSourceSlotMapping() {
        assertEquals(2, QrPatternSwap.slotIndex(-1, 0, 6));
        assertEquals(0, QrPatternSwap.slotIndex(-3, 1, 6));
        assertEquals(0, QrPatternSwap.slotIndex(0, 0, 6));
        assertEquals(-1, QrPatternSwap.slotIndex(5, 3, 0));
        assertEquals(0, QrPatternSwap.slotIndex(-999, 1000, 1));
    }

    @Test public void staggerAndExpiryAreBounded() {
        for (int r = -30; r <= 30; r++) {
            for (int c = -30; c <= 30; c++) {
                double h = latticeHash(r, c);
                float d = QrPatternSwap.delaySeconds(h);
                assertTrue(d >= 0f && d < QrPatternSwap.SWAP_SPREAD);
            }
        }
        assertTrue(QrPatternSwap.isActive(0, true));
        assertTrue(QrPatternSwap.isActive(QrPatternSwap.END_MS - 1, true));
        assertFalse(QrPatternSwap.isActive(QrPatternSwap.END_MS, true));
        assertFalse(QrPatternSwap.isActive(0, false));
    }

    private static double latticeHash(int row, int col) {
        long h = ((long) row) * 0x9E3779B97F4A7C15L ^
                 ((long) col) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (double) (h >>> 11) / (double) (1L << 53);
    }
}

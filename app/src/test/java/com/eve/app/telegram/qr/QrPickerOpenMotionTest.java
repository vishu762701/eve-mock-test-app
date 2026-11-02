package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import org.junit.Test;

/** Math from the production Java source adapter, no Android runtime required. */
public class QrPickerOpenMotionTest {
    @Test public void sourceGridOpensFromThreeQuartersOver420Ms() {
        assertEquals(420L, QrPickerOpenMotion.REVEAL_MS);
        assertEquals(.75f, QrPickerOpenMotion.scale(0f), 0f);
        assertEquals(.96875f, QrPickerOpenMotion.scale(.5f), .000001f);
        assertEquals(1f, QrPickerOpenMotion.scale(1f), 0f);
    }

    @Test public void openingMotionClampsMalformedProgressAndIsMonotonic() {
        assertEquals(.75f, QrPickerOpenMotion.scale(-3f), 0f);
        assertEquals(1f, QrPickerOpenMotion.scale(5f), 0f);
        float previous = 0f;
        for (int i = 0; i <= 100; i++) {
            float next = QrPickerOpenMotion.scale(i / 100f);
            assertTrue(next >= previous);
            previous = next;
        }
    }

    @Test public void sourceSheetFadeReachesOpacityAt55Percent() {
        assertEquals(0f, QrPickerOpenMotion.alpha(-1f), 0f);
        assertEquals(.5f, QrPickerOpenMotion.alpha(.275f), .000001f);
        assertEquals(1f, QrPickerOpenMotion.alpha(.55f), 0f);
        assertEquals(1f, QrPickerOpenMotion.alpha(3f), 0f);
    }
}

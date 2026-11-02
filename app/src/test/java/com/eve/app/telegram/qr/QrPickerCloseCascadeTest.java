package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import org.junit.Test;
public class QrPickerCloseCascadeTest {
    @Test public void sourceTopFirstRowsSettleAtSixTenths() {
        assertEquals(1f, QrPickerCloseCascade.cellScale(0, .5f, 6), 0f);
        float top = QrPickerCloseCascade.cellScale(60, .5f, 6);
        float bottom = QrPickerCloseCascade.cellScale(60, 5.5f, 6);
        assertTrue(top < bottom);
        assertEquals(1f, bottom, 0f);
        assertEquals(.6f, QrPickerCloseCascade.cellScale(500, .5f, 6), 0f);
    }
    @Test public void timeAndRowsRemainBounded() {
        for (int rows=0; rows<20; ++rows) {
            for (int t=-45; t<=240; t+=15) {
                float x = QrPickerCloseCascade.cellScale(t, 3.5f, rows);
                assertTrue(x >= .6f && x <= 1f);
            }
        }
    }
}

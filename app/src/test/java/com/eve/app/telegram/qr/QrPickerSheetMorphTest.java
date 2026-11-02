package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import org.junit.Test;

public class QrPickerSheetMorphTest {
    @Test public void sourceDurationCubicAndContentAlphaAreStable() {
        assertEquals(420L, QrPickerSheetMorph.OPEN_MS);
        assertEquals(360L, QrPickerSheetMorph.CLOSE_MS);
        assertEquals(0f, QrPickerSheetMorph.sourceEase(0f), .001f);
        assertEquals(1f, QrPickerSheetMorph.sourceEase(1f), .001f);
        assertTrue(QrPickerSheetMorph.sourceEase(.25f) > .6f);
        assertEquals(1f, QrPickerSheetMorph.openContent(.8f), 0f);
        assertEquals(0f, QrPickerSheetMorph.closeContent(170), 0f);
        assertEquals(1f, QrPickerSheetMorph.closeContent(0), 0f);
    }
    @Test public void sourceIslandAndFloatingGeometryKeepScreenInsets() {
        QrPickerSheetMorph m = new QrPickerSheetMorph(2f);
        m.sample(400, 700, 0f);
        assertEquals(56f, m.left, 0f);
        assertEquals(344f, m.right, 0f);
        assertEquals(600f, m.top, 0f);
        assertEquals(680f, m.bottom, 0f);
        assertEquals(40f, m.radius, 0f);
        m.sample(400, 700, 1f);
        assertEquals(20f, m.left, 0f);
        assertEquals(380f, m.right, 0f);
        assertEquals(20f, m.top, 0f);
        assertEquals(680f, m.bottom, 0f);
        assertEquals(56.34f, m.radius, .001f);
        assertEquals(1f, m.contentAlpha, 0f);
    }
    @Test public void geometryAndEaseClampAcrossInterruptedOrBadProgress() {
        QrPickerSheetMorph m = new QrPickerSheetMorph(1f);
        m.sample(360, 500, -1f);
        assertEquals(108f, m.left, 0f);
        m.sample(360, 500, 5f);
        assertEquals(10f, m.left, 0f);
        float last = 0f;
        for (int i = 0; i <= 100; i++) {
            float next = QrPickerSheetMorph.sourceEase(i/100f);
            assertTrue(next >= last);
            last = next;
        }
    }
}

package com.eve.app.telegram.qr;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = android.app.Application.class)
public class QrCardLayoutTest {
    @Test public void portraitPreservesSourceDimensionsAndRestPosition() {
        QrCardLayout card = new QrCardLayout();
        assertTrue(card.sample(400, 900, 1, 24, 76, 820, 0, 600));
        assertEquals(298f, card.bounds.width(), .001f);
        assertEquals(298f * 1.07f, card.bounds.height(), .001f);
        assertEquals(900f * .468f - card.bounds.height()/2 + 1, card.bounds.top, .001f);
        assertEquals(1f, card.fitScale, 0f);
    }
    @Test public void sheetRisesFromRestAndNeverPushesCardDown() {
        QrCardLayout card = new QrCardLayout();
        card.sample(400, 900, 1, 24, 76, 820, 0, 430);
        float rest = card.bounds.top;
        card.sample(400, 900, 1, 24, 76, 820, 1, 430);
        float open = card.bounds.top;
        assertEquals(430 - 13 - card.bounds.height(), open, .001f);
        card.sample(400, 900, 1, 24, 76, 820, .5f, 430);
        assertEquals((open + rest)/2, card.bounds.top, .001f);
        card.sample(400, 900, 1, 24, 76, 820, 1, 890);
        assertEquals(rest, card.bounds.top, .001f);
    }
    @Test public void compactWindowsFitCompleteCardInsideMeasuredControls() {
        for (int width : new int[]{320, 780, 1200}) {
            QrCardLayout card = new QrCardLayout();
            assertTrue(card.sample(width, 360, 2, 24, 80, 270, 1, 200));
            assertTrue(card.bounds.top >= 80);
            assertTrue(card.bounds.bottom <= 270.001f);
            assertEquals(card.sourceWidth * card.fitScale, card.bounds.width(), .001f);
            assertEquals(card.sourceHeight * card.fitScale, card.bounds.height(), .001f);
        }
    }
    @Test public void invalidViewportCannotLeaveAVisibleStaleCard() {
        QrCardLayout card = new QrCardLayout();
        assertTrue(card.sample(400, 900, 1, 0, 0, 900, 0, 600));
        assertFalse(card.sample(400, 900, 1, 0, 800, 700, 0, 600));
        assertTrue(card.bounds.isEmpty());
        assertFalse(card.sample(400, 900, Float.NaN, 0, 0, 900, 0, 600));
    }
}

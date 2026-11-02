package com.eve.app.telegram.qr;

import static org.junit.Assert.*;
import android.app.Application;
import android.graphics.RectF;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Source overlay to screen to scaled-grid geometry, exercised by production dispatchDraw. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class QrEmojiGridCutTest {
    @Test public void islandMaskAlignsToOverlayDuringThreeQuartersOpeningScale() {
        RectF island = new RectF(10f, 20f, 70f, 60f);
        float[] radiusAlpha = {20f, .7f};
        QrEmojiGrid.mapIslandCut(island, radiusAlpha,
            new int[]{100, 200}, new int[]{80, 150}, .75f, .75f);
        assertEquals(40f, island.left, .001f);
        assertEquals(120f, island.right, .001f);
        assertEquals(93.3333f, island.top, .001f);
        assertEquals(146.6667f, island.bottom, .001f);
        assertEquals(26.6667f, radiusAlpha[0], .001f);
        assertEquals(.7f, radiusAlpha[1], 0f);
    }

    @Test public void settledGridPreservesIdentityGeometry() {
        RectF island = new RectF(15f, 22f, 100f, 68f);
        float[] radiusAlpha = {23f, 1f};
        QrEmojiGrid.mapIslandCut(island, radiusAlpha,
            new int[]{10, 20}, new int[]{10, 20}, 1f, 1f);
        assertEquals(15f, island.left, 0f);
        assertEquals(22f, island.top, 0f);
        assertEquals(100f, island.right, 0f);
        assertEquals(68f, island.bottom, 0f);
        assertEquals(23f, radiusAlpha[0], 0f);
    }

    @Test public void smallTransientScaleUsesSourceSafeDenominator() {
        RectF island = new RectF(5f, 10f, 35f, 40f);
        float[] radiusAlpha = {15f, 1f};
        QrEmojiGrid.mapIslandCut(island, radiusAlpha,
            new int[]{0, 0}, new int[]{0, 0}, 0f, 0f);
        assertEquals(5f, island.left, 0f);
        assertEquals(10f, island.top, 0f);
        assertEquals(15f, radiusAlpha[0], 0f);
    }
}

package com.eve.app.telegram.qr;
import static org.junit.Assert.*;
import org.junit.Test;

public class QrPatternFieldTest {
    @Test public void wavePushesOncePerGenerationAndSettles() {
        QrPatternField f = new QrPatternField(1f);
        f.advance(1000, true);
        f.beginWave(180, 320, 1000);
        f.advance(1200, true);
        assertTrue(f.place(210, 340, 36, 360, 720, 337, 0, 1, 2, 1f, true));
        assertEquals(1, f.nodes.n);
        int i = f.nodes.list[0];
        float vx = f.nodes.vx[i];
        assertTrue(f.place(210, 340, 36, 360, 720, 337, 0, 1, 2, 1f, true));
        assertEquals(vx, f.nodes.vx[i], 0f);
        f.advance(1233, true);
        assertTrue(f.place(210, 340, 36, 360, 720, 337, 0, 1, 2, 1f, true));
        assertTrue(f.rotation != 0f || f.pulse != 0f);
        f.clear();
        assertEquals(0, f.nodes.n);
    }
    @Test public void disabledMotionHasStableGeometryAndEdgeFade() {
        QrPatternField f = new QrPatternField(1f);
        f.advance(1000, false);
        assertTrue(f.place(45, 110, 28, 400, 800, 300, 0, 1, 1, 1f, false));
        float x = f.x, y = f.y, a = f.alpha;
        assertTrue(x > 0 && x < 90 && a > 0 && a < 1);
        assertTrue(f.place(45, 110, 28, 400, 800, 300, 0, 1, 1, 1f, false));
        assertEquals(x, f.x, 0f);
        assertEquals(y, f.y, 0f);
        assertFalse(f.place(-10000, 110, 28, 400, 800, 300, 0, 1, 1, 1f, false));
        assertFalse(f.place(Float.NaN, 110, 28, 400, 800, 300, 0, 1, 1, 1f, false));
    }
    @Test public void draggingRespectsTouchSlopAndInterruptsGlide() {
        QrPatternField f = new QrPatternField(2f);
        f.advance(1000, true);
        f.onDown(20, 20);
        f.onMove(20, 30, 400, 800);
        assertFalse(f.wasDragged());
        f.onMove(80, 60, 400, 800);
        assertTrue(f.wasDragged());
        assertEquals(60f, f.offsetX(), 0f);
        f.onUp(100000, 100000, 1010);
        f.advance(1043, true);
        assertTrue(f.offsetX() > 60f);
        f.onDown(80, 60);
        float intercepted = f.offsetX();
        f.onMove(80, 60, 400, 800);
        assertEquals(intercepted, f.offsetX(), 0f);
        f.onCancel(1050);
    }
    @Test public void boundedSpringStorageCanBeCleared() {
        QrPatternField.NodeSprings n = new QrPatternField.NodeSprings();
        for(int i = 0; i < 3072; i++) assertTrue(n.obtain(i) >= 0);
        assertEquals(-1, n.obtain(3072));
        n.clear();
        assertEquals(0, n.n);
        assertTrue(n.obtain(3072) >= 0);
    }
}

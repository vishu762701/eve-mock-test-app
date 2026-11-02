package com.eve.app.telegram.qr;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class QrSceneShineTest {
    @Test public void introDelayDrawsNoPrematureLightAndRealBandUsesSourceRaster() {
        QrSceneShine light = new QrSceneShine();
        Bitmap image = Bitmap.createBitmap(240, 480, Bitmap.Config.ARGB_8888);
        try {
            Canvas c = new Canvas(image);
            image.eraseColor(Color.BLACK);
            light.step(1000L, true, 240, 480);
            assertFalse(light.draw(c, 240, 480, true));
            light.step(3500L, true, 240, 480);
            assertTrue("source 12dp bitmap/light band should draw", light.draw(c, 240, 480, true));
            int changed = 0;
            for (int y = 0; y < 480; y += 12)
                for (int x = 0; x < 240; x += 12)
                    if (image.getPixel(x, y) != Color.BLACK) changed++;
            assertTrue("light must modify actual pixels", changed > 0);
        } finally {
            light.close();
            image.recycle();
        }
    }

    @Test public void reducedMotionRevealAndCloseProduceNoLight() {
        QrSceneShine shine = new QrSceneShine();
        Bitmap bitmap = Bitmap.createBitmap(180, 320, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            shine.step(1000, true, 180, 320);
            shine.step(3500, true, 180, 320);
            assertFalse(shine.draw(canvas, 180, 320, false));
            shine.step(3550, false, 180, 320);
            assertFalse(shine.draw(canvas, 180, 320, true));
            shine.close();
            assertFalse(shine.draw(canvas, 180, 320, true));
        } finally {
            shine.close();
            bitmap.recycle();
        }
    }
}

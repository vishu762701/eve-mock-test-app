package com.eve.app.telegram.qr;

import android.graphics.Bitmap;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Runs the production C++ raster on real Robolectric bitmaps, not a fake image. */
@Implements(QrBackdropNative.class)
public final class ShadowQrBackdropNative {
    @Implementation protected static void __staticInitializer__() {
        // Android jnigraphics is supplied by the test-only host bitmap bridge.
    }
    @Implementation protected static void generateNative(Bitmap bitmap, Bitmap ring, float cell,
            int count, float[] geometry, int[] colors, float[] wave) {
        QrBackdropHostBridge.generate(bitmap, ring, cell, count, geometry, colors, wave);
    }
}

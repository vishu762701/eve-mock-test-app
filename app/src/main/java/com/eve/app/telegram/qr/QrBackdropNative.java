package com.eve.app.telegram.qr;

import android.graphics.Bitmap;

/** EVE-only validated binding of the original C++ Utilities.generateQrBackdrop. */
public final class QrBackdropNative {
    static { System.loadLibrary("eve_qr_backdrop"); }
    private QrBackdropNative() {}

    public static void generate(Bitmap bitmap, Bitmap ring, float cell, int count,
                                float[] geometry, int[] colors, float[] wave) {
        if (bitmap == null || bitmap.isRecycled() || !bitmap.isMutable()
                || bitmap.getConfig() != Bitmap.Config.ARGB_8888
                || bitmap.getWidth() > 162 || bitmap.getHeight() > 162
                || !Float.isFinite(cell) || cell <= 0 || count < 1 || count > 2
                || geometry == null || geometry.length < count * 7
                || colors == null || colors.length < (count == 2 ? 7 : 3)
                || count == 2 && (wave == null || wave.length < 15)) {
            throw new IllegalArgumentException("Invalid QR backdrop raster arguments");
        }
        for (int i = 0; i < count * 7; i++) {
            if (!Float.isFinite(geometry[i])) throw new IllegalArgumentException("Nonfinite QR geometry");
        }
        if (count == 2) {
            for (int i = 0; i < 15; i++) {
                if (!Float.isFinite(wave[i])) throw new IllegalArgumentException("Nonfinite QR wave");
            }
        }
        if (ring != null && (ring.isRecycled() || !ring.isMutable()
                || ring.getConfig() != Bitmap.Config.ARGB_8888
                || ring.getWidth() != bitmap.getWidth() || ring.getHeight() != bitmap.getHeight()
                || ring == bitmap)) {
            throw new IllegalArgumentException("Invalid QR backdrop ring bitmap");
        }
        generateNative(bitmap, ring, cell, count, geometry, colors, wave);
    }

    private static native void generateNative(Bitmap bitmap, Bitmap ring, float cell, int count,
                                             float[] geometry, int[] colors, float[] wave);
}

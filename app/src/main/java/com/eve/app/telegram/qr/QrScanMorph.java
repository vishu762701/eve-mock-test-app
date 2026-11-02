
package com.eve.app.telegram.qr;

import org.telegram.ui.Components.CubicBezierInterpolator;

/**
 * ADAPTED original Java QrScanOverlay shape/source curves.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15:
 * QrScanOverlay.java:99-112,429-479,561-568,601-623.
 * EVE supplies its own CameraX/ZXing and username-only QR contract.
 * Copyright Telegram contributors; GPL distribution review remains OPEN.
 */
public final class QrScanMorph {
    public static final long OPEN_MS = 390L, CLOSE_MS = 330L;
    private static final CubicBezierInterpolator STRETCH =
        new CubicBezierInterpolator(0.05, 0.7, 0.1, 1.0);
    private static final CubicBezierInterpolator SHRINK =
        new CubicBezierInterpolator(0.3, 0.0, 0.1, 1.0);
    public float left, top, right, bottom, radius, contentAlpha, blackAlpha, scale;

    private static float clamp(float v) {
        return Float.isFinite(v) ? Math.max(0f, Math.min(1f, v)) : 0f;
    }
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    public static float stretch(float progress, boolean closing) {
        final float p = clamp(progress);
        return closing ? 1f - SHRINK.getInterpolation(1f - p) :
            STRETCH.getInterpolation(p);
    }
    public void sample(float l, float t, float r, float b, int width, int height,
                       float progress, boolean closing, float corner) {
        final float v = stretch(progress, closing);
        left = lerp(l, 0f, v);
        top = lerp(t, 0f, v);
        right = lerp(r, width, v);
        bottom = lerp(b, height, v);
        radius = lerp(Math.max(0f, b - t) / 2f, Math.max(0f, corner), v);
        contentAlpha = clamp((v - 0.15f) / 0.45f);
        blackAlpha = clamp(v / 0.3f);
        scale = width > 0 && height > 0 ?
            Math.max((right - left) / width, (bottom - top) / height) : 1f;
    }
}

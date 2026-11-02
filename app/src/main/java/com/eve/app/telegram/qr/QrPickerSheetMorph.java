package com.eve.app.telegram.qr;

/**
 * ADAPTED EVE host of official Telegram QR island-to-sheet geometric motion.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareView.java:5317-5433 (stepSheet/drawSheet), 1904-1918 (reveal).
 * Upstream 420ms SHEET_EASE cubic-bezier(0.16,1,0.3,1), island
 * 144x40dp -> floating sheet 10dp inset, 28.17dp target radius.
 * The full account-dependent Telegram view is deliberately not imported.
 * Telegram contributors; release compliance remains OPEN.
 */
public final class QrPickerSheetMorph {
    public static final long OPEN_MS = 420L;
    public static final long CLOSE_MS = 360L;
    private final float density;
    public float left, top, right, bottom, radius, contentAlpha;
    public QrPickerSheetMorph(float density) {
        if (!Float.isFinite(density) || density <= 0f)
            throw new IllegalArgumentException("density");
        this.density = density;
    }

    private static float clamp(float t) { return Math.max(0f, Math.min(1f, t)); }
    private static float lerp(float a, float b, float t) { return a + (b-a) * t; }
    private float dp(float v) { return density * v; }

    /** The source's CubicBezierInterpolator x-inverted using bounded bisection. */
    public static float sourceEase(float progress) {
        final float t = clamp(progress);
        float lo = 0f, hi = 1f, u = t;
        for (int i = 0; i < 16; i++) {
            u = (lo + hi) / 2f;
            final float v = 1f - u;
            final float x = 3f * v * v * u * 0.16f +
                3f * v * u * u * 0.3f + u*u*u;
            if (x < t) lo = u; else hi = u;
        }
        final float v = 1f - u;
        return 3f*v*v*u + 3f*v*u*u + u*u*u;
    }

    /** Source stepSheet content alpha during opening. */
    public static float openContent(float sheet) { return clamp(clamp(sheet) / 0.8f); }

    /** Source closing content fade, 170 ms ease-in exponent 1.7. */
    public static float closeContent(long elapsedMillis) {
        final float u = clamp(elapsedMillis / 170f);
        return (float) Math.pow(1f - u, 1.7f);
    }

    /**
     * Local EVE picker host: source island origin is a bar centered at
     * (width/2, height-30dp); full panel is insets 10dp on three edges.
     * Caller must sample sourceEase(time) before this geometry.
     */
    public void sample(int width, int height, float sheet) {
        final float t = clamp(sheet);
        final float cx = width / 2f, originY = height - dp(30f);
        final float finalLeft = dp(10f), finalRight = width - dp(10f);
        final float finalTop = dp(10f), finalBottom = height - dp(10f);
        final float minInset = dp(7f);
        left = Math.max(minInset, lerp(cx - dp(72f), finalLeft, t));
        top = lerp(originY - dp(20f), finalTop, t);
        right = Math.min(width - minInset, lerp(cx + dp(72f), finalRight, t));
        bottom = lerp(originY + dp(20f), finalBottom, t);
        radius = Math.min((bottom-top)/2f, lerp(dp(20f), dp(28.17f), t));
        contentAlpha = openContent(t);
    }
}

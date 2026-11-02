package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.lerp;

/**
 * ADAPTED original Java card bounce/pulse from QrShareView.java:1534-1542,
 * 1578-1596,1043,3668-3687, DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * EVE passes monotonic frame time and motion policy instead of global clocks.
 * Telegram contributors; original root GPLv2 and release-review obligations apply.
 */
public final class QrCardMotion {
    public static final long BOUNCE_MS = 320;
    private long qrBounceStart, cardPulseStart;
    private float qrBounceFrom = 1f;

    public void recolor(long now, boolean enabled) {
        if (!enabled) { clear(); return; }
        qrBounceFrom = bounce(qrBounceStart, now, 0.03f, 0.005f);
        qrBounceStart = now;
    }

    public void changeEmojis(long now, boolean enabled) {
        cardPulseStart = enabled ? now : 0;
    }

    public float scale(long now, boolean enabled) {
        if (!enabled) { clear(); return 1f; }
        final float u = qrBounceStart == 0 ? 1f : clamp01((now - qrBounceStart) / (float) BOUNCE_MS);
        float cs = bounce(qrBounceStart, now, 0.03f, 0.005f) + (qrBounceFrom - 1f) * (1f - u) * (1f - u);
        if (cardPulseStart != 0) {
            final float p = (now - cardPulseStart) / 360f;
            if (p >= 1f) cardPulseStart = 0;
            else if (p > 0f) cs *= 1f - 0.03f * (float) Math.sin(Math.PI * p) * (1f - p * 0.3f);
        }
        return cs;
    }

    public boolean isAnimating(long now) {
        return qrBounceStart != 0 && now - qrBounceStart < BOUNCE_MS
            || cardPulseStart != 0 && now - cardPulseStart < 360;
    }

    private static float bounce(long start, long now, float a, float b) {
        if (start == 0) return 1f;
        final float u = (now - start) / (float) BOUNCE_MS;
        if (u <= 0f || u >= 1f) return 1f;
        if (u < 0.25f) return 1f + a * (float) Math.sin(Math.PI / 2 * u / 0.25f);
        if (u < 0.6f) {
            final float k = (u - 0.25f) / 0.35f;
            return lerp(1f + a, 1f - b, k * k * (3f - 2f * k));
        }
        final float k = (u - 0.6f) / 0.4f;
        return lerp(1f - b, 1f, k * k * (3f - 2f * k));
    }

    private static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }
    public void clear() { qrBounceStart = cardPulseStart = 0; qrBounceFrom = 1f; }
}

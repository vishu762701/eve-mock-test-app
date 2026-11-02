package com.eve.app.telegram.qr;

import java.util.Arrays;

/**
 * ADAPTED original Java ready-item appearance from Telegram QrShareView.java:
 * 1015-1016,3171-3180,3245-3250,3281-3304 at
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Telegram contributors; root GPLv2 and EVE release gate apply.
 * EVE injects the monotonic clock, one per-screen salt and frame readiness;
 * each current/outgoing pattern owns its original bounded timestamp array.
 */
public final class QrEmojiAppearance {
    public static final long APPEAR_MS = 380, SPREAD_MS = 300;
    private final long[] shownAt;
    private final float appearSalt;

    public QrEmojiAppearance(int count, boolean pickedLater, float salt) {
        if (count < 0 || count > 6 || !Float.isFinite(salt))
            throw new IllegalArgumentException("EVE appearance bounds");
        shownAt = new long[count];
        appearSalt = salt;
        if (pickedLater) Arrays.fill(shownAt, 1);
    }

    public void resetForOpen() { Arrays.fill(shownAt, 0); }

    /** Original seeded per-node delay; the salt is stable for this screen's lifetime. */
    public float delay(double h) {
        final double x = Math.sin((h + appearSalt * 17.31) * 12.9898 + appearSalt * 78.233) * 43758.5453;
        return (float) (x - Math.floor(x)) * SPREAD_MS;
    }

    public float progress(int i, boolean ready, float seed, long now,
                          boolean openDone, boolean sharing, boolean immediateFrame) {
        if (sharing) return 1f;
        if (i < 0 || i >= shownAt.length) return 0f;
        if (shownAt[i] == 1) return ready ? 1f : 0f;
        if (!openDone) {
            if (immediateFrame && ready) {
                shownAt[i] = 1;
                return 1f;
            }
            return 0f;
        }
        if (!ready) return 0f;
        if (shownAt[i] == 0) shownAt[i] = now;
        return Math.max(0f, Math.min(1f, (now - shownAt[i] - delay(seed)) / (float) APPEAR_MS));
    }

    /** Original drawEmoji backOut envelope, never an invented light/size response. */
    public static float scale(float appear) {
        if (appear <= 0f) return 0f;
        if (appear >= 1f) return 1f;
        final float c1 = 1.70158f, c3 = c1 + 1f;
        float u = appear - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }
}

package com.eve.app.telegram.qr;

/**
 * Original Telegram Java emoji lattice slot transition equations extracted
 * from QrShareView.java:208-211,3187-3205,3369-3407 at
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * ADAPTED public host for EVE's independently authenticated QR asset catalogue.
 * No Telegram Document/AnimatedEmojiDrawable, account or network dependency.
 * Copyright Telegram contributors; see THIRD_PARTY_NOTICES.md.
 */
public final class QrPatternSwap {
    private QrPatternSwap() {}
    public static final float SHRINK = .18f, GROW_AT = .135f, GROW = .315f;
    public static final float SWAP_SPREAD = .3f;
    public static final long END_MS = 750L;

    /** Original QrShareView.slotScale body. */
    public static float slotScale(float p, boolean old, boolean otherNone) {
        if (old) {
            if (p <= 0f) return 1f;
            return Math.max(0f, 1f - p / SHRINK);
        }
        final float q = otherNone ? p : p - GROW_AT;
        if (q <= 0f) return 0f;
        if (q >= GROW) return 1f;
        return backOut(q / GROW);
    }

    /** Original QrShareView.backOut easing body. */
    private static float backOut(float u) {
        final float c1 = 1.70158f, c3 = c1 + 1f;
        u -= 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /** Source fract(hh * 59.3) * SWAP_SPREAD, identical across redraws. */
    public static float delaySeconds(double hh) {
        final double v = hh * 59.3;
        return (float) (v - Math.floor(v)) * SWAP_SPREAD;
    }

    /** Source QrShareView lattice slot index, including negative odd rows. */
    public static int slotIndex(int row, int col, int size) {
        if (size <= 0) return -1;
        final int q = col - (row - (row & 1)) / 2;
        final int n = q - row;
        return ((n % size) + size) % size;
    }

    /** When original pattern has settled, old EVE Drawables can be released. */
    public static boolean isActive(long elapsedMs, boolean enabled) {
        return enabled && elapsedMs >= 0 && elapsedMs < END_MS;
    }
}

package com.eve.app.telegram.qr;

/**
 * ADAPTED original Java QR emoji-sheet grid opening equations.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareActivity.java:538-569; QrShareView.java:1904-1918.
 * EVE hosts this transform on an AndroidX grid inside its existing sheet.
 * Telegram contributors; applicable GPL/release review in THIRD_PARTY_NOTICES.
 */
public final class QrPickerOpenMotion {
    private QrPickerOpenMotion() {}

    public static final long REVEAL_MS = 420L;
    private static float clamp(float x) {
        return Math.max(0f, Math.min(1f, x));
    }

    /** Original sheet grid's ease-out cubic reveal, scale 0.75 to 1. */
    public static float scale(float revealProgress) {
        final float rt = clamp(revealProgress);
        final float eased = 1f - (1f - rt) * (1f - rt) * (1f - rt);
        return 0.75f + 0.25f * eased;
    }

    /** Source alpha relation: sheet progress / 0.55, clamped. */
    public static float alpha(float sheetProgress) {
        return Math.min(1f, clamp(sheetProgress) / 0.55f);
    }
}

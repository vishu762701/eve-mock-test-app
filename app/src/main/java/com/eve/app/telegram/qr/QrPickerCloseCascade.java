package com.eve.app.telegram.qr;

/**
 * ADAPTED source Java QR picker row close from
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareActivity.java:609-632, QrShareView.java:4720-4730.
 * No Telegram account or document runtime.
 * Telegram contributors, GPL review open.
 */
public final class QrPickerCloseCascade {
    private QrPickerCloseCascade() {}
    public static final long CLOSE_SPAN_MS = 150L;
    public static final float CLOSE_ITEM_MS = 90f;

    public static float cellScale(float elapsedMs, float rowFromTop, float visibleRows) {
        final float rows = Math.max(1f, visibleRows);
        final float stagger = Math.min(25f, 60f / Math.max(1f, rows - 1f));
        final float time = Math.max(0f, Math.min(150f, elapsedMs));
        final float delay = Math.max(0f, rowFromTop - .5f) * stagger;
        final float u = Math.max(0f, Math.min(1f, (time - delay) / CLOSE_ITEM_MS));
        return 1f - .4f * u * u;
    }
}

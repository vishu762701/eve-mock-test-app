package com.eve.app.telegram.qr;

import android.graphics.RectF;

/** ADAPTED original Java QrScanOverlay.java:579-599, ActionBar.java:1914-1920
 * at DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Telegram contributors; root GPLv2 and release gate apply.
 * EVE fits the complete source composition between measured system/close bounds
 * only when a compact window cannot contain the original title/frame/buttons. */
public final class QrScannerLayout {
    public final RectF frame = new RectF(), close = new RectF(),
            flash = new RectF(), gallery = new RectF();
    public float titleTop, scale = 1f;

    public boolean sample(int w, int h, float density, int insetTop, int insetBottom, int titleHeight) {
        if (w <= 0 || h <= 0 || !Float.isFinite(density) || density <= 0) return false;
        final float barHeight = dp(w > h ? 48 : 56, density);
        final float ccy = insetTop + barHeight / 2f;
        close.set(dp(9, density), ccy - dp(20, density), dp(49, density), ccy + dp(20, density));
        final float side = Math.min(w, h) * .66f, cy = h / 2f - dp(20, density);
        frame.set(w / 2f - side / 2f, cy - side / 2f, w / 2f + side / 2f, cy + side / 2f);
        final float fs = dp(56, density), fy = frame.bottom + dp(64, density);
        final float fx = w / 2f + dp(35, density) + fs / 2f;
        flash.set(fx - fs / 2f, fy, fx + fs / 2f, fy + fs);
        final float gx = w / 2f - dp(35, density) - fs / 2f;
        gallery.set(gx - fs / 2f, fy, gx + fs / 2f, fy + fs);
        titleTop = frame.top - dp(36, density) - titleHeight;
        // Source hit() adds 8dp around each visual button. Keep those targets
        // clear of the measured system bars/close row as the full group fits.
        final float safeTop = insetTop + barHeight + dp(8, density);
        final float safeBottom = h - insetBottom - dp(8, density);
        if (safeBottom <= safeTop) return false;
        final float groupTop = titleTop, groupBottom = flash.bottom;
        scale = Math.min(1f, (safeBottom - safeTop) / (groupBottom - groupTop));
        final float top = Math.max(safeTop, Math.min(safeBottom - (groupBottom-groupTop)*scale, groupTop));
        fit(frame, w/2f, groupTop, top);
        fit(flash, w/2f, groupTop, top);
        fit(gallery, w/2f, groupTop, top);
        titleTop = top;
        return true;
    }

    private void fit(RectF r, float cx, float origin, float top) {
        r.set(cx + (r.left-cx)*scale, top + (r.top-origin)*scale,
                cx + (r.right-cx)*scale, top + (r.bottom-origin)*scale);
    }
    private static int dp(float value, float density) { return (int) Math.ceil(value*density); }
}

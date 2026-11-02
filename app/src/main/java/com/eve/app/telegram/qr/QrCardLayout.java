package com.eve.app.telegram.qr;

import android.graphics.RectF;

/**
 * ADAPTED original Java username-card geometry from QrShareView.java:1879-1881,
 * 2046-2061,3808-3819 at DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Telegram contributors; root GPLv2 and EVE release gate apply.
 * EVE has no paid/temporary link chip. Measured native controls bound the compact
 * window fallback; the entire source card scales together instead of adding offsets.
 */
public final class QrCardLayout {
    public final RectF bounds = new RectF();
    public final RectF localCard = new RectF();
    public float sourceWidth, sourceHeight, fitScale = 1f;

    public boolean sample(int width, int height, float density, float insetTop,
                          float safeTop, float safeBottom, float sheet, float sheetTop) {
        if (width <= 0 || height <= 0 || !Float.isFinite(density) || density <= 0 ||
                !Float.isFinite(safeTop) || !Float.isFinite(safeBottom)) {
            bounds.setEmpty();
            return false;
        }
        safeTop = Math.max(0f, safeTop);
        safeBottom = Math.min(height, safeBottom);
        if (safeBottom <= safeTop) { bounds.setEmpty(); return false; }
        sourceWidth = width * 0.745f;
        sourceHeight = sourceWidth * 1.07f; // EVE username card: tempK = 0
        fitScale = Math.min(1f, (safeBottom - safeTop) / sourceHeight);
        final float cw = sourceWidth * fitScale, ch = sourceHeight * fitScale;
        final float left = (width - cw) / 2f;
        // Original rest and sheet rise (paidK = tempK = 0). The clamps affect
        // only windows in which the original card would overlap native chrome.
        final float sourceRest = height * 0.468f - ch / 2f - dp(15, density) + dp(16, density);
        final float rest = Math.max(safeTop, Math.min(safeBottom - ch, sourceRest));
        final float minimum = Math.min(rest, Math.max(safeTop, insetTop + dp(51, density)));
        final float need = sheetTop - dp(13, density) - ch;
        final float open = Math.max(minimum, Math.min(rest, need));
        final float progress = Float.isFinite(sheet) ? Math.max(0f, Math.min(1f, sheet)) : 0f;
        final float top = rest + (open - rest) * progress;
        bounds.set(left, top, left + cw, top + ch);
        localCard.set(0, 0, sourceWidth, sourceHeight);
        return true;
    }

    private static int dp(float value, float density) { return (int) Math.ceil(value * density); }
}

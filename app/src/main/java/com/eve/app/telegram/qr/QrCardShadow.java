package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.dp;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.core.graphics.ColorUtils;

/**
 * ADAPTED original Java card shadow from QrShareView.java:3690-3806 at
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Original mask geometry, alpha, colour and nine-slice algorithms retained.
 * EVE executor/handler ownership replaces Telegram queues; late work is recycled.
 * Telegram contributors; root GPLv2 / release review in THIRD_PARTY_NOTICES.
 */
public final class QrCardShadow implements AutoCloseable {
    private static final java.util.concurrent.Executor WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "EveQrShadow");
        thread.setDaemon(true);
        return thread;
    });
    public QrCardShadow(android.view.View host) { this.host = host; }
    private static final int CARD_RADIUS_DP = 30, SHADOW_BLUR_DP = 24, SHADOW_DROP_DP = 4, SHADOW_DS = 4;
    /** The shadow's alpha, SHADOW_DS px per texel; its geometry in texels. */
    private static volatile byte[] shadowMask;
    private static int shadowN, shadowE, shadowK;
    private boolean cardShadowQueued;
    private int generation;
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.view.View host;
    private Bitmap cardShadow;
    private int[] cardShadowPx;
    private int cardShadowColor;
    private final Paint cardShadowPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final android.graphics.Rect shadowSrc = new android.graphics.Rect();
    private final RectF shadowDst = new RectF();
    private final int[] shadowSx = new int[4];
    private final float[] shadowDx = new float[4], shadowDy = new float[4];

    /** The mask, once per process (any thread; the geometry is published by the volatile mask). */
    private static synchronized byte[] cardShadowMask() {
        if (shadowMask != null) return shadowMask;
        // in texels: the blur's reach (3σ of setShadowLayer's σ = 0.57735·r + 0.5), the corner slice (reach +
        // radius + 1, past the arc), the whole mask (two corners + 2 texels to stretch)
        final float sigma = (0.57735f * dp(SHADOW_BLUR_DP) + 0.5f) / SHADOW_DS;
        final float r = dp(CARD_RADIUS_DP) / (float) SHADOW_DS;
        final int e = (int) Math.ceil(3f * sigma), k = e + (int) Math.ceil(r) + 1, n = 2 * k + 2;
        final Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setMaskFilter(new android.graphics.BlurMaskFilter(Math.max(0.1f, (sigma - 0.5f) / 0.57735f), android.graphics.BlurMaskFilter.Blur.NORMAL));
        new Canvas(b).drawRoundRect(e, e, n - e, n - e, r, r, p);
        final Bitmap a = b.extractAlpha();
        b.recycle();
        final int stride = a.getRowBytes(); // ALPHA_8 rows may be padded
        final byte[] rows = new byte[stride * n], mask = new byte[n * n];
        a.copyPixelsToBuffer(java.nio.ByteBuffer.wrap(rows));
        a.recycle();
        for (int y = 0; y < n; y++) System.arraycopy(rows, y * stride, mask, y * n, n);
        shadowN = n;
        shadowE = e;
        shadowK = k;
        shadowMask = mask;
        return mask;
    }

    /** The mask times the colour: ARGB, unpremultiplied (as setPixels takes it). */
    private static void colorCardShadow(int[] px, byte[] mask, int color) {
        final int rgb = color & 0xFFFFFF, ca = color >>> 24;
        for (int i = 0; i < px.length; i++) {
            px[i] = ((mask[i] & 0xFF) * ca / 255) << 24 | rgb;
        }
    }

    /**
     * The shadow ready before the open's first frame: mask, colour and bitmap in the background, its GPU upload
     * started there too (prepareToDraw); a later colour is baked on the UI thread.
     */
    /** Prewarm as upstream does; generation ownership prevents a detach/reopen leak. */
    public void prepare(int qr) {
        if (cardShadow != null || cardShadowQueued) return;
        cardShadowQueued = true;
        final int owner = generation;
        final int color = cardShadowColorFor(qr);
        WORKER.execute(() -> {
            final byte[] mask = cardShadowMask();
            final int n = shadowN;
            final Bitmap bitmap = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
            final int[] pixels = new int[n * n];
            colorCardShadow(pixels, mask, color);
            bitmap.setPixels(pixels, 0, n, 0, 0, n, n);
            bitmap.prepareToDraw();
            main.post(() -> {
                if (generation != owner || cardShadow != null) { bitmap.recycle(); return; }
                cardShadowQueued = false;
                cardShadow = bitmap;
                cardShadowPx = pixels;
                cardShadowColor = color;
                host.invalidate();
            });
        });
    }

    /** Exports need a finished shadow even when invoked before asynchronous prewarm. */
    public void draw(Canvas canvas, RectF card, int qr, boolean export) {
        if (export && cardShadow == null) {
            generation++;
            cardShadowQueued = false;
            final byte[] mask = cardShadowMask();
            cardShadow = Bitmap.createBitmap(shadowN, shadowN, Bitmap.Config.ARGB_8888);
            cardShadowPx = new int[shadowN * shadowN];
            cardShadowColor = cardShadowColorFor(qr);
            colorCardShadow(cardShadowPx, mask, cardShadowColor);
            cardShadow.setPixels(cardShadowPx, 0, shadowN, 0, 0, shadowN, shadowN);
        }
        if (cardShadow == null) prepare(qr);
        drawCardShadow(canvas, card, cardShadowColorFor(qr));
    }

    private static int cardShadowColorFor(int qr) {
        return ColorUtils.setAlphaComponent(ColorUtils.blendARGB(qr, Color.BLACK, 0.4f), 0x10);
    }

    private void drawCardShadow(Canvas c, RectF card, int color) {
        final Bitmap b = cardShadow;
        if (b == null) {
            return;
        }
        final byte[] mask = shadowMask;
        final int n = shadowN;
        if (color != cardShadowColor) {
            // the colour baked in (follows the code colour, the wave included: ~n² ints, no blur)
            cardShadowColor = color;
            colorCardShadow(cardShadowPx, mask, color);
            b.setPixels(cardShadowPx, 0, n, 0, 0, n, n);
        }
        final float ds = SHADOW_DS, e = shadowE * ds, k = shadowK * ds, drop = dp(SHADOW_DROP_DP);
        shadowSx[0] = 0;
        shadowSx[1] = shadowK;
        shadowSx[2] = n - shadowK;
        shadowSx[3] = n;
        shadowDx[0] = card.left - e;
        shadowDx[1] = shadowDx[0] + k;
        shadowDx[3] = card.right + e;
        shadowDx[2] = shadowDx[3] - k;
        shadowDy[0] = card.top - e + drop;
        shadowDy[1] = shadowDy[0] + k;
        shadowDy[3] = card.bottom + e + drop;
        shadowDy[2] = shadowDy[3] - k;
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 3; i++) {
                shadowSrc.set(shadowSx[i], shadowSx[j], shadowSx[i + 1], shadowSx[j + 1]);
                shadowDst.set(shadowDx[i], shadowDy[j], shadowDx[i + 1], shadowDy[j + 1]);
                c.drawBitmap(b, shadowSrc, shadowDst, cardShadowPaint);
            }
        }
    }

    @Override public void close() {
        generation++;
        cardShadowQueued = false;
        if (cardShadow != null) cardShadow.recycle();
        cardShadow = null;
        cardShadowPx = null;
    }
}

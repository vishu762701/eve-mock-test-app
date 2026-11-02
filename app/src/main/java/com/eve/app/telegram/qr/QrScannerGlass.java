package com.eve.app.telegram.qr;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import java.util.function.BooleanSupplier;

/**
 * ADAPTED original Java QrScanOverlay.java:804-857 drawGlassBody from
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Telegram contributors; GPLv2/source and EVE release gate apply.
 * Separate EVE View backgrounds replace the upstream single Canvas control renderer.
 */
public final class QrScannerGlass extends Drawable {
    private static final int FLAT_SHADE = 0x29000000, NO_BLUR_SHADE = 0x80000000;
    private final float radius;
    private final QrControlBackground.Backdrop backdrop;
    private final BooleanSupplier blurAllowed;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final RimPainter rim = new RimPainter();
    private int alpha = 255;

    public QrScannerGlass(float radius, QrControlBackground.Backdrop backdrop, BooleanSupplier blurAllowed) {
        this.radius = radius;
        this.backdrop = backdrop;
        this.blurAllowed = blurAllowed;
    }

    @Override public void draw(Canvas canvas) {
        if (alpha <= 0) return;
        rect.set(getBounds());
        final int white = (int) (255 * Math.max(0f, Math.min(1f, RimTune.v[RimTune.FILL] / 100f)));
        final boolean allowed = blurAllowed.getAsBoolean();
        boolean sampled = false;
        if (allowed && backdrop != null && getCallback() instanceof View) {
            path.rewind();
            path.addRoundRect(rect, radius, radius, Path.Direction.CW);
            final int save = canvas.save();
            canvas.clipPath(path);
            // Source's alpha is supplied by the scanner parent; Drawable alpha
            // needs its own layer only for a separately faded View background.
            if (alpha < 255) canvas.saveLayerAlpha(rect, alpha);
            sampled = backdrop.draw(canvas, (View) getCallback());
            if (alpha < 255) canvas.restore();
            canvas.restoreToCount(save);
        }
        fill.setColor(sampled ? 0x00FFFFFF | (white << 24) :
                ColorUtils.compositeColors(0x00FFFFFF | (white << 24),
                        allowed ? FLAT_SHADE : NO_BLUR_SHADE));
        fill.setAlpha((int) (Color.alpha(fill.getColor()) * alpha / 255f));
        canvas.drawRoundRect(rect, radius, radius, fill);
        QrGlass.drawSheenRound(canvas, rect, radius, false, alpha / 255f);
        rim.draw(canvas, rect, radius, RimTune.BIG, alpha / 255f);
    }

    @Override public void setAlpha(int value) { alpha = Math.max(0, Math.min(255, value)); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { fill.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

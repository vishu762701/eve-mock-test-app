package com.eve.app.telegram.qr;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Color;
import android.view.View;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** ADAPTED EVE control host: source scene blur/stand-in, glass fill, sheen and rim. */
public final class QrControlBackground extends Drawable {
    private final float radius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int alpha = 255;
    private final Backdrop backdrop;
    private final Path clip = new Path();
    public interface Backdrop { boolean draw(Canvas canvas, View owner); }
    public QrControlBackground(float radius) { this(radius, null); }
    public QrControlBackground(float radius, Backdrop backdrop) {
        this.radius = radius;
        this.backdrop = backdrop;
    }
    @Override public void draw(Canvas canvas) {
        rect.set(getBounds());
        boolean sampled = false;
        if (backdrop != null && getCallback() instanceof View) {
            clip.rewind();
            clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            if (alpha < 255) canvas.saveLayerAlpha(rect, alpha);
            sampled = backdrop.draw(canvas, (View) getCallback());
            if (alpha < 255) canvas.restore();
            canvas.restore();
        }
        paint.setColor(sampled ? QrGlass.controlFill() : 0x99343638);
        paint.setAlpha(Math.round(Color.alpha(paint.getColor()) * alpha / 255f));
        canvas.drawRoundRect(rect, radius, radius, paint);
        QrGlass.drawSheenRound(canvas, rect, radius, false, alpha / 255f);
        QrGlass.drawRim(canvas, rect, radius, QrGlass.RIM_BIG, alpha / 255f);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

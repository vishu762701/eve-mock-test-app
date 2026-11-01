package com.eve.app.telegram.qr;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** EVE Drawable host of upstream QrGlass sheen/rim. Backdrop blur integration is separate. */
public final class QrControlBackground extends Drawable {
    private final float radius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int alpha = 255;
    public QrControlBackground(float radius) { this.radius = radius; }
    @Override public void draw(Canvas canvas) {
        rect.set(getBounds());
        paint.setColor(0x99343638);
        paint.setAlpha(Math.round(0x99 * alpha / 255f));
        canvas.drawRoundRect(rect, radius, radius, paint);
        QrGlass.drawSheenRound(canvas, rect, radius, false, alpha / 255f);
        QrGlass.drawRim(canvas, rect, radius, QrGlass.RIM_BIG, alpha / 255f);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}

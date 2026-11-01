package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ADAPTED original Java grid layer from QrEmojiPanel.java:493-515 and smooth
 * bottom/island masks from QrShareActivity.java:506-518,593-609,659-669.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * EVE uses AndroidX RecyclerView and its local selection overlay; no pack strip.
 * Telegram contributors; GPL release obligations in THIRD_PARTY_NOTICES.
 */
public final class QrEmojiGrid extends RecyclerView {
    private EmojiPickOverlay overlay;
    private final RectF cut = new RectF();
    private final float[] radiusAlpha = new float[2];
    private final Path cutPath = new Path();
    private final Paint cutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public QrEmojiGrid(Context context) {
        super(context);
        cutPaint.setColor(Color.BLACK);
        cutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }

    public void setSelectionOverlay(EmojiPickOverlay value) { overlay = value; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Original nine-stop smoothstep 1 -> 0 mask at the glass bottom (10dp).
        final int n = 9;
        final int[] cols = new int[n];
        final float[] pos = new float[n];
        for (int i = 0; i < n; i++) {
            final float u = i / (float) (n - 1);
            final float a = u * u * (3f - 2f * u);
            cols[i] = ((int) (255 * (1f - a) + 0.5f)) << 24;
            pos[i] = u;
        }
        fadePaint.setShader(new LinearGradient(0, h - dp(10), 0, h, cols, pos, Shader.TileMode.CLAMP));
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        final int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        if (overlay != null) overlay.drawUnder(canvas, this);
        super.dispatchDraw(canvas);
        if (overlay != null && overlay.islandCut(cut, radiusAlpha)) {
            cutPath.rewind();
            cutPath.addRoundRect(cut, radiusAlpha[0], radiusAlpha[0], Path.Direction.CW);
            cutPaint.setAlpha((int) (255 * radiusAlpha[1]));
            canvas.drawPath(cutPath, cutPaint);
        }
        canvas.drawRect(0, getHeight() - dp(10), getWidth(), getHeight(), fadePaint);
        canvas.restoreToCount(save);
    }
}

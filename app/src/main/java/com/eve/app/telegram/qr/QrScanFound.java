package com.eve.app.telegram.qr;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * ADAPTED original Java QR scanner found-code corners:
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrScanOverlay.java:108-112,346-355,700-782,994-1011,1040-1063.
 * Telegram contributors retain copyright; combined GPL review is OPEN.
 * EVE's CameraX/ZXing coordinates are supplied in preview-local pixels.
 */
public final class QrScanFound {
    public static final long FOUND_DELAY_MS = 450L;
    private final Paint foundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private float[] points;
    private float foundK;

    public QrScanFound(float density) {
        foundPaint.setStyle(Paint.Style.STROKE);
        foundPaint.setStrokeCap(Paint.Cap.ROUND);
        foundPaint.setStrokeJoin(Paint.Join.ROUND);
        foundPaint.setColor(0xFFFFDE07);
        foundPaint.setStrokeWidth(6f * density);
    }

    /** EVE's ZXing fallback follows Telegram: only four authentic points. */
    public boolean show(float[] local, float width, float height) {
        if (local == null || local.length != 8 || width <= 0 || height <= 0) return false;
        for (int i = 0; i < local.length; i += 2) {
            if (!Float.isFinite(local[i]) || !Float.isFinite(local[i + 1]) ||
                local[i] < 0f || local[i] > width ||
                local[i + 1] < 0f || local[i + 1] > height) return false;
        }
        points = local.clone();
        foundK = 0f;
        return true;
    }

    /** Source approach(0.3), used for the corner fade and yellow emphasis. */
    public boolean advance() {
        final float target = points != null ? 1f : 0f;
        if (Math.abs(foundK - target) < 0.005f) {
            foundK = target;
            return false;
        }
        foundK += (target - foundK) * 0.3f;
        return true;
    }

    public float progress() { return foundK; }
    public void clear() { points = null; foundK = 0f; }

    public void draw(Canvas canvas) {
        if (points == null || foundK <= 0f) return;
        float cx = 0f, cy = 0f;
        for (int i = 0; i < 4; ++i) {
            cx += points[i * 2];
            cy += points[i * 2 + 1];
        }
        cx /= 4f;
        cy /= 4f;
        path.rewind();
        for (int i = 0; i < 4; ++i) {
            final int prev = (i + 3) % 4, next = (i + 1) % 4;
            final float x = points[i * 2], y = points[i * 2 + 1];
            path.moveTo(x + (points[prev * 2] - x) * 0.18f,
                        y + (points[prev * 2 + 1] - y) * 0.18f);
            path.lineTo(x, y);
            path.lineTo(x + (points[next * 2] - x) * 0.18f,
                        y + (points[next * 2 + 1] - y) * 0.18f);
        }
        final int save = canvas.save();
        final float s = 0.5f + (1.1f - 0.5f) * foundK;
        canvas.scale(s, s, cx, cy);
        foundPaint.setAlpha((int) (255f * foundK));
        canvas.drawPath(path, foundPaint);
        canvas.restoreToCount(save);
    }

    /**
     * ZXing PlanarYUV rotateCounterClockwise() yields coordinates in an
     * artificially rotated buffer. Revert exactly 0/90/180/270 degrees to
     * original ImageProxy buffer coordinates before CameraX maps to PreviewView.
     */
    public static float[] toUnrotated(float[] rotated, int width, int height, int turnsCCW) {
        if (rotated == null || rotated.length != 8 || width <= 0 || height <= 0 ||
            turnsCCW < 0 || turnsCCW > 3) return null;
        final float[] out = new float[8];
        for (int i = 0; i < 8; i += 2) {
            final float x = rotated[i], y = rotated[i + 1];
            if (!Float.isFinite(x) || !Float.isFinite(y)) return null;
            final float ux, uy;
            switch (turnsCCW) {
                case 1: ux = width - 1f - y; uy = x; break;
                case 2: ux = width - 1f - x; uy = height - 1f - y; break;
                case 3: ux = y; uy = height - 1f - x; break;
                default: ux = x; uy = y; break;
            }
            if (ux < 0f || ux >= width || uy < 0f || uy >= height) return null;
            out[i] = ux;
            out[i + 1] = uy;
        }
        return out;
    }
}

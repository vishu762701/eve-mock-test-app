package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.dp;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.Random;

/**
 * ADAPTED original Java emoji-only QR background light sweep and 12dp raster.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * QrShareView.java:2488-2559,2561-2562,2805-2838,3111-3160.
 * No gifts, account, sensors, premium services or Telegram network dependencies.
 * Copyright Telegram contributors; unresolved GPL release obligations remain.
 */
public final class QrSceneShine implements AutoCloseable {
    private final Random random = new Random();
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private Bitmap light;
    private int[] pixels;
    private float startedAt = -1f, fieldT, sweepStart, sweepPeriod = 5f;
    private float sweepDX = -0.7071f, sweepDY = 0.7071f;
    private float shineC, shineW, shineEnv = 1f, shineD, shineDEnv;
    private boolean firstSweepSet, shining;

    /** Original auto-sweep with the source introduction delay; tilt disabled by source default. */
    public void step(long now, boolean motionEnabled, int width, int height) {
        if (startedAt < 0f) startedAt = now;
        fieldT = Math.max(0f, (now - startedAt) / 1000f);
        if (!motionEnabled || width <= 0 || height <= 0) {
            shining = false;
            return;
        }
        final float randK = RimTune.v[RimTune.SH_RAND] / 100f;
        while (fieldT >= sweepStart + sweepPeriod) {
            sweepStart += sweepPeriod;
            sweepPeriod = Math.max(RimTune.v[RimTune.SH_DUR] + .2f,
                RimTune.v[RimTune.SH_PER] * 1.5f + random.nextFloat() * .8f * randK);
            final double a = Math.toRadians(RimTune.v[RimTune.SH_ANG] +
                (random.nextFloat() - .5f) * 12f * randK);
            sweepDX = (float) Math.cos(a);
            sweepDY = (float) Math.sin(a);
        }
        if (!firstSweepSet) {
            firstSweepSet = true;
            sweepStart = fieldT + RimTune.v[RimTune.INTRO_MS] / 1000f + 1.5f;
            sweepPeriod = 1000f;
        }
        if (fieldT >= sweepStart + RimTune.v[RimTune.SH_DUR] && sweepPeriod > 999f)
            sweepPeriod = RimTune.v[RimTune.SH_PER] * 1.5f;
        final float phase = (fieldT - sweepStart) / RimTune.v[RimTune.SH_DUR];
        shining = phase >= 0f && phase < 1f;
        shineW = dp(RimTune.v[RimTune.SH_W]);
        if (shining) {
            final float eased = .5f - .5f * (float) Math.cos(Math.PI * phase);
            final float ext = (Math.abs(sweepDX) * width + Math.abs(sweepDY) * height) / 2f +
                shineW * 3f;
            shineC = -ext + 2f * ext * eased;
            shineEnv = 1f;
        }
    }

    private static float fract(double x) { return (float) (x - Math.floor(x)); }
    /** Original deterministic valueNoise from QrShareView:2497-2502. */
    private static float valueNoise(float x) {
        final int i = (int) Math.floor(x);
        final float f = x - i, u = f * f * (3f - 2f * f);
        final float a = fract(Math.sin(i * 127.1) * 43758.5453);
        final float b = fract(Math.sin((i + 1) * 127.1) * 43758.5453);
        return a + (b - a) * u;
    }

    /** Source curved/cylindrical sweep distance, shared by the soft background-light raster. */
    private void shine(float x, float y, int w, int h) {
        float proj = (x - w / 2f) * sweepDX + (y - h / 2f) * sweepDY;
        final float ext = Math.max(1f,
            (Math.abs(sweepDX) * w + Math.abs(sweepDY) * h) / 2f);
        final float curve = RimTune.v[RimTune.SHINE_CURVE] / 100f;
        if (curve > 0f) {
            final float across = (x - w / 2f) * -sweepDY +
                (y - h / 2f) * sweepDX;
            final float u = across / Math.max(1f,
                (Math.abs(sweepDY) * w + Math.abs(sweepDX) * h) / 2f);
            proj -= curve * w * u * u;
        }
        final float flat = (proj - shineC) / shineW;
        float d = flat, amp = 1f;
        final float cyl = RimTune.v[RimTune.SHINE_CYL] / 100f;
        if (cyl > 0f) {
            final float halfPi = (float) (Math.PI / 2);
            final float tp = (float) Math.asin(Math.max(-1f, Math.min(1f, proj / ext)));
            final float tc = Math.max(-halfPi * 1.4f,
                Math.min(halfPi * 1.4f, shineC / ext * halfPi));
            final float dCyl = (tp - tc) / (shineW / ext * halfPi);
            d = flat + (dCyl - flat) * cyl;
            amp = 1f - cyl * RimTune.v[RimTune.EDGE_DIM] / 100f *
                (1f - (float) Math.cos(Math.max(-halfPi, Math.min(halfPi, tp))));
        }
        shineD = d;
        shineDEnv = shineEnv * amp;
    }

    /** Original coarse 12dp two-octave noise/light raster. True if it draws pixels. */
    public boolean draw(Canvas canvas, int width, int height, boolean fullyOpened) {
        if (!shining || !fullyOpened || width <= 0 || height <= 0 ||
            RimTune.v[RimTune.BG_STR] <= 0f) return false;
        final float cell = dp(12f);
        final int nx = (int) Math.ceil(width / cell) + 1,
            ny = (int) Math.ceil(height / cell) + 1;
        // EVE safety cap; the normal source host stays well below this bound.
        if (nx <= 0 || ny <= 0 || (long) nx * ny > 250000L) return false;
        if (light == null || light.getWidth() != nx || light.getHeight() != ny) {
            releaseRaster();
            light = Bitmap.createBitmap(nx, ny, Bitmap.Config.ARGB_8888);
            pixels = new int[nx * ny];
        }
        final float k = Math.max(1f, RimTune.v[RimTune.BG_W]);
        final float nz = RimTune.v[RimTune.BG_NOISE] / 100f;
        final float peak = .0275f * RimTune.v[RimTune.BG_STR] / 100f;
        final float acrossN = dp(130), t = fieldT * .12f;
        boolean any = false;
        for (int iy = 0; iy < ny; iy++) {
            for (int ix = 0; ix < nx; ix++) {
                final float x = ix * cell, y = iy * cell;
                shine(x, y, width, height);
                final float d = shineD / k;
                float v = 0f;
                if (d < 3.5f && d > -3.5f) {
                    v = shineDEnv * (float) Math.exp(-.5f * d * d);
                    if (nz > 0f) {
                        final float across = ((x - width / 2f) * -sweepDY +
                            (y - height / 2f) * sweepDX) / acrossN;
                        final float n = .65f * valueNoise(across + t) +
                            .35f * valueNoise(across * 2.3f - t * 1.7f + 11.3f);
                        v *= 1f + nz * (n * 2f - 1f);
                    }
                }
                final int a = Math.max(0, Math.min(255, Math.round(255 * peak * v)));
                if (a > 0) any = true;
                pixels[iy * nx + ix] = (a << 24) | 0xFFFFFF;
            }
        }
        if (!any) return false;
        light.setPixels(pixels, 0, nx, 0, 0, nx, ny);
        destination.set(-cell / 2f, -cell / 2f,
            nx * cell - cell / 2f, ny * cell - cell / 2f);
        canvas.drawBitmap(light, null, destination, paint);
        return true;
    }

    private void releaseRaster() {
        if (light != null) light.recycle();
        light = null; pixels = null;
    }

    @Override public void close() {
        releaseRaster();
        firstSweepSet = shining = false;
        startedAt = -1f;
        sweepPeriod = 5f;
        sweepStart = 0f;
    }
}

package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.dp;
import static com.eve.app.telegram.components.QrUiUnits.dpf2;
import static com.eve.app.telegram.components.QrUiUnits.lerp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import androidx.core.graphics.ColorUtils;
import java.util.WeakHashMap;

/**
 * ADAPTED original Java QrShareView scene raster/wave/ink/blur subsystem.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15:
 * QrShareView.java:124-134,685-724,1534-1575,2140-2175,2260-2426,3573-3588.
 * EVE host supplies card geometry, monotonic frame time, palette indices and
 * pattern drawing. No Telegram account, gift, fee or network subsystem.
 * Telegram contributors; GPL/release obligations in THIRD_PARTY_NOTICES.
 */
public final class QrBackdropScene implements AutoCloseable {
    private static final int SCENE_COLOR = 0;
    private static final int TUNED_COLOR = Backdrops.indexOfName(Backdrops.TUNED_NAME);
    private static final long WAVE_MS = 600;
    private static final int BLUR_DS = 9;
    private final View host;
    private final QrSceneShine shine = new QrSceneShine();
    private boolean hasEmojiPattern;
    private Scene scene = colorScene(0), oldScene;
    private float wave = 1f, waveX, waveY;
    private long waveStart;
    private float cardCY;
    private int width, height;
    private Object bgNode, blurNode;
    private int blurBuiltVersion = -1;
    private final WeakHashMap<View, Boolean> glassConsumers = new WeakHashMap<>();
    private final int[] sceneLocation = new int[2], controlLocation = new int[2];

    private static final class Scene {
        int type, index, center, edge, mid, qr;
    }

    private static Scene colorScene(int palette) {
        final Scene s = new Scene();
        s.type = SCENE_COLOR;
        s.index = Backdrops.orderToIndex(palette);
        s.center = QrPalettes.center(palette);
        s.edge = QrPalettes.edge(palette);
        s.qr = QrPalettes.ink(palette);
        s.mid = QrCodeLayer.mixOklab(s.center, s.edge, 0.5f);
        return s;
    }

    public QrBackdropScene(View host) { this.host = host; }

    /** Upstream interruption: the current target becomes the next wave's base. */
    public void setPalette(int palette, float x, float y, long now, boolean animate) {
        final Scene next = colorScene(palette);
        if (!animate) {
            oldScene = null;
            scene = next;
            wave = 1f;
        } else {
            oldScene = scene;
            scene = next;
            wave = 0f;
            waveStart = now;
            waveX = x;
            waveY = y;
        }
        backdropValid = false;
        host.invalidate();
    }

    public void setGeometry(int width, int height, float cardCenterY) {
        this.width = width;
        this.height = height;
        this.cardCY = cardCenterY;
    }

    public boolean isAnimating() { return wave < 1f && oldScene != null; }
    public void setEmojiPatternEnabled(boolean enabled) { hasEmojiPattern = enabled; }
    public void step(long now, boolean motionEnabled) {
        shine.step(now, motionEnabled && hasEmojiPattern, width, height);
        if (wave < 1f) {
            wave = motionEnabled ? Math.min(1f, Math.max(0f, (now - waveStart) / (float) WAVE_MS)) : 1f;
            if (wave >= 1f) oldScene = null;
        }
    }

    public int qrColor() {
        if (wave < 1f && oldScene != null) {
            final float maxR = (float) Math.hypot(Math.max(waveX, width - waveX), Math.max(waveY, height - waveY)) + dp(60);
            final float d = (float) Math.hypot(width / 2f - waveX, cardCY - waveY);
            final float k = clamp01((maxR * waveEase(wave) - d + dp(80)) / dp(160));
            return blendLab(oldScene.qr, scene.qr, k * k * (3 - 2 * k));
        }
        return scene.qr;
    }

    public int targetQrColor() { return scene.qr; }

    /** An independent export cache at the same source scene/wave state. */
    public QrBackdropScene snapshot() {
        QrBackdropScene copy = new QrBackdropScene(host);
        copy.scene = scene;
        copy.oldScene = oldScene;
        copy.wave = wave;
        copy.waveX = waveX;
        copy.waveY = waveY;
        copy.waveStart = waveStart;
        return copy;
    }

    /** Same source composition: backdrop, pattern, ring; blur records that scene. */
    public void draw(Canvas canvas, RunnablePattern pattern, boolean openDone, float corner) {
        if (width <= 0 || height <= 0) return;
        if (QrRender.hasEffects() && openDone && canvas.isHardwareAccelerated()) {
            if (bgNode == null) {
                bgNode = QrRender.createNode("eveQrBg");
                blurNode = QrRender.createNode("eveQrBlur");
            }
            final Canvas source = QrRender.beginRecording(bgNode, width, height);
            drawBackground(source, pattern, corner, openDone);
            QrRender.endRecording(bgNode);
            if (blurBuiltVersion != RimTune.version) {
                blurBuiltVersion = RimTune.version;
                QrRender.setBlur(blurNode, Math.max(0.5f, dpf2(RimTune.v[RimTune.BLUR]) / BLUR_DS), Shader.TileMode.CLAMP);
            }
            final Canvas blurred = QrRender.beginRecording(blurNode, (width + BLUR_DS - 1) / BLUR_DS, (height + BLUR_DS - 1) / BLUR_DS);
            blurred.scale(1f / BLUR_DS, 1f / BLUR_DS);
            QrRender.draw(blurred, bgNode);
            QrRender.endRecording(blurNode);
            QrRender.draw(canvas, bgNode);
        } else {
            drawBackground(canvas, pattern, corner, openDone);
            // A previous hardware frame must not be sampled after a software or
            // reveal frame. The current low-res picture is the source stand-in.
            clearNodes();
        }
        for (View consumer : glassConsumers.keySet()) consumer.postInvalidateOnAnimation();
    }

    public interface RunnablePattern { void draw(Canvas canvas); }

    private void drawBackground(Canvas canvas, RunnablePattern pattern,
                                float corner, boolean fullyOpened) {
        final boolean waving = isAnimating();
        drawBackdrop(canvas, width, height, waving, corner);
        if (hasEmojiPattern) shine.draw(canvas, width, height, fullyOpened);
        pattern.draw(canvas);
        if (waving && backdropRingOn) drawBackdropBitmap(canvas, backdropRing);
    }

    /** Source drawBlur/drawBlurStandIn, mapped from a separate EVE control view. */
    public boolean drawControlBackdrop(Canvas canvas, View control) {
        if (backdrop == null || control == null) return false;
        glassConsumers.put(control, Boolean.TRUE);
        host.getLocationOnScreen(sceneLocation);
        control.getLocationOnScreen(controlLocation);
        canvas.save();
        canvas.translate(sceneLocation[0] - controlLocation[0], sceneLocation[1] - controlLocation[1]);
        if (QrRender.hasNodes() && canvas.isHardwareAccelerated() && QrRender.hasDisplayList(blurNode)) {
            canvas.scale(BLUR_DS, BLUR_DS);
            QrRender.draw(canvas, blurNode);
        } else {
            drawBackdropBitmap(canvas, backdrop);
        }
        canvas.restore();
        return true;
    }

    private static final float SIN18 = (float) Math.sin(Math.toRadians(18)), COS18 = (float) Math.cos(Math.toRadians(18));
    private static final float BACKDROP_TEXELS = 160f;
    private Bitmap backdrop, backdropRing;
    private android.graphics.BitmapShader backdropShader;
    private final Paint backdropPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint backdropRoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix backdropMatrix = new Matrix();
    private final RectF backdropDst = new RectF();
    private float backdropCell;
    private boolean backdropValid, backdropRingOn;
    private int backdropCount;
    private final float[] bdGeom = new float[14], bdGeomDrawn = new float[14];
    private final int[] bdColors = new int[7], bdColorsDrawn = new int[7];
    private final float[] bdWave = new float[15], bdWaveDrawn = new float[15];
    private void drawBackdrop(Canvas c, int w, int h, boolean waving, float corner) {
        if (scene == null || w <= 0 || h <= 0) {
            c.drawColor(Color.BLACK);
            return;
        }
        final float cell = Math.max(w, h) / BACKDROP_TEXELS;
        final int nx = (int) Math.ceil(w / cell) + 1, ny = (int) Math.ceil(h / cell) + 1;
        if (backdrop == null || backdrop.getWidth() != nx || backdrop.getHeight() != ny) {
            final Bitmap next = Bitmap.createBitmap(nx, ny, Bitmap.Config.ARGB_8888);
            final Bitmap nextRing;
            try { nextRing = Bitmap.createBitmap(nx, ny, Bitmap.Config.ARGB_8888); }
            catch (Throwable failure) { next.recycle(); throw failure; }
            releaseBitmaps();
            backdrop = next;
            backdropRing = nextRing;
            backdropShader = new android.graphics.BitmapShader(backdrop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            backdropRoundPaint.setShader(backdropShader);
            backdropValid = false;
        }
        backdropCell = cell;
        putBackdropScene(0, waving ? oldScene : scene, w, h);
        backdropRingOn = false;
        if (waving) {
            putBackdropScene(1, scene, w, h);
            putBackdropWave(w, h);
        }
        final int count = waving ? 2 : 1;
        if (!backdropValid || count != backdropCount || !java.util.Arrays.equals(bdGeom, bdGeomDrawn)
            || !java.util.Arrays.equals(bdColors, bdColorsDrawn) || waving && !java.util.Arrays.equals(bdWave, bdWaveDrawn)) {
            QrBackdropNative.generate(backdrop, backdropRingOn ? backdropRing : null, cell, count, bdGeom, bdColors, bdWave);
            System.arraycopy(bdGeom, 0, bdGeomDrawn, 0, bdGeom.length);
            System.arraycopy(bdColors, 0, bdColorsDrawn, 0, bdColors.length);
            System.arraycopy(bdWave, 0, bdWaveDrawn, 0, bdWave.length);
            backdropCount = count;
            backdropValid = true;
        }
        if (corner > 0f) {
            // the open / close: rounded screen corners from the backdrop itself (no outline clip over a layer)
            backdropMatrix.setScale(cell, cell);
            backdropMatrix.postTranslate(-cell / 2f, -cell / 2f);
            backdropShader.setLocalMatrix(backdropMatrix);
            c.drawRoundRect(0, 0, w, h, corner, corner, backdropRoundPaint);
        } else {
            drawBackdropBitmap(c, backdrop);
        }
    }

    /** Texel (i, j) lands on the view point (i·cell, j·cell). */
    private void drawBackdropBitmap(Canvas c, Bitmap b) {
        final float cell = backdropCell;
        backdropDst.set(-cell / 2f, -cell / 2f, (b.getWidth() - 0.5f) * cell, (b.getHeight() - 0.5f) * cell);
        c.drawBitmap(b, null, backdropDst, backdropPaint);
    }

    /** 7 floats per scene: type (0 linear, 1 radial), origin / centre, direction, 1 / length or radius, middle stop. */
    private void putBackdropScene(int k, Scene s, int w, int h) {
        final int g = k * 7;
        if (s.type == SCENE_COLOR && s.index == TUNED_COLOR) {
            // a plain linear gradient ~18° off vertical: light in the top-left corner, darkest in the bottom-right
            // corner at the screen edge; no hotspot, no drift
            bdGeom[g] = 0f;
            bdGeom[g + 1] = 0f;
            bdGeom[g + 2] = 0f;
            bdGeom[g + 3] = SIN18;
            bdGeom[g + 4] = COS18;
            bdGeom[g + 5] = 1f / (w * SIN18 + h * COS18);
            bdGeom[g + 6] = 0.5f;
        } else {
            // gifts: their centre → edge radial, centred on the QR (no off-centre dark spot)
            bdGeom[g] = 1f;
            bdGeom[g + 1] = w / 2f;
            bdGeom[g + 2] = cardCY;
            bdGeom[g + 3] = 0f;
            bdGeom[g + 4] = 0f;
            bdGeom[g + 5] = 1f / ((float) Math.hypot(w, h) * 0.62f);
            bdGeom[g + 6] = 0.55f;
        }
        bdColors[k * 3] = s.center;
        bdColors[k * 3 + 1] = s.mid;
        bdColors[k * 3 + 2] = s.edge;
    }

    /** The new scene's circle, and its glowing ring: a bright rim with a soft refraction band inside. */
    private void putBackdropWave(int w, int h) {
        final float e = waveEase(wave);
        final float maxR = (float) Math.hypot(Math.max(waveX, w - waveX), Math.max(waveY, h - waveY)) + dp(60);
        final float r = maxR * e;
        bdWave[0] = waveX;
        bdWave[1] = waveY;
        bdWave[2] = r;
        backdropRingOn = r > dp(4);
        if (!backdropRingOn) return;
        // a soft glow in a light tint of the new colour (~38% of the old white rim), wider falloff
        final float bandW = lerp(dp(90), dp(56), e);
        final float a = clamp01((1f - wave) * 1.6f) * 0.38f;
        final float inner = Math.max(0, r - bandW) / r, rr = r + dp(8);
        bdWave[3] = inner * r;
        bdWave[4] = lerp(inner, 1f, 0.45f) * r;
        bdWave[5] = lerp(inner, 1f, 0.82f) * r;
        bdWave[6] = r - 0.004f * rr;
        bdWave[7] = r + dp(3);
        bdWave[8] = rr;
        bdWave[9] = 0f;
        bdWave[10] = 0x33 / 255f * a;
        bdWave[11] = 0x99 / 255f * a;
        bdWave[12] = a;
        bdWave[13] = 0x66 / 255f * a;
        bdWave[14] = 0f;
        bdColors[6] = QrCodeLayer.mixOklab(scene.center, Color.WHITE, 0.55f);
    }

    private final double[] lab1 = new double[3], lab2 = new double[3], labOut = new double[3];

    private int blendLab(int a, int b, float t) {
        ColorUtils.colorToLAB(a, lab1);
        ColorUtils.colorToLAB(b, lab2);
        ColorUtils.blendLAB(lab1, lab2, t, labOut);
        return ColorUtils.LABToColor(labOut[0], labOut[1], labOut[2]);
    }

    private static float waveEase(float t) {
        // quick start, long glide: the ring is still visible when it crosses the far corners
        return 1f - (float) Math.pow(1f - t, 2.4f);
    }


    private static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }

    private void releaseBitmaps() {
        if (backdrop != null) backdrop.recycle();
        if (backdropRing != null) backdropRing.recycle();
        backdrop = backdropRing = null;
        backdropShader = null;
        backdropRoundPaint.setShader(null);
        backdropValid = false;
    }

    private void clearNodes() {
        if (QrRender.hasNodes()) {
            QrRender.discardNode(bgNode);
            QrRender.discardNode(blurNode);
        }
        bgNode = blurNode = null;
        blurBuiltVersion = -1;
    }

    @Override public void close() {
        clearNodes();
        releaseBitmaps();
        shine.close();
        hasEmojiPattern = false;
        glassConsumers.clear();
        oldScene = null;
        wave = 1f;
    }
}

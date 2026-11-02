package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.dpf2;
import static com.eve.app.telegram.components.QrUiUnits.lerp;
import androidx.core.graphics.ColorUtils;

/**
 * ADAPTED original Java QR picker glass sheet lift from official
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15,
 * QrShareView.java:4730-4767,4793-4828.
 * No Telegram account/network state. Copyright Telegram contributors;
 * unresolved GPL distribution obligations in THIRD_PARTY_NOTICES.md.
 */
public final class QrPickerSheetLift {
    private QrPickerSheetLift() {}
    public static final int DOWNSAMPLE = 8;

    /** Source quantization rebuilds RenderEffect only at visible 1/50 steps. */
    public static float quantized(float progress) {
        final float clamped = Float.isFinite(progress) ?
            Math.max(0f, Math.min(1f, progress)) : 0f;
        return Math.round(clamped * 50f) / 50f;
    }

    /** Original source radius ramps from Glass blur to Sheet blur. */
    public static float blurRadius(float progress) {
        final float k = quantized(progress);
        return Math.max(0.5f,
            dpf2(lerp(RimTune.v[RimTune.BLUR], RimTune.v[RimTune.SH_BLUR], k))
            / DOWNSAMPLE);
    }

    /**
     * The original 4x5 RGB contrast/brightness matrix. Light backdrops
     * receive less brightening via (0.85-L)/0.45, never whitewash.
     */
    public static float[] colorMatrix(int center, float progress) {
        final float k = quantized(progress);
        final double lum = ColorUtils.calculateLuminance(center);
        final float brightness = RimTune.v[RimTune.SH_BRIGHT] / 100f * k *
            (float) Math.max(0d, Math.min(1d, (0.85d - lum) / 0.45d));
        final float contrast = 1f + RimTune.v[RimTune.SH_CONTRAST] / 100f * k;
        final float off = 255f * (0.5f - 0.5f * contrast + brightness);
        return new float[]{
            contrast, 0, 0, 0, off,
            0, contrast, 0, 0, off,
            0, 0, contrast, 0, off,
            0, 0, 0, 1, 0
        };
    }
}

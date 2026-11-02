package com.eve.app.telegram.qr;

import static com.eve.app.telegram.components.QrUiUnits.dp;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * ADAPTED original Java reveal equations / whole-layer properties from
 * QrShareView.java:3591-3686 and QrShareActivity.java:819-848,
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * EVE Activity/view/cancellation wiring replaces Telegram fragment transition locks.
 * Original open spring, close curve, durations and control/card scale endpoints
 * retained; interruption continues from the currently displayed state.
 * Telegram contributors; GPL release obligations in THIRD_PARTY_NOTICES.
 */
public final class QrSceneReveal implements AutoCloseable {
    public static final long OPEN_MS = 860L, CLOSE_MS = 200L;
    public interface Frame {
        void apply(float cardScale, float build, boolean active);
    }
    private final View layer;
    private final View[] controls;
    private final Frame frame;
    private ValueAnimator animator;
    private boolean opening = true, closing;
    private float reveal = 1f, revealX = 1f, cardScale = 1f, controlScale = 1f;
    private float closeFromX = 1f, closeCardScale = 1f, closeControlScale = 1f;
    private boolean reducedMotion;

    public QrSceneReveal(View layer, View[] controls, Frame frame) {
        this.layer = layer;
        this.controls = controls.clone();
        this.frame = frame;
    }

    /** Original setReveal body, using only EVE view bindings and frame callback. */
    public void setReveal(float v) {
        reveal = v;
        final float x = revealProgress(v, opening);
        revealX = x;
        final boolean active = v < 1f;
        layer.setTranslationY(active ? Math.round(dp(64) * (1f - x)) : 0f);
        layer.setAlpha(active ? clamp01(x / 0.35f) : 1f);
        if (layer.getParent() instanceof View) {
            ((View) layer.getParent()).setBackgroundColor(active ? ((int) (0x40 * clamp01(x)) << 24) : 0);
        }
        cardScale = !active ? 1f : opening ? 0.9f + 0.1f * x : 0.92f + 0.08f * x;
        controlScale = !active ? 1f : opening ? 0.85f + 0.15f * clamp01(x) : 0.92f + 0.08f * clamp01(x);
        // A separate EVE Activity can receive Back before its open completes.
        // Invert the original close curve and preserve the currently drawn scales.
        if (!opening && closeFromX < 1f) {
            final float fraction = closeFromX <= 0 ? 0 : clamp01(x / closeFromX);
            cardScale = 0.92f + (closeCardScale - 0.92f) * fraction;
            controlScale = 0.92f + (closeControlScale - 0.92f) * fraction;
        }
        for (View control : controls) {
            control.setAlpha(active ? clamp01(x) : 1f);
            control.setScaleX(controlScale);
            control.setScaleY(controlScale);
        }
        final float build = reducedMotion ? 1f : active && opening ? clamp01(x) : -1f;
        frame.apply(cardScale, build, active);
        final int want = active && !reducedMotion ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE;
        if (layer.getLayerType() != want) layer.setLayerType(want, null);
        layer.invalidate();
    }

    /** Canonical upstream spring/close math, shared with EVE motion callers. */
    public static float revealProgress(float v, boolean opening) {
        final float x;
        if (opening) {
            final double t = v * 0.86, z = 0.9, w0 = 2 * Math.PI / 0.62, wd = w0 * Math.sqrt(1 - z * z);
            final float xs = v >= 1f ? 1f : (float) (1 - Math.exp(-z * w0 * t) * (Math.cos(wd * t) + z / Math.sqrt(1 - z * z) * Math.sin(wd * t)));
            final float bu = clamp01((v - 0.85f) / 0.15f), bk = bu * bu * (3f - 2f * bu);
            x = xs + (1f - xs) * bk;
        } else {
            final float u = 1f - v;
            x = 1f - u * u;
        }
        return x;
    }

    /** Restore the scrim after EVE system-bar/theme synchronization. */
    public void refresh() { setReveal(reveal); }

    private void cancelAnimator() {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
            animator = null;
        }
    }

    public void open(boolean motionEnabled) {
        cancelAnimator();
        closing = false;
        opening = true;
        reducedMotion = !motionEnabled;
        if (!motionEnabled) { setReveal(1f); return; }
        setReveal(0f);
        animate(0f, 1f, OPEN_MS, null);
    }

    public void close(boolean motionEnabled, Runnable finished) {
        if (closing) return;
        closing = true;
        cancelAnimator();
        closeFromX = clamp01(revealX);
        closeCardScale = cardScale;
        closeControlScale = controlScale;
        opening = false;
        reducedMotion = !motionEnabled;
        if (!motionEnabled) { setReveal(0f); finished.run(); return; }
        final float from = 1f - (float) Math.sqrt(1f - closeFromX);
        setReveal(from);
        animate(from, 0f, CLOSE_MS, finished);
    }

    private void animate(float from, float to, long duration, Runnable finished) {
        animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(value -> setReveal((float) value.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                animator = null;
                setReveal(to);
                if (finished != null) finished.run();
            }
        });
        animator.start();
    }

    private static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }
    /** Lifecycle disposal cancels without invoking a stale Activity finish callback. */
    @Override public void close() {
        cancelAnimator();
        layer.setLayerType(View.LAYER_TYPE_NONE, null);
        if (layer.getParent() instanceof View) ((View) layer.getParent()).setBackgroundColor(0);
    }
}

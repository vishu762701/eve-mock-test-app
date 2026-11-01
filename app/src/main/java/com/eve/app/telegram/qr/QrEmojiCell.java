package com.eve.app.telegram.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Original Telegram QrEmojiPanel.Cell Java drawing/press algorithm.
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15, lines 517-630.
 * ADAPTED: EVE-owned Drawable and key replace Telegram Document/account services;
 * native animation loading/recycling belongs to the EVE RecyclerView adapter.
 * Telegram contributors; applicable GPL obligations remain in THIRD_PARTY_NOTICES.
 */
public class QrEmojiCell extends View {
    private Drawable drawable;
    private final Paint placeholder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String key;
    private float pressK;
    private boolean motionEnabled = true;
    public float selectScale = 1f;
    public float cascadeScale = 1f;

    public QrEmojiCell(Context context) {
        super(context);
        placeholder.setColor(0x2BFFFFFF);
        setClickable(true);
        setFocusable(true);
    }

    public String getKey() { return key; }
    public void setKey(String value) { key = value; }

    public void setMotionEnabled(boolean enabled) {
        motionEnabled = enabled;
        updatePlayback();
        invalidate();
    }

    public void setFrameDrawable(Drawable value) {
        if (drawable == value) return;
        if (drawable != null) {
            if (drawable instanceof Animatable) ((Animatable) drawable).stop();
            drawable.setCallback(null);
        }
        drawable = value;
        if (drawable != null) drawable.setCallback(this);
        updatePlayback();
        invalidate();
    }

    @Override protected boolean verifyDrawable(Drawable who) {
        return who == drawable || super.verifyDrawable(who);
    }

    @Override public void invalidateDrawable(Drawable who) {
        super.invalidateDrawable(who);
        // The same frames are also drawn in the source picked-items island.
        if (getParent() instanceof View) ((View) getParent()).invalidate();
    }

    private void updatePlayback() {
        if (drawable instanceof Animatable) {
            if (motionEnabled && isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown()) {
                ((Animatable) drawable).start();
            } else {
                ((Animatable) drawable).stop();
            }
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePlayback();
    }
    @Override protected void onDetachedFromWindow() {
        if (drawable instanceof Animatable) ((Animatable) drawable).stop();
        super.onDetachedFromWindow();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updatePlayback();
    }
    @Override protected void onVisibilityChanged(View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility);
        updatePlayback();
    }
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int w = MeasureSpec.getSize(widthMeasureSpec);
            setMeasuredDimension(w, w);
        }

        @Override
        public void setPressed(boolean pressed) {
            if (pressed != isPressed()) {
                super.setPressed(pressed);
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final float target = isPressed() ? 1f : 0f;
            if (!motionEnabled) pressK = target;
            if (pressK != target) {
                pressK = Math.abs(pressK - target) < 0.02f ? target : pressK + (target - pressK) * 0.35f;
                invalidate();
            }
            final float scale = (1f - 0.2f * pressK) * selectScale * cascadeScale;
            if (scale <= 0.01f) {
                return;
            }
            final float size = getWidth() * 0.78f * scale;
            final float cx = getWidth() / 2f, cy = getHeight() / 2f;
            final boolean loaded = drawable != null;
            if (!loaded) {
                canvas.drawCircle(cx, cy, size * 0.42f, placeholder);
            }
            if (drawable != null) {
                drawable.setBounds((int) (cx - size / 2f), (int) (cy - size / 2f), (int) (cx + size / 2f), (int) (cy + size / 2f));
                drawable.draw(canvas);
            }
        }

}

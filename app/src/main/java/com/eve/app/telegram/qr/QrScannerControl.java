package com.eve.app.telegram.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import com.eve.app.designsystem.DesignMotion;

/** ADAPTED original Java QrScanOverlay.java:689-701,770-802,867-920 at
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Telegram contributors; root GPLv2 and release review apply.
 * EVE native View owns accessibility, keyboard clicks and a >=48dp hit target;
 * source visual shape, press convergence, torch fill and icons remain Java. */
public final class QrScannerControl extends View {
    private final Drawable icon;
    private final QrScannerGlass glass;
    private final boolean torch;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF body = new RectF();
    private final float density;
    private float diameter, compositionScale = 1f, press = 1f, torchK;
    private boolean tracking, armed;

    public QrScannerControl(Context context, Drawable icon, boolean torch, int diameterDp, QrScannerGlass glass) {
        super(context);
        this.icon = icon.mutate(); this.torch = torch; this.glass = glass;
        density = getResources().getDisplayMetrics().density;
        diameter = (float) Math.ceil(diameterDp*density);
        icon.setCallback(this);
        glass.setCallback(this);
        setClickable(true); setFocusable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setCompositionScale(float scale) {
        if (Float.isFinite(scale) && scale > 0 && compositionScale != scale) {
            compositionScale = scale; invalidate();
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final boolean motion = DesignMotion.INSTANCE.enabled(getContext());
        press = approach(press, isPressed() ? .88f : 1f, motion);
        torchK = approach(torchK, torch && isSelected() ? 1f : 0f, motion);
        final float side = diameter * compositionScale;
        body.set((getWidth()-side)/2f, (getHeight()-side)/2f,
                (getWidth()+side)/2f, (getHeight()+side)/2f);
        final int save = canvas.save();
        canvas.scale(press, press, body.centerX(), body.centerY());
        glass.setBounds(Math.round(body.left), Math.round(body.top), Math.round(body.right), Math.round(body.bottom));
        glass.draw(canvas);
        if (torch && torchK > 0) {
            fill.setColor(0xFFFFFFFF);
            fill.setAlpha((int) (0x66 * torchK));
            canvas.drawRoundRect(body, side/2f, side/2f, fill);
        }
        final int size = Math.round((icon instanceof BackDrawable ? 24 : 28) * density * compositionScale);
        icon.setBounds((int)(body.centerX()-size/2f), (int)(body.centerY()-size/2f),
                (int)(body.centerX()+size/2f), (int)(body.centerY()+size/2f));
        // BackDrawable source uses its intrinsic origin rather than bounds.
        if (icon instanceof BackDrawable) {
            canvas.translate(body.centerX()-icon.getIntrinsicWidth()*compositionScale/2f,
                    body.centerY()-icon.getIntrinsicHeight()*compositionScale/2f);
            canvas.scale(compositionScale, compositionScale);
        }
        icon.draw(canvas);
        canvas.restoreToCount(save);
    }

    private float approach(float value, float target, boolean motion) {
        if (!motion || Math.abs(value-target) < .005f) return target;
        postInvalidateOnAnimation();
        return value + (target-value)*.3f;
    }

    @Override protected void drawableStateChanged() { super.drawableStateChanged(); invalidate(); }
    @Override protected boolean verifyDrawable(Drawable drawable) {
        return drawable == icon || drawable == glass || super.verifyDrawable(drawable);
    }
    @Override public CharSequence getAccessibilityClassName() { return Button.class.getName(); }
    @Override public boolean performClick() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        return super.performClick();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || !isClickable()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                tracking = armed = true; setPressed(true); return true;
            case MotionEvent.ACTION_MOVE:
                if (tracking && (event.getX()<0 || event.getY()<0 || event.getX()>getWidth() || event.getY()>getHeight())) {
                    armed = false; setPressed(false);
                }
                return tracking;
            case MotionEvent.ACTION_UP:
                boolean click = tracking && armed && event.getX() >= 0 && event.getX() <= getWidth()
                        && event.getY() >= 0 && event.getY() <= getHeight();
                tracking = armed = false; setPressed(false);
                if (click) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                tracking = armed = false; setPressed(false); return true;
            default: return tracking;
        }
    }
    @Override protected void onDetachedFromWindow() {
        tracking = armed = false; setPressed(false); press = 1f; torchK = isSelected() ? 1f : 0f;
        super.onDetachedFromWindow();
    }
}

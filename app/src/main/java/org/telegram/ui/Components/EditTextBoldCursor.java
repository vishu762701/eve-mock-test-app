// EVE-SHIM: minimal EditTextBoldCursor foundation with real cursor & underline physics
package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import androidx.appcompat.widget.AppCompatEditText;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("AppCompatCustomView")
public class EditTextBoldCursor extends AppCompatEditText {
    private float cursorWidth = 1.5f;
    private int cursorSize = 20;
    private int cursorColor = 0xff000000;
    private final Paint cursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean lineVisible = false;
    private int lineColor = 0;
    private int activeLineColor = 0;
    private int errorLineColor = 0;
    private final Paint linePaint = new Paint();

    private boolean lineActive = false;
    private float lineActiveness = 0.0f;
    private float lastLineActiveness = 0.0f;
    private long lineLastUpdateTime = 0;
    private CharSequence errorText;

    private long lastUpdateTime;
    private float cursorAlpha = 1.0f;
    private boolean alphaForward = false;
    private final Rect rect = new Rect();

    public EditTextBoldCursor(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        cursorPaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.FILL);
    }

    protected Theme.ResourcesProvider getResourcesProvider() {
        return null;
    }

    public void setCursorColor(int color) {
        this.cursorColor = color;
        cursorPaint.setColor(color);
        invalidate();
    }

    public void setCursorSize(int size) {
        this.cursorSize = size;
        invalidate();
    }

    public void setCursorWidth(float width) {
        this.cursorWidth = width;
        invalidate();
    }

    public void setLineColors(int normal, int active, int error) {
        this.lineVisible = true;
        this.lineColor = normal;
        this.activeLineColor = active;
        this.errorLineColor = error;
        linePaint.setColor(normal);
        invalidate();
    }

    public void setErrorText(CharSequence text) {
        this.errorText = text;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Underline drawing with 150ms interpolation
        if (lineVisible && lineColor != 0) {
            int lineWidth = AndroidUtilities.dp(1);
            boolean wasLineActive = lineActive;
            if (!TextUtils.isEmpty(errorText)) {
                linePaint.setColor(errorLineColor);
                lineWidth = AndroidUtilities.dp(2);
                lineActive = false;
            } else if (isFocused()) {
                linePaint.setColor(activeLineColor);
                lineWidth = AndroidUtilities.dp(2);
                lineActive = true;
            } else {
                linePaint.setColor(lineColor);
                lineActive = false;
            }

            if (lineActive != wasLineActive) {
                lineLastUpdateTime = SystemClock.elapsedRealtime();
                lastLineActiveness = lineActiveness;
            }

            float t = (SystemClock.elapsedRealtime() - lineLastUpdateTime) / 150.0f;
            if (t < 1f || lineActive && lineActiveness != 1.0f || !lineActive && lineActiveness != 0.0f) {
                lineActiveness = AndroidUtilities.lerp(lastLineActiveness, lineActive ? 1 : 0, Math.max(0, Math.min(1, t)));
                if (t < 1f) {
                    invalidate();
                }
            }

            int bottom = getMeasuredHeight() - AndroidUtilities.dp(2);
            canvas.drawRect(getScrollX(), bottom - lineWidth, getScrollX() + getMeasuredWidth(), bottom, linePaint);
        }

        // Custom bold cursor with pulse alpha animation
        if (isFocused() && cursorWidth > 0 && getLayout() != null) {
            long newTime = SystemClock.elapsedRealtime();
            long dt = newTime - lastUpdateTime;
            lastUpdateTime = newTime;

            if (alphaForward) {
                cursorAlpha += dt / 500.0f;
                if (cursorAlpha >= 1.0f) {
                    cursorAlpha = 1.0f;
                    alphaForward = false;
                }
            } else {
                cursorAlpha -= dt / 500.0f;
                if (cursorAlpha <= 0.0f) {
                    cursorAlpha = 0.0f;
                    alphaForward = true;
                }
            }
            cursorPaint.setColor(cursorColor);
            cursorPaint.setAlpha((int) (255 * cursorAlpha));

            try {
                int pos = getSelectionStart();
                if (pos >= 0) {
                    int line = getLayout().getLineForOffset(pos);
                    float primaryHorizontal = getLayout().getPrimaryHorizontal(pos);
                    int top = getLayout().getLineTop(line);
                    int bottom = getLayout().getLineBottom(line);
                    int height = bottom - top;
                    if (cursorSize > 0) {
                        top = top + (height - cursorSize) / 2;
                        bottom = top + cursorSize;
                    }
                    float w = AndroidUtilities.dpf2(cursorWidth);
                    rect.set((int) primaryHorizontal, top, (int) (primaryHorizontal + w), bottom);
                    canvas.drawRect(rect, cursorPaint);
                }
            } catch (Exception ignored) {
            }
            invalidate();
        }
    }
}

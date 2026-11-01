package com.eve.app.telegram.components;

import android.content.res.Resources;
import android.graphics.RectF;
import android.graphics.Typeface;

/** EVE process/display adapter for the source UI primitives. No Telegram account state. */
public final class QrUiUnits {
    public static final RectF rectTmp = new RectF();
    public static int dp(float value) {
        return value == 0 ? 0 : (int) Math.ceil(Resources.getSystem().getDisplayMetrics().density * value);
    }
    public static float dpf2(float value) { return Resources.getSystem().getDisplayMetrics().density * value; }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    public static Typeface bold() { return Typeface.create("sans-serif-medium", Typeface.NORMAL); }
}

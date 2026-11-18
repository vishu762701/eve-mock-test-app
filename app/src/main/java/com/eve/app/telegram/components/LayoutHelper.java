package com.eve.app.telegram.components;

import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** Required layout overloads from upstream LayoutHelper; the EVE display adapter supplies dp. */
public final class LayoutHelper {
    public static final int MATCH_PARENT = -1, WRAP_CONTENT = -2;

    private static int getSize(float size) {
        return (int) (size < 0 ? size : QrUiUnits.dp(size));
    }

    public static LinearLayout.LayoutParams createLinear(int width, int height,
            float left, float top, float right, float bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                width < 0 ? width : QrUiUnits.dp(width), height < 0 ? height : QrUiUnits.dp(height));
        p.setMargins(QrUiUnits.dp(left), QrUiUnits.dp(top), QrUiUnits.dp(right), QrUiUnits.dp(bottom));
        return p;
    }

    public static FrameLayout.LayoutParams createFrame(int width, float height) {
        return new FrameLayout.LayoutParams(getSize(width), getSize(height));
    }

    public static FrameLayout.LayoutParams createFrame(int width, int height, int gravity) {
        return new FrameLayout.LayoutParams(getSize(width), getSize(height), gravity);
    }
}

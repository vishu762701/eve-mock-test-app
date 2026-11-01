/* Source-extracted FrameLayout helpers from Telegram LayoutHelper.java; GPL v2 or later. */
package com.eve.app.ui.profile.popup;
import android.widget.FrameLayout;
public final class LayoutHelper {
    public static final int WRAP_CONTENT = -2, MATCH_PARENT = -1;

    private static int getSize(float size) {
        return (int) (size < 0 ? size : PopupPlatform.dp(size));
    }
    public static FrameLayout.LayoutParams createFrame(int width, float height) {
        return new FrameLayout.LayoutParams(getSize(width), getSize(height));
    }
    public static FrameLayout.LayoutParams createFrame(int width, int height, int gravity) {
        return new FrameLayout.LayoutParams(getSize(width), getSize(height), gravity);
    }
}

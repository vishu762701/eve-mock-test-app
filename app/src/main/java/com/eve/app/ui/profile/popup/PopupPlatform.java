/* Source-extracted UI math from Telegram AndroidUtilities.java; GPL v2 or later.
 * EVE supplies the host density; no Telegram ApplicationLoader or account state. */
package com.eve.app.ui.profile.popup;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.core.math.MathUtils;
public final class PopupPlatform {
    public static float density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
    public static final RectF rectTmp = new RectF();
    public static final Rect rectTmp2 = new Rect();

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }
    public static float cascade(float fullAnimationT, float position, float count, float waveLength) {
        if (count <= 0) return fullAnimationT;
        final float waveDuration = 1f / count * Math.min(waveLength, count);
        final float waveOffset = position / count * (1f - waveDuration);
        return MathUtils.clamp((fullAnimationT - waveOffset) / waveDuration, 0, 1);
    }
    public static void lerp(Rect a, Rect b, float f, Rect to) {
        if (to != null) {
            to.set(
                lerp(a.left, b.left, f),
                lerp(a.top, b.top, f),
                lerp(a.right, b.right, f),
                lerp(a.bottom, b.bottom, f)
            );
        }
    }
    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }
    public static int lerp(int a, int b, float f) {
        return (int) (a + f * (b - a));
    }
}

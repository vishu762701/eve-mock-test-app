// EVE-SHIM: framework utility shim for EVE (exact method signatures & dp/lerp behavior from upstream AndroidUtilities.java)
package org.telegram.messenger;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import java.util.Hashtable;

public class AndroidUtilities {

    public static float density = 2.0f;
    public static Point displaySize = new Point(1080, 1920);
    public static Rect rectTmp = new Rect();
    public static RectF rectFTmp = new RectF();

    private static final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final Hashtable<String, Typeface> typefaceCache = new Hashtable<>();

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static int dp(int value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static float dpf2(float value) {
        if (value == 0) {
            return 0;
        }
        return density * value;
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (runnable == null) {
            return;
        }
        if (delay == 0) {
            uiHandler.post(runnable);
        } else {
            uiHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        uiHandler.removeCallbacks(runnable);
    }

    public static boolean isTablet() {
        return false;
    }

    public static void checkDisplaySize(Context context, Configuration newConfiguration) {
        try {
            if (context != null && context.getResources() != null) {
                density = context.getResources().getDisplayMetrics().density;
            }
        } catch (Exception ignored) {
        }
    }

    public static Typeface getTypeface(String assetPath) {
        synchronized (typefaceCache) {
            try {
                if (!typefaceCache.containsKey(assetPath)) {
                    Typeface typeface = Typeface.createFromFile(assetPath);
                    if (typeface != null) {
                        typefaceCache.put(assetPath, typeface);
                    }
                }
                return typefaceCache.get(assetPath);
            } catch (Exception ignored) {
                return Typeface.DEFAULT;
            }
        }
    }
}

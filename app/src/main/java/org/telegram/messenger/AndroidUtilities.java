// EVE-SHIM: minimal AndroidUtilities foundation for Telegram screen ports
package org.telegram.messenger;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import java.util.HashMap;

public class AndroidUtilities {
    public static float density = 1.0f;
    public static Point displaySize = new Point();
    private static final Handler mHandler = new Handler(Looper.getMainLooper());
    private static final HashMap<String, Typeface> typefaces = new HashMap<>();

    static {
        try {
            density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
            displaySize.x = android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels;
            displaySize.y = android.content.res.Resources.getSystem().getDisplayMetrics().heightPixels;
        } catch (Exception ignored) {
            density = 1.0f;
        }
    }

    public static boolean isTablet() {
        return false;
    }

    public static void checkDisplaySize(Context context, android.content.res.Configuration newConfiguration) {
        try {
            if (context != null && context.getResources() != null) {
                density = context.getResources().getDisplayMetrics().density;
                displaySize.x = context.getResources().getDisplayMetrics().widthPixels;
                displaySize.y = context.getResources().getDisplayMetrics().heightPixels;
            }
        } catch (Exception ignored) {
        }
    }

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

    public static void showKeyboard(View view) {
        if (view == null) {
            return;
        }
        try {
            InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        } catch (Exception ignored) {
        }
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            mHandler.post(runnable);
        } else {
            mHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        mHandler.removeCallbacks(runnable);
    }

    public static float lerp(float a, float b, float f) {
        return a + f * (b - a);
    }

    public static Typeface getTypeface(String assetPath) {
        synchronized (typefaces) {
            if (!typefaces.containsKey(assetPath)) {
                try {
                    Typeface tf = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
                    typefaces.put(assetPath, tf);
                } catch (Exception ignored) {
                    return Typeface.DEFAULT;
                }
            }
            return typefaces.get(assetPath);
        }
    }
}

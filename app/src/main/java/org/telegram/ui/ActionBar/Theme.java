// EVE-SHIM: framework theme shim for EVE (exact method signatures & palette roles from upstream Theme.java)
package org.telegram.ui.ActionBar;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;

public class Theme {

    public interface ResourcesProvider {
        int getColor(int key);

        default int getColorOrDefault(int key) {
            return getColor(key);
        }

        default int getCurrentColor(int key) {
            return getColor(key);
        }

        default boolean isCompactMode() {
            return false;
        }

        default Drawable getDrawable(String drawableKey) {
            return null;
        }

        default Paint getPaint(String paintKey) {
            return null;
        }
    }

    public static int colorsCount = 0;

    public static final int key_actionBarDefault = colorsCount++;
    public static final int key_actionBarDefaultTitle = colorsCount++;
    public static final int key_actionBarDefaultIcon = colorsCount++;
    public static final int key_actionBarDefaultSelector = colorsCount++;
    public static final int key_windowBackgroundWhite = colorsCount++;
    public static final int key_windowBackgroundGray = colorsCount++;
    public static final int key_windowBackgroundWhiteBlackText = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText = colorsCount++;
    public static final int key_windowBackgroundWhiteGrayText2 = colorsCount++;
    public static final int key_windowBackgroundWhiteBlueHeader = colorsCount++;
    public static final int key_divider = colorsCount++;
    public static final int key_listSelector = colorsCount++;
    public static final int key_voipgroup_dialogBackground = colorsCount++;
    public static final int key_windowBackgroundGrayShadow = colorsCount++;

    public static Paint dividerPaint = new Paint();

    static {
        dividerPaint.setStrokeWidth(1);
        dividerPaint.setColor(0x1F000000);
    }

    public static int getColor(int key) {
        return getColor(key, null);
    }

    public static int getColor(int key, ResourcesProvider provider) {
        if (provider != null) {
            return provider.getColor(key);
        }
        if (key == key_divider) {
            return 0x1F000000;
        } else if (key == key_windowBackgroundWhite) {
            return 0xFFFFFFFF;
        } else if (key == key_windowBackgroundGray || key == key_windowBackgroundGrayShadow) {
            return 0xFFF0F2F5;
        } else if (key == key_windowBackgroundWhiteBlackText) {
            return 0xFF222222;
        } else if (key == key_windowBackgroundWhiteGrayText || key == key_windowBackgroundWhiteGrayText2) {
            return 0xFF8E8E93;
        } else if (key == key_windowBackgroundWhiteBlueHeader) {
            return 0xFF007AFF;
        } else if (key == key_actionBarDefault) {
            return 0xFFFFFFFF;
        } else if (key == key_actionBarDefaultTitle) {
            return 0xFF000000;
        } else if (key == key_voipgroup_dialogBackground) {
            return 0xFF1C1C1E;
        }
        return 0xFF000000;
    }

    public static Drawable getSelectorDrawable(boolean whiteBackground) {
        int color = whiteBackground ? 0x0F000000 : 0x1F000000;
        return new RippleDrawable(ColorStateList.valueOf(color), null, new ColorDrawable(Color.WHITE));
    }

    public static Drawable createSimpleSelectorRoundRectDrawable(int rad, int defaultColor, int selectorColor) {
        GradientDrawable defaultDrawable = new GradientDrawable();
        defaultDrawable.setCornerRadius(rad);
        defaultDrawable.setColor(defaultColor);
        return new RippleDrawable(ColorStateList.valueOf(selectorColor), defaultDrawable, defaultDrawable);
    }

    public static Drawable createSelectorWithBackgroundDrawable(int backgroundColor, int selectorColor) {
        ColorDrawable background = new ColorDrawable(backgroundColor);
        return new RippleDrawable(ColorStateList.valueOf(selectorColor), background, background);
    }
}

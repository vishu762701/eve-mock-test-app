// EVE-SHIM: minimal Theme foundation for Telegram screen ports
package org.telegram.ui.ActionBar;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;

public class Theme {
    public static final int key_dialogBackground = 1;
    public static final int key_windowBackgroundWhite = 2;
    public static final int key_actionBarDefault = 3;
    public static final int key_actionBarDefaultIcon = 4;
    public static final int key_actionBarDefaultTitle = 5;
    public static final int key_actionBarDefaultSelector = 6;
    public static final int key_avatar_actionBarSelectorBlue = 7;
    public static final int key_windowBackgroundWhiteHintText = 8;
    public static final int key_windowBackgroundWhiteBlackText = 9;
    public static final int key_windowBackgroundWhiteInputField = 10;
    public static final int key_windowBackgroundWhiteInputFieldActivated = 11;
    public static final int key_text_RedRegular = 12;

    public interface ResourcesProvider {
        default int getColor(int key) {
            return Theme.getColor(key);
        }
        default int getColorOrDefault(int key) {
            return getColor(key);
        }
        default Paint getPaint(String paintKey) {
            return null;
        }
        default Drawable getDrawable(String drawableKey) {
            return null;
        }
    }

    public static int getColor(int key) {
        return getColor(key, null);
    }

    public static int getColor(int key, ResourcesProvider resourcesProvider) {
        if (resourcesProvider != null) {
            return resourcesProvider.getColor(key);
        }
        return getDefaultColor(key);
    }

    public static int getDefaultColor(int key) {
        switch (key) {
            case key_actionBarDefault:
                return 0xff527da3;
            case key_actionBarDefaultIcon:
            case key_actionBarDefaultTitle:
                return 0xffffffff;
            case key_actionBarDefaultSelector:
            case key_avatar_actionBarSelectorBlue:
                return 0x2f000000;
            case key_windowBackgroundWhite:
                return 0xffffffff;
            case key_windowBackgroundWhiteBlackText:
                return 0xff222222;
            case key_windowBackgroundWhiteHintText:
                return 0xffa8a8a8;
            case key_windowBackgroundWhiteInputField:
                return 0xffdbdbdb;
            case key_windowBackgroundWhiteInputFieldActivated:
                return 0xff37a9f0;
            case key_text_RedRegular:
                return 0xffd32f2f;
            default:
                return 0xff000000;
        }
    }
}

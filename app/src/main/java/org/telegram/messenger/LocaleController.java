// EVE-SHIM: minimal LocaleController foundation for Telegram screen ports
package org.telegram.messenger;

import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import java.util.Locale;

public class LocaleController {
    public static boolean isRTL = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;

    public static String getString(int resId) {
        try {
            com.eve.app.EveApplication app = com.eve.app.EveApplication.Companion.getInstance();
            if (app != null) {
                return app.getString(resId);
            }
        } catch (Throwable ignored) {
        }
        return "";
    }
}

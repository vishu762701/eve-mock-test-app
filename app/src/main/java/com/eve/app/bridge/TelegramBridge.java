package com.eve.app.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.StringRes;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import com.eve.app.R;
import java.util.Locale;

/**
 * CUT-INTERFACE adapter bridge between org.telegram framework and Eve app environment.
 * Complies with LAW 2, LAW 3, R4, R5, and EVE_CAMPAIGN section 0c/3.
 * Eliminates runtime reflection (getIdentifier), wires shared preferences cleanly,
 * and handles system RTL resolution.
 */
public final class TelegramBridge {
    private TelegramBridge() {}

    /**
     * Resolves the primary shared preferences container for Telegram framework settings.
     */
    public static SharedPreferences getMainSettings(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences("mainconfig", Context.MODE_PRIVATE);
    }

    /**
     * Checks if the active locale is Right-To-Left (RTL).
     */
    public static boolean isRTL() {
        return TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Resolves localized Telegram string resources without reflection (no getIdentifier hack).
     * Dispatches stringRes if provided, or maps known Telegram keys to Eve R.string resources.
     */
    public static String resolveString(Context context, String key, @StringRes int stringRes, String fallback) {
        if (context == null) {
            return null;
        }
        if (stringRes != 0) {
            try {
                return context.getString(stringRes);
            } catch (Exception ignored) {}
        }
        if (key != null) {
            switch (key) {
                case "EditName":
                    return context.getString(R.string.EditName);
                case "Done":
                    return context.getString(R.string.Done);
                case "FirstName":
                    return context.getString(R.string.FirstName);
                case "LastName":
                    return context.getString(R.string.LastName);
                default:
                    break;
            }
        }
        if (fallback != null) {
            return resolveString(context, fallback, 0, null);
        }
        return null;
    }
}

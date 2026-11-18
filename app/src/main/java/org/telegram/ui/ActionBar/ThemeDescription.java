// EVE-SHIM: minimal ThemeDescription for Telegram theme descriptors
package org.telegram.ui.ActionBar;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

public class ThemeDescription {
    public static final int FLAG_BACKGROUND = 1;
    public static final int FLAG_AB_ITEMSCOLOR = 2;
    public static final int FLAG_AB_TITLECOLOR = 4;
    public static final int FLAG_AB_SELECTORCOLOR = 8;
    public static final int FLAG_TEXTCOLOR = 16;
    public static final int FLAG_HINTTEXTCOLOR = 32;
    public static final int FLAG_BACKGROUNDFILTER = 64;
    public static final int FLAG_DRAWABLESELECTEDSTATE = 128;

    public ThemeDescription(View view, int changeFlags, Class[] listClasses, Paint[] paint, Drawable[] drawables, Object delegate, int key) {
    }
}

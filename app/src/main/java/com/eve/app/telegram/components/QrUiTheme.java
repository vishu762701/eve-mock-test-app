package com.eve.app.telegram.components;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

/** EVE theme bridge; source rendering reads its actual current host palette through the provider. */
public final class QrUiTheme {
    public static final int key_glass_defaultIcon = 1;
    public static final int RIPPLE_MASK_CIRCLE_20DP = 1;
    public interface ResourcesProvider { int getColor(int key); }
    public static int getColor(int key, ResourcesProvider provider) { return provider.getColor(key); }
    public static Drawable createSelectorDrawable(int color, int mask, int radius) {
        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(Color.WHITE);
        return new RippleDrawable(ColorStateList.valueOf(color), new ColorDrawable(Color.TRANSPARENT), circle);
    }
    public static void setSelectorDrawableColor(Drawable drawable, int color, boolean ripple) {
        if (drawable instanceof RippleDrawable) ((RippleDrawable) drawable).setColor(ColorStateList.valueOf(color));
        else drawable.setTint(color);
    }
    public static void setRippleDrawableForceSoftware(RippleDrawable ripple) {
        // Theme.java:9161-9172 at the pinned Telegram revision, including its fallback.
        if (ripple == null) return;
        try {
            java.lang.reflect.Method method = RippleDrawable.class.getDeclaredMethod("setForceSoftware", boolean.class);
            method.invoke(ripple, true);
        } catch (Throwable ignored) {
            // Upstream retains platform rendering when this optional hint is unavailable.
        }
    }
}

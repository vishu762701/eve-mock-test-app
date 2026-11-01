/* Telegram Theme.java ripple mask and BaseCell.java safe ripple draw; GPL v2 or later.
 * EVE adapter: theme key provider and context resources, no Telegram theme singleton. */
package com.eve.app.ui.profile.popup;
import static com.eve.app.ui.profile.popup.PopupPlatform.dp;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.util.StateSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
public final class Theme {
    public interface ResourcesProvider { int getColor(int key); }
    public static final int key_actionBarDefaultSubmenuBackground=0, key_actionBarDefaultSubmenuItem=1,
        key_actionBarDefaultSubmenuItemIcon=2, key_dialogButtonSelector=3,
        key_actionBarDefaultSubmenuSeparator=4, key_windowBackgroundGrayShadow=5;
    private static final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static int getColor(int key, ResourcesProvider provider) {
        if (provider == null) throw new IllegalStateException("EVE popup requires an explicit theme provider");
        return provider.getColor(key);
    }
    public static Drawable getThemedDrawable(Context context, int resId, int color) {
        Drawable drawable = androidx.core.content.ContextCompat.getDrawable(context, resId).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        return drawable;
    }

    public static class RippleRadMaskDrawable extends Drawable {
        private Path path = new Path();
        private float[] radii = new float[8];
        boolean invalidatePath = true;

        public RippleRadMaskDrawable(float top, float bottom) {
            radii[0] = radii[1] = radii[2] = radii[3] = dp(top);
            radii[4] = radii[5] = radii[6] = radii[7] = dp(bottom);
        }
        public RippleRadMaskDrawable(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            radii[0] = radii[1] = dp(topLeft);
            radii[2] = radii[3] = dp(topRight);
            radii[4] = radii[5] = dp(bottomRight);
            radii[6] = radii[7] = dp(bottomLeft);
        }

        public void setRadius(float top, float bottom) {
            radii[0] = radii[1] = radii[2] = radii[3] = dp(top);
            radii[4] = radii[5] = radii[6] = radii[7] = dp(bottom);
            invalidatePath = true;
            invalidateSelf();
        }
        public void setRadius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
            radii[0] = radii[1] = dp(topLeft);
            radii[2] = radii[3] = dp(topRight);
            radii[4] = radii[5] = dp(bottomRight);
            radii[6] = radii[7] = dp(bottomLeft);
            invalidatePath = true;
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            invalidatePath = true;
        }

        @Override
        public void draw(Canvas canvas) {
            if (invalidatePath) {
                invalidatePath = false;
                path.reset();
                PopupPlatform.rectTmp.set(getBounds());
                path.addRoundRect(PopupPlatform.rectTmp, radii, Path.Direction.CW);
            }
            canvas.drawPath(path, maskPaint);
        }

        @Override
        public void setAlpha(int alpha) {

        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {

        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    }
    public static Drawable createRadSelectorDrawable(int color, int topRad, int bottomRad) {
        maskPaint.setColor(0xffffffff);
        Drawable maskDrawable = new RippleRadMaskDrawable(topRad, bottomRad);
        ColorStateList colorStateList = new ColorStateList(
            new int[][] { StateSet.WILD_CARD },
            new int[] { color }
        );
        return new RippleDrawableSafe(colorStateList, null, maskDrawable);
    }
    public static class RippleDrawableSafe extends RippleDrawable {
        public RippleDrawableSafe(@NonNull ColorStateList color, @Nullable Drawable content, @Nullable Drawable mask) {
            super(color, content, mask);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            final int save = canvas.save();
            try {
                super.draw(canvas);
            } catch (Exception e) {
                android.util.Log.e("ProfilePopup", "Ripple drawing failed", e);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }
}

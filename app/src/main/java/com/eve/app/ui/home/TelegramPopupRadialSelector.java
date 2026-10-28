/*
 * Based on the specified UI-only sections of the official Telegram Android source.
 * Copyright Nikolai Kudashov, 2013-2018. GNU GPL version 2 or later.
 *
 * Reference: DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15
 * Theme.java:5296-5357 (RippleRadMaskDrawable), :5387-5395 (selector factory);
 * BaseCell.java:150-165 (RippleDrawableSafe).
 *
 * EVE isolation changes: original dp() -> local Context density; rectTmp/maskPaint
 * -> instance-owned RectF/Paint; Telegram FileLog -> Android Log; wrapper no longer
 * imports Telegram UI/Account/Network classes. Original eight radii, clockwise
 * round-rect path, AA white mask, and drawable-safe canvas guard are retained.
 */
package com.eve.app.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.Log;
import android.util.StateSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Isolated Telegram popup selector atom; NOT the full ActionBarPopupWindow renderer. */
public final class TelegramPopupRadialSelector {
    private TelegramPopupRadialSelector() {}

    public static Drawable create(Context context, int color, int topRadiusDp, int bottomRadiusDp) {
        final Drawable mask = new RippleRadMaskDrawable(context, topRadiusDp, bottomRadiusDp);
        final ColorStateList colors = new ColorStateList(
                new int[][]{StateSet.WILD_CARD}, new int[]{color}
        );
        return new RippleDrawableSafe(colors, null, mask);
    }

    /** Original Theme.RippleRadMaskDrawable path/radii algorithm, isolated from Theme runtime. */
    private static final class RippleRadMaskDrawable extends Drawable {
        private final Path path = new Path();
        private final float[] radii = new float[8];
        private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean invalidatePath = true;

        RippleRadMaskDrawable(Context context, int top, int bottom) {
            final float density = context.getResources().getDisplayMetrics().density;
            radii[0] = radii[1] = radii[2] = radii[3] = top * density;
            radii[4] = radii[5] = radii[6] = radii[7] = bottom * density;
            maskPaint.setColor(Color.WHITE);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            invalidatePath = true;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (invalidatePath) {
                invalidatePath = false;
                path.reset();
                rect.set(getBounds());
                path.addRoundRect(rect, radii, Path.Direction.CW);
            }
            canvas.drawPath(path, maskPaint);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(@Nullable ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.UNKNOWN; }
    }

    /** Original BaseCell.RippleDrawableSafe canvas-save/exception behavior, EVE logger. */
    private static final class RippleDrawableSafe extends RippleDrawable {
        RippleDrawableSafe(@NonNull ColorStateList color, @Nullable Drawable content,
                           @Nullable Drawable mask) {
            super(color, content, mask);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            final int save = canvas.save();
            try {
                super.draw(canvas);
            } catch (Exception e) {
                Log.e("TelegramPopupSelector", "RippleDrawable draw failed", e);
            } finally {
                canvas.restoreToCount(save);
            }
        }
    }
}

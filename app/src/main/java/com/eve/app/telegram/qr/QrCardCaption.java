package com.eve.app.telegram.qr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.eve.app.R;
import java.util.Locale;

/**
 * ADAPTED original Java username layout/drawing from QrShareView.java:3858-3892,
 * 4060-4109, SettingsSearchCell.java:40-79 and StaticLayoutEx.java:56-145 at
 * DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
 * Copyright Telegram contributors / Nikolai Kudashov, 2013-2018.
 * SettingsSearchCell/StaticLayoutEx: GPLv2 or later; root/per-file and combined
 * release review remain OPEN. EVE supplies only its validated username/resources.
 */
public final class QrCardCaption {
    private final Context context;
    private final String username;
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
    private Drawable at;
    private StaticLayout layout;
    private int cachedWidth = -1, cachedDirection = -1;

    public QrCardCaption(Context context, String username) {
        this.context = context.getApplicationContext();
        this.username = username;
        paint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/eve_qr_condensed_bold.ttf"));
    }

    private int dp(float value) {
        return (int) Math.ceil(context.getResources().getDisplayMetrics().density * value);
    }

    /** Original 30/25/19dp fitting, glyph span and two/three-line width selection. */
    public StaticLayout layout(int cardWidth) {
        final int direction = context.getResources().getConfiguration().getLayoutDirection();
        if (layout != null && cachedWidth == cardWidth && cachedDirection == direction) return layout;
        cachedWidth = cardWidth;
        cachedDirection = direction;
        // EVE's ASCII username contract is locale-independent (including Turkish).
        final String userText = username.toUpperCase(Locale.ROOT);
        final int textMaxWidth = Math.max(1, cardWidth - dp(20) * 2);
        for (int i = 0; i <= 2; ++i) {
            final int icon = i == 0 ? R.drawable.eve_qr_at_large :
                    i == 1 ? R.drawable.eve_qr_at_medium : R.drawable.eve_qr_at_small;
            at = ContextCompat.getDrawable(context, icon).mutate();
            paint.setTextSize(dp(i == 0 ? 30 : i == 1 ? 25 : 19));
            at.setBounds(0, 0, at.getIntrinsicWidth(), at.getIntrinsicHeight());
            final SpannableStringBuilder string = new SpannableStringBuilder(" " + userText);
            string.setSpan(new VerticalImageSpan(at, direction == View.LAYOUT_DIRECTION_RTL),
                    0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            final int glyphW = at.getBounds().width();
            final float textWidth = paint.measureText(string, 1, string.length()) + glyphW;
            if (i <= 1 && textWidth > textMaxWidth) continue;
            int linesCount = textWidth > textMaxWidth ? 2 : 1;
            int layoutWidth = textMaxWidth;
            if (linesCount > 1) layoutWidth = (int) (textWidth + glyphW) / 2 + dp(2);
            if (layoutWidth > textMaxWidth) {
                linesCount = 3;
                layoutWidth = (int) (textWidth + glyphW) / 3 + dp(4);
            }
            layout = createLayout(string, Math.max(1, layoutWidth),
                    Math.min(layoutWidth + dp(10), cardWidth), linesCount);
            break;
        }
        return layout;
    }

    public void draw(Canvas canvas, RectF card, float qrBottom, int color) {
        final StaticLayout name = layout((int) card.width());
        if (name == null) return;
        paint.setColor(color);
        at.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        final float textH = (paint.descent() - paint.ascent()) * name.getLineCount();
        // The normal source path is scale=1. Exceptionally narrow multiwindows
        // fit the entire caption inside its original band, preserving all text.
        final float maxW = Math.max(1f, card.width() - dp(20) * 2);
        final float maxH = Math.max(0f, card.bottom - qrBottom - dp(4) * 2);
        final float scale = Math.min(1f, Math.min(maxW / name.getWidth(), maxH / Math.max(1f, textH)));
        final float nx = card.left + (card.width() - name.getWidth() * scale) / 2f;
        final float ny = qrBottom + (card.bottom - qrBottom - textH * scale) / 2f - dp(4);
        final int save = canvas.save();
        canvas.translate(nx, ny);
        canvas.scale(scale, scale);
        name.draw(canvas);
        canvas.restoreToCount(save);
    }

    /** Source StaticLayoutEx API23+ path; EVE captions cannot contain animated spans or URLs. */
    @SuppressWarnings("deprecation")
    private StaticLayout createLayout(CharSequence text, int width, int ellipsisWidth, int maxLines) {
        if (maxLines == 1) {
            CharSequence fitted = TextUtils.ellipsize(text, paint, ellipsisWidth, TextUtils.TruncateAt.END);
            return new StaticLayout(fitted, 0, fitted.length(), paint, width,
                    Layout.Alignment.ALIGN_CENTER, 1f, 0f, false);
        }
        StaticLayout result = builder(text, width, ellipsisWidth, maxLines, StaticLayout.BREAK_STRATEGY_HIGH_QUALITY).build();
        for (int l = 0; l < result.getLineCount(); ++l) {
            if (result.getLineRight(l) > width) {
                result = builder(text, width, ellipsisWidth, maxLines, StaticLayout.BREAK_STRATEGY_SIMPLE).build();
                break;
            }
        }
        return result;
    }

    private StaticLayout.Builder builder(CharSequence text, int width, int ellipsisWidth,
                                         int maxLines, int breakStrategy) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1f)
                .setIncludePad(false).setEllipsize(null).setEllipsizedWidth(ellipsisWidth)
                .setMaxLines(maxLines).setBreakStrategy(breakStrategy)
                .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE);
    }

    /** Original SettingsSearchCell.VerticalImageSpan; only the RTL provider is EVE-owned. */
    private static final class VerticalImageSpan extends ImageSpan {
        private final boolean rtl;
        VerticalImageSpan(Drawable drawable, boolean rtl) { super(drawable); this.rtl = rtl; }
        @Override public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt metrics) {
            Drawable drawable = getDrawable();
            Rect rect = drawable.getBounds();
            if (metrics != null) {
                Paint.FontMetricsInt fm = paint.getFontMetricsInt();
                int fontHeight = fm.descent - fm.ascent;
                int centerY = fm.ascent + fontHeight / 2;
                metrics.ascent = centerY - (rect.bottom - rect.top) / 2;
                metrics.top = metrics.ascent;
                metrics.bottom = centerY + (rect.bottom - rect.top) / 2;
                metrics.descent = metrics.bottom;
            }
            return rect.right;
        }
        @Override public void draw(Canvas canvas, CharSequence text, int start, int end,
                                   float x, int top, int y, int bottom, Paint paint) {
            Drawable drawable = getDrawable();
            final int save = canvas.save();
            Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            int fontHeight = fm.descent - fm.ascent;
            int centerY = y + fm.descent - fontHeight / 2;
            canvas.translate(x, centerY - (drawable.getBounds().bottom - drawable.getBounds().top) / 2);
            if (rtl) canvas.scale(-1, 1, drawable.getIntrinsicWidth() / 2f, drawable.getIntrinsicHeight() / 2f);
            drawable.draw(canvas);
            canvas.restoreToCount(save);
        }
    }
}

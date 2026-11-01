/* Static-icon self-profile path extracted from Telegram ActionBarMenuSubItem.java.
 * Copyright Telegram contributors. See THIRD_PARTY_NOTICES.md and the source map.
 * EVE adapters: plain TextView/ImageView for labels/static icons, no account,
 * emoji, Lottie, downloaded images, checkboxes or swipe-back submenus. */
package com.eve.app.ui.profile.popup;
import static com.eve.app.ui.profile.popup.PopupPlatform.dp;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class ActionBarMenuSubItem extends FrameLayout {
    public TextView textView;
    public ImageView imageView;
    private final View checkView = null;
    private boolean checkViewLeft;
    private int textColor, iconColor, selectorColor, iconResId;
    private PorterDuff.Mode iconColorMode;
    private int selectorRad = 12;
    private boolean top, bottom, expandIfMultiline;
    private int itemHeight = 48;
    protected final Theme.ResourcesProvider resourcesProvider;
    public ActionBarMenuSubItem(Context context, int needCheck, boolean top, boolean bottom, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        this.top = top;
        this.bottom = bottom;

        textColor = getThemedColor(Theme.key_actionBarDefaultSubmenuItem);
        iconColor = getThemedColor(Theme.key_actionBarDefaultSubmenuItemIcon);
        iconColorMode = PorterDuff.Mode.MULTIPLY;
        selectorColor = getThemedColor(Theme.key_dialogButtonSelector);

        updateBackground();
        setPadding(dp(18), 0, dp(18), 0);

        imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 40, Gravity.CENTER_VERTICAL | ((getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) ? Gravity.RIGHT : Gravity.LEFT)));

        textView = new TextView(context);
        textView.setLines(1);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.LEFT);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, ((getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL));

        checkViewLeft = (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
        if (needCheck != 0) throw new IllegalArgumentException("Self-profile menu has no checkboxes");
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(dp(itemHeight), View.MeasureSpec.EXACTLY));
        if (expandIfMultiline && textView.getLayout().getLineCount() > 1) {
            super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(dp(itemHeight + 8), View.MeasureSpec.EXACTLY));
        }
    }

    public void setTextAndIcon(CharSequence text, int icon, Drawable iconDrawable) {
        textView.setText(text);
        if (icon != 0 || iconDrawable != null || checkView != null) {
            if (iconDrawable != null) {
                iconResId = 0;
                imageView.setImageDrawable(iconDrawable);
            } else {
                iconResId = icon;
                imageView.setImageResource(icon);
            }
            imageView.setVisibility(VISIBLE);
            textView.setPadding(checkViewLeft ? (checkView != null ? dp(43) : 0) : dp(icon != 0 || iconDrawable != null ? 43 : 0), 0, checkViewLeft ? dp(icon != 0 || iconDrawable != null ? 43 : 0) : (checkView != null ? dp(43) : 0), 0);
        } else {
            iconResId = 0;
            imageView.setVisibility(INVISIBLE);
            textView.setPadding(0, 0, 0, 0);
        }
    }

    public void setItemHeight(int itemHeight) { this.itemHeight = itemHeight; }

    public void setTextColor(int textColor) {
        if (this.textColor != textColor) {
            textView.setTextColor(this.textColor = textColor);
        }
    }

    public void setIconColor(int iconColor, PorterDuff.Mode mode) {
        if (this.iconColor != iconColor || this.iconColorMode != mode) {
            imageView.setColorFilter(new PorterDuffColorFilter(this.iconColor = iconColor, this.iconColorMode = mode));
        }
    }

    public void updateSelectorBackground(boolean top, boolean bottom) {
        if (this.top == top && this.bottom == bottom) {
            return;
        }
        this.top = top;
        this.bottom = bottom;
        updateBackground();
    }

    public void updateBackground() {
        setBackground(Theme.createRadSelectorDrawable(selectorColor, top ? selectorRad : 0, bottom ? selectorRad : 0));
    }

    private int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }
}

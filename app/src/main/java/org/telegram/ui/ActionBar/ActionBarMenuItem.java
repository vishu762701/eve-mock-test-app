// EVE-SHIM: minimal ActionBarMenuItem foundation for Telegram screen ports
package org.telegram.ui.ActionBar;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.telegram.ui.Components.LayoutHelper;

public class ActionBarMenuItem extends FrameLayout {
    private int itemId;
    private ImageView iconView;

    public ActionBarMenuItem(Context context, ActionBarMenu menu, int backgroundColor, int iconColor) {
        super(context);
        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER);
        addView(iconView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        if (iconColor != 0) {
            iconView.setColorFilter(iconColor);
        }
    }

    public void setIconColor(int color) {
        if (iconView != null && color != 0) {
            iconView.setColorFilter(color);
        }
    }

    public void setItemId(int id) {
        this.itemId = id;
    }

    public int getItemId() {
        return itemId;
    }

    public void setIcon(int resId) {
        if (iconView != null) {
            iconView.setImageResource(resId);
        }
    }

    public ImageView getIconView() {
        return iconView;
    }
}

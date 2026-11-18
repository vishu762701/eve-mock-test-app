// EVE-SHIM: minimal ActionBar foundation for Telegram screen ports
package org.telegram.ui.ActionBar;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.LayoutHelper;

public class ActionBar extends FrameLayout {
    private ImageView backButtonImageView;
    private TextView titleTextView;
    private ActionBarMenu menu;
    private ActionBarMenuOnItemClick actionBarMenuOnItemClick;
    private int itemsColor;
    private int itemsBackgroundColor;

    public static class ActionBarMenuOnItemClick {
        public void onItemClick(int id) {
        }
    }

    public ActionBar(Context context) {
        this(context, null);
    }

    public ActionBar(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        backButtonImageView = new ImageView(context);
        backButtonImageView.setScaleType(ImageView.ScaleType.CENTER);
        backButtonImageView.setOnClickListener(v -> {
            if (actionBarMenuOnItemClick != null) {
                actionBarMenuOnItemClick.onItemClick(-1);
            }
        });
        addView(backButtonImageView, LayoutHelper.createFrame(54, 54, Gravity.LEFT | Gravity.CENTER_VERTICAL));

        titleTextView = new TextView(context);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleTextView.setSingleLine(true);
        titleTextView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 72, 0, 56, 0));
    }

    public void setItemsBackgroundColor(int color, boolean isActionMode) {
        itemsBackgroundColor = color;
    }

    public void setItemsColor(int color, boolean isActionMode) {
        itemsColor = color;
        if (backButtonImageView != null) {
            backButtonImageView.setColorFilter(color);
        }
        if (titleTextView != null) {
            titleTextView.setTextColor(color);
        }
    }

    public int getItemsColor() {
        return itemsColor;
    }

    public void setTitleColor(int color) {
        if (titleTextView != null) {
            titleTextView.setTextColor(color);
        }
    }

    public static int getCurrentActionBarHeight() {
        if (AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
            return AndroidUtilities.dp(48);
        } else {
            return AndroidUtilities.dp(56);
        }
    }

    public void setBackButtonImage(int resId) {
        if (backButtonImageView != null) {
            backButtonImageView.setImageResource(resId);
            if (itemsColor != 0) {
                backButtonImageView.setColorFilter(itemsColor);
            }
        }
    }

    public void setAllowOverlayTitle(boolean value) {
    }

    public void setTitle(CharSequence value) {
        if (titleTextView != null) {
            titleTextView.setText(value);
        }
    }

    public void setActionBarMenuOnItemClick(ActionBarMenuOnItemClick listener) {
        this.actionBarMenuOnItemClick = listener;
        if (menu != null) {
            menu.setActionBarMenuOnItemClick(listener);
        }
    }

    public ActionBarMenuOnItemClick getActionBarMenuOnItemClick() {
        return actionBarMenuOnItemClick;
    }

    public ActionBarMenu createMenu() {
        if (menu != null) {
            return menu;
        }
        menu = new ActionBarMenu(getContext(), this);
        menu.setActionBarMenuOnItemClick(actionBarMenuOnItemClick);
        addView(menu, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT));
        return menu;
    }
}

// EVE-SHIM: minimal ActionBarMenu foundation for Telegram screen ports
package org.telegram.ui.ActionBar;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import org.telegram.ui.Components.LayoutHelper;

public class ActionBarMenu extends LinearLayout {
    protected ActionBar parentActionBar;
    protected ActionBar.ActionBarMenuOnItemClick listener;

    public ActionBarMenu(Context context, ActionBar actionBar) {
        super(context);
        setOrientation(HORIZONTAL);
        this.parentActionBar = actionBar;
    }

    public void setActionBarMenuOnItemClick(ActionBar.ActionBarMenuOnItemClick listener) {
        this.listener = listener;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof ActionBarMenuItem) {
                ((ActionBarMenuItem) child).setOnClickListener(v -> {
                    if (this.listener != null) {
                        this.listener.onItemClick(((ActionBarMenuItem) child).getItemId());
                    }
                });
            }
        }
    }

    public ActionBarMenuItem addItemWithWidth(int id, int icon, int width, CharSequence title) {
        ActionBarMenuItem item = new ActionBarMenuItem(getContext(), this, 0, 0);
        item.setItemId(id);
        item.setIcon(icon);
        item.setContentDescription(title);
        item.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(id);
            }
        });
        addView(item, LayoutHelper.createLinear(width, LayoutHelper.MATCH_PARENT));
        return item;
    }
}

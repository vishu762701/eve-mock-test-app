// EVE-SHIM: minimal BaseFragment foundation for Telegram screen ports
package org.telegram.ui.ActionBar;

import android.content.Context;
import android.view.View;
import java.util.ArrayList;

public abstract class BaseFragment {
    protected int currentAccount = 0;
    protected ActionBar actionBar;
    protected View fragmentView;
    protected INavigationLayout parentLayout;
    protected boolean isFinished = false;
    protected boolean finishing = false;

    public void setParentLayout(INavigationLayout layout) {
        if (parentLayout != layout) {
            parentLayout = layout;
            if (parentLayout != null && actionBar == null) {
                Context context = null;
                if (parentLayout.getParentActivity() != null) {
                    context = parentLayout.getParentActivity();
                } else {
                    try {
                        View pv = parentLayout.getView();
                        if (pv != null) {
                            context = pv.getContext();
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (context != null) {
                    actionBar = createActionBar(context);
                }
            }
        }
    }

    public INavigationLayout getParentLayout() {
        return parentLayout;
    }

    public View getFragmentView() {
        return fragmentView;
    }

    public ActionBar getActionBar() {
        return actionBar;
    }

    public ActionBar createActionBar(Context context) {
        ActionBar ab = new ActionBar(context, getResourceProvider());
        ab.setBackgroundColor(getThemedColor(Theme.key_actionBarDefault));
        ab.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSelector), false);
        ab.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), false);
        return ab;
    }

    public boolean onFragmentCreate() {
        return true;
    }

    public void onFragmentDestroy() {
        isFinished = true;
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public View createView(Context context) {
        return null;
    }

    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
    }

    public void finishFragment() {
        finishFragment(true);
    }

    public boolean finishFragment(boolean animated) {
        if (isFinished || parentLayout == null) {
            return false;
        }
        finishing = true;
        parentLayout.closeLastFragment(animated);
        return true;
    }

    public int getThemedColor(int key) {
        return Theme.getColor(key, getResourceProvider());
    }

    public Theme.ResourcesProvider getResourceProvider() {
        return null;
    }

    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return null;
    }
}

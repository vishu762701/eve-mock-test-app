// EVE-SHIM: minimal INavigationLayout interface for BaseFragment hosting
package org.telegram.ui.ActionBar;

import android.app.Activity;
import android.view.ViewGroup;

public interface INavigationLayout {
    void closeLastFragment(boolean animated);
    ViewGroup getView();
    Activity getParentActivity();
    default boolean isInBubbleMode() { return false; }
    default boolean isInPassivePreviewMode() { return false; }
    default void rebuildAllFragmentViews(boolean animated, boolean layout) {}
}

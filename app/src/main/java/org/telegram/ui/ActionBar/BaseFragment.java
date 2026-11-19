/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.ActionBar;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class BaseFragment {

    public boolean isFinished;
    protected boolean finishing;
    public Dialog visibleDialog;
    protected int currentAccount = 0; // EVE-PATCH: single-account Eve app without UserConfig

    public View fragmentView;
    protected INavigationLayout parentLayout;
    protected ActionBar actionBar;
    protected boolean inPreviewMode;
    protected boolean inMenuMode;
    protected boolean inBubbleMode;
    protected int classGuid;
    protected Bundle arguments;
    protected boolean hasOwnBackground = false;
    protected boolean isPaused = true;
    protected Dialog parentDialog;
    protected boolean inTransitionAnimation = false;
    protected boolean fragmentBeginToShow;
    private boolean removingFromStack;
    private PreviewDelegate previewDelegate;
    protected View navigationBarFadeView;
    protected Theme.ResourcesProvider resourceProvider;
    private boolean isFullyVisible;
//    public ArrayList<StoryViewer> storyViewerStack;
//    public ArrayList<BotWebViewAttachedSheet> botsStack;
//
    public ArrayList<AttachedSheet> sheetsStack;

    public static interface AttachedSheet {
        public View getWindowView();
        public boolean isShown();
        public void dismiss();
        default void dismiss(boolean tabs) { dismiss(); }
        public void release();

        public boolean isFullyVisible();

        public boolean attachedToParent();

        public boolean onAttachedBackPressed();
        public boolean showDialog(Dialog dialog);

        public void setKeyboardHeightFromParent(int keyboardHeight);

        public boolean isAttachedLightStatusBar();
        public int getNavigationBarColor(int color);

        public void setOnDismissListener(Runnable onDismiss);

        default void setLastVisible(boolean lastVisible) {};

    }

    public static interface AttachedSheetWindow {}

    public AttachedSheet getLastSheet() {
        if (sheetsStack == null || sheetsStack.isEmpty())
            return null;
        for (int i = sheetsStack.size() - 1; i >= 0; --i) {
            if (sheetsStack.get(i).isShown()) {
                return sheetsStack.get(i);
            }
        }
        return null;
    }

    public boolean hasSheet() {
        return sheetsStack != null && !sheetsStack.isEmpty();
    }

    public boolean hasShownSheet() {
        if (!hasSheet()) return false;
        for (int i = sheetsStack.size() - 1; i >= 0; --i) {
            if (sheetsStack.get(i).isShown()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasShownFullyVisibleSheet() {
        if (!hasSheet()) return false;
        for (int i = sheetsStack.size() - 1; i >= 0; --i) {
            if (sheetsStack.get(i).isShown() && sheetsStack.get(i).isFullyVisible()) {
                return true;
            }
        }
        return false;
    }

    public void clearSheets() {
        if (sheetsStack == null || sheetsStack.isEmpty())
            return;
        for (int i = sheetsStack.size() - 1; i >= 0; --i) {
            sheetsStack.get(i).dismiss(true);
        }
        sheetsStack.clear();
    }

    public BaseFragment() {
        this(null);
    }

    public BaseFragment(Bundle args) {
        arguments = args;
        classGuid = System.identityHashCode(this); // EVE-PATCH: local unique guid without ConnectionsManager

    }

    public void setCurrentAccount(int account) {
        if (fragmentView != null) {
            throw new IllegalStateException("trying to set current account when fragment UI already created");
        }
        currentAccount = account;
    }

    public boolean hasOwnBackground() {
        return hasOwnBackground;
    }

    public void setHasOwnBackground(boolean hasOwnBackground) {
        this.hasOwnBackground = hasOwnBackground;
    }

    public boolean getFragmentBeginToShow() {
        return fragmentBeginToShow;
    }

    public ActionBar getActionBar() {
        return actionBar;
    }

    public View getFragmentView() {
        return fragmentView;
    }

    public void setFragmentView(View fragmentView) {
        this.fragmentView = fragmentView;
    }

    protected boolean useFadeDrawableForActionBar() {
        return true;
    }

    @CallSuper
    protected void onViewCreated(View view) {
    }

    public View createView(Context context) { // EVE-PATCH: public view creation for external host activities
        return null;
    }

    public Bundle getArguments() {
        return arguments;
    }

    public int getCurrentAccount() {
        return currentAccount;
    }

    public int getClassGuid() {
        return classGuid;
    }

    public boolean isSwipeBackEnabled(MotionEvent event) {
        return true;
    }

    public void setInBubbleMode(boolean value) {
        inBubbleMode = value;
    }

    public boolean isInBubbleMode() {
        return inBubbleMode;
    }

    public boolean isInPreviewMode() {
        return inPreviewMode;
    }

    public boolean getInPassivePreviewMode() {
        return parentLayout != null && parentLayout.isInPassivePreviewMode();
    }

    public boolean isActionBarCrossfadeEnabled() {
        return actionBar != null;
    }

    public INavigationLayout.BackButtonState getBackButtonState() {
        return actionBar != null ? actionBar.getBackButtonState() : null;
    }

    public void setInPreviewMode(boolean value) {
        inPreviewMode = value;
        if (actionBar != null) {
            actionBar.setOccupyStatusBar(!inPreviewMode);
        }
    }

    public void setInMenuMode(boolean value) {
        inMenuMode = value;
    }

    public void onPreviewOpenAnimationEnd() {
    }

    protected boolean hideKeyboardOnShow() {
        return true;
    }

    public void clearViews() {
        if (fragmentView != null) {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                try {
                    onRemoveFromParent();
                    parent.removeViewInLayout(fragmentView);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            fragmentView = null;
        }
        if (actionBar != null) {
            ViewGroup parent = (ViewGroup) actionBar.getParent();
            if (parent != null) {
                try {
                    parent.removeViewInLayout(actionBar);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            actionBar = null;
        }
        clearSheets();
        parentLayout = null;
    }

    public void onRemoveFromParent() {
        if (sheetsStack == null || sheetsStack.isEmpty())
            return;
        updateSheetsVisibility();
    }

    public void setParentFragment(BaseFragment fragment) {
        setParentLayout(fragment.parentLayout);
        fragmentView = createView(parentLayout.getView().getContext());
    }

    public void setParentLayout(INavigationLayout layout) {
        if (parentLayout != layout) {
            parentLayout = layout;
            inBubbleMode = parentLayout != null && parentLayout.isInBubbleMode();
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    try {
                        onRemoveFromParent();
                        parent.removeViewInLayout(fragmentView);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (parentLayout != null && parentLayout.getView().getContext() != fragmentView.getContext()) {
                    fragmentView = null;
                    clearSheets();
                }
            }
            if (actionBar != null) {
                boolean differentParent = parentLayout != null && parentLayout.getView().getContext() != actionBar.getContext();
                if (actionBar.shouldAddToContainer() || differentParent) {
                    ViewGroup parent = (ViewGroup) actionBar.getParent();
                    if (parent != null) {
                        try {
                            parent.removeViewInLayout(actionBar);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }
                }
                if (differentParent) {
                    actionBar = null;
                }
            }
            if (parentLayout != null && useSimpleActionBar()) {
            } else if (parentLayout != null && actionBar == null) {
                actionBar = createActionBar(parentLayout.getView().getContext());
                if (actionBar != null) {
                    actionBar.parentFragment = this;
                }
            }
        }
    }

    /**
     * Screens with simple glass header return true to get {@link #simpleActionBar} instead of {@link #actionBar}.
     * Warn: the result must not change after the fragment is created.
     */
    protected boolean useSimpleActionBar() {
        return false;
    }

    /**
     * The layer where the menus of {@link #simpleActionBar} open, above the content and the header.
     * Created on first use in the root {@link FrameLayout} of the screen (or the parent of the header).
     */
    @Nullable
    private BaseFragment simpleMenuOverlayHost;

    /**
     * A fragment shown inside another one (a page of the main tabs) opens its menus in the host's overlay:
     * over the host's views too (the tabs), dimmed with the rest of the screen.
     */
    public void setSimpleMenuOverlayHost(@Nullable BaseFragment host) {
        simpleMenuOverlayHost = host;
    }

    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = new ActionBar(context, getResourceProvider());
        actionBar.setBackgroundColor(getThemedColor(Theme.key_actionBarDefault)); // EVE-PATCH: direct background color without ThemeBind
        actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarDefaultSelector), false); // EVE-PATCH: direct selector color without ThemeBind
        actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarActionModeDefaultSelector), true); // EVE-PATCH: direct actionMode selector color without ThemeBind
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarDefaultIcon), false); // EVE-PATCH: direct items color without ThemeBind
        actionBar.setItemsColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), true); // EVE-PATCH: direct actionMode items color without ThemeBind
        if (inPreviewMode || inBubbleMode || parentLayout != null && parentLayout.isLayersLayout()) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    public void movePreviewFragment(float dy) {
        parentLayout.movePreviewFragment(dy);
    }

    public void finishPreviewFragment() {
        if (parentLayout != null) {
            parentLayout.finishPreviewFragment();
        }
    }

    public void finishFragment() {
        if (parentDialog != null) {
            parentDialog.dismiss();
            return;
        }
        if (inPreviewMode && previewDelegate != null) {
            previewDelegate.finishFragment();
        } else {
            finishFragment(true);
        }
    }

    public void setFinishing(boolean finishing) {
        this.finishing = finishing;
    }

    public boolean finishFragment(boolean animated) {
        if (isFinished || parentLayout == null) {
            return false;
        }
        finishing = true;
        parentLayout.closeLastFragment(animated);
        return true;
    }

    public void removeSelfFromStack() {
        removeSelfFromStack(false);
    }

    public void removeSelfFromStack(boolean immediate) {
        if (isFinished || parentLayout == null) {
            return;
        }
        if (parentDialog != null) {
            parentDialog.dismiss();
            return;
        }
        parentLayout.removeFragmentFromStack(this, immediate);
    }

    public boolean allowFinishFragmentInsteadOfRemoveFromStack() {
        return true;
    }

    protected boolean isFinishing() {
        return finishing;
    }

    public boolean onFragmentCreate() {
        return true;
    }

    @CallSuper
    public void onFragmentDestroy() {
        isFinished = true;
        if (actionBar != null) {
            actionBar.setEnabled(false);
        }

        if (hasForceLightStatusBar() && !AndroidUtilities.isTablet() && getParentLayout().getLastFragment() == this && getParentActivity() != null && !finishing) {
            AndroidUtilities.setLightStatusBar(getParentActivity(), Theme.getColor(Theme.key_actionBarDefault) == Color.WHITE);
        }

        if (sheetsStack != null) {
            for (int i = sheetsStack.size() - 1; i >= 0; --i) {
                AttachedSheet sheet = sheetsStack.get(i);
                sheet.setLastVisible(false);
                sheet.dismiss(true);
                sheetsStack.remove(i);
            }
        }
    }

    public boolean needDelayOpenAnimation() {
        return false;
    }

    protected void resumeDelayedFragmentAnimation() {
        if (parentLayout != null) {
            parentLayout.resumeDelayedFragmentAnimation();
        }
    }

    public void onUserLeaveHint() {}

    @CallSuper
    public void onResume() {
        isPaused = false;
        if (actionBar != null) {
            actionBar.onResume();
        }
    }

    @CallSuper
    public void onPause() {
        if (actionBar != null) {
            actionBar.onPause();
        }
        isPaused = true;
        try {
            if (visibleDialog != null && visibleDialog.isShowing() && dismissDialogOnPause(visibleDialog)) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void setPaused(boolean paused) {
        if (isPaused == paused) {
            return;
        }

        if (paused) {
            onPause();
        } else {
            onResume();
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    public BaseFragment getFragmentForAlert(int offset) {
        if (parentLayout == null || parentLayout.getFragmentStack().size() <= 1 + offset) {
            return this;
        }
        return parentLayout.getFragmentStack().get(parentLayout.getFragmentStack().size() - 2 - offset);
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {

    }

    public boolean onBackPressed(boolean invoked) {
        if (hasShownSheet()) {
            if (invoked) closeSheet();
            return false;
        }
        return true;
    }

    public boolean closeSheet() {
        if (sheetsStack != null) {
            for (int i = sheetsStack.size() - 1; i >= 0; --i) {
                if (sheetsStack.get(i).isShown()) {
                    return sheetsStack.get(i).onAttachedBackPressed();
                }
            }
        }
        return false;
    }

    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {

    }

    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {

    }

    public void saveSelfArgs(Bundle args) {

    }

    public void restoreSelfArgs(Bundle args) {

    }

    public boolean isLastFragment() {
        return parentLayout != null && parentLayout.getLastFragment() == this;
    }

    public INavigationLayout getParentLayout() {
        return parentLayout;
    }

    public FrameLayout getLayoutContainer() {
        if (fragmentView != null) {
            final ViewParent parent = fragmentView.getParent();
            if (parent instanceof FrameLayout) {
                return (FrameLayout) parent;
            }
        }
        return null;
    }

    public FrameLayout getBulletinLayoutContainer() {
        return getLayoutContainer();
    }

    public boolean presentFragmentAsPreview(BaseFragment fragment) {
        return allowPresentFragment() && parentLayout != null && parentLayout.presentFragmentAsPreview(fragment);
    }

    public boolean presentFragmentAsPreviewWithMenu(BaseFragment fragment, ActionBarPopupWindow.ActionBarPopupWindowLayout menu) {
        return allowPresentFragment() && parentLayout != null && parentLayout.presentFragmentAsPreviewWithMenu(fragment, menu);
    }

    public boolean presentFragment(BaseFragment fragment) {
        return allowPresentFragment() && parentLayout != null && parentLayout.presentFragment(fragment);
    }

    public boolean presentFragment(BaseFragment fragment, boolean removeLast) {
        return allowPresentFragment() && parentLayout != null && parentLayout.presentFragment(fragment, removeLast);
    }

    public boolean presentFragment(BaseFragment fragment, boolean removeLast, boolean forceWithoutAnimation) {
        return allowPresentFragment() && parentLayout != null && parentLayout.presentFragment(fragment, removeLast, forceWithoutAnimation, true, false, null);
    }

    public boolean presentFragment(INavigationLayout.NavigationParams params) {
        return allowPresentFragment() && parentLayout != null && parentLayout.presentFragment(params);
    }

    public Activity getParentActivity() {
        if (parentLayout != null) {
            return parentLayout.getParentActivity();
        }
        return null;
    }

    public Context getContext() {
        return getParentActivity();
    }

    protected void setParentActivityTitle(CharSequence title) {
        Activity activity = getParentActivity();
        if (activity != null) {
            activity.setTitle(title);
        }
    }

    public void startActivityForResult(final Intent intent, final int requestCode) {
        if (parentLayout != null) {
            parentLayout.startActivityForResult(intent, requestCode);
        }
    }

    public void dismissCurrentDialog() {
        if (visibleDialog == null) {
            return;
        }
        try {
            visibleDialog.dismiss();
            visibleDialog = null;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public boolean dismissDialogOnPause(Dialog dialog) {
        return true;
    }

    public boolean canBeginSlide() {
        return true;
    }

    public void onBeginSlide() {
        try {
            if (visibleDialog != null && visibleDialog.isShowing()) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (actionBar != null) {
            actionBar.onPause();
        }
    }

    public void onSlideProgress(boolean isOpen, float progress) {

    }

    public void onTransitionAnimationProgress(boolean isOpen, float progress) {

    }

    public void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        inTransitionAnimation = true;
        if (isOpen) {
            fragmentBeginToShow = true;
        }
    }

    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        inTransitionAnimation = false;
    }

    public void onBecomeFullyVisible() {
        isFullyVisible = true;
        AccessibilityManager mgr = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (mgr.isEnabled()) {
            ActionBar actionBar = getActionBar();
            if (actionBar != null) { // EVE-PATCH: cut simpleActionBar check
                String title = actionBar.getTitle(); // EVE-PATCH: cut simpleActionBar title
                if (!TextUtils.isEmpty(title)) {
                    setParentActivityTitle(title);
                }
            }
        }
        if (fullyVisibleListener != null) {
            Runnable c = fullyVisibleListener;
            fullyVisibleListener = null;
            c.run();
        }
        updateSheetsVisibility();
    }

    protected void updateSheetsVisibility() {
        if (sheetsStack == null) return;
        for (int i = 0; i < sheetsStack.size(); ++i) {
            AttachedSheet sheet = sheetsStack.get(i);
            sheet.setLastVisible(i == sheetsStack.size() - 1 && isFullyVisible);
        }
    }

    private Runnable fullyVisibleListener;
    public void whenFullyVisible(Runnable callback) {
        fullyVisibleListener = callback;
    }

    public int getPreviewHeight() {
        return LayoutHelper.MATCH_PARENT;
    }

    public void onBecomeFullyHidden() {
        isFullyVisible = false;
        updateSheetsVisibility();
    }

    public AnimatorSet onCustomTransitionAnimation(boolean isOpen, Runnable callback) {
        return null;
    }

    public void onLowMemory() {

    }

    public Dialog showDialog(Dialog dialog) {
        return showDialog(dialog, false, null);
    }

    public Dialog showDialog(Dialog dialog, Dialog.OnDismissListener onDismissListener) {
        return showDialog(dialog, false, onDismissListener);
    }

    public Dialog showDialog(Dialog dialog, boolean allowInTransition, final Dialog.OnDismissListener onDismissListener) {
        if (dialog == null || parentLayout == null || parentLayout.isTransitionAnimationInProgress() || parentLayout.isSwipeInProgress() || !allowInTransition && parentLayout.checkTransitionAnimation()) {
            return null;
        }
        if (sheetsStack != null) {
            for (int i = sheetsStack.size() - 1; i >= 0; --i) {
                if (sheetsStack.get(i).isShown()) {
                    if (sheetsStack.get(i).showDialog(dialog)) {
                        return dialog;
                    }
                }
            }
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            visibleDialog = dialog;
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(dialog1 -> {
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(dialog1);
                }
                onDialogDismiss((Dialog) dialog1);
                if (dialog1 == visibleDialog) {
                    visibleDialog = null;
                }
            });
            visibleDialog.show();
            return visibleDialog;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    protected void onDialogDismiss(Dialog dialog) {

    }

    protected void onPanTranslationUpdate(float y) {

    }

    protected void onPanTransitionStart() {

    }

    protected void onPanTransitionEnd() {

    }

    public Dialog getVisibleDialog() {
        return visibleDialog;
    }

    public void setVisibleDialog(Dialog dialog) {
        visibleDialog = dialog;
    }

    public boolean extendActionMode(Menu menu) {
        return false;
    }

    /**
     * Called on every theme color update (each frame of a theme animation, and after a theme change), after the views
     * of every window were updated by {@link ThemeBind#updateAll}. Recolor here whatever is not a view binding or a
     * {@link Theme.Colorable}: map styles, popup menus, cached bitmaps, ...
     */
    public void onThemeColorsChanged() {
    }

    /** theme editor: color keys this page shows that no ThemeBind binding / Theme.Colorable view on screen reports */
    public void addThemeColorKeys(HashSet<Integer> keys) {
    }

    public ArrayList<ThemeDescription> getThemeDescriptions() {
        return new ArrayList<>();
    }

    public void setFragmentPanTranslationOffset(int offset) {
        if (parentLayout != null) {
            parentLayout.setFragmentPanTranslationOffset(offset);
        }
    }

    public void saveKeyboardPositionBeforeTransition() {

    }

    public Animator getCustomSlideTransition(boolean topFragment, boolean backAnimation, float distanceToMove) {
        return null;
    }

    protected boolean shouldOverrideSlideTransition(boolean topFragment, boolean backAnimation) {
        return false;
    }

    public void prepareFragmentToSlide(boolean topFragment, boolean beginSlide) {

    }

    public int getThemedColor(int key) {
        return Theme.getColor(key, getResourceProvider());
    }

    /**
     * Theme provider for ThemeBind bindings that resolves through getThemedColor() (subclasses override it). A named
     * class held by this fragment (not a lambda): bindings keep it weakly, so it lives exactly as long as the fragment.
     */
    private final Theme.ResourcesProvider themedColors = new ThemedColors();

    private class ThemedColors implements Theme.ResourcesProvider {
        @Override
        public int getColor(int key) {
            return getThemedColor(key);
        }
    }

    public Paint getThemedPaint(String paintKey) {
        Paint paint = getResourceProvider() != null ? getResourceProvider().getPaint(paintKey) : null;
        return paint != null ? paint : Theme.getThemePaint(paintKey);
    }

    public Drawable getThemedDrawable(String key) {
        return Theme.getThemeDrawable(key);
    }

    /**
     * @return If this fragment should have light status bar even if it's disabled in debug settings
     */
    public boolean hasForceLightStatusBar() {
        return false;
    }

    public int getNavigationBarColor() {
        int color = Theme.getColor(Theme.key_windowBackgroundGray, getResourceProvider());
        if (sheetsStack != null) {
            for (int i = 0; i < sheetsStack.size(); ++i) {
                AttachedSheet sheet = sheetsStack.get(i);
                if (sheet.attachedToParent()) {
                    color = sheet.getNavigationBarColor(color);
                }
            }
        }
        return color;
    }

    public boolean isBeginToShow() {
        return fragmentBeginToShow;
    }

    private void setParentDialog(Dialog dialog) {
        parentDialog = dialog;
    }

    public Theme.ResourcesProvider getResourceProvider() {
        return resourceProvider;
    }

    protected boolean allowPresentFragment() {
        return true;
    }

    public boolean isRemovingFromStack() {
        return removingFromStack;
    }

    public void setRemovingFromStack(boolean b) {
        removingFromStack = b;
    }

    public boolean isLightStatusBar() {
        if (hasForceLightStatusBar() && !Theme.getCurrentTheme().isDark()) {
            return true;
        }
        Theme.ResourcesProvider resourcesProvider = getResourceProvider();
        int color;
        int key = Theme.key_actionBarDefault;
        if (actionBar != null && actionBar.isActionModeShowed()) {
            key = Theme.key_actionBarActionModeDefault;
        }
        if (resourcesProvider != null) {
            color = resourcesProvider.getColorOrDefault(key);
        } else {
            color = Theme.getColor(key, null, true);
        }
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }

    public void setPreviewOpenedProgress(float progress) {

    }

    public void setPreviewReplaceProgress(float progress) {

    }

    public boolean closeLastFragment() {
        return false;
    }

    public void setPreviewDelegate(PreviewDelegate previewDelegate) {
        this.previewDelegate = previewDelegate;
    }

    public void resetFragment() {
        if (isFinished) {
            clearViews();
            isFinished = false;
            finishing = false;
        }
    }

    public void setResourceProvider(Theme.ResourcesProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public void onFragmentClosed() {

    }

    public void detachSheets() {
        if (sheetsStack != null) {
            for (int i = 0; i < sheetsStack.size(); ++i) {
                AttachedSheet sheet = sheetsStack.get(i);
                if (sheet != null && sheet.attachedToParent()) {
                    AndroidUtilities.removeFromParent(sheet.getWindowView());
                }
            }
        }
    }

    public void setKeyboardHeightFromParent(int keyboardHeight) {
        if (sheetsStack != null) {
            for (int i = 0; i < sheetsStack.size(); ++i) {
                AttachedSheet storyViewer = sheetsStack.get(i);
                if (storyViewer != null) {
                    storyViewer.setKeyboardHeightFromParent(keyboardHeight);
                }
            }
        }
    }

    public interface PreviewDelegate {
        void finishFragment();
    }

    /**
     * A glass of the parent over this fragment, e.g. the tabs, on the capture of the simple glass: its area in the
     * coordinates of the fragment's view, null for none. Does nothing without the simple glass.
     */
    public void setParentGlass(@Nullable RectF position, @Nullable Runnable onCaptured) {
    }

    private ViewGroup listViewForSimpleGlass;


    public void setTitleOverlayTextIfActionBarAttached(String title, int titleId, Runnable action) {
        if (actionBar != null && actionBar.shouldAddToContainer()) {
            setTitleOverlayText(title, titleId, action);
        }
    }

    public void setTitleOverlayText(String title, int titleId, Runnable action) {
        if (actionBar != null) {
            actionBar.setTitleOverlayText(title, titleId, action);
        }
    }

    public void removeSheet(BaseFragment.AttachedSheet sheet) {
        if (sheetsStack == null) return;
        sheetsStack.remove(sheet);
        updateSheetsVisibility();
    }

    public void onBottomSheetCreated() {

    }

    public static class BottomSheetParams {
        public boolean transitionFromLeft;
        public boolean allowNestedScroll;
        public Runnable onDismiss;
        public Runnable onOpenAnimationFinished;
        public Runnable onPreFinished;
        public boolean occupyNavigationBar;
    }

    @Deprecated
    public boolean isSupportEdgeToEdge() {
        // warn: overridden method must return a constant
        return false;
    }

    public boolean drawEdgeNavigationBar() {
        return isSupportEdgeToEdge();
    }

    protected Insets mSystemInsets = Insets.NONE;

    private int bottomInset;
    public int getBottomInset() {
        return bottomInset;
    }

    public void onInsets(int left, int top, int right, int bottom) {

    }



}
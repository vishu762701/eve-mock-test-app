/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.HashSet;

public class ChangeNameActivity extends BaseFragment {

    private EditTextBoldCursor firstNameField;
    private EditTextBoldCursor lastNameField;
    private View headerLabelView;
    private View doneButton;

    private Theme.ResourcesProvider resourcesProvider; private com.eve.app.ui.profile.TelegramProfileDelegates.ChangeNameDelegate eveDelegate; public void setEveDelegate(com.eve.app.ui.profile.TelegramProfileDelegates.ChangeNameDelegate delegate) { this.eveDelegate = delegate; } // EVE-PATCH: wire Eve CUT-INTERFACE delegate

    private final static int done_button = 1;

    public ChangeNameActivity(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    @Override
    public View createView(Context context) {
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_avatar_actionBarSelectorBlue, resourcesProvider), false); // EVE-PATCH: direct actionBar selector color without ThemeBind
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), false); // EVE-PATCH: direct actionBar items color without ThemeBind
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.EditName));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (firstNameField.getText().length() != 0) {
                        if (eveDelegate != null) { eveDelegate.saveName(firstNameField.getText().toString(), lastNameField.getText().toString()); } // EVE-PATCH: delegate saveName to Eve adapter
                        finishFragment();
                    }
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56), LocaleController.getString(R.string.Done));

        LinearLayout linearLayout = new LinearLayout(context);
        fragmentView = linearLayout;
        fragmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((LinearLayout) fragmentView).setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener((v, event) -> true);

        firstNameField = new EditTextBoldCursor(context) {
            @Override
            protected Theme.ResourcesProvider getResourcesProvider() {
                return resourcesProvider;
            }
        };
        firstNameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        firstNameField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider)); // EVE-PATCH: direct hint color without ThemeBind
        firstNameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider)); // EVE-PATCH: direct text color without ThemeBind
        firstNameField.setBackgroundDrawable(null);
        firstNameField.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_text_RedRegular, resourcesProvider)); // EVE-PATCH: direct line colors without ThemeBind
        firstNameField.setMaxLines(1);
        firstNameField.setLines(1);
        firstNameField.setSingleLine(true);
        firstNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        firstNameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        firstNameField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        firstNameField.setHint(LocaleController.getString(R.string.FirstName));
        firstNameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider)); // EVE-PATCH: direct cursor color without ThemeBind
        firstNameField.setCursorSize(AndroidUtilities.dp(20));
        firstNameField.setCursorWidth(1.5f);
        linearLayout.addView(firstNameField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 24, 24, 24, 0));
        firstNameField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT) {
                lastNameField.requestFocus();
                lastNameField.setSelection(lastNameField.length());
                return true;
            }
            return false;
        });

        lastNameField = new EditTextBoldCursor(context) {
            @Override
            protected Theme.ResourcesProvider getResourcesProvider() {
                return resourcesProvider;
            }
        };
        lastNameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        lastNameField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider)); // EVE-PATCH: direct hint color without ThemeBind
        lastNameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider)); // EVE-PATCH: direct text color without ThemeBind
        lastNameField.setBackgroundDrawable(null);
        lastNameField.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_text_RedRegular, resourcesProvider)); // EVE-PATCH: direct line colors without ThemeBind
        lastNameField.setMaxLines(1);
        lastNameField.setLines(1);
        lastNameField.setSingleLine(true);
        lastNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        lastNameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        lastNameField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        lastNameField.setHint(LocaleController.getString(R.string.LastName));
        lastNameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider)); // EVE-PATCH: direct cursor color without ThemeBind
        lastNameField.setCursorSize(AndroidUtilities.dp(20));
        lastNameField.setCursorWidth(1.5f);
        linearLayout.addView(lastNameField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 24, 16, 24, 0));
        lastNameField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                doneButton.performClick();
                return true;
            }
            return false;
        });

        if (eveDelegate != null) { // EVE-PATCH: load initial name from Eve delegate
            firstNameField.setText(eveDelegate.getInitialFirstName()); // EVE-PATCH: load initial first name from Eve delegate
            firstNameField.setSelection(firstNameField.length());
            lastNameField.setText(eveDelegate.getInitialLastName()); // EVE-PATCH: load initial last name from Eve delegate
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = org.telegram.messenger.ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Context.MODE_PRIVATE); // EVE-PATCH: read animations preference without MessagesController
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return resourcesProvider;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            AndroidUtilities.runOnUIThread(() -> {
                if (firstNameField != null) {
                    firstNameField.requestFocus();
                    AndroidUtilities.showKeyboard(firstNameField);
                }
            }, 100);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();



        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));
        themeDescriptions.add(new ThemeDescription(lastNameField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(lastNameField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));

        return themeDescriptions;
    }

    /** theme editor: colors this page shows (former ThemeDescription keys) */
    @Override
    public void addThemeColorKeys(HashSet<Integer> keys) {
        super.addThemeColorKeys(keys);
        keys.add(Theme.key_actionBarDefaultSelector);
        keys.add(Theme.key_actionBarDefaultTitle);
        keys.add(Theme.key_actionBarDefault);
        keys.add(Theme.key_windowBackgroundWhite);
    }
}

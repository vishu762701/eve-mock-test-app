// EVE-SHIM: Telegram ChangeNameActivity adapted with EVE-PATCH per R3 & R5
/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
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

public class ChangeNameActivity extends BaseFragment {

    private EditTextBoldCursor firstNameField;
    private EditTextBoldCursor lastNameField;
    private View headerLabelView;
    private View doneButton;

    private Theme.ResourcesProvider resourcesProvider;

    private final static int done_button = 1;

    // EVE-PATCH: delegate save flow and user data to Eve adapter per R3 & R5
    public interface ChangeNameDelegate {
        String getInitialFirstName();
        String getInitialLastName();
        void saveName(String firstName, String lastName);
    }
    private ChangeNameDelegate eveDelegate;

    public void setEveDelegate(ChangeNameDelegate delegate) {
        this.eveDelegate = delegate;
    }

    public ChangeNameActivity() {
        this(null);
    }

    public ChangeNameActivity(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    @Override
    public View createView(Context context) {
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_avatar_actionBarSelectorBlue, resourcesProvider), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), false);
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
                        saveName();
                        finishFragment();
                    }
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56), LocaleController.getString(R.string.Done));

        // EVE-PATCH: read initial user name from Eve adapter per R3 & R5
        String initialFirst = eveDelegate != null ? eveDelegate.getInitialFirstName() : "";
        String initialLast = eveDelegate != null ? eveDelegate.getInitialLastName() : "";

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
        // EVE-PATCH: id and state preservation wiring per R3 (b)
        firstNameField.setId(com.eve.app.R.id.etName);
        firstNameField.setSaveEnabled(true);
        firstNameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        firstNameField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider));
        firstNameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        firstNameField.setBackgroundDrawable(null);
        firstNameField.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        firstNameField.setMaxLines(1);
        firstNameField.setLines(1);
        firstNameField.setSingleLine(true);
        firstNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        firstNameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        firstNameField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        firstNameField.setHint(LocaleController.getString(R.string.FirstName));
        firstNameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
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
        lastNameField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider));
        lastNameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        lastNameField.setBackgroundDrawable(null);
        lastNameField.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        lastNameField.setMaxLines(1);
        lastNameField.setLines(1);
        lastNameField.setSingleLine(true);
        lastNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        lastNameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        lastNameField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        lastNameField.setHint(LocaleController.getString(R.string.LastName));
        lastNameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
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

        if (initialFirst != null) {
            firstNameField.setText(initialFirst);
            firstNameField.setSelection(firstNameField.length());
        }
        if (initialLast != null) {
            lastNameField.setText(initialLast);
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // EVE-PATCH: default view_animations to true without MTProto MessagesController
        boolean animations = true;
        if (!animations) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
    }

    // EVE-PATCH: cut Telegram MTProto RPC updateProfile and forward to Eve adapter per R3 & R5
    private void saveName() {
        if (eveDelegate != null) {
            String newFirst = firstNameField.getText() != null ? firstNameField.getText().toString() : "";
            String newLast = lastNameField.getText() != null ? lastNameField.getText().toString() : "";
            eveDelegate.saveName(newFirst, newLast);
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

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));
        themeDescriptions.add(new ThemeDescription(lastNameField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(lastNameField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        themeDescriptions.add(new ThemeDescription(lastNameField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(lastNameField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));

        return themeDescriptions;
    }
}

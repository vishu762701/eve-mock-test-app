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
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.CodepointsLengthInputFilter;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;

import java.util.ArrayList;
import java.util.HashSet;

public class ChangeBioActivity extends BaseFragment {

    private EditTextBoldCursor firstNameField;
    private View doneButton;
    private NumberTextView checkTextView;
    private TextView helpTextView;

    private final static int done_button = 1; private com.eve.app.ui.profile.TelegramProfileDelegates.ChangeBioDelegate eveDelegate; public void setEveDelegate(com.eve.app.ui.profile.TelegramProfileDelegates.ChangeBioDelegate delegate) { this.eveDelegate = delegate; } public EditTextBoldCursor getBioField() { return firstNameField; } // EVE-PATCH: wire Eve CUT-INTERFACE delegate and expose field for host

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.UserBio));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (eveDelegate != null) { eveDelegate.saveBio(firstNameField.getText().toString()); } // EVE-PATCH: delegate saveBio to Eve adapter
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56));
        doneButton.setContentDescription(LocaleController.getString(R.string.Done));

        fragmentView = new LinearLayout(context);
        LinearLayout linearLayout = (LinearLayout) fragmentView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener((v, event) -> true);

        FrameLayout fieldContainer = new FrameLayout(context);
        linearLayout.addView(fieldContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 24, 20, 0));

        firstNameField = new EditTextBoldCursor(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                Editable s = getEditableText();
                int number = (eveDelegate != null ? eveDelegate.getBioMaxLength() : 160) - Character.codePointCount(s, 0, s.length()); // EVE-PATCH: use Eve bio max length via delegate
                info.setText(getText() + ", " + LocaleController.formatPluralString("PeopleJoinedRemaining", number));
            }
        };
        firstNameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        firstNameField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText)); // EVE-PATCH: direct hint color without ThemeBind
        firstNameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); // EVE-PATCH: direct text color without ThemeBind
        firstNameField.setBackgroundDrawable(null);
        firstNameField.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_text_RedRegular)); // EVE-PATCH: direct line colors without ThemeBind
        firstNameField.setMaxLines(4);
        firstNameField.setPadding(AndroidUtilities.dp(LocaleController.isRTL ? 24 : 0), 0, AndroidUtilities.dp(LocaleController.isRTL ? 0 : 24), AndroidUtilities.dp(6));
        firstNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        firstNameField.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        firstNameField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        firstNameField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        InputFilter[] inputFilters = new InputFilter[1];
        inputFilters[0] = new CodepointsLengthInputFilter(eveDelegate != null ? eveDelegate.getBioMaxLength() : 160) { // EVE-PATCH: use Eve bio max length via delegate
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source != null && source.length() > 0 && TextUtils.indexOf(source, '\n') == source.length() - 1) {
                    doneButton.performClick();
                    return "";
                }
                CharSequence result = super.filter(source, start, end, dest, dstart, dend);
                if (result != null && source != null && result.length() != source.length()) {
                    Vibrator v = (Vibrator) getParentActivity().getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        v.vibrate(200);
                    }
                    AndroidUtilities.shakeView(checkTextView);
                }
                return result;
            }
        };
        firstNameField.setFilters(inputFilters);
        firstNameField.setMinHeight(AndroidUtilities.dp(36));
        firstNameField.setHint(LocaleController.getString(R.string.UserBio));
        firstNameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); // EVE-PATCH: direct cursor color without ThemeBind
        firstNameField.setCursorSize(AndroidUtilities.dp(20));
        firstNameField.setCursorWidth(1.5f);
        firstNameField.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                doneButton.performClick();
                return true;
            }
            return false;
        });
        firstNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                checkTextView.setNumber((eveDelegate != null ? eveDelegate.getBioMaxLength() : 160) - Character.codePointCount(s, 0, s.length()), true); // EVE-PATCH: use Eve bio max length via delegate
            }
        });

        fieldContainer.addView(firstNameField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 0, 4, 0));

        checkTextView = new NumberTextView(context);
        checkTextView.setCenterAlign(true);
        checkTextView.setTextSize(15);
        checkTextView.setNumber(eveDelegate != null ? eveDelegate.getBioMaxLength() : 160, false); // EVE-PATCH: use Eve bio max length via delegate
        checkTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
        checkTextView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        fieldContainer.addView(checkTextView, LayoutHelper.createFrame(26, 20, LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT, 0, 4, 4, 0));

        helpTextView = new TextView(context);
        helpTextView.setFocusable(true);
        helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        helpTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8)); // EVE-PATCH: direct help text color without ThemeBind
        helpTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        helpTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.UserBioInfo)));
        linearLayout.addView(helpTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT, 24, 10, 24, 0));

        String initialBio = eveDelegate != null ? eveDelegate.getInitialBio() : null; if (initialBio != null) { // EVE-PATCH: load initial bio from Eve delegate
            firstNameField.setText(initialBio); // EVE-PATCH: set initial bio from Eve delegate
            firstNameField.setSelection(firstNameField.length());
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
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();



        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));


        themeDescriptions.add(new ThemeDescription(checkTextView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }

    @Override
    public void onThemeColorsChanged() {
        try {
            super.onThemeColorsChanged();
            if (checkTextView != null) {
                checkTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
            }
            // firstNameField line / cursor colors: ThemeBind bindings set in createView
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /** theme editor: colors this page shows (former ThemeDescription keys) */
    @Override
    public void addThemeColorKeys(HashSet<Integer> keys) {
        super.addThemeColorKeys(keys);
        keys.add(Theme.key_actionBarDefaultSelector);
        keys.add(Theme.key_actionBarDefaultTitle);
        keys.add(Theme.key_actionBarDefaultIcon);
        keys.add(Theme.key_actionBarDefault);
        keys.add(Theme.key_windowBackgroundWhite);
    }
}

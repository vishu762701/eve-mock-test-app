/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.ForegroundColorSpanThemable;

import java.util.ArrayList;

public class ChangeUsernameActivity extends BaseFragment {

    private View doneButton; private com.eve.app.ui.profile.TelegramUsernameDelegate eveDelegate; public void setEveDelegate(com.eve.app.ui.profile.TelegramUsernameDelegate delegate) { this.eveDelegate = delegate; } public EditTextBoldCursor getUsernameField() { return inputCell != null ? inputCell.field : null; } public TextView getStatusTextView() { return statusTextView; } public View getDoneButton() { return doneButton; } public boolean isLastNameAvailable() { return lastNameAvailable; } public void recheckUsername() { checkUserName(username, false); } // EVE-PATCH: wire Eve CUT-INTERFACE delegate and host accessors

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private Adapter adapter;

    private String lastCheckName;
    private Runnable checkRunnable;
    private boolean lastNameAvailable;
    private boolean ignoreCheck;
    private CharSequence infoText;

    private String username = "";

    private final static int done_button = 1;

    public class LinkSpan extends ClickableSpan {

        private String url;

        public LinkSpan(String value) {
            url = value;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }

        @Override
        public void onClick(View widget) {
            try {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("label", url);
                clipboard.setPrimaryClip(clip);
                if (eveDelegate != null) { eveDelegate.onLinkCopied(url); } if (false) { // EVE-PATCH: route link copy toast through Eve delegate
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }
    private static class LinkMovementMethodMy extends LinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            try {
                boolean result = super.onTouchEvent(widget, buffer, event);
                if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    Selection.removeSelection(buffer);
                }
                return result;
            } catch (Exception e) {
                FileLog.e(e);
            }
            return false;
        }
    }

    public ChangeUsernameActivity() {
        this(null);
    }

    public ChangeUsernameActivity(Bundle args) {
        super(args);

    }

    @Override
    public View createView(Context context) {

        actionBar.setBackButtonImage(R.drawable.ic_ab_back); actionBar.setAllowOverlayTitle(true); // EVE-PATCH: use BaseFragment actionBar
        actionBar.setTitle(LocaleController.getString(R.string.Username)); // EVE-PATCH: use BaseFragment actionBar title
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // EVE-PATCH: use BaseFragment actionBar item click
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (eveDelegate != null) { eveDelegate.saveUsername(username); } // EVE-PATCH: delegate saveUsername to Eve adapter
                }
            }
        });

        doneButton = actionBar.createMenu().addItemWithWidth(done_button, R.drawable.ic_ab_done, dp(56), LocaleController.getString(R.string.Done)); // EVE-PATCH: use BaseFragment ActionBar done menu item

        if (eveDelegate != null) { username = eveDelegate.getCurrentUsername(); if (username == null) username = ""; } fragmentView = new FrameLayout(context); // EVE-PATCH: load initial username from Eve delegate
        listView = new RecyclerListView(context);
        listView.setSections();

        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context));
        listView.setAdapter(adapter = new Adapter());
        listView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray)); // EVE-PATCH: direct background color without ThemeBind
        listView.setSectionsDrawBackground(true);
        ((FrameLayout) fragmentView).addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView.setOnTouchListener((v, event) -> true);

        AndroidUtilities.runOnUIThread(() -> {
            if (username == null || username.length() > 0) {
                ignoreCheck = true;
                focusUsernameField(true); // EVE-PATCH: always show keyboard on username field
                ignoreCheck = false;
            }
        }, 40);

        return fragmentView;
    }


    private InputCell inputCell;
    private void focusUsernameField(boolean showKeyboard) {
        if (inputCell != null) {
            if (!inputCell.field.isFocused()) {
                inputCell.field.setSelection(inputCell.field.length());
            }
            inputCell.field.requestFocus();
            if (showKeyboard) {
                AndroidUtilities.showKeyboard(inputCell.field);
            }
        }
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_HELP1 = 1;
    private static final int VIEW_TYPE_HELP2 = 2;
    private static final int VIEW_TYPE_INPUT = 3;
    private static final int VIEW_TYPE_USERNAME = 4;

    private class Adapter extends RecyclerListView.SelectionAdapter {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    HeaderCell headerCell = new HeaderCell(getContext());
                    return new RecyclerListView.Holder(headerCell);
                case VIEW_TYPE_HELP1:
                    final View view = new UsernameHelpCell(getContext());
                    view.setTag(RecyclerListView.TAG_NOT_SECTION);
                    return new RecyclerListView.Holder(view);
                case VIEW_TYPE_HELP2:
                    return new RecyclerListView.Holder(new TextInfoPrivacyCell(getContext()));
                case VIEW_TYPE_INPUT:
                    return new RecyclerListView.Holder(new InputCell(getContext()));
                case VIEW_TYPE_USERNAME:
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_INPUT:
                    ignoreCheck = true;
                    (inputCell = (InputCell) holder.itemView).field.setText(username);
                    ignoreCheck = false;
                    break;
                case VIEW_TYPE_HEADER:
                    ((HeaderCell) holder.itemView).setText(LocaleController.getString(R.string.SetUsernameHeader)); // EVE-PATCH: single header row for Eve username
                    break;
                case VIEW_TYPE_USERNAME:
                    break;
                case VIEW_TYPE_HELP1:
                    break;
                case VIEW_TYPE_HELP2:
                    ((TextInfoPrivacyCell) holder.itemView).setText(LocaleController.getString(R.string.UsernameHelp)); // EVE-PATCH: use Eve username help string
                    break;
            }
        }

        public void updateSections() {
            if (listView == null) return;
            if (listView.forcedSections != null) {
                listView.forcedSections.clear();
            } else {
                listView.forcedSections = new ArrayList<>();
            }
        }

        @Override
        public int getItemCount() {
            updateSections();
            return 3; // EVE-PATCH: Eve username screen has 3 sections (header, input, help)
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_HEADER;
            } else if (position == 1) {
                return VIEW_TYPE_INPUT;
            } else if (position == 2) {
                return VIEW_TYPE_HELP1;
            } else if (position == 3) {
                return VIEW_TYPE_HEADER;
            } else if (position != getItemCount() - 1) {
                return VIEW_TYPE_USERNAME;
            } else {
                return VIEW_TYPE_HELP2;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false; // EVE-PATCH: no clickable collectible username rows
        }


    }

    private UsernameHelpCell helpCell;
    private LinkSpanDrawable.LinksTextView statusTextView;

    private class UsernameHelpCell extends FrameLayout {

        private LinkSpanDrawable.LinksTextView text1View;
        private LinkSpanDrawable.LinksTextView text2View;

        public UsernameHelpCell(Context context) {
            super(context);

            helpCell = this;

            setPadding(dp(18), dp(10), dp(18), dp(17));
            setClipChildren(false);

            text1View = new LinkSpanDrawable.LinksTextView(context);
            text1View.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            text1View.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8)); // EVE-PATCH: direct text1 color without ThemeBind
            text1View.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            text1View.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText)); // EVE-PATCH: direct link color without ThemeBind
            text1View.setHighlightColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkSelection)); // EVE-PATCH: direct highlight color without ThemeBind
            text1View.setPadding(dp(3), 0, dp(3), 0);

            text2View = statusTextView = new LinkSpanDrawable.LinksTextView(context) { @Override public void setTag(Object tag) { super.setTag(tag); if (tag instanceof Integer) { int k = (Integer) tag; setTextColor(Theme.getColor(k)); if (eveDelegate != null) { eveDelegate.onStatusRoleChanged(k == Theme.key_windowBackgroundWhiteGreenText ? com.eve.app.ui.profile.TelegramUsernameDelegate.ROLE_AVAILABLE : (k == Theme.key_text_RedRegular ? com.eve.app.ui.profile.TelegramUsernameDelegate.ROLE_INVALID : com.eve.app.ui.profile.TelegramUsernameDelegate.ROLE_CHECKING)); } } } // EVE-PATCH: sync status color and Eve delegate role on setTag
                @Override
                public void setText(CharSequence text, BufferType type) {
                    if (text != null) {
                        SpannableStringBuilder tagsString = AndroidUtilities.replaceTags(text.toString());
                        int index = tagsString.toString().indexOf('\n');
                        if (index >= 0) {
                            tagsString.replace(index, index + 1, " ");
                            tagsString.setSpan(new ForegroundColorSpanThemable(Theme.key_text_RedRegular, getResourceProvider()), 0, index, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        TypefaceSpan[] spans = tagsString.getSpans(0, tagsString.length(), TypefaceSpan.class);
                        for (int i = 0; i < spans.length; ++i) {
                            tagsString.setSpan(
                                new ClickableSpan() {
                                    @Override
                                    public void onClick(@NonNull View view) {
                                    }

                                    @Override
                                    public void updateDrawState(@NonNull TextPaint ds) {
                                        super.updateDrawState(ds);
                                        ds.setUnderlineText(false);
                                    }
                                },
                                tagsString.getSpanStart(spans[i]),
                                tagsString.getSpanEnd(spans[i]),
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            );
                            tagsString.removeSpan(spans[i]);
                        }
                        text = tagsString;
                    }
                    super.setText(text, type);
                }
            };
            text2View.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            text2View.setId(com.eve.app.R.id.tvStatus); text2View.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); text2View.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText8)); // EVE-PATCH: set tvStatus id, live region, and direct color
            text2View.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            text2View.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText)); // EVE-PATCH: direct status link color without ThemeBind
            text2View.setHighlightColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkSelection)); // EVE-PATCH: direct status highlight color without ThemeBind
            text2View.setPadding(dp(3), 0, dp(3), 0);

            addView(text1View, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
            addView(text2View, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP)); text1View.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.UsernameHelp))); if (!TextUtils.isEmpty(username)) { checkUserName(username, false); } // EVE-PATCH: set Eve username help text

        }

        private Integer height;

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, height == null ? heightMeasureSpec : MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }

        private ValueAnimator heightUpdateAnimator;
        private void update() {
            if (text2View.getVisibility() == View.VISIBLE) {
                text2View.measure(
                    MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(9999999, MeasureSpec.AT_MOST)
                );
            }
            if (heightUpdateAnimator != null) {
                heightUpdateAnimator.cancel();
            }
            int fromHeight = height == null ? getMeasuredHeight() : height;
            int newHeight = dp(10 + 17) + text1View.getHeight() + (text2View.getVisibility() == View.VISIBLE && !TextUtils.isEmpty(text2View.getText()) ? text2View.getMeasuredHeight() + dp(8) : 0);
            float fromTranslationY = text1View.getTranslationY();
            float newTranslationY = text2View.getVisibility() == View.VISIBLE && !TextUtils.isEmpty(text2View.getText()) ? text2View.getMeasuredHeight() + dp(8) : 0;
            heightUpdateAnimator = ValueAnimator.ofFloat(0, 1);
            heightUpdateAnimator.addUpdateListener(anm -> {
                final float t = (float) anm.getAnimatedValue();
                text1View.setTranslationY(AndroidUtilities.lerp(fromTranslationY, newTranslationY, t));
                height = AndroidUtilities.lerp(fromHeight, newHeight, t);
                requestLayout();
            });
            heightUpdateAnimator.setDuration(200);
            heightUpdateAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            heightUpdateAnimator.start();
        }
    }

    private class InputCell extends FrameLayout implements Theme.Colorable {
        public EditTextBoldCursor field;
        public TextView tme;

        public InputCell(Context context) {
            super(context);

            LinearLayout content = new LinearLayout(getContext());
            content.setOrientation(LinearLayout.HORIZONTAL);
            field = new EditTextBoldCursor(getContext()); field.setId(com.eve.app.R.id.etUsername); // EVE-PATCH: assign R.id.etUsername for host and tests
            field.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            field.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText)); // EVE-PATCH: direct hint color without ThemeBind
            field.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); // EVE-PATCH: direct field text color without ThemeBind
            field.setBackgroundDrawable(null);
//            field.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
            field.setMaxLines(1);
            field.setLines(1);
            field.setPadding(0, 0, 0, 0);
            field.setSingleLine(true);
            field.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
            field.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
            field.setImeOptions(EditorInfo.IME_ACTION_DONE);
            field.setHint(LocaleController.getString(R.string.UsernameLinkPlaceholder));
            field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); // EVE-PATCH: direct cursor color without ThemeBind
            field.setCursorSize(dp(19));
            field.setCursorWidth(1.5f);
            field.setOnEditorActionListener((textView, i, keyEvent) -> {
                if (i == EditorInfo.IME_ACTION_DONE && doneButton != null) {
                    doneButton.performClick();
                    return true;
                }
                return false;
            });
            field.setText(username);
            field.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    String wasUsername = username;
                    username = charSequence == null ? "" : charSequence.toString();
                    updateUsernameCell(wasUsername);
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    String wasUsername = username;
                    username = charSequence == null ? "" : charSequence.toString();
                    updateUsernameCell(wasUsername);
                    if (ignoreCheck) {
                        return;
                    }
                    checkUserName(username, false);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (username.startsWith("@")) {
                        username = username.substring(1);
                    }
                    if (username.length() > 0) {
                        String url = (eveDelegate != null ? eveDelegate.getLinkPrefix() : "@") + username; // EVE-PATCH: use Eve link prefix via delegate
                        String text = LocaleController.formatString("UsernameHelpLink", R.string.UsernameHelpLink, url);
                        int index = text.indexOf(url);
                        SpannableStringBuilder textSpan = new SpannableStringBuilder(text);
                        if (index >= 0) {
                            textSpan.setSpan(new LinkSpan(url), index, index + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
//                        helpTextView.setText(TextUtils.concat(infoText, "\n\n", textSpan));
                    } else {
//                        helpTextView.setText(infoText);
                    }
                }

                private void updateUsernameCell(String was) {
                }
            });
            tme = new TextView(getContext());
            tme.setMaxLines(1);
            tme.setLines(1);
            tme.setPadding(0, 0, 0, 0);
            tme.setSingleLine(true);
            tme.setText(eveDelegate != null ? eveDelegate.getLinkPrefix() : "@"); // EVE-PATCH: use Eve username prefix via delegate
            tme.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            tme.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)); // EVE-PATCH: direct prefix text color without ThemeBind
            tme.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
            tme.setTranslationY(-dp(3));
            content.addView(tme, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER_VERTICAL, 21, 15, 0, 15));
            content.addView(field, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1, Gravity.CENTER_VERTICAL, 0, 15, 21, 15));
            addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP));

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(dp(50), MeasureSpec.EXACTLY)
            );
        }
    
        @Override
        public void updateColors() {
            try {
                field.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                invalidate();
        
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
}


    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Context.MODE_PRIVATE); // EVE-PATCH: read animations preference without MessagesController
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            focusUsernameField(false);
        }
    }

    private boolean checkUserName(String name, boolean alert) {
        if (name != null && name.startsWith("@")) {
            name = name.substring(1);
        }
        if (statusTextView != null) {
            statusTextView.setVisibility(!TextUtils.isEmpty(name) ? View.VISIBLE : View.GONE);
            if (helpCell != null) {
                helpCell.update();
            }
        }
        if (alert && name.length() == 0) {
            return true;
        }
        if (checkRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(checkRunnable);
            checkRunnable = null;
            lastCheckName = null; if (eveDelegate != null) { eveDelegate.cancelCheck(); } // EVE-PATCH: cancel pending check via Eve delegate
        }
        lastNameAvailable = false;
        if (name != null) {
            if (name.startsWith("_") || name.endsWith("_")) {
                if (statusTextView != null) {
                    statusTextView.setText(LocaleController.getString(R.string.UsernameInvalid));
                    statusTextView.setTag(Theme.key_text_RedRegular);
                    if (helpCell != null) {
                        helpCell.update();
                    }
                }
                return false;
            }
            for (int a = 0; a < name.length(); a++) {
                char ch = name.charAt(a);
                if (a == 0 && ch >= '0' && ch <= '9') {
                    if (alert) {
                    } else {
                        if (statusTextView != null) {
                            statusTextView.setText(LocaleController.getString(R.string.UsernameInvalidStartNumber));
                            statusTextView.setTag(Theme.key_text_RedRegular);
                            if (helpCell != null) {
                                helpCell.update();
                            }
                        }
                    }
                    return false;
                }
                if (!(ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_')) {
                    if (alert) {
                    } else {
                        if (statusTextView != null) {
                            statusTextView.setText(LocaleController.getString(R.string.UsernameInvalid));
                            statusTextView.setTag(Theme.key_text_RedRegular);
                            if (helpCell != null) {
                                helpCell.update();
                            }
                        }
                    }
                    return false;
                }
            }
        }
        int minLen = eveDelegate != null ? eveDelegate.getMinimumLength() : 5; if (name == null || name.length() < minLen) { // EVE-PATCH: enforce Eve 5-char minimum username length
            if (alert) {
            } else {
                if (statusTextView != null) {
                    statusTextView.setText(LocaleController.getString(R.string.UsernameInvalidShort));
                    statusTextView.setTag(Theme.key_text_RedRegular);
                    if (helpCell != null) {
                        helpCell.update();
                    }
                }
            }
            return false;
        }
        if (name.length() > 32) {
            if (alert) {
            } else {
                if (statusTextView != null) {
                    statusTextView.setText(LocaleController.getString(R.string.UsernameInvalidLong));
                    statusTextView.setTag(Theme.key_text_RedRegular);
                    if (helpCell != null) {
                        helpCell.update();
                    }
                }
            }
            return false;
        }

        if (!alert) {
            String currentName = eveDelegate != null ? eveDelegate.getCurrentUsername() : ""; // EVE-PATCH: get current username from Eve delegate
            if (currentName == null) {
                currentName = "";
            }
            if (name.equalsIgnoreCase(currentName)) { // EVE-PATCH: case-insensitive current username match
                if (statusTextView != null) {
                    statusTextView.setText(LocaleController.formatString("UsernameAvailable", R.string.UsernameAvailable, name));
                    statusTextView.setTag(Theme.key_windowBackgroundWhiteGreenText);
                    if (helpCell != null) {
                        helpCell.update();
                    }
                }
                return true;
            }

            if (statusTextView != null) {
                statusTextView.setText(LocaleController.getString(R.string.UsernameChecking));
                statusTextView.setTag(Theme.key_windowBackgroundWhiteGrayText8);
                if (helpCell != null) {
                    helpCell.update();
                }
            }
            lastCheckName = name;
            final String nameFinal = name;
            checkRunnable = () -> { if (eveDelegate != null) { eveDelegate.checkUsername(nameFinal, (available, errMsg) -> AndroidUtilities.runOnUIThread(() -> { if (lastCheckName != null && lastCheckName.equals(nameFinal)) { if (available) { if (statusTextView != null) { statusTextView.setText(LocaleController.formatString("UsernameAvailable", R.string.UsernameAvailable, nameFinal)); statusTextView.setTag(Theme.key_windowBackgroundWhiteGreenText); statusTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGreenText)); eveDelegate.onStatusRoleChanged(com.eve.app.ui.profile.TelegramUsernameDelegate.ROLE_AVAILABLE); if (helpCell != null) { helpCell.update(); } } lastNameAvailable = true; } else { if (statusTextView != null) { statusTextView.setText(errMsg != null ? errMsg : LocaleController.getString(R.string.UsernameInUse)); statusTextView.setTag(Theme.key_text_RedRegular); statusTextView.setTextColor(Theme.getColor(Theme.key_text_RedRegular)); eveDelegate.onStatusRoleChanged(com.eve.app.ui.profile.TelegramUsernameDelegate.ROLE_INVALID); if (helpCell != null) { helpCell.update(); } } lastNameAvailable = false; } } })); } // EVE-PATCH: wire debounced checkUsername to Eve Worker API delegate
            };
            AndroidUtilities.runOnUIThread(checkRunnable, 300);
        }
        return true;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            focusUsernameField(false);
        }
    }

    public void shakeIfOff() {
        if (listView == null) {
            return;
        }
        for (int i = 0; i < listView.getChildCount(); ++i) {
            View child = listView.getChildAt(i);
            if (child instanceof HeaderCell && i == 0) {
                AndroidUtilities.shakeViewSpring(((HeaderCell) child).getTextView());
            } else if (child instanceof UsernameHelpCell) {
                AndroidUtilities.shakeViewSpring(child);
            } else if (child instanceof InputCell) {
                AndroidUtilities.shakeViewSpring(((InputCell) child).field);
                AndroidUtilities.shakeViewSpring(((InputCell) child).tme);
            }
        }
        if (eveDelegate != null) { eveDelegate.onErrorVibration(); } // EVE-PATCH: route error vibration through Eve delegate
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();


//        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));

        return themeDescriptions;
    }

}

/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//import com.google.mlkit.common.model.RemoteModelManager;
//import com.google.mlkit.nl.translate.TranslateRemoteModel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu; // EVE-PATCH: import ActionBarMenu instead of SimpleActionBarMenu
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.LanguageCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Timer;

public class LanguageSelectActivity extends BaseFragment { private com.eve.app.ui.settings.TelegramLanguageDelegate eveDelegate; public void setEveDelegate(com.eve.app.ui.settings.TelegramLanguageDelegate delegate) { this.eveDelegate = delegate; } // EVE-PATCH: wire Eve TelegramLanguageDelegate CUT-INTERFACE and cut NotificationCenter

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private ListAdapter searchListViewAdapter;
    private EmptyTextProgressView emptyView;

    private boolean searchWas;
    private boolean searching;

    private Timer searchTimer;
    private ArrayList<LocaleController.LocaleInfo> searchResult;
    private ArrayList<LocaleController.LocaleInfo> sortedLanguages;
    private ArrayList<LocaleController.LocaleInfo> unofficialLanguages;

//    private ArrayList<String> translationModels;

    private ActionBarMenuItem searchItem;
    private int translateSettingsBackgroundHeight;

    @Override
    public boolean onFragmentCreate() {
        fillLanguages();

//        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel.class)
//            .addOnSuccessListener(res -> {
//                translationModels = new ArrayList<>();
//                for (TranslateRemoteModel model : res) {
//                    if (!"en".equalsIgnoreCase(model.getLanguage())) {
//                        translationModels.add(model.getLanguage());
//                    }
//                }
//                if (translationModels.isEmpty()) {
//                    translationModels = null;
//                    return;
//                }
//                if (listAdapter != null) {
//                    listAdapter.notifyDataSetChanged();
//                }
//            });

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);

        searching = false;
        searchWas = false;

        actionBar.setBackButtonImage(R.drawable.ic_ab_back); // EVE-PATCH: use ActionBar
        actionBar.setTitle(LocaleController.getString(R.string.Language)); // EVE-PATCH: use ActionBar
        if (parentLayout != null && parentLayout.isRightLayout()) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_close); // EVE-PATCH: use ActionBar
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() { // EVE-PATCH: use ActionBar
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu(); // EVE-PATCH: use ActionBarMenu
        searchItem = menu.addItem(0, R.drawable.outline_header_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
            }

            @Override
            public void onSearchCollapse() {
                search(null);
                searching = false;
                searchWas = false;
                if (listView != null) {
                    emptyView.setVisibility(View.GONE);
                    listView.setAdapter(listAdapter);
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                search(text);
                if (text.length() != 0) {
                    searchWas = true;
                    if (listView != null) {
                        listView.setAdapter(searchListViewAdapter);
                    }
                } else {
                    searching = false;
                    searchWas = false;
                    if (listView != null) {
                        emptyView.setVisibility(View.GONE);
                        listView.setAdapter(listAdapter);
                    }
                }
            }
        });
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));

        listAdapter = new ListAdapter(context, false);
        searchListViewAdapter = new ListAdapter(context, true);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString(R.string.NoResult));
        emptyView.showTextView();
        emptyView.setShowAtCenter(true);

        listView = new RecyclerListView(context);
        listView.setSections();
        listView.setEmptyView(emptyView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setSectionsDrawBackground(true);
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator(); itemAnimator.setSupportsChangeAnimations(false); listView.setItemAnimator(itemAnimator); // EVE-PATCH: use standard AndroidX DefaultItemAnimator
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            try {
                if (getParentActivity() == null || parentLayout == null || !(view instanceof TextRadioCell)) {
                    return;
                }
                boolean search = listView.getAdapter() == searchListViewAdapter;
                if (!search) {
                    position -= languagesStartsPosition; // (7 - (!(getChatValue() || getContextValue()) ? 1 : 0) - (getMessagesController().premiumFeaturesBlocked() ? 1 : 0));
                }
                LocaleController.LocaleInfo localeInfo;
                if (search) {
                    localeInfo = searchResult.get(position);
                } else if (!unofficialLanguages.isEmpty() && position >= 0 && position < unofficialLanguages.size()) {
                    localeInfo = unofficialLanguages.get(position);
                } else {
                    if (!unofficialLanguages.isEmpty()) {
                        position -= unofficialLanguages.size() + 1;
                    }
                    localeInfo = sortedLanguages.get(position);
                }
                if (localeInfo != null) {
                    LocaleController.LocaleInfo prevLocale = LocaleController.getInstance().getCurrentLocaleInfo();
                    boolean sameLang = eveDelegate != null && TextUtils.equals(eveDelegate.getCurrentLanguageTag(), localeInfo.shortName); if (eveDelegate != null) { eveDelegate.onLanguageSelected(localeInfo); if (listAdapter != null) listAdapter.notifyDataSetChanged(); return; } // EVE-PATCH: apply selected language via Eve delegate

                    final AlertDialog progressDialog = new AlertDialog(getContext(), AlertDialog.ALERT_TYPE_SPINNER);
                    if (!sameLang) {
                        progressDialog.showDelayed(500);
                    }

                    String langCode = localeInfo.pluralLangCode,
                            prevLangCode = prevLocale.pluralLangCode;

                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }
        });

        return fragmentView;
    }

    private void fillLanguages() {
        final LocaleController.LocaleInfo currentLocale = LocaleController.getInstance().getCurrentLocaleInfo();
        Comparator<LocaleController.LocaleInfo> comparator = (o, o2) -> {
            if (o == currentLocale) {
                return -1;
            } else if (o2 == currentLocale) {
                return 1;
            } else if (o.serverIndex == o2.serverIndex) {
                return o.name.compareTo(o2.name);
            }
            if (o.serverIndex > o2.serverIndex) {
                return 1;
            } else if (o.serverIndex < o2.serverIndex) {
                return -1;
            }
            return 0;
        };

        sortedLanguages = new ArrayList<>();
        unofficialLanguages = new ArrayList<>(LocaleController.getInstance().unofficialLanguages);

        ArrayList<LocaleController.LocaleInfo> arrayList = eveDelegate != null ? eveDelegate.getLanguages() : new ArrayList<>(); // EVE-PATCH: load Eve languages via delegate
        for (int a = 0, size = arrayList.size(); a < size; a++) {
            LocaleController.LocaleInfo info = arrayList.get(a);
            if (info.serverIndex != Integer.MAX_VALUE) {
                sortedLanguages.add(info);
            } else {
                unofficialLanguages.add(info);
            }
        }
        Collections.sort(sortedLanguages, comparator);
        Collections.sort(unofficialLanguages, comparator);
    }

    @Override
    public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    public void search(final String query) {
        if (query == null) {
            searching = false;
            searchResult = null;
            if (listView != null) {
                emptyView.setVisibility(View.GONE);
                listView.setAdapter(listAdapter);
            }
        } else {
            processSearch(query);
        }
    }

    private void updateLanguage() {
        if (actionBar != null) { // EVE-PATCH: use ActionBar
            String newTitle = LocaleController.getString(R.string.Language);
            if (!TextUtils.equals(actionBar.getTitle(), newTitle)) { // EVE-PATCH: use ActionBar
                actionBar.setTitleAnimated(newTitle, true, 350, CubicBezierInterpolator.EASE_OUT_QUINT); // EVE-PATCH: use ActionBar
            }
        }
        if (listAdapter != null) {
            listAdapter.notifyItemRangeChanged(0, listAdapter.getItemCount());
//            for (int i = 0; i < listView.getChildCount(); ++i) {
//                View child = listView.getChildAt(i);
//                listAdapter.onBindViewHolder(listView.getChildViewHolder(child), listView.getChildAdapterPosition(child));
//                if (child instanceof TextRadioCell) {
//                    ((TextRadioCell) child).updateRTL();
//                } else if (child instanceof TextInfoPrivacyCell) {
//                    ((TextInfoPrivacyCell) child).updateRTL();
//                }
//            }
        }
    }

    private void processSearch(final String query) {
        Utilities.searchQueue.postRunnable(() -> {

            String q = query.trim().toLowerCase();
            if (q.length() == 0) {
                updateSearchResults(new ArrayList<>());
                return;
            }
            long time = System.currentTimeMillis();
            ArrayList<LocaleController.LocaleInfo> resultArray = new ArrayList<>();

            for (int a = 0, N = unofficialLanguages.size(); a < N; a++) {
                LocaleController.LocaleInfo c = unofficialLanguages.get(a);
                if (c.name.toLowerCase().startsWith(query) || c.nameEnglish.toLowerCase().startsWith(query)) {
                    resultArray.add(c);
                }
            }

            for (int a = 0, N = sortedLanguages.size(); a < N; a++) {
                LocaleController.LocaleInfo c = sortedLanguages.get(a);
                if (c.name.toLowerCase().startsWith(query) || c.nameEnglish.toLowerCase().startsWith(query)) {
                    resultArray.add(c);
                }
            }

            updateSearchResults(resultArray);
        });
    }

    private void updateSearchResults(final ArrayList<LocaleController.LocaleInfo> arrCounties) {
        AndroidUtilities.runOnUIThread(() -> {
            searchResult = arrCounties;
            searchListViewAdapter.notifyDataSetChanged();
        });
    }

    private boolean getContextValue() {
        return false; // EVE-PATCH: chat translation CUT-HIDDEN in Eve
    }

    private boolean getChatValue() {
        return false; // EVE-PATCH: chat translation CUT-HIDDEN in Eve
    }

    public static final int VIEW_TYPE_LANGUAGE = 0;
    public static final int VIEW_TYPE_SHADOW = 1;
    public static final int VIEW_TYPE_SWITCH = 2;
    public static final int VIEW_TYPE_HEADER = 3;
    public static final int VIEW_TYPE_SETTINGS = 4;
    public static final int VIEW_TYPE_SETTINGS_2 = 5;
    public static final int VIEW_TYPE_INFO = 6;

    private int settingsFromPosition = -1;
    private int settingsToPosition = -1;
    @Keep
    private int manualTranslationPosition = -1;
    @Keep
    private int autoTranslationPosition = -1;
    @Keep
    private int doNotTranslatePosition = -1;
    private int infoPosition1;
    private int languagesStartsPosition;

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;
        private boolean search;

        public ListAdapter(Context context, boolean isSearch) {
            mContext = context;
            search = isSearch;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            final int viewType = holder.getItemViewType();
            return viewType == VIEW_TYPE_LANGUAGE || viewType == VIEW_TYPE_SETTINGS || viewType == VIEW_TYPE_SETTINGS_2 || viewType == VIEW_TYPE_SWITCH;
        }

        @Override
        public int getItemCount() {
            if (search) {
                if (searchResult == null) {
                    return 0;
                }
                return searchResult.size();
            } else {
                int count = 0;
                count++;
                count++;
                count += sortedLanguages.size();
                if (!unofficialLanguages.isEmpty()) {
                    count += unofficialLanguages.size() + 1;
                }
                return count;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_LANGUAGE: {
                    view = new TextRadioCell(mContext);
                    break;
                }
                case VIEW_TYPE_SWITCH:
                    TextCheckCell switchCell = new TextCheckCell(mContext);
                    view = switchCell;
                    break;
                case VIEW_TYPE_SETTINGS:
                case VIEW_TYPE_SETTINGS_2:
                    TextSettingsCell settingsCell = new TextSettingsCell(mContext);
                    view = settingsCell;
                    break;
                case VIEW_TYPE_HEADER:
                    HeaderCell header = new HeaderCell(mContext);
                    view = header;
                    break;
                case VIEW_TYPE_INFO:
                    TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(mContext);
                    view = infoCell;
                    break;
                case VIEW_TYPE_SHADOW:
                default: {
                    view = new ShadowSectionCell(mContext);
                    break;
                }
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
            if (holder.itemView instanceof TextRadioCell) {
                ((TextRadioCell) holder.itemView).updateRTL();
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_LANGUAGE: {
                    if (!search) {
                        position -= languagesStartsPosition; // (7 - (!(getChatValue() || getContextValue()) ? 1 : 0) - (getMessagesController().premiumFeaturesBlocked() ? 1 : 0));
                    }
                    TextRadioCell textSettingsCell = (TextRadioCell) holder.itemView;
                    textSettingsCell.updateRTL();
                    LocaleController.LocaleInfo localeInfo = null;
                    boolean last;
                    if (search) {
                        if (position >= 0 && position < searchResult.size()) {
                            localeInfo = searchResult.get(position);
                        }
                        last = position == searchResult.size() - 1;
                    } else if (!unofficialLanguages.isEmpty() && position >= 0 && position < unofficialLanguages.size()) {
                        localeInfo = unofficialLanguages.get(position);
                        last = position == unofficialLanguages.size() - 1;
                    } else {
                        if (!unofficialLanguages.isEmpty()) {
                            position -= unofficialLanguages.size() + 1;
                        }
                        if (position >= 0 && position < sortedLanguages.size()) {
                            localeInfo = sortedLanguages.get(position);
                        }
                        last = position == sortedLanguages.size() - 1;
                    }
                    if (localeInfo != null) {
                        if (localeInfo.isLocal()) {
                            textSettingsCell.setTextAndValueAndCheck(String.format("%1$s (%2$s)", localeInfo.name, LocaleController.getString(R.string.LanguageCustom)), localeInfo.nameEnglish, false, false, !last);
                        } else {
                            textSettingsCell.setTextAndValueAndCheck(localeInfo.name, localeInfo.nameEnglish, false, false, !last);
                        }
                    }
                    textSettingsCell.setChecked(eveDelegate != null ? (localeInfo != null && TextUtils.equals(eveDelegate.getCurrentLanguageTag(), localeInfo.shortName)) : localeInfo == LocaleController.getInstance().getCurrentLocaleInfo()); // EVE-PATCH: highlight active Eve language
                    break;
                }
                case VIEW_TYPE_SHADOW: {
                    if (!search)
                        position--;
                    break;
                }
                case VIEW_TYPE_SETTINGS_2: {
                    TextSettingsCell settingsCell = (TextSettingsCell) holder.itemView;
                    settingsCell.updateRTL();
//                    if (translationModels != null) {
//                        ArrayList<String> languages = new ArrayList<>();
//                        for (int i = 0; i < translationModels.size(); ++i) {
//                            languages.add(TranslateAlert2.languageName(translationModels.get(i)));
//                        }
//                        settingsCell.setTextAndValue("Delete Translation Models", languages.size() >= 3 ? languages.size() + " models" : TextUtils.join(", ", languages), false);
//                    }
                    break;
                }
                case VIEW_TYPE_HEADER: {
                    HeaderCell header = (HeaderCell) holder.itemView;
                    header.setText(LocaleController.getString(R.string.Language)); // EVE-PATCH: chat translation header CUT-HIDDEN in Eve
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int i) {
            final int position = i;
            if (search) {
                return VIEW_TYPE_LANGUAGE;
            } else {
                if (i-- == 0) return VIEW_TYPE_HEADER;
                if (!unofficialLanguages.isEmpty() && (i == unofficialLanguages.size() || i == unofficialLanguages.size() + sortedLanguages.size() + 1) || unofficialLanguages.isEmpty() && i == sortedLanguages.size()) {
                    return VIEW_TYPE_SHADOW;
                }
                languagesStartsPosition = position - i;
                return VIEW_TYPE_LANGUAGE;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();


//        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));


        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));



        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{LanguageCell.class}, new String[]{"checkImage"}, null, null, null, Theme.key_featuredStickers_addedIcon));

        return themeDescriptions;
    }


    /** theme editor: colors this page shows (former ThemeDescription keys) */
    @Override
    public void addThemeColorKeys(HashSet<Integer> keys) {
        super.addThemeColorKeys(keys);
        keys.add(Theme.key_windowBackgroundWhiteGrayText3);
        keys.add(Theme.key_windowBackgroundWhiteBlackText);
        keys.add(Theme.key_windowBackgroundGrayShadow);
        keys.add(Theme.key_divider);
        keys.add(Theme.key_listSelector);
        keys.add(Theme.key_actionBarDefaultSelector);
        keys.add(Theme.key_actionBarDefaultTitle);
        keys.add(Theme.key_actionBarDefaultIcon);
        keys.add(Theme.key_actionBarDefault);
        keys.add(Theme.key_windowBackgroundWhite);
    }
}

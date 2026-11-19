/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import static org.telegram.messenger.AndroidUtilities.formatWholeNumber;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import org.telegram.ui.Components.TypefaceSpan;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;

public class LocaleController {

    static final int QUANTITY_OTHER = 0x0000;
    static final int QUANTITY_ZERO = 0x0001;
    static final int QUANTITY_ONE = 0x0002;
    static final int QUANTITY_TWO = 0x0004;
    static final int QUANTITY_FEW = 0x0008;
    static final int QUANTITY_MANY = 0x0010;

    public static boolean isRTL = com.eve.app.bridge.TelegramBridge.isRTL(); // EVE-PATCH: initialize RTL via Eve bridge
    public static int nameDisplayOrder = 1;
    public static boolean is24HourFormat = false;

    private HashMap<String, PluralRules> allRules = new HashMap<>();

    private Locale currentLocale;
    private Locale systemDefaultLocale;
    private PluralRules currentPluralRules;
    private LocaleInfo currentLocaleInfo;
    private boolean changingConfiguration = false;

    private String currentSystemLocale;

    private HashMap<String, String> currencyValues;

    public static class LocaleInfo {

        public String name;
        public String nameEnglish;
        public String shortName;
        public String pathToFile;
        public String baseLangCode;
        public String pluralLangCode;
        public boolean isRtl;
        public int version;
        public int baseVersion;
        public boolean builtIn;
        public int serverIndex;

        public String getSaveString() {
            String langCode = baseLangCode == null ? "" : baseLangCode;
            String pluralCode = TextUtils.isEmpty(pluralLangCode) ? shortName : pluralLangCode;
            return name + "|" + nameEnglish + "|" + shortName + "|" + pathToFile + "|" + version + "|" + langCode + "|" + pluralLangCode + "|" + (isRtl ? 1 : 0) + "|" + baseVersion + "|" + serverIndex;
        }

        public static LocaleInfo createWithString(String string) {
            if (string == null || string.length() == 0) {
                return null;
            }
            String[] args = string.split("\\|");
            LocaleInfo localeInfo = null;
            if (args.length >= 4) {
                localeInfo = new LocaleInfo();
                localeInfo.name = args[0];
                localeInfo.nameEnglish = args[1];
                localeInfo.shortName = args[2].toLowerCase();
                localeInfo.pathToFile = args[3];
                if (args.length >= 5) {
                    localeInfo.version = Utilities.parseInt(args[4]);
                }
                localeInfo.baseLangCode = args.length >= 6 ? args[5] : "";
                localeInfo.pluralLangCode = args.length >= 7 ? args[6] : localeInfo.shortName;
                if (args.length >= 8) {
                    localeInfo.isRtl = Utilities.parseInt(args[7]) == 1;
                }
                if (args.length >= 9) {
                    localeInfo.baseVersion = Utilities.parseInt(args[8]);
                }
                if (args.length >= 10) {
                    localeInfo.serverIndex = Utilities.parseInt(args[9]);
                } else {
                    localeInfo.serverIndex = Integer.MAX_VALUE;
                }
                if (!TextUtils.isEmpty(localeInfo.baseLangCode)) {
                    localeInfo.baseLangCode = localeInfo.baseLangCode.replace("-", "_");
                }
            }
            return localeInfo;
        }

        public File getPathToFile() {
            if (isRemote()) {
                return new File(ApplicationLoader.getFilesDirFixed(), "remote_" + shortName + ".xml");
            } else if (isUnofficial()) {
                return new File(ApplicationLoader.getFilesDirFixed(), "unofficial_" + shortName + ".xml");
            }
            return !TextUtils.isEmpty(pathToFile) ? new File(pathToFile) : null;
        }

        public File getPathToBaseFile() {
            if (isUnofficial()) {
                return new File(ApplicationLoader.getFilesDirFixed(), "unofficial_base_" + shortName + ".xml");
            }
            return null;
        }

        public String getKey() {
            if (pathToFile != null && !isRemote() && !isUnofficial()) {
                return "local_" + shortName;
            } else if (isUnofficial()) {
                return "unofficial_" + shortName;
            }
            return shortName;
        }

        public boolean hasBaseLang() {
            return isUnofficial() && !TextUtils.isEmpty(baseLangCode) && !baseLangCode.equals(shortName);
        }

        public boolean isRemote() {
            return "remote".equals(pathToFile);
        }

        public boolean isUnofficial() {
            return "unofficial".equals(pathToFile);
        }

        public boolean isLocal() {
            return !TextUtils.isEmpty(pathToFile) && !isRemote() && !isUnofficial();
        }

        public boolean isBuiltIn() {
            return builtIn;
        }

        public String getLangCode() {
            return shortName.replace("_", "-");
        }

        public String getBaseLangCode() {
            return baseLangCode == null ? "" : baseLangCode.replace("_", "-");
        }
    }

    private boolean loadingRemoteLanguages;

    public ArrayList<LocaleInfo> languages = new ArrayList<>();
    public ArrayList<LocaleInfo> unofficialLanguages = new ArrayList<>();
    public ArrayList<LocaleInfo> remoteLanguages = new ArrayList<>();
    public HashMap<String, LocaleInfo> remoteLanguagesDict = new HashMap<>();
    public HashMap<String, LocaleInfo> languagesDict = new HashMap<>();

    private ArrayList<LocaleInfo> otherLanguages = new ArrayList<>();

    private static volatile LocaleController Instance = null;
    public static LocaleController getInstance() {
        LocaleController localInstance = Instance;
        if (localInstance == null) {
            synchronized (LocaleController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LocaleController();
                }
            }
        }
        return localInstance;
    }

    public LocaleController() {
        addRules(new String[]{"bem", "brx", "da", "de", "el", "en", "eo", "es", "et", "fi", "fo", "gl", "he", "iw", "it", "nb",
                "nl", "nn", "no", "sv", "af", "bg", "bn", "ca", "eu", "fur", "fy", "gu", "ha", "is", "ku",
                "lb", "ml", "mr", "nah", "ne", "om", "or", "pa", "pap", "ps", "so", "sq", "sw", "ta", "te",
                "tk", "ur", "zu", "mn", "gsw", "chr", "rm", "pt", "an", "ast"}, new PluralRules_One());
        addRules(new String[]{"cs", "sk"}, new PluralRules_Czech());
        addRules(new String[]{"ff", "fr", "kab"}, new PluralRules_French());
        addRules(new String[]{"ru", "uk", "be"}, new PluralRules_Balkan());
        addRules(new String[]{"sr", "hr", "bs", "sh"}, new PluralRules_Serbian());
        addRules(new String[]{"lv"}, new PluralRules_Latvian());
        addRules(new String[]{"lt"}, new PluralRules_Lithuanian());
        addRules(new String[]{"pl"}, new PluralRules_Polish());
        addRules(new String[]{"ro", "mo"}, new PluralRules_Romanian());
        addRules(new String[]{"sl"}, new PluralRules_Slovenian());
        addRules(new String[]{"ar"}, new PluralRules_Arabic());
        addRules(new String[]{"mk"}, new PluralRules_Macedonian());
        addRules(new String[]{"cy"}, new PluralRules_Welsh());
        addRules(new String[]{"br"}, new PluralRules_Breton());
        addRules(new String[]{"lag"}, new PluralRules_Langi());
        addRules(new String[]{"shi"}, new PluralRules_Tachelhit());
        addRules(new String[]{"mt"}, new PluralRules_Maltese());
        addRules(new String[]{"ga", "se", "sma", "smi", "smj", "smn", "sms"}, new PluralRules_Two());
        addRules(new String[]{"ak", "am", "bh", "fil", "tl", "guw", "hi", "ln", "mg", "nso", "ti", "wa"}, new PluralRules_Zero());
        addRules(new String[]{"az", "bm", "fa", "ig", "hu", "ja", "kde", "kea", "ko", "my", "ses", "sg", "to",
                "tr", "vi", "wo", "yo", "zh", "bo", "dz", "id", "jv", "jw", "ka", "km", "kn", "ms", "th", "in"}, new PluralRules_None());

        LocaleInfo localeInfo = new LocaleInfo();
        localeInfo.name = "English";
        localeInfo.nameEnglish = "English";
        localeInfo.shortName = localeInfo.pluralLangCode = "en";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Italiano";
        localeInfo.nameEnglish = "Italian";
        localeInfo.shortName = localeInfo.pluralLangCode = "it";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Español";
        localeInfo.nameEnglish = "Spanish";
        localeInfo.shortName = localeInfo.pluralLangCode = "es";
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Deutsch";
        localeInfo.nameEnglish = "German";
        localeInfo.shortName = localeInfo.pluralLangCode = "de";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Nederlands";
        localeInfo.nameEnglish = "Dutch";
        localeInfo.shortName = localeInfo.pluralLangCode = "nl";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "العربية";
        localeInfo.nameEnglish = "Arabic";
        localeInfo.shortName = localeInfo.pluralLangCode = "ar";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        localeInfo.isRtl = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Português (Brasil)";
        localeInfo.nameEnglish = "Portuguese (Brazil)";
        localeInfo.shortName = localeInfo.pluralLangCode = "pt_br";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "한국어";
        localeInfo.nameEnglish = "Korean";
        localeInfo.shortName = localeInfo.pluralLangCode = "ko";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Українська";
        localeInfo.nameEnglish = "Ukrainian";
        localeInfo.shortName = localeInfo.pluralLangCode = "uk";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        localeInfo = new LocaleInfo();
        localeInfo.name = "Русский";
        localeInfo.nameEnglish = "Russian";
        localeInfo.shortName = localeInfo.pluralLangCode = "ru";
        localeInfo.pathToFile = null;
        localeInfo.builtIn = true;
        languages.add(localeInfo);
        languagesDict.put(localeInfo.shortName, localeInfo);

        for (int a = 0; a < otherLanguages.size(); a++) {
            LocaleInfo locale = otherLanguages.get(a);
            languages.add(locale);
            languagesDict.put(locale.getKey(), locale);
        }

        for (int a = 0; a < remoteLanguages.size(); a++) {
            LocaleInfo locale = remoteLanguages.get(a);
            LocaleInfo existingLocale = getLanguageFromDict(locale.getKey());
            if (existingLocale != null) {
                existingLocale.pathToFile = locale.pathToFile;
                existingLocale.version = locale.version;
                existingLocale.baseVersion = locale.baseVersion;
                existingLocale.serverIndex = locale.serverIndex;
                remoteLanguages.set(a, existingLocale);
            } else {
                languages.add(locale);
                languagesDict.put(locale.getKey(), locale);
            }
        }

        for (int a = 0; a < unofficialLanguages.size(); a++) {
            LocaleInfo locale = unofficialLanguages.get(a);
            LocaleInfo existingLocale = getLanguageFromDict(locale.getKey());
            if (existingLocale != null) {
                existingLocale.pathToFile = locale.pathToFile;
                existingLocale.version = locale.version;
                existingLocale.baseVersion = locale.baseVersion;
                existingLocale.serverIndex = locale.serverIndex;
                unofficialLanguages.set(a, existingLocale);
            } else {
                languagesDict.put(locale.getKey(), locale);
            }
        }

        systemDefaultLocale = Locale.getDefault();
        is24HourFormat = DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
        LocaleInfo currentInfo = null;
        boolean override = false;

        try {
            SharedPreferences preferences = com.eve.app.bridge.TelegramBridge.getMainSettings(ApplicationLoader.applicationContext); // EVE-PATCH: use app shared preferences via Eve bridge
            String lang = preferences.getString("language", null);
            if (lang != null) {
                currentInfo = getLanguageFromDict(lang);
                if (currentInfo != null) {
                    override = true;
                }
            }

            if (currentInfo == null && systemDefaultLocale.getLanguage() != null) {
                currentInfo = getLanguageFromDict(systemDefaultLocale.getLanguage());
            }
            if (currentInfo == null) {
                currentInfo = getLanguageFromDict(getLocaleString(systemDefaultLocale));
                if (currentInfo == null) {
                    currentInfo = getLanguageFromDict("en");
                }
            }

            currentLocale = currentInfo != null && currentInfo.shortName != null ? new java.util.Locale(currentInfo.shortName) : systemDefaultLocale; // EVE-PATCH: set local currentLocale without MTProto sync
        } catch (Exception e) {
            FileLog.e(e);
        }

        try {
        } catch (Exception e) {
            FileLog.e(e);
        }

        AndroidUtilities.runOnUIThread(() -> currentSystemLocale = getSystemLocaleStringIso639());
    }

    public LocaleInfo getLanguageFromDict(String key) {
        if (key == null) {
            return null;
        }
        return languagesDict.get(key.toLowerCase().replace("-", "_"));
    }
    public LocaleInfo getBuiltinLanguageByPlural(String plural) {
        Collection<LocaleInfo> values = languagesDict.values();
        for (LocaleInfo l : values)
            if (l.pathToFile != null && l.pathToFile.equals("remote") && (l.shortName == null || !l.shortName.endsWith("_raw")) && l.pluralLangCode != null && l.pluralLangCode.equals(plural))
                return l;
        return null;
    }

    private void addRules(String[] languages, PluralRules rules) {
        for (String language : languages) {
            allRules.put(language, rules);
        }
    }

    private String stringForQuantity(int quantity) {
        switch (quantity) {
            case QUANTITY_ZERO:
                return "zero";
            case QUANTITY_ONE:
                return "one";
            case QUANTITY_TWO:
                return "two";
            case QUANTITY_FEW:
                return "few";
            case QUANTITY_MANY:
                return "many";
            default:
                return "other";
        }
    }

    public Locale getSystemDefaultLocale() {
        return systemDefaultLocale;
    }

    public boolean isCurrentLocalLocale() {
        return currentLocaleInfo.isLocal();
    }

    private boolean checkingUpdateForCurrentRemoteLocale;

    public int calculateTranslatedCount(HashMap<String, String> map) {
        int count = 0;
        HashSet<String> added = new HashSet<>();
        for (String k : map.keySet()) {
            if (k == null) {
                continue;
            }
            String real = null;
            if (k.endsWith("_other")) {
                real = k.substring(0, k.length() - 6);
            } else if (k.endsWith("_zero") || k.endsWith("_many")) {
                real = k.substring(0, k.length() - 5);
            } else if (k.endsWith("_one") || k.endsWith("_two") || k.endsWith("_few")) {
                real = k.substring(0, k.length() - 4);
            }
            if (real == null) {
                count++;
            } else if (!added.contains(real)) {
                added.add(real);
                count++;
            }
        }
        added.clear();
        return count;
    }

    private boolean patching = false;

    private String getLocaleString(Locale locale) {
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('_');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getSystemLocaleStringIso639() {
        Locale locale = getInstance().getSystemDefaultLocale();
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleStringIso639() {
        LocaleInfo info = getInstance().currentLocaleInfo;
        if (info != null) {
            return info.getLangCode();
        }
        Locale locale = getInstance().currentLocale;
        if (locale == null) {
            return "en";
        }
        String languageCode = locale.getLanguage();
        String countryCode = locale.getCountry();
        String variantCode = locale.getVariant();
        if (languageCode.length() == 0 && countryCode.length() == 0) {
            return "en";
        }
        StringBuilder result = new StringBuilder(11);
        result.append(languageCode);
        if (countryCode.length() > 0 || variantCode.length() > 0) {
            result.append('-');
        }
        result.append(countryCode);
        if (variantCode.length() > 0) {
            result.append('_');
        }
        result.append(variantCode);
        return result.toString();
    }

    public static String getLocaleAlias(String code) {
        if (code == null) {
            return null;
        }
        switch (code) {
            case "in":
                return "id";
            case "iw":
                return "he";
            case "jw":
                return "jv";
            case "no":
                return "nb";
            case "tl":
                return "fil";
            case "ji":
                return "yi";
            case "id":
                return "in";
            case "he":
                return "iw";
            case "jv":
                return "jw";
            case "nb":
                return "no";
            case "fil":
                return "tl";
            case "yi":
                return "ji";
        }

        return null;
    }

    public LocaleInfo getCurrentLocaleInfo() {
        return currentLocaleInfo;
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    public static String getCurrentLanguageName() {
        LocaleInfo localeInfo = getInstance().currentLocaleInfo;
        return localeInfo == null || TextUtils.isEmpty(localeInfo.name) ? "English" : localeInfo.name; // EVE-PATCH: use default language name
    }

    private String getStringInternal(String key, int res) {
        return getStringInternal(key, null, res);
    }

    private String getStringInternal(String key, String fallback, int res) {
        final String value = getStringV2(key, res, fallback);
        if (value == null) {
            return "LOC_ERR:" + key;
        }
        return value;
    }

    public static String getString(@StringRes int res) {
        return getString(null, res);
    }

    // deprecated: String key is no longer necessary
    @Deprecated
    public static String getString(String key, @StringRes int res) {
        return getInstance().getStringInternal(key, res);
    }

    // deprecated: String key is no longer necessary
    @Deprecated
    public static String getString(String key) {
        if (TextUtils.isEmpty(key)) {
            return "LOC_ERR:" + key;
        }
        return getString(key, 0);
    }

    public static String nullable(String val) {
        if (val == null || val.startsWith("LOC_ERR")) return null;
        return val;
    }

    public static String getPluralString(String key, int plural) {
        if (key == null || key.length() == 0 || getInstance().currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        return getInstance().getStringInternal(param, key + "_other", 0);
    }

    public static String formatPluralString(String key, int plural, Object... args) {
        if (key == null || key.length() == 0 || getInstance().currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        Object[] argsWithPlural = new Object[args.length + 1];
        argsWithPlural[0] = plural;
        System.arraycopy(args, 0, argsWithPlural, 1, args.length);
        return formatString(param, key + "_other", 0, argsWithPlural);
    }

    public static CharSequence formatPluralSpannable(String key, int plural, CharSequence... args) {
        if (key == null || key.length() == 0 || getInstance().currentPluralRules == null) {
            return "LOC_ERR:" + key;
        }
        String param = getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(plural));
        param = key + "_" + param;
        Object[] argsWithPlural = new Object[args.length + 1];
        argsWithPlural[0] = plural;
        System.arraycopy(args, 0, argsWithPlural, 1, args.length);
        return formatSpannable(param, key + "_other", 0, argsWithPlural);
    }

    public static String getStringParamForNumber(int number) {
        return getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(number));
    }

    public static String formatPluralStringComma(String key, int plural) {
        return formatPluralStringComma(key, plural, ',');
    }

    public static String formatPluralStringSpaced(String key, int plural) {
        return formatPluralStringComma(key, plural, ' ');
    }

    public static String formatPluralStringSpaced(String key, int plural, Object... args) {
        return formatPluralStringComma(key, plural, ' ', args);
    }

    public static String formatPluralStringComma(String key, int plural, Object... args) {
        return formatPluralStringComma(key, plural, ',', args);
    }


    public static String formatPluralStringComma(String key, int plural, char symbol) {
        return formatPluralStringComma(key, plural, symbol, new Object[] {});
    }

    public static CharSequence bold(CharSequence text) {
        if (text instanceof Spannable) {
            ((Spannable) text).setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return text;
        } else {
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            ssb.setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ssb;
        }
    }

    public static String formatPluralStringComma(String key, int plural, char symbol, Object... args) {
        try {
            if (key == null || key.length() == 0 || getInstance().currentPluralRules == null) {
                return "LOC_ERR:" + key;
            }
            String param = getInstance().stringForQuantity(getInstance().currentPluralRules.quantityForNumber(plural));
            param = key + "_" + param;
            StringBuilder stringBuilder = new StringBuilder(String.format("%d", plural));
            for (int a = stringBuilder.length() - 3; a > 0; a -= 3) {
                stringBuilder.insert(a, symbol);
            }

            String value = getInstance().getStringInternal(param, key + "_other", 0); // EVE-PATCH: resolve plural with fallback from resources
            if (value == null) {
                // no-op // EVE-PATCH: skip cloud strings fallback
            }
            if (value == null) {
                try {
                    // no-op // EVE-PATCH: skip internal localization lookup
                } catch (Exception e2) {}
            }
            if (value == null) {
                // no-op // EVE-PATCH: skip internal localization lookup
            }
            value = value.replace("%d", "%1$s");
            value = value.replace("%1$d", "%1$s");

            Object[] a = new Object[(args == null ? 0 : args.length) + 1];
            for (int i = 0; i < a.length; ++i) {
                a[i] = i == 0 ? stringBuilder : args[i - 1];
            }
            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, value, a);
            } else {
                return String.format(value, a);
            }
        } catch (Exception e) {
            FileLog.e(e);
            return "LOC_ERR: " + key;
        }
    }

    public static String formatNumber(long count, char symbol) {
        if (count < 0) {
            return "-" + formatNumber(-count, symbol);
        }

        StringBuilder stringBuilder = new StringBuilder(String.format("%d", count));
        for (int a = stringBuilder.length() - 3; a > 0; a -= 3) {
            stringBuilder.insert(a, symbol);
        }
        return stringBuilder.toString();
    }

    public static String formatNumberWithMillion(long count, char symbol) {
        if (count < 1_000_000) {
            return formatNumber(count, symbol);
        }

        return formatWholeNumber((int) count, 1_000_000);
    }

    public static String formatString(@StringRes int res, Object... args) {
        return formatString(null, res, args);
    }

    // deprecated: String key is no longer necessary
    @Deprecated
    public static String formatString(String key, int res, Object... args) {
        return formatString(key, null, res, args);
    }

    private static String formatString(String key, String fallback, int res, Object... args) {
        try {
            final String value = getInstance().getStringV2(key, res, fallback);
            if (value == null) {
                return "LOC_ERR: " + key;
            }

            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, value, args);
            } else {
                return String.format(value, args);
            }
        } catch (Exception e) {
            FileLog.e(e);
            if (res != 0) {
                // a broken language pack string (bad placeholders): show the built-in one instead of LOC_ERR
                try {
                    return String.format(ApplicationLoader.applicationContext.getString(res), args);
                } catch (Exception ignore) {}
            }
            return "LOC_ERR: " + key;
        }
    }

    public static CharSequence formatSpannable(@StringRes int res, Object... args) {
        return formatSpannable(null, res, args);
    }

    public static CharSequence formatSpannable(String key, int res, Object... args) {
        return formatSpannable(key, null, res, args);
    }

    private static CharSequence formatSpannable(String key, String fallback, int res, Object... args) {
        try {
            final String value = getInstance().getStringV2(key, res, fallback);
            if (value == null) {
                return "LOC_ERR: " + key;
            }

            SpannableStringBuilder builder = new SpannableStringBuilder(value);
            for (int i = 0; i < args.length; i++) {
                String formatter = "s";
                CharSequence replaceWith = "";
                if (args[i] instanceof CharSequence) {
                    replaceWith = (CharSequence) args[i];
                } else if (args[i] instanceof Integer) {
                    formatter = "d";
                    replaceWith = "" + (Integer) args[i];
                } else if (args[i] instanceof Long) {
                    formatter = "d";
                    replaceWith = "" + (Long) args[i];
                } else if (args[i] == null) {
                    replaceWith = "null";
                }
                final String placeholder = "%" + (i + 1) + "$" + formatter;
                int start = builder.toString().indexOf(placeholder);
                if (start != -1) {
                    builder.replace(start, start + placeholder.length(), replaceWith);
                } else {
                    final String placeholder2 = "%" + formatter;
                    start = builder.toString().indexOf(placeholder2);
                    if (start != -1) {
                        builder.replace(start, start + placeholder2.length(), replaceWith);
                    }
                }
            }
            return builder;
        } catch (Exception e) {
            FileLog.e(e);
            return "LOC_ERR: " + key;
        }
    }

    public static String formatTTLString(int ttl) {
        if (ttl < 60) {
            return LocaleController.formatPluralString("Seconds", ttl);
        } else if (ttl < 60 * 60) {
            return LocaleController.formatPluralString("Minutes", ttl / 60);
        } else if (ttl < 60 * 60 * 24) {
            return LocaleController.formatPluralString("Hours", ttl / 60 / 60);
        } else if (ttl < 60 * 60 * 24 * 7) {
            return LocaleController.formatPluralString("Days", ttl / 60 / 60 / 24);
        } else if (ttl < 60 * 60 * 24 * 31) {
            int days = ttl / 60 / 60 / 24;
            if (ttl % 7 == 0) {
                return LocaleController.formatPluralString("Weeks", days / 7);
            } else {
                return String.format("%s %s", LocaleController.formatPluralString("Weeks", days / 7), LocaleController.formatPluralString("Days", days % 7));
            }
        } else if (ttl >= 60 * 60 * 24 * 365 && ttl % (60 * 60 * 24 * 365) == 0) {
            return LocaleController.formatPluralString("Years", ttl / 60 / 60 / 24 / 365);
        } else {
            return LocaleController.formatPluralString("Months", ttl / 60 / 60 / 24 / 30);
        }
    }

    private static char[] defaultNumbers = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    private static char[][] otherNumbers = new char[][]{
            {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'},
            {'۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'},
            {'०', '१', '२', '३', '४', '५', '६', '७', '८', '९'},
            {'૦', '૧', '૨', '૩', '૪', '૫', '૬', '૭', '૮', '૯'},
            {'੦', '੧', '੨', '੩', '੪', '੫', '੬', '੭', '੮', '੯'},
            {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'},
            {'೦', '೧', '೨', '೩', '೪', '೫', '೬', '೭', '೮', '೯'},
            {'୦', '୧', '୨', '୩', '୪', '୫', '୬', '୭', '୮', '୯'},
            {'൦', '൧', '൨', '൩', '൪', '൫', '൬', '൭', '൮', '൯'},
            {'௦', '௧', '௨', '௩', '௪', '௫', '௬', '௭', '௮', '௯'},
            {'౦', '౧', '౨', '౩', '౪', '౫', '౬', '౭', '౮', '౯'},
            {'၀', '၁', '၂', '၃', '၄', '၅', '၆', '၇', '၈', '၉'},
            {'༠', '༡', '༢', '༣', '༤', '༥', '༦', '༧', '༨', '༩'},
            {'᠐', '᠑', '᠒', '᠓', '᠔', '᠕', '᠖', '᠗', '᠘', '᠙'},
            {'០', '១', '២', '៣', '៤', '៥', '៦', '៧', '៨', '៩'},
            {'๐', '๑', '๒', '๓', '๔', '๕', '๖', '๗', '๘', '๙'},
            {'໐', '໑', '໒', '໓', '໔', '໕', '໖', '໗', '໘', '໙'},
            {'꧐', '꧑', '꧒', '꧓', '꧔', '꧕', '꧖', '꧗', '꧘', '꧙'}
    };

    public static String fixNumbers(CharSequence numbers) {
        StringBuilder builder = new StringBuilder(numbers);
        for (int c = 0, N = builder.length(); c < N; c++) {
            char ch = builder.charAt(c);
            if (ch >= '0' && ch <= '9' || ch == '.' || ch == ',') {
                continue;
            }
            for (int a = 0; a < otherNumbers.length; a++) {
                for (int b = 0; b < otherNumbers[a].length; b++) {
                    if (ch == otherNumbers[a][b]) {
                        builder.setCharAt(c, defaultNumbers[b]);
                        a = otherNumbers.length;
                        break;
                    }
                }
            }
        }
        return builder.toString();
    }

    public String formatCurrencyString(long amount, String type) {
        return formatCurrencyString(amount, true, true, false, type);
    }

    public String formatCurrencyString(long amount, boolean fixAnything, boolean withExp, boolean editText, String type) {
        type = type.toUpperCase();
        String customFormat;
        double doubleAmount;
        boolean discount = amount < 0;
        amount = Math.abs(amount);
        Currency currency = Currency.getInstance(type);
        switch (type) {
            case "XTR": // EVE-PATCH: replace StarsController constant
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "CLF":
                customFormat = " %.4f";
                doubleAmount = amount / 10000.0;
                break;

            case "IRR":
                doubleAmount = amount / 100.0f;
                if (fixAnything && amount % 100 == 0) {
                    customFormat = " %.0f";
                } else {
                    customFormat = " %.2f";
                }
                break;

            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                customFormat = " %.3f";
                doubleAmount = amount / 1000.0;
                break;

            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "MRO":
                customFormat = " %.1f";
                doubleAmount = amount / 10.0;
                break;

            default:
                customFormat = " %.2f";
                doubleAmount = amount / 100.0;
                break;
        }
        if (!withExp) {
            customFormat = " %.0f";
        }
        if (currency != null) {
            NumberFormat format = NumberFormat.getCurrencyInstance(currentLocale != null ? currentLocale : systemDefaultLocale);
            format.setCurrency(currency);
            if (editText) {
                format.setGroupingUsed(false);
            }
            if (!withExp || fixAnything && type.equals("IRR")) {
                format.setMaximumFractionDigits(0);
            }
            String result = (discount ? "-" : "") + format.format(doubleAmount);
            int idx = result.indexOf(type);
            if (idx >= 0) {
                idx += type.length();
                if (idx < result.length() && result.charAt(idx) != ' ') {
                    result = result.substring(0, idx) + " " + result.substring(idx);
                }
            }
            return result;
        }
        return (discount ? "-" : "") + String.format(Locale.US, type + customFormat, doubleAmount);
    }

    public static int getCurrencyExpDivider(String type) {
        switch (type) {
            case "CLF":
                return 10000;
            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                return 1000;
            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                return 1;
            case "MRO":
                return 10;
            default:
                return 100;
        }
    }

    public String formatCurrencyDecimalString(long amount, String type, boolean inludeType) {
        type = type.toUpperCase();
        String customFormat;
        double doubleAmount;
        amount = Math.abs(amount);
        switch (type) {
            case "CLF":
                customFormat = " %.4f";
                doubleAmount = amount / 10000.0;
                break;

            case "IRR":
                doubleAmount = amount / 100.0f;
                if (amount % 100 == 0) {
                    customFormat = " %.0f";
                } else {
                    customFormat = " %.2f";
                }
                break;

            case "BHD":
            case "IQD":
            case "JOD":
            case "KWD":
            case "LYD":
            case "OMR":
            case "TND":
                customFormat = " %.3f";
                doubleAmount = amount / 1000.0;
                break;

            case "BIF":
            case "BYR":
            case "CLP":
            case "CVE":
            case "DJF":
            case "GNF":
            case "ISK":
            case "JPY":
            case "KMF":
            case "KRW":
            case "MGA":
            case "PYG":
            case "RWF":
            case "UGX":
            case "UYI":
            case "VND":
            case "VUV":
            case "XAF":
            case "XOF":
            case "XPF":
                customFormat = " %.0f";
                doubleAmount = amount;
                break;

            case "MRO":
                customFormat = " %.1f";
                doubleAmount = amount / 10.0;
                break;

            default:
                customFormat = " %.2f";
                doubleAmount = amount / 100.0;
                break;
        }
        return String.format(Locale.US, inludeType ? type : "" + customFormat, doubleAmount).trim();
    }

    public static String formatStringSimple(String string, Object... args) {
        try {
            if (getInstance().currentLocale != null) {
                return String.format(getInstance().currentLocale, string, args);
            } else {
                return String.format(string, args);
            }
        } catch (Exception e) {
            FileLog.e(e);
            return "LOC_ERR: " + string;
        }
    }

    public static String formatDuration(int duration) {
        if (duration <= 0) {
            return formatPluralString("Seconds", 0);
        }
        final int hours = duration / 3600;
        final int minutes = duration / 60 % 60;
        final int seconds = duration % 60;
        final StringBuilder stringBuilder = new StringBuilder();
        if (hours > 0) {
            stringBuilder.append(formatPluralString("Hours", hours));
        }
        if (minutes > 0) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(formatPluralString("Minutes", minutes));
        }
        if (seconds > 0) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(formatPluralString("Seconds", seconds));
        }
        return stringBuilder.toString();
    }

    public static String formatCallDuration(int duration) {
        if (duration > 3600) {
            String result = LocaleController.formatPluralString("Hours", duration / 3600);
            int minutes = duration % 3600 / 60;
            if (minutes > 0) {
                result += ", " + LocaleController.formatPluralString("Minutes", minutes);
            }
            return result;
        } else if (duration > 60) {
            return LocaleController.formatPluralString("Minutes", duration / 60);
        } else {
            return LocaleController.formatPluralString("Seconds", duration);
        }
    }

    public static String formatShortDuration(int duration) {
        final int hours = duration / 3600;
        final int minutes = duration / 60 % 60;
        final int seconds = duration % 60;
        final StringBuilder stringBuilder = new StringBuilder();
        if (hours > 0) {
            if (stringBuilder.length() > 0) stringBuilder.append(":");
            stringBuilder.append(hours >= 10? "" : "0");
            stringBuilder.append(hours);
        }
        if (stringBuilder.length() > 0) stringBuilder.append(":");
        stringBuilder.append(minutes >= 10 ? "" : "0");
        stringBuilder.append(minutes);
        if (stringBuilder.length() > 0) stringBuilder.append(":");
        stringBuilder.append(seconds >= 10 ? "" : "0");
        stringBuilder.append(seconds);
        return stringBuilder.toString();
    }


    public static String formatShortDuration2(int time) {
        final int minutes = time / 60;
        final int hours = time / 3600;

        if (hours > 0) {
            return LocaleController.formatPluralString("ShortHoursAgo", hours) + " " + LocaleController.formatPluralString("ShortMinutesAgo", minutes % 60);
        }

        return LocaleController.formatPluralString("ShortMinutesAgo", minutes);
    }

    public static boolean isRTLCharacter(char ch) {
        return Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING || Character.getDirectionality(ch) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE;
    }

    public static String formatShortNumber(int number, int[] rounded) {
        StringBuilder K = new StringBuilder();
        int lastDec = 0;
        int KCount = 0;
        while (number / 1000 > 0) {
            K.append("K");
            lastDec = (number % 1000) / 100;
            number /= 1000;
        }
        if (rounded != null) {
            double value = number + lastDec / 10.0;
            for (int a = 0; a < K.length(); a++) {
                value *= 1000;
            }
            rounded[0] = (int) value;
        }
        if (lastDec != 0 && K.length() > 0) {
            if (K.length() == 2) {
                return String.format(Locale.US, "%d.%dM", number, lastDec);
            } else {
                return String.format(Locale.US, "%d.%d%s", number, lastDec, K.toString());
            }
        }
        if (K.length() == 2) {
            return String.format(Locale.US, "%dM", number);
        } else {
            return String.format(Locale.US, "%d%s", number, K.toString());
        }
    }

    private static final int SAVE_SKIPPED = 0;
    private static final int SAVE_REQUEST_DIFFERENCE = 1;
    private static final int SAVE_REQUEST_LANGPACK = 2;

    private DispatchQueue langpackQueue;
    // version of each saved langpack file; owned by langpackQueue, which can be ahead of LocaleInfo
    private final HashMap<String, Integer> savedLangpackVersions = new HashMap<>();

    // one langpack request per file at a time: every account checks for updates on its own,
    // and without this each of them downloads the same difference or the whole langpack
    private static class LangpackRequest {
        boolean full;
        long startTime;
        int token;
        int account;
        // callers that wanted the same langpack while this was loading
        ArrayList<Runnable> waiters = new ArrayList<>();

    }

    private final HashMap<String, LangpackRequest> langpackRequestsInFlight = new HashMap<>();

    abstract public static class PluralRules {
        abstract int quantityForNumber(int n);
    }

    public static class PluralRules_Zero extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0 || count == 1) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Welsh extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (count == 3) {
                return QUANTITY_FEW;
            } else if (count == 6) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Two extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Tachelhit extends PluralRules {
        public int quantityForNumber(int count) {
            if (count >= 0 && count <= 1) {
                return QUANTITY_ONE;
            } else if (count >= 2 && count <= 10) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Slovenian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (rem100 == 1) {
                return QUANTITY_ONE;
            } else if (rem100 == 2) {
                return QUANTITY_TWO;
            } else if (rem100 >= 3 && rem100 <= 4) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Romanian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if ((count == 0 || (rem100 >= 1 && rem100 <= 19))) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Polish extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) {
                return QUANTITY_FEW;
            } else if (rem10 >= 0 && rem10 <= 1 || rem10 >= 5 && rem10 <= 9 || rem100 >= 12 && rem100 <= 14) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_One extends PluralRules {
        public int quantityForNumber(int count) {
            return count == 1 ? QUANTITY_ONE : QUANTITY_OTHER;
        }
    }

    public static class PluralRules_None extends PluralRules {
        public int quantityForNumber(int count) {
            return QUANTITY_OTHER;
        }
    }

    public static class PluralRules_Maltese extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 0 || (rem100 >= 2 && rem100 <= 10)) {
                return QUANTITY_FEW;
            } else if (rem100 >= 11 && rem100 <= 19) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Macedonian extends PluralRules {
        public int quantityForNumber(int count) {
            if (count % 10 == 1 && count != 11) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Lithuanian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Latvian extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count % 10 == 1 && count % 100 != 11) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Langi extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_French extends PluralRules {
        public int quantityForNumber(int count) {
            if (count >= 0 && count < 2) {
                return QUANTITY_ONE;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Czech extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 1) {
                return QUANTITY_ONE;
            } else if (count >= 2 && count <= 4) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Breton extends PluralRules {
        public int quantityForNumber(int count) {
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (count == 3) {
                return QUANTITY_FEW;
            } else if (count == 6) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Balkan extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && rem100 != 11) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) {
                return QUANTITY_FEW;
            } else if ((rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14))) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Serbian extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            int rem10 = count % 10;
            if (rem10 == 1 && rem100 != 11) {
                return QUANTITY_ONE;
            } else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) {
                return QUANTITY_FEW;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static class PluralRules_Arabic extends PluralRules {
        public int quantityForNumber(int count) {
            int rem100 = count % 100;
            if (count == 0) {
                return QUANTITY_ZERO;
            } else if (count == 1) {
                return QUANTITY_ONE;
            } else if (count == 2) {
                return QUANTITY_TWO;
            } else if (rem100 >= 3 && rem100 <= 10) {
                return QUANTITY_FEW;
            } else if (rem100 >= 11 && rem100 <= 99) {
                return QUANTITY_MANY;
            } else {
                return QUANTITY_OTHER;
            }
        }
    }

    public static String addNbsp(String src) {
        return src.replace(' ', '\u00A0');
    }

    public static String getTimeZoneName(String timeZoneId, boolean full) {
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        if (timeZone == null) return "";
        String name = timeZone.getDisplayName(true, TimeZone.SHORT, getInstance().getCurrentLocale());
        if (full) {
            String longname = timeZone.getDisplayName(true, TimeZone.LONG, getInstance().getCurrentLocale());
            if (!TextUtils.equals(longname, name)) {
                name = longname + ", " + name;
            }
        }
        return name;
    }

    // -------- API 24+ localized relative formatter --------
    @RequiresApi(api = Build.VERSION_CODES.N)
    private static final class RelativeIcu {
        static String format(long diffMillis, Locale locale) {
            RelativeDateTimeFormatter f = RelativeDateTimeFormatter.getInstance(locale);

            final boolean future = diffMillis > 0;
            final long absSeconds = Math.max(1, Math.round(Math.abs(diffMillis) / 1000.0));
            final RelativeDateTimeFormatter.RelativeUnit unit;
            final long value;

            if (absSeconds < 60) {
                unit = RelativeDateTimeFormatter.RelativeUnit.SECONDS;
                value = absSeconds;
            } else if (absSeconds < 3600) {
                unit = RelativeDateTimeFormatter.RelativeUnit.MINUTES;
                value = Math.round(absSeconds / 60.0);
            } else if (absSeconds < 86400) {
                unit = RelativeDateTimeFormatter.RelativeUnit.HOURS;
                value = Math.round(absSeconds / 3600.0);
            } else if (absSeconds < 86400L * 30L) {
                unit = RelativeDateTimeFormatter.RelativeUnit.DAYS;
                value = Math.round(absSeconds / 86400.0);
            } else if (absSeconds < 86400L * 365L) {
                unit = RelativeDateTimeFormatter.RelativeUnit.MONTHS;
                value = Math.round(absSeconds / (86400.0 * 30.0));
            } else {
                unit = RelativeDateTimeFormatter.RelativeUnit.YEARS;
                value = Math.round(absSeconds / (86400.0 * 365.0));
            }

            RelativeDateTimeFormatter.Direction dir = future ?
                RelativeDateTimeFormatter.Direction.NEXT :
                RelativeDateTimeFormatter.Direction.LAST;

            // "in X days" / "X days ago" in the user locale
            return f.format(value, dir, unit);
        }
    }

    @Nullable
    private String getStringV2(String key, @StringRes int stringRes, String fallback) {
        final Context context = ApplicationLoader.applicationContext;
        String value;

        return com.eve.app.bridge.TelegramBridge.resolveString(context, key, stringRes, fallback); // EVE-PATCH: resolve strings via Eve bridge without reflection

    }


    /* */

}

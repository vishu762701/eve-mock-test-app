# TELEGRAM_PARITY_LEDGER.md

> Honest, code-verified audit and implementation ledger for transforming the EVE Android app (`com.eve.app`) onto the official Telegram Android UI (`Telegram-Android-master` @ `21f7d109da6ab3506c9e9577f98c6a38771ab335`).

## 1. Environment & Verification Results (Pass 1 — Foundation Lock)

- **Telegram Official Source**: `Telegram-Android-master` (`21f7d109da6ab3506c9e9577f98c6a38771ab335`, LITE copy without native/jni, media3, jlatexmath).
- **Gate & Static Review Results (Verified by Static Review — NOT compiled or tested locally)**:
  - `python3 tools/port_gate.py --check --upstream <tg-src>` — **PASS** (5 screens, 143 files verified, exit code 0).
  - `python3 tools/check_no_shortcuts.py` — **PASS** (0 shortcuts, wrappers, or governance omissions, exit code 0).
  - `python3 tools/check_no_stubs.py` — **PASS** (0 stubs or false claims, exit code 0).
  - **Gradle / `javac` / AAPT2 / Unit & Instrumentation Tests**: **NOT RUN** in sandbox VM (`javac` absent, `ANDROID_HOME` unset, internet disabled). Must run in GitHub Actions CI (`Build Eve APK`).

---

## 2. Code-Verified 61-Activity Breakdown (Honest Status)

| Tier | Count | Activities |
|---|---|---|
| `REAL_TG_FRAG` (Ported `org.telegram.ui.*` Fragment) | **7** | `ChangeBioActivity`, `ChangeNameActivity`, `ChangeUsernameActivity`, `LanguageSelectActivity`, `PasscodeActivity`, `PhotoCropActivity`, `TelegramHostActivity` |
| `REAL_TG_VIEWS` (Pure Programmatic `org.telegram.ui.*`, `xml=False`, `ActionBar` + `Theme.getColor`) | **29** | `AboutActivity`, `AdminRightsEditActivity`, `AdminsAndRightsActivity`, `ApiUsageActivity`, `AppConfigActivity`, `CacheControlActivity`, `ChangePhoneActivity`, `ContentDisplayActivity`, `EditAboutActivity`, `ExamTestsActivity`, `FeedbackActivity`, `GeneratedTestsActivity`, `HistoryActivity`, `LeaderboardActivity`, `MainActivity`, `ManageExistingExamsActivity`, `MistakesActivity`, `NotificationsActivity`, `PaymentCheckoutActivity`, `PerformanceActivity`, `PracticeActivity`, `PremiumActivity`, `ProfileActivity`, `PyqActivity`, `ResultDetailActivity`, `SettingsActivity`, `SyllabusActivity`, `SystemMonitorActivity`, `ThemeActivity` |
| `XML_HYBRID` (Still inflates XML layout, partial `org.telegram.ui.*`) | **4** | `EmojiStudioActivity`, `FeedbackMessagesActivity`, `ManagePollsActivity`, `PollCreateActivity` |
| `XML_KOTLIN_TG_DS` (XML-based / EVE Design System — NOT yet `REAL_TG_VIEWS`) | **17** | `ActivityLogActivity`, `AdminActivity`, `AdminAnalyticsActivity`, `BookmarksActivity`, `EditExamActivity`, `FlaggedQuestionsActivity`, `LoginActivity`, `ManageExamsActivity`, `ManagePremiumActivity`, `ManageQuestionsActivity`, `ManageSyllabusActivity`, `ManageUsersActivity`, `QrShareActivity`, `ResultActivity`, `SendNotificationActivity`, `TestActivity`, `UsernameCardActivity` |
| `BASE_OR_ALIAS` (Base class / Compatibility Alias / Specialized Scanner) | **4** | `AppearanceActivity`, `AvatarCropActivity`, `EveBaseActivity`, `EveQrLiveScannerActivity` |

> **Correction Notice**: Previously, `MainActivity`, `ProfileActivity`, and `LoginActivity` were inaccurately listed under `REAL_TG_VIEWS`. Code inspection confirms `MainActivity.kt` (`activity_main.xml`), `ProfileActivity.kt` (`activity_profile.xml`), `TestActivity.kt`, `ResultActivity.kt`, `BookmarksActivity.kt`, `LoginActivity.kt` (untouched per scope), and the remaining XML-based Admin/QR screens inflate XML layouts or EVE design-system views and have `0` calls to `Theme.getColor()`.

---

## 3. Pass 1 — Foundation Lock Changes

1. **Upstream `.attheme`, Fonts, Selectors & `colors.xml` (32 verbatim upstream files)**:
   - `assets/{day,night,darkblue,arctic,bluebubbles}.attheme` & `assets/themes/*.attheme`
   - `assets/fonts/{rmedium,ritalic,rmediumitalic,rcondensedbold,rextrabold,rmono,rmono_var,mw_bold,mw_bolditalic,gram}.ttf`, `num.otf`, `currency_symbols.txt`
   - `res/{drawable,drawable-v21}/{bar_selector_style,bar_selector_white,bar_selector_lock,list_selector_ex}.xml`
   - `res/{values,values-night}/colors.xml`
2. **`Theme.java` (`docs/port-recipes/Theme.cut`) & `TelegramThemeEngine.kt`**:
   - Null-checked `currentColors` and `currentColorsNoAccent` in `syncFromEvePalette`.
   - Populated BOTH `currentColors` (accent-shifted) and `currentColorsNoAccent` (unshifted `.attheme`) across all 929 keys via `ThemeColors.createDefaultColors()` + `ThemeColors.stringKeyToInt()`.
   - Cleared `animatingColors` and refreshed cached `Paint`/`Drawable` instances (`dividerPaint`, `dividerExtraPaint`, `linkSelectionPaint`, `checkboxSquare_checkPaint`, `checkboxSquare_backgroundPaint`, `dialogs_onlineCirclePaint`, `profile_aboutTextPaint`, `listSelector`).
3. **`EveApplication.kt` & `AndroidUtilities.java` (`docs/port-recipes/AndroidUtilities.cut`)**:
   - Confirmed `ApplicationLoader.applicationContext = this` is set first in `EveApplication.onCreate()` and added `Theme.syncFromEvePalette(this)` immediately after `TelegramThemeEngine.init(this)`.
   - Guarded `AndroidUtilities.getTypeface(String)` against null `ApplicationLoader.applicationContext`.
4. **Theme Switch & System Bars**:
   - Aligned `ThemeSwitchAnimator.kt` and `ThemeManager.kt` to upstream `LaunchActivity.java` `450L` ms + `Easings.easeInOutQuad`, plus `Theme.syncFromEvePalette` and `AndroidUtilities.setLightStatusBar`/`setLightNavigationBar`.
5. **`AlertDialog` Migration Hardening (13 Activities)**:
   - Audited every `setPositiveButton`, `setNegativeButton`, `setNeutralButton`, `setItems`, `dialog.dismiss()`, `makeRed()`, and `getButton()` call across all 13 migrated Activities.
   - Replaced `requireNotNull(dialog.getButton(...))` in `ResultActivity.kt` with a null-safe cast (`as? android.widget.TextView ?: return@setOnShowListener`).
6. **ProGuard & CI Workflow Verification**:
   - Confirmed `-keep class org.telegram.** { *; }` is present in `app/proguard-rules.pro`.
   - Confirmed `.github/workflows/build.yml` & `release.yml` install `platforms;android-36` + `build-tools;36.0.0` with JDK 21 and Gradle 8.7 (matching `compileSdk = 36` + AGP `8.5.2`; no Gradle version changes required).
7. **Zero Deletions in Pass 1**:
   - No legacy XML layouts, tests, or shadow classes were deleted in Pass 1.

# Telegram parity sweep

Source is the owner-supplied reference extracted outside git. Telegram paths below
are paths inside that archive, not files committed to EVE. The original archive
provenance SHA-256 is `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`.
The repacked UI/security ZIP used here has SHA-256
`fabf0b7370abf2ee5661063ea1c274cb3e537048f008a6871ff3169cbe89cb1e`.
Its manifest verification is separate from runtime fidelity.

## A1 — Loading circles

| Property | EVE file:line | Telegram file:line | Value | Action |
|---|---|---|---|---|
| Profile progress dialog | `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt:327` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | 40dp arc, 3dp round stroke | Replaced platform spinner; retained dialog/callback behavior |
| Settings progress dialog | `app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt:300` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | 40dp arc, 3dp round stroke | Replaced platform spinner; retained dialog/callback behavior |
| Edit question reformat binding | `app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt:373` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | TelegramRadialProgressView | Fixed stale ProgressBar cast to match existing XML |
| System Monitor | `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt:44` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | TelegramRadialProgressView | Already migrated; removed unused ProgressBar import |
| Shared loading container | `app/src/main/java/com/eve/app/designsystem/DesignLoadingView.kt:13` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | TelegramRadialProgressView child | Already migrated; retained full-screen container API |
| activity_main.xml | `app/src/main/res/layout/activity_main.xml:52` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| activity_history.xml | `app/src/main/res/layout/activity_history.xml:15` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| activity_manage_existing_exams.xml | `app/src/main/res/layout/activity_manage_existing_exams.xml:21` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| activity_test.xml | `app/src/main/res/layout/activity_test.xml:14` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| Generic loading style | `app/src/main/res/values/design_widgets.xml:107` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | Sizing-only style | Removed obsolete platform ProgressBar parent; leaf owns painting |
| Poll result percentage | `app/src/main/java/com/eve/app/ui/admin/PollsAdminAdapter.kt:207`; `app/src/main/res/layout/item_poll_option_result.xml:7` | NOT FOUND: EVE poll metric counterpart | Determinate, max 100 | Preserved; not an indeterminate spinner |
| Result metric item_answer.xml | `app/src/main/res/layout/item_answer.xml:50` | NOT FOUND: EVE exam metric counterpart | Determinate | Preserved |
| Result metric item_topic_stat.xml | `app/src/main/res/layout/item_topic_stat.xml:7` | NOT FOUND: EVE exam metric counterpart | Determinate | Preserved |

The complete production sweep leaves four Material circles, all explicitly
`android:indeterminate="false"`: two answer-time metrics, topic accuracy, and
poll percentage. No platform `ProgressBar(...)` loading constructors remain.
`Widget.Eve.Token.Ring` stays available for these determinate metrics. No
horizontal progress bar was changed. The existing radial leaf has EVE XML,
palette, drawable and static-capture glue; a green build does not prove exact
runtime parity of that adaptation. Its source motion values remain unchanged.

## A2 — Login

| Property | EVE file:line | Telegram file:line | Value | Action |
|---|---|---|---|---|
| Login title | `app/src/main/res/values/telegram_typography.xml:119` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java:2021–2027`; `Telegram-master/TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java:262–270` | 18sp, bundled Medium, normal style | Removed synthetic bold; dp→sp is an accessibility deviation |
| Subtitle | `app/src/main/res/layout/activity_login.xml:8` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java:2050–2055` | 14sp, centered, 2dp line spacing, 32dp side insets, 8dp top gap | Preserved exact values; removed extra 28dp bottom gap |
| Title placement | `app/src/main/res/layout/activity_login.xml:7` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java:2021–2027` | 32dp inner insets, 2dp line spacing, no top margin | Removed invented 36dp title margin; moved EVE header outside centered content |
| Phone/tablet outer insets | `app/src/main/res/values/telegram_login_dimens.xml:4` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java:692` | 18dp phone / 26dp sw600dp; 30dp content top margin | Replaced shared 12dp screen insets locally; no global token changes |
| Next-action bounds | `app/src/main/res/layout/activity_login.xml:27` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/FragmentFloatingButton.java:166–170,188–192` | 56dp container; 48dp painted height from button insets; capsule radius 24dp | Corrected local button insets from 2dp to 4dp (48dp painted height); full-width Google action is EVE adaptation, not Telegram phone arrow |
| Keyboard | `app/src/main/AndroidManifest.xml:44` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java:3371–3397` | No initial IME for Google-only page; resize if legacy fields are revealed | Explicit adaptation: Telegram opens phone keyboard, EVE has no visible input |

### Needs my decision — legacy email/password

Email/password controls and all handlers remain intact. `app/src/main/java/com/eve/app/ui/login/LoginActivity.kt:118–128`
binds submit, mode toggle and IME Done; `LoginActivity.kt:211–266` contains Firebase email sign-in
and account creation; `LoginActivity.kt:282–297` shares post-auth admin lookup with Google.
`AdminRepository.kt:14–59` authorizes by account identity/cache/API, not by a
Google-only provider rule. Existing Firebase email users therefore cannot be
assumed migrated to Google, and no account data or backend authorization changed.
`ProfileSettingsLoginTypographyAndColorTest.kt:161–188` still requires the legacy
fields/buttons; `StudentUiRenderingTest.kt:190` exercises the signup fixture.
Owner decision: restore an accessible email-login route or define and validate
an existing-user/admin account-linking migration. Until then those views stay
hidden and their code is retained.

### Explicit login deviations

Google authentication, EVE branding/content and a full-width Google action replace
Telegram phone entry and its floating arrow. EVE uses sp, accessible foregrounds,
48dp back-button touch bounds, and a retained EVE header. Telegram's custom phone
keyboard, passkey prompt, country-picker text animation and inter-fragment login
animations have no counterpart on this Google-only first page (NOT FOUND).
No animation was invented. Hidden legacy mode-switch motion remains unchanged
pending the email-login decision; it is not claimed as source-exact first-page motion.

## A3 — Floating tabs

| Property | EVE file:line | Telegram file:line | Value | Action |
|---|---|---|---|---|
| Unselected tab font | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:116` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java:92,237; messenger/AndroidUtilities.java:262–270` | bundled Medium 500 | Fixed incorrect 700 and corrected comparison document |
| Selected tab font | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:232` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java:237` | 800 on API 28+ | Preserved numeric weight; platform glyph substitute remains an explicit deviation |
| Weighted-width rounding | `app/src/main/java/com/eve/app/ui/navigation/MainTabsLayout.kt:180` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/MainTabsLayout.java:141` | Math.round | Replaced truncation |
| Density geometry | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:302` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java:2701–2705` | ceil(density × dp) | Corrected per-density rounding in tab leaf and sizing helper |
| Selector visual width | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:273` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java:152–160` | visualWidth when supplied | Fixed use of measured width |
| Label font padding | `app/src/main/java/com/eve/app/ui/navigation/GlassTabView.kt:111` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java:87–96` | Android default font padding | Removed unsupported false override |

The complete geometry/property comparison remains in
`docs/telegram-floating-tabs-source-comparison.md`, checked against extracted
source rather than accepted as authority. Outer placement, 56dp height, 344dp cap,
8dp margins + navigation inset, three 12/12/10dp text passes with 16/8/4dp padding,
24dp icons, 22dp avatars, 28.33dp label position, 320ms selection and 0.09 alpha
were rechecked. Existing 0dp vertical padding and four EVE destinations are
explicit adaptations. Existing Lottie files and triggers are untouched.

Bottom-placement regression:
`app/src/androidTest/java/com/eve/app/ui/TelegramAppearanceAndThemeAuthorityNativeTest.kt:79`
(`actualNavigationHostKeepsBottomGravityAfterNavigationInsets`) checks gravity,
real lower-half placement, actual bottom coordinate, confined glass background,
inset accounting, height and max width after window traversal.
`.github/workflows/build.yml` runs the native suite with both `gesture` and
`three-button` navigation modes at the final mission gate. A green compile alone
does not prove these runtime or visual claims.

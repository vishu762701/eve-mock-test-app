# TELEGRAM_PARITY_LEDGER.md

> Cumulative audit and implementation ledger for transforming the EVE Android app (`com.eve.app`) into a faithful Telegram Android UI/UX implementation using ONLY the existing EVE repository and the official Telegram Android source archive (`Telegram-Android-master` @ `21f7d109da6ab3506c9e9577f98c6a38771ab335`).

## 1. Environment & Verification Results (Actual PASS/FAIL)

- **Starting Baseline Commit**: `258cfb3` (`eve-mock-test-app-main.zip`: 625 Java/Kotlin files, 61 Activities, 143 ported `org.telegram.*` files).
- **Telegram Official Source**: `Telegram-Android-master-unchanged.tar.zst` (`21f7d109da6ab3506c9e9577f98c6a38771ab335`).
- **Verification Results**:
  - `python3 tools/port_gate.py --check --upstream <tg-src>` — **PASS** (5 screens, 143 files verified, exit code 0).
  - `python3 tools/check_no_shortcuts.py` — **PASS** (0 shortcuts, wrappers, or governance omissions, exit code 0).
  - `python3 tools/check_no_stubs.py` — **PASS** (0 stubs or false claims, exit code 0).
  - Static Kotlin/Import/Drawable/String/Delimiter Audit across all 20 modified Kotlin files — **PASS** (0 missing `org.telegram.*` imports, 0 missing `R.drawable.*`/`R.string.*`, 19/19 rewritten files balanced; `ProfileActivity.kt` touched only 4 symbol names).
  - Gradle Build (`./gradlew assembleDebug`), Android Lint, Unit/Instrumentation Tests, APK Generation — **NOT RUN / BLOCKED BY ENVIRONMENT** (sandbox VM has OpenJDK 17 `java` runtime only: `javac` missing, `ANDROID_HOME` unset, outbound internet disabled). Must be executed in GitHub Actions CI (`FAST` / `FULL` workflows).

---

## 2. Updated 61-Activity Breakdown (Honest Status)

| Tier | Count | Activities |
|---|---|---|
| `REAL_TG_FRAG` (Ported `org.telegram.ui.*` Fragment) | **7** | `TelegramHostActivity`, `ChangeNameActivity`, `ChangeBioActivity`, `ChangeUsernameActivity`, `PhotoCropActivity`, `LanguageSelectActivity`, `PasscodeActivity` |
| `REAL_TG_VIEWS` (Real `org.telegram.ui.*` Views/ActionBar/Cells) | **31** (+17 in Phases 1–4) | `AboutActivity`, `ContentDisplayActivity`, `FeedbackActivity`, `HistoryActivity`, `LeaderboardActivity`, `PerformanceActivity`, `ChangePhoneActivity`, `EveQrLiveScannerActivity`, `ProfileActivity`, `CacheControlActivity`, `SettingsActivity`, `ThemeActivity`, `LoginActivity`, `MainActivity`, **`NotificationsActivity`**, **`SyllabusActivity`**, **`ExamTestsActivity`**, **`ResultDetailActivity`**, **`PremiumActivity`**, **`PaymentCheckoutActivity`**, **`ApiUsageActivity`**, **`ManageExistingExamsActivity`**, **`EmojiStudioActivity`**, **`PracticeActivity`**, **`MistakesActivity`**, **`PyqActivity`**, **`AdminsAndRightsActivity`**, **`FeedbackMessagesActivity`**, **`GeneratedTestsActivity`**, **`ManagePollsActivity`**, **`SystemMonitorActivity`** |
| `KOTLIN_TG_DS` (EVE Telegram Design System — Pending Full `org.telegram.ui.*` Rewrite) | **21** (down from 30) | `EveBaseActivity`, `BookmarksActivity`, `PollCreateActivity`, `ResultActivity`, `TestActivity`, `QrShareActivity`, `UsernameCardActivity`, `AdminActivity`, `ActivityLogActivity`, `AdminAnalyticsActivity`, `AdminRightsEditActivity`, `AppConfigActivity`, `EditAboutActivity`, `EditExamActivity`, `FlaggedQuestionsActivity`, `ManageExamsActivity`, `ManagePremiumActivity`, `ManageQuestionsActivity`, `ManageSyllabusActivity`, `ManageUsersActivity`, `SendNotificationActivity` |
| Compatibility Aliases | **2** (down from 10 `LEGACY_XML`) | `AppearanceActivity` (6-line subclass of `ThemeActivity`), `AvatarCropActivity` (0 active callers; `ProfileActivity` now launches ported `PhotoCropActivity`) |

---

## 3. Motion Emoji & Sticker End-to-End Parity (Phase 3 — DONE)

- **`EmojiRenderer.kt`** (`app/src/main/java/com/eve/app/ui/admin/emoji/EmojiRenderer.kt`):
  - Fixed critical `activeAnimators` starvation leak (attached `AnimatorListenerAdapter` to release `LottieDrawable` on `onAnimationEnd`/`onAnimationCancel` so animations never freeze after 8 plays).
  - Fixed RecyclerView recycle race via atomic bind tokens (`bindTokens`) on `attachEmoji`.
  - Added `pauseAll()` / `resumeAll()` lifecycle hooks, unified `bindPreview()` with reduced-motion respect and Unicode alt fallback on load error, and persisted `Recent` motion emoji store (`recordRecent` / `getRecentIds`, max 24 items, matching Telegram `MediaDataController` recent order).
- **`EmojiGridAdapter.kt`** (`app/src/main/java/com/eve/app/ui/admin/emoji/EmojiGridAdapter.kt`):
  - Upgraded tile selector to real `org.telegram.ui.ActionBar.Theme.createRadSelectorDrawable`, added `org.telegram.ui.Components.ButtonBounce`, `onViewRecycled` animation cancellation, and `EmojiRenderer.bindPreview` fallback.
- **`EmojiStudioActivity.kt`** (`app/src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt`):
  - Migrated to real ported `org.telegram.ui.ActionBar.ActionBar` (with `ActionBarMenuItem` live search matching alt/pack/slug/source/ID + refresh), `RecyclerListView` (4-column `GridLayoutManager`), `EmptyTextProgressView`, and `ButtonWithCounterView` (`Manage Packs` & `Import Emoji`).
  - Added **"Recent"** category tab + **Telegram `ContentPreviewViewer`-style long-press animated preview dialog** (large looping preview + metadata + direct `Apply Placement` CTA) while preserving all placement, pack manager, Noto/Telegram/custom upload workflows and RBAC.

---

## 4. Exact File Inventory (Cumulative Against Baseline Commit `258cfb3`)

### Added Files (1)
- `TELEGRAM_PARITY_LEDGER.md`

### Modified Files (22)
1. `app/src/main/java/org/telegram/ui/ActionBar/Theme.java` (rebuilt from `docs/port-recipes/Theme.cut`)
2. `docs/port-manifest.json` (re-recorded hash for `Theme.java`)
3. `app/src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt` (`ActionBar`, `RecyclerListView`, `HeaderCell`, `TextDetailSettingsCell`, `EmptyTextProgressView`)
4. `app/src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt` (`ActionBar` + `ActionBarMenuItem` search, `RecyclerListView`, `TextDetailSettingsCell`, `EmptyTextProgressView`, `ButtonWithCounterView`)
5. `app/src/main/java/com/eve/app/ui/home/ExamTestsActivity.kt` (`ActionBar`, `RecyclerListView`, `HeaderCell`, `TextCell`, `TextDetailSettingsCell`, `EmptyTextProgressView`, `ButtonWithCounterView`)
6. `app/src/main/java/com/eve/app/ui/result/ResultDetailActivity.kt` (`ActionBar`, `HeaderCell`, `TextDetailSettingsCell`, `ShadowSectionCell`, `TextInfoPrivacyCell`, `ButtonWithCounterView`)
7. `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt` (updated crop/theme launches to `PhotoCropActivity` & `ThemeActivity`)
8. `app/src/main/java/com/eve/app/ui/premium/PremiumActivity.kt` (`ActionBar`, `HeaderCell`, `TextCell`, `TextDetailSettingsCell`, `EmptyTextProgressView`, `ButtonWithCounterView`)
9. `app/src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt` (`ActionBar`, `HeaderCell`, `TextDetailSettingsCell`, `TextCell`, `ButtonWithCounterView`, `TextInfoPrivacyCell`)
10. `app/src/main/java/com/eve/app/ui/admin/ApiUsageActivity.kt` (`ActionBar`, `HeaderCell`, `TextDetailSettingsCell`, `EmptyTextProgressView`)
11. `app/src/main/java/com/eve/app/ui/admin/ManageExistingExamsActivity.kt` (`ActionBar` + `ActionBarMenuItem` search, `RecyclerListView`, `EmptyTextProgressView`)
12. `app/src/main/java/com/eve/app/ui/admin/emoji/EmojiRenderer.kt` (fixed animation starvation & recycle race, added `bindPreview` & Recent store)
13. `app/src/main/java/com/eve/app/ui/admin/emoji/EmojiGridAdapter.kt` (Telegram selector, `ButtonBounce`, recycle cancel, fallback)
14. `app/src/main/java/com/eve/app/ui/admin/emoji/EmojiStudioActivity.kt` (`ActionBar` + search, `RecyclerListView`, `EmptyTextProgressView`, `ButtonWithCounterView`, Recent tab, live looping long-press preview)
15. `app/src/main/java/com/eve/app/ui/practice/PracticeActivity.kt` (`ActionBar`, `AlertDialog` exam picker, `RecyclerListView`, `HeaderCell`, `TextCell`, `TextDetailSettingsCell`)
16. `app/src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt` (`ActionBar`, `RecyclerListView`, `EmptyTextProgressView`, `ButtonWithCounterView`, `TelegramSegmentedTabs`)
17. `app/src/main/java/com/eve/app/ui/pyq/PyqActivity.kt` (`ActionBar`, `AlertDialog` exam picker, `RecyclerListView`, `HeaderCell`, `TextCell`, `TextDetailSettingsCell`, `TelegramSegmentedTabs`)
18. `app/src/main/java/com/eve/app/ui/admin/AdminsAndRightsActivity.kt` (`ActionBar`, `HeaderCell`, `EditTextBoldCursor`, `ButtonWithCounterView`, `RecyclerListView`, `EmptyTextProgressView`)
19. `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt` (`ActionBar`, `RecyclerListView`, `EmptyTextProgressView`, `ButtonWithCounterView`)
20. `app/src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt` (`ActionBar`, `RecyclerListView`, `EmptyTextProgressView`)
21. `app/src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt` (`ActionBar`, `RecyclerListView`, `EmptyTextProgressView`, `ButtonWithCounterView`)
22. `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt` (`ActionBar`, `HeaderCell`, `TextDetailSettingsCell`, `ShadowSectionCell`, `EmptyTextProgressView`, `ButtonWithCounterView`; purged `MaterialButton` & `DesignCardView`)

### Deleted Files (0)
- **Reason (Reference Proof)**: Legacy XML layouts for migrated screens (`activity_notifications.xml`, `activity_syllabus.xml`, `activity_exam_tests.xml`, `activity_result_detail.xml`, `activity_premium.xml`, `activity_payment_checkout.xml`, `activity_api_usage.xml`, `activity_manage_existing_exams.xml`, `activity_emoji_studio.xml`, `activity_practice.xml`, `activity_mistakes.xml`, `activity_pyq.xml`, `activity_admins_and_rights.xml`, `activity_feedback_messages.xml`, `activity_generated_tests.xml`, `activity_manage_polls.xml`, `activity_system_monitor.xml`, plus earlier `activity_about.xml`, `activity_settings.xml`, etc.) are still directly referenced in `app/src/test/` and `app/src/androidTest/` (`LockedLayoutInflationTest`, `StudentUiRenderingTest`, `UiBehaviorRegressionTest`, `Ios26DesignFidelityTest`, `AdminRefreshCompatibilityTest`, etc.). Because Gradle cannot run locally to verify test-suite retirement, deleting those XMLs without running CI tests would break compilation of `androidTest`/`test`.

---

## 5. Remaining Items & Genuine Blockers

1. **Gradle Compilation, Lint, Unit/UI Tests & APK Generation (Environment Blocker)**: Local VM lacks Android SDK (`ANDROID_HOME`), `javac`, and internet access. Push the incremental patch to `main` so GitHub Actions `FAST` / `FULL` builds the APK and runs tests.
2. **Remaining 21 `KOTLIN_TG_DS` Activities**: `BookmarksActivity`, `PollCreateActivity`, `ResultActivity`, `TestActivity`, `QrShareActivity`, `UsernameCardActivity`, `EveBaseActivity`, and 14 remaining Admin screens (`AdminActivity`, `ActivityLogActivity`, `AdminAnalyticsActivity`, `AdminRightsEditActivity`, `AppConfigActivity`, `EditAboutActivity`, `EditExamActivity`, `FlaggedQuestionsActivity`, `ManageExamsActivity`, `ManagePremiumActivity`, `ManageQuestionsActivity`, `ManageSyllabusActivity`, `ManageUsersActivity`, `SendNotificationActivity`) currently use EVE's Telegram design system (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TelegramChip`, `TelegramCheckBox2`, `TelegramRadioButton`) and have not yet been rewritten to direct `org.telegram.ui.*` Java views.
3. **Legacy XML Layout + Test Retirement**: Once CI verifies the current 17 migrated screens green, retire the legacy XML layout inflation tests and purge the unused `activity_*.xml` files in a dedicated CI-verified commit.

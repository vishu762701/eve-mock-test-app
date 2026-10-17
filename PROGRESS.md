# EVE — MASTER UI/UX + FUNCTIONAL POLISH TASK PROGRESS

## Task Checklist

- [x] **Task Group A — Result Overview: Single-Screen Dashboard**
  - [x] Transform Overview into compact single-screen summary/dashboard (no vertical page scroll in normal state)
  - [x] Summary blocks: (1) Result hero/score, (2) Statistics summary, (3) Performance summary, (4) Cutoff summary, (5) Compact analytics summary
  - [x] Tappable summary blocks leading to dedicated detail views (Statistics, Performance, Analytics)
  - [x] Create reusable `ResultDetailActivity` with detail-type modes and clean title/back behavior
  - [x] Keep tabs intact (Review, Overview, Leaderboard) with data-driven values (no hardcoded numbers)
  - [x] Light and dark theme parity

- [x] **Task Group B — Result Header Language / Reattempt**
  - [x] Remove `btnLanguage` (HI) from Result header
  - [x] Place compact Reattempt pill (`btnReattempt`) in Result header beside Share
  - [x] Remove large bottom Reattempt bar from Result screen
  - [x] Rebalance Share icon and spacing
  - [x] Preserve all Reattempt behavior (dialog, data clearing, navigation)
  - [x] Preserve Test screen HI language toggle completely

- [x] **Task Group C — Result Review: No Page-Level Scroll**
  - [x] Review fits in controlled viewport without page-level scroll
  - [x] Controlled internal scrolling for question content if long
  - [x] "View Solution" isolated scrolling/detail area
  - [x] Keep question palette, filters (Right/Wrong/Unattempted), empty filter state, report action

- [x] **Task Group D — Test Screen: Action Grid**
  - [x] 2x2 grid for bottom action buttons (Clear, Mark Review, Prev, Save & Next / Submit)
  - [x] Identical width, height, touch targets, centered text, equal spacing, equal corner radii
  - [x] Visual emphasis on primary Save & Next via style/color, not size
  - [x] Preserve all click handlers and navigation logic
  - [x] Usable at 360dp width and 1.3x font scale

- [x] **Task Group E & F — Test Screen: Question + Options Fitting & Premium Polish**
  - [x] Replace fixed 210dp / length > 75 heuristics with robust real measurement
  - [x] Exact viewport calculation (header, question, options, 2x2 grid, insets)
  - [x] Controlled internal question-area scroll fallback only if needed; options never hidden under bottom bar
  - [x] Maintain intentional stable gap between question and options
  - [x] Support multiline Hindi options, long questions, 360dp width, 1.3 font scale
  - [x] Premium polish: consistent vertical rhythm, balanced spacing, clean proportions

- [x] **Task Group G — Theme Switch / Status Bar Glitch**
  - [x] Centralize system bar and theme transition in shared infrastructure (`SystemBarHelper`, `ThemeSwitchAnimator`, `ThemeManager`)
  - [x] Eliminate flash, wrong background, wrong icon appearance, jump during Light <-> Dark transitions
  - [x] Preserve circular reveal animation with correct coordinates and origin

- [x] **Task Group H — Home Streak Disappear Bug**
  - [x] Persist structured streak data + timestamp in storage/cache
  - [x] Immediate restore on Activity creation/recreation (no visible GONE or blank slot during theme switch)
  - [x] Silent background refresh from API, fallback to cached state on network failure

- [x] **Task Group I — Home Fire Animation**
  - [x] Subtle continuous looping fire animation replacing static `ic_flame_line`
  - [x] Locally bundled, license-safe asset (vector animation / lightweight Lottie)
  - [x] Works in both light and dark themes
  - [x] Pauses when backgrounded, resumes when visible, respects reduced-motion

- [x] **Task Group J — Home Find-Your-Next-Test Panel**
  - [x] Reduce side margins from 20dp toward 16dp on 4dp grid
  - [x] Preserve light pastel wash (`HomePanelWashDrawable`) and dark surface
  - [x] Maintain rounded corners, streak pill, admin button, clean hierarchy

- [x] **Task Group K — Global Color Saturation Polish**
  - [x] Increase saturation ~25% where design uses color (lilac, yellow, pastel washes, secondary surfaces)
  - [x] Preserve neutrals (black/white/gray) and contrast (4.5:1 text, 3:1 UI)
  - [x] Green/red strictly reserved for right/wrong; no lime #E3FF3B
  - [x] Centralized in `colors.xml` and `values-night/colors.xml`

- [x] **Task Group L — Home Premium Finish**
  - [x] Proportions, spacing, alignment, card aesthetics
  - [x] Light/dark parity, preserve all existing functionality

- [x] **Task Group M — Design Token / Dark-Light Consistency**
  - [x] Full token adherence across all touched screens and new `ResultDetailActivity`

- [x] **Task Group N & O — Accessibility, Verification & Final Report**
  - [x] 360dp width & 1.3x font scale static checks & measurement verification
  - [x] Run `./gradlew testDebugUnitTest` (191/191 tests passed)
  - [x] Run `./gradlew assembleDebug` (APK generated cleanly: `app-debug.apk`)
  - [x] Document verified and unverified items in final report

## 3-Part Master Prompt Polish Pass

- [x] **Part 1 — Result Screen Polish**
  - [x] Result Overview: Natural vertical scrolling (`scrollResultContent` with `overScrollMode="ifContentScrolls"`, `clipToPadding="false"`, `paddingBottom="36dp"`, comfortable section margins)
  - [x] Result Tabs: Noticeably easier to read, 14sp Poppins Semibold typography (`TextAppearance.Eve.TabSegment`), crisp contrast (`eve_tab_unselected_text`: #4B5563 light / #D1D5DB dark), equal tab widths, 360dp width and 1.3x font scale safe
  - [x] Result Reattempt: Confident warm orange treatment (`Widget.Eve.Button.OrangePill`, `eve_reattempt_*` tokens), not neon; preserves dialog confirmation & attempt limits
  - [x] Result Review Viewport: Non-page-scrolling in normal state, reduced top/palette/filter margins, compact `item_answer.xml` (14dp padding, 44dp View Solution) eliminating large unused vertical gaps

- [x] **Part 2 — Test Screen Spacing, HI Removal & Top Theme-Switch Glitch**
  - [x] Test Screen Spacing: 10dp gap between question and options; 2x2 bottom actions systematically equal (44dp height, 6dp vertical row spacing)
  - [x] Language Toggle Removal: Completely removed `btnLanguage` (HI pill) and its listeners from all non-Settings screens (`activity_test.xml`, `activity_bookmarks.xml`, `activity_mistakes.xml`) while preserving global `LanguageManager` and Settings language toggle
  - [x] Top Status-Bar/Notch Theme Glitch: Updated `SystemBarHelper.syncSystemBars()` to match target theme appearance immediately during transitions, eliminating delayed old-theme strip while keeping circular reveal animation 100% intact

- [x] **Part 3 — Target Exams Sheet, Home Pills, Reattempt Consistency & Typography**
  - [x] Target Exams Bottom Sheet: Mature personalization component with clear heading ("Which exams are you preparing for?"), descriptive subtitle, Poppins Medium dynamic exam chips, and equal-sized Skip / Continue buttons
  - [x] Home Streak / Admin Pills: Equalized `layoutStreakPill` to `match_parent` width matching `btnAdmin` with centered alignment, keeping exact vertical thickness/height unchanged and flame animation unclipped
  - [x] Reattempt Orange Consistency: Applied `Widget.Eve.Button.Orange` to `btnReattempt` in `bottom_sheet_completed_exam.xml` and `Widget.Eve.Button.OrangePill` in `activity_result.xml`
  - [x] Global Typography & Color Contrast: Richer accents (~25% richer contrast/richness on lilac, yellow, orange), high-contrast neutrals, WCAG AA compliance, and 191/191 unit tests passing cleanly

## Full iOS Redesign and Admin-Managed Home Hero (2026-10-06)

- [x] Rebuilt Login presentation while retaining existing email sign-in/sign-up, Google sign-in, validation, session, loading/error, and analytics/reporting code paths.
- [x] Replaced the hardcoded Home hero with empty-by-default admin-managed content and supported CTA actions.
- [x] Added protected Worker API draft/read-write behavior, D1 migration, validation, and API tests.
- [x] Applied shared iOS-inspired visual tokens and controls across touched screens, including light and dark resources and reduced-motion-aware helpers.
- [x] `./gradlew testDebugUnitTest assembleDebug` passed (194 tests, no failures); backend `npm test` passed (68 tests); backend `npm run build` passed; all ten migrations applied in SQLite; `git diff --check` passed.
- [x] GitHub Actions deployed the Worker, applied D1 migrations, and passed live production smoke tests for the pushed changes ([run 37424680290](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37424680290)).
- [ ] Emulator/device visual and functional checks remain pending. `adb` cannot start in this environment due to a missing linker symbol, and no emulator executable is available. Build/test success is not recorded as UI verification.

## Home Banner Consolidation and Test Thin Material (2026-10-06)

- [x] Replaced the separate Home Hero and banner card with one admin-managed, rounded Home Banner surface; retained multi-banner swipe/rotation inside that surface and removed the old dots/card presentation.
- [x] Added optional banner URL/CTA label storage and validation through additive migration `0011_home_banner_links.sql`. Admin can preview/publish images, edit or clear links, reorder, and delete banners.
- [x] Student Home uses the uploaded banner image and shows a CTA only when its valid URL and label are present.
- [x] Applied Thin Material capsules only to Test answer options A–D and the four bottom Test actions. Selected answer accent is system green; the bottom action area has no extra visible background.
- [x] Removed the superseded Home Hero editor/runtime code and proven-unused Home wash/dot resources. Older migration history remains untouched.
- [x] `./gradlew testDebugUnitTest assembleDebug` passed with 195 tests and no failures; `git diff --check` passed.
- [x] Backend `npm test` passed (71 tests); `npm run build` passed; all 11 migrations applied in order to in-memory SQLite with legacy banner rows readable and link metadata defaulting empty.
- [x] GitHub [Build Eve APK](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37439163538) passed. GitHub [Worker deploy/D1 migration/live smoke tests](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37439163585) passed.
- [ ] Android device visual and interaction checks remain pending: `adb` cannot start because its binary is missing `_ZNSt6__ndk113__hash_memoryEPKvm`, no emulator executable is available, and no `androidTest` source set exists.
- `./gradlew lintDebug` was run and failed on two existing `NewApi` errors for `android:windowLightNavigationBar` in the unchanged theme files (minSdk 24; API requirement 27), with 1,560 warnings. These files were not changed as part of this task.

## Master Redesign Polish — Overflow, Drawer, Notifications & Fluid Navigation (2026-10-06)

- [x] **3-Dot Overflow Menu Material:** Resolved opaque white box in light mode by configuring `eve_menu_glass_bg` to subtle translucent light material (`#59FFFFFF`) and charcoal translucent material in dark mode (`#731C1C1E`).
- [x] **Profile Drawer Material:** Replaced solid opaque drawer backgrounds with transparent base in `bg_drawer_glass.xml` and translucent tint overlays (`eve_drawer_glass_tint` / `eve_drawer_glass_bg` at `#59FFFFFF` light / `#731C1C1E` dark), allowing underlying blurred content to be perceptible in both themes.
- [x] **Notifications Empty-State Bell Contrast:** Added high-contrast neutral light mapping (`0xE2E8F0`) for `notification_bell.json` (`0x020B19`) in dark mode via `EmptyStateAnimationHelper.kt` while preserving light mode and layout integrity.
- [x] **iOS 26 Contextual Fluid Navigation:** Replaced generic horizontal Activity slide animations with coordinated fluid scale-and-fade window transitions (`fluid_scale_enter`, `fluid_scale_fade_out`, `fluid_scale_fade_in`, `fluid_scale_exit`). Built `EveNavigationHelper` for source-aware scale-up expansion from source element bounds (Profile, Notifications, Syllabus, Admin, overflow items) and natural reverse transitions.
- [x] **Dead Code Cleanup:** Removed obsolete, unreferenced slide animation resources (`slide_in_left.xml`, `slide_in_right.xml`, `slide_out_left.xml`, `slide_out_right.xml`).
- [x] **Protected Feature Intact:** Verified Telegram-style Day/Light circular reveal theme toggle animation is 100% untouched in `ThemeSwitchAnimator.kt`, `ThemeManager.kt`, and `TelegramMenuPopup.kt`.
- [x] Build and tests verified: Gradle unit tests pass (196/196 tests), debug APK builds cleanly, backend tests pass (71/71).

## EVE UI Studio / App Builder Foundation (2026-10-06)

- [x] **Backend & D1 Migration:** Added `0012_ui_studio.sql` with tables for published config (`ui_studio_published`), drafts (`ui_studio_drafts`), historical snapshots (`ui_studio_versions`), and audit logging (`ui_studio_audit_log`).
- [x] **Backend Routes & Authorization:** Built `backend/src/routes/uiStudio.ts` with public student read `/api/ui-studio/published` (with ETag & 304 conditional support) and admin-guarded draft saving, validation, publishing, historical versions, rollback, reset, and audit log endpoints. Mounted in `index.ts` and allowlisted in `authMiddleware.ts`.
- [x] **Android Data Layer:** Added schema models in `UiStudioModels.kt`, Retrofit endpoints in `EveApiService.kt`, and `UiStudioRepository.kt` managing disk caching, background fetch, draft mutations, and resilient native fallbacks.
- [x] **Runtime Application Engine:** Built `UiStudioEngine.kt` to safely parse colors, dimensions (dp/sp), margins, paddings, corner radius, stroke, elevation, opacity, typography, and visibility without crashing or blanking views.
- [x] **Consumer Screen Wiring:** Connected `MainActivity` (Hero banner, Streak pill, Search panel), `TestActivity` (Timer pill, bottom action buttons), `QuestionAdapter` (Question card container, question text), and `ResultActivity` (Score hero card, analytics summary).
- [x] **Admin Studio Interface:** Created `UiStudioActivity` and `activity_ui_studio.xml` with screen selector, component selector, grouped property controls, live interactive preview canvas, draft saving, validation, publish dialog with change notes, version rollback, and JSON export/import. Integrated with `AdminActivity` and declared in `AndroidManifest.xml`.
- [x] **Verification & Test Coverage:**
  - Android unit tests: 203/203 passed (`UiStudioTest.kt` verifying color parsing, default template, schema validation, JSON export/import, and null safety).
  - Backend tests: 79/79 passed (`ui_studio.test.cjs` verifying validation, public empty fallback, authorization rejection, draft save/get, publishing, ETag 304, rollback, and reset).
  - Debug APK built and packaged cleanly via `./gradlew assembleDebug`.
  - `git diff --check` passed with 0 errors.

## EVE Admin and Shared UI Visual Consistency — Task 3 (2026-10-12)

- [x] **Styles & Input Hierarchy:** Added `Widget.Eve.TextInput.Outlined.ExposedDropdownMenu` in `styles.xml`. Replaced all legacy `Widget.MaterialComponents.TextInputLayout.OutlinedBox` instances with `Widget.Eve.TextInput.Outlined` across admin forms (`activity_edit_exam.xml`, `activity_manage_premium.xml`, `dialog_add_question.xml`, `dialog_edit_question_details.xml`, `dialog_manual_grant_premium.xml`, `dialog_create_feedback_post.xml`, `dialog_reply_feedback.xml`).
- [x] **Switches & Chips:** Replaced legacy `SwitchMaterial` with canonical `MaterialSwitch` in admin screens and dialogs (`activity_manage_exams.xml`, `dialog_create_poll.xml`, `item_generated_test.xml`). Standardized admin filter chips to `Widget.Eve.CategoryChip` (`activity_admin_analytics.xml`, `activity_activity_log.xml`).
- [x] **Semantic Tokens in Adapters & Activities:** Migrated status badge colors in `AdminActivity.kt`, `AnalyticsExamAdapter.kt`, `PollsAdminAdapter.kt`, and `UserAdminAdapter.kt` to semantic tokens (`R.color.eve_status_error_text`, `R.color.eve_status_warning_text`, `R.color.eve_status_success_text`). Standardized count and metric text colors in `activity_api_usage.xml` and item layouts.
- [x] **Destructive Actions & Buttons:** Standardized admin delete/remove/revoke actions with `@color/selector_eve_destructive_text` and stroke `@color/eve_shape_border` across admin lists, items, and dialogs (`item_manage_question.xml`, `item_admin_email.xml`, `item_admin_syllabus.xml`, `item_poll_admin.xml`, `item_admin_premium_user.xml`, `item_admin_banner.xml`, `item_feedback_message.xml`, `dialog_post_replies.xml`, `activity_send_notification.xml`, `popup_telegram_menu.xml`).
- [x] **Surfaces & Cards:** Normalized admin surfaces and cards to `eve_card_bg` and `eve_shape_border`, removing non-standard radii overrides and hardcoded colors.
- [x] **Shared Components:** Standardized `layout_bulletin.xml` and `layout_undo_bar.xml` card corners to `@dimen/eve_radius_card` (16dp). Standardized `popup_telegram_menu.xml` popup corner radius to `@dimen/eve_radius_menu` (16dp squircle) and logout action to destructive tint.
- [x] **Protected Constraints Intact:** Maintained business logic, security, authorization, deletion workflows, data density, student-facing UI, and circular reveal theme switching.
- [x] **Verification & Test Coverage:**
  - Design system contrast checks: 196 composited semantic pairs passed (min 4.51:1).
  - Unit tests: `./gradlew testDebugUnitTest` passed (209/209 tests).
  - Debug APK built cleanly: `./gradlew assembleDebug` passed.
  - Android Lint analysis: `./gradlew lintDebug` passed with 0 errors.
  - Documentation: `node .github/scripts/verify-docs.mjs` passed (29 documents, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## EVE Full-App iOS 26 Visual & Functional Release Gate — Task 4 (2026-10-12)

- [x] **Full Coverage Source Recount:** Inventoried 95 XML layouts (22 student, 17 admin, 14 dialogs/sheets, 37 item layouts, 5 shared components), 40 registered Activities in `AndroidManifest.xml`, 30 adapters, 17 custom views, 127 drawables, and 26 color selectors.
- [x] **Visual Consistency Repairs:** Standardized `activity_manage_syllabus.xml` card background to `@color/eve_card_bg` and title to `@color/eve_text`.
- [x] **Token & Contrast Integrity:** Verified all 196 composited semantic color pairs meet WCAG AA contrast (min 4.51:1 text, >= 3.0:1 UI components).
- [x] **Functional Invariants Protected:** Verified rigid Mock Test viewport and 2x2 action grid, server-side scoring and deduplication, admin authorization boundaries, and Telegram-style circular reveal Day/Night theme switcher (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`).
- [x] **Verification Gates Passed:**
  - `python3 -B .github/scripts/check_design_system.py` passed (196 pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (11/11 tests).
  - `node .github/scripts/verify-docs.mjs` passed (28 docs, 0 errors, 0 warnings).
  - `./gradlew testDebugUnitTest` passed (209/209 tests).
  - `./gradlew assembleDebug` passed (Debug APK compiled).
  - `./gradlew lintDebug` passed (0 lint errors).
  - `git diff --check` passed with 0 errors.

## EVE Full iOS 26 Color Migration (2026-10-12)

- [x] **Complete Elimination of Legacy Color Tokens:** Migrated all 58 layout locations referencing `?attr/colorOnSurface` to canonical `@color/eve_text` or `?attr/eveLabelPrimary`.
- [x] **WCAG AA Contrast Standardization:** Replaced low-contrast `@color/eve_grey` (#8E8E93, failing 4.5:1 on light backgrounds) in `HistoryAdapter.kt`, `AnswerAdapter.kt`, `item_feedback_message.xml`, and `activity_manage_syllabus.xml` with adaptive high-contrast `@color/eve_text_secondary` (4.8:1+ contrast).
- [x] **Divider & Control Standardization:** Replaced invisible `@color/eve_bg` dividers in `activity_about.xml` with hairline `@color/eve_separator`. Upgraded legacy `SwitchMaterial` instances to canonical `MaterialSwitch` with iOS 26 track/thumb selectors (`selector_eve_switch_thumb`, `selector_eve_switch_track`).
- [x] **Authoritative Apple iOS 26 Palette (Light / Dark):**
  - Red: `#FF383C` / `#FF4245`
  - Orange: `#FF8D28` / `#FF9230`
  - Yellow: `#FFCC00` / `#FFD600`
  - Green: `#34C759` / `#30D158`
  - Mint: `#00C8B3` / `#00DAC3`
  - Teal: `#00C3D0` / `#00D2E0`
  - Cyan: `#00C0E8` / `#3CD3FE`
  - Blue: `#0088FF` / `#0091FF`
  - Indigo: `#6155F5` / `#6D7CFF`
  - Purple: `#CB30E0` / `#DB34F2`
  - Pink: `#FF2D55` / `#FF375F`
  - Brown: `#AC7F5E` / `#B78A66`
- [x] **Apple System Grays 1–6 (Light / Dark):** Gray `#8E8E93` / `#8E8E93`, Gray 2 `#AEAEB2` / `#636366`, Gray 3 `#C7C7CC` / `#48484A`, Gray 4 `#D1D1D6` / `#3A3A3C`, Gray 5 `#E5E5EA` / `#2C2C2E`, Gray 6 `#F2F2F7` / `#1C1C1E`.
- [x] **Apple Increased Contrast Variants:** Full set of official Apple High Contrast tokens implemented in `values/colors.xml` and `values-night/colors.xml`.
- [x] **Semantic System Tokens:** Android-native appearance-aware equivalents implemented for `systemFill`, `secondarySystemFill`, `tertiarySystemFill`, `quaternarySystemFill`, `link`, `darkText`, `lightText`, and `clear`.
- [x] **Protected Behavior Intact:** Preserved Telegram circular theme reveal, Lottie animations, SVG assets, Mock Test 2x2 grid, test scoring, and admin authorization.
- [x] **SystemUI Navigation Mode Night Synchronization:** Resolved API 35 SystemUI indicator tone synchronization in `RepairRegressionTest.kt` to cover both gesture and three-button navigation modes, ensuring gesture indicators contrast correctly across theme switches.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (196 composited pairs, min text 4.51:1).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (28 docs, 0 errors, 0 warnings).
  - `./gradlew testDebugUnitTest` passed (209/209 tests).
  - `./gradlew compileDebugAndroidTestKotlin` passed.
  - `./gradlew assembleDebug` passed (Debug APK built).
  - `./gradlew lintDebug` passed (0 lint errors).
  - `git diff --check` passed with 0 errors.

## EVE Navigation-Chrome Correction & iOS 26 Segmented Control Refinement (2026-10-16)

- [x] **Transparent Navigation Headers:** Refactored `EveGlassToolbar` to a transparent `FrameLayout` (`Color.TRANSPARENT`), removing continuous rounded capsule clipping (`EveShapes.capsule`), background blur/tint (`setupBlurWith`), and hairline bottom separator across all 36 toolbar screens (student and admin).
- [x] **Separated Tab Surfaces:** Decoupled `tabLayoutResult` in `activity_result.xml` from the outer toolbar wrapper by replacing the outer wrapper with a standard `FrameLayout`, and giving the tabs their own dedicated bounded surface track via `bg_result_segmented_track.xml` (`eve_result_tab_track`).
- [x] **Neutral Segmented Control Selection:** Standardized segmented control selection surfaces to Apple HIG neutral tokens (`eve_system_background` / `#FFFFFF` in Light, `eve_system_gray2` / `#636366` in Dark) with primary-label text (`eve_label_primary`), eliminating hard 1dp stroke outlines.
- [x] **Protected Invariants:** Preserved bare Lottie notification bell (`btnNotification`) and parent container without any decorative background/ring, preserved student avatar, Home category chips horizontally scrollable with 40dp visual height / 48dp target, and Telegram circular reveal day/night transition.
- [x] **Motion & Accessibility:** Added rapid-tap animation cancellation and reduced-motion compliance (`EveMotionHelper.areAnimationsEnabled`) in `EveGlassTabLayout`.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed.
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - Unit test `NavigationChromeAndTabsTest.kt` added.
  - `git diff --check` passed with 0 errors.

## EVE Apple-Inspired Liquid Glass Regular & Mock Test Submission Confirmation Dialog (2026-10-17)

- [x] **Unified Specification (`EVE_REGULAR_GLASS`):** Codified unified Liquid Glass `.regular` specification with coordinated Light appearance (`#E6FFFFFF`, 90% white alpha with 1dp `#1A000000` rim) and Dark appearance (`#E61C1C1E`, 90% `#1C1C1E` alpha with 1dp `#26FFFFFF` rim) under ADR-012.
- [x] **Mock Test Submission Confirmation Dialog:** Replaced the visual material treatment of `confirmSubmit()` in `TestActivity.kt` using dedicated `Theme.Eve.Dialog.SubmitConfirmation` and `@drawable/bg_dialog_submit_confirmation`.
- [x] **Hardware Blur & Runtime Fallback:** Configured SurfaceFlinger window background blur on API 31+ (`16dp` radius), dynamically monitored via `WindowManager.addCrossWindowBlurEnabledListener` with automatic fallback to opaque `@color/eve_secondary_system_background` if transparency or battery saver is active, with complete listener unregistration on dismiss.
- [x] **Content & Invariants Preserved:** Preserved exact title (`"Submit test?"`), question count calculations (`calculateSubmitDialogCounts`), positive button (`"Submit"` calling `submit()`), negative button (`"Cancel"`), and all other dialogs and content cards in the app.
- [x] **Automated Verification:** Expanded `LiquidGlassRegularMaterialTest.kt` validating tokens, themes, drawables, contrast math (>= 10:1), and confirmation dialog source contracts.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 color pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `git diff --check` passed with 0 whitespace errors.

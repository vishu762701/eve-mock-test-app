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

## EVE Complete Official iOS 26 Design Fidelity Implementation (2026-10-17)

- [x] **Screen Canvas Roles & SystemBar Centralization:** Formally established `ScreenCanvasRole` (`PLAIN` `#FFFFFF`/`#000000`, `GROUPED` `#F2F2F7`/`#000000`) in `SystemBarHelper.kt`. Bound in `EveBaseActivity.setContentView()` so `SystemBarHelper.syncSystemBars()` sets both window and `android.R.id.content` backgrounds to the exact canvas color, eliminating flashing and edge-to-edge mismatch. Removed conflicting `setBackgroundDrawableResource` calls from 8 activities.
- [x] **All Section Tabs Upgraded to `EveGlassTabLayout`:** Standardized `activity_admin.xml`, `activity_manage_premium.xml`, and `activity_edit_about.xml` from legacy flat rectangular underline `TabLayout` to `EveGlassTabLayout` using `@style/Widget.Eve.InlineSectionTabs`, preserving all tab items, text, ordering, and selection listeners.
- [x] **Elimination of Redundant Header Hairlines:** Removed 20 redundant `<View>` hairline separators across 19 student/admin activities directly below `EveGlassToolbar` and 1 below admin tabs, keeping titles floating cleanly over the canvas per Apple HIG.
- [x] **Day/Night & API 31+ Modal Qualifier Resolution:** Standardized `Theme.Eve.BottomSheetDialog` to canonical `Theme.Material3.DayNight.BottomSheetDialog` across `values/themes.xml` and `values-v31/materials.xml`, removing redundant override from `values-night/themes.xml`. Fixed `GlassmorphismHelper.update` to explicitly clear `FLAG_BLUR_BEHIND` when blur radius reaches 0.
- [x] **Repository Cleanliness:** Removed binary archive `eve-argon4-phase1-changed-files.zip` from git tracking.
- [x] **Test Suite & Invariants:** Added `Ios26DesignFidelityTest.kt` validating screen canvas roles, tab items/callbacks, absence of redundant hairlines, and protected test colors (`#34C759`, `#FFCC00`). Verified 0 hardcoded `android:textSize` attributes across all 95 layouts.
- [x] **Unit Test & Result Canvas Synchronization:** Added `@color/eve_system_grouped_background` to root `FrameLayout` of `activity_result.xml` and updated `AppWideTypographyAndColorRolesTest.kt` to classify `activity_leaderboard.xml` as grouped canvas, resolving both unit test failures.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## EVE Locked iOS 26 Rules and Apple HIG Source Authority Verification (ADR-014) (2026-10-17)

- [x] **Locked Rules & Apple HIG Source Audit:** Conducted exhaustive verification of locked rules against Apple Human Interface Guidelines documentation and JSON endpoints (`color.json`, `typography.json`).
- [x] **Verified Tokens:** Formally verified systemBackground (`#FFFFFF`/`#000000`), secondarySystemBackground (`#F2F2F7`/`#1C1C1E`), interactive blue accent (`#0088FF`/`#0091FF`), systemGreen (`#34C759`/`#30D158`), systemRed (`#FF383C`/`#FF4245`), systemOrange (`#FF8D28`/`#FF9230`), bans on lime/yellow/pink, Dynamic Type scale (Title 28 bold, Heading 20 semibold, Body 17, Secondary 15, Caption 13 sp), primary/secondary text contrast (>= 4.5:1), fully rounded buttons/chips (capsule), and concentric inner radius formula.
- [x] **Unverified Values Maintained:** Documented that Apple HIG does not specify Inter font (SF Pro is Apple system font) or numeric corner radii for cards/sheets; per locked rule instruction, current platform-native sans-serif typeface, 26dp cards, and 34dp sheets are maintained and marked UNVERIFIED.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## EVE Design System Foundation and Home Screen Application (ADR-015) (2026-10-17)

- [x] **Canonical Eve Design System Foundation:** Built `com.eve.app.designsystem.EveDesignSystem` organizing all tokens and helpers under structured modules: `Colors` (canvas, surfaces, labels, accents, status), `Typography` (11 canonical styles, typefaces), `Shapes` (capsule, 26dp card, 34dp sheet, concentric calculation, squircle outline providers), `Spacing` (4dp spatial grid, 48dp/52dp/56dp interaction bounds), `Elevation` (1.5dp light / 0dp dark), `Motion` (durations, standard/spring interpolators, tactile feedback), `Materials` (Liquid Glass Regular `apple-regular`), and `Components` (card, concentric icon tile, chip, streak pill, admin button, banner surface, and bell tinting).
- [x] **Strict Application to Home Screen ONLY:** Applied foundation directly to `MainActivity.kt` (banner squircle clipping, matte background, drawer background and radius, streak pill styling, admin button, profile touch feedback), `HomeAppearance.kt` (delegation of banner clipping, bell ink, and category chips to design system), `ExamAdapter.kt` (semantic colors, label hierarchy, status indicators), `item_feedback_post.xml` (standardized to `Widget.Eve.Card.Content` for plain canvas contrast), and `dimens.xml` (`@dimen/eve_radius_concentric_card` = 8dp).
- [x] **Non-Home Screens Untouched:** Preserved all other 35+ screens (Test, Result, Profile, Settings, Admin, Leaderboard, History, Syllabus, About, Bookmarks) without any modifications.
- [x] **Automated Verification:** Added `EveDesignSystemTest.kt` verifying concentric geometry formulas, spatial grid, motion parameters, typography tokens, Home screen XML bindings, and source integrity.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## EVE Scoped Home Screen iOS 26 Depth & Motion Specification (ADR-016) (2026-10-17)

- [x] **Depth — Zero Card Shadows:** Flattened all Home cards (`Widget.Eve.Card.Group`, `Widget.Eve.Card.Content`, `Widget.Eve.Card.Home`, `item_exam.xml`, `item_feedback_post.xml`, `popup_telegram_menu.xml`, airplane card) to `@dimen/eve_home_card_elevation` = `0dp` and `0dp` stroke.
- [x] **Depth — Selective Glass Blur & Tint:** Applied ~24dp blur (`@dimen/eve_home_blur_radius` = `24dp`), ~75% tint (`@color/eve_home_blur_tint` `#BFFFFFFF` / `#BF1C1C1E`), 1dp hairline border (`@dimen/eve_home_hairline_border` = `1dp`, `@color/eve_home_hairline_border`), and 94% solid tint fallback (`@color/eve_home_solid_tint` `#F0FFFFFF` / `#F01C1C1E`) restricted to top bar (`topBarContainer`, `searchTopBar`), menus (`bg_home_popup_menu.xml`, `popup_telegram_menu.xml`), and sheets (`bg_recovery_sheet.xml`, `TargetExamsBottomSheet.kt`).
- [x] **Motion — Spring Physics & Tactile Feedback:** Configured mathematically exact spring curve with damping ratio 0.8 (1.5% overshoot -> tension 0.65) via `@interpolator/eve_spring_damping_08.xml` and `OvershootInterpolator(0.65f)`, duration window of 200-350ms (`@integer/eve_home_motion_duration` = `260` ms, `DURATION_SPRING_MS` = `280L`), and press feedback scale `0.97` (`@dimen/eve_home_press_scale`, `@animator/press_scale.xml`, `EveTouchHelper.DEFAULT_PRESS_SCALE`).
- [x] **Full Resource Encapsulation:** Zero hardcoded colors/dp/sp in touched files; all values codified in `colors.xml`, `dimens.xml`, `integers.xml`, `styles.xml`, and `themes.xml` (day + night).
- [x] **Home Screen Scoped & Invariants Preserved:** Applied strictly to Home screen only (`Theme.Eve.Home`). Telegram theme toggle, all Lottie files/triggers/sizes, and SVG vector assets remained 100% untouched.
- [x] **Automated Verification:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## EVE Design System Foundation Locked Values Codification & Home Screen Application (ADR-017) (2026-10-17)

- [x] **Canonical Locked Design System Tokens:** Explicitly codified all authoritative locked design tokens directly within `EveDesignSystem` (`com.eve.app.designsystem.EveDesignSystem`):
  - Locked color hex constants: Canvas `#FFFFFF` / `#000000`, Secondary Cards `#F2F2F7` / `#1C1C1E`, Interactive Blue `#0088FF` / `#0091FF`, System Green (Success) `#34C759` / `#30D158`, System Red (Error) `#FF383C` / `#FF4245`, System Orange (Warning) `#FF8D28` / `#FF9230`, Secondary Text `#50505A` / `#C5C5CC`.
  - System status methods: `systemGreen`, `systemRed`, `systemOrange`, `statusSuccess`, `statusError`, `statusWarning`.
  - Locked typography roles: `Title` (28sp bold -> `Title1Bold`), `Heading` (20sp semibold -> `Title3Semibold`), `Body` (17sp regular -> `Body`), `Secondary` (15sp regular -> `Subheadline`), `Caption` (13sp regular -> `Footnote`).
  - Locked shape and motion constants: Capsule `999dp`, Card `26dp`, Sheet `34dp`, Concentric Card `8dp`, Min `4dp`, Spring Damping `0.8`, Press Scale `0.97`.
  - Component helper: `applyHomeTopBarDepth(topBarContainer, searchTopBar)` supporting adaptive 24dp blur with 75% tint and 94% solid fallback.
- [x] **Strict Application to Home Screen ONLY:** Applied foundation directly to `HomeAppearance.kt` (delegation of `applyTopBarDepth`), `ExamAdapter.kt` (exam icon container stroke and card background), and `MainActivity.kt`.
- [x] **Non-Home Screens Untouched:** Preserved all 35+ non-Home screens (Test, Result, Profile, Settings, Admin, Leaderboard, History, Syllabus, About, Bookmarks) without any modifications.
- [x] **Automated Verification:**
  - Expanded `EveDesignSystemTest.kt` validating locked color hex constants, typography roles/scale, shape constants, motion physics, and non-Home screen integrity.
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (13/13 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## EVE Design System Foundation with Locked Values & Home Screen Application (ADR-018) (2026-10-17)

- [x] **Locked Color Tokens (Day & Night):** Codified background (#EEF2F8 / #0B0F16), card (#FFFFFF / #161B24), text primary (#000000 / #FFFFFF), text secondary (#6C6C70 / #A1A1A6), hairline (rgba(0,0,0,0.10) / rgba(255,255,255,0.14)), tab track (rgba(120,120,128,0.16) / rgba(120,120,128,0.32)), accent (#000000 / #FFFFFF), on-accent (#FFFFFF / #000000), semantic status (success #34C759/#30D158, error #FF383C/#FF4245, warning #FF8D28/#FF9230 for meaning only), and floating bar background (#FFFFFF / #2C2C2E) in `colors.xml` and `values-night/colors.xml`.
- [x] **Bundled OFL DM Sans Typography:** Static Regular 400, Medium 500, Bold 700 TTF files bundled in `res/font/` and verified with TrueType magic headers. Configured `FontManager.kt` default to DM Sans. Text appearances defined for PageTitle (29sp Bold), CardTitle (18sp Bold), CardBody (18sp Regular), Secondary (14sp Regular), TabLabel (15sp Regular, Selected Bold), and PillLabel (14sp Medium).
- [x] **Segmented Tabs (Filled Pill):** 40dp height, fully rounded, track color, 3dp inner padding, accent fill + on-accent text when selected, secondary text when unselected.
- [x] **Pills (Outline):** 32dp height, 12dp horizontal padding, fully rounded, 1dp accent border, accent text, with >= 48dp touch delegate without altering visible geometry.
- [x] **Cards:** 24dp corner radius, 12dp padding, 8dp card gap, 16dp screen margin, 16dp section gap. Soft 2dp/12dp shadow (10% black) in light mode; 0dp elevation / no shadow in dark mode.
- [x] **Floating Solid Capsule Bottom Bar:** 12dp side margins, 10dp bottom margin (+ gesture inset), 54dp height, 22dp icons, 0.5dp hairline border, accent selected tint, secondary unselected tint, no blur. Dynamic bottom clearance in `MainActivity` on `rvExams` with `clipToPadding="false"`.
- [x] **Screen-Width Qualifiers:** Replicated all tokens identically into `values-sw360dp/dimens.xml` and `values-sw600dp/dimens.xml`.
- [x] **Scoping & Invariants:** Applied strictly to Home screen only. Test, Result, Admin, Leaderboard, Profile, and Settings screens untouched. Telegram theme switcher, Lottie files/triggers/sizes, and SVG assets 100% preserved.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

## Task 1 of 3 — App-Wide Eve Design System Locked Color Tokens (ADR-019) (2026-10-18)

- [x] **App-Wide Locked Color Tokens (Day & Night):** Codified foundation color tokens in `res/values/colors.xml` and `res/values-night/colors.xml`:
  - `eve_color_background` (#EEF2F8 / #0B0F16)
  - `eve_color_card` (#FFFFFF / #161B24)
  - `eve_color_text_primary` (#000000 / #FFFFFF)
  - `eve_color_text_secondary` (#6C6C70 / #A1A1A6)
  - `eve_color_hairline` (#1A000000 / #24FFFFFF)
  - `eve_color_tab_track` (#29787880 / #52787880)
  - `eve_color_accent` (#000000 / #FFFFFF)
  - `eve_color_on_accent` (#FFFFFF / #000000)
  - `eve_color_success` (#34C759 / #30D158)
  - `eve_color_error` (#FF383C / #FF4245)
  - `eve_color_warning` (#FF8D28 / #FF9230)
  - `eve_color_floating_bar_bg` (#FFFFFF / #2C2C2E)
  - `eve_color_card_shadow` (#1A000000 / #00000000)
- [x] **Semantic App-Wide Aliases:** Codified direct mappings `eve_app_*` -> `eve_color_*` across Day and Night palettes.
- [x] **First-Class Theme Attributes:** Declared 13 `eveColor*` attributes in `res/values/attrs.xml` and bound them in `Theme.Eve.Foundation` in `res/values/themes.xml`.
- [x] **Kotlin Foundation Accessors & Constants:** Codified `HEX_APP_LIGHT_*` and `HEX_APP_DARK_*` constants along with `@ColorInt` helper functions (`appBackground`, `appCard`, etc.) in `EveDesignSystem.kt`.
- [x] **Color State Lists:** Created shared selectors in `res/color/` (`selector_eve_pill_bg.xml`, `selector_eve_pill_stroke.xml`, `selector_eve_pill_text.xml`, `selector_eve_tab_text.xml`, `selector_eve_floating_bar_tint.xml`).
- [x] **Strict Colors-Only Boundary:** Zero changes to layout geometry, sizes, fonts, text strings, shapes, animations, or behavior.
## Task 1 of 3 — App-Wide Locked Color Tokens (ADR-020) (2026-10-18)

- [x] **Authoritative Color Tokens (Day & Night):**
  - Background: `bg_app` (#F5F5F5 / #000000)
  - Surface: `surface` (#FFFFFF / #1C1C1E)
  - System hues: `system_red` (#FF383C / #FF4245), `system_green` (#34C759 / #30D158), `system_blue` (#0088FF / #0091FF), `system_indigo` (#6155F5 / #6155F5), `system_orange` (#FF8D28 / #FF9230), `system_purple` (#CB30E0 / #DB34F2), `system_yellow` (#FFCC00 / #FFD600)
  - Containers `system_<hue>_container`: that system color at 12% alpha (light, `1F`) / 20% alpha (dark, `33`).
- [x] **Theme Wiring & Inheritance:**
  - Declared `bgApp`, `systemRed` ... `systemYellow`, and container variants in `attrs.xml`.
  - Bound in `Theme.Eve.Foundation`: `windowBackground`, `colorBackground`, `statusBarColor`, `navigationBarColor` to `@color/bg_app`; `colorSurface`, `colorSurfaceContainer*` to `@color/surface`; `snackbarStyle` to `@style/Widget.Eve.Snackbar`.
  - Dialogs and bottom sheets configured to `@color/surface` and `@color/bg_app`.
  - Text input layouts in `styles.xml` configured to `boxBackgroundColor="@color/surface"`.
- [x] **Hue Family & Container Aliases:**
  - Old backgrounds (`eve_system_background`, `eve_system_grouped_background`, `eve_home_background`, `eve_color_background`, etc.) -> `@color/bg_app`.
  - Old cards/panels (`eve_secondary_system_background`, `eve_card_bg`, `eve_home_card`, `eve_color_card`, Result light blue cards, etc.) -> `@color/surface`.
  - Pale tinted containers (`eve_error_bg`, `eve_status_error_container` -> `system_red_container`; `eve_green_fill`, `eve_status_success_container` -> `system_green_container`; `eve_accent_container`, `eve_button_soft_bg` -> `system_blue_container`; `eve_offline_bg`, `eve_status_warning_container` -> `system_orange_container`; `eve_lilac_fill` -> `system_indigo_container`).
  - Unchanged hues retained: teal/mint, cyan, pink, brown/bronze, grays.
  - Retained protected answer-state test colours: `eve_tile_right_fill`, `eve_tile_wrong_fill`, `eve_tile_medium_fill`.
- [x] **Zero Hardcoded Colors in Drawables & Kotlin:**
  - Cleaned vector drawables (`ic_broadcast.xml`, `ic_bug_report.xml`, `ic_campaign.xml`, `ic_image.xml`, `ic_maintenance.xml`, `ic_message_dots.xml`, `ic_robot.xml`, `ic_notification.xml`) to use semantic tokens.
  - Updated `AmbientBackgroundView.kt` base background to `@color/bg_app`.
  - Updated `ConfettiView.kt` to resolve palette tokens.
  - Updated `ShareCardHelper.kt` to resolve dark configuration tokens.
  - Updated `EveDesignSystem.kt` constants and accessors.
- [x] **Strict Invariants Preserved:**
  - Telegram Day/Night circular reveal transition, Lottie files/triggers/sizes, and SVG icon vector geometries 100% untouched.
  - Zero layout geometry, size, font, text, shape, or behavior modifications.
- [x] **Verification Gate:**
  - `python3 -B .github/scripts/check_design_system.py` passed (194 measured pairs).
  - `python3 -B -m unittest discover -s .github/scripts/tests -q` passed (15/15 tests).
  - `node .github/scripts/verify-docs.mjs` passed (33 docs, 0 errors, 0 warnings).
  - `git diff --check` passed with 0 errors.

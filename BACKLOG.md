# EVE Product & Engineering Backlog

Ranked by impact on user experience, stability, and design system fidelity per `ENGINEERING_STANDARD.md`.

## [COMPLETED] Phase 3: Official Apple iOS 26 Colors + Typography (Profile, Settings & Login)
- **Status**: Completed (2026-10-13)
- **Scope**: `activity_profile.xml`, `ProfileActivity.kt`, `activity_settings.xml`, `SettingsActivity.kt`, `bottom_sheet_font_picker.xml`, `activity_login.xml`, `LoginActivity.kt`.
- **Details**: Full migration of Profile, Settings, and Login screens to canonical Apple iOS 26 semantic color roles, typography hierarchy, adaptive switch green, zero hardcoded text sizes, and comprehensive automated JVM verification.

## [COMPLETED] Phase 4: Official Apple iOS 26 Typography Migration for Mock Test Screen
- **Status**: Completed (2026-10-13)
- **Scope**: `activity_test.xml`, `item_question.xml`, `item_palette_circle.xml`, `CircularTimerView.kt`, `type.xml`.
- **Details**: Standardized question text to Callout (16sp, lineSpacingMultiplier 1.5), option labels (A–D) to Subheadline (14sp), per-question timer chip to Footnote.Medium (13sp), palette circle numbers to Caption1.Medium (12sp), screen title to Headline (17sp), loading/message states to semantic roles, 0 hardcoded text sizes, and preserved all rigid 2x2 geometry, server scoring, and answer state colors.

## [COMPLETED] Phase 5: Official Apple iOS 26 Typography Migration for Result & Secondary Screens
- **Status**: Completed (2026-10-13)
- **Scope**: `activity_result.xml`, `activity_result_detail.xml`, `item_answer.xml`, `item_result_topic.xml`, `item_topic_stat.xml`, `ResultDetailActivity.kt`, `ResultAnalysisRows.kt`, `activity_leaderboard.xml`, `item_leaderboard.xml`, `activity_history.xml`, `item_history.xml`, `activity_syllabus.xml`, `item_syllabus.xml`, `activity_feedback.xml`, `item_feedback_message.xml`, `item_post_reply.xml`, `dialog_create_feedback_post.xml`, `dialog_reply_feedback.xml`, `dialog_report_question.xml`, `activity_about.xml`, `activity_content_display.xml`.
- **Details**: Full migration of Result (Overview, Statistics, Performance, Analytics, Review), Leaderboard, History, Syllabus, Feedback, and About screens to canonical Apple iOS 26 typography hierarchy and semantic color roles. 100% elimination of hardcoded text sizes across 19 layouts. Leaderboard visual geometry strictly preserved with zero redesign. Comprehensive automated JVM test suite (`ResultAndSecondaryScreensTypographyTest.kt`).

## [COMPLETED] App-wide Apple iOS 26 Dynamic Typography & 26 Colour Roles (All 95 Layouts)
- **Status**: Completed (2026-10-13)
- **Scope**: All 95 Android layouts in `app/src/main/res/layout/`, `attrs.xml`, `themes.xml`, `type.xml`, ADR-008 (`DECISIONS.md`), `AppWideTypographyAndColorRolesTest.kt`.
- **Details**: Full migration of all remaining layouts (student secondary, monetization, core admin dashboard, admin management, admin forms, admin item rows, and admin dialogs). Codified all 26 authoritative Apple iOS colour roles in `attrs.xml` and `themes.xml`. Achieved 100% elimination of hardcoded `android:textSize` attributes project-wide (0 remaining across all 95 layout files). Automated verification suite added in `AppWideTypographyAndColorRolesTest.kt`. Preserved pure `#FFFFFF` / `#000000` canvas, Telegram transitions, test engine, and asset invariants.

## [COMPLETED] EVE JARVIS Gemini Gem Google Drive Synchronization Pipeline (ADR-009 & ADR-010)
- **Status**: Completed (2026-10-14)
- **Scope**: `.github/workflows/sync-jarvis-gemini.yml`, `.github/scripts/gdrive-sync/`, `docs/EVE_JARVIS_GEMINI_SYNC_SETUP.md`, ADR-009, ADR-010 (`DECISIONS.md`).
- **Details**: Built the complete, secure, autonomous GitHub-to-Google-Drive synchronization pipeline for EVE JARVIS Gemini Gem. Features Dual-Mode Authentication (Option A: Google User OAuth 2.0 with personal quota; Option B: Service Account for Workspace Shared Drives), 7-volume knowledge consolidation under the Gemini Gem 10-file cap, line-numbered source code with exact commit SHA headers, stable Google Drive document ID preservation via in-place `files.update`, atomic publication with partial failure rollback protection (master index published only when all 6 volumes succeed), exponential backoff with randomized jitter on 429/5xx, transparent 401 token refresh, universal Shared Drive support (`supportsAllDrives=true`), SHA consistency with Android APK builds, automated secret sanitization across adversarial strings, and a zero-dependency OAuth setup helper (`oauth-helper.mjs`). Verified via 25 automated unit tests (`node:test`).

## [COMPLETED] Navigation Chrome Correction & iOS 26 Neutral Segmented Navigation (ADR-011)
- **Status**: Completed (2026-10-16)
- **Scope**: `EveGlassToolbar.kt`, `activity_result.xml`, `EveGlassTabLayout.kt`, `bg_result_segmented_track.xml`, `bg_tab_segmented_indicator.xml`, `bg_tab_segmented_track.xml`, `approved_ui_colors.xml`, `colors.xml`, `EVE_IOS26_UI_CONTRACT.md`, `ApprovedUiRenderingTest.kt`, `NavigationChromeAndTabsTest.kt`, ADR-011 (`DECISIONS.md`).
- **Details**: Eliminated full-width rounded capsule slabs, blur overlays, and bottom separator borders behind headers across all 36 screens by converting `EveGlassToolbar` to a transparent `FrameLayout`. Separated header containers from tab surfaces in `activity_result.xml` by replacing the outer toolbar wrapper with a neutral `FrameLayout` and eliminating the duplicate nested track. Implemented official Apple iOS 26 neutral segmented control styling (light mode: `#FFFFFF` selected surface with `#000000` primary text; dark mode: `#636366` raised dark surface with `#FFFFFF` primary text). Added rapid-tap animation cancellation and disabled-animation checks in `EveGlassTabLayout`. Preserved bare Home Lottie notification bell, avatar, and 40dp/48dp category pills. Verified via automated unit tests and design-system validation.

## [COMPLETED] Apple-Inspired Liquid Glass Regular & Mock Test Submission Confirmation Dialog (ADR-012)
- **Status**: Completed (2026-10-17)
- **Scope**: `TestActivity.kt`, `EveLiquidGlass.kt`, `Theme.Eve.Dialog.SubmitConfirmation`, `bg_dialog_submit_confirmation.xml`, `values-v31/materials.xml`, `values-night-v31/materials.xml`, `EVE_IOS26_UI_CONTRACT.md`, `DECISIONS.md`, `LiquidGlassRegularMaterialTest.kt`.
- **Details**: Replaced visual material treatment of the "Submit test?" confirmation dialog in `TestActivity.kt` with unified Apple-inspired Liquid Glass `.regular` (`EVE_REGULAR_GLASS`). Standardized Light appearance (`#E6FFFFFF` 90% white alpha with 1dp `#1A000000` rim) and Dark appearance (`#E61C1C1E` 90% alpha with 1dp `#26FFFFFF` rim) on API 31+ via SurfaceFlinger window background blur (`16dp` radius). Implemented runtime adaptation via `WindowManager.addCrossWindowBlurEnabledListener` with automatic fallback to opaque `@color/eve_secondary_system_background` if transparency or battery saver is active. Fully cleans up listener on dismiss. Preserved exact title ("Submit test?"), question count calculations, positive "Submit" and negative "Cancel" actions, and server submission mechanics. Leaves all other dialogs and content cards untouched. Verified via unit test suite.

## [COMPLETED] Eve Design System Foundation & Home Screen Application (ADR-015)
- **Status**: Completed (2026-10-17)
- **Scope**: `EveDesignSystem.kt`, `MainActivity.kt`, `HomeAppearance.kt`, `ExamAdapter.kt`, `item_feedback_post.xml`, `dimens.xml`, `EveDesignSystemTest.kt`, ADR-015 (`DECISIONS.md`).
- **Details**: Built canonical `EveDesignSystem` Kotlin foundation facade (`com.eve.app.designsystem.EveDesignSystem`) unifying `Colors`, `Typography`, `Shapes`, `Spacing`, `Elevation`, `Motion`, `Materials`, and `Components`. Applied strictly and exclusively to Home screen components (banner squircle clipping/matte, drawer radius/background, streak pill, admin button, category chips, exam cards, and feedback card background). Kept all 35+ non-Home screens completely untouched. Verified via automated unit tests, 194 passing contrast pairs, and documentation verification.

## [COMPLETED] Scoped Home Screen Apple iOS 26 Depth & Motion Specification (ADR-016)
- **Status**: Completed (2026-10-17)
- **Scope**: `colors.xml`, `dimens.xml`, `integers.xml`, `styles.xml`, `themes.xml`, `MainActivity.kt`, `HomeAppearance.kt`, `TargetExamsBottomSheet.kt`, `EveMotionHelper.kt`, `EveTouchHelper.kt`, `EveDesignSystem.kt`, `press_scale.xml`, `bg_home_popup_menu.xml`, `bg_recovery_sheet.xml`, `item_exam.xml`, `item_feedback_post.xml`, `popup_telegram_menu.xml`, `activity_main.xml`, `AndroidManifest.xml`, ADR-016 (`DECISIONS.md`).
- **Details**: Full migration of Home screen to iOS 26 Depth (0dp card elevation, no shadows, 24dp blur on top bar/menus/sheets with 75% tint, 1dp hairline border, 94% solid tint fallback) and Motion (spring with damping ratio 0.8 / tension 0.65, 200-350ms duration window, 0.97 press tactile feedback scale). 100% elimination of hardcoded colors/dp/sp across touched files. Preserved Telegram theme toggle, all Lottie files/triggers/sizes, and non-Home screens.

## [P3] Accessibility: Extreme Font Scale (1.78x / 2.0x) Audit
- **Impact**: Low-Medium
- **Scope**: Navigation drawer list items in `activity_main.xml` and form layouts.
- **Details**: Verify text wrapping on narrow devices (320dp) under maximum system font scales to ensure UI rows remain cleanly accessible with zero overlap.

## [P1] iOS 26 migration: physical-device verification
- **Scope**: Device compositor blur, clear Sign In outside area, pre-31 fallback, live account/exam flows, and Eve/Device font appearance.
- **Details**: Both API 35 native navigation suites pass (34 tests each), with complete light/dark evidence gates. Requested chrome/modal and Home/Test/Result/Settings/Login captures were reviewed with fixture limitations recorded in docs/ios26/MIGRATION_REPORT.md. Inspect the remaining hardware and live-flow behavior on physical devices; emulator/Canvas evidence does not certify these items.

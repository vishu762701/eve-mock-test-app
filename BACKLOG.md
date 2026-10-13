# EVE Product & Engineering Backlog

Ranked by impact on user experience, stability, and design system fidelity per `ENGINEERING_STANDARD.md`.

## [COMPLETED] Phase 3: Official Apple iOS 26 Colors + Typography (Profile, Settings & Login)
- **Status**: Completed (2026-10-13)
- **Scope**: `activity_profile.xml`, `ProfileActivity.kt`, `activity_settings.xml`, `SettingsActivity.kt`, `bottom_sheet_font_picker.xml`, `activity_login.xml`, `LoginActivity.kt`.
- **Details**: Full migration of Profile, Settings, and Login screens to canonical Apple iOS 26 semantic color roles, typography hierarchy, adaptive switch green, zero hardcoded text sizes, and comprehensive automated JVM verification.

## [P1] Phase 4: Official Apple iOS 26 Typography Migration for Mock Test Screen
- **Impact**: High
- **Scope**: `activity_test.xml`, `item_question.xml`, `QuestionAdapter.kt`, `CircularTimerView.kt`, `QuestionPaletteAdapter.kt`.
- **Details**: Standardize question text, option labels (A–D), timer display, and 2x2 bottom action buttons (Clear, Review, Previous, Next/Submit) to canonical iOS 26 text styles while preserving rigid 2x2 geometry, server scoring, and answer state colors.

## [P2] Phase 5: Official Apple iOS 26 Typography Migration for Result & Secondary Screens
- **Impact**: Medium-High
- **Scope**: `activity_result.xml`, `ResultDetailActivity.kt`, `ResultActivity.kt`, syllabus, history, feedback, about.
- **Details**: Map Overview statistics, Performance breakdown, Analytics cards, Review explanations, and Leaderboard preview rows to iOS 26 typography hierarchy.

## [P3] Accessibility: Extreme Font Scale (1.78x / 2.0x) Audit
- **Impact**: Low-Medium
- **Scope**: Navigation drawer list items in `activity_main.xml` and form layouts.
- **Details**: Verify text wrapping on narrow devices (320dp) under maximum system font scales to ensure UI rows remain cleanly accessible with zero overlap.

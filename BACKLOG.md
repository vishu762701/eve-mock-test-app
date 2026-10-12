# EVE Product & Engineering Backlog

Ranked by impact on user experience, stability, and design system fidelity per `ENGINEERING_STANDARD.md`.

## [P1] Phase 3: Official Apple iOS 26 Typography Migration for Mock Test Screen
- **Impact**: High
- **Scope**: `activity_test.xml`, `item_question.xml`, `QuestionAdapter.kt`, `CircularTimerView.kt`, `QuestionPaletteAdapter.kt`.
- **Details**: Standardize question text, option labels (A–D), timer display, and 2x2 bottom action buttons (Clear, Review, Previous, Next/Submit) to canonical iOS 26 text styles while preserving rigid 2x2 geometry, server scoring, and answer state colors.

## [P2] Phase 4: Official Apple iOS 26 Typography Migration for Result Screens
- **Impact**: Medium-High
- **Scope**: `activity_result.xml`, `ResultDetailActivity.kt`, `ResultActivity.kt`, tab navigation, metric cards.
- **Details**: Map Overview statistics, Performance breakdown, Analytics cards, Review explanations, and Leaderboard preview rows to iOS 26 typography hierarchy.

## [P3] Phase 5: Official Apple iOS 26 Typography Migration for Secondary Screens
- **Impact**: Medium
- **Scope**: Profile, Syllabus, Manage Syllabus, History, Feedback, About, and Settings screens.
- **Details**: Audit and replace remaining hardcoded text sizes across secondary activities with semantic `TextAppearance.Eve.*` styles.

## [P4] Accessibility: Extreme Font Scale (1.78x / 2.0x) Navigation Drawer Audit
- **Impact**: Low-Medium
- **Scope**: Navigation drawer list items in `activity_main.xml`.
- **Details**: Verify text wrapping on narrow devices (320dp) under maximum system font scales to ensure drawer rows remain cleanly accessible with zero overlap.

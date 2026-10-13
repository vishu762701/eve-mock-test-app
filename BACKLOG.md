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

## [P3] Accessibility: Extreme Font Scale (1.78x / 2.0x) Audit
- **Impact**: Low-Medium
- **Scope**: Navigation drawer list items in `activity_main.xml` and form layouts.
- **Details**: Verify text wrapping on narrow devices (320dp) under maximum system font scales to ensure UI rows remain cleanly accessible with zero overlap.

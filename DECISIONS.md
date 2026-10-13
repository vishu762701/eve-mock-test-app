# Architectural Decisions

## ADR-001: Official Apple iOS 26 Typography Foundation for Android (Phase 2)

- **Date**: 2026-10-12
- **Status**: Accepted
- **Author**: Principal Apple Typography Engineer, Android UI Architect

### Context
EVE is undergoing a controlled 5-phase migration to Apple iOS 26 design principles. Phase 1 established the dynamic semantic color foundation. Phase 2 establishes the official Apple iOS 26 dynamic typography foundation and applies it to the EVE Home screen and its directly associated UI components.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple Developer UIKit UIFont.TextStyle: https://developer.apple.com/documentation/uikit/uifont/textstyle
- Apple Human Interface Guidelines - Accessibility: https://developer.apple.com/design/human-interface-guidelines/accessibility
- Android Developers - Typography in Material Design / Android: https://developer.android.com/design/ui/mobile/guides/styles/typography

### Decisions
1. **11 Canonical Text Styles in `app/src/main/res/values/type.xml`**:
   - `LargeTitle` (`34sp`, bold, line height `41sp`, letterSpacing `-0.02`)
   - `Title1` (`28sp`, bold, line height `34sp`, letterSpacing `-0.015`)
   - `Title2` (`22sp`, bold, line height `28sp`, letterSpacing `-0.01`)
   - `Title3` (`20sp`, medium, line height `25sp`)
   - `Headline` (`17sp`, medium / semibold, line height `22sp`, letterSpacing `-0.01`)
   - `Body` (`17sp`, regular, line height `22sp`, letterSpacing `-0.01`)
   - `Callout` (`16sp`, regular, line height `21sp`, letterSpacing `-0.01`)
   - `Subheadline` (`15sp`, regular, line height `20sp`)
   - `Footnote` (`13sp`, regular, line height `18sp`)
   - `Caption1` (`12sp`, regular, line height `16sp`)
   - `Caption2` (`11sp`, medium, line height `13sp`, letterSpacing `0.01`)

2. **Component-Specific Typography Tokens**:
   - `TextAppearance.Eve.Eyebrow`: `12sp`, medium, uppercase tracking `0.04`, line height `16sp` (EVE branding text).
   - `TextAppearance.Eve.Subheadline.Medium`: `15sp`, medium, line height `20sp` (Category headers, navigation drawer items).
   - `TextAppearance.Eve.Footnote.Medium`: `13sp`, medium, line height `18sp` (Streak summary, category chips, badges).
   - `TextAppearance.Eve.Caption1.Medium`: `12sp`, medium, line height `16sp` (Offline banner, status badges).
   - `TextAppearance.Eve.Search`: `17sp`, regular, mapped to `@color/eve_header_ink_text` and `@color/eve_placeholder_text`.

3. **Android Platform Typeface & Hindi/Devanagari Safety**:
   - Do not bundle proprietary Apple SF Pro font files.
   - Use standard Android system typefaces (`sans-serif` and `sans-serif-medium`).
   - Ensures zero font file bloat, full system fallback support for Devanagari (Hindi) glyphs, and native Dynamic Type scaling under Android font accessibility settings.

4. **Line Height Compatibility**:
   - Use `lineHeight` attribute on styles rather than `android:lineHeight` to prevent lint warnings across minSdk 24 while maintaining full AppCompat and Material Components leading support.

5. **Home Screen Migration**:
   - Removed 100% of hardcoded `android:textSize` instances in `activity_main.xml`, `item_exam.xml`, and `item_category_header.xml`.
   - Programmatic category chips in `HomeAppearance.kt` styled via `setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)` and `sans-serif-medium`.

## ADR-002: Official Apple iOS 26 Typography and Semantic Colors for Profile, Settings & Login (Phase 3)

- **Date**: 2026-10-13
- **Status**: Accepted
- **Author**: Principal Apple Design Systems Engineer, Android UI Architect

### Context
Continuing the 5-phase migration, Phase 3 applies the authoritative Apple iOS 26 typography hierarchy and dynamic semantic color foundations to the complete Profile, Settings, and Login user journeys.

### Decisions
1. **Component-to-Style Mapping**:
   - **Screen Titles**: `TextAppearance.Eve.Title3` (`20sp`, medium, `25sp` line height).
   - **User Identity & Taglines**: `TextAppearance.Eve.Title1` (`28sp`, bold, `34sp` line height) for login hero taglines; `TextAppearance.Eve.Title2` (`22sp`, bold, `28sp` line height) for profile name display.
   - **Section Headings & Group Overlines**: `TextAppearance.Eve.Eyebrow` (`12sp`, medium, uppercase) for grouped settings overlines; `TextAppearance.Eve.Headline` (`17sp`, medium, `22sp` line height) for profile form section headers.
   - **Form Inputs**: `TextAppearance.Eve.Body` (`17sp`, regular, `22sp` line height) for profile personal details; `TextAppearance.Eve.Callout` (`16sp`, regular, `21sp` line height) for authentication credentials.
   - **List Rows & Setting Titles**: `TextAppearance.Eve.Body` (`17sp`, regular) with primary label tint.
   - **Row Captions & Samples**: `TextAppearance.Eve.Footnote` (`13sp`, regular, `18sp` line height) with secondary label tint.
   - **Inline Validation & Destructive Rows**: `TextAppearance.Eve.Footnote` with `@color/eve_destructive_text` (#96211C Light / #FF9A92 Dark) passing WCAG AAA contrast.
   - **Primary Action Buttons**: `TextAppearance.Eve.Headline` (`17sp`, medium) for full-width primary submit and save controls.
   - **Secondary & Tertiary Buttons**: `TextAppearance.Eve.Subheadline.Medium` (`15sp`, medium) for social auth and authentication toggle controls.

2. **Switch Track Contrast and Accessibility Compliance**:
   - Preserved `@color/eve_primary` for `state_checked="true"` in `selector_eve_switch_track.xml`.
   - Rationale: Under WCAG 2.1 SC 1.4.11 (Non-text Contrast) and the repository's native UI test contract (`SharedDesignSystemTest`), the graphical switch thumb (`#FFFFFF`) against the track must satisfy at least 3.0:1 contrast ratio (`ColorUtils.calculateContrast(thumb, track) >= 3.0`). While Apple HIG mentions system green for iOS devices, on Android `#34C759` against `#FFFFFF` achieves only 2.22:1 contrast. `@color/eve_primary` (`#0088FF`) provides 3.52:1 contrast in Day mode and 3.1:1 in Night mode, strictly satisfying WCAG AA non-text contrast and the EVE design system contract.

3. **Elimination of Hardcoded Text Sizes**:
   - Eliminated 100% of hardcoded `android:textSize` attributes in `activity_profile.xml`, `activity_settings.xml`, `bottom_sheet_font_picker.xml`, and `activity_login.xml`.
   - Updated programmatic progress dialogs in `ProfileActivity.kt` and `SettingsActivity.kt` to `TextAppearance.Eve.Subheadline.Primary`.

4. **Protected Requirements Preserved**:
   - Maintained pure `#FFFFFF` Light / `#000000` Dark canvas invariant.
   - Preserved circular Telegram theme transition, student photo un-tinted rendering, and authentication business logic.

## ADR-003: Official Apple iOS 26 Typography for Mock Test Screen (Phase 4)

- **Date**: 2026-10-13
- **Status**: Accepted
- **Author**: Principal Apple Typography Engineer, Android UI Architect

### Context
Phase 4 applies the official Apple iOS 26 dynamic typography hierarchy to the active Mock Test screen (`activity_test.xml`, `item_question.xml`, and `item_palette_circle.xml`) while preserving rigid test viewport geometry, 2x2 action button bounds, Telegram radio selection animations, server scoring integrity, and color invariants.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple Developer UIKit UIFont.TextStyle: https://developer.apple.com/documentation/uikit/uifont/textstyle
- Android Developers - Support different font sizes: https://developer.android.com/develop/ui/views/typography/scalable-type

### Decisions
1. **Mock Test Typography Role Mapping**:
   - **Exam / Top Bar Title** (`tvTestTitle`): `TextAppearance.Eve.Test.Title` (parented to `TextAppearance.Eve.Headline`: `17sp`, medium, `22sp` line height, single line with `ellipsize="end"`).
   - **Question Text** (`tvQuestion`): `TextAppearance.Eve.QuestionText` (parented to `TextAppearance.Eve.Callout`: `16sp`, regular, `24sp` line height, with explicit `android:lineSpacingMultiplier="1.5"` preserved per test engine specification).
   - **Per-Question Live Timer Chip** (`tvQuestionTimer`): `TextAppearance.Eve.Test.QuestionTimer` (parented to `TextAppearance.Eve.Footnote.Medium.Secondary`: `13sp`, medium, `18sp` line height).
   - **Option Choices A–D** (`rbA`, `rbB`, `rbC`, `rbD`): `TextAppearance.Eve.Test.Option` (parented to `TextAppearance.Eve.Subheadline`: `14sp`, regular, `20sp` line height).
   - **Question Palette Cell Number** (`tvCircleNumber`): `TextAppearance.Eve.Test.PaletteNumber` (parented to `TextAppearance.Eve.Caption1.Medium`: `12sp`, medium, centered).
   - **Circular Countdown Timer** (`CircularTimerView.kt`): Scalable `sp(12f)` dynamically refreshed in `onSizeChanged` to support Dynamic Type accessibility scaling.
   - **Offline Banner** (`tvOfflineBanner`): `TextAppearance.Eve.Test.OfflineBanner` (parented to `TextAppearance.Eve.Caption1.Medium`: `12sp`, bold, centered).
   - **Loading State Text**: `TextAppearance.Eve.Test.Loading` (parented to `TextAppearance.Eve.Subheadline`: `14sp`, regular).
   - **Message / Error State** (`tvMessage`, `tvMessageSub`): `TextAppearance.Eve.Test.Message` (`16sp`, bold, Headline role) and `TextAppearance.Eve.Test.MessageSub` (`13sp`, regular, Footnote role).
   - **Bottom 2x2 Action & Navigation Buttons** (`btnClear`, `btnMarkReview`, `btnPrev`, `btnNext`): Retained `Widget.Eve.ApprovedTestAction` (`13sp`, medium, Footnote.Medium metric) and `Widget.Eve.ApprovedTestNavigation` / `Widget.Eve.ApprovedTestNext` (`14sp`, medium) with zero elevation and approved corner radius.

2. **Elimination of Hardcoded Text Sizes**:
   - Removed 100% of hardcoded `android:textSize` attributes across `activity_test.xml`, `item_question.xml`, and `item_palette_circle.xml`.

3. **Invariants Preserved**:
   - Preserved `android:lineSpacingMultiplier="1.5"` on `tvQuestion` and `questionScroll` container.
   - No `BlurView` added to active test layouts.
   - Preserved server-side submission, attempt limit checks, and rigid 2x2 bottom grid layout.

## ADR-004: Official Apple iOS 26 Typography and Semantic Colors for Result and Secondary Screens (Phase 5)

- **Date**: 2026-10-13
- **Status**: Accepted
- **Author**: Principal Apple Design Systems Engineer, Senior Android Architect

### Context
Phase 5 completes the 5-phase migration to official Apple iOS 26 dynamic typography and semantic color foundations across all active student-facing Result and secondary screens (Leaderboard, History, Syllabus, Feedback, Dialogs, and About).

### Official Documentation References Opened
- Apple Human Interface Guidelines - Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple Human Interface Guidelines - Color: https://developer.apple.com/design/human-interface-guidelines/color
- Apple Developer UIKit UIFont: https://developer.apple.com/documentation/uikit/uifont
- Apple Developer UIKit UIColor: https://developer.apple.com/documentation/uikit/uicolor
- Apple Human Interface Guidelines - Accessibility: https://developer.apple.com/design/human-interface-guidelines/accessibility

### Decisions
1. **Canonical Typography Role Mapping**:
   - **Hero Score Display** (`tvTotalScore`): `TextAppearance.Eve.Result.HeroScore` (`36sp`, bold, LargeTitle role, tabular numerals).
   - **Hero Score Badge** (`tvScorePercentage`): `TextAppearance.Eve.Result.ScoreBadge` (`13sp`, medium, Footnote.Medium role).
   - **Metric Counts** (`tvCorrectCount`, `tvIncorrectCount`, `tvSkippedCount`): `TextAppearance.Eve.Result.MetricCount` (`24sp`, bold, Title2 role).
   - **Metric Labels** (`tvCorrectLabel`, `tvIncorrectLabel`, `tvSkippedLabel`): `TextAppearance.Eve.Result.MetricLabel` (`13sp`, medium, Footnote.Medium role).
   - **Result Detail Inset Headers & Values**: `TextAppearance.Eve.Result.CardHeader` (`15sp`, medium) and `TextAppearance.Eve.Result.InsetValue` (`15sp`, bold).
   - **Answer Review Question Text** (`tvQ`): `TextAppearance.Eve.QuestionText` (`16sp`, regular, Callout role).
   - **Answer Review Option Badges & Text**: `TextAppearance.Eve.Result.OptionKey` (`13sp`, medium) and `TextAppearance.Eve.Result.OptionText` (`16sp`, regular).
   - **Solution Explanation**: `TextAppearance.Eve.Result.ExplanationHeader` (`15sp`, bold) and `TextAppearance.Eve.Result.ExplanationBody` (`15sp`, regular).
   - **Leaderboard**: `TextAppearance.Eve.Leaderboard.Rank` (`15sp`, medium), `TextAppearance.Eve.Leaderboard.Name` (`15sp`, medium), `TextAppearance.Eve.Leaderboard.Score` (`15sp`, regular).
   - **Secondary Items (History, Syllabus, Feedback, About)**:
     - Titles: `TextAppearance.Eve.Secondary.ItemTitle` (`17sp`, medium, Headline role)
     - Section Headers: `TextAppearance.Eve.Secondary.SectionHeader` (`20sp`, medium, Title3 role)
     - Subtitles / Metadata: `TextAppearance.Eve.Secondary.ItemSubtitle` (`13sp`, regular, Footnote role)
     - Body Text: `TextAppearance.Eve.Secondary.Body` (`15sp`, regular, Subheadline role)
     - Search Input: `TextAppearance.Eve.Search` (`17sp`, regular, Body role)

2. **100% Elimination of Hardcoded Text Sizes**:
   - Removed 100% of hardcoded `android:textSize` attributes across 19 production layout files (`activity_result.xml`, `activity_result_detail.xml`, `item_answer.xml`, `item_result_topic.xml`, `item_topic_stat.xml`, `activity_leaderboard.xml`, `item_leaderboard.xml`, `activity_history.xml`, `item_history.xml`, `activity_syllabus.xml`, `item_syllabus.xml`, `activity_feedback.xml`, `item_feedback_message.xml`, `item_post_reply.xml`, `dialog_create_feedback_post.xml`, `dialog_reply_feedback.xml`, `dialog_report_question.xml`, `activity_about.xml`, `activity_content_display.xml`).
   - Converted programmatic text sizing in `ResultDetailActivity.kt` and `ResultAnalysisRows.kt` to use `TextViewCompat.setTextAppearance()`.

3. **Strict Leaderboard Boundary Maintained**:
   - Zero structural or visual redesign performed on the Leaderboard. Existing card geometry, rankings, and components preserved completely per user rules in `AGENTS.md`.

4. **Invariants Preserved**:
   - Pure `#FFFFFF` Light / `#000000` Dark canvas.
   - Protected exam status colors (`eve_tile_right_*`, `eve_tile_wrong_*`, `eve_tile_medium_*`).
   - Server-side scoring and test submission integrity.
   - Minimum 48dp touch targets on interactive controls.


## ADR-005: Profile-only grouped form surface (iOS 26 semantic hierarchy)

- **Date**: 2026-10-13
- **Status**: Accepted, subject to native CI verification
- **Scope**: Profile screen only. No shared theme tokens, other screens, protected animations, Lottie, or backend changes.
- **Evidence**: Native Day/Night Profile and Settings fixture screenshots from the GitHub Actions native UI artifact show Profile's previous standard white canvas with gray form card; Settings uses grouped gray canvas with white grouped rows.
- **Decision**: Reuse the existing semantic grouped background only for the Profile root (Day #F2F2F7, Night #000000), and the Profile-exclusive personal-information card (Day #FFFFFF, Night #1C1C1E). The opaque toolbar stays unchanged. This is an explicit Profile-specific override of the earlier standard-white treatment; canonical global canvas tokens are unchanged.
- **Why**: Profile is an account form, and this grouped form treatment creates a clear surface hierarchy without altering accent saturation, typography, geometry, interactions, or approved states.
- **Verification**: ProfileSettingsLoginTypographyAndColorTest.testProfileScopedGroupedDayNightHierarchy, lint/unit/build, both native navigation modes, and Day/Night Profile screenshots.
- **Documentation**: Apple HIG Color (https://developer.apple.com/design/human-interface-guidelines/color; JS-required shell in this environment) and existing EVE_IOS26_UI_CONTRACT.md for Android tokens. No claim of native UIKit rendering.


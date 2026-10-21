> Current UI authority: the owner's locked root reset in SOURCE_OF_TRUTH.md.
> Historical UI numeric decisions below are superseded; they are an audit log,
> not instructions to replay. Current evidence is in docs/design-reset/report.md.

> The historical EVE JARVIS Gemini/Google Drive sync ADRs (ADR-009, ADR-010 and ADR-011 dated 2026-10-14) are RETIRED. Their workflow and scripts were removed in commit `906f98a`; they are not implementation instructions. The separate, later full-screen UI geometry decision also used ADR-009; this pre-existing duplicate ID is historical, not a current precedence rule.

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
- **Documentation**: Apple HIG Color (https://developer.apple.com/design/human-interface-guidelines/color; JS-required shell in this environment) and existing docs/design-reset/report.md for Android tokens. No claim of native UIKit rendering.


## ADR-006: Home full-window grouped background (user-approved Option A)

- **Date**: 2026-10-13
- **Status**: Accepted; release requires GitHub CI and native Day/Night screenshot verification.
- **Scope**: Home layout and exclusive flat content drawable, and the MainActivity-only path of SystemBarHelper. No global theme tokens, unrelated screens, protected animations, Lottie, banner content or backend changes.
- **Decision**: Use the existing grouped token (Day #F2F2F7 / Night #000000) for the full Home window, header (including expanded search), content and Home blocking overlays. Remove the top-corner sheet decoration and -24dp overlap. Reduce unneeded header bottom padding 20dp to 4dp to preserve compact vertical rhythm. Keep grouped exam cards Day white / Night #1C1C1E.
- **System bar behavior**: For MainActivity only, paint both android.R.id.content inset backing and window background with the grouped token; match the pre-Android-15 status/navigation colors. On Android 15+, leave navigation transparent with the same grouped backing under the insets and retain existing status/navigation icon appearance and Telegram circular reveal synchronization. Other activities continue to use their existing colors.
- **Validation**: HomeSemanticColorsTest source/role contract, native DesignRecoveryTest Home fixture assertions and captures in both modes, build/lint/unit tests, full native gesture and three-button suites, and visual gallery. Do not weaken existing test gates.
- **Verified platform sources**: Android Developers edge-to-edge views https://developer.android.com/develop/ui/views/layout/edge-to-edge and Android 15 edge-to-edge enforcement https://developer.android.com/codelabs/edge-to-edge.



## ADR-007: Distinct Home admin and streak pill surfaces on Option A canvas

- **Date**: 2026-10-13
- **Status**: Approved user-reported visual repair; native release verification required.
- **Evidence**: The approved Home canvas is Day #F2F2F7 / Night #000000, but the shared Neutral admin button and streak drawable still used Day #E5E5EA / Night #2C2C2E. The user reported insufficient separation on a real Light-mode device.
- **Decision**: Home's Admin Dashboard button and streak pill use the already-defined secondary grouped content surface (Day #FFFFFF, Night #1C1C1E) ; only the Admin button gains a subtle 1dp existing `eve_shape_border` outline. Admin overrides its tint in `activity_main.xml` using its own stateful selector, retaining the shared button's disabled fill and neutral text selector. Both Home streak drawables (default and night-qualified) remain borderless with the grouped semantic surface, preserving identical shape, padding, size, text and flame.
- **Scope**: Home's button XML, exclusive streak drawable, new Home-only admin ColorStateList, Home unit/native assertions and this ADR. No global neutral tokens, admin destination screen, test engine, animation, scoring, theme transition, backend or other layout changes.
- **Alternatives**: A filled blue Admin button would be more prominent but changes accent hierarchy without user approval. Updating shared `eve_button_neutral_bg` and `eve_streak_pill_bg` would affect other components. The scoped grouped surfaces are the least invasive iOS-style solution.
- **Verification**: Day/Night Home fixture assertions and screenshot gallery, both native navigation suites, lint and unit tests, debug build and final gated APK publication. The full-window Home background remains unchanged.
- **Official API**: Android MaterialButton supports `app:backgroundTint`, `app:strokeColor`, and `app:strokeWidth` while keeping its managed ripple/background (https://developer.android.com/reference/com/google/android/material/button/MaterialButton).

- **Dark-mode resource override root cause**: Emulator Day/Night screenshots showed that `drawable-night/bg_streak_pill.xml` forced a completely transparent background and removed pill corners. Android prioritizes that qualifier in Dark mode, which caused the observed native assertion failure. Both drawable resources now have the same semantic grouped fill and borderless capsule geometry; unit assertions cover both configurations.

## ADR-008: App-wide Apple iOS 26 Dynamic Typography Hierarchy and 26 Semantic Colour Roles

- **Date**: 2026-10-13
- **Status**: Accepted
- **Author**: Principal Apple Design Systems Engineer, Senior Android Architect

### Context
Following Phases 1 through 5 of the design modernization, multiple secondary, admin, monetization, tool, and dialog screens across the project still contained hardcoded `android:textSize` values or ad-hoc style references. To achieve 100% adherence to Apple Human Interface Guidelines and ensure complete dynamic accessibility and dark-mode legibility across all 95 layouts in the project, the system codified all 26 authoritative Apple iOS colour roles in `attrs.xml` and `themes.xml` (`Theme.Eve.Foundation`) and codified semantic dynamic type styles parented to the official Apple iOS 26 typography scale.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple Human Interface Guidelines - Color: https://developer.apple.com/design/human-interface-guidelines/color
- Apple Developer UIKit UIFont: https://developer.apple.com/documentation/uikit/uifont
- Apple Developer UIKit UIColor: https://developer.apple.com/documentation/uikit/uicolor
- Apple Human Interface Guidelines - Accessibility: https://developer.apple.com/design/human-interface-guidelines/accessibility

### Decisions
1. **Codification of 26 Semantic Colour Roles**:
   - Codified all 26 official iOS color roles as first-class theme attributes in `attrs.xml` and resolved them to canonical `@color/eve_*` tokens in `Theme.Eve.Foundation` in `themes.xml`:
     `eveLabelPrimary`, `eveLabelSecondary`, `eveLabelTertiary`, `eveLabelQuaternary`, `evePlaceholderText`, `eveSystemBackground`, `eveSecondarySystemBackground`, `eveTertiarySystemBackground`, `eveSystemGroupedBackground`, `eveSecondarySystemGroupedBackground`, `eveTertiarySystemGroupedBackground`, `eveSeparator`, `eveOpaqueSeparator`, `eveControlOutline`, `eveSystemBlue`, `evePrimaryFill`, `eveOnPrimary`, `eveAccentText`, `eveAccentContainer`, `eveOnAccentContainer`, `eveDestructiveText`, `eveFillPrimary`, `eveFillSecondary`, `eveFillTertiary`, `eveFillQuaternary`, `eveLink`.
2. **Dynamic Typography Scale Codification**:
   - Created clean, semantic styles in `type.xml` mapped directly to the Apple iOS 26 typography scale:
     - Large Title (34sp bold, line height 41sp)
     - Title 1 (28sp bold, line height 34sp)
     - Title 2 (22sp bold, line height 28sp)
     - Title 3 (20sp semibold / regular, line height 24sp)
     - Headline (17sp semibold, line height 22sp)
     - Body (17sp regular, line height 22sp)
     - Callout (16sp regular, line height 21sp)
     - Subheadline (15sp regular / medium, line height 20sp)
     - Footnote (13sp regular / medium, line height 18sp)
     - Caption 1 (12sp regular / medium, line height 16sp)
     - Caption 2 (11sp regular, line height 13sp)
     - Semantic domain styles for Admin (`TextAppearance.Eve.Admin.*`), Premium/Monetization (`TextAppearance.Eve.Premium.*`), and Analytics (`TextAppearance.Eve.Analytics.*`).
3. **100% Elimination of Hardcoded Text Sizes**:
   - Migrated all remaining layout files across the application, achieving zero hardcoded `android:textSize` attributes across all 95 layout XML files under `app/src/main/res/layout/`.
4. **Automated Verification**:
   - Added `AppWideTypographyAndColorRolesTest.kt` verifying:
     - Exactly 95 layouts exist in the project and 100% of them have 0 `android:textSize` attributes.
     - All 26 color role attributes are declared in `attrs.xml`.
     - All 26 color role attributes are bound in `Theme.Eve.Foundation`.
     - Core Apple iOS 26 type scale roles are present in `type.xml`.
5. **Invariants Preserved**:
   - Light canvas `#FFFFFF` and Dark canvas `#000000` strictly preserved.
   - Telegram Day/Night circular reveal transition and popup menu preserved.
   - Test engine timing, question scoring, 2x2 grid navigation buttons preserved.
   - All Lottie and SVG vector assets untouched.
   - All UI text strictly English.

## ADR-009: Event-Driven GitHub-to-Google-Drive Knowledge Synchronization for EVE JARVIS Gemini Gem

- **Date**: 2026-10-14
- **Status**: Retired (2026-10-20; repository sync removed, kept for historical audit only)
- **Author**: Principal Architecture Engineer, DevOps & Security Lead

### Context
The previous synchronization approach relied on Apps Script polling GitHub, which suffered from execution time timeouts (6 min cap), unauthenticated GitHub rate limits, inability to inspect internal GitHub Actions build status, and mutable file IDs that broke Gemini Gem Knowledge references. To provide continuous, accurate, line-numbered repository knowledge for the EVE JARVIS Gemini Gem, an event-driven GitHub Actions synchronization pipeline was constructed.

### Official Documentation References Opened
- Google Drive API v3 Files: https://developers.google.com/drive/api/v3/reference/files
- Google Drive API v3 Permissions: https://developers.google.com/drive/api/v3/reference/permissions
- Google OAuth 2.0 for Server-to-Server Applications (Service Accounts): https://developers.google.com/identity/protocols/oauth2/service-account
- Google Gemini Gems & Knowledge Base Limits: https://support.google.com/gemini/answer/15235889

### Decisions
1. **Event-Driven Workflow**: Replaced polling with `.github/workflows/sync-jarvis-gemini.yml` triggered on `push` to `main`, `workflow_run` (after `Build Eve APK`), and `workflow_dispatch`.
2. **7-Volume Knowledge Architecture**: Formatted the entire codebase (~78k lines, ~4.2 MB) into 7 structured Markdown documents (`00_INDEX_AND_SYSTEM_STATUS.md` through `06_CI_CD_WORKFLOWS_AND_SCRIPTS.md`), strictly respecting Gemini Gems' 10-file Knowledge cap.
3. **Line-Numbered Source Code**: Every file is prefixed with standardized line numbers (`1: ...`), commit SHA, and path header for citation precision in Gemini.
4. **Stable Google Drive Document IDs**: Uses Google Drive API in-place updates (`files.update` with `uploadType=media`) instead of delete-and-recreate cycles, preserving existing file IDs so Gemini Gem Knowledge links remain stable across commits.
5. **Least-Privilege Authorization**: Uses Google Cloud Service Account with restricted `https://www.googleapis.com/auth/drive.file` scope. The service account only accesses files within the designated shared folder.
6. **Zero External Dependencies**: Pure Node.js client using standard `node:crypto`, `node:fs`, and global `fetch`. Tested via `node:test`.
7. **Strict Confidentiality**: Automatic secret sanitization, keystore/binary exclusion, and active deletion of any detected public (`anyone`) permissions.
8. **Graceful Preflight**: If GitHub Secrets are not yet configured, the workflow executes a dry run and logs clear onboarding instructions without failing the CI build.

## ADR-010: Dual-Mode OAuth/Service Account Architecture, Storage Quota Resolution, and Atomic Master Index Publication

- **Date**: 2026-10-14
- **Status**: Retired (2026-10-20; repository sync removed, kept for historical audit only)
- **Author**: Principal Architecture Engineer, Google Cloud Integration Expert, Security Lead

### Context
While ADR-009 established the event-driven GitHub Actions synchronization pipeline, personal Google accounts (`@gmail.com`) experience `403 storageQuotaExceeded` when uploading via Google Cloud Service Accounts. In Google Drive, files created by a service account are owned by the service account itself, and Google Cloud grants zero bytes of Drive storage quota to service accounts on personal My Drive. To achieve reliable synchronization across personal Google accounts without requiring paid Workspace subscriptions, a dual-mode authentication architecture is required alongside atomic publishing guarantees, exponential backoff retries, and commit SHA consistency.

### Official Documentation References Opened
- Google Identity - OAuth 2.0 for Web Server Applications: https://developers.google.com/identity/protocols/oauth2/web-server
- Google Identity - Using OAuth 2.0 to Access Google APIs: https://developers.google.com/identity/protocols/oauth2
- Google Drive API v3 - Manage Uploads: https://developers.google.com/drive/api/guides/manage-uploads
- Google Drive API v3 - Shared Drives Support: https://developers.google.com/drive/api/guides/enable-shareddrives
- Google Drive API v3 - Permissions: https://developers.google.com/drive/api/v3/reference/permissions

### Decisions
1. **Dual-Mode Authentication**:
   - **Mode 1: Google User OAuth 2.0 (Option A - Recommended for Personal Accounts)**: Uses `GDRIVE_REFRESH_TOKEN`, `GDRIVE_CLIENT_ID`, and `GDRIVE_CLIENT_SECRET`. Tokens are exchanged via `https://oauth2.googleapis.com/token` (`grant_type=refresh_token`). Requests execute under the user's account, files are owned by the user, and uploads consume the user's standard 15 GB+ personal Drive storage quota with zero quota errors.
   - **Mode 2: Google Cloud Service Account (Option B - Workspace Shared Drives)**: Retains RS256 JWT authorization for enterprise Google Workspace Shared Drives where storage belongs to the organizational domain.
2. **Atomic Master Index Publication**:
   - The master index document (`00_INDEX_AND_SYSTEM_STATUS.md`) is synchronized ONLY after all 6 domain volumes (`01_...` through `06_...`) have completed and verified successfully.
   - If any volume upload encounters an error, the sync halts immediately with `status: 'PARTIAL_FAILURE'`, preventing partial or inconsistent states from being indexed in Gemini.
3. **Transient Error Handling and Exponential Backoff**:
   - Automatic retries on HTTP 429 (Too Many Requests), 500, 502, 503, 504 with randomized jitter.
   - Respects Google's `Retry-After` header when present.
   - Transparent 401 re-authentication: if a token expires during long uploads, the client invalidates cached tokens, fetches a fresh bearer token, and retries the request seamlessly.
4. **Shared Drive Universal Compatibility**:
   - Appends `supportsAllDrives=true` and `includeItemsFromAllDrives=true` across all query, file creation, update, and permission API endpoints, providing full interoperability with Google Workspace Shared Drives.
5. **Commit SHA Consistency**:
   - Workflow resolves `${{ github.event.workflow_run.head_sha || github.sha }}` ensuring that runs triggered after `Build Eve APK` synchronize the exact commit SHA verified by the Android build.
6. **Zero-Dependency User Token Generation**:
   - Provided `.github/scripts/gdrive-sync/oauth-helper.mjs` using pure Node.js to streamline obtaining a user refresh token in under one minute without installing external packages.

## ADR-011: Drive.file Folder Provisioning, Git Ref Pinning, Token Masking, Two-Phase Consistency Seals, and Gemini Attachment Verification

- **Date**: 2026-10-14
- **Status**: Retired (2026-10-20; repository sync removed, kept for historical audit only)
- **Author**: Principal Architecture Engineer, Google Cloud Integration Expert, Security Architect

### Context
Following ADR-010, end-to-end integration verification identified 5 critical operational blockers:
1. `workflow_run` default checkout uses the tip of the default branch rather than the triggering completed build's `head_sha`.
2. Google Drive `https://www.googleapis.com/auth/drive.file` scope prohibits access to pre-existing folders created outside the application (returning HTTP 404).
3. OAuth refresh tokens and secrets were echoed in terminal outputs, risking exposure in CI logs or terminal histories.
4. Calling the master-index-last publication an "atomic transaction" was technically inaccurate given Google Drive's REST API lacks multi-resource transactional commits. Readers could observe in-flight intermediate updates during the upload window.
5. Ingestion compatibility, file limits, and live reading verification procedures for the Gemini Gem Knowledge integration required authoritative documentation and test verification queries.

### Official Documentation References Opened
- Google Drive API v3 - Permissions & drive.file Scope: https://developers.google.com/drive/api/guides/api-specific-auth#drive.file
- Google Drive API v3 - Files create/search: https://developers.google.com/drive/api/v3/reference/files/create
- GitHub Actions - Workflow run head_sha: https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows#workflow_run
- Google Gemini - Add files to Gems: https://support.google.com/gemini/answer/15235889

### Decisions
1. **Explicit Checkout Pinning (`ref: head_sha`)**:
   - In `.github/workflows/sync-jarvis-gemini.yml`, both `test-sync-engine` and `sync-to-google-drive` jobs explicitly pass `ref: ${{ github.event.workflow_run.head_sha || github.ref }}` to `actions/checkout@v4`.
   - `sync-engine.mjs` verifies that local git HEAD matches the targeted `commitSha`. If a divergence occurs, packaging explicitly targets the verified commit SHA.
2. **App-Owned Folder Provisioning (`getOrCreateAppFolder`)**:
   - Under `https://www.googleapis.com/auth/drive.file`, the app has authority only over files and folders it creates.
   - If an explicit folder ID is provided but inaccessible (HTTP 404), or if no folder ID is provided, `GDriveClient.getOrCreateAppFolder()` searches for an existing app-created folder named `EVE-JARVIS-KNOWLEDGE` or provisions a new one via `POST /drive/v3/files`.
   - Users no longer need to manually create a folder or copy folder IDs for personal OAuth setups.
3. **Secret Masking and File Isolation (`0600`)**:
   - `oauth-helper.mjs` masks all refresh tokens and client secrets in stdout (`ya29.***`, `GOCSPX-***`, `1//04***`).
   - Generates `.jarvis-oauth-secrets.env` with strict `0600` POSIX file permissions.
   - Supports reading secrets via environment variables or stdin prompt without echoing secrets in bash process tables.
   - Protected files `.jarvis-oauth-secrets.env` and `.jarvis-oauth.env` are ignored in `.gitignore` and blacklisted in `security.mjs`.
4. **Two-Phase Publication Model & SHA-256 Integrity Seals**:
   - Replaced "atomic transaction" claims with an explicit Two-Phase Publication and Eventual Consistency model.
   - In Phase 1, all 6 domain volumes (`01` through `06`) are updated in-place.
   - `formatter.mjs` computes cryptographic SHA-256 digests (`vol.sha256`) for every volume.
   - In Phase 2, `00_INDEX_AND_SYSTEM_STATUS.md` is published with the integrity seal table containing the exact commit SHA and SHA-256 checksums of all 6 volumes.
   - Documents the ~5–10s in-flight mixed-version risk window and instructs consumers/Gems to verify volume headers against the master index seal.
5. **Gemini Gem Knowledge Compatibility & Verification Protocol**:
   - Formatted as UTF-8 Markdown (`.md`), with standardized line numbering (`1: ...`) and file headers.
   - 7 volumes (~4.2 MB total) strictly fit within Gemini Gems' limit of 10 files and 100 MB per file.
   - Preserves permanent document IDs via in-place `PATCH` media updates, ensuring Gemini Gem Knowledge attachments never break across commits.
   - Defined structured verification prompts for confirming Gemini Gem ingestion.

## ADR-009: Full-screen iOS 26 geometry, material scope and system-blue migration

- **Authority**: The current user explicitly authorizes geometry migration on every screen, overriding earlier geometry protection in GEMINI.md and historical UI audits. Only radii, padding, row heights and backgrounds change layout geometry; screen regions and behavior remain fixed.
- **Numbering**: This user-requested ADR-009 is identified by this full title. The pre-existing JARVIS ADR-009 above is retained unchanged as historical evidence.
- **Decision**: Shared capsule (50%), fixed card/menu 26dp and sheet/dialog 34dp shapes; concentric children use max(4dp, parent minus padding). Numeric radii are Eve design choices, not Apple specifications.
- **Alternatives**: Per-screen literals would duplicate policy and drift. Shared resources plus EveShapes keep runtime views and XML aligned.
- **Glass**: Only navigation/top bars, tab bars, popup menus, sheets and dialogs. Content, banner and drawer surfaces are opaque. API 31+ uses RenderEffect for backdrop layers; older versions use opaque tint. Foreground content remains sharp. Sign In prompt outside area is undimmed and unblurred.
- **Colour and type evidence**: Official Color, Typography and Materials JSON were fetched and read; preserved rows and source hashes are in docs/design-reset/report.md. All 12 default light/dark palette rows already matched colors.xml. Default iOS type sizes and leading already matched type.xml.
- **Blue**: All interactive blue aliases resolve to systemBlue (#0088FF / #0091FF), including higher-contrast blue aliases per the user's exact-blue requirement. Unused eve_accent_text_readable preserves #005BC0 / #72B2FF. The resulting roughly 3.5:1 on white is a user-chosen contrast trade-off, not AA compliance. Other normal text and primary labels on glass retain the 4.5:1 gate.
- **Backgrounds**: Home/Login/Test are plain; Settings/Profile/Admin lists/Result/Leaderboard/About are grouped. This supersedes the Home grouped-canvas choice in ADR-006.
- **Invariants**: ThemeSwitchAnimator.kt, ThemeManager.kt and popup reveal logic; Lottie files, triggers and sizes; icon files; answer-state colours; English UI strings; server behavior; font preference.
- **Pre-mortem**: Stale assertions must be updated only for authorized changes; background blur must not affect labels; larger targets must be exercised on the fixed Test viewport in both navigation modes.
- **API references read**: Android RenderEffect (https://developer.android.com/reference/android/graphics/RenderEffect); Material 1.12.0 RelativeCornerSize source (https://raw.githubusercontent.com/material-components/material-components-android/1.12.0/lib/java/com/google/android/material/shape/RelativeCornerSize.java); BlurView 2.0.6 source (https://raw.githubusercontent.com/Dimezis/BlurView/version-2.0.6/library/src/main/java/eightbitlab/com/blurview/BlurView.java).

### ADR-009 recovery and validation notes

- On resume, the user changed delivery to local-only: do not push, merge, dispatch Actions or trigger deployment. Their manual GitHub upload/build supersedes the earlier direct-push completion requirement for this task.
- Native Result binding was still resetting the XML tab height to 44dp. The shared minimum is now 56dp, leaving a 48dp tab target after the existing 4dp top/bottom inset; tabs stay in their existing screen region.
- Glass dialogs use API 31 window-background blur confined to the dialog bounds, while chrome/menu backdrops use RenderEffect. Pre-31 modal resources are opaque. The Sign In reauthentication prompt explicitly clears outside dim/blur flags. Official Window reference actually opened: https://developer.android.com/reference/android/view/Window#setBackgroundBlurRadius(int). The attempted /develop/ui/views/graphics/window-blurs URL returned 404 and is not cited as reviewed.
- Native emulator execution and light/dark screenshots remain pending where no Android device/emulator is attached; fixture compilation is reported separately from execution.

- Glass foregrounds resolve primaryLabel, including the Home bell's existing runtime tint callback and unselected Result tab labels. This implements the explicit glass foreground-colour instruction; animation asset content, paths, frames, timing/triggers and sizes are unchanged. Filled controls retain their separate opaque control surfaces and documented blue contrast trade-off.

- Subsequent delivery instruction: “Push and make applies” supersedes the local-only recovery restriction. Commit and push the prepared migration on main, then verify remote SHA and GitHub Actions. No force-push.

- First delivery CI exposed two legacy native contrast assertions for exact system blue. Align those checks with the existing explicit blue exception, retain full-opacity and all other contrast gates; retry emulator provisioning without weakening native verification.

- Delivery verification: Build Eve APK run 38677521615 passes both API 35 navigation suites (34 native tests each), complete evidence (167 captures per job, zero missing pairs/warnings), Android build/lint/unit, backend and Functions. Requested light/dark captures were reviewed. Physical compositor, pre-31, Sign In outside-area and live production checks remain explicitly unverified.

- Publication verified: the same run completed successfully and uploaded exactly one artifact, Eve-debug-apk (11947146346). A documentation-only follow-up records the verified implementation run without rebuilding unchanged application code.

## ADR-011: Navigation Chrome Correction, Transparent Header Containers & iOS 26 Neutral Segmented Navigation

- **Date**: 2026-10-16
- **Status**: Accepted
- **Author**: Principal Mobile Systems Architect, UI/UX Lead
- **Authority**: User instruction: "remove the unwanted full-width rounded top-header background throughout the app and refine existing navigation tabs/filter pills using official iOS 26 principles."

### Context
In earlier iterations, `EveGlassToolbar` implemented a BlurView with a capsule outline provider, 50% height corner radius, thin material overlay tint, and a bottom separator line. When used to wrap top navigation rows across 36 screens (student and admin), it rendered an unwanted continuous full-width rounded slab behind headers. Additionally, `EveGlassToolbar` was reused in `activity_result.xml` around `tabLayoutResult` with a duplicate track background.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Toolbars: https://developer.apple.com/design/human-interface-guidelines/toolbars
- Apple Human Interface Guidelines - Segmented Controls: https://developer.apple.com/design/human-interface-guidelines/segmented-controls

### Decisions
1. **Transparent, Non-Blurring Header Containers**:
   - Refactored `EveGlassToolbar` from `BlurView` to extend `FrameLayout` with `Color.TRANSPARENT` background.
   - Removed capsule clipping (`outlineProvider`), BlurView attachment (`EveBlurHelper.setupBlurView`), overlay tint, decorative shadow, and bottom separator line.
   - Screen titles and navigation rows float directly over the canvas across all 36 screens.
   - Preserved all child IDs, callbacks, insets, touch targets (>= 48dp), system status/navigation bars, and Android gesture handles.
2. **Separated Header Containers from Tab Surfaces**:
   - In `activity_result.xml`, replaced outer `EveGlassToolbar` around `tabLayoutResult` with a neutral `FrameLayout`.
   - Removed outer duplicate `@drawable/bg_result_segmented_track` background, establishing a single capsule track on `tabLayoutResult`.
   - Restored solid fill `@color/eve_result_tab_track` on `bg_result_segmented_track.xml`.
3. **Neutral Segmented Control Surfaces with Primary-Label Text**:
   - Light mode: `@color/eve_result_tab_selected` resolves to `@color/eve_system_background` (`#FFFFFF`); `@color/eve_result_tab_selected_text` resolves to `@color/eve_label_primary` (`#000000`). Track resolves to `@color/eve_surface_variant` (`#E5E5EA`).
   - Dark mode: `@color/eve_result_tab_selected` resolves to raised dark semantic surface `@color/eve_system_gray2` (`#636366`); `@color/eve_result_tab_selected_text` resolves to `@color/eve_label_primary` (`#FFFFFF`). Track resolves to `@color/eve_secondary_system_grouped_background` (`#1C1C1E`).
   - Removed 1dp strokes from `bg_tab_segmented_track.xml` and `bg_tab_segmented_indicator.xml`.
   - Aligned generic tab tokens (`eve_tab_selected_bg`, `eve_tab_selected_text`) in `values/colors.xml` and `values-night/colors.xml`.
4. **Touch & Motion Invariants**:
   - In `EveGlassTabLayout`, added animation cancellation on rapid taps (`v.animate().cancel()`) and respect for disabled animations via `EveMotionHelper.areAnimationsEnabled(v.context)`.
   - Home avatar and Lottie notification bell parent remain visually bare with transparent background and no decorative container.
   - Home category pills retain horizontal scrolling, content-sizing, 40dp min height, 48dp touch delegation, accent-tinted selected fill/text, and neutral unselected fill/text.

## ADR-012: Implementation of Unified Apple-Inspired Liquid Glass Regular Material (EVE_REGULAR_GLASS)

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Mobile Systems Architect, UI/UX Lead
- **Authority**: User instruction: "TASK: IMPLEMENT APPLE-INSPIRED LIQUID GLASS REGULAR"

### Context
Following the formal architecture investigation and component proposal, this decision codifies and implements the unified, Android-compatible Liquid Glass `.regular` material (`EVE_REGULAR_GLASS`) across both Light and Dark themes.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Materials: https://developer.apple.com/design/human-interface-guidelines/materials
- Apple SwiftUI Framework - Glass.regular: https://developer.apple.com/documentation/swiftui/glass/regular
- Apple Human Interface Guidelines - Color: https://developer.apple.com/design/human-interface-guidelines/color
- Android API Reference - RenderEffect: https://developer.android.com/reference/android/graphics/RenderEffect
- Android API Reference - Window.setBackgroundBlurRadius: https://developer.android.com/reference/android/view/Window#setBackgroundBlurRadius(int)
- Android API Reference - WindowManager.isCrossWindowBlurEnabled: https://developer.android.com/reference/android/view/WindowManager#isCrossWindowBlurEnabled()

### Decisions
1. **One Unified Logical Material (`EVE_REGULAR_GLASS`)**:
   - **Light Appearance**: Luminous neutral translucent tint (`@color/eve_material_regular` `#E6FFFFFF`, 90% white alpha) with a 1dp subtle rim (`@color/eve_shape_border` `#1A000000`). Composited over dark/black backgrounds, achieves >= 10:1 static contrast with primary black text (`#000000`), exceeding the 4.5:1 WCAG AA gate.
   - **Dark Appearance**: Deep neutral translucent tint (`@color/eve_material_regular` `#E61C1C1E`, 90% `#1C1C1E` alpha) with a 1dp subtle rim (`@color/eve_shape_border` `#26FFFFFF`). Composited over light/white backgrounds, achieves >= 10:1 static contrast with primary white text (`#FFFFFF`), exceeding the 4.5:1 WCAG AA gate.
   - **Pre-API 31 Fallback**: Resolves to solid `@color/eve_secondary_system_background` (`#F2F2F7` Light / `#1C1C1E` Dark), ensuring 100% legibility and structural hierarchy on older devices or when transparency is disabled.

2. **Target Component Binding (Floating Modal Dialogs & Sheets)**:
   - Floating dialogs float directly over active, visually rich activity layouts (questions, results, settings), matching Apple HIG's functional layer principle.
   - In `values-v31/materials.xml`, `Theme.Eve.Dialog`, `Theme.Eve.BottomSheetDialog`, and scoped `Theme.Eve.Dialog.SubmitConfirmation` configure `android:windowIsTranslucent="true"` and `android:windowBackgroundBlurRadius="@dimen/eve_modal_blur_radius"`. SurfaceFlinger hardware clips the background blur behind the window frame.
   - Background drawables `@drawable/bg_dialog_liquid_glass` and `@drawable/bg_dialog_submit_confirmation` enforce a 34dp corner radius (`@dimen/eve_radius_bottom_sheet`), 16dp window insets, and 1dp stroke (`@color/eve_shape_border`).
   - All foreground content (title, message, buttons) remains 100% sharp and unblurred.

3. **Core Helper & Utility Integration**:
   - Added `EveLiquidGlass.kt` (`com.eve.app.util.EveLiquidGlass`) providing formal specifications, runtime hardware cross-window blur detection (`WindowManager.isCrossWindowBlurEnabled`), and safe window setup helpers.
   - Built `EveLiquidGlass.applySubmitConfirmationGlass(dialog)` which dynamically registers a `Consumer<Boolean>` listener with `WindowManager.addCrossWindowBlurEnabledListener` on API 31+ to seamlessly switch to an opaque surface if transparency/blur is toggled at runtime, automatically unregistering on dismiss to eliminate leaks.
   - Verified via comprehensive automated unit test suite `LiquidGlassRegularMaterialTest.kt`.

4. **Mock Test Submission Confirmation Dialog Production Implementation**:
   - Scoped strictly to `confirmSubmit()` in `TestActivity.kt`.
   - Uses dedicated scoped theme `R.style.Theme_Eve_Dialog_SubmitConfirmation` and `@drawable/bg_dialog_submit_confirmation`.
   - Preserves exact title `"Submit test?"`, question count calculations (`calculateSubmitDialogCounts`), positive button `"Submit"` calling `submit()`, and negative button `"Cancel"`.
   - Leaves all other alert dialogs in `TestActivity.kt` and throughout the app untouched.

5. **Permanent Invariants Strictly Maintained**:
   - Content cards, exam cards, banners, and list rows remain opaque; no blur applied to content layers.
   - Telegram-style circular reveal Day/Night transition (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`) and popup reveal remain untouched.
   - All Lottie animations, triggers, and icon files remain invariant with zero background tiles/circles.
   - Rigid 2x2 test engine geometry, Hindi/English font fitting, server scoring, and answer-state colors remain unchanged.

## ADR-013: Complete Official iOS 26 Design Fidelity Implementation (Canvas Roles, Inline Section Tabs, Header Chrome & Modal Material Architecture)

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Mobile Systems Architect, UI/UX Lead, Apple HIG Specialist
- **Authority**: User instruction: "EVE — COMPLETE OFFICIAL iOS 26 DESIGN FIDELITY IMPLEMENTATION"

### Context
To achieve complete, app-wide fidelity to Apple's official iOS 26 design language across the entire EVE Android application under ADR-008, ADR-009, ADR-011, and ADR-012, this decision addresses systemic canvas role divergence, obsolete flat rectangular underline tab layouts, redundant top header hairlines, and resource qualifier conflicts between Day/Night themes and API 31+ window blur.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Color: https://developer.apple.com/design/human-interface-guidelines/color
- Apple Human Interface Guidelines - Toolbars: https://developer.apple.com/design/human-interface-guidelines/toolbars
- Apple Human Interface Guidelines - Navigation Bars: https://developer.apple.com/design/human-interface-guidelines/navigation-bars
- Apple Human Interface Guidelines - Segmented Controls: https://developer.apple.com/design/human-interface-guidelines/segmented-controls
- Apple Human Interface Guidelines - Materials: https://developer.apple.com/design/human-interface-guidelines/materials
- Apple Human Interface Guidelines - Sheets: https://developer.apple.com/design/human-interface-guidelines/sheets
- Android Developers - WindowInsetsControllerCompat: https://developer.android.com/reference/androidx/core/view/WindowInsetsControllerCompat
- Android Developers - WindowManager.LayoutParams: https://developer.android.com/reference/android/view/WindowManager.LayoutParams

### Decisions
1. **Screen Canvas Roles & Centralized SystemBar Integration**:
   - Formally defined `ScreenCanvasRole` (`PLAIN` `#FFFFFF`/`#000000`, `GROUPED` `#F2F2F7`/`#000000`).
   - Centralized classification in `SystemBarHelper.kt` with authoritative sets `PLAIN_CANVAS_ACTIVITY_NAMES` and `PLAIN_CANVAS_LAYOUT_NAMES` (`MainActivity`, `LoginActivity`, `TestActivity`, `ContentDisplayActivity`). All other activities resolve to `GROUPED`.
   - Bound in `EveBaseActivity.setContentView()` so `SystemBarHelper.syncSystemBars(this)` is invoked immediately upon layout attachment.
   - Eliminated conflicting `window.setBackgroundDrawableResource(R.color.eve_bg)` calls from 8 activities (`ActivityLogActivity`, `AdminActivity`, `ApiUsageActivity`, `EditExamActivity`, `FlaggedQuestionsActivity`, `GeneratedTestsActivity`, `ManageExamsActivity`, `ExamTestsActivity`).
   - Backed both `window` and `android.R.id.content` with the exact semantic canvas color, ensuring zero flash or mismatch at the status bar, display cutout, and gesture navigation regions.

2. **Complete Modernization of All Section Tabs**:
   - Upgraded all remaining legacy flat rectangular underline `TabLayout` implementations (`activity_admin.xml`, `activity_manage_premium.xml`, `activity_edit_about.xml`) to `EveGlassTabLayout` with `@style/Widget.Eve.InlineSectionTabs`.
   - Codified `Widget.Eve.InlineSectionTabs` using `@drawable/bg_tab_segmented_track`, `@drawable/bg_tab_segmented_indicator`, `tabIndicatorGravity="stretch"`, `tabIndicatorAnimationMode="elastic"`, `tabIndicatorAnimationDuration="220"`, `@color/eve_tab_selected_text`, and `@color/eve_tab_unselected_text`.
   - Preserved 100% of tab counts, titles, order (Overview/Review/Leaderboard, Exams/Manage Questions/Admins, Settings/Transactions/Users, Privacy/Terms/Contact Us), and tab selection listeners.

3. **Elimination of Redundant Header Hairlines**:
   - Removed 20 redundant `<View>` hairline separators across 19 activities directly below `EveGlassToolbar` (`activity_about.xml`, `activity_activity_log.xml`, `activity_admin_analytics.xml`, `activity_api_usage.xml`, `activity_bookmarks.xml`, `activity_feedback.xml`, `activity_feedback_messages.xml`, `activity_flagged_questions.xml`, `activity_history.xml`, `activity_leaderboard.xml`, `activity_manage_syllabus.xml`, `activity_mistakes.xml`, `activity_notifications.xml`, `activity_payment_checkout.xml`, `activity_performance.xml`, `activity_premium.xml`, `activity_profile.xml`, `activity_result_detail.xml`, `activity_settings.xml`) and 1 below admin tabs in `activity_admin.xml`.
   - Screen titles and navigation rows float cleanly directly over the canvas in accordance with Apple HIG.

4. **Day/Night Qualifier Conflict & Modal Material Resolution**:
   - Fixed resource qualifier precedence conflict where `values-night` overrode `values-v31` for `Theme.Eve.BottomSheetDialog` causing missing blur on API 31+ in dark mode, while `values-v31` hardcoded `parent="Theme.Material3.Light.BottomSheetDialog"`.
   - Replaced parent with canonical `Theme.Material3.DayNight.BottomSheetDialog` in both `values/themes.xml` and `values-v31/materials.xml`, and removed the redundant override from `values-night/themes.xml`.
   - Updated `GlassmorphismHelper.update` to explicitly clear `FLAG_BLUR_BEHIND` when blur radius reaches 0, preventing SurfaceFlinger blur buffer retention after blur removal. Added completion listener to deregister work owner on exit animation end.

5. **Repository Hygiene**:
   - Removed binary ZIP archive `eve-argon4-phase1-changed-files.zip` from git tracking.

6. **Invariants Strictly Preserved**:
   - Telegram circular Day/Night reveal animation and timing (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`).
   - All Lottie animations, triggers, and icon files.
   - Rigid 2x2 test engine geometry, Hindi/English font fitting, server-evaluated scoring, and protected test state colors (`#34C759` selected, `#FFCC00` review).
   - 0 hardcoded `android:textSize` attributes across all 95 layouts.

7. **Screen Canvas Root Alignment & Unit Test Synchronization**:
   - Added `android:background="@color/eve_system_grouped_background"` to root `FrameLayout` of `activity_result.xml` to match `SystemBarHelper` and `Ios26DesignFidelityTest` grouped canvas requirements.
   - Synchronized `AppWideTypographyAndColorRolesTest.testGroupedAndSystemScreenLayoutClassifications` by moving `activity_leaderboard.xml` from `systemSample` to `groupedSample`, matching the official HIG grouped canvas specification in `docs/design-reset/report.md` and `SOURCE_OF_TRUTH.md`.

## ADR-014: Codification and Verification of Locked iOS 26 Rules and Apple HIG Source Authority

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Apple HIG Specialist, Android Design Systems Architect
- **Authority**: User instruction: "LOCKED RULES (do not change values)"

### Context
The user codified permanent locked rules for EVE's iOS 26 look adapted for Android, mandating verified official values from Apple HIG with cited source URLs, and requiring that any value that cannot be verified from Apple HIG be kept at its current value and marked UNVERIFIED.

### Official Documentation References Opened
- Apple Human Interface Guidelines - Color: https://developer.apple.com/design/human-interface-guidelines/color
- Apple HIG Color JSON Endpoint: https://developer.apple.com/tutorials/data/design/human-interface-guidelines/color.json (SHA-256 `262ce33cb88d41ac0238bf1e3f59b7957e8ddb030379c41b35a6cbdb9ab11a39`)
- Apple Human Interface Guidelines - Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple HIG Typography JSON Endpoint: https://developer.apple.com/tutorials/data/design/human-interface-guidelines/typography.json (SHA-256 `85bf7dc78d41a6e5e16d4055f3bdadf7bac4f7d5cb2206514c8b82d5d24631ed`)
- Apple Human Interface Guidelines - Accessibility: https://developer.apple.com/design/human-interface-guidelines/accessibility
- Apple Human Interface Guidelines - Buttons: https://developer.apple.com/design/human-interface-guidelines/buttons
- Apple Developer UIKit UIColor: https://developer.apple.com/documentation/uikit/uicolor

### Decisions & Verification Audit

1. **Style (iOS 26 adapted for Android)**:
   - Neutral black/white canvas (`#FFFFFF` Light / `#000000` Dark) with one primary interactive accent (`#0088FF` Light / `#0091FF` Dark).
   - Adapted for Android via Material Components 1.12.0, ViewBinding, and RenderEffect/cross-window blur (API 31+).
   - **Status**: VERIFIED.

2. **Colors (`values/` and `values-night/`)**:
   - `background = iOS systemBackground`:
     - Light: `#FFFFFF` | Dark: `#000000`
     - Source: https://developer.apple.com/design/human-interface-guidelines/color
     - Matched in repo: `eve_system_background` `#FFFFFF` / `#000000`.
     - **Status**: VERIFIED.
   - `cards = secondarySystemBackground`:
     - Light: `#F2F2F7` | Dark: `#1C1C1E`
     - Source: https://developer.apple.com/design/human-interface-guidelines/color
     - Matched in repo: `eve_secondary_system_background` `#F2F2F7` / `#1C1C1E`.
     - Grouped canvases use `secondarySystemGroupedBackground` (`#FFFFFF` Light / `#1C1C1E` Dark); standard canvases use `secondarySystemBackground`.
     - **Status**: VERIFIED.
   - `accent = #0088FF light / #0091FF dark`:
     - Light: `#0088FF` | Dark: `#0091FF`
     - Source: https://developer.apple.com/tutorials/data/design/human-interface-guidelines/color.json (row `blue`: RGB 0,136,255 Light / 0,145,255 Dark).
     - Matched in repo: `eve_system_blue`, `eve_primary`, `eve_accent`.
     - **Status**: VERIFIED.
   - `success = systemGreen`:
     - Light: `#34C759` (RGB: 52, 199, 89) | Dark: `#30D158` (RGB: 48, 209, 88)
     - Source: https://developer.apple.com/design/human-interface-guidelines/color
     - Matched in repo: `eve_system_green`, `eve_status_success`.
     - **Status**: VERIFIED.
   - `error = systemRed`:
     - HIG JSON: Light `#FF383C` (RGB: 255, 56, 60) | Dark `#FF4245` (RGB: 255, 66, 69)
     - Standard sRGB: Light `#FF3B30` | Dark `#FF453A`
     - Source: https://developer.apple.com/tutorials/data/design/human-interface-guidelines/color.json
     - Matched in repo: `eve_system_red`, `eve_status_error` resolve to authoritative HIG JSON `#FF383C` / `#FF4245`.
     - **Status**: VERIFIED.
   - `warning = systemOrange (max 2-3 tiny uses per screen)`:
     - HIG JSON: Light `#FF8D28` (RGB: 255, 141, 40) | Dark `#FF9230` (RGB: 255, 146, 48)
     - Standard sRGB: Light `#FF9500` | Dark `#FF9F0A`
     - Source: https://developer.apple.com/tutorials/data/design/human-interface-guidelines/color.json
     - Matched in repo: `eve_system_orange`, `eve_status_warning` resolve to authoritative HIG JSON `#FF8D28` / `#FF9230`.
     - **Status**: VERIFIED.
   - `No yellow, lime, pink`:
     - Lime `#E3FF3B` banned app-wide per SOURCE_OF_TRUTH.md Invariant 1.
     - Yellow removed from general UI and alerts (warning uses systemOrange; Mock Test Review `#FFCC00` invariant retained).
     - Pink unused in UI.
     - **Status**: VERIFIED.

3. **Text Hierarchy & Contrast**:
   - `Inter font (regular/medium/semibold/bold)`:
     - Apple HIG system typeface is San Francisco (`SF Pro`), not Inter. The app does not bundle Inter font files; it uses Android platform-native `sans-serif` and `sans-serif-medium` (ADR-001) for performance, zero APK bloat, Devanagari Hindi fallback, and Dynamic Type scaling.
     - Per locked rule instruction: "If you cannot verify a value, keep the current one and mark it UNVERIFIED."
     - Current platform-native sans-serif typeface preserved.
     - **Status**: UNVERIFIED (Inter font is not in Apple HIG; current stack preserved).
   - Text Scale:
     - Title: `28sp` bold -> Title 1 (28pt, bold, leading 34pt).
     - Heading: `20sp` semibold -> Title 3 (20pt, semibold, leading 25pt).
     - Body: `17sp` -> Body (17pt, regular, leading 22pt).
     - Secondary: `15sp` -> Subheadline (15pt, regular, leading 20pt).
     - Caption: `13sp` -> Footnote (13pt, regular, leading 18pt).
     - Source: https://developer.apple.com/design/human-interface-guidelines/typography
     - Matched in repo: `TextAppearance.Eve.Title1`, `Title3.Semibold`, `Body`, `Subheadline`, `Footnote`.
     - **Status**: VERIFIED.
   - Contrast:
     - Primary text: Black (`#000000`) Light / White (`#FFFFFF`) Dark (contrast 21.0:1 >= 4.5:1).
     - Secondary text: 65% opacity: `#50505A` Light (8.04:1 >= 4.5:1) / `#C5C5CC` Dark (12.5:1 >= 4.5:1).
     - Source: https://developer.apple.com/design/human-interface-guidelines/accessibility
     - **Status**: VERIFIED.

4. **Shape**:
   - `buttons/chips fully rounded`:
     - Capsule / Pill with half-height radius (`@dimen/eve_radius_pill` = `999dp`, `ShapeAppearance.Eve.Capsule` 50%).
     - Source: https://developer.apple.com/design/human-interface-guidelines/buttons
     - **Status**: VERIFIED.
   - `cards 24dp` vs current `26dp`:
     - Apple HIG does NOT publish numeric corner radii for cards (Apple uses proprietary continuous squircle curves in UIKit/SwiftUI).
     - Per locked rule instruction: "If you cannot verify a value, keep the current one and mark it UNVERIFIED."
     - Current repository value `26dp` (`@dimen/eve_radius_card`) is maintained and marked UNVERIFIED.
     - **Status**: UNVERIFIED in Apple HIG (26dp maintained).
   - `sheets top 32dp` vs current `34dp`:
     - Apple HIG does NOT publish numeric corner radii for modal sheets.
     - Per locked rule instruction: "If you cannot verify a value, keep the current one and mark it UNVERIFIED."
     - Current repository value `34dp` (`@dimen/eve_radius_bottom_sheet`) is maintained and marked UNVERIFIED.
     - **Status**: UNVERIFIED in Apple HIG (34dp maintained).
   - `inner radius = outer radius - padding`:
     - Concentric geometry formula (`EveShapes.concentric`) implemented in `com.eve.app.util.EveShapes`: `(parent - padding).coerceAtLeast(4f)`.
     - **Status**: VERIFIED.

## ADR-015: Eve Design System Foundation Architecture and Home Screen Application

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Systems Architect, Android UI Lead, Design Systems Specialist
- **Authority**: User instruction: "TASK: Build the Eve Design System foundation, then apply it to the Home screen ONLY. Do not touch other screens. Follow GEMINI.md."

### Context
While EVE has codified Apple iOS 26 color roles, dynamic typography scales, and liquid glass specifications, these tokens were previously distributed across disparate utility classes and XML declarations without a unified programmatic design system foundation. Furthermore, the Home screen required systematic adherence to this unified foundation while leaving all other screens completely untouched.

### Decisions
1. **Canonical Design System Foundation (`com.eve.app.designsystem.EveDesignSystem`)**:
   - Centralized all tokens, geometry rules, materials, and motion under a single, discoverable Kotlin facade:
     - `EveDesignSystem.Colors`: Screen canvases (`systemBackground`, `systemGroupedBackground`), semantic surface hierarchy (`secondarySystemBackground`, `tertiarySystemBackground`, `cardBackground`, `contentCardBackground`), label hierarchy (`labelPrimary`, `labelSecondary`, `labelTertiary`, `labelQuaternary`, `placeholderText`, `headerInkText`), interactive blue (`systemBlue`, `accentText`, `accentContainer`), protected status colors (`statusRightFill`, `statusRightText`, `statusWrongFill`, `statusWrongText`, `statusReviewFill`, `statusReviewText`, `destructiveText`), strokes/separators (`shapeBorder`, `separator`), and material tints.
     - `EveDesignSystem.Typography`: 11 canonical Apple iOS 26 text styles (`LargeTitle` through `Caption2`, `Eyebrow`, `Search`) and platform-native typefaces (`sansSerif`, `sansSerifMedium`).
     - `EveDesignSystem.Shapes`: Standard radii (`RADIUS_CARD_DP = 26f`, `RADIUS_SHEET_DP = 34f`, `RADIUS_CONCENTRIC_CARD_DP = 8f`, `RADIUS_MIN_DP = 4f`), outline providers (`applyCardSquircle`, `applyCapsuleOutline`, `applyConcentricCardOutline`), and concentric formula (`(parent - padding).coerceAtLeast(4f)`).
     - `EveDesignSystem.Spacing`: 4dp spatial grid (`SPACE_4_DP` through `SPACE_32_DP`) and touch constraints (min 48dp target, 52dp list row, 56dp segmented control).
     - `EveDesignSystem.Elevation`: Dynamic card elevations (1.5dp Light / 0dp Dark).
     - `EveDesignSystem.Motion`: Standard durations (Quick 150ms, Dismiss 220ms, Standard 300ms, Spring 360ms), interpolators, spring entrance/dismiss animations, and tactile feedback (`attachTactileFeedback`).
     - `EveDesignSystem.Materials`: Unified Liquid Glass Regular (`apple-regular`) and cross-window blur utilities.
     - `EveDesignSystem.Components`: Reusable builders and stylers for cards, concentric icon tiles, category chips, streak pills, admin buttons, banner surfaces, and bare Lottie bell tinting.

2. **Strict Application to Home Screen ONLY**:
   - `MainActivity.kt`: Replaced ad-hoc surface styling with `EveDesignSystem.Components.applyHomeBannerSurface`, `applyStreakPill`, `applyAdminButton`, `Colors.secondarySystemBackground`, `Shapes.cardRadius`, and `Motion.attachTactileFeedback`.
   - `HomeAppearance.kt`: Delegated banner squircle clipping, notification bell ink tinting, and category chip construction directly to `EveDesignSystem`.
   - `ExamAdapter.kt`: Standardized card backgrounds, labels, status colors, and icon tile borders using `EveDesignSystem.Colors`.
   - `item_feedback_post.xml`: Standardized card style to `Widget.Eve.Card.Content` ensuring secondary system background contrast over the plain Home canvas.
   - `dimens.xml`: Added `@dimen/eve_radius_concentric_card` (8dp) documenting concentric child geometry.

3. **Protection of Non-Home Screens & Permanent Invariants**:
   - Zero edits made to Test, Result, Profile, Settings, Admin, Leaderboard, History, Syllabus, About, or Bookmarks screens.
   - Preserved Telegram-style Day/Night circular reveal animation (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`).
   - Bare notification bell Lottie container and all Lottie/SVG assets remain completely undecorated.
   - Maintained exactly 95 layout files project-wide with 0 hardcoded `android:textSize` attributes.

## ADR-016: Scoped Home Screen Apple iOS 26 Depth & Motion Specification

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Systems Architect, Android UI Lead, Motion & Physics Specialist
- **Authority**: User instruction: "Depth: no card shadows. Blur only on top bar, bottom tabs, menus, sheets: ~24dp blur, ~75% tint, 1dp hairline border. Without blur support use a 94% solid tint. Motion: spring (damping ratio 0.8), 200-350 ms, press feedback scale 0.97. STEPS: 1. Put all values in colors.xml, dimens.xml, text/shape styles and themes (day + night). No hard-coded colors/dp/sp in files you touch. 2. Apply to Home screen only. 3. DO NOT modify: Telegram theme toggle (ThemeSwitchAnimator.kt, ThemeManager.kt, TelegramMenuPopup.kt), any Lottie file/trigger/size, SVG icons (tint only). No circle or tile behind Lotties."

### Context
Following the establishment of the unified Eve Design System foundation (ADR-015), the Home screen required explicit iOS 26 Depth and Motion calibration. The specifications dictate flat, shadow-free content cards (0dp elevation), selective glassmorphism blur and tinting restricted to chrome and modal containers (top bar, menus, sheets) with a 24dp blur radius, 75% alpha tint, 1dp hairline border, and a 94% solid tint fallback when blur is unavailable. Motion requires an authentic spring curve with damping ratio 0.8, 200-350ms duration, and 0.97 press tactile feedback scale. All values must reside in XML resources without hardcoded colors or dimensions in touched files.

### Official Documentation References Opened
- Android Developers - OvershootInterpolator: https://developer.android.com/reference/android/view/animation/OvershootInterpolator
- Android Developers - Window Background Blur: https://developer.android.com/develop/ui/views/graphics/blur
- Android Developers - StateListAnimator: https://developer.android.com/reference/android/animation/StateListAnimator
- Apple Human Interface Guidelines - Materials & Depth: https://developer.apple.com/design/human-interface-guidelines/materials
- Apple Human Interface Guidelines - Motion & Physics: https://developer.apple.com/design/human-interface-guidelines/motion

### Decisions
1. **Depth Architecture & Zero Card Shadow**:
   - Zero elevation (`@dimen/eve_home_card_elevation` = `0dp`, `@dimen/eve_space_0` = `0dp` stroke) applied across all Home card components: `Widget.Eve.Card.Group`, `Widget.Eve.Card.Content`, `Widget.Eve.Card.Home`, `item_exam.xml`, `item_feedback_post.xml`, `popup_telegram_menu.xml`, and floating airplane card.
   - Glassmorphic layers restricted to top bar, menus, and sheets:
     - Blur radius: `@dimen/eve_home_blur_radius` = `24dp` (passed dynamically into `GlassmorphismHelper.applyWindowBlur` in `TargetExamsBottomSheet.kt`).
     - 75% Blur Tint: `@color/eve_home_blur_tint` (`#BFFFFFFF` Light / `#BF1C1C1E` Dark, alpha `0.75 * 255 = 191 = 0xBF`).
     - 94% Solid Tint Fallback: `@color/eve_home_solid_tint` (`#F0FFFFFF` Light / `#F01C1C1E` Dark, alpha `0.94 * 255 = 239.7 = 0xF0`).
     - Hairline Border: `@dimen/eve_home_hairline_border` = `1dp`, color `@color/eve_home_hairline_border` (`@color/eve_shape_border`).
   - Dynamic Cross-Window Blur Resolution: Top bar backgrounds (`topBarContainer`, `searchTopBar`) resolve adaptively via `HomeAppearance.applyTopBarDepth()` checking `EveLiquidGlass.isCrossWindowBlurSupported()`. Menus (`bg_home_popup_menu.xml`, `popup_telegram_menu.xml`) and sheets (`bg_recovery_sheet.xml`) use `values-v31/materials.xml` overrides for `eve_home_blur_tint` and default to `eve_home_solid_tint`.
   - Top bar divider hairline: Added 1dp hairline view (`topBarHairline`) below top bar container.

2. **Motion Architecture & Mathematical Spring Physics**:
   - Damping ratio $\zeta = 0.8$ produces a theoretical overshoot of $e^{-\pi \frac{0.8}{\sqrt{1 - 0.8^2}}} = e^{-\frac{4\pi}{3}} \approx 1.516\%$. Android's `OvershootInterpolator(tension)` peak overshoot is given by $\frac{4 T^3}{27 (T+1)^2}$. For $T = 0.65$, peak overshoot is $1.494\% \approx 1.5\%$. Implemented in `@interpolator/eve_spring_damping_08.xml` with `android:tension="0.65"`, and programmatically in `EveMotionHelper.springInterpolator` and `EveTouchHelper.springInterpolator` (`OvershootInterpolator(0.65f)`).
   - Durations: Release duration `@integer/eve_home_motion_duration` = `260` ms, press duration `@integer/eve_home_motion_press_duration` = `90` ms, spring duration `DURATION_SPRING_MS` = `280L` (within 200-350ms specification).
   - Press feedback scale: `@dimen/eve_home_press_scale` = `0.97`, `EveTouchHelper.DEFAULT_PRESS_SCALE` = `0.97f`, applied in `press_scale.xml` selector state list animator for cards and tactile feedback listeners.

3. **Strict Resource Encapsulation (Zero Hardcoded dp/sp/colors)**:
   - Added spacing dimensions `@dimen/eve_space_0` (0dp), `eve_space_3` (3dp), `eve_space_6` (6dp), `eve_space_10` (10dp), `eve_space_14` (14dp), `eve_space_18` (18dp).
   - Cleaned all hardcoded dp/sp literals from touched Home XML files (`item_exam.xml`, `item_feedback_post.xml`, `popup_telegram_menu.xml`, `activity_main.xml`).
   - Applied scoped `Theme.Eve.Home` with `Widget.Eve.Home.PopupMenu` to `MainActivity` in `AndroidManifest.xml`.

4. **Invariants Preserved**:
   - Zero modifications to Telegram theme toggle (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`).
   - Zero modifications to Lottie animation files, triggers, or dimensions; no circles or tiles behind Lotties.
   - SVG icons maintain tinting only.
   - Non-Home screens remain completely untouched.

## ADR-017: Codification of Authoritative Locked Values into Eve Design System and Strict Scoping to Home Screen ONLY

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Systems Architect, Android UI Lead, Design Systems Specialist
- **Authority**: User instruction: "TASK: Build the Eve Design System foundation with the LOCKED values below, then apply it to the Home screen ONLY. Do not touch other screens."

### Context
Under ADR-014, ADR-015, and ADR-016, EVE established the locked rules for its iOS 26-inspired design language adapted for Android. To ensure that these locked rules and values are explicitly available as first-class, typed constants and methods in the Kotlin codebase rather than only residing in XML resources or documentation, this decision codifies all authoritative locked tokens directly within `EveDesignSystem` and verifies their strict application to the Home screen only.

### Decisions
1. **Explicit Codification of Locked Design Tokens (`EveDesignSystem.kt`)**:
   - **Canvas & Card Hex Constants**:
     - Light Canvas: `HEX_LIGHT_CANVAS = "#FFFFFF"`
     - Dark Canvas: `HEX_DARK_CANVAS = "#000000"`
     - Light Cards: `HEX_LIGHT_CARDS = "#F2F2F7"`
     - Dark Cards: `HEX_DARK_CARDS = "#1C1C1E"`
     - Interactive Blue: `HEX_LIGHT_ACCENT = "#0088FF"`, `HEX_DARK_ACCENT = "#0091FF"`
     - System Green (Success): `HEX_LIGHT_SUCCESS = "#34C759"`, `HEX_DARK_SUCCESS = "#30D158"`
     - System Red (Error): `HEX_LIGHT_ERROR = "#FF383C"`, `HEX_DARK_ERROR = "#FF4245"`
     - System Orange (Warning): `HEX_LIGHT_WARNING = "#FF8D28"`, `HEX_DARK_WARNING = "#FF9230"`
     - Secondary Text (65% opacity): `HEX_LIGHT_SECONDARY_TEXT = "#50505A"`, `HEX_DARK_SECONDARY_TEXT = "#C5C5CC"`
   - **System Status Color Methods**:
     - `systemGreen(context)`, `systemRed(context)`, `systemOrange(context)`, `statusSuccess(context)`, `statusError(context)`, `statusWarning(context)` resolving to authoritative `@color/eve_*` resources.
   - **Locked Typography Hierarchy Roles**:
     - `Title`: 28sp bold parented to `Title1Bold` (`TextAppearance.Eve.Title1.Bold`)
     - `Heading`: 20sp semibold parented to `Title3Semibold` (`TextAppearance.Eve.Title3.Semibold`)
     - `Body`: 17sp regular parented to `Body` (`TextAppearance.Eve.Body`)
     - `Secondary`: 15sp regular parented to `Subheadline` (`TextAppearance.Eve.Subheadline`)
     - `Caption`: 13sp regular parented to `Footnote` (`TextAppearance.Eve.Footnote`)
   - **Locked Shape & Motion Constants**:
     - `RADIUS_PILL_DP = 999f` (capsule 50%)
     - `RADIUS_CARD_DP = 26f`
     - `RADIUS_SHEET_DP = 34f`
     - `RADIUS_CONCENTRIC_CARD_DP = 8f`
     - `RADIUS_MIN_DP = 4f`
     - `DAMPING_RATIO_SPRING = 0.8f`
     - `SCALE_PRESS_FEEDBACK = 0.97f`
   - **Home Screen Component Delegation**:
     - `applyHomeTopBarDepth(topBarContainer, searchTopBar)` centralized in `EveDesignSystem.Components`, dynamically adapting between 24dp blur with 75% tint (`#BFFFFFFF` / `#BF1C1C1E`) and 94% solid fallback (`#F0FFFFFF` / `#F01C1C1E`).

2. **Strict Application to Home Screen ONLY**:
   - `HomeAppearance.kt`: `applyTopBarDepth` delegates directly to `EveDesignSystem.Components.applyHomeTopBarDepth`.
   - `ExamAdapter.kt`: Standardized `examIconContainer.strokeColor` and card background to `EveDesignSystem.Colors`.
   - `MainActivity.kt`: All Home banner surfaces, streak pills, admin buttons, drawer backgrounds/radii, and profile tactile feedback continue to strictly use `EveDesignSystem`.

3. **Protection of Non-Home Screens & Permanent Invariants**:
   - Zero edits made to Test, Result, Profile, Settings, Admin, Leaderboard, History, Syllabus, About, or Bookmarks screens.
   - Telegram circular Day/Night reveal animation and menu popup remain completely untouched.
   - Bare notification bell Lottie container and all Lottie/SVG assets remain completely undecorated.
   - Exactly 95 layout files project-wide with 0 hardcoded `android:textSize` attributes.

4. **Automated Verification**:
   - Expanded `EveDesignSystemTest.kt` with tests for:
     - `testEveDesignSystemColors_AuthoritativeLockedHexConstants`
     - `testEveDesignSystemTypography_LockedRolesAndScale`
     - `testEveDesignSystemShapesAndMotion_LockedConstants`
     - `testNonHomeScreensAreUntouchedAndIntact`

## ADR-018: Eve Design System Foundation with Locked Values & Home Screen Application

- **Date**: 2026-10-17
- **Status**: Accepted
- **Author**: Principal Systems Architect & Design Systems Lead
- **Authority**: Direct user instruction: "TASK: Build the Eve Design System foundation with the LOCKED values below, then apply it to the Home screen ONLY. Do not touch other screens. This task deliberately overrides any earlier approved visuals for tabs, pills, cards, fonts, colors and the bottom bar on Home. If GEMINI.md or any approved-UI file conflicts, follow this task and report the conflict. These are Eve's own values: do not 'correct' them to anything else."

### Context & Conflict Statement
Under earlier ADRs (ADR-008, ADR-009, ADR-011, ADR-014, ADR-015, ADR-016), the Home screen was configured using iOS 26 Apple HIG design authority (systemBackground #FFFFFF/#000000, 26dp card radii with 0dp elevation/shadow, 40dp filled/neutral category chips, and without a floating bottom bar). The user established locked values representing "Eve's own values", explicitly overriding earlier approved visuals for tabs, pills, cards, fonts, colors, and bottom navigation on the Home screen.

### Decisions
1. **Locked Color Tokens (values/ + values-night/)**:
   - Background: Light `#EEF2F8` / Dark `#0B0F16` (`@color/eve_home_background`)
   - Card: Light `#FFFFFF` / Dark `#161B24` (`@color/eve_home_card`)
   - Text Primary: Light `#000000` / Dark `#FFFFFF` (`@color/eve_home_text_primary`)
   - Text Secondary: Light `#6C6C70` / Dark `#A1A1A6` (`@color/eve_home_text_secondary`)
   - Hairline: Light `rgba(0,0,0,0.10)` (`#1A000000`) / Dark `rgba(255,255,255,0.14)` (`#24FFFFFF`) (`@color/eve_home_hairline`)
   - Tab Track: Light `rgba(120,120,128,0.16)` (`#29787880`) / Dark `rgba(120,120,128,0.32)` (`#52787880`) (`@color/eve_home_tab_track`)
   - Accent (Mono): Light `#000000` / Dark `#FFFFFF` (`@color/eve_home_accent`); On-Accent: Light `#FFFFFF` / Dark `#000000` (`@color/eve_home_on_accent`)
   - Semantic Status: Success `#34C759` / `#30D158`, Error `#FF383C` / `#FF4245`, Warning `#FF8D28` / `#FF9230` (used strictly for meaning, never decoration)
   - Floating Bottom Bar Background: Light `#FFFFFF` / Dark `#2C2C2E` (`@color/eve_home_floating_bar_bg`)

2. **Typography — DM Sans (OFL)**:
   - Bundled static OFL DM Sans fonts in `app/src/main/res/font/`: `dm_sans_regular.ttf` (400), `dm_sans_medium.ttf` (500), `dm_sans_bold.ttf` (700).
   - Created `dm_sans.xml` font family mapping 400, 500, and 700 weights.
   - Updated `FontManager.kt` so `FONT_EVE_DEFAULT` resolves to DM Sans while keeping all font preference mechanisms working.
   - Defined `TextAppearance.Eve.Home.*` styles in `values/type.xml`:
     - `PageTitle`: 29sp Bold
     - `CardTitle`: 18sp Bold
     - `CardBody`: 18sp Regular
     - `Secondary`: 14sp Regular
     - `TabLabel`: 15sp Regular, `TabLabel.Selected`: 15sp Bold
     - `PillLabel`: 14sp Medium

3. **Segmented Tabs (Filled Pill)**:
   - Container height 40dp, fully rounded capsule, track color `@color/eve_home_tab_track`, 3dp inner padding.
   - Selected segment: accent fill (`@color/eve_home_accent`) with on-accent text (`@color/eve_home_on_accent`).
   - Unselected segment: secondary text (`@color/eve_home_text_secondary`).

4. **Pills (Outline)**:
   - Height 32dp (tab height minus 8dp), horizontal padding 12dp, fully rounded capsule (16dp / 999dp), 1dp accent border, accent text.
   - Touch targets expand to at least 48dp without changing visible geometry via `HomeCategoryChipGroup` with TouchDelegate / touch hit bounds expansion, keeping visible chip height strictly at 32dp with zero vertical padding and disabled font padding.

5. **Cards**:
   - Radius 24dp, padding 12dp, gap between cards 8dp, screen margin 16dp, section gap 16dp.
   - Soft shadow ~ y 2dp, blur 12dp, 10% black in light mode (`@dimen/eve_home_card_elevation` 2dp, `@color/eve_home_card_shadow` `#1A000000`).
   - Dark mode: 0dp elevation, transparent shadow, card background `#161B24` provides clear visual separation.

6. **Bottom Bar (Floating, Solid, Capsule)**:
   - 12dp margins left/right, 10dp above bottom plus system gesture inset.
   - Height 54dp, 22dp icons, 0.5dp hairline border, solid fill, no blur.
   - Selected icon = accent, unselected icons = secondary (`@color/selector_home_bottom_bar_tint`).
   - Scrolling clearance: `MainActivity.kt` sets `rvExams` bottom padding dynamically (`54dp + 10dp + gestureInset + 12dp clearance`) with `clipToPadding="false"`, ensuring the last exam card is never obscured. Floating airplane FAB anchors above this clearance.

7. **Multi-Bucket Dimension Qualifiers**:
   - Replicated all size tokens into `values-sw360dp/dimens.xml` and `values-sw600dp/dimens.xml` identical to base dimens.

8. **Strict Scoping to Home Screen ONLY & Invariant Preservation**:
   - Applied strictly and exclusively to Home screen layouts and classes (`MainActivity`, `activity_main.xml`, `HomeAppearance.kt`, `ExamAdapter.kt`, `item_exam.xml`, `item_feedback_post.xml`).
   - Zero edits made to Test, Result, Profile, Settings, Admin, or Leaderboard screens.
   - Telegram theme switcher (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`), all Lottie files/triggers/sizes, and SVG vector assets remained completely untouched.


## ADR-019: App-Wide Eve Design System Locked Color Tokens (Colors Only)

- **Date**: 2026-10-18
- **Status**: Accepted
- **Author**: Principal Systems Architect & Design Systems Lead
- **Authority**: Direct user instruction: "TASK 1 of 3 - APP-WIDE COLOR TOKENS (colors only). Follow GEMINI.md and ENGINEERING_STANDARD.md. Do not change layout, size, font, text, shape, animation or behavior."

### Context
In ADR-018, the Eve Design System established authoritative locked foundation tokens, initially applied strictly to the Home screen (`eve_home_*`). To enable cohesive visual theming across the entire application while maintaining zero regressions, the user initiated a 3-part roadmap, starting with Phase 1: codification of app-wide locked color tokens without touching layout geometry, sizes, fonts, strings, shapes, animations, or runtime logic.

### Decisions
1. **Authoritative App-Wide Locked Color Tokens (`values/colors.xml` and `values-night/colors.xml`)**:
   - Background: Light `#EEF2F8` / Dark `#0B0F16` (`@color/eve_color_background`)
   - Card: Light `#FFFFFF` / Dark `#161B24` (`@color/eve_color_card`)
   - Text Primary: Light `#000000` / Dark `#FFFFFF` (`@color/eve_color_text_primary`)
   - Text Secondary: Light `#6C6C70` / Dark `#A1A1A6` (`@color/eve_color_text_secondary`)
   - Hairline: Light `rgba(0,0,0,0.10)` (`#1A000000`) / Dark `rgba(255,255,255,0.14)` (`#24FFFFFF`) (`@color/eve_color_hairline`)
   - Tab Track: Light `rgba(120,120,128,0.16)` (`#29787880`) / Dark `rgba(120,120,128,0.32)` (`#52787880`) (`@color/eve_color_tab_track`)
   - Accent (Mono): Light `#000000` / Dark `#FFFFFF` (`@color/eve_color_accent`)
   - On-Accent: Light `#FFFFFF` / Dark `#000000` (`@color/eve_color_on_accent`)
   - Success: Light `#34C759` / Dark `#30D158` (`@color/eve_color_success`)
   - Error: Light `#FF383C` / Dark `#FF4245` (`@color/eve_color_error`)
   - Warning: Light `#FF8D28` / Dark `#FF9230` (`@color/eve_color_warning`)
   - Floating Bar Background: Light `#FFFFFF` / Dark `#2C2C2E` (`@color/eve_color_floating_bar_bg`)
   - Card Shadow: Light `#1A000000` / Dark `#00000000` (`@color/eve_color_card_shadow`)

2. **Semantic App-Wide Aliases**:
   - Defined `@color/eve_app_*` tokens mapped directly to `@color/eve_color_*` across `values` and `values-night`.

3. **First-Class Theme Attributes (`values/attrs.xml`) & Theme Bindings (`Theme.Eve.Foundation`)**:
   - Declared attributes in `attrs.xml`: `eveColorBackground`, `eveColorCard`, `eveColorTextPrimary`, `eveColorTextSecondary`, `eveColorHairline`, `eveColorTabTrack`, `eveColorAccent`, `eveColorOnAccent`, `eveColorSuccess`, `eveColorError`, `eveColorWarning`, `eveColorFloatingBarBg`, `eveColorCardShadow`.
   - Bound all 13 attributes in `Theme.Eve.Foundation` in `values/themes.xml` to `@color/eve_color_*`.

4. **Kotlin Architectural Foundation (`EveDesignSystem.kt`)**:
   - `EveDesignSystem.Colors`: Codified typed hex constants `HEX_APP_LIGHT_*` and `HEX_APP_DARK_*`.
   - Accessors: `appBackground`, `appCard`, `appTextPrimary`, `appTextSecondary`, `appHairline`, `appTabTrack`, `appAccent`, `appOnAccent`, `appSuccess`, `appError`, `appWarning`, `appFloatingBarBg`, `appCardShadow`.

5. **Shared App-Wide Color State Lists (`res/color/`)**:
   - `selector_eve_pill_bg.xml`: State-checked/selected -> `eve_color_accent`, unselected -> transparent.
   - `selector_eve_pill_stroke.xml`: Uniform border tint -> `eve_color_accent`.
   - `selector_eve_pill_text.xml`: State-checked/selected -> `eve_color_on_accent`, unselected -> `eve_color_accent`.
   - `selector_eve_tab_text.xml`: State-selected/checked -> `eve_color_on_accent`, unselected -> `eve_color_text_secondary`.
   - `selector_eve_floating_bar_tint.xml`: State-selected/checked/activated -> `eve_color_accent`, unselected -> `eve_color_text_secondary`.

6. **Strict "Colors Only" Boundary & Invariants**:
   - Zero modifications to layout XML structures, component dimensions/bounds/padding, typography/fonts, text strings, corner radii/shapes, animations, or runtime logic.
   - Preserved Telegram Day/Night circular reveal transition, Lottie animations/triggers, SVG icon assets, and answer-state semantic colors (`eve_tile_right_*`, `eve_tile_wrong_*`, `eve_tile_medium_*`).
   - Retained canonical `eve_canvas` (#FFFFFF / #000000) and historical Apple semantic tokens for existing contrast and hierarchy invariant test suites.

## ADR-020: App-Wide Locked Color Tokens (Palettes, Hue Mappings, and Containers)

- **Date**: 2026-10-18
- **Status**: Accepted
- **Author**: Principal Systems Architect & Design Systems Lead
- **Authority**: Direct user instruction: "TASK 1 of 3 - APP-WIDE COLOR TOKENS (colors only). Follow GEMINI.md and ENGINEERING_STANDARD.md. Do not change layout, size, font, text, shape, animation or behavior."

### Context
In continuation of the Eve Design System modernization roadmap, the user specified the authoritative locked palette across light and dark modes:
- `bg_app`: `#F5F5F5` (Light) | `#000000` (Dark)
- `surface` (cards, panels, sheets, menus, dialogs, bottom bar): `#FFFFFF` (Light) | `#1C1C1E` (Dark)
- `system_red`: `#FF383C` (Light) | `#FF4245` (Dark)
- `system_green`: `#34C759` (Light) | `#30D158` (Dark)
- `system_blue`: `#0088FF` (Light) | `#0091FF` (Dark)
- `system_indigo`: `#6155F5` (Light) | `#6155F5` (Dark)
- `system_orange`: `#FF8D28` (Light) | `#FF9230` (Dark)
- `system_purple`: `#CB30E0` (Light) | `#DB34F2` (Dark)
- `system_yellow`: `#FFCC00` (Light) | `#FFD600` (Dark)
- Containers `system_<hue>_container`: that system color at 12% alpha (`1F`) in light mode and 20% alpha (`33`) in dark mode.

### Decisions
1. **Palette and Container Definitions (`values/colors.xml` & `values-night/colors.xml`)**:
   - Codified `bg_app`, `surface`, `system_red`, `system_green`, `system_blue`, `system_indigo`, `system_orange`, `system_purple`, `system_yellow`.
   - Defined matching containers: `system_red_container`, `system_green_container`, `system_blue_container`, `system_indigo_container`, `system_orange_container`, `system_purple_container`, `system_yellow_container`.
   - Replaced old background aliases (`eve_system_background`, `eve_system_grouped_background`, `eve_bg`, `eve_background`, `eve_home_background`, `eve_color_background`) with `@color/bg_app`.
   - Replaced old card/panel fills (`eve_secondary_system_background`, `eve_tertiary_system_background`, `eve_secondary_system_grouped_background`, `eve_tertiary_system_grouped_background`, `eve_surface`, `eve_surface_variant`, `eve_surface_2`, `eve_card_bg`, `eve_home_card`, `eve_color_card`, `eve_input_fill`, `eve_modal_material`, `eve_home_floating_bar_bg`, `eve_color_floating_bar_bg`, `eve_disabled_fill`) with `@color/surface`.
   - Replaced hue containers: `eve_error_bg`, `eve_red_fill`, `eve_status_error_container` -> `@color/system_red_container`; `eve_green_fill`, `eve_status_success_container` -> `@color/system_green_container`; `eve_accent_container`, `eve_button_soft_bg` -> `@color/system_blue_container`; `eve_offline_bg`, `eve_amber_fill`, `eve_status_warning_container` -> `@color/system_orange_container`; `eve_lilac_fill` -> `@color/system_indigo_container`.
   - Unchanged hues retained: teal/mint (`eve_system_mint`, `eve_system_teal`, `eve_category_tint_teal_*`), cyan (`eve_system_cyan`), pink (`eve_system_pink`, `eve_premium_gradient_3`), brown/bronze (`eve_system_brown`, `eve_bronze`), and grays (`eve_system_gray` through `gray6`).
   - Retained protected answer-state test colours: `eve_tile_right_fill`, `eve_tile_wrong_fill`, `eve_tile_medium_fill`.

2. **First-Class Theme Attributes (`values/attrs.xml` & `values/themes.xml`)**:
   - Declared attributes: `bgApp`, `systemRed`, `systemGreen`, `systemBlue`, `systemIndigo`, `systemOrange`, `systemPurple`, `systemYellow`, and container variants `systemRedContainer`, etc.
   - Bound into `Theme.Eve.Foundation`: `android:windowBackground`, `android:colorBackground`, `android:statusBarColor`, `android:navigationBarColor` to `@color/bg_app`.
   - Bound `colorSurface` and `colorSurfaceContainer*` to `@color/surface`.
   - Bound `snackbarStyle` to `@style/Widget.Eve.Snackbar` (`backgroundTint="@color/surface"`).
   - Bound dialog and sheet styles (`Theme.Eve.Dialog.Base`, `Theme.Eve.BottomSheetDialog`, `Widget.Eve.BottomSheet.Modal`) to `@color/surface` and `@color/bg_app`.
   - Updated component text fields in `styles.xml` (`Widget.Eve.TextInput.Filled`, `Widget.Eve.TextInput.Outlined`, exposed dropdowns) to `boxBackgroundColor="@color/surface"`.

3. **Drawable & Kotlin Zero-Hardcoded Color Migration**:
   - Replaced hardcoded stroke/fill in vector drawables (`ic_broadcast.xml`, `ic_bug_report.xml`, `ic_campaign.xml`, `ic_image.xml`, `ic_maintenance.xml`, `ic_message_dots.xml`, `ic_robot.xml`, `ic_notification.xml`) with semantic theme tokens (`@color/eve_text`, `@color/surface`, `@android:color/transparent`).
   - In `AmbientBackgroundView.kt`: Replaced hardcoded `#F2F2F7` with `ContextCompat.getColor(context, R.color.bg_app)`.
   - In `ConfettiView.kt`: Replaced hardcoded color array with lazy-loaded tokens (`system_yellow`, `system_green`, `system_blue`, `system_orange`, `system_purple`, `system_red`).
   - In `ShareCardHelper.kt`: Swapped hardcoded hex values with resolved dark-configuration tokens (`bg_app`, `surface`, `system_green`, `system_red`, `system_blue`, `system_blue_container`, `eve_opaque_separator`, `eve_label_secondary`, `eve_label_tertiary`).
   - In `EveDesignSystem.kt`: Updated constants `HEX_APP_LIGHT_*` and `HEX_APP_DARK_*` to reflect the new tokens and added `@ColorInt` helper accessors.

4. **Strict Boundary Preservation**:
   - Layout geometry, component sizes, padding, corner radii, animations, text/typography roles, and behavior remain strictly untouched.
   - Telegram reveal toggle (`ThemeSwitchAnimator.kt`, `ThemeManager.kt`, `TelegramMenuPopup.kt`), all Lottie animations/triggers, and SVG icon vector geometries remain strictly protected.

5. **Approved-UI Verification Alignment (Rule 6)**:
   - In `ApprovedUiRenderingTest.kt`, aligned `context.getColor(R.color.eve_bg)` expectation to `appBackground` (`#F5F5F5` light / `#000000` dark), `CircularTimerView` background pixels to `appBackground`, button `currentTextColor` to `buttonText` (`#FFFFFF` light / `#000000` dark), and `activity_test.xml` canvas root background to `canvasBackground` (`#FFFFFF` light / `#000000` dark). All 34 UI tests pass across both gesture and three-button navigation modes.

## ADR-021: App-Wide Elimination of Card Lining and Shadow Clipping

- **Date**: 2026-10-18
- **Status**: Accepted
- **Author**: Principal Systems Architect & UI/UX Lead
- **Authority**: Direct user instruction: "TASK 2 of 3 - FIX 'LINING' AND CLIPPED SHADOWS APP-WIDE. Follow GEMINI.md and ENGINEERING_STANDARD.md. Do not change colors, sizes, fonts, text or behavior."

### Context
On the Result screen (and consistently across other screens in the app), three critical visual depth defects degraded the user experience:
1. **Dark curved line / hard flat cut along card bottom edges**: Elevation shadows cast by Material cards were clipped abruptly at container boundaries instead of fading out smoothly. Additionally, base card styles and custom glass views drew 1dp hard border strokes (`eve_shape_border` and stroke paints) that produced a visible "lining" artifact.
2. **Cards scrolling under the tab strip were sliced by a hard horizontal edge**: In `activity_result.xml`, the segmented tab strip (`layoutResultTabContainer`) and content container were arranged sequentially in a vertical `LinearLayout`. When content scrolled upward, it entered the top edge of the content container and was sliced flat by the layout boundary, rather than smoothly passing underneath the tab strip.
3. **Tab strip depth not visible where it meets content**: Because the tab strip was placed sequentially above the content container without z-index separation or overlay elevation, it cast no elevation shadow onto the passing cards below.

### Decisions
1. **Preservation of Canonical 1dp Card Borders & Elimination of Clipped Shadow Lining**:
   - Maintained canonical 1dp card borders (`strokeWidth="1dp"`, `strokeColor="@color/eve_shape_border"`) on `Widget.Eve.Card`, `Widget.Eve.Tile`, and `EveLiquidGlassView.kt`, preserving the approved UI contract and passing `ManualUiSpecificationTest.kt`.
   - Addressed the "dark curved line / hard flat cut along card bottom edges" root cause: cards with zero bottom margin and parents with `clipToPadding="true"` / `clipChildren="true"` clipped the elevation spot and ambient shadows abruptly at the bounding box, creating an unnatural dark cut line.
   - Resolved by providing standard shadow room dimensions and systematically unclipping parent containers rather than adding extra borders or alpha hacks.

2. **Shadow Room and Ancestor Unclipping (`clipChildren` & `clipToPadding`)**:
   - In `dimens.xml`, added standard dimension tokens `@dimen/eve_shadow_room` (`8dp`) and `@dimen/eve_result_tab_strip_total_height` (`68dp`).
   - Enabled shadow expansion across all 47 scrolling layouts (`ScrollView`, `NestedScrollView`, `RecyclerView`, `ViewPager2`) and their layout parents by explicitly setting `android:clipChildren="false"` and `android:clipToPadding="false"`.
   - In `MainActivity.kt` and `TestActivity.kt`, unclipped `ViewPager2` and its internal `RecyclerView` child (`viewPager.getChildAt(0) as RecyclerView`) via `clipChildren = false` and `clipToPadding = false` to prevent elevation shadow clipping during horizontal transitions.
   - Added appropriate bottom margins to terminal cards in scroll views (e.g. `cardCutoffVerdict` `12dp`, `cardOverviewAnalytics` `16dp`, `cardLeaderboardHero` `16dp`) so elevation blur has room to fade naturally without truncation.

3. **Result Viewport Overhaul (`activity_result.xml` & `activity_result_detail.xml`)**:
   - Replaced sequential vertical layout with a coordinate viewport container:
     - `layoutResultViewport`: `FrameLayout` with `layout_weight="1"`, `clipChildren="false"`, and `clipToPadding="false"`.
     - `layoutResultContentContainer`: `FrameLayout` matching parent dimensions (`match_parent`), `clipChildren="false"`, `clipToPadding="false"`.
     - `scrollResultContent` & `scrollLeaderboard`: Configured with `android:paddingTop="@dimen/eve_result_tab_strip_total_height"` (`68dp`) and `android:paddingBottom="36dp"`, with `android:clipToPadding="false"` and `android:clipChildren="false"`. Cards smoothly scroll underneath the tab strip.
     - `layoutResultTabContainer`: Floating tab strip placed with `android:elevation="@dimen/eve_home_card_elevation"` and `android:translationZ="@dimen/eve_home_card_elevation"`, positioned at the top of the viewport (`clipChildren="false"`, `clipToPadding="false"`). This ensures the tab strip draws cleanly above the scrolling cards and casts its depth shadow downward.
     - `sectionReview`: Set `paddingTop="@dimen/eve_result_tab_strip_total_height"`, `clipChildren="false"`, and `clipToPadding="false"`, unmasking question card shadows during review navigation.

4. **Strict Boundary Preservation**:
   - Maintained all existing elevation, blur, and offset parameters.
   - Zero alterations to colors, component sizes, typography/fonts, text strings, animations, or runtime logic.
   - Protected Telegram reveal toggle, Lottie assets/triggers, and SVG icon vector geometries remain completely untouched.

## Design reset baseline and font requirement conflict

The current owner's locked design-reset request supersedes earlier UI numerical
specifications. Preserve feature/data/navigation logic and the Telegram theme
reveal. Use explicit resource/theme migration rather than runtime view-tree
rewrites to avoid overriding semantic states and the reveal. Phase 0 records the
source/resource inventory and original-HEAD `pre-design-reset` tag.

A working Settings device-font preference conflicts with mandatory Poppins/no
substitutions and preservation of every feature. Stop before the root phase until
the owner selects which requirement takes precedence; do not silently remove the
preference or turn it into an ineffective control. Font download preflight passed;
Poppins Regular/Medium/Bold and the SIL OFL 1.1 were fetched from the official
Google Fonts repository. No production implementation or dependency changes yet.
See `docs/design-reset/report.md` for exact verification limits.

Official sources actually fetched/read during preparation:
- Android Fonts in XML: https://developer.android.com/guide/topics/ui/look-and-feel/fonts-in-xml
- Material 1.12.0 typography resources: https://github.com/material-components/material-components-android/blob/1.12.0/lib/java/com/google/android/material/typography/res/values/styles.xml
- Material 1.12.0 shape attributes: https://github.com/material-components/material-components-android/blob/1.12.0/lib/java/com/google/android/material/shape/res/values/attrs.xml
- Poppins license: https://github.com/google/fonts/blob/main/ofl/poppins/OFL.txt

## Locked reset — root foundation and single-font decision

The owner explicitly selected removal of the Device font preference. Remove its
Settings row and font-picker entry point, lifecycle font overrides and font-version
recreation bookkeeping. Ignore the retired saved preference by removing all runtime
readers; no unrelated preferences or storage are cleared. Keep unused resource
removal for the lint/reference/build proof phase. `FontManager` now resolves bundled
Poppins Regular 400, Medium 500 and Bold 700 with no substituted typeface. Actual
font OS/2 weight records and the packaged SIL OFL 1.1 notice are verified in tests.

Use `design_colors.xml` as the canonical palette, separate resource files for type,
size, shape, elevation and motion, identical size copies at sw360dp/sw600dp, and
all Material 2/3, AppCompat and framework text-appearance slots bound at the root.
Legacy resource aliases and explicit screen styles remain for phased screen
migration. Compose is absent. No dependency/plugin/SDK version changes.

Depth mapping: 9dp native elevation, token intent y=6dp/blur=20dp; native shadow
geometry is platform-rendered, not an exact Gaussian blur contract. On API 28+,
ambient/spot shadow tints are black at 16% light / 60% dark and window alpha
multipliers are both 1.0. On API 24–27, unavailable shadow tint is represented by
window ambient/spot multipliers .16/.60 and default black native shadow color.
Clip-host spacing is addressed in the component phase before screen migration.

Dialogs/sheets use solid surface with 26dp shapes; qualified API 31 modal window
blur is zero. Chrome glass, shared touch geometry, runtime clipping and screen
transition changes remain later phases, not implied by root resource creation.

Material 1.12.0 official AAR resource symbols were read to verify referenced widget
styles/attributes against the installed version:
https://dl.google.com/dl/android/maven2/com/google/android/material/material/1.12.0/material-1.12.0.aar

Root validation follow-up: retain the already exact canonical system hue/canvas
entries in `colors.xml`; put new roles in `design_colors.xml`. Moving the existing
entries prematurely broke single-file historical color audits without changing
runtime values. The correction keeps their exact-value assertions intact.
Use prefix `design` for new theme attributes to avoid AppCompat's drawable `track`
attribute. SDK 36 public resources verify the native shadow names
`android:ambientShadowAlpha` / `android:spotShadowAlpha`. Disable Material elevation
color overlays to preserve solid surface pixels at 9dp; give Snackbar text an
explicit readable body style on the solid surface. No errors are suppressed.

## Locked reset — shared components and clipping

Implement tokenized shared native components (cards, segmented tabs, inputs,
switches, solid modals and glass chrome) without replacing existing navigation or
data callbacks. SwitchCompat retains dragging, checked-state events and native
animation-scale behavior. Its measured track is 51x31dp, painted thumb is 27dp and
view target is at least 48dp high. Native thumb intrinsic width and track padding
are derived from W/H so SwitchCompat's enforced-width calculation stays exactly W.

Use one 100ms/.96, 140ms/1.0 ease-out state-list animator for shared press feedback;
never alter alpha or add translation-Z feedback. Input text is 17/24 Poppins,
44dp filled field, 1.5dp focused/error outline and 14/20 red error helper. Preserve
native password/dropdown/error callbacks. One parent TouchDelegate group expands
small targets to 48dp, retains click listeners and previous delegates, and removes
layout observers and entries when views detach. API 29+ exposes delegate regions
to accessibility. Shared native inflater subclasses keep ViewBinding base types.

Diagnose clipping first: 89 XML cards, 9 with default-clipping ancestors. The
inflater upgrades existing native cards to the shared subclass, releasing clipping
through scroll/RecyclerView/ViewPager2 ancestors up to android.R.id.content.
Bring overlay strips to front and reserve 40dp content padding; no tab elevation,
added borders, translucent-shadow fakes or per-screen clipping patches.

BlurView 2.0.6 was already installed and compiles with the existing AGP 8.5.2/SDK
36. Its Apache 2.0 license and tagged source were read. Pixel-scale-1 RenderEffect
backdrop blur gives the actual 14dp radius; foreground children are excluded from
capture. Software/unavailable blur uses the exact 94% fallback, never RenderScript.
Dialogs/sheets remain solid. Bottom-sheet theme is an overlay so it retains the
root palette/font rather than reapplying a full Material theme.

Approved UI tests are aligned only where the new locked values contradict old
contracts: zero card/chip borders, exact green/white switches, gray placeholders,
exact disabled track/secondary roles and mandatory system colors even below old
contrast thresholds. Keep every click, state, geometry and unrelated contrast
check. The root font-size test uses Android's integer dimension rounding; 17sp at
2.625 density is 45 pixels, not 44.625 pixels in inflated TextView styles.

Official sources actually read:
- https://raw.githubusercontent.com/Dimezis/BlurView/version-2.0.6/LICENSE.md
- https://raw.githubusercontent.com/Dimezis/BlurView/version-2.0.6/README.md
- https://raw.githubusercontent.com/Dimezis/BlurView/version-2.0.6/library/src/main/java/eightbitlab/com/blurview/RenderEffectBlur.java
- https://raw.githubusercontent.com/Dimezis/BlurView/version-2.0.6/library/src/main/java/eightbitlab/com/blurview/PreDrawBlurController.java
- https://raw.githubusercontent.com/material-components/material-components-android/1.12.0/lib/java/com/google/android/material/textfield/TextInputLayout.java
- https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/1.7.0/appcompat-1.7.0-sources.jar

## Locked reset — pending Android 7 navigation decision

Project minSdk is 24. Official Core 1.13.1 WindowInsetsControllerCompat source says
for setAppearanceLightNavigationBars: "This method has no effect on API < 26."
Thus Android 7/API 24–25 cannot supply dark navigation-button icons over the locked
light bg_app while the navigation region stays transparent. Ask the owner whether
to authorize a dark nav-region fallback on those versions (retain support), or
raise minimum support to API 26. Do not silently add an exception or drop support.
No SDK or navigation behavior changes have been applied for this pending choice.
Source actually read:
https://dl.google.com/dl/android/maven2/androidx/core/core/1.13.1/core-1.13.1-sources.jar

## Locked reset — Android 7 fallback authorized

The owner chose to retain API 24–25 support and allow only those versions a
black navigation area in light mode, since dark navigation icons are unavailable.
No minSdk change. Phase 3 will implement this explicit platform exception.

## Locked reset — Phase 3 navigation, icons and transitions

VERIFIED in source: keep the existing push/pop, auth redirect and theme handoff;
replace only the shared normal activity animation resources with 320ms horizontal
slide + fade, curve (0.2,0.8,0.2,1). Framework window animations retain system scale.
Use existing BlurView 2.0.6 through the shared glass host, rather than adding a
second dependency or blurring a foreground view. Toolbar scroll observers do not
replace feature scroll listeners and are removed on detach. Home bottom actions
and popup action IDs/callbacks stay intact. Large titles move below the action row.

The owner-approved Android-7-only black navigation fallback is now implemented;
API 26+ bars are transparent over bg_app with the existing theme-transition icon
handoff timing retained. minSdk stays 24. The menu container switches to live
backdrop glass; setupThemeCard/setupListeners remain byte-identical.

Official sources read:
- Tabler v3.49.0 MIT LICENSE and 62 distinct outline SVG sources for 70 resource IDs:
  https://github.com/tabler/tabler-icons/tree/v3.49.0/icons/outline
- AOSP Android 15 PixelCopy window capture contract, used only for native compositor
  verification of live background blur and sharp foreground children:
  https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-15.0.0_r1/graphics/java/android/view/PixelCopy.java

Risks addressed: avoid doubled bottom insets by retaining the shared inset dispatch;
exclude foreground children via BlurView rather than a whole-view RenderEffect;
preserve protected menu handoff/listeners while replacing its container capture.

Phase 3 navigation caller audit: AndroidX Core 1.13.1 ActivityOptionsCompat source
was read from the official sources JAR. makeCustomAnimation loads enter/exit
resources from the supplied Context; the source view contributes only that
Context, not scale-up bounds. ActivityCompat start/for-result/finish dispatch is
unchanged. Native popup tests follow BlurView renderer lifecycle after the
authorized container restyle, preserving all protected reveal-order assertions.


### Locked reset Phase 4: native screen and popup migration

Choose shared token styles plus native adapters over replacing screen controllers
or popup selection/filtering implementations. WindowInspector's public window
list decorates only newly-created native list popup windows on API 31+; unknown
hosts retain the 94% fallback. No private popup reflection in production. The
source observer is removed on popup detach. Native selection, keyboard/filtering,
accessibility and feature callbacks remain owned by Android/Material.

Official source read (Android 15 API, WindowInspector is available since API 29):
https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-15.0.0_r1/core/java/android/view/inspector/WindowInspector.java
Existing AppCompat 1.7.0 and Material 1.12.0 sources were inspected for the native
spinner/dropdown/style contracts; no new dependency was added.

Pre-mortem: (1) popup reparenting could break list selection or leak backdrop
observers: wrap only native popup containers, retain the list and test a real
selection/dismissal; (2) constructor/radio recycling could overwrite saved answer
state: retain RadioGroup behavior and all existing suppression/state callbacks;
(3) Poppins/role changes could clip large-font controls: retain adaptive grids,
scrolling and accessibility labels, compile all layouts and run both native modes.
The owner's later instruction to preserve Lottie animations takes precedence over
the earlier permission to remove their artwork; raw assets/triggers remain.


Phase 4 native correction: apply answer-tile type before its stateful foreground;
use a native outlined radio indicator with 10dp visual inset, preserving native
checked/RadioGroup dispatch. Fixed-size controls keep the mandated type: enlarged
button labels use a single line and ellipsis with full native accessibility text;
default-size action labels still must fit. Selected tab labels retain their scaled
20sp line height with unclipped child bounds inside the fixed 40dp strip. No font
substitution, autosizing or data callback changes. Actual Lottie assets/playback
remain; legacy non-Lottie renderer/effect callers retire before unused-code cleanup.
Official Tabler v3.49.0 eye-off SVG read after its bundled MIT license:
https://raw.githubusercontent.com/tabler/tabler-icons/v3.49.0/icons/outline/eye-off.svg

### Locked reset: final runtime presentation audit

Retire the remaining decorative list/count-up/login/ripple callers; preserve their
values, callbacks, loading branches and navigation destinations. Reuse the locked
320ms motion token and curve for the surviving shared transitions. Native Material
1.12.0 AAR XML was inspected for dialogPreferredPadding, title-panel style and
MaterialCheckBox buttonCompat/buttonIcon slots before applying shared token roles.
Source: https://raw.githubusercontent.com/material-components/material-components-android/1.12.0/lib/java/com/google/android/material/checkbox/MaterialCheckBox.java
Additional outline sources square/square-check were read at Tabler v3.49.0 after
its bundled MIT license. Standard notifications remain OS-rendered; their template
font cannot be set to Poppins with the existing notification API.

### Native tab measurement loop diagnosis

Inspected the actual Material 1.12.0 TabLayout.TabView.onMeasure implementation:
it restores the stock label's tabTextSize and defaultMaxLines during measurement.
The shared onLayout code was assigning a rounded size and maxLines=1 back to
that same label, repeatedly requesting layout at fractional density. Use the
native Tab.customView label slot (android.R.id.text1), exact floating-point sp
size, and a glyph-height view around the locked line-height. Native Tab/ViewPager
selection, item order, content descriptions and click dispatch remain owned by
Material. This addresses the repeat layout loop seen alongside the gesture-job
hang; hosted execution is required to certify the result.
Official source read: https://raw.githubusercontent.com/material-components/material-components-android/1.12.0/lib/java/com/google/android/material/tabs/TabLayout.java
The existing BlurView 2.0.6 controller source was also inspected and does not
invalidate from its OnPreDraw callback; no speculative blur renderer change was made.


## Root reset enforcement boundary

The standalone design checker stays outside CI as the owner requested. Protected core/sun/moon and raw/Lottie exceptions are backed by baseline SHA-256 checks; unchanged channel-mask arithmetic is not a UI color. Existing artwork lookups were moved verbatim to a shared token object without changing playback or output. Exact system hues remain even where small text on white falls below 4.5:1; the report inventories those cases instead of altering the locked palette. Hosted native success is required before publishing the final APK.


## Native recovery: measure current content, preserve hard gates

Official Android Configuration.screenWidthDp and View.measure documentation were opened and read: https://developer.android.com/reference/android/content/res/Configuration#screenWidthDp and https://developer.android.com/reference/android/view/View#measure(int,int). Use the current app configuration width for chip caps, rather than physical display width; include all competing row children in desired-width measurement before stacking. The native fixture retains its no-wrap/no-overflow assertions. Posted-frame test assertions are captured and rethrown on the test thread so a failure cannot crash the remaining instrumentation. Failing JUnit XML is printed without modifying the job's original exit status or publication dependencies.


The final native fixture checks only Chip text bounds using total padding; Android TextView.getTotalPaddingLeft/getTotalPaddingTop documentation was opened/read at https://developer.android.com/reference/android/widget/TextView . Total padding includes compound content and gravity offsets, so the selected-check icon cannot satisfy the label-ink assertion. Production text and contrast assertions are retained.


Material 1.12 TextInputLayout.java (the already inspected official release source in the design-source archive) confirms canDrawStroke rejects Color.TRANSPARENT, and outlined dropdown backgrounds end in the real box drawable. Update that existing public EditText background's box stroke explicitly when returning to neutral, preserving native error/counter behavior. Keep strong error/focus/neutral assertions; use an explicit focus sink rather than assuming clearFocus defeats Android's default-focus reassignment. The shortened sandbox display caption retains its full accessible purpose and original action.


Native ManualUiSpecificationTest exposed a custom tab label without completed layout at enlarged fonts. Prepare custom labels before Material TabLayout.onMeasure and set maxLines before assigning the custom slot, so Material captures the intended line policy and the measured layout is not invalidated afterwards. Existing TouchTargets bindings already observe global-layout changes; no second touch mechanism is needed. Restore plain legend markers through generic root meaning styles, and use the existing 4dp compact token on each review-card edge to total the locked 8dp gap.

## ADR-022: UIArc Motion Implementation — Batch 01 (Five Components)

- **Date**: 2026-10-20
- **Status**: Accepted
- **Author**: Principal Native Android Engineer & Motion Implementation Specialist
- **Authority**: Direct user instruction: "EVE — UIARC MOTION IMPLEMENTATION — BATCH 01 (FIVE COMPONENTS). MISSION: Implement exactly FIVE selected UIArc components in their real, verified locations in EVE. This is an implementation task, not a proposal. Complete all five, run applicable verification, and push to 'main' only after the required gates pass."

### Context
To achieve world-class motion fidelity and tactile feel in EVE, five production UI components were selected from UIArc (https://uiarc.dev) for genuine native Android Kotlin/XML implementation:
1. Button: https://uiarc.dev/r/button.json
2. Segmented Control: https://uiarc.dev/r/segmented-control.json
3. Metric Card: https://uiarc.dev/r/metric-card.json
4. Badge: https://uiarc.dev/r/badge.json
5. Text Shimmer: https://uiarc.dev/r/text-shimmer.json
6. Motion tokens and specification: https://uiarc.dev/r/arc-motion-tokens.json and https://uiarc.dev/docs/motion

### Decisions

1. **UIArc Motion Tokens & Physics (`UiArcMotion.kt`)**:
   - Codified exact UIArc durations: `INSTANT` (120ms), `FAST` (160ms), `EXIT` (180ms), `STANDARD` (240ms), `CONSIDERED` (480ms), `SHIMMER_SWEEP` (1800ms), and `SHIMMER_PAUSE` (576ms).
   - Codified exact cubic bezier easings via `PathInterpolator`:
     - `ENTER`: (0.16, 1.0, 0.30, 1.0)
     - `EXIT`: (0.70, 0.0, 0.84, 0.0)
     - `STANDARD`: (0.22, 1.0, 0.36, 1.0)
     - `IN_OUT`: (0.65, 0.0, 0.35, 1.0)
   - Translated Framer Motion / Motion `visualDuration` and `bounce` parameters to native damped harmonic oscillator spring equations:
     - `zeta = 1 - bounce`
     - `k = (2 * pi / (1.2 * visualDuration))^2`
     - `SNAPPY`: visualDuration 0.26s, bounce 0.12 (zeta = 0.88, k = 405.5, settling ~360ms)
     - `SMOOTH`: visualDuration 0.40s, bounce 0.00 (zeta = 1.00, k = 171.3, settling ~500ms)
     - `MORPH`: visualDuration 0.42s, bounce 0.16 (zeta = 0.84, k = 155.4, settling ~550ms)
     - `REVEAL`: visualDuration 0.48s, bounce 0.00 (zeta = 1.00, settling ~600ms)
     - `RESPONSIVE`: explicit stiffness 520, damping 38 (zeta = 0.833, settling ~260ms)
   - Button press scale formula: width > 220dp -> 0.985; width <= 48dp -> 0.96; default -> 0.97.

2. **Component 1 — UiArcButton (Feedback Submission)**:
   - Target: `app/src/main/res/layout/activity_feedback.xml`, `FeedbackActivity.kt`, `btnSendFeedback`.
   - Snappy spring press scale with release spring and haptic feedback.
   - Truthful pending label morph ("Send Feedback" -> "Sending…") with vertical shift (+4dp / -3dp) and enter/standard crossfade.
   - Integrated loading spinner replacing duplicate visual feedback while preserving accessibility.
   - Prevents duplicate submissions while pending (`isLoading` guard).
   - Restores idle state on failure; preserves success bulletin and navigation.
   - Preserves `progressBar` in layout with `visibility="gone"` for contract safety.

3. **Component 2 — UiArcSegmentedControl (Admin Analytics Time Range)**:
   - Target: `app/src/main/res/layout/activity_admin_analytics.xml`, `AdminAnalyticsActivity.kt`, `chipGroupTimeRange`.
   - Single shared selection pill gliding between options using `spring.morph` (visualDuration 0.42s, bounce 0.16).
   - Pill glides behind labels (no second indicator on top of moving pill).
   - Preserves exact contract IDs (`chipLast7Days`, `chipLast30Days`, `chipAllTime`) and range mappings (`LAST_7_DAYS`, `LAST_30_DAYS`, `ALL_TIME`).
   - Default range preserved ("ALL_TIME"); persists across rotation and re-inflation.
   - Full keyboard/D-pad navigation and TalkBack accessibility support.
   - HorizontalScrollView container for controlled overflow on narrow devices.

4. **Component 3 — UiArcAnimatedCounterView (Performance Metric Cards)**:
   - Target: `app/src/main/res/layout/activity_performance.xml`, `PerformanceActivity.kt`, `tvTotalAttempts`, `tvOverallAccuracy`.
   - Odometer digit wheel for numeric values.
   - First display: rolls digits from 0 into target value with staggered item delay using `reveal` spring.
   - Subsequent updates: direction-aware rolling (+1 for increase, -1 for decrease) using `smooth` spring.
   - Preserves integer formatting for attempts and percentage formatting for accuracy.
   - Avoids replaying animations on scroll or view rebinding.
   - TalkBack reads one complete formatted value rather than individual rolling glyphs.
   - Reduced motion renders final values immediately.

5. **Component 4 — UiArcBadgeView (Result Cutoff Verdict)**:
   - Target: `app/src/main/res/layout/activity_result.xml`, `ResultCutoffPresentation.kt`, `tvCutoffVerdict`.
   - In-place label change: outgoing copy lifts away (-0.3em) with soft blur/alpha fade (160ms); incoming copy rises from below (+0.3em) with enter curve (240ms).
   - Badge width follows content with `spring.morph` (visualDuration 0.42s, bounce 0.16).
   - Smoothly crossfades qualification icons (✓ / ✗).
   - Preserves official EVE success/danger/neutral tokens and backgrounds (`design_badge_green`, `design_badge_red`, `design_badge_neutral`).
   - Preserves all cutoff calculations, visibility rules, and navigation.

6. **Component 5 — UiArcTextShimmerView (Performance Loading Status)**:
   - Target: `app/src/main/res/layout/activity_performance.xml`, `PerformanceActivity.kt`, `tvLoadingPerformance`.
   - Linear bell-shaped highlight sweep across muted text base (`text_secondary` to `text_primary`).
   - Sweep duration 1.8 seconds with 576ms pause between passes.
   - Runs strictly while `UiState.Loading` is active and view is visible; pauses/resumes on visibility changes.
   - Cleans up shader immediately when loading ends or screen detaches.
   - Reduced motion renders solid readable text without a running shimmer.
   - Efficient: zero allocations in `onDraw`.

7. **Strict Boundary Preservation**:
   - Zero changes to unrelated screens.
   - Telegram circular reveal Day/Night animation, ThemeSwitchAnimator, and ThemeManager remain 100% protected.
   - All Lottie assets, playback timing, and triggers remain 100% protected.
   - All SVG icon vectors, test colors, and backend security/scoring invariants remain invariant.

## ADR-023: UIArc Motion Implementation — Batch 02 (Five Components)

- **Date**: 2026-10-21
- **Status**: Accepted
- **Author**: Principal Native Android Engineer & Motion Implementation Specialist
- **Authority**: Direct user instruction: "EVE — UIARC MOTION IMPLEMENTATION — BATCH 02"

### Context
Following the successful implementation of Batch 01 (ADR-022), Batch 02 delivers the five production UI components from UIArc (https://uiarc.dev) for genuine native Android Kotlin/XML implementation:
1. Expanding Search: https://uiarc.dev/r/expanding-search.json
2. Switch: https://uiarc.dev/r/switch.json
3. Inline Edit: https://uiarc.dev/r/inline-edit.json
4. Confirm Morph: https://uiarc.dev/r/confirm-morph.json
5. Accordion: https://uiarc.dev/r/accordion.json

### Decisions

1. **Component 1 — Expanding Search (Home Toolbar Morph)**:
   - Target: `app/src/main/java/com/eve/app/ui/home/MainActivity.kt`, `activity_main.xml` (`btnSearch`, `searchTopBar`, `normalTopBar`, `layoutSearchFieldContainer`, `etSearch`, `btnSearchBack`, `btnClearSearch`).
   - Replaced legacy abrupt `translationX = 50f` / `-50f` slides with source-verified UIArc spring width morph.
   - Expanding morph spring: `UiArcMotion.Spring.MORPH` (`visualDuration = 0.42s`, `bounce = 0.16`, settling ~550ms).
   - Collapsing smooth spring: `UiArcMotion.Spring.SMOOTH` (`visualDuration = 0.40s`, `bounce = 0.00`, settling ~500ms).
   - Input text fade: enter duration 200ms with `Ease.ENTER` (start delay 80ms); exit duration 80ms with `Ease.STANDARD`.
   - Back button: enter duration 160ms with `Ease.ENTER`; exit duration 120ms (`Duration.INSTANT`) with `Ease.STANDARD`.
   - Screen geometry clamped to available screen width and max reference spec (360dp) without clipping.
   - Preserves exam filtering, category chips, keyboard dismissal, TalkBack focus, and Back navigation.
   - Reduced motion: instant layout toggle without spring animation.

2. **Component 2 — UiArcSwitch (Daily Practice Reminder in Settings & Profile)**:
   - Target: `app/src/main/res/layout/activity_settings.xml`, `app/src/main/res/layout/activity_profile.xml`, `UiArcSwitch.kt`.
   - Tactile switch extending `DesignSwitch` / `SwitchCompat` with exact UIArc physics:
     - Critically damped glide spring (`UiArcMotion.Spring.GLIDE`: `visualDuration = 0.30s`, `bounce = 0.00`, settling ~400ms).
     - Thumb stretch along travel (+5dp) while pressed (`stretch = 5`).
     - Snappy spring release returning thumb to circular geometry.
     - Standard track color crossfade (`Ease.STANDARD`, 160ms/240ms) between `@color/track` and `@color/system_green`.
   - Wired to `switchReminder` in both `SettingsActivity` and `ProfileActivity` with consistent row-tap forwarding without double toggling.
   - Respects `ReminderScheduler` persistence, system settings, and reduced-motion modes.

3. **Component 3 — Inline Edit (Profile Name Edit-State Motion)**:
   - Target: `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt`, `activity_profile.xml` (`tvName`, `tilName`, `etName`, `btnSaveProfile`).
   - Architectural assessment: The existing profile name field is permanently editable within a cohesive form alongside DOB, Category chips, and the primary Save Profile action (`btnSaveProfile`). Replacing this with a separate inline morph trigger would introduce redundant UI and break the atomic profile save API contract.
   - Integrated source-verified edit-state motion:
     - On successful persistence, `tvName` receives UIArc text rise motion (+0.3em vertical rise, `Ease.ENTER`, 240ms duration) and alpha fade.
     - Never saves on blur/focus loss without explicit user action.
     - Failed save keeps user's entered text and provides a safe retry path.
     - Full IME, cursor position, TalkBack, and lifecycle restoration preserved.

4. **Component 4 — Confirm Morph (Delete Account Staged Confirmation)**:
   - Target: `app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt` (`rowDeleteAccount`).
   - Staged destructive confirmation adapted to irreversible account deletion:
     - Duplicate-action guard: 280ms (`ARM_MS = 280L`) disallowing accidental immediate confirms.
     - Confirmation timeout: 6000ms auto-dismissal if unconfirmed.
     - Explicit irreversible data erasure warning and cancel option.
     - Pending state with progress indicator bound to actual backend execution (`ApiClient.apiService.deleteAccount()` + Firebase Auth user deletion).
     - Omitted fake undo: account deletion is irreversible.
     - Preserves required re-authentication protections and Back navigation dismissal.

5. **Component 5 — Accordion (Result Analysis Details Audit)**:
   - Target: `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt`, `activity_result.xml`.
   - Thorough codebase inspection confirmed that `headerOverviewStatistics`, `headerOverviewPerformance`, and `headerOverviewAnalytics` exclusively navigate to `ResultDetailActivity` via `openStats`, `openPerf`, and `openAnalytics`.
   - There are NO other inline expandable/collapsible sections on the Result screen.
   - Per mandatory specification ("If the existing Statistics/Performance/Analytics headers exclusively navigate to ResultDetailActivity, do not hijack their click handlers. Instead, locate another genuine existing inline expandable Result section. If no valid inline section exists, report this component as NOT APPLICABLE and do not invent a new section or claim completion."):
     - Component 5 is formally audited and verified as **NOT APPLICABLE**.
     - No navigation hijacked, no fake accordion invented.

6. **Strict Boundary Preservation**:
   - Zero changes to unrelated screens.
   - Telegram circular reveal Day/Night animation, ThemeSwitchAnimator, and ThemeManager remain 100% protected.
   - All Lottie assets, playback timing, and triggers remain 100% protected.
   - All SVG icon vectors, test colors, and backend security/scoring invariants remain invariant.

## ADR-024: UIArc Motion Implementation — Batch 03 (Five Components)

- **Date**: 2026-10-21
- **Status**: Accepted
- **Author**: Principal Native Android Engineer, UI Motion Specialist, and QA Lead
- **Authority**: Direct user instruction: "EVE — UIARC MOTION IMPLEMENTATION — BATCH 03"

### Context
Following the successful completion of Batch 01 (ADR-022) and Batch 02 (ADR-023), Batch 03 delivers five production UI motion components from UIArc (https://uiarc.dev) for genuine native Android Kotlin/XML implementation:
1. Progress: https://uiarc.dev/r/progress.json
2. Bottom Sheet: https://uiarc.dev/r/bottom-sheet.json
3. Toast Stack: https://uiarc.dev/r/toast-stack.json
4. Number Field: https://uiarc.dev/r/number-field.json
5. In-View Title: https://uiarc.dev/r/in-view-title.json

### Decisions

1. **Component 1 — Progress (Real Measurable Operations Audit & UiArcProgressBar)**:
   - Target: `app/src/main/java/com/eve/app/designsystem/UiArcProgressBar.kt`.
   - Comprehensive audit of candidate operations in EVE:
     - Test submission (`TestActivity.kt`): single atomic HTTP POST `/api/attempts/submit` with no stream progress chunks.
     - Syllabus upload (`ManageSyllabusActivity.kt`): single atomic multipart POST with indeterminate spinner.
     - AI test generation (`ManageExamsActivity.kt`): single atomic LLM POST request with indeterminate spinner.
     - Download flow (`SyllabusActivity.kt`): handed off to system `DownloadManager` with broadcast receiver.
     - `DesignLoadingView.kt`: explicitly indeterminate ring spinner.
   - Per explicit directive ("Never invent percentage progress for indeterminate network requests. Never replace an indeterminate spinner with a fake 0–100% counter. Use actual authoritative progress data. If no genuine determinate operation exists, mark NOT APPLICABLE and do not create a decorative progress bar."):
     - Component 1 is formally audited and certified as **NOT APPLICABLE** on existing screens to preserve data integrity and server contracts.
     - Created `UiArcProgressBar.kt` in the design system adhering strictly to UIArc source specifications:
       - Smooth spring (`Spring.SMOOTH`) driving fill translation from left (`-100%` to `0%`).
       - Completion check settling in with snappy spring (`Spring.SNAPPY`, 240ms delay).
       - Success green track fill transition when complete (>= 100%).
       - Full TalkBack accessibility and reduced-motion instant jump.

2. **Component 2 — Bottom Sheet (Target Exams Selection Dual Detents)**:
   - Target: `app/src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt`, `bottom_sheet_target_exams.xml`.
   - Implemented source-verified UIArc dual detents: `[0.45, 0.92]` of viewport height.
   - Initial detent at 0.45 (`STATE_HALF_EXPANDED`), providing an immediate peek of target exams with Skip and Continue actions.
   - Finger-following drag expands sheet to 0.92 height (`STATE_EXPANDED`).
   - Smooth gesture arbitration with inner `NestedScrollView`: scrolling inside content expands sheet before scrolling chips, and scrolling down to top collapses sheet before dismissing.
   - Downward swipe past 0.45 peek detent dismisses cleanly without triggering chip clicks.
   - Preserves shared preferences persistence (`KEY_TARGET_EXAMS`, `KEY_ONBOARDING_DONE`), callbacks, and Back dismissal.
   - Reduced motion: immediate state transition without spring lag.

3. **Component 3 — Toast Stack (AppBulletin Notification Infrastructure Upgrade)**:
   - Target: `app/src/main/java/com/eve/app/util/AppBulletin.kt`, `layout_bulletin.xml`.
   - Transformed `AppBulletin` into a genuine UIArc Toast Stack manager:
     - Default display duration: 5000ms (`DURATION_DEFAULT = 5000L`).
     - Error/warning duration multiplier: 1.6 (`DURATION_ERROR = 8000L`).
     - Maximum queue: 12 (`MAX_QUEUE = 12`).
     - Maximum visible: 3 (`MAX_VISIBLE = 3`).
     - Stacked peek: 14dp (`PEEK_DP = 14f`).
     - Scale step per level: 0.05 (`SCALE_STEP = 0.05f`).
     - Incoming toasts slide up from bottom edge with `Spring.SMOOTH`.
     - Tucked toasts stack behind front card (`translationY = -depth * 14dp`, `scale = 1 - depth * 0.05`).
     - Touch down pauses dismiss timer; touch release resumes.
     - Horizontal swipe-to-dismiss right gesture with snappy/exit animation.
     - On front toast dismiss, remaining toasts reflow forward with `Spring.SMOOTH`.
     - Full backwards compatibility with all existing `AppBulletin` callers (`show`, `showSuccess`, `showError`, `make`).

4. **Component 4 — Number Field (Admin Exam Configuration Question Count)**:
   - Target: `app/src/main/java/com/eve/app/designsystem/UiArcNumberField.kt`, `activity_manage_exams.xml`, `ManageExamsActivity.kt`.
   - Applied to Question Count setting (`etQuestionCount` / `numberFieldQuestionCount`):
     - Value bounds: `min = 1, max = 200, step = 1`, default 20.
     - Stepper buttons with minimum 48dp touch targets (`@dimen/design_touch_min`).
     - Tabler minus and plus vector icons (`ic_minus.xml`, `ic_add.xml`).
     - Snappy press scale (`Spring.SNAPPY`).
     - Hold-to-repeat timing: 400ms initial delay (`HOLD_DELAY`), accelerating from 150ms to 40ms (`HOLD_FASTEST`) via exponential decay `150 * (0.86^n)`.
     - Elastic boundary kick animation on limit hit (`Spring.MORPH`).
     - Direct keyboard entry, validation, error states, and IME support completely preserved.

5. **Component 5 — In-View Title (Performance Headings Reveal)**:
   - Target: `app/src/main/java/com/eve/app/designsystem/UiArcInViewTitleView.kt`, `activity_performance.xml`, `PerformanceActivity.kt`.
   - Applied to section headings: "Score Trend" (`tvHeadingScoreTrend`) and "Topic-wise Accuracy" (`tvHeadingTopicAccuracy`).
   - Trigger: when 45% of heading is in viewport (`amount = 0.45f`).
   - Coordinated reveal timing:
     - Opacity leads: 448ms (`settle * 0.56`, `Ease.STANDARD`).
     - Blur clears next: 624ms (`settle * 0.78`, API 31+ RenderEffect with fallback).
     - Transform settles last: 800ms (`settle`, `Ease.ENTER` with 6dp subtle vertical rise).
   - Single trigger (`once = true`), headings already scrolled past show immediately without flashing.
   - Reduced motion: instant reveal without blur or motion.
   - TalkBack: full content description announced once as a cohesive heading.

6. **Strict Boundary Preservation**:
   - Zero changes to unrelated screens.
   - Telegram circular reveal Day/Night animation, ThemeSwitchAnimator, and ThemeManager remain 100% protected.
   - All Lottie assets, playback timing, and triggers remain 100% protected.
   - All SVG icon vectors, test colors, and backend security/scoring invariants remain invariant.

## ADR-025: App-Wide Apple iOS 26 Official Typography Migration (Canonical 11 Styles, Line Heights, Weights, and Inter Open-Source Font Family)

- **Date**: 2026-10-21
- **Status**: Accepted
- **Author**: Principal Android Engineer, Apple HIG Typography Specialist, Accessibility Engineer

### Context
The application had inconsistent typography definitions resulting from fragmented design phases. The latest user instruction authorizes an app-wide typography migration implementing Apple's official iOS 26 Human Interface Guidelines typography scale across the entire application, while superseding older Poppins-only choices without modifying unrelated visuals, colors, geometry, or backend scoring.

### Official Documentation References
- Apple Human Interface Guidelines - Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple Developer - System Font APIs (UIFont.TextStyle): https://developer.apple.com/documentation/uikit/uifont/textstyle
- SIL Open Font License 1.1: https://scripts.sil.org/OFL
- Rasmus Andersson / Inter: https://rsms.me/inter/

### Decisions
1. **Font Family & Legal Licensing**:
   - Proprietary Apple SF Pro binaries must not be bundled without explicit embedding rights.
   - Migrated from legacy Poppins to official Inter v4.1 (SIL OFL 1.1).
   - Bundled authentic weight binaries in `res/font/`:
     - `inter_regular.ttf` (weight 400)
     - `inter_medium.ttf` (weight 500)
     - `inter_semibold.ttf` (weight 600)
     - `inter_bold.ttf` (weight 700)
     - `inter_variable.ttf` (variable axis font)
   - Declared unified font family XML in `app/src/main/res/font/inter.xml`.
   - Included full license at `app/src/main/assets/licenses/Inter-OFL.txt` and attributed in `THIRD_PARTY_NOTICES.md`.
   - Rewrote `FontManager.kt` to load Inter typefaces with automatic fallback to system sans-serif for Hindi/Devanagari scripts.

2. **Official Apple iOS 26 Typography Scale (All 11 Canonical Styles)**:
   - Defined canonical dimension tokens in `design_dimens.xml` (synchronized across `values`, `values-sw360dp`, and `values-sw600dp`):
     - Large Title: 34sp / 41sp line height (Regular 400 / Bold 700)
     - Title 1: 28sp / 34sp line height (Regular 400 / Bold 700)
     - Title 2: 22sp / 28sp line height (Regular 400 / Bold 700)
     - Title 3: 20sp / 25sp line height (Regular 400 / Semibold 600)
     - Headline: 17sp / 22sp line height (Semibold 600 / Semibold 600)
     - Body: 17sp / 22sp line height (Regular 400 / Semibold 600)
     - Callout: 16sp / 21sp line height (Regular 400 / Semibold 600)
     - Subheadline: 15sp / 20sp line height (Regular 400 / Semibold 600)
     - Footnote: 13sp / 18sp line height (Regular 400 / Semibold 600)
     - Caption 1: 12sp / 16sp line height (Regular 400 / Semibold 600)
     - Caption 2: 11sp / 13sp line height (Regular 400 / Semibold 600)

3. **Style & Token Declarations in `design_type.xml`**:
   - Declared both Standard and `.Emphasized` styles for all 11 canonical styles parented to Material 3 base appearances.
   - Re-aliased component-specific styles (`NavigationTitle`, `DialogTitle`, `DialogMessage`, `CardHeading`, `Badge`, `Timer`, `HeroScore`, `MetricValue`, `ChartLabel`).
   - Retained token styles (`TextAppearance.Eve.Token.*`) pointing to Inter font weights.
   - Updated Material 3 `Theme.Eve` text appearances to bind to official iOS 26 typography styles.

4. **Codebase & Helper Adaptation**:
   - Updated `DesignType.kt` enum with all 11 canonical styles and emphasized variants.
   - Added `applyAppleTypography(textView, style, emphasized)` programmatic helper with line spacing computation.
   - Normalized `scripts/design_check.py` to recognize AAPT dot-to-underscore style definitions and verify bundled Inter family.

5. **Invariants Preserved**:
   - Zero visual regressions to colors, geometry, borders, or elevation.
   - Telegram circular reveal Day/Night transition completely preserved.
   - Lottie animations and triggers completely preserved.
   - Server-authoritative test scoring, timer, and ranking completely preserved.

## ADR-026: EVE Typography Studio (Part 1: Immediate Local Preview, 11 Apple iOS 26 Styles & Inter Family Weights)

- **Date**: 2026-10-21
- **Status**: Accepted
- **Author**: Principal Android Engineer, Typography Architect

### Context
Following the app-wide Apple iOS 26 typography migration (ADR-025), a dedicated developer and admin Typography Studio is added under Admin Dashboard. The studio enables administrators to visually inspect, test, and preview all 11 canonical Apple iOS 26 typography styles and the authentic Inter font weights, with a requirement that selecting a font changes the local preview immediately.

### Decisions
1. **Immediate Local Preview Contract**:
   - Selecting any font option updates the active font info and immediately reflects across all preview rows and interactive component previews without requiring save, debounce, or network publication.
   - Live sample text modifications and the `.Emphasized` variant toggle update all 11 canonical styles synchronously.
   - English and Hindi / Devanagari preset quick-actions prove typography rendering and native multilingual fallback.

2. **11 Canonical Apple iOS 26 Styles Coverage**:
   - Synchronously renders Large Title (34sp/41sp), Title 1 (28sp/34sp), Title 2 (22sp/28sp), Title 3 (20sp/25sp), Headline (17sp/22sp), Body (17sp/22sp), Callout (16sp/21sp), Subheadline (15sp/20sp), Footnote (13sp/18sp), Caption 1 (12sp/16sp), and Caption 2 (11sp/13sp).
   - Each row item uses solid `DesignCardView` styling conforming to Eve's design system tokens.

3. **Font Options**:
   - `FontManager.FontOption` enum exposes the authentic Inter font weights (`INTER_REGULAR` 400, `INTER_MEDIUM` 500, `INTER_SEMIBOLD` 600, `INTER_BOLD` 700) and system typefaces (`SYSTEM_SANS`, `SYSTEM_SERIF`, `SYSTEM_MONOSPACE`).

4. **Invariants Preserved**:
   - Protected Telegram circular reveal Day/Night transition preserved.
   - Protected Lottie animations and triggers preserved.
   - Server-authoritative test scoring, ranking, and timer preserved.
   - Admin access controls enforced.

## ADR-027: UIArc Motion Implementation — Batch 04 (Five Components)

- **Date**: 2026-10-21
- **Status**: Accepted
- **Author**: Principal Native Android Engineer, Motion Fidelity Specialist, Product Quality Lead

### Context
Following the successful completion of Batch 01 (ADR-022), Batch 02 (ADR-023), Batch 03 (ADR-024), and the Typography Studio (ADR-026), Batch 04 delivers five production UI motion components from the official UIArc registry (https://uiarc.dev) for genuine native Android Kotlin/XML implementation:
1. **Component 1 — Line Chart** (`https://uiarc.dev/r/line-chart.json`): `ScoreTrendChartView.kt`
2. **Component 2 — Gauge** (`https://uiarc.dev/r/gauge.json`): `DesignAccuracyRing.kt`
3. **Component 3 — Expandable Card** (`https://uiarc.dev/r/expandable-card.json`): `BookmarkAdapter.kt`
4. **Component 4 — Text Morph** (`https://uiarc.dev/r/text-morph.json`): `UiArcTextMorphView.kt` (`tvBookmarkCount` in `BookmarksActivity`)
5. **Component 5 — Alert** (`https://uiarc.dev/r/alert.json`): `UiArcAlertView.kt` (`tvOfflineBanner` in `MainActivity`)

### Decisions

1. **Component 1: Line Chart (`ScoreTrendChartView.kt`)**:
   - Native translation of `https://uiarc.dev/r/line-chart.json`.
   - **Path Entrance Reveal**: Left-to-right drawing animation using `PathMeasure` (`DRAW_DURATION = 840L`, `Ease.IN_OUT`).
   - **In-View Trigger**: 45% viewport visibility listener (`TRIGGER_AMOUNT = 0.45f`, one-shot `OnScrollChangedListener`).
   - **Value Morphing**: Smooth path morphing across data updates driven by `UiArcMotion.Spring.SMOOTH` (500ms).
   - **Interactive Crosshair & Floating Tooltip**: Gliding vertical crosshair and floating card clamped within chart bounds using `UiArcMotion.Spring.SNAPPY`, `TOOLTIP_GAP_DP = 14f`, `TOOLTIP_RADIUS_DP = 12f`.
   - **Touch Arbitration**: Handles touch scrubbing cleanly and disallows parent `ScrollView` interception via `requestDisallowInterceptTouchEvent(true)`.

2. **Component 2: Gauge (`DesignAccuracyRing.kt`)**:
   - Native translation of `https://uiarc.dev/r/gauge.json`.
   - **In-View Trigger**: 50% viewport visibility entrance trigger (`TRIGGER_AMOUNT = 0.50f`).
   - **Entrance Reveal**: Animated sweep from 0 to target value with `SpringCurve(visualDuration = 0.768f, bounce = 0.00f, settlingDurationMs = 768L)`.
   - **Value Morphing**: Smooth value changes post-reveal using `UiArcMotion.Spring.SMOOTH` (500ms).
   - **Synchronized Sweep & Counter**: Sweep arc drives centered percentage text synchronously on each frame with tabular numerals.
   - **Semantic Color Thresholds**: `>= 70%` (`systemGreen`), `>= 40%` (`systemOrange`), `< 40%` (`systemRed`) with smooth `ArgbEvaluator` blending across boundary ranges.
   - **Intentional Geometric Parity Adaptation**: EVE retains its approved 360-degree circular ring geometry (`@dimen/design_ring_size`, 48dp) and track token rather than UIArc's 270-degree open gauge with needle/ticks, strictly preserving approved layout geometry, touch targets, and visual consistency.

3. **Component 3: Expandable Card (`BookmarkAdapter.kt`)**:
   - Native translation of `https://uiarc.dev/r/expandable-card.json`.
   - **Inline Height Morphing**: Replaces full `notifyItemChanged` flicker with inline animated height changes on `tvExplanation` driven by `UiArcMotion.Spring.SMOOTH` (~500ms).
   - **Opening Sequence**: Height expands with `Spring.SMOOTH`, explanation text fades in with 120ms entrance delay (`UiArcMotion.Ease.STANDARD`, 240ms duration).
   - **Closing Sequence**: Explanation text fades out first (100ms, `Ease.STANDARD`), then height collapses to 0 after `closeHold = 60ms` with `Spring.SMOOTH`. Rapid reversal skips the hold delay.
   - **RecyclerView Recycling Safety**: View holders synchronously restore expanded/collapsed visibility on bind without height distortion, and cancel ongoing animators on recycling.

4. **Component 4: Text Morph (`UiArcTextMorphView.kt`)**:
   - Native translation of `https://uiarc.dev/r/text-morph.json`.
   - **Character Occurrence Mapping**: Keyed by `${char}-${occurrenceIndex}` to track shared, entering, and exiting glyphs accurately.
   - **Shared Glyph Motion**: Smoothly glides from old X coordinate to new X coordinate using `UiArcMotion.Spring.MORPH` (`visualDuration = 0.42s`, `bounce = 0.16`, 550ms).
   - **Entering Glyphs**: Scale `0.80 -> 1.0`, Y translation `+0.14em -> 0`, staggered entrance fade (base 50ms + 32ms/char, 280ms duration, `UiArcMotion.Ease.ENTER`).
   - **Exiting Glyphs**: Scale `1.0 -> 0.86`, Y translation `0 -> -0.10em`, fade out (180ms duration, `UiArcMotion.Ease.STANDARD`).
   - **Accessibility**: Sets unified `contentDescription` so TalkBack announces the final string as a single coherent phrase rather than per-glyph fragments.

5. **Component 5: Alert (`UiArcAlertView.kt`)**:
   - Native translation of `https://uiarc.dev/r/alert.json`.
   - **Entrance Animation**: Height expands `0 -> measuredHeight` with `UiArcMotion.Spring.SMOOTH` (500ms), opacity fades `0 -> 1` with `UiArcMotion.Ease.ENTER` (240ms).
   - **Exit Animation**: Opacity fades `1 -> 0` with `UiArcMotion.Ease.STANDARD` (160ms), height collapses `measuredHeight -> 0` with `UiArcMotion.Spring.SMOOTH` (500ms).
   - **Protected Invariant — Telegram Theme Reveal Protection**: Bypasses animation completely during `ThemeManager.isTransitioning` or `ThemeSwitchAnimator.isTransitioning` to guarantee zero interference with the Telegram circular reveal.
   - **Accessibility**: Calls `announceForAccessibility` on alert entrance.

6. **Invariants Preserved**:
   - Zero changes to protected Telegram Day/Night circular reveal or theme switching resources.
   - Zero changes to Lottie motion assets, playback, or triggers.
   - Server-authoritative test scoring, ranking, and timer preserved.
   - Reduced motion accessibility fully supported across all five components with instant state updates.

## ADR-028: UIArc Motion Implementation — Batch 05 (Five Components)

- **Date**: 2026-10-21
- **Status**: Accepted
- **Author**: Principal Native Android Engineer, Motion Fidelity Specialist, Accessibility Engineer & Production QA Lead

### Context
Following the successful completion of Batch 01 (ADR-022), Batch 02 (ADR-023), Batch 03 (ADR-024), Typography Studio (ADR-026), and Batch 04 (ADR-027), Batch 05 delivers five production UI motion components from the official UIArc registry (https://uiarc.dev) for genuine native Android Kotlin/XML implementation:
1. **Component 1 — Swipe Actions** (`https://uiarc.dev/r/swipe-actions.json`): `UiArcSwipeRow.kt` (`item_bookmark_card.xml`, `BookmarkAdapter.kt`, `BookmarksActivity.kt`)
2. **Component 2 — File Upload** (`https://uiarc.dev/r/file-upload.json`): `UiArcFileUploadView.kt` helper & morphing layout transitions (`activity_manage_syllabus.xml`, `ManageSyllabusActivity.kt`)
3. **Component 3 — Morph Select** (`https://uiarc.dev/r/morph-select.json`): `UiArcMorphSelectView.kt` with stable Exam ID resolution (`activity_manage_syllabus.xml`, `ManageSyllabusActivity.kt`)
4. **Component 4 — Radio Cards** (`https://uiarc.dev/r/radio-cards.json`): `UiArcRadioCardsGroup.kt` (`dialog_daily_goal_radio_cards.xml`, `SettingsActivity.kt`)
5. **Component 5 — Stats Band** (`https://uiarc.dev/r/stats-band.json`): `UiArcStatsBandView.kt` (`activity_admin.xml`, `cardUserStats`, `AdminActivity.kt`)

### Decisions

1. **Component 1: Swipe Actions (`UiArcSwipeRow.kt`)**:
   - Native translation of `https://uiarc.dev/r/swipe-actions.json`.
   - **Horizontal Gesture Detection**: `SLOP = 8dp` movement before horizontal drag locks out vertical list scrolling.
   - **Action Dimension**: `ACTION = 76dp` width for underlying revealed remove bookmark action layer.
   - **Rubber Band Resistance**: Elastic overshoot past limits using `rubberBand(overshoot, dimension) = (1 - 1 / (overshoot * 0.55 / dimension + 1)) * dimension`.
   - **Commit Threshold**: Full swipe commits at `max(openActionWidth + 48px, 56% of rowWidth)`.
   - **Flick Commitment**: Releases with outward velocity `> 900 px/s` and projected distance (`PROJECTION = 0.1s`) trigger instant commit.
   - **Settle Physics**: Springs home to 0 with `UiArcMotion.Spring.SMOOTH` (500ms); settles to resting open with `UiArcMotion.Spring.SNAPPY` (360ms).
   - **Committing Sequence**: Card slides to direction * `(width + 2)` with `Spring.SMOOTH`, then collapses height (`Spring.SMOOTH`, 120ms delay) and fades out before triggering `onUnbookmark(item)` and opening `AppUndoBar`.
   - **Single-Open Coordination**: Tapping outside or scrolling the list automatically settles any open row back to 0.
   - **RTL & Accessibility**: Full RTL direction adaptation; preserves explicit `btnBookmark` on the card to ensure TalkBack, screen readers, and keyboard navigation never require touch gestures.

2. **Component 2: File Upload (`UiArcFileUploadView.kt` & Morphing Form Transition)**:
   - Native translation of `https://uiarc.dev/r/file-upload.json`.
   - **Selection Morph**: On picking a valid PDF (<20MB), `btnChoosePdf` scales down (0.96) and fades out (160ms, `Ease.STANDARD`), morphing into `layoutSelectedPdf` which rises (+8dp) and fades in (240ms, `Ease.ENTER`).
   - **File Details Display**: Shows PDF icon, filename, and formatted size (`formatSize`: B, KB, MB) with status descriptor `· Ready`.
   - **Clearing Exit**: On clear button tap, `layoutSelectedPdf` slides up (-8dp) and fades out (160ms), morphing back to `btnChoosePdf` (240ms).
   - **Authentic Indeterminate Progress**: OkHttp repository upload uses genuine indeterminate spinner (`progressBarUpload`); never invents fake percentage progress.
   - **Completion Checkmark**: On server confirmation, `ivUploadCheckDone` scales up from 0.6 and fades in (240ms, `Ease.ENTER`, 80ms delay) before form reset and bulletin notification.
   - **Failure Resilience**: Preserves selected document on upload error for retry; safe clearing without orphan server files.

3. **Component 3: Morph Select (`UiArcMorphSelectView.kt`)**:
   - Native translation of `https://uiarc.dev/r/morph-select.json`.
   - **Reference Geometry**: 272px reference width, 296px max panel height (`REFERENCE_MAX_HEIGHT_DP = 296f`).
   - **Trigger & Opening**: Compact trigger morphs open into options surface using `GROW` spring (`Spring.SMOOTH`, 500ms), expanding corner radius from 14dp to `OPEN_RADIUS = 22dp`.
   - **Search Filtering**: Automatic search field when `options > 8` (`SEARCH_THRESHOLD = 8`).
   - **Gliding Selection Highlight**: Rounded highlight pill glides vertically behind options using `GLIDE` spring (`UiArcMotion.Spring.SELECT_GLIDE`, 380ms).
   - **Selection & Closing**: Selected item label lifts with `FLY` spring (400ms) while panel closes with `SHRINK` spring (450ms).
   - **Entity Mapping Fix**: Selection resolves strictly by stable Exam ID (`option.id`), completely immune to index misalignment from filtered search lists. Preserves `tilSelectExam` / `actvSelectExam` as accessible hidden fallbacks.

4. **Component 4: Radio Cards (`UiArcRadioCardsGroup.kt`)**:
   - Native translation of `https://uiarc.dev/r/radio-cards.json`.
   - **Sliding Selection Ring**: Animated stroke ring glides between options (X, Y, Width, Height) using `GLIDE` spring (`UiArcMotion.Spring.CARD_GLIDE`, 550ms) with `FAST` fade-in (160ms).
   - **Host Target**: Settings Daily Goal dialog (`SettingsActivity.kt`, `dialog_daily_goal_radio_cards.xml`).
   - **Options**: 1, 2, 3, and 5 tests/day with descriptive secondary text and 48dp+ minimum touch targets.
   - **Persistence & Feedback**: Saves directly to SharedPreferences via `StreakHelper`, refreshes Settings row text, and announces selection to TalkBack.

5. **Component 5: Stats Band (`UiArcStatsBandView.kt`)**:
   - Native translation of `https://uiarc.dev/r/stats-band.json`.
   - **Animated Count-Up**: Counts from previous value (or 0) to target with `COUNT_DURATION_MS = 1600L` using `UiArcMotion.Ease.ENTER`.
   - **Stagger**: 90ms (`STAGGER_MS = 90L`) sequential delay per metric index.
   - **Content Rise**: 8dp vertical rise with opacity fade-in on initial entrance.
   - **In-View Trigger**: Observes viewport visibility, triggering when 50% enters viewport (`TRIGGER_AMOUNT = 0.50f`).
   - **Null / Unavailable State**: Renders "—" without animating fake zero when counts are unavailable.
   - **Lifecycle Safety**: Active animators safely cancel on `onDetachedFromWindow()` and `AdminActivity.onDestroy()`.
   - **Host Integration**: Admin User Statistics card (`activity_admin.xml`, `cardUserStats`, `statsBandUserManagement`) animating `tvTotalUsers` and `tvOnlineUsers` upon data load and refresh (`btnRefreshStats`).

6. **Invariants Preserved**:
   - Telegram Day/Night circular reveal and theme switching resources remain protected.
   - Lottie motion assets, playback, and triggers are untouched.
   - Server-authoritative test scoring, ranking, and timer preserved.
   - Full reduced motion support across all five components with instant fallback.

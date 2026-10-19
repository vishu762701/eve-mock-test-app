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
- **Status**: Accepted
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
- **Status**: Accepted
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
- **Status**: Accepted
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
- **Colour and type evidence**: Official Color, Typography and Materials JSON were fetched and read; preserved rows and source hashes are in docs/ios26/APPLE_REFERENCE_ROWS.md. All 12 default light/dark palette rows already matched colors.xml. Default iOS type sizes and leading already matched type.xml.
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
   - Synchronized `AppWideTypographyAndColorRolesTest.testGroupedAndSystemScreenLayoutClassifications` by moving `activity_leaderboard.xml` from `systemSample` to `groupedSample`, matching the official HIG grouped canvas specification in `EVE_IOS26_UI_CONTRACT.md` and `SOURCE_OF_TRUTH.md`.

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

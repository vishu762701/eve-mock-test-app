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



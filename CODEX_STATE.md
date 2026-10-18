# EVE — CODEX STATE

## Current Project State

- Eve is a native Android app in the Gradle `:app` module, implemented in Kotlin with XML layouts/resources. Firebase provides authentication; the client calls the TypeScript/Hono Cloudflare Worker in `backend/`, which stores relational data in D1 and media in Supabase Storage. `functions/` contains separate Firebase Cloud Functions.
- Home has one admin-managed banner surface in the former large hero location. Active banners rotate/swipe inside that surface; images are clipped to its rounded bounds. Each banner can have an optional HTTP(S) URL and CTA label.
- Admin has one Home Banner manager for image preview/upload, link editing, reorder, and delete. The separate Home Hero editor and its runtime code have been removed.
- The Test screen applies the Thin Material capsule treatment only to answer options A–D and its four bottom actions. Answer selection is green. Other app controls retain their existing styles.
- Light app background tokens are pure `#FFFFFF`; dark background tokens are pure `#000000`.
- Test submission remains server-side through `POST /api/attempts/submit`; authentication, scoring, answer persistence, and navigation code were not changed by the Home Banner/Test appearance task.

### EVE: App-Wide Eve Design System Locked Color Tokens (ADR-019) — 2026-10-18

- Codified authoritative locked foundation color tokens app-wide across `values/colors.xml` and `values-night/colors.xml`:
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
- Defined semantic app-wide aliases `@color/eve_app_*` mapping directly to `@color/eve_color_*`.
- Added 13 theme attributes in `values/attrs.xml` and bound them in `Theme.Eve.Foundation` in `values/themes.xml`.
- Added Kotlin foundation constants `HEX_APP_LIGHT_*`, `HEX_APP_DARK_*` and `@ColorInt` accessors `appBackground`, `appCard`, etc. in `EveDesignSystem.kt`.
- Created shared app-wide color state lists in `res/color/` (`selector_eve_pill_bg.xml`, `selector_eve_pill_stroke.xml`, `selector_eve_pill_text.xml`, `selector_eve_tab_text.xml`, `selector_eve_floating_bar_tint.xml`).
- Maintained strict "colors only" boundary: zero changes to layout structures, bounds/dimensions, typography, strings, shapes/radii, animations, or runtime behavior.

### EVE: Design System Foundation with Locked Values & Home Screen Application (ADR-018) — 2026-10-17

- Codified the complete locked Eve Design System foundation and applied it strictly to the Home screen ONLY:
  - Locked color tokens (values/ and values-night/): `eve_home_background` (#EEF2F8 / #0B0F16), `eve_home_card` (#FFFFFF / #161B24), `eve_home_text_primary` (#000000 / #FFFFFF), `eve_home_text_secondary` (#6C6C70 / #A1A1A6), `eve_home_hairline` (rgba(0,0,0,0.10) / rgba(255,255,255,0.14)), `eve_home_tab_track` (rgba(120,120,128,0.16) / rgba(120,120,128,0.32)), `eve_home_accent` (#000000 / #FFFFFF), `eve_home_on_accent` (#FFFFFF / #000000), semantic status tokens (success #34C759/#30D158, error #FF383C/#FF4245, warning #FF8D28/#FF9230 for meaning only), and `eve_home_floating_bar_bg` (#FFFFFF / #2C2C2E).
  - Bundled static OFL DM Sans fonts: Regular 400, Medium 500, Bold 700 TTF files in `res/font/`, verified with TrueType magic header `00 01 00 00`. Default font resolves to DM Sans via `FontManager.kt`.
  - Type styles in `values/type.xml`: `TextAppearance.Eve.Home.PageTitle` (29sp Bold), `CardTitle` (18sp Bold), `CardBody` (18sp Regular), `Secondary` (14sp Regular), `TabLabel` (15sp Regular, Selected Bold), `PillLabel` (14sp Medium).
  - Segmented Tabs (Filled Pill): 40dp height, fully rounded, track color, 3dp inner padding; selected = accent fill + on-accent text; unselected = secondary text.
  - Pills (Outline): 32dp height, 12dp horizontal padding, fully rounded, 1dp accent border, accent text, with >= 48dp touch delegate without altering visible geometry.
  - Cards: 24dp radius, 12dp padding, 8dp card gap, 16dp screen margin, 16dp section gap. Soft 2dp elevation/12dp blur (10% black) in light mode; 0dp elevation/no visible shadow in dark mode (card background #161B24 separates).
  - Bottom Bar (Floating, Solid, Capsule): 12dp side margins, 10dp bottom margin (+ gesture inset), 54dp height, 22dp icons, 0.5dp hairline border, solid fill, no blur. Selected icon = accent, unselected = secondary. Dynamic bottom clearance in `MainActivity` on `rvExams` with `clipToPadding="false"`.
  - Screen-width qualifiers: Replicated all size tokens identically into `values-sw360dp/dimens.xml` and `values-sw600dp/dimens.xml`.
  - Scoping & Invariants: Applied strictly to Home screen only (`MainActivity`, `activity_main.xml`, `HomeAppearance.kt`, `ExamAdapter.kt`, `item_exam.xml`, `item_feedback_post.xml`). Test, Result, Admin, Leaderboard, Profile, and Settings screens untouched. Telegram theme switcher, Lottie files/triggers/sizes, and SVG assets 100% preserved.

### EVE: Design System Foundation Locked Values Codification & Home Screen Application (ADR-017) — 2026-10-17

- Codified all authoritative locked design tokens directly within `EveDesignSystem` (`com.eve.app.designsystem.EveDesignSystem`):
  - Locked color hex constants: Canvas `#FFFFFF` / `#000000`, Secondary Cards `#F2F2F7` / `#1C1C1E`, Interactive Blue `#0088FF` / `#0091FF`, System Green (Success) `#34C759` / `#30D158`, System Red (Error) `#FF383C` / `#FF4245`, System Orange (Warning) `#FF8D28` / `#FF9230`, Secondary Text `#50505A` / `#C5C5CC`.
  - System status methods: `systemGreen`, `systemRed`, `systemOrange`, `statusSuccess`, `statusError`, `statusWarning`.
  - Locked typography roles: `Title` (28sp bold -> `Title1Bold`), `Heading` (20sp semibold -> `Title3Semibold`), `Body` (17sp regular -> `Body`), `Secondary` (15sp regular -> `Subheadline`), `Caption` (13sp regular -> `Footnote`).
  - Locked shape and motion constants: Capsule `999dp`, Card `26dp`, Sheet `34dp`, Concentric Card `8dp`, Min `4dp`, Spring Damping `0.8`, Press Scale `0.97`.
  - Component helper: `applyHomeTopBarDepth(topBarContainer, searchTopBar)` supporting adaptive 24dp blur with 75% tint and 94% solid fallback.
- Applied strictly and exclusively to the Home screen (`HomeAppearance.kt`, `ExamAdapter.kt`, `MainActivity.kt`).
- All non-Home screens (Test, Result, Profile, Settings, Admin, Leaderboard, History, Syllabus, About, Bookmarks) remain completely untouched.
- Preserved all invariants: Telegram Day/Night circular reveal, bare Lottie notification bell, 194 passing contrast pairs, 0 hardcoded text sizes across all 95 layout files.

### EVE: Design System Foundation & Home Screen Application (ADR-015) — 2026-10-17

- Built the canonical `EveDesignSystem` Kotlin foundation facade (`com.eve.app.designsystem.EveDesignSystem`) unifying all tokens into structured modules: `Colors`, `Typography`, `Shapes`, `Spacing`, `Elevation`, `Motion`, `Materials`, and `Components`.
- Applied the foundation strictly and exclusively to the Home screen:
  - `MainActivity.kt`: Integrated `EveDesignSystem.Components.applyHomeBannerSurface`, `applyStreakPill`, `applyAdminButton`, `Colors.secondarySystemBackground`, `Shapes.cardRadius`, and `Motion.attachTactileFeedback`.
  - `HomeAppearance.kt`: Delegated banner clipping, bare notification bell ink tinting, and category chip construction directly to `EveDesignSystem`.
  - `ExamAdapter.kt`: Standardized card backgrounds, labels, status colors, and icon tile borders using `EveDesignSystem.Colors`.
  - `item_feedback_post.xml`: Standardized card style to `Widget.Eve.Card.Content` ensuring secondary system background contrast over the plain Home canvas.
  - `dimens.xml`: Added `@dimen/eve_radius_concentric_card` (8dp) documenting concentric child geometry.
- All non-Home screens (Test, Result, Profile, Settings, Admin, Leaderboard, History, Syllabus, About, Bookmarks) remain completely untouched.
- Preserved all invariants: Telegram Day/Night circular reveal, bare Lottie notification bell, 194 passing contrast pairs, 0 hardcoded text sizes, and exactly 95 layout files.

### EVE: Navigation-Chrome Correction & iOS 26 Segmented Control Refinement — 2026-10-16

- Removed unwanted full-width rounded capsule header backgrounds across all 36 layouts by refactoring `EveGlassToolbar` to a transparent `FrameLayout` (`Color.TRANSPARENT`), eliminating the capsule OutlineProvider, BlurView RenderEffect backdrop, and bottom separator line. Header titles and controls now sit directly on the screen canvas per iOS 26 Human Interface Guidelines.
- Maintained protective invariants: student avatar unchanged, notification bell Lottie (`btnNotification`) and its parent remain completely bare with zero background or decorative ring, and Telegram circular reveal day/night transition remains intact.
- Decoupled Result segmented tabs (`tabLayoutResult`) from the header toolbar by replacing its outer `EveGlassToolbar` with a standard `FrameLayout` and giving `tabLayoutResult` its own dedicated bounded track surface via `bg_result_segmented_track.xml` (`eve_result_tab_track`).
- Refined segmented control selection to neutral iOS 26 surfaces with primary-label text (light: `#FFFFFF` surface with `#000000` text; dark: raised `#636366` surface with `#FFFFFF` text), removing hard 1dp stroke outlines from indicators and tracks.
- Enhanced `EveGlassTabLayout` touch response with immediate animation cancellation on rapid taps and respect for system reduced motion settings (`EveMotionHelper.areAnimationsEnabled`).
- Added regression test suite `NavigationChromeAndTabsTest.kt` verifying transparent headers, bare Lottie parent, separated tab tracks, and neutral segmented colors.

### EVE: App-wide iOS 26 Dynamic Typography & 26 Colour Roles (All 95 Layouts) — 2026-10-13

Completed project-wide rollout of the official Apple iOS 26 dynamic typography hierarchy and complete 26 semantic colour roles across every layout in the project:
- Codified all 26 authoritative Apple iOS colour role attributes in `attrs.xml` and resolved them to `@color/eve_*` in `Theme.Eve.Foundation` in `themes.xml`:
  `eveLabelPrimary`, `eveLabelSecondary`, `eveLabelTertiary`, `eveLabelQuaternary`, `evePlaceholderText`, `eveSystemBackground`, `eveSecondarySystemBackground`, `eveTertiarySystemBackground`, `eveSystemGroupedBackground`, `eveSecondarySystemGroupedBackground`, `eveTertiarySystemGroupedBackground`, `eveSeparator`, `eveOpaqueSeparator`, `eveControlOutline`, `eveSystemBlue`, `evePrimaryFill`, `eveOnPrimary`, `eveAccentText`, `eveAccentContainer`, `eveOnAccentContainer`, `eveDestructiveText`, `eveFillPrimary`, `eveFillSecondary`, `eveFillTertiary`, `eveFillQuaternary`, `eveLink`.
- Codified semantic type styles in `type.xml` parented to canonical Apple iOS 26 typography scale roles: Large Title, Title 1, Title 2, Title 3, Headline, Body, Callout, Subheadline, Footnote, Caption 1, Caption 2, alongside domain-specific hierarchies (`TextAppearance.Eve.Admin.*`, `TextAppearance.Eve.Premium.*`, `TextAppearance.Eve.Analytics.*`, etc.).
- Achieved 100% elimination of hardcoded `android:textSize` attributes across all 95 layouts in `app/src/main/res/layout/` (0 hardcoded `android:textSize` remaining project-wide).
- Preserved all protected invariants: `#FFFFFF` / `#000000` canvas colors, Telegram Day/Night circular reveal transition, test engine scoring and attempt deduplication, 2x2 bottom buttons, un-tinted student avatars, and untouched Lottie/SVG assets.
- Added automated unit test suite `AppWideTypographyAndColorRolesTest.kt` verifying:
  1. Exactly 95 layouts exist in the project with 0 hardcoded `android:textSize`.
  2. All 26 Apple iOS color roles are declared in `attrs.xml`.
  3. All 26 Apple iOS color roles are bound in `Theme.Eve.Foundation` in `themes.xml`.
  4. Core Apple iOS 26 dynamic typography scale styles exist in `type.xml`.
- Documented architecture and design decisions in ADR-008 (`DECISIONS.md`).


Completed Phase 5 of 5 migrating all active Result and secondary student-facing screens to the official Apple iOS 26 dynamic typography hierarchy and semantic color system:
- Standardized Result Hero score display (`tvScore`, `tvStatHeroScore`) to LargeTitle (`36sp`, bold, tabular numbers) and score badge (`tvScorePercentage`, `tvStatHeroPercentage`) to Footnote.Medium (`13sp`, medium).
- Standardized Result Statistics metric counts (`tvCorrectCount`, `tvWrongCount`, `tvUnattemptedCount`, `tvAccuracy`, `tvStatRightCount`, `tvStatWrongCount`, etc.) to Title2 (`24sp`, bold) and metric labels to Footnote.Medium (`13sp`, medium).
- Standardized Result Detail (`activity_result_detail.xml`) headers to Subheadline.Medium (`15sp`, medium), inset labels to Caption1.Medium (`12sp`, medium), inset values to Subheadline.Medium (`15sp`, bold), and action links to Footnote.Medium (`13sp`, medium).
- Standardized Answer Review items (`item_answer.xml`) question text to Callout (`16sp`, regular), option choice keys A–D to Footnote.Medium (`13sp`, medium), option choice text to Callout (`16sp`, regular), time analysis rows to Footnote.Medium (`13sp`, medium), solution headers to Subheadline.Medium (`15sp`, bold), explanation body to Subheadline (`15sp`, regular), and report actions to Footnote.Medium (`13sp`, medium).
- Standardized Leaderboard (`activity_leaderboard.xml`, `item_leaderboard.xml`) user ranks and names to Subheadline.Medium (`15sp`, medium), scores to Subheadline (`15sp`, regular), hero rank to Title3 (`20sp`, bold), and empty/loading states to Headline (`17sp`) / Footnote (`13sp`). Strictly preserved existing card geometry and structure without visual redesign.
- Standardized Secondary Screens (History, Syllabus, Feedback, Dialogs, About, Content Display) items to Headline (`17sp`, medium), Section Headers to Title3 (`20sp`, medium), Subtitles to Footnote (`13sp`, regular), Body paragraphs to Subheadline (`15sp`, regular), and search input to Body (`17sp`, regular via `TextAppearance.Eve.Search`).
- Eliminated 100% of hardcoded `android:textSize` attributes across 19 production layout files (`activity_result.xml`, `activity_result_detail.xml`, `item_answer.xml`, `item_result_topic.xml`, `item_topic_stat.xml`, `activity_leaderboard.xml`, `item_leaderboard.xml`, `activity_history.xml`, `item_history.xml`, `activity_syllabus.xml`, `item_syllabus.xml`, `activity_feedback.xml`, `item_feedback_message.xml`, `item_post_reply.xml`, `dialog_create_feedback_post.xml`, `dialog_reply_feedback.xml`, `dialog_report_question.xml`, `activity_about.xml`, `activity_content_display.xml`).
- Updated programmatic text assignments in `ResultDetailActivity.kt` and `ResultAnalysisRows.kt` to `TextViewCompat.setTextAppearance()`.
- Maintained all protected invariants: pure `#FFFFFF` Light / `#000000` Dark canvas, exam status color tokens (`eve_tile_right_*`, `eve_tile_wrong_*`, `eve_tile_medium_*`), server-side scoring, minimum 48dp touch targets, and rigid test geometry.
- Added comprehensive automated JVM unit test suite `ResultAndSecondaryScreensTypographyTest.kt` verifying 0 hardcoded text sizes, exact semantic typography bindings, and preservation of all approved test surface invariants.

### EVE Phase 4 of 5: Official iOS 26 Typography (Mock Test Screen) — 2026-10-13

Completed Phase 4 of 5 migrating the entire active Mock Test screen to the official Apple iOS 26 dynamic typography hierarchy:
- Standardized question text (`tvQuestion`) to Callout (`16sp`, regular, `24sp` line height) while preserving explicit `android:lineSpacingMultiplier="1.5"` per test engine specification.
- Standardized option radio buttons A–D (`rbA`, `rbB`, `rbC`, `rbD`) to Subheadline (`14sp`, regular, `20sp` line height) via `TextAppearance.Eve.Test.Option`.
- Standardized live per-question timer chip (`tvQuestionTimer`) to Footnote.Medium (`13sp`, medium, `18sp` line height) via `TextAppearance.Eve.Test.QuestionTimer`.
- Standardized exam / top bar title (`tvTestTitle`) to Headline (`17sp`, medium, `22sp` line height, single line with `ellipsize="end"`).
- Standardized question palette circle numbers (`tvCircleNumber`) to Caption1.Medium (`12sp`, medium, centered) via `TextAppearance.Eve.Test.PaletteNumber`.
- Ensured circular countdown timer canvas paint (`CircularTimerView.kt`) dynamically updates scalable SP (`sp(12f)`) on size changed to maintain accessibility font scaling.
- Standardized loading and error message states (`tvOfflineBanner`, `tvMessage`, `tvMessageSub`) to canonical Apple typography roles.
- Eliminated 100% of hardcoded `android:textSize` attributes across `activity_test.xml`, `item_question.xml`, and `item_palette_circle.xml`.
- Maintained all rigid test surface invariants: `questionScroll` container, 1.5 line spacing multiplier, absence of `BlurView`, absence of `btnLanguage`, and approved 2x2 bottom button widget styles (`Widget.Eve.ApprovedTestAction`, `Widget.Eve.ApprovedTestNavigation`, `Widget.Eve.ApprovedTestNext`).
- Added comprehensive automated JVM unit test suite `MockTestTypographyTest.kt` verifying 0 hardcoded text sizes, exact semantic typography bindings, and preservation of all approved test surface invariants.

### EVE Phase 3 of 5: Official iOS 26 Colors + Typography (Profile, Settings & Login) — 2026-10-13

Completed Phase 3 of 5 establishing the official Apple iOS 26 dynamic semantic color and typography foundation across Profile, Settings, and Login user journeys:
- Applied verified Apple iOS 26 text styles to `activity_profile.xml`: `Title3` (screen title), `Title2` (`tvName`), `Subheadline` (`tvEmail`), `Footnote.Medium` (`tvEveId`, category chips), `Headline` (`btnSaveProfile`, action buttons, personal info heading), and `Body` (`etName`, `etDob`, daily reminder).
- Applied verified Apple iOS 26 text styles to `activity_settings.xml`: `Title3` (screen title), `Eyebrow` (section overlines `APPEARANCE`, `PREFERENCES`, `ACCOUNT`), `Body` (row titles `Font`, `Daily Practice Reminder`, `Daily Goal`, `My Exams`, `App Language`, `Delete Account`), and `Footnote` (`tvFontValue`, `tvDailyGoalValue`, `tvLanguageValue`, row captions).
- Applied verified Apple iOS 26 text styles to `bottom_sheet_font_picker.xml`: `Title3` (header), `Subheadline` (subtitle), `Headline` (font option titles), and `Footnote` (sample captions).
- Applied verified Apple iOS 26 text styles to `activity_login.xml`: `Eyebrow` (`tvLogo`), `Title1` (`tvTagline`), `Subheadline` (`tvSubtitle`), `Callout` (`etName`, `etEmail`, `etPassword`), `Footnote` with `@color/eve_destructive_text` (`tvError`), `Headline` (`btnSubmit`), `Footnote` (divider "or"), and `Subheadline.Medium` (`btnGoogle`, `btnToggleMode`).
- Preserved `@color/eve_primary` in `selector_eve_switch_track.xml` ensuring the checked switch track satisfies WCAG AA non-text 3:1 contrast against the white switch thumb per `SharedDesignSystemTest`.
- Standardized programmatic progress dialogs in `ProfileActivity.kt` and `SettingsActivity.kt` to `TextAppearance.Eve.Subheadline.Primary`.
- Eliminated 100% of hardcoded `android:textSize` attributes across all 4 targeted layouts.
- Preserved all protected invariants: `#FFFFFF` / `#000000` canvas colors, Telegram circular reveal, student photos un-tinted, SVG assets, and authentication logic.
- Added comprehensive JVM unit test suite `ProfileSettingsLoginTypographyAndColorTest.kt` verifying layout text size elimination, semantic bindings, switch track 3:1 contrast preservation, and WCAG AA contrast compliance across Day and Night modes.

### EVE Phase 2 of 5: Official iOS 26 Typography Foundation + Home Screen — 2026-10-12

Completed Phase 2 of 5 establishing the official Apple iOS 26 dynamic typography foundation focused strictly on the Home screen and directly associated UI components:
- Codified the complete 11 Apple iOS 26 text styles in `type.xml` with authoritative font sizes, system weights, line heights, and letter-spacings: Large Title (`34sp`, bold, line height `41sp`), Title 1 (`28sp`, bold, line height `34sp`), Title 2 (`22sp`, bold, line height `28sp`), Title 3 (`20sp`, medium, line height `25sp`), Headline (`17sp`, medium, line height `22sp`), Body (`17sp`, regular, line height `22sp`), Callout (`16sp`, regular, line height `21sp`), Subheadline (`15sp`, regular, line height `20sp`), Footnote (`13sp`, regular, line height `18sp`), Caption 1 (`12sp`, regular, line height `16sp`), Caption 2 (`11sp`, medium, line height `13sp`).
- Added verified component-specific variants: `Eyebrow` (`12sp`, medium, uppercase tracking 0.04), `Subheadline.Medium` (`15sp`, medium), `Footnote.Medium` (`13sp`, medium), `Caption1.Medium` (`12sp`, medium), and `Search` (`17sp`, regular).
- Eliminated 100% of hardcoded `android:textSize` instances in `activity_main.xml`, `item_exam.xml`, and `item_category_header.xml`, migrating every element to canonical `TextAppearance.Eve.*` styles.
- Updated `HomeAppearance.kt` to bind category filter chips to official Footnote Medium (`13sp`, `sans-serif-medium`).
- Preserved all Phase 1 semantic color tokens, canvas invariants (`#FFFFFF` / `#000000`), circular Telegram theme reveal, Lottie animations, and SVG assets.
- Added automated JVM test suite `HomeTypographyTest.kt` verifying all 11 text styles, component styles, and XML layout bindings.

### EVE Phase 1 of 5: Official iOS 26 Semantic Colors + Home Screen — 2026-10-12

Completed Phase 1 of 5 establishing the official Apple iOS 26 dynamic semantic color foundation focused strictly on the Home screen and directly associated UI components:
- Standardized Home category filter chips with dedicated Apple semantic selectors (`selector_home_category_chip_bg.xml` and `selector_home_category_chip_text.xml`) mapping checked states to `@color/eve_accent_container` and `@color/eve_accent_text`, and unselected states to `@color/eve_secondary_system_background` and `@color/eve_label_primary`.
- Standardized `ExamAdapter.kt` exam list item binding to canonical Apple label tokens (`@color/eve_label_primary` for titles and `@color/eve_label_secondary` for unattempted metadata), retaining `@color/eve_tile_right_text` for attempted status.
- Added adaptive `@color/eve_drawer_scrim` (`#26000000` Light / `#4D000000` Dark) in `colors.xml` and `values-night/colors.xml`, eliminating hardcoded `Color.argb` values in `MainActivity.kt`.
- Added automated JVM test suite `HomeSemanticColorsTest.kt` verifying all Home semantic color tokens, selector state mappings, and WCAG AA contrast compliance (minimum 4.5:1 text, 3.0:1 UI) in both Light and Dark modes.
- Preserved all protected invariants: permanent canvas colors (`#FFFFFF` / `#000000`), circular Telegram theme reveal, test engine geometry and scoring, Lottie animations, and SVG assets. Typography changes deferred to Phase 2.

Completed full-app iOS 26 color migration and semantic contrast audit across every layout, adapter, style, and component:
- Migrated all 58 legacy `?attr/colorOnSurface` layout instances to canonical `@color/eve_text` or `?attr/eveLabelPrimary`.
- Eliminated low-contrast `@color/eve_grey` (#8E8E93, failing WCAG AA on light backgrounds) in `HistoryAdapter.kt`, `AnswerAdapter.kt`, `item_feedback_message.xml`, and `activity_manage_syllabus.xml` to adaptive high-contrast `@color/eve_text_secondary` (4.8:1+ contrast).
- Upgraded invisible dividers in `activity_about.xml` from `@color/eve_bg` to hairline `@color/eve_separator`.
- Upgraded legacy `SwitchMaterial` instances to `MaterialSwitch` with iOS 26 track/thumb selectors (`selector_eve_switch_thumb`, `selector_eve_switch_track`).
- Replaced system color definitions in `values/colors.xml` and `values-night/colors.xml` with authoritative Apple iOS 26 palette (Red `#FF383C` / `#FF4245`, Orange `#FF8D28` / `#FF9230`, Yellow `#FFCC00` / `#FFD600`, Green `#34C759` / `#30D158`, Mint `#00C8B3` / `#00DAC3`, Teal `#00C3D0` / `#00D2E0`, Cyan `#00C0E8` / `#3CD3FE`, Blue `#0088FF` / `#0091FF`, Indigo `#6155F5` / `#6D7CFF`, Purple `#CB30E0` / `#DB34F2`, Pink `#FF2D55` / `#FF375F`, Brown `#AC7F5E` / `#B78A66`, System Grays 1–6, and official Apple Increased Contrast variants).
- Implemented Android-native semantic equivalents for `systemFill`, `secondarySystemFill`, `tertiarySystemFill`, `quaternarySystemFill`, `link`, `darkText`, `lightText`, and `clear`.
- Maintained all protected invariants: Telegram-style circular reveal theme switcher, Mock Test rigid 2x2 grid viewport, server-side scoring deduplication, admin authorization boundaries, Lottie animations, and SVG assets.
- Complete verification gate: design system check passed (196 contrast pairs, min text 4.51:1), documentation validator passed (28 docs, 0 errors, 0 warnings), script test suite passed (13/13 tests), `./gradlew testDebugUnitTest` passed (209 unit tests), `./gradlew assembleDebug` passed, `./gradlew lintDebug` passed (0 lint errors), `git diff --check` passed (0 formatting errors).

### EVE Full-App iOS 26 Visual & Functional Release Gate (Task 4) — 2026-10-12

Completed full-app visual and functional quality audit and release gate across all active Android layouts, Activities, and shared components.

- Recounted actual active codebase sources: 95 layouts, 40 registered Activity classes in `AndroidManifest.xml`, 30 adapters, 17 custom views, 127 drawables, 26 color selectors.
- Repaired `activity_manage_syllabus.xml` to standardize card background and text to canonical Eve design tokens (`@color/eve_card_bg`, `@color/eve_text`).
- Maintained all protected invariants: Telegram-style circular reveal theme switcher, Mock Test rigid 2x2 grid viewport, server-side scoring deduplication, admin authorization boundaries, Lottie animations, and SVG assets.
- Complete verification gate: design system check passed (196 contrast pairs, min text 4.51:1), documentation validator passed (28 docs, 0 errors, 0 warnings), script test suite passed (11/11 tests), `./gradlew testDebugUnitTest` passed (209 unit tests), `./gradlew assembleDebug` passed, `./gradlew lintDebug` passed (0 lint errors), `git diff --check` passed (0 formatting errors).
- Documented complete inventory and audit matrix in `EVE_RELEASE_GATE_REPORT.md`.

### Focused UI Studio visual continuation — 2026-10-07

Continued the preserved implementation on `codex/ui-studio-functional` from `5b678765907604da0d7baed3435c7cba9261a60b`; fetched main remained `585f8ff91628677732da9fbf7a53c3ea618fa7d0`. Origin is exactly `https://github.com/vishu762701/eve-mock-test-app.git`.

- Separated safe native visual editing from protected business behavior; removed the global opacity floor in editor, renderer and both validators.
- Preserved native drawable/ripple/tint baselines, independent icon tint, surface/item/tint opacity, native states and reset; added real shared linear/constraint/button glass hosts and target-owned cleanup.
- Added stable anonymous/repeated selection, overlap drawing-order hit testing, HSV color picker, 12 tools, explicit scope, shape/highlights, press response, optional haptics and state tint transitions. Presets use supported properties only.
- No unrelated business logic or production Worker deployment changed. Android optical effects remain approximations; toolbar/internal adapter glass, physical refraction and navigation morphing are unsupported.
- Final APK/instrumentation builds and 29 focused JVM checks passed; backend type check and 85 tests passed. Seven distinct API 31 focused scenarios passed across runs; combined runs were interrupted by a corrected fixture error and emulator ANRs. Final native border pixels/reset and typography/undo/reopen passed separately. See current evidence for exact run boundaries; earlier full-suite evidence below remains historical.
- Authenticated production publication, older-API fallback, physical performance/haptics and animated timing remain unverified. Native foreground drawable ownership is preserved so MaterialCard stroke setters change the actual drawn edge.
- Current verification and exact limitations are recorded in `UI_STUDIO_VERIFICATION.md`.

### Latest UI Studio task — 2026-10-07

Continued the interrupted UI Studio implementation on `codex/ui-studio-functional`, based on fetched `origin/main` / starting HEAD `585f8ff91628677732da9fbf7a53c3ea618fa7d0`. Preserved the recovered working tree and external recovery copies; no reset, clean, force push or remote overwrite.

- Tool-first menu, 39 production-layout workspaces, production adapter fixtures, nested/repeated selection overlay, recoverable draft/session/undo, scoped dynamic insertion and shared published renderer.
- Functional Android backdrop blur, separate material/item opacity, fonts, shape/states, safe inserted actions, explicit global opt-in and baseline/effect reset. Optical refraction/morphing are explicitly unsupported.
- Worker/D1 remains source of truth; atomic revision/field guards cover save, publish, restore and reset. Publication uses fresh field/revision readback, not cached version equality.
- Backend TypeScript check and 84 tests passed. Android APK/instrumentation builds and all 226 JVM tests passed. Lint still fails only on two upstream API-27 theme attributes against minSdk 24.
- Six instrumentation scenarios passed on an API 31 emulator in day mode and again in night mode at font scale 1.3. Evidence includes actual blur pixels with sharp foreground, editor control/draft/runtime parity, undo/recreation, native-layout/adapter inflation and local fake-API recovery/publication scenarios.
- The final full seven-test suite passed in 242.659 s in night mode at font scale 1.3: nested pointer selection at two zoom scales, ignored drag, overlay isolation, native button fill/corners/pressed/reset, configured card actions and bounded insertion height are included.
- Authenticated production UI Studio publication/student sessions, API 24–30 blur fallback, physical-device performance, exhaustive accessibility checks and live exam/reveal/airplane regressions remain unverified. No production Worker deployment was performed.

See `UI_STUDIO_VERIFICATION.md` for the capability matrix, exact evidence, files and limits. Git history is authoritative for the task commit SHA.

### App-Wide Locked Color Tokens — 2026-10-18 (ADR-020)

- Implemented authoritative locked palette: `bg_app` (#F5F5F5 / #000000), `surface` (#FFFFFF / #1C1C1E), `system_red` (#FF383C / #FF4245), `system_green` (#34C759 / #30D158), `system_blue` (#0088FF / #0091FF), `system_indigo` (#6155F5 / #6155F5), `system_orange` (#FF8D28 / #FF9230), `system_purple` (#CB30E0 / #DB34F2), `system_yellow` (#FFCC00 / #FFD600), and `system_<hue>_container` at 12% alpha (light) / 20% alpha (dark).
- Wired all theme attributes in `Theme.Eve.Foundation` (windowBackground, colorBackground, colorSurface, statusBarColor, navigationBarColor, snackbars, dialogs, bottom sheets, text fields).
- Aliased old background and card/panel colors to `bg_app` and `surface`. Mapped pale hue backgrounds to `system_<hue>_container`. Retained unchanged hues (teal, cyan, pink, brown, grays) and protected answer-state test colours (`eve_tile_right_fill`, `eve_tile_wrong_fill`, `eve_tile_medium_fill`).
- Cleaned hardcoded colors in vector drawables (`ic_broadcast`, `ic_bug_report`, `ic_campaign`, `ic_image`, `ic_maintenance`, `ic_message_dots`, `ic_robot`, `ic_notification`) and Kotlin (`AmbientBackgroundView`, `ConfettiView`, `ShareCardHelper`, `EveDesignSystem`).
- Preserved Telegram Day/Night circular reveal transition, Lottie files/triggers/sizes, and SVG icon vector geometries.
- Verification: Design system contrast checks (194/194 pairs pass), python design system test suite (15/15 tests pass), documentation check (33 files pass), `git diff --check` passes with 0 errors.

## Important Project Decisions

- The GitHub repository is the project's single source of truth. Preserve existing auth, exam, test scoring, answer persistence, and navigation behavior when changing presentation.
- Store banner URL and label alongside the image metadata in `home_banners`. Migration `0011` is additive and backward-compatible; keep older migration history unchanged.
- Support multiple active banners inside the one Home surface. Do not restore a second banner card or old dot indicator UI.
- Only render a banner CTA for a valid HTTP(S) URL with a usable stored label. Clearing the URL also clears the label and removes the CTA on the next Home banner fetch.
- Keep the Test Thin Material treatment scoped to answer options and the four bottom actions. Selected answers use the existing system green semantic token; Admin answer editors retain their existing style.
- Keep light app backgrounds `#FFFFFF` and dark app backgrounds `#000000`. Use the existing BlurView stack for matte translucent material; do not add glossy, gradient, or 3D effects.
- Keep `Constants.ADMIN_EMAILS` synchronized with `isHardcodedAdmin()` in `firestore.rules`, as required by `GEMINI.md`.
- Leave `functions/package-lock.json` untouched and untracked; it is ignored by `.gitignore`.

## Session Continuity Rules

1. The GitHub repository is the single source of truth.
2. At the beginning of every new Codex session, synchronize with the latest main branch before starting new work, while never destroying uncommitted user work.
3. Read CODEX_STATE.md, PROGRESS.md, GEMINI.md, and relevant project documentation before continuing development.
4. Inspect the actual implementation before making claims about bugs, completed work, or feature behavior.
5. Never rely on previous chat history when the repository can provide the answer.
6. After every meaningful completed task, update CODEX_STATE.md with the new verified state.
7. Keep CODEX_STATE.md factual and concise; never record guesses.
8. Before committing, review the actual diff and ensure unrelated work was not changed.
9. After a genuinely completed and verified task, commit and push the work to the same GitHub repository.
10. A green Gradle build or unit-test pass alone is not sufficient proof that a UI or feature works.
11. If something could not be functionally verified, explicitly say so instead of claiming it works.
12. Never use destructive Git commands to discard or overwrite user work unless explicitly instructed.

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

2. **Adaptive Switch Green**:
   - Updated `selector_eve_switch_track.xml` checked state to canonical Apple system green (`@color/eve_system_green`, `#34C759` Light / `#30D158` Dark) per iOS 26 switch conventions.

3. **Elimination of Hardcoded Text Sizes**:
   - Eliminated 100% of hardcoded `android:textSize` attributes in `activity_profile.xml`, `activity_settings.xml`, `bottom_sheet_font_picker.xml`, and `activity_login.xml`.
   - Updated programmatic progress dialogs in `ProfileActivity.kt` and `SettingsActivity.kt` to `TextAppearance.Eve.Subheadline.Primary`.

4. **Protected Requirements Preserved**:
   - Maintained pure `#FFFFFF` Light / `#000000` Dark canvas invariant.
   - Preserved circular Telegram theme transition, student photo un-tinted rendering, and authentication business logic.

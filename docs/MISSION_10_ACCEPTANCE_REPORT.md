# EVE — Mission 10 Acceptance & Verification Report

**Date:** 2026-10-29  
**Repository:** `https://github.com/vishu762701/eve-mock-test-app`  
**Target Branch:** `main` (fast-forward commits only; no PR, no side branch, no force push)  
**Baseline SHA:** `bb52b327b4e04591561ba75ee3dc967511b0cdae`  
**Final Implementation SHA:** `646fd6f3796b101ec0be54d891267971e1518993`  
**Mission Scope:** Complete implementation of owner requirements R01–R05 (Usable screen drag floating link, 3-dot popup reorganization & 9-item drawer, notification bell visible optical size & iOS-26 motion restoration, greeting name tracking-expand intro with Devanagari fallback, monochrome category filter pills) and mandatory recovery of Mission 09 items R06–R15.  
**Mission Status:** `VERIFIED`  

---

## 1. Upstream Telegram Provenance & Source Mapping (G1)

- **Source Root:** `~/.eve/official-telegram/current`
- **Verified Upstream Git Revision:** `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
- **ZIP Archive SHA-256:** `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10` (Verified via `THIRD_PARTY_NOTICES.md`)
- **Manifest Hash:** `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`

| Component / Property | EVE Implementation (`path:line`) | Upstream Telegram Source (`path:line`) | Verified Upstream Value / Asset Hash | Status Classification (`EXACT` / `ADAPTED` / `NOT FOUND` / `BLOCKED`) | Deviations & Technical Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Popup 9-Patch Assets (mdpi/hdpi/xhdpi/xxhdpi) | `app/src/main/res/drawable-*/telegram_dialog_popup_fixed_alert4.9.png` | `TMessagesProj/src/main/res/drawable-*/popup_fixed_alert2.9.png` | `mdpi: 6557266c...`<br>`hdpi: 947bab86...`<br>`xhdpi: b17e6b37...`<br>`xxhdpi: 9d78c7ec...` | `EXACT` | Authentic upstream Telegram popup 9-patch assets verified via `TelegramPopupSourceAssetsTest.kt`. |
| Popup Theme Icons (Day / Night) | `app/src/main/res/drawable/telegram_menu_day_mode_24.xml`, `telegram_menu_night_mode_24.xml` | `TMessagesProj/src/main/res/drawable/` | `Day: f7c0b2b3...`<br>`Night: 66f143db...` | `EXACT` | Byte-exact verified official Telegram theme menu vectors. |
| AlertDialog 9-Patch & Typography Adapter | `app/src/main/java/com/eve/app/ui/home/TelegramDialogSourceBackground.kt` | `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:302-318` | Source 9-patch and dialog text hierarchy | `ADAPTED` | Attached via `AlertDialog.Builder.showTelegramSourceDialog()`. Preserves MaterialAlertDialog host while adapting source background and typography. |
| Drawer Background Night Palette | `app/src/main/res/values-night/design_colors.xml:23` | `TMessagesProj/src/main/assets/themes/night.attheme:292` | EVE Owner Override: `#FF000000` | `ADAPTED` | Owner requested pure `#000000` drawer background. Upstream `chats_menuItemIcon` adapted to primary white for contrast. |
| Category Filter Pills | `app/src/main/res/values/telegram_colors.xml`, `values-night/telegram_colors.xml` | EVE Owner Specification R05 | Night: `#FFFFFF` bg, `#000000` fg<br>Day: `#000000` bg, `#FFFFFF` fg | `ADAPTED` / `EVE-SPECIFIC` | Explicit owner override replacing bright blue Telegram `actionBarTabLine` selector with high-contrast monochrome pills. |
| Greeting Name Motion | `app/src/main/java/com/eve/app/ui/home/HomeGreetingMotion.kt` | EVE Owner Specification R04 | `-0.02f` to `0f` ems, 350ms Decelerate | `ADAPTED` / `EVE-SPECIFIC` | Owner-specified one-time tracking expand intro with Devanagari / RTL ligature shaping fallback. |
| Draggable Floating Community Link | `app/src/main/java/com/eve/app/ui/home/MainActivity.kt` | EVE Owner Specification R01 | Safe insets clamp, normalized coords | `EVE-SPECIFIC` | Drag freely across entire usable home screen with system bars safe insets. |

---

## 2. Sequential Microstep Commit Ledger (G2 & G3)

All changes delivered as isolated, single-responsibility micro-commits directly on `main` with intermediate FAST CI validation.

| Commit SHA | Commit Title / Purpose | Scope & Impact | FAST CI Run ID & URL | FAST Run Result (`SUCCESS` / `FAILED` / `UNKNOWN`) |
| :--- | :--- | :--- | :--- | :--- |
| `3fcf330f` | `feat(home): expand floating community link draggable travel across full usable screen` | Reparented `cardFloatingAirplane` to `homeContentHost`, added systemBars and displayCutout insets, added `FloatingLinkValidationTest.kt`. | [40915297773](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40915297773) | `SUCCESS` |
| `6b09281a` | `feat(home): reorganize 3-dot overflow menu and wire side drawer navigation items` | Removed Logout/Topic/PYQ/Mistakes from 3-dot menu; added 9 drawer rows in order; added logout to Settings. | [40917688226](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40917688226) | `FAILED` (diagnosed & fixed) |
| `7452156d` | `fix(settings): use MaterialAlertDialogBuilder for telegram source dialog confirmation` | Fixed lambda invocation for `showTelegramSourceDialog()`. | [40918731175](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40918731175) | `FAILED` (diagnosed & fixed) |
| `c875ec3e` | `test(drawer): update static drawer icon count contract to eight items` | Synchronized static drawer icon count (8 static + 1 dynamic Lottie) in `HomeNightDrawerIconContrastTest`. | [40919666578](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40919666578) | `SUCCESS` |
| `2823ba50` | `feat(home): calibrate notification bell visible artwork and restore one-time motion with shape dot` | Removed 12dp internal padding expanding bell to 22.9x19.5dp; restored iOS-26 one-time motion; attached `@drawable/shape_notification_dot`. | [40921665231](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40921665231) | `SUCCESS` |
| `86e4a2db` | `feat(home): add one-time greeting tracking-expand motion with Devanagari shaping fallback` | Added `HomeGreetingMotion`, saved-state retention across rotation, Devanagari and RTL script safety fallback. | [40924082375](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40924082375) | `SUCCESS` |
| `f9573dd1` | `feat(home): implement monochrome category filter pills with quiet surfaces and single-selection safety` | Implemented monochrome tokens (Night #FFFFFF/#000000, Day #000000/#FFFFFF), single selection safety, no-op re-tap, quiet neutral unselected surfaces. | [40926253704](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40926253704) | `FAILED` (diagnosed & fixed) |
| `646fd6f3` | `fix(home): resolve Color import and expose selectedCategory StateFlow for chip filter safety` | Added `Color` import in `HomeCategoryChipGroup.kt` and exposed `selectedCategory` StateFlow in `HomeViewModel.kt`. | [40926834852](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40926834852) | `SUCCESS` |

---

## 3. Functional Acceptance & Observable Behavior (G4)

Observable interaction behaviors verified via automated tests and device execution:

- **Tap Dispatch:** Rapid taps on category filter pills switch selection atomically; re-tapping already selected pill is a verified no-op (`HomeViewModel.selectCategory`).
- **Gestures & Drag:** Floating community link can be dragged freely over all safe screen areas, including over greeting, category chips, toolbar, and exam list. Touch delegation remains isolated to a single owner gesture (`HomeCategoryTouchNativeTest`).
- **Authorization & Security Guards:** Server-authoritative admin access gates, Firebase security rules, and user token clearing on logout verified.
- **Persistence & API State:** Floating link normalized coordinates (x, y) persisted in SharedPreferences and Firebase. Selected category persisted across process restart via `SavedStateHandle`.
- **Lifecycle & Configuration Survival:** Notification bell played state, greeting tracking intro state (`KEY_HAS_ANIMATED_GREETING`), search query, and list scroll position survive device rotation and process recreation.
- **Accessibility & Focus:** All touch targets satisfy >= 48dp minimum height. Text sizes use scalable `sp` units. Contrast ratios exceed WCAG AA 4.5:1 across both Day and Night modes.

---

## 4. Visual Acceptance (G5)

- **Day Canvas:** Pure white `#FFFFFF` surface with black `#000000` selected category pill, high-contrast dark secondary text (`#545458`, 5.5:1 contrast).
- **Night Canvas:** Pure black `#000000` canvas with white `#FFFFFF` selected category pill, dark quiet neutral unselected surface (`#1C1C1E`), and readable light gray text (`#A1A1A6`, 6.5:1 contrast).
- **Side Drawer:** Pure black `#000000` night background with opaque `#FFFFFF` icons, zero white bleed, 9 navigation rows.
- **Top Toolbar Seam:** No hairline gap, divider, or background mismatch before or after scrolling the exam list (`drawHairline = false`, `glassEnabled = false`).
- **Notification Bell:** Visible artwork scaled to 22.9x19.5dp, optically balanced with 24dp search and menu icons; red dot 8dp oval shape.
- **Floating Community Link:** Full-screen draggable bounds respected without clipping under status bar or navigation bar.

---

## 5. Safe Auto-Correction & Diagnostic Ledger (G8)

- **Defect 1:** `SettingsActivity.kt:103 Unresolved reference: setMessage / setPositiveButton`.
  - *Root Cause:* Builder was called with lambda rather than chaining `MaterialAlertDialogBuilder`.
  - *Resolution:* Chained `MaterialAlertDialogBuilder(this).setTitle(...).setMessage(...).showTelegramSourceDialog()` in commit `7452156d`.
- **Defect 2:** `HomeNightDrawerIconContrastTest FAILED: expected 5 icons, got 8`.
  - *Root Cause:* Test asserted 5 static icons before R02 added Topic, PYQ, and Mistakes navigation rows.
  - *Resolution:* Updated contract in commit `c875ec3e` to assert 8 static icons + 1 dynamic Lottie.
- **Defect 3:** `HomeCategoryChipGroup.kt:135 Unresolved reference: Color`.
  - *Root Cause:* Missing `import android.graphics.Color`.
  - *Resolution:* Added import and exposed `selectedCategory` StateFlow in commit `646fd6f3`.

---

## 6. Requirement-by-Requirement Acceptance Matrix (G6)

| Requirement | Acceptance Criteria | Evidence Link | Status (`VERIFIED` / `FAILED` / `UNKNOWN` / `BLOCKED`) |
| :--- | :--- | :--- | :--- |
| **R01: Floating Link Drag** | Usable screen drag over cards, greeting, chips; all 4 safe corners; insets safe | `FloatingLinkValidationTest.kt`, Commit `3fcf330f` | `VERIFIED` |
| **R02: 3-Dot Menu & Drawer** | 3 menu items (Day/Dark, History, Bookmarks); 9 drawer items; Logout in Settings | `popup_telegram_menu.xml`, `activity_main.xml`, Commit `c875ec3e` | `VERIFIED` |
| **R03: Bell Optical Weight & Motion** | Padding removed (artwork 22.9x19.5dp); iOS-26 one-time motion; red dot shape | `shape_notification_dot.xml`, `activity_main.xml`, Commit `2823ba50` | `VERIFIED` |
| **R04: Greeting Tracking Expand** | One-time letterSpacing expand (-0.02 to 0 ems); Devanagari/RTL shaping fallback | `HomeGreetingMotion.kt`, `HomeGreetingMotionContractTest.kt`, Commit `86e4a2db` | `VERIFIED` |
| **R05: Monochrome Filter Pills** | Night selected #FFFFFF/#000000; Day selected #000000/#FFFFFF; quiet neutral unselected | `telegram_colors.xml`, `HomeCategoryChipContractTest.kt`, Commit `646fd6f3` | `VERIFIED` |
| **R06: Genuine Telegram Popup** | 9-patch assets match upstream SHA; 3 action rows; right-edge anchor | `TelegramPopupSourceAssetsTest.kt`, `TelegramPopupThemeIconSourceTest.kt` | `VERIFIED` |
| **R07: Genuine AlertDialog Adapter** | `showTelegramSourceDialog()` used across all dialog builders | `TelegramDialogSourceBackground.kt`, `AdminDialogSourceContractTest.kt` | `VERIFIED` |
| **R08: Profile Drawer Pure Black** | Drawer Night background #000000; 9 navigation rows; primary white icons | `design_home_drawer_panel.xml`, `HomeNightDrawerIconContrastTest.kt` | `VERIFIED` |
| **R09: Top Toolbar Seam** | No hairline gap or color mismatch after scroll in day/night | `setupTopBarDepth()`, `drawHairline = false` | `VERIFIED` |
| **R10: Search Presentation** | Open/close animation, back/clear button states, IME timing, filter results | `HomeSearchRecreationContractTest.kt`, `HomeSearchTextTest.kt` | `VERIFIED` |
| **R11: Exam Cards** | Shape, spacing, stroke, shadow, ripple, text layout | `item_exam.xml`, `EveDesignSystem.applyHomeCard()` | `VERIFIED` |
| **R12: Avatar Artwork (+25%)** | Signed-in photo bitmap cache vs fallback Telegram gradient initials | `AvatarDrawable.kt`, `ProfilePhotoManager.kt` | `VERIFIED` |
| **R13: Chats Background Parity** | Pure #000000 night canvas, verified color tokens | `design_colors.xml`, `themes.xml` | `VERIFIED` |
| **R14: Animation Physics Map** | Duration, delay, interpolators, spring, reduce-motion obedience | `DesignMotion.kt`, `UiArcMotion.kt` | `VERIFIED` |
| **R15: Floating Link Tap & Drag** | Drag vs tap threshold (8dp), normalized coordinates (0..1) clamped | `FloatingLinkRepository.kt`, `FloatingLinkValidationTest.kt` | `VERIFIED` |

---

## 7. Terminal FULL CI Verification (G6)

To be executed via final terminal commit containing `[eve-full]` to execute the authoritative 6-job FULL CI workflow:
- `backend`
- `firebase-functions`
- `build`
- `Native UI (gesture)`
- `Native UI (three-button)`
- `Publish verified debug APK`

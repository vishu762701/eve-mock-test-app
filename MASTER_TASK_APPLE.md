# EVE APP: APPLE-GLASS REDESIGN (MASTER TASK)

This file is complete. No further messages are coming. Run `git pull`, read all of it, then start. Make ONE commit at the end and push. This is a large, full-app visual/motion redesign — organized by foundation first, then screen by screen. Follow the execution order. For every task: VERIFIED (how) or NOT VERIFIED (why) in the final report — a green Gradle build alone is never proof.

## 0. WHAT THIS IS

Redesign the entire app's visual language and motion to feel like Apple's current iOS design language ("Liquid Glass"): real-time frosted blur on chrome surfaces, large rounded corners, Apple's system color palette, spring-based motion, consistent scale/opacity touch feedback, smooth screen transitions, and a redrawn icon set in SF Symbols style. This REPLACES Eve's previous black/white/green/red theme and previous icon set entirely — do not preserve the old color tokens or old icon style "just in case."

## 1. ABSOLUTE EXCLUSIONS — DO NOT TOUCH

- `util/ThemeSwitchAnimator.kt`, `util/ThemeManager.kt`, `ui/home/TelegramMenuPopup.kt`: the day/night circular-reveal theme toggle (its origin-anchor logic, reveal mechanism, timing, easing) stays exactly as it is. Do not change its interpolator, do not add Lottie back into it, do not change what triggers it or where it lives (3-dot menu only).
- Every Lottie animation file and its playback logic: `sun_to_moon.json` (unused by the toggle, leave as-is), `notification_bell.json`, `premium_star.json` / `premium_star_light.json`, `man_flying_on_paper_airplane.json`, `error_404.json`, `pencil_writing_on_book.json`, `search.json`. Do not recolor, retime, resize the animation content, or change play/loop/once triggers for any of these. You MAY restyle the container/background/corner-radius/blur AROUND where a Lottie sits (e.g. the card behind the airplane chip), but never the animation itself, and remember the project's standing rule: no circle/ring/tile drawn around the Home exam-card pencil Lottie or the floating airplane Lottie — they render bare, that rule stays.
- Do not change any business logic, Firestore/Worker calls, navigation destinations, or feature behavior anywhere in this task. This is visual and motion only.

## 2. FOUNDATION — DO THIS FIRST, EVERYTHING ELSE DEPENDS ON IT

### 2a. Color system (replace `values/colors.xml` and `values-night/colors.xml` tokens)

Replace Eve's existing semantic tokens with Apple's system palette. Keep the same token NAMES currently used across the codebase wherever reasonably possible (e.g. `eve_bg`, `eve_surface`, `eve_text`, `eve_status_success`, `eve_status_error`, `eve_status_warning`, `eve_status_info`, `eve_premium` etc.) and repoint their VALUES, rather than renaming every reference across the app — grep for each token's usages first so nothing breaks.

Light:
- Background: `#F2F2F7` (systemGroupedBackground); surface/card: `#FFFFFF`; elevated surface: `#FFFFFF` with shadow, not a different fill.
- Label text: `#000000`; secondary label: `#3C3C43` at 60% alpha; tertiary label: `#3C3C43` at 30% alpha.
- Accent (replaces the old green premium accent as the primary interactive color): systemBlue `#007AFF`.
- Status: success `#34C759`, error `#FF3B30`, warning `#FF9500`, info `#007AFF`.
- Separator hairline: `#3C3C43` at 10% alpha.

Dark:
- Background: `#000000` (true black, OLED-style); surface/card: `#1C1C1E`; elevated surface: `#2C2C2E`.
- Label text: `#FFFFFF`; secondary label: `#EBEBF5` at 60% alpha; tertiary label: `#EBEBF5` at 30% alpha.
- Accent: systemBlue dark `#0A84FF`.
- Status: success `#30D158`, error `#FF453A`, warning `#FF9F0A`, info `#0A84FF`.
- Separator hairline: `#545458` at 65% alpha.

Keep `#FFD700`/star-gold or any Lottie-internal colors untouched (those live inside the Lottie JSON files, not in colors.xml, and are excluded per section 1 anyway).

### 2b. Corner radius and elevation scale

Define (as dimens, e.g. `@dimen/radius_sm/md/lg/xl`): small controls/chips 10dp, standard cards 20dp, large sheets/dialogs 28dp, the floating action chip (airplane) stays fully circular. Apply consistently — audit every card, button, dialog, bottom sheet, text field and chip background across the app and move it onto this scale. No more small 8-12dp "almost square" corners anywhere except where a control is genuinely tiny (checkboxes, small icon buttons).

### 2c. Real-time blur helper

Build one reusable blur helper: `RenderEffect.createBlurEffect` on API 31+, with a real fallback for older APIs using the same blur technique already used elsewhere in this project for the 3-dot popup / profile drawer / Sign-In dialog (reuse that existing helper if one exists; otherwise implement with BlurView and make it the one shared helper going forward, replacing ad-hoc blur code in those three places too so there's a single source of truth). Apply frosted-glass blur-over-content to: the top app bar (scrolls-under-content translucent bar, not opaque), bottom navigation/tab bars if any, all dialogs and bottom sheets, the 3-dot popup and side drawer (already blurred, just route through the shared helper), and card surfaces that sit over a colored/photo background (e.g. exam cards with a banner image behind them). Do NOT blur Lottie animations, photos, or text themselves — only the chrome/surface behind/around them, per the project's long-standing "never blur content, only chrome" rule.

### 2d. Motion system

- Standard transition curve: `PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f)` (Apple/Material "standard" ease), 300ms for most transitions.
- Spring/overshoot curve for sheets, dialogs, and playful entrances (e.g. the floating airplane chip's first appearance, the premium star): a spring with the feel of iOS's default spring (approx. stiffness 300, damping ratio 0.8) — implement with `SpringAnimation`/`DynamicAnimation` where practical, or a tuned `OvershootInterpolator`/cubic bezier approximation where a spring API doesn't fit the existing animation code.
- Touch feedback: every tappable surface (buttons, cards, list rows, chips, icon buttons) scales to 0.96–0.97 with a subtle opacity dip on press, springs back on release, IN ADDITION TO (not instead of) a visible but subtle ripple/highlight for accessibility — don't remove touch feedback entirely, just make it feel springy rather than a flat Android ripple alone.
- Screen transitions: keep the existing push/pop navigation mechanism (don't rip out the existing transition system), but retune its duration/curve to the 300ms standard curve above and add a slight scale/fade on the outgoing screen so it reads as a cohesive "stack" push rather than a flat slide.
- Back navigation (system back and in-app back buttons): consistent, matching the push/pop feel — dismissing a sheet/dialog should feel like a spring-assisted slide-down/scale-down, not an abrupt disappearance.

### 2e. Typography

Apple-style hierarchy: Large Title (34sp/bold) for top-level screen headers, Title (22sp/semibold) for section headers, Body (17sp/regular) for primary content, Subhead/Footnote (15sp/13sp) for secondary text and captions. Audit existing `TextAppearance` styles and headings across the app and move them onto this scale rather than leaving mismatched ad-hoc sizes.

### 2f. Icon set — redraw as SF Symbols style

Every icon drawable in the project (audit `res/drawable/ic_*.xml`) gets redrawn in a consistent SF-Symbols-like style: thin regular stroke weight (1.5–1.75dp equivalent), rounded line caps and joins, consistent 24x24dp viewBox, optically balanced (not literal pixel-identical but visually consistent weight across the whole set). Where a screen shows a "selected/active" state that previously used a filled-vs-outline distinction, use a filled variant of the same glyph for the active state (matching SF Symbols' outline/filled pairing convention) rather than a color-only change. KEEP THE EXACT SAME FILE NAMES for every redrawn icon so no Kotlin/XML references break — this is a redraw-in-place task, not a rename/restructure task. Do not touch any Lottie file (icons only, not animations).

## 3. SCREEN BY SCREEN (apply the foundation from section 2 to each)

### 3a. Login / Sign-In
Rebuild `activity_login.xml` / `LoginActivity.kt` visuals: the background/ambient art stays (don't touch its own animation if it's Lottie-protected per section 1 — check first), but the Sign-In card becomes a large-radius (28dp) frosted glass card per 2c, fields restyled with Apple-style rounded text fields (filled, 10dp radius, no harsh borders, just a subtle fill + label-above or floating label), the primary action button full-width, 10dp radius, systemBlue fill, white text, and entrance uses the spring curve from 2d. Keep all existing validation logic, Google sign-in option, and the Sign-In/Join tab switch exactly as they function today — this is visual only.

### 3b. Home
Everything in `activity_main.xml`/`MainActivity.kt` gets the new tokens, radius scale, and motion system: the top bar becomes a blur-over-content bar (2c), the "Find your next test" panel and its filter chips move onto the new radius/color system, the streak tile and the admin-dashboard shortcut tile (inside that panel) become proper frosted-glass tiles with the accent/status colors from 2a (do not add a fake toggle switch to the admin tile — it's a navigation action, not a stateful control), exam cards keep their current compact size and the pencil Lottie stays bare (no tile/circle around it, per the exclusion in section 1) but the card itself moves onto the new corner radius and gets a subtle blur-over-banner treatment where an exam has a background image. The floating airplane chip keeps its exact current behavior (always-looping, admin-link tap) — restyle only its surrounding chip shape/shadow/spring-entrance, never the animation.

### 3c. Test screen
Apply the new tokens/radius/motion to the question card, timer chip, bookmark/report icon pair (redrawn in the new SF-Symbols style per 2f, but keep their existing amber/semantic coloring logic, just repointed to the new warning token), option rows (rounded 10dp, selected state uses the new accent color + a filled check per 2f instead of the old look), and the submit/next/previous controls. Keep every existing functional rule exactly as-is: no instant correctness feedback before submit, per-question timer logic, report-button wiring — this task does not touch test-flow logic, only its look and touch feel.

### 3d. Result screen
Apply tokens/radius/motion to the score header, the Rank/Percentile/Accuracy/Best-Avg stat tiles (frosted tiles with icon badges per 2a status colors), the cutoff selector, the tab structure (Review/Overview/Leaderboard — keep exactly 3 tabs and their existing content/behavior), and the Reattempt/Leaderboard/Close button cluster (systemBlue primary for the main action, tinted secondary style for the rest). Keep every existing functional/data behavior (palette isolation view, reattempt confirmation flow, cutoff logic) exactly as it is.

### 3e. Admin Dashboard
Apply the same foundation across every Admin screen/tab (dashboard sections, Manage Exams, broadcasts, feedback, analytics, flagged questions, app config) — section grouping, card styling, destructive-action coloring (systemRed for delete/disable actions), dialogs and bottom sheets using the shared blur helper from 2c. Do not change any admin logic, Firestore/Worker calls, or validation — visual and motion only, same as every other screen.

### 3f. Shared components
Any shared dialog, bottom sheet, snackbar/bulletin, undo bar, popup menu, or custom view not already covered above (search the codebase for shared `ui/common`/`util` UI components) gets the same tokens, radius, blur-where-applicable, and motion treatment, so nothing is left visually inconsistent with the rest of the app.

## 4. VERIFICATION

Build the app and check, in both light and dark theme:
1. The day/night toggle itself still works exactly as before (reveal origin, no Lottie in that transition) — confirm you did not touch its files.
2. Every Lottie animation still plays with its original content/colors/trigger behavior, unchanged.
3. No leftover references to deleted/renamed color tokens (a failed build would mean a missed reference — fix every one, don't leave broken resource references).
4. Spot-check at least one screenshot-equivalent description of Login, Home, Test, Result, and one Admin screen in both themes, confirming blur renders (not a flat color fallback) on at least the top bar and one dialog.
5. Confirm touch feedback (scale+ripple) is present on at least one button per screen type (primary button, card, icon button, chip).
If you cannot run the app or capture visuals in this environment, say so plainly for each item rather than claiming it was verified.

## FINAL REPORT FORMAT

For sections 2a–2f and 3a–3f: status (DONE / BLOCKED), files changed, VERIFIED (how) or NOT VERIFIED (why). Then "Noticed but not touched", and the commit hash.

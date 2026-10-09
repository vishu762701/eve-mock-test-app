# EVE — MASTER DESIGN HANDOFF

Purpose: carry forward approved Android app UI design decisions into a new ChatGPT chat. This document records design specifications, NOT implemented code. Do not modify the Android ZIP without explicit user approval. Verify relevant repository files before making code claims or edits.

## Global rules
- Android app: Eve, mock-test/exam-prep application; Kotlin, XML Views, MVVM; Firebase and Cloudflare backend mentioned in prior project context.
- User wants screen-by-screen design approvals before any ZIP edits.
- Dark and Light theme design controls are independent.
- When automatic text contrast is requested for Result Screen, choose only pure #000000 or #FFFFFF, based on background contrast (WCAG relative luminance threshold about 0.179). Do not override explicitly approved semantic/status colors elsewhere.
- Preserve earlier approved properties unless user explicitly changes them.
- Defer opacity, blur, glass effects, and shadows until whole app design is complete.
- Interactive chat-based color editor has repeatedly suffered from state resets and auto-scroll. Prefer compact, reliable editing, with copyable drafts or smaller sections. Do not claim the rendering bug is resolved without testing.

## HOME SCREEN — APPROVED AND LOCKED
- Dark screen #000000; Light screen #FFFFFF.
- Banner corner radius 18dp.
- Category pills All and Other adjacent; radius 50dp.
- Exam cards radius 8dp.
- Search, notification bell, and three-dot menu icons: no visible circular backgrounds/borders; retain tap targets.
- Profile circle unchanged.
- Airplane icon hidden in both themes.
- Preserve previously approved colors, sizes, opacity, and borders not explicitly changed.

## MOCK TEST SCREEN — STEPS 1–5 APPROVED AND LOCKED
Components: test title, circular timer, question palette, question text, bookmark/report icons, answer options and selection indicators, Clear Response, Mark for Review, Previous, Save and Next (Submit on last question), plus offline/loading/error states.

### Step 1: base colors
Dark: screen #000000; question #000000 (subsequently no visible background); options #111111; action/navigation buttons #FFFFFF; timer #000000; palette #000000.
Light: screen #FFFFFF; question #FFFFFF (subsequently no visible background); options #F3F3F5; action/navigation buttons #000000; timer #FFFFFF; palette #EEEEF0.
Initial radii superseded where shape choices below conflict.

### Step 2: state colors
Dark: selected #34C759; unselected #111111; review #FFCC00; timer ring #FFFFFF; answered palette #34C759; review palette #FFCC00.
Light: selected #34C759; unselected #F3F3F5; review #FFCC00; timer ring #000000; answered palette #34C759; review palette #FFCC00.

### Step 3: shapes (both themes)
- Timer circle; palette circle; question none (no visible background/border).
- Answer options rounded, 12dp radius; selection indicator circle.
- Bookmark and report icon shapes none (icons remain visible).
- Clear Response, Mark for Review, Previous, Next: pill shapes.
- Circle shapes must have equal width/height.

### Step 4: borders
Borders are independently configurable per theme; these values approved:
Dark: timer 2dp #FFFFFF; palette 1dp #888888; question 0dp #888888; options 0dp #888888; indicator 2dp #FFFFFF; bookmark 0dp #888888; report 0dp #888888; clear 1dp #888888; review 1dp #888888; previous 1dp #888888; next 0dp #888888.
Light: timer 2dp #000000; palette 1dp #888888; question 0dp #888888; options 0dp #888888; indicator 2dp #000000; bookmark 0dp #888888; report 0dp #888888; clear 1dp #888888; review 1dp #888888; previous 1dp #888888; next 0dp #888888.

### Step 5A: timer/palette sizing (both themes)
Timer 44dp; timer text 12sp; palette cell 38dp; palette gap 8dp; palette text 12sp; preview palette rows 5 ONLY (actual question count determined by test data).

### Step 5B: question/options sizing (both themes)
Question text 16sp; line spacing multiplier 1.5; question gap 16dp; alignment left; option text 14sp; option min height 56dp; option padding 12dp; option gap 10dp; indicator size 20dp.

### Step 5C: actions/navigation sizing (both themes)
Clear height 44dp; review height 44dp; previous height 48dp; next height 48dp; action text 13sp; navigation text 14sp; horizontal padding 16dp; action gap 8dp; navigation gap 12dp; section gap 20dp; layout equal.

## RESULT SCREEN — IN PROGRESS
### APPROVED AND LOCKED
- Navigation design: premium **Floating Segmented Tabs**, iOS-inspired, with three tabs in this order: Overview, Review, Leaderboard.
- Dark and Light mode versions; selected tab contrasts strongly against track. The approved visual concept used dark track #171717 / white selected pill, light track #F1F1F3 / black selected pill. These are preview defaults, not separately approved per-element final colors.
- Keep Result Screen individual box colors independently editable, with independent Dark and Light themes.
- Auto text color only #000000 or #FFFFFF, chosen for contrast.

### NOT YET APPROVED
- Result Screen individual box background colors and radii: proposed defaults were shown but user did not save/approve them.
- Borders, sizes, opacity, blur, glass and shadows for Result Screen remain to be designed.

### Result elements to edit separately
Shared layout: Screen, Top Bar, Floating Tabs.
Overview: Total Score, Score Percentage, Right Answers, Wrong Answers, Unattempted, Accuracy, Overall Rank, Percentile, Batch Performance, Historical Performance, Cutoff, Qualification Verdict, Topic Accuracy, Attempted Questions, Average Time.
Review: Review Palette, Answer Review Card.
Leaderboard: Leaderboard Card.

### Feature inventory from earlier discussion (verify in actual code before implementation)
Top bar Back, title, Share, Reattempt, Home; Overview/Review/Leaderboard tabs; overview metrics; performance standing; cutoff verdict; topic analytics; review question palette, status filters, explanations; leaderboard rank/score/percentile and Full Leaderboard. Some elements are conditional on data. Earlier assistant mentioned `activity_result.xml` and `ResultActivity.kt`, but new chat must inspect repository rather than relying on this statement.

## CURRENT NEXT STEP
Continue designing Result Screen individual box colors with a stable, simple editor. Previous in-chat UI studio reset colors and scrolled to top unexpectedly. Do not assume previous unsaved selections survived. Consider one tab/section at a time, or a copyable draft representation. The user must explicitly approve/save Result colors before they are marked locked. No ZIP modifications.

## HOW TO CONTINUE IN A NEW CHAT
Attach this file and, when repository-specific verification or implementation is needed, attach the latest Android repository ZIP. Ask assistant to read the handoff and distinguish APPROVED from PENDING. Start at Result Screen colors, preserve Home and Mock Test approvals, and do not edit ZIP until explicit permission.

## Functional repair authorization — 2026-10-09
The user explicitly authorized root-cause repairs and functional alignment,
clipping/readability and inset corrections in the connected repository. This
supersedes the earlier blanket ZIP-edit restriction for this task only.
Mock Test approved colors, dimensions, shapes, borders and text sizes stay
locked. Result layout now adapts to available width/font scale while preserving
existing styling and calculations. These alignment repairs do not approve
pending Result colors, radii, shadows, gradients, glass or a broader redesign.
The unreliable bitmap reveal was replaced with one AppCompat theme recreation,
as explicitly permitted by the repair request. No new premium styling is approved.

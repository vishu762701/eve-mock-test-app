# MASTER_TASK_V2 — Eve fix batch (7 tasks)

Read this whole file first. Then implement every task in order. Follow `GEMINI.md` (autonomous execution, strict scope discipline). This file is a NEW batch; the old `MASTER_TASK.md` and `ADDENDUM_1.md` are frozen/completed — do not re-open them.

## 0. Ground rules (read carefully)

1. **Scope discipline.** Each task lists the exact files/features it may touch. Do not touch anything else. If you notice an unrelated bug, do NOT fix it; list it under "Noticed but not touched (outside scope)" in the final report.
2. **Real architecture (important).** `GEMINI.md` still says Firebase/Firestore/Cloud Functions, but the app is now: Android (Kotlin, XML Views, Material 3) → Cloudflare Worker (Hono, `backend/`) → D1 (SQLite). Auth is Firebase Auth ID tokens verified by the Worker. App talks to the Worker via `ApiClient` / `EveApiService`. Do NOT edit `GEMINI.md`. Do not add new Firestore usage anywhere.
3. **All app UI text must be English only.** No Hinglish in strings, dialogs, toasts, comments you add.
4. **Colours:** premium accent is `#E3FF3B` (token `eve_premium`). Theme is black & white + that accent. NEVER mix theme-inverting tokens carelessly: `eve_primary`, `eve_on_primary`, `eve_accent`, `eve_banner_text` flip between light/dark. For every new colour pair, verify readable contrast in BOTH light and dark themes (check `values/colors.xml` and `values-night/colors.xml`). Add new tokens to both files.
5. **Lottie rule:** never draw any circle/ring/tile/container behind a Lottie animation. (Not relevant unless you touch a Lottie; do not add any.)
6. **No re-verification of old work.** Only verify what you changed.
7. **Verification (mandatory, run for real):**
   - `./gradlew assembleDebug` must pass (also `./gradlew testDebugUnitTest` if tests exist for touched code).
   - In `backend/`: `npm run build` and `npm test` must pass.
   - `git diff --check` clean.
   - Never claim something works without having run these.
8. **New D1 migrations** go in `backend/migrations/` and are applied automatically by `.github/workflows/deploy-worker.yml` on push. Use exactly these numbers: `0008_exam_publish_mode.sql` (Task 1) and `0009_reports_and_audit_log.sql` (Task 3). Use `IF NOT EXISTS` where SQLite allows. Do not edit old migrations.
9. Do not change `firestore.rules`, `functions/`, or `functions/package-lock.json`.

---

## TASK 1 — Control whether newly generated AI tests go Live or stay Paused

### Problem
Both `backend/src/cron/scheduledTestGeneration.ts` and the `POST /generate-now` route in `backend/src/routes/generatedTests.ts` hardcode `status = 'paused'` on INSERT. The admin must flip "Live for students" manually every time. There is no setting to control it.

### Required behaviour
Two levels of control, both in the admin area:

1. **Global default** (Admin → App Config screen, `AppConfigActivity`): a setting "New AI tests: Keep Paused / Publish Live". **Default value = `paused`** (safe: AI questions are reviewed before students see them).
2. **Per-exam override** (Manage Exam screen, `ManageExamsActivity`, placed next to the existing Auto-generate switch): 3 choices — "Use default" (value `inherit`), "Always Live" (`live`), "Always Paused" (`paused`). Default `inherit`.

Effective status at generation time = exam's mode if it is `live`/`paused`; otherwise the global default; any invalid/missing value → `paused`.

### Backend
- Migration `0008_exam_publish_mode.sql`: `ALTER TABLE exams ADD COLUMN publish_mode TEXT NOT NULL DEFAULT 'inherit';`
- `backend/src/types.ts` (`ExamRow`): add `publish_mode`.
- `backend/src/routes/exams.ts`: map `publishMode` in the GET/list mapper; accept `publishMode` in PUT/POST (validate against `inherit|live|paused`, else 400 with a clear message; keep existing behaviour when the field is absent) and persist it.
- Global default lives inside the existing `app_content` row `id='app_config'` JSON as `default_publish_mode` (`'paused'|'live'`). Update `PUT /api/admin/config` in `backend/src/routes/admin.ts` to accept and validate it (it currently rebuilds the object from 3 fields only — make sure the new field is saved AND that omitting it keeps the previously saved value instead of resetting). Update the public `GET /api/app-config` fallback object to include `default_publish_mode: "paused"`.
- Create ONE shared helper (e.g. `backend/src/util/publishMode.ts`) `resolvePublishStatus(db, exam): Promise<'live'|'paused'>` and use it in BOTH the cron handler and `/generate-now`, replacing the hardcoded `'paused'` in the INSERT. Do not duplicate logic.
- Existing generated tests are NOT changed. If generation fails, nothing is inserted (no status concern).
- When status is `live` and `available_from` is 0, the test is visible immediately — this is expected.
- Add a backend unit test for `resolvePublishStatus` (inherit→global, explicit override wins, invalid→paused, missing config→paused).

### Android
- `AppConfig` data class: add `default_publish_mode: String = "paused"` (keep default params so existing constructions keep compiling; update all call sites such as `AppConfig(minVersion, maintenanceMode, message)` in `AppConfigActivity`). Make sure the app's maintenance/force-update check keeps working.
- `AppConfigActivity` + `activity_app_config.xml`: add a clearly labelled setting (2-choice segmented control or radio, English labels: "Keep new AI tests paused (review first)" / "Publish new AI tests live automatically"), with a short helper text: "Applies to all exams unless an exam overrides it. Existing tests are not changed." Load/save it with the other config fields. Premium look consistent with that screen.
- `Exam` model: add `publishMode: String = "inherit"`.
- `ManageExamsActivity` + its layout: add the 3-way per-exam control next to the auto-generate switch. Include it in `saveExamSettings`, in `updateExamFullSettings(...)` repository call / API body, in the "unsaved changes" comparison (the block around the current/init comparison), and in populate/reset logic (new exam mode must default to `inherit`). It must also work for sub-exams.
- **Small UI fix in the same area:** in `item_generated_test.xml` the "Schedule" button text is clipped ("Schedul…") because the row "Live for students: [switch] … Schedule" is too tight. Fix so nothing is clipped at 360dp wide (e.g. put the Schedule/Preview/Delete actions on their own row or allow wrapping). Keep `switchLive`, `btnSchedule`, `btnPreview`, `btnDelete` ids.

### Acceptance
- With global=paused and exam=inherit → new tests are paused. Exam=live → live. Global=live and exam=inherit → live. Exam=paused with global=live → paused.
- Manual "Generate now" and the nightly cron behave identically.

---

## TASK 2 — Result screen bottom bar: remove Leaderboard button, enlarge Reattempt

Files: `app/src/main/res/layout/activity_result.xml` (bottom actions container), `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt`.

- Remove `btnLeaderboard` from the layout and delete its visibility/click code in `ResultActivity.kt` (the Leaderboard tab already exists, so the button is redundant). Remove any now-unused variables/imports it leaves behind (e.g. `showLeaderboard` only if no longer used elsewhere — check before deleting; `LeaderboardActivity` import may still be needed by the tab).
- `btnReattempt`: make it full-width and the same size as the `btnHome` (Close) button below it (match height 48dp, same corner radius, full width), no longer a half-width outlined button.
- Reattempt style: **solid fill `@color/eve_premium` (#E3FF3B), text and icon colour near-black (#111111) in BOTH themes**, bold, icon `ic_refresh` kept. Do not use `?attr/colorOnSurface`. Keep a subtle press animation consistent with other Eve buttons.
- Keep existing logic: Reattempt is shown only when `canReattempt`; when hidden the cluster must collapse (no empty gap) and only Close remains. Update the `layoutActionCluster` visibility logic accordingly (it currently depends on `canReattempt || showLeaderboard`).
- Keep `btnHome` ("Close") as is.

---

## TASK 3 — "Report question" fails with `PERMISSION_DENIED: Missing or insufficient permissions`

### Root cause (verified in code)
`FlaggedQuestionRepository.flagQuestionResult()` still writes directly to Firestore (`flagged_questions` / `reported_bugs`), while the rest of the app moved to the Worker + D1. The Worker has NO report endpoint or table. The deployed Firestore rules reject the write. Do NOT try to fix this by editing/deploying Firestore rules. Move reports to the Worker + D1.

### Backend
- Migration `0009_reports_and_audit_log.sql` creating:
  - `question_reports` (id TEXT PK, question_id, exam_id, exam_name, question_text, reason, comment, student_id, student_email, timestamp INTEGER, status TEXT DEFAULT 'pending', report_type TEXT — values `content` | `technical`) with indexes on `(report_type, status, timestamp)` and `(question_id)`.
  - `admin_audit_log` (id TEXT PK, action_type, description, admin_email, timestamp INTEGER) with index on `timestamp`.
- New route file `backend/src/routes/reports.ts` mounted in `backend/src/index.ts`, mirroring the style/auth/validation/rate-limit patterns used by `feedback.ts`:
  - `POST /api/reports` — any signed-in user (Firebase-authenticated; no anonymous fallback). Validate: reason must be one of the reasons the dialog offers (map to type using the same content-vs-technical rule as `FlaggedQuestion.isContentIssue`), comment ≤ 500 chars, questionText truncated to a safe length, ids non-empty. Use the authenticated user's uid/email (never trust client-supplied identity). Rate-limit per user. De-duplicate: same user + same question + same reason still pending → return success without inserting a duplicate.
  - Admin only (use the existing admin guard — admin authority must be enforced on the backend): `GET /api/admin/reports?type=content|technical` (pending only, newest first), `PUT /api/admin/reports/:id/dismiss` (or status update), `DELETE /api/admin/reports/:id`. Keep the response shape compatible with what `FlaggedQuestionsActivity` needs (it currently groups by `questionId` into `AggregatedFlaggedQuestion` — aggregate either in the Worker or in the repository, keep the model classes).
- Admin audit log routes (admin only): `POST /api/admin/audit-log` and `GET /api/admin/audit-log?range=today|7d|all`.
- Add backend unit tests for report validation + dedupe + admin guard (follow existing test style in `backend/test`).

### Android
- Add the new calls to `EveApiService` (+ DTOs if needed).
- Rewrite `FlaggedQuestionRepository` to use `ApiClient` instead of `FirebaseFirestore`/`FirebaseAuth` for: submit report (`flagQuestionResult`), list pending (content & technical), dismiss, delete. Keep the public method names/signatures used by `ReportQuestionDialog` and `FlaggedQuestionsActivity` so call sites don't change. Remove the anonymous-sign-in fallback.
- Rewrite `AuditLogRepository` the same way (keep `recordLog(actionType, description, adminEmail)` fire-and-forget and non-blocking — it must never break admin flows — plus `getLogs(range)` and the `AuditLogTimeRange` enum). Use a lifecycle-safe scope for the fire-and-forget call (e.g. an application-level `CoroutineScope(SupervisorJob() + Dispatchers.IO)`), not `GlobalScope`.
- `ReportQuestionDialog`: show a clear, human error message on failure (map network/401/429 to friendly English text via the existing `toUserFriendlyMessage()`), never the raw backend string. Add `Log.d/Log.e` with the HTTP status so future failures are diagnosable.
- Do NOT migrate `ApiUsageRepository` (it also uses Firestore). Just mention it in the final report under "Noticed but not touched".

### Acceptance
A signed-in student can report a question from BOTH the Test screen and the Result review screen; it appears in the admin flagged/bug list; admin can dismiss/delete; audit log entries appear in the activity log screen.

---

## TASK 4 — Premium report button (Test screen + Result review)

Replace the plain yellow warning triangle (`ic_warning`, tint `eve_status_warning`) used for "Report this question" in:
- `item_question.xml` → `btnReport` (Test screen),
- `item_answer.xml` → `ivReportQuestion` (Result review).

Design (single shared component so both screens look identical):
- New vector icon `ic_report_flag_premium` — refined outlined flag, 24dp viewport, rounded line caps/joins, ~1.8 stroke (do NOT reuse the old blocky `ic_flag`).
- A 36dp rounded-square (12dp radius) container drawable `bg_report_button`: very subtle amber tint fill (~12% alpha) + 1dp amber border; icon in amber. Add colour tokens in both theme files: dark theme amber ≈ `#FFC83D`, light theme deeper amber ≈ `#B26A00` (must be clearly readable on white). Press state: slight scale + brighter tint (use the existing `press_scale` animator style used by Eve buttons) — no grey default ripple box (this was a known bug elsewhere).
- Keep 48dp touch target, keep ids and click wiring, keep `contentDescription`.

---

## TASK 5 — Leaderboard screen: "Your Rank" card is unreadable + premium polish

Files: `activity_leaderboard.xml`, `LeaderboardActivity.kt`, `item_leaderboard.xml`, `LeaderboardAdapter.kt`, colour tokens if needed.

### Root cause (verified)
`cardYourRank` uses `cardBackgroundColor=@color/eve_primary` (white in dark theme, #111 in light). Text uses `eve_banner_text` (#F5F5F5 dark / #111 light) and the trophy uses `eve_accent` → same colour as the card in both themes, so text/icon are invisible. `tvYourRank`/`tvYourScore` use `eve_on_primary`, which works only by luck.

### Redesign (premium, both themes)
- Card on a neutral surface (`eve_card_bg`) with a 1.5dp `eve_premium_line`/#E3FF3B-style border (match the existing highlighted "(You)" row so they feel related), 20dp radius.
- Left: rank medallion showing `#<rank>` bold, with a thin accent ring (this is a static badge, not a Lottie, so a ring is fine). Replace the old trophy ImageView or make it theme-safe.
- Middle: small label "YOUR RANK", large rank value, then the percentile line in `eve_text_secondary`.
- Right: score large and bold with the "/total" part in muted colour.
- Use only semantic tokens that are readable in both themes. Verify in light AND dark.
- "Top Scorers" heading gets a small subtitle; rank colours for #1/#2/#3 (accent/silver/bronze) stay as in the adapter. Polish spacing; keep the rest of the list as is.

### Logic/text fixes in this task
- `Top 1% of 1 students` is wrong UX. If `totalParticipants <= 1`: show "You're the first to attempt this test". Otherwise "Top X% of N students" (use correct singular/plural).
- Replace Hinglish loading text "Leaderboard load ho rahi hai…" with "Loading leaderboard…".
- Clean the stale/Hinglish KDoc comment at the top of `LeaderboardActivity.kt` (comment only).

---

## TASK 6 — Result → Overview tab: category dropdown + merge Analysis into Overview (3 tabs)

Files: `activity_result.xml`, `ResultActivity.kt`, new bottom-sheet layout/class, resources.

### 6a. Remove the "SELECT CATEGORY TO COMPARE" list; make the category chip a dropdown
- Delete the section header + the whole 5-row categories card (`rowCatGeneral/Obc/Sc/St/Ews`, `tvSub*`, `ivCheck*`) from the layout, and remove their code in `ResultActivity.kt` (`updateCategoryRow` usages etc.).
- In `cardCutoffVerdict`, make the "Selected Category: **General**" value (`tvCutoffSelectedCategory`) a tappable pill with a down-chevron icon (add `ic_chevron_down` if missing). Min touch target 48dp. Premium styling, readable in both themes.
- Tap opens a Material `BottomSheetDialog` listing General, OBC, SC, ST, EWS. Each row shows the category name and below it the cutoff status using exactly the logic of the old `updateCategoryRow` ("Cutoff: X • Qualified ✓" green / "Not Qualified ✗" red / "No cutoff configured" muted). The selected row shows a check (use the premium accent, NOT the old red tint). Selecting a row dismisses the sheet and updates the card via the existing `selectCutoffCategory` → `updateCutoffUI`. Keep `selectedCutoffCategory` and `examCutoffs` logic unchanged. Sheet must look right in light and dark.

### 6b. Remove the Analysis tab and move its content into Overview
- Remove the 4th `TabItem` ("Analysis") from `tabLayoutResult` (tabs become Review | Overview | Leaderboard) and the `pos == 3` branch in `setupTabLayout()`. Grep for any other use of tab index 3 / "Analysis" tab selection (intents, `selectTab`) and fix.
- Move the content of `sectionAnalysis` (Performance Summary, Time per Question chart, Slowest Questions, Topics + Practice Weakest Topic) into the Overview tab, placed directly below `cardCutoffVerdict` (where the category list used to be), under a section heading "ANALYSIS". `sectionAnalysis` currently has `visibility="gone"` and `padding=16dp` for tab use — it must now be visible and must not add double padding. KEEP all existing view ids (`tvAnalysisAttempted`, `tvAnalysisAccuracy`, `tvAnalysisAvgTime`, `chartTimeView`, `layoutSlowestList`, `layoutTopicList`, `btnPracticeWeakest`, `cardSlowestQuestions`, …) so `setupAnalysisSection(...)` and other code keep working untouched.
- The old small "TIME & PACE INSIGHTS" card (`tvTimeSummary`) in Overview duplicates the Analysis data → remove that card and the code lines that set `tvTimeSummary` (leave no dangling reference).
- Consequence to verify: with 3 tabs the "Leaderboard" tab label must fit on ONE line (it currently wraps as "Leaderboar/d"). Confirm `tabMode=fixed` + `tabGravity=fill` give it enough width; if it still wraps at 360dp, fix it.
- Do not change Review or Leaderboard tab content.

---

## TASK 7 — Test screen: no scrolling, always-visible options, premium finish, animated bookmark star

Files: `item_question.xml`, `activity_test.xml`, `QuestionAdapter.kt`, `TestActivity.kt` (only where needed), `type.xml` only if needed, star drawables, new helper class.

### Problem (verified)
Each page is a `ScrollView`. Long questions (e.g. a 9-line Hindi "statement" question) take most of the screen, so options B/C/D fall below the fold and the student must scroll to answer. Vertical space is also wasted by: 16dp spacer `View` + 14dp bottom margin around the question, 48dp action row, the separate `tvProgress` row ("Fftyu - Test 1 • Question 2 / 5", duplicates what the palette and the "Q2." prefix already show), and two tall bottom button rows.

### Honest requirement note
"Zero scrolling for every possible question" is physically impossible for extremely long questions on small phones. Implement the standard approach used by real exam apps instead: **the 4 options are ALWAYS fully visible and never scroll; the question is auto-fit to the space left; only in the rare worst case (text still doesn't fit at the minimum readable size) the QUESTION TEXT alone scrolls inside its own bounded box** — options never move off screen. Make that rare fallback look intentional (fading edge + small chevron hint).

### Layout restructure
- `item_question.xml`: root becomes a vertical `LinearLayout` (match_parent), no page-level ScrollView. Children in order:
  1. Compact meta row (height ≈ 40dp): timer chip left; bookmark star + premium report button right (Task 4 component). Tighten margins.
  2. Question region: `0dp` height with `layout_weight=1` containing a `ScrollView` (`svQuestion`, fading edge only when scrollable) wrapping `tvQuestion`. Remove the 16dp spacer `View`; question bottom margin 14dp → 8dp.
  3. Options block: `wrap_content`, pinned below the question region (the four `TelegramRadioButton`s; keep ids `rgOptions`, `rbA..rbD`). Row vertical padding 12dp → 10dp, margin 8dp → 6dp, text 16sp → 15sp max.
- `activity_test.xml`: remove the standalone `tvProgress` row; show the exam/test title (ellipsized, single line, secondary colour, 14sp) in the header between the circular timer and the language button instead, and update `TestActivity.kt` (line ~394) so nothing references a deleted view. Keep the "Q<n>." prefix in the question text and the palette as the progress indicator. Compact the bottom bar: button `minHeight` 48dp → 44dp, row gap 6dp, bottom padding 12dp → 8dp. Keep all four buttons and their ids/behaviour.
- Question region minimum height ≈ 96dp so it never collapses.

### Auto-fit helper
- New small class (e.g. `QuestionFitHelper`) that, once the question region height is known (use `doOnLayout`/`OnGlobalLayoutListener`, remove listeners properly), picks the largest question text size from 18sp down to 15sp in 1sp steps that fits without scrolling (measure with `StaticLayout`/`TextView` measuring at the real width, honour the current line spacing; use `lineSpacingMultiplier` 1.25 at ≥17sp, 1.15 below). If even 15sp doesn't fit, use 15sp and enable the internal scroll fallback (fade edge + chevron). If the OPTIONS block is very tall, apply the same step-down (min 14sp) to options so all four stay visible.
- Must work for Hindi (Devanagari has tall glyph metrics) and English, and re-run on language toggle (HI/EN), question change, font change (Settings → Font option), and rotation/size changes. No flicker: compute before first draw where possible. Recycled ViewPager2 pages must not carry stale sizes — reset on every `bind`.
- Put the pure "pick size" logic in a testable function and add a small unit test.
- Manual verification matrix (state results in the final report): 360×640dp and 411×891dp screens; question lengths: 2 lines, 5 lines, 9 lines Hindi statement question with long options.

### Premium finish (Test screen)
- Calm, consistent spacing; clear visual hierarchy (question text strongest, options clearly tappable cards, meta row quiet).
- Option rows: keep the existing TelegramRadioButton behaviour (do NOT reintroduce the grey touch-highlight bug); use a subtle rounded outline per option and a clear selected state using the accent (#E3FF3B) that is readable in both themes.
- Timer chip, star and report use the same visual language (rounded, subtle border).
- Everything must be readable in light and dark theme.

### Bookmark star animation
Current behaviour (in `QuestionAdapter.kt`) is a tiny shrink/overshoot and the star icons use `eve_accent` (white/black). Replace with:
- **Bookmark ON:** the star does one full 360° spin (~420ms, decelerate) with a slight scale pop (1 → ~1.3 → 1 with overshoot); at about 40% of the animation swap outline → filled star. Add a very small sparkle burst (4–6 tiny dots flying outward and fading, ~300ms) drawn lightweight (no Lottie, no ring/circle/tile behind the star). Light haptic via the existing `HapticHelper`.
- **Bookmark OFF:** quick reverse feel (≈180ms: small scale down + slight counter-rotation), swap to outline, no sparkles.
- Filled star colour: `#E3FF3B` fill. In light theme it must stay visible on white — add a thin dark (#111) outline in light theme; in dark theme the accent fill alone is fine. Implement through theme-aware tokens in both colour files and update `ic_star_filled`/`ic_star_outline` (outline uses a readable muted/text colour in both themes).
- Robustness: rapid double taps must not leave the star mid-rotation, wrong icon, or off-scale. Cancel running animators cleanly (don't let a cancelled animation's end-callback flip the icon wrongly), derive the final icon only from `getBookmarked(position)`, reset `rotation/scale` to 0/1 on every `bind` (recycled views), and clean up the sparkle overlay so nothing leaks or stays visible. The bookmark state itself (toggle logic via `onToggleBookmark`) must not change.

---

## Final report (required format)
1. Per task: files changed, what was done, key decisions (and why).
2. Exact verification results: `./gradlew assembleDebug`, unit tests, `npm run build`, `npm test`, `git diff --check`.
3. Task 7 manual verification matrix results.
4. Migrations added (`0008`, `0009`) and a note that they apply on push via the deploy-worker workflow.
5. "Noticed but not touched (outside scope)" list (must include: `ApiUsageRepository` still on Firestore; stale Firebase wording in `GEMINI.md`).
6. Commit to `main` with a clear message per task (or one message listing all 7 tasks).

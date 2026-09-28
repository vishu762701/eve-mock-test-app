# EVE APP: MASTER TASK

Everything Antigravity must do is in this file. It is complete. No further messages are coming. Read all of it, then start.

## 0. HOW TO WORK (applies to every task)

1. Run `git pull` first. Work on `main`. Make ONE commit at the end and push it.
2. Scope discipline: touch only what a task below needs. Do not re-edit, refactor or "improve" earlier completed work. If you notice an unrelated problem, list it under "Noticed but not touched" in the final report.
3. Facts below were read from the codebase. Verify each with grep before acting. If reality differs from what is written here, trust the code, adapt, and say so in the report.
4. Honest verification: unit tests and a green Gradle build are NOT functional proof. For every task the final report must say VERIFIED (how) or NOT VERIFIED (why). If you cannot run the app or deploy, say so plainly. Never write "verified" for something you did not exercise.
5. Never draw or generate a substitute animation. If a required Lottie file is missing, STOP that task and report it.
6. Colors: no new hard-coded colors anywhere. Use the tokens from task T1.
7. Execution order: T0, T1, T2, T3, T5, T6, T7, T4, T8, T9. T8 and T9 are conditional: verify first, skip if already done.

## T0. LOTTIE ASSETS

Three Lottie files are used. Find each one (case-insensitive search of `app/src/main/res/raw` and `app/src/main/assets`):
- `man_flying_on_paper_airplane.json` (user's original name: `Man_flying_on_paper_airplane.json`)
- `Pencil__Writing_on_Book.json` (may be uploaded with capital letters)
- `search.json` (exists at `res/raw/search.json`; identical copies exist in `assets/lottie/`; do NOT touch those copies)

Android `res/raw` file names may contain only `[a-z0-9_]`. If a file in `res/raw` has capital letters, `git mv` it to lowercase (`pencil_writing_on_book.json`) and update every reference. Confirm each file parses as JSON. If a file is not found anywhere, stop the tasks that need it and report.

## T1. SEMANTIC COLOR TOKENS + CONTRAST AUDIT

Root cause found: `eve_primary` is `#111111` in light theme and `#FFFFFF` in night theme, so top bars are white in dark mode. `CircularTimerView` hard-codes `Color.WHITE` (track, progress, text), so the test timer is white-on-white and invisible in dark mode. The existing `eve_timer_warning` (`#9C2F22` light / `#FFAB91` night) also has low contrast on those bars.

Add these tokens (do not change existing ones). Values are pre-checked for WCAG contrast (text >= 4.5:1, icons >= 3:1).

| token | values/ (light) | values-night/ |
|---|---|---|
| eve_status_success | #1B7A3D | #4ADE80 |
| eve_status_error | #C62828 | #FF6B6B |
| eve_status_warning (amber) | #B45309 | #FFC53D |
| eve_status_info | #1D5FB8 | #6CB6FF |
| eve_status_warning_container | #FFF4D6 | #2A2110 |
| eve_header_warning (drawn on the top bar) | #FF5252 | #C62828 |
| eve_lottie_tile_bg | #F5F5F5 | #F2F2F2 |

Rules:
- Anything drawn on a top bar (`eve_primary` background) must use `eve_on_primary`, and `eve_header_warning` for urgency.
- `CircularTimerView`: track = `eve_on_primary` at 25% alpha; progress and text = `eve_on_primary`; below the warning threshold use `eve_header_warning` with the existing smooth color transition. Resolve colors from the current context on attach and on configuration change; never cache colors across theme switches.
- Audit and fix (only where the drawing sits on a theme-dependent background): hard-coded `Color.WHITE / BLACK / parseColor / argb` in `CircularTimerView`, `CircularCountdownView`, `TelegramRadioButton`, `RippleHelper`, `ShimmerSkeletonView`, `MainActivity`, `AppBulletin`, `AppUndoBar`. Leave `AmbientBackgroundView` (login art) and `AvatarDrawable` (white initials on a colored disc) as they are.
- Audit vector drawables with hard-coded white or black fills (for example `ic_flag`) that are used without `app:tint` / `android:tint` on a theme-dependent background. Give them a token tint at the usage site.
- Add a JVM unit test `ContrastTokensTest` that parses `values/colors.xml` and `values-night/colors.xml`, computes WCAG ratios, and fails below the thresholds for: status colors vs `eve_bg` and `eve_surface`; `eve_on_primary` vs `eve_primary`; `eve_header_warning` vs `eve_primary`; `eve_status_warning` vs `eve_status_warning_container`.

## T2. TEST SCREEN (`ui/test/TestActivity.kt`, `QuestionAdapter.kt`, `item_question.xml`, `activity_test.xml`)

2a. Timer visible: done through T1.

2b. Live per-question timer above each question:
- A small chip above `tvQuestion` (id `tvQuestionTimer`, timer icon + `m:ss`, `h:mm:ss` from one hour) showing how long the student has spent on THIS question. Neutral colors (`eve_surface_variant` background, `eve_text_secondary` text).
- Time is cumulative per question: leaving a question pauses its clock, returning resumes it. Keep it in `TestViewModel` (survives rotation), not in the holder. Currently `questionStartTimeMs` is reset on every page change; replace this with accumulation.
- One shared 1-second tick updates only the visible page (use an adapter payload such as `notifyItemChanged(pos, PAYLOAD_TIMER)`, no full rebind, no per-holder timers). Pause accumulation while the activity is not resumed.
- Use the same accumulated value for `viewModel.recordQuestionTime` and for the data later sent to `question_stats`, so there is only one measure of per-question time.

2c. Report button on every question DURING the test:
- Add `btnReport` in the question header row next to `btnBookmark` (48dp touch target). Style: flag icon tinted `eve_status_warning`, circular container `eve_status_warning_container` with a 1dp stroke of `eve_status_warning` at 35% alpha. contentDescription "Report this question".
- It opens the existing unified report dialog (`dialog_report_question`). Extract a shared helper (e.g. `ReportQuestionDialog.show(activity, question, examId)`) used by Test, Review and any other caller. Reporting must not pause or reset any timer and must not reveal the correct answer. Submission uses the existing `FlaggedQuestionRepository` routing (content issues to `flagged_questions`, technical to `reported_bugs`), unchanged.
- Restyle the report flag in Review (`item_answer.xml`) with the same amber style.

2d. NO instant feedback while taking a test (this was a design mistake):
- Remove the `showTimerComparisonPopup(...)` call in `onSelect` and remove the `timerPopup` include from `activity_test.xml`. Keep recording time and the selected answer.
- Audit the whole Test screen: before submit nothing may reveal correctness, explanation, accuracy, average time or community stats. That includes colored option states, Bulletin/Toast messages, and different haptics for right/wrong. A selected option shows only the neutral selected state.
- `questionStatsRepo.recordQuestionAttempt` must run once per answered question at submit time (it already runs at submit; confirm that re-tapping options never inflates counts).

## T3. REVIEW SCREEN (after submit): show the time/accuracy information here

- Move the message logic from `showTimerComparisonPopup` (fast/slow, correct/wrong wording, emoji) into a shared formatter used by `AnswerAdapter`.
- Per question in Review show a static inline card (no popup, no auto-dismiss): "You" bar vs "Avg" bar with seconds, plus one short message. Colors from the status tokens (success when faster and correct, warning when slower, error when wrong).
- The card appears only when the per-question time is available (right after a fresh submit, held in memory) and average data exists. For results opened from history or Home, omit the card. Do not add a database column for this now.
- Load average stats once per Review load (parallel, cached in the ViewModel), not once per row bind.

## T4. SUBMITTED TEST + REATTEMPT

Current behavior (verified): in `ExamAdapter.ExamVH.bind`, an attempted exam card is disabled (`isEnabled = false`, alpha 0.62, click listener null) with the text "Completed • View Result in History". `TestViewModel` (around line 89) shows an "already attempted" message and finishes when `hasAttempted` is true. Worker `POST /api/attempts/submit` returns 409 when an `attempt_locks` row exists (`id = "${uid}_${examId}"`; created only for standard mocks: no topic, no pyqYear, non-admin).

Required behavior (standard mock tests only; topic-wise and PYQ tests and admin behavior stay unchanged):

Client:
1. Attempted cards become normal (alpha 1, tappable). Subtitle string becomes "Submitted • Tap to view result".
2. Tapping one opens the student's submitted result: fetch the lock and the attempts (`api.checkAttemptLock`, `historyRepo.getAttempts`), pick the attempt whose `timestamp` equals the lock's `timestamp` (fall back to the latest attempt for that examId), then open `ResultActivity` exactly like `HistoryActivity` does (`ResultDataHolder.setAnswers`, `EXTRA_EXAM_ID`, `EXTRA_EXAM_NAME`, `EXTRA_ATTEMPT_DATE`, `EXTRA_FROM_HISTORY = true`) plus a new extra `EXTRA_CAN_REATTEMPT = true` and the exam's time limit and category.
3. In `ResultActivity`, when `EXTRA_CAN_REATTEMPT` is set, show a "Reattempt Test" outlined button.
4. Tapping it shows a MaterialAlertDialog: title "Reattempt this test?"; message "Your previous result for "{exam}" (score, answers and rank) will be permanently deleted and replaced by your new attempt. This cannot be undone."; buttons "Cancel" (default) and "Clear & Reattempt" (tinted `eve_status_error`).
5. On confirm: disable buttons, show progress, call `POST /api/attempts/reset` (below). On success: clear `ResultDataHolder`, drop the examId from the cached attempted set (`HomeViewModel._attemptedIds`) immediately, start `TestActivity` with the same extras Home uses (see `MainActivity` around line 116), and finish the Result screen. On failure: show an AppBulletin error and change nothing. Home must refresh the attempted set in `onResume`.
6. Keep the existing `hasAttempted` guard in `TestViewModel` as a safety net.

Backend (`backend/src/routes/attempts.ts`):
- Extend `GET /api/attempts/locks/:examId` to also return `timestamp` (nullable; additive, keep `hasLock`). Add `timestamp: Long? = null` to the Android response model.
- Add `POST /api/attempts/reset` (authenticated user, body `{ examId }`). Read the lock `${uid}_${examId}`; if none, return `{ success: true, data: { cleared: false } }`. Practice attempts of the same exam are stored in `attempts` too (no practice flag), so identify the mock attempt as the row with `user_id = uid AND exam_id = examId AND timestamp = lock.timestamp` (submit writes both with the same `now`). If no exact match, use the nearest timestamp within 60 s; if still none, delete only the lock and return `clearedAttempts: 0`.
- In ONE `db.batch`: delete its `attempt_answers`; delete the `attempts` row; recompute `leaderboard` row `${examId}_${uid}` from the user's remaining attempts for that exam (best score), or delete the row if none remain; subtract its `score`, `total`, `correct` from `overall_leaderboard`, decrement `tests_taken` (not below 0), recompute `accuracy`, and delete the row when `tests_taken` reaches 0; delete the `attempt_locks` row.
- Deliberate decisions (state them in the report): `admin_analytics_exams.attempt_count` and `admin_analytics_exam_users` are NOT decremented (they measure activity); community `question_stats` averages are not modified.
- Add a backend test in `backend/test/` if a D1 mock/harness exists there. Otherwise describe the manual SQL verification.

## T5. HOME EXAM CARD: PENCIL LOTTIE INSTEAD OF THE DEFAULT ICON

Current: `item_exam.xml` has a 52dp circular `ShapeableImageView` `ivExamImage` (default `@drawable/ic_exam_placeholder`); `ExamAdapter.ExamVH.bind` calls `ExamImageHelper.loadExamImage(b.ivExamImage, exam.imageUrl)`. `Exam.imageUrl` is a `data:image/...;base64` string or a URL, empty when the admin added no logo.

Lottie facts: `pencil_writing_on_book.json` is 1200x1200, 25 fps, 70 frames (2.8 s), contains 2 embedded raster PNGs (black pencil, white book). It cannot be recolored, and the black pencil body would vanish on a dark card. So it always sits on a light tile (`eve_lottie_tile_bg`).

Implementation:
- In `item_exam.xml` replace the bare image with a 52dp circular container `examIconContainer` (same size and position, circular clip, background tinted `eve_lottie_tile_bg` only in Lottie mode) holding the existing `ivExamImage` (id unchanged) and a `LottieAnimationView` `lottieExamIcon` (`match_parent`, 4dp padding, `lottie_autoPlay=false`, `lottie_loop=false`, raw `pencil_writing_on_book`). The `tvExamName` layout uses `layout_toEndOf="@id/ivExamImage"`; update it to the container id.
- `hasLogo = exam.imageUrl.isNotBlank()`. Logo present: show `ivExamImage` via `ExamImageHelper`, hide and cancel the Lottie. No logo: hide `ivExamImage`, show the Lottie. If a logo fails to decode (`data:` decode failure or Coil error), fall back to Lottie mode for that holder, guarded by checking the holder still shows the same exam id.
- Play once per appearance of the page, never as a loop:
  - The adapter keeps `playedExamIds`. In `onViewAttachedToWindow`, if the holder is in Lottie mode and its id is not in the set: `progress = 0f; playAnimation()` and add the id. Otherwise `progress = 1f` (the finished frame). `onViewRecycled`/detach: `cancelAnimation()`.
  - Scrolling, filter chips (All/SSC), DiffUtil refreshes and pin toggles must NOT replay.
  - `adapter.resetPlayedAnimations()` plus `replayVisible(recyclerView)` (walk visible view holders) is called from `MainActivity.onStart()` only when Home returns from a stopped state, because attached views do not re-attach when returning from another screen.
  - If system animations are disabled (`ANIMATOR_DURATION_SCALE == 0`), show the final frame only.
- Attempted cards (T4) use the same rule. Admin logo upload flow is unchanged.

## T6. HOME EMPTY STATE: `search.json` INSTEAD OF THE FOLDER ILLUSTRATION

The "No exams available" state (Home) currently shows a Lottie/illustration (`EmptyStateAnimationHelper.kt`, `no_files`). Replace only this animation with `res/raw/search.json` (642x642, 60 fps, 316 frames = 5.27 s; the red X appears near frame 252). Do not change `ErrorStateView` / `error_404`, and do not delete `no_files.json`.
- Play once when the empty state becomes visible after the shimmer hides; hold the last frame. Do not replay on theme change or recreate (show the last frame). Stop when content arrives; if the list becomes empty again, play once more.
- Dark mode: light theme keeps original colors. In night theme apply color callbacks with `KeyPath("**")` for `LottieProperty.COLOR` (fills) and `LottieProperty.STROKE_COLOR` (strokes), mapping original color to themed color, unmapped colors unchanged:
  #E6E6E6 to #1C1C1F; #C7EBF5 and #C4EDF5 to #1E3440 (strokes #C7EBF5 to #2F5566); #A6CCD6 to #5D8A9A; #0A2B4A to #DCEBF5; #0A4F80 to #6CB6FF; #2B4559 to #9DB7C8; #FFFFFF to #262B31; #EB0000 to #FF6B6B; #6EE3FF unchanged. Re-apply on theme change. Null-safe and no per-frame allocations.
- If you can render frames in both themes (screenshot test), do so for frames 0, 150 and 315 and report. If not, say the dark palette is not visually verified.

## T7. FLOATING AIRPLANE LOTTIE + ADMIN-SET LINK

Home screen, bottom-right corner: a floating round chip containing `man_flying_on_paper_airplane.json` (1080x1080, 30 fps, 4 s, black outlines; never recolor it).
- Chip: 72dp circle, white (`#FFFFFF`) in both themes, 6dp elevation, 1dp `eve_stroke` border in light theme only, Lottie 60dp inside. Position: bottom end, 16dp margin plus the navigation-bar inset (WindowInsets). Give the exam list extra bottom padding (`clipToPadding=false`, about 96dp) so the last card is never covered.
- Animation loops forever (`REPEAT_INFINITE`) while Home is visible. Pause in `onPause`/`onStop`, resume in `onResume`/`onStart`. Show the final frame if system animations are disabled.
- Entrance: one 250 ms scale-in with slight overshoot when it first appears. Press feedback: scale to 0.92 on touch down, back on release.
- Visible only when a valid link exists; hidden otherwise. Tap opens the link with `Intent.ACTION_VIEW`, handling `ActivityNotFoundException`/`SecurityException` with an AppBulletin "Couldn't open link". contentDescription "Open community link".
- Storage: do NOT add the link to `AppConfig`. Every existing save path builds `AppConfig(...)` from three fields and the Worker `PUT /api/admin/config` whitelists three fields, so saving maintenance settings would wipe the link. Store it separately. Verify the `app_content` table and how other rows (e.g. the home banner) are read; if a `floating_link` row fits its schema use it, otherwise add a tiny table by migration. Make sure the new row cannot appear in any list-all endpoint.
- Routes: `GET /api/floating-link` (same auth level as other config reads) returns `{ url }`; `PUT /api/admin/floating-link` (admin only). Server validation: https only, valid URL, at most 500 chars, no embedded credentials; an empty value clears it.
- Client: `FloatingLinkRepository`, fetch on Home `onStart` with a 10-minute cache; on failure keep the last cached value; block `javascript:`, `intent:`, `file:` and any non-https scheme.
- Admin: in the Admin tab, Content Management section, add a card "Floating Link". It opens a dialog or screen with a URL field, Save and Clear. If the admin types a link without a scheme, prepend `https://` before validating. Record changes with the existing `AuditLogRepository`. Confirm success with AppBulletin.

## T8. THEME REVEAL ORIGIN AND EASING (CONDITIONAL: verify first, skip if already done)

Check `ui/home/TelegramMenuPopup.kt` (cardTheme click handler) and `util/ThemeSwitchAnimator.kt`. Done means: origin is computed from `anchorViewRef` (the 3-dot button) and the interpolator is `PathInterpolator(0.455f, 0.03f, 0.515f, 0.955f)`. If both are true, skip and report "already done". Otherwise:
- Currently the origin comes from `binding.ivThemeIcon` (the "Day Mode" row inside the popup), which sits near the screen center. Telegram (`DialogsActivity.switchTheme`) uses the 3-dot options button. Replace with:
  `val anchor = anchorViewRef ?: return@setOnClickListener`
  `val loc = IntArray(2); anchor.getLocationOnScreen(loc)`
  `val iconW = if (anchor.measuredWidth > 0) anchor.measuredWidth else anchor.width` (same for height)
  `val cx = loc[0] + iconW / 2; val cy = loc[1] + iconH / 2`
  with a temporary `Log.d("ThemeOrigin", "cx=$cx cy=$cy")`. Compute fresh on every tap. Never use touch coordinates or `ivThemeIcon` for the origin. Keep dismiss-then-`onThemeToggle(cx, cy, iconW, iconH)` as is.
- In `ThemeSwitchAnimator.kt` replace `FastOutSlowInInterpolator()` with the PathInterpolator above (Telegram `Easings.easeInOutQuad`), remove the unused import, keep 400 ms. Do not touch the reveal layering or radius logic. No Lottie may exist in this flow.
- Verify on device: toggle 6 times; the circle must grow from or shrink into the top-right 3-dot button every time. Report the `ThemeOrigin` values (about (957, 207) on a 1080x2340 screen), then remove the log.

## T9. DUPLICATE SCHEDULER (CONDITIONAL: verify first, skip if already done)

If `functions/index.js` has `exports.scheduledTestGeneration = onSchedule(...)` AND `backend/wrangler.toml` has `crons = [...]` with `scheduled: handleScheduledTestGeneration` in `backend/src/index.ts`, both run every 5 minutes independently (race risk: duplicate generation and duplicate Gemini cost).
- Evidence: the app uses the Cloudflare Worker for exams and "Generate Now"; the Worker cron reads D1; the Firebase function reads the Firestore `exams` collection. Some features (e.g. cutoffs in `EditExamActivity`) still use Firestore, so confirm which store is the source of truth for `autoGenTime`, `lastGeneratedDate` and the auto-generation flag before deciding.
- Keep exactly one scheduler. Remove the other completely (do not just add a guard). Redeploy the remaining one and confirm via logs that only one system fires.
- Report which backend you kept and which you removed.

## DEPLOY NOTES

Backend changes (T4, T7, T9) must be deployed to the Worker (`wrangler deploy`, plus a migration command if you added a migration) BEFORE the new APK is released, otherwise Reattempt and the floating link will fail. If you cannot deploy from this environment, do not mark it done: list the exact commands the user must run.

## FINAL REPORT FORMAT

For T0 to T9: status (DONE / SKIPPED-already-done / BLOCKED), files changed, VERIFIED (how) or NOT VERIFIED (why). Then: "Noticed but not touched", the commit hash, and any commands the user still has to run.

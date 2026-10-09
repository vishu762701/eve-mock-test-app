# Eve final repair verification — 2026-10-09

## Status
Scoped implementation and local validation passed. Native gesture checks passed;
three-button contrast is still failing and under diagnosis. The full task is
not claimed complete. Actual CI/deployment results and remaining limits follow. No new ZIP or user
screenshot was attached; comparison with that exact build remains unavailable.
Starting main revision: e1a3a79814b29097bfdde7b78ded4e46d8901038.

## Repairs and reproducible evidence
| User-visible symptom | Reproduction / verified cause | Files and exact repair | Regression / result |
|---|---|---|---|
| Raw HTTP409 and disappearing exam | Delete an exam with saved attempts/session; optimistic removal and unconditional success | `ManageExistingExamsActivity.kt`, `GeneratedTestsActivity.kt`, `ManageExamsActivity.kt`, new `AdminDeleteFlow.kt`: one guarded preflight/confirmation flow, no optimistic removal, refresh on confirmed outcome, parsed errors and copyable safe request UUID | `AdminDeletionRegressionTest`: 200/409/500/offline and repeated taps; native execution pending |
| Fake Undo creates another exam | Delete then tap Undo: old code called addExam | Same controller/callers: irreversible delete has honest confirmation, no Undo and no POST to create replacement | Native regression asserts zero create calls and no Undo action; pending |
| Cannot understand delete dependencies | Isolated real route returned404 for missing deletion-info | `services/deletion.ts`, `routes/exams.ts`, `routes/generatedTests.ts`, `DeletionInfo.kt`, `EveApiService.kt`: admin-only aggregate counts, stable `DELETE_BLOCKED_*` codes, counts, explanation, safe unpublish action, request ID | Actual Hono/D1 SQLite tests pass: sessions, sub-exams, completed attempts, student rejection, eligible delete, rollback |
| Delete/start race risks orphans | Insert a session after dependency check; remove a test between read and start insert | Same backend service: transactional conditional deletes recheck protections inside D1 batch; `attempts.ts`: insert session only while parent records exist and return404 `TEST_REMOVED` if not stored | Real SQL race-injection tests pass; no migration/schema change |
| Legacy reattempt erases results/statistics | Submit, seed an authentic legacy completion lock, reset: baseline attempt count becomes0 | `attempts.ts`: POST/reset and legacy DELETE/exam now release caller's completion lock only; preserve attempts/answers/stats, reject unfinished sessions; `ResultActivity.kt` removes destructive wording/offline fake success and clears local completed session only after confirmation | Actual route tests pass: new UUID, unchanged history/answers/stats, protected ongoing work and three-attempt limit; obsolete simulation-only deletion tests replaced |
| Fresh/recycled question looks selected | Bind a checked holder to an unanswered question mid-animation; drawable changes before custom progress | `TelegramRadioButton.kt`: snap custom progress and press transforms on reset/detach; snap unchecked approved options immediately. `QuestionAdapter.kt`: reset each option after bind/clear/frozen-selection restore; disable hierarchy answer persistence | `RepairRegressionTest`: immediate pixel check in both themes, approved #34C759, clear/navigation/recycling, no hierarchy restore; native pending |
| A new attempt restores stale answers | Persist exam/user answers under old UUID, server returns a different session UUID | `TestSessionStore.kt`, new `TestSessionPolicy.kt`, `TestViewModel.kt`, `TestActivity.kt`: explicit owner/exam scope, server UUID comparison, clear stale maps on new UUID, validate answer letters, saved activity state includes exam key | `TestSessionPolicyTest`: matching resume, wrong owner/exam/UUID, fresh/reattempt, clear; Android unit tests passed |
| Navigation-bar flashes/touch overlay | Old-theme decor bitmap remains while target icons are applied; duplicate recreation | `ThemeSwitchAnimator.kt`: single AppCompat recreation, no screenshots/reveal overlays/hidden icons/window touch flags. `SystemBarHelper.kt`, `EveBaseActivity.kt`: visible-resource icon/color synchronization and explicit edge-to-edge; disable system navigation scrim over app's own background | Native lifecycle regression switches both directions, retains state, checks icon flags/overlays and screenshots; gesture and three-button CI pending |
| Bottom/horizontal inset loss | Existing listener calls setPadding(0,status,0,0) | `SystemBarHelper.kt`, `inset_ids.xml`: install once, add system bars/cutout to captured padding and pass remaining insets to descendants; TestActivity preserves original footer padding | Native regression verifies nonzero initial padding, repeat dispatch, descendant navigation not added twice and footer bounds; pending |
| Stale management refresh | Multiple loads can return out of order after a mutation | Three management activities cancel previous list-load job and propagate cancellation instead of showing cancellation as error | Android compilation passed; native shared deletion flow verification pending |

## Alignment evidence and limits
| Area | Functional adjustment | Evidence |
|---|---|---|
| Result Statistics | Adaptive two-column/four-column grid, content-based height, existing20sp numbers/12sp labels instead of shrinking labels to9sp | Native layout checks at320/390/600dp, font1.0/1.5, both themes pending |
| Performance Standing | Existing columns stack when constrained; long rank/percentile/pill values wrap without truncation | Same checks assert full text, no ellipsis, height/line width bounds |
| Analytics | Gauge and metrics stack when cramped; metric row also responds to width/font scale | Same checks cover100.0%,200/200, Attempted/Accuracy labels |
| Cutoff | Existing category/header no longer shares one cramped horizontal row; unconfigured cutoff hides verdict/score and explains unavailable qualification; configured zero is a valid cutoff | Shared `ResultCutoffPresentation` used by real Activity and native regression, pending |
| Mock Test | Approved option style preserved; footer respects navigation insets; animation reset removes phantom checked indicator | Native screen/bitmap regressions pending; user-supplied screenshot unavailable |

Native screenshots use isolated fixtures and production layouts/custom controls.
They do not prove live authentication, AI calls, actual student results or
end-to-end timer/network behavior. No production student data is modified.

## Local checks
- Baseline Android build/lint/instrumentation APK compiled successfully. Cached
  baseline unit outputs are distinguished from fresh final execution.
- Backend **121 automated tests passed**, including current
  generation/parser/timeouts/duplicates, transactional scoring/submission
  idempotency/history, auth/admin monitoring and new delete/reattempt/race cases.
- Optional Firebase Functions: syntax/build and **16 tests passed**.
- Final Android assembleDebug/testDebugUnitTest/assembleDebugAndroidTest/lintDebug:
  BUILD SUCCESSFUL in9m13s.196 tests,0 failures/errors; lint0 errors and1525
  warnings. The formatting error in the touched logout builder was corrected.
- Worker dry-run validation passed; read-only health returned200 and missing-token admin access401. See safe request IDs below.
- Two intermediate Android instrumentation compiles failed because source edits
  added classes after the production task snapshot. They are not reported as
  passed; a settled-tree validation is required.

## Compatibility/deployment
No migration is introduced or modified; all existing forward migrations run in
the actual SQLite route tests. New admin endpoints are additive. Older clients
retain deletion protection; reattempt endpoints intentionally become
non-destructive. New Android deletion preflight needs the Worker update before
rollout. Deploy only through existing authorized GitHub Worker workflow.
Firebase Functions/rules were not deployed: no configured Firebase credentials
or authorized Firebase deployment workflow is available.

Unpublish preserves results/sessions and disables this exam's auto-generation;
it does not archive the exam or hide its separate question bank. No arbitrary
session-expiry purge or student-history deletion is authorized. A running
manual generation may create a subsequent test; administrators must monitor
jobs before treating the exam's tests as permanently unpublished.

## Remaining verification blockers
No attached latest ZIP/screenshots, no local KVM, no production student/admin
Firebase tokens, no diagnostic key/provider deployment credentials in this
workspace. Public health/missing-token checks are not authenticated end-to-end
production tests. Android24/29/34/36, OEMs, landscape/RTL/full localization,
rotation during real live attempts and a complete logged-in feature sweep remain
unverified unless listed explicitly below. Existing repository lint warnings
are not suppressed or falsely reported fixed.

## Main push and CI
Pending commit, verified remote SHA, emulator screenshots/artifact and Actions
links will be added after actual execution.

## Read-only production baseline limitation
2026-10-09 workspace GETs to `/api/health`, `/api/exams/exam/deletion-info`,
`/api/generated-tests/test/deletion-info` and `/api/attempts` all received HTTP403
with safe body `error code: 1010` and no X-Request-ID using Python's default
client. This is an edge response, not evidence that Worker route authorization
or D1 failed. A supported client retry and authorized deployment workflow smoke
checks are recorded separately. No authenticated production delete is executed.
Worker deploy dry-run bundles successfully (316.68KiB); configuration readiness
output is not a live provider/D1 health test.

A curl browser-user-agent retry confirmed GET `/api/health` HTTP200,
body `{"status":"ok","app":"Eve Mock Test API","version":"2.0.0",...}`,
request ID `5f19516d-960a-4c6d-ba74-981cf135b8f5`.
GET `/api/exams/exam/deletion-info` without a token returned HTTP401,
`{"success":false,"error":"Missing or malformed Authorization header"}`,
request ID `3da83351-420f-4297-b23e-4f5fa6f87e7d`.
These checks verify edge connectivity/auth rejection, not an authenticated
admin deletion or deployed implementation SHA.
The workspace restarted once, preserving files but stopping a Gradle process;
the final settled-tree build was restarted rather than marked passed.

## Additional deletion/persistence repairs
- Syllabus/banner storage deletion before a failed D1 mutation reproduced in a
  real-handler mock-provider test (HTTP500, storage DELETE called once). Database
  confirmation now comes first. Incomplete storage cleanup returns an explicit
  notice and records `MEDIA_CLEANUP_INCOMPLETE` with a safe correlation ID,
  no object path/credentials and no fake Retry action. Existing30-day retention
  applies. Record deletion and media cleanup are distinguished.
- Banner UI ignored `Result.failure`; it now unwraps failures in the shared
  confirmation flow. Banner/syllabus deletes have no misleading Undo; repeated
  taps are guarded and UI refresh follows a confirmed outcome.
- Answer and Clear Response taps save the actual session immediately instead of
  waiting up to five seconds for periodic saving.
- A stale source assertion expected the former single-argument bundle restore
  call. It failed in the196-test Android run and was updated for exam-key scope.
  This does not substitute for the added actual session policy/store/UI tests.

Backend scoped commit `017d447bd3263eca9d63603ec92d59c7e281438f` was pushed directly
to origin/main and verified with git ls-remote. 121 backend tests and Worker dry-run passed before that push. The typecheck
actually failed: its yielded output was not collected, and a following test
command masked the exit status. This is corrected below. Android changes were subsequently built and pushed;
this scoped backend push is not a claim that the entire task is complete.


### Corrected compiler evidence
The first backend deployment run37868829247 failed at TypeScript checking before
any deployment. Its nullable route/header type errors were also present in
queued local output, missed because the shell sequence did not fail fast.
Earlier local-typecheck-pass statements were incorrect. Route IDs now receive an
explicit empty fallback, and media cleanup accepts nullable header values.
Validation now uses `set -e` and collects completion output. No security rule was
weakened; the initial failed deployment left the prior Worker in place.


### Concurrent work preserved
A concurrent origin/main commit1b67018 updated AGENTS.md. Direct push was rejected
(non-fast-forward); it was merged normally without conflicts or discarding any
local Android changes. New operating/scope/final-report instructions were read.
Merge commit f7d0c781ea390b7631b6a31487acdd885807016f includes the strict-type fix
and was pushed/verified remotely. Fail-fast local TypeScript checking and all121
backend tests passed after the correction. Corrective deployment CI succeeded, as recorded below.

### Connected extra fixes
Feedback-post deletion also ignored Result.failure in Home/Admin paths. The
shared guarded flow now unwraps errors and reports success after confirmation;
Home removes the original post from its state after confirmed deletion. Poll
management uses the same honest confirmation rather than claiming deletion
while its delayed request was still pending. Question/broadcast handlers already
confirm the response before success; their business logic is preserved.

### Decisions and follow-up suggestions
One AppCompat recreation replaces the reveal to reduce lifecycle/system-bar
races. Reattempt keeps history instead of erasing metrics. Incomplete storage
cleanup is explicitly diagnosed, with no misleading automatic Retry because no
safe retry payload is retained. Result uses adaptive existing arrangements,
without changing approved Mock Test styling or approving pending Result styles.
Potential future work requiring separate scope: isolated staging QA accounts;
API24/API36/OEM UI coverage; a reviewed non-destructive orphan-media cleanup
queue. No additional premium visual design was implemented.

### Authorized Worker deployment
Corrective merge f7d0c781ea390b7631b6a31487acdd885807016f deployment succeeded:
https://github.com/vishu762701/eve-mock-test-app/actions/runs/37869703292
The workflow ran checking/tests, found no pending D1 migrations, and deployed
Worker version4585e301-7735-4e4a-98d6-dacefbf7f1e8. Its public health smoke probe
returned200. This is an authorized workflow deployment, not a direct production
migration or destructive production test. Authenticated generation/submission
still require a signed-in isolated test account; staging access was requested.


## Verification commands and coverage boundaries
Commands run locally:
- `JAVA_HOME=/workspace/tools/jdk /workspace/tools/gradle-8.7/bin/gradle assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug` with the documented workspace proxy/truststore/memory flags. Exact command and complete output are retained in `/workspace/tools/eve-android-final-pass.log`.
- `set -e; npm run build --prefix backend; npm test --prefix backend` — actual fail-fast completion exit0;121 tests,0 failures,0 skipped. Output `/workspace/tools/eve-backend-failfast.log`.
- `npm run build --prefix functions` and `npm test --prefix functions` — syntax/build exit0 and16 tests,0 failures.
- Backend working directory: `npx --no-install wrangler deploy --dry-run` — bundled successfully; actual deploy used the authorized workflow instead.
- `git diff --check`; XML parsing; `bash -n .github/scripts/verify-native-ui.sh`.

Current-flow review: Login uses Firebase Auth and authenticated ApiClient;
logout clears session/premium cache and signs out Firebase/Google. Home/search/
category/filter data comes from existing HomeViewModel repositories. Result,
history, performance and leaderboard consume confirmed Worker attempts/stats.
Notifications/FCM and optional Firestore/Functions require live credentials for
full-runtime checks. No old report is treated as proof of those live flows.
Existing older tests include simulations/source checks; the new critical
backend tests execute actual handlers/SQL, and new native tests execute actual
views/dialogs/window lifecycle with isolated network/session fixtures.
All-screen authenticated UI alignment, OEM/OS combinations and real production
AI/timer/offline recovery are **Unverified** without suitable test access; the
following emulator results must not be generalized beyond their stated scope.


### Settled Android local result
`/workspace/tools/eve-android-final-pass.log` ends BUILD SUCCESSFUL in9m13s,
86 actionable tasks (21 executed,65 up-to-date); the Gradle process returned0.
JUnit XML totals28 suites,196 tests,0 failures,0 errors. Lint XML totals0 errors,
1525 warnings. No lint suppression or exception-swallowing fix was introduced.
Both app and instrumentation APKs compiled. Native tests are not marked passed
until the pushed-commit emulator workflow actually executes them.


### First native run and test synchronization correction
Code commit43d4c58d3b9d47422d2300237a67ad112ea371f1 was pushed/verified on main.
Run37870654344 executed16 API35 gesture-navigation tests:15 passed,1 failed.
The deletion regression called Espresso before asynchronous preflight had opened
the confirmation dialog; Espresso selected the base activity just as it lost
focus (`RootViewWithoutFocusException`, AdminDeletionRegressionTest.kt:94).
Commit069840994c62be26d8fcf7688c122c0d1569ec86 waits for the visible accessibility
dialog and explicitly targets its root. The complete scenarios are retained;
no assertion was removed. Three-button execution was blocked by that first
suite failure and is not counted as passed.
The first native artifact was downloaded and its four full-screen gesture
Mock/Result PNGs visually inspected. Result statistics labels fit in two columns
and selected options show the approved green in both themes. Scrolled analytics
captures and render-idle synchronization were added for the next run. Native
results will be finalized after run37871263268 completes.


Run37871263268 again executed16 tests with15 passing; the remaining focus race
was at the later HTTP409 error-body assertion, not the preflight assertion.
Commit4d3fd8a64ea4e0e2ea14a01d7bd803a0b66b010e synchronizes both kinds of dialog
and explicitly targets every error-dialog assertion. It also asserts status-bar
icon appearance and actual positive scroll position before analytics capture.
The prior attempted analytics image was not scrolled and is not used as
analytics evidence. Both failed workflow results remain visible on GitHub.

The user delegated selection of the test environment. Existing configured
Firebase/Worker infrastructure and isolated fixtures were selected to avoid
creating paid services or manipulating production student records. That choice
does not supply a signed-in test identity. Authenticated live testing remains
blocked by missing account access, not by a request for another preference.


Run37871777964 passed all three jobs, including16 gesture native tests and one
three-button theme test. Screenshot review then found two evidence weaknesses:
the attempted analytics capture was still at the top, and the dark three-button
icons were dim despite the reported appearance flag. This passing run is not
claimed as complete visual acceptance. Commit79a08aa5cd45a4f56a0c6a865455a82ddc414cae
waits for SystemUI rendering/tint to settle and asserts actual contrasting pixels
in the navigation area. Final screenshots must be inspected after that run.


Run37872294394 failed the new pixel assertion on Light gesture navigation:
the initial arbitrary RGB<100 threshold rejected Android's gray gesture pill.
Commit04914087bc554e32c6bf9416510f4ecdd78ab2fa uses WCAG relative-luminance
contrast>=3:1 against the visible black/white navigation background instead.
Dim dark-theme three-button icons remain below that requirement and will fail.
The delayed dark analytics screenshot was actually scrolled and visually
inspected:100.0%, Attempted and Accuracy are readable; the unconfigured cutoff
explains that qualification cannot be determined. No misleading top-only
analytics capture is presented as that evidence.


Run37872711338 passed16 gesture tests but failed the measured dark three-button
contrast assertion. This is a verified rendered failure, not waived as a test
problem. Android15 framework source shows DecorView.setWindowBackground can
set APPEARANCE_FORCE_LIGHT_NAVIGATION_BARS independently of the ordinary flag.
The helper now synchronizes the visible window background for API35+, avoids
obsolete bar-color setters there, sets appearance after background/contrast
configuration, and reapplies when the app window obtains focus. Older APIs retain
bar-color handling. Commit2b2f7be8335c5e23641d1ab22d6213a47bd87a0e was pushed and
verified on main; native pixel tests must pass before visual acceptance. Framework
source evidence: android.googlesource.com/platform/frameworks/base/+/refs/heads/android15-release/core/java/com/android/internal/policy/DecorView.java
(setWindowBackground) and PhoneWindow.java (setNavigationBarColor).


Run37873302479 still failed dark three-button contrast after all16 gesture tests
passed. The background/focus change alone was insufficient and is not reported
as a visual fix. Local full Android verification for2b2f7be passed in5m48s.
PhoneWindow.setNavigationBarColor explicitly clears FORCE_LIGHT_NAVIGATION_BARS,
but can return early when the same opaque color is already forced. API35+ now
uses the standard transparent edge-to-edge navigation color over the matching
window background; no hidden API, contrast scrim, alternate-color toggle or
weakened pixel requirement was introduced. Commit3d05b044f9b583d94fb3d626ed7a97146d2176c5
was pushed/verified. Rendered contrast must still be verified by its workflow.


Run37873931200 again passed16 gesture tests and failed dark three-button
contrast. Transparent/background/focus handling is not yet proven to resolve
this failure. The Android framework forced-appearance explanation is a
source-supported hypothesis, not a measured emulator root cause. Diagnostic
commit14843415d81715afc61826f78b04ada0a082a4bf preserves failed PNGs and captures
focused-window appearance/insets and bounded SystemUI state before further
changes. The3:1 test requirement remains unchanged.


Diagnostic run37874462085 measured focused Eve window=true, appearance=0,
legacy flags=0, navigation insets=48px and secure navigation_mode=0 while the
three-button screenshot remained dim. Thus the ordinary and forced application
appearance bits were clear at capture; the forced-bit hypothesis does not
explain this persistent test failure. SystemUI/window dump filters returned
empty data after teardown and are not treated as verified SystemUI health.
The regression immediately toggled a just-launched activity after a navigation
overlay swap, before an initial Light frame had been observed. Commitda1df60aa3d3aca55a4a4d86cc0cfc95f2f409e2
renders/captures the Light window before the actual Light->Dark->Light actions,
keeping the same3:1 contrast assertion. This setup explanation remains under
verification rather than being reported as the final root cause in advance.

Read-only connectivity with Android's OkHttp4.12.0 user agent returned200 from
the configured Worker/api/health; request IDaba3b9dc-9f51-4ef4-8ca2-e05d5d0ab5c2.
The earlier Python edge403 does not reproduce for that user agent. This is not
a signed-in generation/submission test.


Run37875096645 still failed the same three-button condition after an initial
Light frame. That setup hypothesis is not reported resolved. The empty SystemUI
files were explained by hosted-runner `rg: command not found`; grep is available
and replaces that filter in665998a760e6d5fe5a138cc9959a7f0ec5d31742. A bounded
SystemUI capture now runs while the failed app window remains focused. This is
verification infrastructure repair; the actual contrast assertion is unchanged.


Run37875535770 was blocked earlier by a banner confirmation focus race
(AdminDeletionRegressionTest.kt:57). Commit7bf5b8dbc2c965561f225c369a1826dda00b84b2
adds the same visible-dialog wait and explicit dialog root before that click.
The three-button phase did not execute and is not counted as passed. Hosted
diagnostics now use grep because ripgrep is absent.


Run37876181142 captured the state while the failed app window was focused:
appearance=0, flags=80810100 (draws system-bar backgrounds), legacy=0,
navigation inset48px. SystemUI simultaneously reported mNavigationLight=false,
mHasLightNavigationBar=false, mDarkIntensity=0 and no force-for-scrim flags,
but the three-button PNG remained dim. This disproves an app appearance-bit
mismatch as the remaining cause. Commitbd388d8aef5c37a55edb16c3677a9e8bdc2298e6
boots the emulator into its selected three-button mode before running the
same rendered3:1 theme regression, isolating live navigation-overlay renderer
state. Reboot is confined to the disposable CI emulator, not production devices.
No application-color workaround or weakened assertion was introduced. Its
result remains pending until actual execution.


Run37876832512 still reproduced the three-button failure after a verified reboot
into navigation_mode=0, disproving hot-swap alone as the explanation.
Commitc01925269f07bc2e9387b365af27180fa4a4f3cd adds a read-only stock Clock
control screenshot on failure and runtime SystemUI light/dark-color lookup.
The test failure status remains nonzero; the control never substitutes for a pass.

Parallel comparison uses the official Google APIs API35 image (d603820) and an
explicit Pixel2 phone profile (bf1916c2e9bce254a29c7b265c3e44fb83f33548), matching
the Firebase/Google application environment. Navigation pixel checks use actual
WindowInsets navigation bounds at any density instead of fixed12/30 pixel rows,
retaining the3:1 requirement. Original AOSP evidence remains retained and is not
claimed resolved merely by selecting another image.


AOSP stock Clock control launched successfully and focus was verified on
com.android.deskclock/.DeskClock. Its dark-blue screen also showed the same dim
three-button indicators; SystemUI reported navigationLight=false/darkIntensity=0
for that independent app. Runtime color lookup returned light=#ffffffff and
dark=#99000000. This is evidence that the remaining generic-image rendering
problem occurs outside Eve too; it is not waived as an Eve success.

Pixel2 run37878483545 stopped on older approved-design test rounding assumptions:
14sp rendered37px (14.095238sp at2.625density);38dp correctly rounded to100px
while a test truncated to99;13sp rounded to34px. Commitbf1622a21f4866bf1507da12f63d0c27be373bf0
compares against Android's SP conversion with half-physical-pixel rounding
tolerance and rounds integer dimensions as Android does. Approved app fonts,
colors and sizes did not change. Both navigation suites now execute independently
and the overall workflow fails if either returns nonzero.


## CI #227 evidence correction

See [EVE_CI_NATIVE_VERIFICATION_REPORT.md](EVE_CI_NATIVE_VERIFICATION_REPORT.md) for the focused #209–#227 investigation. An unchanged #227 rerun reproduced the spacing assertion and SystemUI ANR. The retrieved Light and Dark three-button screenshots contain a system ANR dialog; the app is not focused. These captures cannot prove an app navigation-color defect. Earlier candidate explanations above remain historical, unverified hypotheses. This continuation changes CI/tests only and preserves production UI.

# EVE Telegram final mission checkpoint

> Active continuation: see the latest entry below and TELEGRAM_REMAINING_REQUIREMENTS.md. Whole mission is PARTIAL; historical next-action paragraphs are superseded by the current source/CI state.

Status: PARTIAL. This is a continuation record, not screen acceptance.

## Official-source continuation requirements — 2026-11-03

Recovered remote main `3c316522ae0596b49acedac23f55b8728131bcf2`, clean checkout,
main only. FAST 41640467269 failed in Android JVM tests (1077 run, one failure):
`AdminPremiumResponseContractsTest.legacyWorkerSnakeCaseConfigIsReadWithoutClientDefaults`,
Gson numeric boolean at `is_enabled`. Backend and Firebase jobs succeeded;
FAST native/publication jobs were skipped. No concurrent change found at recovery.

| Requirement | Canonical owner | Implementation status | Tests | Commit SHA / exact-SHA CI | Runtime acceptance / remaining evidence |
| --- | --- | --- | --- | --- | --- |
| R1 Premium narrow boolean repair | AdminPremiumConfigDto / existing Gson Retrofit converter | MET at host/FAST scope; preserve permissions, billing and entitlement owners | Real DTO valid/invalid/null/alias-conflict deserialization and acknowledgment tests | 74836d344a38833dfe7c34e7e0768f62738d82d6 / FAST41643535083 SUCCESS | physical premium matrix remains open |
| R2 Official Telegram retrieval and provenance | Existing source maps / notices | MET direct authenticated clone/pin; existing revision retained | Git identity, source/asset hashes and dependency verification | no import authorized by evidence alone | matched source/runtime comparison outstanding |
| R3 System Monitor retry and read lifetime | SystemMonitorActivity / existing API and Worker | MET scoped implementation/host/FAST | suspended permissions, denied rights, owner/stop, duplicate requests, failed acknowledgments, stale reads | 93e2f87717a630bfad6404de5a2a5060842e5cd8 / FAST41646834423 SUCCESS | device lifecycle and role matrix outstanding |
| R4 Remaining Admin functional audit | Existing Dashboard Activities, repositories and Worker routes | NOT MET as a whole; preserve listed green implementations | inventory actual UI→Worker actions and behavioral acceptance; repair demonstrated defects only | existing per-feature budgets apply; Syllabus already has two green commits | no destructive production tests or deployment |
| R5 Home/shared Telegram fidelity | Existing Home, drawer, popup, dialog and theme owners | NOT MET whole runtime fidelity; preserve completed actions/search/isolation | upstream maps plus available behavior and native checks | existing feature budgets apply | matched device, Hindi/RTL/font/a11y matrix outstanding |
| R6 Profile and personal QR acceptance | Existing Profile/viewer/scanner/native owners | NOT MET whole acceptance; preserve existing systems | existing fidelity contract and actual caller/runtime evidence | no restart or speculative replay | matched references, camera/GPU/device evidence outstanding |
| R7 Student/quiz/shared contracts | Existing canonical Activities/ViewModels/repositories | NOT MET whole acceptance; preserve repaired account and scoring boundaries | reconcile real flows; add tests only for missing contracts/reproduced defects | existing feature budgets apply | timer/navigation/recreation/device matrix outstanding |
| R8 Duplication prevention | PRODUCTION_OWNERSHIP.md and existing ownership tests | ONGOING; no second registry or manager | trace overlapping callers before any removal | scoped changes only | static checks do not establish universal absence |
| R9 Integration and release boundary | Existing build.yml and release compliance gate | NOT MET final integration | each batch exact-SHA FAST; eligible independent FULL checks later | FULL includes unconditional successful-gate debug APK artifact publication, so do not dispatch without separate authorization | publication approval separate; production/legal release NOT REQUESTED, human compliance unresolved |

R1 local verification: reproduced original four-test suite FAIL (one failure,
`Expected a boolean but was NUMBER`). After the scoped class-level Gson adapter,
six production DTO/acknowledgment JUnit tests PASS, including all four flags in
both naming forms, numeric/boolean values, malformed/null/missing/contradictory
and duplicate fields, generic ApiResponse parsing, and canonical serialization.
Kotlin 2.1.0 standalone compiler / Gson 2.11.0 / JUnit 4.13.2 used actual production
sources; this is not a full Android build. Existing host Python suite PASS and
acceptance-claim structural check PASS (all three targets remain OPEN).
Class-level adapter preserves ApiClient's Retrofit configuration and all other
DTOs; no backend, entitlement, payment or permission behavior is changed.
Exact-SHA FAST is pending on the carrying repair. Next: retrieve and pin official
Telegram source only after FAST succeeds, then SystemMonitorActivity.confirmRetry.

No agent delegation, production Worker deployment, D1 writes, APK publication,
release-variable changes or legal self-approval is authorized or performed.

## Verified Premium repair and authenticated source recovery — 2026-11-03

R1 repair: `74836d344a38833dfe7c34e7e0768f62738d82d6`, exact-SHA FAST
[41643535083](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41643535083)
SUCCESS. All required build/backend/Firebase jobs succeeded; native navigation
and APK publication were skipped. Rust C API suite: 243 passed. Docs 41643535071
SUCCESS. Original failed FAST 41640467269 remains visible and is not reclassified.
The failed fixture is retained; absent-only original input now explicitly must
throw, and a separate complete-flag/incomplete-config input still fails validation.
Premium has one successful implementation batch after forward repair; physical
payment/admin acceptance and Worker configuration validation remain outstanding.

R2 source retrieval: direct HTTPS Git clone from `https://github.com/DrKLO/Telegram.git`
with `--filter=blob:none --no-checkout`, followed by detached full checkout. Official
`https://telegram.org/apps/android` independently links `https://github.com/DrKLO/Telegram`.
Live remote master, origin/master and checkout HEAD all resolve to
`3de37c8cfb039452be7398909461da6288babf15`, root tree
`cc3c6ccf48e4167fdec764dfd04e765043f55093`. This is actual live retrieval,
not an assumed historical pin or a ZIP dependency. Existing authenticated mappings
use the same revision, so no pin upgrade or source-value change is required.
Only required `TMessagesProj/jni/tlottie` initialized at upstream gitlink
`31f1b542f88e7b4be9a01e749920d857535fc715`; 18 unrelated submodules remain
uninitialized. `.gitmodules`, all 19 gitlinks and root GPLv2 inspected. The existing
129-entry native/QR manifest matches source and EVE SHA-256 values (zero mismatches);
existing four-entry verbatim asset verifier PASS. These inventories do not prove
complete app provenance, dependency closure, runtime equivalence or legal clearance.

Source opened for this recovery includes `ActionBar/AlertDialog.java:302-332,1137-1215`
and full `ActionBar/DrawerLayoutContainer.java:1-214`. Its current `onTouchEvent`
returns false: do not claim this revision provides classic drawer swipe physics.
The existing EVE AndroidX drawer stays an explicit adaptation. No Telegram account,
network, authentication, service credentials or unrelated native libraries imported.

| Telegram path:lines@full SHA | EVE path:lines | Classification / limits |
| --- | --- | --- |
| TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:302-332,1137-1215@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt:64,237,272 → SystemMonitorActivity.confirmRetry | ADAPTED existing renderer, native message/confirmation and lifecycle subset; preserve its current source-backed resources; matched window/blur/animation acceptance OPEN |
| TMessagesProj/src/main/java/org/telegram/ui/ActionBar/DrawerLayoutContainer.java:49-57@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/home/MainActivity.kt (existing AndroidX drawer) | NOT FOUND for requested classic gesture physics in this class; ADAPTED current EVE host; no invented transplant |
| TMessagesProj/jni/tlottie@31f1b542f88e7b4be9a01e749920d857535fc715 (official superproject gitlink above) | app/src/main/cpp/vendor/tlottie; existing qr-native-upstream-manifest.json | existing exact/adapted file classifications retained; 129 recorded source/EVE identities match; complete device/reference/legal acceptance remains OPEN |

R3 current preparation: actual SystemMonitorActivity.confirmRetry dispatches using
entry-time rights only, and read/retry responses can survive stop/account replacement.
The Worker already enforces both view_analytics_logs and manage_generated_tests;
its generation implementation and request key remain canonical. Prepared actual
Activity transport-boundary tests cover suspended fresh rights, revoke/account/stop,
duplicate dispatch, failed/malformed acknowledgment and newest-read ownership.
No second job manager, backend contract or dialog renderer is being created.

## System Monitor retry/read boundary implementation — 2026-11-03

R3 scoped implementation is in the existing Activity. Capture initiating UID/email
before the entry gate; dispatch requires fresh membership and validated
manage_generated_tests AND view_analytics_logs. Acquire pending before awaiting
rights; cancelled/denied/foreign/expired work cannot dispatch. The existing
Telegram confirmation remains; successful retry requires Worker testId, jobId and
positive integral questionCount. No automatic retry or confirmed-job repeat.
Read generations and stop epochs suppress obsolete success/error/finally effects;
Activity-owned jobs/dialogs cancel/dismiss on stop/destroy. Reads resume from the
existing screen without creating another generation manager or backend contract.

Actual Activity baseline: 10 tests, 9 failures (seven explicit assertion failures,
two because the old path never suspended for fresh membership). The analytics-only
entry control passed. Expanded actual Activity/ack suite now has 18 tests PASS,
plus two retained/strengthened dialog source contracts. New suspended permission
snapshot cases exercise getAdminMe as well as membership. Transport and SDK are
synthetic boundaries; Activity, native dialog, existing gate and callbacks execute.
This is host evidence, not physical account/device/runtime acceptance.

Stable local integration command (toolchain/proxy/CA configured outside Git):
`gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
-Peve.nativeAbis=x86_64 --init-script /workspace/scratch/eve-test-runtime.init.gradle`.
PASS: 1097 JVM tests / 238 suites, zero failures/errors/skips; AndroidTest source
compilation and lint PASS. The historical bare SDK35 shutdown diagnostic remains
UNKNOWN (no JUnit error/failure), not suppressed or represented as crash-free proof.
65 existing/extended Python helper tests PASS; docs validation (82 files), scoped
acceptance structural validation and diff check PASS. Two actual Worker isolated
SQLite/Hono tests PASS: compound retry permissions and generation monitor results.
Worker production source is unchanged from green Premium FAST; no deployment.
No local APK assembly or native emulator execution claimed for this batch; exact-SHA
FAST must compile all ABIs/builds before advancing unrelated implementation.

Source provenance check: both existing Profile maps' 57 upstream-file/asset entries
match source and EVE asset SHA-256 values. Corrected one demonstrated metadata typo:
regression-source-map.json.root_tree contained the commit ID instead of the actual
Git root tree. Revision, assets, renderer code, fidelity OPEN claims and notices
are unchanged. The 129-entry native manifest/four verbatim imports also match as
recorded above. Local adb reports zero attached devices; /dev/kvm absent.

Requirement status: R1 repair MET at host/exact-SHA FAST level; R2 official retrieval
MET (whole-source fidelity still NOT MET); R3 implementation/host MET, carrying CI
PENDING, physical acceptance OPEN. R4–R9 remain as scoped above. Next after exact-SHA
green: Premium Worker config malformed-value validation, then remaining real Admin
controls. Syllabus reassign retains two green feature commits and needs a concrete
reviewed exception before further integration; its two-write transfer is unaccepted.
Existing EditExam Delete/Undo timing was pending at that checkpoint; owner later explicitly selected the five-second deferred DELETE (see latest decision below); no inferred restore API or timing change. No publication/release/deployment.

## Premium Worker validation continuation — 2026-11-03

Predecessor `93e2f87717a630bfad6404de5a2a5060842e5cd8` exact-SHA FAST
41646834423 SUCCESS: build/backend/Firebase; native navigation/publication skipped.
Docs 41646834336 SUCCESS. R3 host/exact-SHA integration MET; device acceptance OPEN.

R4/Premium remaining requirement before implementation: existing full-config PUT
coerces null/malformed price, duration, expiry, flags and payment fields, and malformed
JSON becomes a defaulted replacement. Actual isolated Hono/all-migration SQLite
baseline: six tests, three failures (malformed JSON accepted, null price defaulted,
and absent D1 write evidence acknowledged); three valid/permission/rollback controls
PASS. The supported Android caller always sends the complete AdminPremiumConfigDto.
Repair will require complete typed fields, preserve existing INR/client numeric bounds
(price 1..1000000, duration 1..3650, expiry 1..1440), explicit boolean flags, payment
strings and benefits list. Invalid/missing critical inputs must return 400 before
config/audit writes. No new pricing, entitlement, billing, payment-method, secret or
production policy is introduced; existing valid full DTO and legacy GET aliases stay.
Atomic config+audit remains; missing D1 confirmation must never become success.
Also inspect the same Premium Activity's stop/resume mutation lifetime with suspended
rights and acknowledgments before consuming its second/final successful batch.

Supabase keepalive investigation is separate: run41629517670 failed at27bc15d5.
Workflow has an old fallback hostname. Repository secret metadata inspection returned
403 Resource not accessible by integration; configured URL/key readiness is UNKNOWN.
No replacement hostname, credential, production request or secret write invented.

## Premium validation/lifecycle batch local evidence

Actual Worker handler plus all-migration isolated SQLite: six focused regression
tests PASS; full backend329 tests PASS and TypeScript build PASS. Expanded cases
preserve seeded existing payment orders and entitlements through valid config
writes, reject malformed/missing/typed/range failures without writes, verify fresh
rights/membership revocation, audit rollback and missing D1 evidence. A standalone
cross-layer host check serialized the real Android AdminPremiumConfigDto/Gson,
sent those bytes through the real Worker Hono route/SQLite transaction, fetched
its legacy-plus-canonical GET and parsed back through the real Android DTO adapter:
PASS equality. This is synthetic local transport/identity evidence, not live Worker,
Firebase authentication, deployed D1 or device acceptance.

Actual Premium Activity baseline: 8 tests, 2 assertion failures (stop/return late
permission dispatch and late PUT acknowledgment starting a new readback). All8
now PASS; existing acknowledgment suite plus fractional/malformed entitlement
numbers now7 PASS. Stop epochs apply to all existing mutation calls; config's
follow-up GET and success state must belong to that epoch. No mutation replay.

Broad local integration first found competing ApiClient shadow types on the Kotlin
singleton. One shared TEST-ONLY transport shadow now serves both actual Activities;
Monitor18 + Premium8 + acknowledgment7 run together:33 PASS. No production API
injection point or generic manager was added. Subsequent broad run had95 failures,
all temporary disk exhaustion during native-Skia extraction in this 8-GiB executor.
External init-script serial JVM groups (forkEvery3) bound temporary extraction;
every test/assertion remains enabled and repository/CI workflows are unchanged.
Stable full JVM rerun:1106 tests /239 suites PASS, zero failures/errors/skips.
AndroidTest compilation and lint PASS (zero errors;1626 warnings, no new suppression/baseline). This batch still requires its own
exact-SHA FAST before unrelated implementation. Runtime/device and legal/publication status stay OPEN.

## Owner Delete/Undo decision — 2026-11-03

Owner explicitly answered: "Telegram jaisa: 5-second Undo window ke baad DELETE bhejo
(Recommended)". This authorizes the existing EditExam question Delete/Undo to defer
its backend DELETE until the five-second Undo window expires. Undo must cancel
the pending deletion; do not call the unsupported UPDATE-as-restore path. Reuse
existing AppUndoBar/native confirmation/repository/Worker authorization. Preserve
standalone ManageQuestions confirmed immediate deletion and Bookmarks Undo.
Implement this chosen contract after the carrying Premium exact-SHA FAST succeeds;
no production deletion, new restore API, backend deployment or migration authorized.
The earlier paragraphs saying the timing choice is unanswered are historical.

## Owner-selected EditExam deferred Delete/Undo (owner-approved integration batch)

Live main `78781fcf45de032000be72471ab5c1695db1380b`, exact-SHA FAST41655102457
SUCCESS (all required jobs; native/publication skipped, artifact count0). Premium
second/final scoped batch is integrated; prior green implementations preserved.

Owner product decision above authorizes five-second deferred deletion. Baseline
actual EditExam Activity:8 cases,7 failures; current DELETE executes before Undo and
Undo invokes UPDATE on the deleted row. Preserve existing native confirmation,
canonical currentQuestions/adapter, AppUndoBar, fresh server rights and Worker
question deletion. Requirements: U1 no DELETE before5000ms; U2 Undo cancels without
UPDATE; U3 one pending/dispatch and exact Worker acknowledgment; U4 fresh original
UID/email + manage_questions/entry right at expiry; U5 account/stop/navigation
cancellation and stale-response rejection; U6 row/controls recover on denial/error;
U7 preserve other AppUndoBar callers' current immediate/server-confirmed behavior;
U8 tests, provenance and an explicit budget exception before integration if needed.

Budget evidence independently checked: AppUndoBar introduction
09aa2ae5e1a07a9d578c6f2bc3f729cb1dfa8783 / Build36316965199 SUCCESS; immediate-delete
change2cc00e4d3a96f8c1854ced4aea570de20b08fed7 / Build36530476892 SUCCESS.
These are two successful earlier commits for the same Undo feature. The owner's
timing answer is recorded as product authorization, not an inferred budget waiver.
Owner explicitly approved one third-successful-commit exception for this tested Undo
repair in the active conversation: "Haan, is tested Undo repair ke liye ek exception
allow hai". This is scoped to this patch, not a waiver for other features.

Official source actually opened: UndoView.java:384-403 (hide apply/cancel callbacks)
and1685-1712 (5000ms arc/expiry), at3de37c8cfb039452be7398909461da6288babf15.
EVE must adapt this semantic contract to its existing Activity/Worker callbacks;
Telegram MessagesController/account/deletion services stay excluded. No full source
UndoView renderer or matched native behavior is claimed. A main-Looper deadline,
independent of animation scale, is required for the authorized minimum Undo window.
Stopping/recreating cancels uncommitted work; an already-dispatched request remains
server-authoritative and must be revalidated, never replayed as an invented restore.

Undo verification: original eight-case Activity baseline had seven failures. Final
actual Activity + shared Undo + existing EditExam integration/source contracts:
24 tests PASS, zero failures/errors/skips. Covers saved-state recreation, suspended
fresh rights, late Worker success after stop/return, and the unchanged legacy
immediate callback. Robolectric virtual vsync is explicitly paused so idle() cannot
advance through the five-second deadline; production deadline remains a Handler
independent of ValueAnimator. AndroidTest Kotlin compilation PASS. Lint PASS with
zero errors/1622 warnings (no new suppression); 65 host tests,82-document check,
acceptance structural check, original asset verification and diff checks PASS.
No device/production mutation, deployment, artifact publication or parity claim.

| Requirement | Canonical owner | Implementation status | Tests | Commit SHA | Exact-SHA CI | Runtime acceptance | Remaining evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| U1–U7 owner-selected question Undo | EditExamActivity / AppUndoBar / ExamRepository / existing Worker | Implemented locally; approved one third-successful-commit exception | 24 JVM; AndroidTest compile; lint; host65 | carrying commit | required after push | OPEN | physical gestures, failure/recreation matrix and visual comparison |

Source evidence (behavioral adaptation, not renderer equivalence):
`Telegram UndoView.java:384-403,1685-1712@3de37c8cfb039452be7398909461da6288babf15`
→ `app/src/main/java/com/eve/app/util/AppUndoBar.kt:69,94,160`
→ `app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt:806,848`: ADAPTED.
EVE's existing native dialog, countdown renderer and Worker are retained; Telegram
account/message services are excluded. No upstream asset copied in this repair.

## Live mission continuation: 2026-11-03 (implementation evidence, not final signoff)

Recovery began at the audited main 23f4f9788eedff5e8d3895a21a2a3edbf46d97be.
The original Bookmarks server-confirmed read/remove and ownership-registry
implementations were preserved, not reintroduced. The source ZIP
Telegrammaster.zip SHA-256 is
4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10,
with 14,794 safe-path entries and ZIP integrity PASS. Five representative
Git blob hashes match official DrKLO/Telegram
3de37c8cfb039452be7398909461da6288babf15, not a license or
whole-archive revision attestation. No Telegram files were transplanted in
these EVE-only functional repairs.

| Scoped requirement | Canonical implementation and callers | Implementation / acceptance | Exact-SHA evidence | Next action | Successful commits consumed in this continuation |
| --- | --- | --- | --- | --- | --- |
| Student PYQ current exam/request | PyqViewModel.kt / PyqActivity.kt → ExamRepository | IMPLEMENTED / DEVICE NOT YET ACCEPTED | e8f49234dd379d1c9f23f2b9323838e682558f1a; FAST 41612644017 SUCCESS | Device selection/recreation/account matrix | 1 |
| Standalone question import ownership | ManageQuestionsActivity.kt → QuestionImportHelper / AdminPermissionSnapshot / ExamRepository | IMPLEMENTED / DEVICE NOT YET ACCEPTED | 6e6ff042a9e76b948ad9a53e74b64a0d1d554a6b; FAST 41614129880 SUCCESS | Provider/rotation/auth-mutation hardware matrix | 1 |
| Exam image decoding and current account | ExamImageHelper.kt / ManageQuestionsActivity.kt | IMPLEMENTED / DEVICE NOT YET ACCEPTED | cf54cb57 initial test failure 41616305949; 976471a8 follow-up red due Maven 429 and malformed input; 80b2718c FAST 41618909774 SUCCESS and dependency evidence 41618909759 SUCCESS | Physical content provider / image selection matrix | 1 |
| Admin JSON export ownership | AdminActivity.exportExamData → existing Worker /api/admin/export/exams | IMPLEMENTED / DEVICE NOT YET ACCEPTED | fe047f6eafee3b9e2081b8e5a6d4323f4d1a50f8 FAST 41620583493 SUCCESS; docs 41620583357 SUCCESS; Worker/D1 production state UNKNOWN | UI/account/partial-page device matrix and live-service approval separate | 1 |
| Admin Home Banner success contract | AdminActivity → HomeBannerRepository | IMPLEMENTED / DEVICE NOT YET ACCEPTED | 4602c5e3630a5c3c2393680bc21afb4afde2f56f; FAST 41621866480 SUCCESS; documentation 41621866537 SUCCESS; Result.getOrThrow, fresh rights, one-flight and bounded 5 MiB read regressions | Physical image-provider/rotation/account changes and real Worker confirmation | 1 |
| Admin Feedback Posts confirmation and owner isolation | AdminActivity → FeedbackRepository → existing Worker feedback endpoints | IMPLEMENTED / DEVICE NOT YET ACCEPTED | 1a8d6fd7801fc9f5e826518c307582d41828cc5c; FAST 41623461744 SUCCESS; docs 41623461762 SUCCESS; nonblank ID and fresh `moderate_feedback` guard | Physical admin/recreation/account/network matrix and production service acknowledgment | 1 |
| Admin Floating Link confirmation and owner isolation | AdminActivity.showFloatingLinkDialog → FloatingLinkRepository → existing Worker `/api/admin/floating-link` | IMPLEMENTED / DEVICE NOT YET ACCEPTED; CI latest pending external recheck | a41dfec8a356b6b3bf608eeeddf1dac75b029dc1, FAST 41624909742 (check conclusion before claiming green); docs 41624909816 SUCCESS | Finish exact-SHA gate; real account/role/clear/rotation device matrix | 1 if FAST green |
| Active exam deletion account and fresh-right boundary | ManageQuestionsActivity.confirmDeleteExam → AdminViewModel.deleteExam → existing ExamRepository/Worker | IMPLEMENTED / DEVICE NOT YET ACCEPTED | 0071eac336c118828cd1daa42a80646305dfa258, FAST 41625781538 SUCCESS; docs 41625781547 SUCCESS; held-response Robolectric tests included, no production delete/Undo change | Authorized/denied/rotation physical-device matrix | 1 |
| Admin broadcast send/list/delete ownership | SendNotificationActivity → existing Worker `/api/broadcasts` | IMPLEMENTED / DEVICE NOT YET ACCEPTED | 568f48019d71cd59ba1f0b0bd6b3ab571d91173b FAST 41626893622 SUCCESS and docs 41626893662 SUCCESS; server ID/count and owner/right/single-flight guards | Physical inbox/admin role/rotation/accessibility and real-service confirmation | 1 |
| Admin config editor owner/fresh-right and AI health entitlement | AppConfigActivity → AdminRepository → existing Worker `/api/admin/config`, `/api/admin/ai-health` | IMPLEMENTED / DEVICE NOT YET ACCEPTED | 27bc15d56608229142ee4f04a31b19bd9107f68c; exact-SHA FAST 41629150715 SUCCESS, docs 41629150859 SUCCESS; bound UID/email, distinct Worker rights, server readback | Physical ON/OFF/return/rights matrix; no production config mutation. | 1 |
| Standalone Syllabus picker, upload, replacement and removal | ManageSyllabusActivity → existing ExamRepository and Worker `/api/exams/:id/syllabus` | IMPLEMENTED / DEVICE AND DEPLOYMENT NOT ACCEPTED | Android 9ad8784d FAST 41633160383 SUCCESS; server 027b2fe8 FAST 41634619466 SUCCESS; docs 41634619491 SUCCESS; 323 backend tests PASS (4 new route contracts). No Worker deployment. | Physical picker/rotation/account matrix, separately authorized Worker deploy/live service verification; reassign two-write atomicity OPEN. | 2 successful implementation commits |
| Admin rights save/dismiss fresh owner and confirmation | AdminRightsEditActivity → existing AdminViewModel/AdminRepository → Worker `/api/admin/admins/:email/rights` | IMPLEMENTED IN CARRYING BATCH / CI NOT YET ACCEPTED | Reuses fresh `manage_admins_rights`, prevents duplicate queues, rejects missing/mismatched Worker acknowledgment, suppresses late owner-changing callbacks; production fake-transport and held-response tests. The historical `40a7bb30` passed FAST; two earlier directly related commits had failed FAST, not independently successful. | Exact-SHA FAST on carrying commit, device role/rotation/revocation matrix; no production admin permission mutation. | 1 earlier green + 1 carrying batch |
| Admin premium config and manual entitlement actions | ManagePremiumActivity → existing EveApiService → Worker `/api/admin/premium/*` | IMPLEMENTED IN CARRYING BATCH / NOT YET CI ACCEPTED | Original UID/email, fresh `manage_premium` before each write, one in-flight mutation, no optimistic success, config GET readback, exact entitlement acknowledgment, legacy config key mapping and response-contract JVM tests. Existing payment QR and entitlement backend untouched. | Exact-SHA FAST on carrying commit; physical account/rights/rotation/network checks; production Worker and payment configuration need separate validation. | 0 before carrying batch |
| Personal QR and full Telegram screen parity | Canonical viewer/scanner/native/picker/emoji/export owners unchanged | IMPLEMENTED PORTIONS / WHOLE QR NOT ACCEPTED | Existing QR_ACTIVE_PATH_RECONCILIATION and acceptance validator strict personal-qr open | Reproduced functional defects only; preserve source composition differences and device/reference evidence | no new QR batch |
| Final FULL and distribution | Existing six-job build.yml; release compliance gate | NOT MET / HUMAN RELEASE REVIEW BLOCKED | FAST jobs do not run navigation emulators; FULL includes Eve-debug-apk artifact upload | Obtain explicit publication boundary before FULL dispatch; no production deployment or legal signoff inferred | n/a |

These source fixes have no Telegram whole-screen EXACT claim. Current tests are
host/CI evidence, not physical CameraX/HWUI/gesture acceptance. The existing
production ownership map is extended, not replaced. No Worker production deploy,
D1 migration, APK publication, release signing or legal approval took place.
Historical STAGED notes below must not be read as the current remote state.

Premium source-review risks (not silently fixed in this Android-only batch):
1. `backend/src/routes/admin.ts` premium config PUT currently defaults or coerces malformed price/duration/session inputs rather than explicitly rejecting every invalid value; payment product/price policy requires its own coherent server regression and release review.
2. Physical premium dialogs, permission-revocation timing and server readback have no device or production acceptance. The Worker is still the authoritative entitlement ledger; no payment simulation or live grant was executed.

Syllabus server acceptance boundary: backend source can be staged and host-tested
without deploying. The existing Supabase Activity Keepalive scheduled run
41629517670 FAILED due DNS resolution of its fallback project hostname
(`curl (6)`, HTTP 000). That does not establish a Worker production failure or
permission to fabricate a replacement project URL. Actual production Worker,
Supabase project status, storage permissions, and release signoff remain UNKNOWN.
The Syllabus reassign path still issues two independent writes; a successful
client upload does not make this transfer atomic.


Source-only related risks, ranked for independent follow-up (not repaired in this config batch):
1. Syllabus reassign in `ManageSyllabusActivity.showReassignDialog` performs two independent `updateExamFullSettings` writes. A partial failure can leave duplicate references or an uncompleted transfer. Server atomic transfer semantics and authorization need investigation before changing data.
2. Syllabus picker/upload/replacement owner and permission boundaries are addressed in the carrying batch below. Source/unit coverage is planned for exact-SHA FAST; real document-provider and rotation acceptance remains OPEN. Existing PDF size/signature handling and stepper are preserved.
3. Poll management has prior independently successful Android/Worker fixes; investigate current caller authority and respect existing per-feature commit budget before modifying.



## Live verified implementation pointer — 2026-11-03

Latest verified implementation main: **c57dfa64693047e3e0b76530d4e930e4cd0baba1**.
Official Telegram rechecked after implementation: **3de37c8cfb039452be7398909461da6288babf15**.
Reconcile this pointer with the carrying implementation commit and live main/CI on restart.

| Scoped delivery | Commit | Exact-SHA FAST |
|---|---|---|
| A3.2 source camera glass | 7bb581b1b95d7e4e79489db77dfa50c3f9b92b6a | [41392841830](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41392841830) SUCCESS |
| A3.4/A3.6 scanner lifetime/results | 2e5655911e0755a0b3a1892a15d4bafd683631dd | [41394531948](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41394531948) SUCCESS |
| C1-C3/C5 confirmed rights/entries | 40a7bb30fcc2bbb3468d482a0ec6a9e4633e811b | [41395975426](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41395975426) SUCCESS |
| A1 ready-frame item appearance | 66574962d7dc1eb484ec81fb8440112d0279fc5d | [41397993734](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41397993734) SUCCESS |
| A1 card/caption and A4 export isolation | 3b2245bc0743ebd45d89e708eb6a899de0cb7e0a | [41402360574](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41402360574) SUCCESS |
| A3 scanner controls/title | e0963d7dcdfec04bcbe90f56dea26246b549e93b | [41403654857](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41403654857) SUCCESS |
| Profile avatar resource lifecycle | 66f3eea0ad5ffae629e2e2cf5cd8d5f78f508861 | [41405867198](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41405867198) SUCCESS |
| Poll Android response confirmation | 14c9e61be42fd7b969d5788b9ddf37d406df9ce4 | [41406623011](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41406623011) SUCCESS |
| Poll Worker atomic status/audit | a8433b6484b7141f881aa5fb271808703c042302 | [41407181451](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41407181451) SUCCESS |
| D4 live filter callbacks | 936e93aae0273daea08ea0e14fd6f9538d60f7b7 | [41409105317](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41409105317) SUCCESS |
| Seven Home menu actions, first batch | a6c0e042f58135d234b450e50c3c5f96ce38757c | [41411244614](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41411244614) SUCCESS |
| Seven Home menu labels/source assets, second batch | bc139bd3b2cc236d20d399e64e41531bf812d269 | [41413589407](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41413589407) SUCCESS |
| Confirmed Home exam-list contract | e1885471f913d7499116f9e4fe6fa1a6ae31d5bf | [41457197984](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41457197984) SUCCESS |
| Shared dismiss policy/question form retention | 412edc6ed80ca867635cbcf8cee06e56196728f4 | [41462667732](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41462667732) SUCCESS |
| C5 selected question-list request ownership | 03c07dc45031b9c2361db53e7ba650dca729bdce | [41466628937](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41466628937) SUCCESS |
| Confirmed exam rename contract/form | 196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee | [41469072072](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41469072072) SUCCESS |
| Profile editor draft account restoration | 2212767c4de9e0d23edd4439ff5bba0ab85215a3 | [41472089611](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41472089611) attempt 2 SUCCESS |
| Username source role/localized accessible status | 372b108a0b1b1c957566c53eeb7fd69b139e7a3e | [41475078791](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41475078791) SUCCESS |
| Shared question-response integrity | 381a76a610a3f1847f59a4c48939d7f2e4e6aea8 | [41479914039](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41479914039) SUCCESS |
| Category live-palette repair (owner exception) | d2b649f5bac8f4b86549ff854385bafbf3d97c6a | [41481845128](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41481845128) SUCCESS |
| Server question mutation/audit confirmation | 5bf15f8444dd65d5e947b43a538404db701a500f | [41523395700](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41523395700) SUCCESS |
| EditExam nested rights/canonical Add/account-bound gate | e83b2d4c918340d18da5bddb934bfddd2abdb80e | [41527865210](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41527865210) SUCCESS |
| Question review target/rights/response confirmation | 9cf0523ed859b68e70c03911b09d08dce97a2634 | [41532575336](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41532575336) SUCCESS |
| Analytics/report filter read ownership and truthful states | 980a92cf4a10e4b16b0a691303b4c340ad30bf6e | [41535796716](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41535796716) SUCCESS |
| Verified metadata saves, owner scope and staged cutoff backing | 07b448e5e70b53569db6a4ba328bd0baf34edca8 | [41544036446](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41544036446) SUCCESS |

| Authorized import-result lifecycle | 22fd6f8129e26bef75c7a26cc609a6525c1684e2 | [41568018939](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41568018939) SUCCESS |

| Bounded import document/cell identity | d638d3b20a4c10e6462b6d95adb8f35fada3a584 | [41571297755](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41571297755) SUCCESS |

| Standalone question deletion confirmation | 9fdeca47309ea9da0f228119591c410a70b4e057 | [41573024314](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41573024314) SUCCESS |

| Account-bound attempt/result handoff | c57dfa64693047e3e0b76530d4e930e4cd0baba1 | [41575420756](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41575420756) SUCCESS |

Each FAST passed Android build/JVM/AndroidTest source compilation, all four ABI
builds, pinned Rust security/C API, backend and Firebase. Historical scanner-batch docs 41403654877
and verbatim-import 41403654789 SUCCESS. Native navigation and publication skipped;
no protected verified APK artifact/checksum. Verified scanner batch local: 757 JVM tests, 243 Rust tests,
288 backend tests, 19 Functions tests and 59 CI helper tests pass; lint 0 errors /
1646 warnings. Source inventory 129 entries + 4 verbatim popup assets match pinned
Git/EVE bytes. Local x86_64 APK package com.eve.app and all four ELF libraries'
LOAD alignment 0x4000 verified, not a final published APK or screen acceptance.

Whole requirement ledger remains TELEGRAM_REMAINING_REQUIREMENTS.md. These scoped
scoped engineering increments are MET at source/host/FAST integration level, all
ADAPTED. Full A1/A2/A3/A4/Profile/Admin/Home/student/exam/theme fidelity remains
NOT MET. Physical camera/GPU/reference/device matrix and human GPL review remain
BLOCKED/unavailable; missing gift/service data is not fabricated. Strict personal-qr
acceptance still rejects acceptance. Mission-final protected FULL is not eligible.
Current owner correction: STOP QR-only iteration. QR active-path audit is
QR_ACTIVE_PATH_RECONCILIATION.md; only proven critical QR functional defects justify
new QR changes. First unfinished implementation is the latest continuation entry below; completed
Profile lifecycle/Home filter/menu/poll work stays preserved. Continue concrete
Admin/Home/Profile/Exam/shared requirements outside QR.
Device/reference absence does not authorize speculative QR refinement. No production deployment/migration, signing,
release variable, payment, scoring or backend authority changed. P2 historical bare
SDK35 shutdown exception remains UNKNOWN despite green JUnit. No new development
owner decision is required; production/license/distribution decisions stay separate.


## Recovery (2026-11-01)

Fetched live `main`: `1488ae8a9b3505cfd9e11befa85e3dd3cc5d0a39`; clean checkout.
FAST [41330630656](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41330630656)
succeeded at that SHA (build, backend, Firebase; native suites/publication skipped).
Official HTTPS Git fetch and `ls-remote` identify Telegram HEAD
`3de37c8cfb039452be7398909461da6288babf15`, root tree
`cc3c6ccf48e4167fdec764dfd04e765043f55093`. Existing compatible pin retained.
Its tlottie gitlink is `31f1b542f88e7b4be9a01e749920d857535fc715`.
No branches, resets, production calls, deployment or migration are authorized here.

## Requirement ledger

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R1 | Recover current state and preserve successful work | main/checkpoints/history | pinned Git objects | GitHub access | MET | fetched main, inspected latest exact-SHA FAST and checkpoint reports |
| R2 | Complete QR scene rendering | TelegramQrSceneView/QrCodeLayer | QrShareView/QrRender | scene/blur/motion | NOT MET | existing source maps; complete scene and matched runtime absent |
| R3 | Animated emoji selection, restoration, lifecycle | QrEmojiPattern/UsernameCardActivity | QrShareView/QrEmojiPanel | EVE catalogue, native drawable | NOT MET | existing tests; picker/rendering differences remain |
| R4 | Gifts against actual authorized data | no demonstrated EVE gift service | QrShareView premium/gifts | real entitlements | BLOCKED | do not fabricate inventory or payments |
| R5 | Scanner complete motion/lifecycle/permissions | EveQrLiveScannerActivity | QrScanOverlay | CameraX/ZXing | NOT MET | geometry adapter exists; morph and full device matrix absent |
| R6 | QR export correctness | QrGalleryWriter/UsernameCardActivity | QrShareActivity | MediaStore/FileProvider | UNKNOWN | historical PNG/publication/rollback tests; current runtime not rerun |
| R7 | Profile complete source subsystem | ProfileActivity/actions/gooey/popup adapters | ProfileActivity/ProfileActionsView/ProfileGooeyView | original parent/renderer | NOT MET | fidelity contract OPEN; preserve no Bio row |
| R8 | All Admin functions with source navigation/rows | admin activities + Telegram pager | PagerSlidingTabStrip/FilterTabsView/cells | Worker authorization | NOT MET | historical bound-page test; full action/device matrix absent |
| R9 | Home/navigation complete | MainActivity/home adapters | ActionBar/menus/drawer/tabs | discovery/filter contracts | NOT MET | existing atoms; whole flow source/runtime acceptance absent |
| R10 | Remaining student screens complete | settings/history/etc | shared Telegram components | existing business actions | UNKNOWN | whole-screen audit/runtime acceptance remains |
| R11 | Mock Test/Result preserve correctness and integrate Quiz UI | TestActivity/QuestionAdapter/ResultActivity | quiz/radio components | authoritative server scoring | UNKNOWN | no scoring changes authorized; full UI evidence remains |
| R12 | Theme/motion/RTL/accessibility/device matrix | shared themes/motion | source tokens/physics | real devices 60/90/120Hz | NOT MET | Home drawer Night #000000 exception retained; matrix incomplete |
| R13 | Native engine verified, existing failures resolved | vendored tlottie/JNI/CMake and original backdrop C++ | official tlottie gitlink/image.cpp | Rust/NDK/C++ | NOT MET | Rust fixture repaired and 243 tests green; native host raster passes; all-ABI/device/final APK acceptance remains |
| R14 | Preserve security and production-data invariants | existing auth/Worker/D1/Firebase unchanged | EVE contract | no deployments/data changes | MET | scoped diff review: no backend/auth/scoring/data/permissions/link-validation changes; bounded native input adapter; no production test requests introduced |
| R15 | Provenance and distribution safeguards | manifests/notices/release gate | pinned source/license | per-file GPL review | NOT MET | existing byte manifests; combined release obligations OPEN |
| R16 | Exact-SHA FAST for pushed implementation batches | protected build.yml | n/a | GitHub Actions | MET | dc3b7ee7, 24e79891, cdee4932 and A1 544c371f / 41369816744 green; carrying follow-up requires live exact-head reconciliation below |
| R17 | Mission-final six-job FULL and verified APK | protected build.yml | n/a | all feasible requirements | NOT MET | must not label an intermediate run mission-final |
| R18 | Main-only commits, checkpoint and honest reporting | this record | n/a | successful feature gates | MET | two coherent implementation batches; no branches/PR/reset/force push; mission explicitly PARTIAL |

## Native verification batch

Native test fixture repaired; production limits and every renderer assertion retained.
Source map: `docs/NATIVE_RENDERER_TEST_REPAIR.md`. Changed files: build.yml,
THIRD_PARTY_NOTICES.md, vendored src/tests/dos.rs, qr-native-upstream-manifest.json,
this checkpoint and the repair report. Optimized CPU and C API suites each pass
243 tests, zero failures/ignored; 59 CI helper tests pass. Docs/claim/diff checks pass.
Unchanged-source release and debug serial runs each reproduced 241 pass / 1 parser
memory failure; the historical timeouts did not reproduce locally.
Implemented/pushed native verification batch:
`dc3b7ee7329b2517cbb4ac0d63e65e75f5a3c50b`.
FAST [41358042268](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41358042268)
passed build/backend/Firebase, including 243 native tests; native UI and publication
were skipped as required by FAST. Current fetched main before the QR picker batch
is that SHA. The implementation commit containing this checkpoint identifies the
next code batch; always fetch live main before continuing.
The source panel uses 46dp cells (~7 columns), not the current four-column
Unicode/Airbnb grid. Do not present Unicode presets as animated documents.
Remaining QR source scene work follows this dependency repair. All other workstreams
remain open; historical reports are not substituted for fresh acceptance.

Native C API verification command and result: `cargo +1.99.1 test --locked --release --lib --features c-api --manifest-path app/src/main/cpp/vendor/tlottie/Cargo.toml -- --test-threads=1`: 243 passed, 0 failed/ignored, 2.14s.


## QR picker implementation batch

Source map: `docs/QR_EMOJI_PICKER_SOURCE_MAP.md`. New original-Java adaptations:
QrEmojiCell, EmojiPickOverlay and QrEmojiGrid, plus source shadeFor in QrPalettes.
EVE Kotlin integration: QrEmojiPickerGrid, UsernameCardActivity and TelegramQrSceneView.
The four-column Unicode/Airbnb picker is replaced by actual animated EVE catalogue
items through source Java press/selection badges/removal island/scrollbar/masks.
Six-key toggle/drop-oldest remains. Lifecycle cancellation, late asset disposal,
picker-open/scroll/choice restoration and reduced-motion/window visibility are wired.
The Material host and complete source sheet/blur/services remain ADAPTED/open.

Changed tests: QrEmojiPickerGridTest (four real view/lifecycle/restoration cases),
QrEmojiPickerNativeTest (new, compiled), TelegramQrSceneNativeTest (choice/source
restoration), and AdminDashboardRealDataVerificationTest (real Retrofit offline
contract fixture replaces its legacy production GET). No original schema assertion
is removed. Backend, authentication, permissions, Firebase rules, Worker/D1,
scoring, answers, rank, Home Night drawer and Profile Bio removal are unchanged.
Manifests, notices, fidelity contract, source map and this checkpoint accompany code.

Local commands/results (Gradle 8.7 / JDK 21 / Platform 36):

- `:app:testDebugUnitTest`: **677 tests, 151 suites, 0 failed/error/skipped**.
- `:app:compileDebugAndroidTestKotlin`: passed, including new native picker test.
- `:app:lintDebug`: **0 errors**, 1643 warnings (not all triaged); no suppression/baseline changes.
- `:app:assembleDebug -Peve.nativeAbis=x86_64`: passed; local development APK,
  not a mission-final artifact. Actual JNI library packaged; all three LOAD segments
  have `0x4000` (16KiB) alignment. Other ABIs require remote FAST build verification.
- Existing native-graphics QR raster test: **160 real decodes** pass.
- Python CI helper suite: **59 passed**. Docs, scoped acceptance-claim and diff
  checks pass. Claim targets remain OPEN.
- **85** native/QR source manifest entries match pinned official Git source digests
  and actual EVE digests. Prior Bezier facade drift is reconciled, with canonical
  original Java separately recorded EXACT. No source revision is silently changed.

Local environment failures were repaired outside the repository: missing jlink
(JRE -> full JDK) and test-JVM proxy/CA configuration. No TLS verification disabled.
The scroll test's first failure (expected 6, actual 0) came from changing window/span
sizes in the fixture. Fixed-size child viewport retains the exact equality assertion.
A plain Robolectric SDK-35 window reports GONE; the playback unit fixture controls
that window input, with actual platform visibility/playback asserted in AndroidTest.
An earlier changing-source lint snapshot failed SuspiciousIndentation; a stable-source
rerun passed. The full JVM console emitted a bare `Exception in thread "SDK 35 Main
Thread"` fragment at worker shutdown with no attached stack; JUnit XML has no failure
or error. Cause is UNKNOWN, not asserted fixed or used as native-device evidence.

## First unfinished requirement and exact next action

R2/A1 full QR scene remains NOT MET. R3/A2 is improved but still NOT MET for full
source sheet, shared AnimatedEmojiDrawable cache/service equivalence and matched
runtime acceptance. Gift entitlements remain BLOCKED by absent real EVE service/data.
No limitation is represented as owner-accepted. Profile/Admin/Home/student/Quiz,
full theme/accessibility/device matrix and final native acceptance remain open in
the requirement ledger. GPL release clearance remains unapproved.

The picker batch is pushed and exact-SHA FAST is green (evidence below).
Next implementation action: trace QrShareView's full scene draw/composition/recolor/open-close/cancellation
entry points into the existing Java QrCodeLayer/QrRender/QrGlass and Canvas host,
keeping Telegram account/network/premium services excluded. Complete that minimum
source subsystem and matched native runtime evidence before changing its OPEN claim.

Mission-final FULL: NOT RUN, because implementation/acceptance requirements remain
open. Verified GitHub APK artifact and final checksum: NOT AVAILABLE. Do not dispatch
a premature run and describe it as mission-final; do not bypass publication gates.
Local `/dev/kvm` is absent; no local emulator/device or 60/90/120Hz/reference match
is claimed. No production deployment, migration, signing or GPL gate override occurred.


## Latest observed main implementation and exact-SHA CI

Current fetched **implementation main SHA**:
`24e79891acb3cdf875d24dcff295b372475e01b2`.
This evidence-only checkpoint commit follows that implementation SHA; on resume,
fetch `origin/main` and inspect the checkpoint commit's own FAST before new edits.

| Commit | Scoped delivery | Exact-SHA evidence |
|---|---|---|
| `dc3b7ee7329b2517cbb4ac0d63e65e75f5a3c50b` | native security fixture repair + protected build-job step | [FAST 41358042268](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41358042268) SUCCESS |
| `24e79891acb3cdf875d24dcff295b372475e01b2` | original Java QR picker atoms / EVE lifecycle + restoration / offline config contract test | [FAST 41360696652](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696652) SUCCESS; [source import 41360696800](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696800) SUCCESS; [docs 41360696660](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696660) SUCCESS |

Both FAST runs passed Android build/JVM/AndroidTest compilation, native Rust tests,
backend and Firebase. Native navigation and publication were intentionally skipped;
neither run is FULL or screen acceptance. Remote native suite: 243 passed, 0
failed/ignored (2.67s at 24e79891). Local broad JVM: 677 passed, 0 failed/error/skipped.
All three local x86_64 libraries (QR engine and both CameraX JNI libraries) have
`0x4000` LOAD alignment. All four ABIs compile in remote FAST; final all-ABI APK
inspection/publication still requires the protected FULL artifact.

The bare SDK-35 shutdown fragment is present in **both** remote build logs,
including dc3b7ee7 before the new picker code. It predates that picker integration;
its cause is still UNKNOWN, not silently suppressed. CI's actual JVM task is green.
GitHub's approval-variable read returned HTTP 403; the current value of
`EVE_GPL_RELEASE_APPROVED` is UNKNOWN. No variable was written. The release workflow
file and its fail-closed condition are unchanged; documented license review remains
OPEN. No production-ready or legally cleared distribution claim is made.

## File / commit mapping

All paths below are repository-relative. Original source files retain their
upstream/adapter attribution; full line/hash maps are in the linked source reports.

| Commit | Changed files |
|---|---|
| dc3b7ee7 | `.github/workflows/build.yml`; `app/src/main/cpp/vendor/tlottie/src/tests/dos.rs`; `docs/NATIVE_RENDERER_TEST_REPAIR.md`; `docs/TELEGRAM_FINAL_MISSION_CHECKPOINT.md`; `docs/qr-native-upstream-manifest.json`; `THIRD_PARTY_NOTICES.md` |
| 24e79891 | `app/src/main/java/com/eve/app/telegram/qr/QrEmojiCell.java`; `app/src/main/java/com/eve/app/telegram/qr/EmojiPickOverlay.java`; `app/src/main/java/com/eve/app/telegram/qr/QrEmojiGrid.java`; `app/src/main/java/com/eve/app/telegram/qr/QrPalettes.java` |
| 24e79891 | `app/src/main/java/com/eve/app/ui/profile/QrEmojiPickerGrid.kt`; `app/src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt`; `app/src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt` |
| 24e79891 | `app/src/test/java/com/eve/app/ui/profile/QrEmojiPickerGridTest.kt`; `app/src/test/java/com/eve/app/ui/admin/AdminDashboardRealDataVerificationTest.kt`; `app/src/androidTest/java/com/eve/app/ui/profile/QrEmojiPickerNativeTest.kt`; `app/src/androidTest/java/com/eve/app/ui/profile/TelegramQrSceneNativeTest.kt` |
| 24e79891 | `THIRD_PARTY_NOTICES.md`; `docs/qr-native-upstream-manifest.json`; `docs/telegram-profile-fidelity.json`; `docs/QR_EMOJI_PICKER_SOURCE_MAP.md`; `docs/QR_NATIVE_ADMIN_CHECKPOINT.md`; `docs/TELEGRAM_FINAL_MISSION_CHECKPOINT.md` |

## Resume verification commands

The managed workspace toolchain is outside Git under `/workspace/toolchains`.
Use JDK `/workspace/toolchains/jdk-21.0.13+9`, SDK
`/workspace/toolchains/android-sdk`, Gradle `/workspace/toolchains/gradle-8.7/bin/gradle`.
Apply the environment runtime skill's proxy/CA configuration to Gradle **and** test
JVMs; do not disable TLS validation. The repository continues to build with Gradle.

```
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64
cargo +1.99.1 test --locked --release --lib --features c-api --manifest-path app/src/main/cpp/vendor/tlottie/Cargo.toml -- --test-threads=1
python3 -B -m unittest discover -s .github/scripts/tests -v
node .github/scripts/verify-docs.mjs
python3 .github/scripts/verify_telegram_acceptance.py
git diff --check
```

Do not rerun completed implementation or treat a previous report as the live head.
First unfinished **product requirement** remains R2/A1 full source QR scene;
remaining source sheet/shared-cache/runtime A2 work and scanner morph A4 follow.
Other workstream requirements remain as listed above. Final status: **PARTIAL**.

## Resume 2026-11-02 — active A1 scene batch

Fetched live main `cdee49324290c27995833d178e7eaa0e97de2eb9`, clean checkout;
FAST 41361717397 and docs 41361717394 are successful. All earlier successful
native/picker work remains. Official Telegram fetched master still resolves to
`3de37c8cfb039452be7398909461da6288babf15`; no upstream pin changes.

First unfinished product requirement remains R2/A1. Active implementation scope:
original native backdrop raster + Java scene recolor wave/QR ink, scene glass
composition/cleanup, source card shadow and whole-layer reveal integration.
Required source dependency discovered: `TMessagesProj/jni/image.cpp:1211-1377`
`generateQrBackdrop`, not an Android gradient approximation. Its only imported
native dependencies are JNI, AndroidBitmap and math; unrelated image/network/
account code is excluded. Java source is kept Java, C++ kept C++.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.1 | Original background raster and recolor wave subsystem | C++ telegram_qr_backdrop + Java QrBackdropScene | QrShareView:1534-1575,2260-2426; image.cpp:1211-1366 | JNI, AndroidBitmap, QrPalettes | MET | same production native raster runs in host tests; original helpers/pixel body compared unchanged; interrupted target and independent snapshot tested; whole-screen runtime acceptance separate |
| R2.2 | Wave-following QR ink and colored card shadow algorithms | Java QrBackdropScene/QrCardShadow | QrShareView:3573-3588,3690-3806 | existing QrCodeLayer, Java mask | MET | LAB wave ink settlement, colored soft shadow pixels, disposal/recreation and 162 real QR/PNG decodes |
| R2.3 | Glass scene sampling and supported native blur acceptance | Java scene nodes and QrControlBackground | QrShareView:2140-2175,4533-4583 | QrRender, source backdrop, screen origin | NOT MET | CPU stand-in exact screen-offset pixels and cleanup pass; API31 GPU blur implemented/compiled, matching device evidence absent |
| R2.4 | Whole-layer reveal and snapshot platform acceptance | QrSceneReveal + Activity root/translucent theme + independent source crop | QrShareView:2095-2121,3591-3686 | Activity host, EVE branding | NOT MET | source equations/root/control endpoints, interrupted/duplicate close, stale callback cancellation, reduced motion and rotation-before-draw PNG pass; actual Android window/navigation matrix pending |
| R2.5 | Card recolor bounce and emoji pulse algorithms | original Java QrCardMotion | QrShareView:1043,1534-1542,1578-1596,3668-3687 | monotonic frame clock and motion policy | MET | original 320ms peak/undershoot, source continuation, 360ms dip and reduced-motion settlement tested; full-device physics not inferred |

Original root GPLv2/per-file review and production release gate remain applicable.
No release clearance, account service, fake gifts or backend changes are implied.


## A1 scene implementation verification (2026-11-02)

Pre-batch fetched main: `cdee49324290c27995833d178e7eaa0e97de2eb9`.
Implementation commit: `544c371f0ce9419ee1b56892cda8e44d2d37759e`.
This checkpoint is updated by its single focused native-test-input follow-up;
obtain the carrying commit with `git log -- docs/QR_SCENE_NATIVE_SOURCE_MAP.md`.
All source adaptations and line maps are in that report and the 95-entry native/QR
provenance manifest. Original C++ helpers and pixel/release body compare byte-equal
to the pinned image.cpp sections; only includes/JNI binding/null-allocation cleanup
are adapted. Java rendering/motion stays Java. Existing native/picker work is preserved.

Final stable local command:

```
gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64
```

Result: **688 tests, 153 suites, zero failures/errors/skips**; AndroidTest source
compilation, resource/Java/Kotlin/C++ compilation, lint and x86_64 debug assembly
PASS. Lint: **0 errors, 1643 warnings**, no suppression or baseline change.
Eleven added JVM tests cover actual original native gradient/wave/ring pixels,
invalid-boundary rejection, interruption/snapshot state, cross-view glass sampling,
source shadow/bounce/pulse, reveal/cancellation/no-motion and real PNG after rotation.
Existing 160 palette-size QR decodes and two rotation PNG decodes pass.
Actual packaged-JNI test and scene export assertions are compiled in AndroidTest;
not executed locally. The workspace still has no usable local device/KVM matrix.

59 Python CI-helper tests PASS. Docs/acceptance-claim checks, pinned official
verbatim-import comparison and 95 original/EVE SHA-256 provenance checks PASS.
An initial lint pass read a changing source snapshot and flagged one indentation
error; explicit statement braces plus the subsequent stable-source full reruns
pass. No gate/assertion was weakened. The pre-existing bare SDK-35 shutdown
fragment without stack remains visible with green JUnit; cause still UNKNOWN.

The local x86_64 APK contains `libeve_qr_backdrop.so`, `libeve_qr_animation.so`,
`libimage_processing_util_jni.so` and `libsurface_util_jni.so`. All LOAD segments
have `0x4000` alignment; no host test library is packaged. Local-only SHA-256:
`19a3102e291951505d034e3f61edbb58f0c754cfd01446213af0471126a8c46e`.
This is not an Actions artifact, all-ABI final verification, release clearance or
mission-final APK. Published artifact and mission-final FULL remain **NOT MET**.

Changed-file groups in this implementation commit:

- Native production: CMakeLists.txt and telegram_qr_backdrop.cpp; Gradle host-test
  compilation, JNI headers/shim/bridge/shadow and Robolectric config are test-only.
- Java production: QrBackdropNative, QrBackdropScene, QrCardMotion, QrCardShadow,
  QrSceneReveal, QrRender disposal hook and QrControlBackground scene sampling.
- EVE integration: TelegramQrSceneView, canonical-delegating TelegramQrMotion,
  UsernameCardActivity, QR-only manifest/theme; existing auth/routing/exports remain.
- Tests: QrBackdropSceneTest, QrSceneRevealTest, TelegramQrMotionTest,
  TelegramQrRasterTest, QrBackdropNativeTest and TelegramQrSceneNativeTest.
- Documentation: THIRD_PARTY_NOTICES, QR_SCENE_NATIVE_SOURCE_MAP,
  qr-native-upstream-manifest, telegram-profile-fidelity and this checkpoint.

Security review: no Worker/auth/API/scoring/D1/Firebase/production-data changes,
new permissions, external-link routing, secrets or network calls. Native arrays,
finite values and small mutable RGBA buffers are validated; pinned allocations
are released on failure. New asynchronous shadow work has generation ownership;
late results recycle their bitmap. Window/input ownership is scoped to QR; the
whole root consumes touches while revealing. GPL/signing/distribution gates stay
unchanged and unresolved combined release review remains OPEN.

**First unfinished requirement:** R2/A1 full original pattern/scene behavior and
matching Android GPU/window verification. **Exact next implementation action:**
open the pinned QrShareView Pattern/place/wave-push/shared-cache and input callers,
map their minimum dependency graph to EVE's existing native Drawable catalogue,
then replace the remaining Kotlin pattern/gesture approximation with original
Java where compatible. Preserve username payloads, six-pick EVE assets and all
security contracts. Complete source sheet/morph A2 and scanner A4 follow; all other
NOT MET/UNKNOWN rows still apply. Before advancing, require exact-SHA FAST for this
batch and repair any failure. FAST 41369816744 at the implementation SHA is green; FULL is not yet eligible.


## Exact-SHA A1 CI and forward recovery update (2026-11-02)

Live main fetched before this follow-up is
`544c371f0ce9419ee1b56892cda8e44d2d37759e`.
FAST [41369816744](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41369816744)
**SUCCESS** at that exact SHA: Android/JVM/AndroidTest compilation, backend and
Firebase Functions/rules all pass. All four `buildCMakeDebug` targets (arm64-v8a,
armeabi-v7a, x86, x86_64) execute successfully. Native Rust security/C API tests:
243 passed / 0 failed / 0 ignored (2.61s). New host C++ renderer compiles and the
JVM task passes. Source-verbatim 41369816777, docs 41369816731 and dependency
provenance 41369816701 also succeed at the same SHA. GitHub artifacts API returns
an empty list for this FAST run; it publishes no APK and is not FULL acceptance.

The first staged diff check detected one extra EOF blank line in the new C++
file; the compound shell did not stop before commit/push. This forward-only
follow-up removes it, refreshes its manifest SHA-256, and leaves all original
rendering math unchanged. No amend/reset/history rewrite occurred. It also declares
the generated host native library as a JVM Test input: changed renderer bytes must
invalidate Gradle's up-to-date test state. Host platform/JDK versions are compiler
task inputs. The dependency omission did not invalidate the first fresh CI tests;
this repair protects incremental and future native edits.

Final stable follow-up local run:
`gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64`:
**688 tests / 153 suites / zero failures, errors or skips**, lint **0 errors /
1643 warnings**, AndroidTest compile and debug assembly PASS. Native renderer
host task and JVM tests execute again after the dependency declaration. APK
contents/alignment and local-only SHA-256 above are unchanged. Final staged
whitespace and 95-entry provenance checks pass. All earlier limitations remain.

Commit mapping: 544c371f contains all 31 scene/native/host/resource/test/provenance
paths listed in the preceding groups; this single follow-up changes only
`app/build.gradle.kts`, `app/src/main/cpp/telegram_qr_backdrop.cpp` (EOF),
`docs/qr-native-upstream-manifest.json`, `docs/QR_SCENE_NATIVE_SOURCE_MAP.md` and
this checkpoint. This is at most two successful implementation commits for this
backdrop/card/reveal subsystem. No unrelated feature is included.

The follow-up's own SHA/run ID cannot be self-embedded before committing. On
resume fetch main and query the Build Eve APK run whose headSha exactly equals
the fetched commit; require its success before advancing. The final live-head
result must be reported separately once observed. Do not infer it from 544c371f's
success. **Current next action:** push this focused follow-up, observe its exact-SHA
FAST, diagnose any failure, then resume the first unfinished R2 pattern subsystem.
Read-only next-source recovery located `Pattern` at QrShareView:135-166,
place/wave-push at 2568-2662, node key and `NodeSprings` at 2663-2740, atlases via
ensureAtlases:1264 and source input handler:6637 onward. These are investigation
locations, not imported/accepted implementations. Do not import gift documents or
account services into the EVE native-asset adapter.

## 2026-11-02 recovery continuation — R2/A1 pattern physics increment

Live baseline `5ced5663d6d669650a19379ca92a6f4873d38ae7`; official upstream pinned
`3de37c8cfb039452be7398909461da6288babf15`. This change scopes the
first unfinished original pattern-placement/node-wave/gesture subsystem,
with its production Kotlin caller, Java host tests, provenance and notice.
Existing QR backdrop/scene/picker/native/security/CI work is retained.
No backend, scoring, accounts, release variables or migrations are changed.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.A1-field | Source Java field placement, springs and gestures | QrPatternField / TelegramQrSceneView | QrShareView:208-229,2565-2787,3369-3407,6637-6768 | Canvas, Drawable, Android MotionEvent | UNKNOWN before exact-SHA CI; screen acceptance OPEN | Four Java production-path JVM tests added; native comparisons pending |
| R2.A1-atlas | Original emoji pattern atlas and swap compositor | Existing EVE shared NativeQrDrawable cache | QrShareView:1264,2843,3364 | Telegram document loader must not be imported | NOT MET | Source atlas/item old-new transitions absent |
| R2.A1-acceptance | Matched-device motion and GPL clearance | Existing fidelity/release contracts | upstream Android source and licenses | camera/device; redistribution review | NOT MET | No verified human-reviewed frames or legal clearance |

Next unfinished piece remains A1 source old/new pattern swap/cache/shine,
then picker-sheet morph. FULL mission and personal-qr remain NOT MET.

## 2026-11-02 R2/A1 additional source slot compositor batch

Baseline prior to change: `eb21b088d8d806500a48170c10d77f468432a9b1` whose exact-SHA FAST
[41372927365](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41372927365)
was SUCCESS. This batch adds `QrPatternSwap.java`, host JVM regression
tests, EVE QR live pattern renderer and source documentation. No backend
or release/security configuration was changed. Commit SHA and CI must be
read from GitHub after push, never filled speculatively.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.A1-swap | Original Java per-cell old/new shrink/overshoot and expiry | QrPatternSwap + TelegramQrSceneView | QrShareView:1218,2837,3187,3369 | EVE native Drawable cache | UNKNOWN until CI; screen NOT MET | New four-case pure Java regression tests, native device still OPEN |
| R2.A1-atlas | Original per-pattern texture pooling and shimmer | EVE shared-frame drawable/scene | QrShareView:1264,3050+,3364 | upstream account-dependent animated documents | NOT MET | No original source atlas/lighting compositor |
| R2.A1-runtime | Matched Android runtime visuals and export | Existing EVE source-adapted QR screen | QrShareActivity/QrShareView | Device, GPU/blur, lifecycle | NOT MET | Native matched evidence absent |

First next unaccepted scope: source-compatible per-pattern atlas/scene
shimmer and picker-sheet morph, then scanner and remaining whole mission.

## 2026-11-02 R2/A1 background light-band Java increment

Baseline `50e26ee6e5198b647fe470e0b090420c9dc480f0`, official upstream `3de37c8cfb039452be7398909461da6288babf15`.
The official actual QR emoji field uses reusable Drawables; its ensureAtlases
only initializes gift loaders, not a missing active emoji GPU atlas. No invented
atlas/texture renderer was added.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R2.A1-light | Background shimmer auto sweep, curved/cylinder noise/raster | QrSceneShine, QrBackdropScene, TelegramQrSceneView | QrShareView:2488-2559,2805-2838,3111-3160 | RimTune, RenderNode scene, motion policy | UNKNOWN pending exact-SHA CI; full runtime NOT MET | Java Robolectric pixel/reduced-motion checks added |
| R2.A1-frames | Original animated-emoji draw/cache phase on real device | Existing NativeQrDrawable with previousEmojis | QrShareView:1264,3260-3410 | original Telegram ImageReceiver excluded | NOT MET | native frame/timing comparison not accepted |
| R2.A1-runtime | GPU glass, input, native 60/90/120Hz and GPL | QR activity and CI | QrShareView/QrShareActivity | device/legal source-review | NOT MET | no matched runtime evidence; license gate remains fail-closed |

Next unfinished scope: source per-symbol reflective shine/gesture sampling and
native matched-device validation, then picker sheet + scanner. Whole mission
FULL and published APK still unaccepted.

## A2 picker opening source transition increment — 2026-11-02

Previous live base `acb0fec72f4aaa635c869e3325e38f79dd47c49c`, original Telegram pin remains
`3de37c8cfb039452be7398909461da6288babf15`. First unfinished
picker opening transform implemented through Java `QrPickerOpenMotion`
and `QrEmojiPickerGrid`/Activity production integration. No duplicated
R2 pattern field, slot compositor or Java shine code. The unit tests cover
source 420ms cubic scaling and sheet fade constants; CI and device
validation must be recorded from the actual commit SHA.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R3.A2-open | 420ms source emoji-grid scale and fade | QrPickerOpenMotion, QrEmojiPickerGrid, UsernameCardActivity | QrShareActivity:542-569 | Activity sheet, RecyclerView | UNKNOWN until exact-SHA CI | New host JVM math tests, runtime still OPEN |
| R3.A2-sheet | Original shape/cascade/compositor/blur and complete picker | Current EVE BottomSheetDialog | QrShareActivity:532-758 | original view parent, pack and Telegram services excluded | NOT MET | Source full sheet not imported |
| R2.A1-source | Source patterns and atlas comments | Existing Java field/slot/shine, shared native frame | QrShareView:1264,3364 | EVE native Drawable cache | NOT MET full fidelity | Current upstream ensureAtlases does not actually implement emoji texture batching; do not invent it |

Remaining first required work: original source picker sheet shape,
row cascade and correct glass composition; scanner morph, native device
matrix and remaining mission subsystems follow. GPL release review OPEN.

## A2 follow-up — source island cut in scaled grid (2026-11-02)

Follow-up to `c18a07139dc48080b31e15098deea6f618824513`: preserve its production picker grow. Adapt
`QrShareActivity.java:663-676` in `QrEmojiGrid.java` so the selected
island mask stays aligned while the RecyclerView's 420ms source scale is
0.75→1. Java geometry regressions and Robolectric actual-picker
detach cancellation test added. No backend, access rule, native dependency,
payload, permission, release gate or product setting is changed.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Evidence |
|---|---|---|---|---|---|---|
| R3.A2-mask | Opening picker overlay cut aligned with scaled grid | QrEmojiGrid/mapIslandCut; QrEmojiPickerGrid | QrShareActivity:663-676 | location transforms, Canvas | UNKNOWN until exact-SHA FAST | Java cut tests + actual-picker lifecycle JVM test; device pending |
| R3.A2-morph | Complete original sheet parent shape and glass/cascade | EVE BottomSheetDialog | QrShareActivity:532-758 | Telegram parent/scene compositor | NOT MET | Native matched frame/motion comparison absent |

Next piece: parent morph/cascade/blur of A2, source-driven scanner button
morph, then other unfinished mission workstreams. No production GPL clearance.

## A2 sheet island-to-panel source geometry (2026-11-02)

Verified precommit live baseline `b33ad5a78d6d267838ea01ca172378e950f13729`; upstream source
`3de37c8cfb039452be7398909461da6288babf15`. Source Java `QrPickerSheetMorph`, Kotlin
`QrPickerSheetSurface`, real Activity picker caller, JVM motion tests,
picker source map and notices changed in this one feature batch.
Explicit open gaps: the surrounding Material modal/drag differs from
Telegram's all-in-scene compositor; controls, selection and owned
animated Drawables remain EVE-native; mobile GPU/device acceptance and
license review are not yet proven.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R3.A2.shape | Original 420ms cubic sheet shape 144x40 to 10dp inset/radius 28.17 | QrPickerSheetMorph + QrPickerSheetSurface + UsernameCardActivity | QrShareView:5317-5433 | EVE Material host + QR glass Drawable | UNKNOWN before exact-SHA FAST | new 3 JVM geometry/curve tests, device comparison OPEN |
| R3.A2.parent | Complete native sheet parent/drag/cascade/glass lift | existing BottomSheetDialog / QrEmojiPickerGrid | QrShareView:5317-5570, QrShareActivity:532-758 | entire source scene/compositor | NOT MET | native source parity/GPU frame matrix absent |

Next first unfinished requirement: finish original source sheet parent
and close/cascade/blur compositor and run matched physical Android UI
tests. After that scanner morph, remaining QR/Profile/Admin/Home/student,
quiz/theme/RTL/CI/release gates. Whole mission PARTIAL.

## A2 island morph follow-up test and input gating (2026-11-02)

Following exact-SHA FAST success for `0ccbaaa5d6cd865bff8a634f24bd912365045d58` on
`Build Eve APK #41377373314`, the same A2 island morph feature gets
one focused forward-only follow-up: `QrPickerSheetSurface` suppresses
hidden controls while source sheet fraction <0.5; attached native-canvas
Robolectric pixel/input regression is added alongside source-map evidence.
Source `QrShareView.java:6654-6729`, original pin
`3de37c8cfb039452be7398909461da6288babf15`. The FOLLOW-UP SHA's
own FAST must be checked separately. Full parent compositor/source
sheet-sheet parity, device matrix, licensing and mission FULL remain NOT MET.

## A2 close choreography (2026-11-02)

Precommit live HEAD `480cb2a7a714af8682669332e91f89759ffe4eb2`; official source
`3de37c8cfb039452be7398909461da6288babf15`.
Previously completed A2 opening/clip commits and FAST green preserved.
This scoped close choreography integrates original Java source timings,
real picker cell closing and dialog Back/outside/drag dismissal paths;
the source-map and third-party notices were updated.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Verification |
|---|---|---|---|---|---|---|
| R3.A2.close | 360ms close + 170ms content fade, interrupted open | QrPickerSheetSurface, QrPickerMorphDialog | QrShareView:5317-5352 | Material dialog window/cancellation | UNKNOWN before exact-SHA FAST | source math and Robolectric view test |
| R3.A2.cascade | 150ms top-first cell closing | QrPickerCloseCascade, QrEmojiPickerGrid | QrShareActivity:609-632 | QrEmojiCell and RecyclerView | UNKNOWN before exact-SHA FAST | pure Java regression tests |
| R3.A2.glass | Original in-scene sheet blur/lift and full morph | EVE Material modal | QrShareView:4740-4790,5317-5570 | original RenderNode GPU | NOT MET | native runtime and licensing OPEN |

Next: source sheet blur/lift compositor or source scanner morph
after exact-SHA FAST; all QR/Profile/Admin/Home/student/quiz/RTL and
mission-final FULL acceptance remain PARTIAL/NOT MET.

## A2 separate source sheet blur/lift (2026-11-02)

Baseline `3650a3e6c1eb794574f1b102f69ce5e3a198047c`, exact-SHA FAST 41379437122 SUCCESS for preceding
A2 source closing/choreography. Official upstream
`3de37c8cfb039452be7398909461da6288babf15`.
This coherent feature isolates `QrShareView:4730-4767,4793-4828`
source Java 1/8-resolution sheet blur/color lift, integrates it into
production `QrBackdropScene` and `UsernameCardActivity` panel,
tests matrix/quantization and updates source/rights maps.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Evidence |
|---|---|---|---|---|---|---|
| R3.A2.glass-node | Independent 1/8 blurred node, brightness/contrast lift | QrPickerSheetLift / QrBackdropScene / UsernameCardActivity | QrShareView:4730-4767,4793-4828 | RenderEffect API31, source QR node | UNKNOWN until exact-SHA FAST | Java matrix/motion tests, GPU device evidence OPEN |
| R3.A2.full-parent | Exact in-scene sheet/scroll, rim and gestures | EVE existing Material modal | QrShareView:5317-5570 | native original scene compositor | NOT MET | Matched original-device frames and cancellation matrix absent |

Next unresolved: full Telegram source sheet compositor/runtime and
scanner source morph. Mission-final FULL/license release remain OPEN.

## QR scanner source button-to-screen morph (2026-11-02)

Live precommit `caf429b0b50468876ce38d8ff21f1217ab61cf5f`; previous source picker glass repair's
exact-SHA FAST `41380609101` SUCCESS. Pinned original Telegram
`3de37c8cfb039452be7398909461da6288babf15`.
This coherent scoped scanner feature adds original Java geometry,
Kotlin Canvas/CameraX host, real scan-button Activity caller,
translucent QR theme, two source tests, contract tests,
provenance and notices. No backend, Firebase, auth, admin rules,
release variables, signing, scoring or production migration touched.

| ID | Requirement | Current code | Upstream source | Dependencies | Status | Verification evidence |
|---|---|---|---|---|---|---|
| R5.scan-button | Original source 390ms button-to-screen and 330ms reverse geometry | QrScanMorph, QrScanMorphLayout, UsernameCardActivity, EveQrLiveScannerActivity | QrScanOverlay:99-112,168-195,370-479,561-568,601-623 | EVE QR translucent theme / Activity / CameraX | UNKNOWN until exact-SHA FAST | production-path source contract and Java tests; physical motion OPEN |
| R5.camera-order | Start after open, bounded ephemeral close preview, release before close | EveQrLiveScannerActivity / QrScanMorphLayout | QrScanOverlay:386-403,481-546 | existing CameraX + own username QR parser | UNKNOWN device | EVE permission/gallery/duplicate-result existing checks retained |
| R5.runtime | Match original scan-found polygon, hardware glass and camera lifecycle matrix | EVE QrScannerFrameOverlay | QrScanOverlay:579-708 and full camera dependency | device GPU, real permission lifecycle | NOT MET | original found corners/visual matrix and full permission paths not verified |

Next unfinished: original found-code outline plus camera/gallery/torch
permissions, rotation, rapid cancellation and native runtime; then
remaining QR/Profile/Admin/Home/student/Quiz/theme and FULL/release gates.
Root GPLv2 and combined-app distribution decision remain OPEN.

## R5 scanner permission recovery continuation (2026-11-02)

Live starting main 8ff0a7f4bf05601a6d760b652079d6cfd3a47c94
with exact-SHA FAST 41384393894 SUCCESS. This feature adds
EVE-owned denied/request-blocked settings and gallery choice,
Settings return recovery, single CameraX binding and lifecycle-safe
dialog cleanup. No Telegram camera, accounts or network system imported.
No backend, Firebase, signing, license gate or scoring changes.

| ID | Requirement | Code | Source | Dependencies | Status | Verification |
|---|---|---|---|---|---|---|
| R5.permission | Denial rationale, manual Settings, gallery, granted return | EveQrLiveScannerActivity | EVE Android permission contract | Android ActivityResult + existing TelegramNativeAlertDialog | UNKNOWN until exact-SHA FAST/device | Scanner source contract updated |
| R5.binding | No double async CameraX binding, lifecycle cleanup | EveQrLiveScannerActivity | EVE CameraX contract | ProcessCameraProvider | UNKNOWN until device | One-in-flight guard, release in onDestroy |
| R5.found | Source scanner detected polygon / native GPU parity | QrScannerFrameOverlay | QrScanOverlay:700-760 | ZXing result geometry and transform | NOT MET | Actual device and source comparison pending |

Next: original yellow found-outline with camera image-to-view transforms;
native permission/rotation/torch/gallery tests and wider mission still open.
Root GPL/per-file combined distribution and final FULL remain unaccepted.

## R5 scanner torch async integration checkpoint (2026-11-02)

Baseline main 963c8a7322ee155cbe68c0291b5805074485a117,
exact-SHA FAST 41385170876 SUCCESS. Official Telegram pin
3de37c8cfb039452be7398909461da6288babf15.
New QrTorchGate and production EveQrLiveScannerActivity use
CameraX future completion before reflecting flashlight state,
reject rapid taps and discard stale callbacks. Native tests
remain required after the pushed SHA. No changes to backend,
auth, Firebase contracts, scoring, signing, release gates or
source licenses.

| ID | Requirement | Code | Upstream | Status | Evidence |
|---|---|---|---|---|---|
| R5.torch | Accurate async torch state and accessible button | EveQrLiveScannerActivity, QrTorchGate | QrScanOverlay:783-787,914-922 | UNKNOWN before new FAST | 3 gate JVM tests + scanner source contract |
| R5.device | Flash hardware, background/rotation | same | EVE CameraX | UNKNOWN | Real device matrix pending |
| R5.found | Four-point original found QR outline | QrScannerFrameOverlay | QrScanOverlay:700-782 | NOT MET | Camera image-to-preview geometry and valid points pending |

Next unfinished requirement: source-constrained four-point scan-found
feedback and camera/gallery/torch permission runtime validation,
then original QR/Profile/Admin/Home/student/Quiz/theme and FULL.
Production GPL/per-file combined distribution approval remains OPEN.


## 2026-11-02 continuation: R5 QR scanned-code found overlay

Baseline before implementation: main
813a2b927ff1de25e5c05795323ae4a5d72a78e5,
exact-SHA FAST 41386124717 SUCCESS (Android/JVM/Backend/Firebase).
Official Telegram source pin
3de37c8cfb039452be7398909461da6288babf15.
This continuation adds the original Java QrScanFound source equations,
a real QR frame Canvas caller, exact four-point detection policy,
CameraX shared-ViewPort analysis-to-preview mapping, source found delay,
focused tests and provenance. The old/repaired scanner motion and
permissions work are preserved. No Telegram accounts, MTProto, original
Telegram camera service or network/documents imported; no production
data, server auth, scoring, CI gate, GPL approval or signing changes.

| ID | Requirement | Current code | Upstream ref | Dependencies | Status | Evidence |
|---|---|---|---|---|---|
| R5.found-shape | Source yellow 6dp rounded 18% outline and 0.3 approach | QrScanFound.java / QrScannerFrameOverlay.kt | QrScanOverlay:700-782 | Java Canvas/paint, four genuine ZXing points | UNKNOWN pending exact-SHA CI | New Java pixels/rotation and real overlay JVM tests |
| R5.found-map | Crop/rotation-correct camera-to-preview mapping and four-point-only 450ms delay | EveQrLiveScannerActivity.kt | QrScanOverlay:994-1011,1040-1063 | CameraX ImageProxy/PreviewView shared ViewPort | UNKNOWN pending device matrix | Source contract + buffer-rotation JVM tests; actual CameraX device evidence absent |
| R5.found-runtime | Physical preview orientation and found-timing fidelity | EVE CameraX | Original QrScanOverlay renderer | Emulator/device, navigation, real QR | NOT MET | No matched Android reference frames/60-90-120Hz measurement |

Next after exact-SHA FAST: real Android camera/gallery/torch/denial,
rotation and valid-point display verification; complete original
scanner live blur/source glass and further QR/Profile/Admin/Home/
student/quiz/theme requirements. Mission-final six-job FULL and
verified published APK still NOT MET. GPL review remains OPEN.


## Same mission checkpoint: R5 scan-found Java source integration (2026-11-02)

Current **verified** live main before this evidence-only change:
2f9727862b355f87b676dbc3d15be542074895f3.
Original upstream pin unchanged:
3de37c8cfb039452be7398909461da6288babf15.
The original Java QrScanFound.java, EVE Canvas overlay, real
CameraX shared-ViewPort ImageProxy/PreviewView transformation,
genuine four-point ZXing-only policy and source 450ms found
delay are implemented. Current production code in
EveQrLiveScannerActivity.kt / QrScannerFrameOverlay.kt.
No Telegram account/network/camera infrastructure, migrations,
production secrets, auth/scoring or CI/security bypasses added.

| ID | Requirement | Current code | Upstream reference | Dependencies | Status | Evidence |
|---|---|---|---|---|---|---|
| R5.found-source | Source yellow #FFFFDE07 6dp 18%-arm 0.3 approach and genuine four-point-only display | QrScanFound.java; QrScannerFrameOverlay.kt | QrScanOverlay.java:108-112,346-355,700-782 | Java Canvas + EVE view | MET scoped | FAST 41388793850 SUCCESS, native Robolectric pixel + four-point/rotation tests green |
| R5.found-transform | ImageProxy original pixel coordinates to displayed PreviewView, source conditional 450ms delay | EveQrLiveScannerActivity.kt | QrScanOverlay.java:994-1011,1040-1063 | CameraX 1.4.2, shared ViewPort | MET source-contract/compile, UNKNOWN physical runtime | FAST 41388793850 compiled/tests; hardware display matrix unverified |
| R5.found-device | Actual scanner 60/90/120Hz, crop/rotation, 3-button/gestures, gallery/torch/permissions | Existing Activity/overlay | full QrScanOverlay | Physical Android device/GPU | NOT MET | Native UI matrix skipped in FAST; no comparable source playback |
| Mission FULL/production | Protected six-job FULL, published checksum, license clearance | release gate | Telegram GPLv2/per-file | authorized review and final mission acceptance | NOT MET | No FULL/verified artifact; approval variable value UNKNOWN |

Source implementation commit ff4bc279 failed first FAST
41387565607 because of test-only ApplicationProvider import.
First repair 3d52de7 failed FAST 41387986583 because new
pixel tests used Robolectric legacy graphics (two red/yellow raster
assertions). Second repair 2f972786 uses established native
Skia graphics and **passes** exact-SHA FAST 41388793850.
All prior QR picker/wave/scanner/permissions/torch changes preserved.
Latest docs job 41388793852 SUCCESS. Native Rust tests 243
passed, no job gate weakened. Historical bare SDK35 thread
exception remains UNKNOWN and present in green JVM log.

First next requirement: real Android CameraX-to-preview coordinate
and gallery/camera/torch/permission runtime acceptance, plus
Telegram source scanner live blur/glass and content-fidelity gaps.
Profile/Admin/Home/Student/Quiz/theme/release/FULL remain open.
This checkpoint does not claim whole QR or mission completeness.

## 2026-11-02 continuation — scanner camera-layer source glass

Recovered clean main 58b415857bce257de4ca966a7174ff74b9ae910d; exact-SHA FAST
41389589665 and docs 41389589728 SUCCESS, no artifacts; native/publication skipped.
Latest official Telegram HTTPS Git HEAD remains
3de37c8cfb039452be7398909461da6288babf15, tree
cc3c6ccf48e4167fdec764dfd04e765043f55093. telegram.org/apps official link checked.
No duplicate implementation or concurrent remote main change found before edits.
The full new owner-scope ledger is TELEGRAM_REMAINING_REQUIREMENTS.md; previous
feature/commit/CI records above are retained. Whole mission remains PARTIAL.

A3.2 source gap implemented: original Java CameraView preview-child recording and
32dp DECAL blur, original QrScanOverlay glass fill/fallback/sheens/rim, screen-space
control sampling and RAM-only closing blurred shader. New QrCameraBackdrop.java /
QrScannerGlass.java integrate through QrScanMorphLayout and the actual CameraX
Activity. No live bitmap readback, new permission, backend/Worker/Firebase/scoring,
account service, production mutation or release gate change. Source map:
QR_SCANNER_CAMERA_GLASS_SOURCE_MAP.md. Everything is ADAPTED, not screen EXACT.
PowerManager substitutes for Telegram LiteMode; source controls/layout and actual
TextureView/permission/torch/rotation/reference acceptance remain NOT MET.

All 97 recorded native/QR manifest entries match authenticated official Git blobs
and EVE digests; 4 preexisting stale EVE hashes are reconciled with their actual
successful main file commits recorded in JSON. Four verbatim popup assets match.
This is the recorded inventory, not a claim that it inventories the entire app.
Superproject tags/tree fetched. Full recursive optional media/codec/account clones
were attempted but exceed the 8 GiB executor alongside Android tooling. Unused
partial clones discarded; required tlottie is checked out at exact official gitlink
31f1b542f88e7b4be9a01e749920d857535fc715. Other submodules are uninitialized and
not asserted verified. Source files/pins imported into EVE are unchanged.

Local verification so far:
- Optimized Rust C API command: 243 passed, zero failed/ignored (2.17s).
- Focused source camera-glass and production scanner contract: 4 passed.
- Broad JVM JUnit XML: 727 tests / 167 suites, zero failed/error/skipped.
- AndroidTest compilation passes, including synthetic HWUI/PixelCopy camera-glass
  fixture. It has NOT run on Android. Actual camera is not simulated as device proof.
- Backend npm run build and npm test: 288 passed, zero failed/skipped.
- Functions JS syntax checks and node --test functions/test/*.test.js: 19 passed.
- CI Python helper suite: 59 passed. Docs/claim/import/diff checks pass.
- Stable local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 passes (2m17s); lint 0 errors / 1645 warnings. This local debug APK is not a protected published artifact.

Local setup failures: overlapping Gradle attempts raced on generated resource JSON;
subsequent runs are serial. Installed Java runtime lacked JNI headers/jlink; full
JDK21 installed outside Git with normal proxy/CA trust. First new fallback pixel
test sampled low-alpha transparent-buffer unpremultiplication (red 26 vs 28); fixture
now uses the real scanner's opaque backing and retains visible color/clip assertions.
Backend has no npm lockfile; npm ci correctly rejected it, then repository-supported
npm install --no-package-lock + build/tests passed. No production test/assertion,
lint, TLS, native security cap or CI gate was weakened. The historical bare SDK35
thread-shutdown exception fragment remains present/UNKNOWN in otherwise passing JVM.

No local /dev/kvm; after installing platform tools adb devices is empty. Physical
camera/reference/60-90-120Hz/native navigation evidence is unavailable locally.
No premature mission-final FULL, published APK or legal clearance is claimed.
First overall unfinished requirement stays full A1 source/runtime acceptance;
next independent gaps include remaining source scanner control/lifecycle behavior
and an observed dashboard pending-rights visibility path (all default actions can
remain visible when its second rights fetch fails; Worker authorization is separate).
The carrying implementation commit requires its own exact-SHA FAST before advancing.

Local lint initially found five missing endFrame API guards and ten preexisting CameraX Java-marker opt-in errors. Explicit QrRender.hasEffects guard plus androidx.annotation.OptIn repair all 15 without suppressions, baselines or target changes. Stable-source rerun passed. Temporary upstream checkout was shallow-compacted after fetching tags to reclaim build space; current commit/tree and required tlottie objects remain verified; fetched tag IDs are retained in workspace evidence. No EVE/shared history was rewritten.

## Scanner lifecycle continuation — next coherent implementation batch

Current fetched/pushed main before this batch:
7bb581b1b95d7e4e79489db77dfa50c3f9b92b6a.
Exact-SHA FAST 41392841830 SUCCESS, docs 41392841837 SUCCESS,
source-import 41392841829 SUCCESS. All four native ABI build tasks and
243 Rust security/C API tests pass (2.59s). Artifact count zero; both native
navigation suites and publication skipped by FAST. Source glass is ADAPTED;
actual CameraX TextureView/reference/frame evidence remains open.

New EVE ownership implementation: QrCameraSessionGate.kt, actual
EveQrLiveScannerActivity pause/resume/bind/analyzer/permission/Gallery/close paths,
QrCameraSessionGateTest.kt and QrScannerPausedResultTest.kt. Original source
pause/destruction ordering inspected at QrShareActivity:1070-1077 and
QrScanOverlay:370-425,490-546, same official full SHA as above. Full technical
map and integration/test changes: QR_SCANNER_SOURCE_FRAME_CHECKPOINT.md.
Camera/torch stop on pause; old async binds, frames and delayed navigation cannot
survive a session change. Gallery delivery waits for resume and rejects superseded
requests. Saved in-flight permission/Gallery flags prevent duplicate launch during
recreation; denial/Settings return preserves manual recovery. Snapshots remain
before release and all secure-link/four-real-point contracts remain intact.

Ten focused tests (five ticket-state, two actual scanner Activity lifecycle and
three existing source/caller cases) pass; AndroidTest compilation and lint pass.
Stable local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 passes (38s): 734 tests / 169 suites, zero failures/errors/skips; lint zero errors / 1645 warnings. All 99 recorded source manifest entries match pinned Git objects/current EVE hashes. Actual device execution remains unavailable.
No new backend, Firebase, scoring, migration, production calls/permissions,
release variable, signing or data-model changes. Owner Bio/About removal verified
in current production Profile source/layout; backend bio data/API tests remain green.
Complete QR/Profile/Admin/Home/student/Quiz/theme/device and mission FULL still open.
Next independent engineering action after this batch's own green FAST: fail-closed
Admin pending-rights visibility and fresh per-screen right checks, preserving Worker
authorization and every legitimate source-tab action. Remaining original scanner
control geometry/physics and matched physical-device matrix are not accepted.

## Admin destination rights/confirmed dashboard continuation

Base fetched main 2e5655911e0755a0b3a1892a15d4bafd683631dd, exact-SHA
FAST 41394531948 SUCCESS and docs 41394531950 SUCCESS. Scanner lifecycle
734-test local/remote batch preserved. Both source scanner batches are now green;
FAST native navigation/publication skipped and artifact lists empty.

New scope C1/C2/C3/C5: shared AdminAccessGate now requires the fresh server
membership AND validated /admin/me metadata before revealing any protected root,
then rechecks current UID/email. Each privileged destination supplies its actual
Worker right; confirmed metadata is passed to dashboard setup so loading/failure
cannot expose all default actions. All 21 destination screens plus hub and extra
ManageUsers resume guard are integrated (23 calls). Existing source pager/tab and
row callbacks remain. Questions exam edits and monitor generation retry require
their compound rights. All four AdminViewModel callers traced; constructor no
longer issues unrelated directory/statistics requests. Missing server response
throws instead of synthesized permissions. Server authority/rights definitions,
Firebase/Worker/D1/scoring, data, production deployments, QR-free decision and GPL
release/signing gates remain unchanged. Source/action map: ADMIN_RIGHTS_CONTINUATION.md.

Local stable validation: :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 SUCCESS (53s).
740 tests / 170 suites, zero failures/errors/skips; lint 0 errors / 1645 warnings.
109 scoped admin/policy/identity cases pass, including 13x13 role pairing,
server owner, real malformed Gson metadata, pending/cancelled/failed permissions,
foreign identity, and offline actual repository missing-data handling.
Two existing source guards initially failed because the confirmed callback variable
and retry-right predicate changed; updated guards retain all behavioral assertions.
One overlapping local run polluted a reused log; the final serial broad log and
JUnit XML are clean. No assertion/lint/CI/auth gate weakened. Pinned source 99-file
inventory and unchanged native renderer evidence remain as above.

This batch needs its own exact-SHA FAST after push. Whole mission remains PARTIAL.
Source A1 follow-up discovered by actually opening QrShareView:3171-3180,3245-3304:
original emoji have no band-reflective lighting; that old pending atlas/shine claim
must not drive invented code. Actual ready-item seeded appearance/popIn is missing
from EVE drawEmojiSlot. Next concrete source action: integrate that original Java
appearance path, test late frames/rapid selections/export/reduced motion, then
source scanner control layout/press physics and remaining ledger items. Physical
camera/GPU/matched references, Profile/Admin/Home/student/Quiz app-wide acceptance,
protected mission-final six-job FULL/APK and human GPL review are still NOT MET/BLOCKED.

## A1 real-frame emoji appearance continuation

Base fetched main 40a7bb30fcc2bbb3468d482a0ec6a9e4633e811b; exact-SHA FAST
41395975426 SUCCESS (Android/all four ABI/JVM/AndroidTest compilation, native
243 tests, backend/Firebase); docs 41395975313 SUCCESS. All prior source QR,
scanner permission/torch/found/lifecycle and fresh Admin rights work remains.
Original source pin: 3de37c8cfb039452be7398909461da6288babf15.

Original Java QrEmojiAppearance now supplies ready-item timestamps, per-screen
seeded 300ms spread, 380ms pop and source backOut. Actual SceneView root reveal,
current/outgoing patterns, late native frames, picked-later sentinel, reduced motion,
export and cleanup are integrated. NativeQrDrawable checks real JNI frame return,
freezes the cached frame for export and stops a failed renderer instead of retrying
for every lattice node. Source map, dependencies, licensing and known adaptations:
QR_EMOJI_APPEARANCE_SOURCE_MAP.md. No invented reflective emoji light/GPU atlas,
Unicode preset, gifts, premium service or backend/auth/scoring change.

Stable local Gradle tasks (:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64) PASS:
745 tests / 171 suites, zero failures/errors/skips; lint 0 errors / 1645 warnings.
The new five source math/readiness/isolation/export tests pass alongside all old
actual QR raster decodes. The new real Android JNI readiness/frozen-frame/close
fixture compiles but is NOT executed locally. Its synthetic solid JSON was rendered
by the same optimized host C API: parse OK, render status 0, 16,384 visible pixels.
No host result is represented as physical Android/GPU evidence. Recorded source
inventory: 101 entries match authenticated Git blobs and current EVE SHA-256;
four verbatim popup imports also match. Python CI helpers 59 pass; docs/claim/diff
checks pass. Pinned Rust 243/backend 288/Functions 19 evidence remains as above.

The carrying implementation SHA needs exact-SHA FAST. Current overall status:
NOT MET for whole A1/A2/A3/A4/Profile/Admin/Home/student/exam/theme source fidelity
and original-versus-EVE/native matrix; BLOCKED locally for physical hardware,
matched reference captures and human GPL distribution approval. No premature final
FULL, production deployment/migration, release-variable change or published APK.
The concrete next implementation action is source whole-scene card/typography/
parent geometry and original scanner control layout/press physics, followed by all
remaining ledger requirements. Active source engine/permissions/security repairs
must be preserved. The historical bare SDK35 thread shutdown fragment remains
unresolved despite green JUnit; no hidden/suppressed failure or legal clearance claim.


## A1 card/caption and A4 export isolation continuation — 2026-11-02

Recovered/refetched main e4dd748651a4b7abecc52f5c9be204fd67a1a64f; its FAST
41399010870 and docs 41399010801 SUCCESS, no artifacts. Official Telegram HEAD
re-fetched, unchanged 3de37c8cfb039452be7398909461da6288babf15; optional submodule,
workspace/hardware and human release limitations remain as recorded above.

QrCardLayout.java and QrCardCaption.java extract original Java card/rest/sheet rise,
condensed glyph/StaticLayout and caption drawing. Exact source font and 12 glyph
bytes are renamed only; root/per-file/font legal review stays OPEN. EVE measured
chrome/compact fit, locale-independent username, native accessibility controls and
Material modal adapter are explicit ADAPTED integration. Actual picker progress
now lifts the scene card, dismissal clears progress/callback, and both root and
child layout changes refresh viewport bounds. Independent export geometry/ink
keeps the live card/gesture bounds intact. Existing native/QR/scanner/Admin paths,
backend/scoring/identity contracts and fail-closed release gate are preserved.
Source map: QR_CARD_CAPTION_SOURCE_MAP.md. Manifest now records 116 identities.

Serial local broad Gradle (:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64) PASS: 753 tests / 173
suites, zero failures/errors/skips. New geometry, actual font/glyph raster,
30/25/19 fitting, long32-character identity, Turkish locale, interrupted picker
export/live-state isolation and dismissal tests pass. Existing real PNG/ZXing
portrait/landscape/color scenes remain decodable. One old renderer source-text
guard needed the new local card parameter; its actual assertions are preserved.
Python CI helpers 59, docs, claim consistency, four verbatim imports and all116
pinned identities PASS. AndroidTest compiles; no physical test is inferred. The
historical bare SDK35 teardown fragment remains UNKNOWN, despite green JUnit.

The carrying implementation commit still requires exact-SHA FAST. Whole mission
NOT MET; no final FULL, published APK or legal acceptance. First unfinished source
implementation: QrScanOverlay original controls/title placement and press/torch
interpolation through EVE CameraX host. Then all remaining Profile/Admin/Home/
student/exam/theme and native-reference requirements in the full live ledger.


## A3 original scanner controls/title continuation — 2026-11-02

Carrying base main 3b2245bc0743ebd45d89e708eb6a899de0cb7e0a exact-SHA FAST
41402360574 SUCCESS; docs 41402360616 and import verification 41402360702 SUCCESS.
Build includes all four ABIs/pinned native tests/backend/Firebase, with native
navigation and publication skipped. Official source pin unchanged. Previous source
card/caption/export work remains intact; its full runtime acceptance stays OPEN.

QrScannerLayout.java and QrScannerControl.java extract original Java source geometry,
round icon controls, title placement, press convergence and confirmed-torch fill.
Original BackDrawable.java draw/animation bodies match after reversing mechanical
package/unit changes. Eight icons and existing Roboto medium match upstream bytes.
EVE native click/accessibility nodes and measured complete-group compact fit are
ADAPTED; actual Gallery contract, CameraX, permissions, futures, found coordinates,
allowlist and session cancellation remain. English/Hindi labels are resources.
See QR_SCANNER_CONTROLS_SOURCE_MAP.md; inventory now 129 upstream/EVE identities.

Serial local broad Gradle testDebugUnitTest/compileDebugAndroidTestKotlin/lintDebug/
assembleDebug (-Peve.nativeAbis=x86_64) PASS: 757 JVM tests / 174 suites, zero
failures/errors/skips, lint zero errors / 1646 warnings. Final focused four control
tests and compilation of the updated real-control HWUI fixture PASS. Actual native
HWUI fixture is not executed locally; no camera/device/reference proof inferred.
Python CI helpers 59, docs/claims/four verbatim imports/diff and all129 identities
PASS. No native engine or backend change. Historical bare SDK35 teardown fragment
still UNKNOWN despite green JUnit.

The carrying scanner implementation commit requires its own exact-SHA FAST.
Whole A1/A2/A3/A4/B/C/D/E/F remains NOT MET as in the ledger; human legal review,
physical/reference matrix and final protected FULL/APK remain unavailable/not eligible.
Next source action: finish scanner original cached corner renderer and scene parent/
picker gesture/compositor gaps, then remaining Profile/Admin/Home/student requirements.
Do not replay prior successful commits or treat unavailable hardware as a global stop.


## Owner-directed full-mission reconciliation / Profile implementation

Live main e0963d7dcdfec04bcbe90f56dea26246b549e93b refetched; exact-SHA FAST
41403654857 SUCCESS, docs 41403654877/imports 41403654789 SUCCESS; artifacts zero.
No concurrent commit. Green QR is preserved unchanged. The uncommitted QR frame/
corner-cache experiment is removed from production/test sources and deferred.
QR_ACTIVE_PATH_RECONCILIATION.md records active routes, every implemented feature
caller group, intentional adapters/test-only helpers, and critical/source/device/
external-legal gap classes. No new personal-QR critical defect reproduced; whole
acceptance OPEN. Earlier QR-only next-action paragraphs are superseded.

ProfileGooeyView existing Impl now releases CPU bitmap/Canvas and four GPU display
lists/effects on detach; same-size reattach restores size/intensity/blur. No new
renderer/state owner, draw equation, Add bio/About or backend change. Existing
RenderNode mask/blur/bridge code was already present; stale manifest wording is
corrected. Source/lifecycle map: PROFILE_REGRESSION_REPAIR.md latest section.

Local broad :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 PASS: 759 tests / 175 suites, zero
failures/errors/skips; focused actual bitmap/RenderNode lifecycle tests pass. Python
helpers 59/docs/claims/diff PASS. Hardware/reference/full-screen/legal results are
not inferred. The carrying Profile implementation needs exact-SHA FAST.

First concrete next action outside QR: repair C4 PollRepository failed/null list
responses becoming empty success and empty create ID being reported successful.
Existing ManagePollsActivity -> PollRepository -> EveApiService -> authenticated
Worker moderate_feedback routes must retain role/data contracts. Continue full
Profile/Admin/Home/Exam/shared ledger; all whole-screen requirements remain NOT MET.
No final FULL/release, legal variable change, deployment or production data write.


## Admin poll response continuation

Recovered main 66f3eea0ad5ffae629e2e2cf5cd8d5f78f508861; exact-SHA FAST
41405867198 SUCCESS and docs 41405867218 SUCCESS. Profile lifecycle repair is
verified at source/host/FAST level; hardware acceptance stays OPEN.

C4.poll-response: PollRepository now rejects failed/null list responses and
creation responses without a nonblank server ID. ManagePollsActivity propagates
cancellation rather than displaying a failure after its lifecycle ends. Existing
fresh moderate_feedback gate, Worker authentication/permissions, vote contracts,
source dialogs and deletion flow remain unchanged. No backend deployment/write.
Before repair, two focused tests reproduced the false-success defects. After
repair, protocol and actual Day/Night bound-row label/callback tests pass. Broad
local unit/AndroidTest compilation/lint/x86_64 assemble PASS; counts recorded in
TELEGRAM_REMAINING_REQUIREMENTS.md. This carrying commit requires exact-SHA FAST.

Next: observe this poll commit's FAST, repair any red gate, then continue Admin
mutation/action audit and Home/Exam/shared requirements. Worker poll status handling
of a nonexistent ID is a separately identified unverified confirmation gap; inspect
with isolated database tests before changing it. QR code remains untouched and
acceptance OPEN. Whole mission NOT MET; device/reference/legal/FULL limits remain.


## Live continuation: Worker poll status confirmation (uncommitted)

Fetched main remains 14c9e61be42fd7b969d5788b9ddf37d406df9ce4, no concurrent
changes. Exact-SHA FAST 41406623011 is still IN PROGRESS (Android build; backend
and Firebase jobs SUCCESS). Do not push over/cancel this required validation.

Pending local files: backend/src/routes/polls.ts and
backend/test/poll_status_confirmation.test.mjs plus requirement ledger. Worker PUT
now rejects non-boolean active, conditions audit on changes()=1 in the same D1
batch, and returns 404 for zero updated rows. Existing authorization is untouched.
Actual isolated SQLite/Hono test passes for malformed input, missing row/no audit,
and successful update/audit. Full backend npm test: 289 PASS, zero failures/skips;
npm run build PASS. Initial npm command used repository root by mistake; rerun
from backend/ completed successfully. No deployment, migration or production write.

Exact next action: wait for FAST 41406623011; repair red before proceeding. Then
review pending Worker diff, add denied-role/transaction rollback cases if needed,
run final docs/diff checks, commit coherent second poll fix to main, and verify
its exact-SHA FAST. Continue remaining Admin/Home/Exam/shared work; QR remains OPEN
and unchanged. Whole mission NOT MET; device/legal/FULL limits unchanged.


Worker poll review result: added real server-right denial and injected audit-failure
rollback cases, plus JSON null validation. Final local backend npm test: 291 PASS,
0 failures/skips; npm run build PASS. The conditional audit uses the immediately
preceding UPDATE changes() inside the same D1 batch; zero-row updates return 404.
Ready for coherent second poll commit once base FAST 41406623011 finishes.
Remaining whole Admin/Home/Exam/shared acceptance stays NOT MET.

Base 14c9e61be42fd7b969d5788b9ddf37d406df9ce4 exact-SHA FAST 41406623011
SUCCESS observed before push; no concurrent remote commit. Worker status fix is
the second coherent poll implementation batch. Its carrying SHA requires FAST.


## Live pointer after Worker push

main a8433b6484b7141f881aa5fb271808703c042302 pushed fast-forward; working tree
was clean immediately after push. Base poll FAST 41406623011 SUCCESS. Current
exact-SHA FAST 41407181451 is IN PROGRESS; docs 41407181426 also triggered. Local
291 backend tests and TypeScript build PASS. No production deploy/migration.

Next confirmed source-level functional gap outside QR: D4 MainActivity.renderChips
reuses chips when categories match, but initial onSelect closure tests
category != selected against the creation-time selected argument. After selection
changes, returning to the original selected category can be ignored. Existing
HomeViewModel.selectCategory already compares current state and handles no-op,
validation/persistence. Exact next action after green Worker FAST: remove stale
Activity guard and dispatch to that existing state owner; add a real repeated
All -> Other -> All callback/state regression test. Do not add a second selection
owner or recreate chips to hide the closure bug. Whole mission remains NOT MET.


## Home D4 live-selection callback implementation

Recovered main a8433b6484b7141f881aa5fb271808703c042302; exact-SHA Worker FAST
41407181451 SUCCESS. Official upstream HEAD rechecked unchanged full pin
3de37c8cfb039452be7398909461da6288babf15. Existing local checkpoint preserved.
HomeAppearance now hosts the same canonical chip rendering code previously in
MainActivity.renderChips. Activity passes current ViewModel selection getter and
existing selectCategory action; reused callbacks no longer capture obsolete state.
No new state owner, visual component, backend change or QR change.

Meaningful regression: 3 actual renderer/native callback tests FAIL under old
captured guard and PASS under live getter. Repeated All/Other/named transitions,
selected no-op event counts, chip reuse, reordering, presentation recreation,
fallback, stable measured widths, Day/Night/Hindi/RTL/200% font are exercised.
Existing ViewModel persistence and native touch fixtures remain. Source map and
limits: TELEGRAM_REMAINING_REQUIREMENTS.md D4 entry; all adaptation, no EXACT claim.

Serial local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 PASS: 769 JVM tests / 178 suites, no
failures/errors/skips. Python helpers 59/docs/claim/diff PASS. Physical device and
whole-screen/source-motion acceptance remain OPEN. Carrying commit needs FAST.

Next action: verify carrying D4 exact-SHA FAST, repair red before moving on. Then
Gate 2 extend existing TelegramMenuPopup from current Theme/History/Bookmarks to
latest owner's seven actions using existing destinations/logout confirmation;
audit existing popup source maps, lifecycle/scroll and older whitelist tests.
No third poll refinement, QR-only iteration, release gate bypass or production
deployment. Full ledger remains NOT MET/BLOCKED where previously documented.


## Gate 2 seven-action canonical Home popup

Base main 936e93aae0273daea08ea0e14fd6f9538d60f7b7 exact-SHA FAST 41409105317
SUCCESS, backend/Firebase SUCCESS, native navigation/publication skipped. D4 filter
callback is source/host/FAST verified; physical matrix remains OPEN.

Current canonical TelegramMenuPopup and MainActivity now wire all seven owner
actions: Theme, History, Bookmarks, Mistake Notebook, Topic-wise Test, PYQ, Logout.
Existing destination Activities and existing logout/presence/cache flow reused;
new native confirmation is single-window guarded and disposed on host teardown.
Existing scroll cap, source roles/assets, shadow, radial selectors, motion and
back/outside behavior preserved. Latest owner requirement supersedes old tests
removing four actions; assertions now require all seven accessible source rows.
Actual popup host test verifies six callbacks after dismissal and no duplicate
callback from stale rows; stale-row case first failed, then isShowing guard fixed.

Final local serial :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64 PASS: 770 tests / 179 suites,
zero failures/errors/skips. Python helpers 59/docs/claim/diff PASS. Initial missing
string reference and old removed-row assertion repaired without weakening current
requirements. Expanded Android fixture compiles, no physical run claimed. Source
map/dependencies/remaining limits in TELEGRAM_REMAINING_REQUIREMENTS.md latest entry.

Carrying seven-action commit needs exact-SHA FAST before next feature. Next action:
observe FAST, repair red if any, then Gate 2 shared dialogs/drawer/search/toolbar
production caller audit and first reproducible independent gap. Preserve both poll
commits, Profile/native and QR code; all whole-screen source/device acceptance
remains NOT MET/OPEN as recorded. No FULL/release/deploy/legal gate bypass.


## Seven-action feature second/final planned batch: locale and source icon reuse

Base main a6c0e042f58135d234b450e50c3c5f96ce38757c FAST 41411244614 SUCCESS,
docs 41411244627 SUCCESS. No concurrent remote implementation observed. Actual
Home popup callbacks, D4 filter fix and previous Profile/poll/QR work preserved.
Current second batch local results: 772 JVM tests / 179 suites, no failures/errors/
skips, AndroidTest compilation/lint/x86_64 APK PASS. Python helpers 59/docs/claims/
diff PASS. Actual popup tests cover Night and Hindi/RTL 200% font with full localized
accessibility labels and all six destination callbacks/stale-row cancellation.
Existing original notes/paper icons now replace generic icon choices; eight density
assets match official pinned Git bytes. No imported bytes changed or legal gate set.

Carrying commit requires exact-SHA FAST. This is the second planned successful
implementation commit for the seven-action feature; do not open cosmetic third
iterations. Hardware/reference acceptance is still OPEN. Next Gate 2 work: inspect
remaining source-faithful drawer/dialog/search/toolbar integration from actual
upstream and current callers; preserve working implementations and fix evidenced
gaps only. Drawer currently also has existing Topic/PYQ/Mistakes destinations;
these are active navigation entries, not duplicate renderers/screens. They are not
removed merely because overflow links the same destination. No production deploy,
migration, FULL/release gate bypass or claimed device pass. Full ledger NOT MET.


## Home exam-list integrity continuation

Fetched main bc139bd3b2cc236d20d399e64e41531bf812d269 and its exact-SHA FAST
41413589407 SUCCESS, docs 41413589354 SUCCESS. No concurrent changes. Seven-action
Home menu feature has TWO successful implementation commits (a6c0e042 and
bc139bd3); preserve it and do not start cosmetic third iterations. Official HEAD
rechecked unchanged 3de37c8cfb039452be7398909461da6288babf15.

Actual HomeViewModel.load saves/publishes ExamRepository.getExams output. Missing
data in a success response previously became empty and could replace valid cache.
Canonical repository now rejects missing data while retaining valid empty/sorted
lists, category mapping, existing error type and cancellation. All actual callers
(Home, Admin, EditExam, Settings and repository filters) remain on this same method.
Worker GET /api/exams:68-84 confirms nonnull array contract. No backend/scoring/
locks/timer/answer/data migration change. EVE business validation, no Telegram
transplant/parity claim. Three actual repository tests include before/after failure.
Cache retention is inferred from caller flow, not claimed as native/authenticated
end-to-end verification.

Serial local broad Gradle (:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
:app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64) PASS: 775 JVM tests / 180
suites, no failures/errors/skips. Python helpers 59/docs/claims/diff PASS. Carrying
commit requires exact-SHA FAST. First next action: observe FAST, fix red if any,
then continue Gate 2 actual Home/dialog/drawer/search/toolbar callers and remaining
full ledger. Latest official DrawerLayoutContainer.java is a 214-line host whose
onTouchEvent returns false; do not transplant it as if it contains classic drawer
physics. A complete matching active source subsystem has not been established;
preserve current working AndroidX gesture owner rather than inventing source math.
Native/reference/physical matrix and GPL/FULL/release remain OPEN/NOT MET.


## Gate 2 shared dialog policy / active question-form retention

Base main e1885471f913d7499116f9e4fe6fa1a6ae31d5bf FAST 41457197984 SUCCESS,
docs 41457198260 SUCCESS. No concurrent implementation overwritten. Added actual
upstream AlertDialog dismissDialogByButtons policy to existing native renderer;
no new dialog system. Actual gated ManageQuestionsActivity owns one editor window,
canonical builder retains invalid/server-failed drafts, shows inline live-region
errors, disables pending submit/cancel and closes only on server success. Existing
VM/repository/Worker authorization remains canonical; optional failure callback and
cancellation fix avoid silent failure. Constructor injection enables real VM/repo
handler tests without eager authorized reads or production request execution.

Old unconditional dismiss policy was transiently restored: 3 actual Dialog tests
failed, fixed policy passes. Actual production form -> VM -> repo test uses held
API continuations for validation/pending/double tap/denial/retry/success. Two initial
fixture issues (async OnDismiss assertion and layout script selector) repaired;
no legitimate assertion or gate removed. Final serial local Gradle unit/AndroidTest
compile/lint/x86_64 assemble PASS: 782 tests / 183 suites, no failures/errors/skips.
Python helpers 59/docs/claims/diff PASS. Source/adaptation map and exact limitations
are in TELEGRAM_REMAINING_REQUIREMENTS.md latest entry.

Carrying commit requires its own exact-SHA FAST. Native/authenticated UI, TalkBack,
matched appearance, rotation/process-death draft/pending-request restoration remain
OPEN, not claimed passed. Exact next action: observe carrying FAST; repair red
before new feature. Then continue Gate 2 remaining shared-dialog callers and form
state/lifecycle, drawer/search/toolbar source tracing before later gates. Preserve
all green Home/menu (two commits), poll (two commits), Profile and QR work. No
production deployment/migration, scoring change, legal/FULL/release bypass. Whole
mission NOT MET. The source onTouchEvent=false DrawerLayoutContainer finding stays
a scoped trace limitation, never an invented drawer physics implementation.

## C5 selected question-list ownership continuation

Recovered clean main 412edc6ed80ca867635cbcf8cee06e56196728f4; exact-SHA FAST
41462667732 SUCCESS, docs 41462667822 SUCCESS. Official Telegram HEAD unchanged
3de37c8cfb039452be7398909461da6288babf15; no upstream imports changed. Gate 2
shared question-editor lifecycle tracing exposed a prerequisite C5 race: old exam
reads and mutation completions could replace current exam rows. Implemented in the
existing VM and spinner caller; no parallel state owner/screen. First six real
VM/repository tests reproduced failures before repair; actual Spinner callbacks
also cover reorder/rebind/new-View retained identity/fallback/no-op. Detailed scoped
ledger and limits in TELEGRAM_REMAINING_REQUIREMENTS.md latest C5 entry. This is
not a full editor rotation/process-death solution; that stays OPEN. No production
writes/deploy/migration, scoring changes or QR work. Carrying commit requires its
exact-SHA FAST; on red repair before unrelated implementation. Next eligible step
after green: active shared-dialog validation/server-failure callers (including
ManageQuestions promptRenameExam), then remaining full Home/Profile/Admin ledger.

C5 local final checks: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 791 tests / 184 suites, zero failures/errors/skips. Nine new ownership tests PASS; initial six fail-before evidence in question-list-before.log. Python CI helpers 59/docs/acceptance/diff PASS. Historical bare SDK 35 Main Thread exception also appeared in this successful suite; root cause remains UNKNOWN, not suppressed. FAST still required for carrying commit.

## Active exam rename continuation

Base main 03c07dc45031b9c2361db53e7ba650dca729bdce exact-SHA FAST 41466628937
SUCCESS, docs 41466628886 SUCCESS; artifact list empty (FAST publication skipped).
No concurrent changes on fetch. Active shared-dialog caller audit found Android
examName vs Worker newName mismatch, invalid form auto-dismissal, unobserved errors,
and missing-target false Worker success. Coherent fix covers existing repository,
VM, same dialog renderer, gated Activity caller, English/Hindi resources and Worker
route, preserving installed-client alias and existing transactional name cascades.
No production deploy/migration. Source classification ADAPTED; see current ledger.
C5 selected-list ownership and all earlier green Home/Profile/QR/poll work preserved.

Fail-before evidence: two actual form tests fail; four of six original isolated
Worker cases fail. Fixture extraction keeps the sole real production form/caller,
not a competing implementation. First backend command had an incorrect relative
path; subsequent TypeScript check caught required condition.bindings, repaired
locally. Final uniquely named logs/results are authoritative, not overlapping early
logs. Native authenticated rotation/process-death draft restoration remains OPEN.
Carrying commit needs exact-SHA FAST; on red repair before unrelated features.

Rename final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS (794 JVM tests / 185 suites, zero failures/errors/skips). `cd backend && npm test && npm run build` PASS (299 tests, no failures/skips); eight new real-handler cases. CI helpers 59, docs, structural acceptance and diff checks PASS. Bare historical SDK 35 Main Thread line still UNKNOWN in successful JVM suite. Carrying exact-SHA FAST required; FULL/native/reference/legal acceptance not established. After green continue remaining shared-dialog/Home/Profile production paths; next Profile category/data restoration/identity audit, preserving no Add bio/About and existing confirmed-store/request-owner mechanisms.

## Profile editor restoration ownership continuation

Recovered clean main 196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee FAST 41469072072
SUCCESS, docs 41469072384 SUCCESS. Official Telegram HEAD rechecked unchanged
3de37c8cfb039452be7398909461da6288babf15. Rename Android+Worker coherent batch has
one successful implementation commit; no production deploy. Whole mission NOT MET.

Existing editor guard rebound current Firebase identity after recreation, losing
draft ownership. Current implementation saves original owner with native registry,
passes owner metadata from three active Profile callers, rejects mismatched/unknown
restoration before inflation, and retains existing live listener cleanup. Protects
Name/Phone/Username and retained Bio compatibility Activity; no visible Bio/About
row restored. Actual production Activity tests use only synthetic SDK boundary,
not an auth bypass in shipped code. Fixture bootstrap errors separated from real
fail-before evidence (two test failures, two existing behaviors pass). Fixed
original tests pass; expanded per-editor tests/broad checks pending below. Existing
API readbacks/queued-request owner metadata/Worker authorization unchanged.

First action on interruption: inspect this carrying commit/local changes and CI.
Finish required broad checks/FAST, repair red before new implementation. Then
continue full ledger Home/shared/Profile functionality; pending-request restoration
and device/reference matrix remain OPEN, not claimed solved by draft ownership.
No production deploy/migration, scoring/security downgrade, new account service,
GPL variable change or speculative QR iteration. Source classification ADAPTED EVE
adapter, not exact Telegram account implementation.

B5/B7 ownership final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 805 JVM tests / 186 suites, zero failures/errors/skips. Eleven new Activity lifecycle tests PASS; Python helpers 59/docs/structural acceptance/diff PASS. Existing backend unchanged from predecessor FAST (299 tests). Historical bare SDK 35 Main Thread warning persists UNKNOWN; no suppression. Exact-SHA FAST required. Next independent Profile investigation: username availability callback cancellation/error ordering, field pending-save restoration and remaining source UI paths; preserve this ownership guard and no Bio/About rows. Full mission/device/GPL/FULL acceptance NOT MET.

## Username source theme/localization continuation

Base 2212767c4de9e0d23edd4439ff5bba0ab85215a3 FAST 41472089611 attempt 2 SUCCESS,
docs 41472089940 SUCCESS. First attempt stopped at transient CMake SDK TLS handshake;
failed job rerun at unchanged SHA passed, no source/gate changes or suppression.
Generated Gradle cache entries were trimmed locally to free ~300MiB; source,
dependencies and evidence logs preserved. Official upstream remains pinned at
3de37c8cfb039452be7398909461da6288babf15. No concurrent implementation overwritten.

Four actual Username Activity tests reproduce hardcoded status contrast/missing
Hindi failure before repair. Existing canonical color roles/palette and Activity
callback now use original three source role values, existing accessibleForeground
policy, native semantic status/live region and localized English/Hindi resources.
Actual source-file hashes, night byte comparison and source properties are recorded
in color-migration/source-role-contract.json and current requirement ledger.
Five-character EVE username contract is intentionally retained, classified ADAPTED
against source's Telegram-specific four-character flow. No Telegram accounts/API,
backend mutation, scoring, new renderer, Bio/About row or QR implementation change.

Carrying exact-SHA FAST required after broad checks. First next action on resume:
reconcile local/main and carrying CI; repair red before another feature. Then
continue actual pending profile/edit/lifecycle and remaining Home/shared/Admin/
student paths, preserving completed work. Whole UI/device/reference/GPL/FULL/APK
acceptance still NOT MET; no protected FULL eligibility or release approval implied.

Username final local checks: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 809 tests / 187 suites, zero failures/errors/skips. Four new actual Activity status/theme/contrast/localization tests PASS. Python helper 59/docs/claims/diff PASS; three source scalar comparisons and Night full-byte identity PASS. Bare historical SDK 35 Main Thread line still UNKNOWN; no suppression. Exact-SHA FAST required; complete Profile/F1/source visual/device acceptance NOT MET.

## Shared question-response integrity continuation

Base live main 372b108a0b1b1c957566c53eeb7fd69b139e7a3e FAST 41475078791 SUCCESS,
docs 41475079226 SUCCESS. Existing canonical getQuestions converted missing data
to empty success; same-exam VM refresh erased confirmed rows. Actual repository/
Gson/VM fail-before tests reproduce missing data and null entries. Guard now
rejects them, preserving valid arrays/order/mock/topic/PYQ filters, cancellation,
request ownership and server-redacted answers. No backend/scoring/schema change.
Five added host tests pass. Final serial local unit/AndroidTest-compile/lint/
x86_64 assemble PASS: 814 tests / 188 suites, zero failures/errors/skips; helpers
59/docs/claims/diff PASS. Required carrying exact-SHA FAST still pending at commit.

Category B4/F1 theme repair was separately prepared and tested: native unselected
ring remains stale after live theme traversal. Small existing-component repair
passes 811 tests /188 suites and broad local checks, but category already has TWO
successful feature commits verified LIVE: feb9c888 / FAST 41163967661 SUCCESS,
14a62972 / FAST 41165900930 SUCCESS. Owner exception requested asynchronously
under ENGINEERING_STANDARD.md:31; owner explicitly answered "Haan, ek additional repair commit authorize hai" while this question-response batch was being prepared. ONE additional coherent category repair commit is authorized after the current required FAST. Patch is NOT applied,
committed or pushed; local tree restored only after archived copies/hash checks.
Review /workspace/pending-category-theme-repair/REVIEW.md; repair.patch SHA-256
236f8f9716ba51d0b14bbf7dec20a688cd63328ebac0227bfa03a6d11699e757;
base 372b108a0b1b1c957566c53eeb7fd69b139e7a3e, git apply --check PASS. Full copied
source/tests/map + metadata.json retained in same archive. If authorized later,
reconcile current main, apply reviewed patch, run checks and exact-SHA FAST. Do
not treat this external local artifact as an integrated main implementation.

Ledger IDs normalized: editor restoration/status batches belong B5/B7 (fields/
identity), not B4 (category). Nothing dropped; old evidence/commits preserved.
Latest successful UI status source scalar+Night identity evidence and legal notice
remain on main. No new QR iteration, device pass, legal release or FULL/APK claim.

First next action: observe carrying FAST; repair red before another feature. Then
continue actual Profile popup backdrop paint/alpha lifecycle and Home/shared/
Admin/student requirements. ProfilePopupBlurBackdrop has one prior implementation
commit (89254123); inspect real paint/runtime/source before changes. Complete
mission remains NOT MET. Production Worker/D1 deployment and GPL review remain
separate; no release variable touched. Unavailable hardware does not block code.

## Authorized category live-theme repair

Recovered main 381a76a610a3f1847f59a4c48939d7f2e4e6aea8 FAST 41479914039 SUCCESS,
docs 41479913807 SUCCESS. Owner explicitly authorized ONE additional category
repair commit: "Haan, ek additional repair commit authorize hai". Reviewed archive
patch/hash applied without conflict; source/test bytes unchanged from review.
Existing row updatePalette is called by canonical ThemeApplier, preserving same
indicator/selection/animation/click/geometry and prior color-role adaptation.
Source map, two preceding successful category SHAs and approval are recorded in
EVE_PROFILE_CATEGORY_PROVENANCE.md. Final local broad checks PASS: 816 JVM tests /
189 suites, zero failures/errors/skips; AndroidTest compile/lint/x86_64 assemble,
helpers59/docs/acceptance/diff PASS. Carrying exact-SHA FAST mandatory; this is the
single specifically authorized third category repair, not unlimited iterations.

First next action: observe carrying FAST, repair red. Then prioritize newly traced
EditExam question-action visibility/permission + active duplicated question form
callbacks, C1/C5, over cosmetic popup rim work. EditExam only gates manage_exams
while question mutations require manage_questions; Worker enforcement preserved.
Question UPDATE/DELETE zero-row fake success/false audit and UI Undo contract need
actual handler tests, never guessed restore semantics or live writes. Current
source has one active EditExam add button and separately built dialog using the
same dialog_add_question layout; canonical ManageQuestions builder already handles
confirmed/invalid/failed submissions. Reuse it if compatible without creating
another ViewModel/state owner or removing destinations. Full mission NOT MET;
QR/reference/physical/FULL/GPL/release gates remain OPEN. No production deployment.

## Question mutation confirmation continuation

Live recovered main d2b649f5bac8f4b86549ff854385bafbf3d97c6a FAST 41481845128
SUCCESS, docs 41481845112 SUCCESS. Category authorized third repair integrated;
no additional category exception or iteration. Current server question mutation
change is independent: conditional atomic audits, missing UPDATE -> 404, existing
DELETE idempotent absence success retained without false duplicate audit. Actual
Hono + all-migration SQLite tests reproduce 3 failures before repair, 5 controls
pass; initial fixture POST path corrected before product evidence. Full backend
307 PASS, TypeScript build PASS, helpers59/docs/acceptance/diff PASS. No production
write/deploy/migration; Android source unchanged from verified predecessor. Own
carrying exact-SHA FAST required; fix red before unrelated implementation.

Owner Undo product question is PENDING, not answered by generic resume message:
current AppUndoBar comment mandates immediate delete while no supported restore
API exists. Real DELETE then UPDATE test proves current fake restoration. Choices
asked: Telegram-style five-second deferred commit, or immediate delete plus staged
safe restore API (production deployment/migration still separately authorized).
Do not assume recommended option or create an invented restore service meanwhile.
Continue independent EditExam question-action role visibility and canonical Add
form integration after carrying gate green. Avoid another ViewModel/data owner:
EditExam already owns its repository/list; share stateless existing form presentation
with submit/busy callbacks if compatible. Fresh entry gate is manage_exams, but
question Add/import/edit/delete Worker routes require manage_questions; hide/guard
those controls with fresh returned rights and preserve Worker enforcement. Existing
private unused AdminActivity forms remain unrelated; don't delete by name alone.

Other known actual paths: EditExam add/edit validators auto-dismiss invalid inputs;
its delete currently changes list/shows success before network and Undo UPDATE
cannot restore. Full C1/C5/global/device/reference/FULL/GPL acceptance NOT MET.
First next action: verify carrying FAST, then independently repair real role/form
caller path, incorporating actual Undo answer only when it arrives. No speculative
QR work, source parity inflation, license variable change or production deploy.

## EditExam child-action authorization / canonical Add form

Base 5bf15f8444dd65d5e947b43a538404db701a500f FAST 41523395700 SUCCESS, docs
41523395561 SUCCESS. Server question confirmation batch has 307 backend tests
PASS, no production deployment. Owner Undo timing/API choice is STILL PENDING;
generic resume was not its answer. Continue independent authorized work.

Actual protected EditExam Activity tests isolate ONLY auth/data boundaries and
reproduce 4 failures/1 passing owner+denied-entry control. Fresh child rights and
host identity lifetime are now wired into production controls; same canonical Add
presentation handles both real active creation entrypoints without another VM/
repository/state owner. EditExam retains its list/repository, title and mutation;
window owned/dismissed on destroy. Shared AdminAccessGate hides/closes stale UID/
email host and cleans listener. Question row callbacks guard stale host rights.
Seven expanded actual Activity tests + existing real canonical form/VM/repo tests
PASS. Local serial broad Gradle PASS: 823 tests /190 suites, zero failures/errors/
skips; AndroidTest compile/lint/x86_64 assemble, helpers59/docs/acceptance/diff PASS.
No backend/scoring/answer API or new permission change. Source ADAPTED existing
native dialog policy + EVE authorization adapter; no whole-device parity claim.

Existing source assertions updated for canonical ownership while preserving four
modal workflows, original inputs/mutations, cancel and destructive styling checks;
no gate/legitimate assertion suppressed. Other dialog_add_question XML callers
Analytics/Flagged EDIT existing records; unused AdminActivity Add private method
has no caller. Neither was deleted by name or confused with an active duplicate.

First next action: verify carrying FAST; repair red. Then PRIORITIZE newly traced
Analytics/Flagged question edit target identity and nested-rights bug: their lookup
uses id == target OR questionText == text, so a first same-text row can override the
provided authoritative ID. Entry rights are view_analytics_logs/moderate_feedback,
not manage_questions. Trace real handlers/adapters, add meaningful regressions,
fix with one canonical target resolver and fresh action bindings, preserve existing
roles/data/score and do not invent support for unavailable generated-question IDs.
Also OPEN: EditExam bulk-picker result/context restoration through pending fresh
permission after recreation; current callback fails closed. Undo decision pending;
implement its chosen model only on actual owner answer. Source/UI/native/FULL/GPL
acceptance remains NOT MET; QR remains preserved/OPEN, no speculative iterations.

## C1/C2/C5 question review target, nested rights and response integrity

Recovery base e83b2d4c918340d18da5bddb934bfddd2abdb80e: exact-SHA FAST
[41527865210](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41527865210)
SUCCESS, documentation 41527865279 SUCCESS, no published artifacts. Live main and
official Telegram HEAD rechecked; upstream remains
3de37c8cfb039452be7398909461da6288babf15 (tree
cc3c6ccf48e4167fdec764dfd04e765043f55093), official origin, tlottie gitlink
31f1b542f88e7b4be9a01e749920d857535fc715. Optional uninitialized submodules are
not claimed verified. Existing successful QR/Home/Profile/poll code preserved.

R1: resolve known question ID exclusively within the requested exam; no same-text
replacement for a missing known ID. Legacy ID-less reports retain unique scoped
text/legacy-ID compatibility. Ambiguous, cross-exam and unidentified targets fail
closed. R2: actual Analytics and Flagged row callbacks use the fresh gate's
manage_questions right; parent view_analytics_logs/moderate_feedback alone does
not grant question editing, and Exam Editor navigation needs manage_exams.
R3: both actual same-layout EDIT workflows now use createQuestionEditorDialog in
ManageQuestionsActivity, extending the existing canonical presentation rather
than adding a renderer/Activity/ViewModel. Add callers renamed with it. Existing
ID, Hindi fields, topic, PYQ and other metadata survive edits. Missing correct
answer requires selection instead of silently choosing A. Invalid, pending and
failed drafts stay open; owned windows dismiss on teardown, stale account callbacks
cannot submit. R4: failed/missing analytics/report data raises an error, never fake
zero/all-resolved success; valid empty lists and sort order remain supported.
Legacy report grouping is scoped by exam/type/identity, not global text. R5: an
updated question plus failed report dismissal is explicitly partial and retains
the draft, never a fully successful edit. Cancellation propagates in touched paths.

| Upstream property | EVE path:lines | Classification and evidence |
|---|---|---|
| TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | ManageQuestionsActivity.kt:368-487 -> AdminAnalyticsActivity.kt:123 / FlaggedQuestionsActivity.kt:132 | ADAPTED existing native Kotlin renderer/button-dismiss policy plus EVE validation and async action callbacks; actual Activity/native dialog tests, not whole original UI parity |
| No Telegram exam-question/admin backend equivalent imported | QuestionEditorTarget.kt:6; AdminAnalyticsRepository.kt:10; FlaggedQuestionRepository.kt:62 | ADAPTED EVE identity/data contracts; real repository response tests and protected Activity callers; no source transplant or license change |

Complete caller chain: fresh AdminAccessGate -> existing row adapters -> original
Activity lifecycle/repository owner -> scoped target resolver -> shared native
form -> ExamRepository.updateQuestion -> authenticated Worker questions.ts:265
manage_questions. Flag resolution additionally requires existing Worker
moderate_feedback reports.ts:162. No new API, local authority, answer exposure,
production request/deploy/migration, scoring, auth, or user-data changes.

Before repair: four real protected Activity cases failed (wrong same-text target,
nested-right exposure); four real repository cases failed (false empty/zero,
cross-exam grouping, swallowed cancellation). Expanded tests include eleven
protected Activity cases, five target-resolution cases and five real repository
cases. Test auth/transport boundaries are isolated fixtures, never shipped grants.
Two broad source-contract assertions initially failed because they assumed local
builders. They now trace the actual canonical native host, view, cancel/dismiss
policy and mutation callbacks; no native/authority/destructive assertions removed.

Local final integration PASS: 844 tests / 193 suites, zero failures/errors/skips;
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 (JDK21/Gradle8.7). Lint 0 errors / 1634
warnings. Helpers59, documentation, acceptance-claim structure, four verbatim
upstream imports and git diff --check PASS. Backend unchanged from green base
(307 tests in prior exact-SHA FAST); no redundant local Node run claimed. Bare
SDK35 Main Thread shutdown exception recurred without a stack trace; UNKNOWN P2,
not converted into a device/runtime PASS. Carrying commit/exact-SHA FAST pending.
R1-R5 MET at scoped code/host level, pending FAST integration; whole Admin/source/
runtime acceptance NOT MET.
Physical GPU/camera/reference/accessibility/navigation matrix and qualified human
GPL review remain OPEN/BLOCKED; no FULL or verified publication claimed.
First next independent defect: concurrent Analytics time-range loads (and Flagged
report-type loads) can publish stale results after a newer selection. Reproduce
through existing owners, then fix scoped request ownership; do not create parallel
state owners. Question Delete/Undo product choice remains explicitly unanswered;
no generic resume message is treated as an answer. Bulk-picker pending-gate URI
restoration also remains OPEN. QR acceptance stays OPEN; no speculative QR work.

## C3/C5 Analytics/report read ownership implementation and evidence

Base main 9cf0523ed859b68e70c03911b09d08dce97a2634 exact-SHA FAST
[41532575336](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41532575336)
SUCCESS, docs 41532575497 SUCCESS. That predecessor's R1-R5 question-review
integration is verified; full screen/device/source parity remains OPEN. Its FAST
skipped native UI/publication, with no verified APK artifact.

Current R1-R5 code/host scope: canonical Analytics ViewModel cancels the prior job,
generation-checks before a dependent question read and before success/error, and
invalidates on clear. Existing Flagged Activity does the same in its original
lifecycle owner; switching type removes old actionable rows and invalidates stale
bound Edit/Dismiss callbacks. No parallel repository/renderer/ViewModel introduced.
Both existing controls restore selected requests before initial protected loads;
saved Activity state is persistence, not a new data owner. Selected filter taps are
no-ops. Native report chips inherited non-checkable Material Assist defaults;
only these TWO actual filter controls now enable checkability/single selection.
Shared Assist style, Home filters and existing UIArc rendering/physics untouched.

Analytics no longer displays static 99.8%, zero fatal crashes/ANRs or an unverified
Crashlytics Active badge. Existing console link remains; English/Hindi unavailable
copy explains that this screen does not fetch telemetry. Empty report lists state
only no pending reports, never unproven all-systems-operational health. Actual
telemetry integration remains unavailable, not fabricated or marked working.

| Property / upstream relationship | EVE path:lines | Classification / evidence |
|---|---|---|
| EVE request/selection ownership; no Telegram exam/Worker subsystem transplanted | AdminAnalyticsViewModel.kt:28-59; AdminAnalyticsActivity.kt:45,76,86,127-135 | ADAPTED EVE business integration; real repository/ViewModel transport-order tests plus actual protected Activity filter/recreation callbacks |
| Existing native report controls/lifecycle | FlaggedQuestionsActivity.kt:58-120,124,136,160,167,214 | ADAPTED existing EVE/Material controls, not claimed original Telegram tabs; real native callbacks, reordered transport, stale actions and destruction tests |
| Existing dialog primitive remains AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | existing TelegramNativeAlertDialog -> canonical question editor, unchanged by this batch | ADAPTED existing source-derived host; predecessor source map/tests retained, no new imported source/assets or source-value claims |
| Existing EVE telemetry display contract, no upstream service imported | activity_admin_analytics.xml:28-38; English/Hindi strings; Flagged empty-state strings | ADAPTED truthful unavailable/empty states; actual Activity English and Hindi/RTL/Night resources tested; real telemetry/service/device acceptance NOT MET |

Fail-before evidence: four actual Analytics ViewModel/repository ordering cases
failed (old range/refresh overwrite, old failure, unnecessary obsolete second
read); one actual Analytics Activity telemetry case failed. Three initial report
Activity tests exposed non-checkable filters; one stopped at the missing second
request. These are NOT counted as a completed stale-report-response reproduction.
After enabling the real filters, expanded native callbacks exercise actual
out-of-order results, empty/error states, live stale-row actions and teardown.
Expanded 29 scoped cases PASS: six real VM/repository, eight real Flagged Activity,
two actual Analytics filter/recreated Activity, and thirteen retained/extended
question-review Activity tests. An early setup(null) harness error was corrected
to setup() for a fresh Activity; no product assertion was suppressed.

Full local integration PASS: 862 tests / 196 suites, zero failures/errors/skips;
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 (3m39s). Lint 0 errors / 1634 warnings.
Focused command selected AnalyticsReadOwnershipTest, AnalyticsFilterActivityTest,
ReportReadOwnershipTest and QuestionReviewActivityTest (29 PASS). Helpers59,
docs/acceptance structure/four upstream byte imports/diff PASS. R1-R5 MET at scoped
code/host level; carrying commit/exact-SHA FAST integration PENDING. Backend unchanged
from the green predecessor (307 tests); no production request/write/deployment,
migration, auth/scoring/data contract change or new permission. Whole C3/C5 and
source/device acceptance remain NOT MET; qualified human GPL/FULL/publication
safeguards remain intact. Historical bare SDK35 shutdown exception remains UNKNOWN.

Next independent implementation after carrying FAST SUCCESS: inspect EditExam's
bulk-picker Activity-result callback while the fresh AdminAccessGate is pending;
reproduce lost selected URI, then retain/consume it once only under the verified
same account/exam. No new permission, parallel import flow or production writes.
Question Delete/Undo decision remains explicitly unanswered; generic resume is
not approval. Related P2 limitation: existing QuestionAnalytics API has no days
parameter (all-time aggregates); this batch does not invent range-specific question
statistics. QR acceptance remains OPEN and does not block independent work.

## C1/C5 metadata save integrity and cutoff backing

Base 980a92cf4a10e4b16b0a691303b4c340ad30bf6e FAST 41535796716 / docs
41535796467 SUCCESS; the Analytics/report read-ownership carrying gate is verified.
Higher-impact default/hidden-settings metadata writes reprioritized before queued
picker recovery. Complete scoped ledger, source relationships, caller chain,
fail-before evidence and rollout limits: EXAM_METADATA_SAVE_CONTRACT.md.
Current local Android883/198 suites, AndroidTest compile/lint (0 errors/1629 warnings)/x86_64 assemble and backend318/TypeScript PASS; carrying FAST pending.
No production migration/deployment. Cutoff persistence is development-stage only;
old deployed service cannot falsely confirm it because client verifies readback.
First next independent action after carrying FAST: pending fresh-gate picker URI
recovery. Undo product answer remains pending. Whole mission/QR/device/FULL/legal
acceptance remains OPEN/NOT MET. Existing successful feature/commit ledger preserved.

## C5 import result handoff/read lifecycle

Recovery main 07b448e5e70b53569db6a4ba328bd0baf34edca8, FAST 41544036446 and
docs 41544036310 SUCCESS. Queued picker defect now has actual registry/provider
fail-before and expanded host evidence. Implementation/source/requirement ledger:
EXAM_IMPORT_RESULT_LIFECYCLE.md. Single existing Activity/window owner; no new
permission, persistent grant, parser rewrite, backend or production operation.
Scoped16 picker tests and broad Android899/199 suites, AndroidTest compile/lint (0 errors/1629 warnings)/x86_64 assemble PASS; helpers59/docs/claims/four imports/diff PASS. Carrying FAST pending. First next
after carrying FAST: shared parser resource/batch bounds and sparse XLSX column
mapping. Physical providers/grants, unknown submit acknowledgement/idempotent retry,
whole source UI/device/FULL/GPL acceptance remain OPEN. Existing successful QR and
other work preserved; cutoff production rollout and Undo decision remain separate.

## 2026-11-03 shared parser continuation

Recovered 22fd6f8129e26bef75c7a26cc609a6525c1684e2 with FAST41568018939,
docs41568018836 and dependency41568018767 SUCCESS; no concurrent remote changes.
Official origin HEAD remains 3de37c8cfb039452be7398909461da6288babf15.
C1/C5 existing shared import parser repaired for sparse/inline/rich XLSX identity,
explicit Worker batch bound and bounded/cancellable input/decompression/XML.
Both actual callers retain existing native preview; fatal messages localized.
Requirement/source/evidence ledger: EXAM_IMPORT_PARSER_CONTRACT.md.

Local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (3m21s); 916 tests / 200 suites, zero failures/errors/skips.
Lint 0 errors / 1629 warnings. Document parser16 and actual picker17 cases PASS;
59 CI-helper tests, docs78, acceptance structure, four byte-identical imports and
git diff --check PASS. Backend unchanged from green main. Historical bare SDK35
thread diagnostic recurred without a JUnit failure; remains UNKNOWN, not resolved.
Carrying exact-SHA FAST pending; native navigation, physical providers, matched
visual captures, protected FULL, verified publication and legal clearance OPEN.

First next after carrying FAST: reproduce and repair ManageQuestions immediate
false deletion success with real callback/server-denial coverage, preserving
existing deletion semantics and unrelated pending Delete/Undo owner choice.
Standalone picker lifecycle remains separate. Whole mission NOT MET; QR OPEN;
no deployment, remote migration, signing or release-variable change performed.

## 2026-11-03 standalone deletion confirmation

Recovered clean d638d3b20a4c10e6462b6d95adb8f35fada3a584 FAST41571297755
SUCCESS, docs41571297931 SUCCESS. Import parser batch is integrated and must not
be replayed. Three actual ManageQuestions native callback tests reproduced pending
false success, denial masked by success and duplicate DELETE while pending. Existing
ViewModel/native dialog/caller repaired with confirmed success/failure callbacks,
busy/identity/current-target guards and localized destructive controls. Eleven
Activity and four added repository/API-response-boundary cases pass. No new owner,
renderer, endpoint or production operation. Scoped ledger/source map:
MANAGE_QUESTION_DELETE_CONFIRMATION.md.

Local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (3m57s), 931 tests / 201 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Helpers59, docs79, acceptance structure, four recorded
byte-identical imports and git diff --check PASS. Backend unchanged from green base.
Bare SDK35 thread diagnostic recurred with zero JUnit failures; remains UNKNOWN.
Carrying exact-SHA FAST pending. Native navigation, physical device/reference,
protected FULL, verified artifact publication and human GPL clearance remain OPEN.

Next: reconcile independently implementable D3 Home search/restoration requirements
against actual native callbacks; source currently restores query/list state without
a saved Firebase-owner tag (potential account-isolation gap, not yet reproduced).
Do not invent a seam/IME fix without evidence. Standalone ManageQuestions picker
lifecycle remains open; the import reliability batches must not be replayed.
Separate EditExam Delete/Undo product choice still pending; cutoff migration/Worker
production rollout still unauthorized. Whole mission NOT MET and QR acceptance OPEN.

## 2026-11-03 account-bound attempt/result handoff

Recovered 9fdeca47309ea9da0f228119591c410a70b4e057 with FAST41573024314
and docs41573024250 SUCCESS, no concurrent changes. Import d638d3b2 and standalone
delete9fdeca47 are green and must not be replayed. Official origin HEAD remains
3de37c8cfb039452be7398909461da6288babf15. Six initial isolation regressions
failed, including actual fallback navigation with another account's cached attempt;
same-account control passed. Canonical Home companion cache and ResultDataHolder
now bind owner/exam; all actual producers/consumers/internal result/detail launches
carry immutable source identity. Native screens reject foreign/missing tags and
dispose auth listeners; same-account render/recreation verified in Robolectric.
No new state owner, screen, backend, scoring or production operation. Full scoped
requirement/source map: ATTEMPT_RESULT_OWNER_ISOLATION.md.

Final local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (2m44s), 948 tests / 202 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Scoped ownership18 cases (including the intact moved
tracking assertions) PASS. Helpers59, docs80, acceptance structure, four recorded
byte imports and git diff --check PASS. One preceding local run's indentation lint
error was repaired; no suppression/baseline or assertion removal. Historical bare
SDK35 thread diagnostic remains UNKNOWN; no blanket crash-free/device claim.
Carrying exact-SHA FAST pending. Backend/Firebase source unchanged from green base.

Whole mission NOT MET; QR/device/reference/native-navigation/protected FULL/GPL
and verified APK publication remain OPEN. Cutoff production rollout is unauthorized;
separate EditExam Delete/Undo choice remains unanswered. First next after carrying
FAST: reproduce D3 Home search SavedState owner scope using actual production
callbacks, preserving existing successful seam/search work. Mission11 search has
285a9528/FAST40995030992 and460e093d/FAST41005412909 green; reconcile that feature's
two-commit budget before any additional integration, and obtain a concrete tested
repair exception if applicable. The search-owner risk is still INFERRED, not a proven
defect. Standalone ManageQuestions picker lifecycle also remains open; preserve
existing import batches and do not manufacture a third iteration under a new name.

## 2026-11-03 Home search owner repair prepared for exception review

Recovered c57dfa64693047e3e0b76530d4e930e4cd0baba1 FAST41575420756
and docs41575420725 SUCCESS; artifacts zero, native navigation/publication skipped.
Attempt/result ownership batch is integrated and must not be replayed.
Actual full MainActivity tests now reproduce manual and Android-hierarchy foreign
search restoration; same-account control PASS. Existing UID-tagged restore repair
preserves native cursor and original search renderer/physics, no new state owner.
Source/requirement ledger: HOME_SEARCH_OWNER_REPAIR.md.

Local complete validation PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (2m52s), 956 tests / 203 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Native Home8 cases PASS; helper59, docs81, acceptance
structure, four byte-identical imports and git diff --check PASS. No new backend,
production service, permission or Telegram source import. This result is local-only;
no additional Home search commit or exact-SHA FAST until owner exception arrives.

Two prior search commits reverified live:285a95288e7ee08eb9ef22d744965f8e7421627e
FAST40995030992 SUCCESS and460e093d30f55631c46e16a20c937da444d01c7b
FAST41005412909 SUCCESS. Section2 therefore requires an owner exception for one
additional repair commit. The earlier category exception was consumed by d2b649f5
and is not reused. Preserve the reviewed patch and all its assertions; no integration
claim before approval/commit/exact-SHA FAST. Whole mission remains NOT MET, QR OPEN;
no FULL, signing, release-variable change, production write/deploy/migration or
verified APK publication. Separate Delete/Undo choice and cutoff rollout remain
pending. Next action after exception: fetch/reconcile, apply reviewed patch if archived,
commit once to main, observe exact-SHA FAST and repair red before other features.

### Home exception authorization received

On2026-11-03 the owner replied "Haan, ek additional repair commit authorize hai"
to the reviewed Home confidentiality repair. Live main remained c57dfa64, clean;
patch SHA256 c996573268a71ac25bfa30ba041e5b2b0a0fffe3e33b6f74ad7c395c37873a77
and all eight full file hashes reverified. Exact tested source/test bytes reapplied;
only documentation then changed to record approval. Local956/203/lint0/1629/build
evidence remains applicable. Integrate once, require exact-SHA FAST, repair any red
forward before unrelated work. This Home exception is separate from the consumed
category exception. Next independent investigation after green: ManageUsers read/
detail response ownership and malformed data (source observations only; no new
implementation started while Home approval was pending).

## 2026-11-03 continuation: production ownership and account-isolation stabilization

Recovered live `main` f0c923994cfed1deb2026e0b09d3d64bb7982ed0 and official Telegram master/pin 3de37c8cfb039452be7398909461da6288babf15. Local Telegrammaster.zip SHA-256 matched recorded snapshot: 4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10, ZIP integrity accepted. No local Gradle/emulator run: remote GitHub Actions provide compile/unit/backend evidence, but device fidelity remains unaccepted.

- Production ownership registry and concrete anti-duplication host regressions committed as 0605998348346f30c5e44e97393116748c32b194. Exact-SHA FAST 41585490684 SUCCESS, docs 41585490736 SUCCESS. Existing QR identity/card/scanner renderers, payment-specific QR, dialogs, Home filter and Poll Worker preserved without deletion.
- Home/Notifications account-change response isolation committed as 6456b00814bc10a1f077c54768cb49390c63ce25. Exact-SHA FAST 41586337126 SUCCESS. One account's late broadcast read cannot be imported into another account's dynamic NotificationStore namespace; no change to inbox-only acknowledgment or backend.
- Next independently actionable ManageUsers correction uses the existing Activity, AdminRepository, AdminAccessGate and Worker API. Source examination found `success=true` with absent list was silently treated as zero users; page metadata and row identity were trusted without validation; status mutation reported success without comparing returned ID/status; details/error callbacks could outlive their selected owner/query/page. Prepared regression fixtures reject malformed/denied acknowledgments and trace restored owner, stale loads, detail and rapid action behavior. Carrying commit and exact-SHA FAST are PENDING at the time of this checkpoint text. No Worker deployment or permission changes.

Next after confirmed Admin FAST: inspect remaining independent Admin/student/quiz defects against production callers and preserve all green work. Full QR scene motion/device comparison, native two-navigation acceptance, six-job FULL and legal GPL release signoff remain OPEN; no production release authorization. Do not replay completed feature commits, and do not label source-level coverage as device parity.

## 2026-11-03 Admin read/ownership recovery and shared-gate lifetime

The production-ownership registry and account-isolated notification repairs were
integrated without recreating QR/Profile/Home/Worker implementations:
- 0605998348346f30c5e44e97393116748c32b194, FAST 41585490684 SUCCESS, docs 41585490736 SUCCESS.
- 6456b00814bc10a1f077c54768cb49390c63ce25, FAST 41586337126 SUCCESS.
- ManageUsers source/repository repair 33d2d23212f4f911af38a0a0f21a6a80e0b9cf2a initially failed FAST 41587771730 due to two brittle source assertions. The first forward-only test correction 2a7d5478f60fda0f4756b42098eb4b38fc48dfc9 still failed FAST 41589522149 on one stale literal assertion. The second correction 00ecb5cecdf4499c8202008b16079d076976c239 passed exact-SHA FAST 41590177002; backend and Firebase Functions succeeded, 243 Rust C API tests passed, Android JVM/build passed. Prior red runs remain visible, neither is represented as success.
- AdminRepository now requires actual users list, internally coherent pagination and unique nonempty user identities; matching mutation ID/disabled acknowledgment; and complete attempts list rather than fabricated emptiness. Existing authenticated Worker endpoints and permission policy unchanged. ManageUsers retains one Activity, rejects stale owner/query/page callbacks, and prevents overlapping details and status windows.
- New separately scoped defect found in the active shared AdminAccessGate: repeated ManageUsers onResume authorization previously registered additional Firebase AuthStateListeners and independent asynchronous permission requests, each removed only on Activity destroy. The next implementation adapts the *existing* shared gate with a per-root disposable lease and regression tests. No separate authorization source or replacement admin screen.

New shared gate FAST status: PENDING at time of this checkpoint. Native navigation tests, source-comparison hardware matrix, QR full-scene parity, protected six-job FULL, verified APK publication, and human GPL release clearance are still NOT MET/BLOCKED, not implied by FAST. No Worker deployment, production D1 mutation, signing-key change or release approval was performed. Re-fetch main and exact SHA before the next feature; fix a red required gate forward-only.

## Admin user status request contract (source-only integration)

The current Worker status route accepted missing/malformed `disabled` JSON by
silently coercing it to false, which could enable an account on a bad privileged
request. A separate backend-only correction is prepared on the existing
`backend/src/routes/admin.ts` handler, preserving server `manage_users`
authorization, self/peer admin protection and atomic status+audit D1 batch.
It requires explicit JSON boolean values; malformed JSON, absent values, arrays,
numbers, and strings return HTTP 400 before any lookup or write. Existing
`backend/test/admin_users_pagination.test.mjs` runs the genuine route against
in-memory SQLite and asserts denied inputs do not mutate rows/audit, while a
valid mutation still succeeds. There is no additional endpoint or Worker deploy.

Carry the backend correction through the required exact-SHA FAST; at this
checkpoint it is staged only and its tests are NOT yet executed. Source and legal
Telegram parity labels, physical acceptance and production release status remain
unchanged. The shared-gate exact-SHA run is required to turn green first.

## Quiz Activity authentication lifetime boundary (staged independent repair)

Source review of the existing `TestActivity` found that `TestViewModel`
already checks owner-tagged bundle restoration and the attempt is server-scored,
but the Activity itself had no auth-state listener and could still finish a late
submission callback after Firebase changed accounts. The scoped change preserves
the existing Activity, ViewModel, clientAttemptId, test timer, scoring and UI,
reusing `SubmissionRetryPolicy.ownsSubmission` as the **only** owner predicate.

The Activity now binds its launch/restored UID, refuses foreign re-creation,
registers one listener during onStart, unregisters during onStop/onDestroy,
rechecks the owner after the async initial admin check and submit response,
and rejects a mismatched error/retry callback. It avoids persisting a foreign
account's abandoned session during onPause. New JVM owner-matrix/source-path
regressions live in `TestActivityOwnerLifecycleTest.kt`. These are source and
JVM safeguards, NOT native-device lifecycle or backend scoring acceptance.

Prior Worker payload validation 1a3d45c1dbddb29a75f4d749fb41d4ca3f9f8a81
completed exact-SHA FAST 41591963720 SUCCESS. The Quiz Activity implementation
is the current carrying batch and requires its own exact-SHA FAST. No production
endpoint, Firebase rules, D1 data, ABI or release flag changes.
Source-exact Telegram/runtime matrix and protected final FULL remain OPEN.

## Quiz ViewModel suspended-response isolation (prepared follow-up)

Following the integrated Quiz Activity host owner/lifecycle boundary, the existing
`TestViewModel.start` still awaited server attempt or question requests before
its final state mutations, without checking whether the captured Firebase owner
had been replaced. A narrowly scoped follow-up uses the existing
`SubmissionRetryPolicy.ownsSubmission` predicate after each startAttempt
suspension, after question loading, before session restoration/question
publication, inside error handling and before bookmark collection updates.
No new ViewModel, state owner, repository, endpoint, scoring, test timing or
data format was introduced. Targeted `TestViewModelNetworkOwnerTest.kt`
covers same/other/absent owner and actual source ordering.

Carry as ONE coherent follow-up to the same Quiz account isolation feature,
subject to the earlier host commit's exact-SHA FAST success. Until this
follow-up is committed and its own exact-SHA FAST succeeds, mark the change
PREPARED, not MET. Physical UI, two native navigation modes, QR scene parity,
protected six-job FULL, verified APK publication and human GPL release
authorization remain OUTSTANDING. No production Worker or D1 deployment.

## Notification cache malformed-row resilience (prepared independent repair)

Existing `util/NotificationStore.kt` read paths can encounter historical or
partial SharedPreferences JSON arrays. Before this repair, `getAll()` iterated
with `getJSONObject`, `getString` and `getLong`, which threw for a mixed
or incomplete row. Home's unread dot and NotificationsActivity both call
`getAll`; `markAllRead` also assumed every row was a JSON object.

The prepared fix keeps one store and its existing account-hashed namespace,
broadcast ID cursor, inbox-only acknowledgment and server interface. It adds
one tolerant entry decoder, preserves valid read/emoji/source metadata, and
passes a sanitized JSON object array to the existing add/import/mark-read
mutations. A Robolectric JVM regression covers malformed JSON, mixed valid
and invalid rows, read ordering and invalid optional emoji metadata. No new
notification owner, mutation handler or API call was introduced.

Status: STAGED ONLY, subject to previous exact-SHA FAST green and its own
FAST. Full Android device behavior, Telegram visual parity, final six-job FULL,
physical validation, APK publication and human GPL clearance are still OPEN.

## Admin Hub resume permission freshness (independent staged defect)

Existing AdminActivity only called its fresh AdminAccessGate during onCreate.
On resume from a child screen, it refreshed maintenance config but not the
server's current Admin rights. After server-side role revocation or promotion,
the Hub could retain outdated row visibility/search destinations until a full
Activity recreation, despite backend request-time enforcement.

The scoped existing-Activity repair uses AdminAccessGate's already-verified
one-lease-per-root design on return from STOPPED, reapplies freshly approved
permissions and the current search query, and reloads the permitted summary
without re-running one-time UI setup or collectors. The same existing
applyAdminPermissions path now treats the Owner as having all visible Hub rights
and preserves the Version settings click listener across role promotion.
A live maintenance dialog suppresses redundant background config refresh.
Source-path regression lives in AdminHubPermissionResumeTest.kt.
Backend rights, endpoints and permissions remain unchanged; no new framework.

Status: STAGED ONLY pending exact-SHA FAST of the prior independent
NotificationStore data-resilience commit. Android device permission/recreation
matrix, release distribution/legal clearance and six-job FULL remain open.

## Notification single-owner write serialization (prepared follow-up)

The canonical NotificationStore had a synchronized broadcast import, but its
local-add and inbox mark-all-read paths also read and rewrite the same stored
JSON array without the same monitor. Overlapping FCM/reminder delivery and
inbox/broadcast operations could clobber updates. The bounded follow-up applies
one JVM object lock to all JSON mutation paths (add, import, mark-read, deleted
user cleanup), retaining the existing cache namespace, cursor, sourceId, read
semantics and screens. Reflection regression covers real production method
synchronization. No new storage owner, worker endpoint or async state manager.

This is PREPARED ONLY pending the Admin Hub exact-SHA FAST gate; do not claim
concurrent device delivery was run. QR source/full-scene hardware evidence,
final six-job FULL, signing and human legal release clearance remain OPEN.

## 2026-11-03 student personal collections — owner-bound lifecycle (prepared)

Source review found that the existing HistoryViewModel captured UID for
getAttempts but could publish a suspended response after Firebase switched
accounts; existing BookmarksViewModel collectors and delayed Undo/mutation
callbacks similarly lacked a stable Activity opening-owner boundary. The same
screens and repositories are retained; no new data store, navigation destination
or extra Firebase observer abstraction is introduced.

The prepared integrated repair captures the authenticated launch/restored UID
in HistoryActivity and BookmarksActivity, fails closed on mismatched restore,
checks auth in onStart/onResume/renderer and result/bookmark navigation, and
unregisters the listener onStop. Existing HistoryViewModel pins its initiating
UID, cancels old load jobs and rechecks owner after getAttempts and in exception
callbacks. Existing BookmarksViewModel pins its initiating UID, rejects
stale flow emissions, guards mutation triggers, and cancels prior observe jobs.
Bookmark Undo and swipe mutations are also owner-checked.

New host/source+pure-policy tests:
`app/src/test/java/com/eve/app/ui/history/HistoryAccountBoundaryTest.kt`,
`app/src/test/java/com/eve/app/ui/bookmarks/BookmarksAccountBoundaryTest.kt`.
No production attempt/scoring endpoint or Firestore rules are modified.
Code remains STAGED and unverified until current notification write-lock
exact-SHA FAST passes and this batch receives its own exact-SHA FAST.
The all-device/auth-switch instrumentation matrix and final FULL remain OPEN.

## Student repository owner and malformed-success hardening (prepared follow-up)

The canonical BookmarkRepository holds mutable in-memory StateFlows per instance.
Before this scoped repair its explicit userId parameter was never checked against
the live authenticated UID. It also accepted late read/mutation responses after
an account swap, potentially publishing stale ids and rows in the retained
repository. The existing repository now binds its construction-time UID,
reuses SubmissionRetryPolicy's exact-match predicate, verifies owner before
requests and after suspensions, and preserves coroutine cancellation while
returning Result failure on a mismatched account. No new cache, identity
authority, API endpoint or backend rules were added.

The existing HistoryRepository previously converted a success envelope with
null list data into an empty history. That is not evidence the student has
zero attempts. Its same list method now rejects malformed data while genuine
empty lists and descending timestamps behave as before.

Targeted JVM host tests: BookmarkRepositoryOwnerContractTest (owner matrix,
actual source read/mutation guards, cancellation) and
HistoryMalformedResponseTest (real repository with fake API and empty/null/
ordered response matrix). This batch is PREPARED ONLY and may be integrated
after exact-SHA FAST of the owner-bound student Activity/ViewModel commit.
The original server rights/scoring, device/QR hardware parity and legal FULL
release gates remain unchanged.

## 2026-11-03 BookmarkRepository constructor FAST repair

Exact-SHA FAST 41598626045 for b78d27512bc33fbda5aaa079dd8a70101e8395b5
failed only Android JVM test `QuestionCumulativeTimerTest.testCumulativeQuestionTimer_sequenceAccumulation`
(994 total, 1 failed). The exception was `Default FirebaseApp is not initialized`
during `BookmarkRepository.<init>(BookmarkRepository.kt:24)` from
`TestViewModel.<init>`. Backend and Firebase Functions jobs passed.
This is a constructor-side regression introduced by eagerly reading
`FirebaseAuth.getInstance()`; not a timer correctness failure.

Forward-only repair changes only the original BookmarkRepository's binding
time: first valid authenticated request wins an immutable repository owner
under a synchronized guard. Retained repository instances still reject a
different UID, including after suspended reads/writes. A plain JVM
constructor regression and existing owner-response tests cover the contract;
`QuestionCumulativeTimerTest` is unchanged. One parent FAST run is still
FAILED historically; repair commit's exact-SHA FAST is PENDING until it
actually finishes. No duplicate repository, new API endpoint, timer rewrite
or production data mutation.

## Mistake Notebook per-owner learned flags and authoritative read (prepared)

Production `MistakesActivity.kt` previously stored learned question IDs in one
global `eve_mistakes` SharedPreferences namespace for every Firebase user.
That was a concrete cross-account personal-state leak. A server error or
malformed successful response was also incorrectly rendered as an empty,
completed notebook, suppressing retry.

The prepared repair retains the existing Mistakes Activity, API, list adapter
and filters. A tiny `MistakesPreferenceScope` utility maps verified nonblank
UIDs to distinct SHA-256-derived SharedPreferences namespaces. The unowned
legacy global flags are not assigned to any student. The same Activity binds
its original launch/restored UID, cancels late loads on auth changes, stops
rendering stale lists, and checks current ownership before learned-state
writes, filter changes and post-network processing. Server failures show a
separate tap-to-retry state rather than claiming `All Caught Up!`.
JVM/Robolectric regression tests exercise distinct account namespaces,
unchanged legacy prefs, owner boundary and failed-response renderer contract.

Status remains STAGED ONLY, pending preceding exact-SHA FAST success
and a new exact-SHA FAST. No production D1 mutation, Telegram component
replacement or device fidelity claim.

## 2026-11-03 Performance and Leaderboard account isolation (prepared)

The existing PerformanceViewModel could publish the old student's history-derived
summary after a request completed under another Firebase identity. The existing
LeaderboardViewModel loaded public scorers first, then captured the *then-current*
UID for private rank, potentially mismatching the Activity highlight adapter.
Neither retained Activity previously enforced saved-state UID or signed-in
account-switch lifetime.

This prepared change modifies only existing Activities/ViewModels. Each captures
and restores an original authenticated UID, rejects another UID, registers its
AuthStateListener at STARTED and unregisters on STOPPED, and gates UI renders.
Both ViewModels cancel old load jobs and verify owner after suspended network
calls and before publishing state. The original leaderboard adapter is now
initialized after saved-state/UID verification, rather than during property
initialization. Public top scorers and personal rank remain separate. New JVM
regressions: PerformanceAccountBoundaryTest.kt and
LeaderboardAccountBoundaryTest.kt.

Status: STAGED only until preceding Mistake Notebook exact-SHA FAST turns green;
then this change requires its own exact-SHA FAST. No source-device/QR/full
release evidence is implied. The source code of these destinations was verified
unchanged since baseline b78d275 by matching Git blob hashes.

## 2026-11-03 Settings account switch safety (prepared)

Source investigation found `SettingsActivity.executeLogout` awaited presence revocation
and then unconditionally cleared SessionManager/current Firebase login, with no
verification that the initiating user remained signed in. The destructive
`executeAccountDeletion` similarly awaited server deletion and Firebase
credential deletion without refusing a replacement account before subsequent
global session cleanup. Delayed Google/email reauthentication callbacks could
also continue the old account's deletion action under a different session.

This bounded existing-Activity repair also closes a retained Home task on
account replacement and checks Home's existing UID predicate after the
asynchronous presence revoke and before global logout. No new Home menu,
dialog or session manager is introduced.

The Settings repair pins the UID at launch/restoration,
checks fresh UID at lifecycle/dialog/callback boundaries, cancels late
operations on a real account switch, and rechecks before destructive network
and credential stages. After deletion, FirebaseAuth.currentUser may be null
automatically; a pure `SettingsDeletionBoundary` guard permits clearing
global login only for the deleted UID or expected null, never another
authenticated student. Existing password/Google reauth dialogs, the original
server deletion endpoint, presence revocation, and UID-specific cleanup
remain intact. A pure UID matrix and actual async source-order tests are
added. No production destructive test, account mutation, or backend deployment.

Status STAGED ONLY pending Performance/Leaderboard exact-SHA FAST before safe
commit; final device/account-switch behavior remains to be accepted.

## 2026-11-03 Home active-account lifecycle and late navigation (prepared follow-up)

The previous Settings/Home logout fix already guards session-clearing after
bounded presence revocation. Inspecting current production Home onStart found
no Firebase auth listener; Home could remain visible while a different user
signed in and could still apply a stale admin result or navigate an exam from
a completed getLiveGeneratedTests request. An onResume check alone is
insufficient while an Activity stays RESUMED.

This second/final Home account-bound safety increment reuses `homeOwnerUid`
and the existing `searchOwnerCurrent` predicate, installs one Firebase auth
listener at STARTED and removes it at STOPPED, hides stale exams on account
replacement, and checks both admin and generated-exam network callbacks
before publishing UI or navigation. The canonical ViewModel's Continue exam
reads and the existing Home click write now use a UID-keyed recent-exam
preference via `HomeRecentExamStore`; the device-global legacy key is not attributed to a new account. The store has an isolated UID+legacy Robolectric test and a deletion-specific clear operation.
No duplicate Home controller, popup,
session manager or navigation destination; no backend behavior changes.
`HomeAccountLifecycleContractTest.kt` protects source-order and listener
lifetime. This is prepared only pending the Settings/Home fix exact-SHA FAST.

## 2026-11-03 delete-account owner-scoped local data cleanup (prepared)

After the source-derived Mistake Notebook account separation and Home's
`HomeRecentExamStore` partitioning, the original Settings deletion cleanup
did not erase those owner-specific preferences or `HomeSearchHistory`.
The prepared follow-up extends the **existing confirmed-success** deletion
path to clear precisely the deleted UID's learned flags, Continue exam and
submitted search history. Global/legacy ownerless preferences and all other
users remain untouched. A Robolectric fixture exercises two identities plus
the legacy key, and a source-order regression ensures cleanup follows both
server and Firebase credential deletion, not a failed request.
No destructive production test, Worker deployment, additional session owner,
or change to app-wide appearance settings.
Status: prepared only until the Home account-bound commit has exact-SHA FAST
SUCCESS, then this deletion-cleanup feature needs its own exact-SHA FAST.

## 2026-11-03 Bookmarks server-confirmed swipe and Undo (current implementation batch)

Live recovery: remote `main` 0439e46b78475728a51fa43d2376f02ab0cbea13,
FAST 41605741117 SUCCESS and documentation 41605741049 SUCCESS.
Earlier BookmarkRepository eager-Firebase constructor regression was repaired at
8e341ef0a6e617fe48c0f710bcc175aba64b508b (FAST 41600188252 SUCCESS).
Mistake Notebook 424764bd, Performance/Leaderboard 8aeabea5, Settings/Home
9ff424bc and 402b3519, and user-deletion cleanup 0439e46b are already
integrated with their exact-SHA green FAST workflows; never replay them.

New issue in the active BookmarksActivity/BookmarksViewModel path:
the swipe animation collapsed a row and immediately removed it locally, then
launched an unobserved `BookmarkRepository.unbookmarkQuestion`. A rejected
server response or network error left a misleading `Bookmark removed` Undo
while the server still owned the bookmark. Original Undo could race with the
pending delete. The canonical Worker DELETE/POST routes are UID-authorized and
idempotent; no backend modification is needed.

The bounded fix changes only the existing Bookmarks Activity/ViewModel:
await original-account-confirmed repository Result before offering Undo, track
pending IDs to ignore duplicate swipes, rebind/reset the collapsed
`UiArcSwipeRow` on failure, and only show `Bookmark restored` after the
existing POST result. Same owner's return from TestActivity reloads the
repository-backed list; a successful restore reloads too. Existing
`BookmarkAdapter`, `AppUndoBar`, `BookmarkRepository`, Worker routes,
scoring, UI primitives and permission handling remain intact.
Four source-integrated JVM regression assertions are in
`BookmarksConfirmedMutationContractTest.kt`; the preexisting
`BookmarksAccountBoundaryTest.kt` was updated to assert the stronger
combined account/lifecycle guard. No server or device mutations were performed.

Status: IMPLEMENTED/STAGED pending carrying commit and exact-SHA FAST. Full
bookmarks device gesture/failure/rotation testing, broader Telegram parity,
final six-job FULL CI and GPL approval are separately OPEN.

## 2026-11-03 Bookmarks initial read truthfulness (prepared second/final batch)

The previous server-confirmed swipe repair is carried by
b425cb45c4f0c52130b41254d49c5ac51346e0eb; its exact-SHA FAST
41607774069 and documentation 41607774150 are SUCCESS.
This is its second and final accepted implementation increment.

Source review of the same canonical BookmarkRepository found the initial
`observeBookmarkedQuestions` GET swallowed network/permission exceptions and
accepted `success=true, data=null` as cached empty. The existing Bookmarks
ViewModel consequently displayed `No bookmarks yet` when the server had not
actually confirmed an empty page. This batch preserves the existing single
repository/StateFlow/Activity and replaces the silent catch in that read path
with a validated response contract. A signed-in owner is still checked after
network suspension and the existing ViewModel `.catch` presents the error.
Valid empty pages and stable ordering are unchanged. The distinct quiz-side
`observeBookmarkIds` read contract is deliberately not modified.

New JVM tests in `BookmarkInitialReadContractTest.kt` cover genuine empty,
denied, missing payload, missing ID and valid ordering, plus the actual
production flow's owner/error propagation. No Worker endpoint, D1 migration,
new data source or competing UI state owner is introduced. This batch remains
STAGED (not tested/committed) until preceding exact-SHA FAST succeeds.
Device error-state/Undo gestures, source-exact Telegram parity, protected FULL
and human release/legal approval remain separate outstanding requirements.

## 2026-11-03 Bookmarks initial-read assertion repair

Exact-SHA FAST 41608844817 for 91c8c6a0fa0c528ea821cefcc0faea1c3fac8394
FAILED Android JVM step with 1,025 tests / one failure:
`BookmarkRepositoryOwnerContractTest.asynchronousReadsCannotPublishForeignBookmarkRows`.
The source-only assertion hardcoded `val res = api.getBookmarks()`; the updated
production read legitimately renamed that local to `response` while retaining
the post-await `if (!accepts(userId)) return@flow`. The first GET and guard
ordering were independently checked on live source before this repair.

The forward-only correction changes only the test's matching rule to search
for the real `api.getBookmarks()` call, followed by the mandatory owner
guard. This tests the same contract more strongly, regardless of local naming.
No production source, Worker route or assertion logic is removed; earlier
confirmed empty/null/error truth-table tests remain intact. The carrying
repair's own exact-SHA FAST is pending, and the PYQ independent feature
must not advance until that gate is SUCCESS.

# EditExam metadata save contract

Development integration; whole Admin/source/device acceptance remains NOT MET.
Base main 980a92cf4a10e4b16b0a691303b4c340ad30bf6e, exact-SHA FAST
[41535796716](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41535796716)
SUCCESS; documentation 41535796467 SUCCESS. Official upstream remains
3de37c8cfb039452be7398909461da6288babf15; no Telegram code/assets are imported here.

## Requirement and production chain

| ID | Requirement | Current integration | Status / evidence |
|---|---|---|---|
| R1 | No default/foreign snapshot metadata writes | EditExam disables metadata editing until a matching confirmed exam exists | MET scoped host; real Activity pending/missing cases fail before and pass after |
| R2 | Preserve unshown settings and planned count | Sole metadata repository caller sends name/time/category/cutoffs only; Worker updates only provided columns at write time | MET scoped local; repository payload, real Hono/SQLite committed-edit ordering tests; production rollout separate |
| R3 | One pending save, retryable draft, confirmed success, owner/teardown safety | Existing Activity owns pending state; repository verifies GET readback before success | MET scoped host; native callbacks, denied/retry, stale account, destruction and actual offline Retrofit/Gson compatibility tests |
| R4 | Existing Worker time/cutoff validation | 1..600 minutes; finite non-negative cutoff marks for existing General/OBC/SC/ST/EWS fields | MET scoped local; malformed input/denied-role tests; no evaluation rule changes |
| R5 | Same-account/exam draft/category restoration | Existing AdminAccessGate optionally verifies SavedStateRegistry owner/scope; Android remains the text/cursor state owner | MET scoped host; late metadata/new Activity, custom category, foreign UID/exam and legacy unowned rejection |
| R6 | Real cutoff persistence | Additive 0030 migration and existing GET/PUT backing with readback | MET local code/database tests; BLOCKED live until separately authorized production migration/Worker rollout |
| R7 | Native/reference/full acceptance | Existing UI primitives retained | NOT MET; no physical device/reference proof, FULL or published verified APK |

Actual chain: existing EditExam metadata button -> current confirmed Exam ->
ExamRepository.updateExam -> existing EveApiService PUT /api/exams/:id -> Firebase
principal/Worker manage_exams -> validated partial UPDATE + conditional audit +
canonical name cascades in one D1 batch -> existing GET /api/exams/:id -> matching
readback -> success. HTTP success alone cannot confirm ignored cutoff settings on
the old deployed backend. Verification failure retains the draft and tells the
user to reload; it does not claim rollback of an uncertain remote result.

No new screen, state manager, permission, Telegram service or client admin grant.
Scoring, answer secrecy, negative marking rules, attempts and ranks are unchanged.
The SOURCE_OF_TRUTH answer-header description was corrected to actual existing
server authorization; no authorization implementation was relaxed.

## State, storage and compatibility

Metadata controls cannot submit or accept native input before the authoritative
snapshot. Failed/pending saves preserve text; repeat taps cannot enqueue another
save. Same-account/exam recreation uses ordinary Android text restoration, not a
second draft repository. Stable category identity supplements the positional
Spinner. Existing server categories remain selectable. Foreign/legacy saved
owners fail closed before protected reads; other gate callers retain defaults.

Current code revealed that Worker/D1 never stored the existing Android cutoff
inputs. Migration 0030 adds only cutoffs_json TEXT NOT NULL DEFAULT '{}'. Existing
rows/settings are preserved; old clients safely ignore the added response field.
Existing result cutoff presentation is unchanged. The parser validates the existing
five category keys and finite non-negative numeric values; omitted settings stay
unchanged, explicit empty object clears cutoffs. Invalid requests cannot mutate
rows or create audits. No production migration or Worker deployment was executed.

Production rollout requires its separate authorization and the existing protected
manual workflow, with migration before the new Worker. Until then cutoff writes
remain unavailable on the deployed service; client readback prevents false success.
A production deployment is not authorized by a green development FAST.

Direct current-UID/email checks also cover the interval before Firebase delivers
its queued auth-listener notification. Two additional native fail-before cases
proved stale Save and late metadata enablement; one proved late question-save row
rebinding. A retained Delete confirmation control also passes. Existing editor
callbacks check the same origin metadata; cancellation propagates. This does not
resolve the separate unsupported hard-delete Undo product semantics.

The generic metadata PUT and installed rename endpoint now share destination
uniqueness and audit-witness name cascades. Original rename validation, legacy
payload alias and eight regression cases remain. New write-time tests prove missing
records cannot create success/audit, sibling collisions reject all changes, and
omitted hidden settings retain another committed update. This is a necessary second
integration of the previously green rename subsystem (first implementation
196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee / FAST 41469072072), not a new parallel
rename path or cosmetic third iteration. Explicitly provided competing settings
still use the existing API semantics; no optimistic-version protocol is claimed.

## Provenance and verification

| Upstream relationship | EVE integration | Classification / reason |
|---|---|---|
| No Telegram exam/Worker service transplanted | app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt:52,166,198,252; app/src/main/java/com/eve/app/data/repository/ExamRepository.kt:166,175; app/src/main/java/com/eve/app/util/AdminAccessGate.kt:38 | ADAPTED EVE integration; source renderer and business state are not claimed EXACT |
| Existing source dialog policy AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | Existing native dialog/canonical question form, unchanged by this metadata save | ADAPTED existing source mapping; no new import or visual parity claim |
| EVE canonical rename algorithm from 196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee | backend/src/routes/exams.ts:55,68,344 shared by both existing entry points | ADAPTED EVE business integration; original eight tests plus real metadata race/rollback cases |

Fail-before: six actual Activity/repository cases failed (pending/missing defaults,
planned count 100 to 1, duplicate Save, stale UID and hidden payload fields). Cutoff
Worker cases: three failures, ten controls pass. Later atomic metadata cases: four
failures, fourteen controls pass. Failed Python edit attempts used the wrong
working directory and changed no files; their unchanged test reruns are not counted
as new regression evidence.

Expanded Android scope: sixteen native metadata/editor Activity cases and five real
repository cases, alongside retained EditExam/question-review tests. The old-backend
and supported-backend readback cases use actual offline Retrofit/Gson HTTP JSON.
Worker tests execute real Hono handlers with all migrations in isolated SQLite/D1,
including additive migration preservation, role denial, malformed settings,
readback, omitted committed edits, deletion/collision and atomic audit rollback.

Final integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (JDK21/Gradle8.7; 3m2s), 883 tests / 198 suites,
zero failures/errors/skips; lint 0 errors / 1629 warnings. Backend npm test:
318 PASS / zero failures/skips; npm run build TypeScript PASS. Helpers59/docs/
acceptance-claims/four recorded byte-import/diff checks PASS; final docs rechecked before push. Carrying exact-SHA
FAST pending. Physical lifecycle/GPU,
full UI/reference/RTL/font/accessibility/navigation acceptance and qualified GPL
review remain OPEN. Historical bare SDK35 shutdown exception recurred, UNKNOWN P2;
passing JUnit is not proof that this exception or native runtime is resolved.

First independent next action after green: queued EditExam GetContent result is
currently discarded while fresh authorization is pending. Reproduce actual picker
callbacks and retain one same-owner/exam result until authorized context exists.
Import parsing is synchronous/unbounded and is a separate open reliability gap.
Delete/Undo product semantics remain unanswered; do not infer approval. Whole QR,
Home/Profile/Admin/student/theme mission remains partial; QR acceptance stays OPEN.

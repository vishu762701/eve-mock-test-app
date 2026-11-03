# Account-bound attempt cache and result handoff

Base 9fdeca47309ea9da0f228119591c410a70b4e057: FAST
[41573024314](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41573024314)
SUCCESS; docs41573024250 SUCCESS. This newly reproduced P1 confidentiality defect
precedes the independently suspected Home search SavedState-owner gap. It is an
account-isolation repair, with no search renderer iteration or QR change.

| ID | Requirement | Existing implementation / repair | Status / evidence |
|---|---|---|---|
| R1 | No foreign attempt/flag/payload reads | Existing HomeViewModel companion cache now checks live UID, purges transient cache on owner change, rejects stale producer UID and copies synchronized snapshots; existing ResultDataHolder has UID/exam-tagged bounded payloads | MET scoped account-switch/signed-out/native fallback tests; no parallel cache |
| R2 | Correct producer/consumer context | All six result-intent sites and the detail launcher tag immutable source UID. Every answer/detail producer and consumer passes UID/exam context; generated-test target key retained. Missing/foreign internal launch tags fail closed | MET scoped native screen and handoff tests; manifest already keeps both result Activities non-exported |
| R3 | Cancellation, restoration and cleanup | Actual previous-result helper propagates cancellation and rechecks actor; native result/detail screens hide and finish on auth change, dispose listeners, and clear only their own payload. Result recreation uses its existing ViewModel | MET scoped host: actual delayed API continuation, native recreation, late callbacks, own-account controls and listener cleanup |
| R4 | Preserve server evaluation and data | No scoring, question/answer model, Worker/API, attempt ID, deduplication, negative marking or database change; existing authenticated attempts GET/answers JOIN remain scoped by server UID | MET scoped unchanged contract and host regression; existing staged backend CI preserved; no production operation |
| R5 | Whole student/Home/physical acceptance | This closes local attempt/result transport ownership, not every screen's account/lifecycle matrix | NOT MET whole mission; signed-in physical device, all student states, reference visuals/FULL/legal remain OPEN |

Actual fail-before: six of seven initial cases failed, including the real
ExamLaunchHelper fallback launching ResultActivity for account B with account A's
cached attempt. Same-account navigation control PASS. Other failures exposed foreign
submitted/cleared flags and both untagged ResultDataHolder slots. Initial fixture
AnswerItem-constructor compilation was corrected and is not a product reproduction.

The same canonical cache/holder remain. HomeViewModel clears transient owner-specific
attempt/pin presentation when loadForUser changes UID and prevents old UID attempt
responses/pin observers from publishing. The existing immutable Firebase UID is
metadata, not a new permission source. Server-authoritative attempts.ts:1049-1101
filters both attempts and joined answers by authenticated user.uid and returns userId;
local checks do not grant rights or replace that server predicate.

ResultDataHolder preserves one answer payload and one detail payload, without Binder
answer arrays. A mismatched UID/exam cannot consume or clear a newer payload; callers
must pass source identity explicitly and a stale producer cannot relabel old answers
with the new account. Internal result/detail intents now carry an owner tag before
any UI/retained answers are read. Existing UI, original source dialogs and navigation
destinations remain. The existing legacy tracking test was moved intact into the
Robolectric auth suite, with an explicit valid UID; its assertions were preserved.
Source wiring tests now require the scoped signatures rather than unowned calls.

| Upstream path:lines@SHA | EVE path:lines | Classification / reason |
|---|---|---|
| EVE-owned Firebase/Worker attempt contract; no Telegram account subsystem imported | app/src/main/java/com/eve/app/ui/home/HomeViewModel.kt:138; app/src/main/java/com/eve/app/ui/home/ExamLaunchHelper.kt:31; app/src/main/java/com/eve/app/ui/result/ResultDataHolder.kt:7 | ADAPTED EVE integration; new local ownership checks are EVE-authored, no EXACT claim |
| Existing AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/common/CompletedExamBottomSheet.kt:85; app/src/main/java/com/eve/app/ui/result/ResultActivity.kt reattempt callbacks | ADAPTED existing native dialog integration, source renderer unchanged; callbacks check their origin UID |
| Existing EVE result/history/test navigation | app/src/main/java/com/eve/app/ui/history/HistoryActivity.kt:131; app/src/main/java/com/eve/app/ui/test/TestActivity.kt result producers; app/src/main/java/com/eve/app/ui/result/ResultActivity.kt:86; ResultDetailActivity.kt:30 | UID/exam-tagged internal handoff, not Telegram service or UI equivalence |

Eighteen scoped ownership tests cover cache/flags, actual offline fallback, result
and detail rejection, same-account native rendering/recreation, delayed lookup,
cancellation, foreign producers, wrong exam, old-screen cleanup and listener disposal.
Synthetic auth/transport only; no live Worker, real tokens or user data. Original
server responses/graded answers are preserved rather than recomputed. Full app-wide
account switching and physical/native-navigation acceptance remain unverified.

Current integration checks are recorded in the mission checkpoint only after actual
completion. No production deployment/migration, source import, new Android permission,
GPL approval, protected FULL or verified APK publication is performed by this batch.

Scoped ledger/source evidence: ATTEMPT_RESULT_OWNER_ISOLATION.md.

Final local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (2m44s), 948 tests / 202 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Scoped ownership18 cases (including the intact moved
tracking assertions) PASS. Helpers59, docs80, acceptance structure, four recorded
byte imports and git diff --check PASS. One preceding local run's indentation lint
error was repaired; no suppression/baseline or assertion removal. Historical bare
SDK35 thread diagnostic remains UNKNOWN; no blanket crash-free/device claim.
Carrying exact-SHA FAST pending. Backend/Firebase source unchanged from green base.

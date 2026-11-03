# Exam import result lifecycle

Base main 07b448e5e70b53569db6a4ba328bd0baf34edca8: exact-SHA FAST
[41544036446](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41544036446)
SUCCESS, documentation 41544036310 SUCCESS. Official Telegram refetched on recovery,
still 3de37c8cfb039452be7398909461da6288babf15. No new upstream import or revision mix.

| ID | Authorized outcome | Actual implementation | Status |
|---|---|---|---|
| R1 | Result survives pending fresh gate/recreated context | Existing ActivityResultRegistry callback queues one URI; existing AdminAccessGate owner/exam scope and confirmed metadata precede consumption | MET scoped host; actual registered callback and native provider tests |
| R2 | Denied/stale identity, rapid launch, duplicate result/window/import callbacks cannot act | Existing editor identity predicate, pending-picker flag, one read job/generation and one native dialog owner | MET scoped host; role denial, delayed-listener UID replacement, duplicate launch/result/confirmation and cancellation tests |
| R3 | Off-Main read/query/parse, handled provider errors, owned resource cleanup | Dispatchers.IO, CancellationSignal, cursor/descriptor/stream use blocks, cancellable reads and existing Activity lifecycle | MET scoped host; synthetic ContentResolver/provider/PFD APIs; actual physical providers/grants remain unverified |
| R4 | Preserve explicit import and restoration contract | Existing parser and Worker batch method, URI-only saved state, cached parsed preview while stopped; uncertain submit is never automatically replayed | MET scoped host; manual confirmation and interrupted-recreation controls; unknown server acknowledgement/idempotent manual retry remains OPEN |
| R5 | Bounded parser and all supported file semantics | Existing shared QuestionImportHelper unchanged | NOT MET; shared parser bounds/sparse XLSX handling require separate tests/repair |
| R6 | Physical device/source/full/legal acceptance | Whole mission still partial | NOT MET; external blockers tracked in mission ledger; no FULL, verified publication or legal clearance |

Actual path: original EditExam Bulk Upload button -> existing GetContent launcher ->
registered ActivityResultRegistry callback -> same editor's queued URI -> fresh
manage_exams/manage_questions + matching metadata/owner scope -> background provider
query/read -> existing QuestionImportHelper -> existing TelegramNativeAlertDialog
preview -> explicit existing ExamRepository.addQuestions/Worker manage_questions.
No new screen, repository, state manager, permission, persistent grant, automatic
mutation/replay or production API was introduced. Derived parsed data stays in the
existing Activity; saved state contains URI/operation markers, not question arrays.

One existing Add-dialog field now owns all four actual native question/import
modals. Entry guards prevent parallel windows; dismissal and destruction dispose
that same owner. All original titles, inputs, mutation callbacks, Cancel actions
and destructive Delete styling remain. Source-contract tests now trace four callers
to one show method, instead of requiring four duplicated show sites. Native host,
source skin, dismissal behavior and business mutations are retained.

| Source relationship | EVE path:lines | Classification / reason |
|---|---|---|
| EVE file/result/permission contract; no Telegram exam-import service transplanted | app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt:96,237,383,403,579 | ADAPTED EVE integration; existing registry/editor owner, bounded operation concurrency and lifecycle; not a Telegram source-parity claim |
| Existing AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | EditExamActivity.kt:459,517,554,738,821 -> existing TelegramNativeAlertDialog | ADAPTED existing source-derived renderer; current Activity owns its four windows; no new upstream code/assets |
| Host PFD verification | app/build.gradle.kts host Test task JVM arguments | EVE test-toolchain adaptation only; java.io reflection opening required by Robolectric on JDK21, never an APK permission or gate suppression |

Three initial actual callback/provider cases failed: recreated pending-gate result
was dropped; query SecurityException escaped; provider read ran on Main. A later
rapid-tap test proved two external picker launches. Descriptor diagnostic isolated
JDK21 InaccessibleObjectException in Robolectric's real FileDescriptor close;
--add-opens=java.base/java.io=ALL-UNNAMED is now limited to host test JVMs.
Null-provider and dismissed-window-pointer fixture issues were corrected without
removing the no-read/no-preview/no-mutation assertions.

Sixteen production-path picker/provider tests PASS alongside retained metadata,
action and dialog tests. They cover fresh-role denial, stale UID before listener,
rapid/cancel/duplicate delivery, preview recreation, null/throwing provider, stopped
read reuse, destroyed-host descriptor closure, explicit confirmation, duplicate
retained buttons and interrupted-submit no-replay. Provider/auth/repository fixtures
are synthetic test boundaries; no production write or physical-grant proof.

Final Android integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (4m23s), 899 tests / 199 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Helpers59, documentation, four recorded byte imports,
acceptance claim structure and git diff --check PASS. Carrying exact-SHA FAST pending. Backend source unchanged from green main (318 tests in
that gate). GPL gate, protected FULL and publication controls remain unchanged.
Historical bare SDK35 shutdown exception remains UNKNOWN; this specific descriptor
reflection diagnostic is fixed, not a blanket crash-free claim.

First next implementation after carrying FAST: reproduce shared parser limits and
sparse XLSX column mapping using existing production callers (EditExam and
ManageQuestions). Worker already limits a batch to 300 questions; do not silently
truncate or replay oversized/uncertain imports. Parser safety is the remaining
import reliability increment, not permission for repeated cosmetic iterations.
Whole Profile/Admin/Home/student/theme/QR acceptance stays OPEN/NOT MET. Metadata
cutoff production migration/Worker rollout and Delete/Undo product choice remain
separately pending; no approval inferred from a generic resume message.

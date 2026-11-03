# Standalone question deletion: truthful confirmation

Recovery base d638d3b20a4c10e6462b6d95adb8f35fada3a584 has exact-SHA FAST
[41571297755](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41571297755)
SUCCESS and docs41571297931 SUCCESS. The complete mission remains NOT MET.

| ID | Requirement | Existing implementation / repair | Status / evidence |
|---|---|---|---|
| R1 | Success only after server confirmation | Real ManageQuestions row -> native Delete -> existing AdminViewModel -> ExamRepository -> EveApiService; success callback follows repository confirmation | MET scoped host pending/confirmed callback tests; no optimistic removal or fabricated success |
| R2 | Truthful failure | Existing ViewModel passes normalized error to current native Activity bulletin; malformed/missing success, denial and cancellation covered | MET scoped Activity plus real repository/API-response boundary tests; failure never calls success, cancellation propagates and releases busy |
| R3 | Duplicate/stale callbacks | Existing ViewModel busy guard; current UID/email, host and selected exam/question checked before destructive callback; late identity notices suppressed | MET scoped rapid taps, stale bound row, queued identity change, finishing host, late response and role denial tests |
| R4 | Preserve source UI and authority | Same TelegramNativeAlertDialog, destructive button, Cancel and dismissal; existing Add dialog owner now also owns Delete window and disposes it on destruction; localized English/Hindi text | MET scoped native controls; Hindi/RTL/Night/150% font measured 48dp target. Physical TalkBack/window/device matrix remains OPEN |
| R5 | Server authorization/data integrity | Existing Worker DELETE questions.ts:337-358 retains requirePermission(manage_questions), conditional audit, batch confirmation and idempotent absence | MET existing staged server contract and green base CI; unchanged in this batch. No production mutation/deployment performed |
| R6 | Whole Admin/source/device acceptance | Only reproduced standalone deletion defects repaired | NOT MET whole dashboard; standalone picker lifecycle, other modules and source/reference matrix remain in mission ledger |

The similarly named private AdminActivity.confirmDeleteQuestion has no caller in
current source. It is not claimed as an active duplicate and was not removed.
Existing AdminDeleteFlow serves other dashboard deletion protocols; this path keeps
its established AdminViewModel/request/refresh owner rather than adding another
repository or mutation manager. No new dialog renderer or screen.

| Upstream file:lines@SHA | EVE file:lines | Classification / reason |
|---|---|---|
| AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt:224,235,244 -> ui/admin/ManageQuestionsActivity.kt:248 | ADAPTED existing source renderer, unchanged; EVE retains 48dp accessible controls and current destructive color token |
| EVE question/permission/API contract; no Telegram exam administration equivalent transplanted | ui/admin/AdminViewModel.kt:226 -> data/repository/ExamRepository.kt:278 -> data/remote/EveApiService.kt:304 -> backend/src/routes/questions.ts:337 | EVE-authored business integration, no EXACT or Telegram backend claim |

Actual fail-before: three native callback cases failed (premature success, denial
masked by success, two writes while pending), with confirmed/cancel controls PASS.
Fixture compilation method-name and unsupported font qualifier were corrected and
are not counted as reproduced product defects. TextView single-line mode switches
the minimum to lines; touch-target verification measures actual native window/button
bounds rather than incorrectly comparing getMinHeight() to pixels.

Eleven actual standalone Activity cases and four added ViewModel/repository deletion
cases cover confirmation, cancellation, malformed missing-success JSON, role denial,
queued identity changes, stale row and rapid action. Synthetic auth/transport boundaries
only; no production credentials, Firebase tokens or live writes. Scoring, attempts,
answer secrecy, batch import, question data and Worker API are unchanged.

This preserves existing immediate permanent-delete semantics. The separate owner
choice about EditExam Delete/Undo timing remains unanswered; no deferred delete or
restoration endpoint was silently introduced. GPL approval, protected FULL, APK
publication, native navigation and device/reference acceptance stay OPEN.

C5 scoped implementation/evidence: MANAGE_QUESTION_DELETE_CONFIRMATION.md.

Local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (3m57s), 931 tests / 201 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Helpers59, docs79, acceptance structure, four recorded
byte-identical imports and git diff --check PASS. Backend unchanged from green base.
Bare SDK35 thread diagnostic recurred with zero JUnit failures; remains UNKNOWN.
Carrying exact-SHA FAST pending. Native navigation, physical device/reference,
protected FULL, verified artifact publication and human GPL clearance remain OPEN.

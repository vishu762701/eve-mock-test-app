# Shared question import: bounded document contract

Recovery base: 22fd6f8129e26bef75c7a26cc609a6525c1684e2; exact-SHA FAST
[41568018939](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41568018939)
SUCCESS. This is the second import reliability increment, following editor handoff.
The whole mission and QR acceptance remain OPEN.

| ID | Requirement | Existing implementation / change | Status / evidence |
|---|---|---|---|
| R1 | Match existing Worker batch maximum without truncation | QuestionImportHelper rejects the 301st valid question, clears the entire offer; Worker questions.ts:222 remains authoritative | MET scoped parser tests: 300 accepted, 301 rejected; actual editor preview has no Import action |
| R2 | Preserve XLSX field identity | Existing parser now honors sparse cell references, inline strings and rich shared-string items; duplicate references fail closed | MET scoped real ZIP/XML tests; Hindi, namespace prefixes, CDATA, predefined entities covered |
| R3 | Bound hostile/unsupported document shapes | Raw 8MiB, total expanded ZIP 16MiB (including ignored entries), 10000 rows, 64 columns, 65536 UTF-16 characters/cell, 80000 shared strings; DTD and malformed input rejected | MET scoped limit/cancellation tests; budgets are EVE engineering choices, never Telegram values |
| R4 | Preserve CSV and both production integrations | Same helper called by EditExamActivity and ManageQuestionsActivity; localized English/Hindi fatal reason, existing row validation and manual confirmation retained | MET scoped helper/editor regression; standalone ManageQuestions picker lifecycle remains separately NOT MET |
| R5 | Stream ownership and security | Decoder closes its inflater without closing caller stream; cancellation propagates; no external entity/network resolver, new permission, backend/scoring changes or automatic writes | MET scoped ownership/DTD/cancellation tests; original caller owns stream closure |
| R6 | Physical provider and whole-mission acceptance | Existing first-sheet-only contract retained; no workbook relationship/style/formula engine added | NOT MET device acceptance; broader Excel support is not claimed or added to scope |

Production paths: existing EditExam GetContent callback -> background provider read ->
QuestionImportHelper.parseQuestions -> native preview -> explicit repository batch;
existing ManageQuestions picker -> same helper -> native preview -> explicit batch.
No parallel parser, screen, repository or state manager. The standalone caller's
Main-thread file query, launch-target/restoration handling and stale preview windows
are NOT repaired by this parser change and remain independently actionable.

| Upstream property | EVE path:lines | Classification / reason |
|---|---|---|
| EVE CSV/XLSX exam-import contract; no Telegram business subsystem imported | app/src/main/java/com/eve/app/util/QuestionImportHelper.kt:12 | EVE-authored parser repair; no EXACT/source-parity claim |
| AlertDialog.java:1526-1528@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/designsystem/TelegramNativeAlertDialog.kt:66 -> existing editor/ManageQuestions preview | ADAPTED existing source-derived dialog, unchanged renderer; localized fatal reason only |

Authenticated official origin was rechecked at the same full SHA. No upstream file,
asset or new dependency imported. Existing notices and fail-closed GPL release gate
are unchanged; legal release clearance remains BLOCKED on human review.

Fail-before evidence: five initial production-helper tests failed for sparse cells,
inline text, rich shared strings, oversized valid batch and swallowed cancellation.
Expanded document tests additionally exercise every budget, compressed ignored-entry
draining, DTD rejection, duplicate references, multiline quoted CSV, exact batch
boundary, Hindi text and caller-owned stream closure. The real ActivityResultRegistry,
provider, parser and native preview test rejects a 301-question file under Hindi/
RTL/Night qualifiers without a repository write. Host tests do not establish physical
provider grants, GPU rendering, TalkBack or matched Telegram captures.

Full integration results and carrying commit/FAST receipt belong in the mission
checkpoint once actually observed. Backend is unchanged from the green base;
production migration/deployment, protected FULL, signing and publication are untouched.

Local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (3m21s); 916 tests / 200 suites, zero failures/errors/skips.
Lint 0 errors / 1629 warnings. Document parser16 and actual picker17 cases PASS;
59 CI-helper tests, docs78, acceptance structure, four byte-identical imports and
git diff --check PASS. Backend unchanged from green main. Historical bare SDK35
thread diagnostic recurred without a JUnit failure; remains UNKNOWN, not resolved.
Carrying exact-SHA FAST pending; native navigation, physical providers, matched
visual captures, protected FULL, verified publication and legal clearance OPEN.

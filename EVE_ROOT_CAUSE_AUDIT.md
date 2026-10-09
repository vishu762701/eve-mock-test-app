# Eve root-cause audit — 2026-10-09

Baseline repository: https://github.com/vishu762701/eve-mock-test-app, main,
e1a3a79814b29097bfdde7b78ded4e46d8901038. Clean working tree, fetched origin/main
and confirmed matching HEAD before changes. Root AGENTS.md read. No latest ZIP
or new screenshot attachment available: ZIP/revision comparison remains blocked.

## Baseline evidence
- Android assembleDebug, testDebugUnitTest, lintDebug, assembleDebugAndroidTest:
  BUILD SUCCESSFUL in 2m49s (cached unit-test task; fresh execution required below).
- Backend existing 114 tests: all passed. Initial baseline typecheck output was
  not collected separately; final fail-fast checking is recorded in the verification report.
- Optional Firebase Functions: 16 tests passed. Firebase credentials unavailable.
- Local machine has no /dev/kvm. Native runtime evidence will use emulator CI;
  no local Logcat or newly captured emulator screenshot exists at baseline.

## Verified root causes (source and isolated handler reproductions)
| Symptom | Evidence | Cause |
|---|---|---|
| Exam disappears despite HTTP409 | ManageExistingExamsActivity.confirmDeleteExam | Optimistic list removal and unconditional success bulletin, exception only reports raw message |
| Undo changes exam identity | Same method | Calls addExam, creating a new ID without relationships |
| Delete shows raw HTTP409 | Delete callers use exception.message/localizedMessage | Retrofit HTTP error body bypasses existing ApiError parser |
| No pre-delete dependency explanation | Real Hono /generated-tests/test/deletion-info returns404 in isolated SQLite test | Endpoint absent; protected dependencies only checked at deletion |
| Checkmark on dark unselected option | TelegramRadioButton.setChecked/onDraw and QuestionAdapter.bind | Check progress animates separately from immediately switched drawable, with no recycling reset |
| Stale answers on new server attempt | TestViewModel.start | Local restore checks user/exam but never compares saved UUID with confirmed server session UUID |
| Insets lose bottom/horizontal padding | SystemBarHelper.setupInsetsListener | setPadding(0,statusTop,0,0), repeated listener installation, no navigation inset |
| Theme reveal mixes system bar themes | ThemeSwitchAnimator/SystemBarHelper | Captured old decor overlay remains while icons switch to target; colors resolved from old resource configuration; manual recreate also follows delegate recreation |
| Result labels/analytics cramped | activity_result.xml | Four 76dp tiles with 9sp minimum autosizing; gauge occupies92dp plus14dp beside three weighted mini-cards |
| Legacy reattempt loses history | attempts.ts POST/reset and DELETE/exam | Deletes attempts/answers and decrements stats for legacy lock; user reattempt UI also starts on offline reset failure |

HTTP409 for saved attempts, sessions and sub-exams is intentional protection.
Elapsed and paused sessions are not proven disposable: no new purge policy is
introduced. Generation/submission code-level tests passing does not establish
live authenticated provider or student behavior.

## Architecture and operation map
Android XML/Activity -> Kotlin ViewModel/repository -> EveApiService Retrofit ->
ApiClient Firebase JWT -> Worker Firebase JWT verification + requireAdmin -> D1.
Generated tests use Worker Gemini server-side integration, validated questions,
generation lease/job and atomic publication. Test start freezes grading data in
attempt_sessions; submission scores server-side, confirms an idempotent UUID,
and atomically writes attempts/answers/leaderboards/stats. Android retains failed
submissions in an owner-scoped queue. Results/history query confirmed attempts.
Firebase Auth is active; optional legacy Functions/Firestore differ from the
Worker path and cannot be verified live without configured access. Media uses
Supabase storage. CI builds Android, runs API35 instrumentation, TypeScript and
backend/Functions tests; Worker deployment workflow applies forward migrations.

## Deletion audit boundary
Exam/test protections above must remain. Other existing routes: questions,
syllabus, banners, broadcasts, polls/votes and feedback are admin guarded;
bookmarks are user scoped. Poll/feedback children use cascade/batch. Student
history is never removed by this repair. Storage deletion is separate from D1,
so production storage/provider failure cannot be inferred from SQLite tests.

Detailed final results, screenshots, limitations and changed files are recorded
in EVE_FINAL_REPAIR_VERIFICATION.md as verification completes.

Additional verified media/banner bugs: isolated real syllabus DELETE with an
injected D1 UPDATE failure returned500 after calling storage DELETE once (expected
zero). Banner handler uses the same unsafe ordering. AdminActivity ignores the
failure returned by HomeBannerRepository.deleteBanner because runCatching wraps
it in Result; its try/catch does not see that exception. Storage cleanup now
follows confirmed D1 mutation, reports incomplete cleanup and stores a safe
bounded diagnostic event. Banner/syllabus UI uses honest guarded confirmation
and unwraps Result. Clear/answer taps previously waited for the five-second
periodic session save; they now save immediately, preserving clear state.

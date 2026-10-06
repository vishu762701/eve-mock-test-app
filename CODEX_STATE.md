# EVE — CODEX STATE

## Current Project State

- Eve is a native Android app in the Gradle `:app` module, implemented in Kotlin with XML layouts/resources. It uses Firebase authentication and a Retrofit/OkHttp client for the Cloudflare Worker API. The root `backend/` is a TypeScript/Hono Worker with D1 migrations; `functions/` separately contains Firebase Cloud Functions.
- The iOS-inspired visual refresh updates shared light/dark tokens, cards, controls, motion helpers, Login, and Home. Login retains the existing email sign-in/sign-up and Google authentication code paths.
- Home's hero is hidden and empty without enabled content. Admins edit it through the existing content editor; published content is served by the Worker from the D1 `app_content` table.
- Android attempt submission remains `HistoryRepository` → `EveApiService` `POST /api/attempts/submit` → `backend/src/routes/attempts.ts`. Firestore rules deny direct client creation of `attempts` documents.

## Last Completed Task

Implemented the requested full iOS-style redesign, rebuilt Login, and added the admin-managed Home hero. Implementation is in commit `eac464b` (`feat: rebuild iOS-style app experience`). Local build, unit, backend, and migration checks passed; device-level visual/functional checks remain outstanding because no usable emulator or device was available.

## Last Verified Changes

- Added Home hero fields and an additive D1 migration, public redaction for disabled content, authenticated admin draft reads/writes, validation, and tests for the API paths.
- Added the Home hero editor and app rendering with only the supported Practice, PYQ, and Browse Exams actions.
- Rebuilt the Login screen while retaining the existing Firebase email and Google sign-in code paths, validation, loading/error handling, session bypass, analytics, and Crashlytics identification in source.
- Updated shared light/dark resources, card and input shapes, category controls, and reduced-motion-aware touch/motion helpers.
- Reviewed the source diff and confirmed the changed files are limited to the redesign, related tests, and Home hero backend/API work.

## Current In-Progress Task

NONE

## What Remains

- Run the requested visual and functional smoke checks on an Android emulator/device, including light/dark mode, Login success/failure and Google sign-in where credentials permit, Home with empty and published hero content, exam cards, Test, Result, Admin, and a dialog/sheet.
- Reconcile `README.md` with the current architecture: it says there is no custom backend and describes attempt submission through a callable Cloud Function, while the Android app calls the Cloudflare Worker. The README also describes durable retries as future work; `HistoryRepository` currently retries in memory up to three times.

## Known Issues / Blockers

- No verified application defect was found during this task's checks.
- Runtime UI verification is blocked in this environment: `adb devices` cannot start because its executable cannot link `_ZNSt6__ndk113__hash_memoryEPKvm`, and no emulator executable is available.
- The README architecture/retry discrepancies listed under What Remains are verified documentation inconsistencies.

## Verification Status

- `./gradlew testDebugUnitTest assembleDebug`: passed; 194 tests, 0 failures, 0 errors. Android resource/XML processing and Kotlin compilation completed as part of the build.
- Backend `npm test`: passed, 68 tests. `npm run build`: passed.
- Applied all ten backend migrations in order to an in-memory SQLite database and confirmed the Home hero columns exist. Backend tests cover public empty/disabled reads, admin read/write authorization, sanitization, and invalid CTA input.
- Static checks confirmed the existing email/Google auth and analytics/reporting source paths remain, stale old Login references and hardcoded Home hero copy are absent, and raw/assets containing Lottie content were not changed.
- `git diff --check`: passed. Both light and dark resource variants were inspected.
- GitHub Actions workflow `Deploy Eve Worker & Apply D1 Migrations` completed successfully for the pushed redesign; its D1 migration, Worker deployment, and live production smoke-test steps passed ([run 37424680290](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37424680290)).
- No emulator/device launch or screenshot review was possible. A green build and passing tests are not evidence of runtime Android UI correctness.

## Last Commit

`eac464b` — `feat: rebuild iOS-style app experience` (implementation commit). This state/progress refresh is committed separately as a documentation-only follow-up.

## Next Recommended Action

Run the pending emulator/device smoke checks when a working Android runtime is available. Until then, the local implementation and API checks are complete, with runtime UI behavior explicitly unverified.

## Important Project Decisions

- The GitHub repository is the project's source of truth. Preserve existing app behavior when changing presentation.
- Home hero content belongs in the existing Worker/D1 `app_content` store. Public reads redact disabled drafts; admin draft reads and writes require admin authorization. CTA actions are limited to `open_practice`, `open_pyq`, and `browse_exams`.
- Keep both light and dark resource variants, use blue semantically, and respect the system reduced-motion setting.
- Android attempt submission is server-side through the Worker endpoint; Firestore rules prohibit direct client creation of attempt records.
- Keep `Constants.ADMIN_EMAILS` synchronized with `isHardcodedAdmin()` in `firestore.rules`, as required by `GEMINI.md`.
- Leave `functions/package-lock.json` untouched and untracked; it is ignored by `.gitignore`.
- Existing Lottie assets and playback behavior were not changed by this redesign.

## Session Continuity Rules

1. The GitHub repository is the single source of truth.
2. At the beginning of every new Codex session, synchronize with the latest main branch before starting new work, while never destroying uncommitted user work.
3. Read CODEX_STATE.md, PROGRESS.md, GEMINI.md, and relevant project documentation before continuing development.
4. Inspect the actual implementation before making claims about bugs, completed work, or feature behavior.
5. Never rely on previous chat history when the repository can provide the answer.
6. After every meaningful completed task, update CODEX_STATE.md with the new verified state.
7. Keep CODEX_STATE.md factual and concise; never record guesses.
8. Before committing, review the actual diff and ensure unrelated work was not changed.
9. After a genuinely completed and verified task, commit and push the work to the same GitHub repository.
10. A green Gradle build or unit-test pass alone is not sufficient proof that a UI or feature works.
11. If something could not be functionally verified, explicitly say so instead of claiming it works.
12. Never use destructive Git commands to discard or overwrite user work unless explicitly instructed.

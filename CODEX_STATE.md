# EVE — CODEX STATE

## Current Project State

- The Android project has one Gradle module, `:app`, with Kotlin sources under `app/src/main/java/com/eve/app` and XML layouts/resources. The application ID is `com.eve.app`.
- The app includes Firebase integrations and a Retrofit/OkHttp client for a Cloudflare Worker API. The root `backend/` directory contains a TypeScript/Hono Worker, D1 migrations, and Supabase configuration. `functions/` separately contains Firebase Cloud Functions.
- The current Android attempt-submission path is `HistoryRepository` → `EveApiService` `POST /api/attempts/submit` → `backend/src/routes/attempts.ts`. `firestore.rules` denies direct client creation of `attempts` documents. Do not infer that the similarly named callable in `functions/index.js` is the Android client's current path.
- The latest application commit at setup was `f99cbe95f646bdbc40aaa278cc32540d885c94e4`, on `main`, tracking `origin/main`, with a clean working tree. This state record is being added as a separate documentation-only commit.
- `MASTER_TASK_APPLE.md` says its Apple Liquid Glass redesign task is complete, and the latest application commit has the matching completion subject. The older `PROGRESS.md` checklist also marks its listed polish tasks complete. No clearly active task is recorded.

## Last Completed Task

The latest meaningful completed work recorded by the repository is the Apple iOS Liquid Glass visual and motion redesign. `MASTER_TASK_APPLE.md` identifies the task as complete, and commit `f99cbe95f646bdbc40aaa278cc32540d885c94e4` is titled `feat(ui): complete Apple iOS Liquid Glass visual and motion redesign`.

## Last Verified Changes

- The latest commit changes 71 files, including light/dark color resources, dimensions and typography, transition animations, many vector icons, and the shared `EveBlurHelper`, `EveMotionHelper`, and `EveTouchHelper` utilities. It also updates `LoginActivity`, `MainActivity`, and `TelegramRadioButton`.
- Current source confirms that the Android client submits attempts through the Worker API and that Firestore rules block direct client attempt creation.
- These are repository and Git inspections. They do not establish that the latest visual redesign was functionally or visually verified on a device.

## Current In-Progress Task

NONE

## What Remains

- No unfinished application task is clearly active in the task/progress records inspected.
- Reconcile `README.md` with the current architecture: its introduction says there is no custom backend, while the repository contains and the app calls the Cloudflare Worker backend. Its Phase 23 description says attempt submissions use a callable Cloud Function; the current Android client instead calls the Worker endpoint.
- `README.md` describes durable retries for failed offline submissions as a future improvement. The current `HistoryRepository` retries at most three times in an in-memory coroutine; there is no persistent retry queue in that repository.

## Known Issues / Blockers

- `README.md` contains the architecture and attempt-submission discrepancies listed above.
- `AUDIT_PROGRESS.md` is stale: it reports `beaba41` as current and says the branch is four commits ahead of `origin/main`; the inspected setup baseline was `f99cbe95f646bdbc40aaa278cc32540d885c94e4` with `main` tracking `origin/main` and a clean working tree.
- The inspected records do not include per-item runtime or screenshot verification for the latest Apple Liquid Glass redesign. Its master task and commit mark it complete, but visual/functional verification for that redesign is not established here.
- No external deployment status for Firebase or Cloudflare services can be determined from this checkout.

## Verification Status

- `PROGRESS.md` records 191/191 `testDebugUnitTest` tests and a successful `assembleDebug` for its earlier UI/UX polish work. These are historical project-record claims, not tests rerun for the latest redesign or this setup.
- `AUDIT_PROGRESS.md` records build, test, lint, TypeScript, and release-build results for an older audit ending at `beaba41`; its commit/status information is stale relative to the inspected baseline.
- The latest Apple redesign commit and changed files were inspected. No Gradle build, unit tests, backend tests, emulator run, device run, or screenshot review was performed for this continuity setup.
- Only repository inspection and the Git diff checks for this new state document are verification for this setup.

## Last Commit

Latest application/source baseline at state-file creation: `f99cbe95f646bdbc40aaa278cc32540d885c94e4` — `feat(ui): complete Apple iOS Liquid Glass visual and motion redesign`. The following continuity commit adds this record only; its resulting hash is reported in the setup completion message.

## Next Recommended Action

The project is ready for the next user-requested task. A future documentation task can reconcile the README with the current backend and attempt-submission implementation.

## Important Project Decisions

- Preserve the Android app's existing feature and business logic when doing visual-only work. The Apple redesign specifies retaining the day/night circular-reveal theme behavior and protected Lottie content/playback.
- Keep semantic resource token names where practical and maintain both light and dark color resources. The current design uses the Apple Liquid Glass visual system documented in `MASTER_TASK_APPLE.md`.
- The Worker API is part of the current application architecture. Attempt grading/submission is server-side through the Worker route used by the Android client; Firestore rules prohibit direct client creation of attempt records.
- Keep `Constants.ADMIN_EMAILS` synchronized with `isHardcodedAdmin()` in `firestore.rules`, as required by `GEMINI.md`.
- Leave `functions/package-lock.json` untouched and untracked; it is ignored by `.gitignore`.

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

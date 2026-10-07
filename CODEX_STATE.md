# EVE — CODEX STATE

## Current Project State

- Eve is a native Android app in the Gradle `:app` module, implemented in Kotlin with XML layouts/resources. Firebase provides authentication; the client calls the TypeScript/Hono Cloudflare Worker in `backend/`, which stores relational data in D1 and media in Supabase Storage. `functions/` contains separate Firebase Cloud Functions.
- Home has one admin-managed banner surface in the former large hero location. Active banners rotate/swipe inside that surface; images are clipped to its rounded bounds. Each banner can have an optional HTTP(S) URL and CTA label.
- Admin has one Home Banner manager for image preview/upload, link editing, reorder, and delete. The separate Home Hero editor and its runtime code have been removed.
- The Test screen applies the Thin Material capsule treatment only to answer options A–D and its four bottom actions. Answer selection is green. Other app controls retain their existing styles.
- Light app background tokens are pure `#FFFFFF`; dark background tokens are pure `#000000`.
- Test submission remains server-side through `POST /api/attempts/submit`; authentication, scoring, answer persistence, and navigation code were not changed by the Home Banner/Test appearance task.

### Last Completed Task

Complete EVE UI Studio Premium Visual App Builder Rebuild (Parts 1–5):
- Responsive Device Preview Canvas: Added authentic phone chrome (09:41 status bar, punch hole, 5G/battery, bottom gesture indicator), 50/50 weight-balanced split mode eliminating nested scroll conflict, and dynamic `FIT TO SCREEN` viewport calculation.
- Authentic Screen Fidelity across 8 screens: Home, Test, Result, Profile, Notifications, Syllabus, Login, Admin. Replaced generic cards with type-specific EVE renderers.
- Interactive Test Screen Capsules: Upgraded `buildOptionItemPreview` to render 4 answer options (A, B [Selected - Green #16A34A], C, D) with instant tactile touch feedback and selection switching in interact mode.
- 13 Formal Typed Runtime Adapters in `UiStudioEngine`: TextAdapter, ButtonAdapter, TimerAdapter, QuestionOptionAdapter, TestActionAdapter, ResultStatAdapter, NavigationAdapter, MaterialSurfaceAdapter, ImageAdapter, IconAdapter, SwitchAdapter, SliderAdapter, RowAdapter.
- Fixed Save & Publish Semantics: Enforced truthful failure handling on draft network errors, strict version equality matching on publish verification (`liveConfig.version == expectedVersion`), and atomic Cloudflare D1 batch transactions (`db.batch`).
- 100% Protection Maintained: Telegram-style circular reveal Day/Night transition, test timer countdown/auto-submit, and exam authentication remain completely untouched.

## Last Verified Changes

- Android unit tests: passed (226 unit tests completed, 0 failures via `./gradlew testDebugUnitTest`).
- Android APK build: passed (clean APK packaging via `./gradlew assembleDebug`).
- Backend unit tests: passed (80/80 tests passing via `npm test`).
- Backend typecheck: passed (`npm run build` tsc --noEmit).
- Git diff hygiene: verified zero whitespace or formatting errors (`git diff --check`).

## Current In-Progress Task

NONE. Implementation, verification, and test execution are complete.

## What Remains

- Physical device visual inspection on a physical Android handset with active touch display.

## Known Issues / Blockers

- Headless CLI environment without connected physical Android hardware or adb daemon.

## Verification Status

- `./gradlew testDebugUnitTest`: passed (226 tests completed, 0 failures).
- `./gradlew assembleDebug`: passed (clean APK packaging in 1m 8s).
- Backend `npm test`: passed (80/80 tests passing).
- Backend `npm run build`: passed (clean TypeScript compilation).
- Runtime adapters, 8-screen default templates, session persistence, and publish verification verified via unit tests.

## Last Commit

`f744463` — `feat(ui-studio): rebuild UI Studio with session persistence, tabbed inspector, truthful save, and verified publish`.

## Next Recommended Action

Run the pending Android device smoke checks when a working emulator/device is available.

## Important Project Decisions

- The GitHub repository is the project's single source of truth. Preserve existing auth, exam, test scoring, answer persistence, and navigation behavior when changing presentation.
- Store banner URL and label alongside the image metadata in `home_banners`. Migration `0011` is additive and backward-compatible; keep older migration history unchanged.
- Support multiple active banners inside the one Home surface. Do not restore a second banner card or old dot indicator UI.
- Only render a banner CTA for a valid HTTP(S) URL with a usable stored label. Clearing the URL also clears the label and removes the CTA on the next Home banner fetch.
- Keep the Test Thin Material treatment scoped to answer options and the four bottom actions. Selected answers use the existing system green semantic token; Admin answer editors retain their existing style.
- Keep light app backgrounds `#FFFFFF` and dark app backgrounds `#000000`. Use the existing BlurView stack for matte translucent material; do not add glossy, gradient, or 3D effects.
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

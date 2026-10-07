# EVE — CODEX STATE

## Current Project State

- Eve is a native Android app in the Gradle `:app` module, implemented in Kotlin with XML layouts/resources. Firebase provides authentication; the client calls the TypeScript/Hono Cloudflare Worker in `backend/`, which stores relational data in D1 and media in Supabase Storage. `functions/` contains separate Firebase Cloud Functions.
- Home has one admin-managed banner surface in the former large hero location. Active banners rotate/swipe inside that surface; images are clipped to its rounded bounds. Each banner can have an optional HTTP(S) URL and CTA label.
- Admin has one Home Banner manager for image preview/upload, link editing, reorder, and delete. The separate Home Hero editor and its runtime code have been removed.
- The Test screen applies the Thin Material capsule treatment only to answer options A–D and its four bottom actions. Answer selection is green. Other app controls retain their existing styles.
- Light app background tokens are pure `#FFFFFF`; dark background tokens are pure `#000000`.
- Test submission remains server-side through `POST /api/attempts/submit`; authentication, scoring, answer persistence, and navigation code were not changed by the Home Banner/Test appearance task.

### Last Completed Task

Rebuilt EVE UI Studio into a full visual App Builder / UI Editor system:
- Dedicated top-level Admin Dashboard tab item for UI Studio in `activity_admin.xml` and `AdminActivity.kt`.
- Comprehensive screen registry ([UiStudioRegistry.kt](file:///data/data/com.termux/files/home/eve-mock-test-app/app/src/main/java/com/eve/app/uistudio/UiStudioRegistry.kt)) covering 8 application screens and 16 component primitives.
- Interactive canvas with tap-to-select element highlighting and instant inspector synchronization.
- Component tree hierarchy with search filter, add, duplicate, delete with core component protection, and reorder.
- Full property inspector with appearance, layout (width/height dimensions), visible material glass blur slider (0-35px), material opacity, tint, typography, content, actions, animations, and screen transition presets.
- 30-step Undo/Redo configuration snapshot history stack.
- Advanced Mode raw JSON editor with syntax validation and live application.
- Persistence bug fix via local persistent draft caching (`KEY_CACHED_DRAFT`) ensuring zero loss of draft configurations on reopen or recreation.
- Runtime application engine ([UiStudioEngine.kt](file:///data/data/com.termux/files/home/eve-mock-test-app/app/src/main/java/com/eve/app/util/UiStudioEngine.kt)) applying dimensions, blur tints, typography, and entrance animations safely without touching core exam business logic or theme toggles.

## Last Verified Changes

- Dedicated top-level tab in `AdminActivity` and `activity_admin.xml` functioning cleanly with tab index management.
- Backend validator in `uiStudio.ts` enforcing blur radius (0-50), material opacity, tint, and animation parameters.
- All 79 backend tests passing (`npm test`).
- All 207 Android unit tests passing (`./gradlew testDebugUnitTest`).
- Clean debug APK build via `./gradlew assembleDebug`.
- Verified Telegram circular reveal theme animation and core test scoring logic remain 100% untouched.

## Current In-Progress Task

NONE. Implementation, verification, and test execution are complete.

## What Remains

- Physical device visual inspection of hardware-accelerated blur effects across diverse Android OS versions (API 31+ vs fallback).

## Known Issues / Blockers

- Headless CLI environment without connected physical Android hardware or adb daemon.

## Verification Status

- `./gradlew testDebugUnitTest`: passed (207 tests completed, 0 failures).
- `./gradlew assembleDebug`: passed (clean APK packaging).
- Backend `npm test`: passed (79/79 tests passing).
- Configuration persistence, validation, and JSON export/import verified via unit tests.

## Last Commit

`11451e9` — `feat(ui-studio): implement EVE UI Studio foundation and master redesign polish`. The immediately following commit records this continuity update.

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

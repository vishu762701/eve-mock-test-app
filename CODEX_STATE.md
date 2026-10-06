# EVE — CODEX STATE

## Current Project State

- Eve is a native Android app in the Gradle `:app` module, implemented in Kotlin with XML layouts/resources. Firebase provides authentication; the client calls the TypeScript/Hono Cloudflare Worker in `backend/`, which stores relational data in D1 and media in Supabase Storage. `functions/` contains separate Firebase Cloud Functions.
- Home has one admin-managed banner surface in the former large hero location. Active banners rotate/swipe inside that surface; images are clipped to its rounded bounds. Each banner can have an optional HTTP(S) URL and CTA label.
- Admin has one Home Banner manager for image preview/upload, link editing, reorder, and delete. The separate Home Hero editor and its runtime code have been removed.
- The Test screen applies the Thin Material capsule treatment only to answer options A–D and its four bottom actions. Answer selection is green. Other app controls retain their existing styles.
- Light app background tokens are pure `#FFFFFF`; dark background tokens are pure `#000000`.
- Test submission remains server-side through `POST /api/attempts/submit`; authentication, scoring, answer persistence, and navigation code were not changed by the Home Banner/Test appearance task.

## Last Completed Task

Implemented the foundation and first working version of EVE UI Studio (App Builder):
- Schema-driven component customization engine (`UiStudioEngine`) targeting real Android views (Home, Test, Result screens) with safe fallbacks.
- Backend D1 migration (`0012_ui_studio.sql`), public published config endpoint with ETag & 304 conditional support, and admin-authenticated draft, publish, version history, rollback, reset, and audit logging routes.
- Android UI Studio data layer (`UiStudioRepository`, `UiStudioModels`), caching, and Retrofit endpoints.
- Full Admin UI Studio interface (`UiStudioActivity`, `activity_ui_studio.xml`) featuring screen/component selector, grouped property controls (Appearance, Layout, Typography, Visibility, Content), live preview canvas, draft saving, validation, publish dialog with change notes, version rollback, and JSON export/import.
- Unit tests: 203 Android unit tests passing (`UiStudioTest`), 79 backend tests passing (`ui_studio.test.cjs`), debug APK successfully built via `assembleDebug`.

## Last Verified Changes

- Updated `eve_menu_glass_bg` to translucent `#59FFFFFF` (light) and `#731C1C1E` (dark) so the 3-dot popup renders subtle translucent material rather than an opaque white rectangle in light mode.
- Updated `bg_drawer_glass.xml` to use transparent base with translucent tint overlays (`#59FFFFFF` light / `#731C1C1E` dark), allowing underlying blurred content to be perceptible in both themes.
- Updated `EmptyStateAnimationHelper.kt` to map `notification_bell.json` color (`0x020B19`) to high-contrast neutral light (`0xE2E8F0`) in dark mode, fixing bell visibility against `#000000`.
- Added `EveNavigationHelper.kt` supporting contextual scale-up navigation from source view bounds (Profile, Notifications, Syllabus, Admin, overflow actions) and coordinated reverse transitions.
- Replaced generic horizontal slide animations (`slide_in_right`, `slide_out_right`, `slide_in_left`, `slide_out_left`) with iOS 26 fluid scale and fade transitions (`fluid_scale_enter`, `fluid_scale_fade_out`, `fluid_scale_fade_in`, `fluid_scale_exit`).
- Removed obsolete, unreferenced slide animation resources.
- Confirmed the locked Telegram-style Day/Light circular reveal theme toggle animation is 100% untouched.
- Unit tests: 196 tests passing (including `MasterRedesignContinuationTest`). Debug APK compiles and packages cleanly. Backend tests (71/71) passing.

## Current In-Progress Task

NONE. Implementation and repository/CI checks are complete. Android device-side visual and interaction checks remain pending a usable Android runtime.

## What Remains

- On a working emulator/device, verify Admin image selection/preview/publish/delete, conditional URL/label behavior, Home image/CTA visibility and link opening, answer selection/clear, all four Test actions, and both themes.
- Reconcile the previously verified README architecture/retry notes: README still describes no custom backend and callable Cloud Function submission, while the app uses the Cloudflare Worker; it also describes durable retries as future work while `HistoryRepository` retries in memory.

## Known Issues / Blockers

- `adb devices` cannot start in this environment because the installed executable cannot link `_ZNSt6__ndk113__hash_memoryEPKvm`; no emulator executable or `app/src/androidTest` suite is available.
- `./gradlew lintDebug` reports two `NewApi` errors for `android:windowLightNavigationBar` in the unchanged `values/themes.xml` and `values-night/themes.xml`; minSdk is 24 and that attribute requires API 27. The lint run also reports 1,560 warnings. These theme files were outside this task and were not changed.
- The README architecture/retry discrepancies listed under What Remains are verified documentation inconsistencies.

## Verification Status

- `./gradlew testDebugUnitTest assembleDebug`: passed; 195 tests, 0 failures/errors/skips. Android XML/resource processing, Kotlin compilation, and debug APK packaging completed.
- Backend `npm test`: passed; 71 tests. `npm run build`: passed.
- Applied all 11 backend migrations in order to an in-memory SQLite database and confirmed a pre-link banner reads back with empty `link_url` and `link_label` defaults.
- Static checks confirmed the scoped Material pill style is used by exactly the four Test actions, no obsolete Home Hero runtime/resource references remain, and light/dark background tokens are white/black.
- `git diff --check`: passed.
- GitHub Actions [Build Eve APK](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37439163538) and [Deploy Eve Worker & Apply D1 Migrations](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37439163585) both completed successfully. The Worker run's D1 migration, deployment, and live production smoke-test steps passed.
- Android UI behavior, image rendering, touch interactions, and light/dark screenshots were not verified on-device. Build/test success is not recorded as UI verification.

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

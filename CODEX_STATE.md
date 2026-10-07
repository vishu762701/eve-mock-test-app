# EVE — CODEX STATE

## Current Project State

- Eve is a native Android app in the Gradle `:app` module, implemented in Kotlin with XML layouts/resources. Firebase provides authentication; the client calls the TypeScript/Hono Cloudflare Worker in `backend/`, which stores relational data in D1 and media in Supabase Storage. `functions/` contains separate Firebase Cloud Functions.
- Home has one admin-managed banner surface in the former large hero location. Active banners rotate/swipe inside that surface; images are clipped to its rounded bounds. Each banner can have an optional HTTP(S) URL and CTA label.
- Admin has one Home Banner manager for image preview/upload, link editing, reorder, and delete. The separate Home Hero editor and its runtime code have been removed.
- The Test screen applies the Thin Material capsule treatment only to answer options A–D and its four bottom actions. Answer selection is green. Other app controls retain their existing styles.
- Light app background tokens are pure `#FFFFFF`; dark background tokens are pure `#000000`.
- Test submission remains server-side through `POST /api/attempts/submit`; authentication, scoring, answer persistence, and navigation code were not changed by the Home Banner/Test appearance task.

### Focused UI Studio visual continuation — 2026-10-07

Continued the preserved implementation on `codex/ui-studio-functional` from `5b678765907604da0d7baed3435c7cba9261a60b`; fetched main remained `585f8ff91628677732da9fbf7a53c3ea618fa7d0`. Origin is exactly `https://github.com/vishu762701/eve-mock-test-app.git`.

- Separated safe native visual editing from protected business behavior; removed the global opacity floor in editor, renderer and both validators.
- Preserved native drawable/ripple/tint baselines, independent icon tint, surface/item/tint opacity, native states and reset; added real shared linear/constraint/button glass hosts and target-owned cleanup.
- Added stable anonymous/repeated selection, overlap drawing-order hit testing, HSV color picker, 12 tools, explicit scope, shape/highlights, press response, optional haptics and state tint transitions. Presets use supported properties only.
- No unrelated business logic or production Worker deployment changed. Android optical effects remain approximations; toolbar/internal adapter glass, physical refraction and navigation morphing are unsupported.
- Final APK/instrumentation builds and 29 focused JVM checks passed; backend type check and 85 tests passed. Seven distinct API 31 focused scenarios passed across runs; combined runs were interrupted by a corrected fixture error and emulator ANRs. Final native border pixels/reset and typography/undo/reopen passed separately. See current evidence for exact run boundaries; earlier full-suite evidence below remains historical.
- Authenticated production publication, older-API fallback, physical performance/haptics and animated timing remain unverified. Native foreground drawable ownership is preserved so MaterialCard stroke setters change the actual drawn edge.
- Current verification and exact limitations are recorded in `UI_STUDIO_VERIFICATION.md`.

### Latest UI Studio task — 2026-10-07

Continued the interrupted `MASTER_UI_STUDIO.md` implementation on `codex/ui-studio-functional`, based on fetched `origin/main` / starting HEAD `585f8ff91628677732da9fbf7a53c3ea618fa7d0`. Preserved the recovered working tree and external recovery copies; no reset, clean, force push or remote overwrite.

- Tool-first menu, 39 production-layout workspaces, production adapter fixtures, nested/repeated selection overlay, recoverable draft/session/undo, scoped dynamic insertion and shared published renderer.
- Functional Android backdrop blur, separate material/item opacity, fonts, shape/states, safe inserted actions, explicit global opt-in and baseline/effect reset. Optical refraction/morphing are explicitly unsupported.
- Worker/D1 remains source of truth; atomic revision/field guards cover save, publish, restore and reset. Publication uses fresh field/revision readback, not cached version equality.
- Backend TypeScript check and 84 tests passed. Android APK/instrumentation builds and all 226 JVM tests passed. Lint still fails only on two upstream API-27 theme attributes against minSdk 24.
- Six instrumentation scenarios passed on an API 31 emulator in day mode and again in night mode at font scale 1.3. Evidence includes actual blur pixels with sharp foreground, editor control/draft/runtime parity, undo/recreation, native-layout/adapter inflation and local fake-API recovery/publication scenarios.
- The final full seven-test suite passed in 242.659 s in night mode at font scale 1.3: nested pointer selection at two zoom scales, ignored drag, overlay isolation, native button fill/corners/pressed/reset, configured card actions and bounded insertion height are included.
- Authenticated production UI Studio publication/student sessions, API 24–30 blur fallback, physical-device performance, exhaustive accessibility checks and live exam/reveal/airplane regressions remain unverified. No production Worker deployment was performed.

See `UI_STUDIO_VERIFICATION.md` for the capability matrix, exact evidence, files and limits. Git history is authoritative for the task commit SHA.

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

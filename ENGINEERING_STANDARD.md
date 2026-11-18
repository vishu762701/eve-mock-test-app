# EVE Engineering Standard

The single workflow policy and repository constitution. Authority order: (1) the owner's latest explicit message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. The 9 Permanent Core Laws (1-Screen / 1-Feature Focused Workflow)

### LAW 1 — FINISH IT FOR REAL
Do exactly what the owner asked, completely, in the running app with zero placeholders or wrappers.
Done means it works end-to-end: UI, state, Day/Night, RTL, Hindi, back/gesture navigation, accessibility, and tests.

### LAW 2 — 1:1 TELEGRAM SOURCE PORTING
Authenticate official source origin from https://github.com/DrKLO/Telegram and pin commit SHA or record path/lines or hashes.
Port the minimum complete functional subsystem 1:1: Canvas/Matrix, physics/springs/interpolators, settings/state branches, with zero host interference. Deliver the EXACT upstream implementation and runtime behavior. Provenance goes in code comments: `// Telegram: <path>:<lines> @ <short sha>`.

### LAW 3 — TELEGRAM UI ARCHITECTURE
Never replace Telegram UI with Material or lookalikes. Kotlin Telegram* approximations are temporary and MUST be replaced by the verbatim Java from the Telegram zip when their foundation/screen job runs. Ported components (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TextCell`, `HeaderCell`, `ShadowSectionCell`, `DividerCell`, `TextCheckCell`, `TextDetailCell`, `TextInfoPrivacyCell`, `RadioCell`, `CheckBoxCell`, `TelegramSwitch`, `TelegramRadioButton`, `TelegramCheckBox2`, `TelegramTabsView`, `CubicBezierInterpolator`, `LayoutHelper`, `BackDrawable`, `MenuDrawable`, `ButtonBounce`, `TelegramThemeEngine`, QR & Gooey renderers) remain part of the app.

### LAW 4 — UNRESTRICTED OWNER FREEDOM
The owner has absolute authority to add, delete, or change any file, screen, layout, Telegram component, or feature at any time (No UI/UX choice is permanently locked against the owner). Never block requested changes with tests: update or delete conflicting tests in the exact same commit.

### LAW 5 — MANDATORY CI BUILD RECOVERY LOOP
A task is never complete if any build fails. Never abandon a failing build or push docs-only commits to bypass CI. On failure, fetch logs (`gh run view <run_id> --log-failed`), fix forward, push to `main`, and poll (`gh run watch <run_id> --exit-status`) until 100% green on the exact SHA.

### LAW 6 — ONE SCREEN OR ONE FEATURE AT A TIME
Development is strictly scoped to 1 screen or 1 feature at a time. Work only on the requested target without silently broadening scope. Complete that single screen/feature end-to-end and verify green build before moving to the next.

### LAW 7 — ZERO-SHORTCUT & ANTI-WRAPPER ENFORCEMENT
Taking shortcuts to bypass checks or fake Telegram compatibility is strictly forbidden (`ZERO-SHORTCUT`). Never create fake subclasses, empty/no-op stubs, or low-contrast hardcoded colors. A Telegram* class forwarding to another Telegram*/Eve class, even with a few extra lines (or declaring no member of its own), is banned.

### LAW 8 — BACKEND & RULES SYNCHRONIZATION
When a target screen or feature changes backend fields, permissions, or API flows, inspect and update corresponding Cloudflare Worker routes, D1 SQL migrations, `firestore.rules`, `storage.rules`, and backend tests so the feature works end-to-end.

### LAW 9 — CONTINUOUS DEAD-FILE PURGE
Remove only files proven obsolete within the touched scope. When a legacy class, layout, or drawable is replaced and has zero remaining callers, delete it in the same commit with grep proof.

## 2. Telegram Porting Rules (R1–R10)

### R1 — SOURCE
The only truth is the official Telegram source zip (https://github.com/DrKLO/Telegram). Never write Telegram UI from memory. If the zip cannot be listed with `unzip -l`, STOP and report BLOCKED.

### R2 — COPY, DON'T REWRITE
Telegram classes are copied byte-for-byte from the zip into `app/src/main/java/org/telegram/<same path>`, same name, same package, staying Java (no Kotlin conversion). Same for res (layouts, drawables, animations, Lottie, strings, styles): copy the real files.

### R3 — ALLOWED EDITS
Allowed edits to a copied file: only (a) cutting account/chat/network/DB dependencies via a small interface, (b) resource/id wiring. Mark each `// EVE-PATCH: <reason>`. Constants, durations, interpolators, thresholds and matrices stay exactly as in the source.

### R4 — MISSING FRAMEWORK CLASS
When a framework class is missing (AndroidUtilities, Theme, LocaleController, ActionBar, Components…): copy the real class from the zip. If it drags in account/chat code, make the smallest `// EVE-SHIM` with the SAME method signatures and real behavior for what the screen uses. Never re-implement behavior from memory.

### R5 — EVE LOGIC BOUNDARY
EVE logic (data, auth, admin, API, navigation) lives only in `com.eve.app` adapter classes, never inside `org/telegram`.

### R6 — FULL FUNCTION
Full function, not only visuals: every gesture, physics (spring/fling/overscroll), matrix/canvas math, animation, haptic, state, back gesture, keyboard inset, Day/Night, RTL and Hindi behavior must come from the source and work in the running app.

### R7 — NO EXTRA CODE
Add nothing the screen does not need (no new features, helpers, abstractions, docs, report files, demo tests). Touch no other screen. Search the repo before creating any class; reuse the existing owner; never create a duplicate.

### R8 — NO FAKES
No wrapper subclass forwarding to another class, no typealias, no empty/no-op stub, no TODO, no "simplified / approximate / inspired by".

### R9 — DELETE DEAD
After the swap, delete old Eve files of this screen with zero callers (show grep proof). Remove duplicate copies of the same Telegram class (keep the `org/telegram` one).

### R10 — TERMUX
No Gradle/NDK/emulator builds on the phone; GitHub Actions only. Extract the Telegram zip to `~/tg-src` (outside the repo, never commit it). After every step S0–S6 write done/next/commit SHA into `~/eve-handoff.txt` so a stopped session can resume.

## 2b. Porting Workflow Steps (S0–S6)

Execute in strict order and print each step's output:

- **S0 PREFLIGHT**: Run `unzip -l <zip> | head` and `unzip -z <zip>` (commit SHA). Locate the screen's source files; print path + line count.
- **S1 CLOSURE TABLE** (no code before this): Open the real entry class and follow every import, layout, drawable, style, string, animation, Lottie transitively. Table: item | upstream path:lines | VERBATIM / SHIM / CUT | already in Eve? (path).
- **S2 PHYSICS SHEET**: For every gesture/animation/matrix on the screen, list source file:line and the literal constants (duration, interpolator, stiffness, thresholds, matrix math).
- **S3 IMPLEMENT**: Copy per the table, add adapters, switch Eve's entry point to the ported screen, delete dead files (R9).
- **S4 BUILD**: Push to main, wait for GitHub Actions FAST on the exact SHA. If red: read the log, fix, push, repeat until green.
- **S5 PROOF**: Print sha256 of every VERBATIM file next to the zip's sha256. List every EVE-PATCH and EVE-SHIM. Label each element EXACT / ADAPTED (reason) / NOT FOUND / BLOCKED.
- **S6 REPORT**: Use the FINAL REPORT format of ~/.gemini/AGENTS.md; no line limit.

## 3. Core Workflow Rules
1. Do exactly what the owner asked—**1 screen or 1 feature at a time**. Never substitute, simplify, reinterpret or "improve" the requested behavior or look.
2. Stay in scope: the owner's request, not unrelated work. Unrelated problems get one line under "Noticed but not touched". Do not silently broaden product scope.
3. Authorized work needs no second approval: work proceeds without asking for a second approval. Ask only for an owner-only product, security, data or legal decision.
4. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Report implementation present separately from functional, visual/gesture, device and release acceptance.
5. Search for the existing owner of the affected state, navigation or mutation before adding another. Replace old code, do not stack a second copy. Remove only files proven obsolete.
6. Commit and push coherent changes to `main` only (no feature branches, no force-push). Reconcile remote main before each push. Exact-SHA FAST CI must pass on the final SHA. Treat emulator/ADB infrastructure errors separately from app test failures.
7. `[skip ci]` batching: when pushing several sequential commits, put `[skip ci]` in every commit message except the last, so exactly one clean run happens on the final SHA.
8. Never commit secrets, google-services.json contents, tokens or admin emails. Never stage `functions/package-lock.json`.
9. Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions require separate explicit owner authorization.
10. Termux handoff: if a real tool or session limit stops you, leave one short handoff in `~/eve-handoff.txt`: HEAD, uncommitted work, failing gate, next action.

## 4. Report format
Use the FINAL REPORT format of ~/.gemini/AGENTS.md; no line limit.

## 5. Minimal reporting & Development-only fast-build mode
- The owner is developing a private app and may change any UI/UX behavior. No UI/UX choice is permanently locked. Own the complete authorized outcome, but not unrelated work. Do not silently broaden product scope.
- Remove only files proven obsolete. For a source-exact request, authenticate its official origin and record path/lines or hashes plus runtime behavior where available. Do not substitute an unofficial implementation without owner permission. Proceed within the authorized scope without asking for a second approval.
- On ordinary main pushes, compile the debug APK and test sources rapidly. Keep manual FULL and release/security protections available and truthful.
- Automatic FAST is ARM64-only for rapid Android development; manual FULL builds all four native ABIs and exercises both emulator navigation modes.

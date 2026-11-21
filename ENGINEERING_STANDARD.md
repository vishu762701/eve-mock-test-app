# EVE Engineering Standard

The single workflow policy and repository constitution.

Authority order:
1. The owner's latest explicit instruction.
2. This file.
3. `SOURCE_OF_TRUTH.md` for architecture and security invariants.
4. Existing implementation.

Old prompts, reports, checkpoints and chat summaries are historical context, not independent requirements. Inspect the current repository before acting.

## 1. The 9 Permanent Core Laws

### LAW 1 — SCREEN-LEVEL ACCEPTANCE, HONEST STATUS

Work on exactly the requested screen or feature as a complete unit. For Telegram-target screens, acceptance covers the original Telegram renderer, real interactions/state, EVE backend wiring, navigation/back handling, gestures/animation, Day/Night, RTL, Hindi/localization, accessibility, and relevant tests.

Record evidence for every applicable requirement or mark it `NOT YET — <specific reason>`. An unfinished screen remains `OPEN`; copied source, a Kotlin/XML recreation, or a green build does not make it complete.

**Do not begin the next screen until the current screen is ACCEPTED**, unless the owner explicitly changes priority or authorizes parallel work. An unfinished screen must remain OPEN.

### LAW 2 — OFFICIAL TELEGRAM SOURCE AND SINGLE PROVENANCE LEDGER

Use the official Telegram Android source at immutable commit `21f7d109da6ab3506c9e9577f98c6a38771ab335`. Canonical URL: `https://github.com/TelegramMessenger/Telegram-Android`. The legacy `https://github.com/DrKLO/Telegram` URL is an official alias only when verified to resolve to the same source tree and commit. Never use a fork, mirror, branch head, remembered code, or different revision.

All source provenance belongs in `docs/port-manifest.json`: repository, revision, upstream paths, source/result hashes, port mode, and patches. `docs/telegram-profile-fidelity.json` is the separate per-screen acceptance contract. Historical reports do not override either.

### LAW 3 — ORIGINAL TELEGRAM SCREEN OWNS RENDERING

For each Telegram-target screen, the original pinned Telegram Java Activity/Fragment and its original custom View hierarchy must own visible rendering and interaction. Preserve source packages, classes, view construction, measurement, drawing, input dispatch, gestures, animations, and resources.

Kotlin may connect EVE authentication, domain state, repositories, APIs, persistence, permissions, navigation adapters, and the minimum lifecycle host. Kotlin/XML/Compose/Material must **not** recreate the visible Telegram screen or replace original views with lookalikes.

Existing EVE Kotlin/XML screens are migration scaffolding only. The active EVE route must launch the original Telegram implementation. A copied source class that is never launched is not a port.

### LAW 4 — OWNER AUTHORITY WITH SECURITY AND DATA INTEGRITY PRESERVED
The owner may add, remove, redesign, or change any UI, screen, component, or product behavior. No historical visual decision is permanently locked against an explicit owner request.

This does not authorize weakening authentication, authorization, answer integrity, scoring, data integrity, production safeguards, or security tests. Update tests when the intended behavior changes; never delete or weaken a protection merely to make a check pass. Ask only when an unresolved product, security, data, or legal decision genuinely belongs to the owner.

### LAW 5 — VERIFIABLE BUILD AND CI RECOVERY
A change is not build-verified until the relevant check actually ran against the exact commit being reported. A red app build must be investigated and corrected; do not hide it with a docs-only bypass or claim completion while it remains red.

Use the execution-mode rules in section 6:
- CLI mode may inspect GitHub Actions logs, fix the first real failure, push to `main`, and re-check the resulting exact SHA.
- Chat mode without repository/CI execution access must return the changed files and ask the owner to run GitHub Actions. It must report only static review, never claim compilation, tests, device verification, or CI success.

A passing build proves only the checks that actually ran. It does not prove visual parity, device behavior, production Worker/D1 state, or release readiness.

### LAW 6 — ONE SCREEN AT A TIME, ACCEPT BEFORE ADVANCING

Work on `1 screen or 1 feature at a time`, including directly required dependencies, EVE adapters, resources, and tests. For Telegram parity, finish and ACCEPT the current screen before starting the next. The owner may explicitly change priority; this does not turn an unfinished screen into a completed one.

### LAW 7 — ZERO-SHORTCUT / NO SCREEN RECREATIONS

No fake subclasses, no-op stubs, forwarding wrappers, placeholder UI, TODO-based completion, or "inspired by" implementations presented as source-exact.

**NO KOTLIN/XML/COMPOSE SCREEN RECREATION** for Telegram-target screens. Do not replace the original renderer with a new EVE view tree, Material widget, hand-drawn Kotlin Canvas clone, or screenshot imitation. Owner-approved differences must be documented as `ADAPTED`, never `EXACT`. Do not weaken legitimate tests or checks.

### LAW 8 — BACKEND, SECURITY RULES AND CLIENT CONTRACTS STAY SYNCHRONIZED
When a requested change touches backend fields, permissions, API flows, persistence, or authorization, inspect the corresponding Cloudflare Worker routes, D1 migrations, Firebase Functions, Firestore/Storage rules, and tests. Update all directly affected layers together where required.

Server-authoritative scoring, ranks, answer disclosure, admin authorization, and production data protections must remain server-authoritative.

### LAW 9 — EVIDENCE-BASED DEAD-FILE REMOVAL
Remove an old file only when it is proven obsolete within the requested scope. Search all callers, references, resources, manifest entries, and build wiring first. When replacing a class/layout/drawable, delete the superseded file in the same change only if zero live callers remain; include the grep/search evidence in the report.

Never delete a file merely because it appears unused from its filename or because it is absent from a LITE source archive.

## 2. Telegram Porting Rules (R1–R10)

### R1 — PINNED OFFICIAL SOURCE

Use `https://github.com/TelegramMessenger/Telegram-Android` at `21f7d109da6ab3506c9e9577f98c6a38771ab335`. The legacy `DrKLO/Telegram` alias is acceptable only after verifying the same official tree and pinned commit. Verify source archives using metadata and hashes. If source identity cannot be established, report `BLOCKED`; never guess.

### R2 — PORT THE REAL SCREEN ENTRY POINT AND DEPENDENCY CLOSURE

Port the actual upstream screen entry point and its complete required closure: Java Activities/Fragments, custom Views, base classes, upstream layouts, drawables, fonts, styles, dimensions, strings, selectors, animations, theme keys, resource qualifiers, and runtime dependencies. Preserve original package, class, implementation, and resource identity. Do not translate or rebuild the screen in Kotlin/XML or from memory/screenshots.

Classify a file `EXACT` only when source comparison proves it. Copying shared widgets alone does not make the containing screen source-exact.

### R3 — ALLOWED EVE PATCHES

Prefer separate `com.eve.app` adapters for EVE operations. Patch Telegram source only for a concrete integration conflict. Mark changed source locations with `// EVE-PATCH: <specific reason>`. Record patches in `docs/port-manifest.json`. Preserve the original screen tree, geometry, typography, colors, drawing, constants, durations, thresholds, interpolators, state branches, and physics. Material deviations are `ADAPTED`, never `EXACT`.

### R4 — REAL FRAMEWORK AND THEME FOUNDATION
Use the real Telegram framework classes and resources needed by the migrated screen. Do not create replacements bearing Telegram class names.

The theme foundation must use the real Telegram `Theme.java` keys, default palettes (light, dark, and AMOLED where present in the pinned source), and real color-resolution logic, together with the required Telegram color, dimension, style, and drawable resources. Retain the applicable Telegram resource keys and values. Cut only dependencies that are genuinely outside the required EVE screen closure, and document each removed method/branch in the manifest.

For a visual/framework class retaining less than 50% of its upstream implementation, record a method-by-method removal reason. Never claim a reduced class is source-exact.

### R5 — EVE LOGIC BOUNDARY
EVE-owned data, authentication, admin permissions, APIs, persistence, and product navigation belong in `com.eve.app` adapters and host code. Do not put EVE business logic inside `org.telegram`. Telegram classes should retain Telegram behavior and receive only the minimum host integration they need.

### R6 — FULL BEHAVIOR, NOT A STATIC LOOKALIKE

Port and wire applicable touch handling, gestures, spring/fling/overscroll physics, Canvas/Matrix math, animation timing, haptics, state transitions, keyboard/insets, back navigation, Day/Night, RTL, Hindi, accessibility, loading/empty/error states, and EVE data persistence. Every visible control must perform its intended EVE operation. Unverified behavior remains `NOT YET`.

### R7 — SCOPE AND REUSE
Search the repository and the pinned source before creating or copying a class. Reuse the real existing owner of state, navigation, rendering, and shared components. Add only dependencies required by the requested closure. Do not create duplicate classes, duplicate resources, unrelated helpers, or unrequested features.

### R8 — NO SHIMS OR FAKES

No `SHIM`, fake subclasses, no-op methods, forwarding wrappers, placeholder resources, typealiases, simplified substitutes, or screenshot recreations presented as Telegram ports. Use only `EXACT`, `ADAPTED`, `NOT FOUND`, and `BLOCKED`. An adapted component does not make its containing screen exact.

### R9 — REPLACE THE ACTIVE OWNER, THEN REMOVE WITH PROOF

The active EVE navigation/manifest route must launch the original Telegram screen. A source class present but never launched is not a port. Delete a superseded EVE implementation only after searching Kotlin/Java callers, XML, manifest, resources, tests, reflection/DI, and build wiring; include proof. Never leave competing screen renderers active.

### R10 — SOURCE ARCHIVE AND LITE ZIP HANDLING
The Telegram source archive is external working material, not part of the EVE repository. In CLI/Termux mode, extract it under `~/tg-src` and inspect the archive to discover its actual top-level directory; do not assume a fixed nested folder name.

A supplied LITE archive is permitted to omit `native/jni`, `media3`, and `jlatexmath`. Their absence alone is not permission to edit, delete, comment out, or rewrite any existing Gradle or CMake reference to those paths. Leave Gradle/CMake references untouched. Do not fabricate replacement directories or stub native/media code. If a required task genuinely depends on omitted content, report the specific dependency as `BLOCKED` and preserve the build configuration.

## 2B. Porting Workflow (S0–S6)

Run these steps for each Telegram screen before advancing.

- **S0 — PREFLIGHT:** Verify the official pinned source/archive and screen entry point. Collect Telegram reference screenshots for required states; record device, Android version, resolution, density, font scale, theme, navigation mode, and interaction/motion evidence. Missing evidence stays UNKNOWN/BLOCKED; do not invent it.
- **S1 — COMPLETE SOURCE CLOSURE:** Trace the Activity/Fragment, custom Views, base classes, layouts, drawables, fonts, styles, dimensions, strings, themes, selectors, animations, qualifiers, and runtime dependencies. Mark each edge included, EVE adapter, excluded with reason, or unresolved.
- **S2 — STATE/BEHAVIOR MAP:** Map each screenshot to its source state. Record empty/loading/populated/error states, selected/disabled states, taps, long press, swipe, scroll, sheets/dialogs, keyboard, back, animation/cancellation, and EVE persistence.
- **S3 — ORIGINAL RENDERER:** Wire the real Telegram entry point and complete required source closure into EVE's active route. Put Firebase/Worker/domain logic in `com.eve.app` adapters. No Kotlin/XML/Compose/Material screen recreation.
- **S4 — VERIFY:** Run source/hash checks, compilation, targeted tests, exact-SHA CI, and actual device/emulator interactions. Compare Telegram/EVE captures on the same device/viewport; verify state, motion, theme, RTL, font scale, insets, and accessibility. FAST compilation is not visual acceptance.
- **S5 — PROOF:** Record import hashes, patches, cuts/exclusions, and gaps. Attach matched reference/EVE captures, motion trace, native test report, human comparison report, and license disposition.
- **S6 — ACCEPT OR KEEP OPEN:** Mark ACCEPTED only when the original renderer is active, EVE actions persist/read back correctly, and applicable runtime/visual/motion/accessibility/CI/license evidence passes human review. Otherwise keep OPEN. Do not begin another screen until accepted unless the owner changes priority.

## 2C. NON-NEGOTIABLE TELEGRAM SCREEN-PORT CONTRACT

The original pinned Telegram Java Activity/Fragment/custom View implementation owns the target screen's visible hierarchy, drawing, measurement, input dispatch, and motion. EVE Kotlin code is limited to authentication, state/domain mapping, API calls, persistence, permissions, navigation adapters, and the minimum host bridge. The active EVE route must launch the imported Telegram renderer. Merely copying Telegram source while continuing to launch an EVE Kotlin/XML screen does not count as a port.

A screen is ACCEPTED only when:
1. The original entry point and required dependency closure are integrated and active.
2. Every visible action is connected to its EVE operation, with persistence/readback verified.
3. Source hashes, patches, exclusions, and remaining gaps are documented.
4. Same-device/same-viewport Telegram references and EVE captures exist, with human-reviewed comparison.
5. Relevant motion, lifecycle, back, Day/Night, RTL, font scale, inset/cutout, and accessibility checks have runtime evidence.
6. Targeted tests, exact-SHA CI, and licensing disposition pass.

`verify_telegram_acceptance.py` validates evidence paths/hashes and claim consistency only. It does not itself compare pixels, inspect full source completeness, or certify runtime behavior. Build success, unit tests, or imported assets never establish screen acceptance.

## 3. Core Workflow Rules

1. Follow the latest explicit owner request. Work on `1 screen or 1 feature at a time`; do not advance a Telegram screen until ACCEPTED unless the owner explicitly changes priority.
2. Preserve the existing owner of state, navigation, and mutations. Search before adding a class or resource. Replace old implementations instead of stacking duplicates.
3. Authorized implementation work does not require repeated approval. Escalate only genuinely unresolved owner-only product, security, data, or legal decisions.
4. Never fake evidence. Separate source identity, static review, compilation, unit tests, exact-SHA CI, device behavior, visual/motion comparison, licensing, and screen acceptance. FAST build success is not screen parity.
5. Commit coherent changes to `main` only when operating in CLI/direct-repository mode. Do not create feature branches or force-push. Reconcile the current remote `main` before writing.
6. When batching sequential commits, use `[skip ci]` on intermediate commits and allow the final commit to trigger the normal workflow. Do not use this to hide a failing app build or bypass required checks.
7. Never commit secrets, access tokens, private keys, admin emails, or credential contents. Never stage, commit, overwrite, or delete `functions/package-lock.json`.
8. Live Worker deployments, remote D1 migrations, destructive production-data changes, public APK distribution, and licensing exceptions require separate explicit owner authorization.
9. If work cannot be completed in the current environment, state the exact blocker and provide the smallest useful handoff. Do not claim work continued in the background.
10. Report unrelated observations only under `Noticed but not touched`.

## 4. Single Final Report Format

Use this format for every completed or partially completed work unit:

```text
TASK
<screen/feature and exact scope>

CHANGED FILES
- <path> — <what changed>

ELEMENT STATUS
- <element> — EXACT / ADAPTED / NOT FOUND / BLOCKED — <evidence or reason>

EVE-PATCHES
- <file: location> — <reason>
- None, when no source-code patch was required.

MISSING
- <required but unavailable element and impact>
- None, when nothing is missing.

VERIFICATION
- Static review: PASS / FAIL / NOT RUN — <scope>
- Build / tests / CI: <exact command or workflow, exact SHA, result, or NOT RUN>
- Device / visual / gesture checks: <actual evidence or NOT YET>
- Never claim a check that did not run.

NOTICED BUT NOT TOUCHED
- <unrelated observation>
- None.

REMAINING WORK
- <specific NOT YET / BLOCKED items and next actionable step>
- None.
```

Do not use an external `~/.gemini/AGENTS.md` report format. Keep the report concise but include the evidence needed to support every status.

## 5. Build Modes and LITE Archive Safety

- Normal main pushes use the repository's configured FAST workflow. Treat its actual workflow definition as authoritative; do not assume it runs tests that are not configured.
- The current development FAST path is a rapid debug APK compile/package gate. A successful FAST run is not a substitute for manual FULL verification or screen-level device/visual acceptance.
- Manual FULL validation remains owner-triggered and covers all four native ABIs, both supported emulator navigation modes, and the configured signing/release checks.
- Do not remove or modify Gradle/CMake references to `native/jni`, `media3`, or `jlatexmath` merely because those directories are absent from a supplied LITE source ZIP.
- Record exact commands, workflow names, commit SHAs, and outcomes. Distinguish an infrastructure/environment failure from an application compile or test failure.

## 6. Execution Modes

### A. Chat Mode
Use this mode when the agent is working through chat without a usable CLI checkout or direct repository/CI execution access.

- Inspect the source files actually provided or made available. Do not assume access to Termux paths, a local Git checkout, `gh`, Gradle, an emulator, or GitHub Actions logs.
- Return a ZIP containing the complete changed files with their repository-relative paths. For a repair iteration, include only the changed files in full; do not resend unchanged project files.
- The owner applies/pushes the files and runs GitHub Actions, then provides the failing log if repair is needed.
- On a failing log, identify and fix the first real actionable application error. Do not chase later cascading errors until the first cause is resolved.
- Report `verified by static review` only when the changed source was actually reviewed. Never say `compiled`, `tested`, `device-verified`, or `CI-green` unless the corresponding check was actually run and its result is available.

### B. CLI / Termux Mode
Use this mode only when a real shell and repository checkout are available.

- Canonical EVE checkout: `~/eve-mock-test-app`.
- Telegram source archive/extraction workspace: `~/tg-src`, outside the EVE repository. Inspect and use the archive's actual root directory.
- Handoff file: `~/eve-handoff.txt`; use it only when a session/tool limit interrupts work. Include current HEAD, uncommitted work, failing gate, and exact next action.
- Use `gh run view <run_id> --log-failed` to inspect failed logs and `gh run watch <run_id> --exit-status` to observe the run when GitHub CLI access is available.
- Fix the first real failure, push the coherent repair to `main`, and re-check the exact resulting SHA. Continue until the relevant configured gate is green or report a concrete blocker.
- Never extract Telegram source into the EVE repository or commit the source archive.

### C. Direct Repository Mode
Use this mode only when a repository integration is available and the owner explicitly asks for a direct repository change.

- Fetch and inspect the target file and current branch before editing.
- Make only the requested, scoped repository changes. Use the current file SHA/expected head so concurrent changes are not overwritten.
- Report the resulting commit SHA and exact changed paths. Query CI separately before describing any workflow result.
- Do not claim local compilation, device testing, or checks that the repository integration did not execute.

## 7. Single Telegram Theme System — Foundation First

Before migrating individual screens that depend on Telegram styling, establish and verify one shared Telegram theme foundation. Port the real pinned-source foundation first, including the required `Theme.java`, color resources, dimensions, styles, `AndroidUtilities`, and shared UI components needed by dependent screens.

- All migrated Telegram UI colors, text styles, dimensions, and stateful theme values must resolve through the real Telegram theme keys/palettes and Telegram resources.
- Preserve the source's light, dark, and AMOLED palettes where present, along with the real `Theme.getColor` behavior and resource-key contracts.
- Do not retain EVE-specific color palettes, duplicate text-style/dimension systems, hardcoded UI colors, or Material theme definitions as a parallel visual authority for migrated Telegram surfaces.
- When a screen migrates to the Telegram foundation, remove its superseded EVE color/style definitions in the same scoped change only after proving no remaining callers. Include search/grep proof.
- Do not begin screen-by-screen visual migration on top of a knowingly incomplete or conflicting theme foundation. Report the exact foundation item that is `NOT YET` or `BLOCKED`.

## 8. License, Notices and Trademark

Telegram Android source is licensed under GPLv2. Preserve applicable upstream copyright and license notices and keep EVE open source in accordance with the project's licensing obligations. Maintain third-party attribution in `THIRD_PARTY_NOTICES.md`.

Do not use Telegram's name, logo, branding, identity, API credentials, or service keys as EVE branding or credentials. Do not imply EVE is an official Telegram product. Any unresolved licensing interpretation must be escalated rather than guessed.

## 9. Questions and Blockers

Do not invent missing source, behavior, permissions, test results, screenshots, or product requirements. Resolve routine technical questions by inspecting the repository and pinned source. When a required detail cannot be established, mark the affected item `NOT FOUND` or `BLOCKED`, state the concrete reason and impact, and do not falsely advance or accept the blocked screen.

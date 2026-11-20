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
Work on the requested screen or feature as a complete unit. Its acceptance checklist must cover the applicable items: real UI and state behavior, navigation and back handling, gestures and animation, Day/Night, RTL, Hindi/localization, accessibility, backend/data behavior, and relevant tests.

Do not silently omit an applicable requirement. For each item, report evidence or mark it `NOT YET — <specific reason>`. An unfinished item must never be described as complete, but it does not automatically block work from proceeding to the next requested screen or feature. Continue in the owner's requested order and keep remaining work visible.

### LAW 2 — OFFICIAL TELEGRAM SOURCE AND SINGLE PROVENANCE LEDGER
The only permitted Telegram source is the official repository `https://github.com/DrKLO/Telegram` at pinned commit `21f7d109da6ab3506c9e9577f98c6a38771ab335`. Do not substitute another Telegram repository, mirror, branch head, fork, or remembered implementation.

All Telegram provenance belongs exclusively in `docs/port-manifest.json`: upstream repository, pinned revision, upstream paths, source/result hashes, port mode, and patch metadata. Never add Telegram provenance comments, source URLs, commit IDs, or path/line annotations to copied source or resource files.

### LAW 3 — REAL TELEGRAM IMPLEMENTATIONS, NO PERMANENT KOTLIN LOOKALIKES
Telegram UI and framework classes must come from the pinned Java source and real Telegram resources. Existing Kotlin `Telegram*` approximations are temporary migration scaffolding only. When a screen or foundation component using one is ported, replace it with the real Java implementation and remove the obsolete approximation in that same scoped change once it has no callers.

Do not create a permanent Kotlin lookalike, wrapper, forwarding class, or parallel implementation to avoid porting the actual dependency.

### LAW 4 — OWNER AUTHORITY WITH SECURITY AND DATA INTEGRITY PRESERVED
The owner may add, remove, redesign, or change any UI, screen, component, or product behavior. No historical visual decision is permanently locked against an explicit owner request.

This does not authorize weakening authentication, authorization, answer integrity, scoring, data integrity, production safeguards, or security tests. Update tests when the intended behavior changes; never delete or weaken a protection merely to make a check pass. Ask only when an unresolved product, security, data, or legal decision genuinely belongs to the owner.

### LAW 5 — VERIFIABLE BUILD AND CI RECOVERY
A change is not build-verified until the relevant check actually ran against the exact commit being reported. A red app build must be investigated and corrected; do not hide it with a docs-only bypass or claim completion while it remains red.

Use the execution-mode rules in section 6:
- CLI mode may inspect GitHub Actions logs, fix the first real failure, push to `main`, and re-check the resulting exact SHA.
- Chat mode without repository/CI execution access must return the changed files and ask the owner to run GitHub Actions. It must report only static review, never claim compilation, tests, device verification, or CI success.

A passing build proves only the checks that actually ran. It does not prove visual parity, device behavior, production Worker/D1 state, or release readiness.

### LAW 6 — ONE SCREEN OR ONE FEATURE PER WORK UNIT
Keep each implementation unit scoped to one requested screen or one feature, including directly required shared dependencies and affected tests. Do not silently expand into unrelated screens. Record adjacent issues under `Noticed but not touched`.

A screen can carry explicit `NOT YET` acceptance items without blocking the next screen when the owner has asked to continue. Never relabel that screen as fully complete.

### LAW 7 — ZERO-SHORTCUT / ANTI-WRAPPER
No fake subclasses, no-op stubs, forwarding wrappers, typealiases used to disguise missing implementations, placeholder UI, TODO-based completion, or "inspired by" approximations presented as source-exact ports.

Do not hardcode UI colors or invent a second theme system. Do not alter tests or checks simply to bypass a legitimate failure.

### LAW 8 — BACKEND, SECURITY RULES AND CLIENT CONTRACTS STAY SYNCHRONIZED
When a requested change touches backend fields, permissions, API flows, persistence, or authorization, inspect the corresponding Cloudflare Worker routes, D1 migrations, Firebase Functions, Firestore/Storage rules, and tests. Update all directly affected layers together where required.

Server-authoritative scoring, ranks, answer disclosure, admin authorization, and production data protections must remain server-authoritative.

### LAW 9 — EVIDENCE-BASED DEAD-FILE REMOVAL
Remove an old file only when it is proven obsolete within the requested scope. Search all callers, references, resources, manifest entries, and build wiring first. When replacing a class/layout/drawable, delete the superseded file in the same change only if zero live callers remain; include the grep/search evidence in the report.

Never delete a file merely because it appears unused from its filename or because it is absent from a LITE source archive.

## 2. Telegram Porting Rules (R1–R10)

### R1 — PINNED OFFICIAL SOURCE
Use only `https://github.com/DrKLO/Telegram` at `21f7d109da6ab3506c9e9577f98c6a38771ab335`. Verify that the supplied archive corresponds to the pinned revision using its available metadata and source hashes. If the source cannot be inspected or its revision cannot be established, stop the affected source-exact claim and report `BLOCKED`; do not guess or silently use another revision.

### R2 — COPY THE REAL IMPLEMENTATION
Copy the required Java classes and Telegram resources from the pinned archive, preserving their original package, class name, implementation, and resource identity. Do not translate Java to Kotlin or rewrite source from memory.

A source file may be classified `EXACT` only when the source comparison supports that claim. Any necessary EVE-specific source change must be explicit and limited to the allowed patch rules below.

### R3 — ALLOWED EVE PATCHES
Only make the minimum EVE integration changes required to connect the source implementation to EVE-owned behavior, resource IDs, or host lifecycle. Mark each changed source-code location with:

`// EVE-PATCH: <specific reason>`

Do not use this marker for Telegram provenance. Record provenance and patch metadata only in `docs/port-manifest.json`. Preserve source constants, durations, thresholds, interpolators, state branches, drawing operations, and matrix/physics behavior unless a specific EVE integration conflict makes a change unavoidable; explain every such change in the manifest and report.

### R4 — REAL FRAMEWORK AND THEME FOUNDATION
Use the real Telegram framework classes and resources needed by the migrated screen. Do not create replacements bearing Telegram class names.

The theme foundation must use the real Telegram `Theme.java` keys, default palettes (light, dark, and AMOLED where present in the pinned source), and real color-resolution logic, together with the required Telegram color, dimension, style, and drawable resources. Retain the applicable Telegram resource keys and values. Cut only dependencies that are genuinely outside the required EVE screen closure, and document each removed method/branch in the manifest.

For a visual/framework class retaining less than 50% of its upstream implementation, record a method-by-method removal reason. Never claim a reduced class is source-exact.

### R5 — EVE LOGIC BOUNDARY
EVE-owned data, authentication, admin permissions, APIs, persistence, and product navigation belong in `com.eve.app` adapters and host code. Do not put EVE business logic inside `org.telegram`. Telegram classes should retain Telegram behavior and receive only the minimum host integration they need.

### R6 — FULL BEHAVIOR, NOT A STATIC LOOKALIKE
Port the full applicable behavior, not just the visible appearance: touch handling, gestures, spring/fling/overscroll physics, Canvas/Matrix math, animation timing, haptics, state transitions, keyboard and inset handling, back navigation, Day/Night, RTL, and Hindi/localization behavior. Verify each applicable item on the target screen; mark unverified behavior `NOT YET`.

### R7 — SCOPE AND REUSE
Search the repository and the pinned source before creating or copying a class. Reuse the real existing owner of state, navigation, rendering, and shared components. Add only dependencies required by the requested closure. Do not create duplicate classes, duplicate resources, unrelated helpers, or unrequested features.

### R8 — NO SHIMS OR FAKES
No `SHIM` classification. No fake subclasses, no-op methods, forwarding wrappers, placeholder resources, typealiases, or simplified substitutes presented as Telegram ports. Use only the classifications `EXACT`, `ADAPTED`, `NOT FOUND`, and `BLOCKED` in the final element-status report.

### R9 — REPLACE AND REMOVE WITH PROOF
When a Telegram implementation replaces an EVE approximation, switch the actual owner/call sites to the ported implementation. Remove the obsolete EVE implementation only after proving it has no remaining callers. Keep a search/grep proof in the final report. Do not leave both implementations active as parallel UI systems.

### R10 — SOURCE ARCHIVE AND LITE ZIP HANDLING
The Telegram source archive is external working material, not part of the EVE repository. In CLI/Termux mode, extract it under `~/tg-src` and inspect the archive to discover its actual top-level directory; do not assume a fixed nested folder name.

A supplied LITE archive is permitted to omit `native/jni`, `media3`, and `jlatexmath`. Their absence alone is not permission to edit, delete, comment out, or rewrite any existing Gradle or CMake reference to those paths. Leave Gradle/CMake references untouched. Do not fabricate replacement directories or stub native/media code. If a required task genuinely depends on omitted content, report the specific dependency as `BLOCKED` and preserve the build configuration.

## 2B. Porting Workflow (S0–S6)

Execute these steps in order for each porting work unit. Do not start implementation before the source closure is understood.

- **S0 — PREFLIGHT:** Inspect the archive with `unzip -l`; inspect available revision metadata and verify the pinned revision. Record the archive root, relevant source paths, line counts, and hashes. If the supplied archive is LITE, record the known omitted directories without changing build files.
- **S1 — CLOSURE TABLE:** Trace the entry class and its transitive imports, layouts, drawables, styles, strings, animations, and shared components. Table columns: element | pinned upstream path/lines | status (`EXACT` / `ADAPTED` / `NOT FOUND` / `BLOCKED`) | current EVE owner/path | required EVE integration. Do not use `SHIM`.
- **S2 — BEHAVIOR / PHYSICS SHEET:** For each applicable gesture, animation, Canvas/Matrix operation, and state transition, record the source file/line and literal constants or formulas. Identify what can and cannot be verified in the available environment.
- **S3 — IMPLEMENT:** Port the smallest complete closure. Use the real Java/resources, add only required EVE adapters, wire the real entry point, and remove proven-dead approximations. Preserve the owner’s product behavior and backend contracts.
- **S4 — VERIFY:** Run only checks available in the current execution mode. In CLI mode, push a coherent change to `main` and inspect the exact-SHA workflow result. In Chat mode, perform static review and return the changed files; the owner runs GitHub Actions.
- **S5 — PROOF:** Compare source and result hashes for every claimed `EXACT` file. List every `EVE-PATCH`, every removed dependency/method and its reason, and all `NOT FOUND` / `BLOCKED` items. Do not infer visual or runtime parity from a hash alone.
- **S6 — REPORT:** Use the single Final Report format in section 4. No external agent-specific report template is required.

## 3. Core Workflow Rules

1. Follow the latest explicit owner request. Work on one screen or feature per unit; include directly affected dependencies and tests, not unrelated cleanup.
2. Preserve the existing owner of state, navigation, and mutations. Search before adding a class or resource. Replace old implementations instead of stacking duplicates.
3. Authorized implementation work does not require repeated approval. Escalate only genuinely unresolved owner-only product, security, data, or legal decisions.
4. Never fake evidence. Separate implementation status from static review, compilation, unit tests, CI, device/visual verification, and release acceptance.
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
- The current development FAST path is intended for rapid debug APK compilation and ARM64 native packaging. A successful FAST run is not a substitute for manual FULL verification.
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

Do not invent missing source, behavior, permissions, test results, or product requirements. Resolve routine technical questions by inspecting the repository and pinned source. When a required detail cannot be established, mark the affected item `NOT FOUND` or `BLOCKED`, state the concrete reason and impact, and continue with independent authorized work.

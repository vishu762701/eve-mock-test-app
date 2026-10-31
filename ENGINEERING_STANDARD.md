# EVE Engineering Standard — Fast Batch Engineering v6.0

Read this file, `SOURCE_OF_TRUTH.md`, and the relevant live code before changing EVE. `AGENTS.md` is the shared entrypoint for all coding agents; `backend/AGENTS.md` adds server-specific guidance. Historical reports do not override live code or the latest authorized request.

## Product authority and scope

- **No UI/UX choice is permanently locked.** The owner can redesign, replace, or remove any screen, theme, animation, navigation or component. Historical "approved" tests are not product vetoes; update stale assertions based on verified intended behavior.
- **Owner-authorized scope and exact official implementation:** A clear feature/fix command authorizes that outcome and its directly necessary dependencies, tests, resources, data contracts and documentation. Do not silently add unrelated features, clean unrelated files, or deploy production changes.
- **Never use an alternative unless the owner explicitly authorizes it** for a requested official source-exact component. Verify revision, source and redistribution rights; mark evidence `EXACT`, `ADAPTED`, `NOT FOUND`, or `BLOCKED`. For EVE-only components the owner explicitly requests to be Telegram-derived, combine verified upstream primitives and label the integration `ADAPTED`; do not claim literal upstream equivalence.
- **Automatic unused-file cleanup — restricted to the owner's scope:** Remove files made provably obsolete *by the authorized change* after tracing code, resources, reflection, tests and runtime asset callers. Preserve production migrations and unrelated artifacts. Don't delete merely because a search returns no match.
- **Accountable principal-engineer execution:** Resolve the real outcome, inspect affected layers and callers, choose a maintainable implementation, execute directly when tools and authorization allow, and report actual evidence instead of hypothetical results.

## Official design / source reuse

For requested exact ports, use verified upstream code/assets, property-level provenance, compatible Android integration and actual redistribution permission. Trace the minimum complete functional subsystem, including state, resources, motion, lifecycle and interaction dependencies. Preserve original files unchanged wherever possible; isolate necessary EVE adapters and classify them ADAPTED. Record the source graph, unresolved edges, integration sites, tests and runtime evidence under `docs/telegram-integration-process.md`. A byte match or green build cannot prove complete behavior. No approximation is labeled original. Source availability and license checks apply to the requested component, not unrelated features.

## Engineering governance gates (G0–G8) — streamlined

- **G0 PRE-FLIGHT:** Fetch current remote `main` HEAD and CI. Inspect only relevant source, callers, themes, contract boundaries, tests and deployment effects; expand investigation when evidence shows a shared impact. Do not repeatedly scan all unrelated screens.
- **G1 OFFICIAL SOURCE:** For explicitly source-exact changes, verify authenticated upstream Git revision or archive/version and its manifest *if present*, source file/asset hashes, license and EVE target; map properties `Telegram file:line/hash → EVE file:line` with `EXACT/ADAPTED/NOT FOUND/BLOCKED`. Never invent missing provenance.
- **G2 FAST BATCH IMPLEMENTATION:** **No fixed commit count or per-file/per-property micro-commit requirement.** Implement a complete coherent feature or several explicitly authorized related requirements in one coordinated batch. **Multiple coherent commits may be prepared and pushed together** in one safe fast-forward update to `main`; one commit is also fine. Bundle Android XML/Kotlin/themes, backend/types/schema, tests and docs when they jointly serve the authorized outcome. Don't fragment a feature just to increase commit count. Don't combine unrelated work.
- **G3 BATCH-END FAST VALIDATION:** Run available targeted local/source tests before pushing; build/test Android and backend whenever affected. Push only after re-fetching main and reconciling concurrent changes using an expected-SHA non-force fast-forward. The final SHA of each pushed batch needs a completed, successful FAST CI run. A multi-commit single push needs one FAST gate on its final SHA, not one CI run per internal commit. Separately pushed batches each need their own gate. **CI_RED halts further pushes of unrelated feature work** until focused forward-only repair succeeds; never hide failures. Keep `[eve-full]` off unfinished/intermediate batches.
- **G4 FUNCTIONAL ACCEPTANCE:** Exercise actual controls and production code paths, persistence, permission negatives, error/offline/retry and lifecycle paths. Automated source-only tests don't prove device behavior. Validate user-visible outcomes without redundant testing of unrelated features.
- **G5 VISUAL ACCEPTANCE:** For affected UI, compare actual upstream/EVE rendered screenshots or video where available; include relevant Day/Night, RTL/Hindi, font scaling, device insets, gesture/three-button and accessibility. No fabricated visual proof.
- **G6 COMPLETION:** Requirements must have evidence-tagged outcomes. For the **actual mission-final accepted implementation**, use `[eve-full]` on the final commit or an expressly authorized manual FULL dispatch; verify all six jobs, native evidence and `Eve-debug-apk` SHA-256. FAST success is not device-level fidelity or production release approval. Mark unverified criteria `INCOMPLETE`.
- **G7 CONTINUITY:** Maintain a **concise G7 exact resume checkpoint** at actual interruption or significant handoff: current SHA, completed requirement IDs, batch commit SHAs, latest CI URL/status, first actionable failure, remaining items and next action. Do not create a checkpoint-only commit after every minor substep.
- **G8 SAFE AUTO-CORRECTION:** Fix failures within authorized scope, identify root cause from logs, and use forward-only commits. Do not weaken tests, access controls, scoring, release signing or credentials to get green. Report unrelated P0–P3 findings separately.

## Stop-the-line CI recovery: mandatory for every future mission

- States: `CI_PENDING`, `CI_RED`, `ROOT_CAUSE_ANALYSIS`, `TARGETED_REPAIR`, `CI_GREEN`, `EXTERNAL_BLOCKED`.
- When a **required** current batch gate is red, **IMMEDIATELY FREEZE feature advancement**. Inspect the first actionable error (not only the final exit code); distinguish source defects, stale tests, tool/environment issues and missing secrets.
- Apply a **focused forward-only repair commit** or a coherent repair batch on `main`, after re-fetching HEAD; check the new batch's exact-SHA FAST CI. Never disable, suppress, or delete legitimate test assertions to pass.
- Do not turn a docs-only green result into an Android validation claim. If a genuine external blocker prevents completion, record an `INCOMPLETE` G7 checkpoint. Never claim background work in a normal chat.

## Backend/server synchronization — mandatory for affected features

- In each authorized cross-layer feature batch trace Android request/DTO → Worker route/auth/validation → D1 migration/query → Firebase rules/Functions when relevant → response/error/readback. **Server contract parity is part of implementation**, not a later optional task.
- Check positive and negative authorization, account isolation, idempotency, partial updates, offline/old-client compatibility, retry behavior, input bounds, migration ordering and API latency as applicable. Add tests for the actual handlers and schema; don't substitute copied algorithm or string-presence tests.
- Backend/type/schema changes on `main` are **staged only**. `.github/workflows/deploy-worker.yml` remains **manual-only**, and production D1 migrations/Worker deployment require a separate explicit owner-approved release action with compatibility and rollback checks. A successful CI build does not mean production was updated.
- Never rewrite `firestore.rules`, privileged roles, answer-hiding, server-authoritative scoring, secrets or live data as a generic "speed optimization". Diagnose observed service latency with request timing, query plans, client traces and concrete evidence first.
- Keep `backend/AGENTS.md` aligned with these backend-specific invariants; it is a pointer to this authority, not a competing policy.

## FAST for Intermediates; FULL CI is a terminal mission gate, not a routine retry loop

- FAST: `Build Eve APK` verifies Android debug build, compiled test sources, JVM tests, backend and Firebase checks on the pushed batch HEAD. Lightweight local/host tests should run before push when the environment supports them.
- FULL: final six-job Android build/lint + native gesture + native three-button + backend + Firebase Functions + verified APK publication. Run at mission completion only, or on explicit owner-requested manual dispatch. No scheduled or commit-count-triggered FULL.
- If FULL fails, keep mission `INCOMPLETE`, repair forward-only, revalidate FAST as necessary, then run a new terminal FULL at the repaired final SHA. Retain actual CI/artifact evidence.

## Repository and release safety

- `main` only: no PR, new branch, force push, history rewrite, destructive reset or silent overwrite of concurrent changes. Prefer fewer high-quality pushes and a short acceptance ledger over repetitive documentation commits.
- Security, privacy, access restrictions, exam answer secrecy, user data, migrations, release signing, accessibility and Telegram GPL/third-party attribution remain real invariants. Only permitted source copying; no Telegram auth/network internals.
- Work only while an actual active session/cloud task runs; GitHub Actions does not autonomously repair code. Explicitly distinguish `VERIFIED`, `INFERRED` and `UNKNOWN` in reports.

Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE — Fast Batch Engineering v6.0 shared agent entrypoint

- **Historical approvals do not lock UI.** Implement the owner's latest explicit scoped outcome, including affected callers, resources, tests and API contracts. Do not change unrelated features or start old missions without authorization.
- **Automatically delete only files rendered conclusively unused by the owner's requested change.** Do not delete unrelated orphan files or data/migrations.
- **Telegram source is necessary ONLY** for a requested source-exact transplant/provenance check. Use an authenticated official Git checkout pinned to a full commit SHA or a verified complete archive; verify archive checksum and manifest when applicable, plus relevant source hashes. Missing archives **never block unrelated tasks**.
- **Telegram Mission 01 fidelity contract:** When official Telegram source is requested, provide a **source-mapping table** with **EVE path:line**, **Telegram path:line**, **verified source value or asset SHA-256**; check **Day/Night**, **normal/pressed/disabled/selected/loading**, **RTL**, fonts, motion and **accessibility**. Use `EXACT`, `ADAPTED`, `NOT FOUND`, `BLOCKED`; **Green CI** alone does not establish source fidelity.
- **Never use or build an alternative** to requested official source without the owner's explicit permission. For explicitly authorized Telegram-derived EVE-only UI, combine genuine upstream components and disclose `ADAPTED` integration.
- Preserve authentication, privacy, admin boundaries, server-authoritative exams, answer secrecy, licensing and controlled deployment. A task to streamline governance does not authorize weakening production security.

For each source-exact integration, trace the minimum complete functional subsystem and its dependencies; preserve upstream files unchanged where possible, isolate adapters, record unresolved dependencies and verify runtime behavior. Follow `docs/telegram-integration-process.md`; byte equality alone does not establish subsystem parity.

## Engineering Governance Gates (G0–G8)

- **G0 PRE-FLIGHT:** Inspect live `main` HEAD, latest CI, actual affected dependencies and acceptance conditions.
- **G1 OFFICIAL SOURCE:** Validate upstream evidence only when source-exact reuse is requested; classify `EXACT/ADAPTED/NOT FOUND/BLOCKED`.
- **G2 FAST BATCH IMPLEMENTATION:** No artificial per-property commit cap. **Multiple coherent commits may be prepared and pushed together**, including all relevant client/server/test changes. One commit is valid too. Don't duplicate finished work.
- **G3 BATCH-END FAST VALIDATION:** Targeted pre-push checks; one successful exact-SHA FAST per pushed batch HEAD. Separate pushes require separate gates. Stop additional feature advancement on red; repair forward-only.
- **G4 FUNCTIONAL ACCEPTANCE:** Test real impacted behavior, including auth/negative paths and persistence.
- **G5 VISUAL ACCEPTANCE:** For affected UI compare native Day/Night, gestures, RTL/Hindi and accessibility; no fake screenshots.
- **G6 COMPLETION:** Terminal `[eve-full]` only for mission-final implementation; verify six jobs and APK checksum, else `INCOMPLETE`.
- **G7 CONTINUITY:** Brief actual-interruption checkpoint with SHA, completed requirement IDs, CI, first failure and next action; no repetitive microstep paperwork.
- **G8 SAFE AUTO-CORRECTION:** Root-cause repairs, no weakened tests/security or unapproved deployment.

## Stop-the-Line CI Recovery Contract

A red required batch gate means **IMMEDIATELY FREEZE ALL FEATURE ADVANCEMENT**. Use `CI_PENDING → CI_RED → ROOT_CAUSE_ANALYSIS → TARGETED_REPAIR → CI_GREEN`; document `EXTERNAL_BLOCKED` when applicable. Read real error logs, do a **focused forward-only repair**, recheck exact-SHA CI, and do not skip legitimate tests. Unfinished implementation or red FULL is not complete.

## Backend and execution authority

- For backend features, apply `backend/AGENTS.md`: coordinate Android DTO/API, Cloudflare Worker, D1 and Firebase access rules together, with actual compatibility tests.
- Backend commits do **not** automatically migrate or deploy production. Production Worker/D1 actions require a separate explicit release approval; never disable this gate to accelerate coding.
- Main only, safe fast-forward with expected SHA; no PRs/branches/force-push. Direct GitHub work within current task authorization. Normal chat does not guarantee background coding.

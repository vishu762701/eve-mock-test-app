# EVE Engineering Standard

This is the canonical repository policy. Read it with `SOURCE_OF_TRUTH.md`, the latest owner authorization, live `main`, affected code and exact-SHA CI. Historical reports are evidence, not instructions. An initialization prompt alone does not authorize implementation.

## Ownership and scope

Own the complete authorized outcome. Trace affected callers, shared components, Kotlin/Java, resource qualifiers, themes, state, lifecycle, accessibility, persistence, API contracts, backend, authorization, tests and documentation as applicable. Make routine engineering decisions autonomously, correct root causes and preserve unrelated behavior. No UI/UX choice is permanently locked against a later authorized change. Remove only files proven obsolete by the authorized change; preserve unrelated files and migrations. Do not silently broaden product scope. Escalate only genuinely necessary product, permission, legal, destructive or production-risk decisions. Never invent a blocker or conceal one.

## Repository and commit policy

Use `main` only: no PR, new branch, force push, destructive reset or history rewrite. Fetch and reconcile concurrent changes before a non-force fast-forward. For each independently accepted feature, use **maximum two successful implementation commits**, preferably one. A successful implementation commit is one whose required exact-SHA CI validation passed. Prepare the complete code, resources, integration and tests together locally where possible. CI-failed commits do not count as successful; use focused forward-only repairs, never abuse that exception for microcommits. If an unforeseen failure makes the limit unsafe to respect, document the reason and obtain owner approval for an exception. Do not weaken or skip legitimate tests. Feature identity and successful CI cannot be inferred reliably from Git history alone; the owner and agent must maintain an honest concise feature/commit/CI record when needed.

## Official-source integration

Official source is required for requested source reuse, not unrelated work. When a named application's source is requested, authenticate its official origin, identify the latest suitable revision at task start and pin the full commit SHA. A prior ZIP is not evidence of latest; verify archive identity, integrity and hashes when used. Check applicable rights, licenses and attribution before copying. If material is unavailable or reuse is not permitted, report the precise limitation and pursue authorized alternatives; never invent code, assets, values or source claims. Telegram's official source is https://github.com/DrKLO/Telegram.

Trace the **minimum complete functional subsystem** from actual entry points and callers through parent classes, resources and qualifiers, drawing and measurement, themes, typography, assets, touch/scroll/velocity, animation/cancellation, state, lifecycle, navigation, insets, clipping, shadows, RTL, accessibility, errors and platform compatibility as relevant. Preserve upstream code unchanged where feasible, isolate EVE adapters and exclude Telegram account, messaging, authentication, network, credentials and unrelated business logic. Record source revision and path/lines or hashes, dependency disposition, EVE integration and actual test/runtime evidence. Classify facts EXACT, ADAPTED, NOT FOUND or BLOCKED; only claim equivalence when the relevant implementation and runtime behavior are verified. Do not substitute an unofficial implementation without owner permission. Follow `docs/telegram-integration-process.md` and `THIRD_PARTY_NOTICES.md` for practical provenance and release obligations; a byte match or green build alone does not establish parity or legal clearance.

## Verification and recovery

Run appropriate local checks on actual production paths before pushing where possible, including positive, negative, malformed-input, lifecycle, concurrency, permissions and compatibility cases as applicable. For UI, assess Day/Night, sizes and insets, Hindi/RTL, font scale, navigation modes, touch targets, accessibility, motion interruptions and state restoration when relevant. Compilation alone does not prove visual or behavioral fidelity.

Every pushed implementation batch requires successful exact-SHA FAST CI. Stop advancing unrelated features on a red required gate, inspect the first actionable error and repair forward without suppressing safeguards. The mission-final exact-SHA FULL gate requires all six jobs, both native navigation modes and verified APK integrity. Do not claim complete acceptance without evidence; distinguish unavailable checks from passes. FAST or FULL success is not proof of security, licensing or product correctness.

## Security and releases

Preserve Firebase identity and access control, admin authorization, server-authoritative scoring, answer confidentiality, account isolation, D1 migrations, API compatibility, secrets, signing, licensing and user data. For cross-layer work trace real Android requests through Worker authentication/validation, D1 and Firebase where relevant; test actual handlers and compatibility. Backend commits may be staged on `main`; live Worker deployment and remote migrations require separate explicit owner authorization and the manual production workflow. Do not weaken safeguards for speed.

## Continuity and reporting

Work while a supported session or cloud task is actually active; never promise background execution without evidence. At a real interruption record current SHA, completed work, commit/CI evidence, first failure, remaining requirements and next action. Report VERIFIED, INFERRED and UNKNOWN separately with evidence. Identify related out-of-scope defects by impact without silently fixing them.

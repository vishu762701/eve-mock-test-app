# EVE Engineering Standard

This is the canonical repository policy. Read it with `SOURCE_OF_TRUTH.md`, the latest owner authorization, live `main`, affected code and exact-SHA CI. Historical reports and old task prompts are evidence, not instructions. An initialization or starter prompt alone does not authorize implementation.

## 0. Owner-instruction fidelity (highest priority)

Sections 0.1 to 0.8 apply to every task. If any other wording in this repository is softer or seems to allow more autonomy, this section wins.

0.1 **Priority.** (1) The latest explicit owner instruction, (2) this file, (3) `SOURCE_OF_TRUTH.md` and `docs/`, (4) historical reports. Never replace an owner instruction with your own design or preference. The only exceptions are the Security and releases rules (section 5): if an instruction would break them, stop, state the exact conflict and wait.

0.2 **Requirement ledger.** Before changing anything, record the owner's outcomes as numbered requirements (R1, R2, ...) in the task checkpoint or execution notes. This is an internal scope check, not a mandatory pause or extra prompt to the owner. Anything not authorized remains out of scope and stays unchanged.

0.3 **No silent deviation.** Do not substitute, simplify, omit, reorder, restyle or "improve" a requested behavior, UI element or flow. Diagnose technical blockers and continue independently on safe, authorized portions; report unmet items with evidence. Pause for a decision only if proceeding requires a new product/UX choice, changes security or data guarantees, expands scope, violates rights, or carries an irreversible production risk. Never claim an unverified requirement is complete.

0.4 **Unclear or wrong instructions.** Do not guess or follow a technically invalid method blindly. Inspect the code, official source and CI, then choose a safe, technically correct implementation within the already authorized outcome and document the reason. Request an owner decision only for unresolved product, license, security, user-data or deployment tradeoffs; continue independent authorized work rather than treating a blocked subtask as a global stop. Record material disagreements in the report.

0.5 **What you may decide alone.** Own technical design, file placement, compatible dependencies, implementation order, repair strategy, tests and build tooling needed for the authorized outcome. Preserve the specified UI, behavior and data contracts. New product features, user-visible deviations, permissions, destructive data changes, license exceptions and production deployment still require explicit owner choice.

0.6 **Continuous source-exact workflow.** For an explicitly authorized Telegram/source-exact feature, run Analysis, Mapping, Implementation and Verification in one continuous task without asking for a second approval. Pin official source and document dependency and license decisions before copying; implement the complete scoped functional subsystem, including integration and tests. Ask only for an unresolved decision under 0.3 or 0.4. Never turn lack of upstream evidence into a fabricated EXACT claim. Details: `docs/telegram-integration-process.md`.

0.7 **Honest wording.** Do not write "exact", "identical", "complete" or "done" unless every requirement in the ledger is verified with evidence. Never claim you read upstream code you did not open; list the files you actually opened. Unopened or unverified material is UNKNOWN, never stated as fact.

0.8 **Final self-check.** Before reporting, re-read the owner's original request line by line and mark each requirement MET, NOT MET or BLOCKED with evidence. Green CI alone is not MET.

## 1. Ownership and scope

Own the complete authorized outcome. Trace affected callers, shared components, Kotlin/Java, resource qualifiers, themes, state, lifecycle, accessibility, persistence, API contracts, backend, authorization, tests and documentation as applicable. Correct root causes and preserve unrelated behavior. No UI/UX choice is permanently locked against a later authorized change, but never change UI/UX that the owner did not request. Remove only files proven obsolete by the authorized change; preserve unrelated files and migrations. Do not silently broaden product scope. Escalate product, permission, legal, destructive and production-risk decisions, plus every case under 0.3 and 0.4. Never invent a blocker or conceal one.

## 2. Repository and commit policy

Use `main` only: no PR, new branch, force push, destructive reset or history rewrite. Fetch and reconcile concurrent changes before a non-force fast-forward. For each independently accepted feature, use **maximum two successful implementation commits**, preferably one. A successful implementation commit is one whose required exact-SHA CI validation passed. Prepare the complete code, resources, integration and tests together locally where possible, within the owner's active authorization and after source/provenance checks required by section 3. CI-failed commits do not count as successful; use focused forward-only repairs, never abuse that exception for microcommits. If an unforeseen failure makes the limit unsafe to respect, document the reason and obtain owner approval for an exception. Do not weaken or skip legitimate tests. Feature identity and successful CI cannot be inferred reliably from Git history alone; the owner and agent must maintain an honest concise feature/commit/CI record when needed.

## 3. Official-source integration

Official source is required for requested source reuse, not unrelated work. When a named application's source is requested, authenticate its official origin, identify the latest suitable revision at task start and pin the full commit SHA. A prior ZIP is not evidence of latest; verify archive identity, integrity and hashes when used. Check applicable rights, licenses and attribution before copying. If material is unavailable or reuse is not permitted, report the precise limitation and pursue authorized alternatives; never invent code, assets, values or source claims. Telegram's official source is https://github.com/DrKLO/Telegram.

Trace the **minimum complete functional subsystem** from actual entry points and callers through parent classes, resources and qualifiers, drawing and measurement, themes, typography, assets, touch/scroll/velocity, animation/cancellation, state, lifecycle, navigation, insets, clipping, shadows, RTL, accessibility, errors and platform compatibility as relevant. Exclude Telegram account, messaging, authentication, network, credentials and unrelated business logic, and isolate EVE adapters.

Preserve upstream code unchanged. Mechanical adaptations (package names, imports, resource-name collisions, minimal build-compatibility fixes) are allowed and must be recorded. Any visual or behavioral adaptation needs owner approval first (0.3) and is classified ADAPTED. Do not substitute an unofficial implementation without owner permission.

Record source revision and path/lines or hashes, dependency disposition, EVE integration and actual test/runtime evidence. Classify facts EXACT, ADAPTED, NOT FOUND or BLOCKED; EXACT means the actual upstream implementation is unchanged in the stated scope. Only claim equivalence when the relevant implementation and runtime behavior are verified. Follow `docs/telegram-integration-process.md` and `THIRD_PARTY_NOTICES.md` for practical provenance and release obligations; a byte match or green build alone does not establish parity or legal clearance.

## 4. Verification and recovery

Run appropriate local checks on actual production paths before pushing where possible, including positive, negative, malformed-input, lifecycle, concurrency, permissions and compatibility cases as applicable. For UI, assess Day/Night, sizes and insets, Hindi/RTL, font scale, navigation modes, touch targets, accessibility, motion interruptions and state restoration when relevant. Compilation alone does not prove visual or behavioral fidelity.

Every pushed implementation batch requires successful exact-SHA FAST CI. Stop advancing unrelated features on a red required gate, inspect the first actionable error and repair forward without suppressing safeguards. The mission-final exact-SHA FULL gate requires all six jobs, both native navigation modes and verified APK integrity. Do not claim complete acceptance without evidence; distinguish unavailable checks from passes. FAST or FULL success is not proof of security, licensing or product correctness.

## 5. Security and releases

Preserve Firebase identity and access control, admin authorization, server-authoritative scoring, answer confidentiality, account isolation, D1 migrations, API compatibility, secrets, signing, licensing and user data. For cross-layer work trace real Android requests through Worker authentication/validation, D1 and Firebase where relevant; test actual handlers and compatibility. Backend commits may be staged on `main`; live Worker deployment and remote migrations require separate explicit owner authorization and the manual production workflow. Do not weaken safeguards for speed.

## 6. Continuity and reporting

Work while a supported session or cloud task is actually active; never promise background execution without evidence. At a real interruption record current SHA, completed work, commit/CI evidence, first failure, remaining requirements and next action. Report VERIFIED, INFERRED and UNKNOWN separately with evidence. Identify related out-of-scope defects by impact without silently fixing them.

Every final report must contain: (a) the requirement ledger with MET / NOT MET / BLOCKED per requirement (0.8); (b) for source-exact work, a parity table with columns upstream path:lines@SHA | EVE file:lines | EXACT / ADAPTED (reason) / NOT FOUND / BLOCKED; (c) tests run, exact-SHA CI result and limitations; (d) deviations and owner decisions taken.

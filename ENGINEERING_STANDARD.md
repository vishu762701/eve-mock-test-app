# EVE Engineering Standard

Read this file and `SOURCE_OF_TRUTH.md` before changing the repository. Inspect live code and the current task. Historical plans are not active instructions.

## Active development: owner controls all UI/UX
EVE is being actively developed. **No UI/UX choice is permanently locked**: the owner may add, change or remove any screen, navigation bar, shape, typography, color, animation, Lottie effect, Telegram/iOS-inspired component or feature. Labels such as "locked", "protected", "approved" or "exact" in old documents, tests, fixtures or code comments reflect past states, not permanent prohibitions. Implement the owner's latest safe scoped change and update code, resources, Day/Night and tests coherently. Preserve unrelated functions by default, but an explicit request to replace/remove a feature overrides old UI preservation instructions. Authentication, privacy, server-authoritative exam integrity, accessibility, and license compliance must remain sound.

## Owner-authorized scope and exact official implementation
- **Owner has full product-design freedom:** during development, any UI/UX, animation, color, feature, file or screen can be requested for addition, change, replacement or removal. Historical approvals never permanently lock visual decisions.
- **Strict scope:** change ONLY what the owner expressly requests and directly necessary callers, resources, Day/Night variants, tests and build contracts. Inspect dependencies without editing unrelated functionality, files, designs, animations, or settings. No unsolicited refactors, improvements, new features or broad orphan cleanup. Report unrelated issues as findings; require explicit owner approval before changing them.
- **Official means verified official:** when the owner requests an official asset, component, behavior, animation or implementation, find and verify its genuine upstream source/documentation, version and required resource/hash/manifests, and match what the owner requested. Never call an approximate, guessed or re-created lookalike official. **Never use an alternative unless the owner explicitly authorizes it.**
- **Exact blocker handling:** if the real source, verification, compatibility or copying permission is missing, mark that specific item BLOCKED and explain what is needed; do not replace it with an alternative or change it silently. Work on other explicitly authorized independent items only. Future distribution licensing must not become a blanket barrier to unrelated internal development; keep actual security, legal and release obligations.

## Instruction priority
1. Latest explicit owner request controls the product outcome; old visual rules, numeric choices and historical approvals never veto a safe redesign.
2. Maintain security/privacy, data integrity, legal compliance and meaningful regression verification. Unrelated features are preserved only by default; explicit safe removals and replacements are authorized.
3. `SOURCE_OF_TRUTH.md` describes architecture/current code, not immutable UI. `APPROVED_UI_IMPLEMENTATION_AUDIT.md` and `CODEX_STATE.md` are historical archives, not instruction authorities.
4. `AGENTS.md` is the shared entrypoint, and `GEMINI.md` / `CLAUDE.md` are pointers. None can override the latest safe owner request.

## Accountable principal-engineer execution
- Understand the owner's real outcome and observable acceptance criteria before coding. Inspect the live implementation rather than accepting older agent reports. Do not simply say "cannot" because a prior visual was approved, a source archive is absent for unrelated work, or an old fixture disagrees.
- Inspect every impacted layer: Kotlin/Java, XML, themes/qualifiers, navigation, callers, runtime overrides, resource IDs, dynamic assets, state, accessibility, font scale, RTL, animations, backend APIs, persistence, tests, CI, docs and licensing. Implement complete related changes; preserve unrelated functionality.
- Analyze actual root causes, not symptoms. Consider lifecycle, threading, responsiveness, offline/loading/error/empty states, permissions, API compatibility and performance where relevant. Prefer maintainable native implementations instead of temporary hacks.
- Make technical choices autonomously ONLY within the requested scope. When exact source, implementation rights or compatibility are missing, report that specific blocker. Alternatives may be proposed but NEVER implemented without the owner's express approval; do not modify unrelated work.
- Own the outcome: run available preflight checks, compile and test, inspect current SHA-specific CI logs, repair failures without removing legitimate assertions or bypassing gates. Report VERIFIED, INFERRED and UNKNOWN clearly; enumerate remaining unrelated issues by impact. Work only in the active session, never promise background completion.

## Engineering governance gates (G0–G8) — non-negotiable contract
Every engineer and autonomous agent operating on EVE must enforce nine sequential, testable quality gates before, during, and upon completion of any mission or task:

### G0: Pre-flight & baseline audit
- Before modifying repository files, fetch the live remote `main` branch HEAD, record its exact commit SHA, and reconcile any concurrent commits.
- Inspect the live source files and caller graph rather than relying on historical summaries or conversational memory.
- Enumerate concrete, testable acceptance criteria, identify affected layers and dependencies, and check the latest CI run status for known baseline failures.
- Proactively report material risks, regressions, or contradictions before writing code.

### G1: Official source & provenance verification
- When the owner requests official Telegram assets, layouts, typography, animations, or components, verify the upstream Git revision and local archive SHA-256 (`~/.eve/official-telegram/verify.sh` and `THIRD_PARTY_NOTICES.md`).
- Map each property in a source-mapping table: property name, EVE `file:line`, upstream Telegram `file:line`, verified source value or asset SHA-256 hash, and status classification:
  - `EXACT`: Verbatim copied upstream asset, identical math/color/token, or verified asset hash match.
  - `ADAPTED`: Telegram architecture adapted to native Android standard with documented technical rationale.
  - `NOT FOUND`: Requested property does not exist in the verified upstream source.
  - `BLOCKED`: Upstream item cannot be verified, accessed, or legally redistributed under current licensing.
- Never label an approximation or wrapper as exact if only surface attributes or wrappers were copied. Keep EVE backend, security, and authentication invariants strictly intact.

### G2: Serial micro-change contract
- Work exclusively in sequential, atomic micro-commits directly on `main`. Each commit must represent one coherent, reviewable unit of change.
- Bundle code, resources, Day/Night qualifiers, callers, and directly dependent test contracts together in the same commit.
- Never create multi-feature omnibus commits. Never unnaturally split a single atomic change merely to inflate commit count.
- **Strict intermediate commit rule:** Never add [eve-full] to intermediate commits. Intermediate commits must run FAST CI only.

### G3: FAST CI validation on every intermediate push
- After pushing an intermediate micro-commit to `main`, inspect the remote FAST CI run on that exact SHA.
- A red CI run stops all forward implementation: do not proceed to the next feature while CI is failing.
- Diagnose failures from first actionable logs and implement focused, forward root-cause repairs on `main`.
- Never disable, suppress, or delete legitimate test assertions to bypass failures. If GitHub Actions APIs cannot establish run status, mark it `UNKNOWN` and never claim `PASS`.
- Enforce the mandatory stop-the-line state machine and failure category playbook defined in [Stop-the-line CI recovery: mandatory for every future mission](#stop-the-line-ci-recovery-mandatory-for-every-future-mission).

### G4: Functional behavior acceptance
- Verify observable runtime behavior (e.g., tap dispatch, long-press gestures, drag state machines, coordinate translations, navigation transitions, persistence, admin authorization gates, normal-user read access, lifecycle survival on `onResume` / rotation, accessibility).
- Green CI, compilation, or static code presence alone is insufficient to prove functional acceptance; verify with targeted JVM tests, instrumentation tests, and physical/emulator device execution.

### G5: Visual fidelity & device acceptance
- For UI/UX changes, verify visual correctness across all relevant environments: Day and Night themes, phone and tablet breakpoints, system font scaling, window insets/cutouts, RTL layout direction, and Hindi/Devanagari typography.
- Compare EVE rendered output with upstream Telegram reference implementations where feasible.
- Never generate, cite, or rely on synthetic or fake visual evidence.

### G6: Mission completion & terminal FULL CI gate
- Compile a comprehensive requirement-by-requirement acceptance matrix classifying every deliverable as `VERIFIED`, `FAILED`, `UNKNOWN`, or `BLOCKED` with links to SHA-specific evidence.
- Only after all authorized mission acceptance criteria are implemented, verified, and passing may a mission-final commit carry `[eve-full]` to trigger the terminal six-job FULL CI pipeline (`build`, `Native UI (gesture)`, `Native UI (three-button)`, `backend`, `firebase-functions`, `Publish verified debug APK`).
- Inspect and record the exact final SHA, all six passing job IDs, and the published debug APK SHA-256 checksum. If any legitimate blocker remains, report the mission as `INCOMPLETE`.

### G7: Continuity & resumable checkpoint contract
- If execution is interrupted by session limits, token boundaries, tool timeouts, or external blockers, establish a precise resumable checkpoint before halting.
- The checkpoint must record: current remote `main` SHA, completed microsteps and commit SHAs, latest CI run ID and job status, first actionable failure (if any), uncommitted changes, next exact action, source proof, and remaining acceptance criteria.
- Never promise or claim unattended background continuation.

### G8: Safe auto-correction & root-cause repair
- Proactively diagnose and repair defects within authorized mission scope; choose maintainable native architecture over temporary workarounds.
- Explicitly disclose material failures, architectural tradeoffs, and behavioral deviations.
- Never silently roll back previous approved work, change secrets/credentials, weaken authorization guards or server-authoritative scoring, or substitute unauthorized code.
- Escalate genuinely new product decisions, changed scope, or demonstrated legal/security blockers to the owner.
- Follow the disciplined retry and fault-tree protocol: re-examine full logs and revise diagnostic hypotheses after two failed repairs; stop only for demonstrated external blockers or tool limits with a precise G7 checkpoint.

### Standardized mission acceptance report
All completed or checkpointed missions must document results using the canonical report format defined in [docs/MISSION_ACCEPTANCE_TEMPLATE.md](file:///data/data/com.termux/files/home/eve-mock-test-app/docs/MISSION_ACCEPTANCE_TEMPLATE.md).

## Official design / source reuse: lawful friction-minimizing workflow
- The owner can request any official design or component from Apple, Samsung, Telegram, Android or other sources. Use the verified original upstream documentation/code/asset when the owner requests "official", not a self-authored approximation. Require an upstream ZIP only when source-exact verification genuinely needs that ZIP.
- For **exact copied code, illustrations, icons, fonts, animation files or assets**, inspect the item's specific permissions, copyright/license, attribution, redistribution and derivative-source obligations, platform API compatibility and branding. An "official" label is not a universal reuse license.
- Prefer the authentic officially documented API/source/asset requested by the owner. If specific code/assets cannot be verified, integrated or lawfully copied, mark only that exact item BLOCKED; never switch to an original or licensed substitute unless the owner explicitly requests that option.
- Development/testing and public distribution differ. Keep third-party notices, required source supply, and `.github/workflows/release.yml` GPL/signing/security checks. A repo policy cannot waive legal rights. Defer release-only reviews until release decisions when appropriate; report actual exact-copy blockers without refusing unrelated development or substituting assets.

## Execute, do not just report
- Determine the real objective and concrete acceptance criteria. For a coding request, make the requested implementation changes, not merely an audit or a new prompt, unless the user specifically asks for analysis only.
- Inspect the actual implementation, callers, resources, themes, tests, backend and workflows relevant to the change. Resolve conflicts with existing code rather than inventing dependencies.
- Make the smallest complete change. Update all directly affected Day/Night variants, reusable components, and tests; preserve unrelated screens and behavior.
- Challenge a risky or contradictory requirement briefly, with evidence. Do not silently replace a safe user choice with your own aesthetic preference or an older rule.
- Keep the current Telegram theme reveal, Lottie triggers and visual features only when not in scope; the owner may explicitly redesign or remove any UI element. Never accidentally weaken server-authoritative scoring, authorization, answer secrecy or data integrity.
- Never commit credentials, bypass authorization, disable failing gates, fake evidence, or change production data just to satisfy a check.
- **Telegram licensed-source reuse:** if official Telegram code/assets are requested, verify matching upstream source, hashes/manifests where applicable, provenance, actual per-item permissions and dependency compatibility. Copy only when permitted; preserve notices, attribution and applicable source-delivery obligations. If exact source/rights are missing, mark that copy BLOCKED and ask the owner; do not implement any alternative without approval. Never treat release licensing as a blanket refusal of unrelated tasks.
- Avoid unrelated refactors, broad rewrites, unnecessary dependencies and **blind** retries. After two unsuccessful attempts at the same error, inspect full logs, fixture, dependency and diff evidence, then make a materially different root-cause repair; keep working within available tools/time. Only stop for a demonstrated external blocker, never simply because CI was red twice.

## Automatic unused-file cleanup — restricted to the owner's scope
- During a requested add/edit/removal, automatically delete only the files/resources proven unused directly BECAUSE of that authorized change, together with now-obsolete call sites. Do not delete unrelated files just because they look unused; repo-wide cleanup runs only when expressly requested. Report deleted paths and dependency proof.
- First inspect references in Kotlin/Java, XML and all Day/Night/density qualifiers, manifests, Gradle, reflection/dynamic asset loading, tests, CI, backend/Functions, deployment and documentation. A simple zero-match grep is not sufficient proof for dynamic Android resources. Preserve needed historical database migrations, prod compatibility, licensing/attribution, secrets configuration and rollback/provenance until safe removal is proven.
- If usage cannot be established confidently, mark UNKNOWN and investigate; never delete live code or production data speculatively. "Permanent delete" means remove from current repository state, not secretly erase Git history. Cleanup is triggered by each active task; this is not a background agent.
- For owner-approved visual changes, replace stale pixel/color/geometry comparisons and "locked" tests with assertions for the **new** design, contrast, accessibility and functional regression contracts; never weaken legitimate quality gates simply to manufacture green CI.

## CI profiles and mission completion (permanent across models/agents)
- Every eligible intermediate main push runs **FAST**: debug APK compilation, Kotlin/JVM test-source compilation AND JVM unit tests, plus backend and Firebase validation. **FULL** additionally enforces Android lint, AndroidTest Kotlin compilation, both gesture and three-button emulators, theme/visual evidence and release signing safeguards. Trigger FULL on a mission-final commit with `[eve-full]`, or manually via `workflow_dispatch` of **Build Eve APK**. FAST never publishes the verified artifact.
- **Mission-final only:** Native UI emulators must run ONLY after the entire user-defined mission or task batch is implemented, integrated and ready for final delivery. Individual components, intermediate implementation commits, ordinary build repairs, CI/rule changes, and progress checkpoints DO NOT complete a larger ongoing mission. Use FAST for every such push; NEVER automatically trigger FULL because a day passed, a push counter reached 15–20, a model restarted, a high-risk change occurred, or an agent wants extra reassurance. Do not put `[eve-full]` on any non-final commit. After all mission acceptance criteria are implemented, mark the final implementation commit `[eve-full]` and wait for that exact commit's FULL run. A manual `workflow_dispatch` is an explicitly owner-requested exception, not an agent-invented intermediate checkpoint. Only successful FULL publishes `Eve-debug-apk`; FAST success never implies complete runtime verification.
- For each change update source, related call sites/resources and relevant test contracts together, including Light/Dark; never modify an assertion merely to hide a real regression. Classify failures by SHA/job/log and repair the actual cause; avoid endless reruns, suppressions, skipping checks, rewriting unrelated code or weakening security gates.
- Do not stop work simply because the first or second build fails. Use FAST builds and targeted source/tests to diagnose and repair intermediate failures, without prematurely running FULL. If the final FULL fails, inspect the failing source/log/fixture, fix on main, use FAST for intermediate repair commits, and run FULL again ONLY on the repaired final implementation commit. Continue until the latest requested acceptance criteria and final FULL check pass within the active session. External tool/authentication/context limits require an exact resumable checkpoint (current main SHA, failed run/job, source cause, remaining tasks). No agent or GitHub Actions runner can continue forever or operate in the background after its session ends.
- Every write to main must be a non-force fast-forward based on the current remote head with an expected-SHA lease; concurrent changes must be reconciled before retry. Never claim success using an earlier commit's green workflow. A 3-4 minute FAST build is a performance target, not a promise for FULL emulator verification.

## Stop-the-line CI recovery: mandatory for every future mission
Every engineer and autonomous agent operating in this repository is strictly bound by this stop-the-line recovery contract. If an authorized implementation causes ANY required build, compilation, lint, test, documentation, backend, Firebase Functions, native-device, or final APK verification job to fail, do NOT abandon the failed feature, label it complete, start the next feature, or skip the failed gate. Diagnose the exact failure, perform a focused root-cause repair in the SAME active mission, rerun the relevant validations, and continue the repair/verification cycle until the required gate is green or a demonstrated external blocker makes completion impossible within the available tools/session. Only after the failed feature's required checks pass may you advance to the next authorized feature.

### A. Pre-flight / baseline
1. **Repository & Task Inspection:** Before starting any authorized feature or repair: read `ENGINEERING_STANDARD.md`, `SOURCE_OF_TRUTH.md`, and `AGENTS.md`. Inspect the current source, callers, tests, and resources. Fetch the live remote `main` branch HEAD, record its exact commit SHA, and inspect the current required CI status at that exact SHA.
2. **Baseline Status Classification:** Record whether the baseline is green, red, pending, skipped-by-design, or unknown. Never attribute a pre-existing failure to a new change without diff and log proof.
3. **Stop on Red Baseline:** If required baseline checks are red, STOP feature advancement immediately. Diagnose the first actionable failure. Repair it only where current owner authorization covers it; otherwise disclose the unrelated blocker and request explicit expanded scope. Do not silently modify an unrelated product area.
4. **Scoped Units of Change:** Plan one coherent feature at a time, with code, resources, Day/Night variants, callers, directly related tests, and license/security dependencies scoped together.

### B. Pre-commit validation
1. **Local Checks Where Supported:** Where the local environment supports it, execute: `git diff --check`; focused static/resource/compilation checks; Android app + unit-test + instrumentation-test SOURCE compilation (`:app:assembleDebug`, `:app:compileDebugUnitTestKotlin`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`); directly affected backend and Firebase Functions builds/tests; and relevant docs/governance checks (`node .github/scripts/verify-docs.mjs` and `python3 -B -m unittest discover -s .github/scripts/tests -v`).
2. **Exact Tooling Alignment:** Use the project's actual Gradle and tool configuration; inspect `build.yml` and Gradle files. Do not guess a command, Java version, class, SDK, dependency, resource ID, or imported method.
3. **Transparent Reporting for Limited Environments:** If local Android builds are impossible (for example, a limited Termux or API-only agent), do NOT claim local PASS. Mark the local check UNKNOWN; commit only a scoped, thoroughly inspected change, then use exact-SHA GitHub FAST validation as the remote gate and repair any resulting failure.
4. **Multi-Qualifier Impact Audit:** Changing visual or shared components requires checking all affected callers, themes, layout qualifiers, dynamic overrides, accessible states, RTL/font-scale, and corresponding tests; do not stop after one Kotlin or XML edit.

### C. Post-commit stop-the-line state machine
- **Formal States:**
  - Forward Flow: `NOT_STARTED` -> `IMPLEMENTING` -> `LOCAL_CHECKS` -> `COMMITTED` -> `CI_PENDING` -> `CI_GREEN` -> `NEXT_AUTHORIZED_FEATURE`
  - Recovery Loop: `CI_PENDING` -> `CI_RED` -> `ROOT_CAUSE_ANALYSIS` -> `TARGETED_REPAIR` -> `LOCAL_CHECKS` -> `COMMITTED` -> `CI_PENDING` (repeat until `CI_GREEN`)
  - Non-Pass Terminal States: `CI_UNKNOWN` and `EXTERNAL_BLOCKED`. Neither permits a PASS claim or feature advancement.
1. **Exact-SHA Inspection:** After EACH intermediate main commit, locate the exact commit SHA's FAST workflow run(s), then inspect all required jobs. Only a completed SUCCESS for that SHA is a green gate. Check successful compile, tests, backend, Functions, and policy jobs as applicable.
2. **Handling CI_PENDING:** While CI_PENDING, obtain completion and evidence using available foreground tools; do not silently advance. A skipped job is acceptable only when explicitly expected by the existing FAST/FULL job dependency or path filters, with the reason documented.
3. **Freeze on CI_RED:** When CI_RED, IMMEDIATELY FREEZE feature advancement. Do not start another product feature, call the broken change complete, run a cosmetic next commit to mask the failure, or publish an APK.
4. **Structured Failure Ledger:** Record failure category, exact SHA, run URL/ID, failing job/step, first actionable compiler/test/error message, file:line or assertion/fixture, and whether the cause was introduced by this commit.
5. **Root-Cause Investigation:** Inspect relevant complete logs and source, imports, actual API/SDK versions, call graph, XML/theme resources, affected unit/androidTest files, backend tests, CI scripts, and current diff. Fix the source/root cause and directly dependent tests in the SAME unfinished feature scope.
6. **Forward Repair Commit:** Run targeted local checks again where possible, then create a small forward-only repair commit on main. Recheck current remote HEAD before pushing. Verify its NEW exact-SHA FAST CI. Repeat until CI_GREEN; the previous failed run remains historical evidence, not a pass.
7. **Fault Tree Prioritization:** If multiple jobs fail, make a fault tree and repair the earliest causal failures first; avoid blind shotgun edits. Never infer that the last stack-trace line is the primary cause.
8. **Disciplined Retry Protocol:** Two unsuccessful repairs to an apparently identical error are NOT an automatic stop and NOT permission for infinite blind retries. Re-examine full raw logs, changed files, dependency graph, test fixtures and run environment, explicitly revise the hypothesis, add a focused regression test when useful, then use a materially different evidence-based repair. Stop only for a demonstrated blocker or unavoidable tool/session limit; leave a precise checkpoint.
9. **Advancement Gate:** After CI_GREEN, confirm directly affected functional criteria. Only then move to the next ALREADY AUTHORIZED feature. For UI work, FAST green does not prove on-device pixel/gesture fidelity; preserve G4/G5 and final FULL requirements.

### D. Failure category playbook — classify before repair
- **Kotlin/Java compilation:** unknown symbol/import, incorrect API, missing dependency, signature, type inference, nullability, visibility; inspect real version and callers.
- **Android resources/AAPT:** absent drawable/color/style/attr, qualifier mismatch, XML syntax, generated binding name; inspect every relevant variant.
- **JVM unit test / test-source compilation:** production contract vs stale test expectation vs real regression; update evidence-backed assertions, never blindly loosen or delete tests.
- **Android instrumentation SOURCE compilation:** check actual androidTest code separately from app compile. Passing assembleDebug is not enough.
- **Native/emulator UI:** distinguish assertion/behavior, wrong color/shape/dimension/animation, accessibility, timing/flakiness, and verified runner/infrastructure errors. Inspect screenshots, raw instrumentation output and real UI code; do not inflate delays to hide regressions.
- **Gradle/dependency/environment:** check dependency graph, SDK, Java/Gradle/AGP compatibility, access, cache, network; unchanged-code rerun is justified ONLY for evidence-backed transient failures.
- **Backend/Functions/security:** honor module boundaries, token validation and data integrity; debug first failing test or compilation step without weakening API protections.
- **Lint, licensing, signing, release/artifact:** correct policy violations rather than skipping gates; never publish a debug artifact as a verified release.
- **Governance/docs:** use Verify Documentation and governance tests; preserve expected canonical entrypoint and security checks.

### E. FULL CI is a terminal mission gate, not a routine retry loop
1. **FAST for Intermediates:** Every ordinary intermediate feature or repair commit must run FAST only. Do NOT add [eve-full] to governance updates, intermediate fixes, documentation changes, or incomplete missions. Do not invent automatic mid-mission FULL checkpoints.
2. **Terminal FULL Criteria:** Only when ALL owner-authorized mission requirements and applicable G4/G5 acceptance criteria are satisfied, use the existing mission-final [eve-full] trigger (or owner-authorized manual workflow_dispatch exception) to run six-job FULL: Android build/lint, backend, Firebase Functions, gesture native UI, three-button native UI, gated verified APK publication.
3. **Handling FULL Failure:** If terminal FULL fails, that same mission is INCOMPLETE. Freeze the next feature. Diagnose FULL logs and native artifacts, make focused forward-only repair commits with FAST on each intermediate SHA, then trigger a NEW mission-final FULL at the repaired final SHA only when eligible. Never treat an older successful FULL as proof for the new SHA.
4. **Artifact Integrity:** Verify each of the six required FULL jobs and the exact final SHA and APK checksum. A green FAST build, successful assembleDebug, or generic 'build passed' message is NOT final delivery.

### F. External blocker, resume, and honest reporting
1. **Honest Gate Classification:** If access is missing, owner credentials/authorization are needed, a security/legal/licensing blocker is demonstrated, GitHub API/CI is unavailable, or a hard session limit interrupts work: mark BLOCKED or UNKNOWN, do not pretend green, and do not move to the next feature.
2. **G7 Resumable Checkpoint:** Before halting, write a G7 exact resume checkpoint into the CURRENT mission's existing acceptance report (or a tightly scoped existing checkpoint if no mission report exists): remote HEAD SHA, last known-good SHA, active unfinished feature, commit and run IDs/URLs, first actionable error, evidence, previous attempted repairs, local uncommitted work, and the NEXT SINGLE SAFE ACTION. Never claim asynchronous continuation.
3. **Resumed Execution Protocol:** A resumed agent must inspect the remote state and checkpoint, resume the SAME first unfinished repair, and only advance when the gate becomes green; do not restart the mission or repeat completed work.
4. **Rigorous Reporting:** Report VERIFIED evidence (path:line / exact-SHA CI URL), INFERRED possibilities, and UNKNOWN/EXTERNAL_BLOCKED facts distinctly. Keep unrelated findings in a separate, impact-ranked section; do not silently repair them.

### G. Historical failure modes to prevent (examples, not current defect claims)
- A previous EVE FAST run had unresolved chipSurfaceColor / TelegramThemeEngine symbols after a UI palette edit: compile against actual classes and imports before pushing.
- A previous run failed because a new production layout changed a layout-count assertion: determine whether the new layout is intended, then update the full test contract with evidence.
- A previous FULL failed in compileDebugAndroidTestKotlin due to invalid if expression/type mismatch in ApprovedUiRenderingTest.kt: compile androidTest sources in FAST.
- A prior native run failed geometry, color, or dialog accessibility assertions: source compilation alone cannot confirm runtime UI acceptance.
Historical runs may already be repaired. Never revert unrelated code based on this list, or treat stale failure examples as current HEAD facts.

## Failure prevention and root-cause workflow (all agents, all future missions)
- Before changing CI or Gradle tasks, inspect **all** tests referencing the workflow, Gradle tasks, branch names or step names. Change source and contract checks atomically. Tests must enforce **behavior** (gates, dependencies, release safety) rather than brittle exact shell command strings or cosmetic step names. Run host Python checks before any build if tooling permits.
- Every FAST push must compile Android app, JVM test sources, and **Android instrumentation test sources**, and run JVM unit tests. Compiling tests before the costly final emulators catches source-compatibility failures without paying emulator startup cost. FULL still runs lint, real native UI tests in both navigation modes, theme evidence, backend checks and release-signing protection.
- CI failing means **an incomplete mission**. Read exact run SHA, first failing job, FIRST actionable Kotlin/Java/AAPT/JVM/native assertion/dependency or infra error, and affected caller/tests. Repair the root code/test contract rather than repeating builds, deleting legitimate assertions, increasing arbitrary timeouts, hiding exceptions, or disabling quality gates. Only rerun unchanged code when evidence identifies transient infrastructure failure.
- FULL native-emulator jobs depend on a successful Android build, backend and Firebase Functions check: do not waste emulator minutes after a failed prerequisite. Compile app + instrumentation APKs once in the FULL build, transfer by checksum-verified cache, run isolated gesture and three-button device tests without Gradle compilation. Native raw results must fail closed for crashes, skipped, incomplete and failing assertions. The internal candidate-APK cache key must be stable across GitHub rerun-failed-jobs attempts for the same workflow run and commit, and the restored APK must pass a SHA-256 check. Never republish an unverified candidate.
- CI log summaries are diagnostic hints, not automatic fixes. The implementing agent is responsible for investigation and source repair; GitHub Actions must never self-edit main, mark a red job green, or auto-publish an unverified APK.
- A repeat of the same failure class requires a regression test covering that failure. Preserve regression gates and protect the last known-good main SHA. For API-only agents, push minimal complete changes to main, inspect FAST/FULL outcome on **that exact SHA**, and issue safe forward fixes; never claim verification based on an older commit.
- No build system can guarantee a future change will never fail. The goal is early deterministic error detection, clear diagnosis, and rare repeat failures without suppressing genuine defects.

## Repository and verification
- **Owner's standing repository policy:** work and deliver directly on `main`; do not create separate implementation branches or PRs unless the owner explicitly asks. Keep changes small and reviewable, preserve the previous SHA as a restore point, compare the live remote head before each write, use only fast-forward updates with an expected SHA lease, and never force-push or overwrite concurrent work. If a main commit fails CI, diagnose and repair it on main rather than claiming success. Do not push when a task explicitly requests a proposal only.
- Before editing a UI role, enumerate XML/runtime call sites, theme qualifiers, resource references and affected Kotlin/JVM/native assertions. Make Day/Night tokens and test changes in the SAME change. For an icon, inspect its use-site semantics, dynamic setters, selectors, tint, viewport/density, touch/labels and any real source animation. Never replace an exam-specific icon with an unrelated messaging symbol.
- Prefer pre-push static/resource checks, Kotlin/application AND unit-test compilation, lint/unit tests and actual device evidence when local tools support them. With GitHub API-only writes, state that first remote CI is the validation gate and repair any red main commit with a fresh root-cause-based fast-forward commit. Release signing must fail closed; do not publish production binaries signed with a debug key.
- Capture the first actionable failure by SHA/run/job/type (Kotlin/Java compile, unit-test compile, AAPT/resource, lint, JVM assertion, native UI/theme/geometry, backend, dependency, infrastructure, signing). Trace broken source AND its test contract; distinguish source-defined theme values from stale hard-coded test colors. Never change expectations without source/theme evidence, suppress tests, enable continue-on-error or bypass the required artifact gate.
- Keep both native gesture and three-button suites and their Light/Dark and font-scale evidence. Publish debug APK only when build, both native suites, backend and Firebase Functions gates all pass. A green build does not prove exact Telegram art or full runtime fidelity.
- Run checks appropriate to touched layers: Android build/unit/lint/native UI when affected, backend tests/build when affected, and docs/workflow validations when affected. Check `git diff --check` where a local checkout is available.
- Treat test output and CI status as evidence only for the exact commit and tests actually inspected. If a check cannot run, say UNKNOWN, not PASS.
- Report changed files, what was implemented, evidence, any remaining limits, and unrelated findings separately. Mark key claims VERIFIED (file:line or command output), INFERRED, or UNKNOWN.

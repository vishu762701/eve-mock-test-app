# EVE Engineering Standard

Read this file and `SOURCE_OF_TRUTH.md` before changing the repository. Inspect relevant code and the current task; never treat historical tickets, reports, or old agent conversations as an active request.

## Instruction priority
1. Follow the latest explicit user-approved task for its requested product outcome and scope. A later request can deliberately replace older UI choices; do not reimpose historical numeric design decisions.
2. Preserve security, privacy, data integrity, and existing functionality unless the task explicitly authorizes a safe change. Flag destructive or unsafe requests and seek clarification when necessary.
3. Use `SOURCE_OF_TRUTH.md` for current architecture and default approved design choices; use `APPROVED_UI_IMPLEMENTATION_AUDIT.md` for the current UI baseline. These are context, not permission to ignore a newer explicit task.
4. `AGENTS.md` is the common agent entrypoint. `GEMINI.md` and `CLAUDE.md` only point to it; none may override the user's explicit scope.

## Execute, do not just report
- Determine the real objective and concrete acceptance criteria. For a coding request, make the requested implementation changes, not merely an audit or a new prompt, unless the user specifically asks for analysis only.
- Inspect the actual implementation, callers, resources, themes, tests, backend and workflows relevant to the change. Resolve conflicts with existing code rather than inventing dependencies.
- Make the smallest complete change. Update all directly affected Day/Night variants, reusable components, and tests; preserve unrelated screens and behavior.
- Challenge a risky or contradictory requirement briefly, with evidence. Do not silently replace a safe user choice with your own aesthetic preference or an older rule.
- Protected by default: Telegram Day/Night circular reveal and theme switching; existing Lottie assets, playback and triggers; server-authoritative scoring, authorization, and answer visibility. Modifications need explicit scoped approval.
- Never commit credentials, bypass authorization, disable failing gates, fake evidence, or change production data just to satisfy a check.
- Avoid unrelated refactors, broad rewrites, unnecessary dependencies and **blind** retries. After two unsuccessful attempts at the same error, inspect full logs, fixture, dependency and diff evidence, then make a materially different root-cause repair; keep working within available tools/time. Only stop for a demonstrated external blocker, never simply because CI was red twice.

## Repository and verification
- **Owner's standing repository policy:** work and deliver directly on `main`; do not create separate implementation branches or PRs unless the owner explicitly asks. Keep changes small and reviewable, preserve the previous SHA as a restore point, compare the live remote head before each write, use only fast-forward updates with an expected SHA lease, and never force-push or overwrite concurrent work. If a main commit fails CI, diagnose and repair it on main rather than claiming success. Do not push when a task explicitly requests a proposal only.
- Before editing a UI role, enumerate XML/runtime call sites, theme qualifiers, resource references and affected Kotlin/JVM/native assertions. Make Day/Night tokens and test changes in the SAME change. For an icon, inspect its use-site semantics, dynamic setters, selectors, tint, viewport/density, touch/labels and any real source animation. Never replace an exam-specific icon with an unrelated messaging symbol.
- Prefer pre-push static/resource checks, Kotlin/application AND unit-test compilation, lint/unit tests and actual device evidence when local tools support them. With GitHub API-only writes, state that first remote CI is the validation gate and repair any red main commit with a fresh root-cause-based fast-forward commit. Release signing must fail closed; do not publish production binaries signed with a debug key.
- Capture the first actionable failure by SHA/run/job/type (Kotlin/Java compile, unit-test compile, AAPT/resource, lint, JVM assertion, native UI/theme/geometry, backend, dependency, infrastructure, signing). Trace broken source AND its test contract; distinguish source-defined theme values from stale hard-coded test colors. Never change expectations without source/theme evidence, suppress tests, enable continue-on-error or bypass the required artifact gate.
- Keep both native gesture and three-button suites and their Light/Dark and font-scale evidence. Publish debug APK only when build, both native suites, backend and Firebase Functions gates all pass. A green build does not prove exact Telegram art or full runtime fidelity.
- Run checks appropriate to touched layers: Android build/unit/lint/native UI when affected, backend tests/build when affected, and docs/workflow validations when affected. Check `git diff --check` where a local checkout is available.
- Treat test output and CI status as evidence only for the exact commit and tests actually inspected. If a check cannot run, say UNKNOWN, not PASS.
- Report changed files, what was implemented, evidence, any remaining limits, and unrelated findings separately. Mark key claims VERIFIED (file:line or command output), INFERRED, or UNKNOWN.

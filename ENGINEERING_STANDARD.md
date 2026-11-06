# EVE Engineering Standard — lean execution policy

This is the single workflow policy. The latest explicit owner request defines scope; SOURCE_OF_TRUTH.md describes architecture and permanent security/data boundaries. Historical mission prompts, audit reports, checkpoint files and templates are not execution mandates.

## 1. Execute the owner's actual outcome
- Implement authorized results end-to-end, including impacted Android resources, Day/Night, RTL, accessibility, state, backend and tests as relevant. Decide routine engineering details independently. Do not stop for repeated approvals or finish with an audit-only report.
- Preserve unrelated working behavior. Do not invent requirements, "EXACT" fidelity, CI results or background execution. For changed files, use actual code/test evidence. Report uncertainty as UNKNOWN and blocked external acceptance as BLOCKED; continue independent work.
- No mandatory per-feature R1–R99 document, heavyweight ledger, duplicate source map, audit-only milestone or checkpoint-only commit. Keep a short working task list internally. At a real interruption, save one concise resume record with HEAD, local work, failing gate and next action.
- Search for the existing owner of affected production state, navigation or mutations before introducing another. Remove proven obsolete code/resources with caller and compatibility checks. Keep required migrations, licenses and public contracts.
- Mark implementation present separately from functional, visual/gesture, device and release acceptance. A green build does not prove exact UI/UX. A lack of physical hardware is not a reason to stop unrelated coding.

## 2. Fast source and review decisions
- Use official Telegram source when the requested outcome depends on Telegram source. Authenticate the revision, pin its full commit SHA, and inspect the minimum complete functional subsystem (including required resources, callers and motion). Do not repeat source authentication for ordinary EVE fixes.
- Reuse verified upstream atoms where lawful. Classify source claims EXACT, ADAPTED, NOT FOUND or BLOCKED with concrete provenance. An adapter is not unchanged upstream code. Do not import Telegram account credentials, messaging, network or unrelated services.
- Preserve relevant attribution, third-party notices and license evidence. Avoid duplicate documentation; update the existing source/ownership record only when its facts materially change.

## 3. Commit, build and delivery
- main only, fast-forward batch, one writer. There is no numeric commit ceiling. No PR/branch detours, force push, destructive resets or history rewrite. Batch related implementation and test fixes into coherent commits; do not create a new checkpoint/report commit after every task. Reconcile remote main before push.
- Run smallest relevant local tests first. Each pushed implementation batch must pass exact-SHA FAST CI on its final SHA. Diagnose first real error and repair forward; do not re-run the same failing build repeatedly without a cause. Treat emulator/ADB/GPU infrastructure errors separately from application test failures.
- FULL native emulator testing is a final integrated-candidate gate, not a tax on small commits. Test both gesture and three-button navigation and verify built APK SHA-256. Publish an APK only through a separately authorized and license-cleared delivery path; never describe an unavailable artifact as delivered.
- Do not run expensive Gradle/NDK/emulator builds on a phone by default when equivalent hosted CI is available. Keep Termux work to source operations and lightweight checks unless the owner explicitly chooses a phone build.

## 4. Security and release protections
- Keep Firebase identity, Worker/D1 admin authority, answer secrecy, server-side scoring, account isolation, data compatibility, signing and secrets protected. Test relevant production request/response paths when modifying them.
- Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions require separate explicit owner authorization. Keep release compliance gates and manual production workflow intact. Do not bypass tests or permissions to force a green check.
- If a real owner-only product, security, data or legal decision is required, state the exact blocker and keep progressing on unrelated authorized work.

## 5. Minimal reporting
- During execution: report only material failures or decisions. At delivery: changed behavior, exact commit SHA, relevant tests/FAST/FULL results, APK access and unresolved external blockers. Do not keep re-reading or rewriting multi-thousand-line mission reports for ordinary fixes.

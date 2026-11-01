# Telegram integration process

At the start of each source-related task identify the latest suitable official revision and pin its full commit SHA; a previous archive does not establish latest. An authenticated official `DrKLO/Telegram` Git checkout pinned by full commit SHA is an acceptable source, as is an independently verified archive. Record how origin and revision were authenticated; a local Git SHA or remote URL alone does not establish authenticity. For an archive, record its actual SHA-256, integrity, identity, and manifest result when available. Missing dependencies are NOT FOUND or BLOCKED, never invented.

Before choosing an import boundary, trace the actual upstream entry point, transitive classes, resources and qualifiers, themes, assets, state, lifecycle, input dispatch, animation and platform dependencies. Record each edge and its disposition (included, EVE adapter, excluded with reason, or unresolved). Explicitly identify account, network, credential and unrelated messaging dependencies that must not enter EVE. A dependency list is not proof of completeness; inspect call sites and runtime paths and keep unresolved branches visible.

Keep verbatim imports unchanged where possible, with upstream path, revision, digest, copyright and applicable license. Isolate EVE-specific host and business wiring; document unavoidable changes separately. Assess combined distribution obligations using `telegram-release-compliance-gate.md`; do not interpret a checksum as legal clearance.

For each property and integration path record upstream file:line and revision → dependency edges → EVE file:line → production-path tests and runtime comparison → EXACT, ADAPTED, NOT FOUND or BLOCKED, with evidence and limitations. EXACT requires the actual unchanged upstream implementation (hash or diff verified) in the stated scope; modified code is ADAPTED even if believed equivalent. EXACT is not a label for the whole component merely because one asset matches. Compare actual behavior, cancellation, gestures, measurement, lifecycle, Day/Night, RTL, insets and accessibility where relevant. Green compilation and static screenshots are insufficient on their own.

## Gated workflow for source-exact tasks

Run these phases in order for each screen or feature. Do not start a later phase before the earlier one is finished. Phases A and B may be presented in one reply, but implementation waits for owner approval.

**Phase A: Analysis (no code, no commits).** Pin the upstream full commit SHA. List the upstream files you actually opened, with path and line ranges. Describe layout structure, spacing, colors, fonts, assets, states, and every user action (tap, long press, swipe, scroll, search) with what each does. Mark each item UI-only, needs local logic, or needs Telegram backend (cannot be copied). Anything not opened or not found is NOT FOUND or UNKNOWN. Do not guess.

**Phase B: Mapping (no code, no commits).** For each upstream item give the EVE file to change or create, the dependency disposition (included, EVE adapter, excluded with reason, or unresolved), a proposed Firebase/Worker-compatible adaptation for every backend-dependent function, and the license/provenance note per `THIRD_PARTY_NOTICES.md`. Present the plan and wait for explicit owner approval. Any change to the plan after approval needs new approval.

**Phase C: Implementation.** Change only files named in the approved Phase B. Work in small locally verified pieces, UI first, then functions; these pieces are not separate commits (commit limits stay as in `ENGINEERING_STANDARD.md`). Do not fake backend features.

**Final report.** One parity table row per element or function: upstream path:lines@SHA | EVE file:lines | EXACT / ADAPTED (reason) / NOT FOUND / BLOCKED, followed by the requirement ledger result. Never call the result exact unless every row is EXACT.

## Byte verification for recorded assets

`python3 .github/scripts/verify_telegram_imports.py --upstream /path/to/authenticated/telegram` compares the recorded upstream Git objects and EVE files with their recorded SHA-256 values. It requires the checkout HEAD to match the full revision. `docs/telegram-verbatim-imports.json` currently covers only four popup divider assets; it is not a complete inventory. The verifier does not authenticate the checkout, discover dependencies, assess license or prove runtime behavior. Its regression tests execute real Git fixtures in the normal CI host-test suite.

## Fail-closed acceptance claims for Profile and QR

The tracked scope contract is `docs/telegram-profile-fidelity.json`; CI runs
`python3 .github/scripts/verify_telegram_acceptance.py` on every main push.
The targets `profile-shell`, `profile-overflow` and `personal-qr` are
currently OPEN: compile success or passing emulator tests cannot change this.
During source-exact work, update the dependency/source map and list unresolved
subsystems instead of renaming adapted pieces EXACT. Run
`python3 .github/scripts/verify_telegram_acceptance.py --require-accepted personal-qr`
(or the relevant target) before reporting that target source-exact complete.
This *must fail* until a complete native reference screenshot, matched EVE
capture, motion trace, native test report, comparison report with actual human
review and licensing disposition exist and are SHA-256-pinned. Evidence must be
permission-cleared and anonymized before committing; never commit real phone
numbers, usernames, photos, auth data or private screenshots.

This validator checks evidence existence/checksums, source-map linkage and
claim consistency. It **does not** inspect the original Telegram executable,
automatically compare pixels/frames or certify animation, accessibility, source
completeness or GPL compatibility. An ACCEPTED claim still requires actually
reviewed frames and real-device behavior on matched device/build/viewport,
including Day/Night, RTL, font scale, inset/cutout and interruptions. The
`OPEN` contract passing CI is deliberately NOT an accepted Telegram UI.

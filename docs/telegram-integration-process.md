# Telegram integration process

At the start of each source-exact mission authenticate and pin a suitable official revision by full commit SHA; reuse this verified pin for subsequent tasks in that mission unless a relevant upstream change or owner requirement justifies repinning. A previous archive does not establish latest. An authenticated official `DrKLO/Telegram` Git checkout pinned by full commit SHA is an acceptable source, as is an independently verified archive. Record how origin and revision were authenticated; a local Git SHA or remote URL alone does not establish authenticity. For an archive, record its actual SHA-256, integrity, identity, and manifest result when available. Missing dependencies are NOT FOUND or BLOCKED, never invented.

Before choosing an import boundary, trace the actual upstream entry point, transitive classes, resources and qualifiers, themes, assets, state, lifecycle, input dispatch, animation and platform dependencies. Record each edge and its disposition (included, EVE adapter, excluded with reason, or unresolved). Explicitly identify account, network, credential and unrelated messaging dependencies that must not enter EVE. A dependency list is not proof of completeness; inspect call sites and runtime paths and keep unresolved branches visible.

Keep verbatim imports unchanged where possible, with upstream path, revision, digest, copyright and applicable license. Isolate EVE-specific host and business wiring; document unavoidable changes separately. Assess combined distribution obligations using `telegram-release-compliance-gate.md`; do not interpret a checksum as legal clearance.

For each property and integration path record upstream file:line and revision → dependency edges → EVE file:line → production-path tests and runtime comparison → EXACT, ADAPTED, NOT FOUND or BLOCKED, with evidence and limitations. EXACT requires the actual unchanged upstream implementation (hash or diff verified) in the stated scope; modified code is ADAPTED even if believed equivalent. EXACT is not a label for the whole component merely because one asset matches. Compare actual behavior, cancellation, gestures, measurement, lifecycle, Day/Night, RTL, insets and accessibility where relevant. Green compilation and static screenshots are insufficient on their own.

## Continuous workflow for an already-authorized source-exact task

**Owner authorization is the feature requirement itself.** Do not request a second approval for the same scoped task. Complete these phases in order inside one execution; keep the source map and checkpoint current. A genuinely new product/UX choice, security or data change, licensing exception or production deployment still requires an explicit owner decision. A missing source file or failing build blocks the affected claim, not unrelated authorized work.

**Phase A: Analysis.** Identify the latest suitable official upstream revision and pin its full commit SHA. Inspect actual upstream entry points, caller graph, source files, resources, motion and behavior. Record which files and line ranges were actually opened and which features depend on Telegram accounts or servers and cannot be transplanted. Mark unknown evidence UNKNOWN.

**Phase B: Mapping.** Trace each source element into its EVE integration path, dependencies, themes and resources. Classify source code and adaptations EXACT, ADAPTED, NOT FOUND or BLOCKED. Keep EVE authentication, admin permissions, scoring and server APIs isolated. Record copyright, license, origin and distribution implications before importing.

**Phase C: Implementation.** Within the authorized scope, implement the mapped subsystem and directly required integrations, resources and tests. No arbitrary UI lookalikes, invented animation parameters or cosmetic-only ports presented as source-exact. Preserve unrelated EVE flows. Resolve compile/runtime failures before starting another feature. Use coherent multi-commit batches without a numeric per-feature limit; test the assembled work and require exact-SHA FAST on the pushed batch tip. CI-failed repairs are forward-only.

**Phase D: Verification.** Run local and exact-SHA CI gates on actual code paths, then emulator/device comparisons for pixels, physics, cancellation, lifecycle, Day/Night, RTL, insets, accessibility and state restoration. If tooling is unavailable, mark it BLOCKED and continue only independent work. Never treat Python tests or a compiling APK as proof of 100% source parity.

**Final report.** Per-element parity ledger: upstream path:lines@SHA | EVE file:lines | EXACT / ADAPTED (reason) / NOT FOUND / BLOCKED, followed by MET / NOT MET / BLOCKED for every requested requirement. Record precise test commands, CI run, omissions, scope decisions and the first unfinished requirement.

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

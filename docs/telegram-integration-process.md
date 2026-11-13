# Telegram integration process

Deep procedure behind `ENGINEERING_STANDARD.md` section 2. If the two differ, `ENGINEERING_STANDARD.md` wins.

## 1. Source
At the start of each source-exact task authenticate and pin a suitable official revision of `DrKLO/Telegram` by full commit SHA; reuse this pin for the rest of the task unless an upstream change or owner requirement justifies repinning. A previous archive does not establish latest. An authenticated official Git checkout pinned by full commit SHA is acceptable, as is an independently verified archive. Record how origin and revision were authenticated; a local Git SHA or remote URL alone does not establish authenticity. For an archive, record its actual SHA-256, identity and manifest result. Missing dependencies are NOT FOUND or BLOCKED, never invented.

## 2. Import boundary
Before choosing a boundary, trace the actual upstream entry point, transitive classes, resources and qualifiers, themes, assets, state, lifecycle, input dispatch, animation and platform dependencies. Give each edge a disposition: included, EVE adapter, excluded with reason, or unresolved. Account, network, credential and unrelated messaging dependencies never enter EVE. A dependency list is not proof of completeness; inspect call sites and runtime paths.

## 3. Import rules
Keep verbatim imports unchanged where possible, with upstream path, revision, digest, copyright and applicable license. Isolate EVE-specific host and business wiring. Assess combined distribution obligations using `telegram-release-compliance-gate.md`; a checksum is not legal clearance.

## 4. Evidence per item
upstream file:line and revision → dependency edges → EVE file:line → tests and runtime comparison → EXACT, ADAPTED, NOT FOUND or BLOCKED, with limitations. EXACT requires the actual unchanged upstream implementation (hash or diff verified) in the stated scope; modified code is ADAPTED even if believed equivalent. Compare real behavior, cancellation, gestures, measurement, lifecycle, Day/Night, RTL, insets and accessibility where relevant. Green compilation and static screenshots are insufficient on their own.

## 5. Phases inside one execution
Owner authorization is the feature requirement itself. Do not request a second approval for the same scoped task. A genuinely new product choice, security or data change, licensing exception or production deployment still needs an explicit owner decision. A missing source file or failing build blocks the affected claim, not unrelated authorized work.
- **A. Analysis:** pin the revision, open the real entry points, callers, resources and motion; note which files and line ranges were actually opened; mark unknown evidence UNKNOWN.
- **B. Mapping:** trace each source element into its EVE path, dependencies, themes and resources; classify EXACT / ADAPTED / NOT FOUND / BLOCKED; keep EVE auth, admin permissions, scoring and server APIs isolated.
- **C. Implementation:** implement the mapped subsystem and its required integrations, resources and tests. No lookalikes, invented animation parameters or cosmetic ports presented as source-exact. Fix compile/runtime failures before starting another feature.
- **D. Verification:** local checks, exact-SHA FAST, then device comparison for pixels, physics, cancellation, lifecycle, Day/Night, RTL, insets, accessibility and state restoration. If tooling is unavailable mark BLOCKED and continue independent work. Never treat Python tests or a compiling APK as proof of 100% source parity.

Final report (in chat, not a repo file): per element `upstream path:lines@SHA | EVE file:lines | EXACT / ADAPTED (reason) / NOT FOUND / BLOCKED`, then MET / NOT MET / BLOCKED per requested requirement, test commands, CI run, omissions and the first unfinished requirement.

## Byte verification for recorded assets

`python3 .github/scripts/verify_telegram_imports.py --upstream /path/to/authenticated/telegram` compares the recorded upstream Git objects and EVE files with their recorded SHA-256 values. It requires the checkout HEAD to match the full revision. `docs/telegram-verbatim-imports.json` currently covers sixteen byte-identical assets (popup dividers plus the profile photo-menu camera/gallery/delete payloads); it is not a complete inventory. The verifier does not authenticate the checkout, discover dependencies, assess license or prove runtime behavior. Its regression tests execute real Git fixtures in the normal CI host-test suite.

## Fail-closed acceptance claims for Profile and QR

The tracked scope contract is `docs/telegram-profile-fidelity.json`; the manually triggered FULL CI runs
`python3 .github/scripts/verify_telegram_acceptance.py`; ordinary pushes compile only.
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

# EVE — Mission 06 Source-Fidelity Evidence Ledger

## Entry baseline (2026-10-28)

- **VERIFIED — remote main at inspection:** `8fe6b247033bc3f8981c68c00fa93c3849ce6091` (GitHub branch endpoint). The preceding FAST Build Eve APK run `40616134606` succeeded. This is **not** a Mission 06 run.
- **VERIFIED — owner-provided source attachment:** `Telegram-master (1).zip`, 115,700,719 bytes; SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; 14,794 ZIP entries; Python `ZipFile.testzip()` returned `None` (CRC integrity check succeeded).
- **BLOCKED — original-source identity:** attachment hash differs from mandatory complete-source hash `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3` and extract hash `b5bbfb7f78ec76769c888d30eb58831845e809b11ab413c722c3646a30be161e`. No exact Telegram renderer, asset or physics transplant is authorized from this differing attachment. ZIP integrity does not establish source identity or license clearance.
- **VERIFIED — GitHub connectivity:** connected repository has read/push permission; branch is `main`; no branch/PR is requested.
- **Licensing:** existing `THIRD_PARTY_NOTICES.md` documents bundled upstream GNU GPL v2 and unresolved per-item license/release questions. Do not alter release safeguards or infer license clearance from an archive hash.

## Implementation ledger

| Microstep | Scope | EVE evidence | Telegram evidence | Status | Commit and CI |
|---|---|---|---|---|---|
| 00 | Remote HEAD, rules, uploaded ZIP integrity, relevant code inspected | `ENGINEERING_STANDARD.md`, `AGENTS.md`, `SOURCE_OF_TRUTH.md`; `activity_main.xml:49`, `EveDesignSystem.kt:50-53` | ZIP identity differs; no source-exact transplant | VERIFIED baseline / SOURCE BLOCKED | Read-only |
| 01 | Original shared primitives | No changes | Expected SHA mismatch | BLOCKED | None |
| 02 | Home toolbar scroll seam | `DesignGlassLayout.kt:144-151` draws an explicit full-rect hairline while `glassEnabled`; `EveGlassToolbar.kt:40-65` enables glass during scroll. Home alone now sets `homeGlassToolbar.drawHairline = false` in `MainActivity.setupTopBarDepth`; shared toolbar code untouched. | No new Telegram renderer or guessed physics; existing EVE frame stroke removed | IMPLEMENTED, actual scroll pixel comparison NOT VERIFIED | SHA-specific FAST pending |
| 03 | Home search lifecycle safety, Telegram-exact renderer/physics | `MainActivity.openSearch/closeSearch` now guard repeated calls, cancel pending ViewPropertyAnimator transitions, prevent cancelled morph callbacks from resetting the wrong state, cancel stale debounce and clean up at destruction. Original existing `UiArcMotion` duration/easing still unchanged. | Source-exact presentation and physics remain BLOCKED by source SHA mismatch | SAFETY ADAPTATION IMPLEMENTED / SOURCE-EXACT BLOCKED / native NOT VERIFIED | SHA-specific FAST pending |
| 04 | Category single-selection functional guard | `activity_main.xml:49` now requires selection; `EveDesignSystemTest.kt` XML contract; `HomeCategorySelectionNativeTest.kt` on-device repeated-tap test | No Telegram styling/physics changed | IMPLEMENTED, runtime NOT YET VERIFIED | \`e996dcc9d920a714fe21dadec287faff29e65e43\`; FAST [#40620365549](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40620365549) **SUCCESS** |
| 09 | Owner's drawer-only pure-black Night background | `MainActivity.prepareDrawerSurface()`, `navDrawerPanel` and drawer-only `home_drawer_background` resource; retained existing corner token and six rows | Owner override, explicitly **not** Telegram Night's original drawer surface | IMPLEMENTED, runtime NOT YET VERIFIED | Initial FAST [#40620882198](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40620882198) FAILED: 420 JVM tests, 1 stale shared-surface assertion in `MasterRedesignContinuationTest.kt:47`. Forward test-contract repair pending FAST |
| 05–08, 10–15 | Source look, cards, popup, drawer motion, alerts, physics, integration and release | Unchanged by this step | Exact comparisons pending accepted source and validation | PENDING / SOURCE BLOCKED where necessary | None |

## Scope and evidence constraints

The Microstep 04 change affects only the Home filter's state contract, not Day/Night palette, icon assets, motion, dimensions, data source, Firebase/backend, or category click destinations. Android `ChipGroup` single-selection alone permits deselection; `selectionRequired` requests the invariant at the widget layer. The existing `HomeViewModel.buildUiData()` falls back visually to All when a category disappears; persistent reappearance behavior still needs integration verification. Instrumentation test source compilation is checked by FAST, but its actual tap behavior requires the final native FULL gate. Any prior CI green state cannot prove this new code.

## Source archive extra checks

- ZIP root `LICENSE` SHA-256 `8177f97513213526df2cf6184d8ff986c675afb514d4e68a404010521b880643` (GNU GPL version 2 text); no `EVE_MANIFEST.sha256` entry is present in the uploaded archive. CRC validity is not a source-authenticity signature.
- Scroll seam: source-level `drawHairline` path removed only for Home; no emulator/frame screenshot currently establishes zero pixels of visual discontinuity.

## Remaining quality gates

Current SHA-specific FAST, runtime native gesture/three-button, source renderer/property mapping, visual frame comparison, physical Samsung device and mission-final FULL/APK: **NOT VERIFIED** until each has actual evidence. Owner night drawer #000000 is an **implemented owner override** with Day/Night resource-contract assertions; final native pixel verification is still pending. This black background is not claimed as Telegram's own Night drawer color.

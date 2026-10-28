# EVE — Mission 06 Source-Fidelity Evidence Ledger

## Entry baseline (2026-10-28)

- **VERIFIED — remote main at inspection:** `8fe6b247033bc3f8981c68c00fa93c3849ce6091` (GitHub branch endpoint). The preceding FAST Build Eve APK run `40616134606` succeeded. This is **not** a Mission 06 run.
- **VERIFIED — owner-provided source attachment:** `Telegram-master (1).zip`, 115,700,719 bytes; SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; 14,794 ZIP entries; Python `ZipFile.testzip()` returned `None` (CRC integrity check succeeded).
- **BLOCKED — original-source identity:** attachment hash differs from mandatory complete-source hash `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3` and extract hash `b5bbfb7f78ec76769c888d30eb58831845e809b11ab413c722c3646a30be161e`. No exact Telegram renderer, asset or physics transplant is authorized from this differing attachment. ZIP integrity does not establish source identity or license clearance.
- **VERIFIED — GitHub connectivity:** connected repository has read/push permission; branch is `main`; no branch/PR is requested.
- **Licensing:** existing `THIRD_PARTY_NOTICES.md` documents bundled upstream GNU GPL v2 and unresolved per-item license/release questions. Do not alter release safeguards or infer license clearance from an archive hash.

## Implementation ledger — remote main evidence

| Step | Scope and source mapping | Implementation status | Commit / SHA-specific CI |
|---|---|---|---|
| 00 | Main rules, head, notices, historical Mission 05 artifacts, relevant Home code and ZIP CRC/hash inspected. No authenticated matching original renderer/source line map available. | VERIFIED read-only baseline; source identity BLOCKED | Baseline `8fe6b247`; older FAST `40616134606` is historical only |
| 01 | Original Telegram shared renderers, dependencies, per-asset SHA/physics tables | BLOCKED — input SHA differs from accepted complete archive | No source transplant/commit |
| 02 | Home-only toolbar perimeter stroke: `DesignGlassLayout.kt:144-151` renders a round-rect stroke; scroll-managed `EveGlassToolbar.kt:40-65` enables glass. `activity_main.xml:7` identifies this instance; `MainActivity.kt:1138-1144` disables only its perimeter hairline. | IMPLEMENTED EVE-local defect repair; zero-seam pixel acceptance NOT VERIFIED | `601d775f65a6cbd192b43ce4cb3b64798ccbc6ab` — FAST [#40621762592](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40621762592) SUCCESS |
| 03 | `MainActivity.kt:562-739` search guards repeated transitions, cancels running ViewPropertyAnimators/debounce, and ignores cancelled `ValueAnimator.onAnimationEnd` callbacks. No changes to existing `UiArcMotion.Spring.MORPH` or `SMOOTH` values. | Lifecycle safety ADAPTED; original Telegram renderer/IME/motion SOURCE BLOCKED; native NOT TESTED | `7b2f3d9a02b1b0497ac4fff7d3281e721abd2291` — FAST [#40622312166](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40622312166) SUCCESS |
| 04 | `activity_main.xml:49` sets `selectionRequired=true` with existing singleSelection. `EveDesignSystemTest.kt:218+` JVM contract and `HomeCategorySelectionNativeTest.kt` repeated-touch case. | EVE functional selection invariant IMPLEMENTED; selected-icon width and Telegram visual physics PENDING; native execution NOT TESTED | `e996dcc9d920a714fe21dadec287faff29e65e43` — FAST [#40620365549](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40620365549) SUCCESS |
| 05 | Telegram original filter-tab appearance/selection physics, actual upstream calls | SOURCE BLOCKED; no substitute renderer invented | No commit |
| 06 | Telegram original card/list atoms with EVE exam arrangement, overlapping shadows, motion | SOURCE BLOCKED; current `item_exam.xml`/`DesignCardView` not claimed exact | No commit |
| 07 | Telegram `ActionBarPopupWindow` full renderer plus seven EVE actions | SOURCE BLOCKED. Historical authentic icon assets retained; current glass popup remains ADAPTED | No commit |
| 08 | Telegram `DrawerLayoutContainer` drawer gesture/contour physics | SOURCE BLOCKED. Existing AndroidX DrawerLayout and 6 destinations unchanged | No commit |
| 09 | Owner override Night Home drawer `#FF000000`, scoped only to drawer. `values*/design_colors.xml:23`, `design_home_drawer_panel.xml`, `activity_main.xml:73-75`, `MainActivity.kt:1332-1337`. Day retains `@color/surface`. Six destinations and original icon ink roles preserved. | IMPLEMENTED OWNER OVERRIDE, not Telegram source-exact; native Night pixel check NOT TESTED | `3229ee1b7bb4de22ab58d4c41bd2860ab936b2f5` — FAST [#40620882198](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40620882198) FAILED (420 JVM tests, 1 stale legacy assertion `MasterRedesignContinuationTest.kt:47`); repair `dd53040e8677c0019bc537d0a0389f0cdc707b77` — FAST [#40621233645](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40621233645) SUCCESS |
| 10 | Original Telegram AlertDialog, DarkAlertDialog and relevant BottomSheet dependency-complete renderer | SOURCE BLOCKED; no app confirmation migrated | No commit |
| 11+ | App-owned test/session, account/security and admin dialog family adoption, once-only semantics | PENDING due upstream alert-source mismatch; current scoring/auth preserved by noninterference | No commit |
| 12 | Integrated cancellation/IME/insets, popup/drawer/alerts and Telegram physics diff | PARTIAL independent search cancellation only; source physics/frames/native verification BLOCKED | Included in step 03; no independent integration commit |
| 13 | Obsolete resources, caller cleanup, GPL provenance | PENDING. No unproved deletions; Mission 06 report added. Notices/release gate unchanged | This ledger/report edit only |
| 14 | Production/backend/functions, app unit/instrumentation source compilation, acceptance matrix | FAST JVM/build/backend/functions per referenced commits SUCCESS; complete source fidelity/native verification PENDING | FAST links above |
| 15 | `[eve-full]` mission-final 6-job native gesture/three-button and gated verified `Eve-debug-apk` | **NOT RUN**: source-exact requirements materially BLOCKED. No new FULL run and no Mission 06 published APK. | None |

**Statuses:** VERIFIED refers to GitHub source/CI or exact ZIP bytes inspected in this session. ADAPTED is an EVE-specific behavior change, not an identical Telegram implementation. BLOCKED means no exact-source substitution has been made. Successful FAST verifies gates it actually ran; its skipped Native UI/Publish jobs are not passes.

## Scope and evidence constraints

The Microstep 04 change affects only the Home filter's state contract, not Day/Night palette, icon assets, motion, dimensions, data source, Firebase/backend, or category click destinations. Android `ChipGroup` single-selection alone permits deselection; `selectionRequired` requests the invariant at the widget layer. The existing `HomeViewModel.buildUiData()` falls back visually to All when a category disappears; persistent reappearance behavior still needs integration verification. Instrumentation test source compilation is checked by FAST, but its actual tap behavior requires the final native FULL gate. Any prior CI green state cannot prove this new code.

## Source-specific and integration evidence

| Component | Original Telegram evidence and physics property | EVE state / deviations | Unverified acceptance |
|---|---|---|---|
| Home 3-dot popup | `ActionBarPopupWindow.java`, `ActionBarMenuSubItem.java`, 9-patches and true callers require accepted original bytes; no validated origin-line/asset hash for this attachment | Existing `TelegramMenuPopup` plus historical icon imports remain ADAPTED; seven actions preserved by no modification | Exact shape, clipping, blur, scale/pivot, rapid tap, RTL, 320dp |
| Confirmations | `AlertDialog.java`, `DarkAlertDialog.java`, `BottomSheet.java` and per-variant callers not accepted from differing archive | Existing EVE dialogs untouched | Original variants/avatar/action layout, confirm semantics, long Hindi text, backend scoring |
| Left Home drawer | `DrawerLayoutContainer.java` complete gesture/draw/caller chain not accepted from differing ZIP | AndroidX DrawerLayout existing 6 actions; owner-only Night fill #000000 is deliberate deviation | Gesture velocity, nav region/pill, corner geometry, rotation/font scale/RTL, exact shadow |
| Home search | Original ActionBar search/field call chain, easing constants and focus timing not verified | Existing UiArc easing retained, cancellation race mitigated. `searchTopBar` still has `match_parent` height under wrap-content parent — investigate layout as potential mismatch; no arbitrary change. | IME first frame, screen bounds, close/open/rotate, theme reveal, frame-by-frame motion |
| Filter pills | Actual `FilterTabsView`/other caller match not verified; no original constants claimed | `selectionRequired` ensures no user re-tap uncheck at ChipGroup level; EVE checked icon remains visible and can change width. | Native 10-tap, long text/RTL, stable footprint, persisted category |
| Exam cards | Telegram chat cell not assumed to be an exam card; source-backed specific atoms unverified | EVE cards unchanged | Adjacent shadows, bottom pill clearance, recycler, Hindi, font scale, native pixels |
| Toolbar seam | EVE drawing: `DesignGlassLayout.kt:144-151`, `EveGlassToolbar.kt:40-65` | `drawHairline=false` only on Home; NOT a Telegram source renderer transplant | Actual 0-gap pixel contract across theme/search/upward scroll/insets |

**Physics source map:** For popup, drawer, dialogs, cards and filter-tab selection the source revision, motion constants, easing, duration, spring/velocity (where applicable), cancellation and per-frame data remain **UNKNOWN/BLOCKED** because the provided ZIP's whole-archive identity fails the required SHA. No numeric values were invented. The Home search retains the pre-existing `UiArcMotion.Spring.MORPH` and `SMOOTH` EVE motion classes; the new cancellation guards are **ADAPTED**, not a claim of original Telegram timings or renderer.

**Day/Night and accessibility:** Day drawer uses existing surface token, Night uses the owner-only black override; current `telegram_drawer_icon` roles and text roles remain in use. Token assertions pass via FAST but final contrast/actual alpha compositing, TalkBack and native screenshot evidence are **NOT TESTED**. App-wide palette unchanged.

**Native/manual matrix NOT TESTED:** phone widths 320/360/393dp, tablets, gesture+three-button, 60/90/120Hz, RTL, Hindi and 1.0/1.3/1.5/2.0 font scales, keyboard/IME, rapid open-close, cold recreate and Android permission system prompts. Verify on Samsung Galaxy Android edge-to-edge separately; no physical device is connected here.

**Ranked unresolved findings (no silent fixes):** (1) archive SHA mismatch prevents authenticated original Telegram renderer and motion work, (2) original popup/dialog/drawer/filter/cards full fidelity is missing, (3) native visual/motion evidence is missing, (4) selected chip's checked icon still changes its footprint; selection persistence/fallback requires integration check, (5) search layout/IME and drawer nav-region measurements need runtime instrumentation. No Mission 06 release APK is claimed.

## Source archive extra checks

- ZIP root `LICENSE` SHA-256 `8177f97513213526df2cf6184d8ff986c675afb514d4e68a404010521b880643` (GNU GPL version 2 text); no `EVE_MANIFEST.sha256` entry is present in the uploaded archive. CRC validity is not a source-authenticity signature.
- Scroll seam: source-level `drawHairline` path removed only for Home; no emulator/frame screenshot currently establishes zero pixels of visual discontinuity.

## Remaining quality gates

Last implementation commit FAST #40622312166 SUCCESS; latest report-only commit FAST pending. Runtime native gesture/three-button, source renderer/property mapping, visual frame comparison, physical Samsung device and mission-final FULL/APK: **NOT VERIFIED** until each has actual evidence. Owner night drawer #000000 is an **implemented owner override** with Day/Night resource-contract assertions; final native pixel verification is still pending. This black background is not claimed as Telegram's own Night drawer color.


## Continuation (2026-10-28) — category state reconciliation (Microstep 04b)

- Source requirement remains BLOCKED: supplied `Telegram-master (1).zip` SHA-256 does not equal the owner-accepted complete ZIP; no Telegram visual, motion, asset or source properties were transplanted.
- **EVE-only data correctness:** `HomeViewModel` now uses `SavedStateHandle` for process-recreation category state; exam cache/network refresh resolves deleted categories back to All **before** publishing the new exam state; unknown category requests are ignored and repeat selections are no-ops.
- `MainActivity.renderChips` uses a single `ChipGroup.check(id)` instead of individually mutating every existing chip, avoiding transient multi/empty selection on a data refresh. `HomeAppearance.categories` excludes duplicate All category entries.
- Tests: new pure JVM reducer suite, source wiring contract; prior Native category tap test remains awaiting actual emulator execution. This fixes state semantics only; **Telegram-exact filter pill graphics, checked-icon width, physics and pixel fidelity remain BLOCKED**.
- Commit/FAST evidence for this continuation must be checked against the latest remote SHA, not historic runs.

## Continuation Microstep 03b — search teardown when signed out

- **VERIFIED source defect:** `MainActivity.onCreate` returns to login before view-binding inflation when Firebase user is absent. The prior `onDestroy` animator cleanup unconditionally dereferenced binding. `cancelSearchVisualAnimations` now safely returns when binding is not initialized, preserving authorized-user cleanup and existing animations.
- Existing category suite was strengthened: Kotlin runtime `assert` calls were replaced with unconditional JUnit `assertTrue` assertions. Focused source regression ensures the signed-out lifecycle path cannot regress.
- This is an **EVE lifecycle safety repair**, not a Telegram search renderer/physics transplant. Native execution of this path remains NOT TESTED. Fast CI evidence must be recorded for the repair commit.

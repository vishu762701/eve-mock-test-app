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

## Continuation completion checkpoint — 2026-10-28

**Latest application-code SHA checked:** `eaefdbe84d09eaf84edd4335177e567c529fc81b`. All implementation commits in this continuation were pushed to `main` without force and used SHA-specific FAST, never FULL. Current attached complete ZIP SHA remains `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; expected is `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`. ZIP has 14,794 entries, passes CRC and has no EVE_MANIFEST.sha256; no accepted Telegram-source transplant is allowed.

| Continuation | Commit on remote main | Exact SHA-specific CI proof | Classification |
|---|---|---|---|
| 04b: selection persisted through process recreation; removed category falls back before exam publish; existing chips transfer selection with a single `group.check`; categories deduplicate All | `0ee0ca4146499f4ba159da3e8038c8cb06114dc6` | [FAST 40623531526](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40623531526) — Build/Backend/Firebase Functions SUCCESS; Native/Publish SKIPPED | **DONE — source-neutral behavior**; Telegram tab visual exactness BLOCKED |
| 03b: signed-out Home early-return cannot crash search teardown on uninitialized binding; source wiring assertions changed from disabled-prone Kotlin `assert` to JUnit `assertTrue` | `eaefdbe84d09eaf84edd4335177e567c529fc81b` | [FAST 40623968230](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40623968230) — Build/Backend/Firebase Functions SUCCESS; Native/Publish SKIPPED | **DONE — source-neutral lifecycle protection**; original search implementation/physics BLOCKED |

**Latest Android build log evidence, FAST 40623968230, build job 122604368039:**

```text
2026-10-28T02:59:12Z > Task :app:compileDebugKotlin
2026-10-28T02:59:58Z > Task :app:assembleDebug
2026-10-28T03:00:09Z > Task :app:compileDebugAndroidTestKotlin
2026-10-28T03:00:28Z > Task :app:testDebugUnitTest
2026-10-28T03:00:28Z BUILD SUCCESSFUL in 1m 59s
```

**Every numbered mission workstream classification at this checkpoint:**

| Step | Status | Evidence / exact blocker |
|---|---|---|
| 00 baseline | DONE | Live HEAD/rules/code, ZIP SHA/CRC/contents and historical vs new CI distinguished |
| 01 original shared primitives | BLOCKED | Supplied archive SHA not accepted |
| 02 Home seam | Functional code patch FAST-VERIFIED; **native no-gap acceptance BLOCKED** | `601d775f`, FAST [40621762592](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40621762592); actual pixel/scroll tests not run mid-mission |
| 03 Home search | Lifecycle patches FAST-VERIFIED; **Telegram-exact presentation/physics BLOCKED** | `7b2f3d9a` / `eaefdbe8` FAST green; accepted original caller/renderer unavailable |
| 04 category selection logic | DONE for data/state and source-neutral native-test source compilation; **device execution BLOCKED** | `e996dcc9` / `0ee0ca41` FAST green; both nav-mode emulator runs reserved for terminal FULL |
| 05 category look/physics | BLOCKED | No accepted exact upstream renderer and animation property map |
| 06 exam cards | BLOCKED | No accepted source-backed card atoms, dimensions, motion or original caller mapping |
| 07 original popup | BLOCKED | Original ActionBarPopupWindow rendering/dependency transplant requires accepted source; seven existing EVE actions/icons preserved, not newly certified |
| 08 original drawer motion/curves | BLOCKED | Original DrawerLayoutContainer and caller chain cannot be source-verified |
| 09 drawer pure-black Night override | DONE code/FAST; **native composited-pixel/edge acceptance BLOCKED** | `3229ee1b` initial FAST failed stale test; `dd53040e` repaired and FAST [40621233645](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40621233645) SUCCESS |
| 10 AlertDialog renderer/variants | BLOCKED | Complete original source/caller/license chain not accepted |
| 11+ migration of confirmations | BLOCKED | Cannot replace app-owned dialogs with unverified approximations; current secure exam/account semantics preserved by noninterference |
| 12 complete physics/integration | BLOCKED | Source properties and actual native motion/IME/theme verification missing |
| 13 cleanup/provenance | DONE for in-scope report; unproven resource deletion deliberately not performed | No now-obsolete source/render assets created; third-party notices/release gate preserved |
| 14 integrated pre-final | FAST gates VERIFIED; **FULL native/lint and true visual/source fidelity BLOCKED** | Latest app-code FAST above; green compilation is not on-device pixel/gesture verification |
| 15 final FULL and published APK | BLOCKED / NOT RUN | Mission-exact implementation not complete; `[eve-full]` must be reserved for a genuine mission-final SHA. No Mission 06 `Eve-debug-apk` artifact exists |

**Resolution requirement:** owner-supplied complete source with the exact accepted SHA-256 (or a newly owner-approved source-identity contract) is needed before starting source-dependent work. Never infer permission merely from a similar file name or matching CRC. After completing exact renderers/physics and required native acceptance, run mission-final FULL on the actual final implementation SHA, then record six job results and uploaded APK's artifact ID/size/digest. This remains **SOURCE FIDELITY BLOCKED**, not 100% complete.


## Source identity investigation — full immutable Git-tree verification (2026-10-28)

**VERIFIED file-content provenance, not old archive identity.** The sole attached ZIP, `Telegram-master (1).zip`, still has SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`, and is **not** one of the owner's two accepted archive hashes. It does not contain `EVE_MANIFEST.sha256`. The previously documented archive-SHA mismatch remains factual; do not mislabel this file the accepted historical ZIP.

A stronger independent source-content comparison was completed against Telegram's public **immutable** repository commit [`3de37c8cfb039452be7398909461da6288babf15`](https://github.com/DrKLO/Telegram/commit/3de37c8cfb039452be7398909461da6288babf15). The GitHub commit/tree API resolves its root tree to **`cc3c6ccf48e4167fdec764dfd04e765043f55093`**. A local read-only verifier computed canonical Git `blob <length>\\0<bytes>` object SHA-1 values for **all 14,086 ZIP file bodies**, preserving executable Git modes, and rebuilt the recursive Git tree, adding the 19 exact Gitlink object IDs read from that immutable upstream root tree (GitHub-generated archives do not embed submodule content). The independently reconstructed root tree is **`cc3c6ccf48e4167fdec764dfd04e765043f55093`**, an exact match. The ActionBar subtree also exactly matches immutable upstream SHA **`66f744c5d2a2fefba55940df092cc03bc41661ee`**.

Hence **all ZIP-contained files are byte-for-byte the official public Telegram revision `3de37c8c`**, rather than a mixed or tampered variant, even though the ZIP container has a different checksum than the historical owner-accepted archives. This is evidence for this **specific new revision only**; it does not retroactively certify historical original or approved extract bytes, hidden submodule bodies, or binary release compliance. Prior commit `0aae959806c57be8ad457980f2a5c6277ff18635` is a different Telegram revision and **must not** be used to claim matching bits for files that changed (e.g. `AlertDialog.java`). Example officially matched Git blob SHA-1: `ActionBarPopupWindow.java` `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea`; `DrawerLayoutContainer.java` `e16c4da8ff7a0f67a7120d85636c57ec8053bdf4`; `AlertDialog.java` `c5574d9682d98a276d3d84892e1b6a584f1c7794`; `FilterTabsView.java` `52238e832e9abcdbf9f177717cdf281fe219f175`.

**Dependency/licensing verification:** original files explicitly bear the GPL-2-or-later source header. Upstream `ActionBarPopupWindow.java` (1,141 lines) depends on `AndroidUtilities`, `AnimationNotificationsLocker`, `UserConfig`, `LayoutHelper`, `PopupSwipeBackLayout`, etc. `AlertDialog.java` (2,069 lines) imports Telegram application/account/network/runtime code (`ApplicationLoader`, `NotificationCenter`, `UserConfig`, `LaunchActivity`, `StarsIntroActivity`, etc.). `FilterTabsView.java` (2,033 lines) imports `MessagesController`, `MessagesStorage`, `TLRPC` and source-dependent account/runtime resources. Copying these wholesale would entangle the EVE/Firebase-only app with Telegram services. NO renderer or replacement has been transplanted as part of this provenance step. Their source *identity* is VERIFIED for the new immutable commit; **dependency-complete safe integration and compliant distribution remain BLOCKED until isolated, precise source/caller maps and compatible native wiring are established**. Do not mistake a similar-looking Kotlin approximation for the original.

This verification upgrades “missing genuine official source” to “official source bytes available for revision `3de37c8c`”; it does NOT silently change the owner's historical accepted ZIP hashes. Unblocked EVE-only layout/state/accessibility defects can proceed normally while source integration dependencies are investigated. FULL/native/release safeguards remain unchanged.


## Continuation verification closure — source-neutral functionality (2026-10-28)

The previously described old-archive mismatch is **not** evidence that the attached ZIP contains inauthentic code: the full **immutable upstream Git tree matched** as documented above. However, this different revision is **not** the owner's old accepted ZIP, and source-authenticated Java classes are not yet integrated renderers. The following are EVE-specific reliability/accessibility patches only; they must not be represented as copied Telegram popup, drawer, dialog, filter or card renderers.

| Implementation | Remote main commit | Exact same-SHA FAST run | Verification |
|---|---|---|---|
| Search activity recreation restores open/closed state, input and visual header without replaying morph or forcing IME | `52250198547ecd428bc1f6523a2145e86ff40bcb` | [40626501856](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40626501856) | Android build/JVM, backend, Firebase Functions SUCCESS; Native/Publish SKIPPED |
| Popup accessibility: one label/focus target for each of seven EVE overflow actions, decorative icon/text excluded, original icons and handlers preserved | `6d1eea8eac19db17b2a8570278bacef7d8644597` | [40626911281](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40626911281) | Android build/JVM, backend, Firebase Functions SUCCESS; Native/Publish SKIPPED |
| Search bar intrinsic-height measurement inside wrap-content Home toolbar; XML contract test | `d3e0799a89bf4026b1b88c8b91d34584f3a6098e` | [40627229005](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40627229005) | Android build/JVM, backend, Firebase Functions SUCCESS; Native/Publish SKIPPED |
| Exam-card accessible combined label (name + already-localized status/duration + pinned state), redundant children silent, existing launch/long-click and Lottie intact | `bc36ccf3cd29a9ad937b83111451bd1e4430ad23` | [40627664219](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40627664219) | Android build/JVM, backend, Firebase Functions SUCCESS; Native/Publish SKIPPED |

**Actual latest implementation build log**, FAST run 40627664219, build job 122616368082:

```text
2026-10-28T03:37:43Z > Task :app:compileDebugKotlin
2026-10-28T03:38:09Z > Task :app:assembleDebug
2026-10-28T03:38:19Z > Task :app:compileDebugAndroidTestKotlin
2026-10-28T03:38:36Z > Task :app:testDebugUnitTest
2026-10-28T03:38:36Z BUILD SUCCESSFUL in 1m 32s
```

### Source-exact integration blockers — classified against actual verified code

1. **ActionBarPopupWindow (BLOCKED):** Original `ActionBarPopupWindow.java` (1,141 lines; official Git blob SHA-1 `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea`) imports Telegram's `AnimationNotificationsLocker`, `UserConfig`, `PopupSwipeBackLayout`, theme/drawable runtime and application resources. Its actual background-scale/cascaded-item entrance at source lines 843–965 uses `150 + 16 × visibleCount` milliseconds, and dismissal at source lines 1024–1069 uses different 150ms path(s). EVE's existing `TelegramPopupHelper` uses an independently-authored anchored-scale/overshoot animation; they are **NOT** the same implementation. No fake exactness, copied dependency stubs or unverified timing changes were made. The seven actions remain connected.
2. **DrawerLayoutContainer (BLOCKED):** Original 214-line `DrawerLayoutContainer.java` at verified SHA-1 `e16c4da8ff7a0f67a7120d85636c57ec8053bdf4` is tied to `INavigationLayout` and `ActionBarLayout` (its source onInterceptTouchEvent calls `checkTransitionAnimation()`), rather than a drop-in AndroidX DrawerLayout physics implementation. Replacing the latter without that original caller architecture would break the functional drawer. Night #000000 EVE owner override remains intact but is not the original gesture renderer.
3. **AlertDialog/DarkAlertDialog/BottomSheet (BLOCKED):** The verified 2,069-line `AlertDialog.java` (`c5574d9682d98a276d3d84892e1b6a584f1c7794`) requires Telegram `ApplicationLoader`, `NotificationCenter`, `UserConfig`, `LaunchActivity`, `StarsIntroActivity`, original components/assets and lifecycle. No dependency-complete compatible renderer was integrated, and no exam/security confirmation was replaced.
4. **FilterTabsView (BLOCKED):** Verified 2,033-line source at `52238e832e9abcdbf9f177717cdf281fe219f175` references Telegram message/account/RPC models (`MessagesStorage`, `ConnectionsManager`, `TLRPC`) unrelated to EVE exam categories. Source-neutral category selection logic has FAST proof; source-exact original filter renderer, checked-icon footprint and physics are **NOT** transplanted.
5. **Exam-card original design (BLOCKED):** A Telegram chat cell cannot silently be declared an original exam card. No verified one-to-one upstream card renderer/shape/source-property mapping for EVE was established. The existing card styling and original Lottie remain; only accessibility labeling changed.
6. **Integrated pixel/frame/gesture/lint/release evidence (BLOCKED):** Both navigation-mode emulators, TalkBack/RTL/font-scale verification, source-versus-rendered frame comparison, integrated dialog confirmation checks, final FULL and `Eve-debug-apk` are **NOT RUN/NOT PUBLISHED**. The owner explicitly forbids premature FULL; do not trigger `[eve-full]` for these independent partial fixes.

**Snapshot:** latest verified *application* SHA `bc36ccf3cd29a9ad937b83111451bd1e4430ad23`. No non-force main-update policy violations, no release gate suppression and no replacement icon/physics approximations. Completion criteria still require a safe dependency-complete original renderer port, source/property mapping and final native FULL at the actual final implementation SHA. The accepted historical extract is not separately attached in this conversation; if fidelity must target that **particular old revision**, its accepted bytes/manifest remain unavailable here.


## Mission 06 continuation — implementation and same-SHA CI (2026-10-28, 04:13 UTC)

The 2026-10-28 continuation inspected live `main`, `ENGINEERING_STANDARD.md`, `AGENTS.md`, `SOURCE_OF_TRUTH.md` and all then-current CI jobs BEFORE writing. The only mounted original archive remains `Telegram-master (1).zip`, actual SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`, 115,700,719 bytes / 14,794 ZIP entries, with **no** embedded `EVE_MANIFEST.sha256` or nested approved extract. It differs from the historical accepted complete/extract archive SHA-256 values `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3` / `b5bbfb7f78ec76769c888d30eb58831845e809b11ab413c722c3646a30be161e`. **Official byte-level Git-tree match for upstream revision `3de37c8c` was already proved above; that source identity fact is preserved.** No Telegram renderer/physics was copied or reverse-invented in these changes; the approved older extract bytes are still unavailable.

| Microstep | Exact application SHA on remote main | Same-SHA FAST outcome | Real scope |
|---|---|---|---|
| 04c: capture only one touch delegate per gesture DOWN; route subsequent MOVE/UP/CANCEL to same owner and clear ownership; source-contract test and new native touch/cancel test | `34492679b35e8eccc83fef4071b58215cbf55ddf` | [40629576474](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40629576474) SUCCESS: build/backend/functions | Home EVE touch reliability only; original Telegram chip renderer/physics unchanged and BLOCKED |
| 03c: Home search spans all main-exam categories and published feedback even with a selected category; keeps Home chip selection and normal grouped list; no duplicate Continue/Pinned presentation rows; locale-independent case folding and Devanagari matching tests | `753b0cda2d3c812561fc8255ce846a74db0cbfec` | [40630017441](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40630017441) SUCCESS: build/backend/functions | EVE search result correctness; original Telegram ActionBar search motion unchanged/BLOCKED |
| 04d: native category touch regression fixture measures screen height using dp scaled by display density (instead of 240 raw pixels); JVM fixture contract | `8974ac96467222b640b462b6103d5e48a3870d3d` | [40630451985](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40630451985) SUCCESS: build/backend/functions | Native test-source correctness; emulator execution not claimed |
| 04e: TargetExamsBottomSheet onboarding receives all published main exams through the de-duplicated global Home index rather than only current category; regression contract | `dfd863a64c59cf4c560f64993a79bc2269ad66df` | [40630805559](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40630805559) SUCCESS: build/backend/functions | Home onboarding data correctness; existing sheet UI/authorization/selected filter unchanged |

**Real Android CI build-log proof** (final application SHA `dfd863a64...`, run `40630805559`, build job `122626465152`):

```text
2026-10-28T04:11:43Z > Task :app:compileDebugKotlin
2026-10-28T04:12:27Z > Task :app:assembleDebug
2026-10-28T04:12:38Z > Task :app:compileDebugAndroidTestKotlin
2026-10-28T04:12:59Z > Task :app:testDebugUnitTest
2026-10-28T04:12:59Z BUILD SUCCESSFUL in 1m 58s
```

For **each** of the four exact-SHA FAST runs: Android app/JVM, backend and Firebase Functions concluded SUCCESS; Native UI navigation matrix and `Publish verified debug APK` were SKIPPED, not falsely marked as passes. Previous native-test source `HomeCategoryTouchNativeTest.kt` was only **compiled** by FAST; no actual gesture/three-button emulator execution or touch hit-test success is claimed.

**Source-fidelity and remaining mission gate classification:** Microsteps 00, 02 functional seam guard, 03 EVE search fixes, 04 category data/touch fixes, 09 EVE owner Night drawer override, and in-scope test/provenance changes have FAST evidence; exact physical pixels and dynamic frames remain UNKNOWN until terminal native tests. The original ActionBarPopupWindow (01/07), DrawerLayoutContainer (08), FilterTabsView graphic/physics (05), AlertDialog/DarkAlertDialog/BottomSheet (10–11), a one-to-one authenticated exam-card renderer (06), source-exact integrated physics (12), final native acceptance (14), and six-gate FULL plus published `Eve-debug-apk` (15) remain **BLOCKED/NOT COMPLETED**, not replaced with unlicensed or guessed adaptations. Source bytes for one official immutable Telegram commit are present, but the original runtime callers/dependencies and GPL distribution requirements have not been integrated or proven compatible with EVE's Firebase-only architecture. Historical *accepted extract* bytes/manifest remain absent here.

**Next exact blocker to clear for source transplantation:** obtain the approved extract with its manifest if the owner requires that historical revision; otherwise scope and verify a legally distributable dependency-complete import of the genuine already-verified Telegram upstream revision `3de37c8c` (real resource/caller/motion mappings), without importing account/network services or changing Firebase scoring. No `[eve-full]` tag, native suite, release validation or published APK is permitted until all material source-fidelity and product acceptance tasks actually pass.


## Owner-approved source revision and actual renderer atoms — 2026-10-28

**Approval supersedes the historical ZIP-container hash requirement for Mission 06 only.** Source revision is now explicitly owner-approved: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. Supplied archive SHA-256 is `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; its 14,086 Git-tracked file bodies were independently confirmed to reconstruct upstream immutable root tree `cc3c6ccf48e4167fdec764dfd04e765043f55093` (as documented in the earlier source-identity section). Historical complete/extract archives remain DIFFERENT and are not represented as equal. Official source revision authorization does not import Telegram account/network services or waive GPL, dependency, release or source-verification requirements.

| Source-backed implementation | Main SHA | Same-SHA FAST proof | Precise scope / deviation |
|---|---|---|---|
| Original `Theme.RippleRadMaskDrawable` white AA clockwise 8-radii mask, `BaseCell.RippleDrawableSafe` guarded canvas, original 6dp first/last row selector mapping | `57ec3539deecb13fbe51780f092b1b3b113cb554` | [40635244808](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40635244808) SUCCESS | Isolated source geometry, Context density/instance Paint and EVE logger replace Telegram global runtime; NOT full popup class |
| Byte-exact `popup_fixed_alert2.9.png` mdpi/hdpi/xhdpi/xxhdpi; 8dp inset, original `PorterDuff.MULTIPLY` submenu tint only in Home popup | `3cfea8103bd220174dcb6e602f5fb5163d570d8c` | [40635813342](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40635813342) SUCCESS | Original asset Git blob SHA match for all four densities; EVE still hosts scroll/popup lifecycle, original background does NOT equal original complete ActionBarPopupWindow |
| Remove unsupported Home selected checked-icon width; keep selected color and Target Exams checkbox | `b3927854da1480631c41cb84d58d5e89c89d2ca6` | [40636258630](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40636258630) SUCCESS | Source `FilterTabsView.java:388-400,649-659` has text/counter without selected checked glyph; EVE Chip is still a different presentation |
| Replace EVE width-spring search with original `ActionBarMenuItem.java:935-1041` 150ms `AnimatorSet` alpha crossfade, cancellation guards and keyboard/state compatibility | `b255146295521c819a2bc4929324ac57a10186ad` | [40636741140](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40636741140) SUCCESS | Original source duration/alpha property; two EVE toolbar groups replace Telegram's actual searchContainer/menu-icon view hierarchy |
| Binary asset hash/source provenance JVM regression gate | This checkpoint | CI proof must be associated with its actual commit | Pins four original images' SHA-256, Git blob SHA-1 and byte sizes; no runtime frame/pixel claims |

**REAL latest application-code Android job evidence**, `b2551462`, run `40636741140`, build job `122645339730`:

```text
2026-10-28T05:14:59Z > Task :app:compileDebugKotlin
2026-10-28T05:15:41Z > Task :app:assembleDebug
2026-10-28T05:15:54Z > Task :app:compileDebugAndroidTestKotlin
2026-10-28T05:16:13Z > Task :app:testDebugUnitTest
2026-10-28T05:16:13Z BUILD SUCCESSFUL in 1m 56s
```

**Live source mapping** (approved revision, immutable upstream path:lines, Git blob SHA-1):

- Popup mask: `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java:5296-5357,5387-5395`, `b56ee0d84558b738144f16961eeaec8b767bb993` -> `app/src/main/java/com/eve/app/ui/home/TelegramPopupRadialSelector.java`. Original radii/AA white mask, Android-only lifecycle substitutions documented in `THIRD_PARTY_NOTICES.md`.
- Popup safe ripple: `TMessagesProj/src/main/java/org/telegram/ui/Cells/BaseCell.java:150-165`, `f352ac78dc0438bd50f6c62ce2a438137540159b` -> same EVE class.
- Popup top/bottom selector and 9-patch: `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java:148-170,294-297,604-609`, `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea` -> `TelegramMenuPopup.kt`, `DesignGlassLayout.kt` popup-only opt-in and four unchanged `drawable-*/telegram_popup_fixed_alert2.9.png` binaries.
- Home filter checked glyph removal: `TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java:388-400,649-659`, `52238e832e9abcdbf9f177717cdf281fe219f175` -> `EveDesignSystem.kt`.
- Home search: `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:935-1041`, `55c3634ea5658c31d58df0682c0a73a620472ede` -> `MainActivity.kt` alpha animation. EVE toolbar hierarchy is an explicitly documented difference.

**Remaining non-waived material blockers:** Original full ActionBarPopupWindow entrance and dismissal `backScaleY` / `backAlpha` cascade and 150ms dismissal remain NOT PORTED (existing `TelegramPopupHelper` is EVE-made). Original DrawerLayoutContainer delegates transition interception to `INavigationLayout.checkTransitionAnimation()` at source lines 58–61; its 214 lines do not themselves define requested drawer slide physics, so the upstream navigation/caller dependency chain must be verified before replacing EVE's functional DrawerLayout. Original `AlertDialog.java` imports Telegram ApplicationLoader, NotificationCenter, LaunchActivity/StarsIntro and account/network state; no dependency-safe complete renderer installed. Original `FilterTabsView` is NOT interchangeable with an exam category ChipGroup; its full tab indicator/physics and original exam-card renderer lack a verified one-to-one EVE screen mapping. On-device gesture/three-button emulators, Day/Night screenshots, TalkBack, 320/360/393dp measurements, RTL/IME/theme frame fidelity and GPL distribution review are NOT VERIFIED. **Final `[eve-full]`, published `Eve-debug-apk` and artifact ID/size/digest are NOT RUN / NOT AVAILABLE**. FAST success cannot validate those gates.

Latest source-neutral logic patches from prior section remain intact: all-category search index, category selection preservation, density-aware native touch fixture, target-exam onboarding, pure-black Night Home drawer override, and source-independent search teardown protection. Native tests remain **source-compiled only**, not executed.


## Verified source correction — Home Bookmarks icon (2026-10-28)

The existing `ic_bookmark.webp` four-density files are already verified unchanged official Telegram `msg_saved.webp` originals at owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. A later source audit compared all four source/EVE Git blob SHA-1 values and found them **identical**, disproving the assumption that this asset was a Tabler alternative. The intervening `d55ef3a6` commit imported another *genuine* Telegram icon, `menu_browser_bookmarks.webp`, but that represents the Telegram browser bookmarking feature, not Saved Messages or saved exam questions. A forward fix restored the existing appropriate source icon and deleted the four files newly added by that misguided swap; it also replaced the test with original `msg_saved.webp` SHA-256/Git-blob assertions. The error and its correction are recorded transparently; no original Telegram or EVE saved content was lost. The correction's exact commit/FAST result must be checked at the live remote SHA.


## Workstream 5 completion & card geometry polish (2026-10-28)

- **ResultActivity**: Reattempt confirmation and 3-attempt limit dialogs migrated to `TelegramDialogSourceBackground.apply(...)`. Re-entrancy guards prevent double-launch; `preserveDestructivePositive = true` maintains the red action button contrast. Both dialog references are dismissed in `onDestroy()`.
- **Admin activities dialog migration**:
  - `AdminActivity`: Plain dialog builders routed through `showTelegramSourceDialog()`; custom view dialogs (Feedback Post, Home Banner, Floating Link, Post Replies, Export progress) styled via `TelegramDialogSourceBackground.apply()`.
  - `ManageExamsActivity`: 14 plain dialog builders (prompts, generation confirm/success/error, test details) migrated to `showTelegramSourceDialog()`.
  - `AdminDeleteFlow`: Uses `showDestructive()` on confirmed deletion; non-deletable and failure alerts use `showTelegramSourceDialog()`.
  - `EditAboutActivity`: Tab-switching and back-navigation unsaved-changes alerts migrated to `showTelegramSourceDialog()`.
  - `ManageUsersActivity`: Student details and enable user use `showTelegramSourceDialog()`; disable user uses `showDestructive()`.
  - `FlaggedQuestionsActivity`, `GeneratedTestsActivity`, `ManagePremiumActivity`, `ManageSyllabusActivity`, `SendNotificationActivity`, `AdminAnalyticsActivity`: All dialogs migrated to `showTelegramSourceDialog()`.
  - `AppConfigActivity`: AI health check failure and connection health dialogs migrated to `showTelegramSourceDialog()`.
  - `ProfileActivity` & `SettingsActivity`: Custom progress dialogs styled with `TelegramDialogSourceBackground.apply(this)`.
  - `DebugCrashReporter`: Crash alert dialog styled with `TelegramDialogSourceBackground.apply(dialog)`.
- **Card Geometry Polish (`item_exam.xml`)**: Removed redundant `android:layout_marginBottom="@dimen/design_card_gap"` from inner `examIconContainer`, eliminating vertical center asymmetry and clipping risk.
- **Contract Unit Tests**:
  - `ResultReattemptSourceDialogTest.kt`: Enforces re-entrancy protection, lifecycle safety, `TelegramDialogSourceBackground` application, destructive contrast preservation, and reset/navigation intents.
  - `AdminDialogSourceContractTest.kt`: Verifies all plain and custom dialog builders across `AdminActivity`, `ManageExamsActivity`, `AdminDeleteFlow`, and secondary admin activities route through source adapters.
- **FAST CI Verification**: Commit `d7d6ca01` ran under FAST CI run [40686902501](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40686902501): 505 JVM unit tests passed, debug APK compiled, backend verified, firebase-functions verified.

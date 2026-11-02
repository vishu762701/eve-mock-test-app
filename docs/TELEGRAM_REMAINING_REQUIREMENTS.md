# Telegram mission live requirement ledger

Status: PARTIAL; no screen or distribution acceptance. This supplements, and does
not replace, the feature/commit/CI history in TELEGRAM_FINAL_MISSION_CHECKPOINT.md.

Recovery 2026-11-02: clean EVE main 58b415857bce257de4ca966a7174ff74b9ae910d,
refetched over HTTPS; latest commits and exact-SHA Actions inspected. FAST
[41389589665](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41389589665)
and docs [41389589728](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41389589728)
SUCCESS. Build/backend/Firebase passed; native navigation/publication skipped;
artifact list empty. No concurrent implementation found at recovery.

Official Telegram HEAD independently refetched over HTTPS:
3de37c8cfb039452be7398909461da6288babf15, tree
cc3c6ccf48e4167fdec764dfd04e765043f55093. telegram.org/apps links this
DrKLO/Telegram origin. The complete superproject was cloned; submodule setup is
in progress and must be reported separately. No pin migration needed. All Telegram
references below use this full SHA, abbreviated T only in this table. References
marked UNKNOWN are audit tasks, not asserted source equivalence.

The earlier ledger's MET sub-algorithms remain preserved; whole requirements are
NOT MET until all acceptance evidence exists. Device requirements are BLOCKED in
this local environment (no adb, emulator or /dev/kvm at recovery). Continue code,
host tests and exact-SHA CI independently. Unavailable gifts are an explicit
service dependency; QR remains free. No production writes/deploy/migration,
release-variable change or legal self-certification is authorized.

| ID | Requirement | Existing implementation | Upstream file:lines@SHA | Dependencies | Status | Verification evidence | Next action |
|---|---|---|---|---|---|---|---|
| REC | Live recovery/concurrency/CI | clean main and checkpoint history | official Git HEAD/tree@T | Git/GitHub | MET | above exact-SHA reads | re-fetch before push |
| A1.1 | Whole scene/card/layers/typography/insets | TelegramQrSceneView, QrCodeLayer, QrCardShadow | QrShareView:2041-2430,3690-3819@T | EVE Activity/native scene | NOT MET | source map and existing FAST; matched runtime absent | inspect remaining composition differences |
| A1.2 | Native backdrop, wave, QR ink/shadow | QrBackdropScene, telegram_qr_backdrop.cpp | QrShareView:2260-2426,3573-3806; image.cpp:1211-1377@T | JNI/Canvas | NOT MET whole experience | existing scoped host pixels/PNG decode evidence | preserve algorithms; collect GPU/reference evidence |
| A1.3 | Pattern placement/gesture/spring/shine | QrPatternField, QrPatternSwap, QrSceneShine | QrShareView:2488-2787,3111-3411,6637-6768@T | EVE native Drawables | NOT MET | source equation tests; full reflection/frame parity open | inspect remaining per-symbol lighting |
| A1.4 | Reveal/close/interruption/repeated actions | QrSceneReveal/QrCardMotion | QrShareView:3591-3687@T | Activity/scrim | NOT MET | existing host cancellation tests | device interruption/window matrix |
| A1.5 | Scene glass/API fallback/resource cleanup | QrControlBackground/QrBackdropScene | QrShareView:2140-2175,4533-4583@T | API31 GPU | NOT MET | host offset/disposal tests; device pending | device GPU comparison |
| A1.6 | Rotation/restoration/reduced motion/font/theme/accessibility | EVE scene host and motion policy | upstream full host audit UNKNOWN | Android/device | NOT MET | partial tests, no complete matrix | audit production paths and execute matrix |
| A2.1 | Picker sheet island/open/reverse/cascade | QrPickerSheetMorph/Surface/MorphDialog | QrShareView:5317-5570; QrShareActivity:532-758@T | Material modal host | NOT MET | existing math/attached-view tests | reconcile source parent/drag/compositor |
| A2.2 | Layout/physics/badges/masks/glass | QrEmojiCell/Grid/EmojiPickOverlay/SheetLift | QrEmojiPanel:493-630; QrShareView:4730-4828@T | AndroidX/native renderer | NOT MET | original Java algorithms adapted; runtime unaccepted | matched native grid/blur/scroll test |
| A2.3 | Real EVE animations/six-order/removal/state/slow/error | QrEmojiPickerGrid/QrEmojiPattern/EmojiRepository | QrShareView:972-992@T; EVE catalogue adapter | authorized EVE media | NOT MET | existing actual-Drawable lifecycle and ordering tests | exercise slow/empty/offline and restoration |
| A2.4 | Bounded native frame scheduling/memory/recycle | NativeQrDrawable/QrAnimationAssets | RLottieNative/lottie.cpp; tlottie gitlink | Rust/JNI/Android | NOT MET | 243 Rust tests in prior FAST; device pacing absent | preserve bounds; device frame/memory tests |
| A3.1 | Preview crop/rotation/four genuine points/timing | CameraX shared ViewPort/QrScanFound | QrScanOverlay:700-782,994-1063@T | PreviewView/ZXing | NOT MET | scoped Java/native Skia tests; physical map UNKNOWN | actual CameraX device matrix |
| A3.2 | Original live camera glass and fallback | QrControlBackground sheen/rim only at recovery | QrScanOverlay:804-879; CameraView:1012-1035,1096-1106@T | API31 RenderNode/CameraX | NOT MET | actual upstream opened; live camera blur missing | adapt original record/blur/draw subsystem |
| A3.3 | Source frame/controls/geometry | QrScannerFrameOverlay/EVE text buttons | QrScanOverlay:579-599,659-700@T | EVE accessible nodes | NOT MET | frame math tests; control geometry differs | source control layout without losing access |
| A3.4 | Camera lifecycle/permission/settings/gallery | EveQrLiveScannerActivity | QrScanOverlay:370-546@T; EVE permission adapter | CameraX/ActivityResult | NOT MET | source contracts/compile; device pending | trace pause/resume and stale callbacks |
| A3.5 | Torch errors/futures/rapid taps | QrTorchGate | QrScanOverlay:783-787,914-922@T | CameraX future ownership | NOT MET full hardware | three existing JVM state tests | hardware/failure/pause verification |
| A3.6 | Malformed/gallery/allowlist/Back/cancel/dedup | scanner/ProfileQrPayload/QrScanImageSizing | EVE secure routing; source found callback | Android URIs/worker | NOT MET | existing parser/sizing/contracts | verify stale concurrent decode ownership |
| A3.7 | Physical-device scanner/reference comparison | no matched evidence | full QrScanOverlay@T | physical device/reference build | BLOCKED locally | no device/KVM | run when runtime/reference available |
| A4.1 | Export PNG appearance/resolution/scanability/no chrome | scene export/QrGalleryWriter | QrShareView:2095-2121@T | independent render snapshot | NOT MET full acceptance | historical 160+ native Skia QR decodes | rerun export/platform matrix |
| A4.2 | Username-only deep links/share URI/permissions | ProfileQrPayload/FileProvider/UsernameCardActivity | EVE contract/QrShareActivity adapter | MediaStore/FileProvider | NOT MET full acceptance | existing parser and publication rollback tests | trace actual share/save/cancellation |
| A5 | Gift/premium services absent; QR free | no demonstrated EVE inventory/entitlement | Telegram account service branches@T | authorized real service | BLOCKED | no service contract identified; never fabricate | retain free EVE QR and document dependency |
| B1 | Profile source structure/spacing/type/touch/transitions | ProfileActivity/sections/actions | ProfileActivity/ProfileActionsView@T, ranges in profile maps | EVE authenticated profile | NOT MET | fidelity contract OPEN | inspect full parent/caller differences |
| B2 | Gooey avatar/scroll/collapse/shared motion | ProfileHeaderExpansion/ProfilePullScrollView | ProfileGooeyView/ProfileActivity@T | source renderer/scroll parent | NOT MET | existing provenance/native fixtures | complete missing source renderer dependencies |
| B3 | Action row/popup/no duplicate Admin/genuine online | ProfileActionsCanvasLayout/ProfileOverflowPopup | ProfileActionsView/ActionBarPopupWindow@T | fresh server authority/presence | NOT MET whole requirement | existing source and isolation tests | inspect current label/presence paths |
| B4 | Category selection/save/loading/rollback/restore | ProfileActivity/ProfileSession | EVE category adapter/source grouped rows | authorized API/readback | NOT MET full runtime | existing tests, signed-in matrix absent | verify lifecycle/concurrent mutation |
| B5 | Durable username/phone/uniqueness/display name/DOB | profile editors/Worker | EVE APIs; upstream editor audit UNKNOWN | Firebase/Worker validation | NOT MET | current endpoints require fresh audit | trace server and malformed responses |
| B6 | Remove visible Add bio and About; preserve data | current ProfileActivity/activity_profile.xml contain neither row nor editor action | latest owner correction (EVE) | layout/production caller | MET scoped visible removal | fresh production source/layout search; backend 288 tests include unchanged bio data contracts | preserve data/API compatibility; native whole-profile acceptance separate |
| B7 | Account isolation/RTL/Hindi/font/theme/rotation | ProfileRequestOwner/ConfirmedProfileStore | EVE adapter; upstream matrix UNKNOWN | identity/lifecycle/device | NOT MET | existing owner-tag/cache tests | signed-in/native matrix |
| C1 | Audit all exam/question/poll admin actions | AdminActivity/content activities | Telegram navigation/rows ADAPTED, no business equivalent | Android/API/Worker | NOT MET | full current action matrix not recovered | inventory callers and authorization/tests |
| C2 | Users/admin/roles/broadcast/notifications/feedback | admin production activities | authentic primitives only; ranges UNKNOWN | Firebase/Worker | NOT MET | full current action matrix pending | audit each real handler/role |
| C3 | AI/settings/maintenance/analytics/all actual sections | admin pager and bound original rows | PagerSlidingTabStrip@T + EVE business | fresh AdminAccessGate/Worker | NOT MET | prior bound pager test | enumerate all real actions/error states |
| C4 | Poll destructive text/server-confirmed success | existing polls UI/API | EVE business/source dialogs | Android themes/Worker response | NOT MET pending audit | known regression to reverify | trace text colors and mutation callbacks |
| C5 | Refresh/nav/rotation/concurrency/role separation | admin Activities/ViewModels | ADAPTED Telegram primitives | mocked server/device | NOT MET | no complete action/lifecycle matrix | verify without live production writes |
| D1 | Home seven overflow actions/source popup/dialogs | MainActivity/shared popup/dialogs | ActionBarMenuItem/PopupWindow@T | current destinations | NOT MET | earlier reports; fresh caller audit pending | inspect seven actions and anchoring |
| D2 | Drawer gestures/insets/scrim/Night pure black | Home drawer | DrawerLayoutContainer@T + owner #000000 exception | Android input | NOT MET full acceptance | existing implementation preserved | test drag/fling/navigation/theme matrix |
| D3 | Toolbar seam/search/IME/rapid/theme/Back/restore | MainActivity/HomeAppearance | source parent/search audit UNKNOWN | Activity/IME | NOT MET | no fresh full evidence | inspect source and shared callers |
| D4 | All/Other one selection; selected no-op | current Home filters | EVE filter adapter | discovery data contract | NOT MET pending verification | existing tests need recovery | run actual selection tests |
| D5 | Exam cards/navigation/shared colors/resources/RTL | EVE shared components | source rows/tokens; exact ranges UNKNOWN | all callers | NOT MET | whole-screen matrix absent | inspect shared components before layout edits |
| D6 | Bell provenance/tap/incoming/unread | licensed bell and notification store | Apple provenance not asserted | existing inbox semantics | NOT MET pending fresh verification | bell provenance/report/tests exist | verify no read-on-Home acknowledgement |
| E1 | History/bookmarks/notebook/topics/PYQs | existing student Activities | applicable Telegram primitives UNKNOWN | existing APIs/user isolation | NOT MET | fresh inventory pending | audit each production screen |
| E2 | Notifications/settings/syllabus/leaderboard/analytics | existing Activities/shared components | applicable source controls UNKNOWN | existing server contracts | NOT MET | full matrix pending | inventory caller/layout/source coverage |
| E3 | Answer green/Save Next monochrome/progressive numbers | TestActivity/QuestionAdapter | quiz/radio source mappings | server-authoritative scoring | NOT MET pending fresh verification | scoring invariants unchanged | inspect shared option/navigation draw paths |
| E4 | Submit/exit/time-up/results/review/ranks | test/results Activities | applicable source dialogs/radio | timers/dedup/answer secrecy | NOT MET | no current full acceptance | test UI while preserving scoring |
| F1 | Day/Night/reveal/maintenance startup/off/theme | shared themes/Main maintenance gate | source motion + EVE authority | fresh Worker admin check | NOT MET full acceptance | prior tests; no fresh matrix | inspect startup/resume/theme routing |
| F2 | Recreation/insets/cutout/navigation/font/RTL/a11y | app-wide existing components | source helpers; per-screen ranges UNKNOWN | Android/device | NOT MET | no complete runtime matrix | execute available tests; record hardware gaps |
| F3 | 60/90/120Hz/physics/reduced motion/memory/performance | DesignMotion/native renderer/scene hosts | source algorithms, maps above | physical devices/traces | BLOCKED hardware evidence | no available devices | continue code cleanup verification independently |
| G1 | File hashes/pins/notices/licenses/dependency edges | manifests/notices/release gate | official full SHA and submodule gitlinks | Git/licensing review | NOT MET overall | authenticated source; manifests to rerun | verify imported files and keep release gate closed |
| G2 | Security/data/no Telegram services/production writes | existing Android/Worker/Firebase | EVE invariants | scoped diff + tests | NOT MET mission audit | no new deployment or permission planned | review each touched production path |
| G3 | Main-only/coherent commits/exact-SHA FAST | protected workflows/history | n/a | GitHub | MET recovery, ongoing | initial FAST/docs green | local checks; fetch; push; wait for exact-SHA gate |
| G4 | Final six-job FULL/both modes/verified APK/checksum | protected workflow | n/a | ready implementation/native gate | NOT MET | initial FAST has no artifacts | run only when eligible; never bypass |
| G5 | Human GPL/distribution/signing approval | fail-closed EVE_GPL_RELEASE_APPROVED | root GPLv2/per-file grants | authorized human/legal review | BLOCKED | release compliance remains OPEN | no code agent may approve/bypass |

First overall unfinished requirement remains full A1 source/runtime acceptance.
Next independently implementable source gap after preserving completed A1/A2 work:
A3.2 camera layer RenderNode glass, followed by scanner lifecycle/controls and the
remaining audited subsystems. This is continuation, not a restart or final report.

## Current delivery evidence

Source camera glass commit 7bb581b1b95d7e4e79489db77dfa50c3f9b92b6a: FAST [41392841830](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41392841830) SUCCESS, docs 41392841837 and source imports 41392841829 SUCCESS. Native renderer 243 tests green; all four ABI build tasks pass. FAST artifacts: zero. Full screen/device/legal acceptance stays open.

A3.4/A3.6 next code batch implements pause/rebind ticket ownership, explicit found navigation cancellation, resumed Gallery delivery and pending permission/Gallery restoration. Ten focused JVM tests, including two actual scanner Activity lifecycle tests, and lint/AndroidTest compilation pass. Final broad/CI evidence must come from the carrying implementation commit.

## Admin rights integration batch

At 2e5655911e0755a0b3a1892a15d4bafd683631dd, exact-SHA FAST 41394531948 and
docs 41394531950 SUCCESS. No published artifact/native navigation execution.
C1/C2/C3/C5 entry-control implementation now verifies fresh Worker membership and
validated permission snapshot before showing protected UI, checks the actual right
at all 21 destinations plus dashboard (23 existing calls including resume), binds
UID/email to the current account, and propagates cancellation. Pending/malformed/
failed rights never grant all dashboard actions. Questions exam edits and monitor
retry need their extra rights; ViewModel construction no longer requests unrelated
privileged directories/statistics. Full source/handler map: ADMIN_RIGHTS_CONTINUATION.md.
Local broad verification: 740 tests, 170 suites, zero failures/errors/skips;
AndroidTest compilation, lint zero errors and x86_64 assembly pass. Exact-SHA FAST
is required for this carrying batch; whole Admin fidelity/action/device matrix stays
NOT MET. No backend/deploy/data/role definition changes or local authority added.

Fresh A1 source inspection clarifies a historical gap: QrShareView:3245-3304
explicitly says the light band does NOT light/enlarge emoji. Per-symbol reflective
lighting is NOT FOUND in that emoji branch and must not be invented. The actual
missing source behavior is ready-item popIn: QrShareView:3171-3180,3281-3304,
with seeded 300ms delay + 380ms item appearance; current EVE drawEmojiSlot has no
such ready-frame appearance gate. First unfinished overall A1 remains NOT MET;
next concrete source implementation is that appearance path with retained EVE
native-frame ownership, selection/recycle/rapid-load cancellation and export rules,
then source scanner controls and all previously open requirements. Device/GPU,
matched reference, six-job FULL and legal release acceptance are still pending.

## A1 source appearance code delivery

Current base 40a7bb30fcc2bbb3468d482a0ec6a9e4633e811b is exact-SHA green at
FAST 41395975426 (docs 41395975313). A1 appearance is now implemented through
original Java QrEmojiAppearance + actual SceneView/native Drawable adapters:
ready-only post-open seeded pop, picked-later swap isolation, separate old/new
arrays, reduced motion and frozen export. All 745 JVM tests, lint (0 errors),
AndroidTest compilation and x86_64 assembly pass. Source manifest 101 entries and
four verbatim assets match; synthetic readiness fixture renders via host C API.
Source map: QR_EMOJI_APPEARANCE_SOURCE_MAP.md. The carrying batch requires its own
FAST; no Android device execution or whole-scene acceptance is inferred.

Completed engineering increments in this continuation: scanner camera source glass,
scanner pause/rebind/delayed-result ownership, admin fresh per-destination rights,
source ready-frame item appearance; each is scoped ADAPTED development integration.
Remaining overall A1 hierarchy/card/type/parent geometry, source picker parent/drag,
scanner controls/physical transforms and complete B/C/D/E/F runtime/UI/action
requirements remain NOT MET. Device/reference/GPL/service dependencies remain
BLOCKED as listed. Mission-final FULL and protected verified APK remain NOT MET.
No new owner decision is needed for continued development; legal/distribution,
production deploy/migration and actual unavailable service decisions remain separate.

## Verified implementation integration status

| Scoped requirement | Status | Exact-SHA evidence |
|---|---|---|
| A3.2 source camera glass integration (not whole device parity) | MET | 7bb581b1 / FAST 41392841830 SUCCESS |
| A3.4/A3.6 pause/bind/result/permission restoration integration | MET | 2e565591 / FAST 41394531948 SUCCESS |
| C1/C2/C3/C5 fresh destination rights and confirmed dashboard integration | MET | 40a7bb30 / FAST 41395975426 SUCCESS |
| A1 ready-frame seeded source appearance and frozen export integration | MET | 66574962 / FAST 41397993734 SUCCESS |

Full requirement rows above retain their overall NOT MET/BLOCKED status. All four
increments are ADAPTED, not original whole-screen EXACT. Latest implementation
66574962d7dc1eb484ec81fb8440112d0279fc5d is green; exact links, counts and next action
are at the top of TELEGRAM_FINAL_MISSION_CHECKPOINT.md. Whole mission PARTIAL.

## Resume: A1 card/caption/parent composition (2026-11-02)

Live recovery: e4dd748651a4b7abecc52f5c9be204fd67a1a64f clean main,
FAST 41399010870 and docs 41399010801 SUCCESS; artifact count zero. No concurrent
changes found. Official HEAD re-fetched, unchanged 3de37c8cfb039452be7398909461da6288babf15;
required tlottie remains at 31f1b542f88e7b4be9a01e749920d857535fc715. Optional
uninitialized submodules and previous 8 GiB workspace limitation remain documented.

| ID | Requirement | Existing implementation | Upstream file:lines@SHA | Dependencies | Status | Verification evidence | Next action |
|---|---|---|---|---|---|---|---|
| A1.card-composition | Source card size/rest/picker rise with responsive fitting | Kotlin bounds/independent Material sheet, no source card rise | QrShareView:1879-1881,2046-2061,3808-3819@3de37c8cfb039452be7398909461da6288babf15 | measured EVE chrome, source sheet fraction | NOT MET | live code opened; body ratio already source-derived; ad hoc fit reserves and missing rise remain | extract Java geometry, fit to measured viewport, bind actual sheet progress |
| A1.caption | Original condensed font/@ glyph/30-25-19 fitting/StaticLayout | plain system-font single-line @ text | QrShareView:3858-3892,4060-4109; SettingsSearchCell:40-79; StaticLayoutEx:56-145@same full SHA | exact source font and three density-qualified glyphs | NOT MET | all source paths opened; current long username can overrun card | integrate Java layout/span with source assets, bounded compact-window fit and RTL |
| A4.export-isolation | Export rest layout without mutating live card or ink state | export samples member bounds/frameQrColor | QrShareView:2095-2121@same full SHA | independent geometry/backdrop/frame | NOT MET | inspected production path | independent export metrics and ink, test during interrupted open sheet |

License boundary before copying: official root GPLv2 applies; SettingsSearchCell
and StaticLayoutEx carry GPLv2-or-later headers. Roboto Condensed Bold embeds
Copyright 2011 Google Inc. All Rights Reserved and no embedded license IDs 13/14.
No new font-specific redistribution clearance is inferred; preserve attribution,
record binary identity and retain the existing combined/per-file human release
review gate. Development source integration is already authorized. Telegram
account, paid/temporary link, premium and username-burst services stay excluded;
EVE remains username-only/free. Existing working QR/scanner/native/admin subsystems
are preserved. Whole mission is NOT MET; no legal/device acceptance claimed.

Scoped implementation result: A1.card-composition and A1.caption are MET for
source-derived code/host integration; A4.export-isolation is MET for the exercised
live-state invariant. All three remain ADAPTED, with full device/screen parity NOT
MET. Source map: QR_CARD_CAPTION_SOURCE_MAP.md. Local serial broad Gradle run
(:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64) PASS: 753 JVM tests / 173 suites,
zero failures/errors/skips. Python CI helpers 59 PASS, docs/claim/import/diff checks
PASS, 116 source/EVE identities match pinned Git blobs. A stale local-coordinate
source guard was updated without removing renderer or branding assertions; real
bitmap PNG decode and live geometry/ink tests also pass. No native code changed.
Exact-SHA FAST is required after the carrying implementation commit; whole mission
remains PARTIAL. First unfinished source implementation: scanner controls/title
geometry and press/torch visual interpolation, followed by the remaining ledger.


## A3 scanner controls/title continuation (2026-11-02)

Authorized requirement ledger recorded before integration:

| ID | Requirement | Existing implementation | Upstream file:lines@SHA | Dependencies | Status | Verification evidence | Next action |
|---|---|---|---|---|---|---|---|
| A3.controls | Original round Gallery/torch, title above frame, close and press feedback | 126dp text pills, bottom caption, static 48dp close | QrScanOverlay.java:328-350,579-599,659-708,770-802,867-920; BackDrawable.java; ActionBar.java:1914-1920; AndroidUtilities.java:253-270@3de37c8cfb039452be7398909461da6288babf15 | existing glass/live backdrop, source glyphs and Roboto medium, EVE native accessibility/actions | NOT MET | actual source and production callers opened; camera lifecycle/torch futures already implemented and preserved | extract original Java geometry/draw/press, native accessible hit targets, measured compact-window fit; test production actions/cancellation |

Dependency/license disposition: source root GPLv2 and BackDrawable GPLv2-or-later
header retained. Existing telegram_roboto_medium.ttf is compared with source
rmedium.ttf before reuse; source qr_flashlight/qr_gallery density assets preserve
actual Git identities. Keep original Java draw/geometry; Kotlin host owns CameraX,
permissions/Gallery/deep-link allowlist and fresh torch-future state. Telegram camera,
account, LiteMode, recognition and network services remain excluded. No new
permission, backend/API or production-release scope is authorized by this batch.
Compact windows must fit the whole frame/title/control composition to measured
safe bounds; no arbitrary offsets or delay hide clipping. Whole scanner device
parity remains NOT MET; human combined-app release gate remains fail closed.

Scoped A3.controls Java/integration is MET for source/host tests, ADAPTED; scanner
whole-screen and physical-runtime requirements remain NOT MET. Source map is
QR_SCANNER_CONTROLS_SOURCE_MAP.md. Serial broad local Gradle verification: 757
JVM tests / 174 suites, no failures/errors/skips, AndroidTest compilation, lint
0 errors / 1646 warnings and x86_64 debug APK PASS. Updated real-control HWUI fixture
compiles, not executed. Python CI helpers 59 PASS; 129 upstream/EVE identities,
docs/claim/import/diff checks PASS. Exact-SHA FAST pending the carrying scanner
commit; base card commit 3b2245bc0743ebd45d89e708eb6a899de0cb7e0a FAST
41402360574, docs 41402360616, imports 41402360702 all SUCCESS. Native and release
limits unchanged. Next: source corner-bitmap renderer and parent/picker gaps.


Owner steering: stop QR-only iteration. The uncommitted frame/corner raster-cache
experiment was removed from production and tests; successful main's overlay is
restored byte-for-byte. No green QR implementation is removed. Corner bitmap/API29
analytic-dim changes are a source-fidelity gap, not a proven critical defect, and
are deferred. Keep whole QR acceptance OPEN. Audit actual active paths and critical
functional defects, then continue independently implementable Profile/Admin/Home/
Exam/shared requirements. Device/reference absence must not drive speculative code.


## Full-mission reconciliation and Profile continuation

QR_ACTIVE_PATH_RECONCILIATION.md records every implemented QR feature/caller group,
active-versus-test-only paths and the four owner-requested issue classes. Current
main e0963d7dcdfec04bcbe90f56dea26246b549e93b exact-SHA FAST 41403654857,
docs 41403654877 and imports 41403654789 SUCCESS. No new personal-QR critical defect
was reproduced; green code stays unchanged, whole QR acceptance OPEN. Source-only
and hardware/legal gaps are recorded, not treated as reasons to add more QR code.

| ID | Requirement | Existing implementation | Upstream file:lines@SHA | Dependencies | Status | Verification evidence | Next action |
|---|---|---|---|---|---|---|---|
| B1/F.avatar-lifecycle | Release existing Profile CPU/GPU renderer resources on detach, restore on reattach | ProfileGooeyView already contains source-adapted CPU/GPU effects; CPU bitmap only recycled on size change, no detach release; four RenderNodes retain display lists | Components/ProfileGooeyView.java:142-254,270-482,486-497@3de37c8cfb039452be7398909461da6288babf15 | Existing View/bitmap/RenderNode ownership; no new renderer or state repository | NOT MET | Production implementation and actual upstream opened; prior report's blanket missing-RenderNode claim is stale | Add release/rebind lifecycle to existing Impl; test CPU pixels/recycle/reopen and GPU display-list cleanup where supported |

This is an EVE lifecycle adaptation to the existing renderer; preserve source draw
math, avatar trajectory/gestures, identity, backend and visible Profile requirements.
Do not restore Add bio/About. Full B1/B2 visual/motion acceptance remains OPEN.


B1/F.avatar-lifecycle: MET for code/host lifecycle behavior, ADAPTED; whole Profile
visual/motion acceptance remains NOT MET. Serial broad Gradle unit/AndroidTest
compile/lint/assemble x86_64 PASS: 759 JVM tests / 175 suites, zero failures/errors/
skips. Python CI helpers 59 and docs/claim/diff checks PASS. Actual GPU display-list
lifecycle runs under host native graphics; no physical HWUI/avatar parity inferred.
The carrying Profile commit requires exact-SHA FAST. Next concrete non-QR defect:
C4 PollRepository.getPolls turns failed/null responses into empty success, and
createPoll accepts an empty server ID; trace existing callers/Worker and repair
failure handling without changing authorized roles or deploying backend code.


## C4 Poll failure/success contract

| ID | Requirement | Existing implementation | Upstream file:lines@SHA | Dependencies | Status | Verification evidence | Next action |
|---|---|---|---|---|---|---|---|
| C4.poll-response | Failed load must show error, create requires confirmed nonblank server ID | ManagePollsActivity.loadPolls catches thrown errors; PollRepository currently converts false/null list to empty and missing ID to empty string | EVE business adapter; no Telegram poll-admin business equivalent | EveApiService /api/polls; Worker authMiddleware + moderate_feedback for POST/PUT/DELETE; existing source dialog | NOT MET | Actual Activity/repository/API/Worker opened; no live writes | Repair only repository response validation, test false/null/empty/valid/cancellation and real bound row visibility |

Preserve current fresh Android gate and independent Worker permission checks,
option/date/delete controls and all poll/vote data contracts. No new screen/state
owner, backend deployment or migration. This is ADAPTED EVE business handling;
Telegram source primitives already integrated remain unchanged. Server success
with no ID cannot serve as confirmation of the created entity. Whole Admin
acceptance remains NOT MET until action/runtime/security matrix is verified.


C4.poll-response scoped code/host behavior MET (EVE business adaptation, no
Telegram business equivalent). 766 JVM tests / 177 suites, zero failures/errors/
skips; AndroidTest compilation, lint and x86_64 assemble PASS. Day/Night production
adapter labels/callbacks pass. Exact-SHA FAST required after carrying commit.
Full Admin action/rotation/concurrency/security acceptance remains NOT MET.
Profile commit 66f3eea0ad5ffae629e2e2cf5cd8d5f78f508861 FAST 41405867198 SUCCESS.


C4.poll-status-server: NOT MET before repair. Existing Worker PUT accepts malformed
active values and returns success/audit for nonexistent IDs. Authorized scope:
validate boolean input and confirm actual UPDATE row count, conditional audit in
the same transaction; isolated SQLite route tests, no deployment/migration.

C4.poll-status-server scoped handler behavior MET locally: 291 backend tests PASS,
TypeScript build PASS. Actual Hono + migrated isolated SQLite verifies null/invalid
input rejection, zero-row 404 without audit, valid update with audit, denied server
right despite local isAdmin=true, and atomic rollback on audit insertion failure.
No production deployment/migration is authorized or performed. Server business
validation is EVE-owned; no upstream Telegram poll-admin equivalent is claimed.
Exact-SHA FAST is required after the coherent second poll implementation commit.


D4 stale Home filter callback: base a8433b6484b7141f881aa5fb271808703c042302
FAST 41407181451 SUCCESS, official upstream rechecked unchanged full pin
3de37c8cfb039452be7398909461da6288babf15. Existing Activity renderChips captures
creation-time selected; existing ViewModel owns current selection/no-op/persistence/
fallback. Scope: move that same chip renderer into existing HomeAppearance for
actual production callback testing, supply live selection getter, preserve reuse/
check/palette/touch geometry. No new selection owner/component. Functional EVE
adaptation, source FilterTabs geometry unchanged; full native matrix stays OPEN.

D4 production callback repair: HomeAppearance.renderCategoryChips now contains
the existing reused-chip presentation path. MainActivity supplies
viewModel.selectedCategory.value through a live getter and the existing
selectCategory method; no second selection owner. Native callbacks reject only
the currently selected category. Before/fixed regression: all three actual native
chip callback tests FAIL with the old captured guard and PASS with live state.
They cover repeated/rapid All/Other/SSC, selected no-op event counts, reused-view
identity, reordering/recreation/fallback, stable measured widths, Day/Night and
Hindi/RTL 200% font. Existing SavedStateHandle/fallback/touch-target native tests
remain; physical restoration/TalkBack/navigation/reference matrix stays OPEN.

| Upstream path:lines@SHA | EVE path:lines | Classification | Evidence |
|---|---|---|---|
| FilterTabsView.java:388-400,649-659@3de37c8cfb039452be7398909461da6288babf15 (existing unchanged visual property) | HomeAppearance.kt:31; MainActivity.kt renderChips; existing EveDesignSystem.createCategoryChip | ADAPTED EVE category callback contract; no new Telegram transplant or visual equivalence claim | Actual production renderer/callback tests; source geometry untouched |

Next Gate 2 source/code gap found by live caller audit: TelegramMenuPopup currently
binds only Theme/History/Bookmarks. Latest owner instruction explicitly requires
seven actions. After D4 exact-SHA FAST SUCCESS, extend this canonical popup with
Mistake Notebook, Topic-wise Test, PYQ and Logout callbacks to existing destinations/
logout confirmation; inspect existing source maps/row geometry/scroll/lifecycle
and previous whitelist tests before changes. No alternate popup or duplicate screen.

Gate 1 D4 commit 936e93aae0273daea08ea0e14fd6f9538d60f7b7 exact-SHA FAST
41409105317 SUCCESS. Gate 2 authorized seven-action popup: current three-row
implementation and old tests explicitly removing four rows are superseded by the
latest owner instruction. Extend existing popup/scroll/row source primitives,
existing MistakesActivity/PracticeActivity/PyqActivity and logout flow; no alternate
screen, auth change or new asset import. ADAPTED EVE menu destinations.

Gate 2 seven menu actions: MET for wired production destinations and host popup
callbacks, ADAPTED. Existing TelegramMenuPopup/row/scroll/source background and
motion retained; all seven rows participate in accessibility/selector/animation.
MainActivity binds MistakesActivity, PracticeActivity, PyqActivity and a single
logout confirmation to its existing presence-revoke/cache-clear/sign-out flow.
Confirmation is disposed on teardown; its message uses English/Hindi resources.
A new actual-popup test clicks all six non-theme actions, checks dismissal before
callback and rejects stale detached-row repeat callbacks. This last case failed
before the isShowing guard and passes after it. Existing theme handoff tests stay.
Old removed-row assertions are superseded by latest owner's explicit seven-action
requirement; all-seven geometry/accessibility checks replace them, not disabled.

Source mapping: original ItemOptions.java:1390-1407, ActionBarMenuSubItem.java:91-98
and ActionBarPopupWindow.java:1024-1069 at full pin
3de37c8cfb039452be7398909461da6288babf15 -> existing TelegramMenuPopup.kt /
popup_telegram_menu.xml / TelegramPopupSourceMotion.kt: ADAPTED native EVE host,
unchanged source-based background/roles/timing; seven destinations are EVE business
callbacks, not an official Telegram equivalent. No new source/asset transplant.
Matched native appearance/Back/IME/large-font matrix remains OPEN; compiled native
fixture now checks seven rows. Final serial local broad Gradle PASS: 770 JVM tests
/ 179 suites, zero failures/errors/skips, AndroidTest compilation/lint/x86_64 APK.
Python helper 59/docs/claim/diff checks PASS. Carrying commit requires FAST.

Seven-action popup first commit a6c0e042f58135d234b450e50c3c5f96ce38757c
FAST 41411244614 SUCCESS. Second/final planned commit for this feature: remove
remaining hardcoded History/Bookmarks/Day/Dark labels via existing Android string
resources and complete Hindi/RTL/font host coverage; reuse existing original
large_notes/msg_filehq icons instead of generic edit/PYQ vectors. All 8 relevant
density assets compared with official Git pin bytes; no new binary imported.
No drawer paths removed merely for sharing a destination with overflow.

Seven-action feature second scoped batch locally MET: History/Bookmarks/Day/Dark
labels now use Android English/Hindi resources; original Notebook/PYQ source
icons reused. All 8 notes/paper density bytes match immutable official Git blobs
at 3de37c8cfb039452be7398909461da6288babf15 (large_notes.webp/msg_filehq.webp).
No new asset import/license clearance claim. Actual popup tests now run Day,
Night and Hindi/RTL at 200% font, assert full localized accessibility row labels,
all six callbacks after dismissal and stale-row no-op. Initial fixture configuration
override was too late; fixed using Robolectric's font-scale API, no product hack.
Serial broad Gradle PASS: 772 tests / 179 suites, zero failures/errors/skips;
AndroidTest compilation/lint/x86_64 assemble PASS. Python helpers 59/docs/claims/diff
PASS. This is the second planned implementation commit for the seven-action
feature; remaining matched native/source-screen evidence stays OPEN.

Gate 2 Home exam-load integrity requirement recorded before code: existing
ExamRepository.getExams treats success=true/data=null as an empty list. Actual
HomeViewModel.load then saves/publishes that fabricated empty dataset, replacing
valid cached exams; AdminViewModel/EditExam/Settings also call this repository.
Repair the canonical response validation only; preserve confirmed empty list,
sorting, category emoji mapping, cancellation and server contracts. EVE business
adaptation, no Telegram equivalent/transplant. Scoring/locks/timers/backend remain
unchanged. First reproduce malformed success with actual repository test.

Home exam-list integrity scoped repository contract MET locally: malformed
success/null now throws through existing error handling instead of fabricating an
empty dataset. Regression first failed on old code, then passes; confirmed empty,
nonempty ordering, denied response with stale data and cancellation are tested.
HomeViewModel cache preservation follows inspected caller control flow (INFERRED
from code, not an authenticated Activity/device test). Worker GET /api/exams
returns an array on confirmed success; deployed API/scoring/attempt logic unchanged.
Serial broad Gradle PASS: 775 JVM tests / 180 suites, zero failures/errors/skips;
AndroidTest compilation/lint/x86_64 assemble PASS. Python helpers 59/docs/claim/diff
PASS. Carrying commit still requires exact-SHA FAST.

Gate 2 shared native-dialog retention: base e1885471f913d7499116f9e4fe6fa1a6ae31d5bf
FAST 41457197984 SUCCESS. Official AlertDialog.java:157,1161-1173,1526-1528 at
3de37c8cfb039452be7398909461da6288babf15 exposes dismissDialogByButtons; EVE's
existing renderer omits it and always dismisses. Active ManageQuestionsActivity
showAddQuestionDialog early validation returns therefore close the form; server
errors only enter an unobserved VM message on this screen. Scope: source-derived
button-dismiss policy in existing renderer, explicit active-form validation/save
retention, one editor window, duplicate-submit gate and teardown cleanup. Preserve
default confirmation dismissal and all authorization/API/question/scoring contracts.
AdminActivity's similarly named old private add dialog has no active caller found;
it is not deleted or treated as a second active implementation.

Shared button-dismiss policy and question form runtime retention scoped MET locally.
NativeDialogDismissPolicyTest exercises actual Dialog/EditText/default confirmation
behavior in Day/Night. Restoring old unconditional policy made all 3 tests FAIL;
source policy makes them PASS. QuestionSubmitConfirmationTest verifies real VM/repo
denial/success/cancellation with isolated API. QuestionFormInteractionTest executes
the SAME canonical createAddQuestionDialog used by the gated Activity, through its
real ViewModel and repository, replacing only API transport with held continuations:
invalid input -> no request; pending -> one request/disabled controls; denial ->
inline accessible error + preserved draft; retry -> server success closes dialog.
No role grant/authentication pass inferred from these UI fixtures. Fresh Activity
gate/independent Worker authorization remain unchanged. Existing private form body
moved into a stateless builder in the same file to test actual callbacks without
bypassing the protected Activity; no alternate renderer/repository/state manager.

| Upstream path:lines@SHA | EVE path:entry | Classification | Evidence / limits |
|---|---|---|---|
| ActionBar/AlertDialog.java:157,1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | designsystem/TelegramNativeAlertDialog.kt:63,235 | ADAPTED scalar policy/setter in existing Kotlin renderer; defaults unchanged, custom forms explicitly own dismissal | actual dialog tests; complete renderer visual parity OPEN |
| EVE business form; no official Telegram exam-question equivalent | ui/admin/ManageQuestionsActivity.kt:createAddQuestionDialog; AdminViewModel.kt:addQuestion | ADAPTED validation, confirmed-save callbacks, inline errors, busy controls and teardown | actual production form/VM/repo host tests; rotation/process-death draft/request restoration OPEN |

Inline error lives inside the dialog (Activity bulletin otherwise renders behind
its window), uses native live-region text and measured rectangle scrolling.
Default builder confirmations keep automatic dismissal; Cancel explicitly dismisses
retained forms. Pending submission locks duplicate taps/Back/cancel. Permanent
host destruction dismisses the owned window. AdminViewModel exposes optional
failure callback and propagates cancellation; all-default injected repository keeps
its no-arg Activity factory and no unauthorized eager loads. No question/API/scoring
or permission schema changed. Local serial broad Gradle PASS: 782 tests / 183
suites, zero failures/errors/skips; AndroidTest compile/lint/x86_64 assemble PASS.
Python helpers 59/docs/claim/diff PASS. Carrying commit requires FAST; whole shared
dialog/Admin/native/reference acceptance remains NOT MET/OPEN.

## C5 question-list request ownership (active recovery)

R1: Only the current selected exam/request may publish questions or load errors.
R2: Selection changes/empty selection must invalidate older requests and remove
old-exam rows; confirmed same-exam refresh may retain its existing rows.
R3: Mutation/import refresh must not select an obsolete exam after navigation.
R4: Preserve the selected exam identity across list refresh/reordering, using the
existing ViewModel and spinner; no second state owner or screen. Existing server
rights/API/scoring contracts stay unchanged. Whole rotation/process-death editor
restoration remains OPEN; this race is an independently implementable prerequisite.
Base 412edc6ed80ca867635cbcf8cee06e56196728f4 FAST 41462667732 SUCCESS and docs
41462667822 SUCCESS. Official HEAD rechecked unchanged 3de37c8cfb039452be7398909461da6288babf15.
Before repair: AdminViewModel.loadQuestions has no request/context guard; active
ManageQuestions spinner and mutation callbacks call it with captured exam IDs.
C5 scoped status NOT MET pending real VM/repository out-of-order response tests.

C5 scoped implementation: existing AdminViewModel owns the selected exam ID and
monotonic read generation. Older success/error cannot publish after any later load,
including empty selection; cancellation propagates. Switching exam clears actionable
old rows immediately, while same-exam refresh retains confirmed rows. All VM
question mutations and the active bulk-import completion use refreshQuestions,
which does not change selection. The existing spinner binding is extracted in the
same production file, preserving identity across adapter reorder/rebind and
suppressing its selected-item duplicate event. No second repository/state owner.
QuestionListOwnershipTest drives real VM/repository transport continuations and
actual Spinner callbacks; the first six tests all FAILED before repair with
specific obsolete data/error/request assertions. The spinner recreation check uses
a new View with the retained VM; it is NOT authenticated Activity rotation or
process-death proof. No Telegram question-admin business equivalent is claimed.

| Telegram reference | EVE active path | Classification / reason | Evidence / uncertainty |
|---|---|---|---|
| No Telegram EVE exam-question request contract | AdminViewModel.kt:loadQuestions/refreshQuestions; ManageQuestionsActivity.kt:bindQuestionExamChoices/importQuestionsForSelectedExam | ADAPTED EVE business ownership around existing Telegram-derived screen primitives; no new upstream code/assets | held real repository responses + native Spinner callbacks; fresh gate/Worker unchanged; full Admin/device/process restoration NOT MET |

C5 local final checks: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 791 tests / 184 suites, zero failures/errors/skips. Nine new ownership tests PASS; initial six fail-before evidence in question-list-before.log. Python CI helpers 59/docs/acceptance/diff PASS. Historical bare SDK 35 Main Thread exception also appeared in this successful suite; root cause remains UNKNOWN, not suppressed. FAST still required for carrying commit.

## C1/C5 active exam rename contract and shared form retention

Predecessor 03c07dc45031b9c2361db53e7ba650dca729bdce FAST 41466628937 SUCCESS,
docs 41466628886 SUCCESS. No concurrent main changes; successful C5 ownership batch
preserved. R1: repair real Android examName/Worker newName payload mismatch while
preserving installed-client compatibility. R2: missing/invalid/denied/duplicate
rename must not report success or create false audit/cascade changes. R3: retain
invalid/server-failed names in the SAME source-adapted native dialog, gate pending
requests, own teardown and close only on confirmed save. R4: preserve current
manage_exams Worker right, existing rename cascade/data/scoring contracts, and
fresh Android entry gate. No deployment/migration authorized. Scope NOT MET before
repair; full configuration/process-death editor restoration remains independently
OPEN. No official Telegram exam-renaming business subsystem exists; reuse the
existing ADAPTED AlertDialog button-dismiss policy, not another renderer.

Rename scoped implementation: canonical Android repository now sends newName.
Worker also accepts installed clients' examName only when newName is absent;
invalid canonical values never silently fall back. Malformed types/empty names
fail without writes. Target update checks the EXISTING full-exam-update sibling
name rule (same normalized parent), in the atomic write itself. Unchanged names
preserve existing legacy behavior. Audit immediately follows UPDATE and requires
changes()=1. Its fresh unique audit ID gates all four existing name cascades;
missing/conflicting targets cannot mutate historical rows or emit false audit.
D1 result confirmation is mandatory; missing targets return 404, conflicts 409.
No scoring, attempt identity, schema, question data or role contract changes.

Active ManageQuestions rename uses its original builder body extracted in the same
file for actual callback tests. One owned window is dismissed on teardown. Existing
source-adapted native dialog policy retains invalid/server-failed input; pending
controls/input/Back are locked; explicit failure restores retry and visible native
EditText error; success alone dismisses. Labels/errors/loading have English/Hindi
resources. Old private AdminActivity.rename helper has no caller found; it stays
untouched rather than being falsely called a duplicate active implementation.

| Upstream path:lines@SHA | EVE production path | Classification / technical reason | Evidence / limits |
|---|---|---|---|
| ActionBar/AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | designsystem/TelegramNativeAlertDialog.kt:setDismissDialogByButtons -> ui/admin/ManageQuestionsActivity.kt:createRenameExamDialog | ADAPTED existing Kotlin renderer's native button policy with EVE form validation/async state; no new source import | actual Dialog/EditText/action callbacks; native authenticated rotation/full visual parity OPEN |
| No Telegram EVE exam rename/backend contract | ExamRepository.kt:renameExam -> AdminViewModel.kt:renameExam; backend/src/routes/exams.ts:PUT /:id/rename | ADAPTED EVE business integration; fresh manage_exams middleware/cascade preserved | real VM/repository transport plus actual Hono handler/all-migration isolated SQLite; production deployment NOT performed |

Fail-before: both actual form tests fail (invalid/unconfirmed dismissal, plus
old examName payload assertion); four of six initial real Worker cases fail
(legacy payload, malformed value, missing target fake success, sibling conflict).
Scoped passing tests include actual pending/rapid/denied/retry/success form path
and Hindi/RTL/Night/200% error/cancel path. No assertion/gate deleted. The SQLite
fixture serializes batches to model D1 atomic batches; concurrent sibling rename
and late-cascade/audit-trigger rollback are host evidence, not deployed D1 proof.
Full editor rotation/process-death restore, complete Admin matrix and native
source visual acceptance remain NOT MET. Worker protection is staged only until
a separately authorized deployment; no production safety claim is inferred.

Rename final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS (794 JVM tests / 185 suites, zero failures/errors/skips). `cd backend && npm test && npm run build` PASS (299 tests, no failures/skips); eight new real-handler cases. CI helpers 59, docs, structural acceptance and diff checks PASS. Bare historical SDK 35 Main Thread line still UNKNOWN in successful JVM suite. Carrying exact-SHA FAST required; FULL/native/reference/legal acceptance not established. After green continue remaining shared-dialog/Home/Profile production paths; next Profile category/data restoration/identity audit, preserving no Add bio/About and existing confirmed-store/request-owner mechanisms.

## B4/C5 profile editor restoration ownership

Base 196c59d55b9710fcfb99bb1f2b4e8629f6bb9aee exact-SHA FAST 41469072072 SUCCESS,
docs 41469072384 SUCCESS. No concurrent changes. R1: restored editor drafts must
remain tied to the account that created them, never acquire the newly current
Firebase identity. R2: bind fresh editor launch metadata to the initiating profile
owner; reject mismatched/restored-unverifiable ownership before private view-state
restoration. R3: preserve same-account native draft restoration and existing live
auth listener teardown, server readback/request-owner checks and all four retained
editor implementations. Do not re-add visible Bio/About or change profile data/API.
Existing bind only checks current identity on each onCreate, losing original owner
across destruction; scoped NOT MET pending actual Activity regression evidence.
This is EVE identity isolation, not an imported Telegram account subsystem.

B4 restoration scoped implementation uses the EXISTING ProfileEditorAccountGuard
and native SavedStateRegistry. Original owner UID is saved privately with the
editor's existing OS view state; no account repository/preferences/API introduced.
Profile's three active editor launches carry their initiating owner. Restored
unknown/mismatched ownership and delayed mismatched launch finish BEFORE inflating
private controls. Same-owner recreation restores native text normally. Existing
auth listener still closes on replacement and unregisters on destroy. A legacy
restored draft without verifiable owner is closed instead of rebound to a possibly
different user; fresh legacy internal launches retain current-account compatibility.
All four editors remain non-exported. No Bio/About row or backend schema change.

| Upstream reference | EVE production path:lines | Classification / reason | Verification / limits |
|---|---|---|---|
| No Telegram authentication/account subsystem imported (owner exclusion) | ProfileEditorAccountGuard.kt:14-38 -> ChangeName/Phone/Username/BioActivity.onCreate, before binding inflation | ADAPTED EVE identity adapter; native SavedStateRegistry ownership around existing source-derived editors | actual four Activity lifecycle/native draft tests with SDK identity boundary isolated; real Firebase/OS-kill/device acceptance OPEN |
| Existing source-derived Profile action/cells, unchanged geometry | ProfileActivity.kt:337,388,398 | ADAPTED launch ownership metadata only; no visual parity claim or new renderer | delayed launch rejected by actual targets; authenticated Profile launcher/runtime matrix OPEN |

ProfileEditorRestorationTest uses actual production Activities/guard/native saved
view state, not an isolated selector. Test-only SDK shell returns immutable synthetic
identities/listeners, skips SDK initialization, carries no credentials and performs
no Firebase/API writes. Fixture bootstrap failures (SDK shell construction and
setup(null)) were fixed first; they are not product regression evidence. With the
working fixture, two original tests FAIL before repair (restored identity and
fresh delayed launch), while same-owner draft and live signout tests PASS. Fixed
four-test run PASS; expanded per-editor rejection and unknown-owner cases cover all
retained editor paths. These are host lifecycle evidence, not physical-device or
real Firebase authentication proof. Pending-save/process-death reconciliation and
whole Profile/source visual acceptance remain OPEN/NOT MET.

B4 ownership final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 805 JVM tests / 186 suites, zero failures/errors/skips. Eleven new Activity lifecycle tests PASS; Python helpers 59/docs/structural acceptance/diff PASS. Existing backend unchanged from predecessor FAST (299 tests). Historical bare SDK 35 Main Thread warning persists UNKNOWN; no suppression. Exact-SHA FAST required. Next independent Profile investigation: username availability callback cancellation/error ordering, field pending-save restoration and remaining source UI paths; preserve this ownership guard and no Bio/About rows. Full mission/device/GPL/FULL acceptance NOT MET.

## B4/F1 username status roles and accessible localized rendering

Base 2212767c4de9e0d23edd4439ff5bba0ab85215a3 FAST 41472089611 attempt 2 SUCCESS,
docs 41472089940 SUCCESS. Attempt 1 failed only CMake 3.22.1 remote TLS handshake
in prepare-qr-native; unchanged exact-SHA rerun passed. No gate weakened or bypassed.
R1: replace current hardcoded available/error/checking ink with real source theme
roles through the canonical engine and live palette callbacks. R2: retain current
EVE 5-character/server uniqueness contract (official source now accepts four for
its Telegram-specific availability service); classify validation ADAPTED. R3:
localize existing editor labels/status/help in English/Hindi, preserve real API
errors, and expose status changes to accessibility. No new screen/state owner,
Telegram network dependency or API/scoring/identity change. Status NOT MET before
actual native status/contrast/theme tests. Full editor geometry/reference still OPEN.

Username rendering scoped implementation: original ThemeColors scalars and existing
byte-identical night.attheme roles now resolve through TelegramColorRoles / the
canonical ResolvedPalette. All three statuses repaint from current palette without
retyping; semantic tag prevents generic theme repaint from taking ownership. The
existing accessibleForeground policy adjusts ink for both actual field canvases;
normal underline uses current accent, invalid underline uses error role. Existing
validation, 300ms debounce, cancellation, same-input response guard and server
save/readback remain unchanged. Empty status clears semantic error paint. Source
comment corrected from false Telegram-exact claim: official source now checks four
characters; EVE backend auth.ts:18 requires five and is preserved.

| Official upstream path:lines@SHA | EVE production property | Classification / reason | Evidence |
|---|---|---|---|
| ActionBar/ThemeColors.java:94,96,106@3de37c8cfb039452be7398909461da6288babf15 | theme/TelegramColorRoles.kt three status defaults | EXACT scalar values only, not file/screen; no invented defaults | direct parsed original/EVE numeric comparison PASS, hashes in color-migration/source-role-contract.json |
| assets/night.attheme:145,178,425@same full SHA | assets/themes/night.attheme -> canonical role resolver | EXACT existing asset bytes; resolved ink ADAPTED by existing contrast policy | full-byte cmp + SHA-256 c93fb2934d2684888a5d9e72a36361c0789d6bc745db23043da7eb88155c5469 |
| ChangeUsernameActivity.java:1205-1280,1288-1353@same full SHA | ChangeUsernameActivity.kt:setStatus/onApplyThemePalette + activity_change_username.xml | ADAPTED semantic source roles, EVE 5-character validation/network contract, native Kotlin editor and accessible ink/localized text | four actual Activity/View callback tests fail before repair, pass after; matched geometry/device parity OPEN |

English/Hindi resources cover existing local status/help/title/Back/Done labels and
fallback failures. Real server errors are preserved; no fabricated translation of
server meaning. Status is a polite live region. Actual native tests cover available,
invalid and checking colors/contrast, live Day/Night callback without retyping,
Hindi/RTL/200% validation and native accessibility property. Checking test tears down
before its 300ms debounce; no availability request or account write performed.
Same-account and mismatch lifecycle regression suite still passes. These tests do
not prove TalkBack announcements or full small-window/font geometry on devices.
THIRD_PARTY_NOTICES updated; ThemeColors has no per-file header grant, so root GPL
and combined-work legal gate remain OPEN; no self-certified redistribution.

Username final local checks: `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 809 tests / 187 suites, zero failures/errors/skips. Four new actual Activity status/theme/contrast/localization tests PASS. Python helper 59/docs/claims/diff PASS; three source scalar comparisons and Night full-byte identity PASS. Bare historical SDK 35 Main Thread line still UNKNOWN; no suppression. Exact-SHA FAST required; complete Profile/F1/source visual/device acceptance NOT MET.

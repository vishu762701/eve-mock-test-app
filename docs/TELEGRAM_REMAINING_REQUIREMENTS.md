# Telegram mission live requirement ledger

Status: PARTIAL; no screen or distribution acceptance. This supplements, and does
not replace, the feature/commit/CI history in TELEGRAM_FINAL_MISSION_CHECKPOINT.md.

Recovery 2026-11-05: clean EVE main 8279af6f035cac6dee0e54d138623d57b1c80d8e,
refetched over HTTPS; latest commits and exact-SHA Actions inspected. FAST
run [41964090773](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41964090773)
SUCCESS. Terminal FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006)
and docs [41960297982](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960297982)
SUCCESS at SHA 1c74e97bac3ac8ff53aca1595445cad3893175a2. Build/backend/Firebase
and both Native UI emulators (gesture and three-button) passed 100% green with zero
failures. No concurrent implementation found.

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
| C4 | Poll destructive text/server-confirmed success | existing polls UI/API | EVE business/source dialogs | Android themes/Worker response | NOT MET full device acceptance | scoped response/row controls: 14c9e61b / FAST 41406623011 and a8433b64 / FAST 41407181451; green work preserved | device labels/whole action matrix; no duplicate poll iteration |
| C5 | Refresh/nav/rotation/concurrency/role separation | admin Activities/ViewModels | ADAPTED Telegram primitives | mocked server/device | NOT MET | no complete action/lifecycle matrix | verify without live production writes |
| D1 | Home seven overflow actions/source popup/dialogs | MainActivity/shared popup/dialogs | ActionBarMenuItem/PopupWindow@T | current destinations | NOT MET | earlier reports; fresh caller audit pending | inspect seven actions and anchoring |
| D2 | Drawer gestures/insets/scrim/Night pure black | Home drawer | DrawerLayoutContainer@T + owner #000000 exception | Android input | NOT MET full acceptance | existing implementation preserved | test drag/fling/navigation/theme matrix |
| D3 | Toolbar seam/search/IME/rapid/theme/Back/restore | MainActivity/HomeAppearance | source parent/search audit UNKNOWN | Activity/IME | NOT MET | no fresh full evidence | inspect source and shared callers |
| D4 | All/Other one selection; selected no-op | canonical HomeAppearance renderer + live selection getter | FilterTabsView source property / EVE filter adapter | discovery data contract | MET scoped callbacks | 936e93aa / FAST 41409105317; actual reused Chip callback tests retained in 883-test suite | physical appearance/motion matrix remains D5/F |
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

## B5/B7 profile editor restoration ownership

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

B5/B7 restoration scoped implementation uses the EXISTING ProfileEditorAccountGuard
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

B5/B7 ownership final local verification: serial `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug -Peve.nativeAbis=x86_64` PASS; 805 JVM tests / 186 suites, zero failures/errors/skips. Eleven new Activity lifecycle tests PASS; Python helpers 59/docs/structural acceptance/diff PASS. Existing backend unchanged from predecessor FAST (299 tests). Historical bare SDK 35 Main Thread warning persists UNKNOWN; no suppression. Exact-SHA FAST required. Next independent Profile investigation: username availability callback cancellation/error ordering, field pending-save restoration and remaining source UI paths; preserve this ownership guard and no Bio/About rows. Full mission/device/GPL/FULL acceptance NOT MET.

## B5/B7/F1 username status roles and accessible localized rendering

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

## C5/E1/E3 shared question-response integrity

Base main 372b108a0b1b1c957566c53eeb7fd69b139e7a3e FAST 41475078791 SUCCESS,
docs 41475079226 SUCCESS. Category live-theme repair is separately tested/archived
pending explicit owner commit-limit exception; it is NOT applied to this tree.
R1: missing question data must not masquerade as confirmed empty questions.
R2: preserve valid empty arrays, existing order/mock/topic/PYQ filters and cancellation.
R3: actual Admin VM refresh must retain confirmed same-exam rows on malformed data.
Canonical ExamRepository.getQuestions feeds AdminViewModel/EditExam/analytics/flagged
questions/results and Test/Practice/PYQ filters. Worker questions.ts:134-153 always
returns a nonnull mapped array and independently enforces manage_questions/answer
redaction. No API/permissions/scoring/answer model change. EVE business validation,
no Telegram exam-question contract/equivalence claim. Scoped NOT MET before tests.

Requirement-ID normalization: prior editor continuation labels called B4 refer to
the owner's workstream summary, but the authoritative table above uses B4 for
category, B5 for editable fields and B7 for identity/lifecycle. Those continuation
headings are normalized to B5/B7 here; no requirement or evidence is dropped.

Category B4/F1 local repair remains NOT MET integration, pending one explicit
commit-limit exception. Both existing successful category commits were checked
live (feb9c888 / FAST 41163967661, 14a62972 / FAST 41165900930). Concrete native
ring failure reproduced; local repair passes 811 tests/188 suites, AndroidTest
compile/lint/x86_64 assemble and helpers59/docs/claims/diff. Patch, full source map,
files and metadata are safely archived under
/workspace/pending-category-theme-repair/; repair.patch SHA-256
236f8f9716ba51d0b14bbf7dec20a688cd63328ebac0227bfa03a6d11699e757.
It is NOT applied to main, committed or pushed. Owner question asks only for the
mandatory third-commit exception under ENGINEERING_STANDARD.md:31; no approval
by silence. `git apply --check` passed against base 372b108a. Reconcile before
applying if authorized; keep continuing independent requirements otherwise.

C5/E1/E3 scoped implementation rejects success without data and Gson null question
entries in the existing getQuestions method. Valid [] and API order remain intact;
no filtering/dropping malformed rows as if valid. All existing mock/topic/PYQ
wrappers share this guard. No correct-answer/nonblank-answer requirement added:
server-redacted student answers remain valid and scoring stays server-authoritative.

Evidence: initial real repository + actual Admin VM tests fail before repair
(malformed success becomes empty; confirmed rows erased). Separate real Gson
[null] test also fails before repair. New four repository tests and one added
VM refresh test pass after repair; existing request-generation/spinner/old-write
regressions preserved. The VM result/error retention is executed host evidence;
full authenticated Activity error presentation/device matrix is not inferred.
Worker questions.ts:134-153 actual mapped-array/redaction path unchanged; no
production reads/writes/deploy/migration or new source import. Source relationship:
EVE business contract -> ExamRepository.kt:getQuestions -> existing callers,
ADAPTED integration, no official Telegram exam-question equivalent/EXACT claim.

Final serial local Gradle unit/AndroidTest-compile/lint/x86_64 assemble PASS: 814
JVM tests / 188 suites, no failures/errors/skips. CI helpers 59/docs/acceptance/
diff PASS. The category review patch is NOT active in these results. Bare SDK 35
Main Thread line still UNKNOWN, not suppressed. Carrying exact-SHA FAST required;
whole C5/E1/E3/Admin/student acceptance NOT MET. First next independent UI
investigation after green: actual Profile popup backdrop paint/alpha lifecycle;
preserve already integrated source-based blur and do not fabricate Blur3 parity.

Owner decision received during question-response preparation: "Haan, ek additional
repair commit authorize hai". ONE additional category live-theme repair commit is
now authorized, with mandatory exact-SHA FAST. Original review patch/hash remains
unchanged; apply only after the carrying question-response gate succeeds. This is
not permission for further cosmetic category iterations or another release/deploy.

## B4/F1 authorized category live-theme repair integration

Owner explicitly authorized ONE additional repair commit beyond the two already
successful category commits. Reviewed patch 236f8f9716ba51d0b14bbf7dec20a688cd63328ebac0227bfa03a6d11699e757
applied cleanly after main 381a76a610a3f1847f59a4c48939d7f2e4e6aea8 exact-SHA FAST
41479914039 SUCCESS / docs 41479913807 SUCCESS. No approval re-requested. Existing
color method becomes updatePalette and canonical theme traversal calls it. No new
renderer/state owner/style change. Original EVE accent/secondary adaptation is
preserved, not silently relabeled original radioBackground keys. Upstream mutation
mapping and exact hashes are in EVE_PROFILE_CATEGORY_PROVENANCE.md. Native
unselected-ring test fails before repair; selected-dot preservation control passes.
Both pass after repair, including state/accessibility/callback preservation.

Final local serial unit/AndroidTest-compile/lint/x86_64 assemble PASS: 816 tests /
189 suites, zero failures/errors/skips; helpers59/docs/claims/diff PASS. Own carrying
FAST required. This consumes the specifically authorized third category repair;
no further cosmetic iteration authorized by that exception. Whole category/save/
rotation/device/source reference acceptance remains NOT MET. No API/scoring/data/
permission/deployment or legal-gate changes.

Next higher-priority actual C1/C5 finding: EditExamActivity entry verifies
manage_exams but exposes add/import/edit/delete question actions that require
manage_questions on Worker. Its independently implemented add form also duplicates
the canonical active ManageQuestions form and auto-dismisses failed validation.
Investigate/fix that real control path before cosmetic popup alpha refinement;
Worker authorization already denies wrong rights. Also UPDATE/DELETE missing
question rows currently return false success/audit; existing Undo uses UPDATE after
hard delete, so do not claim restore works. Trace contracts before any data/Undo
behavior decision. No live mutations authorized.

## C1/C5 authoritative question mutation confirmation

Base d2b649f5bac8f4b86549ff854385bafbf3d97c6a FAST 41481845128 SUCCESS, docs
41481845112 SUCCESS. Approved single third category repair is integrated/green;
no further exception inferred. R1: UPDATE missing target must fail without false
QUESTION_EDITED audit. R2: DELETE preserves existing idempotent absence success,
but audit must record only a row actually deleted. R3: same atomic batch rolls back
write/audit on failure; existing manage_questions middleware remains authoritative.
R4: prove real DELETE + UPDATE cannot restore a hard-deleted question; do not invent
an unsupported restore service or fake UI success. Existing EditExam Undo uses
UPDATE after immediate hard-delete and current route reports success despite zero
rows. Android role/confirmed-dialog callers will be reconciled after this contract.
No deployment/migration/scoring/answer-redaction change. Scope NOT MET before tests.

Question mutation server scoped implementation: UPDATE target/audit confirmation
is checked from the same atomic batch, missing target returns 404 and no edit audit.
DELETE keeps its existing idempotent confirmed-absence success; zero deleted rows
produce no fabricated deletion audit. Missing/unexpected batch confirmation fails
closed. Both actual changes and audit remain one transaction; permission/validation
middleware unchanged. No restore endpoint, schema or score/answer-key change.

Eight actual Hono/all-migration isolated SQLite tests: valid edit; missing edit;
delete+repeat audit; denied manage_exams-only despite local admin claim; malformed
edit; edit/delete audit-trigger rollback; DELETE then UPDATE cannot restore.
Working-fixture fail-before: 3 tests FAIL, 5 PASS. First fixture POST trailing-slash
404 was corrected to the real Android route before counting product failures.
After repair full `cd backend && npm test` PASS: 307 tests, zero failed/skipped;
`npm run build` PASS (TypeScript). CI helpers59/docs/claims/diff PASS. Android source
unchanged from verified category FAST and 816-test local integration; no redundant
local Android rebuild claimed. Required carrying exact-SHA FAST pending.

Source relationship: EVE questions.ts PUT/DELETE -> existing Android repository/
callers, ADAPTED EVE business contract; no Telegram exam-question backend equivalent
or source import. Whole Admin caller/Undo acceptance remains NOT MET. Owner product
question pending: keep immediate hard-delete with a staged safe restore API, or
Telegram-style deferred delete during existing 5-second Undo window. Current
AppUndoBar.kt:48 documents immediate deletion; original UndoView.java:384-403,
1701-1702@3de37c8cfb039452be7398909461da6288babf15 applies callback on expiry and
cancels it for Undo. No choice/approval inferred from generic resume message.
Continue independent fresh-role/active-form work; never fake a restored question.

## C1/C5 EditExam child actions and canonical Add presentation

Base main 5bf15f8444dd65d5e947b43a538404db701a500f exact-SHA FAST 41523395700
SUCCESS, docs 41523395561 SUCCESS. R1: use fresh returned rights for question
Add/import/edit/delete (manage_questions) and Emoji Studio (manage_content), while
preserving manage_exams entry/metadata editing. R2: stale privileged controls must
not invoke callbacks after identity replacement. R3: consolidate the TWO actual
active Add Question form builders (ManageQuestions and EditExam) into the existing
canonical presentation, retaining each screen's repository/state owner and title.
R4: active EditExam Add form retains invalid/failed drafts, bounded pending submit,
confirmed success and teardown. No new screen/ViewModel/auth bypass/role/API.
Unused similarly named AdminActivity forms remain untouched. Undo product choice
is independent/PENDING; no unsupported restore implementation assumed.

C1/C5 EditExam scoped integration: fresh returned rights now own question controls
and Emoji Studio visibility; question callbacks/rows recheck current host permission
state. Read-only exam role does not display a fabricated Correct answer label.
Shared AdminAccessGate binds its host to the initiating UID/email, hides protected
root and finishes on replacement/signout, and removes the listener on destruction.
This is UI defense in depth, never Worker authority or a local admin grant.

Two verified active Add builders now share createAddQuestionDialog in the existing
ManageQuestions source file. Its stateless submit/busy callbacks let EditExam keep
its OWN repository/list rather than adding a redundant AdminViewModel. Existing
screen titles preserved. Invalid/failed/pending/success behavior is the same
canonical native renderer; EditExam owns one window and dismisses on teardown.
Unused AdminActivity private Add form remains untouched; Analytics/Flagged uses of
the same XML are EDIT workflows and were not mistaken for duplicate Add paths.

| Source property | Actual EVE caller | Classification / reason | Evidence / uncertainty |
|---|---|---|---|
| AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 existing dismiss policy | canonical createAddQuestionDialog -> EditExamActivity/ManageQuestionsActivity | ADAPTED existing Kotlin/native renderer with EVE domain submit callbacks; no new imported code/assets | real protected Activity/widget callbacks + real ManageQuestions form/VM/repository test; full device geometry OPEN |
| No Telegram exam/admin auth subsystem imported | AdminAccessGate + EditExam fresh snapshot + QuestionManageAdapter callbacks | ADAPTED EVE server-owned authorization boundary, no client authority | actual gate/Activity lifecycle with isolated SDK/repository boundaries; real Worker denied-role tests in predecessor; not end-to-end Firebase proof |

Actual EditExamActionIntegrationTest before repair: 4 failures (invalid Add closes,
question/emoji wrong-right exposure, identity replacement), 1 owner/denied-entry
control passes. Seven expanded production Activity tests now PASS: role visibility,
owner access, denied entry no data reads, stale bound rows after identity change,
invalid draft, held pending/duplicate tap/denial/retry/success/list refresh, destroy/
late result no old-host reload, listener cleanup. Synthetic data/auth repositories
are test-only; no shipped auth bypass or production write. Source dialog contract
assertions retain all four modal paths, original mutations/cancel/destructive UI;
they now check three local builders + the one canonical Add call and mapped input
fields in their actual new owner. No assertion removed merely to hide a failure.

Final serial Gradle unit/AndroidTest-compile/lint/x86_64 assemble PASS: 823 tests /
190 suites, zero failures/errors/skips; helpers59/docs/claims/diff PASS. Backend
unchanged from exact-SHA predecessor (307 PASS); no redundant local Node rerun.
Required carrying FAST pending. Whole Admin/rotation/device acceptance NOT MET.
Undo semantics still awaiting explicit product answer; no fake restore fix claimed.
Known independent next issue: Analytics/Flagged editors expose question editing
under other entry rights and use ID OR text matching, which can select wrong same-
text question even when an authoritative ID is present. Fix that concrete target/
action path after this gate; do not silently fallback to an arbitrary question.
Bulk picker result restoration while fresh gate is pending is also OPEN; current
callbacks fail closed instead of importing without fresh rights/context.

## C1/C2/C5 Analytics/Flagged question edit identity and response integrity

Base e83b2d4c918340d18da5bddb934bfddd2abdb80e FAST 41527865210 SUCCESS, docs
41527865279 SUCCESS. R1: authoritative question ID wins over duplicate text;
unknown ID never edits another row. Legacy ID-less fallback must be unique and
within the requested exam. R2: fresh manage_questions controls nested edits;
parent analytics/moderation rights alone do not grant it; Exam Editor neutral
navigation also needs manage_exams. R3: reuse canonical field presentation for
both same-layout EDIT workflows, preserve each domain repository/action/title and
copy existing question metadata, validate/preserve failed/pending input. R4: report
load failure cannot become fake all-resolved; grouping cannot resolve another
exam's same-text reports. R5: failed boolean flag resolution cannot report success.
No new APIs, migration, score/answer exposure, Telegram service or state owner.
Undo product choice remains separately pending; do not infer its answer here.

## C1/C2/C5 question review target, nested rights and response integrity

Recovery base e83b2d4c918340d18da5bddb934bfddd2abdb80e: exact-SHA FAST
[41527865210](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41527865210)
SUCCESS, documentation 41527865279 SUCCESS, no published artifacts. Live main and
official Telegram HEAD rechecked; upstream remains
3de37c8cfb039452be7398909461da6288babf15 (tree
cc3c6ccf48e4167fdec764dfd04e765043f55093), official origin, tlottie gitlink
31f1b542f88e7b4be9a01e749920d857535fc715. Optional uninitialized submodules are
not claimed verified. Existing successful QR/Home/Profile/poll code preserved.

R1: resolve known question ID exclusively within the requested exam; no same-text
replacement for a missing known ID. Legacy ID-less reports retain unique scoped
text/legacy-ID compatibility. Ambiguous, cross-exam and unidentified targets fail
closed. R2: actual Analytics and Flagged row callbacks use the fresh gate's
manage_questions right; parent view_analytics_logs/moderate_feedback alone does
not grant question editing, and Exam Editor navigation needs manage_exams.
R3: both actual same-layout EDIT workflows now use createQuestionEditorDialog in
ManageQuestionsActivity, extending the existing canonical presentation rather
than adding a renderer/Activity/ViewModel. Add callers renamed with it. Existing
ID, Hindi fields, topic, PYQ and other metadata survive edits. Missing correct
answer requires selection instead of silently choosing A. Invalid, pending and
failed drafts stay open; owned windows dismiss on teardown, stale account callbacks
cannot submit. R4: failed/missing analytics/report data raises an error, never fake
zero/all-resolved success; valid empty lists and sort order remain supported.
Legacy report grouping is scoped by exam/type/identity, not global text. R5: an
updated question plus failed report dismissal is explicitly partial and retains
the draft, never a fully successful edit. Cancellation propagates in touched paths.

| Upstream property | EVE path:lines | Classification and evidence |
|---|---|---|
| TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | ManageQuestionsActivity.kt:368-487 -> AdminAnalyticsActivity.kt:123 / FlaggedQuestionsActivity.kt:132 | ADAPTED existing native Kotlin renderer/button-dismiss policy plus EVE validation and async action callbacks; actual Activity/native dialog tests, not whole original UI parity |
| No Telegram exam-question/admin backend equivalent imported | QuestionEditorTarget.kt:6; AdminAnalyticsRepository.kt:10; FlaggedQuestionRepository.kt:62 | ADAPTED EVE identity/data contracts; real repository response tests and protected Activity callers; no source transplant or license change |

Complete caller chain: fresh AdminAccessGate -> existing row adapters -> original
Activity lifecycle/repository owner -> scoped target resolver -> shared native
form -> ExamRepository.updateQuestion -> authenticated Worker questions.ts:265
manage_questions. Flag resolution additionally requires existing Worker
moderate_feedback reports.ts:162. No new API, local authority, answer exposure,
production request/deploy/migration, scoring, auth, or user-data changes.

Before repair: four real protected Activity cases failed (wrong same-text target,
nested-right exposure); four real repository cases failed (false empty/zero,
cross-exam grouping, swallowed cancellation). Expanded tests include eleven
protected Activity cases, five target-resolution cases and five real repository
cases. Test auth/transport boundaries are isolated fixtures, never shipped grants.
Two broad source-contract assertions initially failed because they assumed local
builders. They now trace the actual canonical native host, view, cancel/dismiss
policy and mutation callbacks; no native/authority/destructive assertions removed.

Local final integration PASS: 844 tests / 193 suites, zero failures/errors/skips;
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 (JDK21/Gradle8.7). Lint 0 errors / 1634
warnings. Helpers59, documentation, acceptance-claim structure, four verbatim
upstream imports and git diff --check PASS. Backend unchanged from green base
(307 tests in prior exact-SHA FAST); no redundant local Node run claimed. Bare
SDK35 Main Thread shutdown exception recurred without a stack trace; UNKNOWN P2,
not converted into a device/runtime PASS. Carrying commit/exact-SHA FAST pending.
R1-R5 MET at scoped code/host level, pending FAST integration; whole Admin/source/
runtime acceptance NOT MET.
Physical GPU/camera/reference/accessibility/navigation matrix and qualified human
GPL review remain OPEN/BLOCKED; no FULL or verified publication claimed.
First next independent defect: concurrent Analytics time-range loads (and Flagged
report-type loads) can publish stale results after a newer selection. Reproduce
through existing owners, then fix scoped request ownership; do not create parallel
state owners. Question Delete/Undo product choice remains explicitly unanswered;
no generic resume message is treated as an answer. Bulk-picker pending-gate URI
restoration also remains OPEN. QR acceptance stays OPEN; no speculative QR work.

## C3/C5 Analytics and report-filter read ownership

Base 9cf0523ed859b68e70c03911b09d08dce97a2634 exact-SHA FAST 41532575336
SUCCESS, docs 41532575497 SUCCESS; build/backend/Firebase passed, native UI and
verified publication skipped. R1: only the latest selected range/exam/refresh may
publish Analytics success or failure; obsolete exam reads must not start question
reads. R2: only the current report-type request may replace rows/empty state or
hide loading; old actionable rows must not survive a type change. R3: actual
controls and lifecycle restoration must agree with the selected request, with
selected-item no-op and no extra state owner. R4: cancellation/teardown must not
publish stale UI. R5: the Analytics screen must not present hardcoded 99.8% crash-
free/zero-crash/zero-ANR values as fetched telemetry. Keep existing console link;
no new Firebase telemetry permissions, service or invented data. Reproduce through
existing production owners, then scoped regression tests; no speculative QR work.

## C3/C5 Analytics/report read ownership implementation and evidence

Base main 9cf0523ed859b68e70c03911b09d08dce97a2634 exact-SHA FAST
[41532575336](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41532575336)
SUCCESS, docs 41532575497 SUCCESS. That predecessor's R1-R5 question-review
integration is verified; full screen/device/source parity remains OPEN. Its FAST
skipped native UI/publication, with no verified APK artifact.

Current R1-R5 code/host scope: canonical Analytics ViewModel cancels the prior job,
generation-checks before a dependent question read and before success/error, and
invalidates on clear. Existing Flagged Activity does the same in its original
lifecycle owner; switching type removes old actionable rows and invalidates stale
bound Edit/Dismiss callbacks. No parallel repository/renderer/ViewModel introduced.
Both existing controls restore selected requests before initial protected loads;
saved Activity state is persistence, not a new data owner. Selected filter taps are
no-ops. Native report chips inherited non-checkable Material Assist defaults;
only these TWO actual filter controls now enable checkability/single selection.
Shared Assist style, Home filters and existing UIArc rendering/physics untouched.

Analytics no longer displays static 99.8%, zero fatal crashes/ANRs or an unverified
Crashlytics Active badge. Existing console link remains; English/Hindi unavailable
copy explains that this screen does not fetch telemetry. Empty report lists state
only no pending reports, never unproven all-systems-operational health. Actual
telemetry integration remains unavailable, not fabricated or marked working.

| Property / upstream relationship | EVE path:lines | Classification / evidence |
|---|---|---|
| EVE request/selection ownership; no Telegram exam/Worker subsystem transplanted | AdminAnalyticsViewModel.kt:28-59; AdminAnalyticsActivity.kt:45,76,86,127-135 | ADAPTED EVE business integration; real repository/ViewModel transport-order tests plus actual protected Activity filter/recreation callbacks |
| Existing native report controls/lifecycle | FlaggedQuestionsActivity.kt:58-120,124,136,160,167,214 | ADAPTED existing EVE/Material controls, not claimed original Telegram tabs; real native callbacks, reordered transport, stale actions and destruction tests |
| Existing dialog primitive remains AlertDialog.java:1161-1173,1526-1528@3de37c8cfb039452be7398909461da6288babf15 | existing TelegramNativeAlertDialog -> canonical question editor, unchanged by this batch | ADAPTED existing source-derived host; predecessor source map/tests retained, no new imported source/assets or source-value claims |
| Existing EVE telemetry display contract, no upstream service imported | activity_admin_analytics.xml:28-38; English/Hindi strings; Flagged empty-state strings | ADAPTED truthful unavailable/empty states; actual Activity English and Hindi/RTL/Night resources tested; real telemetry/service/device acceptance NOT MET |

Fail-before evidence: four actual Analytics ViewModel/repository ordering cases
failed (old range/refresh overwrite, old failure, unnecessary obsolete second
read); one actual Analytics Activity telemetry case failed. Three initial report
Activity tests exposed non-checkable filters; one stopped at the missing second
request. These are NOT counted as a completed stale-report-response reproduction.
After enabling the real filters, expanded native callbacks exercise actual
out-of-order results, empty/error states, live stale-row actions and teardown.
Expanded 29 scoped cases PASS: six real VM/repository, eight real Flagged Activity,
two actual Analytics filter/recreated Activity, and thirteen retained/extended
question-review Activity tests. An early setup(null) harness error was corrected
to setup() for a fresh Activity; no product assertion was suppressed.

Full local integration PASS: 862 tests / 196 suites, zero failures/errors/skips;
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 (3m39s). Lint 0 errors / 1634 warnings.
Focused command selected AnalyticsReadOwnershipTest, AnalyticsFilterActivityTest,
ReportReadOwnershipTest and QuestionReviewActivityTest (29 PASS). Helpers59,
docs/acceptance structure/four upstream byte imports/diff PASS. R1-R5 MET at scoped
code/host level; carrying commit/exact-SHA FAST integration PENDING. Backend unchanged
from the green predecessor (307 tests); no production request/write/deployment,
migration, auth/scoring/data contract change or new permission. Whole C3/C5 and
source/device acceptance remain NOT MET; qualified human GPL/FULL/publication
safeguards remain intact. Historical bare SDK35 shutdown exception remains UNKNOWN.

Next independent implementation after carrying FAST SUCCESS: inspect EditExam's
bulk-picker Activity-result callback while the fresh AdminAccessGate is pending;
reproduce lost selected URI, then retain/consume it once only under the verified
same account/exam. No new permission, parallel import flow or production writes.
Question Delete/Undo decision remains explicitly unanswered; generic resume is
not approval. Related P2 limitation: existing QuestionAnalytics API has no days
parameter (all-time aggregates); this batch does not invent range-specific question
statistics. QR acceptance remains OPEN and does not block independent work.

## C1/C5 EditExam metadata-only save integrity (critical reprioritization)

Base 980a92cf4a10e4b16b0a691303b4c340ad30bf6e FAST 41535796716 SUCCESS,
docs 41535796467 SUCCESS. Read-only tracing exposed a higher-impact data defect
than the queued lost picker URI: Save fabricates Exam defaults after a failed or
pending metadata read, and sends hidden count/negative-marking/publish/parent
settings although this form edits only name, time, category and cutoffs. Actual
sole ExamRepository.updateExam production caller is EditExamActivity; other full
settings/AI editors call different repository methods. Worker exams.ts preserves
omitted settings. Reprioritize this scoped save contract before picker recovery.
R1: no metadata mutation before a confirmed matching exam snapshot. R2: send only
this form's edited fields, preserving server-owned/unshown settings and planned
count. R3: bounded pending save, retryable failure, confirmed success and stale-
account/host rejection. R4: validate time against existing Worker 1..600 contract.
R5: preserve existing category/drafts through same-account/exam recreation without
restoring another account/exam's state. No scoring/API/backend production change,
new permission, parallel form or state manager. Original picker and Undo gaps stay
OPEN; no combined-app/device/source/legal completion implied.

## C1/C5 metadata save integrity and cutoff backing

Base 980a92cf4a10e4b16b0a691303b4c340ad30bf6e FAST 41535796716 / docs
41535796467 SUCCESS; the Analytics/report read-ownership carrying gate is verified.
Higher-impact default/hidden-settings metadata writes reprioritized before queued
picker recovery. Complete scoped ledger, source relationships, caller chain,
fail-before evidence and rollout limits: EXAM_METADATA_SAVE_CONTRACT.md.
Current local Android883/198 suites, AndroidTest compile/lint (0 errors/1629 warnings)/x86_64 assemble and backend318/TypeScript PASS; carrying FAST pending.
No production migration/deployment. Cutoff persistence is development-stage only;
old deployed service cannot falsely confirm it because client verifies readback.
First next independent action after carrying FAST: pending fresh-gate picker URI
recovery. Undo product answer remains pending. Whole mission/QR/device/FULL/legal
acceptance remains OPEN/NOT MET. Existing successful feature/commit ledger preserved.

## C5 EditExam picker result handoff and lifecycle

Base 07b448e5e70b53569db6a4ba328bd0baf34edca8 FAST 41544036446 SUCCESS,
docs 41544036310 SUCCESS. Metadata save integrity gate is green; cutoff production
rollout stays separately BLOCKED. R1: actual GetContent result delivered during a
recreated Activity's pending fresh gate must survive until same-owner/exam rights
and metadata are confirmed. R2: no file read/preview/write under denied or changed
identity; consume once, preserve cancellation and avoid duplicate previews. R3:
provider errors/null streams must be handled, and file query/read/parse must run off
Main with lifecycle-owned stream/job/window cleanup. R4: preserve existing parser,
preview and explicit import contracts; no new permission, persistent URI grant,
state manager or automatic backend replay. Test actual ActivityResultRegistry and
ContentResolver callers, not only a selector helper. Shared parser bounds/sparse
XLSX handling and unknown in-flight import acknowledgement remain separate OPEN
reliability requirements; never claim device/provider-grant proof from Robolectric.

## C5 import result handoff/read lifecycle

Recovery main 07b448e5e70b53569db6a4ba328bd0baf34edca8, FAST 41544036446 and
docs 41544036310 SUCCESS. Queued picker defect now has actual registry/provider
fail-before and expanded host evidence. Implementation/source/requirement ledger:
EXAM_IMPORT_RESULT_LIFECYCLE.md. Single existing Activity/window owner; no new
permission, persistent grant, parser rewrite, backend or production operation.
Scoped16 picker tests and broad Android899/199 suites, AndroidTest compile/lint (0 errors/1629 warnings)/x86_64 assemble PASS; helpers59/docs/claims/four imports/diff PASS. Carrying FAST pending. First next
after carrying FAST: shared parser resource/batch bounds and sparse XLSX column
mapping. Physical providers/grants, unknown submit acknowledgement/idempotent retry,
whole source UI/device/FULL/GPL acceptance remain OPEN. Existing successful QR and
other work preserved; cutoff production rollout and Undo decision remain separate.

## C1/C5 shared question-import parser safety and cell correctness

Base 22fd6f8129e26bef75c7a26cc609a6525c1684e2 FAST 41568018939 SUCCESS,
docs 41568018836 / dependency evidence 41568018767 SUCCESS. R1: reject a >300-valid-
question batch explicitly, never silently truncate (existing Worker questions.ts
limit). R2: XLSX sparse cell references, inline strings and rich shared strings must
retain correct question/option/answer/topic identity. R3: bound raw/expanded input,
rows, columns, cells and shared strings; reject malformed/DTD input and propagate
cancellation. EVE-authored safety budgets, not Telegram values: 8MiB raw, 16MiB total
expanded ZIP, 10000 rows, 64 columns, 65536 characters/cell and 80000 shared strings.
These bound unsupported document shapes rather than changing evaluation rules.
R4: preserve existing CSV quoting/numeric normalization, error rows and both actual
production callers (EditExam and ManageQuestions), with localized fatal messages.
R5: retain stream ownership, avoid network/external entity access, and keep manual
confirmation/server permission contracts. No production writes, new permission,
backend/scoring change or new parser/state implementation parallel to the existing
QuestionImportHelper. This is the remaining shared parser increment following the
green editor handoff, not a cosmetic third iteration of that feature.

Implementation and evidence: EXAM_IMPORT_PARSER_CONTRACT.md.

Local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (3m21s); 916 tests / 200 suites, zero failures/errors/skips.
Lint 0 errors / 1629 warnings. Document parser16 and actual picker17 cases PASS;
59 CI-helper tests, docs78, acceptance structure, four byte-identical imports and
git diff --check PASS. Backend unchanged from green main. Historical bare SDK35
thread diagnostic recurred without a JUnit failure; remains UNKNOWN, not resolved.
Carrying exact-SHA FAST pending; native navigation, physical providers, matched
visual captures, protected FULL, verified publication and legal clearance OPEN.

Next independently actionable Admin defects: standalone ManageQuestions false
immediate deletion success/error presentation and picker lifecycle/target ownership.
Reproduce actual callbacks before changing those flows; Delete/Undo timing choice
is still pending and does not block confirmation-only repairs.

## C5 ManageQuestions confirmed deletion presentation

Base d638d3b20a4c10e6462b6d95adb8f35fada3a584 FAST41571297755 and
Docs41571297931 SUCCESS. R1: actual standalone row -> native Delete -> existing
AdminViewModel/repository must show success only after confirmed server response.
R2: denied/malformed/network failure must remain visible as failure, never success.
R3: pending rapid row/button taps must not submit duplicate writes; cancellation
and stale host/identity callbacks must not produce misleading notices or writes.
R4: preserve current permanent-delete timing, Cancel/destructive dialog/source
renderer, selected-exam refresh and server manage_questions authority. No change
to the separate unanswered Delete/Undo choice; no new repository, renderer or state
owner. Legacy AdminActivity's similarly named private dialog has no production
caller; do not call it an active duplicate or remove it on naming alone.

C5 scoped implementation/evidence: MANAGE_QUESTION_DELETE_CONFIRMATION.md.

Local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (3m57s), 931 tests / 201 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Helpers59, docs79, acceptance structure, four recorded
byte-identical imports and git diff --check PASS. Backend unchanged from green base.
Bare SDK35 thread diagnostic recurred with zero JUnit failures; remains UNKNOWN.
Carrying exact-SHA FAST pending. Native navigation, physical device/reference,
protected FULL, verified artifact publication and human GPL clearance remain OPEN.

## E4/D5 account-bound local attempt/result handoff

Base 9fdeca47309ea9da0f228119591c410a70b4e057 FAST41573024314 SUCCESS;
Docs41573024250 recheck required. Higher-priority isolation audit discovered the
HomeViewModel companion cache keyed only by exam ID and ResultDataHolder's untagged
answer/detail slots. R1: current account must never read another account's local
attempt, submitted/cleared flags or result/detail answers. R2: every actual producer,
consumer and fallback must bind immutable source UID/exam context, reject stale
callbacks/account changes and preserve same-account navigation/recreation. R3: no
new state owner or parallel cache; extend canonical holders, preserve server scoring,
answer confidentiality, attempt deduplication, history and existing UI. R4: real
previous-result fallback and native result consumers must be tested with synthetic
SDK/transport boundaries, including account switch and late completion. Treat source
risk as INFERRED until regression tests reproduce it. This security correctness work
precedes the separately suspected Home search SavedState gap; no speculative QR or
Home geometry changes. No production mutation/deployment or permission change.

Scoped ledger/source evidence: ATTEMPT_RESULT_OWNER_ISOLATION.md.

Final local integration PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (2m44s), 948 tests / 202 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Scoped ownership18 cases (including the intact moved
tracking assertions) PASS. Helpers59, docs80, acceptance structure, four recorded
byte imports and git diff --check PASS. One preceding local run's indentation lint
error was repaired; no suppression/baseline or assertion removal. Historical bare
SDK35 thread diagnostic remains UNKNOWN; no blanket crash-free/device claim.
Carrying exact-SHA FAST pending. Backend/Firebase source unchanged from green base.

## D3 Home search SavedState owner boundary (investigation)

Recovery c57dfa64693047e3e0b76530d4e930e4cd0baba1 FAST41575420756 and
docs41575420725 SUCCESS. R1: a recreated Home must not restore another UID's query
or pending search/list presentation. R2: automatic Android hierarchy restoration
must obey the same boundary. R3: same-UID recreation/theme, current filtering,
selected category, IME/source fade and native callbacks remain functional. R4:
reproduce using actual MainActivity callbacks before any repair, with offline SDK/
transport only. Prior Mission11 search deliveries285a9528 and460e093d are green;
prepare a concrete reviewed repair and reconcile the two-successful-commit limit
before any additional Home-search integration. No new screen/state manager or
speculative seam/physics change. Current missing-owner source observation is still
INFERRED until the production-path test executes.

Two Home production restoration defects are now reproduced; same-owner control
passes. The tested repair and source/requirement evidence are in
HOME_SEARCH_OWNER_REPAIR.md. Integration BLOCKED only on the section2 commit-budget
exception, not another product redesign or release approval.

Local complete validation PASS: ./gradlew :app:testDebugUnitTest
:app:compileDebugAndroidTestKotlin :app:lintDebug :app:assembleDebug
-Peve.nativeAbis=x86_64 (2m52s), 956 tests / 203 suites, zero failures/errors/skips;
lint 0 errors / 1629 warnings. Native Home8 cases PASS; helper59, docs81, acceptance
structure, four byte-identical imports and git diff --check PASS. No new backend,
production service, permission or Telegram source import. This result is local-only;
no additional Home search commit or exact-SHA FAST until owner exception arrives.

Owner explicitly authorized ONE additional Home search repair commit on2026-11-03:
"Haan, ek additional repair commit authorize hai". Reviewed patch/file hashes matched
unchanged live c57dfa64 before application. The section2 integration blocker is
resolved for this single repair; carrying commit/exact-SHA FAST pending. No release,
FULL, production or additional future-success exception is implied.

## B1/B2 Profile actions RenderNode blur, radial gradient, and loading lifecycle (2026-11-05)

Base `8279af6f035cac6dee0e54d138623d57b1c80d8e`, FAST [41964090773](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41964090773) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Integrated Telegram's authentic `RadialGradient` shader highlight (from `ProfileActionsView.java:206-215, 318-325`) and light-mode shadow layer (`ProfileActionsView.java:311-315`) into `ProfileActionsCanvasLayout`.
R2: Added API 29+ `RenderNode` drawing blur support (`setDrawingBlur`, `drawRenderNodeBlur` from `ProfileActionsView.java:155-162, 341-373`) connected to `ProfileGooeyView`'s blurred avatar render node (`blurNode`).
R3: Integrated dynamic action lifecycle (`startLoading`, `stopLoading`, `isLoading`, and radial sweep progress arc from `ProfileActionsView.java:375-384, 465-475`) bound to photo save/preview in `ProfileActivity`.
R4: Updated palette picker in `UsernameCardActivity` to set transparent background on `design_bottom_sheet`, ensuring clean Telegram glass rendering without Material container artifacts.
Classification: **ADAPTED**, whole Profile/screen visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## B1/B2 Profile actions Canvas pipeline reorder, coordinate alignment, and memory lifecycle fix (2026-11-05)

Base `0f381d692113bdfe7b60cf0903a712bff285fdda`, exact-SHA FAST [41973946194](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41973946194) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Reordered onDraw pipeline stages in `ProfileActionsCanvasLayout` to strictly follow Telegram upstream (`ProfileActionsView.java:298-338`): Stage 1 button backgrounds & radial highlights, Stage 2 RenderNode avatar blur shader, Stage 3 labels and icons/loading spinner on top.
R2: Corrected avatar clipping path coordinates using window-relative translation delta via `getLocationInWindow` (`locAvatar` and `locActions`), preventing offset clipping between full-window gooey view and nested scroll actions.
R3: Added lifecycle cleanup on `onDetachedFromWindow()` in `ProfileActionsCanvasLayout` to release `renderNode`, `avatarView`, `radialGradient`, `shaderPaint.shader`, and reset loading states, preventing memory leaks.
R4: Updated `ProfileActivity` to offload `savePendingPhoto()` disk I/O and bitmap compression to a background coroutine (`Dispatchers.IO`), allowing the main looper to render the action loading arc without blocking, and cleared blur nodes on `onDestroy()` and resting scroll.
Classification: **ADAPTED**, whole Profile/screen visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## B1/B2 Profile actions LoadingDrawable port and photo save lifecycle isolation (2026-11-05)

Base `258402d9d5327b7defe753432f7e6cc6432d2fb1`, exact-SHA FAST [41995403586](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41995403586) SUCCESS, Verify Documentation run [41995403752](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41995403752) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Ported authentic Telegram `LoadingDrawable` (`LoadingDrawable.java:28-405@3de37c8cfb039452be7398909461da6288babf15`), featuring shimmering gradient sweep, customizable stroke outline, and appear/disappear gradient transitions.
R2: Integrated `LoadingDrawable` into `ProfileActionsCanvasLayout` Stage 3 matching `ProfileActionsView.java:475-515`, replacing the synthetic rotating arc with Telegram's authentic shimmering sweep and outline overlay, with light/dark theme adaptation.
R3: Bound disk photo persistence across `ProfilePhotoManager.kt` (`savePhoto`, `hasCustomPhoto`, `loadBitmap`) strictly to the authenticated `ownerUid`, preventing cross-account state leakage during authentication transitions.
R4: Added `photoSaveJob` lifecycle management in `ProfileActivity.kt`, canceling background photo save operations on preview cancellation, activity destroy, or profile owner mismatch.
Classification: **ADAPTED**, whole Profile/screen visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## A1.5 Telegram QR backdrop scene lifecycle release (2026-11-05)

Base `28e64ebfe03e15a0a74ee2f1e842c7f8f2dfc71f`, carrying commit `bceccaeb0fe8be0fcc62a42aa7ecd612a830d3ef`,
exact-SHA FAST [41996865156](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41996865156) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — 243 Rust tests & all JVM tests passed),
Verify Documentation run [41996865177](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41996865177) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added explicit `release()` method to `TelegramQrSceneView.kt` that stops animation drawables, stops motion, and invokes `backdrop.close()`.
R2: Connected `release()` to `onDetachedFromWindow()` in `TelegramQrSceneView.kt`, releasing GPU RenderNodes (`bgNode`, `blurNode`, `sheetLiftNode`), shine shaders, and native bitmap buffers when leaving the active window hierarchy.
R3: Updated `UsernameCardActivity.kt:onDestroy()` to invoke `scene.release()`, ensuring complete detachment and zero memory leaks across activity teardown.
R4: Added `sceneReleaseClosesBackdropAndReattachesSafely` in `TelegramQrRasterTest.kt` verifying backdrop disposal and safe subsequent on-demand recreation.
Classification: **ADAPTED**, whole QR scene/viewer visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## A4.2 Telegram QR gallery save coroutine tracking and activity cancellation (2026-11-05)

Base `bceccaeb0fe8be0fcc62a42aa7ecd612a830d3ef`, carrying commit `adccdceaaac4a745267f0f862b0200e8960a8a2d`,
exact-SHA FAST [41999281057](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41999281057) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — all JVM tests passed),
Verify Documentation run [41999281073](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41999281073) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added `gallerySaveJob` tracking in `UsernameCardActivity.kt` to monitor background `QrGalleryWriter.save` operations.
R2: Connected `gallerySaveJob?.cancel()` to `finishWithMotion()` and `onDestroy()`, ensuring immediate abort of file I/O upon activity exit or destroy.
R3: Added lifecycle safeguards in `saveToGallery()` and `shareImage()`, preventing multiple simultaneous saves and suppressing UI actions on dead activities.
R4: Added `UsernameCardActivityLifecycleTest.kt` verifying activity initialization, closing motion state, and proper teardown.
Classification: **ADAPTED**, whole QR scene/viewer visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## D1 Telegram Home overflow menu pause/stop lifecycle teardown (2026-11-05)

Base `adccdceaaac4a745267f0f862b0200e8960a8a2d`, carrying commit `09628ce3c3b518e3d6ef0f5f060d4fd64bcf9ebe`,
exact-SHA FAST [42001626557](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42001626557) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — all JVM tests passed in 5m 56s),
Verify Documentation run [42001626555](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42001626555) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added immediate teardown dismissal (`homeOverflowPopup?.dismissForHostTeardown()`) and reference clearing (`homeOverflowPopup = null`) in `MainActivity.kt:onPause()`, matching `ProfileActivity.kt:804` pattern and preventing detached floating window retention when leaving Home.
R2: Added immediate teardown dismissal in `MainActivity.kt:onStop()`, ensuring secondary cleanup on stopped activities.
R3: Added immediate teardown dismissal in `categoryPaletteListener`, resetting active popups before dynamic theme switching.
R4: Added contract assertions in `HomeOverflowSingleWindowContractTest.kt` and Robolectric unit tests in `HomeSevenActionPopupTest.kt` verifying immediate teardown and overlay disposal.
Classification: **ADAPTED**, whole Home overflow menu visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## C1/C5 ManageQuestions activity resume permissions re-validation and question refresh (2026-11-05)

Base `09628ce3c3b518e3d6ef0f5f060d4fd64bcf9ebe`, carrying commit `3275c68ff842523275a5c68b6b194d2e82ce5012`,
exact-SHA FAST [42004501441](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42004501441) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — all JVM tests passed in 4m 17s),
Verify Documentation run [42004501459](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42004501459) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added `onStop()` (`reauthorizeOnResume = true`) and `onResume()` lifecycle re-validation using `AdminAccessGate.requireAdmin(..., requireFreshServer = true, requiredRight = "manage_questions")` in `ManageQuestionsActivity.kt`, terminating the activity immediately if rights were revoked on the Worker.
R2: Guarded against account changes on resume via `questionIdentityCurrent`, finishing immediately if UID or email changed while backgrounded.
R3: Re-evaluated `canManageExams` button controls and triggered fresh question updates (`viewModel.refreshQuestions(viewModel.questionExamId)`) on successful re-authorization without duplicate listener registration or flow collector attachment.
R4: Added `ManageQuestionsPermissionResumeTest.kt` containing Robolectric unit tests and source contract assertions verifying fresh server re-validation, revocation teardown, retained rights survival, and zero duplicate UI/observer wiring.
Classification: **ADAPTED**, whole Admin questions management visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## C1/C5 ManageExams activity resume permissions re-validation and exam refresh (2026-11-05)

Base `3275c68ff842523275a5c68b6b194d2e82ce5012`, carrying commit `fc5844ba52077e62551a31940989bfe18f6f6e52`,
exact-SHA FAST [42006922752](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42006922752) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — all JVM tests passed in 6m 2s),
Verify Documentation run [42006923008](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42006923008) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added `onStop()` (`reauthorizeOnResume = true`) and `onResume()` lifecycle re-validation using `AdminAccessGate.requireAdmin(..., requireFreshServer = true, requiredRight = "manage_exams")` in `ManageExamsActivity.kt`, terminating the activity immediately if rights were revoked on the Worker.
R2: Enforced account match (`live.uid == authorizedUid && live.email == authorizedEmail`) on resume, closing immediately upon identity change.
R3: Reloaded exams and preserved current exam selection (`loadExams(targetExamId = currentExamId)`) upon verified resume without duplicating UI listeners (`initializeAuthorizedUi()`) or reattaching adapters.
R4: Added `ManageExamsPermissionResumeTest.kt` containing Robolectric unit tests and contract assertions verifying fresh server re-validation, revocation teardown, retained rights survival, and zero duplicate UI wiring.
Classification: **ADAPTED**, whole Admin exams management visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## C1/C5 ManagePolls activity resume permissions re-validation and polls refresh (2026-11-05)

Base `fc5844ba52077e62551a31940989bfe18f6f6e52`, carrying commit `62d1589b2ce8fef56ec945037d0c3eb1759600e1`,
exact-SHA FAST [42009610711](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42009610711) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — all JVM tests passed in 8m 45s),
Verify Documentation run [42009610772](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42009610772) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added `onStop()` (`reauthorizeOnResume = true`) and `onResume()` lifecycle re-validation via `AdminAccessGate.requireAdmin(..., requireFreshServer = true, requiredRight = "moderate_feedback")` in `ManagePollsActivity.kt`, terminating the activity immediately if rights were revoked on the Worker.
R2: Checked live authentication match (`live.uid == authorizedUid && live.email == authorizedEmail`), terminating immediately if the signed-in user changed.
R3: Reloaded polls (`loadPolls()`) upon verified resume without duplicating UI adapter setup or click listeners (`setupAdminControls()`).
R4: Added `ManagePollsPermissionResumeTest.kt` containing Robolectric unit tests and contract assertions verifying fresh server re-validation, revocation teardown, retained rights survival, and zero duplicate UI wiring.
Classification: **ADAPTED**, whole Admin polls management visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## C1/C5 FeedbackMessages activity resume permissions re-validation and feedback refresh (2026-11-05)

Base `62d1589b2ce8fef56ec945037d0c3eb1759600e1`, carrying commit `c8f7edb73be5d8b7468d0bb145fbba53e77f0d46`,
exact-SHA FAST [42011757382](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42011757382) SUCCESS (Job `backend`: ✓, Job `firebase-functions`: ✓, Job `build`: ✓ — all JVM tests passed in 8m 33s),
Verify Documentation run [42011757373](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42011757373) SUCCESS,
and preceding six-job FULL run [41960298006](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41960298006) SUCCESS.
R1: Added `onStop()` (`reauthorizeOnResume = true`, `replyDialog?.dismiss()`) and `onResume()` lifecycle re-validation via `AdminAccessGate.requireAdmin(..., requireFreshServer = true, requiredRight = "moderate_feedback")` in `FeedbackMessagesActivity.kt`, terminating the activity immediately if rights were revoked on the Worker.
R2: Checked live authentication match (`live.uid == authorizedUid && live.email == authorizedEmail`), terminating immediately if the signed-in user changed.
R3: Reloaded feedback messages (`loadMessages()`) upon verified resume without duplicating UI adapter setup or click listeners (`setupAdminControls()`).
R4: Added `FeedbackMessagesPermissionResumeTest.kt` containing Robolectric unit tests and contract assertions verifying fresh server re-validation, revocation teardown, retained rights survival, and zero duplicate UI wiring.
Classification: **ADAPTED**, whole Admin feedback messages management visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## C1/C5 SendNotification activity resume permissions re-validation and broadcasts refresh (2026-11-05)

Base `c8f7edb73be5d8b7468d0bb145fbba53e77f0d46`, carrying commit `86d939c065fca1c67623910c2c2f42e4bfd0a514`,
Verify Documentation run [42013164609](https://github.com/vishu762701/eve-mock-test-app/actions/runs/42013164609) SUCCESS,
Build run 42013164489: `backend` ✓, `firebase-functions` ✓, build JVM test failed on pre-existing `ExamImageHelperTest` mark buffer size in Robolectric (1233/1234 other JVM tests passed, including `SendNotificationPermissionResumeTest`). Addressed via 64 KB header buffer forward fix.
R1: Added `onStop()` (`reauthorizeOnResume = true`) and `onResume()` lifecycle re-validation via `AdminAccessGate.requireAdmin(..., requireFreshServer = true, requiredRight = "send_broadcasts")` in `SendNotificationActivity.kt`, terminating the activity immediately if rights were revoked on the Worker.
R2: Checked live authentication match (`live.uid == authorizedUid && live.email == authorizedEmail`), terminating immediately if the signed-in user changed.
R3: Reloaded broadcasts (`listenToSentBroadcasts()`) upon verified resume without duplicating UI adapter setup or click listeners (`initializeAuthorizedUi()`).
R4: Added `SendNotificationPermissionResumeTest.kt` containing Robolectric unit tests and contract assertions verifying fresh server re-validation, revocation teardown, retained rights survival, and zero duplicate UI wiring.
Classification: **ADAPTED**, whole Admin send notification visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.

## C1/C5 ExamImageHelper decode stream mark buffer expansion and SystemMonitor resume permissions (2026-11-05)

Base `86d939c065fca1c67623910c2c2f42e4bfd0a514`.
R1: Expanded `BufferedInputStream` size and mark limit to 64 KB (`BufferedInputStream(first, 65536)` with `stream.mark(65536)`) in `ExamImageHelper.kt`, ensuring image header signature inspection and bounds decoding never invalidate stream buffer on PNG chunks or EXIF metadata.
R2: Added `onStop()` (`reauthorizeOnResume = true`) and `onResume()` lifecycle re-validation using `AdminAccessGate.requireAdmin(..., requireFreshServer = true, requiredRight = "view_analytics_logs")` in `SystemMonitorActivity.kt`, terminating immediately on privilege revocation or identity change.
R3: Refreshed `canRetryGeneration = permissions.isOwner || "manage_generated_tests" in permissions.rights` and reloaded monitor statistics (`load()`) upon verified resume without duplicating UI sections or control buttons.
R4: Added `SystemMonitorPermissionResumeTest.kt` containing Robolectric unit tests and contract assertions verifying fresh server re-validation, revocation teardown, retained rights survival, and zero duplicate UI wiring.
Classification: **ADAPTED**, whole Admin system monitor visual/motion parity remains NOT MET / OPEN. Device/hardware evidence and GPL distribution clearance remain BLOCKED.









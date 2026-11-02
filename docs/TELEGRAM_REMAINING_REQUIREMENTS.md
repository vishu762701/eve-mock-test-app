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

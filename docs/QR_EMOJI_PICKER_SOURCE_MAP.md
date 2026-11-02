# QR animated picker source integration

Status: ADAPTED picker subsystem implemented; full QR fidelity remains OPEN.
This is not acceptance of the complete A2 experience or QR screen.

Official source is the HTTPS Git checkout `DrKLO/Telegram` at
`3de37c8cfb039452be7398909461da6288babf15`. Before copying, opened
`QrShare/QrEmojiPanel.java:1-315,490-638`, `EmojiPickOverlay.java` (whole file),
`QrShareActivity.java:480-755`, `QrShareView.java:711-715,862,940-1025,1868-1880`.
Root license is GPLv2; no absent per-file later-version grant is invented. Existing
release-compliance gate remains enforced. New derived Java must be delivered with
corresponding source if distribution is cleared. No Telegram document/account,
MediaDataController, MTProto, UserConfig, presets IDs, entitlement or credential
classes were imported into EVE.

The minimum selected subsystem is original Java cell drawing plus the complete
original Java selection overlay: press interpolation, picked-state scale, numbered
spring badges, renumbering crossfade, rounded selection fill/ripple, removal island,
item insertion/removal/reordering and scrollbar. It depends on existing adapted
QrRender/RimPainter/RimTune and QrUiUnits. Drawable and catalogue ownership are EVE
adapters around the existing bounded native Lottie loader and authenticated public
EVE catalogue. The grid host is an AndroidX RecyclerView adapter. The complete
QrEmojiPanel pack loader, premium unlock, Telegram recent/account preferences and
QrShareView sheet/blur compositor are excluded, with no fake data substitute.

| Telegram upstream file:lines@SHA | EVE file:line | Classification | Tests | Runtime evidence |
|---|---|---|---|---|
| QrEmojiPanel.java:517-630@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrEmojiCell.java:18 | ADAPTED original Java draw/press/measure; EVE Drawable ownership, labels, visibility and motion inputs | QrEmojiPickerGridTest lifecycle/selection tests | host controlled-window test; actual platform test compiled, not executed locally |
| EmojiPickOverlay.java:1-572@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/EmojiPickOverlay.java:34 | ADAPTED package/units/public API/cell type/native Drawable; reduced-motion endpoints and delayed scrollbar teardown | actual overlay paint/selection and six-key integration | Robolectric attached views; native reference comparison absent |
| QrEmojiPanel.java:493-515; QrShareActivity.java:506-518,593-609,659-669@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrEmojiGrid.java:24 | ADAPTED original Java grid layer, smoothstep bottom mask and picked-island cut; AndroidX host | actual attached grid paint and restoration tests | host only; device and full scene blur pending |
| QrEmojiPanel.java:134-194,222-233@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/QrEmojiPickerGrid.kt:19 | ADAPTED EVE catalogue/lifecycle/ownership adapter; >=48dp cells vs original nominal 46dp | six selections, recycling, late asset completion after dismiss, empty-layout scroll restoration | four Robolectric cases; both-navigation AndroidTest compiled only |
| QrShareView.java:972-992@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/QrEmojiPattern.kt:35 | existing ADAPTED six-key toggle/drop-oldest logic retained | existing 3 pattern tests + actual picker integration | host only |
| QrShareView.java:711-715@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrPalettes.java:23 | ADAPTED extraction of original Java shadeFor algorithm; palette index host | compiled Java and picker paint paths | host only; no screen parity claim |

Remaining differences: Material sheet container, source packs/recent/premium
services unavailable in EVE, original blur/scene morph and matched device evidence.
Unicode remains only an existing saved-scene compatibility path, never a presented
animated-document preset. This batch must not classify the full panel/screen EXACT.


All upstream paths in the table are under
`TMessagesProj/src/main/java/org/telegram/ui/QrShare/`. Source blobs are
`809c5dbbe58749c1427af9cb4415ce832ea955a7` (QrEmojiPanel) and
`ca935677c164fc16b25798a5747dd71316e38026` (EmojiPickOverlay).
All **85** QR/native manifest entries were rechecked against immutable upstream
Git objects and current EVE SHA-256 values. A stale facade entry from
`deee53b30b511288d9279ba9a08db9187d84b6b6` is reconciled; its unchanged canonical
Java interpolation class is now separately recorded as EXACT bytes.

## EVE integration and supported behavior

- Actual enabled Lottie catalogue entries flow through the existing authenticated
  public EVE endpoint. The four-column Unicode/Airbnb preview grid is removed.
  No Telegram preset document ID, inventory, premium unlock or entitlement is faked.
- Preview frames use the existing bounded media-only loader and bundled native
  renderer. The scene retains its own selected-frame ownership; this is not the
  complete Telegram shared AnimatedEmojiDrawable account/document cache.
- Tap toggles and drops the oldest seventh pick. Long press removes an existing
  pick. The original Java overlay renumbers badges, reverses selection motion and
  inserts/removes/reorders the removal-island items. Ripple, native scrollbar and
  original mask algorithms are used. All removal actions remain accessible through
  >=48dp labelled grid cells; the 34dp visual island is not a duplicate TalkBack node.
- Picker dismissal cancels catalogue work. Recycling/detachment stops scheduled
  frames, clears callbacks and closes owned native resources. A generation check
  rejects stale recycled loads; a late non-cooperative load is explicitly tested.
- Activity saved state retains choices, source strings, picker-open state and grid
  scroll. Pending grid state survives the empty layout before async data arrives.
  Height adapts to the window; headers/counts use wrap-content for scaled fonts.
  Material's opaque sheet background is removed so the QR glass adapter remains
  visible in Day and Night. Full source sheet morph/blur is still absent.
- Reduced motion settles original cell/overlay state, suppresses native playback
  and scene drift/reveal, and still hides an idle scrollbar. Hidden windows stop
  frame playback. A restored standard glyph remains legacy standard text, not an
  animated-document preset.

## Validation and repairs

Local toolchain: Gradle 8.7, Temurin JDK 21.0.13, Platform 36,
NDK 27.2.12479018, CMake 3.22.1, Rust 1.99.1. JDK and test-JVM proxy setup were
repaired outside the repository; no TLS validation was disabled.

The four picker tests pass. AndroidTest source compilation, lint (zero errors,
1643 warnings) and x86_64 debug APK assembly pass. The bundled
`libeve_qr_animation.so` has **0x4000** LOAD alignment in all three segments.
The existing raster test also passes **160 actual QR decodes** (80 palettes x two
sizes). Native test execution, matching Telegram hardware/video and the full
Day/Night/RTL/Hindi/large-font/inset/navigation matrix remain unverified.

The first full JVM attempt ran 677 tests: 676 passed and the new scroll fixture
failed because the Activity's default window changed its span count between
snapshots. A fixed-size child viewport repairs the fixture; the exact position
assertion remains. The visible-playback host test explicitly supplies its window
visibility input because Robolectric's plain SDK-35 host reports window GONE;
actual platform playback is separately asserted by the compiled native test.
An earlier lint snapshot read source while it changed; the subsequent stable-source
lint run passes without suppressions or baseline changes. Whole-suite and exact-SHA
FAST outcomes belong in the persistent mission checkpoint once observed.

The full test scan found one legacy production `/api/app-config` request.
`AdminDashboardRealDataVerificationTest` now uses the real Retrofit API/DTO parser
with a terminal interceptor at `fixture.invalid`, preserving its original schema
assertions and adding request path/method, request count and decoded-field checks.
Timeout/network catches that previously returned success are removed; no production
socket, token or configuration read is required. Backend route tests remain unchanged.

Production auth, Worker authorization/scoring, Firebase, D1, media APIs, app routing,
permissions, Home's black Night drawer and the absent Profile Bio row are unchanged.
Licensing and distribution safeguards remain enforced. Local debug assembly is not
mission-final APK publication; no production release clearance is claimed.

Final local broad rerun: **677 tests, 151 suites, zero failures/errors/skips**.
Four picker cases, AndroidTest compilation, lint and x86_64 assembly all pass.
The console contains a bare SDK-35 worker-thread shutdown exception fragment without
stack; cause remains UNKNOWN despite green JUnit XML. This is reported in the active
checkpoint and is not converted into evidence of device acceptance.

Exact-SHA FAST [41360696652](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41360696652)
is green at `24e79891acb3cdf875d24dcff295b372475e01b2`; source-import/docs workflows
are also green. The shutdown fragment also exists in pre-picker dc3b7ee7 remote
logs, so it is not first introduced by these picker changes. Its cause remains
unresolved. No FULL/native-device acceptance or published APK is inferred.

## A2 source Java picker grid opening transform — 2026-11-02

Status **ADAPTED, not complete sheet-morph parity**. Opened pinned
`DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
`QrShareActivity.java:532-569,613-663,685-758` and
`QrShareView.java:1904-1918`.

| Upstream path:lines@SHA | EVE path | Classification / dependency |
|---|---|---|
| `QrShareActivity.java:542-569@3de37c8...` | `app/src/main/java/com/eve/app/telegram/qr/QrPickerOpenMotion.java`; `app/src/main/java/com/eve/app/ui/profile/QrEmojiPickerGrid.kt` | ADAPTED actual source Java scale 0.75→1, 420ms cubic easing and 0.55 fade relation; EVE animator uses time progress for sheet progress because its Material sheet lacks source morph variable |
| `QrShareActivity.java:553-568@3de37c8...` | `QrEmojiPickerGrid.animateSourceOpen()`; `UsernameCardActivity.showAnimatedEmojiPicker()` | ADAPTED visible-bottom pivot, initial transform after sheet measurement; cancellation/reduced-motion settle; existing AndroidX grid/overlay |
| `QrShareActivity.java:613-663,685-758@3de37c8...` | existing `QrEmojiGrid` and EVE `BottomSheetDialog` | NOT MET: exact original parent sheet shape/morph, row-cascade/alpha mask, pack strip and liquid glass/blur; no claim of native sheet equivalence |

`QrPickerOpenMotionTest.java` exercises source transform endpoints,
easing, clamping and alpha. This is a source-authentic *math extraction*,
but the sheet presentation is ADAPTED, with original Telegram
account/pack/premium services still excluded. Actual native Android
open/close visual and gesture comparison remains OPEN. EVE catalogue
ownership, six-selection restoration, accessibility and release gates are
unchanged.

## A2 native grid opening island-cut coordinate correction — 2026-11-02

Source opened `QrShareActivity.java:663-676@3de37c8cfb039452be7398909461da6288babf15`.
The picker overlay remains in unscaled parent coordinates while the source
grid opens 0.75→1, so directly cutting its `islandRect` in the scaled
RecyclerView's Canvas was incorrect. `QrEmojiGrid.mapIslandCut()` now uses
the original overlay→screen→scaled-grid transform and the source radius
division by grid `scaleY`, reusing two location arrays per frame.
**ADAPTED** from source Java inside EVE AndroidX RecyclerView and overlay;
the unported Telegram parent compositor remains NOT MET.

`QrEmojiGridCutTest.java` validates 0.75-scale island/radius geometry,
settled 1.0 identity and the source safe division. The real
`QrEmojiPickerGridTest.kt` covers attached animation transform and detach
reset without network calls. CI/runtime acceptance remains separate and
the combined Telegram license review OPEN.

## A2 native QR sheet island geometry and opening clip (2026-11-02)

**ADAPTED / full source sheet parity NOT MET.** Official Telegram Android
`DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `QrShareView.java:5317-5433`:
`SHEET_EASE = cubic-bezier(0.16,1,0.3,1)`, opening 420ms, closing
360ms, 144x40dp original island, 10dp sheet insets, 28.17dp
target radius, and source opening `contentA = clamp(sheet / 0.8)`.

Source local mapping:
- `app/src/main/java/com/eve/app/telegram/qr/QrPickerSheetMorph.java`: Java source motion/geometry extraction and 170ms close content math. Source x-inverted cubic solved by bounded iteration. **ADAPTED** EVE host origin Y from its modal panel, not Telegram original QrShareView bottom bar and inset contract.
- `app/src/main/java/com/eve/app/ui/profile/QrPickerSheetSurface.kt`: clip actual picker children through moving rounded island and fade. **ADAPTED** EVE dialog parent remains Material; input/back/outside tap/cancellation stay native, not a Telegram exact-screen claim.
- `app/src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt`: real Android picker caller wraps existing panel in surface and opens concurrently with original source grid scaling.
- `app/src/test/java/com/eve/app/telegram/qr/QrPickerSheetMorphTest.java`: source geometry, alpha, timing/ease monotonicity and endpoints.

Material parent slide/drag, original independent elevated blur/lift, full
per-row dismissal cascade and physical GPU motion parity remain **NOT MET**.
GPLv2/per-file combined-distribution review remains OPEN.

## A2 clip/touch production-path regression (2026-11-02)

Source `QrShareView.java:6654-6729` keeps input routing gated by the
open sheet/picker state; `QrPickerSheetSurface.kt` now consumes touches
while its island-to-panel clip is below 0.5. This avoids a Material sheet
child responding while visually hidden. **ADAPTED**, same EVE/Telegram
code boundary; existing native Material dialog/back/outside taps remain.
`QrPickerSheetSurfaceTest.kt` paints the actual attached Java/Kotlin
production container and checks source inset shape after the 420ms
animation, transparent pre-open content, and hidden-control input gating
using local Robolectric SDK 35 graphics. Physical device/GPU fidelity OPEN.

# QR scene native rendering integration

Status: ADAPTED backdrop/card/reveal subsystem integrated. Full personal-qr
acceptance remains OPEN; this is not complete A1, A2 or screen parity.

Official origin is the HTTPS Git checkout of DrKLO/Telegram. Fetched master and
pinned HEAD both resolve to `3de37c8cfb039452be7398909461da6288babf15`, tree
`cc3c6ccf48e4167fdec764dfd04e765043f55093`. Required bytes were read from
immutable Git blobs, not an unverified archive. Relevant blobs:
`image.cpp` `b0b8b3e6a2d654b048ab938d8b1b0490f65ec4a0`;
`QrShareView.java` `c2a57a19bd96d6b71da270c1e80dd9519d4ca26d`.
Full original-file and EVE SHA-256 values, blob IDs, ranges and adaptations are in
`qr-native-upstream-manifest.json`. All 95 entries were compared to actual pinned
Git objects and EVE files locally. Source identity does not establish runtime parity.

Opened source: QrShareView:124-135,551-743,1518-1596,2041-2430,3573-3819,
4515-4583; QrShareActivity:819-848; image.cpp:1211-1377; Utilities.java:86;
existing QrRender/QrGlass and EVE scene/card host. Also inspected original emoji
change trigger at QrShareView:1043. Root LICENSE is GPLv2. Missing per-file
later-version grant and combined-app redistribution review remain OPEN. No license
exception is inferred; production release safeguards are unchanged.

## Subsystem and source fidelity

The upstream Java root for this table is
`TMessagesProj/src/main/java/org/telegram/`. The native root is `TMessagesProj/jni/`.
Each row is pinned independently to the full revision below; revisions are not mixed.

| Telegram upstream file:lines@SHA | EVE file:line | Classification | Tests | Runtime evidence |
|---|---|---|---|---|
| jni/image.cpp:1211-1366@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/cpp/telegram_qr_backdrop.cpp:14 | ADAPTED extraction; minimal includes/JNI name and null-allocation cleanup; original helpers and complete pixel loop byte-identical | QrBackdropSceneTest tests actual production C++ pixels, radial/linear stops, wave AA and premultiplied ring | host JNI execution; packaged Android JNI test compiled, device execution pending |
| messenger/Utilities.java:86@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrBackdropNative.java:6 | ADAPTED EVE library binding and bounded argument validation | malformed arrays/nonfinite inputs reject before mutation | Robolectric native bitmap bridge |
| ui/QrShare/QrShareView.java:124-134,685-724,1534-1575,2140-2175,2260-2426,3573-3588@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrBackdropScene.java:26 | ADAPTED original Java scene/wave/ink/cache/blur; EVE clock/card geometry and separate control consumers | source interruption base, export cache isolation, motion-disabled settlement, cleanup, actual native raster | software/native host pixels; GPU blur/display-list parity pending |
| ui/QrShare/QrShareView.java:3690-3806@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrCardShadow.java:18 | ADAPTED original Java mask, tint/drop and nine-slice; EVE executor/handler ownership and export preparation | real soft colored shadow pixels, cleanup/recreation | Robolectric native Canvas; device prewarm/frame timing pending |
| ui/QrShare/QrShareView.java:1043,1534-1542,1578-1596,3668-3687@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrCardMotion.java:11 | ADAPTED original Java bounce/pulse; injected monotonic time and reduced motion | source peak, undershoot, interruption, pulse and settlement endpoints | JVM equations; device motion comparison pending |
| ui/QrShare/QrShareView.java:3591-3686; QrShareActivity.java:819-848@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrSceneReveal.java:19 | ADAPTED original Java spring, close curve, shared root/scrim/control/card motion; EVE Activity/lifecycle and interrupted-close continuity | attached parent/layer properties, interrupted opening, duplicate close, cancellation, no-motion endpoints | Robolectric views; physical frame/gesture evidence absent |
| ui/QrShare/QrRender.java:1-192@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrRender.java:99 | ADAPTED existing helper; guarded display-list disposal | Java/resource and AndroidTest compilation | API29/31 device paths unexecuted locally |
| ui/QrShare/QrShareView.java:4533-4583@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrControlBackground.java:14 | ADAPTED EVE Drawable, source scene sampling plus existing QrGlass fill/sheen/rim | exact offset control-to-scene native bitmap samples; cleanup and QR raster scan | CPU stand-in and cross-view screen coordinates tested; actual blurred controls need device comparison |
| ui/QrShare/QrShareView.java:2095-2121@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt:486 | ADAPTED EVE export/username/E mark; source height/crop policy with EVE insets/card bounds | 160 palette/size decodes, rotation-before-draw and actual PNG round trip | native Canvas/ZXing host output; platform share/save matrix pending |
| ui/QrShare/QrShareActivity.java:819-848@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/UsernameCardActivity.kt:145 | ADAPTED complete EVE root/controls binding and QR-only translucent window theme | compile and source integration review | underlying-screen, Back/navigation-mode device verification pending |

## Dependency and lifecycle boundary

The minimum imported native subsystem uses JNI, AndroidBitmap and math only.
Image codecs, libyuv, Telegram Utilities implementation, FileLog/tgnet, MTProto,
account/premium/free-link/gift services and credentials are excluded. Original Java
stays Java and native raster stays C++; EVE's Kotlin host supplies only integration.
The old Kotlin reveal math now delegates to the extracted canonical Java equations.
The all-ABI library build remains Gradle/CMake with 16 KiB linker alignment.

The background uses source 160-texel long-side raster geometry, the original
three-stop linear/radial color mixing, expanding circular recolor and six-stop ring,
source LAB QR-ink transition and source colored nine-slice card shadow. On supported
API31 hardware, after reveal, the original background/pattern/ring order is recorded
into a RenderNode and blurred at source downsample factor 9 for glass controls.
Software/earlier APIs use the source low-resolution scene stand-in. Telegram's
high-end-device early-blur allowance and LiteMode/device classifier are not imported;
GPU cost and visual behavior need matching-device validation.

Whole-root reveal also moves/fades EVE controls. The QR-specific translucent Activity
lets the calling screen appear behind its moving layer, with original parent scrim;
EVE system-bar synchronization is followed by restoring this backing/scrim. Window
animations are disabled for this Activity so they do not compose another transition.
Interrupted closing starts from the drawn progress and scales; duplicate Back is
ignored. Disposal removes animator listeners before cancellation so a destroyed
Activity cannot receive a stale finish callback. Motion-disabled endpoints allocate
no animation hardware layer. Existing focus/visibility/native asset ownership remains.

Shadow preparation uses one bounded process worker and a generation-owned main
callback. Late bitmaps are recycled after detach or synchronous export. Scene
bitmaps/display lists/consumer references are released on detach. Export uses an
independent backdrop snapshot, finished QR build and resting card, with source
`min(height, width * 1.92)` crop centered on current card geometry. It does not
consume the live recolor wave or include app controls. Current bounds are calculated
before cropping, including export immediately after rotation.

The test-only JNI bitmap shim supplies AndroidBitmap operations over real
Robolectric pixels. It compiles the same production C++ renderer; no alternate
raster math is duplicated. It is excluded from APKs and requires a host C++17
compiler plus JDK JNI headers. Platform JNI has a separate instrumentation test.

## Verification and remaining acceptance

Final stable local run: **688 tests across 153 suites**, zero failures/errors/skips;
AndroidTest compilation, lint (0 errors / 1643 warnings) and x86_64 debug assembly
pass. 160 palette-size QR decodes plus two actual PNG/rotation decodes pass.
Final command outcomes and exact-SHA FAST runs are recorded in
`TELEGRAM_FINAL_MISSION_CHECKPOINT.md`. The first broad pass had 685 passing JVM
tests but lint read a changing source snapshot and reported one suspicious-indent
error. A stable-source full rerun is required; no suppression/baseline/assertion was
removed. The historical bare SDK-35 worker-thread shutdown fragment still appears
without a stack despite green JUnit; its cause remains UNKNOWN and predates this batch.

Remaining A1 scope includes the original per-pattern-item wave push/shared cache,
complete original gesture/scene transitions, picker sheet morph/compositor,
matching-device GPU glass and blur behavior, and source/EVE video/frame comparison.
The existing EVE pattern and insets are ADAPTED; no full-screen EXACT claim is made.
A2 service/picker differences, A4 scanner morph/permissions/camera matrix, A5 platform
publication and the other mission workstreams remain as recorded in the main ledger.
No gifts, inventory, payments or Telegram document identities are fabricated.

No backend, scoring, Firebase identity, Worker authorization, D1, production-data,
secret, app-link validation, media API, Home Night drawer or Profile Bio behavior is
changed by this scene integration. Release signing/GPL safeguards stay enforced.
Local debug assembly is development evidence, not a published mission-final APK.


Exact-SHA implementation FAST [41369816744](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41369816744)
passes at `544c371f0ce9419ee1b56892cda8e44d2d37759e`, including all four native
ABI builds, 243 Rust tests, JVM/AndroidTest compilation, backend and Firebase.
Source-import/docs/dependency workflows also pass. Its artifacts list is empty;
FAST neither executes the native UI matrix nor publishes the final APK.

The focused follow-up declares the compiled host renderer as an actual Gradle
Test input (plus host platform/JDK compiler inputs), so future native byte changes
rerun pixel tests. It removes one extra C++ EOF blank line and updates the EVE
hash; no source rendering math changes. The full local command passes again with
688 tests, lint and assembly; require the follow-up's own exact-SHA FAST on resume.

## R2/A1 pattern Java placement / wave / gesture increment (2026-11-02)

Status: **ADAPTED, NOT MET for full R2/A1 and personal-qr**.
Upstream official `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
opened: `QrShareView.java:135-166,208-229,1264-1305,2565-2787,
3210-3218,3369-3407,6637-6768`. Exact source-backed algorithms:
`NodeSprings` bounded array and 220/0.62 dynamics; node key/wave impulse,
card radial scale/alpha, 90dp exponential edge squeeze and 12dp final fade;
idle drift/touch threshold, pan continuity, vector-clamped velocity,
0.35s exponential glide and stretch damping.

| Upstream path:lines@SHA | EVE path | Classification / dependency |
|---|---|---|
| `QrShareView.java:2565-2740@3de37c8...` | `app/src/main/java/com/eve/app/telegram/qr/QrPatternField.java` | ADAPTED Java implementation; source springs/placement with EVE palette callback and injected density |
| `QrShareView.java:208-229,2742-2787,6637-6768@3de37c8...` | `QrPatternField.java`, `app/src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt` | ADAPTED source pan/release/easing with Kotlin MotionEvent adapter and reduced-motion policy |
| `QrShareView.java:3369-3407@3de37c8...` | `TelegramQrSceneView.drawPattern()` | ADAPTED source lattice placement; EVE selected native Drawable frames instead of Telegram Document/AnimatedEmojiDrawable |
| `QrShareView.java:1264-1305,2843-2867@3de37c8...` | existing EVE NativeQrDrawable frame cache | NOT MET original atlas, item swap compositor or gift loader; excluding account/document services intentionally |

New `app/src/test/java/com/eve/app/telegram/qr/QrPatternFieldTest.java`
exercises production Java geometry, wave impulses, gesture interruption and
bounded state cleanup. Live Android GPU behavior, compositor transitions,
60/90/120Hz motion, picker morph, and full native screenshot comparison remain
unaccepted. Existing source library, authentication, six-pick selection,
QR payload and export behavior remain retained. The source original shine
pipeline is not imported. GPL combined-release review remains OPEN.

## R2/A1 source Java old/new emoji slot compositor (2026-11-02)

Official source `QrShareView.java:198-211,1218-1260,2837-2872,
3187-3212,3369-3411@3de37c8cfb039452be7398909461da6288babf15` opened against the pinned Git
revision. Original `slotScale` and `backOut` equations, 0.18s shrink,
0.135s incoming start, 0.315s growth and 0.3s per-cell delay are in Java
`app/src/main/java/com/eve/app/telegram/qr/QrPatternSwap.java`.
`TelegramQrSceneView.kt` retains the current and outgoing EVE selected
Drawables until the last 0.75s pattern swap settles; same-key slots are
unchanged, others use source old/new transforms. Rapid subsequent selections
replace the last outgoing set without retaining accumulated old patterns.
Export draws the target only and does not advance the live pattern clock.

| Upstream path:lines@SHA | EVE file | Classification |
|---|---|---|
| QrShareView.java:3187-3212@3de37c8cfb039452be7398909461da6288babf15 | QrPatternSwap.java:17-40 | EXACT source equation bodies, ADAPTED public helper and class |
| QrShareView.java:3369-3411@3de37c8cfb039452be7398909461da6288babf15 | TelegramQrSceneView.kt:drawPattern/drawEmojiSlot | ADAPTED EVE animation cache and local-only scene hosting |
| QrShareView.java:1218-1260,2837-2872@3de37c8cfb039452be7398909461da6288babf15 | TelegramQrSceneView.kt:setEmojiChoices/loadSelectedAnimations | ADAPTED lifecycle and replacement/retention boundary; no Telegram account or gift loading |

`QrPatternSwapTest.java` tests source curve/delay, no-old pattern
pop, negative odd lattice rows, and transition expiry. Exact-SHA
CI and Android native matched visual/gesture comparison remain required.
Original per-pattern frame atlas, GPU shimmer and picker sheet composition
are still NOT MET. EVE six-selection/authentication and upstream GPL
release obligations remain unchanged.

## R2/A1 original Java background shimmer raster (2026-11-02)

Current status: ADAPTED; full pattern/scene fidelity remains NOT MET.
Pinned official Telegram revision: `3de37c8cfb039452be7398909461da6288babf15`.
Opened `QrShareView.java:2488-2559,2561-2562,2805-2838,3111-3160` and
current `RimTune.java`, `QrBackdropScene.java`, `TelegramQrSceneView.kt`.

| Upstream reference | EVE production path | Classification / dependencies |
|---|---|---|
| `QrShareView.java:2805-2838,3111-3160@3de37c8...` | `app/src/main/java/com/eve/app/telegram/qr/QrSceneShine.java` | ADAPTED original Java time/phased curved/cylindrical auto-sweep; EVE motion and root-open gate; source tilt disabled by current SHINE_TILT=0 |
| `QrShareView.java:2488-2559@3de37c8...` | `QrSceneShine.draw()` | ADAPTED original 12dp coarse bitmap, valueNoise, BG_STR/BG_W/BG_NOISE math; EVE allocation cap/close |
| `QrShareView.java:2833-2838@3de37c8...` | `QrBackdropScene.drawBackground()` | ADAPTED draw background light ahead of actual shared Drawable pattern, through existing RenderNode scene sampling |
| `QrShareView.java:154-155,1264-1305,3260-3289,3459@3de37c8...` | existing `NativeQrDrawable` frame sharing | Existing EVE ADAPTED cache; upstream's "atlas" comments do NOT implement an active emoji GPU atlas; actual emoji draws per Drawable. Source explicitly avoids drawVertices on gift symbols due device performance. No invented GPU batching is claimed. |

Production Java rendered pixel test `QrSceneShineTest.java` checks the original
intro/opaque-pixel band and skip on reduced motion, reveal and dispose.
It does NOT verify real GPU blur, screen matched frames, touch physics
or GLSL/tilt/light on native devices. Existing QR username/auth, six choices
and gallery export remain EVE-owned. Source GPL/release disposition OPEN.

## A2 independent source Java sheet RenderNode blur and color lift (2026-11-02)

**ADAPTED, not full Telegram sheet parity.** Inspected official
`DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
`QrShareView.java:4730-4767,4793-4828`. Original `LIFT_DS=8`
records the QR scene backdrop into a separate RenderNode, applies
stronger blur ramp `BLUR→SH_BLUR` plus luminance-aware
contrast/brightness matrix, and quantizes the effect to 1/50 steps.
Mapped to `QrPickerSheetLift.java` and
`QrBackdropScene.drawPickerSheetBackdrop()`; legacy and software paths
use original already-adapted scene stand-in, while the separate sheet
node is discarded at detach. EVE `UsernameCardActivity` wires the
actual picker panel background to this source blur using the current
`QrPickerSheetSurface` fraction. Source Java is ADAPTED for EVE's
dialog window/Drawable resources; independent source sheet window,
rim/packs/overscroll gestures and physical GPU frame parity are
NOT MET. `QrPickerSheetLiftTest.java` exercises quantization, blur,
color matrix/luminance endpoints. Production GPL review remains OPEN.

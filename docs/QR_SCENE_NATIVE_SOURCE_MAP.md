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

# QR ready-frame item appearance

Status: ADAPTED source subsystem; whole A1/QR/source-device acceptance remains OPEN.
Base main 40a7bb30fcc2bbb3468d482a0ec6a9e4633e811b; exact-SHA FAST
[41395975426](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41395975426)
SUCCESS, docs 41395975313 SUCCESS. All earlier scanner/native/picker/admin work
is preserved. Official fetched source stays pinned to
DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.

Actual source opened: QrShareView.java:995-1024,1200-1272,3164-3415,3588-3660;
Components/RLottieNative.java:123-144,226-242; jni/lottie.cpp:190-264;
existing EVE SceneView/NativeQrDrawable and renderer tests. Upstream emoji explicitly
receive NO lighting/enlargement from the light band. A reflective emoji shader or
GPU emoji atlas is NOT FOUND in this path and is not invented.

## Minimum original subsystem and mapping

All upstream Java paths below are under TMessagesProj/src/main/java/org/telegram
at the full SHA above; native file is under TMessagesProj/jni.

| Original property:line@SHA | EVE path:line | Classification / reason | Verification |
|---|---|---|---|
| QrShareView.java:1015-1016,3171-3180,3281-3304@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrEmojiAppearance.java:34 | ADAPTED original Java per-pattern timestamp array, per-screen salt, seeded 0..300ms delay and 380ms ready-item appearance; EVE clock/readiness inputs and six-item bounds | five deterministic production-helper JVM tests |
| QrShareView.java:3187-3192,3245-3250@3de37c8cfb039452be7398909461da6288babf15 | QrEmojiAppearance.java:52; ui/profile/TelegramQrSceneView.kt:411 | ADAPTED original backOut envelope multiplied by existing source slot/breath/wave scale; no new shine response | source endpoints/overshoot, separate old/new arrays and late-frame timing tests |
| QrShareView.java:1015-1016,3369-3411@3de37c8cfb039452be7398909461da6288babf15 | ui/profile/TelegramQrSceneView.kt:134,379 | ADAPTED actual current/outgoing pattern ownership; picked-later sentinel avoids applying a second appearance delay on top of source slot swap | rapid pattern replacement/isolation helper tests, existing six-pick/restoration suite |
| QrShareView.java:3291-3304,3596-3652@3de37c8cfb039452be7398909461da6288babf15 | ui/profile/TelegramQrSceneView.kt:219,226 | ADAPTED real scene reveal callback; items wait until open settles, source close keeps already-open items; EVE reduced-motion/unbound host settles immediately | existing reveal/cancellation tests and whole host suite; physical motion pending |
| QrShareView.java:3314-3334; RLottieNative.java:128-130; lottie.cpp:190-258@3de37c8cfb039452be7398909461da6288babf15 | telegram/animation/NativeQrDrawable.kt:28 | ADAPTED EVE Drawable around unchanged native renderer; ready only after actual native frame return, no synthetic thumbnail/Unicode frame; export reads cached frame without advancing live playback | Android JNI readiness/frozen-frame/close test compiles; actual device execution pending |

Original path is emojiPatternOrNone -> Pattern.emojiShownAt -> drawEmojiField ->
drawEmoji -> popIn -> emojiFrame/ImageReceiver. Telegram ImageReceiver/account/
AnimatedEmojiDrawable services stay excluded. EVE supplies authorized catalogue
JSON and bounded owned NativeQrDrawable frames. The Java appearance equations stay
Java; Kotlin contains EVE lifecycle/frame/Canvas integration only. Per-pattern
arrays hold at most six timestamps; there are only current and outgoing arrays,
and old arrays are discarded with the corresponding source slot transition.
Rapid changes replace the previous target/outgoing pair, never accumulate patterns.

No appearance clock starts while opening or while the actual frame is unavailable.
Export and reduced motion take the settled endpoint without mutating live appearance
timestamps. Native export does not advance the live frame index or schedule a new
tick. Frame errors stop that Drawable instead of retrying native work once for
every visible lattice copy; the owner must load a new asset instance to retry.
Existing stop/recycle/callback ownership and six-selection ordering remain.

The source optional first-frame/high-device branch is exposed/tested by the helper
but EVE does not import Telegram's device classifier and uses the standard post-open
path. This is an explicit adaptation. Standard glyphs remain saved-data compatibility,
not new animated presets. No premium, gift, Telegram documents/account/network or
payment/inventory is introduced. EVE QR branding and username-only payload remain.

Root Telegram GPLv2/per-file and combined-distribution review remains unresolved.
QrEmojiAppearance retains Telegram attribution; THIRD_PARTY_NOTICES and the existing
fail-closed EVE_GPL_RELEASE_APPROVED gate apply. No release clearance is inferred.

## Actual validation

Stable local :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:lintDebug
:app:assembleDebug -Peve.nativeAbis=x86_64 passes: 745 tests / 171 suites,
zero failures/errors/skips; lint zero errors / 1645 warnings. Existing source QR
raster test still decodes all 160 palette/size combinations plus PNG rotation cases.

A synthetic EVE-owned solid Lottie fixture was verified against the same optimized
host C API: parse succeeds, render status TLOTTIE_OK, all 16,384 first-frame pixels
visible. The new AndroidTest uses this fixture, waits 100ms of real playback clock,
asserts export keeps the cached native index/pixels while playback remains active,
and checks closed resources never report ready. This is compiled, NOT executed on
Android locally. Host C API fixture verification is not physical JNI evidence.
Pinned renderer security suite remains 243 green in the prior exact-SHA FAST.

Remaining NOT MET: original parent/complete scene composition and typography,
source first-frame device classification, full native sharing/cache/scheduling and
GPU/memory/frame pacing validation, exact scanner controls, picker parent/gestures,
matched Telegram/EVE 60/90/120Hz captures, device/a11y/theme/navigation matrices,
mission-final six-job FULL/verified APK and human GPL distribution review.

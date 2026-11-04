# Native Android 35 three-button emulator investigation | 2026-11-04

## Evidence-based requirement ledger

R1: Keep original QR JNI picker, full Android 35 x86_64 core suite, all four native screenshot shards, gesture and three-button nav, genuine animation and fail-closed instrumentation. No test skipping, synthetic screenshots, extra timeout or changed EVE runtime features.

R2: Stabilize the three-button emulator. On FULL 41784526767 and 41786398388, Android build/JVM/lint/backend/Functions and gesture mode passed; three-button emulator disconnected after starting real QrEmojiPickerNativeTest, before any test completion. The deprecated swiftshader_indirect renderer and Vulkan-disabled form both failed. On FULL 41789370853 at db03e384f28d7fe54a81d73fff8d614f7317b59b, three-button still failed with supported -gpu software. Hosted emulator logs specifically recorded 'Failed to find ColorBuffer' and 'Failed to bind to texture: invalid color buffer', then TCP port 5554 connection refused and no terminal JUnit report. This is evidence of emulator graphics/device failure, not an app assertion failure. The gesture job of 41789370853 was still running when this version-control experiment was prepared; recheck exact results.

R3: Eliminate floating emulator-version drift for both nav modes. Official Android Emulator 37.2.12 is the prior stable (October 2026) and its observed build ID is 16428233; current failing 37.2.13 is build 16740729. The installed android-emulator-runner supports 'emulator-build' pinning through Google's official distribution URL. Both modes now use 16428233 with software GPU/Vulkan disabled; APK checksums, API 35, Android test corpus and all gating remain unchanged.

## Status

Pin effectiveness is UNVERIFIED until exact-SHA FULL on the carrying commit has completed successfully in BOTH native navigation modes. The APK publish job remains manually gated and is expected SKIPPED for an ordinary [eve-full] push. Never call whole-EVE UI, Telegram source parity, 60/90/120Hz device physics or GPL legal clearance complete from this infrastructure experiment. No Worker production deployment, D1 migration or release artifact publication.

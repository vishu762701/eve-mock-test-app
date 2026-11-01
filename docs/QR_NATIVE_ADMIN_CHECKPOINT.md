# Telegram QR / admin implementation checkpoint

This is an **incomplete implementation checkpoint**, not final feature acceptance.

## Owner scope and decisions

R1: “QR screen exact Telegram” including supplied screen, exported image, palette, emoji/gift sheets, scanner, gallery/torch, share/save, geometry, motion, gestures and states.
R2: “admin dashboard ... telegram jaisi ... with all tabs”, preserving existing authorized EVE actions.

Owner subsequently approved dependencies and routine implementation (“Boss sab approve h bas exact telegram krva de”). Production deployment remains separately gated. Existing EVE identity, permissions and backend authorization remain authoritative. Telegram account/protocol infrastructure is excluded by the engineering standard. EVE has no Telegram-owned gifts/account data: this is a required explicit service-adapter difference, never a license to fabricate inventory.

Base EVE main: `5d8cbae75a9ac72bcf5e819bf478726a2611e72a`.
Official Telegram checkout: `3de37c8cfb039452be7398909461da6288babf15`.
Official Telegram-selected tlottie gitlink: `31f1b542f88e7b4be9a01e749920d857535fc715`.

## Requirement ledger

| Requirement | Status | Evidence / remaining work |
|---|---|---|
| R1 full QR screen parity | NOT MET | Actual rounded QR writer, complete offline palette, native animation frames and gallery publication integrated. Main scene remains an EVE Canvas host. Full QrShareView transitions, source emoji sheet, gifts, scanner scene, background blur and runtime frame comparison are incomplete. |
| R2 usable admin tabs, every existing action retained | NOT MET overall | Original Telegram pager renderer hosts Content / People / Communication / Insights / System; original bound rows/callbacks retained. Activity-backed JVM navigation test passes. Device/theme/permission and full action verification remain required. Telegram has no demonstrated equivalent of EVE's whole mock-test administration dashboard. |
| Official source pin/provenance | MET for imported files | Per-file manifest authenticates all imported files against pinned Git objects; copied submodule files match bytes. This proves origin, not UI parity. |
| Complete exact-SHA CI and final APK integrity | NOT MET at checkpoint creation | Local checks recorded below. No new-SHA CI success or mission-final APK is claimed. |
| Production deployment | Outside authorized scope | No live Worker deploy, migration or release approval. |

## Parity map

All upstream Telegram paths below are under `TMessagesProj/` at the pinned revision. Exact hashes and complete file paths are in `qr-native-upstream-manifest.json`.

| upstream path:lines@SHA | EVE file:lines | Classification |
|---|---|---|
| `jni/lottie.cpp` (whole file) + `src/main/java/org/telegram/ui/Components/RLottieNative.java` (whole file) @ `3de37c8...` | `app/src/main/cpp/telegram_lottie.cpp` + `app/src/main/java/org/telegram/ui/Components/RLottieNative.java` | EXACT source bytes; device frame/lifecycle test compiled, runtime acceptance OPEN |
| Official gitlink `jni/tlottie` source + C API @ `31f1b542...` | `app/src/main/cpp/vendor/tlottie/` | EXACT vendored bytes; CPU build only, original tests retained, upstream failures below |
| `src/main/java/org/telegram/messenger/TelegramQRCodeWriter.java` whole file | `app/src/main/java/com/eve/app/telegram/qr/TelegramQRCodeWriter.java` | ADAPTED package/import, EVE center drawable supplied; original rounded-module algorithm retained; upstream fallback SVG loader omitted |
| `src/main/java/org/telegram/ui/QrShare/{QrGlass,QrRender,RimPainter,RimTune}.java` whole files | `app/src/main/java/com/eve/app/telegram/qr/` | ADAPTED packages/display-unit imports. EVE Drawable host uses sheen/rim; **no real backdrop blur yet** |
| `src/main/java/org/telegram/ui/QrShare/Backdrops.java` whole file; `QrShareView.java:630-713,1304-1400` | `Backdrops.java`, `QrPalettes.java`, `QrPaletteInk.java` | ADAPTED: offline table/order, tuning/contrast math retained; server palette loader excluded |
| `src/main/java/org/telegram/ui/Components/{PagerSlidingTabStrip,AnimatedFloat,ScaleStateListAnimator,CubicBezierInterpolator}.java` whole files | `app/src/main/java/com/eve/app/telegram/components/` | ADAPTED package/host theme/display units; AppCompatImageView/AppCompatTextView inheritance required by EVE lint, original animation algorithms retained |
| `src/main/java/org/telegram/ui/QrShare/QrShareActivity.java:255-292` | `app/src/main/java/com/eve/app/ui/profile/QrGalleryWriter.kt` | ADAPTED Android storage adapter, real MediaStore pending publication and rollback on write failure; legacy permission only through API 28 |
| `src/main/java/org/telegram/ui/QrShare/QrShareView.java`, `QrEmojiPanel.java`, `QrScanOverlay.java` complete scenes | Existing EVE screen/pickers/scanner | NOT MET: full source subsystem not transplanted. Four-column Unicode/Airbnb preview picker and Copy tool remain, so screenshots must not be called identical. |

`NativeQrDrawable` is an EVE lifecycle/frame scheduling adapter, **not** upstream `RLottieDrawable`'s complete background rendering pipeline. Its current frame draw is on the UI thread; performance/frame pacing requires device validation before acceptance. Remote TGS input is bounded to 4 MiB compressed and decompressed; media client sends no EVE authentication headers. Catalogue availability remains a real dependency.

## Constants and origin

| Constant | Value | Origin |
|---|---|---|
| Palette entries/order | 80; `Backdrops.SKETCH_ORDER` | VERIFIED pinned `QrShare/Backdrops.java` |
| Neon Blue | center `#3FB1FF`, edge `#166ACE`, ink `#0075F1` | VERIFIED `QrShareView.java:694` |
| Edge LAB adjustment | L - 3; a/b × 1.03 | VERIFIED `QrShareView.java:630-639` |
| Pager indicator interpolation | 350 ms, EASE_OUT_QUINT | VERIFIED `Components/PagerSlidingTabStrip.java:249-250` |
| QR payload | registered `eve-mock-test://username/...` | EVE adaptation; never a Telegram account/ownership assertion |
| QR center | EVE E | EVE branding adaptation |
| Palette picker | 6 columns, Material sheet host | ADAPTED, full Telegram sheet motion/geometry unverified |
| Native toolchain | Rust 1.99.1; NDK 27.2.12479018; CMake 3.22.1 | EVE pinned build choices; not original UI constants |

## Checks and known failures

- Debug APK and AndroidTest Kotlin compilation pass for the all-ABI source set. All four native libraries are built.
- First JVM run: 664 tests, 1 new pager-test failure. Root cause: test inflated an unattached root; ViewPager does not populate without a real window. Corrected the test to attach to a real Activity, measure/layout each selected page, assert the actual bound row exists and invoke its real callback. Focused rerun passes; no production assertion removed.
- Vendored tlottie debug suite: 239 passed / 3 failed. Two timeout assertions and `dos::renderer_rejects_generated_work_after_successful_parse` failed. Release serial rerun also failed. Isolated release reproduction of the last test consistently fails at `src/tests/dos.rs:2674` because parse returns `LimitExceeded(ParseMemory)` while that test requires parsing to succeed. Source remains byte-identical to official gitlink; this is an unresolved upstream test/limits conflict, **not** a passed security suite. Do not bypass limits or mark this accepted.
- Local device runtime has not been executed; no `/dev/kvm` is available. The added actual JNI frame/recycle test is included in the existing FULL instrumentation package, not merely a mock.
- Local APK build, AndroidTest compilation, full 664-test JVM run and lint passed. Later new gallery tests and expanded palette matrix passed separately. Exact-SHA CI and native emulator results remain pending; no final feature acceptance is inferred.
- Existing fake hidden admin “contract hierarchy” is preserved because removal would affect unrelated legacy bindings/tests. It is not evidence that those hidden controls are usable.
- GPL combined-distribution review remains OPEN under `telegram-release-compliance-gate.md`.

## Latest local validation result

- All four ABI native libraries compile: arm64-v8a, armeabi-v7a, x86_64 and x86. `llvm-readelf -l` verifies every LOAD segment uses `0x4000` alignment (16 KiB).
- Scoped local `assembleDebug`, `compileDebugAndroidTestKotlin` and both Activity-backed pager navigation / real Canvas QR decode tests pass. JUnit XML: 2 tests, 0 failures/errors/skips. Tests explicitly use plain `android.app.Application` so production startup/auth/typography fetch is not executed.
- Acceptance-claim checker passes structurally; 54 CI helper tests pass; docs verification passes. These are not visual acceptance.
- Initial lint dependency resolution failed (missing lint-gradle 31.5.2 offline, then daemon proxy properties missing online). Supplying proxy system properties to the Gradle command resolved dependencies. Actual lint then found 2 AppCompatCustomView errors in imported pager classes; fixed through AppCompatImageView/AppCompatTextView inheritance, without suppressing lint. The scoped rerun and subsequent full JVM test/lint run both passed. Existing lint warnings remain; no lint rule or baseline was weakened.
- Automatic approval review rejected polling the previous broad Gradle/test process because it could contact `eve-backend.anyqueairdrop.workers.dev` without authorization to send potentially sensitive data. Inspection found a legacy public GET at `AdminDashboardRealDataVerificationTest.kt:39-48` (no headers/body/credentials/personal data) plus production Application startup fetches. A materially safer JVM-only fixture now initializes real UI theme/inset lifecycle without production auth/presence/typography jobs (`OfflineUiTestApplication` + test-only `robolectric.properties`). Every existing assertion remains; the resulting broad rerun passed **664 tests, 0 failures/errors/skips** and lint. New-SHA CI is still pending.
- Two intermediate packaging attempts hit missing cached `.rsync-tmp/classes.dex`; disabling the Gradle build cache regenerated dex successfully. No source or assertion was removed to address this environment/cache failure.
- `app/src/main/assets/licenses/tlottie-MIT.txt` now includes the original MIT license in the APK.

Additional focused checks: **160 real raster decodes** (80 palettes × 2 screen sizes) passed. Two gallery tests passed: real PNG signature and MediaStore pending-to-published transition; injected write failure deletes the pending row and never publishes it. The tests use a registered local provider/output stream. An initial fixture setup/Java file-descriptor incompatibility was corrected through supported Robolectric stream registration; production storage behavior was not replaced.

## Side-by-side acceptance checklist (all pending device evidence)

1. Same device density/insets/font scale: compare default screen, all palettes, long/short username, light/dark and small/tall screens; scan the QR from screen and saved/shared image.
2. Record original and EVE open/close, interruption, wave/recolour, drag/fling, emoji pick/unpick, six-selection cap, picker transitions and gift states with timestamps. Screenshots alone cannot establish physics.
3. Verify scanner camera permission denied/permanent denial, gallery cancelled/invalid/malformed/foreign QR, supported EVE route, torch unavailable, pause/resume and result de-duplication.
4. Verify real gallery publication/failure/cancellation and share URI grants; exported image must omit app chrome and preserve intended background.
5. Admin: verify every existing action in every section with owner/full admin/limited admin, search, swipe/tab/back, rotation restoration and live theme change. No authorization path may be skipped.
6. Run exact-SHA FAST, then mission-final six-job FULL and checksum the single verified APK. Keep fidelity acceptance OPEN until original/EVE video/frame evidence and human review are recorded.

## Rollback and continuation

Revert this implementation commit with a forward-only revert; do not reset or force-push main. New source files can only be removed together with their build/JNI/call-site changes; deleting native files alone breaks the build. Existing backend/migrations and unrelated profile/maintenance/notifications are unchanged.

Next: resolve the upstream renderer-test conflict with the pinned source (without weakening limits); finish dependency-complete QrShareView/emoji/scanner scene adapters, restore source gift UI against actual authorized EVE data, replace remaining Material/Unicode substitutes, collect device evidence, then run the final gate. A green checkpoint build is not feature acceptance.

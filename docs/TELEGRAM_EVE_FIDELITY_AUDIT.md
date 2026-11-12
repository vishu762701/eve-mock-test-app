# Telegram → EVE fidelity audit

**Recorded:** 2026-11-12
**Telegram source:** `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`,
13.1.1/7237, extracted from `/sdcard/Download/Telegrammaster.zip`
(SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`).
**EVE working tree:** `ab1ec2cefb1883575cbdeba1bd022edf3332cd79` plus uncommitted
changes; see `docs/TELEGRAM_EVE_IMPLEMENTATION_LOG.md`.

Statuses are limited to **EXACT / ADAPTED / MISSING / BLOCKED**. No item is
declared EXACT unless the source algorithm/data was directly reused and no
material behavioral deviation exists.

## Summary

| Status | Meaning in this audit |
| --- | --- |
| EXACT | Source bytes/constants/algorithm reused without material behavioral deviation |
| ADAPTED | Source behavior retained with a documented EVE host, identity, data-contract, or theme adaptation |
| MISSING | Required source behavior is absent or materially different |
| BLOCKED | A concrete external limitation prevents implementation or verification |

The photo-menu/editor batch is code-complete against the inspected source, but
Android Kotlin/resource compilation and targeted pure-JVM tests were executed
for this tree, and APK packaging passed with native CMake disabled in a
temporary copy. Instrumentation, the full Robolectric suite, exact-SHA CI and
device/pixel/motion verification remain **BLOCKED** in this environment. Prior
profile/QR subsystems remain ADAPTED code from earlier work; this audit does not
convert their historical build reports into current device evidence.

## Feature ledger

| # | Feature | Telegram source | EVE implementation | Status | Code-level differences and evidence | Verification |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Photo action menu composition | `ImageUpdater.java:253-332` | `ProfilePhotoMenuModel.items` in `ProfilePhotoMenu.kt` | **ADAPTED** | Source order and labels retained: `Take photo` → `msg_camera`, `Upload from gallery` → `msg_photos`, conditional `Remove photo` → `msg_delete` (red). EVE omits Telegram-only video recording and web-search items because its product scope has neither. Telegram's `BottomSheet.Builder` is replaced by a Material `BottomSheetDialog`. | Code inspection; `ProfilePhotoInputContractTest`. Android execution BLOCKED. |
| 2 | Photo menu geometry | `BottomSheet.java:1040-1145,1400-1432` | `ProfilePhotoMenu.show` | **ADAPTED** | 48 dp title and rows, 20 sp bold title, 21 dp title side padding (6/8 top/bottom), 56×48 dp icon column, 72 dp text start, 16 sp labels, 5 dp icon leading inset, and destructive `#FFCC4747`/`#FFCC2929` roles are retained. Host ripple/selector, rounded surface and window scrim are EVE Material adaptations, not Telegram's full `BottomSheet` drag/IME renderer. | Code inspection; source-constant test. Pixel comparison BLOCKED. |
| 3 | Camera permission and capture | `ImageUpdater.java:650-718`; `AndroidUtilities.java:6269-6320` | `ProfileActivity.requestCameraCapture` / `launchCameraCapture` / `takePhotoLauncher`; `ExifTransform` | **ADAPTED** | `CAMERA` runtime permission, `ACTION_IMAGE_CAPTURE`, FileProvider URI, read/write URI grant (provided by `TakePicture`), and preview reuse match the source flow. EVE uses `ActivityResultContracts.TakePicture` rather than a raw request code and creates a cache file instead of `AndroidUtilities.generatePicturePath()`. Captured EXIF orientation is now applied on save using the source `(rotation, invert)` mapping and matrix order. Unlike Telegram, EVE does not copy the capture into the public device gallery; the cache file is deleted after save/cancel. The capture enters the source-derived square crop screen (row 6). | Code inspection; manifest/FileProvider inspection; `compileDebugKotlin` PASS. Camera runtime BLOCKED. |
| 4 | Gallery selection | `ImageUpdater.java:719-760,817-830`; `AndroidUtilities.java:6269-6320` | `ProfileActivity.pickPhotoLauncher`; `AvatarCropActivity`; `ExifTransform` | **ADAPTED** | `GetContent("image/*")` uses the Android Storage Access Framework; Telegram's `PhotoAlbumPickerActivity` and its media grid are not imported. The selected image opens the source-derived square crop screen (row 6), then enters EVE's authenticated local-photo preview/save path with EXIF orientation baked in. No storage permission is needed. | Code inspection; `compileDebugKotlin` PASS. Runtime BLOCKED. |
| 5 | Remove photo | `ImageUpdater.java:297-321`; `ProfileActivity.java:6993-7000` (caller) | `ProfileActivity.removeProfilePhoto`; `ProfilePhotoManager.removePhoto` | **ADAPTED / MISSING (fallback avatar)** | The destructive item appears only for an EVE-managed local photo, deletes it, verifies by `hasCustomPhoto`, and shows success/failure bulletins. It does **not** delete a Firebase/Google fallback avatar, so a Google profile photo can reappear after removal. That provider-owned deletion is MISSING by deliberate scope decision; EVE has no safe API to delete the Google account photo. | Code inspection; no device test. |
| 6 | Crop / avatar constructor after selection | `ImageUpdater.java:592-646,845-930`; `PhotoCropActivity.java:40-520`; `ProfileActivity.java:861,2911` | `ui/profile/AvatarCropActivity.kt`, `AvatarCropView.kt`, `AvatarCropGeometry.kt`, `res/layout/activity_avatar_crop.xml`; `ProfilePhotoManager.decodeForCrop` / `saveCroppedPhoto` | **ADAPTED** | Ports the fixed square avatar crop from `PhotoCropActivity.PhotoCropView`: 14 dp image inset, centered initial square, drag-to-move, four corner-resize states, 80 % scrim, 2 dp border, 20 px-class handles, rule-of-thirds grid, and source dark colours. Output is an up-to-800×800 quality-80 JPEG in cache, adopted by one atomic copy without a second encode; non-square/oversized results fall back to `savePhoto`. EVE fixes the upstream `getBitmap` width/width divisor for non-square photos and maps each axis independently. Telegram's rotate control, freeform crop, video-avatar constructor, and PhotoViewer transition are not ported; this is avatar-only. | Code inspection; `AvatarCropGeometryTest` 7 tests PASS; `compileDebugKotlin` PASS. Touch/pixel behavior BLOCKED. |
| 7 | Photo temp-file lifecycle and rotation | `ImageUpdater.java:650-718,817-830` | `ProfileActivity` camera state (`STATE_CAMERA_URI`, `STATE_CAMERA_FILE`, `onSaveInstanceState`, `restoreCameraCaptureState`, `clearCameraTemp`, `isChangingConfigurations`) | **ADAPTED** | EVE restores a pending capture after configuration change and deletes the temp file on cancellation/failure/non-configuration destruction. Telegram keeps `currentPicturePath` in the fragment and relies on `onActivityResult`. EVE's process-death restoration is an addition; it does not claim Telegram's exact lifecycle. | Code inspection; no Robolectric execution. |
| 8 | Photo action error/loading/dismissal states | `ImageUpdater.java:253-332,650-718` | `ProfilePhotoMenu`, `ProfileActivity`, `AppBulletin` | **ADAPTED** | Menu dismiss, one-dialog guard, permission denial, missing camera app, FileProvider failure, save in-flight disable, verification failure and success are defined. Telegram's exact sheet animation/dismiss physics is not reproduced. | Code inspection. Runtime BLOCKED. |
| 9 | Username editor open/save | `ChangeUsernameActivity.java:790-800,1360-1400,1442-1448` | `ChangeUsernameActivity.kt` | **ADAPTED** | IME Done forwards to Done; unchanged handle closes with no write; save is followed by authenticated readback before success. EVE shows the keyboard on open while source only focuses (`focusUsernameField(false)`). EVE's minimum is 5, source's service minimum is 4. Source allows clearing to empty; EVE's Worker rejects it (row 10). | Code inspection; source-property tests. Android tests BLOCKED. |
| 10 | Username clear-to-empty | `ChangeUsernameActivity.java:1360-1400` | `ChangeUsernameActivity.saveUsername` | **BLOCKED** | Source sends an empty username to clear it. The existing EVE Worker `validateUsername` rejects length < 5 (`backend/src/routes/auth.ts:16-21`), and `PUT /api/users/profile` always validates a supplied username. Changing that contract is outside this UI batch; the editor now explains the rejection instead of silently returning. | Worker source/test inspection. Not implemented. |
| 11 | Username validation and rejection feedback | `ChangeUsernameActivity.java:1196-1300,1400-1412,1448-1463` | `ChangeUsernameActivity.checkUsernameLocallyAndRemotely`, `rejectUsername` | **ADAPTED** | Source rules (no leading digit, no leading/trailing `_`, ASCII alnum/`_`), 300 ms debounce, availability check, and `shakeIfOff` spring/APP_ERROR are retained. EVE shakes the local field and status; Telegram shakes its header/help/field/alias cells. EVE's server error text/localization differs. | Code inspection; source-constant tests. Runtime BLOCKED. |
| 12 | Display-name editor | `ChangeNameActivity.java:45-230` | `ChangeNameActivity.kt` | **ADAPTED** | IME Done, focus/keyboard, unchanged-name close without write and readback confirmation are retained. Telegram edits separate first/last single-line fields with no client length/blank validation; EVE edits one multi-line display name with a 1–100-character contract and adds a shake/bulletin rejection. | Code inspection; existing Robolectric test source is unrun. |
| 13 | Bio editor | `ChangeBioActivity.java:88-180,200-252` | `ChangeBioActivity.kt`; `res/layout/activity_change_bio.xml` | **ADAPTED** | Codepoint-based counting, 18 sp input, 36 dp minimum height, four-line maximum, IME Done, unchanged-bio close, newline stripping on save and source `shakeView` + 200 ms vibration are retained. EVE's limit is 160 (not Telegram's 70/140 service limits) and also enforces the Worker's 160 UTF-16-unit cap. The counter is a plain `TextView`, not Telegram's animated `NumberTextView`; hint/help copy is EVE-owned. The field is disabled while the authenticated request is in flight, which is an EVE async-network adaptation. | Code inspection; filter/layout tests source-written but unrun. |
| 14 | Bio trailing-newline Done | `ChangeBioActivity.java:111-128` | `ChangeBioActivity` input filter | **ADAPTED** | Inserted source ending in `\n` calls `saveBio()` and returns `""`, matching the source filter's `doneButton.performClick()`. EVE implements it in an EVE filter wrapper that also composes the server unit cap. The source's animated counter/line colors are not reproduced. | Code inspection; filter test source-written. |
| 15 | Codepoint filter | `CodepointsLengthInputFilter.java:7-30` | `CodepointsLengthInputFilter.kt`, `BioLengthInputFilter.kt` | **ADAPTED** | The pure codepoint algorithm is ported directly, including surrogate-safe truncation. `BioLengthInputFilter` composes it with EVE's 160-unit server cap so a 160-codepoint emoji bio cannot be rejected by the Worker. | Code inspection; JVM tests unrun. |
| 16 | Rejection shake/haptic | `AndroidUtilities.java:3489-3554`; `BotWebViewVibrationEffect.java:18` | `TelegramShake.kt`; `VibrationHelper.vibrateAppError`; `vibrateOneShot` | **ADAPTED** | `shakeView` is 300 ms, 4 dp, damped sine; `shakeViewSpring` is 600 stiffness with `-shift*100` velocity; APP_ERROR is 30/10/150/10 ms at 0/100/0/100 with the source fallback. EVE uses a weak registry and no Telegram View tags; vibration hardware failures are swallowed. | Code inspection; no vibration hardware test. |
| 17 | Profile shell / header / rows | `ProfileActivity.java:3909-4004,5354-5359,6039-6047`; `TextDetailCell.java:68-145` | `ProfileActivity.kt`, `activity_profile.xml`, `TelegramProfileSection.kt` | **ADAPTED** | Existing EVE work retains source row roles and grouped geometry but uses EVE navigation, Firebase identity, backend fields and Material views. Full screen pixel parity is not asserted. | Code inspection; prior evidence documents only. Current runtime BLOCKED. |
| 18 | Gooey avatar / cutout / blur | `ProfileGooeyView.java`; `NotchInfoUtils.java` | `ProfileGooeyView.kt`, `NotchInfoUtils.kt`, `ProfileStackBlur.kt`, `ProfileAvatarTrajectory.kt` | **ADAPTED** | Existing EVE RenderEffect/CPU fallback and full-window host are source-derived but not the complete upstream renderer. GPU/cutout/device behavior unverified here. | Code inspection. Device/GPU verification BLOCKED. |
| 19 | Pull-to-expand and scroll settle | `ProfileActivity.java:4190-4237,4320-4340,8465-8520` | `ProfilePullScrollView.kt`, `ProfileScrollState.kt`, `ProfileHeaderExpansion.kt` | **ADAPTED** | Source resistance/latch/settle constants are retained; EVE's NestedScrollView and accessibility sizing are host adaptations. Physical touch/velocity behavior unverified. | Code inspection; prior Robolectric source tests unrun. |
| 20 | Action press/release physics | `ButtonBounce.java:60-94,119-126` | `ProfileActionBounce.kt` | **ADAPTED** | Exact 60/350 ms, 4 % scale, 20 px drift, 250 ms haptic constants; EVE touch dispatch and accessibility click path differ from Telegram's Canvas action renderer. | Code inspection; `ProfileActionBounceSourceTest` 2 tests PASS. |
| 21 | Action canvas/collapse/accessibility | `ProfileActionsView.java:247-337,412-460,512-587,1247-1328` | `ProfileActionsCanvasLayout.kt`, `ProfileActionsCollapse.kt`, `ProfileActionAccessibility.kt` | **ADAPTED** | Source row/control sizes and alpha stages retained; the full Canvas/shader parent renderer and virtual accessibility nodes are not ported. | Code inspection. Runtime BLOCKED. |
| 22 | Overflow popup | `ProfileActivity.java:4009`; `ActionBar.java`; `BaseFragment.java` | `ProfileOverflowPopup.kt` | **ADAPTED** | Owner-only actions, one-window dismissal and lifecycle cleanup; EVE popup host/anchoring is not Telegram's `ActionBarPopupWindow`. | Code inspection. Pixel/outside-touch unverified. |
| 23 | Personal QR scene | `QrShareActivity.java:466,819-845`; `QrShareView.java` cited ranges; `Backdrops.java:8-22`; `QrCodeLayer.java:139-154` | `UsernameCardActivity.kt`, `TelegramQrSceneView.kt`, `TelegramQrMotion.kt`, `QrEmojiPattern.kt`, `ProfileQrPayload.kt` | **ADAPTED** | Source timings/palettes/emoji-slot math are reused in an EVE Activity. The payload is `eve-mock-test://username/<username>` and contains only the username. Telegram account themes, gifts, paid messages and media documents are excluded. | Code inspection; prior QR tests. Current execution BLOCKED. |
| 24 | QR scanner/presentation | `QrScanOverlay.java` cited ranges; `CameraView.java:1012-1035,1096-1106` | `EveQrLiveScannerActivity.kt`, `telegram/qr/*.java`, `QrScanMorphLayout.kt` | **ADAPTED** | Source overlay geometry/motion and camera-glass paint are adapted to EVE CameraX/ZXing. Telegram CameraController/account/session and MTProto are excluded. | Code inspection; camera runtime BLOCKED. |
| 25 | QR payload privacy | EVE-specific integration; source QR identity excluded | `ProfileQrPayload.kt`, `UsernameCardActivity.kt` | **ADAPTED** | Only normalized username bytes are encoded; phone, DOB, UID, tokens, query/fragment/extra path, userinfo and non-EVE schemes fail closed. A shared username is not proof of account ownership. | Code inspection; prior payload tests. Current execution BLOCKED. |
| 26 | Theme/Day-Night roles | `ThemeColors.java:79,96-97,133,199,205`; `night.attheme` | `TelegramThemeEngine.kt`, `TelegramThemeSelectors.kt`, `telegram_colors.xml` | **ADAPTED** | Source roles and selector alpha math retained; EVE contrast overrides are documented separately. New destructive red pair matches source defaults. | Code inspection; no screenshot/device comparison. |
| 27 | RTL, large font, insets, keyboard | Source layout/geometry branches | Existing EVE layouts and host | **BLOCKED** | RTL icon/text padding uses `setPaddingRelative`, and EVE layouts have tokenized touch targets, but no emulator/device is available to verify RTL, font scale, IME resize, cutout or navigation-bar insets. | Not run. |
| 28 | Android build/tests/CI | n/a | Gradle/CI | **BLOCKED** | Kotlin and unit-test sources compile, 11 targeted pure-JVM tests pass, and a native-disabled copy produced an 18 MB debug APK. Production native CMake cannot execute on this aarch64 host (x86_64 NDK/CMake tools), the full Robolectric suite fails on its unsupported aarch64 native runtime, and no commit/push authorization exists for exact-SHA GitHub Actions. No emulator/KVM. | Partial build/test PASS; native build, full suite, CI and device BLOCKED. |
| 29 | Pixel/motion/device parity | n/a | n/a | **BLOCKED** | No matched Telegram frames, physical device or emulator. Code inspection and byte-exact assets cannot establish pixel or frame timing equality. | Not run. |
| 30 | GPL/combined-work release clearance | `LICENSE` (GPLv2); per-file GPLv2-or-later Java headers | `THIRD_PARTY_NOTICES.md`, `docs/telegram-release-compliance-gate.md` | **BLOCKED** | Source adaptation increases the existing combined-work review scope. No release approval is implied; `EVE_GPL_RELEASE_APPROVED` remains unapproved. | Legal/release review outstanding. |
| 31 | EXIF orientation and avatar export transform | `AndroidUtilities.java:6269-6320`; `PhotoViewer.java:11350-11366`; `ImageUpdater.java:845-855` | `util/ExifTransform.kt`; `ProfilePhotoManager.savePhoto` | **ADAPTED** | All eight EXIF orientation values map to Telegram's `(rotation, invert)` pairs, and the save path applies `postRotate` then horizontal/vertical `postScale` before center-cropping. Decode is sampled so the shorter side stays at least 800 px, avoiding full-resolution camera memory spikes; export is one up-to-800×800 JPEG at quality 80 instead of Telegram's separate 800×800/150×150 cloud variants. The completed JPEG is published by a same-directory rename, so an interrupted save cannot truncate the prior avatar. EVE's local-only storage and single-file avatar contract remain. | Code inspection; `ProfilePhotoTransformSourceTest` PASS; `compileDebugKotlin` PASS. Bitmap/runtime behavior BLOCKED. |

## Intentionally excluded Telegram-only features

These are not counted as EVE MISSING because EVE does not have the Telegram
account/media product that gives them meaning. They must not be added as
fake UI:

- Telegram login, MTProto, account switching, contacts, chats, calls, bots.
- Telegram avatar video recording, custom emoji-document avatars, web image
  search, gifts, paid messages, and server theme catalogues.
- Telegram camera controller/session, media picker grid, and cloud photo sync.

## Verification boundary

**Code inspected:** source and EVE files cited above; XML layouts, manifest
permissions/FileProvider, string resources, and backend `validateUsername` /
profile field contract.

**Source identity verified:** 16 representative archive files, including all
edited source Java files and the imported assets, match
`DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` byte-for-byte through a
partial Git checkout; `verify_telegram_imports.py` passes 16/16 recorded assets.

**Build passed:** `:app:compileDebugKotlin` and
`:app:compileDebugUnitTestKotlin` PASS with Gradle 8.7/JDK 21/Android SDK 36.
A temporary copy with `externalNativeBuild` removed produced an 18 MB debug APK.
The production native CMake build remains BLOCKED on this aarch64 host.

**Tests passed:** 11 targeted pure-JVM profile tests
(`AvatarCropGeometryTest` 7, `ProfilePhotoTransformSourceTest` 1,
`ProfilePhotoScopeTest` 1, `ProfileActionBounceSourceTest` 2), plus repository
Node backend/function tests and Python governance tests. The full 1331-test
Robolectric run remains BLOCKED by its unsupported aarch64 native runtime (487
environmental failures); instrumentation was not executed.

**CI passed:** none for these uncommitted changes.
**Runtime/device verified:** none.

The first unfinished verification is therefore exact-SHA CI and
device-level camera/crop/touch/pixel comparison. Telegram rotate/freeform crop
and the video-avatar constructor (row 6 scope) remain intentionally unported.

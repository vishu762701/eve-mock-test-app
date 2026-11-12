# Telegram → EVE implementation log

**Session date:** 2026-11-12
**Repository:** `eve-mock-test-app`, branch `main`, base
`ab1ec2cefb1883575cbdeba1bd022edf3332cd79` plus uncommitted working-tree
changes. **No commit or push was made.**
**Telegram source:** `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
(13.1.1/7237), `Telegrammaster.zip` SHA-256
`4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`.

## Implemented batches

### 1. Source-derived profile photo action menu

**New:** `app/src/main/java/com/eve/app/ui/profile/ProfilePhotoMenu.kt`

- `ProfilePhotoMenuModel` reproduces `ImageUpdater.openMenu`
  (`ImageUpdater.java:253-332`): camera, gallery, conditional destructive
  remove.
- `ProfilePhotoMenu.show` reproduces source `BottomSheet` title/cell geometry
  (`BottomSheet.java:1040-1145,1400-1432`): 48 dp title/rows, 20 sp bold title,
  21 dp side padding, 56×48 dp icon column, 5 dp icon leading inset, 72 dp text
  start, 16 sp labels.
- Destructive roles use source defaults: text `#FFCC4747`
  (`telegram_text_red_bold`) and icon `#FFCC2929`
  (`telegram_text_red_regular`).
- EVE theming, scrim, ripple, rounded surface and Material window are documented
  host adaptations.

**Wiring:** `ProfileActivity.kt`

- `btnSetPhoto` and the no-photo avatar click now open the menu instead of
  launching the gallery directly.
- One-sheet guard; dismiss on pause/stop/destroy/theme change; pending-preview
  guard.

### 2. Source-style camera capture and photo removal

**`ProfileActivity.kt`**

- `CAMERA` permission through `RequestPermission`; on grant, capture starts.
- `ActivityResultContracts.TakePicture` sends `ACTION_IMAGE_CAPTURE` with a
  FileProvider `cacheDir` URI, matching `ImageUpdater.java:650-718`’s system
  camera flow.
- Capture result reuses EVE’s existing `previewSelectedPhoto` / save pipeline;
  cancellation/failure deletes the temp file.
- `STATE_CAMERA_URI` / `STATE_CAMERA_FILE` restore a pending capture across
  rotation; `onDestroy` keeps the file only when `isChangingConfigurations`.
- `removeProfilePhoto` removes the EVE-local photo and verifies by file
  existence before showing success; it does not delete the Firebase/Google
  fallback avatar.
- Added English/Hindi strings for the menu, permission denial, unavailable
  camera, removal success and removal failure.

### 3. Editor behavior from Telegram sources

**`ChangeBioActivity.kt`**

- Imports `ChangeBioActivity.java:88-180,200-252` behavior: IME Done,
  trailing-newline Done, codepoint count, newline stripping, unchanged-value
  close, autofocus + keyboard, and source rejection feedback.
- New `CodepointsLengthInputFilter.kt` is a direct port of
  `CodepointsLengthInputFilter.java:7-30`.
- New `BioLengthInputFilter.kt` composes the codepoint algorithm with EVE’s
  existing 160-UTF-16-unit Worker cap, so an emoji-heavy bio cannot pass the
  client and fail the server. The counter shows the stricter remaining value.
- `activity_change_bio.xml`: removed the fixed `maxLength`, set source
  `minHeight=36dp` / `maxLines=4` without a 3-line minimum, and changed the
  counter’s initial text from `0/160` to `160`.
- Rejection uses source `AndroidUtilities.shakeView` (300 ms, 4 dp damped sine)
  and `Vibrator.vibrate(200)`. The field is disabled while the authenticated
  save is in flight so edits cannot race the readback.

**`ChangeUsernameActivity.kt`**

- IME Done, unchanged-handle close, explicit empty-handle rejection, duplicate
  save guard, source 300 ms debounce retained.
- `rejectUsername` applies `shakeIfOff` (`ChangeUsernameActivity.java:1448-1463`)
  spring shakes plus `APP_ERROR` vibration.
- Username clearing remains blocked by the Worker’s 5–32-character contract
  (`backend/src/routes/auth.ts:16-21`); the UI now explains it.

**`ChangeNameActivity.kt`**

- IME Done, focus/keyboard on first open, unchanged-name close without network write
  (`ChangeNameActivity.java:67-72,178-180`), duplicate save guard, 1–100 validation
  with source-style shake/error haptic.
- EVE keeps one display-name field instead of Telegram’s first/last pair.

**`ChangePhoneActivity.kt`**

- IME Done, focus/keyboard on first open, unchanged-phone close without network write,
  duplicate save guard, input disabled during save, and source spring shake + `APP_ERROR`
  vibration on invalid number.
- Retains authenticated readback confirmation before completion.

### 4. Source feedback helpers

**New:** `app/src/main/java/com/eve/app/util/TelegramShake.kt`

- `shakeView`: `AndroidUtilities.java:3489-3512`, 300 ms, 4 dp damped sine.
- `shakeViewSpring`: `AndroidUtilities.java:3526-3554`, 600 stiffness,
  `-shift*100` start velocity, source tag behavior replaced by a weak registry.

**`VibrationHelper.kt`**

- `vibrateAppError` ports `BotWebViewVibrationEffect.APP_ERROR`
  (`BotWebViewVibrationEffect.java:18`): 30/10/150/10 ms, amplitudes
  0/100/0/100, fallback 40/60/40/60/65/60/40.
- `vibrateOneShot(200)` provides the bio filter’s source 200 ms vibration.

### 5. Assets and notices

- Copied `msg_photos.webp` at mdpi/hdpi/xhdpi/xxhdpi and
  `drawable-xxhdpi/msg_delete.png` → `ic_delete.png`, byte-identical.
- Extended `docs/telegram-verbatim-imports.json` from 4 to 16 entries with
  SHA-256 values.
- Updated `docs/telegram-integration-process.md` asset-coverage wording.
- Added the new 2026-11-12 provenance section to `THIRD_PARTY_NOTICES.md`.

### 6. Source square crop before avatar save

**New:** `ui/profile/AvatarCropGeometry.kt`, `AvatarCropView.kt`,
`AvatarCropActivity.kt`, `res/layout/activity_avatar_crop.xml`

- Ports the fixed square avatar crop from `PhotoCropActivity.PhotoCropView`
  (`PhotoCropActivity.java:40-360`): fitted bitmap, centered initial square,
  drag-to-move, four corner resize states, four-rectangle 80 % scrim, 2 dp
  border, 20 px-class corner handles, rule-of-thirds grid, source dark
  `#333333`/`#C8000000`/`#3FFAFAFA`/white colours
  (`PhotoCropActivity.java:55-64,315-350`).
- `AvatarCropGeometry` is pure Kotlin with independently mapped x/y source
  coordinates. It fixes the upstream `getBitmap` width/width divisor
  (`PhotoCropActivity.java:288-310`) for non-square photos and keeps the
  opposite corner fixed during resize.
- Crop decode reuses `ProfilePhotoManager.decodeForCrop`: EXIF orientation is
  baked in, sampled decoding keeps at least 800 px on the shorter side, and the
  longest side is bounded to 2048 px before the interactive surface.
- Done renders the selected square to an up-to-800×800 quality-80 JPEG in
  `cacheDir` (the source `bigPhoto` export) and returns it to ProfileActivity.
  The file is written as `.tmp` and renamed before being published.
- `ProfilePhotoManager.saveCroppedPhoto` adopts that already-square JPEG by one
  atomic copy, avoiding a second re-encode. Non-square/oversized inputs fall
  back to the existing EXIF-normalized `savePhoto` path.
- Gallery and camera results now open the crop screen before EVE's preview.
  Camera temp files are deleted once the crop result returns; crop preview
  files are deleted on save, cancel, non-configuration destruction, or a
  replaced selection. Pending crop state survives rotation via
  `STATE_PENDING_PHOTO_*`/`STATE_CROP_FILE`.
- Telegram's rotate control, freeform crop, video avatar constructor, and
  PhotoViewer transition are not ported; this is an avatar-only ADAPTED path.

### 7. EXIF orientation and source avatar export

**New:** `app/src/main/java/com/eve/app/util/ExifTransform.kt`

- Ports `AndroidUtilities.getImageOrientation` (`AndroidUtilities.java:6269-6320`):
  all eight EXIF values map to Telegram's `(rotation, invert)` pairs.
- Applies `postRotate` then horizontal/vertical `postScale`, matching
  `PhotoViewer.applyCurrentEditMode` (`PhotoViewer.java:11346-11370`).
- Reads orientation from the selected content URI before decoding and fails
  safe to identity when the provider cannot reopen or has no EXIF block.

**`ProfilePhotoManager.kt`**

- `savePhoto` now bakes EXIF orientation into the stored JPEG before
  center-cropping, fixing sideways camera/gallery avatars.
- Export changed from 512×512/q90 to the source's up-to-800×800/q80
  (`ImageUpdater.java:845-855`). EVE still stores one local file instead of
  Telegram's big/small cloud variants.
- Decode now reads the URI once into memory, decodes bounds, and samples to
  keep at least 800 px on the shorter side. This avoids full-resolution camera
  memory spikes; intermediate bitmaps are recycled after compression.
- The completed JPEG is written to a sibling `.tmp` file and renamed over the
  existing avatar only after compression succeeds, so a failed save cannot
  truncate the last verified local photo.
- Photo success/failure bulletins moved to `profile_photo_updated` and
  `profile_photo_save_failed` in English and Hindi instead of hardcoded
  English.

### 8. Required deliverable documents

- `docs/TELEGRAM_SOURCE_DISCOVERY.md`
- `docs/TELEGRAM_EVE_SOURCE_MAP.md`
- `docs/TELEGRAM_EVE_FIDELITY_AUDIT.md`
- `docs/TELEGRAM_EVE_IMPLEMENTATION_LOG.md` (this file)

### 9. List Flicker Elimination & Telegram ButtonBounce Physics Integration

- Upgraded `ExamAdapter.kt` and `ExamTestsAdapter.kt` to calculate granular `DiffUtil` diffs instead of firing `notifyDataSetChanged()`. This eliminates full-list card flashing, preserves scroll position during periodic background reloads, and prevents redundant View re-binding.
- Upgraded `AdminBannerAdapter.kt` and `PollsAdminAdapter.kt` with `DiffUtil` dispatch, preventing UI jumping and flashing when banners are reordered or poll disclosures change.
- Upgraded `ButtonBounce.kt` (`org.telegram.ui.Components.ButtonBounce.java:60-94,119-126`) so `bounce()` animates `view.scaleX` and `view.scaleY` directly during spring recovery.
- Replaced legacy `UiArcMotion` in `TargetExamsBottomSheet.kt` with authentic Telegram `ButtonBounce` physics on target exam chip selection.

## Source identity verification

1. Fetched the pinned commit into a throwaway partial checkout:
   `git fetch --depth 1 --filter=blob:none origin 3de37c8c...` into
   `/tmp/eve-telegram-git`; `HEAD` was set to that commit. No EVE file or ZIP was
   modified.
2. Hashed 16 representative archive files and compared them with
   `git show HEAD:<path>`: **16/16 MATCH**, including all edited Java sources,
   the three editor classes, `CodepointsLengthInputFilter.java`,
   `AndroidUtilities.java`, `BotWebViewVibrationEffect.java`, `gradle.properties`
   and the imported raster assets.
3. `python3 .github/scripts/verify_telegram_imports.py --upstream /tmp/eve-telegram-git`
   → `PASS: 16 recorded byte-identical imports; no runtime parity or license
   conclusion`.

The original ZIP was re-hashed and remains
`4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`.

## Verification actually run

| Command | Result | Evidence class |
| --- | --- | --- |
| `node .github/scripts/verify-docs.mjs` | PASS; 87 Markdown files, 0 errors, 0 warnings | Docs/static |
| `python3 -m unittest discover -s .github/scripts/tests -p 'test_*.py'` | `Ran 78 tests ... OK` | Python governance/host |
| `npm run build && npm test` in `backend/` | `tsc --noEmit` passed; 372 tests passed, 0 failed | Backend build/test |
| `npm run build && npm test` in `functions/` | `node --check` passed; 19 tests passed, 0 failed | Functions build/test |
| `python3 .github/scripts/verify_telegram_acceptance.py` | PASS structurally (personal QR, profile overflow, profile shell); gate itself notes it cannot prove pixel/motion/source/license equivalence | Static acceptance |
| `python3 .github/scripts/verify_telegram_imports.py --upstream /tmp/eve-telegram-git` | PASS; 16/16 byte-identical assets | Source byte verification |
| Python XML parse of changed layouts/resources/manifest/FileProvider | 7/7 parsed OK | Resource syntax |
| `git diff --check` | No output (no whitespace errors) | Static |
| `gradle :app:compileDebugKotlin` (Gradle 8.7, JDK 21, Android SDK 36, AGP 8.5.2) | **PASS** | Android Kotlin compile |
| `gradle :app:compileDebugUnitTestKotlin` | **PASS** | Android test-source compile |
| Targeted pure JVM tests: `AvatarCropGeometryTest`, `ProfilePhotoTransformSourceTest`, `ProfilePhotoScopeTest`, `ProfileActionBounceSourceTest` | **PASS; 11 tests, 0 failures** | Android JVM unit tests |
| Full `:app:testDebugUnitTest` | 1331 tests run, 487 failed; output is dominated by `Robolectric native runtime is not supported on Linux (aarch64)`. Not a code verdict. | BLOCKED environment |
| `:app:assembleDebug` in `/tmp/eve-apk-check` with `externalNativeBuild` removed | **PASS; 18 MB debug APK** | APK packaging/resources/manifest/dex |
| Production `:app:assembleDebug` with native CMake | **BLOCKED**: NDK/CMake host tools are x86_64 and cannot execute natively on this aarch64 host; Kotlin/resources already compile. | BLOCKED environment |
| CI exact-SHA run | **Not run** | BLOCKED (uncommitted tree) |
| Physical/emulator runtime | **Not run** | BLOCKED |

**Environment:** the session installed JDK 21, Gradle 8.7, Android SDK 36,
build-tools 34/36 and NDK 27 in `/tmp`; aapt2 is run through `qemu-x86_64`
with an x86-64 loader. `compileDebugKotlin`, test compilation, the targeted JVM
tests and native-disabled APK packaging therefore have real evidence. The
production native/CMake build, Robolectric sandbox tests, instrumentation,
emulator/device checks and exact-SHA CI remain **BLOCKED**. No commit or push
was authorized.

## Files changed

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/eve/app/ui/profile/ProfilePhotoMenu.kt` | New source-composed photo action sheet |
| `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt` | Menu wiring, camera capture, crop launch/result, temp-file lifecycle, removal |
| `app/src/main/java/com/eve/app/ui/profile/AvatarCropActivity.kt` | New source crop host, decode/export/result |
| `app/src/main/java/com/eve/app/ui/profile/AvatarCropView.kt` | New source square crop surface/gestures/drawing |
| `app/src/main/java/com/eve/app/ui/profile/AvatarCropGeometry.kt` | New pure crop fit/move/resize/source-mapping geometry |
| `app/src/main/java/com/eve/app/ui/profile/ChangeBioActivity.kt` | Source bio behavior and rejection feedback |
| `app/src/main/java/com/eve/app/ui/profile/CodepointsLengthInputFilter.kt` | New direct codepoint-filter port |
| `app/src/main/java/com/eve/app/ui/profile/BioLengthInputFilter.kt` | New codepoint + Worker-unit contract filter |
| `app/src/main/java/com/eve/app/ui/profile/ChangeUsernameActivity.kt` | IME/close/guard/shake/APP_ERROR behavior |
| `app/src/main/java/com/eve/app/ui/profile/ChangeNameActivity.kt` | IME/close/guard/validation behavior |
| `app/src/main/java/com/eve/app/ui/profile/ChangePhoneActivity.kt` | IME/close/guard/shake/APP_ERROR phone editor behavior |
| `app/src/main/java/com/eve/app/util/TelegramShake.kt` | New `shakeView`/`shakeViewSpring` port |
| `app/src/main/java/com/eve/app/util/ExifTransform.kt` | New EXIF orientation/rotation port |
| `app/src/main/java/com/eve/app/util/ProfilePhotoManager.kt` | EXIF-normalized 800×800/q80 avatar export, crop decode and crop adoption |
| `app/src/main/java/com/eve/app/util/VibrationHelper.kt` | APP_ERROR and one-shot vibration |
| `app/src/main/res/layout/activity_change_bio.xml` | Source max-lines and counter defaults |
| `app/src/main/res/layout/activity_avatar_crop.xml` | New dark source crop toolbar and crop surface host |
| `app/src/main/res/values/strings.xml`, `values-hi/strings.xml` | Photo/camera/removal/save and crop labels |
| `app/src/main/AndroidManifest.xml` | `AvatarCropActivity` registration |
| `app/src/main/res/values/telegram_colors.xml` | Source destructive red roles |
| `app/src/main/res/drawable-*/msg_photos.webp`, `drawable-xxhdpi/ic_delete.png` | Byte-identical source assets |
| `app/src/test/java/com/eve/app/ui/profile/AvatarCropGeometryTest.kt` | New 7-test pure crop geometry/source-contract regression |
| `app/src/test/java/com/eve/app/ui/profile/ProfilePhotoTransformSourceTest.kt` | New pure EXIF/sample/atomic-save contract |
| `app/src/test/java/com/eve/app/ui/profile/ProfilePhotoInputContractTest.kt` | New menu/camera/filter/EXIF Robolectric regression tests |
| `app/src/test/java/com/eve/app/ui/profile/ProfileActionBounceSourceTest.kt` | Updated Set Photo menu assertion |
| `docs/telegram-verbatim-imports.json` | 16 pinned asset hashes |
| `docs/telegram-integration-process.md` | Asset-inventory wording |
| `THIRD_PARTY_NOTICES.md` | New source/license attribution |
| `docs/TELEGRAM_*.md` | Required discovery/source-map/audit/log deliverables |

## Unresolved / not claimed complete

1. **Production native build and full Android test suite are BLOCKED.** Kotlin
   and unit-test sources compile; the x86_64 NDK/CMake host tools cannot execute
   on this aarch64 host, and Robolectric's native runtime does not support
   aarch64 Linux. Targeted pure-JVM tests pass.
2. **CI and device verification are BLOCKED.** No commit/push; no hardware.
3. **Telegram rotate/freeform crop, video avatars and the PhotoViewer
   avatar-constructor transition are not ported.** The fixed square avatar crop
   and 800×800/q80 export are implemented (ADAPTED).
4. **Firebase/Google fallback avatar removal is MISSING.** EVE can delete only
   its own local photo; a Google provider photo can reappear after removal.
5. **Username clearing is BLOCKED** by the existing Worker 5–32-character
   contract; the UI explains the rejection.
6. **Full `BottomSheet` drag/IME physics is not ported.** EVE uses a Material
   host with source title/cell geometry.
7. **Video avatars and web image search are excluded** because EVE has no
   matching product scope.
8. **Pixel/motion/RTL/large-font/cutout parity is unverified.**
9. **GPL/combined-work release clearance remains OPEN.** No production
   distribution approval is implied.

## Git state

Working tree only; base `ab1ec2cefb1883575cbdeba1bd022edf3332cd79`.
No commits, pushes, releases, production deployments, or schema migrations were
performed. The original Telegram ZIP remains unmodified.

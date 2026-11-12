# Telegram source → EVE implementation map

**Recorded:** 2026-11-12
**Telegram source:** `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`,
version 13.1.1/7237, extracted at `/tmp/eve-telegram-source/Telegram-master`
from `/sdcard/Download/Telegrammaster.zip` (SHA-256
`4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`).
**EVE base:** `ab1ec2cefb1883575cbdeba1bd022edf3332cd79` on `main`, plus the
uncommitted working-tree changes listed in
`docs/TELEGRAM_EVE_IMPLEMENTATION_LOG.md`.

Classification uses only **EXACT**, **ADAPTED**, **MISSING** and **BLOCKED**.
This map records code-level mapping. It is not pixel, motion, device, or
runtime evidence; those remain unavailable in this environment.

## Mapping table

| # | Component | Telegram source (path:lines) | EVE implementation | Classification / rationale |
| --- | --- | --- | --- | --- |
| 1 | Own-profile shell, toolbar, rows | `ui/ProfileActivity.java:3909-4004,5354-5359,6039-6047` | `ui/profile/ProfileActivity.kt`, `res/layout/activity_profile.xml`, `ui/profile/TelegramProfileSection.kt` | **ADAPTED**: EVE navigation, Firebase account, backend fields and Material views replace Telegram Fragment/account/RecyclerListView. Row typography/geometry and grouped sections are source-derived. |
| 2 | Avatar/header Gooey rendering | `ui/Components/ProfileGooeyView.java`; `messenger/NotchInfoUtils.java`; `ui/Components/ProfileGalleryView.java` (inspected) | `ProfileGooeyView.kt`, `NotchInfoUtils.kt`, `ProfileAvatarTrajectory.kt`, `ProfileStackBlur.kt`, `ProfileHeaderExpansion.kt` | **ADAPTED**: RenderEffect path, CPU/stack-blur fallback, cutout/notch math and full-window host are EVE-owned. Telegram gallery/pager/media services are excluded. |
| 3 | Pull-to-expand, scroll state, settle | `ui/ProfileActivity.java:4190-4237,4320-4340,8465-8520`; `ui/Components/CubicBezierInterpolator.java:13-15` | `ProfilePullScrollView.kt`, `ProfileScrollState.kt`, `ProfileHeaderExpansion.kt` | **ADAPTED**: half resistance, 0.33 latch, distance/velocity-scaled 250 ms settle and `.42,0,.58,1` curve retained; NestedScrollView input remains the EVE parent. |
| 4 | Action canvas, collapse, loading | `ui/Components/ProfileActionsView.java:247-337,412-460,512-587,1247-1328` | `ProfileActionsCanvasLayout.kt`, `ProfileActionsCollapse.kt`, `ProfileActionIcons.kt` | **ADAPTED**: native action containers retain 74 dp row / 54 dp control and source alpha/loading stages; the complete Canvas/shader parent renderer is not transplanted. |
| 5 | Action press/release physics | `ui/Components/ButtonBounce.java:60-94,119-126` | `ProfileActionBounce.kt` | **ADAPTED**: 60 ms press, 350 ms tension-5 release, 4 % scale, 20 px drift cancel and 250 ms haptic are retained in an EVE touch listener. See `docs/EVE_PROFILE_ACTION_PRESS_PROVENANCE.md`. |
| 6 | Action accessibility | `ui/Components/ProfileActionsView.java` accessibility nodes (source-inspected) | `ProfileActionAccessibility.kt` | **ADAPTED**: EVE exposes button roles/descriptions and keeps 48 dp targets; source Canvas virtual nodes are not reproduced. |
| 7 | Overflow menu | `ui/ProfileActivity.java:4009`; `ui/ActionBar/ActionBar.java`; `ui/ActionBar/BaseFragment.java` | `ProfileOverflowPopup.kt`, `ProfileActivity.kt` | **ADAPTED**: owner-only Refresh, Edit info, Settings, Copy username, Close; one popup, lifecycle dismiss, EVE native host. |
| 8 | Photo action menu | `ui/Components/ImageUpdater.java:253-332`; `ui/ActionBar/BottomSheet.java:1040-1145,1400-1432` | `ui/profile/ProfilePhotoMenu.kt` | **ADAPTED**: source title/cell order and geometry retained in a themed Material BottomSheetDialog. Telegram `BottomSheet` window, drag physics and MP4/video/search items are not ported. |
| 9 | Camera capture | `ui/Components/ImageUpdater.java:650-718`; `messenger/AndroidUtilities.java:6269-6320` | `ProfileActivity.kt` (`requestCameraCapture`, `launchCameraCapture`, `takePhotoLauncher`, `cameraPermissionLauncher`); `util/ExifTransform.kt` | **ADAPTED**: CAMERA permission, `ACTION_IMAGE_CAPTURE` via `ActivityResultContracts.TakePicture`, FileProvider cache URI and preview reuse. Saved captures are EXIF-orientation-normalized with the source rotation/invert mapping and enter the source square crop (row 21). |
| 10 | Gallery selection | `ui/Components/ImageUpdater.java:719-760,817-830`; `messenger/AndroidUtilities.java:6269-6320` | `ProfileActivity.kt` (`pickPhotoLauncher`); `util/ExifTransform.kt` | **ADAPTED**: system `GetContent("image/*")` replaces Telegram `PhotoAlbumPickerActivity`; the pick enters the source square crop (row 21) and then EVE's authenticated local preview/save pipeline with EXIF orientation applied. |
| 11 | Remove photo | `ui/Components/ImageUpdater.java:297-321`; `ui/ProfileActivity.java:6993-7000` (caller inspected) | `ProfileActivity.removeProfilePhoto`, `ProfilePhotoManager.removePhoto` | **ADAPTED**: destructive menu item only with an EVE-managed local photo; removal is verified by file existence. Firebase/Google fallback avatars are not deleted. |
| 12 | Display-name editor | `ui/ChangeNameActivity.java:45-230` | `ui/profile/ChangeNameActivity.kt`, `res/layout/activity_change_name.xml` | **ADAPTED**: IME Done, focus/keyboard, and unchanged-name no-write are retained. EVE uses one 1–100-character display name instead of Telegram first/last fields. |
| 13 | Username editor | `ui/ChangeUsernameActivity.java:790-800,1196-1300,1360-1466` | `ui/profile/ChangeUsernameActivity.kt`, `res/layout/activity_change_username.xml` | **ADAPTED**: IME Done, unchanged-handle close, source character rules, 300 ms check debounce, readback confirmation, `shakeIfOff` spring/APP_ERROR. EVE keeps its 5–32-character Worker contract. |
| 14 | Bio editor | `ui/ChangeBioActivity.java:88-180,200-252`; `ui/Components/CodepointsLengthInputFilter.java:7-30` | `ui/profile/ChangeBioActivity.kt`, `CodepointsLengthInputFilter.kt`, `BioLengthInputFilter.kt`, `res/layout/activity_change_bio.xml` | **ADAPTED**: 18 sp input, 36 dp minimum, four-line maximum, codepoint counting, IME Done, trailing-newline Done, newline stripping, unchanged-bio close, source rejection shake/200 ms vibration. EVE's 160-code-point / 160-UTF-16-unit Worker limit replaces Telegram's 70/140 service limits and disables the field during the authenticated save. |
| 15 | Rejection feedback | `messenger/AndroidUtilities.java:3489-3554`; `messenger/BotWebViewVibrationEffect.java:18` | `util/TelegramShake.kt`, `util/VibrationHelper.kt` | **ADAPTED**: source 300 ms/4 dp damped-sine `shakeView`, 600-stiffness `shakeViewSpring`, and APP_ERROR waveform/fallback retained; EVE uses a weak registry instead of Telegram view tags. |
| 16 | Personal QR scene/picker | `ui/QrShare/QrShareActivity.java:466,819-845`; `ui/QrShare/QrShareView.java:189-212,972-992,2085-2121,2140-2175,2260-2426,3171-3407,3573-3806,4533-4583,6637-6768`; `ui/QrShare/Backdrops.java:8-22`; `ui/QrShare/QrCodeLayer.java:139-154` | `UsernameCardActivity.kt`, `TelegramQrSceneView.kt`, `TelegramQrMotion.kt`, `QrEmojiPattern.kt`, `QrPickerMorphDialog.kt`, `QrPickerOpenMotion.kt`, `TelegramQRCodeWriter.kt`, `ProfileQrPayload.kt` | **ADAPTED**: source timings, palettes, emoji lattice and morph math are used in an EVE Activity with a username-only `eve-mock-test://username/...` payload. Telegram accounts, themes, gifts, paid messages, media documents and network are excluded. |
| 17 | QR scanner/presentation | `ui/QrShare/QrScanOverlay.java:99-112,168-195,328-350,370-479,517-546,561-623,689-701,770-802,867-920,994-1063`; `messenger/camera/CameraView.java:1012-1035,1096-1106` | `EveQrLiveScannerActivity.kt`, `QrScanMorphLayout.kt`, `QrScannerFrameOverlay.kt`, `telegram/qr/*.java`, `ui/profile/QrCameraSessionGate.kt` | **ADAPTED**: CameraX/ZXing EVE scanner and EVE-only routing use source overlay geometry/motion. Telegram CameraController/session/MTProto is not imported. See `docs/QR_SCANNER_CAMERA_GLASS_SOURCE_MAP.md`. |
| 18 | Field rows: phone, birthday, category | `ui/ProfileActivity.java:13194,13517-13636`; `ui/Cells/TextDetailCell.java:68-145`; `ui/Cells/TextCell.java` | `ProfileActivity.kt`, `ChangePhoneActivity.kt`, `ProfileBirthday.kt`, existing category selector | **ADAPTED**: 16 sp value / 13 sp label roles and source row structure; EVE validation, backend fields and category business rules are retained. |
| 19 | Theme roles and selectors | `ui/ActionBar/ThemeColors.java:79,96-97,133,199,205`; `assets/night.attheme` | `TelegramThemeEngine.kt`, `TelegramThemeSelectors.kt`, `values/telegram_colors.xml`, `values-night/telegram_colors.xml` | **ADAPTED**: Day/Night roles and selector math retained; EVE contrast overrides are documented separately. |
| 20 | Source raster assets | `res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/msg_photos.webp`, `msg_camera.webp`, `msg_delete.webp`, `drawable-xxhdpi/msg_delete.png` | `msg_photos.webp`, `ic_camera.webp`, `ic_delete.webp`, `ic_delete.png` in matching density folders | **EXACT bytes** where the pinned resource exists. All 16 pairs and SHA-256 values are in `docs/telegram-verbatim-imports.json`. |
| 21 | Square avatar crop surface | `ui/PhotoCropActivity.java:40-360` (`PhotoCropView`, `updateBitmapSize`, drag/resize, scrim/border/handles/grid) | `ui/profile/AvatarCropActivity.kt`, `AvatarCropView.kt`, `AvatarCropGeometry.kt`, `res/layout/activity_avatar_crop.xml` | **ADAPTED**: fixed square selection, 14 dp image inset, centered initial square, move, four corner resizes, 80 % scrim, 2 dp border, 20 px-class corner handles, rule-of-thirds grid, source dark/white colours. EVE maps x/y source coordinates independently (fixing the upstream width/width divisor) and hosts the surface in an Activity; rotate/freeform gesture modes are not ported. |
| 22 | Crop result export and adoption | `ui/Components/ImageUpdater.java:592-646,845-930` | `AvatarCropActivity.completeCrop`/`writeCrop`; `ProfilePhotoManager.decodeForCrop`/`saveCroppedPhoto` | **ADAPTED**: selected image is decoded with EXIF orientation and bounded to 2048 px, cropped square, exported as an up-to-800×800 quality-80 cache JPEG via `.tmp`+rename, then adopted by one atomic copy. Non-square/oversized output falls back to `savePhoto`. Telegram's 150×150 cloud small photo and avatar-constructor upload are not reproduced. |

## Dependencies and resource mapping

- **Strings:** `profile_choose_photo`, `profile_take_photo`,
  `profile_upload_from_gallery`, `profile_remove_photo`,
  `profile_camera_permission_denied`, `profile_camera_unavailable`,
  `profile_photo_removed`, `profile_photo_remove_failed` are present in
  `values/strings.xml` and `values-hi/strings.xml`. Source labels
  (`ChoosePhoto`, `ChooseTakePhoto`, `ChooseFromGallery`, `DeletePhoto`) are
  preserved in English.
- **Colors:** `telegram_text_red_regular` (#FFCC2929) and
  `telegram_text_red_bold` (#FFCC4747) match
  `ThemeColors.java:96-97` defaults and are used for the destructive icon/text.
- **FileProvider:** `${applicationId}.fileprovider` with `res/xml/file_paths.xml`
  exposing `cache-path` is already declared in the manifest. Camera temp files
  are created in `cacheDir` and deleted on cancellation, failure, or
  non-configuration destruction.
- **Permissions:** `android.permission.CAMERA` and
  `uses-feature android.hardware.camera.any (required=false)` are already
  declared. The gallery path uses the Storage Access Framework and needs no
  storage permission.
- **Material host:** `Theme.Eve.BottomSheetDialog`, `DesignSurfaces.dialog/sheet`,
  and `TelegramBottomSheetSourceBackdrop` provide EVE's themed surface and the
  already source-derived Telegram scrim. These are host adaptations, not new
  Telegram dependencies.
- **Animation:** `androidx.dynamicanimation` supplies `SpringAnimation`; the
  dependency already exists and no Telegram physics library is imported.

## Source helper ports

| Source helper | EVE port | Notes |
| --- | --- | --- |
| `CodepointsLengthInputFilter.java:7-30` | `ui/profile/CodepointsLengthInputFilter.kt` | Pure codepoint limit; used as the source unit of the combined EVE filter and tested directly. |
| `AndroidUtilities.shakeView` / `shakeViewSpring` (`AndroidUtilities.java:3489-3554`) | `util/TelegramShake.kt` | Same 300 ms damped-sine and 600-stiffness spring constants; weak registry replaces source `R.id` tags. |
| `BotWebViewVibrationEffect.APP_ERROR` (`BotWebViewVibrationEffect.java:18`) | `VibrationHelper.vibrateAppError` | Same timings/amplitudes and fallback; no Telegram WebView dependency. |
| `AndroidUtilities.getImageOrientation` (`AndroidUtilities.java:6269-6320`) and `PhotoViewer.applyCurrentEditMode` (`PhotoViewer.java:11346-11370`) | `util/ExifTransform.kt` | Eight EXIF values map to source `(rotation, invert)`; save applies rotate then mirror before crop. |
| `ImageUpdater.processBitmap` 800×800/quality-80 export (`ImageUpdater.java:845-855`) | `ProfilePhotoManager.savePhoto` | One local up-to-800×800 JPEG instead of Telegram's big/small cloud variants; sampled decode bounds the shorter side at 800 px and a completed temp file is renamed into place. |
| `PhotoCropActivity.PhotoCropView` fit/move/resize/getBitmap (`PhotoCropActivity.java:40-360`) | `ui/profile/AvatarCropGeometry.kt`, `AvatarCropView.kt` | Pure floating-point square-crop geometry with independently mapped x/y source coordinates; EVE host view draws the source scrim/handles/grid. |
| `ImageUpdater.startCrop` → `processBitmap` result handoff (`ImageUpdater.java:592-646`) | `AvatarCropActivity` result; `ProfilePhotoManager.saveCroppedPhoto` | Activity result returns a cache URI/path; the square ≤800 px JPEG is adopted atomically without re-encoding. |
| `ImageUpdater.openMenu` composition | `ProfilePhotoMenuModel` + `ProfilePhotoMenu.show` | Source list order and dimensions, EVE host. |
| `BottomSheet` title/cell dimensions | `ProfilePhotoMenu` title/row construction | 48 dp height, 21 dp title inset, 56 dp icon column, 72 dp text start, 16 sp/20 sp roles. |

## Excluded Telegram-only dependencies

The following source branches exist upstream but are intentionally not ported
because they require Telegram identity, media, network, or unrelated product
features: MTProto/account login, `MessagesController`/`ConnectionsManager`,
`PhotoAlbumPickerActivity` internals, `ImageUpdater` avatar video recording,
the crop rotate/freeform controls and `PhotoViewer` avatar-constructor
transition, Telegram web image search, custom emoji documents,
gift/paid-message themes, Telegram camera controller/session, and Telegram
telemetry. Their absence is a scope decision, not a claim of EVE parity for
Telegram features EVE does not have.

## Data and security boundaries

- EVE Firebase Auth remains the sole identity authority.
- Every profile read/write is tagged with `ProfileRequestOwner(ownerUid)` and
  checked again before and after the request; user A cannot read or write user
  B's profile.
- Photo files use the existing UID-scoped `ProfilePhotoManager` name and never
  leave the device. No Firebase Storage or Telegram media service is added.
- The Worker profile endpoint remains the only write path:
  `PUT /api/users/profile` with its fixed field allowlist and
  `validateUsername` 5–32-character contract (`backend/src/routes/auth.ts`).
- The QR payload contains only a normalized username and never a UID, token,
  phone, DOB, or private profile data.

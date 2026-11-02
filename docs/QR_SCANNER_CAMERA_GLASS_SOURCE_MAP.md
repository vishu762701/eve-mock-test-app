# QR scanner camera glass integration

Status: ADAPTED, complete scanner/QR fidelity NOT MET. Same mission, baseline
58b415857bce257de4ca966a7174ff74b9ae910d. Latest official HTTPS Git HEAD was
refetched as 3de37c8cfb039452be7398909461da6288babf15 (tree
cc3c6ccf48e4167fdec764dfd04e765043f55093). telegram.org/apps links this official
DrKLO/Telegram origin. No source revisions are mixed. Actual opened source:
QrScanOverlay.java:95-170,370-425,490-570,579-700,780-950;
CameraView.java:970-1045,1080-1110; QrRender.java:1-110; root LICENSE.
Full upstream-file blob IDs and SHA-256 are in qr-native-upstream-manifest.json.

## Source-to-production mapping

Paths in the first column are under TMessagesProj/src/main/java/org/telegram;
all use the full pinned SHA above.

| Upstream file:lines@SHA | EVE file:line | Classification / reason | Evidence / uncertainty |
|---|---|---|---|
| messenger/camera/CameraView.java:1012-1035,1096-1106@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrCameraBackdrop.java:24 | ADAPTED original Java camera-child RenderNode recording, second node with 32dp DECAL blur; EVE uses existing QrRender API guards and explicit disposal | Java/Android production integration; hardware CameraX TextureView playback remains unverified |
| ui/QrShare/QrScanOverlay.java:804-879@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/telegram/qr/QrScannerGlass.java:22 | ADAPTED source white fill, sheen, rim and 0x29000000/0x80000000 fallback; isolated Drawable and owner-to-camera screen coordinates replace monolithic Canvas | actual source fill/clip/sampling host pixel tests; physical reference comparison absent |
| ui/QrShare/QrScanOverlay.java:517-546@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/QrScanMorphLayout.kt:130 | ADAPTED existing bounded half-size snapshot gets a separate 1/8 copy, radius-6 existing source-derived ProfileStackBlur; bitmap shader maintained by Java owner | ownership replacement/dispose/coordinate tests; platform closing playback pending |
| ui/QrShare/QrScanOverlay.java:549-552,859-879@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/QrScanMorphLayout.kt:44 | ADAPTED API31/hardware/settled-open/camera-stream gate; Android PowerManager replaces Telegram LiteMode app settings | earlier APIs/software/power-save use source darker fallback; device power-save changes unverified |
| ui/QrShare/QrScanOverlay.java:804-879@3de37c8cfb039452be7398909461da6288babf15 | app/src/main/java/com/eve/app/ui/profile/EveQrLiveScannerActivity.kt:144 | ADAPTED actual scanner preview stream observes CameraX, all existing control backgrounds use source glass host | contract and compilation checks; EVE text controls remain a separate source-geometry gap |

Minimum dependency edges: QrScanOverlay controls -> CameraView child recording ->
RenderNode/RenderEffect -> QrGlass/RimPainter/RimTune. Existing QrRender keeps all
newer framework classes behind API checks. CameraX PreviewView COMPATIBLE supplies
the EVE TextureView hierarchy; it is drawn once into the camera node, then to the
window and the blur node. Controls, frame and captions are siblings and cannot be
captured recursively. There is no live bitmap readback, GPU-to-CPU copy, analysis
image upload, disk persistence or network dependency. Only closing uses the
existing bounded preview snapshot. Nested View offsets/insets use actual screen
locations, not guessed crop ratios. Detach/dispose discards both display lists,
clears shaders and recycles owned snapshot buffers; stopping the stream discards
live nodes. Optional snapshot-blur allocation failures preserve closing and source
flat glass. The original foreground snapshot and four-point scanner remain intact.

Excluded: Telegram CameraController/CameraSession/GL camera business, LiteMode
account settings, Telegram link acceptance, accounts, MTProto, documents, private
keys/network services. EVE retains CameraX lifecycle, local ZXing recognition,
permission/Settings/Gallery recovery, async torch state, allowlisted username-only
routes and duplicate-result guard. No new permission or backend/API change.

The source root license is GPLv2. No absent per-file later-version grant is invented.
Telegram contributors retain rights; combined-app redistribution and corresponding
source/license compatibility need human review. EVE_GPL_RELEASE_APPROVED remains
untouched and fail-closed. Debug verification is not distribution clearance.

## Verification scope

QrCameraBackdropTest exercises actual closing-image sampling using nested View
locations, owned bitmap replacement/double-dispose, software fallback and real
source-glass pixels/clipping. EveQrScannerContractTest traces the production caller.
QrCameraGlassNativeTest is a real hardware Canvas/PixelCopy test of this production
host with synthetic PreviewView-child pixels (no private camera/image/network).
Compilation is not execution; it does not prove the actual CameraX TextureView,
orientation/crop, original Telegram pixels, timing or 60/90/120Hz behavior.

Exact-SHA CI and stable local command/results must be recorded in the mission
checkpoint after execution. The current entire scanner remains NOT MET: original
control layout/touch physics, permission/camera/gallery/torch/pause/rotation matrix,
actual live TextureView blur and matched reference/native motion remain open.
No premature final FULL or published APK is claimed.

## Upstream checkout limitations

The superproject source tree and all official gitlink identities were fetched.
Recursive submodule initialization was attempted, but optional TDLib/media/codec
clones plus Android toolchain/dependencies exceed this executor's 8 GiB filesystem.
Unused partial clones were discarded to preserve build space. The actually imported
tlottie dependency is checked out at exact official gitlink
31f1b542f88e7b4be9a01e749920d857535fc715, including its MIT license. Other submodules
are explicitly uninitialized, not falsely verified or imported. This does not alter
EVE or official source files and is not a complete recursive-source acceptance.

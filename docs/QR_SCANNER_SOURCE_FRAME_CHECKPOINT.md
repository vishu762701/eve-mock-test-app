# QR scanner source-frame checkpoint

R1 (QR full Telegram fidelity): NOT MET. This batch integrates selected source geometry and camera ordering only.
R2 (admin, Profile, Home, Quiz): pending in the same mission; no changes in this batch.

Upstream inspected: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`,
`TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrScanOverlay.java:95-120,430-510,579-599,659-708`.

| Source property | EVE path | Classification |
|---|---|---|
| Frame 66% of min dimensions, raised 20dp | `app/src/main/java/com/eve/app/ui/profile/QrScannerFrameOverlay.kt` | ADAPTED |
| 25% dim, 14dp radius, 4dp strokes, 28dp corner arms | same | ADAPTED |
| Begin camera only after 390ms opening | `app/src/main/java/com/eve/app/ui/profile/EveQrLiveScannerActivity.kt` | ADAPTED |
| Release CameraX/torch before scanner dismissal | same | ADAPTED |
| Source-derived sheen/rim for close, gallery and torch controls | `EveQrLiveScannerActivity.kt` using existing Java `QrControlBackground` → `QrGlass` | ADAPTED (no live blur) |
| Real Telegram button-to-screen morph/snapshot blur | not imported | NOT MET |
| On-device frame, RTL, cutout, permissions, camera behavior | native evidence not yet collected | UNKNOWN |

EVE retains CameraX/ZXing, username-only QR links, non-exported scanner Activity.
No upstream Java implementation is claimed byte-exact in this batch. Existing Telegram license obligations remain OPEN.
Verification: `QrScannerFrameGeometryTest.kt`, existing QR contract test, exact-SHA Android FAST CI.

## Source Java button-to-full-screen morph and bounded close snapshot (2026-11-02)

**ADAPTED; full scanner source parity NOT MET.** Official
`DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`
`QrScanOverlay.java:99-112,168-195,370-479,517-546,561-568,601-623`
was opened. Upstream source stretch (390ms, Bezier 0.05,0.7,0.1,1),
shrink (330ms, Bezier 0.3,0,0.1,1), screen-space button origin,
rounded rectangle and layer scaling were extracted in original Java
language into `QrScanMorph.java`.
`QrScanMorphLayout.kt` is the EVE Android View/Canvas host for that
source shape and source content/black fade. The production
`UsernameCardActivity.scanButton()` passes the centered square
origin; `EveQrLiveScannerActivity` reuses the QR scene's translucent
theme, starts CameraX only after opening, and carries the last preview
frame as a bounded in-memory close-only bitmap before camera unbind.
Gallery and ZXing, QR username-only payloads, non-exported camera
permission path, rotation restore and duplicate scan guard stay
EVE-authoritative. The isolated `QrScanMorphTest` and
`EveQrScannerContractTest` cover numeric source geometry and the
real integration path. **NOT MET**: exact Telegram camera rendering,
scan-found yellow outline, action-glass blur, gallery transition,
full permission/device rotation matrix and matched GPU frame parity.
Camera image is never transmitted or persisted by this morph.
Production GPL distribution review remains OPEN.

## Scanner camera permission and settings-return recovery (2026-11-02)

**ADAPTED EVE-owned permission handling**, not an original Telegram
permission service. The source-backed scanner button morph and EVE
CameraX/ZXing decoding are unchanged. Following a denial, the Activity
uses Android shouldShowRequestPermissionRationale to offer a manual
permission retry or a user-initiated Android App Settings route.
The existing TelegramNativeAlertDialog supplies EVE-owned presentation;
Gallery remains available without camera access. On return from Android
Settings, an actual granted permission resumes CameraX with a
single-in-flight binding guard; the permission dialog is discarded on
Activity destruction. This never auto-grants, bypasses or hides a
permission. Source contract tests cover actual integration, while
real hardware denial, settings-return and rotation remain UNKNOWN.

## R5 CameraX torch result ownership (2026-11-02)

ADAPTED original session-aware flashlight behavior from
DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15,
QrScanOverlay.java:783-787,914-922. EVE retains its CameraX owner.
The real scanner button now calls CameraX enableTorch and updates
selected/accessibility status only upon successful future completion.
QrTorchGate.kt prevents rapid duplicate requests, retains state
when the future fails and ignores callbacks from previous camera
generations (close/rotate/rebind). QrTorchGateTest.kt covers success,
failure and old-generation rejection. Real camera hardware and
60/90/120Hz/source glass parity remain UNKNOWN/NOT MET; license
and release review remain OPEN.


## R5 authentic scan-found outline with CameraX display mapping (2026-11-02)

The official pinned Telegram Java QrScanOverlay.java at
DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15 was
inspected at 108-112, 346-355, 700-782, 994-1011 and 1040-1063.
The source uses four genuine QR points (no synthetic finder corners),
color #FFFFDE07, 6dp round-capped strokes, 18% neighbor arms,
a 0.5-to-1.1 centroid zoom, approach factor 0.3, fades white frame
corners and defers completion by 450ms only when a polygon is available.

| Original source | EVE production path | Status |
|---|---|---|
| QrScanOverlay.java:108-112,346-355,700-782 | app/src/main/java/com/eve/app/telegram/qr/QrScanFound.java; QrScannerFrameOverlay.kt | ADAPTED Java source geometry, original color/stroke/animator math, source-only four-point policy |
| QrScanOverlay.java:994-1011,1040-1063 | EveQrLiveScannerActivity.decode/handleLink | ADAPTED actual ZXing four-point extraction and source 450ms delay, EVE username-only link security |
| EVE CameraX frame/preview contract | EveQrLiveScannerActivity.analyzeFrame/startLiveCamera | ADAPTED explicit CameraX shared ViewPort, ImageProxyTransformFactory with raw unrotated coordinates, CoordinateTransform to PreviewView, skip overlay if reliable transform unavailable |

CameraX 1.4.2's own shared ViewPort and transform APIs provide
crop/rotation/FILL_CENTER mapping instead of hard-coded screen ratios.
The original Java source language remains Java; Android View integration
remains Kotlin. QR image decode uses real ZXing four result points
only when available; three finder points never fabricate a rectangle
or add the 450ms delay. The original EVE CameraX/ZXing, Gallery,
permission, torch, duplicate-result guard and safe EVE deep-link
routing remain intact.

New QrScanFoundTest.java validates source pixels/rotation/polygon policy;
QrScannerFoundOverlayTest.kt validates production Canvas integration;
EveQrScannerContractTest binds CameraX transform and source delay callers.
Actual device rotation, camera preview transforms, yellow outline
pixel/frame timings, hardware and 60/90/120Hz parity remain UNKNOWN.
Whole scanner/QR source parity NOT MET. Combined GPL release review OPEN.

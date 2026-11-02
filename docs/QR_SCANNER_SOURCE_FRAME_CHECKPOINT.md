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

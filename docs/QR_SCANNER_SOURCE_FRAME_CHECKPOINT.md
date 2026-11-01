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
| Real Telegram button-to-screen morph/snapshot blur | not imported | NOT MET |
| On-device frame, RTL, cutout, permissions, camera behavior | native evidence not yet collected | UNKNOWN |

EVE retains CameraX/ZXing, username-only QR links, non-exported scanner Activity.
No upstream Java implementation is claimed byte-exact in this batch. Existing Telegram license obligations remain OPEN.
Verification: `QrScannerFrameGeometryTest.kt`, existing QR contract test, exact-SHA Android FAST CI.

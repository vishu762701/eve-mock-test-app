# Telegram Java QrCodeLayer integration checkpoint

Pinned official source: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`.
Upstream: `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrCodeLayer.java:1-926`.
Git blob SHA-1: `b9b5fcb69f538dfbc7dd87f628fe261ce1e69923`.

| Source element | EVE location | Claim |
|---|---|---|
| Atlas/halftone/particle assemble and draw algorithms | `app/src/main/java/com/eve/app/telegram/qr/QrCodeLayer.java` | ADAPTED package, units, performance-gate, public host API |
| QR drawing and live recolor | `app/src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt` | ADAPTED Android Canvas host |
| Public EVE username-only link and centered E logo | same | EVE-only branding, no Telegram account |
| Static share capture at fully assembled QR layer | same | ADAPTED: separate nonanimating layer |
| Full QrShareView, source emoji sheet/gifts, GL composition, blur and scanner button morph | not imported | NOT MET |
| Matched-device frame physics and GDPR/GPL distribution clearance | no evidence here | UNKNOWN / BLOCKED |

Original code remains Java; no XML rewrite. The Telegram `SharedConfig` performance
classifier cannot be safely imported without Telegram app/account dependencies. The
EVE adapter selects the upstream hardware `QrRender.hasVertices()` predicate only.
Device performance/frames require evidence. Existing Firebase/Worker routes unchanged.
Existing native QR raster decode and end-to-end visual tests must pass before any
claim of screen fidelity. Production GPL gate remains unchanged.

Release/compliance checkpoint: `THIRD_PARTY_NOTICES.md` now records the precise
upstream Git blob, port classification and OPEN combined-distribution gate.
The exact-SHA FAST CI proves compilation/tests, not real-device pixel parity.

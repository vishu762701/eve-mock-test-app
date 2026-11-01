# EVE QR Phase C UI source map

Official reference: DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15.
Archive SHA-256: 4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10.

| Telegram source | EVE implementation | Classification |
|---|---|---|
| QrShareActivity.java:466,819-845 opening/closing durations | ui/profile/TelegramQrMotion.kt, TelegramQrSceneView.kt | ADAPTED: EVE Activity instead of Telegram Fragment |
| QrShareView.java:3631-3665 spring/alpha/translate | TelegramQrMotion.kt, TelegramQrSceneView.kt | ADAPTED: numerical curve retained, Android View host differs |
| QrShareView.java:208-212,2785-2787,6637-6768 gesture drift, rubber, glide | TelegramQrSceneView.kt | ADAPTED: EVE background field |
| QrShareView.java:189-210 theme wave, emoji pattern | TelegramQrSceneView.kt, UsernameCardActivity.kt | ADAPTED: standard device emojis; no Telegram custom media |
| QrCodeLayer.java:139-154 assembly | TelegramQrSceneView.kt | ADAPTED: progressive module appearance, not original particle pool |
| Backdrops.java:8-22 color values | TelegramQrSceneView.kt | ADAPTED: eight local reference palettes, not live Telegram server palette |
| QrShareServer.java:27-47 account themes/paid messages | none | NOT APPLICABLE: EVE username QR, no Telegram identity |
| QrScanOverlay.java:101-102,370-396,429-456 | EveQrLiveScannerActivity.kt, UsernameCardActivity.kt | ADAPTED: on-demand CameraX live scanning, gallery import, EVE-only links; source-timed 390ms/330ms enter/exit |

EVE QR payload stays local to `eve-mock-test://username/...`, never Telegram authentication.
Source-based motion and visual behavior are not proven pixel/frame-identical. Release GPL compatibility remains unresolved per docs/telegram-release-compliance-gate.md.

## Animated QR emoji upgrade (2026-11-01)

- Upstream `QrShareView.java:175,972-992,3171-3233,3239-3269,3369-3407` → `QrEmojiPattern.kt` and `TelegramQrSceneView.kt`: original six-pick toggle, hashed 86dp × 70dp staggered lattice, 20% omissions, independent 9–20s orbit, pop-in (380ms + 300ms spread), breath/rotation. **ADAPTED** to EVE Canvas and login-free identity. No upstream source files copied wholesale.
- Upstream `QrEmojiPanel.java:56-95,122-138` → `UsernameCardActivity.kt`: EVE emoji catalogue is queried through the existing authenticated `EmojiRepository`; an on-device Lottie preview and animated scene use real EVE-hosted compositions. Presets remain Unicode glyphs. **ADAPTED**, not original Telegram custom emoji documents, keyboard, pack layout or RenderNode effects.
- EVE GIFTS, paid messages and persisted multi-emoji account themes remain **NOT IMPLEMENTED**; this stage alters only QR emoji rendering/selection and does not introduce charges. EVE QR source-exact acceptance remains OPEN.

- The EVE QR animated catalogue uses new read-only authenticated `GET /api/emoji/items/public`. Existing `/api/admin/emoji/items` remains strictly admin-only. The public endpoint filters disabled items/packs, respects the Telegram-source kill switch, caps pagination, and returns asset URLs without storage keys. QR uses `EmojiRepository.getPublicQrItems`. No production deployment performed.

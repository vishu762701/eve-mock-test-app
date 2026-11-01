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
| QrScanOverlay.java | UsernameCardActivity.kt | NOT DONE in this UI batch |

EVE QR payload stays local to `eve-mock-test://username/...`, never Telegram authentication.
Source-based motion and visual behavior are not proven pixel/frame-identical. Release GPL compatibility remains unresolved per docs/telegram-release-compliance-gate.md.

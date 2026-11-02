# Scanner controls and title composition

Status: ADAPTED; complete scanner runtime/source acceptance remains OPEN.
Official HTTPS origin https://github.com/DrKLO/Telegram at
3de37c8cfb039452be7398909461da6288babf15, independently re-fetched during this
continuation. Paths below start at TMessagesProj/src/main/. Asset and source
Git identities/SHA-256 are in qr-native-upstream-manifest.json.

| Upstream file:lines at the full SHA above | EVE file:entry | Classification / reason | Test evidence / uncertainty |
|---|---|---|---|
| java/org/telegram/ui/QrShare/QrScanOverlay.java:579-599; java/org/telegram/ui/ActionBar/ActionBar.java:1914-1920 | app/src/main/java/com/eve/app/telegram/qr/QrScannerLayout.java:sample | ADAPTED original Java .66 frame, -20dp center, 56dp circular Gallery/flash, 64dp gap, +/-35dp button edges, 40dp close at x29 and actionbar center. Compact windows fit the complete title/frame/buttons together between measured system bars and close row. | QrScannerControlsTest portrait source geometry and compact group ordering/bounds; physical landscape/cutout matrix OPEN. |
| java/org/telegram/ui/QrShare/QrScanOverlay.java:328-350,659-708,770-802,867-920 | app/src/main/java/com/eve/app/telegram/qr/QrScannerControl.java:onDraw/onTouchEvent | ADAPTED original Java draw, .88 press, .3 approach, .005 convergence, 0x66 confirmed-torch fill, 28dp icons. EVE native accessible Button-class View and >=48dp hit targets replace the single custom overlay dispatcher. Motion policy supplies direct reduced-motion endpoints. | Actual native-graphics control pixels, press recovery, drag-away cancellation, keyboard/performClick, accessible production Gallery contract tests; HWUI device evidence OPEN. |
| java/org/telegram/ui/ActionBar/BackDrawable.java:1-171 | app/src/main/java/com/eve/app/telegram/qr/BackDrawable.java:draw | ADAPTED package/unit imports only. Original Java draw and animation bodies retained. QrUiUnits dp/lerp replace Telegram AndroidUtilities; unused Utilities import excluded. | Source body comparison, Android/JVM compilation and actual control draw. No unrelated Telegram account dependency. |
| java/org/telegram/messenger/AndroidUtilities.java:253-270; assets/fonts/rmedium.ttf; res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/qr_{flashlight,gallery}.webp | app/src/main/res/font/telegram_roboto_medium.ttf; corresponding eve_qr_* resources | EXACT existing font and eight new icon bytes only; Android resource names/loader are EVE adapters. Original bold() default font is rmedium.ttf; Telegram configurable system-font preference is excluded. | Current Git blob/hash matches. Does not imply exact screen or legal distribution acceptance. |
| java/org/telegram/ui/QrShare/QrScanOverlay.java:579-599,679-685 | app/src/main/java/com/eve/app/ui/profile/EveQrLiveScannerActivity.kt:onCreate; QrScanMorphLayout.kt:onLayout; QrScannerFrameOverlay.kt:setSourceFrame/onDraw | ADAPTED Kotlin integration: source 22dp medium title above frame, native EVE username labels/English+Hindi descriptions, source circles, measured safe inset group fitting, source-scaled frame arms. Existing frame/found-code rendering and camera backdrop preserved. | Actual Activity control construction, touch targets and Gallery ACTION_GET_CONTENT; existing permissions/session/found/torch tests retained. Original cached corner bitmap optimization remains a separate gap. |

Minimum dependencies opened: QrShareView.java:5164-5187 supplies close geometry,
QrScanOverlay.java:95-130,328-350,430-520,560-608,650-930 supplies parent animation,
layout, glass and dispatch; original BackDrawable, ActionBar height and default
Roboto medium provider are traced. Existing EVE QrScannerGlass/QrCameraBackdrop
continue source live-camera sampling; radius is bounded by the fitted local visual
circle. Native controls keep source hit expansion where feasible and a minimum
48dp target in compact windows. Source orientation is preserved; native labels
support RTL/TalkBack. The full device RTL/font/navigation matrix is unverified.

Unchanged security/product behavior: CameraX preview and view-port transforms,
CameraSessionGate, pending provider cancellation, permissions/Settings return,
Gallery image bounds, stale-result rejection, real four-point found visualization,
EVE-only allowlist, one navigation, and confirmed torch futures remain in their
existing production paths. No Telegram camera/auth/account/network or new Android
permission is imported. No fake torch success or source screenshot parity claim.

Licensing: Telegram root GPLv2 applies. BackDrawable retains its GPLv2-or-later
header, Copyright Nikolai Kudashov 2013-2018. The existing Roboto license notices
remain; no font bytes changed. All new derived Java/art requires the existing human
combined-app source/distribution review. EVE_GPL_RELEASE_APPROVED stays untouched.

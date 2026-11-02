# QR card, caption and picker composition

Status: ADAPTED; whole QR experience remains OPEN. Official origin independently
re-fetched: https://github.com/DrKLO/Telegram, full commit
3de37c8cfb039452be7398909461da6288babf15. All source paths below start at
TMessagesProj/src/main/. Binary identities and current EVE digests are recorded in
qr-native-upstream-manifest.json; hashes prove identity, not screen acceptance.

| Official source path:lines at the full revision above | EVE production path | Classification / technical reason | Evidence / uncertainty |
|---|---|---|---|
| java/org/telegram/ui/QrShare/QrShareView.java:1879-1881,2046-2061,3808-3819 | app/src/main/java/com/eve/app/telegram/qr/QrCardLayout.java | ADAPTED original Java .745 width, 1.07 username height, .468 rest, source sheet rise/13dp gap and 51dp upper limit. EVE measured controls bound a uniform compact-window fit. Paid/temporary chips excluded. | QrCardLayoutTest exercises portrait equations, interpolation, no downward movement, compact windows and invalid viewport. Physical geometry comparison OPEN. |
| java/org/telegram/ui/QrShare/QrShareView.java:3858-3892,4060-4109 | app/src/main/java/com/eve/app/telegram/qr/QrCardCaption.java | ADAPTED original Java 30/25/19dp fitting, glyph span, two/three-line widths and vertical placement. Locale.ROOT protects ASCII username identity; compact caption scales as a whole inside the source band. | QrCardCaptionTest renders actual font/glyph pixels, all three fitting sizes, full 32-character username and Turkish locale. |
| java/org/telegram/ui/Cells/SettingsSearchCell.java:40-79; java/org/telegram/ui/Components/StaticLayoutEx.java:56-145 | QrCardCaption.java VerticalImageSpan/createLayout | ADAPTED extracted original span metrics/draw and API23+ StaticLayout builder/fallback. EVE minSdk24; validated username cannot contain Telegram animated-emoji spans/URLs. Old-platform, URL, Telegram logging and excess-line truncation branches excluded. RTL uses Android configuration. | Native graphics layout tests retain every username character through the final rendered line. Full device RTL/font matrix OPEN. |
| assets/fonts/rcondensedbold.ttf; res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/qr_at_{large,medium,small}.webp | app/src/main/assets/fonts/eve_qr_condensed_bold.ttf; corresponding res/drawable-*/eve_qr_at_*.webp | EXACT binary bytes only, renamed resources. This does not classify the adapted caption or scene as EXACT. | SHA-256 and Git blob identity recorded for all 13 assets. Font legal disposition remains OPEN. |
| java/org/telegram/ui/QrShare/QrShareView.java:2046-2061,2095-2121,5317-5433 | app/src/main/java/com/eve/app/ui/profile/TelegramQrSceneView.kt; UsernameCardActivity.kt; QrPickerSheetSurface.kt | ADAPTED Kotlin integration adapters. Actual sheet fraction and screen-relative panel top drive card rise. Independent export geometry/ink use resting card; root and child layout changes refresh measured chrome bounds. | Real PNG/ZXing decoding in portrait/landscape and interrupted picker fractions; live bounds/ink invariant. Existing parent Material modal/drag remains a parity gap. |

Minimum subsystem: source QrShareActivity parent/scene, card geometry, background,
shadow, original QR layers, StaticLayoutEx, VerticalImageSpan and density-qualified
glyph/font. Existing Java/C++ backdrop, wave, pattern, reveal, shadow, native emoji,
QR mask/logo and scanner are preserved. EVE controls, validated username, free QR,
content-URI sharing and Gallery services remain EVE-owned adapters. No Telegram
account, username burst, paid link, inventory, authentication or network dependency
is imported. Export retains the existing maximum 1.92 aspect crop and clean scene.

Licenses: Telegram root GPLv2; SettingsSearchCell/StaticLayoutEx carry GPLv2-or-later
notices. Font embeds Copyright 2011 Google Inc. All Rights Reserved, with no license
name IDs13/14. Preserve the binary notice and request specific font disposition
within the existing human combined-app redistribution review. Development tests
do not authorize distribution. EVE_GPL_RELEASE_APPROVED stays untouched/fail closed.

Reduced motion continues through DesignMotion; dispose emits sheet progress zero
and removes its callback. Card fit scales font, QR, rounded corners and shadow
together. Original caption size uses dp rather than user font sp, as upstream;
accessible native controls keep their labels. GPU, TalkBack, large display scale,
rotation, live parent drag and matched Telegram captures still require device
validation. No hardware/reference result is inferred from Robolectric or compilation.

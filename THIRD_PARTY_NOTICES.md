# Third Party Notices

This repository contains third-party components and assets. Keep license obligations even when older files are retired.

## Telegram for Android

- **Upstream:** https://github.com/DrKLO/Telegram
- **License:** GNU General Public License version 2 or later (GPL-2.0-or-later).
- **Retained derived behavior:** `app/src/main/java/com/eve/app/util/ThemeManager.kt` (circular Day/Night reveal and transition layering) and `app/src/main/java/com/eve/app/util/TelegramThemeEasing.kt`.
- **Upstream snapshot:** `dc780e81ed1261c369c27870e8e0999a1eb0b600`; relevant origin: Telegram `LaunchActivity` / `DialogsActivity` transition handling.
- **Credits:** Telegram FZ-LLC / DrKLO and Nikolai Kudashov. The derived theme code is subject to applicable GPL conditions; distributor obligations must be reviewed before publishing a binary.

Old Telegram premium GL renderer classes, models, textures and shaders were deleted during the approved UI design reset. They are **not** current bundled runtime assets; see `docs/design-reset/deleted-files.md` for the removal inventory. Retain these historical attributions for provenance rather than implying those files still ship.

## Inter (Official iOS 26 Typography Approximation)

- Source: https://rsms.me/inter/ and https://github.com/rsms/inter
- Copyright (c) 2016-2026 The Inter Project Authors (Rasmus Andersson).
- License: SIL Open Font License 1.1; full notice distributed in
  `app/src/main/assets/licenses/Inter-OFL.txt`.
- Bundled, unmodified font binaries: Inter Regular 400, Medium 500, SemiBold 600, Bold 700, and InterVariable.

> This Font Software is licensed under the SIL Open Font License, Version 1.1.

## BlurView 2.0.6

- Existing dependency reused; no dependency version added or changed.
- Source: https://github.com/Dimezis/BlurView/tree/version-2.0.6
- License: Apache License 2.0; full notice included in APK assets at
  `app/src/main/assets/licenses/BlurView-LICENSE.txt`.
- `RenderEffectBlur` is used only on API 31+ with a hardware canvas. Other cases
  use the owner's 94% glass tint. The shared chrome keeps foreground children sharp.

## Tabler Icons v3.49.0

Outline SVG paths converted to Android VectorDrawable on their original 24-unit
grid, with the owner's 2.75 stroke and round caps/joins. MIT License, Copyright
(c) 2020–2026 Paweł Kuna. Full license: `app/src/main/assets/licenses/Tabler-LICENSE.txt`.
Source: https://github.com/tabler/tabler-icons/tree/v3.49.0/icons/outline
Resource-to-source mapping: `docs/design-reset/tabler-icons.json`.

## Official Telegram ActionBar and search icon art (Mission 01 source import)

- **Origin:** user-supplied official Telegram Android source archive (`Telegram-master (1).zip`, SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`), `TMessagesProj/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/`.
- **Files copied verbatim:** `ic_ab_back.webp` -> EVE `ic_back.webp`; `ic_ab_other.webp` -> EVE `ic_more_vert.webp`; `msg_search.webp` -> EVE `ic_search.webp`, four original density variants each. Existing EVE drawable resource IDs remain unchanged; prior independently drawn vectors with these same resource IDs were removed after replacement.
- **Copyright and grant:** Telegram Android source contributors, including Telegram FZ-LLC / Nikolai Kudashov as applicable; the upstream project states GPL-2.0-or-later. For these files EVE elects GPL version 3, with the complete GPL-3 text in root `LICENSE` and the upstream GPL-2 text in `app/src/main/assets/licenses/Telegram-GPL2.txt`. Relevant source is publicly available in this repository and the upstream repository.
- **Appearance:** upstream white alpha-mask WebP shapes with the existing Day/Night EVE tint and native touch/accessibility behavior. These particular assets are **static**; no intrinsic Telegram icon motion was found or invented. Not the Telegram brand/logo.
- **Distribution:** publishing an APK with these GPL-covered images requires the complete corresponding source, preservation of notices and a compatible licensing assessment of all included non-free SDKs and other dependencies. Public source publication alone is not a guarantee of compliance. Do not represent release compliance as audited until third-party binary terms and installation-source obligations are checked.


## Seven additional original Telegram static action icons

The source archive's original `msg_camera.webp`, `msg_copy.webp`, `msg_download.webp`, `msg_info.webp`, `msg_link.webp`, `msg_leave.webp`, and `msg_send.webp` have been imported verbatim in four density variants (mdpi, hdpi, xhdpi, xxhdpi), renamed to EVE `ic_camera`, `ic_copy`, `ic_download`, `ic_info`, `ic_link`, `ic_logout`, and `ic_send`. Existing EVE callers, tints, labels and actions remain intact. These are original Telegram GPL-2.0-or-later assets, NOT independently drawn vectors or Telegram logo. The earlier complete GPL texts and source-publication obligations continue to apply. None has a proven intrinsic motion: do not invent transitions. The `TelegramAdditionalSourceIconAssetsTest` pins all 28 binaries to the user-supplied upstream ZIP. Release compatibility with separately licensed SDK binaries remains an audit requirement.

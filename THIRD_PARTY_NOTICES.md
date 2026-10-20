# Third Party Notices

This repository contains third-party components and assets. Keep license obligations even when older files are retired.

## Telegram for Android

- **Upstream:** https://github.com/DrKLO/Telegram
- **License:** GNU General Public License version 2 or later (GPL-2.0-or-later).
- **Retained derived behavior:** `app/src/main/java/com/eve/app/util/ThemeManager.kt` (circular Day/Night reveal and transition layering) and `app/src/main/java/com/eve/app/util/TelegramThemeEasing.kt`.
- **Upstream snapshot:** `dc780e81ed1261c369c27870e8e0999a1eb0b600`; relevant origin: Telegram `LaunchActivity` / `DialogsActivity` transition handling.
- **Credits:** Telegram FZ-LLC / DrKLO and Nikolai Kudashov. The derived theme code is subject to applicable GPL conditions; distributor obligations must be reviewed before publishing a binary.

Old Telegram premium GL renderer classes, models, textures and shaders were deleted during the approved UI design reset. They are **not** current bundled runtime assets; see `docs/design-reset/deleted-files.md` for the removal inventory. Retain these historical attributions for provenance rather than implying those files still ship.

## Poppins

- Source: https://github.com/google/fonts/tree/main/ofl/poppins
- Copyright 2020 The Poppins Project Authors (https://github.com/itfoundry/Poppins).
- License: SIL Open Font License 1.1; full notice distributed in
  `app/src/main/assets/licenses/Poppins-OFL.txt`.
- Bundled, unmodified font binaries: Poppins Regular 400, Medium 500, Bold 700.

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

# Third Party Notices

This project incorporates code and assets from the following open source project:

## Telegram for Android (DrKLO/Telegram)

- **Source**: [https://github.com/DrKLO/Telegram](https://github.com/DrKLO/Telegram)
- **License**: GNU General Public License v2.0 (GPLv2)

### Ported Components & Assets
- **3D Star Model & Textures**:
  - `app/src/main/assets/models/star.binobj`
  - `app/src/main/assets/shaders/vertex2.glsl`
  - `app/src/main/assets/shaders/fragment4.glsl`
  - `app/src/main/assets/flecks.png`
  - Star specular highlight texture adapted from `res/raw/start_texture.svg`
- **OpenGL Rendering & Interaction Logic**:
  - `app/src/main/java/com/eve/app/ui/premium/gl/ObjLoader.kt` (ported from `org.telegram.ui.Components.Premium.GLIcon.ObjLoader`)
  - `app/src/main/java/com/eve/app/ui/premium/gl/Icon3D.kt` (ported from `org.telegram.ui.Components.Premium.GLIcon.Icon3D`)
  - `app/src/main/java/com/eve/app/ui/premium/gl/GLIconRenderer.kt` (ported from `org.telegram.ui.Components.Premium.GLIcon.GLIconRenderer`)
  - `app/src/main/java/com/eve/app/ui/premium/gl/Telegram3DStarView.kt` (ported from `org.telegram.ui.Components.Premium.GLIcon.GLIconTextureView`)
- **Day/Night Theme Change Animation & Easing**:
  - `app/src/main/java/com/eve/app/util/TelegramThemeEasing.kt` (ported from `org.telegram.ui.Components.CubicBezierInterpolator` and `Easings.easeInOutQuad`)
  - `app/src/main/java/com/eve/app/util/ThemeManager.kt` (circular reveal layer placement, radius calculation, and transition sequencing adapted from `org.telegram.ui.LaunchActivity` lines 7351–7473 and `org.telegram.ui.DialogsActivity` lines 14233–14239)
  - **Upstream Snapshot**: `dc780e81ed1261c369c27870e8e0999a1eb0b600`
  - **Copyright Notice**: `LaunchActivity.java` is Copyright (c) Nikolai Kudashov, 2013–2018.
  - **License**: GNU General Public License version 2 or later (GPL-2.0-or-later).
  - **Modifications**: Adapted from Telegram's in-place theme update flow to Android AppCompat Activity recreation and lifecycle-safe transition handoff in Kotlin, faithfully preserving the native `ViewAnimationUtils.createCircularReveal` direction, dual-layer stacking, corner radius calculations, and easeInOutQuad cubic bezier curve.

### Distribution & Licensing Notice
Portions of this software are derived from Telegram for Android, copyright (c) 2013-present Telegram FZ-LLC / DrKLO and Nikolai Kudashov. These components and derivative works are provided under the terms of the GNU General Public License version 2 as published by the Free Software Foundation.
Any distribution of binary artifacts containing these ported components must comply with the terms of the GPLv2.

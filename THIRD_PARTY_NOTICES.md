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

### Distribution & Licensing Notice
Portions of this software are derived from Telegram for Android, copyright (c) 2013-present Telegram FZ-LLC / DrKLO. These components and derivative works are provided under the terms of the GNU General Public License version 2 as published by the Free Software Foundation.
Any distribution of binary artifacts containing these ported components must comply with the terms of the GPLv2.

# Third Party Notices

This repository contains third-party components and assets. Keep license obligations even when older files are retired.

**Development-stage official-reference policy:** Exact official code/art/assets require verified upstream source, identity and appropriate use permissions; public design guidance is not blanket copying permission. Never silently substitute independently authored or other assets unless the owner expressly authorizes them. If the exact source/rights are unavailable, report that item as BLOCKED, continue only other authorized work, and keep third-party attribution and production release obligations.

## Telegram for Android

- **Upstream:** https://github.com/DrKLO/Telegram
- **Source delivery and accepted SHA-256:** Telegram reference source is NOT stored in this repository. The owner attaches `Telegram-master-core.zip` (default; SHA-256 `3ecbbeab06e0cafebd462721f048b637a454522167db94d26c52ccad10ea1db0`), `Telegram-master-extra.zip` (SHA-256 `ef00b8495bbd7775e5e9a9c2acecbf87b9ebac472a30651964fd38b833245517`), or the original complete `Telegram-master.zip` (SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`). Verify against the attached archive's own `EVE_MANIFEST.sha256`, extract outside git, and mark absent files NOT FOUND (request the matching `extra` or `rest-1..5` archive). Only a requested source-exact transplant/comparison depends on these archives. Missing archives never block unrelated Eve work or a separately authored design. Historical split/repacked archives and manifests are provenance references, not EVE build inputs.
- **Archive exclusions (confirmed absent by reference-verification run 40091599695; this checks the repacked archives, not every historical upstream revision):** `TMessagesProj/config/release.keystore`, `TMessagesProj/google-services.json`, `TMessagesProj_App/google-services.json`, `TMessagesProj_AppHockeyApp/google-services.json`, `TMessagesProj_AppHuawei/google-services.json`, `TMessagesProj_AppStandalone/google-services.json`, `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java`. These signing/configuration sources must never be used as Eve credentials.
- **Historical upstream provenance:** original Telegram Git reference `0aae959806c57be8ad457980f2a5c6277ff18635` remains a citation for previously imported icons, not an active submodule or an assertion that root archives contain that exact commit.

### Historical repacked Telegram archive SHA-256 (2026-10-24)

Recorded from [GitHub Actions Telegram reference verifier (run 40091599695)](https://github.com/vishu762701/eve-mock-test-app/actions/runs/40091599695), commit a4be53c718a87357a4cf09935776624e237a85b2. These hashes describe the **former split/repacked root archives**, not the earlier unsplit original archive.

| Archive | SHA-256 |
|---|---|
| Telegram-master.zip | fabf0b7370abf2ee5661063ea1c274cb3e537048f008a6871ff3169cbe89cb1e |
| Telegram-master-rest-1.zip | fb4fdcde402b904e33635a96c7090b523f0c32d2e02ec6db47769889f9b6633c |
| Telegram-master-rest-2.zip | af56e94af6a97287faf329d9422220f19bfe3d281cfcbc13efb4355a365f17b1 |
| Telegram-master-rest-3.zip | 2e2f30a7e86530bf5e3c4256398ccdced45b3609ab675cfb380f83126c7db78a |
| Telegram-master-rest-4.zip | 44aae5a3d18d4d72091f69abcfafbf2e633b07150c6d07be701f3d2c9c5dcb91 |
| Telegram-master-rest-5.zip | c1ce238a7ab23b9a878d588b3e0f97cf43e3756992911897abf71785d5884308 |
| Earlier original complete Telegram-master.zip (different archive) | 0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3 |

The UI/core manifest verified 7,575 file checks, the full manifest verified 13,976 file checks, and the icon registry verified 32 imported icon IDs with 254 source/app hash comparisons; all reported zero mismatches. The seven intentionally excluded files named above were confirmed absent in extracted source. Root LICENSE contained GNU GPL version 2 (June 1991); per-file "or later" grants depend on applicable notices and need separate licensing assessment.

- **Asset verification:** `docs/telegram-source-verification.tsv` records SHA-256 comparisons against that commit; candidate-only rows are not import claims.
- **License evidence:** The extracted Telegram root `LICENSE` contains GNU GPL version 2 (June 1991). Any "or later" permission must be established for each imported asset or derived source file; this archive-level evidence alone does not establish GPLv3 compatibility. See `docs/telegram-gpl-audit-2026-10-24.md`.
- **Retained derived behavior:** `app/src/main/java/com/eve/app/util/ThemeManager.kt` (circular Day/Night reveal and transition layering) and `app/src/main/java/com/eve/app/util/TelegramThemeEasing.kt`.
- **Upstream snapshot:** `dc780e81ed1261c369c27870e8e0999a1eb0b600`; relevant origin: Telegram `LaunchActivity` / `DialogsActivity` transition handling.
- **Credits:** Telegram FZ-LLC / DrKLO and Nikolai Kudashov. The derived theme code is subject to applicable GPL conditions; distributor obligations must be reviewed before publishing a binary.

Old Telegram premium GL renderer classes, models, textures and shaders were deleted during the approved UI design reset. They are **not** current bundled runtime assets. Retain these historical attributions for provenance rather than implying those files still ship.

### Telegram ButtonBounce motion attribution

`app/src/main/java/com/eve/app/designsystem/ButtonBounce.kt` declares a Kotlin port
of upstream `TMessagesProj/src/main/java/org/telegram/ui/Components/ButtonBounce.java`.
The extracted Telegram root license is GPL v2; the applicable version grant
for this specific derived component still requires per-file review. EVE plans
GPL-3.0-or-later distribution, subject to license compatibility clearance. Preserve upstream
attribution and provide this modified Kotlin source with corresponding-source
deliverables. This attribution records provenance; it does **not** establish
byte-for-byte source parity, complete dependency compatibility, or production
release clearance. See `docs/telegram-release-compliance-gate.md`.

### Owner-approved Telegram original icon imports — 2026-10-25

- **Provenance:** owner-supplied `Telegram-master-core.zip`, verified SHA-256 `3ecbbeab06e0cafebd462721f048b637a454522167db94d26c52ccad10ea1db0`, internal `EVE_MANIFEST.sha256` valid for 6,942 files; Telegram `TMessagesProj/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/` original bytes, upstream reference `0aae959806c57be8ad457980f2a5c6277ff18635`.
- **Unmodified assets and EVE aliases:** `msg_channel` → `ic_broadcast`; `msg_go_down` → `ic_chevron_down`; `msg_pin` → `ic_pin`; `msg_bot` → `ic_robot`; `msg_brightness_high` → `ic_theme_sun`; `msg_mini_upload` → `ic_upload`; `ic_rating_star_filled` → `ic_star_filled`; `ic_rating_star` → `ic_star_outline`; `msg_remove` → `ic_minus` (all `.webp`). Original per-density file SHA-256 values are pinned in `docs/telegram-icon-source-registry.tsv` and `docs/telegram-icon-confidence-2026-10-25.md`.
- **Attribution/licensing:** Telegram Android contributors and owners as applicable; root Telegram source archive bundles GPL version 2 text. The applicability of "or later" to individual icon files and combined-work compatibility is unresolved. Do not change the existing fail-closed production distribution guard; preserve notices and corresponding source obligations. No Telegram logos, launcher branding, Lottie, or other assets were imported.

## Inter (Official iOS 26 Typography Approximation)

- Source: https://rsms.me/inter/ and https://github.com/rsms/inter
- Copyright (c) 2016-2026 The Inter Project Authors (Rasmus Andersson).
- License: SIL Open Font License 1.1; full notice distributed in
  `app/src/main/assets/licenses/Inter-OFL.txt`.
- Bundled, unmodified font binaries: Inter Regular 400, Medium 500, SemiBold 600, Bold 700.

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

## Official Telegram ActionBar and search icon art (Mission 01 source import)

- **Origin:** user-supplied official Telegram Android source archive (`Telegram-master (1).zip`, SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`), `TMessagesProj/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/`.
- **Files copied verbatim:** `ic_ab_back.webp` -> EVE `ic_back.webp`; `ic_ab_other.webp` -> EVE `ic_more_vert.webp`; `msg_search.webp` -> EVE `ic_search.webp`, four original density variants each. Existing EVE drawable resource IDs remain unchanged; prior independently drawn vectors with these same resource IDs were removed after replacement.
- **Copyright and grant:** Telegram Android source contributors, including Telegram FZ-LLC / Nikolai Kudashov as applicable; the reference archive's root `LICENSE` contains GPL version 2. An "or later" grant for each of these specific files has not been independently established. EVE's GPL-3 licensing intent does not override third-party terms; the GPL-3 text is in root `LICENSE` and the upstream GPL-2 text is in `app/src/main/assets/licenses/Telegram-GPL2.txt`. Relevant source is publicly available in this repository and the upstream repository.
- **Appearance:** upstream white alpha-mask WebP shapes with the existing Day/Night EVE tint and native touch/accessibility behavior. These particular assets are **static**; no intrinsic Telegram icon motion was found or invented. Not the Telegram brand/logo.
- **Distribution:** publishing an APK with these GPL-covered images requires the complete corresponding source, preservation of notices and a compatible licensing assessment of all included non-free SDKs and other dependencies. Public source publication alone is not a guarantee of compliance. Do not represent release compliance as audited until third-party binary terms and installation-source obligations are checked.


## Seven additional original Telegram static action icons

The source archive's original `msg_camera.webp`, `msg_copy.webp`, `msg_download.webp`, `msg_info.webp`, `msg_link.webp`, `msg_leave.webp`, and `msg_send.webp` have been imported verbatim in four density variants (mdpi, hdpi, xhdpi, xxhdpi), renamed to EVE `ic_camera`, `ic_copy`, `ic_download`, `ic_info`, `ic_link`, `ic_logout`, and `ic_send`. Existing EVE callers, tints, labels and actions remain intact. These are original Telegram assets from a GPL-v2-licensed source archive (per-file "or later" grants unverified), NOT independently drawn vectors or Telegram logo. The earlier complete GPL texts and source-publication obligations continue to apply. None has a proven intrinsic motion: do not invent transitions. The registry-driven `TelegramIconSourceRegistryTest` checks the binaries against pinned upstream SHA-256 values. Release compatibility with separately licensed SDK binaries remains an audit requirement.

### Further original Telegram action icons: Close, Share, Edit

The unchanged Telegram `msg_close.webp`, `msg_share.webp` and `msg_edit.webp` (all four densities) are now shipped as `ic_close`, `ic_share`, `ic_edit` with the original EVE action resource IDs and all existing UI callbacks/tints. All are static masks, not animated sources. Telegram FZ-LLC / contributors retain original copyright; the archive contains GPL version 2, while per-file "or later" grants remain unverified. Root GPL-3 licensing intent and bundled upstream GPL text do not remove corresponding-source or third-party notice obligations. Binary asset SHA-256 is pinned in `TelegramIconSourceRegistryTest`.

### Original Settings and Saved/bookmark action artwork

Two further original Telegram static RGBA icons from the owner-supplied official source ZIP were imported byte-for-byte in four standard Android densities: `msg_settings.webp` as EVE `ic_settings`, and `msg_saved.webp` as EVE `ic_bookmark`. Original Telegram copyrights remain; the archive's GPL-v2 license and EVE's intended GPL-3 distribution require per-file compatibility clearance. Existing view IDs, Day/Night color tints, drawer/menu destinations, touch interactions and saved-question state logic are unchanged. `TelegramIconSourceRegistryTest` checks all source bytes. No static icon motion has been invented.

- Original Telegram `msg_delete.webp` is imported unchanged for `ic_delete.webp` at source-provided mdpi/hdpi/xhdpi; upstream does not provide a matching xxhdpi file, so Android scales the closest source density. This is a static delete glyph; no Telegram animation is implied. GPL/source-distribution obligations above apply.

## Mission 02: Telegram Complete Visual System, Themes, Icons, and Navigation Pill

- **Source:** Reference archive from user-authorized Telegram Android source package.
- **Icons imported across 4 densities (mdpi, hdpi, xhdpi, xxhdpi):**
  - `msg_report.webp` -> `ic_report_flag_premium.webp` (Report question action)
  - `msg_archive.webp` -> `ic_history.webp` (History action)
  - `msg_openprofile.webp` -> `ic_person.webp` and `ic_user_profile.webp` (Profile actions)
  - `msg_add.webp` -> `ic_add.webp`
  - `msg_language.webp` -> `ic_language.webp`
  - `msg_qrcode.webp` -> `ic_qr_code.webp`
  - `msg_secret.webp` -> `ic_key.webp`
  - `msg_groups.webp` -> `ic_users.webp`
  - `msg_calendar.webp` -> `ic_calendar.webp`
  - `msg_stats.webp` -> `ic_analytics.webp`
  - `msg_home.webp`, `msg_theme.webp`, `msg_palette.webp`
- **Theme and Navigation Components:**
  - `TelegramThemeEngine`: `.attheme` parsing, `changeColorAccent` HSV shift, fallback chain, House Day/9 and Night/0 defaults.
  - Historical Mission 02 bundled themes (`day.attheme`, `night.attheme`, `arctic.attheme`, `darkblue.attheme`, `bluebubbles.attheme`) in `app/src/main/assets/themes/`.
  - Settings > Appearance (`AppearanceActivity` and `activity_appearance.xml`) theme picker and preview.
  - Floating bottom navigation pill (`MainTabsLayout`, `GlassTabView`, `EveBottomPillBar`, `EveNavigationHost`) with Telegram's 320ms decelerate capsule animation, 56dp height, 8dp margin, 344dp max width, and `BlurredBackgroundProviderImpl.mainTabs()` glass drawable with AGSL shader fallback.
  - Tab Settings animation (`tab_settings.json`) in `app/src/main/res/raw/`.
- **Copyright & License:** Telegram FZ-LLC / Nikolai Kudashov and contributors; the reference archive's root `LICENSE` contains GNU GPL version 2. Per-file "or later" grants and GPLv3 compatibility remain under review. EVE-authored code uses GPL-3.0-or-later (root `LICENSE`), which does not relicense upstream assets.


### Telegram RadialProgressView adaptation
The EVE loading spinner in `app/src/main/java/com/eve/app/designsystem/TelegramRadialProgressView.kt`
adapts the animation and stroke/arc calculations in Telegram Android
`TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java`
(Copyright Nikolai Kudashov, 2013–2018; GNU GPL version 2 or later,
explicitly stated in the Java file header at upstream commit
`0aae959806c57be8ad457980f2a5c6277ff18635`).
EVE-specific modifications: AndroidUtilities dp calls replaced with local density
calculation, Telegram Theme progressCircle replaced with the EVE resolved accent
color, Java source translated to Kotlin, XML-friendly constructor, and an EVE
`TelegramRadialProgressIcon.kt` Drawable bridge. This is not verbatim source
reuse and exact behavioural parity has not been independently established.
No Telegram service marks or strings are imported. This file-specific license
header does not establish compatibility for all other Telegram assets or the
combined release binary; the production GPL release gate remains mandatory.

### Mock-test Telegram radio indicator adaptation

`app/src/main/java/com/eve/app/ui/common/TelegramRadioIndicator.kt` derives the
no-icon drawing and checked-progress transition from official Telegram
`Components/RadioButton.java` at revision
`0aae959806c57be8ad457980f2a5c6277ff18635` (SHA-256
`2422b0fecc33129021ec0654f66f0ec3dd5be78c1191890a5ff25b0758abb61b`).
`Cells/RadioButtonCell.java` (SHA-256
`7745a9d5ac7a9e6f52e5089ab8b1c221f16db06c5edac5225eb6f772a610c0c2`)
provides the 20dp size and binding/accessibility reference. Both original headers
credit Nikolai Kudashov, 2013–2018, and explicitly grant GPL version 2 or later.
The upstream bytes match the owner's original reference file hashes; the original
ZIP was not available and its whole-archive hash was not verified in this mission.
See `docs/telegram-radio-indicator.md` for compatibility deviations. Preserve the
bundled GPL text and corresponding modified source; all existing production
release clearance/signing guards and other component obligations still apply.

### Original-source Light/Night role migration

Owner-supplied color source pack SHA-256
`4b37c4c6749c5347cafdc16c6a1cbb6371599e09222e40be983135400f4fe469`
was checked against its generated (not Telegram-signed) per-file manifest: 57
original source files, sizes, CRC32 and SHA-256 values. The original complete
archive hash is attested by the pack author, not independently verified here.
See `docs/color-migration/source-role-contract.json` and the adjacent mapping.
ThemeColors/Theme definitions, original Night asset and main-tabs coating math
inform the EVE adaptation. The Light asset is an explicitly generated default-role
projection; Night bytes retain their verified source hash. Existing Telegram
copyright/license text, corresponding-source obligations and fail-closed production
release clearance remain unchanged. Platform blur, AA adjustments and unmatched
filled segmented controls are explicitly documented EVE deviations.

### Telegram Bulletin source-traced EVE adaptation

`AppBulletin` and `layout_telegram_bulletin.xml` are an EVE-authored behavioral
adaptation of Bulletin.java, BulletinFactory.java and theme-role facts supplied
by the owner for Mission 04. No upstream Bulletin source file was copied or
independently verified in this session; the owner-provided archive hashes are
provenance claims, not a validation performed here. The EVE implementation uses
its own host and AndroidX spring transitions, with accessibility
and lifecycle deviations described in `docs/BULLETIN_MISSION_04.md`. Existing
Telegram license, corresponding-source and release gates remain in force.

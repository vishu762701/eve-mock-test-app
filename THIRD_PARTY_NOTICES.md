# Third Party Notices

This repository contains third-party components and assets. Keep license obligations even when older files are retired.

**Development-stage official-reference policy:** Exact official code/art/assets require verified upstream source, identity and appropriate use permissions; public design guidance is not blanket copying permission. Never silently substitute independently authored or other assets unless the owner expressly authorizes them. If the exact source/rights are unavailable, report that item as BLOCKED, continue only other authorized work, and keep third-party attribution and production release obligations.

## Telegram for Android

### Mission 06 additionally authenticated official revision (2026-10-28)

- **Uploaded ZIP:** `Telegram-master (1).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10` is **not** byte-for-byte any historical accepted archive. It has no `EVE_MANIFEST.sha256`; never relabel or re-hash it as the historical approved source.
- **Independent official content identity:** GitHub `DrKLO/Telegram` immutable commit [`3de37c8cfb039452be7398909461da6288babf15`](https://github.com/DrKLO/Telegram/commit/3de37c8cfb039452be7398909461da6288babf15) has canonical root tree `cc3c6ccf48e4167fdec764dfd04e765043f55093`. The locally reconstructed Git root tree from **all 14,086 ZIP file blobs**, mode metadata, recursive directory trees and **19 verified upstream Gitlink references** matched this value exactly. This establishes source-content fidelity to this **different, newer official revision**, not the historical archive container.
- **Verified exact source candidates, not imported in this mission:** `ActionBar/ActionBarPopupWindow.java`, `ActionBar/DrawerLayoutContainer.java`, `ActionBar/AlertDialog.java`, `Components/FilterTabsView.java`. They bear upstream GPL v2-or-later notices; dependency-complete Android integration, visual acceptance and combined-binary license compliance were **not** verified. No Telegram account infrastructure, proprietary keys, signing keystore, network client or source-renderer copy was added by this check.
- **Source-specific implementation ledger:** [Mission 06 source-fidelity report](docs/MISSION_06_SOURCE_EXACT_REPORT.md) distinguishes immutable source-file authentication from source-compatible rendering. Existing historical owner-accepted ZIP checksums, notices, original import history and fail-closed release conditions below remain unchanged.

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

### AndroidX DynamicAnimation 1.0.0

The bulletin springs use `androidx.dynamicanimation:dynamicanimation:1.0.0`
from the Android Open Source Project. Its published Maven POM identifies the
Apache License, Version 2.0; the license text is included in
`app/src/main/assets/licenses/BlurView-LICENSE.txt`. Source:
https://android.googlesource.com/platform/frameworks/support/.

### Source-exact Telegram assets for EVE Home overflow (post-Mission 05 icon parity)

The owner-supplied complete Telegram Android archive was checked against SHA-256
`0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`.
EVE's Home 3-dot popup now references the unchanged upstream binary assets
`TMessagesProj/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/large_notes.webp`
(Notebook), `msg_topics.webp` (Topic-wise) and `msg_filehq.webp` (PYQ),
and unchanged `TMessagesProj/src/main/res/drawable/menu_night_mode_24.xml`
(Dark Mode). The EVE resource names are `telegram_popup_notes`,
`telegram_popup_topics`, `telegram_popup_paper` and
`telegram_menu_night_mode_24`. Each new resource's SHA-256 is tested against
original ZIP bytes in `UiBehaviorRegressionTest`; no WebP was re-encoded and
the vector was copied verbatim. History, Bookmarks, Light Mode and Logout
already used imported original Telegram assets.

These are genuine Telegram artwork assets, not a claim Telegram itself has
EVE's exam-specific menu functions. Each selected item is a source-exact asset,
while its EVE placement and Android PopupWindow/glass renderer remain adaptations.
Retain upstream Telegram GPL license notices and existing distribution/release
compliance gates, including obligations for copied source/assets; do not assume
a successful development build alone establishes redistribution clearance.


### Mission 06 — isolated Telegram popup radial selector mask (approved revision)

- Owner approved original source revision: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. The ZIP containing those **same source file bytes** is SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`. The historical ZIP hash requirement was superseded for Mission 06 only; no archived ZIP equality is claimed.
- Original `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java:5296-5357,5387-5395` is Git blob `b56ee0d84558b738144f16961eeaec8b767bb993` (ZIP SHA-256 `b2cc15c52f4b82ca79cee15c3e3d62f93c4b4e91e578e6da850fd279a1591e90`).
- Original `ActionBarPopupWindow.java:604-609` is blob `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea` (SHA-256 `4f4ccd178f82abf4e9cb1cdff330b57d34df9340bf23a2d86d75e1fe429a05c3`). It assigns the first and last visible menu rows original 6dp corner masks.
- Original `Cells/BaseCell.java:150-165` is blob `f352ac78dc0438bd50f6c62ce2a438137540159b` (SHA-256 `a77dd0fe9fad11b3a4d9f055da0492faa7ef9cd171a105d3b9b217372624b183`).
- Isolated into `app/src/main/java/com/eve/app/ui/home/TelegramPopupRadialSelector.java` under the upstream GPL-2-or-later file header. Uses original eight corner radii, white anti-aliased clockwise mask, original RippleDrawableSafe canvas save/restore/error-handling semantics. Necessary platform-only deviations: Telegram `AndroidUtilities.dp` replaced by context density; `AndroidUtilities.rectTmp` and global `Theme.maskPaint` replaced with instance-owned primitives; `BaseCell.RippleDrawableSafe` reconstructed as independent subclass using EVE logger. `TelegramMenuPopup` binds it to existing seven rows using EVE Day/Night selector color, WITHOUT importing Telegram account/network code, modifying actions, theme reveal or implementing a counterfeit full `ActionBarPopupWindow`.
- This is a **source-backed mask/selector atom, not a byte-exact import of the full original popup**, which still requires its own vetted dependency chain, entrance/dismissal physics, original popup background and full frame/native verification. Keep the bundled Telegram GPL text, attribution, corresponding source and release compliance gates; APK distribution compatibility with every binary dependency remains unverified.


### Mission 06 approved-revision popup background source import

From `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java:148-170,294-297` (Git blob `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea`). Original `popup_fixed_alert2.9.png` copied byte-for-byte under EVE name `telegram_popup_fixed_alert2.9.png`, mdpi/hdpi/xhdpi/xxhdpi. Original Git blob SHAs in that order: `e757e48312943bcea7f4577fef9e5d6f48612f4c`, `a59945a2728562a4dbce645d8d6ea70f0b4a894a`, `a755c2b17917492c0e8200a9be0350019fa56e36`, `7f92a8eaa31cd9c527da8e7f677642762122293a`. Original SHA-256 values: `8ab86f9766f8b4501f3d2e29bc6d29c88efd03307d424d04d15ba42cf3824e22`, `84aa21dbbb203d43f751faf1ffa628af3db468265e882fe790f30b240d3bab49`, `561ff08468cbf8745e2622358809904466b5c72748fef97a742fdde83e0e3bfa`, `c0cf9ea52108442f76d39cc736f991e0c8a1ffd7615cf80d5f03db38d6453811`.

The Home overflow alone uses original 8dp content padding, `PorterDuff.Mode.MULTIPLY` and the existing source-mapped Telegram submenuBackground role. A dormant opt-in property on shared `DesignGlassLayout` maintains this original 9-patch when blur is disabled; all other glass screens are unchanged. EVE's PopupWindow host, measured scroll layout, and entrance/dismissal motion are not copied ActionBarPopupWindow renderers; full source-fidelity and native visual tests remain pending. Source attribution (Nikolai Kudashov/Telegram contributors; GNU GPL v2 or later where individually granted), corresponding-source and production binary license checks remain mandatory.


### Mission 06 Home category selected glyph — source property correction

At owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, official `TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java:388-400,649-659` (Git blob `52238e832e9abcdbf9f177717cdf281fe219f175`, source SHA-256 `25e2f34494903fedc86375632c769570083135ddb093bb5868f97f2cd02ae87c`) measures title and optional counter, with no checked-icon slot. EVE's existing Home category Chip previously introduced an unrelated checked-icon glyph that changed its width. Only that EVE-only glyph was removed, preserving filter selection, Day/Night selected colors and touch contract; `TargetExamsBottomSheet` retains its own selection checkmark. This is a **verified source-property correction of an EVE Chip**, not a claim that the Material Chip is the exact original `FilterTabsView` layout or indicator physics. No Telegram services/assets were copied for this step.


### Mission 06 — original Telegram search-container alpha timing source atom

Upstream `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:935-1041`, Git blob `55c3634ea5658c31d58df0682c0a73a620472ede`, SHA-256 `1a52bba54d5ca41cb5f8b41d7f5f6ada6964625df829351803fe86efdee2e72f`. Original uses a **150ms AnimatorSet** alpha crossfade between searchContainer and menu icons and preserves keyboard focus. The previous EVE Home spring-width morph was a different implementation; this adaptation uses the verified source duration/alpha property and cancellation semantics. EVE mapping deviation: normalTopBar/searchTopBar are whole EVE header groups rather than original ActionBarMenuItem children; keyboard, selected categories, persisted queries, Lottie and Firebase scoring are preserved. This is not an import of original ActionBar and does not establish exact frame/IME parity before native tests. Retain original Telegram GPL attribution and all release obligations.


### Mission 06 — isolated official ActionBarPopupWindow row entrance and default exit

Revision `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. Original `ActionBarPopupWindow.java:843-913,1024-1069` (Git blob `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea`, SHA-256 `4f4ccd178f82abf4e9cb1cdff330b57d34df9340bf23a2d86d75e1fe429a05c3`); `AndroidUtilities.java:5225-5230` (Git blob `efa8e2956c84582b5215083c605e7a9756560748`, SHA-256 `6acc294fa98d5628c0e669f31cf5235e655d79934d06e684d3b4ea6d6f7de4cb`). New GPL-attributed `TelegramPopupSourceMotion.kt` isolates original per-visible-row cascade equation, `-6dp` row entrance translation, `150 + 16 * visibleCount` ms timing, disabled-row alpha=.5, background alpha0..255, and default `scaleOut=false` exit's `-5dp` translation plus fade over 150ms. Necessary EVE deviations: it targets seven existing EVE action rows, the source `backScaleY` background-only growth remains **UNIMPLEMENTED** because original `ActionBarPopupWindowLayout.dispatchDraw` is not ported, and theme reveal cancels row animation on popup dismissal. It does not import Telegram UserConfig, notifications, account services, navigation classes or networking. EVE's unrelated `TelegramPopupHelper` stays as-is for existing non-Home callers. Original copied math/animation expressions carry Telegram upstream GPL-2-or-later attribution; retain full GPL source/delivery compliance. This is source-backed motion subset, not the entire original popup renderer.


### Mission 06 — original no-gap popup background-only backScaleY growth

Source: approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java:467-539`, Git blob `6a9dc814bbfdd56f36ac4e5f8ab70fe179b231ea`. Original no-gap, no-swipe, shown-from-top case uses the nine-patch drawing bounds `(0,0,width,(int)(height*backScaleY))`, background alpha `backAlpha`, and clips child drawing to its growth bounds. `TelegramPopupSourceBackground.kt` isolates these source expressions with the genuine unchanged nine-patch and `DesignGlassLayout` popup-only child clip. `TelegramPopupSourceMotion` now drives `backScaleY` with the verified original 150+16×visibleItems animator and 0–255 alpha. **Necessary deviations:** original `GapView`, reactions, swipe-back and shownFromBottom source branches are not transplanted because EVE's seven-action menu has no such content; the original complete ActionBarPopupWindow class with service/navigation dependencies remains NOT fully ported. Source expression license GNU GPL v2 or later per original header; preserve notices, corresponding source and production compatibility audit. No whole-popup scale imitation, no Firebase/account/network import.


### Mission 06 — original Telegram normal dialog background atom (source-revision approved)

Source: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:302-318,680-693` (original Git blob `c5574d9682d98a276d3d84892e1b6a584f1c7794`). Four **unchanged** Telegram `popup_fixed_alert4.9.png` assets were imported under EVE resource name `telegram_dialog_popup_fixed_alert4.9.png`. Original Git blob SHA-1 values, mdpi/hdpi/xhdpi/xxhdpi: `53fa939aa586826cb9edcacd5624aca5e1eb12ea`, `17d032ef07a97dede8c2edb5f0fa7dbdeae9b94b`, `a1ccada709280d953a0ae7175d06efcbb1fb91fa`, `5275c8cd0ff41411cbf5621d56333395a7e871c8`. Original ZIP asset SHA-256 values are pinned in `TelegramDialogSourceAssetsTest.kt` and match the approved immutable upstream revision. The EVE-only `TelegramDialogSourceBackground` sets original `dialogBackground` role tint using original `PorterDuff.MULTIPLY`. This is applied **only** to Home's admin feedback-deletion confirmation after `showDestructive()`, preserving authorization checks, confirmation listeners, and destructive action styling. Important **non-exact host deviation:** EVE uses the MaterialAlertDialog window to host the original drawable, not Telegram's `AlertDialogView` content container; original Telegram account/Stars/notification dependencies are not imported. No exact complete Telegram AlertDialog functionality, shadow/clip/frame parity or release compliance is claimed. The upstream GPL-v2-or-later file header, attribution, source delivery and existing production release safeguards remain in force.


### Mission 06 — source-backed Home category selector geometry and original color roles

Owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. Original `TMessagesProj/src/main/java/org/telegram/ui/Components/FilterTabsView.java:176-178,904-918,1448-1514`, immutable Git blob `52238e832e9abcdbf9f177717cdf281fe219f175` (ZIP SHA-256 `25e2f34494903fedc86375632c769570083135ddb093bb5868f97f2cd02ae87c`). Original `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java:15,211-216`, Git blob `ba9c71fa39c7d0ed54d6d574396f6c35608fc1de` (ZIP SHA-256 `49f27ef5f1a33844ca8818feb4b737a96df8d8d4eaef50a20b11225c46c2000d`). New palette roles `actionBarTabLine` and `actionBarTabActiveText` are source `#298ACF` Day, `actionBarTabUnactiveText` `#777C7F`, `actionBarTabSelector` `#121A1D21`, preserving direct Night .attheme key overrides. `HomeCategoryChipGroup` isolates original selector drawable construction: `GradientDrawable`, eight 14dp corners, 28dp height at centerY-14dp, title-width-centered indicator, 12.5dp internal horizontal padding, alpha 31. `EveDesignSystem.createCategoryChip` removes the independently authored solid selected fill and uses source tab active/unactive text roles, preserving 40dp visual height, 48dp touch delegation, selected state, Hindi and alternate no-checkmark behavior.

**Explicit necessary deviations** from original class: EVE is a horizontally scrolled Material ChipGroup rather than Telegram's RecyclerListView/TabView; Android Layout text measurement substitutes for `HintView2.measureCorrectly`, exact Telegram emoji/counter/editing/reordering/delegate/indicator interpolation and list physical gestures are NOT PORTED. Only the verified selector shape/color atom is transplanted from the GPL-2-or-later source; no Telegram account/RPC dependencies introduced. Full visual/device fidelity and combined-binary release license compliance remain pending.


### Mission 06 — original Telegram tab indicator EASE_OUT_QUINT and 320ms frame timing

The officially verified unmodified Java `TMessagesProj/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java` at owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` is imported **verbatim** to `app/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java` (Git blob `3a9cae100e17a03abacf2f4df71db2b82757b968`, SHA-256 `747f7b8f511249f14bc1b567088d7e0da3639729263a6f56243eb61c2ba19387`; upstream license is GPL version 2, with original root LICENSE and EVE's bundled copy retained). The source `FilterTabsView.java:837,855-881,1179-1190,1454-1514` defines `EASE_OUT_QUINT` Bezier (.23,1,.32,1), 320ms duration with a capped 17ms frame delta, interpolated selector bounds and cancellation. Home's previously static original 28dp alpha31 selector now uses that **exact unmodified original interpolator and timing equation** for category changes. EVE host adapters: `ChipGroup` checked change substitutes for Telegram `FilterTabsViewDelegate`, Android `postOnAnimation` replaces Telegram global runOnUIThread queue, drawing reuses the source-backed 28dp rounded selector and the existing selected-category data state. No account, storage, RPC, premium or server components are imported. This remains a source-authentic animation **subset**, not a complete FilterTabsView class or verified frame-accurate device capture; full native tests and combined-GPL release obligations remain outstanding.


### Mission 06 — narrowly scoped WCAG AA accommodation for source-original category text

Approved Telegram source role values remain byte-exact in `TelegramColorRoles.defaults` (`ThemeColors.java:15,211-216`): `actionBarTabActiveText=#298ACF`, `actionBarTabUnactiveText=#777C7F`, `actionBarTabLine=#298ACF` and original selector alpha=31. On a white background, active/inactive raw text contrast is approximately 3.73:1/4.22:1, below WCAG AA 4.5:1 for 14sp text. EVE applies its existing `TelegramColorRoles.accessibleForeground()` AA algorithm **only** to the active/inactive Home text colors against the actual selected overlay and Day/Night canvas surfaces. The original raw role values, 14dp/28dp/12.5dp selector geometry, original 320ms EASE_OUT_QUINT source animation and source indicator color stay unchanged. This is an explicit accessibility deviation from exact original text pixels, not an unacknowledged Telegram-original color claim. No unrelated view or backend code is changed; actual on-device font scale/TalkBack/theme visual acceptance remains pending.


### Mission 06 — source-verified DarkAlertDialog Night-role subset

Upstream owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/DarkAlertDialog.java:5-19`, Git blob `d90759c4000cb88f709d4e4691a09edd96b924aa`, ZIP SHA-256 `95e78aef9cf6e53fc2555b005f47de8f625c2502394e4a9f76f26c8cc30482a0`. Official DarkAlertDialog overrides `dialogBackground=#FF262626`, `dialogTextBlack=#FFFFFFFF`, `dialogButton=#FFFFFFFF` and `dialogScrollGlow=#FFFFFFFF`. The EVE-only `TelegramDialogSourceBackground.apply` now maps the Home admin confirmation's Night mode to this verified source background/text/negative-button palette while preserving the original unchanged nine-patch, positive **destructive** button fill and admin authorization/confirmation callback. **Explicit deviations:** EVE's MaterialAlertDialog window is not the upstream AlertDialog renderer; it doesn't import original Theme/NotificationCenter or implement the original glow or layout, and Day mode follows original `dialogBackground` role without overriding it. The mapping from EVE Night to Telegram's opt-in DarkAlertDialog variant is an EVE owner-requested integration choice, NOT a byte-exact DarkAlertDialog class import. GPL notices and release-compliance safeguards remain mandatory.


### Mission 06 — exact source typography units with accessible dialog-host adaptation

Official `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:809-821,874-878,1137-1204` (Git blob `c5574d9682d98a276d3d84892e1b6a584f1c7794`), sets the normal title to 20dp, message text to 16dp and both confirmation actions to 16dp. Source `AndroidUtilities.bold():262-271` resolves the official Telegram Roboto Medium typeface. On the Home admin feedback deletion dialog only, `TelegramDialogSourceBackground.apply()` now applies those source numeric text sizes with `SP` units rather than `DIP`, intentionally retaining Android user font-scale accessibility, and applies EVE's verified Telegram medium typeface to title/buttons. This is a **documented font-scale deviation** from unmodified Telegram pixels. MaterialAlertDialog's accessible minimum target, owner-required destructive positive-button fill and callback, original nine-patch, Night DarkAlertDialog override and authorization remain unchanged. This is not a full native Telegram AlertDialog renderer or claim of exact layout hierarchy; GPL and combined-release obligations remain in force.


### Mission 06 — original Telegram BottomSheet source scrim and entrance dim animation

Official owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java:135-192,213-222,1487-1491,1766-1778`, source Git blob `d45bb36117f11adb5c579ed540400c2897fad9e2`, original header GNU GPL v2 or later. The extracted `TelegramBottomSheetSourceScrim` retains original AA Paint, PorterDuff SRC mode, opaque black paint, four inset-adjusted drawing bounds, drawRect, alpha accessors and color filter; original `AndroidUtilities.makingGlobalBlurBitmap` guard is intentionally excluded because the EVE sheet does not import Telegram global blur state. `TargetExamsBottomSheet` now disables the window-level `FLAG_DIM_BEHIND` (original Telegram explicitly does this) and draws this custom scrim behind its actual content, fading alpha 0–51 in 400ms with the **original unchanged** imported `CubicBezierInterpolator.EASE_OUT_QUINT` and 20ms start delay. Reduced-motion branch snaps to original 51 alpha. The full-screen parent is EVE Material's coordinator rather than Telegram's original nested custom FrameLayout; native host/gesture/pixel/IME verification and complete BottomSheet renderer/drag-physics source fidelity remain **NOT VERIFIED**. All target-exam multi-select, persistence, button handlers, body sheet styling, two detents and authentication are unchanged. This source-backed atom is GPL-covered; retain corresponding sources, license notices and production review.


### Mission 06 — original AlertDialog activity-safety gate for admin confirmation

Official approved Telegram `ActionBar/AlertDialog.java:325-328` delegates to `AndroidUtilities.isSafeToShow()`. In verified `TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java:6255-6267` (Git blob `efa8e2956c84582b5215083c605e7a9756560748`), `isActivityRunning(activity)` requires `!activity.isDestroyed() && !activity.isFinishing()`. EVE's Home feedback admin-confirmation flow is asynchronous, so it now preserves this exact activity-safety boolean directly after the backend-admin check, before creating the dialog. No Telegram account/network runtime is imported; the server-access gate, destructive confirmation callback, original nine-patch, title typography, Day/Night roles and all admin operations remain untouched. This is an original-source lifecycle property, not the complete Telegram Dialog renderer.


### Mission 06 — original Telegram search collapse focus release

Official owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:961-1005`, Git blob `55c3634ea5658c31d58df0682c0a73a620472ede`. Original search collapse explicitly calls `searchField.clearFocus()` before returning to its menu layout and hiding the keyboard. EVE's source-backed 150ms crossfade already cleared query/cancelled debounces and hid the IME but did not clear focus from the collapsing field. `MainActivity.closeSearch()` now performs the same source focus release; the input retains `requestFocus()` on expand. Existing exam discovery, query restoration, motion/reduced-motion, Lottie and server-authoritative scoring remain unchanged. Necessary view-host deviation: EVE uses two toolbar groups rather than original ActionBarMenuItem hierarchy; no network/account services imported.


### Mission 06 — original Telegram overflow row title behavior

Official owner-approved source `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuSubItem.java:91-98`, Git blob `3d452aabdf320aae2ebd4a8cfed26bc2902e6df8`. Upstream `textView.setSingleLine(true)`, `setEllipsize(TextUtils.TruncateAt.END)` are now applied to all seven current EVE overflow titles in `TelegramMenuPopup.applyOriginalMenuRoles()`, preserving the existing EVE 48dp row targets, actual menu destinations, Day/Night tint and popup motion. The row's full `contentDescription` remains the untruncated original title for accessibility; high font scales may visually ellipsize as in source. The EVE LinearLayout/TextView row host is an explicit original-view-hierarchy deviation; no claim of full ActionBarMenuSubItem byte-exact transplant or Telegram account imports. Same behavior is asserted in JVM source-contract and final native fixture; original source GPL notice/distribution obligations retained.


### Mission 06 — original Telegram Day/Night popup icon pair

Approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/res/drawable/menu_day_mode_24.xml`, Git blob `53c1495614215b6e69fda039cb8f69464a3998f8`, SHA-256 `f7c0b2b36531b877a01e82a1193ff39ae59ed11784e9123c90fed541d7318c84`. Copied **unchanged** as `app/src/main/res/drawable/telegram_menu_day_mode_24.xml` and used by `TelegramMenuPopup.setupThemeCard()` in Dark mode for the Day action. Existing original Night icon `menu_night_mode_24.xml` is already byte-equal to upstream Git blob `bf0364d4826af33b83d578d2eae1594395590294`, SHA-256 `66f143dbffd1c5abbc85dae15de22161a6a7d3c13ded1fc0168051b2af41d861`. Both remain 24dp original vector geometry with menu tint from official source role and the existing protected EVE circular theme reveal handoff unchanged. These are genuine upstream icons (GPL obligations apply), not invented vector replacements. Two-branch selection is enforced in JVM tests and icon display is covered by the terminal native fixture; source bitmap/color on-device validation remains outstanding.


### Mission 06 — correction: saved-question icon was already official Telegram source

The existing `app/src/main/res/drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_bookmark.webp` files are each **byte-for-byte identical** to original `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/res/drawable-{density}/msg_saved.webp`. Their Git blobs mdpi/hdpi/xhdpi/xxhdpi: `e7cdcff6e452c07459dc31eac58af0f36d1c7d62`, `ec3097732ea3a2462e241386876ca93a00af44d2`, `bf4fda65e037332f7bfe8f02a74d2ebc53238eba`, `63c064cafb3e217d55df9b31575352dc7c9efe66`. Immutable SHA-256 and original byte counts are enforced by `TelegramPopupBookmarkSourceTest`. These files were verified both against the supplied approved source ZIP and upstream immutable GitHub objects.

Source-fidelity correction: initial Mission 06 icon swap at `d55ef3a6` misidentified this existing original Saved Messages artwork as third-party and temporarily used another genuine upstream asset `menu_browser_bookmarks.webp`, intended for Telegram's Browser Bookmarks. For EVE's saved-question Bookmarks destination, the already-present `msg_saved.webp` is the appropriate original source, so the popup row has been restored to `@drawable/ic_bookmark`; the four newly introduced browser-specific files are deleted from the current tree. Existing `ic_bookmark.webp` original bytes have **not** been rewritten, re-encoded or deleted. All actions and tint/Day/Night/theme reveal protections remain unchanged. Original Telegram GPL and corresponding-source obligations apply.



### Mission 06 — source-original Home popup title horizontal alignment

Official upstream `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuSubItem.java:83-84,206-218` (Git blob `3d452aabdf320aae2ebd4a8cfed26bc2902e6df8`). The original popup row uses 18dp outer padding and 43dp title inset when an icon is present, i.e. text starts at 61dp from the logical start edge. EVE has an existing **24dp** icon inside a horizontal LinearLayout, so its previously generic 20dp label gap placed title at 62dp (18+24+20). The **popup-local** `telegram_popup_label_gap=19dp` is derived exactly as `43-24`, restores the original 61dp logical inset in LTR and RTL and changes all seven popup labels only. The shared `design_shadow_room` 20dp remains unchanged for all other screens. Source mapping and RTL/large-screen native geometry tests are included. Host view hierarchy still differs from Telegram's FrameLayout; no unsupported whole-class fidelity is claimed. GPL source and distribution requirements remain unchanged.


### Mission 06 — original ActionBar popup animation ownership

Owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:841-845,884-889` (Git blob `55c3634ea5658c31d58df0682c0a73a620472ede`) explicitly calls `popupWindow.setAnimationStyle(0)` when its custom `ActionBarPopupWindow.startAnimation()` controls entrance. EVE `TelegramMenuPopup.show()` now does the same and clears its platform `exitTransition` before running the already-verified source-backed 150+16×visibleRow cascade; this avoids stacking Android PopupWindow's default transition on top of the original Telegram animation. The existing 150ms custom dismiss, 7 actions, pinned anchor/window bounds, accessibility, and circular reveal handoff remain intact. This is original animation *ownership* only, not full `ActionBarPopupWindow` window/gesture parity. JVM and native contracts check style zero and unchanged source cascade. GPL source obligations retained.


### Mission 06 — shared EVE destructive confirmations adopt verified source renderer atoms

Owner-approved `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:302-318,680-693,809-821,874-878,1137-1204` and `DarkAlertDialog.java:11-19` (original upstream per-file blobs previously verified). The original Telegram four-density `popup_fixed_alert4.9.png`, `dialogBackground`/Dark `#262626`, `MULTIPLY` tint, 20/16/16 text sizing and Roboto Medium are applied through EVE's existing central `AlertDialog.Builder.showDestructive()` extension rather than being limited to a Home-only call. This covers existing EVE-owned destructive action builders (including admin delete, user/premium management and settings) **only when they call the shared showDestructive API**; other plain show()/custom builders and Android system permission dialogs are deliberately untouched. The EVE-hosted MaterialAlertDialog hierarchy, 48dp minimum touch targets, destructive primary button fill/contrast, callbacks, server authentication, and dismissal outcome remain unchanged. `MainActivity` no longer duplicates the source adapter; native fixtures exercise the shared path. It is an **original source-backed background/typography subset** rather than a full byte-exact Telegram AlertDialog/animation port. GPL and release gate obligations remain enforced.


### Mission 06 — TestActivity exit and submit confirmations use verified original AlertDialog atoms

Owner-approved original `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java:302-318,680-693,809-821,874-878,1137-1204` and `DarkAlertDialog.java:11-19` (original Git blobs already recorded). `TestActivity` Leave Test and Submit Test confirmations now use the existing verified `TelegramDialogSourceBackground.apply` adapter **after** their AndroidX dialog has been shown. The unchanged original four-density nine-patch, Day `dialogBackground` / Night `#262626`, `MULTIPLY` and original title/message/action typography are reused, with documented EVE SP accessibility deviation. This does **not** port the Telegram AlertDialog view hierarchy or change the Android system permission UI. New guards allow at most one active Leave or Submit confirmation at a time, reject callbacks after Activity teardown, and dismiss Activity-owned windows on destroy. EVE's original exit pause/session persistence, timer lifecycle, palette counts, cancel/continue behavior, server-authoritative submission and offline queue are unchanged. This is a genuine source-backed renderer subset plus EVE functional guards, not a claimed full Telegram dialog physics transplant. GPL and distribution obligations remain in force.


### Mission 06 — remaining exam notices use original AlertDialog atoms without changing recovery flow

The existing `TestActivity` Attempt Limit Reached, Submission Failed, and Submission Saved Offline notices now use one Activity-scoped `showExamNotice(AlertDialog.Builder)` adapter. All three retain their original title/message, retry, save-offline, cancel, acknowledgement/finish callbacks and noncancelable status where originally specified. The helper guards `!isFinishing && !isDestroyed`, dismisses any still-open earlier exam notice, tracks the currently visible dialog and dismisses it in `onDestroy`. After showing it, the approved-revision `TelegramDialogSourceBackground` applies source `AlertDialog.java:302-318,680-693,809-821,874-878` background/typography and `DarkAlertDialog.java:11-19` Night role. This is **not** a full Telegram dialog/class/animation port; all original EVE server-side scoring, clientAttemptId, pending submission queue and retry/session integrity remain unchanged. GPL source attribution and release obligations remain in force.


### Mission 06 — completed-exam reattempt and attempt-limit confirmations

EVE `CompletedExamBottomSheet` now applies the already verified official Telegram `AlertDialog.java:302-318,680-693` and `DarkAlertDialog.java:11-19` nine-patch/typography atoms to its two EVE-owned reattempt dialog variants (limit reached, confirm new attempt). This is a MaterialAlertDialog host adaptation, **not** a full Telegram AlertDialog class or physics replacement. Existing attempt-limit check, positive action, pending/history clearing, result navigation, callbacks and bottom-sheet UX are preserved. The UI suppresses duplicate asynchronous attempt-limit lookups and stacked windows, ignores completed checks after the parent sheet or Activity closes, and dismisses any outstanding confirmation when the parent sheet goes away. No Firebase scoring or server authorization code changed. Original GPL and complete-source release conditions remain required.


### Mission 06 — original Telegram BottomSheet scrim shared between exam sheets

Verified owner-approved Telegram `BottomSheet.java:135-192,213-222,1766-1778` source atom remains the existing `TelegramBottomSheetSourceScrim` drawing class, with original AA/SRC black inset paint and alpha=51. The unchanged `ValueAnimator` 400ms `EASE_OUT_QUINT`, 20ms delay, no `FLAG_DIM_BEHIND`, reduced-motion snap and detach cancellation move into the one EVE-only `TelegramBottomSheetSourceBackdrop.install` host adapter. Both Target Exams and Completed Exam call this original-source-backed adapter after showing their Material BottomSheetDialog. Existing chip selection/target preference persistence, completed-result review, reattempt authorization, callbacks and Material detents remain unchanged. This remains a **source-backed scrim subset**, not an original Telegram full BottomSheet drag/nested/IME renderer. Upstream GPL-source and distribution requirements apply.


### Mission 06 — Settings account-deletion confirmation window ownership

The existing EVE-owned Settings Delete Account confirmation uses the previously original-source-backed shared `showDestructive()` adapter (approved original Telegram `AlertDialog.java:302-318,680-693,809-821,874-878,1137-1204`, `DarkAlertDialog.java:11-19`). For the source-verified Telegram Activity safety contract, `AndroidUtilities.java:6255-6267` requires `!isDestroyed && !isFinishing`. The Settings confirmation now enforces that exact lifecycle guard plus a single active destructive dialog, clears its Activity-owned dialog on dismissal, and dismisses it during Activity destruction. The existing 280ms protection, 6000ms auto-dismiss, authenticated Firebase deletion/reauthentication, original source-backed nine-patch and destructive button colors/callbacks remain unchanged. This is a lifecycle adapter, not a full original Telegram AlertDialog/animation implementation.

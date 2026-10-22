# Telegram Android icon binary geometry audit

Source: user-supplied Telegram-master (1).zip (SHA-256 0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3).
Each row below comes from inspecting the **actual unmodified archive** with Pillow and SHA-256; prefixes of the **mdpi** file SHA-256 are recorded. Density images are binary WebP unless stated (msg_delete xxhdpi uses PNG). All source paths begin with TMessagesProj/src/main/res/drawable-{density}/.
These are source asset measurements, NOT an EVE icon replacement or guarantee of identical tint, interaction or licensing.

| Telegram asset | EVE semantic candidate | mdpi | hdpi | xhdpi | xxhdpi | mdpi SHA-256 prefix |
|---|---|---|---|---|---|---|
| ic_ab_back | ic_back | 24x24 | 36x36 | 48x48 | 72x72 | dc131a7de3faea33 |
| ic_ab_other | ic_more_vert | 24x24 | 36x36 | 48x48 | 72x72 | 1753372e5d4e9da5 |
| msg_search | ic_search | 24x24 | 36x36 | 48x48 | 72x72 | c31d2043aec83ca8 |
| msg_delete | ic_delete | 24x24 | 36x36 | 48x48 | 72x72 | 0701b4a4ebca6417 |
| msg_share | ic_share | 24x24 | 36x36 | 48x48 | 72x72 | 41a6ccb0401fbb20 |
| msg_close | ic_close | 24x24 | 36x36 | 48x48 | 72x72 | a1e61bda529d8b1b |
| msg_edit | ic_edit | 24x24 | 36x36 | 48x48 | 72x72 | 6a0b85bf5c849f5e |
| msg_calendar | ic_calendar | 24x24 | 36x36 | 48x48 | 72x72 | d81f98f5b5a2bb69 |
| msg_pin | ic_pin | 24x24 | 36x36 | 48x48 | 72x72 | e8ef8bb01e95108a |
| msg_warning | ic_warning | 2x9 | 3x14 | 4x19 | 6x28 | c96d41db3c886e8c |

Source density factors are 1x,1.5x,2x,3x and 24dp assets have measured 24/36/48/72-pixel squares; 24dp vector paths previously drawn at uniform 2.75dp strokes are not pixel-exact replacements for these PNG/WebP rasters. `msg_warning` is 2x9px at mdpi (not the expected 24x24px), proving a filename similarity is insufficient for mapping. Do not substitute it for EVE's semantic warning icon.

**GPL/redistribution blocker:** Telegram officially describes Android source as GPL v2-or-later (https://telegram.org/apps). EVE currently includes proprietary Firebase/Google SDK integrations and has no declared project-wide redistribution license in the root, so a complete corresponding-source and compatibility review is still required before bundling Telegram-copyrighted icon art in APKs. Apache-2.0 code can be combined with GPLv3 in principle but that observation alone does not resolve every dependency or asset's terms. Keep the separately licensed Google Roboto Medium font already bundled; do not copy Telegram glyph bitmap binaries on this finding alone.

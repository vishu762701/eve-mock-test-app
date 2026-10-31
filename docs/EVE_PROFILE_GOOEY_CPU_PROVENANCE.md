# Profile gooey CPU source fallback — 2026-10-31

**Mission FID-04 status: PARTIAL. This is a source-derived CPU fallback; not 100% Telegram screen parity.**

Upstream revision: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. Original `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java:142-254` uses a 6x downscaled `ARGB_8888` buffer and two crossfade paints. `TMessagesProj/jni/image.cpp:620-871` implements Telegram's triangular two-pass four-channel stack blur. Local ZIP `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; `EVE_MANIFEST.sha256` absent; official content tree authenticated in `THIRD_PARTY_NOTICES.md`.

| Property | Original source | EVE implementation | Classification |
|---|---|---|---|
| Software fallback exists (not a plain draw) | `ProfileGooeyView.java:142-254` | `app/src/main/java/com/eve/app/ui/profile/ProfileGooeyView.kt` `CPUImpl` | SOURCE-DERIVED, Android Kotlin ADAPTED |
| Optimized 120dp x 220dp, downsample 6 | `ProfileGooeyView.java:148-183` | `CPUImpl.onSizeChanged` | EXACT numeric values; zero-size guard ADAPTED |
| ColorMatrix and SRC_ATOP composition | `ProfileGooeyView.java:154-166,231-239` | `CPUImpl.bitmapPaint/bitmapPaint2/draw` | EXACT numeric matrix and modes, Android APIs ADAPTED |
| Cutout circle/path/rounded fallback; no-notch bar | `ProfileGooeyView.java:208-225` | `CPUImpl.draw` | EXACT branch structure; actual notch coordinate context UNKNOWN |
| Blur activation and alpha | `ProfileGooeyView.java:189-190,228-229,242-253` | `CPUImpl.draw` | EXACT formulas, original native blur call replaced by Kotlin port |
| Four-channel stack blur weighting and edge replication | `TMessagesProj/jni/image.cpp:642-863` | `ProfileStackBlur.blurArgb` | EXACT triangular convolution formulas, ARGB packing ADAPTED |
| Native RGBA byte-level identity | `image.cpp:620-871` directly locks platform pixels | EVE `Bitmap.getPixels/setPixels` converts to Android ARGB | **ADAPTED / not bitwise exact**, especially semi-transparent premultiplication |
| Profile gooey full-window host and real cutout path | Telegram `ProfileActivity.java:5354-5359` match-parent avatar host | EVE `activity_profile.xml:65-95` 120dp wrapper | **NOT PORTED; high-priority structural mismatch** |

Tests: JVM `ProfileStackBlurTest` checks 1D radius-1 triangular convolution, 4 RGBA channels, fixed colors, radius-zero no-op and presence of source CPU composition. `ProfileStackBlurNativeTest` runs Bitmap bridge comparison only when instrumentation actually executes. FAST compilation alone is not native validation.

**Production impacts:** no backend, account identity, Firestore rules, server data, D1, Firebase, navigation or API changes; no production deployment. GPL source notices remain in `ProfileGooeyView.kt` and `THIRD_PARTY_NOTICES.md`.

**Remaining:** Android API 24-36 physical/emulator frame test, cutout geometry/height mismatch, 60/90/120Hz motion, ProfileActionsView Canvas renderer, live bio/presence contract, official Apple asset permissions and final six-job FULL. Do not call mission complete.

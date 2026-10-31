# EVE Profile Gooey — source-exact end-scale and status bar inset

Scope: Profile avatar-to-cutout numeric behavior. This update does **not** establish real device camera transition parity.

Source identity: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, verified uploaded `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; upstream `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java` and `AndroidUtilities.java`. `EVE_MANIFEST.sha256` ABSENT; GPL notices in `THIRD_PARTY_NOTICES.md`.

| Property | Telegram source | EVE source | Classification |
|---|---|---|---|
| No-cutout status offset uses real statusBarHeight | `ProfileGooeyView.java:66-72`, `AndroidUtilities.statusBarHeight` | `app/src/main/java/com/eve/app/ui/profile/ProfileGooeyView.kt` `getEndOffset` / `statusBarHeightPx` | EXACT intent, ADAPTED AndroidX insets and resource fallback |
| Circular cutout end scale width minus 2dp / 100dp, ceiling 0.8 | `ProfileGooeyView.java:74-85` | `ProfileGooeyView.kt` `getAvatarEndScale` | EXACT numeric property, removed non-source 0.2 floor |
| Cutout metadata in compact avatar wrapper | `ProfileActivity.java:5354-5359` full-screen host, `ProfileGooeyView.java:117-126` | EVE `activity_profile.xml:65-95` 120dp avatar host and `ProfileGooeyView.kt:onSizeChanged` width > height gating | **INCOMPLETE/STRUCTURAL BLOCKER**: a compact avatar wrapper often rejects actual cutout data. Requires dependency-complete full-window host/insets repair, not pretending a math-only patch fixes it |
| Effective avatar motion after scroll | Official `ProfileActivity.java` renderer and scroll callback | `ProfileActivity.kt:124-153` | ADAPTED / native 60-120Hz motion UNKNOWN |

Source and AndroidTest contracts:
- `ProfileGooeyInsetGeometryContractTest` statically checks source numeric intent and dynamic inset API.
- `ProfileGooeyInsetGeometryNativeTest` checks a synthetic 12dp circular cutout scale (0.1, rather than 0.2) and no-cutout measured status offset when actually run on device.
- FAST runs JVM tests / AndroidTest source compilation. This document does not claim AndroidTest device execution.

**Unmodified:** Backend/schema, auth, activity navigation, profile data, other screens. No production deployment. Final source-matched native screenshots, true full-window header geometry, CPU/GPU hardware frame comparison and six-job FULL are pending.

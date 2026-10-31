# EVE Profile camera-notch host — source-backed window layout

**FID-04 STATUS: PARTIAL. Android compile and native source tests alone do not prove 100% visual or gesture parity.**

Reference: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. Owner-supplied `Telegrammaster(5).zip` locally SHA-256-checked: `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`. `EVE_MANIFEST.sha256` is **ABSENT**; official source tree identity/GPL notices in `THIRD_PARTY_NOTICES.md`.

**Root cause verified from current EVE source:** `activity_profile.xml:65-95` nested the original 120dp-high avatar shell directly around `ProfileGooeyView`; EVE `ProfileGooeyView.kt:onSizeChanged` (following upstream `ProfileGooeyView.java:117-126`) discards notch data whenever the view is wider than tall. A phone-width, 120dp-high renderer will normally satisfy that rejection condition even in portrait. Upstream `ProfileActivity.java:5354-5359` instead attaches `avatarGooey` as `MATCH_PARENT` within `avatarContainer2`, itself a full-window content host.

| Property / interaction | Original Telegram | EVE mapping | Result |
|---|---|---|---|
| Full-window gooey view and 100dp avatar child | `ProfileActivity.java:5354-5359` | `app/src/main/res/layout/activity_profile.xml`: root-level `avatarGooey` with `avatarContainer` | Source geometry **EXACT** for match-parent host and 100dp avatar; wrapper is ADAPTED |
| Keep photo taps & scroll gesture ownership | Telegram parent RecyclerListView/gesture dispatch | EVE `avatarTouchTarget` inside `NestedScrollView`; `ProfileActivity.kt:setupActions` opens existing photo picker | **ADAPTED** to existing EVE navigation; no fake new click callback |
| Accessibilty virtual node | `ProfileActionsView.java:1247+`, Telegram avatar semantics | EVE scroll placeholder is focusable/clickable and described as Profile photo; renderer subtree is hidden from accessibility to avoid duplicates | **ADAPTED**; TalkBack runtime UNKNOWN |
| Camera masking outside status-bar padding | Telegram full-window avatar host | `ProfileActivity.kt:setupAvatarWindowHost` turns off Profile-only parent clipping, translates overlay up by real status inset while scroll retains original inset policy | **ADAPTED**, cutout-specific device frames UNKNOWN |
| Camera target center | `ProfileGooeyView.java:397-405` uses `bounds.bottom - bounds.width()/2f` for circular camera | `ProfileActivity.kt:setupScrollAndGooey` and `ProfileAvatarTrajectory.kt` | Cutout geometry **EXACT** target; EVE scroll-to-target interpolation **ADAPTED** |
| No-cutout fallback | `ProfileGooeyView.java:66-72` dynamic status height | EVE `ProfileActivity.kt` legacy scrolling displacement + corrected `ProfileGooeyView.getEndOffset` | **ADAPTED** to EVE scroll architecture |
| Internal mask GPU/CPU | `ProfileGooeyView.java:142-482` | Existing source-backed EVE GPU and CPU ports, separate provenance | **PARTIAL**, no native image comparison |

Files and impact:
- `activity_profile.xml`: real avatar moves into the root-level full-screen gooey surface behind scrolling content; a 120dp placeholder remains solely for gesture targeting and layout height. Other profile sections and row IDs remain unchanged.
- `ProfileActivity.kt`: retains existing photo picker, loading, data, settings and source camera interpolation; places full-window avatar relative to the actual status bar instead of assuming a constant camera offset.
- `ProfileAvatarTrajectory.kt`: deterministic pure interpolation, source camera target with EVE-adapted movement.
- `ProfileAvatarWindowHostContractTest`: JVM hierarchy/caller/trajectory regression.
- `ProfileAvatarWindowHostNativeTest`: Day/Night inflation and accessibility hierarchy (must be **executed** in native FULL; FAST compiles but does not run it).

**Known risks/remaining acceptance:** status-bar cutout metadata returned from OEM resources can differ from visible `DisplayCutout`; runtime cutout and landscape checks, photo touch/drag flings, scrolling under the toolbar, rotation, RTL, 200% font, 60/90/120Hz matched videos and TalkBack still require hardware/emulator verification. Original Telegram `ProfileActionsView` drawing physics remains separate. No backend, schema, login, navigation, storage or production deploy changes.

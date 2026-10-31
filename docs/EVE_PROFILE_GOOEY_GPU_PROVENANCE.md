# EVE Profile gooey GPU pipeline — 2026-10-31

**Scope:** Source-backed GPU liquid-mask and blur compositing in the EVE profile avatar-to-camera transition, **NOT full-screen Telegram fidelity**.

**Original source:** Official `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`:
`TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java` and `org/telegram/messenger/AndroidUtilities.java`. Uploaded `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10` verified locally; `EVE_MANIFEST.sha256` ABSENT; provenance/license in `THIRD_PARTY_NOTICES.md`.

| Property | Original source lines | EVE location | Classification |
|---|---|---|---|
| Image alpha, unclamped + clamped | `ProfileGooeyView.java:342-348`; `AndroidUtilities.java:5206-5207` raw `ilerp` | `ProfileGooeyMotionMath.kt:8-9`, `ProfileGooeyView.kt` GPU | **EXACT numeric math**, Android plumbing ADAPTED |
| Independent blurred source buffer | `ProfileGooeyView.java:350-355` | `ProfileGooeyView.kt` GPU blurNode | **EXACT formula and stage**, adapted language |
| Source alpha mask before liquid cusp | `ProfileGooeyView.java:358-366,381-390` | `ProfileGooeyView.kt` effectNode | **EXACT composition order**, unverified rendered pixels |
| Liquid cusp rises AND falls to zero | `ProfileGooeyView.java:367`; `AndroidUtilities.java:5079-5082` piecewise lerp | `ProfileGooeyMotionMath.kt:12-14`; GPU avatar and notch paths | **EXACT math**; replaces prior monotonic clamp |
| Circular-notch / true-path / rounded fallback + no-notch bar | `ProfileGooeyView.java:392-432` | `ProfileGooeyView.kt` effectNotchNode | **EXACT branch semantics**; actual cutout detection still ADAPTED |
| Mask alpha-filter stage and blurred crossfade | `ProfileGooeyView.java:434-482` | `ProfileGooeyView.kt` canvas saveLayer + filter + RenderNodes | **EXACT pipeline order**, on-screen parity UNKNOWN |
| CPU fallback | `ProfileGooeyView.java:142-254` | EVE `ProfileGooeyView.kt:184+` | **NOT PORTED**; low-end/disabled-animation rendering still not source-complete |
| Performance-class selection | `ProfileGooeyView.java:56-62` | EVE `ProfileGooeyView.kt:45-103` | **ADAPTED**, app uses memory/core/reduced motion heuristic |
| Avatar placement and scroll callback | Telegram `ProfileActivity.java` source-driven hierarchy and dynamics | EVE `ProfileActivity.kt:124-153` | **ADAPTED**, not yet proven equal in real device |
| Real device screenshot parity | Reference frames / 60-120 Hz / status and cutout insets | No native execution at intermediate FAST | **UNKNOWN** |

**Tests:** `app/src/test/java/com/eve/app/ui/profile/ProfileGooeyMotionContractTest.kt` validates upstream 0/0.25/0.5/0.75/1 motion values, image alpha and all essential GPU shader branches. These JVM tests do **not** render the GPU. FAST CI compiles the application and tests; final native FULL and matched upstream-vs-EVE video frames are still required.

**Unmodified:** Backend, account identity, avatar photo storage, application actions, category, Day/Night resources, and security. Licensed-derived implementation provenance: Telegram source GPL version 2 or later, original notices and distribution obligations preserved.

**Remaining high-impact gaps:** true CPU software blur, notch-safe system status-bar offset, verified screen-specific animation curves, RTL/large font and hardware-accelerated native screenshot comparisons; full `ProfileActionsView` Canvas renderer is separate work.

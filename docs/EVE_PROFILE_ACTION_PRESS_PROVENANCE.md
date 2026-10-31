# EVE Profile action press physics — Telegram source mapping (2026-10-31)

Status: **SOURCE-EXACT press/release constants and curves, ADAPTED Android View touch routing. Not complete original ProfileActionsView rendering.**

Genuine source: verified `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`, independently authenticated against `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` as recorded in `THIRD_PARTY_NOTICES.md`; `EVE_MANIFEST.sha256` **ABSENT**. Source file Git blobs:
- `TMessagesProj/src/main/java/org/telegram/ui/Components/ButtonBounce.java`: `0bfa7b714e9bcbf6f4a1ac9955f98adb416f37f6`
- `Components/ProfileActionsView.java`: `ce74c418eb851e1c4234e1a5abedb6e98f85321a`
- `Components/CubicBezierInterpolator.java`: `3a9cae100e17a03abacf2f4df71db2b82757b968`

| Property | Telegram source | EVE property | Fidelity |
|---|---|---|---|
| Press 60ms with DEFAULT cubic Bézier (0.25,0.1,0.25,1) | `ButtonBounce.java:60–85`; `CubicBezierInterpolator.java:11` | `ProfileActionBounce.kt` `PRESS_DURATION_MS`, `PathInterpolator` | **EXACT curve and duration** |
| Release 350ms, overshoot tension 5 | `ButtonBounce.java:86–92` | `ProfileActionBounce.kt` `RELEASE_DURATION_MS`, `OvershootInterpolator(5)` | **EXACT** |
| Bounce scale 1 - pressedT * 0.04 | `ButtonBounce.java:124–125`, `ProfileActionsView.java:1189–1190` | `ProfileActionBounce.kt` `SCALE_DIFF=0.04` applied to clickable parent | **EXACT equation**, View transform ADAPTED |
| Re-press cancels old animator | `ButtonBounce.java:61–67` | `ProfileActionBounce.animatePressed` | **EXACT lifecycle principle** |
| Cancel press when finger travels more than 20 pixels | `ProfileActionsView.java:547–553` | `ProfileActionBounce.onTouch` | **EXACT original pixel threshold** |
| Long-press haptic after 250ms; click on release | `ProfileActionsView.java:554–585` | `ProfileActionBounce.onTouch` / `performClick` | **EXACT threshold**, Android View event dispatch **ADAPTED** |
| No taps while collapsed | `ProfileActionsView.java:521–524` | `ProfileActionsCollapse.applyTo` existing disabled state, bounce checks enabled | **ADAPTED**; view layout vs source Canvas region |
| EVE action destinations | No Telegram auth/identity dependencies copied | `ProfileActivity.kt:setupActions` photo picker, ChangeNameActivity, SettingsActivity untouched | **EVE INTEGRATION**, preserved |
| Touch/pointer interruption, detach lifecycle | Original hit-based `ProfileActionsView.java:520–587` | Cancel on ACTION_CANCEL/pointer-down; reset on view detach | **ADAPTED** safety handling |
| Source Canvas backgrounds, filled/outline switching, virtual accessibility nodes, RenderNode blur | `ProfileActionsView.java:238–464,1247+` | Existing 3 XML buttons and background surface | **NOT SOURCE-EXACT; still outstanding** |

- Production code: `app/src/main/java/com/eve/app/ui/profile/ProfileActionBounce.kt`; `ProfileActivity.kt:setupProfileActionBounce`.
- JVM contract: `ProfileActionBounceSourceTest` preserves original constants and bound actions. Native test **source**, `ProfileActionBounceNativeTest`, checks Day/Night valid taps, cancellation, drift and disabled hits. FAST compiles AndroidTest, it **does not execute** it.
- No changes to backend, Firestore, scoring, Firebase auth, phone, user metadata or production deployments. Existing button foreground ripple remains EVE-specific and is **ADAPTED**, not source parity. Original GPL notices apply; see `THIRD_PARTY_NOTICES.md`.

Remaining: original `ProfileActionsView` renderer fully dependency-isolated, exact official filled/outline resources/tint/motion, matched emulator/physical-device video, Bio/presence data contract with safe production rollout, official Apple bell permissions, full app UI matrix, terminal six-job FULL and APK hash.

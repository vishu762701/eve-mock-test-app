# EVE Profile action collapse stages — 2026-10-31

Status: **SOURCE-EXACT NUMERIC FADES, ADAPTED VIEW INTEGRATION. NOT complete ProfileActionsView port.**

Reference source is official `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`, `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java`. User-supplied `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; original source tree verified as described in `THIRD_PARTY_NOTICES.md`; `EVE_MANIFEST.sha256` ABSENT.

| Property | Telegram source location | EVE implementation | Fidelity |
|---|---|---|---|
| Profile scroll derives action-visible height and position | `ProfileActivity.java:11253-11270` (`height=min(dp74,bottom-newTop)`) | `ProfileActivity.kt:setupScrollAndGooey` with `ProfileActionsCollapse.atScroll` | **ADAPTED**, EVE uses NestedScrollView scrollY rather than Telegram extraHeight |
| Row height, top/bottom insets | `ProfileActionsView.java:128-141,247-251` (`74dp`, `top=8dp`, `ypadding=12dp`, target active area `54dp`) | `app/src/main/java/com/eve/app/ui/profile/ProfileActionsCollapse.kt` numeric height/fraction | **EXACT numerical factors**, original composition DIFFERENT |
| Background staged fade | `ProfileActionsView.java:292-317`: `clamp01((fraction-.2)/.8)` | `ProfileActionsCollapse.backgroundAlpha` and `applyTo` foreground surface opacity | **EXACT equation**, XML background drawable ADAPTED |
| Text/image staged fade and scale | `ProfileActionsView.java:333-337,407-437`: `clamp01((fraction-.4)/.6)` plus `canvas.scale(fraction, fraction, cx, cy)` | `ProfileActionsCollapse.contentAlpha`, `applyTo` child icon/text alpha/scale | **EXACT equation**, View child pivots ADAPTED |
| Collapsed hit gate | `ProfileActionsView.java:521-524`: no taps if `currentHeight<dp8` | `ProfileActionsCollapse.touchEnabled` -> three buttons `isEnabled` | **EXACT numeric threshold**, standard Android event dispatch ADAPTED |
| Collapsed action alpha previously linear | Old `ProfileActivity.kt:151` (`1-progress*1.5`) | Replaced with staged math, preserving name/status independent alpha | **CORRECTED** partial fidelity |
| Source canvas actions / bounce / inset physics | `ProfileActionsView.java:258-337,386-458,520-587,1001+` | Current XML action layout and existing click handlers | **NOT PORTED; BLOCKED native parity claim** |

Actions retain Set Photo/Edit Info/Settings behavior, source-backed geometry, same EVE theme colors; no backend/identity/media/navigation change, no production deployment. Target unit test `ProfileActionsCollapseTest` validates all source math and scroll hookup; native `ProfileActionsCollapseNativeTest` validates Day/Night inflated surfaces and touch/accessibility states **only when executed**. FAST CI compiles AndroidTest but does not run a device.

Remaining FID-04: full Telegram `ProfileActionsView` renderer (original icon filled/outline colors, ButtonBounce physics, long press haptic, virtual accessibility), upstream-matched motion frames, RTL/large-font, camera notch cutout validation. Also personal Bio/real presence backend contract, Apple bell rights, app-wide parity and terminal FULL remain pending.

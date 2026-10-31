# Profile actions accessibility semantics — Telegram source mapping (2026-10-31)

**Status: SOURCE-ALIGNED semantics; ADAPTED native-View representation, NOT a verbatim Telegram AccessibilityNodeProvider.**

Original source identity: locally validated `Telegrammaster(5).zip` SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`, matching official `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`. Original `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java:1247-1356`, Git blob `ce74c418eb851e1c4234e1a5abedb6e98f85321a`. `EVE_MANIFEST.sha256` ABSENT. GPL obligations recorded in `THIRD_PARTY_NOTICES.md`.

| Accessibility property | Telegram source | EVE implementation | Classification |
|---|---|---|---|
| Each active action has one focusable clickable accessibility target | `ProfileActionsView.java:1253–1287` virtual child nodes | `ProfileActionAccessibility.install` one native `LinearLayout` accessibility node per existing action | **ADAPTED equivalent native View semantics** |
| Button class | `ProfileActionsView.java:1288` `android.widget.Button` | `ProfileActionAccessibility.kt` override node className | **EXACT class string** |
| Name from current action text | `ProfileActionsView.java:1290` | native action label `TextView.text` -> parent `AccessibilityNodeInfo.text` | **EXACT value contract**, app-specific labels |
| Click accessibility action | `ProfileActionsView.java:1282-1286,1324-1328` | `ACTION_CLICK` node on enabled buttons, existing native `performClick` handler | **ADAPTED dispatch** |
| Disabled/collapsed actions inaccessible to click | `ProfileActionsView.java:521-524` hit gate | `ProfileActionsCollapse.applyTo` disables clickable view and delegate removes node click action | **ADAPTED safe gate** |
| Avoid duplicate label focus | Telegram exposes virtual action not separate text child | EVE label child set `IMPORTANT_FOR_ACCESSIBILITY_NO`, parent `YES` | **ADAPTED** |
| Original exact virtual bounds / focus event synthesis | `ProfileActionsView.java:1292-1302,1334-1350` | Android-native view bounds/host focus events | **NOT PORTED**; native runtime parity unknown |

Implementation: `app/src/main/java/com/eve/app/ui/profile/ProfileActionAccessibility.kt`; wired after source-derived action bounce in `ProfileActivity.kt`. Existing action click destinations and backend/auth invariants unchanged. Unit test `ProfileActionAccessibilitySourceTest.kt`; native Day/Night source `ProfileActionAccessibilityNativeTest.kt` checks actual `AccessibilityNodeInfo` class, text, focusability, click, and disabled behavior **when executed**, not merely compiled by FAST.

Remaining: full Telegram ProfileActionsView Canvas/camera integration, TalkBack hardware navigation and restoration checks, source-matched screenshots/motion, Bio/presence backend safe staging, official Apple bell redistribution rights, app-wide source map, final six-job FULL and APK SHA-256.

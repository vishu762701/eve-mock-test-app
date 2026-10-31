# Profile action icon luminance source correction — 2026-10-31

**Scope:** Fix original Telegram's **button-color** icon variant decision; no unrelated palette, action destination, or image assets changed.

| Property | Original upstream source | EVE implementation | Classification |
|---|---|---|---|
| Perceived brightness | `Telegram-master/TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java:4925–4926`: `(red*0.2126 + green*0.7152 + blue*0.0722)/255` | `app/src/main/java/com/eve/app/ui/profile/ProfileActionIcons.kt:isLightButtonColor` | **EXACT numeric formula** |
| Outline vs filled switch | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java:185–186,412–418`: light iff brightness strictly `>0.72`; exact black outline for light surface, white filled for dark | `ProfileActionIcons.resourcesForColor`, applied in `ProfileActivity.onApplyThemePalette` | **EXACT threshold / stable endpoints**, icon crossfade ADAPTED |
| Effective surface color | Telegram `ProfileActionsView.setActionsColor(color,hasColorById)` dynamic avatar-derived color | EVE actual `R.color.category_pill_unselected_bg` Day/Night button surface via `ContextCompat.getColor` | **ADAPTED** because EVE doesn't implement avatar-derived dynamic color |
| Vector pixels/paths | `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15` `filled_profile_*.xml` and `outline_profile_*.xml` | Existing six byte-exact resources, hashes mapped in `docs/EVE_PROFILE_ACTION_ICON_VARIANTS_PROVENANCE.md` | **EXACT bytes** |

Earlier EVE chose filled/outline only from `palette.isDark`. That coincidentally matched current fixed Day/Night surfaces, but contradicted the source brightness contract and would fail with changed button-surface colors. Test cases now include source threshold just below/above 0.72, dark and bright endpoint values. Night/Day instrumentation test reads the resolved resource color rather than the mode flag.

**Source archive:** user-uploaded `Telegrammaster(5).zip` independently SHA-256-verified in this conversation as `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10`; prior `EVE_MANIFEST.sha256` ABSENT. GPL notices remain in `THIRD_PARTY_NOTICES.md`. Does **not** establish complete Telegram source fidelity, animated parentExpanded profile photo crossfade, or real-device screenshot match. No backend/Worker production changes.

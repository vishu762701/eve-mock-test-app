# EVE Profile action-row Telegram source geometry — 2026-10-31

Status: **PARTIAL; STATIC PROPERTY PORT, NOT A COMPLETE TELEGRAM RENDERER.**

Official reference: `DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15`; content of uploaded `Telegrammaster(5).zip`, SHA-256 `4b65e10b8fc63b4cbcb7377026a5da470782547fc0c0575bf0b71da676028c10` (local archive actually hashed). The archive does **not** contain `EVE_MANIFEST.sha256`; independent upstream file-tree verification and GPL license obligations are documented in `THIRD_PARTY_NOTICES.md`.

Original source: `TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java` at this immutable revision.

| Property | Original lines and value | EVE destination | Classification |
|---|---|---|---|
| My-profile action keys | 83-109, 631-647: `KEY_SET_PHOTO`, `KEY_EDIT_INFO`, `KEY_SETTINGS` | `app/src/main/res/layout/activity_profile.xml` IDs `btnSetPhoto`, `btnEditInfo`, `btnSettings`; `ProfileActivity.kt:155-180` existing click behavior | ADAPTED: preserve app photo picker, name editor, settings |
| Action view 74dp total | 128-141, 217-222: caller creates 74dp-high renderer; 8dp top + 54dp active box + 12dp bottom | `activity_profile.xml` `layoutActions` row height and padding, three 54dp inner views | EXACT numeric dimensions; XML layout ADAPTED |
| Row inset 14dp | 134, 203-205, 253-270: horizontal inset `xpadding=14dp` | `activity_profile.xml` `layoutActions` marginHorizontal 14dp | EXACT numeric inset; layout ADAPTED |
| Inter-action gap 7dp | 203-205, 253-270: `betweenPadding=xpadding / 2` | `activity_profile.xml` 3.5dp margin pair on adjacent actions | EXACT arithmetic gap; XML edge rounding ADAPTED |
| Corner radius 16dp | 512-514: `getRoundRadius()=dp(16)` | `app/src/main/res/drawable/telegram_profile_action_surface.xml` | EXACT numeric radius; Android drawable ADAPTED |
| Label 11sp | 1029-1033: `new Text(cs, 11, AndroidUtilities.bold())`, up to 3 lines, text scaling | `telegram_typography.xml` `ProfileAction.Source` role 11sp, applied to `activity_profile.xml` labels with two-line ellipsis | EXACT base size, ADAPTED line breaking/scale |
| Icons 24dp | 386-401, 1215-1234: 24dp action icon with filled/outline variants | Existing EVE `telegram_profile_photo/edit/settings` references | Icon dimension EXACT; icon-state switching NOT VERIFIED |
| Background, scrim, bounce and motion | 176-215, 247-337, 407-504, 520-587, 994-1213: dynamic paints, alpha/scale and `ButtonBounce` | XML surface and selectable foreground | ADAPTED / NOT SOURCE-EXACT |
| Font scale, RTL, Night, accessibility | Original Canvas text/virtual accessibility node, 1247+ | EVE TextViews, clickable focusable groups, theme token | ADAPTED; native device acceptance UNKNOWN |
| 16dp vertical margin above actions | Profile EVE composition, not `ProfileActionsView` | `activity_profile.xml` `layoutActions` marginTop | EVE-only; not attributed to official source |

This change **does not transplant the original 1,356-line `ProfileActionsView.java`**. That class requires dependency isolation for its icons, `AnimatedFloat`, `ButtonBounce`, `Text`, `RLottieDrawable`, gesture dispatch and virtual accessibility nodes, plus Profile header integration. Do not claim 100% appearance, animations or touch parity.

Regression proof requested: `ProfileActionsGeometryContractTest` JVM source test; `ProfileActionsGeometryNativeTest` Day/Night inflation, native layout and focus/click metrics. Compilation by FAST is **not** native test execution. Native motion and source-matched screenshots remain mandatory before final acceptance.

No backend, production deployment, credentials, auth, photo storage, navigation destination or other screen style token changed. Source is GPL v2 or later per original file notice; preserve attribution and distribution obligations.

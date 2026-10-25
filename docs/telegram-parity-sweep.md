# Telegram parity sweep

Source is the owner-supplied reference extracted outside git. Telegram paths below
are paths inside that archive, not files committed to EVE. The original archive
provenance SHA-256 is `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`.
The repacked UI/security ZIP used here has SHA-256
`fabf0b7370abf2ee5661063ea1c274cb3e537048f008a6871ff3169cbe89cb1e`.
Its manifest verification is separate from runtime fidelity.

## A1 — Loading circles

| Property | EVE file:line | Telegram file:line | Value | Action |
|---|---|---|---|---|
| Profile progress dialog | `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt:327` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | 40dp arc, 3dp round stroke | Replaced platform spinner; retained dialog/callback behavior |
| Settings progress dialog | `app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt:300` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | 40dp arc, 3dp round stroke | Replaced platform spinner; retained dialog/callback behavior |
| Edit question reformat binding | `app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt:373` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | TelegramRadialProgressView | Fixed stale ProgressBar cast to match existing XML |
| System Monitor | `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt:44` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | TelegramRadialProgressView | Already migrated; removed unused ProgressBar import |
| Shared loading container | `app/src/main/java/com/eve/app/designsystem/DesignLoadingView.kt:13` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | TelegramRadialProgressView child | Already migrated; retained full-screen container API |
| activity_main.xml | `app/src/main/res/layout/activity_main.xml:52` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| activity_history.xml | `app/src/main/res/layout/activity_history.xml:15` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| activity_manage_existing_exams.xml | `app/src/main/res/layout/activity_manage_existing_exams.xml:21` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| activity_test.xml | `app/src/main/res/layout/activity_test.xml:14` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | DesignLoadingView → TelegramRadialProgressView | Already migrated through shared child; preserve hierarchy and state IDs |
| Generic loading style | `app/src/main/res/values/design_widgets.xml:107` | `Telegram-master/TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java:63–71` | Sizing-only style | Removed obsolete platform ProgressBar parent; leaf owns painting |
| Poll result percentage | `app/src/main/java/com/eve/app/ui/admin/PollsAdminAdapter.kt:207`; `app/src/main/res/layout/item_poll_option_result.xml:7` | NOT FOUND: EVE poll metric counterpart | Determinate, max 100 | Preserved; not an indeterminate spinner |
| Result metric item_answer.xml | `app/src/main/res/layout/item_answer.xml:50` | NOT FOUND: EVE exam metric counterpart | Determinate | Preserved |
| Result metric item_topic_stat.xml | `app/src/main/res/layout/item_topic_stat.xml:7` | NOT FOUND: EVE exam metric counterpart | Determinate | Preserved |

The complete production sweep leaves four Material circles, all explicitly
`android:indeterminate="false"`: two answer-time metrics, topic accuracy, and
poll percentage. No platform `ProgressBar(...)` loading constructors remain.
`Widget.Eve.Token.Ring` stays available for these determinate metrics. No
horizontal progress bar was changed. The existing radial leaf has EVE XML,
palette, drawable and static-capture glue; a green build does not prove exact
runtime parity of that adaptation. Its source motion values remain unchanged.

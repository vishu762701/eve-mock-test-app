# Telegram Mission 01 — 95-layout XML inventory

**Repository source SHA:** `19c0d1135b6e8e797593b97022912e55ae5d230a` on `main`. All 95 layout XML files at that SHA were read using the connected GitHub repository. **Telegram reference**: uploaded `Telegram-master (1).zip` with verified SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`. The archive lacks Git metadata: upstream commit **UNKNOWN**.

The table counts literal `<TextView>` nodes and lexical `TextAppearance.Eve.Telegram.` / `TextAppearance.Eve.Token.` occurrences. **This is an XML inventory, NOT proof that all Kotlin/Java-generated text and images, inherited widget styles or indirect resource lookups are converted.** Referenced icons are direct `@drawable/ic_*` occurrences only; dynamic `setImageResource`, `setCompoundDrawables`, custom Paint/Canvas icons, and Lottie/mipmap/Android status icons require separate analysis.

**Measured inventory:** 95 layouts; 578 literal TextViews; 31 direct Telegram-style references; 555 legacy token references; 66 distinct directly referenced `ic_*` resources. *Counts do not identify full visual fidelity; some Telegram role uses live in shared themes rather than XML.*

| Layout | TextViews | Direct Telegram styles | Legacy Token styles | Drawable icon references |
|---|---:|---:|---:|---|
| `activity_about.xml` | 6 | 4 | 2 | ic_back · ic_info |
| `activity_activity_log.xml` | 3 | 0 | 3 | ic_back · ic_refresh |
| `activity_admin.xml` | 30 | 0 | 30 | ic_back · ic_exam_placeholder · ic_edit · ic_robot · ic_image · ic_link · ic_star_filled · ic_syllabus · ic_refresh · ic_users · ic_broadcast · ic_flag · ic_message_dots · ic_campaign · ic_analytics · ic_history · ic_download · ic_maintenance · ic_info |
| `activity_admin_analytics.xml` | 15 | 0 | 15 | ic_back · ic_refresh · ic_bug_report |
| `activity_api_usage.xml` | 12 | 0 | 12 | ic_back · ic_refresh · ic_info |
| `activity_app_config.xml` | 9 | 0 | 9 | ic_back |
| `activity_bookmarks.xml` | 5 | 0 | 6 | ic_back |
| `activity_content_display.xml` | 10 | 0 | 10 | ic_back · ic_email · ic_phone · ic_language |
| `activity_edit_about.xml` | 1 | 0 | 1 | ic_back · ic_save |
| `activity_edit_exam.xml` | 7 | 0 | 7 | ic_back |
| `activity_exam_tests.xml` | 2 | 0 | 2 | ic_back |
| `activity_feedback.xml` | 3 | 0 | 3 | ic_back |
| `activity_feedback_messages.xml` | 1 | 0 | 1 | ic_back · ic_refresh |
| `activity_flagged_questions.xml` | 2 | 0 | 2 | ic_back · ic_refresh |
| `activity_generated_tests.xml` | 3 | 0 | 3 | ic_back · ic_refresh |
| `activity_history.xml` | 5 | 0 | 5 | ic_back |
| `activity_leaderboard.xml` | 11 | 0 | 11 | ic_back |
| `activity_login.xml` | 5 | 0 | 5 | ic_back · ic_social_google |
| `activity_main.xml` | 22 | 0 | 23 | ic_user_profile · ic_search · ic_notification · ic_more_vert · ic_back · ic_close · ic_flame_line · ic_person · ic_history · ic_bookmark · ic_performance_increase · ic_leaderboard_star · ic_syllabus · ic_info · ic_settings · ic_maintenance · ic_refresh |
| `activity_manage_exams.xml` | 15 | 0 | 15 | ic_back |
| `activity_manage_existing_exams.xml` | 3 | 0 | 3 | ic_back · ic_refresh · ic_search |
| `activity_manage_polls.xml` | 1 | 0 | 1 | ic_back · ic_add |
| `activity_manage_premium.xml` | 5 | 0 | 5 | ic_back · ic_refresh · ic_star_filled |
| `activity_manage_syllabus.xml` | 10 | 0 | 10 | ic_back · ic_upload · ic_description · ic_check_circle · ic_close · ic_search · ic_syllabus |
| `activity_manage_users.xml` | 6 | 0 | 6 | ic_back · ic_refresh · ic_search · ic_users |
| `activity_mistakes.xml` | 5 | 0 | 5 | ic_back |
| `activity_notifications.xml` | 3 | 1 | 2 | ic_back |
| `activity_payment_checkout.xml` | 9 | 0 | 9 | ic_back · ic_timer · ic_account_balance |
| `activity_performance.xml` | 9 | 0 | 14 | ic_back |
| `activity_practice.xml` | 5 | 0 | 5 | ic_back |
| `activity_premium.xml` | 11 | 0 | 13 | ic_back · ic_star_outline |
| `activity_profile.xml` | 8 | 0 | 8 | ic_back · ic_person · ic_camera · ic_copy · ic_star_filled · ic_calendar |
| `activity_pyq.xml` | 6 | 0 | 6 | ic_back |
| `activity_result.xml` | 48 | 1 | 51 | ic_back · ic_share · ic_refresh · ic_chevron_down · ic_trophy |
| `activity_result_detail.xml` | 41 | 0 | 41 | ic_back · ic_trophy |
| `activity_send_notification.xml` | 8 | 0 | 8 | ic_back · ic_close · ic_delete · ic_send |
| `activity_settings.xml` | 13 | 12 | 1 | ic_back · ic_bell · ic_trophy · ic_chevron_right · ic_topic · ic_language · ic_delete |
| `activity_syllabus.xml` | 1 | 0 | 1 | ic_back · ic_search |
| `activity_system_monitor.xml` | 1 | 0 | 1 | ic_back |
| `activity_test.xml` | 6 | 0 | 6 | — |
| `bottom_sheet_category_picker.xml` | 12 | 0 | 12 | ic_check_circle |
| `bottom_sheet_completed_exam.xml` | 2 | 0 | 2 | ic_history · ic_refresh |
| `bottom_sheet_payment_method.xml` | 6 | 0 | 6 | ic_qr_code · ic_chevron_right · ic_account_balance |
| `bottom_sheet_target_exams.xml` | 2 | 0 | 2 | — |
| `dialog_add_question.xml` | 1 | 0 | 1 | — |
| `dialog_create_feedback_post.xml` | 2 | 2 | 0 | — |
| `dialog_create_poll.xml` | 4 | 0 | 4 | — |
| `dialog_edit_question_details.xml` | 2 | 0 | 2 | — |
| `dialog_manage_banners.xml` | 5 | 0 | 5 | — |
| `dialog_manual_grant_premium.xml` | 0 | 0 | 0 | — |
| `dialog_post_replies.xml` | 3 | 0 | 3 | — |
| `dialog_reply_feedback.xml` | 5 | 1 | 4 | — |
| `dialog_report_question.xml` | 4 | 0 | 4 | — |
| `item_admin_banner.xml` | 3 | 0 | 3 | ic_link · ic_back |
| `item_admin_email.xml` | 2 | 0 | 2 | — |
| `item_admin_premium_user.xml` | 4 | 0 | 4 | — |
| `item_admin_syllabus.xml` | 4 | 0 | 4 | ic_syllabus · ic_description |
| `item_admin_transaction.xml` | 6 | 0 | 6 | — |
| `item_analytics_exam.xml` | 5 | 0 | 5 | — |
| `item_analytics_question.xml` | 3 | 0 | 3 | ic_edit |
| `item_answer.xml` | 25 | 0 | 21 | ic_status_circle_right · ic_star_bookmark_filled · ic_report_flag_premium · ic_visibility · ic_key · ic_warning |
| `item_audit_log.xml` | 4 | 0 | 4 | — |
| `item_bookmark_card.xml` | 10 | 0 | 10 | ic_star_filled · ic_back |
| `item_category_header.xml` | 1 | 0 | 1 | — |
| `item_exam.xml` | 2 | 0 | 2 | ic_exam_placeholder · ic_pin |
| `item_exam_dropdown.xml` | 1 | 0 | 0 | — |
| `item_exam_test.xml` | 3 | 0 | 3 | — |
| `item_existing_exam_admin.xml` | 6 | 0 | 6 | ic_chevron_right · ic_visibility · ic_edit · ic_more_vert |
| `item_feedback_message.xml` | 5 | 0 | 5 | ic_delete |
| `item_feedback_post.xml` | 4 | 0 | 4 | — |
| `item_flagged_question.xml` | 5 | 0 | 5 | — |
| `item_generated_test.xml` | 5 | 0 | 5 | ic_close |
| `item_history.xml` | 4 | 0 | 4 | — |
| `item_home_banner.xml` | 0 | 0 | 0 | — |
| `item_leaderboard.xml` | 4 | 0 | 4 | — |
| `item_manage_question.xml` | 2 | 0 | 2 | — |
| `item_mistake.xml` | 8 | 0 | 8 | — |
| `item_notification.xml` | 3 | 3 | 0 | ic_bell |
| `item_palette_circle.xml` | 1 | 0 | 1 | — |
| `item_poll_admin.xml` | 6 | 0 | 6 | — |
| `item_poll_option_input.xml` | 0 | 0 | 0 | ic_close |
| `item_poll_option_result.xml` | 3 | 0 | 3 | — |
| `item_post_reply.xml` | 3 | 0 | 3 | — |
| `item_question.xml` | 2 | 0 | 2 | ic_timer · ic_star_outline · ic_report_flag_premium |
| `item_result_topic.xml` | 2 | 0 | 2 | — |
| `item_sent_broadcast.xml` | 4 | 0 | 4 | — |
| `item_student_action.xml` | 0 | 0 | 0 | — |
| `item_syllabus.xml` | 4 | 0 | 4 | ic_download |
| `item_topic_stat.xml` | 3 | 0 | 3 | — |
| `item_user_admin.xml` | 4 | 0 | 4 | ic_more_vert |
| `layout_bulletin.xml` | 1 | 0 | 1 | ic_check_circle |
| `layout_undo_bar.xml` | 1 | 0 | 1 | — |
| `popup_telegram_menu.xml` | 7 | 7 | 0 | ic_theme_sun · ic_history_rounded · ic_bookmark · ic_close · ic_topic · ic_pyq · ic_logout |
| `view_empty_state.xml` | 2 | 0 | 2 | — |
| `view_error_state.xml` | 2 | 0 | 2 | — |

## Telegram source → EVE semantic mapping and completion gates

| Component | Telegram source (ZIP under TMessagesProj/src/main) | Observed variant | EVE target | Status |
|---|---|---|---|---|
| Action bar title | `java/org/telegram/ui/ActionBar/ActionBar.java:522-537,1443-1455`; `java/org/telegram/messenger/AndroidUtilities.java:262-270` | 20dp portrait, 18dp phone landscape, 20dp tablet; bold system 500 if configured, otherwise bundled medium; `key_actionBarDefaultTitle` | `res/values/telegram_typography.xml`, `values-land`, `values-sw600dp-land` | IMPLEMENTED_NOT_TESTED at this inventory baseline; no full SimpleTextView padding/emoji layout |
| Settings row title/details | `java/org/telegram/ui/Cells/TextDetailSettingsCell.java:40-55` | 16dp primary and 13dp gray; RTL gravity and one-line semantics vary with helper | Settings and About | PARTIAL; EVE keeps multiline content where needed |
| Settings section | `java/org/telegram/ui/Cells/HeaderCell.java:94-99` | 14dp medium, themed blue header | Settings | PARTIAL; WCAG day blue differs from Telegram |
| Menu action | `java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:493-504` | 16dp regular, themed submenu label | `popup_telegram_menu.xml` | PARTIAL |
| Search text | `java/org/telegram/ui/ActionBar/ActionBarMenuItem.java:1530-1532` | 18dp regular, `key_actionBarDefaultSearch` | Home search style | PARTIAL |
| Alert dialog title/body | `java/org/telegram/ui/ActionBar/AlertDialog.java:788-795,849-854` | 20dp bold and 16dp regular, `key_dialogTextBlack`; specialized animated dialog variants differ | Shared Material dialog widgets, two feedback modals | PARTIAL; no Telegram-native AlertDialog layout/animation |
| Chat list 17/16/12 paint | `java/org/telegram/ui/ActionBar/Theme.java:7896-7927` | Conversation row name, message and timestamp | EVE notification item | ADAPTED semantic counterpart; not Telegram notification-inbox original |
| Exam question, timer, rank, score | **NO EXACT TELEGRAM COUNTERPART** | Exam-specific dynamic strings, math, count and score state | EVE Mock Test, Result, Admin | PENDING EVE-semantic treatment and full font-scale verification |

## Action icon source register

| EVE action | Telegram resource and observed Java call site | EVE resource | Fidelity and state |
|---|---|---|---|
| Back | `res/drawable-hdpi/ic_ab_back.webp`, `ui/ActionIntroActivity.java:124` | `ic_back.xml` | NOT EXACT; user interaction preserved, GPL pixels not copied |
| Overflow | `ic_ab_other.webp`, `ui/CacheControlActivity.java:1252` | `ic_more_vert.xml` | NOT EXACT; no Telegram art imported |
| Search | `msg_search.webp`, `ui/ChatActivity.java:4465` | `ic_search.xml` | NOT EXACT |
| Delete | `msg_delete.webp`, `ui/Business/BusinessLinksActivity.java:409` | `ic_delete.xml` | NOT EXACT; destructive action semantics preserved |
| Share | `msg_share.webp`, `ui/Business/BusinessLinksActivity.java:399` | `ic_share.xml` | NOT EXACT; EVE-owned share arrow, not Telegram path |
| Edit | `msg_edit.webp`, `ui/Business/BusinessLinksActivity.java:406` | `ic_edit.xml` | PENDING |
| Close | `msg_close.webp`, `ui/DialogsHintCell.java:102` | `ic_close.xml` | NOT EXACT |
| Notification bell | `msg_notifications.webp`, `ui/ProfileActivity.java:14573-14574` | `ic_bell.xml` | NO EXACT TELEGRAM INBOX COUNTERPART |
| Bookmark exam | `msg_saved.webp`, `ui/AudioPlayerAlert.java:2932` | `ic_bookmark.xml` | NO EXACT EXAM BOOKMARK COUNTERPART |
| Settings | `msg_settings.webp`, `ui/BusinessBotButton.java:145` | `ic_settings.xml` | PENDING: Telegram call site has narrower bot context |
| Score/rank/wrong/correct/skipped | No corresponding Telegram exam asset | EVE state graphics | NO EXACT TELEGRAM COUNTERPART; preserve |
| Launcher and status notification small icon | Android-specific app identity/platform constraints | EVE launch/mipmap and notification small icon | Retain EVE branding/platform requirements |

Assets above are static `.webp` lookups and NOT grounds to invent Telegram icon animation. Any separately animated icon needs its actual source drawable/JSON/RLottie trigger and lifecycle proven before replacement. A visual-only 2dp stroke adjustment was tried and subsequently reverted because it had no source-exact Telegram art mapping. The pre-mission EVE icon assets are retained as working fallbacks. No Telegram GPL-derived assets were imported.

## Source/legal/test gating

- Telegram root `LICENSE` states **GNU GPL version 2**; its `README.md:9-15` requests distinct branding and source publication. GPL compatibility with EVE's whole distributed Android build is **BLOCKED**, not automatically solved by the source repository being public. No Telegram art, fonts, native classes or licensed JSON was copied.
- Source units are often `TypedValue.COMPLEX_UNIT_DIP`, whereas EVE deliberately uses `sp` for Android font-scale accessibility; exact px-metric identity is **not claimed**.
- Full layout and dynamic-view visual verification is **UNKNOWN** beyond already recorded CI. Day/night screenshots, 320dp/360dp/tablet, fontScale enlargement, Devanagari/RTL, focus and gesture recordings are required.
- Existing Inter/OFL files and legacy backend typography published configuration must not be deleted until reachability, admin/stored-schema compatibility and source license analysis are complete.
- **Next executable step:** inspect dynamic Kotlin/Java use sites for each icon and text style; confirm CI at **current remote HEAD**, not merely this inventory SHA; then implement remaining source-verifiable components without importing GPL assets until compliance is resolved.


## Source-exact Telegram default medium font integration

The supplied ZIP establishes the actual selection: `SharedConfig.java:317,619` reads `useSystemBoldFont` default **false**; `AndroidUtilities.java:252,262-270` uses bundled `fonts/rmedium.ttf` by default, or system 500 on Android 9+ when user-configured. The upstream official Telegram GitHub file has the identical Git blob `aba251d159c783048b352d17f6581d406f2a6ddb` as the uploaded ZIP; the exact source binary was added as `res/font/telegram_roboto_medium.ttf` and is used for **NavigationTitle, SettingsSection, DialogTitle and NotificationTitle** (including the phone-landscape and tablet-landscape variants). This is a **source-exact bundled 500 font** only for those explicit mapped roles—not a whole-app typography completion claim.

The font has name `Roboto Medium`, weight 500, version 2.138 (2017), embedded copyright `Copyright 2011 Google Inc. All Rights Reserved.`, SHA-256 `c4a17ec40f13be4a229e34893e3e04f19dd190f48b4908e0649402cb2229e9cb`. Google's independent Roboto open-source project documents Apache-2.0 font redistribution; exact full license and copyright/provenance notice ship under `app/src/main/assets/licenses/Roboto-APACHE-2.0.txt` and `Roboto-Medium-NOTICE.txt`. The binary is unchanged. This is independently Google-owned font software, NOT a Telegram-authored GPL visual/code asset. No Telegram GPL icons, RLottie code or animations were copied.

**Remaining deviations**: EVE uses Android `sp` rather than Telegram's `dp` text-size units for accessibility, and numerous legacy EVE shared/procedural roles still choose platform `sans-serif-medium`. Therefore **the entire app is NOT an exact Telegram font implementation**, even though the four audited roles now use an exact Telegram default bundled-font byte match.

## Verification boundary and resumption
Audit committed from XML source SHA `19c0d1135b6e8e797593b97022912e55ae5d230a`. The filename inventory is guarded by `TelegramInventoryCoverageTest.kt`; the guard checks inventory inclusion, NOT rendered fidelity. After any new XML layout is created, expand this table and validate dynamic Kotlin drawing separately. Latest Android tests and artwork distribution must always be checked against live `main`, not this prior SHA.

# EVE admin and shared UI coverage — Task 3

Baseline: `4e4dc6903476ec4d1df4f31ed59a1021811bc109` (verified Task 2 on main).

This document establishes the verified inventory and design decisions for **Task 3: EVE Admin and Shared UI Visual Consistency**. Following the completion of Tasks 1 and 2, all active administrative, management and shared UI surfaces adhere to the [Eve iOS 26 UI contract](EVE_IOS26_UI_CONTRACT.md).

## Production trace and decisions

The admin reconstruction brings administrative and shared surfaces into complete visual harmony with the student application while respecting administrative workflow efficiency, information density, and destructive confirmation clarity.

| Screen / Component | Layout / Class | Dependent presentation | Changes & design decisions |
|---|---|---|---|
| Admin Hub & Dashboard | `AdminActivity` → `activity_admin.xml` | Stat cards, quick links, badge counters | Standardized primary buttons, destructive actions (`selector_eve_destructive_text`), `@drawable/bg_stat_mini` stat badges, canonical 12dp outlined inputs; migrated status badges in code to semantic tokens `eve_status_error_text` and `eve_status_success_text`. |
| Admin Analytics | `AdminAnalyticsActivity` → `activity_admin_analytics.xml` | `AnalyticsExamAdapter` → `item_analytics_question.xml` | Filter chips standardized to `Widget.Eve.CategoryChip`, text secondary tokens applied; adapter migrated to semantic tokens `eve_status_error_text`, `eve_status_warning_text`, and `eve_status_success_text`. |
| Manage Exams | `ManageExamsActivity` → `activity_manage_exams.xml` | `item_manage_question.xml` | Standardized `MaterialSwitch`, dropdown styling via `Widget.Eve.TextInput.Outlined.ExposedDropdownMenu`, card backgrounds to `eve_card_bg`, normalized corner radii, outlined destructive button. |
| Edit Exam Form | `EditExamActivity` → `activity_edit_exam.xml` | Form sections, input groups | Replaced 7 legacy `TextInputLayout` instances with canonical `Widget.Eve.TextInput.Outlined` with 12dp squircle radii and adaptive focus states. |
| Manage Premium & Transactions | `ManagePremiumActivity` → `activity_manage_premium.xml` | `item_admin_premium_user.xml`, `item_admin_transaction.xml`, `dialog_manual_grant_premium.xml` | Replaced 7 legacy `TextInputLayout` instances with `Widget.Eve.TextInput.Outlined`, normalized card backgrounds to `eve_card_bg`, destructive revoke text to `selector_eve_destructive_text`. |
| App Configuration | `AppConfigActivity` → `activity_app_config.xml` | Configuration sections | Verified configuration sections, buttons and stateful inputs conforming to tested 1dp border specification. |
| Send Notifications | `SendNotificationActivity` → `activity_send_notification.xml` | Form controls, recipient filters | Delete button tint to `selector_eve_destructive_text`, canonical 12dp filled inputs, structured hierarchy. |
| Activity Log | `ActivityLogActivity` → `activity_activity_log.xml` | Filter chips, log entries | Filter chips standardized to `Widget.Eve.CategoryChip`, secondary text tokens for audit timestamps. |
| API Usage & System Monitor | `ApiUsageActivity` → `activity_api_usage.xml`, `SystemMonitorActivity` | Metric rows, status indicators | Metric text colors standardized to `eve_status_success_text`; audited programmatic card and status styling in `SystemMonitorActivity`. |
| Generated Tests & Questions | `item_generated_test.xml`, `dialog_add_question.xml`, `dialog_edit_question_details.xml` | `MaterialSwitch`, question forms | Standardized `MaterialSwitch`, card backgrounds to `eve_card_bg`, delete action tint to `selector_eve_destructive_text`, 14 legacy `TextInputLayout` instances upgraded to `Widget.Eve.TextInput.Outlined`. |
| User & Poll Management | `UserAdminAdapter` → `item_admin_email.xml`, `PollsAdminAdapter` → `item_poll_admin.xml`, `dialog_create_poll.xml` | Adapters and dialogs | Status indicators migrated to `eve_status_warning_text` and `eve_status_success_text`; remove/delete actions styled with `selector_eve_destructive_text` and stroke; switch updated to `MaterialSwitch`. |
| Banners, Syllabus & Feedback | `dialog_manage_banners.xml`, `item_admin_banner.xml`, `item_admin_syllabus.xml`, `dialog_reply_feedback.xml`, `dialog_create_feedback_post.xml`, `dialog_post_replies.xml`, `item_feedback_message.xml` | Dialogs and cards | Replaced inputs with `Widget.Eve.TextInput.Outlined`, cancel buttons to `Widget.Eve.TextButton`, delete buttons to `selector_eve_destructive_text` with stroke, cards to `eve_card_bg`. |
| Shared Bulletin & Undo Bar | `layout_bulletin.xml`, `layout_undo_bar.xml` | Notice overlays, action bars | Card corner radius standardized to `@dimen/eve_radius_card` (16dp). |
| Shared Telegram Menu Popup | `popup_telegram_menu.xml` | Popup menu | Corner radius standardized to `@dimen/eve_radius_menu` (16dp squircle), logout action tint and text to `selector_eve_destructive_text`. |

## Critical constraints verified

- **Pure visual presentation**: Zero modifications made to backend endpoints, auth, admin authorization, database models, deletion rules, approval workflows, or payment logic.
- **Destructive confirmation clarity**: Confirmation dialogs, validation messages, and error states maintained and visually sharpened.
- **Data density preserved**: Administrative tables, lists, and forms retain compact spacing suitable for high-density management workflows.
- **Protected student UI intact**: All student-facing screens from Tasks 1 and 2 remain unmodified and fully functional.
- **Circular reveal theme switching**: Telegram-style circular reveal theme switching remains 100% intact.

## Final validation

- Resource XML parsing & design system validation: `python3 -B .github/scripts/check_design_system.py`: **196 composited semantic contrast pairs passed**, minimum text contrast 4.51:1.
- Unit tests: `./gradlew testDebugUnitTest`: **BUILD SUCCESSFUL** (209 unit tests passed).
- Debug APK packaging: `./gradlew assembleDebug`: **BUILD SUCCESSFUL**.
- Android Lint analysis: `./gradlew lintDebug`: **BUILD SUCCESSFUL** (0 lint errors).
- Patch formatting: `git diff --check`: passed with 0 errors.

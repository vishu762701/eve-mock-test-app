# iOS 26 migration — per-file change inventory

Baseline: `5f28980b64b87873984dd2f8d0f70baff54e5971` on `main`. Delivery to main is authorized by the latest user instruction.

This inventory includes every changed layout, drawable, Kotlin source/test, resource, workflow and document.

## Layouts

| File | Change |
|---|---|
| [app/src/main/res/layout/activity_about.xml](../../app/src/main/res/layout/activity_about.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_activity_log.xml](../../app/src/main/res/layout/activity_activity_log.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_admin.xml](../../app/src/main/res/layout/activity_admin.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_admin_analytics.xml](../../app/src/main/res/layout/activity_admin_analytics.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_api_usage.xml](../../app/src/main/res/layout/activity_api_usage.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_app_config.xml](../../app/src/main/res/layout/activity_app_config.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_bookmarks.xml](../../app/src/main/res/layout/activity_bookmarks.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_content_display.xml](../../app/src/main/res/layout/activity_content_display.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_edit_about.xml](../../app/src/main/res/layout/activity_edit_about.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_edit_exam.xml](../../app/src/main/res/layout/activity_edit_exam.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_exam_tests.xml](../../app/src/main/res/layout/activity_exam_tests.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_feedback.xml](../../app/src/main/res/layout/activity_feedback.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_feedback_messages.xml](../../app/src/main/res/layout/activity_feedback_messages.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_flagged_questions.xml](../../app/src/main/res/layout/activity_flagged_questions.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_generated_tests.xml](../../app/src/main/res/layout/activity_generated_tests.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_history.xml](../../app/src/main/res/layout/activity_history.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_leaderboard.xml](../../app/src/main/res/layout/activity_leaderboard.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_login.xml](../../app/src/main/res/layout/activity_login.xml) | uses the Large Title role. |
| [app/src/main/res/layout/activity_main.xml](../../app/src/main/res/layout/activity_main.xml) | Adds a capsule glass backdrop in the existing chrome region; uses shared fixed/capsule/concentric shape roles; uses the appropriate semantic surface/foreground; removes the banner BlurView and uses plain Home canvases. |
| [app/src/main/res/layout/activity_manage_exams.xml](../../app/src/main/res/layout/activity_manage_exams.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_manage_existing_exams.xml](../../app/src/main/res/layout/activity_manage_existing_exams.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_manage_polls.xml](../../app/src/main/res/layout/activity_manage_polls.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_manage_premium.xml](../../app/src/main/res/layout/activity_manage_premium.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_manage_syllabus.xml](../../app/src/main/res/layout/activity_manage_syllabus.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses shared fixed/capsule/concentric shape roles; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_manage_users.xml](../../app/src/main/res/layout/activity_manage_users.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_mistakes.xml](../../app/src/main/res/layout/activity_mistakes.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_notifications.xml](../../app/src/main/res/layout/activity_notifications.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_payment_checkout.xml](../../app/src/main/res/layout/activity_payment_checkout.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_performance.xml](../../app/src/main/res/layout/activity_performance.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_practice.xml](../../app/src/main/res/layout/activity_practice.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_premium.xml](../../app/src/main/res/layout/activity_premium.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_profile.xml](../../app/src/main/res/layout/activity_profile.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_pyq.xml](../../app/src/main/res/layout/activity_pyq.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_result.xml](../../app/src/main/res/layout/activity_result.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses shared fixed/capsule/concentric shape roles; uses the appropriate semantic surface/foreground; keeps tabs in place with an accessible capsule track and inset targets. |
| [app/src/main/res/layout/activity_result_detail.xml](../../app/src/main/res/layout/activity_result_detail.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_send_notification.xml](../../app/src/main/res/layout/activity_send_notification.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_settings.xml](../../app/src/main/res/layout/activity_settings.xml) | Adds a capsule glass backdrop in the existing chrome region; uses an inline Headline title; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_syllabus.xml](../../app/src/main/res/layout/activity_syllabus.xml) | uses an inline Headline title; uses shared fixed/capsule/concentric shape roles; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/activity_test.xml](../../app/src/main/res/layout/activity_test.xml) | uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/dialog_create_poll.xml](../../app/src/main/res/layout/dialog_create_poll.xml) | retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_admin_banner.xml](../../app/src/main/res/layout/item_admin_banner.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_admin_email.xml](../../app/src/main/res/layout/item_admin_email.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_admin_premium_user.xml](../../app/src/main/res/layout/item_admin_premium_user.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_admin_syllabus.xml](../../app/src/main/res/layout/item_admin_syllabus.xml) | sets a 52dp row minimum; retains or raises 48dp control targets; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/item_admin_transaction.xml](../../app/src/main/res/layout/item_admin_transaction.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_analytics_exam.xml](../../app/src/main/res/layout/item_analytics_exam.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_analytics_question.xml](../../app/src/main/res/layout/item_analytics_question.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_answer.xml](../../app/src/main/res/layout/item_answer.xml) | uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/item_audit_log.xml](../../app/src/main/res/layout/item_audit_log.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_bookmark_card.xml](../../app/src/main/res/layout/item_bookmark_card.xml) | sets a 52dp row minimum; uses the appropriate semantic surface/foreground. |
| [app/src/main/res/layout/item_exam.xml](../../app/src/main/res/layout/item_exam.xml) | sets a 52dp row minimum; raises card padding modestly; uses shared fixed/capsule/concentric shape roles. |
| [app/src/main/res/layout/item_exam_test.xml](../../app/src/main/res/layout/item_exam_test.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_existing_exam_admin.xml](../../app/src/main/res/layout/item_existing_exam_admin.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_feedback_message.xml](../../app/src/main/res/layout/item_feedback_message.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_feedback_post.xml](../../app/src/main/res/layout/item_feedback_post.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_flagged_question.xml](../../app/src/main/res/layout/item_flagged_question.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_generated_test.xml](../../app/src/main/res/layout/item_generated_test.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_history.xml](../../app/src/main/res/layout/item_history.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_leaderboard.xml](../../app/src/main/res/layout/item_leaderboard.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_manage_question.xml](../../app/src/main/res/layout/item_manage_question.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_mistake.xml](../../app/src/main/res/layout/item_mistake.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_notification.xml](../../app/src/main/res/layout/item_notification.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_palette_circle.xml](../../app/src/main/res/layout/item_palette_circle.xml) | uses shared fixed/capsule/concentric shape roles. |
| [app/src/main/res/layout/item_poll_admin.xml](../../app/src/main/res/layout/item_poll_admin.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_poll_option_input.xml](../../app/src/main/res/layout/item_poll_option_input.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_poll_option_result.xml](../../app/src/main/res/layout/item_poll_option_result.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_post_reply.xml](../../app/src/main/res/layout/item_post_reply.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_result_topic.xml](../../app/src/main/res/layout/item_result_topic.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_sent_broadcast.xml](../../app/src/main/res/layout/item_sent_broadcast.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/item_syllabus.xml](../../app/src/main/res/layout/item_syllabus.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_topic_stat.xml](../../app/src/main/res/layout/item_topic_stat.xml) | sets a 52dp row minimum. |
| [app/src/main/res/layout/item_user_admin.xml](../../app/src/main/res/layout/item_user_admin.xml) | sets a 52dp row minimum; retains or raises 48dp control targets. |
| [app/src/main/res/layout/popup_telegram_menu.xml](../../app/src/main/res/layout/popup_telegram_menu.xml) | sets a 52dp row minimum; uses the appropriate semantic surface/foreground. |

## Drawables

| File | Change |
|---|---|
| [app/src/main/res/drawable-night/bg_streak_pill.xml](../../app/src/main/res/drawable-night/bg_streak_pill.xml) | Keeps the borderless capsule and uses the raised plain-screen surface in this qualifier. |
| [app/src/main/res/drawable/bg_active_context_badge.xml](../../app/src/main/res/drawable/bg_active_context_badge.xml) | Uses the shared capsule radius. |
| [app/src/main/res/drawable/bg_bottom_sheet_drag_handle.xml](../../app/src/main/res/drawable/bg_bottom_sheet_drag_handle.xml) | Uses Eve modal radii and the appropriate modal material surface. |
| [app/src/main/res/drawable/bg_content_sheet.xml](../../app/src/main/res/drawable/bg_content_sheet.xml) | Uses the flat plain Home canvas. |
| [app/src/main/res/drawable/bg_dialog_liquid_glass.xml](../../app/src/main/res/drawable/bg_dialog_liquid_glass.xml) | Uses Eve modal radii and the appropriate modal material surface. |
| [app/src/main/res/drawable/bg_drawer_glass.xml](../../app/src/main/res/drawable/bg_drawer_glass.xml) | Uses shared fixed/concentric geometry; drawer surfaces are opaque. |
| [app/src/main/res/drawable/bg_drawer_item_selector.xml](../../app/src/main/res/drawable/bg_drawer_item_selector.xml) | Uses shared fixed/concentric geometry; drawer surfaces are opaque. |
| [app/src/main/res/drawable/bg_option_review_correct.xml](../../app/src/main/res/drawable/bg_option_review_correct.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_option_review_default.xml](../../app/src/main/res/drawable/bg_option_review_default.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_option_review_selected.xml](../../app/src/main/res/drawable/bg_option_review_selected.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_option_review_wrong.xml](../../app/src/main/res/drawable/bg_option_review_wrong.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_poll_progress.xml](../../app/src/main/res/drawable/bg_poll_progress.xml) | Uses the shared capsule radius. |
| [app/src/main/res/drawable/bg_recovery_badge.xml](../../app/src/main/res/drawable/bg_recovery_badge.xml) | Uses the shared capsule radius. |
| [app/src/main/res/drawable/bg_recovery_inset.xml](../../app/src/main/res/drawable/bg_recovery_inset.xml) | Uses a shared concentric radius and opaque nested grouped surface. |
| [app/src/main/res/drawable/bg_recovery_sheet.xml](../../app/src/main/res/drawable/bg_recovery_sheet.xml) | Uses Eve modal radii and the appropriate modal material surface. |
| [app/src/main/res/drawable/bg_result_metric_medium.xml](../../app/src/main/res/drawable/bg_result_metric_medium.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_result_metric_right.xml](../../app/src/main/res/drawable/bg_result_metric_right.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_result_metric_unattempted.xml](../../app/src/main/res/drawable/bg_result_metric_unattempted.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_result_metric_wrong.xml](../../app/src/main/res/drawable/bg_result_metric_wrong.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |
| [app/src/main/res/drawable/bg_result_segmented_track.xml](../../app/src/main/res/drawable/bg_result_segmented_track.xml) | Uses a transparent capsule track so its glass backdrop remains visible. |
| [app/src/main/res/drawable/bg_search_field.xml](../../app/src/main/res/drawable/bg_search_field.xml) | Uses the shared capsule radius. |
| [app/src/main/res/drawable/bg_sheet_bottom.xml](../../app/src/main/res/drawable/bg_sheet_bottom.xml) | Uses Eve modal radii and the appropriate modal material surface. |
| [app/src/main/res/drawable/bg_stat_mini.xml](../../app/src/main/res/drawable/bg_stat_mini.xml) | Uses a shared concentric radius and opaque nested grouped surface. |
| [app/src/main/res/drawable/bg_streak_pill.xml](../../app/src/main/res/drawable/bg_streak_pill.xml) | Keeps the borderless capsule and uses the raised plain-screen surface in this qualifier. |
| [app/src/main/res/drawable/bg_tile_surface2.xml](../../app/src/main/res/drawable/bg_tile_surface2.xml) | Replaces a corner literal with the appropriate shared fixed or concentric radius; semantic state fills are retained. |

## Kotlin production and verification

| File | Change |
|---|---|
| [app/src/androidTest/java/com/eve/app/ui/ApprovedUiRenderingTest.kt](../../app/src/androidTest/java/com/eve/app/ui/ApprovedUiRenderingTest.kt) | Updates native assertions for the authorized geometry, background, label and exact-blue exception while retaining interaction checks. |
| [app/src/androidTest/java/com/eve/app/ui/DesignRecoveryTest.kt](../../app/src/androidTest/java/com/eve/app/ui/DesignRecoveryTest.kt) | Updates native assertions for the authorized geometry, background, label and exact-blue exception while retaining interaction checks. |
| [app/src/androidTest/java/com/eve/app/ui/Ios26ChromeTest.kt](../../app/src/androidTest/java/com/eve/app/ui/Ios26ChromeTest.kt) | Adds production-layout chrome/modal fixture exports in both appearances; these are Canvas fixtures, not compositor screenshots. |
| [app/src/androidTest/java/com/eve/app/ui/ManualUiSpecificationTest.kt](../../app/src/androidTest/java/com/eve/app/ui/ManualUiSpecificationTest.kt) | Updates native assertions for the authorized geometry, background, label and exact-blue exception while retaining interaction checks. |
| [app/src/androidTest/java/com/eve/app/ui/SharedDesignSystemTest.kt](../../app/src/androidTest/java/com/eve/app/ui/SharedDesignSystemTest.kt) | Updates native assertions for the authorized geometry, background, label and exact-blue exception while retaining interaction checks. |
| [app/src/main/java/com/eve/app/ui/admin/ExamAttemptsBarChartView.kt](../../app/src/main/java/com/eve/app/ui/admin/ExamAttemptsBarChartView.kt) | Uses the shared minimum-radius token for chart bars. |
| [app/src/main/java/com/eve/app/ui/common/EveGlassButton.kt](../../app/src/main/java/com/eve/app/ui/common/EveGlassButton.kt) | Uses a relative 50% capsule shape. |
| [app/src/main/java/com/eve/app/ui/common/EveGlassPill.kt](../../app/src/main/java/com/eve/app/ui/common/EveGlassPill.kt) | Uses semantic text appearance and reuses capsule backgrounds for bound semantic badges without changing their fills. |
| [app/src/main/java/com/eve/app/ui/common/EveGlassToolbar.kt](../../app/src/main/java/com/eve/app/ui/common/EveGlassToolbar.kt) | Renders the allowed capsule chrome with a backdrop-only BlurView and opaque older-API fallback. |
| [app/src/main/java/com/eve/app/ui/common/EveLiquidGlassView.kt](../../app/src/main/java/com/eve/app/ui/common/EveLiquidGlassView.kt) | Uses the shared fixed card radius for solid fill, stroke and clipping. |
| [app/src/main/java/com/eve/app/ui/common/ShimmerSkeletonView.kt](../../app/src/main/java/com/eve/app/ui/common/ShimmerSkeletonView.kt) | Uses shared card, capsule and concentric radii for loading placeholders. |
| [app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt](../../app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt) | Uses a concentric option radius; selection colours, indicator geometry and callbacks remain unchanged. |
| [app/src/main/java/com/eve/app/ui/home/ExamAdapter.kt](../../app/src/main/java/com/eve/app/ui/home/ExamAdapter.kt) | Uses plain-screen raised surfaces and clears the dedicated tile fill while the Lottie is visible. |
| [app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt](../../app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt) | Uses the shared banner radius and primary-label bell tint required on glass. |
| [app/src/main/java/com/eve/app/ui/home/MainActivity.kt](../../app/src/main/java/com/eve/app/ui/home/MainActivity.kt) | Removes banner/drawer content blur, uses fixed drawer/concentric ripple shapes, and removes obsolete drawer bitmap state. |
| [app/src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt](../../app/src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt) | Uses capsule chips and lifecycle-owned modal blur. |
| [app/src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt](../../app/src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt) | Uses RenderEffect only on the captured menu background and an opaque older-API fallback; reveal logic is retained. |
| [app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardAdapter.kt](../../app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardAdapter.kt) | Applies capsule shapes to rank badges while preserving rank colours and binding behavior. |
| [app/src/main/java/com/eve/app/ui/result/QuestionTimeChartView.kt](../../app/src/main/java/com/eve/app/ui/result/QuestionTimeChartView.kt) | Uses the shared minimum-radius token for chart bars. |
| [app/src/main/java/com/eve/app/ui/result/ResultCutoffPresentation.kt](../../app/src/main/java/com/eve/app/ui/result/ResultCutoffPresentation.kt) | Applies the shared capsule badge helper while retaining semantic state fills/text and cutoff logic. |
| [app/src/main/java/com/eve/app/ui/result/ResultTabs.kt](../../app/src/main/java/com/eve/app/ui/result/ResultTabs.kt) | Preserves the existing tab region while reserving a 56dp track for 48dp inset targets. |
| [app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt](../../app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt) | Clears outside dim and blur flags on the Sign In reauthentication prompt. |
| [app/src/main/java/com/eve/app/util/EveBlurHelper.kt](../../app/src/main/java/com/eve/app/util/EveBlurHelper.kt) | Uses a RenderEffect background engine on API 31+ and an opaque tint fallback below API 31 or after setup failure. |
| [app/src/main/java/com/eve/app/util/EveShapes.kt](../../app/src/main/java/com/eve/app/util/EveShapes.kt) | Adds shared concentric-radius calculation and measured capsule/card helpers. |
| [app/src/main/java/com/eve/app/util/RippleHelper.kt](../../app/src/main/java/com/eve/app/util/RippleHelper.kt) | Uses the concentric shape token as its default mask radius. |
| [app/src/main/java/com/eve/app/util/ShareCardHelper.kt](../../app/src/main/java/com/eve/app/util/ShareCardHelper.kt) | Uses shared shape policy on the fixed export canvas and resolves dark systemBlue from resources. |
| [app/src/main/java/com/eve/app/util/SystemBarHelper.kt](../../app/src/main/java/com/eve/app/util/SystemBarHelper.kt) | Matches inset backing to plain/grouped screen roles through typed Activity checks; reveal/icon synchronization stays intact. |
| [app/src/test/java/com/eve/app/ui/AppWideTypographyAndColorRolesTest.kt](../../app/src/test/java/com/eve/app/ui/AppWideTypographyAndColorRolesTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |
| [app/src/test/java/com/eve/app/ui/LottieContainersNoDecorationTest.kt](../../app/src/test/java/com/eve/app/ui/LottieContainersNoDecorationTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |
| [app/src/test/java/com/eve/app/ui/ManualBorderInventoryTest.kt](../../app/src/test/java/com/eve/app/ui/ManualBorderInventoryTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |
| [app/src/test/java/com/eve/app/ui/ProfileSettingsLoginTypographyAndColorTest.kt](../../app/src/test/java/com/eve/app/ui/ProfileSettingsLoginTypographyAndColorTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |
| [app/src/test/java/com/eve/app/ui/home/HomeSemanticColorsTest.kt](../../app/src/test/java/com/eve/app/ui/home/HomeSemanticColorsTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |
| [app/src/test/java/com/eve/app/ui/result/ResultAndSecondaryScreensTypographyTest.kt](../../app/src/test/java/com/eve/app/ui/result/ResultAndSecondaryScreensTypographyTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |
| [app/src/test/java/com/eve/app/util/EveShapesTest.kt](../../app/src/test/java/com/eve/app/util/EveShapesTest.kt) | Checks inset subtraction, minimum-radius clamping and measured-height capsules. |
| [app/src/test/java/com/eve/app/util/MasterRedesignContinuationTest.kt](../../app/src/test/java/com/eve/app/util/MasterRedesignContinuationTest.kt) | Updates superseded source-contract assertions for the authorized shape, background, typography and blue policy. |

## Other Android resources

| File | Change |
|---|---|
| [app/src/main/res/color/selector_home_admin_surface.xml](../../app/src/main/res/color/selector_home_admin_surface.xml) | Updates the authorized migration and its verification contract. |
| [app/src/main/res/values-night-v31/materials.xml](../../app/src/main/res/values-night-v31/materials.xml) | Provides API 31 material overrides; the day-qualified definition also enables bounded floating-dialog background blur. |
| [app/src/main/res/values-night/approved_ui_colors.xml](../../app/src/main/res/values-night/approved_ui_colors.xml) | Uses primary labels for unselected glass tabs; protected test-state colours are retained. |
| [app/src/main/res/values-night/colors.xml](../../app/src/main/res/values-night/colors.xml) | Keeps the verified system palette; unifies blue aliases, adds unused readable revert colour, and defines contrast-safe material/opaque content roles. |
| [app/src/main/res/values-v31/materials.xml](../../app/src/main/res/values-v31/materials.xml) | Provides API 31 material overrides; the day-qualified definition also enables bounded floating-dialog background blur. |
| [app/src/main/res/values/approved_ui_colors.xml](../../app/src/main/res/values/approved_ui_colors.xml) | Uses primary labels for unselected glass tabs; protected test-state colours are retained. |
| [app/src/main/res/values/approved_ui_styles.xml](../../app/src/main/res/values/approved_ui_styles.xml) | Uses the relative capsule shape for Test actions. |
| [app/src/main/res/values/colors.xml](../../app/src/main/res/values/colors.xml) | Keeps the verified system palette; unifies blue aliases, adds unused readable revert colour, and defines contrast-safe material/opaque content roles. |
| [app/src/main/res/values/design_recovery_styles.xml](../../app/src/main/res/values/design_recovery_styles.xml) | Uses capsule chips and the modal material role. |
| [app/src/main/res/values/dimens.xml](../../app/src/main/res/values/dimens.xml) | Centralizes Eve fixed/capsule/concentric geometry, 52dp rows, 56dp segmented tracks and modal blur approximation. |
| [app/src/main/res/values/styles.xml](../../app/src/main/res/values/styles.xml) | Defines shared ShapeAppearance policies, capsule controls, accessible chip targets and shared widget roles. |
| [app/src/main/res/values/themes.xml](../../app/src/main/res/values/themes.xml) | Uses shared input geometry and dialog/modal surface roles, including the inherited dialog style for API-specific material treatment. |

## CI and verification tooling

| File | Change |
|---|---|
| [.github/scripts/check_design_system.py](../../.github/scripts/check_design_system.py) | Keeps 4.5:1 gates, explicitly records the exact-blue user exception, and checks primary labels on worst-case glass composites. |
| [.github/scripts/eve_visual_review.py](../../.github/scripts/eve_visual_review.py) | Requires paired nav/tab/menu/sheet/dialog fixtures in the existing visual-evidence gate. |
| [.github/scripts/tests/test_visual_review.py](../../.github/scripts/tests/test_visual_review.py) | Updates the authorized migration and its verification contract. |
| [.github/scripts/verify-native-ui.sh](../../.github/scripts/verify-native-ui.sh) | Makes the requested disposable fixture images reviewable through job logs without uploading additional artifacts. |
| [.github/workflows/build.yml](../../.github/workflows/build.yml) | Keeps native/build gates and the sole gated Eve-debug-apk upload; removes the extra verification artifact upload. |

## Authority and evidence documents

| File | Change |
|---|---|
| [AGENTS.md](../../AGENTS.md) | Records explicit full-screen geometry authorization and the protected boundaries. |
| [APPROVED_UI_IMPLEMENTATION_AUDIT.md](../../APPROVED_UI_IMPLEMENTATION_AUDIT.md) | Adds the current authorization override while retaining historical audit evidence. |
| [BACKLOG.md](../../BACKLOG.md) | Records remaining physical-device and live-flow checks after native CI success. |
| [CLAUDE.md](../../CLAUDE.md) | Aligns the equivalent repository rules with the authorized geometry scope. |
| [DECISIONS.md](../../DECISIONS.md) | Records the user-requested UI ADR-009, numbering collision, implementation choices, official references and recovery and subsequent push authorization. |
| [EVE_IOS26_UI_CONTRACT.md](../../EVE_IOS26_UI_CONTRACT.md) | Documents the verified palette/type rows, Eve shape interpretations, glass scope, exact-blue trade-off and verification boundaries. |
| [GEMINI.md](../../GEMINI.md) | Records explicit full-screen geometry authorization and the protected boundaries. |
| [SOURCE_OF_TRUTH.md](../../SOURCE_OF_TRUTH.md) | Updates lasting geometry, glass, blue and background authority without transient build claims. |
| [docs/ios26/APPLE_REFERENCE_ROWS.md](../../docs/ios26/APPLE_REFERENCE_ROWS.md) | Preserves the Apple rows actually read and hashes/URLs of the downloaded source JSON. |
| [docs/ios26/color.json](../../docs/ios26/color.json) | Preserves the fetched official Apple Color JSON evidence. |
| [docs/ios26/materials.json](../../docs/ios26/materials.json) | Preserves the fetched official Apple Materials JSON evidence. |
| [docs/ios26/typography.json](../../docs/ios26/typography.json) | Preserves the fetched official Apple Typography JSON evidence. |

## Final validation and remaining inventory

| File | Change |
|---|---|
| [app/src/main/res/layout/dialog_add_question.xml](../../app/src/main/res/layout/dialog_add_question.xml) | Uses 18dp concentric form fields inside the 34dp modal with its existing 16dp inset. |
| [docs/ios26/CONTRAST_VALIDATION.log](../../docs/ios26/CONTRAST_VALIDATION.log) | Observed contrast results, including user-chosen blue exceptions. |
| [docs/ios26/FILE_CHANGES.md](../../docs/ios26/FILE_CHANGES.md) | Complete per-file migration inventory, including this document. |
| [docs/ios26/GRADLE_VALIDATION.log](../../docs/ios26/GRADLE_VALIDATION.log) | Observed final successful build/unit/lint/instrumentation-compilation output. |
| [docs/ios26/LINT_COMPARISON.json](../../docs/ios26/LINT_COMPARISON.json) | Compares baseline and final warnings without suppressing diagnostics. |
| [docs/ios26/MIGRATION_REPORT.md](../../docs/ios26/MIGRATION_REPORT.md) | Recovered checkpoint, actual checks, Apple rows, light/dark capture review, delivery results and explicit physical/runtime gaps. |
| [docs/ios26/SCRIPT_VALIDATION.log](../../docs/ios26/SCRIPT_VALIDATION.log) | Observed thirteen passing verification-script tests and explicit blue exceptions. |
| [docs/ios26/VALIDATION.json](../../docs/ios26/VALIDATION.json) | Machine-readable observed local validation, source/protection checks and runtime/remote limitations. |

Total inventory entries: 170.

Delivery follow-up also changes `app/src/androidTest/java/com/eve/app/ui/StudentUiRenderingTest.kt`: recognizes only full-opacity system blue as the documented contrast exception; other checks remain.

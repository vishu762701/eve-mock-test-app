# Design reset migration inventory

Baseline: `40ecfd366ccd3df94cbeed8f2526355f940dcec5`.

Phase 4 XML/shared-component migration is implemented and passes local build/JVM/lint gates. Hosted native rendering is pending; entries below distinguish implementation from verification. Data/ViewModel files do not require a design migration.

| Source / layout | Status |
| --- | --- |
| `app/src/main/java/com/eve/app/service/EveMessagingService.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/about/AboutActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/about/ContentDisplayActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminAnalyticsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminAnalyticsViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/admin/AdminBannerAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminDeleteFlow.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminEmailAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminPremiumUserAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminSyllabusAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminTransactionAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/admin/AnalyticsExamAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AnalyticsQuestionAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ApiUsageActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AppConfigActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/AuditLogAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/EditAboutActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ExamAttemptsBarChartView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ExistingExamsAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/FlaggedQuestionsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/FlaggedQuestionsAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageExamsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageExistingExamsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ManagePremiumActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/PollsAdminAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/PostRepliesAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/QuestionManageAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/SendNotificationActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/SentBroadcastAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/admin/UserAdminAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarkAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarksActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarksViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/common/CircularCountdownView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/CompletedExamBottomSheet.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EmptyStateView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/ErrorStateView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveBaseActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveGlassButton.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveGlassPill.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveGlassSegmentedControl.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveGlassTabLayout.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveGlassToolbar.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/EveLiquidGlassView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/ExpandableCardHelper.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/FlameAnimationView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/ReportQuestionDialog.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/ShimmerSkeletonView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/SparkleView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/StudentSelectionControls.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/feedback/FeedbackActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/history/HistoryActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/history/HistoryAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/history/HistoryViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/ExamAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/ExamLaunchHelper.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/HomeBannerAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/HomeUiModels.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/HomeViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/MainActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt` | Protected animation/behavior retained; phase 3 native checks passed |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/login/AmbientBackgroundView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/login/LoginActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/performance/PerformanceActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/performance/PerformanceViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/performance/ScoreTrendChartView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/performance/TopicStatAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/practice/PracticeActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/practice/PracticeViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/PaymentMethodBottomSheet.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/PremiumActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/PremiumBenefitRows.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/gl/GLIconRenderer.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/gl/Icon3D.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/gl/ObjLoader.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/premium/gl/Telegram3DStarView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/pyq/PyqActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/pyq/PyqViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/AnswerAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ConfettiView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/QuestionTimeChartView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResponsiveResultRow.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultAnalysisRows.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultCutoffPresentation.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultDataHolder.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultDetailActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultSection.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultStatisticsGrid.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultTabs.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/result/ResultViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/SemiCircleGaugeView.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/test/QuestionAdapter.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/test/TestActivity.kt` | Pending |
| `app/src/main/java/com/eve/app/ui/test/TestViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/util/GlassmorphismHelper.kt` | Pending |
| `app/src/main/java/com/eve/app/worker/DailyReminderWorker.kt` | Pending |
| `app/src/main/java/com/eve/app/worker/SubmitWorker.kt` | Pending |
| `app/src/main/res/layout/activity_about.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_activity_log.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_admin.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_admin_analytics.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_api_usage.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_app_config.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_bookmarks.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_content_display.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_edit_about.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_edit_exam.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_exam_tests.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_feedback.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_feedback_messages.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_flagged_questions.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_generated_tests.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_history.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_leaderboard.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_login.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_main.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_manage_exams.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_manage_existing_exams.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_manage_polls.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_manage_premium.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_manage_syllabus.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_manage_users.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_mistakes.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_notifications.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_payment_checkout.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_performance.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_practice.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_premium.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_profile.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_pyq.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_result.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_result_detail.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_send_notification.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_settings.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_syllabus.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/activity_test.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/bottom_sheet_category_picker.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/bottom_sheet_completed_exam.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/bottom_sheet_font_picker.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/bottom_sheet_payment_method.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/bottom_sheet_target_exams.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_add_question.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_create_feedback_post.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_create_poll.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_edit_question_details.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_manage_banners.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_manual_grant_premium.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_post_replies.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_reply_feedback.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/dialog_report_question.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_admin_banner.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_admin_email.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_admin_premium_user.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_admin_syllabus.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_admin_transaction.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_analytics_exam.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_analytics_question.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_answer.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_audit_log.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_bookmark_card.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_category_header.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_exam.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_exam_dropdown.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_exam_test.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_existing_exam_admin.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_feedback_message.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_feedback_post.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_flagged_question.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_generated_test.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_history.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_home_banner.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_leaderboard.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_manage_question.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_mistake.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_notification.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_palette_circle.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_poll_admin.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_poll_option_input.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_poll_option_result.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_post_reply.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_question.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_result_topic.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_sent_broadcast.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_student_action.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_syllabus.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_topic_stat.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/item_user_admin.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/layout_bulletin.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/layout_undo_bar.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/popup_telegram_menu.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/view_empty_state.xml` | Shared token styles applied; native inflation/render verification pending |
| `app/src/main/res/layout/view_error_state.xml` | Shared token styles applied; native inflation/render verification pending |

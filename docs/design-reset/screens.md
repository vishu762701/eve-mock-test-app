# Design reset migration inventory

Baseline: `40ecfd366ccd3df94cbeed8f2526355f940dcec5`.

Phase 4 XML/shared-component migration is implemented and passes local build/JVM/lint gates. Hosted native rendering is pending; entries below distinguish implementation from verification. Data/ViewModel files do not require a design migration.

| Source / layout | Status |
| --- | --- |
| `app/src/main/java/com/eve/app/service/EveMessagingService.kt` | Feature logic retained; system notification template uses migrated icon; OS controls template typography |
| `app/src/main/java/com/eve/app/ui/about/AboutActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/about/ContentDisplayActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminAnalyticsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminAnalyticsViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/admin/AdminBannerAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminDeleteFlow.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminEmailAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminPremiumUserAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminSyllabusAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminTransactionAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AdminViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/admin/AnalyticsExamAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AnalyticsQuestionAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ApiUsageActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AppConfigActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/AuditLogAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/EditAboutActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ExamAttemptsBarChartView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ExistingExamsAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/FlaggedQuestionsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/FlaggedQuestionsAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageExamsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageExistingExamsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ManagePremiumActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/PollsAdminAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/PostRepliesAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/QuestionManageAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/SendNotificationActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/SentBroadcastAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/admin/UserAdminAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarkAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarksActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarksViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/common/CircularCountdownView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/CompletedExamBottomSheet.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/EmptyStateView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/ErrorStateView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/EveBaseActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/EveGlassButton.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/EveGlassPill.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/EveGlassSegmentedControl.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/EveGlassTabLayout.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/EveGlassToolbar.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/EveLiquidGlassView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/ExpandableCardHelper.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/FlameAnimationView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/ReportQuestionDialog.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/ShimmerSkeletonView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/SparkleView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/common/StudentSelectionControls.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/feedback/FeedbackActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/history/HistoryActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/history/HistoryAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/history/HistoryViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/ExamAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/ExamLaunchHelper.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/HomeBannerAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/HomeUiModels.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/HomeViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/MainActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt` | Protected animation/behavior retained; phase 3 native checks passed |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/login/AmbientBackgroundView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/login/LoginActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/performance/PerformanceActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/performance/PerformanceViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/performance/ScoreTrendChartView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/performance/TopicStatAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/practice/PracticeActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/practice/PracticeViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/premium/PaymentMethodBottomSheet.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/premium/PremiumActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/premium/PremiumBenefitRows.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/premium/gl/GLIconRenderer.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/premium/gl/Icon3D.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/premium/gl/ObjLoader.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/premium/gl/Telegram3DStarView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/pyq/PyqActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/pyq/PyqViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/AnswerAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ConfettiView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/result/QuestionTimeChartView.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResponsiveResultRow.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultAnalysisRows.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultCutoffPresentation.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultDataHolder.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/ResultDetailActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultSection.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultStatisticsGrid.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultTabs.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/result/ResultViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/SemiCircleGaugeView.kt` | Retired from production hosts; deletion evidence scheduled for Phase 5 |
| `app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/test/QuestionAdapter.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/test/TestActivity.kt` | Shared XML/runtime token migration implemented; hosted native verification pending |
| `app/src/main/java/com/eve/app/ui/test/TestViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/util/GlassmorphismHelper.kt` | Protected popup cleanup support retained; unused glass APIs scheduled for Phase 5 audit |
| `app/src/main/java/com/eve/app/worker/DailyReminderWorker.kt` | Feature logic retained; system notification template uses migrated icon; OS controls template typography |
| `app/src/main/java/com/eve/app/worker/SubmitWorker.kt` | Feature logic retained; system notification template uses migrated icon; OS controls template typography |
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

| `app/src/main/res/layout/activity_system_monitor.xml` | Shared screen/header/cards migrated; hosted native verification pending |

System Monitor now uses `activity_system_monitor.xml` with shared large-title header and card host. No app-widget provider or RemoteViews layout exists (`rg appWidget/RemoteViews app/src/main`: zero matches). Native fixture coverage is distinct from signed-in end-to-end feature verification.

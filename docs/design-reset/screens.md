# Design reset migration inventory

Baseline: `40ecfd366ccd3df94cbeed8f2526355f940dcec5`.

Phase 4 XML/shared-component migration is implemented and passes local build/JVM/lint gates. Hosted native rendering is pending; entries below distinguish implementation from verification. Data/ViewModel files do not require a design migration.

| Source / layout | Status |
| --- | --- |
| `app/src/main/java/com/eve/app/service/EveMessagingService.kt` | Feature logic retained; system notification template uses migrated icon; OS controls template typography |
| `app/src/main/java/com/eve/app/ui/about/AboutActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/about/ContentDisplayActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ActivityLogActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminAnalyticsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminAnalyticsViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/admin/AdminBannerAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminDeleteFlow.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminEmailAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminPremiumUserAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminSyllabusAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminTransactionAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AdminViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/admin/AnalyticsExamAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AnalyticsQuestionAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ApiUsageActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AppConfigActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/AuditLogAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/EditAboutActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ExamAttemptsBarChartView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ExistingExamsAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/FeedbackMessagesAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/FlaggedQuestionsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/FlaggedQuestionsAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/GeneratedTestsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ManageExamsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ManageExistingExamsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ManagePollsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ManagePremiumActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/ManageUsersActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/PollsAdminAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/PostRepliesAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/QuestionManageAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/SendNotificationActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/SentBroadcastAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/SystemMonitorActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/admin/UserAdminAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarkAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarksActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/bookmarks/BookmarksViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/common/CircularCountdownView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/CompletedExamBottomSheet.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/EmptyStateView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/ErrorStateView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/EveBaseActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/EveGlassButton.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/EveGlassPill.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/EveGlassSegmentedControl.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/EveGlassTabLayout.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/EveGlassToolbar.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/EveLiquidGlassView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/ExpandableCardHelper.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/FlameAnimationView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/QuestionPaletteAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/ReportQuestionDialog.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/ShimmerSkeletonView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/SparkleView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/common/StudentSelectionControls.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/common/TelegramRadioButton.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/feedback/FeedbackActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/history/HistoryActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/history/HistoryAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/history/HistoryViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/ExamAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/ExamLaunchHelper.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/ExamTestsAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/HomeAppearance.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/HomeBannerAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/HomeCategoryChipGroup.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/HomeUiModels.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/HomeViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/home/MainActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/TargetExamsBottomSheet.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/home/TelegramMenuPopup.kt` | Protected animation/behavior retained; phase 3 native checks passed |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/leaderboard/LeaderboardViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/login/AmbientBackgroundView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/login/LoginActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/mistakes/MistakesAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/performance/PerformanceActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/performance/PerformanceViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/performance/ScoreTrendChartView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/performance/TopicStatAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/practice/PracticeActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/practice/PracticeViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/premium/PaymentMethodBottomSheet.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/premium/PremiumActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/premium/PremiumBenefitRows.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/premium/gl/GLIconRenderer.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/premium/gl/Icon3D.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/premium/gl/ObjLoader.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/premium/gl/Telegram3DStarView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/pyq/PyqActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/pyq/PyqViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/AnswerAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ConfettiView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/result/QuestionTimeChartView.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResponsiveResultRow.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultAnalysisRows.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultCutoffPresentation.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultDataHolder.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/ResultDetailActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultSection.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultStatisticsGrid.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultTabs.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/result/ResultViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/ui/result/SemiCircleGaugeView.kt` | Retired/deleted; zero-reference evidence in cleanup-evidence.json |
| `app/src/main/java/com/eve/app/ui/settings/SettingsActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/syllabus/SyllabusAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/test/QuestionAdapter.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/test/TestActivity.kt` | Migrated; compilation/native inventory PASS; live feature walkthrough unverified |
| `app/src/main/java/com/eve/app/ui/test/TestViewModel.kt` | Data/state logic retained; no UI migration required |
| `app/src/main/java/com/eve/app/util/GlassmorphismHelper.kt` | Protected popup cleanup support retained; unused glass APIs scheduled for Phase 5 audit |
| `app/src/main/java/com/eve/app/worker/DailyReminderWorker.kt` | Feature logic retained; system notification template uses migrated icon; OS controls template typography |
| `app/src/main/java/com/eve/app/worker/SubmitWorker.kt` | Feature logic retained; system notification template uses migrated icon; OS controls template typography |
| `app/src/main/res/layout/activity_about.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_activity_log.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_admin.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_admin_analytics.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_api_usage.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_app_config.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_bookmarks.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_content_display.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_edit_about.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_edit_exam.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_exam_tests.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_feedback.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_feedback_messages.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_flagged_questions.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_generated_tests.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_history.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_leaderboard.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_login.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_main.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_manage_exams.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_manage_existing_exams.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_manage_polls.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_manage_premium.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_manage_syllabus.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_manage_users.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_mistakes.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_notifications.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_payment_checkout.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_performance.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_practice.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_premium.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_profile.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_pyq.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_result.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_result_detail.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_send_notification.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_settings.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_syllabus.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/activity_test.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/bottom_sheet_category_picker.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/bottom_sheet_completed_exam.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/bottom_sheet_font_picker.xml` | Retired: owner explicitly removed Device font; Poppins is fixed |
| `app/src/main/res/layout/bottom_sheet_payment_method.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/bottom_sheet_target_exams.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_add_question.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_create_feedback_post.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_create_poll.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_edit_question_details.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_manage_banners.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_manual_grant_premium.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_post_replies.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_reply_feedback.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/dialog_report_question.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_admin_banner.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_admin_email.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_admin_premium_user.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_admin_syllabus.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_admin_transaction.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_analytics_exam.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_analytics_question.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_answer.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_audit_log.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_bookmark_card.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_category_header.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_exam.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_exam_dropdown.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_exam_test.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_existing_exam_admin.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_feedback_message.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_feedback_post.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_flagged_question.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_generated_test.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_history.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_home_banner.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_leaderboard.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_manage_question.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_mistake.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_notification.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_palette_circle.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_poll_admin.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_poll_option_input.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_poll_option_result.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_post_reply.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_question.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_result_topic.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_sent_broadcast.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_student_action.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_syllabus.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_topic_stat.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/item_user_admin.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/layout_bulletin.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/layout_undo_bar.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/popup_telegram_menu.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/view_empty_state.xml` | Migrated; native light/dark layout inflation PASS |
| `app/src/main/res/layout/view_error_state.xml` | Migrated; native light/dark layout inflation PASS |

| `app/src/main/res/layout/activity_system_monitor.xml` | Migrated; native light/dark inflation PASS |

System Monitor now uses `activity_system_monitor.xml` with shared large-title header and card host. No app-widget provider or RemoteViews layout exists (`rg appWidget/RemoteViews app/src/main`: zero matches). Native fixture coverage is distinct from signed-in end-to-end feature verification.

Final native evidence: [run 39341884146](https://github.com/vishu762701/eve-mock-test-app/actions/runs/39341884146), code `6e2994598fab2bf6e7d36da031d44624aeae813b`, 40/40 per navigation mode. All 95 production XML layouts inflated in both themes. This does not certify every authenticated data state or OS-controlled surface.

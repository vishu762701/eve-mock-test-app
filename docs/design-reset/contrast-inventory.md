# Small system-color text on white

Exact locked colors are retained. This is a source inventory for light-mode white `surface`, not permission to darken the palette. Ratios use WCAG sRGB relative luminance against white. 14–17sp semantic text is listed conservatively, including bold headings. White-on-red destructive buttons are the reverse foreground/background and are not part of this list.

## XML and inherited styles

Source-resolved style, type and nearest solid background. Runtime predicates below can override an initial XML state. Anonymous labels use their literal English label.

| Layout | View/label | Type | System color | Ratio |
| --- | --- | --- | --- | --- |
| `activity_admin.xml` | `Tap to manage users & view test history →` | 14sp | blue | 3.52:1 |
| `activity_admin.xml` | `tvLiveMaintenanceStatus` | 14sp | green | 2.22:1 |
| `activity_admin.xml` | `tvOnlineUsers` | 17sp | green | 2.22:1 |
| `activity_admin.xml` | `tvVersionStatusBadge` | 14sp | green | 2.22:1 |
| `activity_admin_analytics.xml` | `Crashlytics Active` | 14sp | green | 2.22:1 |
| `activity_admin_analytics.xml` | `tvCrashFreePercentage` | 17sp | green | 2.22:1 |
| `activity_api_usage.xml` | `tvDocsCreatedCount` | 17sp | green | 2.22:1 |
| `activity_api_usage.xml` | `tvGeminiCallsCount` | 17sp | blue | 3.52:1 |
| `activity_app_config.xml` | `Firebase Console Remote Config Guide` | 17sp | blue | 3.52:1 |
| `activity_app_config.xml` | `Live Backend Controls` | 17sp | blue | 3.52:1 |
| `activity_content_display.xml` | `tvEmailValue` | 14sp | blue | 3.52:1 |
| `activity_content_display.xml` | `tvPhoneValue` | 14sp | blue | 3.52:1 |
| `activity_content_display.xml` | `tvWebsiteValue` | 14sp | blue | 3.52:1 |
| `activity_edit_exam.xml` | `Category Cutoffs (Optional)` | 17sp | blue | 3.52:1 |
| `activity_edit_exam.xml` | `Exam Information` | 17sp | blue | 3.52:1 |
| `activity_edit_exam.xml` | `tvQuestionsHeader` | 17sp | blue | 3.52:1 |
| `activity_main.xml` | `tvDrawerPremiumStatus` | 14sp | green | 2.22:1 |
| `activity_performance.xml` | `tvOverallAccuracy` | 17sp | green | 2.22:1 |
| `activity_performance.xml` | `tvTotalAttempts` | 17sp | blue | 3.52:1 |
| `activity_performance.xml` | `tvWeakAreas` | 14sp | red | 3.57:1 |
| `activity_performance.xml` | `⚠ Weak Areas` | 17sp | red | 3.57:1 |
| `activity_premium.xml` | `✓ 3-attempt restriction bypassed` | 17sp | green | 2.22:1 |
| `activity_premium.xml` | `✓ All eligible tests unlocked` | 17sp | green | 2.22:1 |
| `activity_premium.xml` | `✓ Unlimited eligible test reattempts` | 17sp | green | 2.22:1 |
| `activity_result_detail.xml` | `tvPerfDetailPercentile` | 17sp | blue | 3.52:1 |
| `activity_result_detail.xml` | `tvStatCorrectMarks` | 17sp | green | 2.22:1 |
| `activity_result_detail.xml` | `tvStatWrongMarks` | 17sp | red | 3.57:1 |
| `activity_settings.xml` | `Delete Account` | 17sp | red | 3.57:1 |
| `bottom_sheet_completed_exam.xml` | `tvCompletedScore` | 14sp | green | 2.22:1 |
| `dialog_reply_feedback.xml` | `tvOriginalSenderName` | 17sp | blue | 3.52:1 |
| `item_analytics_exam.xml` | `tvCategoryBadge` | 17sp | blue | 3.52:1 |
| `item_analytics_exam.xml` | `tvEngagementBadge` | 17sp | red | 3.57:1 |
| `item_analytics_exam.xml` | `tvMeta` | 14sp | blue | 3.52:1 |
| `item_analytics_question.xml` | `tvStats` | 14sp | red | 3.57:1 |
| `item_answer.xml` | `KEY POINTS` | 14sp | blue | 3.52:1 |
| `item_answer.xml` | `tvCorrectAnswer` | 17sp | green | 2.22:1 |
| `item_answer.xml` | `tvQuestionStatusLabel` | 14sp | green | 2.22:1 |
| `item_audit_log.xml` | `tvActionBadge` | 17sp | blue | 3.52:1 |
| `item_bookmark_card.xml` | `Tap to open in test` | 14sp | blue | 3.52:1 |
| `item_bookmark_card.xml` | `tvExamTag` | 14sp | blue | 3.52:1 |
| `item_exam_test.xml` | `tvStatus` | 14sp | blue | 3.52:1 |
| `item_existing_exam_admin.xml` | `tvCategoryBadge` | 17sp | blue | 3.52:1 |
| `item_flagged_question.xml` | `tvExamName` | 14sp | blue | 3.52:1 |
| `item_flagged_question.xml` | `tvFlagCount` | 17sp | red | 3.57:1 |
| `item_generated_test.xml` | `tvScheduledTime` | 14sp | blue | 3.52:1 |
| `item_leaderboard.xml` | `tvScore` | 17sp | blue | 3.52:1 |
| `item_manage_question.xml` | `tvCorrect` | 14sp | green | 2.22:1 |
| `item_mistake.xml` | `tvMistakeType` | 14sp | red | 3.57:1 |
| `item_poll_admin.xml` | `tvTapToView` | 14sp | blue | 3.52:1 |
| `item_sent_broadcast.xml` | `tvBroadcastTarget` | 17sp | blue | 3.52:1 |
| `item_syllabus.xml` | `tvCategory` | 14sp | blue | 3.52:1 |
| `item_user_admin.xml` | `tvUserStatusBadge` | 17sp | green | 2.22:1 |

## Runtime states on solid white surfaces

Each entry names the actual binding or shared role. Recycled rows are listed for the affected state, not as unconditional colors. Chip/tile rows whose backgrounds are colored or inset are excluded from the white-only list.

| Source | Binding/state | System colors / white contrast |
| --- | --- | --- |
| `ui/home/ExamAdapter.kt` | tvExamTime after submission (shared Colors.statusRightText) | green 2.22:1 |
| `ui/home/ExamTestsAdapter.kt` | tvStatus: Submitted / Live / Start | green 2.22:1; blue 3.52:1 |
| `ui/history/HistoryAdapter.kt` | tvScore: pass/fail threshold | green 2.22:1; red 3.57:1 |
| `ui/result/AnswerAdapter.kt` | tvQuestionStatusLabel: correct/incorrect; tvYourAnswer; tvCorrectAnswer | green 2.22:1; red 3.57:1 |
| `ui/result/ResultActivity.kt` | cutoff-selection sheet item.tvSub: Qualified / Not Qualified | green 2.22:1; red 3.57:1 |
| `ui/result/ResultAnalysisRows.kt` | topic row tvTopicResult when hosted in the Analytics solid card | green 2.22:1; orange 2.31:1; red 3.57:1 |
| `ui/performance/TopicStatAdapter.kt` | tvTopicAccuracy when hosted in the white topic card | green 2.22:1; orange 2.31:1; red 3.57:1 |
| `ui/mistakes/MistakesAdapter.kt` | tvMistakeType for wrong-answer state | red 3.57:1 |
| `ui/premium/PaymentCheckoutActivity.kt` | cardStatus.tvStatusText after payment success | green 2.22:1 |
| `ui/admin/AdminPremiumUserAdapter.kt` | tvUserStatusBadge: ACTIVE / EXPIRED / REVOKED | green 2.22:1; red 3.57:1 |
| `ui/admin/AdminTransactionAdapter.kt` | tvOrderStatus: SUCCESS / PENDING / EXPIRED / REFUNDED | green 2.22:1; orange 2.31:1; red 3.57:1; blue 3.52:1 |
| `ui/admin/UserAdminAdapter.kt` | tvUserStatusBadge: Active / Banned | green 2.22:1; red 3.57:1 |
| `ui/admin/PollsAdminAdapter.kt` | tvPollStatus: Active / Expired | green 2.22:1; orange 2.31:1 |
| `ui/admin/AnalyticsExamAdapter.kt` | tvEngagementBadge: no/low/active engagement | red 3.57:1; orange 2.31:1; green 2.22:1 |
| `ui/admin/AdminActivity.kt` | tvLiveMaintenanceStatus; tvVersionStatusBadge (within their solid card) | green 2.22:1; red 3.57:1 |
| `designsystem/DesignInputLayout.kt` | 14sp error/counter helper whenever an input is hosted in a white form card, dialog or sheet | red 3.57:1 |

## Scope exclusions and uncertainty

Inset/meaning-container backgrounds are not white: review option rows and badge fills, correct bookmark options, result cutoff verdict badge, leaderboard first-place badge, generated-test status badge, time-insight tile, question palette cells, slowest-question inset tiles, Mistakes action buttons and category tint labels are excluded. Shared/generated share-card artwork uses an explicit dark context, not a white surface. Charts/rings/icons are not small text. Android-controlled notification typography is outside app text rendering.

This source inventory includes conditional hosts explicitly. Live authenticated data rendering and OS-internal DatePicker error states remain unverified; native fixture checks are separate evidence. No contrast-compliant substitute was introduced.

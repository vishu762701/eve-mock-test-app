# EVE student UI coverage — Task 2

Baseline: `f5a6347bedc8ef63bb17d6198705ddfb1ffbbae8` (verified Task 1 on main).

The current request authorizes the Result/Leaderboard and decorative redesigns that older audit documents prohibited. Functional invariants in the [shared contract](EVE_IOS26_UI_CONTRACT.md) remain protected. No UIKit APIs or proprietary Apple assets are used. Standard Android typography and Material components implement the shared adaptive roles.

## Production trace and decisions

Evidence names below have `student-` prefix and `-light.png` / `-dark.png` suffix. Captures attach production XML and production adapters to the debug fixture Activity with disposable data, then let the main looper deliver widget layout/font callbacks before capture. The 45 cases are measured at 360dp/1×, 320dp/1.5× and 412dp/1× in both themes (270 configurations). The gallery requires 120 student PNGs: 90 standard captures, 16 enlarged-font captures and 14 scrolled-footer captures. Together with the 22 existing shared/window fixtures, CI requires 142 images in each navigation-mode job. They complement the existing full-window hardware, insets, theme-reveal and protected-test suite.

| Screen | Activity → real XML | Dependent presentation | Evidence stems | Changed / retained decision |
|---|---|---|---|---|
| Home / discovery | MainActivity → activity_main | ExamAdapter → item_exam, item_category_header, item_feedback_post; HomeBannerAdapter → item_home_banner; HomeAppearance, AvatarDrawable, EveLiquidGlassView, FlameAnimationView, ShimmerSkeletonView | home, home-drawer, home-feedback, home-loading | White canvas, secondary exam surfaces, blue navigation/controls, category-tinted Lottie artwork, readable metadata; preserve image clipping, banner, Lottie and drawer blur |
| Exam lists / test selection | ExamTestsActivity → activity_exam_tests | ExamTestsAdapter → item_exam_test; ErrorStateView, ShimmerSkeletonView | exam-tests | Grouped list with 12dp row spacing, responsive status rows, accessible score labels and actions |
| Login / signup | LoginActivity → activity_login | AmbientBackgroundView; shared TextInput and button selectors | login, signup | Refined controls and semantic labels; preserve ambient chrome, authentication and errors |
| Profile | ProfileActivity → activity_profile | ShapeableImageView; ExamImageHelper; shared inputs; profile progress dialog | profile | Identity hierarchy, opaque information section with distinct inset chip fills, 48dp camera/back; photo tint removed to preserve images |
| Settings | SettingsActivity → activity_settings | bottom_sheet_font_picker; TargetExamsBottomSheet; switch selectors; MaterialAlertDialogBuilder | settings, font-picker | Grouped settings, adaptive blue toggles, readable captions and destructive roles; wrap rows and scroll |
| Mock Test | TestActivity → activity_test | QuestionAdapter → item_question; QuestionPaletteAdapter → item_question_palette; TelegramRadioButton; CircularTimerView; CompletedExamBottomSheet; ReportQuestionDialog | mock-test | Protected viewport, 2×2 actions, fitting, timer, palette and answer status geometry unchanged; peripheral role cleanup only |
| Results / review | ResultActivity → activity_result | AnswerAdapter → item_answer; LeaderboardAdapter → item_leaderboard; ResultSegmentedTabs; ResponsiveResultRow; ResultStatisticsGrid; ConfettiView | result, review, result-leaderboard | Opaque grouped cards, blue selected tabs (scrollable full labels at enlarged fonts), readable labels and 48dp actions; protected review palette unchanged |
| Result Detail / statistics / standing / analytics | ResultDetailActivity → activity_result_detail | SemiCircleGaugeView; QuestionTimeChartView; ResultStatisticsGrid; ResponsiveResultRow; ResultAnalysisRows → item_result_topic / slowest rows | statistics, standing, analytics | Responsive metrics and comparisons, scaled chart labels, accessible analysis/status text |
| Full Leaderboard | LeaderboardActivity → activity_leaderboard | LeaderboardAdapter → item_leaderboard; AvatarDrawable | leaderboard | Borderless grouped rows, distinct blue current-user surface, meaningful rank medals and readable initials |
| Practice | PracticeActivity → activity_practice | StudentSelectionControls → item_student_action; item_exam_dropdown | practice | Scrollable topic selection, full-width measured actions, native accessible dropdown |
| PYQ | PyqActivity → activity_pyq | StudentSelectionControls → item_student_action/year selectors; item_exam_dropdown | pyq | Scrollable exam/year/paper hierarchy; selected first-year chip reflects production state |
| Syllabus | SyllabusActivity → activity_syllabus | SyllabusAdapter → item_syllabus; shared TextInput | syllabus | Grouped cards, responsive download controls and readable unavailable state; PDF/download handling retained |
| History | HistoryActivity → activity_history | HistoryAdapter → item_history; ShimmerSkeletonView | history | Grouped rows, accessible score emphasis, list padding |
| Bookmarks | BookmarksActivity → activity_bookmarks | BookmarkAdapter → item_bookmark_card; ResponsiveResultRow | bookmarks | Responsive metadata, inset explanation, correct-answer emphasis and 48dp remove action |
| Mistakes | MistakesActivity → activity_mistakes | MistakesAdapter → item_mistake; ResponsiveResultRow | mistakes | Readable learned/explanation states, responsive metadata, usable content viewport |
| Performance | PerformanceActivity → activity_performance | TopicStatAdapter → item_topic_stat; ScoreTrendChartView | performance | Grouped metrics/topic cards; density/font-aware chart axes, empty chart wrapping |
| Notifications | NotificationsActivity → activity_notifications | NotificationAdapter → item_notification; empty Lottie state | notifications, notifications-empty | Opaque rounded rows, accent icon tiles, readable date/body; local/server merging retained |
| Feedback | FeedbackActivity → activity_feedback | canonical outlined TextInput; shared button states | feedback, feedback-error, feedback-loading | Clear form hierarchy, focus/error outline and measured submit action |
| Premium | PremiumActivity → activity_premium | PremiumBenefitRows; Telegram3DStarView; SparkleView; PaymentMethodBottomSheet → bottom_sheet_payment_method | premium, premium-active, payment-method | Opaque content cards and blue actions; retain existing functional star/glass and purchase flow |
| Payment | PaymentCheckoutActivity → activity_payment_checkout | QR ImageView; canonical status cards and buttons | payment, payment-expired, payment-success | Grouped payment hierarchy and accessible success text; retain white QR backing, expiry and payment logic |
| About | AboutActivity → activity_about | shared icon actions and semantic labels | about | White canvas, clear identity and scrollable information rows |
| Content Display | ContentDisplayActivity → activity_content_display | email/phone contact cards; scrollView; loading progress | content, content-contact | Readable body/date/contact roles, bounded title, grouped contact surfaces |
| Student sheets / dialogs / shared states | TargetExamsBottomSheet, CompletedExamBottomSheet, ReportQuestionDialog; Settings and Payment sheets | bottom_sheet_target_exams/completed_exam/category_picker/font_picker/payment_method; dialog_report_question; view_empty_state/view_error_state; Material dialog theme | target-exams, completed-exam, category-picker, font-picker, payment-method, report-question, empty, error | Measured capsule actions, semantic selection indicators, balanced parent/child corners; preserve focus/selection borders |

## Verification coverage

- The native suite verifies actual TextView layout height, including adapter rows, at all three configurations. The protected Mock Test uses the existing native fitting/action/palette tests.
- Additional captured 320dp / 1.5× states: Login, Signup, Settings, Practice, Statistics, Bookmarks, Payment, Review.
- Scrolled-footer captures: Profile, Settings, Result, Analytics, Premium, Payment, Report Question.
- Selected states: blue segmented Results tabs, current-user leaderboard row, selected exam/year chips, expanded Review solution, learned/unlearned mistakes and selected Mock Test answer.
- The screenshot completeness gate requires all student captures in both navigation-mode CI jobs; a missing capture fails verification.
- Admin layouts, retired UI Studio, backend/business logic and protected answer colours are outside the visual redesign.

## Evidence limits

The deterministic Canvas captures do not execute live sign-in, billing, server responses or external PDF/UPI/email apps. GLSurfaceViews use the existing native fallback artwork in these fixtures; hardware animation and blur are covered by the existing window suite where available. Keyboard-open forms, production image payloads and transaction completion still require a signed-in device review. These states must not be reported as visually verified by fixture coverage.

## Final validation

- `./gradlew testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`: **BUILD SUCCESSFUL**; 209 unit tests, zero failures/errors/skips; application and instrumentation APKs built. Lint: zero errors, 1,585 warnings (not suppressed).
- `./gradlew assembleDebugAndroidTest lintDebug`: **BUILD SUCCESSFUL** after updating the enlarged-font tab specification.
- Resource XML parsing: **281 files passed**. `python -B .github/scripts/check_design_system.py`: **196 composited semantic contrast pairs passed**, minimum normal-text contrast 4.51:1.
- `python -B -m unittest discover -s .github/scripts/tests -q`: **11 passed**. `node .github/scripts/verify-docs.mjs`: **28 documents, zero errors/warnings**. `git diff --check`: passed.
- Native `StudentUiRenderingTest`: the full initial 44-case/264-configuration pass found Google-button clipping at 320dp/1.5× in Login and Signup. The fixed, final 17-case/102-configuration rerun passed **both tests**, including actual text height, full action labels, composited text contrast and 17 rendered avatar gradients. The other 28 cases retain their reviewed rendering. All **120 final student PNGs** decoded successfully, with no numbered duplicates.
- Native component regressions: `adb shell am instrument -w -e class com.eve.app.ui.SharedDesignSystemTest,com.eve.app.ui.ApprovedUiRenderingTest,com.eve.app.ui.ApprovedUiFunctionalTest,com.eve.app.ui.ManualUiSpecificationTest com.eve.app.test/androidx.test.runner.AndroidJUnitRunner`: **OK (11 tests)**. This retains protected Test geometry/palette/interaction checks and verifies responsive geometry through 800dp and 2× fonts.
- Every major screen was visually reviewed in Day and Night, including the enlarged-font and footer captures. Confirmed corrections include measured authentication controls, visible bell artwork, separated Mistakes rows, real Premium benefits, Contact Us cards, a complete Bookmarks heading and padded scrolling Results labels.

The final changes use the existing shared roles rather than a second palette. Generic category selection uses blue; report section headings use secondary labels; amber remains meaningful for Premium and protected accuracy/marked semantics. Protected test colours, fitting/navigation/submission geometry, circular theme reveal, image handling, authentication and server authority remain intact. Native capture checking excludes a RippleDrawable's mask from the painted background; contrast thresholds remain unchanged.

Local API 35 rendering used software emulation because KVM was unavailable. A local framework timeout multiplier of 10 prevented stock service startup ANRs; test assertions and CI configuration were not relaxed. GitHub Actions uses KVM with normal framework timeouts and runs the complete UI package independently in gesture and three-button modes.

package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.*
import com.eve.app.databinding.*
import com.eve.app.ui.home.*
import com.eve.app.ui.result.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real production XML + production binders, with disposable data and no network/authentication.
 * Canvas evidence complements the existing hardware/window/reveal suite, not a substitute for it. */
@RunWith(AndroidJUnit4::class)
class StudentUiRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val cases = linkedMapOf(
        "home" to R.layout.activity_main, "home-drawer" to R.layout.activity_main,
        "home-feedback" to R.layout.activity_main, "home-loading" to R.layout.activity_main, "exam-tests" to R.layout.activity_exam_tests,
        "login" to R.layout.activity_login, "signup" to R.layout.activity_login,
        "profile" to R.layout.activity_profile, "settings" to R.layout.activity_settings,
        "mock-test" to R.layout.activity_test, "result" to R.layout.activity_result,
        "review" to R.layout.activity_result, "result-leaderboard" to R.layout.activity_result,
        "statistics" to R.layout.activity_result_detail, "standing" to R.layout.activity_result_detail,
        "analytics" to R.layout.activity_result_detail, "leaderboard" to R.layout.activity_leaderboard,
        "practice" to R.layout.activity_practice, "pyq" to R.layout.activity_pyq,
        "syllabus" to R.layout.activity_syllabus, "history" to R.layout.activity_history,
        "bookmarks" to R.layout.activity_bookmarks, "mistakes" to R.layout.activity_mistakes,
        "performance" to R.layout.activity_performance, "notifications" to R.layout.activity_notifications,
        "feedback" to R.layout.activity_feedback, "feedback-error" to R.layout.activity_feedback,
        "feedback-loading" to R.layout.activity_feedback, "notifications-empty" to R.layout.activity_notifications,
        "payment-success" to R.layout.activity_payment_checkout, "premium" to R.layout.activity_premium,
        "premium-active" to R.layout.activity_premium, "payment" to R.layout.activity_payment_checkout,
        "payment-expired" to R.layout.activity_payment_checkout,
        "about" to R.layout.activity_about, "content" to R.layout.activity_content_display, "content-contact" to R.layout.activity_content_display,
        "target-exams" to R.layout.bottom_sheet_target_exams,
        "completed-exam" to R.layout.bottom_sheet_completed_exam,
        "category-picker" to R.layout.bottom_sheet_category_picker,
        "font-picker" to R.layout.bottom_sheet_font_picker,
        "payment-method" to R.layout.bottom_sheet_payment_method,
        "report-question" to R.layout.dialog_report_question,
        "empty" to R.layout.view_empty_state, "error" to R.layout.view_error_state
    )
    private val exam = Exam(id = "fixture", examName = "SSC CGL • Graduate Level", category = "SSC",
        syllabusUrl = "https://example.invalid/fixture.pdf", syllabusFileName = "ssc_cgl_syllabus.pdf")
    private val answer = AnswerItem(questionId = "fixture", number = 1,
        questionText = "Which city is the capital of France?", selected = "A", selectedText = "Berlin",
        correct = "C", correctText = "Paris", explanation = "Paris is the capital of France.",
        optionA = "Berlin", optionB = "Madrid", optionC = "Paris", optionD = "Rome", timeTakenSeconds = 42)

    @Test fun allStudentScreensRenderInBothThemesAndPhoneSizes() {
        val failures = mutableListOf<String>()
        val baseline = InstrumentationRegistry.getArguments().getString("visualPhase") == "baseline"
        androidx.test.core.app.ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            val selectedCases = InstrumentationRegistry.getArguments().getString("visualCases")?.split(",")?.toSet()
            for (dark in listOf(false, true)) for ((name, resource) in cases.filterKeys { selectedCases == null || it in selectedCases }) {
                for ((width, font) in listOf(360 to 1f, 320 to 1.5f, 412 to 1f)) {
                    if (baseline && width != 360) continue
                    android.util.Log.i("EveStudentUi", "Render $name ${if (dark) "night" else "day"} $width/$font")
                    lateinit var root: View
                    val settled = java.util.concurrent.CountDownLatch(1)
                    scenario.onActivity { activity ->
                        val config = Configuration(activity.resources.configuration).apply {
                            uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or
                                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                            fontScale = font; screenWidthDp = width
                        }
                        val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.Theme_Eve)
                        root = activity.layoutInflater.cloneInContext(context).inflate(resource, FrameLayout(context), false)
                        prepare(name, root)
                        // Canvas fixtures use fallback artwork; keep unattached GL
                        // surfaces out of this hierarchy (real GL/chrome tests remain).
                        walk(root).filterIsInstance<android.opengl.GLSurfaceView>().toList().forEach { (it.parent as? ViewGroup)?.removeView(it) }
                        val density = context.resources.displayMetrics.density
                        val height = if (root.layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) ViewGroup.LayoutParams.WRAP_CONTENT else Math.round(800 * density)
                        activity.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                        activity.setContentView(FrameLayout(activity).apply {
                            addView(root, FrameLayout.LayoutParams(Math.round(width * density), height))
                        })
                        freezeFixtureAnimations(root)
                        root.postOnAnimation { root.postOnAnimation { settled.countDown() } }
                    }
                    // Widgets need an attached hierarchy and a free main looper: this
                    // delivers global-layout/font callbacks and settles floating labels.
                    assertTrue("Two native frames must settle $name $width/$font", settled.await(45, java.util.concurrent.TimeUnit.SECONDS))
                    scenario.onActivity {
                        val density = root.resources.displayMetrics.density
                        repeat(3) {
                            root.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width * density), View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(Math.round(800 * density), if (root.layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) View.MeasureSpec.AT_MOST else View.MeasureSpec.EXACTLY))
                            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                        }
                        assertTrue("$name has a usable native viewport", root.width > 0 && root.height > 0)
                        if (!baseline && name != "mock-test") {
                            try { checkText(root, "$name $width/$font", root.context.getColor(R.color.eve_canvas)) }
                            catch (error: AssertionError) { failures.add(error.message.orEmpty()); android.util.Log.e("EveStudentUi", error.message.orEmpty()) }
                        }
                        if (width == 360 || (width == 320 && name in listOf("login", "signup", "settings", "practice", "statistics", "bookmarks", "payment", "review"))) {
                            capture("${if (baseline) "baseline" else "student"}-$name${if (width == 320) "-large" else ""}-${if (dark) "dark" else "light"}", root)
                        }
                        if (width == 360 && name in listOf("profile", "settings", "result", "analytics", "premium", "payment", "report-question")) {
                            walk(root).filterIsInstance<ScrollView>().firstOrNull()?.let { it.scrollTo(0, it.getChildAt(0).height) }
                            walk(root).filterIsInstance<androidx.core.widget.NestedScrollView>().firstOrNull()?.let { it.scrollTo(0, it.getChildAt(0).height) }
                            capture("${if (baseline) "baseline" else "student"}-$name-footer-${if (dark) "dark" else "light"}", root)
                        }
                        walk(root).filterIsInstance<com.airbnb.lottie.LottieAnimationView>().forEach { it.cancelAnimation() }
                    }
                    scenario.onActivity { it.setContentView(FrameLayout(it)) }

                }
            }
        }
        assertTrue(failures.distinct().joinToString("\n"), failures.isEmpty())
    }

    @Test fun avatarInitialsRemainReadableAcrossIdentityGradients() {
        for (index in -1..15) {
            val avatar = com.eve.app.util.AvatarDrawable("AS", index)
            avatar.setBounds(0, 0, 128, 128)
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            try {
                avatar.draw(Canvas(bitmap))
                val paint = com.eve.app.util.AvatarDrawable::class.java.getDeclaredField("textPaint").apply { isAccessible = true }.get(avatar) as android.graphics.Paint
                for (position in listOf(20, 108)) {
                    assertTrue("Readable initials on rendered avatar $index", androidx.core.graphics.ColorUtils.calculateContrast(paint.color, bitmap.getPixel(position, position)) >= 4.5)
                }
            } finally { bitmap.recycle() }
        }
    }

    private fun prepare(name: String, root: View) {
        val context = root.context
        fun show(id: Int, visible: Boolean = true) { root.findViewById<View>(id)?.visibility = if (visible) View.VISIBLE else View.GONE }
        fun text(id: Int, value: String) { root.findViewById<TextView>(id)?.text = value }
        for (id in listOf(R.id.progressGroup, R.id.messageGroup, R.id.progressBar, R.id.progress,
            R.id.shimmerSkeletonHome, R.id.shimmerSkeletonTest, R.id.shimmerSkeletonHistory)) show(id, false)
        if (name.startsWith("payment") && name != "payment-method") {
            text(R.id.tvPlanName, "EVE Premium • 30 days"); text(R.id.tvAmount, "₹99"); text(R.id.tvTimer, "Expires in 14:32")
            val image = com.eve.app.util.QrCodeGenerator.generateBitmap("EVE_FIXTURE_NOT_A_PAYMENT", 180)
            root.findViewById<ImageView>(R.id.ivQrCode).setImageBitmap(image)
        }
        if (name.startsWith("premium")) { com.eve.app.ui.premium.PremiumBenefitRows.bind(root.findViewById(R.id.layoutBenefitsList), listOf("Access to all eligible tests", "Unlimited eligible reattempts", "No normal 3-attempt restriction while Premium is active")); text(R.id.tvPrice, "₹99"); text(R.id.tvValidity, "Validity: 30 days"); text(R.id.tvActiveExpiry, "Expires: 10 Nov 2026") }
        when (name) {
            "home", "home-drawer", "home-feedback", "home-loading" -> {
                HomeAppearance.tintNotificationBell(root.findViewById(R.id.btnNotification))
                text(R.id.tvWelcome, "Hi, Ananya"); show(R.id.layoutStreakPill); text(R.id.tvStreakSummary, "7-day streak")
                val group = root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupCategory)
                for (label in listOf("All", "SSC", "Banking")) group.addView(HomeAppearance.categoryChip(context, label, "All") {})
                recycler(root, R.id.rvExams, ExamAdapter({ _, _, _ -> }).apply { submit(listOf(HomeListItem.Header("Recommended exams")) +
                    List(6) { HomeListItem.ExamRow(exam.copy(id = "exam-$it", category = if (it == 0) "SSC" else "Banking", examName = if (it == 0) exam.examName else "Banking • Practice ${it + 1}"), it == 1, it == 0) }) })
                if (name == "home-drawer") { text(R.id.tvDrawerUserName, "Ananya Sharma"); text(R.id.tvDrawerUserEmail, "ananya@example.invalid"); root.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawerLayout).openDrawer(androidx.core.view.GravityCompat.START, false) }
                if (name == "home-feedback") recycler(root, R.id.rvExams, ExamAdapter({ _, _, _ -> }).apply { submit(listOf(HomeListItem.FeedbackPostRow(FeedbackPost(id = "post", title = "Help shape your next practice session", message = "Which topics would you like to see next? Share your feedback with the team.", timestamp = 1780000000000L)))) })
                if (name == "home-loading") { show(R.id.rvExams, false); show(R.id.shimmerSkeletonHome) }
            }
            "exam-tests" -> recycler(root, R.id.rvTests, ExamTestsAdapter({}, { _, _ -> }).apply {
                submitList(List(5) { i -> ExamTestsListItem.TestItem(GeneratedTest(id = "$i"), "Mock Test ${i + 1}", "20 questions • 30 minutes", i == 1, i == 2, i == 3, "Opens tomorrow") })
            }).also { text(R.id.tvTitle, exam.examName) }
            "signup" -> { show(R.id.tilName); text(R.id.tvTagline, "Create your account"); text(R.id.btnSubmit, "Sign Up"); text(R.id.btnToggleMode, "Already have an account? Sign In") }
            "profile" -> { assertNull("Student photos must retain their original image colours", root.findViewById<ImageView>(R.id.ivProfilePhoto).imageTintList); text(R.id.tvName, "Ananya Sharma"); text(R.id.tvEmail, "ananya@example.invalid"); text(R.id.tvEveId, "EV-ABC123"); text(R.id.etName, "Ananya Sharma"); text(R.id.etDob, "12/04/2001"); show(R.id.chipPremium) }
            "settings" -> { root.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchReminder).isChecked = true; text(R.id.tvDailyGoalValue, "3 tests per day"); text(R.id.tvLanguageValue, "English") }
            "result", "review", "result-leaderboard" -> {
                val b = ActivityResultBinding.bind(root); ResultTabs.bind(b) {}
                text(R.id.tvScore, "76.5 / 100"); text(R.id.tvScorePercentage, "76.5% Score"); text(R.id.tvCorrectCount, "80")
                text(R.id.tvWrongCount, "14"); text(R.id.tvUnattemptedCount, "6"); text(R.id.tvAccuracy, "85.1%")
                text(R.id.tvRank, "12 / 486"); text(R.id.tvPercentile, "97.5%")
                text(R.id.tvTopperAvg, "Topper: 96 • Avg: 62"); text(R.id.tvHistoryBestAvg, "82 / 70")
                b.gaugeTopicAccuracy.setPercentage(85f); show(R.id.btnReattempt)
                if (name == "review") { b.tabLayoutResult.getTabAt(1)!!.select(); recycler(root, R.id.rvAnswers, AnswerAdapter().apply { submit(listOf(answer, answer.copy(number = 2, questionId = "other", selected = "C"))); expandSolution("fixture") }) }
                if (name == "result-leaderboard") { b.tabLayoutResult.getTabAt(2)!!.select(); text(R.id.tvLeaderboardTabRank, "#12 / 486"); text(R.id.tvLeaderboardScoreChip, "Score: 76.5"); text(R.id.tvLeaderboardPercentileChip, "Percentile: 97.5%") }
            }
            "statistics", "standing", "analytics" -> {
                val b = ActivityResultDetailBinding.bind(root)
                show(when (name) { "statistics" -> R.id.layoutContainerStatistics; "standing" -> R.id.layoutContainerPerformance; else -> R.id.layoutContainerAnalytics })
                text(R.id.tvDetailTitle, name.replaceFirstChar { it.uppercase() }); text(R.id.tvStatHeroScore, "76.5 / 100")
                text(R.id.tvStatHeroPercentage, "76.5% Score"); text(R.id.tvStatRightCount, "80"); text(R.id.tvStatWrongCount, "14")
                text(R.id.tvStatUnattemptedTileCount, "6"); text(R.id.tvStatAccuracyPct, "85.1%")
                text(R.id.tvStatCorrectMarks, "+80"); text(R.id.tvStatWrongMarks, "−3.5"); text(R.id.tvStatUnattemptedCount, "6"); text(R.id.tvStatFinalNetScore, "76.5")
                text(R.id.tvPerfDetailRank, "#12 / 486"); text(R.id.tvPerfDetailPercentile, "97.5 percentile"); text(R.id.tvPerfDetailSummary, "You scored above the average by 14.5 marks.")
                text(R.id.tvPerfYourScore, "76.5"); text(R.id.tvPerfAvgScore, "62"); text(R.id.tvPerfTopperScore, "96")
                if (name == "analytics") {
                    b.chartTimeView.setItems(List(8) { QuestionTimeChartView.BarItem(it + 1, 12L + it * 6, it % 3 != 0, true) })
                    for ((topic, correct) in listOf("Quantitative aptitude" to 3, "Reasoning" to 5, "General awareness" to 9)) b.layoutTopicList.addView(ResultAnalysisRows.topic(context, com.eve.app.util.TopicAccuracy(topic, correct, 10, correct * 10.0)))
                    for (index in 1..3) b.layoutSlowestList.addView(ResultAnalysisRows.slowest(context, answer.copy(number = index, timeTakenSeconds = 80L - index * 12)))
                    show(R.id.btnPracticeWeakest)
                }
            }
            "leaderboard" -> {
                show(R.id.cardYourRank); text(R.id.tvYourRank, "#12 / 486"); text(R.id.tvRankMedallion, "12"); text(R.id.tvYourPercentile, "97.5 percentile"); text(R.id.tvYourScore, "76.5")
                recycler(root, R.id.rvLeaderboard, com.eve.app.ui.leaderboard.LeaderboardAdapter("me").apply { submit(List(6) { LeaderboardEntry(userId = if (it == 2) "me" else "$it", displayName = listOf("Aarav Patel", "Ishita Singh", "Ananya Sharma")[it % 3], score = 98.0 - it * 2, total = 100) }) })
            }
            "history" -> recycler(root, R.id.rvHistory, com.eve.app.ui.history.HistoryAdapter {}.apply { submit(List(5) { TestAttempt(examName = exam.examName, category = "SSC", timestamp = 1780000000000L, score = 76.5, total = 100, correct = 80, wrong = 14, unattempted = 6) }) })
            "notifications" -> recycler(root, R.id.rvNotifications, com.eve.app.ui.notifications.NotificationAdapter().apply { submit(List(4) { com.eve.app.util.StoredNotification("New mock test available", "A new SSC practice test is ready. Continue preparing at your own pace.", 1780000000000L, false) }) })
            "syllabus" -> staticRows(root, R.id.rvSyllabus, List(3) { index ->
                com.eve.app.ui.syllabus.SyllabusAdapter({ _, _ -> }, {}).onCreateViewHolder(FrameLayout(context), 0).apply { bind(exam.copy(id = "$index", syllabusUrl = if (index == 2) "" else exam.syllabusUrl)) }.itemView
            })
            "bookmarks" -> staticRows(root, R.id.rvBookmarks, listOf(com.eve.app.ui.bookmarks.BookmarkAdapter({}, {}, { false }).onCreateViewHolder(FrameLayout(context), 0).apply {
                bind(BookmarkedQuestion(questionId = "fixture", examName = exam.examName, questionNumber = 3, questionText = answer.questionText, optionA = "Berlin", optionB = "Madrid", optionC = "Paris", optionD = "Rome", correctAnswer = "C", explanation = answer.explanation, topic = "General awareness"))
            }.itemView))
            "mistakes" -> staticRows(root, R.id.rvMistakes, listOf(false, true).map { learned ->
                com.eve.app.ui.mistakes.MistakesAdapter({}, { learned }, { false }).onCreateViewHolder(FrameLayout(context), 0).apply {
                    bind(com.eve.app.data.remote.MistakeItem(questionId = "$learned", questionText = answer.questionText, selected = "A", selectedText = "Berlin", correct = "C", correctText = "Paris", explanation = answer.explanation))
                }.itemView
            })
            "performance" -> {
                show(R.id.contentGroup); text(R.id.tvTotalAttempts, "24"); text(R.id.tvOverallAccuracy, "82%")
                root.findViewById<com.eve.app.ui.performance.ScoreTrendChartView>(R.id.chartTrend).submit(List(6) { ScorePoint(it.toLong(), "Practice", 40 + it * 8) })
                recycler(root, R.id.rvTopics, com.eve.app.ui.performance.TopicStatAdapter().apply { submit(listOf(TopicStat("Quantitative aptitude", 5, 10), TopicStat("Reasoning", 8, 10))) })
            }
            "practice", "pyq" -> {
                root.findViewById<Spinner>(R.id.spExam).adapter = ArrayAdapter(context, R.layout.item_exam_dropdown, listOf(exam.examName))
                val group = root.findViewById<com.google.android.material.chip.ChipGroup>(if (name == "practice") R.id.chipGroupTopics else R.id.chipGroupPapers)
                for (label in listOf("Reasoning • 20 questions", "General awareness • 30 questions", "Quantitative aptitude • 18 questions")) group.addView(com.eve.app.ui.common.StudentSelectionControls.action(context).apply { text = label })
                if (name == "pyq") { show(R.id.tvPapersLabel); root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupYears).addView(com.eve.app.ui.common.StudentSelectionControls.year(context).apply { text = "2025"; isChecked = true }) }
            }
            "premium-active" -> { show(R.id.layoutNotPremium, false); show(R.id.layoutPremiumActive) }
            "feedback-error" -> root.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilFeedback).error = "Please enter your message"
            "feedback-loading" -> { text(R.id.etFeedbackMessage, "Please add more reasoning practice."); show(R.id.progressBar); root.findViewById<View>(R.id.btnSendFeedback).isEnabled = false }
            "notifications-empty" -> { HomeAppearance.tintNotificationBell(root.findViewById(R.id.lottieEmpty)); show(R.id.emptyGroup); show(R.id.rvNotifications, false) }
            "payment-success" -> { text(R.id.tvStatusText, "Payment received"); root.findViewById<TextView>(R.id.tvStatusText).setTextColor(context.getColor(R.color.eve_status_success_text)) }
            "payment-expired" -> { show(R.id.layoutQrSection, false); show(R.id.layoutExpired); show(R.id.cardStatus, false) }
            "about" -> text(R.id.tvVersion, "Version 1.0")
            "payment-method" -> text(R.id.tvSheetSubtitle, "Select how you would like to pay ₹99")
            "content" -> { text(R.id.tvHeaderTitle, "Privacy Policy"); text(R.id.tvTitle, "Your privacy"); text(R.id.tvLastUpdated, "Updated 11 October 2026"); text(R.id.tvBody, "Your learning data belongs to you.\n\nEVE stores your practice history to sync progress across devices. You can manage your account and preferences in Settings.\n\nContact support if you need help with your account.") }
            "content-contact" -> { text(R.id.tvHeaderTitle, "Contact Us"); text(R.id.tvTitle, "We are here to help"); show(R.id.tvLastUpdated, false); show(R.id.layoutContactActions); text(R.id.tvEmailValue, "support@example.invalid"); text(R.id.tvPhoneValue, "+91 90000 00000"); text(R.id.tvWebsiteValue, "https://example.invalid/eve"); text(R.id.tvBody, "Choose a contact option above for help with your account or practice sessions.") }
            "completed-exam" -> { text(R.id.tvCompletedTitle, "SSC CGL • Mock Test 1"); text(R.id.tvCompletedScore, "Score: 76.5 / 100") }
            "target-exams" -> for (label in listOf("SSC CGL", "Banking", "Railways", "Teaching")) root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupExams).addView(HomeAppearance.categoryChip(context, label, "SSC CGL") {})
            "mock-test" -> {
                text(R.id.tvTestTitle, "SSC CGL • Mock Test 1")
                root.findViewById<com.eve.app.ui.common.CircularTimerView>(R.id.circularTimerView).setTime(120, 600)
                root.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).adapter = com.eve.app.ui.test.QuestionAdapter(
                    listOf(Question(id = "fixture", questionText = answer.questionText, optionA = "Berlin", optionB = "Madrid", optionC = "Paris", optionD = "Rome")),
                    { "C" }, { _, _ -> }, { false }, {}, { false }, {}, { 42L })
                root.findViewById<RecyclerView>(R.id.rvQuestionPalette).apply {
                    layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                    adapter = com.eve.app.ui.common.QuestionPaletteAdapter(true) {}.apply {
                        submit(List(8) { com.eve.app.ui.common.PaletteItem(it + 1, if (it == 0) com.eve.app.ui.common.PaletteState.ANSWERED else com.eve.app.ui.common.PaletteState.UNATTEMPTED, it == 0) })
                    }
                }
            }
            "error" -> { text(R.id.tvErrorTitle, "Unable to load content"); text(R.id.tvErrorMessage, "Check your connection and try again."); show(R.id.btnRetry) }
            "empty" -> { text(R.id.tvEmptyTitle, "All caught up"); text(R.id.tvEmptyMessage, "Your saved content will appear here."); show(R.id.tvEmptyMessage) }
        }
        // GLSurfaceViews require the existing hardware evidence; show native fallback artwork in Canvas fixtures.
        for (id in listOf(R.id.lottieDrawerPremiumStar, R.id.starViewNotPremium, R.id.starViewPremiumActive)) show(id, false)
        for (id in listOf(R.id.ivHeaderStarFallback, R.id.ivActiveStarFallback)) show(id)
    }

    private fun freezeFixtureAnimations(root: View) {
        // Deterministic stills; the existing native suite verifies actual playback/reveal.
        walk(root).filterIsInstance<com.airbnb.lottie.LottieAnimationView>().forEach { it.cancelAnimation(); it.progress = 1f }
        walk(root).filter { it is com.eve.app.ui.common.ShimmerSkeletonView || it is com.eve.app.ui.common.FlameAnimationView }.forEach {
            (it.javaClass.getDeclaredField("animator").apply { isAccessible = true }.get(it) as? android.animation.ValueAnimator)?.cancel()
        }
    }

    private fun recycler(root: View, id: Int, adapter: RecyclerView.Adapter<*>) {
        root.findViewById<RecyclerView>(id)?.apply { layoutManager = LinearLayoutManager(context); layoutAnimation = null; itemAnimator = null; this.adapter = adapter }
    }
    private fun staticRows(root: View, id: Int, rows: List<View>) = recycler(root, id, object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = rows.size
        override fun getItemViewType(position: Int) = position
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(rows[viewType]) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    })
    private fun walk(view: View): Sequence<View> = sequence {
        yield(view); if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(walk(view.getChildAt(i)))
    }
    private fun checkText(view: View, label: String, inherited: Int, inheritedAlpha: Float = 1f) {
        if (view.visibility != View.VISIBLE || view.alpha <= 0f) return
        val alpha = inheritedAlpha * view.alpha
        val background = androidx.core.graphics.ColorUtils.compositeColors(surfaceColor(view) ?: android.graphics.Color.TRANSPARENT, inherited)
        if (view is TextView && view.text.isNotBlank() && view.height > 0 && view.width > 0) {
            val layout = view.layout
            if (layout != null && view !is EditText) assertTrue("Readable text height: $label / ${view.text}",
                layout.height <= view.height - view.compoundPaddingTop - view.compoundPaddingBottom)
        }
        if (view is TextView && view.text.isNotBlank() && view.height > 0 && view.width > 0) {
            if (view is com.google.android.material.button.MaterialButton) {
                for (line in 0 until view.lineCount) assertEquals("Action label must fit: $label / ${view.text}", 0, view.layout.getEllipsisCount(line))
            }
            val foreground = androidx.core.graphics.ColorUtils.compositeColors(androidx.core.graphics.ColorUtils.setAlphaComponent(view.currentTextColor, Math.round(android.graphics.Color.alpha(view.currentTextColor) * alpha)), background)
            val largeOrPrimary = view.currentTextColor == view.context.getColor(R.color.eve_on_primary) ||
                (view.textSize / view.resources.displayMetrics.scaledDensity >= if (view.typeface?.isBold == true) 18f else 24f)
            val ratio = androidx.core.graphics.ColorUtils.calculateContrast(foreground, background)
            val lockedSystemColors = listOf(R.color.system_red, R.color.system_green, R.color.system_blue,
                R.color.system_indigo, R.color.system_orange, R.color.system_purple, R.color.system_yellow)
                .map { view.context.getColor(it) }
            val lockedDisabled = view is com.google.android.material.button.MaterialButton && !view.isEnabled &&
                view.currentTextColor == view.context.getColor(R.color.text_secondary) &&
                view.backgroundTintList?.getColorForState(view.drawableState, 0) == view.context.getColor(R.color.track)
            if (lockedDisabled) {
                assertEquals("Disabled roles keep their specified opacity: $label / ${view.text}", 1f, alpha, .001f)
            } else if (view.currentTextColor in lockedSystemColors) {
                // The owner mandates exact system hues and asks that low ratios be
                // listed, never repaired by substituting darker colors.
                assertEquals("Locked system labels retain full opacity: $label / ${view.text}", 1f, alpha, .001f)
                if (ratio < 4.5) android.util.Log.i("LockedColorContrast",
                    "$label / ${view.text}: $ratio; foreground=${view.currentTextColor}; background=$background")
            } else {
                assertTrue("Text contrast $label / ${view.text}: $ratio", ratio >= if (largeOrPrimary) 3.0 else 4.5)
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) checkText(view.getChildAt(i), label, background, alpha)
    }
    private fun surfaceColor(view: View): Int? {
        if (view is com.google.android.material.button.MaterialButton) return view.backgroundTintList?.getColorForState(view.drawableState, 0)
        if (view is com.google.android.material.chip.Chip) return view.chipBackgroundColor?.getColorForState(view.drawableState, 0)
        if (view is com.google.android.material.card.MaterialCardView) return view.cardBackgroundColor.getColorForState(view.drawableState, 0)
        fun color(drawable: android.graphics.drawable.Drawable?): Int? = when (drawable) {
            is android.graphics.drawable.ColorDrawable -> drawable.color
            is android.graphics.drawable.GradientDrawable -> drawable.color?.getColorForState(view.drawableState, 0)
            is com.google.android.material.shape.MaterialShapeDrawable -> drawable.fillColor?.getColorForState(view.drawableState, 0)
            is android.graphics.drawable.InsetDrawable -> color(drawable.drawable)
            is android.graphics.drawable.StateListDrawable -> color(drawable.current)
            is android.graphics.drawable.RippleDrawable -> (0 until drawable.numberOfLayers)
                .lastOrNull { drawable.getId(it) != android.R.id.mask }?.let { color(drawable.getDrawable(it)) }
            is android.graphics.drawable.LayerDrawable -> if (drawable.numberOfLayers > 0) color(drawable.getDrawable(0)) else null
            else -> null
        }
        var ancestor = view.parent
        while (ancestor is View) {
            if (ancestor is com.google.android.material.tabs.TabLayout && view.isSelected &&
                ancestor.getTabAt(ancestor.selectedTabPosition)?.customView === view) {
                // The indicator is painted beneath the selected label, not assigned
                // as its background; use that real drawable for composited contrast.
                return color(ancestor.tabSelectedIndicator)
            }
            ancestor = (ancestor as View).parent
        }
        return color(view.background)
    }
    private fun capture(name: String, root: View) {
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap); canvas.drawColor(root.context.getColor(R.color.eve_canvas)); root.draw(canvas)
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-student-ui")
            }
            val resolver = instrumentation.targetContext.contentResolver
            // Re-running a case must replace its disposable fixture, not leave
            // a stale image plus a numbered filename that escapes the CI gate.
            resolver.delete(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "${android.provider.MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${android.provider.MediaStore.Images.Media.RELATIVE_PATH} = ?",
                arrayOf("$name.png", "Pictures/eve-student-ui/"))
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
}

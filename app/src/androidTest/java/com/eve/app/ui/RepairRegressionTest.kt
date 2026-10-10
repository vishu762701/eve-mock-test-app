package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.databinding.ActivityTestBinding
import com.eve.app.databinding.ItemQuestionBinding
import com.eve.app.ui.result.ResultTabs
import com.eve.app.ui.result.ResultCutoffPresentation
import com.eve.app.ui.test.QuestionAdapter
import com.eve.app.util.SystemBarHelper
import com.eve.app.util.ThemeSwitchAnimator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepairRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var captureWindow: android.view.Window? = null

    @Test fun persistedAnswersClearAndResumeOnlyTheCorrectOwnerExamAndAttempt() {
        val store = com.eve.app.data.local.TestSessionStore(instrumentation.targetContext)
        val key = "instrumentation-${java.util.UUID.randomUUID()}__test"
        val owner = "isolated-instrumentation-owner"
        val saved = com.eve.app.data.local.TestSession(key, "attempt-one", owner, mapOf("q1" to "B"))
        try {
            store.saveSession(saved)
            assertEquals("B", store.getSession(key, owner)!!.answers["q1"])
            assertNull(store.getSession(key, "other-owner"))
            assertNull(store.getSession("other-exam", owner))
            assertTrue(com.eve.app.data.local.TestSessionPolicy.canRestore(store.getSession(key, owner), owner, key, "attempt-one"))
            assertFalse(com.eve.app.data.local.TestSessionPolicy.canRestore(store.getSession(key, owner), owner, key, "new-reattempt"))
            store.saveSession(saved.copy(answers = emptyMap()))
            val reopened = com.eve.app.data.local.TestSessionStore(instrumentation.targetContext)
            assertTrue(reopened.getSession(key, owner)!!.answers.isEmpty())
            reopened.saveSession(saved.copy(attemptKey = "different-record"))
            // A corrupted record cannot restore simply because its preference key matches.
            instrumentation.targetContext.getSharedPreferences("test_session_store_v1", 0).edit()
                .putString("$owner:$key", com.google.gson.Gson().toJson(saved.copy(attemptKey = "different-record"))).commit()
            assertNull(reopened.getSession(key, owner))
        } finally { store.clearSession(key, owner); store.clearSession("different-record", owner) }
    }

    @Test fun recycledQuestionsResetAnimatedIndicatorAndGreenDrawableImmediatelyInBothThemes() {
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply { uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or night }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val saved = mutableMapOf<Int, String>()
                val adapter = QuestionAdapter(List(2) { Question(id = "q$it", questionText = "Question $it", optionA = "First", optionB = "Second") },
                    { saved[it].orEmpty() }, { i, answer -> saved[i] = answer }, { false }, {}, { false }, {}, { 0 })
                val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
                val b = ItemQuestionBinding.bind(holder.itemView)
                adapter.onBindViewHolder(holder, 0)
                assertEquals(-1, b.rgOptions.checkedRadioButtonId)
                b.rbA.performClick()
                assertEquals("A", saved[0])
                // Recycle while the 200ms check animation is still running.
                adapter.onBindViewHolder(holder, 1)
                assertEquals(-1, b.rgOptions.checkedRadioButtonId)
                assertNull(saved[1])
                val d = context.resources.displayMetrics.density
                val width = (240 * d).toInt(); val height = (56 * d).toInt()
                fun pixels(): Bitmap {
                    b.rbA.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    b.rbA.layout(0, 0, width, height)
                    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { b.rbA.draw(Canvas(it)) }
                }
                pixels().also {
                    assertEquals("A fresh option must have no animated dot", context.getColor(R.color.eve_test_option_bg), it.getPixel((22 * d).toInt(), height / 2))
                    it.recycle()
                }
                adapter.onBindViewHolder(holder, 0)
                pixels().also { assertEquals(Color.parseColor("#34C759"), it.getPixel((80 * d).toInt(), (4 * d).toInt())); it.recycle() }
                b.rbB.performClick()
                pixels().also {
                    assertEquals("Changing answer immediately clears the old indicator", context.getColor(R.color.eve_test_option_bg), it.getPixel((22 * d).toInt(), height / 2))
                    it.recycle()
                }
                saved.remove(0); holder.clearSelection(); adapter.onBindViewHolder(holder, 1); adapter.onBindViewHolder(holder, 0)
                assertEquals(-1, b.rgOptions.checkedRadioButtonId)
                assertFalse(b.rbA.isSaveEnabled)
                assertFalse(b.rgOptions.isSaveEnabled)
            }
        }
    }

    @Test fun resultMetricsRemainReadableAtSmallWidthsAndLargeFonts() {
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) for (width in listOf(320, 390, 600)) for (font in listOf(1f, 1.5f)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or night
                    fontScale = font
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val b = ActivityResultBinding.inflate(LayoutInflater.from(context))
                ResultTabs.bind(b) {}
                ResultCutoffPresentation.bind(b, "General", 0.0, emptyMap())
                assertEquals(View.GONE, b.tvCutoffVerdict.visibility)
                assertEquals(View.GONE, b.tvCutoffScore.visibility)
                assertTrue(b.tvCutoffRelationship.text.contains("cannot be determined"))
                ResultCutoffPresentation.bind(b, "General", 0.0, mapOf("General" to 0.0))
                assertEquals(View.VISIBLE, b.tvCutoffVerdict.visibility)
                assertEquals("Qualified ✓", b.tvCutoffVerdict.text.toString())
                b.tvAnalysisAccuracy.text = "100.0%"; b.tvAnalysisAttempted.text = "200/200"
                b.tvRank.text = "123456 / 999999"; b.tvPercentile.text = "100.0%"
                val d = context.resources.displayMetrics.density
                repeat(2) { b.root.measure(View.MeasureSpec.makeMeasureSpec((width * d).toInt(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec((900 * d).toInt(), View.MeasureSpec.EXACTLY)); b.root.layout(0, 0, (width * d).toInt(), (900 * d).toInt()) }
                fun readable(view: TextView) {
                    val layout = view.layout ?: error("Missing text layout")
                    assertEquals("Text lost: ${view.text}, width=$width font=$font", view.text.length, layout.getLineEnd(layout.lineCount - 1))
                    assertEquals("Ellipsized text: ${view.text}", 0, (0 until layout.lineCount).sumOf { layout.getEllipsisCount(it) })
                    assertTrue("Clipped text height: ${view.text}", view.height - view.compoundPaddingTop - view.compoundPaddingBottom >= layout.height)
                    for (line in 0 until layout.lineCount) assertTrue("Overflow: ${view.text}", layout.getLineWidth(line) <= view.width - view.compoundPaddingLeft - view.compoundPaddingRight + 1)
                }
                listOf(b.tvAnalysisAccuracy, b.tvAnalysisAttempted, b.tvRank, b.tvPercentile).forEach(::readable)
                fun walk(view: View) {
                    if (view is TextView && view.text in listOf("Unattempted", "Accuracy", "Attempted")) readable(view)
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) walk(view.getChildAt(i))
                }
                walk(b.rowOverviewStatisticsTiles); walk(b.gaugeTopicAccuracy.parent as View)
            }
        }
    }

    @Test fun insetsPreservePaddingAndDoNotAccumulateOrDoubleApplyToChildren() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val padded = FrameLayout(activity).apply { setPadding(11, 13, 17, 19) }
                SystemBarHelper.setupInsetsListener(padded)
                val supplied = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(7, 24, 9, 32)).build()
                ViewCompat.dispatchApplyWindowInsets(padded, supplied)
                assertEquals(18, padded.paddingLeft); assertEquals(37, padded.paddingTop)
                assertEquals(26, padded.paddingRight); assertEquals(51, padded.paddingBottom)
                SystemBarHelper.setupInsetsListener(padded)
                ViewCompat.dispatchApplyWindowInsets(padded, supplied)
                assertEquals(51, padded.paddingBottom)
                val content = activity.findViewById<View>(android.R.id.content)
                val initial = intArrayOf(content.paddingLeft, content.paddingTop, content.paddingRight, content.paddingBottom)
                val insets = WindowInsetsCompat.Builder().setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(7, 24, 9, 32)).build()
                // Listener was installed once at creation; repeated lifecycle calls must not replace its baseline.
                SystemBarHelper.applySystemBarInsets(activity)
                val child = ViewCompat.dispatchApplyWindowInsets(content, insets)
                assertEquals(7, content.paddingLeft); assertEquals(24, content.paddingTop)
                assertEquals(9, content.paddingRight); assertEquals(32, content.paddingBottom)
                assertEquals(0, child.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom)
                ViewCompat.dispatchApplyWindowInsets(content, insets)
                assertEquals(32, content.paddingBottom)
                assertTrue(initial.all { it >= 0 })
            }
        }
    }

    @Test fun realThemeRecreationPreservesStateAndCapturesBothSystemBarsWithoutOverlays() {
        try {
            instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
            runShell("cmd uimode night no")
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                scenario.onActivity {
                    it.retainedAnswer = "B"
                    captureWindow = it.window
                    val b = ActivityTestBinding.inflate(it.layoutInflater)
                    b.tvTestTitle.text = "Isolated UI verification"
                    it.setContentView(b.root)
                }
                // Start from an actually visible Light window, as a user tapping
                // the theme control would. Activity RESUMED alone does not mean
                // SystemUI has observed its appearance after a nav-mode overlay swap.
                screenshot("mock-screen-initial-light")
                for (dark in listOf(true, false)) {
                    scenario.onActivity {
                        ThemeSwitchAnimator.animate(it, it.window.decorView, dark)
                        ThemeSwitchAnimator.animate(it, it.window.decorView, !dark) // Rapid second tap is ignored.
                    }
                    val deadline = System.currentTimeMillis() + 5000
                    var ready = false
                    var failureDiag = ""
                    while (!ready && System.currentTimeMillis() < deadline) {
                        instrumentation.waitForIdleSync()
                        scenario.onActivity { act ->
                            val actualUiDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            ready = actualUiDark == dark && !ThemeSwitchAnimator.isTransitioning
                            val p = act.getSharedPreferences(com.eve.app.util.ThemeManager.PREFS, 0)
                            val prefVal = if (p.contains(com.eve.app.util.ThemeManager.KEY_DARK_MODE)) p.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, false) else null
                            failureDiag = "requestedTarget=$dark, actualUiMode=$actualUiDark, savedPref=$prefVal, delegateMode=${AppCompatDelegate.getDefaultNightMode()}, transitioning=${ThemeSwitchAnimator.isTransitioning}, transactionId=${com.eve.app.util.ThemeManager.activeTransitionId}, activityState=${if (act.isDestroyed) "destroyed" else if (act.isFinishing) "finishing" else "alive"}"
                        }
                        if (!ready) Thread.sleep(50)
                    }
                    assertTrue("Theme recreation did not settle ($failureDiag)", ready)
                    // API 35 three-button SystemUI icon tone requires system night mode
                    // synchronization (Google Issue 346386744). Do this AFTER the app's
                    // reveal settles: changing system mode beforehand can asynchronously
                    // replace the source Activity during its capture preparation.
                    if (InstrumentationRegistry.getArguments().getString("navigationMode") == "three-button") {
                        runShell("cmd uimode night " + if (dark) "yes" else "no")
                        instrumentation.uiAutomation.waitForIdle(500, 5000)
                        instrumentation.waitForIdleSync()
                    }
                    scenario.onActivity { activity ->
                        captureWindow = activity.window
                        val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                        assertEquals(dark, night); assertEquals("B", activity.retainedAnswer)
                        assertFalse(ThemeSwitchAnimator.isTransitioning)
                        assertEquals(!dark, WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightNavigationBars)
                        assertEquals(!dark, WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
                        assertNull(activity.window.decorView.findViewWithTag<View>("theme_switch_freeze_overlay"))
                        val b = ActivityTestBinding.inflate(activity.layoutInflater)
                        activity.setContentView(b.root)
                        b.tvTestTitle.text = "Isolated UI verification"
                        b.tvTimer.text = "20:00"
                        b.viewPager.adapter = QuestionAdapter(
                            listOf(Question(id = "fixture-1", questionText = "Which number is even?", optionA = "One", optionB = "Two", optionC = "Three", optionD = "Five")),
                            { activity.retainedAnswer }, { _, answer -> activity.retainedAnswer = answer }, { false }, {}, { false }, {}, { 12L }
                        )
                    }
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { activity ->
                        val footer = activity.findViewById<View>(R.id.layoutBottomBar)
                        val position = IntArray(2); footer.getLocationOnScreen(position)
                        val content = activity.findViewById<View>(android.R.id.content)
                        assertTrue(
                            "Footer must stay above system navigation (footerBottom=${position[1] + footer.height}, maxContentBottom=${activity.window.decorView.height - content.paddingBottom})",
                            position[1] + footer.height <= activity.window.decorView.height - content.paddingBottom + 2
                        )
                    }
                    screenshot("mock-screen-${if (dark) "dark" else "light"}")
                    scenario.onActivity { activity ->
                        val b = ActivityResultBinding.inflate(activity.layoutInflater)
                        ResultTabs.bind(b) {}
                        ResultCutoffPresentation.bind(b, "General", 0.0, emptyMap())
                        b.tvAnalysisAccuracy.text = "100.0%"
                        activity.setContentView(b.root)
                    }
                    instrumentation.waitForIdleSync()
                    screenshot("result-screen-${if (dark) "dark" else "light"}")
                    scenario.onActivity { activity ->
                        val scroll = activity.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollResultContent)
                        scroll.scrollTo(0, scroll.getChildAt(0).height)
                        assertTrue("Analytics evidence must actually scroll", scroll.scrollY > 0)
                    }
                    screenshot("result-analytics-${if (dark) "dark" else "light"}")
                }
            }
        } finally {
            runShell("cmd uimode night no")
        }
    }

    @Test fun rotationDuringThemeRecreationKeepsStateAndNeverBlocksTouch() {
        instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity {
                it.retainedAnswer = "C"
                ThemeSwitchAnimator.animate(it, it.window.decorView, true)
                it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            var ready = false
            val deadline = System.currentTimeMillis() + 8000
            while (!ready && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity { ready = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !ThemeSwitchAnimator.isTransitioning }
                if (!ready) Thread.sleep(50)
            }
            assertTrue("Rotated activity did not settle", ready)
            scenario.onActivity {
                assertEquals("C", it.retainedAnswer)
                assertEquals(0, it.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                assertNull(it.window.decorView.findViewWithTag<View>("theme_switch_freeze_overlay"))
                it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    @Test fun rapidOppositeRequestDuringPendingCaptureIsRejectedAndFirstTargetSettles() {
        val targetContext = instrumentation.targetContext
        val prefs = targetContext.getSharedPreferences(com.eve.app.util.ThemeManager.PREFS, 0)
        val initialHasPref = prefs.contains(com.eve.app.util.ThemeManager.KEY_DARK_MODE)
        val initialPref = prefs.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, false)
        val initialDelegate = AppCompatDelegate.getDefaultNightMode()

        try {
            // Symmetrical production path testing:
            // Phase 1: Light -> Dark with rapid Light request rejected while capture is held pending
            // Phase 2: Dark -> Light with rapid Dark request rejected while capture is held pending
            for (phaseStartDark in listOf(false, true)) {
                val targetDark = !phaseStartDark
                val initialMode = if (phaseStartDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                instrumentation.runOnMainSync {
                    prefs.edit().putBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, phaseStartDark).commit()
                    AppCompatDelegate.setDefaultNightMode(initialMode)
                }

                ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                    scenario.onActivity { act ->
                        act.retainedAnswer = "B"
                        val b = ActivityTestBinding.inflate(act.layoutInflater)
                        b.tvTestTitle.text = "Rapid tap regression"
                        act.setContentView(b.root)
                    }
                    instrumentation.waitForIdleSync()

                    // Verify initial activity settled in phaseStartDark and window is measured
                    var measured = false
                    val mDeadline = System.currentTimeMillis() + 3000
                    while (!measured && System.currentTimeMillis() < mDeadline) {
                        instrumentation.waitForIdleSync()
                        scenario.onActivity { act ->
                            val currentDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            assertEquals("Activity must start in expected phase uiMode", phaseStartDark, currentDark)
                            measured = act.window.decorView.width > 0 && act.window.decorView.height > 0
                        }
                        if (!measured) Thread.sleep(20)
                    }
                    assertTrue("Window must be measured before theme switch animation", measured)

                    val captureLatch = java.util.concurrent.CountDownLatch(1)
                    val pendingCommit = java.util.concurrent.atomic.AtomicReference<Runnable?>()

                    com.eve.app.util.ThemeManager.setCaptureInterceptorForTest { commitAction ->
                        pendingCommit.set(commitAction)
                        captureLatch.countDown()
                    }

                    scenario.onActivity { act ->
                        // Request target theme
                        ThemeSwitchAnimator.animate(act, act.window.decorView, targetDark)
                    }

                    // Await capture in progress
                    assertTrue("Capture did not intercept within deadline", captureLatch.await(3, java.util.concurrent.TimeUnit.SECONDS))
                    assertTrue("Transition must be active while capture is pending", ThemeSwitchAnimator.isTransitioning)

                    // While capture is held pending, fire rapid opposite request with a callback
                    var secondCallbackFired = false
                    scenario.onActivity { act ->
                        com.eve.app.util.ThemeManager.toggleWithCircularReveal(act, 100, 100, isDarkModeTarget = phaseStartDark) {
                            secondCallbackFired = true
                        }
                    }

                    // Confirm the second request made NO preference, delegate, or callback changes
                    assertFalse("Second callback must NOT fire while transition is active", secondCallbackFired)
                    assertEquals("Active target must remain the first request's target", targetDark, com.eve.app.util.ThemeManager.activeTargetIsDark)
                    assertEquals(phaseStartDark, prefs.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, !phaseStartDark))
                    assertEquals(initialMode, AppCompatDelegate.getDefaultNightMode())

                    val handoffStages = java.util.concurrent.CopyOnWriteArrayList<String>()
                    com.eve.app.util.ThemeManager.setEventObserverForTest { stage, _, _ -> handoffStages.add(stage) }
                    // Complete capture and verify the app settles in targetDark
                    com.eve.app.util.ThemeManager.setCaptureInterceptorForTest(null)
                    val commitAction = pendingCommit.getAndSet(null)
                    assertNotNull("Held commit action must be present", commitAction)
                    scenario.onActivity {
                        commitAction!!.run()
                    }

                    var ready = false
                    val deadline = System.currentTimeMillis() + 8000
                    var diag = ""
                    while (!ready && System.currentTimeMillis() < deadline) {
                        instrumentation.waitForIdleSync()
                        scenario.onActivity { act ->
                            val currentDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            ready = currentDark == targetDark && !ThemeSwitchAnimator.isTransitioning
                            val p = act.getSharedPreferences(com.eve.app.util.ThemeManager.PREFS, 0)
                            val prefVal = if (p.contains(com.eve.app.util.ThemeManager.KEY_DARK_MODE)) p.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, false) else null
                            diag = "targetDark=$targetDark, currentDark=$currentDark, savedPref=$prefVal, delegateMode=${AppCompatDelegate.getDefaultNightMode()}, isTransitioning=${ThemeSwitchAnimator.isTransitioning}, transactionId=${com.eve.app.util.ThemeManager.activeTransitionId}, actState=${if (act.isDestroyed) "destroyed" else if (act.isFinishing) "finishing" else "alive"}"
                        }
                        if (!ready) Thread.sleep(50)
                    }
                    assertTrue("App did not settle in target theme ($diag)", ready)
                    assertTrue("RenderThread reveal never handed off its cover: $handoffStages", "protection_cover_removal" in handoffStages)
                    assertTrue(handoffStages.indexOf("protection_cover_removal") < handoffStages.indexOf("cleanup:animation_complete"))
                    com.eve.app.util.ThemeManager.setEventObserverForTest(null)

                    scenario.onActivity { act ->
                        assertEquals(0, act.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        assertNull(act.window.decorView.findViewWithTag<View>("theme_switch_freeze_overlay"))
                        assertNull(act.window.decorView.findViewWithTag<View>("pre_reveal_overlay"))
                        assertEquals(targetDark, prefs.getBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, !targetDark))
                    }
                }
            }

            // Cover same-target requests separately: requesting already-settled theme causes no transition
            com.eve.app.util.ThemeManager.setCaptureInterceptorForTest(null)
            instrumentation.runOnMainSync {
                prefs.edit().putBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, false).commit()
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                scenario.onActivity { act ->
                    val b = ActivityTestBinding.inflate(act.layoutInflater)
                    b.tvTestTitle.text = "Same-target regression"
                    act.setContentView(b.root)
                }
                instrumentation.waitForIdleSync()
                var sameTargetCallbackRan = false
                scenario.onActivity { act ->
                    val currentDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    assertFalse(currentDark)
                    com.eve.app.util.ThemeManager.toggleWithCircularReveal(act, 50, 50, isDarkModeTarget = false) {
                        sameTargetCallbackRan = true
                    }
                }
                assertTrue(sameTargetCallbackRan)
                assertFalse(ThemeSwitchAnimator.isTransitioning)
            }

            // Cover preference/resource mismatch separately: preference says Dark, but Activity resource is Light
            instrumentation.runOnMainSync {
                prefs.edit().putBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, true).commit() // Out of sync
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                scenario.onActivity { act ->
                    val b = ActivityTestBinding.inflate(act.layoutInflater)
                    b.tvTestTitle.text = "Mismatch regression"
                    act.setContentView(b.root)
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { act ->
                    val currentDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                    assertFalse("Activity resource must be Light despite mismatched preference", currentDark)
                    // Requesting Dark should NOT be treated as same-target because actual uiMode is Light
                    ThemeSwitchAnimator.animate(act, act.window.decorView, true)
                }
                var settled = false
                val deadline = System.currentTimeMillis() + 8000
                while (!settled && System.currentTimeMillis() < deadline) {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { act ->
                        settled = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) && !ThemeSwitchAnimator.isTransitioning
                    }
                    if (!settled) Thread.sleep(50)
                }
                assertTrue("Mismatched preference was correctly reconciled and Activity transitioned to Dark", settled)
            }
        } finally {
            com.eve.app.util.ThemeManager.setCaptureInterceptorForTest(null)
            com.eve.app.util.ThemeManager.setEventObserverForTest(null)
            instrumentation.runOnMainSync {
                if (initialHasPref) {
                    prefs.edit().putBoolean(com.eve.app.util.ThemeManager.KEY_DARK_MODE, initialPref).commit()
                } else {
                    prefs.edit().remove(com.eve.app.util.ThemeManager.KEY_DARK_MODE).commit()
                }
                AppCompatDelegate.setDefaultNightMode(initialDelegate)
            }
        }
    }

    @Test fun productionPopupHandoffHasMeasuredLayersAndOwnedCancellationInBothDirections() {
        val manager = com.eve.app.util.ThemeManager
        val prefs = instrumentation.targetContext.getSharedPreferences(manager.PREFS, 0)
        val initialMode = AppCompatDelegate.getDefaultNightMode()
        val hadPref = prefs.contains(manager.KEY_DARK_MODE)
        val oldPref = prefs.getBoolean(manager.KEY_DARK_MODE, false)
        try {
            for (startDark in listOf(false, true)) for (popupPhase in listOf("pending", "running", "settled")) {
                instrumentation.runOnMainSync {
                    manager.cleanupPending("popup_fixture_setup")
                    prefs.edit().putBoolean(manager.KEY_DARK_MODE, startDark).commit()
                    AppCompatDelegate.setDefaultNightMode(if (startDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                }
                ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                    scenario.onActivity { it.showLoadedHomeFixture() }
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { (it.homeBinding!!.rvExams.layoutManager as androidx.recyclerview.widget.LinearLayoutManager).scrollToPositionWithOffset(15, 0) }
                    instrumentation.waitForIdleSync()
                    val events = java.util.concurrent.CopyOnWriteArrayList<String>()
                    val eventIds = java.util.concurrent.ConcurrentHashMap<String, Long>()
                    val complete = java.util.concurrent.CountDownLatch(1)
                    val blurRunning = java.util.concurrent.CountDownLatch(1)
                    val failures = java.util.concurrent.CopyOnWriteArrayList<String>()
                    var calls = 0
                    var runningTapped = false
                    lateinit var popup: com.eve.app.ui.home.TelegramMenuPopup
                    instrumentation.runOnMainSync {
                        manager.setEventObserverForTest { stage, id, view ->
                            events.add(stage)
                            eventIds[stage] = id
                            if (stage.startsWith("popup_blur_update:") && "popup_work_cancelled" in events) {
                                failures.add("Blur changed after popup cancellation: $stage")
                            }
                            if (popupPhase == "running" && !runningTapped && stage.startsWith("popup_blur_update:") &&
                                (stage.substringAfter(':').toIntOrNull() ?: 0) > 1) {
                                // Tap in the animator's frame, before returning to instrumentation.
                                // ActivityScenario.onActivity waits for idle and can miss this interval.
                                runningTapped = true
                                if (popup.contentView.alpha >= 1f) failures.add("Entrance settled before its first blur frame")
                                popup.contentView.findViewById<View>(R.id.cardTheme).performClick()
                                popup.contentView.findViewById<View>(R.id.cardTheme).performClick()
                                blurRunning.countDown()
                            }
                            if (stage == "target_layers_ready" || stage == "protection_cover_removal") {
                                val decor = view?.rootView as? android.view.ViewGroup
                                val cover = decor?.findViewWithTag<View>("pre_reveal_overlay")
                                val snapshot = decor?.findViewWithTag<View>("theme_switch_freeze_overlay")
                                if (cover == null || snapshot == null || !snapshot.isLaidOut ||
                                    snapshot.width != decor.width || snapshot.height != decor.height ||
                                    snapshot.left != 0 || snapshot.top != 0) failures.add("Unprotected or unmeasured layers at $stage")
                            }
                            if (stage == "cleanup:animation_complete") complete.countDown()
                        }
                    }
                    runShell("mkdir -p /sdcard/Pictures/eve-approved-ui")
                    val direction = if (startDark) "night-day" else "day-night"
                    val mode = InstrumentationRegistry.getArguments().getString("navigationMode", "default")
                    val recordingPath = "/sdcard/Pictures/eve-approved-ui/popup-$direction-$popupPhase-$mode.mp4"
                    val recording = instrumentation.uiAutomation.executeShellCommand("screenrecord --time-limit 15 $recordingPath")
                    try {
                        // Wait for the muxer to write captured frames before accepting the tap.
                        val recordingDeadline = System.currentTimeMillis() + 4000
                        var recordingReady = false
                        while (!recordingReady && System.currentTimeMillis() < recordingDeadline) {
                            instrumentation.uiAutomation.executeShellCommand("stat -c %s $recordingPath").use { fd ->
                                recordingReady = (java.io.FileInputStream(fd.fileDescriptor).bufferedReader().readText().trim().toLongOrNull() ?: 0L) > 1024L
                            }
                            if (!recordingReady) Thread.sleep(20)
                        }
                        assertTrue("Hardware recording did not start", recordingReady)
                        instrumentation.runOnMainSync { manager.trace("recording_started") }
                        scenario.onActivity { act ->
                            popup = com.eve.app.ui.home.TelegramMenuPopup(act,
                                onThemeToggle = { x, y, w, h -> calls++; ThemeSwitchAnimator.animateAt(act, x, y, !startDark, w, h) },
                                onHistory = {}, onBookmarks = {}, onTopic = {}, onPyq = {}, onLogout = {})
                            popup.show(act.homeBinding!!.btnOverflow)
                            if (popupPhase == "pending") {
                                // Same turn: entrance setup is still queued, and blur work must be canceled.
                                popup.contentView.findViewById<View>(R.id.cardTheme).performClick()
                                popup.contentView.findViewById<View>(R.id.cardTheme).performClick()
                                assertTrue(manager.isTransitioning)
                            }
                        }
                        if (popupPhase == "running") {
                            assertTrue("Entrance blur never started", blurRunning.await(2, java.util.concurrent.TimeUnit.SECONDS))
                        } else if (popupPhase == "settled") {
                            // Test-only wait for the documented 220ms entrance; production has no delay.
                            Thread.sleep(300)
                            scenario.onActivity {
                                popup.contentView.findViewById<View>(R.id.cardTheme).performClick()
                                popup.contentView.findViewById<View>(R.id.cardTheme).performClick()
                            }
                        }
                        assertTrue("Reveal did not complete: $events", complete.await(6, java.util.concurrent.TimeUnit.SECONDS))
                        instrumentation.waitForIdleSync()
                        assertEquals(1, calls)
                        assertTrue(failures.toString(), failures.isEmpty())
                        val required = listOf("tap_accepted", "popup_work_cancelled", "clean_host_frame", "capture_requested",
                            "capture_completed", "source_cover_ready", "theme_application", "target_layers_ready",
                            "native_reveal_start", "protection_cover_removal", "cleanup:animation_complete")
                        assertTrue("Missing events: $events", events.containsAll(required))
                        assertEquals(required, events.filter { it in required })
                        assertTrue("Expected hardware frame fences: $events", events.count { it == "hardware_frame_committed" } >= 3)
                        assertEquals("Handoff stages changed ownership", 1, required.map { eventIds[it] }.toSet().size)
                        scenario.onActivity { act ->
                            assertTrue("Theme identity was unavailable after super.onCreate", act.themeRecreationDetected)
                            assertFalse(popup.isShowing)
                            assertFalse(popup.contentView.isAttachedToWindow)
                            assertEquals(1f, act.homeBinding!!.rvExams.alpha)
                            val lm = act.homeBinding!!.rvExams.layoutManager as androidx.recyclerview.widget.LinearLayoutManager
                            assertEquals("Scrolled content moved on recreation", 15, lm.findFirstVisibleItemPosition())
                            assertNull(act.window.decorView.findViewWithTag<View>("pre_reveal_overlay"))
                        }
                    } finally {
                        runShell("pkill -2 screenrecord")
                        recording.close()
                        instrumentation.runOnMainSync { manager.setEventObserverForTest(null) }
                    }
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                manager.setEventObserverForTest(null)
                manager.cleanupPending("popup_fixture_done")
                val editor = prefs.edit()
                if (hadPref) editor.putBoolean(manager.KEY_DARK_MODE, oldPref) else editor.remove(manager.KEY_DARK_MODE)
                editor.commit()
                AppCompatDelegate.setDefaultNightMode(initialMode)
            }
        }
    }

    @Test fun ordinarySavedStateRecreationDoesNotClaimThemeIdentity() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { assertFalse(it.themeRecreationDetected) }
            scenario.recreate()
            scenario.onActivity { assertFalse(it.themeRecreationDetected) }
        }
    }

    @Test fun backgroundDuringCaptureRetiresPreparationWithoutApplyingItsLateCommit() {
        val manager = com.eve.app.util.ThemeManager
        val captured = java.util.concurrent.CountDownLatch(1)
        val held = java.util.concurrent.atomic.AtomicReference<Runnable>()
        try {
            instrumentation.runOnMainSync {
                manager.cleanupPending("background_fixture_setup")
                ThemeSwitchAnimator.persistThemePreference(instrumentation.targetContext, false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                manager.setCaptureInterceptorForTest { held.set(it); captured.countDown() }
            }
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { ThemeSwitchAnimator.animate(it, it.window.decorView, true) }
                assertTrue(captured.await(3, java.util.concurrent.TimeUnit.SECONDS))
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                instrumentation.runOnMainSync {
                    assertFalse(manager.isTransitioning)
                    held.getAndSet(null)!!.run()
                    assertEquals(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.getDefaultNightMode())
                }
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                scenario.onActivity {
                    assertNull(it.window.decorView.findViewWithTag<View>("pre_reveal_overlay"))
                    assertEquals(0, it.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
            }
        } finally {
            instrumentation.runOnMainSync { manager.setCaptureInterceptorForTest(null); manager.cleanupPending("background_fixture_done") }
        }
    }

    @Test fun animationsDisabledStillSettlesBothThemesWithoutTransitionLayers() {
        val context = instrumentation.targetContext
        val scale = android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        try {
            runShell("settings put global animator_duration_scale 0")
            assertEquals(0f, android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
            instrumentation.runOnMainSync {
                ThemeSwitchAnimator.persistThemePreference(context, false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                for (dark in listOf(true, false)) {
                    scenario.onActivity { ThemeSwitchAnimator.animate(it, it.window.decorView, dark) }
                    var ready = false
                    val deadline = System.currentTimeMillis() + 4000
                    while (!ready && System.currentTimeMillis() < deadline) {
                        instrumentation.waitForIdleSync()
                        scenario.onActivity {
                            ready = ((it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) == dark
                            assertFalse(ThemeSwitchAnimator.isTransitioning)
                            assertNull(it.window.decorView.findViewWithTag<View>("pre_reveal_overlay"))
                            assertNull(it.window.decorView.findViewWithTag<View>("theme_switch_freeze_overlay"))
                        }
                        if (!ready) Thread.sleep(20)
                    }
                    assertTrue("Disabled-animation fallback did not settle", ready)
                }
            }
        } finally { runShell("settings put global animator_duration_scale $scale") }
    }

    @Test fun staleTargetPreparationCannotRemoveNextTransactionsSourceCover() {
        val manager = com.eve.app.util.ThemeManager
        val pending = java.util.concurrent.atomic.AtomicReference<Runnable>()
        val intercepted = java.util.concurrent.CountDownLatch(1)
        val checked = java.util.concurrent.CountDownLatch(1)
        val finished = java.util.concurrent.CountDownLatch(1)
        val errors = java.util.concurrent.CopyOnWriteArrayList<String>()
        try {
            instrumentation.runOnMainSync {
                manager.cleanupPending("stale_fixture_setup")
                ThemeSwitchAnimator.persistThemePreference(instrumentation.targetContext, false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                manager.setPreparationInterceptorForTest { pending.set(it); intercepted.countDown() }
            }
            ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { ThemeSwitchAnimator.animate(it, it.window.decorView, true) }
                assertTrue(intercepted.await(4, java.util.concurrent.TimeUnit.SECONDS))
                scenario.onActivity { act ->
                    manager.cleanupPending("retire_held_preparation")
                    manager.setPreparationInterceptorForTest(null)
                    manager.setEventObserverForTest { stage, _, view ->
                        if (stage == "source_cover_ready") {
                            val cover = view!!.findViewWithTag<View>("pre_reveal_overlay")
                            pending.getAndSet(null)?.run()
                            if (cover == null || cover !== view.findViewWithTag<View>("pre_reveal_overlay")) errors.add("Stale callback removed a newer cover")
                            checked.countDown()
                        }
                        if (stage == "cleanup:animation_complete") finished.countDown()
                    }
                    ThemeSwitchAnimator.animate(act, act.window.decorView, false)
                }
                assertTrue(checked.await(4, java.util.concurrent.TimeUnit.SECONDS))
                assertTrue(errors.toString(), errors.isEmpty())
                assertTrue(finished.await(4, java.util.concurrent.TimeUnit.SECONDS))
            }
        } finally {
            instrumentation.runOnMainSync {
                manager.setPreparationInterceptorForTest(null)
                manager.setEventObserverForTest(null)
                manager.cleanupPending("stale_fixture_done")
                ThemeSwitchAnimator.persistThemePreference(instrumentation.targetContext, false)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    @Test fun canceledPopupWorkCannotRestoreBlurOrEntranceOnFollowingFrames() {
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            lateinit var view: View
            val frames = java.util.concurrent.CountDownLatch(1)
            scenario.onActivity { act ->
                val popup = android.widget.PopupWindow(act).apply {
                    contentView = FrameLayout(act)
                    width = 200; height = 200
                    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.WHITE))
                }
                popup.showAtLocation(act.window.decorView, android.view.Gravity.CENTER, 0, 0)
                view = popup.contentView
                com.eve.app.util.GlassmorphismHelper.applyWindowBlur(view)
                com.eve.app.util.TelegramPopupHelper.animateEntrance(view)
                com.eve.app.util.GlassmorphismHelper.removeWindowBlur(view, false)
                com.eve.app.util.TelegramPopupHelper.cancel(view)
                view.alpha = 0.73f; view.scaleX = 0.91f
                view.postOnAnimation {
                    view.postOnAnimation {
                        assertEquals(0.73f, view.alpha)
                        assertEquals(0.91f, view.scaleX)
                        if (android.os.Build.VERSION.SDK_INT >= 31) {
                            assertEquals(0, (view.rootView.layoutParams as android.view.WindowManager.LayoutParams).blurBehindRadius)
                        }
                        popup.dismiss()
                        frames.countDown()
                    }
                }
            }
            assertTrue("Canceled work test did not draw", frames.await(3, java.util.concurrent.TimeUnit.SECONDS))
        }
    }

    private fun runShell(cmd: String) {
        try {
            instrumentation.uiAutomation.executeShellCommand(cmd).use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).bufferedReader().readText()
            }
        } catch (_: Throwable) {}
    }

    private fun screenshot(name: String) {
        // Ensure device display is awake and idle dimming is reset
        runShell("input keyevent KEYCODE_WAKEUP")
        // Wait for layout/draw and window transitions, not only the main queue.
        instrumentation.uiAutomation.waitForIdle(250, 5000)
        instrumentation.waitForIdleSync()

        val mode = InstrumentationRegistry.getArguments().getString("navigationMode", "default")
        val isMockScreen = name.startsWith("mock-screen")
        val dark = name.contains("dark")

        var bitmap: Bitmap? = null
        var rows = 0
        var windowState = ""
        var focused = false
        var contrastingPixels = 0

        // Allow SystemUI asynchronous tint animations and overlay switches to settle.
        val deadline = System.currentTimeMillis() + if (isMockScreen) 5000L else 1000L
        while (true) {
            instrumentation.waitForIdleSync()
            Thread.sleep(if (isMockScreen && bitmap != null) 200L else 500L)
            bitmap?.recycle()
            bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("No emulator screenshot")

            if (isMockScreen) {
                instrumentation.runOnMainSync {
                    val window = captureWindow!!
                    val navigation = ViewCompat.getRootWindowInsets(window.decorView)?.getInsets(WindowInsetsCompat.Type.navigationBars())
                    rows = navigation?.bottom ?: 0
                    focused = window.decorView.hasWindowFocus()
                    windowState = "focus=${window.decorView.hasWindowFocus()} appearance=${window.insetsController?.systemBarsAppearance} flags=${window.attributes.flags.toUInt().toString(16)} legacy=${window.decorView.systemUiVisibility} navigation=$navigation"
                }
                if (focused && rows in 1..bitmap.height) {
                    contrastingPixels = 0
                    for (y in bitmap.height - rows until bitmap.height) for (x in 0 until bitmap.width) {
                        val color = bitmap.getPixel(x, y)
                        if (androidx.core.graphics.ColorUtils.calculateContrast(color, if (dark) Color.BLACK else Color.WHITE) >= 3.0) contrastingPixels++
                    }
                    if (contrastingPixels > 10 || System.currentTimeMillis() >= deadline) {
                        break
                    }
                } else if (System.currentTimeMillis() >= deadline) {
                    break
                }
            } else {
                break
            }
        }

        val finalBitmap = bitmap ?: error("No emulator screenshot")
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name-$mode.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
        }
        val resolver = instrumentation.targetContext.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use { assertTrue(finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }

        if (isMockScreen) {
            val activeRoot = instrumentation.uiAutomation.rootInActiveWindow
            val systemAnr = activeRoot?.findAccessibilityNodeInfosByViewId("android:id/aerr_app_info")
                ?.any { it.text?.contains("System UI", ignoreCase = true) == true } == true ||
                activeRoot?.findAccessibilityNodeInfosByText("System UI isn't responding")?.isNotEmpty() == true
            assertFalse("EMULATOR_SYSTEM_UI_ANR: $name/$mode; see saved screenshot; $windowState", systemAnr)
            assertTrue("CAPTURE_WINDOW_NOT_FOCUSED: $name/$mode; another window obscures the app; see saved screenshot; $windowState", focused)
            assertTrue("Navigation bounds unavailable for $name/$mode; $windowState", rows in 1..finalBitmap.height)

            if (contrastingPixels <= 10) {
                try {
                    instrumentation.uiAutomation.executeShellCommand("dumpsys activity service SystemUIService").use { descriptor ->
                        val state = java.io.FileInputStream(descriptor.fileDescriptor).bufferedReader().readText()
                        val fields = Regex("appearance|darkIntensity|mNavigationLight|mHasLightNavigationBar|mNavigationBarMode|mForce.*Scrim", RegexOption.IGNORE_CASE)
                        android.util.Log.e("SystemBarHelper", state.lineSequence().filter { fields.containsMatchIn(it) }.take(50).joinToString("\n"))
                    }
                } catch (e: Exception) { android.util.Log.e("SystemBarHelper", "Native SystemUI diagnostics unavailable: ${e.javaClass.simpleName}") }
            }
            assertTrue("System navigation indicator must actually contrast in $name/$mode; pixels=$contrastingPixels; $windowState", contrastingPixels > 10)
        }
        finalBitmap.recycle()
    }
}

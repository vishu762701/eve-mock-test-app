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
                    // On API 35 emulators with 3-button navigation, SystemUI has a known
                    // platform bug (Google Issue 346386744) where navigation bar icon
                    // colors only update upon system-level night mode changes.
                    // Synchronize the emulator system uiMode so SystemUI updates icon tone.
                    runShell("cmd uimode night " + if (dark) "yes" else "no")
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
                            failureDiag = "requestedTarget=$dark, actualUiMode=$actualUiDark, savedPref=$prefVal, delegateMode=${AppCompatDelegate.getDefaultNightMode()}, transitioning=${ThemeSwitchAnimator.isTransitioning}, activityState=${if (act.isDestroyed) "destroyed" else if (act.isFinishing) "finishing" else "alive"}"
                        }
                        if (!ready) Thread.sleep(50)
                    }
                    assertTrue("Theme recreation did not settle ($failureDiag)", ready)
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
                        assertTrue("Footer must stay above system navigation", position[1] + footer.height <= activity.window.decorView.height - content.paddingBottom)
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
                    // Verify initial activity settled in phaseStartDark
                    scenario.onActivity { act ->
                        val currentDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                        assertEquals("Activity must start in expected phase uiMode", phaseStartDark, currentDark)
                    }

                    val captureLatch = java.util.concurrent.CountDownLatch(1)
                    val releaseLatch = java.util.concurrent.CountDownLatch(1)
                    val pendingCommit = java.util.concurrent.atomic.AtomicReference<Runnable>()

                    com.eve.app.util.ThemeManager.setCaptureInterceptorForTest { commitAction ->
                        pendingCommit.set(commitAction)
                        captureLatch.countDown()
                        releaseLatch.await(5, java.util.concurrent.TimeUnit.SECONDS)
                    }

                    var secondCallbackFired = false
                    scenario.onActivity { act ->
                        // Request target theme
                        ThemeSwitchAnimator.animate(act, act.window.decorView, targetDark)
                    }

                    // Await capture in progress
                    assertTrue("Capture did not intercept within deadline", captureLatch.await(3, java.util.concurrent.TimeUnit.SECONDS))
                    assertTrue("Transition must be active while capture is pending", ThemeSwitchAnimator.isTransitioning)

                    // While capture is held pending, fire rapid opposite request with a callback
                    scenario.onActivity { act ->
                        com.eve.app.util.ThemeManager.toggleWithCircularReveal(act, 100, 100, isDarkModeTarget = phaseStartDark) {
                            secondCallbackFired = true
                        }
                    }

                    // Confirm the second request made NO preference, delegate, or callback changes
                    assertFalse("Second callback must NOT fire while transition is active", secondCallbackFired)
                    assertEquals("Active target must remain the first request's target", targetDark, com.eve.app.util.ThemeManager.activeTargetIsDark)

                    // Complete capture and verify the app settles in targetDark
                    releaseLatch.countDown()
                    instrumentation.runOnMainSync {
                        pendingCommit.get()?.run()
                    }

                    var ready = false
                    val deadline = System.currentTimeMillis() + 8000
                    var diag = ""
                    while (!ready && System.currentTimeMillis() < deadline) {
                        instrumentation.waitForIdleSync()
                        scenario.onActivity { act ->
                            val currentDark = (act.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            ready = currentDark == targetDark && !ThemeSwitchAnimator.isTransitioning
                            diag = "targetDark=$targetDark, currentDark=$currentDark, isTransitioning=${ThemeSwitchAnimator.isTransitioning}, actState=${if (act.isDestroyed) "destroyed" else if (act.isFinishing) "finishing" else "alive"}"
                        }
                        if (!ready) Thread.sleep(50)
                    }
                    assertTrue("App did not settle in target theme ($diag)", ready)

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
        Thread.sleep(1000) // SystemUI tint animations run outside the app's main queue.
        val mode = InstrumentationRegistry.getArguments().getString("navigationMode", "default")
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$name-$mode.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
        }
        val resolver = instrumentation.targetContext.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("No emulator screenshot")
        resolver.openOutputStream(uri)!!.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        if (name.startsWith("mock-screen")) {
            val dark = name.contains("dark")
            var rows = 0
            var windowState = ""
            var focused = false
            instrumentation.runOnMainSync {
                val window = captureWindow!!
                val navigation = ViewCompat.getRootWindowInsets(window.decorView)?.getInsets(WindowInsetsCompat.Type.navigationBars())
                rows = navigation?.bottom ?: 0
                focused = window.decorView.hasWindowFocus()
                windowState = "focus=${window.decorView.hasWindowFocus()} appearance=${window.insetsController?.systemBarsAppearance} flags=${window.attributes.flags.toUInt().toString(16)} legacy=${window.decorView.systemUiVisibility} navigation=$navigation"
            }
            // Run #227 captured a SystemUI ANR dialog, which dims the whole app.
            // Keep the screenshot and fail explicitly; never dismiss/skip the
            // dialog or report its dimmed pixels as an application tint defect.
            val activeRoot = instrumentation.uiAutomation.rootInActiveWindow
            val systemAnr = activeRoot?.findAccessibilityNodeInfosByViewId("android:id/aerr_app_info")
                ?.any { it.text?.contains("System UI", ignoreCase = true) == true } == true ||
                activeRoot?.findAccessibilityNodeInfosByText("System UI isn't responding")?.isNotEmpty() == true
            assertFalse("EMULATOR_SYSTEM_UI_ANR: $name/$mode; see saved screenshot; $windowState", systemAnr)
            assertTrue("CAPTURE_WINDOW_NOT_FOCUSED: $name/$mode; another window obscures the app; see saved screenshot; $windowState", focused)
            assertTrue("Navigation bounds unavailable for $name/$mode; $windowState", rows in 1..bitmap.height)
            var contrastingPixels = 0
            for (y in bitmap.height - rows until bitmap.height) for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                if (androidx.core.graphics.ColorUtils.calculateContrast(color, if (dark) Color.BLACK else Color.WHITE) >= 3.0) contrastingPixels++
            }
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
        bitmap.recycle()
    }
}

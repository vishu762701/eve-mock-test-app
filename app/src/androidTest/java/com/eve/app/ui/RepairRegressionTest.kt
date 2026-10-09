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
        instrumentation.runOnMainSync { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
        ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            scenario.onActivity { it.retainedAnswer = "B" }
            for (dark in listOf(true, false)) {
                scenario.onActivity {
                    ThemeSwitchAnimator.animate(it, it.window.decorView, dark)
                    ThemeSwitchAnimator.animate(it, it.window.decorView, !dark) // Rapid second tap is ignored.
                }
                val deadline = System.currentTimeMillis() + 5000
                var ready = false
                while (!ready && System.currentTimeMillis() < deadline) {
                    instrumentation.waitForIdleSync()
                    scenario.onActivity { ready = (it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) == dark && !ThemeSwitchAnimator.isTransitioning }
                    if (!ready) Thread.sleep(50)
                }
                assertTrue("Theme recreation did not settle", ready)
                scenario.onActivity { activity ->
                    val night = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                    assertEquals(dark, night); assertEquals("B", activity.retainedAnswer)
                    assertFalse(ThemeSwitchAnimator.isTransitioning)
                    assertEquals(!dark, WindowInsetsControllerCompat(activity.window, activity.window.decorView).isAppearanceLightNavigationBars)
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
            }
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

    private fun screenshot(name: String) {
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
        bitmap.recycle()
    }
}

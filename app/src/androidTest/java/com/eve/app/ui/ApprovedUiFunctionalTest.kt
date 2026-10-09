package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ActivityTestBinding
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.ui.result.ResultTabs
import com.eve.app.databinding.ItemQuestionBinding
import com.eve.app.databinding.ItemPaletteCircleBinding
import com.eve.app.ui.common.PaletteItem
import com.eve.app.ui.common.PaletteState
import com.eve.app.ui.common.QuestionPaletteAdapter
import com.eve.app.ui.test.QuestionAdapter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApprovedUiFunctionalTest {
    @Test fun resultTabsSelectTheCorrectSections() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_Eve)
            val b = ActivityResultBinding.inflate(LayoutInflater.from(context))
            var reviewCalls = 0
            ResultTabs.bind(b) { reviewCalls++ }
            val sections = listOf(b.scrollResultContent, b.sectionReview, b.scrollLeaderboard)
            for ((position, label) in listOf("Overview", "Review", "Leaderboard").withIndex()) {
                val tab = b.tabLayoutResult.getTabAt(position)!!
                assertEquals(label, tab.text.toString())
                tab.select()
                sections.forEachIndexed { index, view ->
                    assertEquals(if (index == position) View.VISIBLE else View.GONE, view.visibility)
                }
            }
            assertEquals(1, reviewCalls)
        }
    }

    @Test fun submittingOrExpiredQuestionsKeepTheStoredSelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_Eve)
            var saved = "B"
            var editable = false
            val calls = mutableListOf<String>()
            val adapter = QuestionAdapter(
                listOf(Question(questionText = "Question", optionA = "A", optionB = "B")),
                { saved }, { _, answer -> saved = answer; calls += answer },
                { false }, {}, { false }, {}, { 0L }, canSelect = { editable }
            )
            val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
            adapter.onBindViewHolder(holder, 0)
            val binding = ItemQuestionBinding.bind(holder.itemView)
            binding.rbA.performClick()
            assertEquals(R.id.rbB, binding.rgOptions.checkedRadioButtonId)
            assertTrue(calls.isEmpty())
            editable = true
            binding.rbA.performClick()
            assertEquals(listOf("A"), calls)
            editable = false
            binding.rbB.performClick()
            assertEquals(R.id.rbA, binding.rgOptions.checkedRadioButtonId)
            assertEquals(listOf("A"), calls)
        }
    }

    @Test fun nativeOptionsRestoreClearAndSelectInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                val calls = mutableListOf<String>()
                var saved = "B"
                val adapter = QuestionAdapter(listOf(Question(questionText = "Question", optionA = "A", optionB = "B")),
                    { saved }, { _, answer -> calls += answer }, { false }, {}, { false }, {}, { 12L })
                val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
                adapter.onBindViewHolder(holder, 0)
                val b = ItemQuestionBinding.bind(holder.itemView)
                assertEquals(R.id.rbB, b.rgOptions.checkedRadioButtonId)
                assertTrue("Restoring a saved answer must not submit a selection", calls.isEmpty())
                b.rbA.performClick()
                assertEquals(listOf("A"), calls)
                holder.clearSelection()
                assertEquals(-1, b.rgOptions.checkedRadioButtonId)
                b.rbB.performClick()
                assertEquals("Reselection immediately after Clear must save", listOf("A", "B"), calls)
                saved = ""
                adapter.onBindViewHolder(holder, 0)
                assertEquals(-1, b.rgOptions.checkedRadioButtonId)
                assertEquals("Rebinding must not save an answer", listOf("A", "B"), calls)
                val actions = ActivityTestBinding.inflate(LayoutInflater.from(context))
                actions.btnMarkReview.isSelected = true
                val selectedState = intArrayOf(android.R.attr.state_selected)
                assertEquals(Color.parseColor("#FFCC00"), actions.btnMarkReview.backgroundTintList!!.getColorForState(selectedState, 0))
                assertEquals(Color.BLACK, actions.btnMarkReview.textColors.getColorForState(selectedState, 0))
                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 16f,
                    context.resources.displayMetrics), b.tvQuestion.textSize, 0.51f)
                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 14f,
                    context.resources.displayMetrics), b.rbA.textSize, 0.51f)
                assertEquals(1.5f, b.tvQuestion.lineSpacingMultiplier, 0.01f)
                assertEquals((56f * context.resources.displayMetrics.density + 0.5f).toInt(), b.rbA.minHeight)
                // A selected fill must not paint over the approved 2dp indicator border.
                b.rbA.isChecked = true
                b.rbA.jumpDrawablesToCurrentState()
                val density = context.resources.displayMetrics.density
                val width = (320f * density).toInt()
                val height = (56f * density).toInt()
                b.rbA.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                b.rbA.layout(0, 0, width, height)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                b.rbA.draw(Canvas(bitmap))
                val expectedBorder = context.getColor(R.color.eve_test_foreground)
                val actualBorder = bitmap.getPixel((31f * density).toInt(), height / 2)
                // At low density a curved edge shares a pixel with the green surface.
                // Allow antialiasing, while rejecting a selected fill covering the ring.
                assertTrue("Indicator border must remain visible in theme $night",
                    listOf(Color.red(expectedBorder) - Color.red(actualBorder),
                        Color.green(expectedBorder) - Color.green(actualBorder),
                        Color.blue(expectedBorder) - Color.blue(actualBorder))
                        .all { kotlin.math.abs(it) <= 24 })
                bitmap.recycle()
            }
        }
    }

    @Test fun activePaletteKeepsAnswerAndReviewStatesAndNavigation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_Eve)
            var selected = -1
            val adapter = QuestionPaletteAdapter(approvedTestStyle = true) { selected = it }
            val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
            val b = ItemPaletteCircleBinding.bind(holder.itemView)
            for ((state, color) in listOf(PaletteState.ANSWERED to "#34C759",
                PaletteState.MARKED to "#FFCC00", PaletteState.ANSWERED_MARKED to "#FFCC00")) {
                adapter.submit(listOf(PaletteItem(1, state, isActive = true)))
                adapter.onBindViewHolder(holder, 0)
                assertEquals(Color.parseColor(color), b.cardCircle.cardBackgroundColor.defaultColor)
                assertEquals(b.cardCircle.layoutParams.width, b.cardCircle.layoutParams.height)
                assertEquals((38f * context.resources.displayMetrics.density + 0.5f).toInt(), b.cardCircle.layoutParams.width)
                b.root.performClick()
                assertEquals(0, selected)
            }
        }
    }
}

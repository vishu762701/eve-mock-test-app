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
                assertEquals(context.getColor(R.color.on_accent), b.rbB.currentTextColor)
                assertEquals(context.getColor(R.color.text_primary), b.rbA.currentTextColor)
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
                val selectedState = actions.btnMarkReview.drawableState
                assertEquals(context.getColor(R.color.system_yellow), actions.btnMarkReview.backgroundTintList!!.getColorForState(selectedState, 0))
                assertEquals(Color.BLACK, actions.btnMarkReview.textColors.getColorForState(selectedState, 0))
                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 16f,
                    context.resources.displayMetrics), b.tvQuestion.textSize, 0.51f)
                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 16f,
                    context.resources.displayMetrics), b.rbA.textSize, 0.51f)
                assertEquals(1f, b.tvQuestion.lineSpacingMultiplier, 0.01f)
                assertEquals((48f * context.resources.displayMetrics.density + 0.5f).toInt(), b.rbA.minHeight)
                // Telegram's selected state is a dot inside its ring, replacing the old check glyph.
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
                val center = b.rbA.resources.getDimensionPixelSize(R.dimen.design_card_padding) +
                    b.rbA.resources.getDimensionPixelSize(R.dimen.design_icon_size) / 2
                assertEquals("Selected source dot", context.getColor(R.color.on_accent),
                    bitmap.getPixel(center, height / 2))
                bitmap.recycle()
            }
        }
    }

    @Test fun rapidOptionsClearAndRecycledBindingKeepViewModelTruth() {
        androidx.test.core.app.ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
          scenario.onActivity { activity ->
            val context = ContextThemeWrapper(activity, R.style.Theme_Eve)
            val vm = com.eve.app.ui.test.TestViewModel()
            val calls = mutableListOf<String>()
            val adapter = QuestionAdapter(List(2) { Question(id = "radio-$it", questionText = "Question $it") },
                { vm.getAnswer(it) }, { pos, answer -> calls += answer; vm.setAnswer(pos, answer) },
                { false }, {}, { false }, {}, { 0L })
            val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
            val b = ItemQuestionBinding.bind(holder.itemView)
            activity.setContentView(holder.itemView)
            adapter.onBindViewHolder(holder, 0)
            val options = listOf(b.rbA, b.rbB, b.rbC, b.rbD)
            val sequence = listOf(0, 3, 3, 1, 2, 0, 3, 0, 2)
            sequence.forEach { options[it].performClick() }
            assertEquals(listOf("A", "D", "B", "C", "A", "D", "A", "C"), calls)
            assertEquals("C", vm.getAnswer(0))
            adapter.onBindViewHolder(holder, 1)
            assertTrue(options.none { it.isChecked })
            adapter.onBindViewHolder(holder, 0)
            assertTrue(b.rbC.isChecked)
            val count = calls.size
            vm.setAnswer(0, "")
            holder.clearSelection()
            assertTrue(options.none { it.isChecked })
            assertEquals(count, calls.size)
            b.rbD.performClick()
            assertEquals("D", vm.getAnswer(0))
            assertEquals(count + 1, calls.size)
            val recreated = adapter.onCreateViewHolder(FrameLayout(context), 0)
            adapter.onBindViewHolder(recreated, 0)
            assertTrue(ItemQuestionBinding.bind(recreated.itemView).rbD.isChecked)
            assertEquals(count + 1, calls.size)
          }
        }
    }

    @Test fun reducedMotionNativeIndicatorSettlesAndKeepsAccessibleCheckedState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val resolver = instrumentation.targetContext.contentResolver
        val key = android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
        val previous = android.provider.Settings.Global.getFloat(resolver, key, 1f)
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
        }
        try {
            shell("settings put global animator_duration_scale 0")
            androidx.test.core.app.ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val radio = com.eve.app.ui.common.TelegramRadioButton(activity)
                    activity.setContentView(radio)
                    val palette = com.eve.app.theme.TelegramThemeEngine.resolveCurrentPalette(activity)
                    radio.updatePalette(palette)
                    radio.performClick()
                    assertTrue(radio.isChecked)
                    val node = radio.createAccessibilityNodeInfo()
                    assertTrue(node.isCheckable)
                    assertTrue(node.isChecked)
                    assertEquals("android.widget.RadioButton", node.className.toString())
                    val width = (320 * radio.resources.displayMetrics.density).toInt()
                    val height = (56 * radio.resources.displayMetrics.density).toInt()
                    radio.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    radio.layout(0, 0, width, height)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    radio.draw(Canvas(bitmap)) // No jump or wait: scale zero must already show the dot.
                    val center = radio.resources.getDimensionPixelSize(R.dimen.design_card_padding) +
                        radio.resources.getDimensionPixelSize(R.dimen.design_icon_size) / 2
                    assertEquals(palette.onAccent, bitmap.getPixel(center, height / 2))
                    radio.isChecked = false
                    radio.draw(Canvas(bitmap))
                    assertEquals(palette.surfaceInset, bitmap.getPixel(center, height / 2))
                    bitmap.recycle()
                }
            }
        } finally { shell("settings put global animator_duration_scale $previous") }
    }

    @Test fun radioIndicatorMirrorsWithLongMultilineOptionsAtDoubleFontScale() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                    fontScale = 2f
                    setLayoutDirection(java.util.Locale("ar"))
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                val binding = ItemQuestionBinding.inflate(LayoutInflater.from(context))
                binding.root.layoutDirection = View.LAYOUT_DIRECTION_RTL
                val options = listOf(binding.rbA, binding.rbB, binding.rbC, binding.rbD)
                options.forEachIndexed { index, option ->
                    option.text = "${'A' + index}. " + "लंबा उत्तर / Long English answer ".repeat(8)
                }
                val width = (320 * context.resources.displayMetrics.density).toInt()
                binding.root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec((600 * context.resources.displayMetrics.density).toInt(), View.MeasureSpec.EXACTLY))
                binding.root.layout(0, 0, binding.root.measuredWidth, binding.root.measuredHeight)
                options.forEach { option ->
                    option.performClick()
                    option.jumpDrawablesToCurrentState()
                    assertTrue(option.lineCount > 1)
                    assertTrue(option.height >= option.minHeight)
                    assertEquals(View.LAYOUT_DIRECTION_RTL, option.layoutDirection)
                    assertEquals(option.id, binding.rgOptions.checkedRadioButtonId)
                    val bitmap = Bitmap.createBitmap(option.width, option.height, Bitmap.Config.ARGB_8888)
                    option.draw(Canvas(bitmap))
                    val center = option.width - context.resources.getDimensionPixelSize(R.dimen.design_card_padding) -
                        context.resources.getDimensionPixelSize(R.dimen.design_icon_size) / 2
                    assertEquals("RTL source dot must stay at the native leading edge", context.getColor(R.color.on_accent),
                        bitmap.getPixel(center, option.height / 2))
                    bitmap.recycle()
                }
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
            for ((state, color) in listOf(PaletteState.ANSWERED to R.color.system_green,
                PaletteState.MARKED to R.color.system_yellow, PaletteState.ANSWERED_MARKED to R.color.system_yellow)) {
                adapter.submit(listOf(PaletteItem(1, state, isActive = true)))
                adapter.onBindViewHolder(holder, 0)
                assertEquals(context.getColor(color), b.cardCircle.cardBackgroundColor.defaultColor)
                assertEquals(b.cardCircle.layoutParams.width, b.cardCircle.layoutParams.height)
                assertEquals((32f * context.resources.displayMetrics.density + 0.5f).toInt(), b.cardCircle.layoutParams.width)
                b.root.performClick()
                assertEquals(0, selected)
            }
        }
    }
}

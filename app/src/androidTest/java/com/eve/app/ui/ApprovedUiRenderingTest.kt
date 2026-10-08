package com.eve.app.ui

import android.content.ContentValues
import android.provider.MediaStore
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.Question
import com.eve.app.databinding.*
import com.eve.app.ui.common.*
import com.eve.app.ui.home.*
import com.eve.app.ui.result.ResultTabs
import com.eve.app.ui.test.QuestionAdapter
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Renders production XML, custom views, and adapters; fixtures never access user data. */
@RunWith(AndroidJUnit4::class)
class ApprovedUiRenderingTest {
    private lateinit var context: ContextThemeWrapper
    private var dark = false
    private val density get() = context.resources.displayMetrics.density
    private fun dp(value: Int) = (value * density + 0.5f).toInt()
    private fun sp(view: android.widget.TextView, value: Int) =
        assertEquals(value.toFloat(), view.textSize / context.resources.displayMetrics.scaledDensity, 0.01f)
    private val foreground get() = if (dark) Color.WHITE else Color.BLACK
    private val background get() = if (dark) Color.BLACK else Color.WHITE
    private fun color(value: String) = Color.parseColor(value)
    private fun measure(view: View, width: Int = 390, height: Int = 844) {
        repeat(2) {
            view.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(height), View.MeasureSpec.EXACTLY))
            view.layout(0, 0, dp(width), dp(height))
        }
    }
    private fun bitmap(view: View): Bitmap = Bitmap.createBitmap(view.width, view.height,
        Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
    private fun save(view: View, name: String) {
        // Shared Pictures survives Gradle's automatic test APK uninstall.
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name-${if (dark) "dark" else "light"}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        bitmap(view).useBitmap { image -> context.contentResolver.openOutputStream(uri)!!.use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        } }
    }
    private inline fun Bitmap.useBitmap(block: (Bitmap) -> Unit) { try { block(this) } finally { recycle() } }
    private fun bare(view: View) {
        assertEquals(Color.TRANSPARENT, (view.background as ColorDrawable).color)
        assertTrue(view.layoutParams.width >= dp(48))
        assertTrue(view.layoutParams.height >= dp(48))
    }

    @Test fun approvedHomeRendersInBothThemes() = inBothThemes { verifyHome() }
    @Test fun approvedMockRendersInBothThemes() = inBothThemes { verifyMock() }
    @Test fun approvedResultRendersInBothThemes() = inBothThemes { verifyResult() }

    @Test fun approvedPixelSizesAndGapsMatchXmlAtFractionalDensities() = inBothThemes {
        for (dpi in listOf(420, 440, 480)) {
            val config = Configuration(context.resources.configuration).apply { densityDpi = dpi }
            val scaledContext = ContextThemeWrapper(context.createConfigurationContext(config), R.style.Theme_Eve)
            assertEquals(dpi, scaledContext.resources.displayMetrics.densityDpi)
            val scale = scaledContext.resources.displayMetrics.density
            fun pixels(dp: Int) = Math.round(dp * scale)
            val adapter = QuestionPaletteAdapter(approvedTestStyle = true) {}
            adapter.submit(listOf(PaletteItem(1)))
            val holder = adapter.onCreateViewHolder(FrameLayout(scaledContext), 0)
            adapter.onBindViewHolder(holder, 0)
            val p = ItemPaletteCircleBinding.bind(holder.itemView)
            assertEquals(pixels(38), p.cardCircle.layoutParams.width)
            assertEquals(pixels(38), p.cardCircle.layoutParams.height)
            assertEquals(pixels(38) / 2f, p.cardCircle.radius, 0.01f)
            assertEquals(pixels(1), p.cardCircle.strokeWidth)
            assertEquals("Total palette gap must be rounded once, not each half", pixels(8), p.root.paddingLeft + p.root.paddingRight)
            val qAdapter = QuestionAdapter(listOf(Question()), { "" }, { _, _ -> }, { false }, {}, { false }, {}, { 0L })
            val qHolder = qAdapter.onCreateViewHolder(FrameLayout(scaledContext), 0)
            qAdapter.onBindViewHolder(qHolder, 0)
            val q = ItemQuestionBinding.bind(qHolder.itemView)
            assertEquals(pixels(56), q.rbA.minHeight)
            assertEquals(pixels(12), q.rbA.paddingTop)
            assertEquals(pixels(12), q.rbA.paddingRight)
            assertEquals(pixels(44), q.rbA.paddingLeft)
        }
    }

    private fun inBothThemes(verify: () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                dark = night == Configuration.UI_MODE_NIGHT_YES
                val base = instrumentation.targetContext
                val config = Configuration(base.resources.configuration).apply {
                    uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                }
                context = ContextThemeWrapper(base.createConfigurationContext(config), R.style.Theme_Eve)
                verify()
            }
        }
    }

    private fun verifyHome() {
        val b = ActivityMainBinding.inflate(LayoutInflater.from(context))
        assertEquals(background, (b.mainContentContainer.background as ColorDrawable).color)
        assertEquals(background, context.getColor(R.color.eve_bg))
        assertEquals(background, context.getColor(R.color.eve_canvas))
        listOf(b.btnSearch, b.btnNotification, b.btnOverflow).forEach { bare(it) }
        assertEquals(Color.TRANSPARENT, ((b.btnNotification.parent as View).background as ColorDrawable).color)
        assertEquals(dp(40), b.ivProfile.layoutParams.width)
        assertEquals(dp(40), b.ivProfile.layoutParams.height)
        assertEquals(View.GONE, b.cardFloatingAirplane.visibility)
        b.tvWelcome.text = "Hi, Student"
        b.panelHomeBanner.visibility = View.VISIBLE
        HomeAppearance.clipBanner(b.panelHomeBanner)
        b.vpHomeBanners.visibility = View.INVISIBLE // no network banner image in fixture
        b.homeBannerMatte.setBackgroundColor(context.getColor(R.color.eve_home_banner_matte))
        val categories = HomeAppearance.categories(listOf("SSC", "Other", "Banking"))
        assertEquals(listOf("All", "Other", "Banking", "SSC"), categories)
        var clicked = false
        categories.forEach { category ->
            val chip = HomeAppearance.categoryChip(context, category, "All") { clicked = true }
            assertEquals(50f * density, chip.chipCornerRadius, 0.01f)
            b.chipGroupCategory.addView(chip)
        }
        b.chipGroupCategory.getChildAt(1).performClick()
        assertTrue(clicked)
        val adapter = ExamAdapter({ _, _, _ -> })
        adapter.submit(listOf(HomeListItem.ExamRow(Exam(id = "fixture", examName = "Practice Test"), false)))
        b.rvExams.layoutManager = LinearLayoutManager(context)
        b.rvExams.adapter = adapter
        val holder = adapter.onCreateViewHolder(FrameLayout(context), adapter.getItemViewType(0))
        adapter.onBindViewHolder(holder, 0)
        assertEquals(8f * density, ItemExamBinding.bind(holder.itemView).root.radius, 0.01f)
        measure(b.root)
        val outline = Outline()
        b.panelHomeBanner.outlineProvider.getOutline(b.panelHomeBanner, outline)
        assertEquals(18f * density, outline.radius, 0.01f)
        assertTrue(b.panelHomeBanner.clipToOutline)
        save(b.root, "home")
    }

    private fun button(view: MaterialButton, height: Int, font: Int, stroke: Int) {
        assertEquals(dp(height), view.height)
        sp(view, font)
        assertEquals(dp(16), view.paddingStart)
        assertEquals(dp(16), view.paddingEnd)
        assertEquals(dp(stroke), view.strokeWidth)
        assertEquals(color("#888888"), view.strokeColor!!.defaultColor)
        assertEquals(foreground, view.backgroundTintList!!.defaultColor)
        assertEquals(background, view.currentTextColor)
        assertEquals(0f, view.elevation, 0.01f)
        assertEquals(dp(50), view.cornerRadius)
    }

    private fun verifyMock() {
        val b = ActivityTestBinding.inflate(LayoutInflater.from(context))
        assertEquals(background, (b.root.background as ColorDrawable).color)
        assertEquals(if (dark) Color.BLACK else color("#EEEEF0"),
            (b.rvQuestionPalette.background as ColorDrawable).color)
        b.tvTestTitle.text = "Practice Test"
        b.circularTimerView.setTime(1800, 1800)
        val questions = List(60) { Question(questionText = "Which number is a prime number?",
            optionA = "21", optionB = "29", optionC = "35", optionD = "49") }
        var saved = "B"
        var bookmark = false
        var report = false
        val adapter = QuestionAdapter(questions, { saved }, { _, answer -> saved = answer },
            { bookmark }, { bookmark = !bookmark }, { false }, { report = true }, { 12L })
        b.viewPager.adapter = adapter
        val palette = QuestionPaletteAdapter(approvedTestStyle = true) { b.viewPager.setCurrentItem(it, false) }
        palette.submit(List(60) { PaletteItem(it + 1, when (it) {
            0 -> PaletteState.ANSWERED; 1 -> PaletteState.MARKED; else -> PaletteState.UNATTEMPTED }, it == 0) })
        b.rvQuestionPalette.adapter = palette
        measure(b.root)
        button(b.btnClear, 44, 13, 1)
        button(b.btnMarkReview, 44, 13, 1)
        button(b.btnPrev, 48, 14, 1)
        button(b.btnNext, 48, 14, 0)
        assertEquals(b.btnPrev.width, b.btnNext.width)
        assertEquals(dp(16), b.layoutBottomBar.paddingStart)
        assertEquals(dp(16), b.layoutBottomBar.paddingEnd)
        assertEquals(dp(20), b.layoutBottomBar.paddingTop)
        fun gap(first: View, second: View): Int {
            val a = first.parent as View; val c = second.parent as View
            return c.left - a.right
        }
        assertEquals(dp(8), gap(b.btnClear, b.btnMarkReview))
        assertEquals(dp(12), gap(b.btnPrev, b.btnNext))
        val navigationRow = b.btnPrev.parent.parent as View
        val actionRow = b.btnClear.parent.parent as View
        assertEquals(dp(20), navigationRow.top - actionRow.bottom)
        b.btnMarkReview.isSelected = true
        assertEquals(color("#FFCC00"), b.btnMarkReview.backgroundTintList!!.getColorForState(b.btnMarkReview.drawableState, 0))
        assertEquals(Color.BLACK, b.btnMarkReview.currentTextColor)
        b.btnMarkReview.isSelected = false
        assertEquals(dp(44), b.circularTimerView.width)
        assertEquals(b.circularTimerView.width, b.circularTimerView.height)
        fun timerPaint(name: String) = CircularTimerView::class.java.getDeclaredField(name).apply { isAccessible = true }
            .get(b.circularTimerView) as Paint
        assertEquals(2f * density, timerPaint("progressPaint").strokeWidth, 0.01f)
        assertEquals(foreground, timerPaint("progressPaint").color)
        assertEquals(12f * context.resources.displayMetrics.scaledDensity, timerPaint("textPaint").textSize, 0.01f)
        bitmap(b.circularTimerView).useBitmap { image ->
            assertEquals(background, image.getPixel(dp(22), dp(8)))
            assertEquals(foreground, image.getPixel(dp(22), dp(1)))
        }
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        adapter.onBindViewHolder(holder, 0)
        val q = ItemQuestionBinding.bind(holder.itemView)
        measure(q.root, 390, 590)
        assertNull(q.questionContainer.background)
        assertNull(q.tvQuestion.background)
        sp(q.tvQuestion, 16)
        assertEquals(1.5f, q.tvQuestion.lineSpacingMultiplier, 0.01f)
        assertEquals(Gravity.LEFT, q.tvQuestion.gravity and Gravity.HORIZONTAL_GRAVITY_MASK)
        assertEquals(dp(16), q.optionsMaterialHost.top - q.questionContainer.bottom)
        listOf(q.btnBookmark, q.btnReport).forEach { bare(it) }
        q.btnBookmark.performClick(); assertTrue(bookmark)
        q.btnReport.performClick(); assertTrue(report)
        val options = listOf(q.rbA, q.rbB, q.rbC, q.rbD)
        options.forEachIndexed { index, option ->
            sp(option, 14)
            assertEquals(dp(56), option.minHeight)
            assertEquals(dp(12), option.paddingTop)
            assertEquals(dp(12), option.paddingBottom)
            assertEquals(dp(12), option.paddingRight)
            assertEquals(dp(44), option.paddingLeft) // 12 inset + 20 indicator + 12 text gap
            if (index < 3) assertEquals(dp(10), options[index + 1].top - option.bottom)
            val surface = (option.background as StateListDrawable).current as GradientDrawable
            assertEquals(12f * density, surface.cornerRadius, 0.01f)
            assertEquals(if (option.isChecked) color("#34C759") else context.getColor(R.color.eve_test_option_bg), surface.color!!.defaultColor)
        }
        assertEquals(if (dark) color("#111111") else color("#F3F3F5"), context.getColor(R.color.eve_test_option_bg))
        val ring = TelegramRadioButton::class.java.getDeclaredField("ringPaint").apply { isAccessible = true }
            .get(q.rbA) as Paint
        bitmap(q.rbA).useBitmap { image ->
            assertEquals(2f * density, ring.strokeWidth, 0.01f)
            assertEquals(foreground, ring.color)
            // Outer diameter 20dp, centered at x=22dp; surrounding surface has no option stroke.
            assertEquals(context.getColor(R.color.eve_test_option_bg), image.getPixel(dp(10), image.height / 2))
            assertEquals(context.getColor(R.color.eve_test_option_bg), image.getPixel(dp(34), image.height / 2))
        }
        assertEquals(60, palette.itemCount)
        for (state in PaletteState.values()) {
            palette.submit(listOf(PaletteItem(1, state, true)))
            val ph = palette.onCreateViewHolder(FrameLayout(context), 0)
            palette.onBindViewHolder(ph, 0)
            val p = ItemPaletteCircleBinding.bind(ph.itemView)
            assertEquals(dp(38), p.cardCircle.layoutParams.width)
            assertEquals(dp(38), p.cardCircle.layoutParams.height)
            assertEquals(19f * density, p.cardCircle.radius, 0.01f)
            assertEquals(dp(1), p.cardCircle.strokeWidth)
            assertEquals(color("#888888"), p.cardCircle.strokeColor)
            assertEquals(dp(4), p.root.paddingLeft)
            assertEquals(dp(4), p.root.paddingRight)
            sp(p.tvCircleNumber, 12)
            val expected = when (state) {
                PaletteState.ANSWERED -> color("#34C759")
                PaletteState.MARKED, PaletteState.ANSWERED_MARKED -> color("#FFCC00")
                PaletteState.UNATTEMPTED, PaletteState.VISITED -> context.getColor(R.color.eve_test_option_bg)
                PaletteState.CORRECT -> context.getColor(R.color.eve_tile_right_fill)
                PaletteState.WRONG -> context.getColor(R.color.eve_tile_wrong_fill)
            }
            assertEquals(expected, p.cardCircle.cardBackgroundColor.defaultColor)
        }
        palette.submit(List(60) { PaletteItem(it + 1, if (it == 0) PaletteState.ANSWERED else PaletteState.UNATTEMPTED, it == 0) })
        measure(b.root)
        save(b.root, "mock-test")
        // Verify remaining time visibly depletes; the full static track must not conceal it.
        val half = CircularTimerView(context).apply { setTime(900, 1800) }
        measure(half, 44, 44)
        bitmap(half).useBitmap { image ->
            // Native antialiasing blends the curved outer edge with the background.
            // Paint color/stroke assertions above remain exact.
            val pixel = image.getPixel(dp(43), dp(22))
            assertTrue("The remaining half-ring must render", listOf(
                Color.red(pixel) - Color.red(foreground), Color.green(pixel) - Color.green(foreground),
                Color.blue(pixel) - Color.blue(foreground)).all { kotlin.math.abs(it) <= 16 })
            assertEquals(background, image.getPixel(dp(1), dp(22)))
        }
        b.viewPager.setCurrentItem(59, false)
        assertEquals(59, b.viewPager.currentItem)
    }

    private fun verifyResult() {
        val b = ActivityResultBinding.inflate(LayoutInflater.from(context))
        ResultTabs.bind(b) {}
        val margins = b.tabLayoutResult.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(dp(16), margins.marginStart)
        assertEquals(dp(16), margins.marginEnd)
        assertEquals(dp(6), margins.topMargin)
        assertEquals(dp(6), margins.bottomMargin)
        assertEquals(0, b.tabLayoutResult.selectedTabPosition)
        assertEquals(com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH,
            b.tabLayoutResult.tabIndicatorGravity)
        val sections = listOf(b.scrollResultContent, b.sectionReview, b.scrollLeaderboard)
        for ((index, name) in listOf("Overview", "Review", "Leaderboard").withIndex()) {
            b.tabLayoutResult.getTabAt(index)!!.select()
            measure(b.root)
            assertEquals(index, b.tabLayoutResult.selectedTabPosition)
            sections.forEachIndexed { position, view -> assertEquals(if (position == index) View.VISIBLE else View.GONE, view.visibility) }
            bitmap(b.tabLayoutResult).useBitmap { image ->
                val x = ((index + 0.5f) * image.width / 3).toInt()
                assertEquals("Selected pill must render for $name in dark=$dark", foreground, image.getPixel(x, dp(8)))
                assertEquals(if (dark) color("#171717") else color("#F1F1F3"), image.getPixel(image.width / 2, dp(1)))
            }
            val colors = b.tabLayoutResult.tabTextColors!!
            assertEquals(background, colors.getColorForState(intArrayOf(android.R.attr.state_selected), 0))
            assertEquals(foreground, colors.defaultColor)
            save(b.root, "result-${name.lowercase()}")
        }
    }
}

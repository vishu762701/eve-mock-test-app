package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.data.model.Question
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.databinding.ItemQuestionBinding
import com.eve.app.ui.home.HomeAppearance
import com.eve.app.ui.result.ResultTabs
import com.eve.app.ui.test.QuestionAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Production inflation and binding across phone/tablet widths and accessibility text. */
@RunWith(AndroidJUnit4::class)
class ManualUiSpecificationTest {
    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    @Test fun expandedCategoryTouchRegionsActivatePillsInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        androidx.test.core.app.ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val clicked = java.util.concurrent.atomic.AtomicBoolean(false)
                lateinit var home: ActivityMainBinding
                scenario.onActivity { activity ->
                    val config = Configuration(activity.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                        fontScale = 1f
                    }
                    val context = ContextThemeWrapper(activity.createConfigurationContext(config), R.style.Theme_Eve)
                    home = ActivityMainBinding.inflate(LayoutInflater.from(context))
                    home.chipGroupCategory.addView(HomeAppearance.categoryChip(context, "All", "All") { clicked.set(true) })
                    activity.setContentView(home.root)
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    val group = home.chipGroupCategory
                    val chip = group.getChildAt(0)
                    val density = group.resources.displayMetrics.density
                    assertTrue(chip.isAttachedToWindow)
                    assertTrue(group.height >= Math.round(48f * density))
                    assertTrue("Event must be outside the visible pill", chip.top > 1f)
                    val x = (chip.left + chip.right) / 2f
                    val now = android.os.SystemClock.uptimeMillis()
                    for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                        val event = android.view.MotionEvent.obtain(now, now, action, x, 1f, 0)
                        try { assertTrue(group.dispatchTouchEvent(event)) } finally { event.recycle() }
                    }
                }
                // View posts performClick on the UI handler. Let the attached window process it.
                instrumentation.waitForIdleSync()
                assertTrue("48dp delegated target must activate the visible pill in theme $night", clicked.get())
                lateinit var bounds: android.graphics.Rect
                var ink = 0
                scenario.onActivity {
                    val chip = home.chipGroupCategory.getChildAt(0) as Chip
                    val position = IntArray(2).also { chip.getLocationOnScreen(it) }
                    val inset = Math.round(8f * chip.resources.displayMetrics.density)
                    bounds = android.graphics.Rect(position[0] + inset, position[1] + inset,
                        position[0] + chip.width - inset, position[1] + chip.height - inset)
                    ink = chip.currentTextColor
                }
                val committed = java.util.concurrent.CountDownLatch(1)
                scenario.onActivity {
                    home.root.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
                    home.root.invalidate()
                }
                assertTrue("Attached Home frame must commit", committed.await(5, java.util.concurrent.TimeUnit.SECONDS))
                val screenshot = instrumentation.uiAutomation.takeScreenshot()
                assertNotNull("Attached Home hardware screenshot must be available", screenshot)
                screenshot!!.let { bitmap ->
                    try {
                        var inkPixels = 0
                        for (y in bounds.top until bounds.bottom) for (x in bounds.left until bounds.right) {
                            if (bitmap.getPixel(x, y) == ink) inkPixels++
                        }
                        val values = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "home-attached-${if (night == Configuration.UI_MODE_NIGHT_YES) "dark" else "light"}.png")
                            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
                        }
                        val resolver = instrumentation.targetContext.contentResolver
                        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                        resolver.openOutputStream(uri)!!.use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        assertTrue("Category label must visibly render in theme $night (inkPixels=$inkPixels, bounds=$bounds)", inkPixels >= 10)
                    } finally { bitmap.recycle() }
                }
            }
        }
    }

    @Test fun responsiveGeometryAndThemeReapplication() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES, Configuration.UI_MODE_NIGHT_NO)) {
                for (font in listOf(1f, 1.5f, 2f)) for (width in listOf(320, 360, 393, 412, 800)) {
                    val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
                        fontScale = font
                        screenWidthDp = width
                    }
                    val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                    val density = context.resources.displayMetrics.density
                    fun dp(v: Int) = Math.round(v * density)
                    fun layout(view: View, height: Int = 1000) {
                        repeat(2) {
                            view.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(dp(height), View.MeasureSpec.EXACTLY))
                            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                        }
                    }
                    val border = if (night == Configuration.UI_MODE_NIGHT_YES) Color.WHITE else Color.BLACK
                    assertEquals(border, context.getColor(R.color.eve_shape_border))
                    val home = ActivityMainBinding.inflate(LayoutInflater.from(context))
                    home.panelHomeBanner.visibility = View.VISIBLE
                    HomeAppearance.clipBanner(home.panelHomeBanner)
                    val chips = listOf("All", "Other", "बहुत लंबी हिंदी श्रेणी का नाम", "A category with an exceptionally long accessible label").map {
                        HomeAppearance.categoryChip(context, it, "All") {}
                    }
                    chips.forEach { home.chipGroupCategory.addView(it) }
                    layout(home.root)
                    assertEquals(dp(width - 32), home.panelHomeBanner.width)
                    assertEquals(dp(160), home.panelHomeBanner.height)
                    val outline = Outline()
                    home.panelHomeBanner.outlineProvider.getOutline(home.panelHomeBanner, outline)
                    assertEquals(16f * density, outline.radius, 0.01f)
                    assertTrue(home.panelHomeBanner.clipToOutline)
                    if (font == 1f) {
                        assertEquals(dp(65), chips[0].width)
                        assertEquals(dp(40), chips[0].height)
                    }
                    for (chip in chips) {
                        assertEquals(minOf(71f * density, chip.chipMinHeight / 2f), chip.chipCornerRadius, 0.01f)
                        assertEquals(density, chip.chipStrokeWidth, 0.01f)
                        for (checked in listOf(false, true, false)) {
                            chip.isChecked = checked
                            assertEquals(border, chip.chipStrokeColor!!.getColorForState(chip.drawableState, 0))
                        }
                        assertEquals(dp(8), chip.paddingTop)
                        assertEquals(dp(8), chip.paddingBottom)
                        assertTrue(chip.height >= dp(40))
                        assertTrue(chip.width <= dp(width - 32))
                        assertEquals(chip.text.toString(), chip.contentDescription.toString())
                    }
                    for (i in 1 until chips.size) assertEquals(dp(8), chips[i].left - chips[i-1].right)
                    val result = ActivityResultBinding.inflate(LayoutInflater.from(context))
                    ResultTabs.bind(result) {}
                    layout(result.root)
                    assertEquals(dp(width - 32), result.tabLayoutResult.width)
                    if (font == 1f) assertEquals(dp(44), result.tabLayoutResult.height)
                    assertEquals(dp(4), result.tabLayoutResult.paddingTop)
                    assertEquals(dp(4), result.tabLayoutResult.paddingStart)
                    val grid = result.rowOverviewStatisticsTiles
                    assertEquals(if (width < 384) 1 else if (width < 566) 2 else 4, grid.columnCount)
                    for (i in 0 until grid.childCount) {
                        val tile = grid.getChildAt(i)
                        assertEquals(dp(170), tile.width)
                        assertTrue(tile.height >= dp(116))
                        if (font == 1f) assertEquals(dp(116), tile.height)
                        assertEquals(dp(14), tile.paddingStart)
                        assertTrue(tile.right <= grid.width)
                        if (i % grid.columnCount != 0) {
                            assertEquals(dp(12), tile.left - grid.getChildAt(i - 1).right)
                        } else if (i >= grid.columnCount) {
                            assertEquals(dp(12), tile.top - grid.getChildAt(i - grid.columnCount).bottom)
                        }
                        val surface = tile.background as GradientDrawable
                        assertEquals(dp(35).toFloat(), surface.cornerRadius, 0.01f)
                    }
                    repeat(12) { index ->
                        result.tabLayoutResult.getTabAt(index % 3)!!.select()
                        assertEquals(index % 3, result.tabLayoutResult.selectedTabPosition)
                    }
                    val label = result.tabLayoutResult.getTabAt(0)!!.customView as TextView
                    assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, 13f,
                        context.resources.displayMetrics), label.textSize, 0.51f)
                    val adapter = QuestionAdapter(listOf(Question(questionText = "हिंदी / English", optionA = "A long answer ".repeat(30))),
                        { "B" }, { _, _ -> }, { false }, {}, { false }, {}, { 0L })
                    val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
                    adapter.onBindViewHolder(holder, 0)
                    val question = ItemQuestionBinding.bind(holder.itemView)
                    layout(question.root)
                    val options = listOf(question.rbA, question.rbB, question.rbC, question.rbD)
                    for (option in options) {
                        assertEquals(dp(width - 32), option.width)
                        assertEquals(dp(56), option.minHeight)
                        assertTrue(option.height >= dp(56))
                        for (checked in listOf(false, true, false)) {
                            option.isChecked = checked
                            option.jumpDrawablesToCurrentState()
                            val surface = (option.background as StateListDrawable).current as GradientDrawable
                            assertEquals(16f * density, surface.cornerRadius, 0.01f)
                        }
                    }
                    for (i in 1 until options.size) assertEquals(dp(10), options[i].top - options[i-1].bottom)
                    // Existing inherited and stateful outlines, including disabled/focused controls.
                    val styled = LayoutInflater.from(context).inflate(R.layout.activity_app_config, FrameLayout(context), false)
                    fun visit(view: View) {
                        if (view is TextInputLayout) {
                            assertEquals(dp(1), view.boxStrokeWidth)
                            assertEquals(dp(1), view.boxStrokeWidthFocused)
                            val actualBox = TextInputLayout::class.java.getDeclaredField("boxBackground").apply {
                                isAccessible = true
                            }.get(view) as com.google.android.material.shape.MaterialShapeDrawable
                            for (enabled in listOf(true, false, true)) {
                                view.isEnabled = enabled
                                assertEquals(border, actualBox.strokeColor!!.defaultColor)
                                assertEquals(dp(1).toFloat(), actualBox.strokeWidth, 0.01f)
                            }
                            view.error = "Fixture error"
                            assertEquals(border, actualBox.strokeColor!!.defaultColor)
                            view.error = null
                            view.editText?.requestFocus()
                            assertEquals(border, actualBox.strokeColor!!.defaultColor)
                        }
                        if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
                    }
                    visit(styled)
                    if (font == 1f && width == 360) {
                        val replies = com.eve.app.ui.admin.PostRepliesAdapter({}, {})
                        val holder = replies.onCreateViewHolder(FrameLayout(context), 0)
                        val button = com.eve.app.databinding.ItemPostReplyBinding.bind(holder.itemView).btnMarkRead
                        for (read in listOf(true, false, true)) {
                            holder.bind(com.eve.app.data.model.FeedbackPostReply(read = read))
                            assertEquals(1f, button.alpha, 0f)
                            assertEquals(!read, button.isEnabled)
                            assertEquals(dp(1), button.strokeWidth)
                            assertEquals(border, button.strokeColor!!.getColorForState(button.drawableState, 0))
                            assertEquals(if (read) 128 else 255, Color.alpha(button.currentTextColor))
                        }
                    }
                    val inheritedCard = com.google.android.material.card.MaterialCardView(context)
                    assertEquals(dp(1), inheritedCard.strokeWidth)
                    assertEquals(border, inheritedCard.strokeColor)
                    val inherited = Chip(context)
                    assertEquals(density, inherited.chipStrokeWidth, 0.01f)
                    for (state in listOf(intArrayOf(), intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_enabled))) {
                        assertEquals(border, inherited.chipStrokeColor!!.getColorForState(state, 0))
                    }
                    // Offscreen fixtures never receive Android's detach callback. Release their
                    // looping artwork before inflating the next configuration.
                    fun release(view: View) {
                        view.animate().cancel()
                        if (view is com.airbnb.lottie.LottieAnimationView) view.cancelAnimation()
                        if (view is com.eve.app.ui.common.ShimmerSkeletonView) {
                            (view.javaClass.getDeclaredField("animator").apply { isAccessible = true }
                                .get(view) as? android.animation.ValueAnimator)?.cancel()
                        }
                        if (view is ViewGroup) for (i in 0 until view.childCount) release(view.getChildAt(i))
                    }
                    release(home.root)
                    release(result.root)
                    release(question.root)
                    release(styled)
                }
            }
        }
    }
}

package com.eve.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.eve.app.R
import com.eve.app.databinding.ActivityProfileBinding
import com.eve.app.databinding.ActivityResultBinding
import com.eve.app.databinding.BottomSheetTargetExamsBinding
import com.eve.app.databinding.ItemAnswerBinding
import com.eve.app.ui.common.EveLiquidGlassView
import com.eve.app.ui.home.TargetExamsBottomSheet
import com.eve.app.ui.result.ResultTabs
import com.eve.app.util.ThemeManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignRecoveryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun expandingContentAndShapesStayInsideTheirContainers() {
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                for (font in listOf(1f, 1.5f, 2f)) for (width in listOf(320, 360, 393, 412, 800)) {
                    val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                        uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or night
                        fontScale = font
                        screenWidthDp = width
                    }
                    val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                    val inflater = LayoutInflater.from(context)
                    val d = context.resources.displayMetrics.density
                    fun dp(value: Int) = Math.round(value * d)
                    fun layout(view: View, w: Int = width, h: Int = 1000, mode: Int = View.MeasureSpec.EXACTLY) {
                        repeat(2) {
                            view.measure(View.MeasureSpec.makeMeasureSpec(dp(w), View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(dp(h), mode))
                            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                        }
                    }
                    fun readable(text: TextView) {
                        assertNotNull(text.resources.getResourceEntryName(text.id), text.layout)
                        assertTrue("Text height must fit ${text.resources.getResourceEntryName(text.id)} at $width/$font",
                            text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom)
                        for (line in 0 until text.lineCount) assertEquals(0, text.layout.getEllipsisCount(line))
                    }
                    val result = ActivityResultBinding.inflate(inflater)
                    ResultTabs.bind(result) {}
                    result.btnReattempt.visibility = View.VISIBLE
                    result.tvScore.text = "9999.75 / 12000"
                    result.tvScorePercentage.text = "83.3% Score"
                    result.tvStats.visibility = View.VISIBLE
                    result.tvStats.text = "Completed assessment • विस्तृत परिणाम"
                    result.tvAccuracy.text = "100.00%"
                    layout(result.root)
                    assertTrue(result.btnReattempt.width >= dp(48))
                    assertEquals(context.getString(R.string.reattempt), result.btnReattempt.contentDescription.toString())
                    assertTrue(result.btnBack.width >= dp(48))
                    assertTrue(result.btnShare.width >= dp(48))
                    val hero = result.cardResultHero
                    assertTrue(hero.getChildAt(0).bottom <= hero.height)
                    readable(result.tvScore); readable(result.tvScorePercentage); readable(result.tvStats)
                    readable(result.tvAccuracy)
                    val outline = Outline()
                    hero.outlineProvider.getOutline(hero, outline)
                    assertEquals(20f * d, outline.radius, 0.01f)
                    // A radius change must update both pixels and clipping without requiring a size change.
                    val shape = EveLiquidGlassView(context)
                    layout(shape, 200, 100)
                    val first = Bitmap.createBitmap(shape.width, shape.height, Bitmap.Config.ARGB_8888)
                    shape.draw(Canvas(first))
                    assertEquals(Color.TRANSPARENT, first.getPixel(dp(2), dp(2)))
                    first.recycle()
                    shape.cornerRadiusPx = 0f
                    shape.outlineProvider.getOutline(shape, outline)
                    assertEquals(0f, outline.radius, 0f)
                    val square = Bitmap.createBitmap(shape.width, shape.height, Bitmap.Config.ARGB_8888)
                    shape.draw(Canvas(square))
                    assertEquals(context.getColor(R.color.eve_card_bg), square.getPixel(dp(2), dp(2)))
                    square.recycle()
                    val profile = ActivityProfileBinding.inflate(inflater)
                    profile.etName.setText("आरती शर्मा / A very long student name")
                    profile.etDob.setText("31/12/2000")
                    profile.tvEmail.text = "a.long.email.address@example.com"
                    profile.tvEveId.text = "EV-12345678901234567890"
                    layout(profile.root)
                    for (field in listOf(profile.tilName, profile.tilDob)) {
                        assertEquals(TextInputLayout.BOX_BACKGROUND_OUTLINE, field.boxBackgroundMode)
                        assertEquals(dp(1), field.boxStrokeWidth)
                        assertEquals(dp(2), field.boxStrokeWidthFocused)
                        assertTrue(field.editText!!.height >= dp(56))
                        assertTrue(field.editText!!.bottom <= field.height)
                    }
                    assertTrue(profile.btnCopyEveId.width >= dp(48))
                    assertTrue(profile.btnChangePhoto.width >= dp(48))
                    assertTrue(profile.btnSaveProfile.height >= dp(48))
                    assertEquals(0, profile.btnAbout.strokeWidth)
                    val review = ItemAnswerBinding.inflate(inflater)
                    review.tvQuestionNum.text = "Question 999"
                    review.tvQuestionStatusLabel.text = "Unattempted / उत्तर नहीं दिया"
                    review.ivBookmark.visibility = View.VISIBLE
                    review.tvQ.text = "हिंदी और English ".repeat(20)
                    layout(review.root, width - 32, 1000, View.MeasureSpec.AT_MOST)
                    readable(review.tvQ); readable(review.tvQuestionNum); readable(review.tvQuestionStatusLabel)
                    assertTrue(review.ivReportQuestion.right <= (review.ivReportQuestion.parent as View).width)
                    assertTrue(review.ivReportQuestion.width >= dp(48))
                    val sheet = BottomSheetTargetExamsBinding.inflate(inflater)
                    repeat(25) { sheet.chipGroupExams.addView(TargetExamsBottomSheet.examChip(context,
                        "Target exam $it — बहुत लंबा परीक्षा का नाम", it == 0)) }
                    layout(sheet.root, minOf(width, 640), 560, View.MeasureSpec.AT_MOST)
                    assertTrue(sheet.root.height <= dp(560))
                    assertTrue(sheet.root.getChildAt(0).height > sheet.root.height)
                    for (i in 0 until sheet.chipGroupExams.childCount) {
                        val chip = sheet.chipGroupExams.getChildAt(i) as Chip
                        assertTrue(chip.width <= sheet.chipGroupExams.width)
                        assertTrue(chip.height >= dp(48))
                        assertEquals(0f, chip.chipStrokeWidth, 0f)
                        assertEquals(chip.text.toString(), chip.contentDescription.toString())
                    }
                }
            }
        }
    }

    @Test fun metricGridRemeasuresInRtlAndInteractiveColorsHaveReadableContrast() {
        instrumentation.runOnMainSync {
            for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
                val config = Configuration(instrumentation.targetContext.resources.configuration).apply {
                    uiMode = uiMode and Configuration.UI_MODE_NIGHT_MASK.inv() or night
                    fontScale = 1f
                    setLayoutDirection(java.util.Locale("ar"))
                }
                val context = ContextThemeWrapper(instrumentation.targetContext.createConfigurationContext(config), R.style.Theme_Eve)
                val result = ActivityResultBinding.inflate(LayoutInflater.from(context))
                val grid = result.rowOverviewStatisticsTiles
                grid.layoutDirection = View.LAYOUT_DIRECTION_RTL
                val d = context.resources.displayMetrics.density
                for (width in listOf(768, 288, 380, 328)) {
                    grid.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width * d), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    grid.layout(0, 0, grid.measuredWidth, grid.measuredHeight)
                    assertEquals(if (width == 768) 4 else 2, grid.columnCount)
                    for (i in 0 until grid.childCount) {
                        assertTrue(grid.getChildAt(i).left >= 0)
                        assertTrue(grid.getChildAt(i).right <= grid.width)
                    }
                }
                val accent = context.getColor(R.color.eve_recovery_accent)
                for (background in listOf(R.color.eve_recovery_selected, R.color.eve_card_bg, R.color.eve_bg)) {
                    assertTrue("Interactive labels must meet 4.5:1 in both themes",
                        androidx.core.graphics.ColorUtils.calculateContrast(accent, context.getColor(background)) >= 4.5)
                }
                assertTrue(androidx.core.graphics.ColorUtils.calculateContrast(context.getColor(R.color.eve_recovery_on_accent), accent) >= 4.5)
            }
        }
    }

    @androidx.test.filters.SdkSuppress(minSdkVersion = 29)
    @Test fun attachedProfileAndSheetRenderAndSaveSelectionInBothThemes() {
        val context = instrumentation.targetContext
        val themePrefs = context.getSharedPreferences(ThemeManager.PREFS, 0)
        val hadTheme = themePrefs.contains(ThemeManager.KEY_DARK_MODE)
        val oldTheme = themePrefs.getBoolean(ThemeManager.KEY_DARK_MODE, false)
        val oldMode = AppCompatDelegate.getDefaultNightMode()
        val targets = context.getSharedPreferences(TargetExamsBottomSheet.PREFS_NAME, 0)
        val hadTargets = targets.contains(TargetExamsBottomSheet.KEY_TARGET_EXAMS)
        val oldTargets = targets.getStringSet(TargetExamsBottomSheet.KEY_TARGET_EXAMS, emptySet())!!.toSet()
        val hadOnboarding = targets.contains(TargetExamsBottomSheet.KEY_ONBOARDING_DONE)
        val oldOnboarding = targets.getBoolean(TargetExamsBottomSheet.KEY_ONBOARDING_DONE, false)
        try {
            for (dark in listOf(false, true)) {
                instrumentation.runOnMainSync {
                    themePrefs.edit().putBoolean(ThemeManager.KEY_DARK_MODE, dark).commit()
                    AppCompatDelegate.setDefaultNightMode(if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
                }
                ActivityScenario.launch(RepairVerificationActivity::class.java).use { scenario ->
                    lateinit var profile: ActivityProfileBinding
                    scenario.onActivity { activity ->
                        profile = ActivityProfileBinding.inflate(activity.layoutInflater)
                        activity.window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                        activity.setContentView(profile.root)
                        profile.tvName.text = "Aarti Sharma"
                        profile.tvEmail.text = "aarti@example.com"
                        profile.tvEveId.text = "EV-ABC123"
                        profile.etName.setText("Aarti Sharma")
                        profile.etDob.setText("12/06/2001")
                    }
                    instrumentation.waitForIdleSync()
                    capture("profile", dark, profile.root, scenario)
                    lateinit var dialog: BottomSheetDialog
                    var saved: Set<String>? = null
                    val exams = List(25) { com.eve.app.data.model.Exam(id = "recovery-fixture-$it", examName = "Target exam $it — प्रतियोगी परीक्षा") }
                    scenario.onActivity { activity -> dialog = TargetExamsBottomSheet.show(activity, exams) { saved = it } }
                    instrumentation.waitForIdleSync()
                    val root = (dialog.findViewById<View>(R.id.chipGroupExams)!!.parent as View).parent as androidx.core.widget.NestedScrollView
                    val sheet = BottomSheetTargetExamsBinding.bind(root)
                    capture("target-exams", dark, root, scenario)
                    scenario.onActivity {
                        val chip = sheet.chipGroupExams.getChildAt(0) as Chip
                        chip.isChecked = false
                        chip.performClick()
                        assertTrue(chip.isChecked)
                        root.fullScroll(View.FOCUS_DOWN)
                    }
                    instrumentation.waitForIdleSync()
                    scenario.onActivity {
                        assertTrue("Footer must be reachable by scrolling", sheet.btnContinue.getGlobalVisibleRect(android.graphics.Rect()))
                        sheet.btnContinue.performClick()
                        assertNotNull(saved)
                        assertTrue(saved!!.contains(exams.first { it.examName == (sheet.chipGroupExams.getChildAt(0) as Chip).text.toString() }.id))
                        assertEquals(saved, TargetExamsBottomSheet.getTargetExamIds(context))
                        assertTrue(TargetExamsBottomSheet.isOnboardingDone(context))
                        assertFalse(dialog.isShowing)
                    }
                    // Skip closes without replacing selected exam IDs or invoking the save callback.
                    scenario.onActivity { activity -> dialog = TargetExamsBottomSheet.show(activity, exams) { fail("Skip must not save a new selection") } }
                    instrumentation.waitForIdleSync()
                    scenario.onActivity {
                        dialog.findViewById<View>(R.id.btnSkip)!!.performClick()
                        assertEquals(saved, TargetExamsBottomSheet.getTargetExamIds(context))
                        assertFalse(dialog.isShowing)
                    }
                }
            }
        } finally {
            val restore = targets.edit()
            if (hadTargets) restore.putStringSet(TargetExamsBottomSheet.KEY_TARGET_EXAMS, oldTargets) else restore.remove(TargetExamsBottomSheet.KEY_TARGET_EXAMS)
            if (hadOnboarding) restore.putBoolean(TargetExamsBottomSheet.KEY_ONBOARDING_DONE, oldOnboarding) else restore.remove(TargetExamsBottomSheet.KEY_ONBOARDING_DONE)
            restore.commit()
            instrumentation.runOnMainSync {
                val editor = themePrefs.edit()
                if (hadTheme) editor.putBoolean(ThemeManager.KEY_DARK_MODE, oldTheme) else editor.remove(ThemeManager.KEY_DARK_MODE)
                editor.commit()
                AppCompatDelegate.setDefaultNightMode(oldMode)
            }
        }
    }

    private fun capture(name: String, dark: Boolean, root: View, scenario: ActivityScenario<RepairVerificationActivity>) {
        val frame = java.util.concurrent.CountDownLatch(1)
        scenario.onActivity {
            root.viewTreeObserver.registerFrameCommitCallback { frame.countDown() }
            root.invalidate()
        }
        assertTrue(frame.await(5, java.util.concurrent.TimeUnit.SECONDS))
        val bitmap = instrumentation.uiAutomation.takeScreenshot()!!
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "recovery-$name-${if (dark) "dark" else "light"}.png")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/eve-approved-ui")
            }
            val resolver = instrumentation.targetContext.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { bitmap.recycle() }
    }
}

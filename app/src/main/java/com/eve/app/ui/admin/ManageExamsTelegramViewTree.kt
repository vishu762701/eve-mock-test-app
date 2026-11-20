package com.eve.app.ui.admin

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.eve.app.R
import com.eve.app.designsystem.TelegramAutoCompleteTextView
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramEditText
import com.eve.app.designsystem.TelegramNumberField
import com.eve.app.designsystem.TelegramOutlineEditText
import com.eve.app.designsystem.TelegramSwitch
import com.eve.app.ui.common.ErrorStateView
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ManageExamsActivity (Pass 4d).
 * Replaces activity_manage_exams.xml inflation using upstream RecyclerListView,
 * RadialProgressView, HeaderCell, ShadowSectionCell, Theme.getColor(Theme.key_*),
 * and AndroidUtilities.
 */
class ManageExamsTelegramViewTree(context: Context) {

    private fun dp(v: Float): Int = AndroidUtilities.dp(v)
    private fun mediumFont(): Typeface =
        AndroidUtilities.getTypeface("fonts/rmedium.ttf") ?: Typeface.DEFAULT_BOLD

    val root: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private val topBar: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4f), 0, dp(4f), 0)
    }
    private val topDivider: View = View(context)

    val btnBack: ImageButton = ImageButton(context).apply {
        id = R.id.btnBack
        setImageResource(R.drawable.ic_ab_back)
        contentDescription = "Back"
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
    }

    private val tvTitle: TextView = TextView(context).apply {
        text = "Manage Exams"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(22f))
        visibility = View.GONE
    }

    val scrollManageExamContent: ScrollView = ScrollView(context).apply {
        id = R.id.scrollManageExamContent
        isFillViewport = true
    }

    val tvActiveContextBadge: TextView = TextView(context).apply {
        id = R.id.tvActiveContextBadge
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        typeface = mediumFont()
    }
    val tvActiveContextTitle: TextView = TextView(context).apply {
        id = R.id.tvActiveContextTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = mediumFont()
    }
    val tvActiveContextSubtitle: TextView = TextView(context).apply {
        id = R.id.tvActiveContextSubtitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }

    val tilSelectExam: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilSelectExam }
    val actvExam: TelegramAutoCompleteTextView = TelegramAutoCompleteTextView(context).apply { id = R.id.actvExam; hint = "Select exam" }
    val btnNewExam: TelegramButton = TelegramButton(context).apply { id = R.id.btnNewExam; text = "New Exam" }
    val btnAddSubExam: TelegramButton = TelegramButton(context).apply { id = R.id.btnAddSubExam; text = "Add Sub-Exam" }

    val tilParentExam: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilParentExam }
    val actvParentExam: TelegramAutoCompleteTextView = TelegramAutoCompleteTextView(context).apply { id = R.id.actvParentExam; hint = "Parent exam" }

    val tilTestNumber: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilTestNumber }
    val etTestNumber: TelegramEditText = TelegramEditText(context).apply { id = R.id.etTestNumber; hint = "Test number"; inputType = InputType.TYPE_CLASS_NUMBER }

    val numberFieldQuestionCount: TelegramNumberField = TelegramNumberField(context).apply { id = R.id.numberFieldQuestionCount }
    val tilQuestionCount: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilQuestionCount }
    val etQuestionCount: TelegramEditText = TelegramEditText(context).apply { id = R.id.etQuestionCount; hint = "Question count"; inputType = InputType.TYPE_CLASS_NUMBER }

    val tilDuration: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilDuration }
    val etDuration: TelegramEditText = TelegramEditText(context).apply { id = R.id.etDuration; hint = "Duration (minutes)"; inputType = InputType.TYPE_CLASS_NUMBER }

    val tilNegativeMarking: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilNegativeMarking }
    val etNegativeMarking: TelegramEditText = TelegramEditText(context).apply { id = R.id.etNegativeMarking; hint = "Negative marking"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }

    val etGenerationPrompt: TelegramEditText = TelegramEditText(context).apply { id = R.id.etGenerationPrompt; hint = "AI generation prompt"; minLines = 3 }
    val switchAutoGen: TelegramSwitch = TelegramSwitch(context).apply { id = R.id.switchAutoGen }
    val btnPickTime: TelegramButton = TelegramButton(context).apply { id = R.id.btnPickTime; text = "Pick Daily Time" }
    val actvPublishMode: TelegramAutoCompleteTextView = TelegramAutoCompleteTextView(context).apply { id = R.id.actvPublishMode; hint = "Publish mode" }
    val tvPublishModeHelper: TextView = TextView(context).apply { id = R.id.tvPublishModeHelper; setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f) }
    val tvLastRunStatus: TextView = TextView(context).apply { id = R.id.tvLastRunStatus; setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f) }

    val compactErrorView: ErrorStateView = ErrorStateView(context).apply { id = R.id.compactErrorView; visibility = View.GONE }

    val layoutSaveContainer: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutSaveContainer
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
    }
    val btnSave: TelegramButton = TelegramButton(context).apply { id = R.id.btnSave; text = "Save Settings" }
    val btnGenerateNow: TelegramButton = TelegramButton(context).apply { id = R.id.btnGenerateNow; text = "Generate Now" }

    val tvGenTestsSectionTitle: TextView = TextView(context).apply {
        id = R.id.tvGenTestsSectionTitle
        text = "Generated Tests"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        typeface = mediumFont()
        setPadding(dp(20f), dp(12f), dp(20f), dp(6f))
    }
    val progressBarGenTests: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarGenTests
        setSize(dp(28f))
        visibility = View.GONE
    }
    val tvNoGenTests: TextView = TextView(context).apply {
        id = R.id.tvNoGenTests
        text = "No generated tests yet."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
        setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        visibility = View.GONE
    }
    val rvGeneratedTestsForExam: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvGeneratedTestsForExam
        isNestedScrollingEnabled = false
    }

    val progressBarCenter: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarCenter
        setSize(dp(36f))
        visibility = View.GONE
    }

    private val contextCard: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
    }
    private val autoGenLabel: TextView = TextView(context).apply {
        text = "Enable Daily Auto-Generation"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
    }

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        topBar.addView(progressBar, LayoutHelper.createLinear(32, 32, 0f, 0f, 12f, 0f))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6f), 0, dp(24f))
        }

        contextCard.addView(tvActiveContextBadge)
        contextCard.addView(tvActiveContextTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 2f, 0f, 2f))
        contextCard.addView(tvActiveContextSubtitle)
        body.addView(contextCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 8f))

        body.addView(HeaderCell(context).apply { setText("Exam Selection") })
        tilSelectExam.addView(actvExam, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilSelectExam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))

        val topBtns = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        topBtns.addView(btnNewExam, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginEnd = dp(6f) })
        topBtns.addView(btnAddSubExam, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginStart = dp(6f) })
        body.addView(topBtns, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 0f, 16f, 8f))

        tilParentExam.addView(actvParentExam, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilParentExam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))

        body.addView(ShadowSectionCell(context))
        body.addView(HeaderCell(context).apply { setText("Test Parameters") })
        tilTestNumber.addView(etTestNumber, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilTestNumber, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 6f))
        body.addView(numberFieldQuestionCount, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 4f))
        tilQuestionCount.addView(etQuestionCount, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilQuestionCount, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 6f))
        tilDuration.addView(etDuration, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilDuration, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 6f))
        tilNegativeMarking.addView(etNegativeMarking, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilNegativeMarking, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))
        body.addView(etGenerationPrompt, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))

        val swRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
        }
        swRow.addView(autoGenLabel, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        swRow.addView(switchAutoGen)
        body.addView(swRow)
        body.addView(btnPickTime, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 16f, 4f, 16f, 8f))
        body.addView(actvPublishMode, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 2f))
        body.addView(tvPublishModeHelper, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 0f, 16f, 4f))
        body.addView(tvLastRunStatus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 0f, 16f, 6f))
        body.addView(compactErrorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 6f))

        layoutSaveContainer.addView(btnSave, LinearLayout.LayoutParams(0, dp(44f), 1f).apply { marginEnd = dp(6f) })
        layoutSaveContainer.addView(btnGenerateNow, LinearLayout.LayoutParams(0, dp(44f), 1f).apply { marginStart = dp(6f) })
        body.addView(layoutSaveContainer)

        body.addView(ShadowSectionCell(context))
        body.addView(tvGenTestsSectionTitle)
        body.addView(progressBarGenTests, LayoutHelper.createLinear(32, 32, Gravity.CENTER_HORIZONTAL))
        body.addView(tvNoGenTests, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(rvGeneratedTestsForExam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        scrollManageExamContent.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val centerHost = FrameLayout(context)
        centerHost.addView(scrollManageExamContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBarCenter, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        root.addView(centerHost, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        applyTelegramTheme()
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val bgGray = Theme.getColor(Theme.key_windowBackgroundGray)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4)
        val abBg = Theme.getColor(Theme.key_actionBarDefault)
        val abTitle = Theme.getColor(Theme.key_actionBarDefaultTitle)
        val abIcon = Theme.getColor(Theme.key_actionBarDefaultIcon)
        val abSelector = Theme.getColor(Theme.key_actionBarDefaultSelector)

        root.setBackgroundColor(bgWhite)
        topBar.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(Theme.getColor(Theme.key_divider))
        tvTitle.setTextColor(abTitle)
        btnBack.imageTintList = ColorStateList.valueOf(abIcon)
        btnBack.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)

        contextCard.background = GradientDrawable().apply {
            cornerRadius = dp(12f).toFloat()
            setColor(bgGray)
        }
        tvActiveContextBadge.setTextColor(accent)
        tvActiveContextTitle.setTextColor(textPrimary)
        tvActiveContextSubtitle.setTextColor(textSecondary)
        autoGenLabel.setTextColor(textPrimary)
        tvPublishModeHelper.setTextColor(textSecondary)
        tvLastRunStatus.setTextColor(textSecondary)
        tvGenTestsSectionTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader))
        tvNoGenTests.setTextColor(textSecondary)
        rvGeneratedTestsForExam.setBackgroundColor(bgWhite)
        listOf(progressBar, progressBarGenTests, progressBarCenter).forEach {
            it.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        }
    }
}

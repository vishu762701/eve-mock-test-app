package com.eve.app.ui.admin

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
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
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.eve.app.R
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramSpinner
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.DividerCell
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for EditExamActivity (Pass 4c).
 * Replaces activity_edit_exam.xml and Eve XML inputs with upstream EditTextBoldCursor,
 * HeaderCell, ShadowSectionCell, DividerCell, RecyclerListView, RadialProgressView,
 * Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class EditExamTelegramViewTree(context: Context) {

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
        text = "Edit Exam"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val scrollEditContent: NestedScrollView = NestedScrollView(context).apply {
        id = R.id.scrollEditContent
        isFillViewport = true
    }

    private val boldInputs = mutableListOf<EditTextBoldCursor>()

    private fun createTelegramInput(context: Context, viewId: Int, hintText: String, numeric: Boolean = false): EditTextBoldCursor {
        return EditTextBoldCursor(context).apply {
            id = viewId
            hint = hintText
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setCursorSize(dp(20f))
            setCursorWidth(1.5f)
            setSingleLine(true)
            inputType = if (numeric) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_TEXT
            setPadding(dp(20f), dp(12f), dp(20f), dp(12f))
            background = null
            boldInputs.add(this)
        }
    }

    val etExamName: EditTextBoldCursor = createTelegramInput(context, R.id.etExamName, "Exam Name")
    val etExamMinutes: EditTextBoldCursor = createTelegramInput(context, R.id.etExamMinutes, "Duration (minutes)", numeric = true)
    val spCategory: TelegramSpinner = TelegramSpinner(context).apply { id = R.id.spCategory }
    val btnPickEmoji: TelegramButton = TelegramButton(context).apply { id = R.id.btnPickEmoji; text = "Pick Emoji" }

    val etCutoffGeneral: EditTextBoldCursor = createTelegramInput(context, R.id.etCutoffGeneral, "General Cutoff", numeric = true)
    val etCutoffObc: EditTextBoldCursor = createTelegramInput(context, R.id.etCutoffObc, "OBC Cutoff", numeric = true)
    val etCutoffSc: EditTextBoldCursor = createTelegramInput(context, R.id.etCutoffSc, "SC Cutoff", numeric = true)
    val etCutoffSt: EditTextBoldCursor = createTelegramInput(context, R.id.etCutoffSt, "ST Cutoff", numeric = true)
    val etCutoffEws: EditTextBoldCursor = createTelegramInput(context, R.id.etCutoffEws, "EWS Cutoff", numeric = true)

    val btnSaveExamDetails: TelegramButton = TelegramButton(context).apply { id = R.id.btnSaveExamDetails; text = "Save Exam Details" }

    val tvQuestionsHeader: TextView = TextView(context).apply {
        id = R.id.tvQuestionsHeader
        text = "Questions"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        typeface = mediumFont()
        setPadding(dp(20f), dp(12f), dp(20f), dp(6f))
    }

    val btnAddQuestion: TelegramButton = TelegramButton(context).apply { id = R.id.btnAddQuestion; text = "Add Question" }
    val btnBulkUpload: TelegramButton = TelegramButton(context).apply { id = R.id.btnBulkUpload; text = "Bulk Import" }

    val rvQuestions: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvQuestions
        isNestedScrollingEnabled = false
    }

    val tvNoQuestions: TextView = TextView(context).apply {
        id = R.id.tvNoQuestions
        text = "No questions added yet."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
        setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
        visibility = View.GONE
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(36f))
        visibility = View.GONE
    }

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4f), 0, dp(24f))
        }
        body.addView(HeaderCell(context).apply { setText("Exam Details") })
        body.addView(etExamName, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(DividerCell(context))
        body.addView(etExamMinutes, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(DividerCell(context))
        body.addView(spCategory, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 8f, 16f, 8f))
        body.addView(btnPickEmoji, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 16f, 4f, 16f, 10f))

        body.addView(ShadowSectionCell(context))
        body.addView(HeaderCell(context).apply { setText("Category Cutoffs") })
        listOf(etCutoffGeneral, etCutoffObc, etCutoffSc, etCutoffSt, etCutoffEws).forEachIndexed { idx, et ->
            body.addView(et, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
            if (idx < 4) body.addView(DividerCell(context))
        }
        body.addView(btnSaveExamDetails, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 16f, 12f, 16f, 12f))

        body.addView(ShadowSectionCell(context))
        body.addView(tvQuestionsHeader)
        val qBtns = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        qBtns.addView(btnAddQuestion, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginEnd = dp(6f) })
        qBtns.addView(btnBulkUpload, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginStart = dp(6f) })
        body.addView(qBtns, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 10f))
        body.addView(tvNoQuestions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(rvQuestions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        scrollEditContent.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val centerHost = FrameLayout(context)
        centerHost.addView(scrollEditContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBar, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        root.addView(centerHost, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        applyTelegramTheme()
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val hintColor = Theme.getColor(Theme.key_windowBackgroundWhiteHintText)
        val lineNormal = Theme.getColor(Theme.key_windowBackgroundWhiteInputField)
        val lineActive = Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated)
        val lineError = Theme.getColor(Theme.key_text_RedRegular)
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

        boldInputs.forEach { et ->
            et.setTextColor(textPrimary)
            et.setHintTextColor(hintColor)
            et.setCursorColor(textPrimary)
            et.setLineColors(lineNormal, lineActive, lineError)
        }
        tvQuestionsHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader))
        tvNoQuestions.setTextColor(textSecondary)
        rvQuestions.setBackgroundColor(bgWhite)
        progressBar.setProgressColor(Theme.getColor(Theme.key_progressCircle))
    }
}

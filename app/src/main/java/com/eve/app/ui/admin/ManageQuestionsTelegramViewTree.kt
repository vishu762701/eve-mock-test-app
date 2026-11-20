package com.eve.app.ui.admin

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.eve.app.R
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramSpinner
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ManageQuestionsActivity (Pass 4d).
 * Replaces legacy XML inflation using upstream RecyclerListView,
 * RadialProgressView, HeaderCell, ShadowSectionCell, Theme.getColor(Theme.key_*),
 * and AndroidUtilities.
 */
class ManageQuestionsTelegramViewTree(context: Context) {

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

    val telegramActionBar: TelegramActionBar = TelegramActionBar(context).apply {
        id = R.id.telegramActionBar
    }

    val scrollContent: ScrollView = ScrollView(context).apply {
        id = R.id.scrollContent
        isFillViewport = true
    }

    val spExam: TelegramSpinner = TelegramSpinner(context).apply { id = R.id.spExam }
    val btnRenameExam: TelegramButton = TelegramButton(context).apply { id = R.id.btnRenameExam; text = "Rename Exam" }
    val btnEditExamImage: TelegramButton = TelegramButton(context).apply { id = R.id.btnEditExamImage; text = "Edit Image" }
    val btnDeleteExam: TelegramButton = TelegramButton(context).apply { id = R.id.btnDeleteExam; text = "Delete Exam" }
    val btnAddQuestionManual: TelegramButton = TelegramButton(context).apply { id = R.id.btnAddQuestionManual; text = "Add Question" }
    val btnBulkUploadSelectedExam: TelegramButton = TelegramButton(context).apply { id = R.id.btnBulkUploadSelectedExam; text = "Bulk Import" }

    val rvQuestions: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvQuestions
        isNestedScrollingEnabled = false
    }

    val tvNoQuestions: TextView = TextView(context).apply {
        id = R.id.tvNoQuestions
        text = "No questions in this exam yet."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
        setPadding(dp(20f), dp(16f), dp(20f), dp(16f))
        visibility = View.GONE
    }

    val layoutEmptyQuestions: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutEmptyQuestions
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        visibility = View.GONE
    }

    val tvEmptyQuestionsTitle: TextView = TextView(context).apply {
        id = R.id.tvEmptyQuestionsTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
    }

    val tvEmptyQuestionsSubtitle: TextView = TextView(context).apply {
        id = R.id.tvEmptyQuestionsSubtitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(36f))
        visibility = View.GONE
    }

    private val divider: View = View(context)

    init {
        root.addView(telegramActionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4f), 0, dp(24f))
        }
        body.addView(HeaderCell(context).apply { setText("Select Exam") })
        body.addView(spExam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 8f))

        val examActions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        examActions.addView(btnRenameExam, LinearLayout.LayoutParams(0, dp(40f), 1f).apply { marginEnd = dp(4f) })
        examActions.addView(btnEditExamImage, LinearLayout.LayoutParams(0, dp(40f), 1f).apply { marginStart = dp(4f); marginEnd = dp(4f) })
        examActions.addView(btnDeleteExam, LinearLayout.LayoutParams(0, dp(40f), 1f).apply { marginStart = dp(4f) })
        body.addView(examActions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 0f, 16f, 8f))

        body.addView(ShadowSectionCell(context))
        body.addView(HeaderCell(context).apply { setText("Questions") })
        val qActions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        qActions.addView(btnAddQuestionManual, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginEnd = dp(6f) })
        qActions.addView(btnBulkUploadSelectedExam, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginStart = dp(6f) })
        body.addView(qActions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 10f))

        layoutEmptyQuestions.addView(tvEmptyQuestionsTitle)
        layoutEmptyQuestions.addView(tvEmptyQuestionsSubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 0f))
        body.addView(layoutEmptyQuestions)
        body.addView(tvNoQuestions)
        body.addView(rvQuestions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        scrollContent.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val centerHost = FrameLayout(context)
        centerHost.addView(scrollContent, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBar, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        root.addView(centerHost, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        applyTelegramTheme()
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)

        root.setBackgroundColor(bgWhite)
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider))
        rvQuestions.setBackgroundColor(bgWhite)
        tvNoQuestions.setTextColor(textSecondary)
        tvEmptyQuestionsTitle.setTextColor(textPrimary)
        tvEmptyQuestionsSubtitle.setTextColor(textSecondary)
        progressBar.setProgressColor(Theme.getColor(Theme.key_progressCircle))
    }
}

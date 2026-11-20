package com.eve.app.ui.admin

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
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
import com.eve.app.designsystem.TelegramAutoCompleteTextView
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramEditText
import com.eve.app.designsystem.TelegramFileUploadView
import com.eve.app.designsystem.TelegramMorphSelectView
import com.eve.app.designsystem.TelegramOutlineEditText
import com.eve.app.designsystem.TelegramUploadStepperView
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ManageSyllabusActivity (Pass 4c).
 * Replaces activity_manage_syllabus.xml inflation using upstream RecyclerListView,
 * RadialProgressView, HeaderCell, ShadowSectionCell, Theme.getColor(Theme.key_*),
 * and AndroidUtilities.
 */
class ManageSyllabusTelegramViewTree(context: Context) {

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

    val btnBack: ImageButton = createTopIcon(context, R.id.btnBack, R.drawable.ic_ab_back, "Back")
    private val tvTitle: TextView = TextView(context).apply {
        text = "Manage Syllabus"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val progressBarTop: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarTop
        setSize(dp(22f))
        visibility = View.GONE
    }

    val scrollSyllabus: NestedScrollView = NestedScrollView(context).apply {
        id = R.id.scrollSyllabus
        isFillViewport = true
    }

    val stepperNewSyllabus: TelegramUploadStepperView = TelegramUploadStepperView(context).apply { id = R.id.stepperNewSyllabus }
    val morphSelectExam: TelegramMorphSelectView = TelegramMorphSelectView(context).apply { id = R.id.morphSelectExam }
    val tilSelectExam: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilSelectExam }
    val actvSelectExam: TelegramAutoCompleteTextView = TelegramAutoCompleteTextView(context).apply { id = R.id.actvSelectExam; hint = "Select exam" }
    val fileUploadView: TelegramFileUploadView = TelegramFileUploadView(context).apply { id = R.id.fileUploadView }
    val btnChoosePdf: TelegramButton = TelegramButton(context).apply { id = R.id.btnChoosePdf; text = "Choose PDF" }
    val btnClearSelectedPdf: ImageButton = createTopIcon(context, R.id.btnClearSelectedPdf, R.drawable.ic_ab_close, "Clear PDF").apply { visibility = View.GONE }
    val btnUploadSyllabus: TelegramButton = TelegramButton(context).apply { id = R.id.btnUploadSyllabus; text = "Upload Syllabus" }
    val progressBarUpload: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarUpload
        setSize(dp(28f))
        visibility = View.GONE
    }

    val etSearch: TelegramEditText = TelegramEditText(context).apply { id = R.id.etSearch; hint = "Search syllabuses..." }
    val tvSyllabusCount: TextView = TextView(context).apply {
        id = R.id.tvSyllabusCount
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
    }
    val rvSyllabuses: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvSyllabuses
        isNestedScrollingEnabled = false
    }
    val progressBarCenter: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarCenter
        setSize(dp(36f))
        visibility = View.GONE
    }
    val layoutEmpty: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutEmpty
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        visibility = View.GONE
    }

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        topBar.addView(progressBarTop, LayoutHelper.createLinear(32, 32, 0f, 0f, 12f, 0f))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4f), 0, dp(24f))
        }
        body.addView(HeaderCell(context).apply { setText("Upload New Syllabus") })
        body.addView(stepperNewSyllabus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 6f))
        body.addView(morphSelectExam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 4f))
        tilSelectExam.addView(actvSelectExam, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilSelectExam, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))
        body.addView(fileUploadView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))

        val pdfRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        pdfRow.addView(btnChoosePdf, LinearLayout.LayoutParams(0, dp(44f), 1f))
        pdfRow.addView(btnClearSelectedPdf, LayoutHelper.createLinear(44, 44, 6f, 0f, 0f, 0f))
        body.addView(pdfRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 0f, 16f, 8f))
        body.addView(btnUploadSyllabus, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 16f, 0f, 16f, 6f))
        body.addView(progressBarUpload, LayoutHelper.createLinear(32, 32, Gravity.CENTER_HORIZONTAL))

        body.addView(ShadowSectionCell(context))
        body.addView(HeaderCell(context).apply { setText("Uploaded Syllabuses") })
        body.addView(etSearch, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 6f))
        body.addView(tvSyllabusCount, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 0f, 16f, 6f))

        val listFrame = FrameLayout(context)
        listFrame.addView(rvSyllabuses, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat()))
        listFrame.addView(progressBarCenter, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        layoutEmpty.addView(TextView(context).apply {
            text = "No syllabuses uploaded yet."
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))
        })
        listFrame.addView(layoutEmpty, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat()))
        body.addView(listFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        scrollSyllabus.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        root.addView(scrollSyllabus, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        applyTelegramTheme()
    }

    private fun createTopIcon(context: Context, viewId: Int, iconRes: Int, desc: String): ImageButton {
        return ImageButton(context).apply {
            id = viewId
            setImageResource(iconRes)
            contentDescription = desc
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
        }
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
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
        btnClearSelectedPdf.imageTintList = ColorStateList.valueOf(abIcon)
        btnClearSelectedPdf.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        tvSyllabusCount.setTextColor(textSecondary)
        rvSyllabuses.setBackgroundColor(bgWhite)
        listOf(progressBarTop, progressBarUpload, progressBarCenter).forEach {
            it.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        }
    }
}

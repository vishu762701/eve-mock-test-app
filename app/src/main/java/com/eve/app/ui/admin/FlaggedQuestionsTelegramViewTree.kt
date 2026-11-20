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
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.eve.app.R
import com.eve.app.designsystem.TelegramChip
import com.eve.app.designsystem.TelegramChipGroup
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for FlaggedQuestionsActivity (Pass 4b).
 * Replaces activity_flagged_questions.xml inflation using upstream RecyclerListView,
 * RadialProgressView, Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class FlaggedQuestionsTelegramViewTree(context: Context) {

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
        text = "Flagged Questions"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val btnRefresh: ImageButton = createTopIcon(context, R.id.btnRefresh, R.drawable.ic_refresh, "Refresh")

    val scrollReportType: HorizontalScrollView = HorizontalScrollView(context).apply {
        id = R.id.scrollReportType
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
    }

    val chipGroupReportType: TelegramChipGroup = TelegramChipGroup(context).apply {
        id = R.id.chipGroupReportType
    }

    val chipContentIssues: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipContentIssues
        text = "Content Issues"
    }

    val chipTechnicalIssues: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipTechnicalIssues
        text = "Technical Issues"
    }

    val swipeRefresh: SwipeRefreshLayout = SwipeRefreshLayout(context).apply {
        id = R.id.swipeRefresh
    }

    val rvFlaggedQuestions: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvFlaggedQuestions
        clipToPadding = false
        setPadding(0, dp(4f), 0, dp(24f))
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(36f))
        visibility = View.GONE
    }

    val tvEmpty: TextView = TextView(context).apply {
        id = R.id.tvEmpty
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        gravity = Gravity.CENTER
        setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        visibility = View.GONE
    }

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        chipGroupReportType.addView(chipContentIssues)
        chipGroupReportType.addView(chipTechnicalIssues)
        scrollReportType.addView(chipGroupReportType, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(scrollReportType, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val centerHost = FrameLayout(context)
        centerHost.addView(rvFlaggedQuestions, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBar, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        centerHost.addView(tvEmpty, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER))

        swipeRefresh.addView(centerHost, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        root.addView(swipeRefresh, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

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

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnRefresh).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }
        rvFlaggedQuestions.setBackgroundColor(bgWhite)
        progressBar.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        tvEmpty.setTextColor(textSecondary)
    }
}

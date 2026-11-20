package com.eve.app.ui.admin

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.eve.app.R
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramSegmentedControl
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for AdminAnalyticsActivity (Pass 4a).
 * Replaces activity_admin_analytics.xml inflation using real upstream RecyclerListView,
 * RadialProgressView, HeaderCell, Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class AdminAnalyticsTelegramViewTree(context: Context) {

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
        text = "Analytics"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val btnRefresh: ImageButton = createTopIcon(context, R.id.btnRefresh, R.drawable.ic_refresh, "Refresh")

    val swipeRefresh: SwipeRefreshLayout = SwipeRefreshLayout(context).apply {
        id = R.id.swipeRefresh
    }

    val content: ScrollView = ScrollView(context).apply {
        id = R.id.content
        isFillViewport = true
    }

    val chipGroupTimeRange: TelegramSegmentedControl = TelegramSegmentedControl(context).apply {
        id = R.id.chipGroupTimeRange
    }

    val tvSummary: TextView = TextView(context).apply {
        id = R.id.tvSummary
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        typeface = mediumFont()
    }

    val tvMostAttempted: TextView = TextView(context).apply {
        id = R.id.tvMostAttempted
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }

    val barChartView: ExamAttemptsBarChartView = ExamAttemptsBarChartView(context).apply {
        id = R.id.barChartView
    }

    val rvExams: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvExams
        isNestedScrollingEnabled = false
    }

    val rvQuestions: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvQuestions
        isNestedScrollingEnabled = false
    }

    val btnOpenCrashlyticsConsole: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnOpenCrashlyticsConsole
        text = "Open Crashlytics Console"
    }

    val tvEmpty: TextView = TextView(context).apply {
        id = R.id.tvEmpty
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
        setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        visibility = View.GONE
    }

    val progress: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progress
        setSize(dp(36f))
        visibility = View.GONE
    }

    private val summaryCard: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
    }

    private val headerExams: HeaderCell = HeaderCell(context).apply { setText("Exam Performance") }
    private val headerQuestions: HeaderCell = HeaderCell(context).apply { setText("Question Analytics") }

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(10f)
        })
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), dp(12f), dp(14f), dp(24f))
        }
        body.addView(chipGroupTimeRange, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))

        summaryCard.addView(tvSummary)
        summaryCard.addView(tvMostAttempted, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 10f))
        summaryCard.addView(barChartView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 180))
        body.addView(summaryCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))

        body.addView(tvEmpty, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(headerExams, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(rvExams, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))
        body.addView(headerQuestions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(rvQuestions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 14f))
        body.addView(btnOpenCrashlyticsConsole, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44))

        content.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val centerHost = FrameLayout(context)
        centerHost.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progress, LayoutHelper.createFrame(48, 48, Gravity.CENTER))

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
        val bgGray = Theme.getColor(Theme.key_windowBackgroundGray)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val divider = Theme.getColor(Theme.key_divider)
        val abBg = Theme.getColor(Theme.key_actionBarDefault)
        val abTitle = Theme.getColor(Theme.key_actionBarDefaultTitle)
        val abIcon = Theme.getColor(Theme.key_actionBarDefaultIcon)
        val abSelector = Theme.getColor(Theme.key_actionBarDefaultSelector)

        root.setBackgroundColor(bgGray)
        topBar.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(divider)
        tvTitle.setTextColor(abTitle)

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnRefresh).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }

        summaryCard.background = GradientDrawable().apply {
            cornerRadius = dp(12f).toFloat()
            setColor(bgWhite)
        }
        tvSummary.setTextColor(textPrimary)
        tvMostAttempted.setTextColor(textSecondary)
        tvEmpty.setTextColor(textSecondary)
        headerExams.setBackgroundColor(bgWhite)
        headerQuestions.setBackgroundColor(bgWhite)
        rvExams.setBackgroundColor(bgWhite)
        rvQuestions.setBackgroundColor(bgWhite)
        progress.setProgressColor(Theme.getColor(Theme.key_progressCircle))
    }
}

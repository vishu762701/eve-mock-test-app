package com.eve.app.ui.result

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
import com.eve.app.R
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramChip
import com.eve.app.designsystem.TelegramChipGroup
import com.eve.app.designsystem.TelegramTabsView
import com.eve.app.ui.common.TelegramBadgeView
import com.eve.app.ui.common.TelegramSlotTextView
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ResultActivity (Pass 3c).
 * Replaces activity_result.xml inflation using real upstream RecyclerListView,
 * RadialProgressView, Theme.getColor(Theme.key_*), Theme.createSelectorDrawable,
 * and AndroidUtilities.dp/getTypeface.
 */
class ResultTelegramViewTree(context: Context) {

    private fun dp(v: Float): Int = AndroidUtilities.dp(v)
    private fun mediumFont(): Typeface =
        AndroidUtilities.getTypeface("fonts/rmedium.ttf") ?: Typeface.DEFAULT_BOLD

    val tabLayoutResult: TelegramTabsView = TelegramTabsView(context).apply { id = R.id.tabLayoutResult }
    val scrollResultContent: ScrollView = ScrollView(context).apply { isFillViewport = true }
    val sectionReview: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12f), dp(8f), dp(12f), dp(12f))
        visibility = View.GONE
    }
    val scrollLeaderboard: ScrollView = ScrollView(context).apply {
        isFillViewport = true
        visibility = View.GONE
    }

    val root: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    // --- Top Telegram ActionBar ---
    private val topBar: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4f), 0, dp(4f), 0)
    }
    private val topDivider: View = View(context)

    val btnBack: ImageButton = createTopIcon(context, R.id.btnBack, R.drawable.ic_ab_back, "Back")
    val tvResultTitle: TextView = TextView(context).apply {
        id = R.id.tvResultTitle
        text = "Test Result"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val tvResultSubtitle: TextView = TextView(context).apply {
        id = R.id.tvResultSubtitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val btnShare: ImageButton = createTopIcon(context, R.id.btnShare, R.drawable.action_share, "Share result")
    val btnReattempt: ImageButton = createTopIcon(context, R.id.btnReattempt, R.drawable.ic_ab_other, "Reattempt")

    // --- Summary Hero ---
    val tvScore: TextView = TextView(context).apply {
        id = R.id.tvScore
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30f)
        typeface = mediumFont()
    }
    val tvScorePercentage: TextView = TextView(context).apply {
        id = R.id.tvScorePercentage
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
    }
    val tvStats: TextView = TextView(context).apply {
        id = R.id.tvStats
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvRank: TextView = TextView(context).apply {
        id = R.id.tvRank
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
    }
    val tvPercentile: TextView = TextView(context).apply {
        id = R.id.tvPercentile
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
    }
    val tvAccuracy: TextView = TextView(context).apply {
        id = R.id.tvAccuracy
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
    }

    // --- Statistics section ---
    val headerOverviewStatistics: LinearLayout = LinearLayout(context).apply {
        id = R.id.headerOverviewStatistics
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val rowOverviewStatisticsTiles: ResultStatisticsGrid = ResultStatisticsGrid(context).apply {
        id = R.id.rowOverviewStatisticsTiles
    }
    val tvCorrectCount: TelegramSlotTextView = TelegramSlotTextView(context).apply { id = R.id.tvCorrectCount }
    val tvWrongCount: TelegramSlotTextView = TelegramSlotTextView(context).apply { id = R.id.tvWrongCount }
    val tvUnattemptedCount: TelegramSlotTextView = TelegramSlotTextView(context).apply { id = R.id.tvUnattemptedCount }

    // --- Performance & Cutoff section ---
    val headerOverviewPerformance: LinearLayout = LinearLayout(context).apply {
        id = R.id.headerOverviewPerformance
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val cardPerformanceStanding: FrameLayout = FrameLayout(context).apply {
        id = R.id.cardPerformanceStanding
        isClickable = true
        isFocusable = true
    }
    val tvCutoffSelectedCategory: TextView = TextView(context).apply {
        id = R.id.tvCutoffSelectedCategory
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
    }
    val tvCutoffVerdict: TelegramBadgeView = TelegramBadgeView(context).apply {
        id = R.id.tvCutoffVerdict
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvCutoffValue: TextView = TextView(context).apply {
        id = R.id.tvCutoffValue
        text = "Cutoff unavailable"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvCutoffScore: TextView = TextView(context).apply {
        id = R.id.tvCutoffScore
        text = "Score: --"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvCutoffRelationship: TextView = TextView(context).apply {
        id = R.id.tvCutoffRelationship
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvHistoryBestAvg: TextView = TextView(context).apply {
        id = R.id.tvHistoryBestAvg
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvTopperAvg: TextView = TextView(context).apply {
        id = R.id.tvTopperAvg
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }

    // --- Analytics section ---
    val headerOverviewAnalytics: LinearLayout = LinearLayout(context).apply {
        id = R.id.headerOverviewAnalytics
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val cardOverviewAnalytics: FrameLayout = FrameLayout(context).apply {
        id = R.id.cardOverviewAnalytics
    }
    val gaugeTopicAccuracy: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.gaugeTopicAccuracy
        setSize(dp(44f))
        setNoProgress(false)
    }
    val tvAnalysisAccuracy: TextView = TextView(context).apply {
        id = R.id.tvAnalysisAccuracy
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        typeface = mediumFont()
    }
    val tvAnalysisAttempted: TextView = TextView(context).apply {
        id = R.id.tvAnalysisAttempted
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvAnalysisAvgTime: TextView = TextView(context).apply {
        id = R.id.tvAnalysisAvgTime
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }

    // --- Leaderboard summary ---
    val layoutLeaderboardChips: ResponsiveResultRow = ResponsiveResultRow(context).apply {
        id = R.id.layoutLeaderboardChips
    }
    val tvLeaderboardTabRank: TextView = TextView(context).apply {
        id = R.id.tvLeaderboardTabRank
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        typeface = mediumFont()
    }
    val tvLeaderboardTabSubtitle: TextView = TextView(context).apply {
        id = R.id.tvLeaderboardTabSubtitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
    }
    val tvLeaderboardScoreChip: TextView = TextView(context).apply {
        id = R.id.tvLeaderboardScoreChip
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
    }
    val tvLeaderboardPercentileChip: TextView = TextView(context).apply {
        id = R.id.tvLeaderboardPercentileChip
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
    }
    val btnOpenLeaderboard: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnOpenLeaderboard
        text = "View Full Leaderboard"
    }

    // --- Review section (isolated RecyclerListView + filter chips + palette) ---
    val chipGroupFilter: TelegramChipGroup = TelegramChipGroup(context).apply {
        id = R.id.chipGroupFilter
    }
    val chipCorrect: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipCorrect
        text = "Correct"
    }
    val chipWrong: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipWrong
        text = "Wrong"
    }
    val chipNotAttempted: TelegramChip = TelegramChip(context).apply {
        id = R.id.chipNotAttempted
        text = "Unattempted"
    }

    val rvResultPalette: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvResultPalette
        clipToPadding = false
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
    }

    val rvAnswers: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvAnswers
        isNestedScrollingEnabled = true
        clipToPadding = false
        setPadding(0, dp(4f), 0, dp(24f))
    }

    val layoutEmptyFilter: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutEmptyFilter
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
        visibility = View.GONE
    }
    val tvEmptyFilterTitle: TextView = TextView(context).apply {
        id = R.id.tvEmptyFilterTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
    }
    val tvEmptyFilterSub: TextView = TextView(context).apply {
        id = R.id.tvEmptyFilterSub
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        gravity = Gravity.CENTER
    }

    private val sectionCards = mutableListOf<View>()
    private val sectionHeaderTexts = mutableListOf<TextView>()
    private val primaryTexts = mutableListOf<TextView>()
    private val secondaryTexts = mutableListOf<TextView>()

    init {
        // 1. Top bar
        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), 0, dp(8f), 0)
        }
        titleCol.addView(tvResultTitle)
        titleCol.addView(tvResultSubtitle)
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(btnShare, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(btnReattempt, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        // 2. Scrollable Overview + Review Body
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14f), dp(12f), dp(14f), dp(24f))
        }

        // Hero Score Card
        val heroCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        }
        sectionCards.add(heroCard)
        val scoreRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
        }
        scoreRow.addView(tvScore)
        scoreRow.addView(tvScorePercentage, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 10f, 0f, 0f, 4f))
        heroCard.addView(scoreRow)
        heroCard.addView(tvStats, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 10f))
        val metricsRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        metricsRow.addView(tvRank, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metricsRow.addView(tvPercentile, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        metricsRow.addView(tvAccuracy, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heroCard.addView(metricsRow)
        body.addView(heroCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))

        // Statistics section
        addSectionHeader(context, headerOverviewStatistics, "Statistics")
        body.addView(headerOverviewStatistics, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 2f, 4f, 2f, 6f))
        rowOverviewStatisticsTiles.addView(wrapStatTile(context, tvCorrectCount, "Correct"))
        rowOverviewStatisticsTiles.addView(wrapStatTile(context, tvWrongCount, "Wrong"))
        rowOverviewStatisticsTiles.addView(wrapStatTile(context, tvUnattemptedCount, "Unattempted"))
        body.addView(rowOverviewStatisticsTiles, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))

        // Performance & Cutoff section
        addSectionHeader(context, headerOverviewPerformance, "Performance & Cutoffs")
        body.addView(headerOverviewPerformance, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 2f, 4f, 2f, 6f))
        val perfInner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        perfInner.addView(tvCutoffSelectedCategory)
        perfInner.addView(tvHistoryBestAvg, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 2f))
        perfInner.addView(tvTopperAvg)
        perfInner.addView(tvCutoffVerdict, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 8f, 0f, 4f))
        val cutoffValues = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        cutoffValues.addView(tvCutoffValue, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        cutoffValues.addView(tvCutoffScore, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        perfInner.addView(cutoffValues)
        perfInner.addView(tvCutoffRelationship, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 8f, 0f, 0f))
        cardPerformanceStanding.addView(perfInner)
        sectionCards.add(cardPerformanceStanding)
        body.addView(cardPerformanceStanding, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))

        // Analytics section
        addSectionHeader(context, headerOverviewAnalytics, "Analytics")
        body.addView(headerOverviewAnalytics, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 2f, 4f, 2f, 6f))
        val anaInner = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        anaInner.addView(gaugeTopicAccuracy, LayoutHelper.createLinear(52, 52, 0f, 0f, 14f, 0f))
        val anaCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        anaCol.addView(tvAnalysisAccuracy)
        anaCol.addView(tvAnalysisAttempted, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 3f, 0f, 2f))
        anaCol.addView(tvAnalysisAvgTime)
        anaInner.addView(anaCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        cardOverviewAnalytics.addView(anaInner)
        sectionCards.add(cardOverviewAnalytics)
        body.addView(cardOverviewAnalytics, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))

        // Leaderboard card
        val lbCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        sectionCards.add(lbCard)
        lbCard.addView(tvLeaderboardTabRank)
        lbCard.addView(tvLeaderboardTabSubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 2f, 0f, 8f))
        layoutLeaderboardChips.addView(tvLeaderboardScoreChip)
        layoutLeaderboardChips.addView(tvLeaderboardPercentileChip)
        lbCard.addView(layoutLeaderboardChips, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 10f))
        lbCard.addView(btnOpenLeaderboard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42))
        scrollLeaderboard.addView(lbCard, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat()))

        // Review filter chips + palette + isolated answers list
        chipGroupFilter.addView(chipCorrect)
        chipGroupFilter.addView(chipWrong)
        chipGroupFilter.addView(chipNotAttempted)
        sectionReview.addView(rvResultPalette, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52, 0f, 0f, 0f, 8f))
        sectionReview.addView(chipGroupFilter, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 8f))

        layoutEmptyFilter.addView(tvEmptyFilterTitle)
        layoutEmptyFilter.addView(tvEmptyFilterSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 0f))
        sectionReview.addView(layoutEmptyFilter)
        sectionReview.addView(rvAnswers, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        scrollResultContent.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        root.addView(tabLayoutResult, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f)))
        root.addView(scrollResultContent, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(sectionReview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(scrollLeaderboard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        primaryTexts.addAll(listOf(tvScore, tvRank, tvPercentile, tvAccuracy, tvCutoffSelectedCategory, tvAnalysisAccuracy, tvLeaderboardTabRank, tvLeaderboardScoreChip, tvLeaderboardPercentileChip, tvEmptyFilterTitle))
        secondaryTexts.addAll(listOf(tvResultSubtitle, tvStats, tvHistoryBestAvg, tvTopperAvg, tvCutoffValue, tvCutoffScore, tvCutoffRelationship, tvAnalysisAttempted, tvAnalysisAvgTime, tvLeaderboardTabSubtitle, tvEmptyFilterSub))

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

    private fun addSectionHeader(context: Context, host: LinearLayout, title: String) {
        val tv = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            typeface = mediumFont()
        }
        sectionHeaderTexts.add(tv)
        host.addView(tv)
    }

    private fun wrapStatTile(context: Context, slot: TelegramSlotTextView, label: String): LinearLayout {
        val tile = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10f), dp(12f), dp(10f), dp(12f))
        }
        sectionCards.add(tile)
        val lbl = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
            gravity = Gravity.CENTER
        }
        secondaryTexts.add(lbl)
        tile.addView(slot)
        tile.addView(lbl, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 0f))
        return tile
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val bgGray = Theme.getColor(Theme.key_windowBackgroundGray)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val headerBlue = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader)
        val accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4)
        val divider = Theme.getColor(Theme.key_divider)
        val abBg = Theme.getColor(Theme.key_actionBarDefault)
        val abTitle = Theme.getColor(Theme.key_actionBarDefaultTitle)
        val abIcon = Theme.getColor(Theme.key_actionBarDefaultIcon)
        val abSelector = Theme.getColor(Theme.key_actionBarDefaultSelector)

        root.setBackgroundColor(bgGray)
        topBar.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(divider)
        tvResultTitle.setTextColor(abTitle)

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnShare, btnReattempt).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }

        sectionCards.forEach { card ->
            card.background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(bgWhite)
            }
        }
        sectionHeaderTexts.forEach { it.setTextColor(headerBlue) }
        primaryTexts.forEach { it.setTextColor(textPrimary) }
        secondaryTexts.forEach { it.setTextColor(textSecondary) }
        tvScorePercentage.setTextColor(accent)
        gaugeTopicAccuracy.setProgressColor(accent)
    }
}

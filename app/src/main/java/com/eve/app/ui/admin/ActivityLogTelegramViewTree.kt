package com.eve.app.ui.admin

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.ui.admin.tg.TelegramSegmentedTabs
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ActivityLogActivity (Pass 4b).
 * Replaces activity_activity_log.xml inflation using upstream RecyclerListView,
 * RadialProgressView, Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class ActivityLogTelegramViewTree(context: Context) {

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

    val actionBar: TelegramActionBar = TelegramActionBar(context).apply {
        id = R.id.actionBar
    }

    val tabsRange: TelegramSegmentedTabs = TelegramSegmentedTabs(context).apply {
        id = R.id.tabsRange
    }

    val tvActiveFilters: TextView = TextView(context).apply {
        id = R.id.tvActiveFilters
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        setPadding(dp(16f), dp(6f), dp(16f), dp(6f))
        visibility = View.GONE
    }

    val rvAuditLogs: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvAuditLogs
        clipToPadding = false
        setPadding(0, dp(4f), 0, dp(24f))
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(36f))
        visibility = View.GONE
    }

    val emptyContainer: LinearLayout = LinearLayout(context).apply {
        id = R.id.emptyContainer
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
        visibility = View.GONE
    }

    val lottieEmpty: LottieAnimationView = LottieAnimationView(context).apply {
        id = R.id.lottieEmpty
    }

    val tvEmptyTitle: TextView = TextView(context).apply {
        id = R.id.tvEmptyTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
    }

    val tvEmptySubtitle: TextView = TextView(context).apply {
        id = R.id.tvEmptySubtitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
    }

    private val divider: View = View(context)

    init {
        root.addView(actionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        root.addView(tabsRange, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12f, 8f, 12f, 6f))
        root.addView(tvActiveFilters, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val centerHost = FrameLayout(context)
        centerHost.addView(rvAuditLogs, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBar, LayoutHelper.createFrame(48, 48, Gravity.CENTER))

        emptyContainer.addView(lottieEmpty, LayoutHelper.createLinear(112, 112, Gravity.CENTER_HORIZONTAL))
        emptyContainer.addView(tvEmptyTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 12f, 0f, 4f))
        emptyContainer.addView(tvEmptySubtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        centerHost.addView(emptyContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        root.addView(centerHost, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        applyTelegramTheme()
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        root.setBackgroundColor(bgWhite)
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider))
        tvActiveFilters.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4))
        rvAuditLogs.setBackgroundColor(bgWhite)
        progressBar.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        tvEmptyTitle.setTextColor(textPrimary)
        tvEmptySubtitle.setTextColor(textSecondary)
    }
}

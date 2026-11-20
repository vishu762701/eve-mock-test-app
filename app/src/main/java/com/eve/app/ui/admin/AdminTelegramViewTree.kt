package com.eve.app.ui.admin

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.eve.app.R
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.designsystem.TelegramTabsView
import com.eve.app.ui.admin.tg.HeaderCell
import com.eve.app.ui.admin.tg.ShadowSectionCell
import com.eve.app.ui.admin.tg.TextCell
import com.eve.app.ui.admin.tg.TextCheckCell
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for AdminActivity (Pass 4d).
 * Replaces activity_admin.xml inflation using upstream RecyclerListView,
 * RadialProgressView, Theme.getColor(Theme.key_*), and AndroidUtilities,
 * preserving the 5-section LinearLayout inside scrollAdminHub expected by AdminDashboardPager.
 */
class AdminTelegramViewTree(context: Context) {

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

    val tabLayoutAdmin: TelegramTabsView = TelegramTabsView(context).apply {
        id = R.id.tabLayoutAdmin
        visibility = View.GONE
    }

    // Admin profile identity header
    val ivAdminAvatar: ImageView = ImageView(context).apply {
        id = R.id.ivAdminAvatar
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    val tvAdminName: TextView = TextView(context).apply {
        id = R.id.tvAdminName
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val tvAdminEmail: TextView = TextView(context).apply {
        id = R.id.tvAdminEmail
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val tvAdminRole: TextView = TextView(context).apply {
        id = R.id.tvAdminRole
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        typeface = mediumFont()
    }

    // Hub host partitioned into 5 sections by AdminDashboardPager
    val scrollAdminHub: FrameLayout = FrameLayout(context).apply {
        id = R.id.scrollAdminHub
    }

    val rvAdminSearch: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvAdminSearch
        visibility = View.GONE
    }

    val progressBarAdmin: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarAdmin
        setSize(dp(36f))
        visibility = View.GONE
    }

    // Section 1: Content
    val headerContent: HeaderCell = HeaderCell(context).apply { id = R.id.headerContent; setText("Content") }
    val rowExams: TextCell = createCell(context, R.id.rowExams, "Manage Exams")
    val rowQuestions: TextCell = createCell(context, R.id.rowQuestions, "Manage Questions")
    val rowGeneratedTests: TextCell = createCell(context, R.id.rowGeneratedTests, "Generated Tests")
    val rowSyllabus: TextCell = createCell(context, R.id.rowSyllabus, "Manage Syllabus")
    val rowPolls: TextCell = createCell(context, R.id.rowPolls, "Manage Polls")
    val rowHomeBanner: TextCell = createCell(context, R.id.rowHomeBanner, "Home Banner")
    val rowFloatingLink: TextCell = createCell(context, R.id.rowFloatingLink, "Floating Link")
    val rowAnimatedEmoji: TextCell = createCell(context, R.id.rowAnimatedEmoji, "Emoji Studio")
    val rowEditAbout: TextCell = createCell(context, R.id.rowEditAbout, "Edit About")

    // Section 2: People
    val headerPeople: HeaderCell = HeaderCell(context).apply { id = R.id.headerPeople; setText("People") }
    val rowUsers: TextCell = createCell(context, R.id.rowUsers, "Manage Users")
    val rowAdmins: TextCell = createCell(context, R.id.rowAdmins, "Admins & Rights")
    val rowPremium: TextCell = createCell(context, R.id.rowPremium, "Manage Premium")

    // Section 3: Communication
    val headerCommunication: HeaderCell = HeaderCell(context).apply { id = R.id.headerCommunication; setText("Communication") }
    val rowBroadcast: TextCell = createCell(context, R.id.rowBroadcast, "Send Notification")
    val rowFeedback: TextCell = createCell(context, R.id.rowFeedback, "Feedback Messages")
    val rowCreateFeedbackPost: TextCell = createCell(context, R.id.rowCreateFeedbackPost, "Create Feedback Post")
    val btnManageFeedbackPosts: TextCell = createCell(context, R.id.btnManageFeedbackPosts, "Manage Feedback Posts")
    val rowFlaggedQuestions: TextCell = createCell(context, R.id.rowFlaggedQuestions, "Flagged Questions")

    // Section 4: Insights
    val headerInsights: HeaderCell = HeaderCell(context).apply { id = R.id.headerInsights; setText("Insights") }
    val rowAnalytics: TextCell = createCell(context, R.id.rowAnalytics, "Analytics")
    val rowApiUsage: TextCell = createCell(context, R.id.rowApiUsage, "API Usage")
    val rowRecentActions: TextCell = createCell(context, R.id.rowRecentActions, "Activity Log")
    val rowExportData: TextCell = createCell(context, R.id.rowExportData, "Export Data")

    // Section 5: System
    val headerSystem: HeaderCell = HeaderCell(context).apply { id = R.id.headerSystem; setText("System") }
    val cellMaintenance: TextCheckCell = TextCheckCell(context).apply { id = R.id.cellMaintenance }
    val rowAppConfig: TextCell = createCell(context, R.id.rowAppConfig, "App Config")
    val rowSystemMonitor: TextCell = createCell(context, R.id.rowSystemMonitor, "System Monitor")
    val rowVersionStatus: TextCell = createCell(context, R.id.rowVersionStatus, "Version Status")

    private val headerCard: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
    }

    init {
        root.addView(telegramActionBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val infoCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), 0, 0, 0)
        }
        infoCol.addView(tvAdminName)
        infoCol.addView(tvAdminEmail)
        infoCol.addView(tvAdminRole)
        headerCard.addView(ivAdminAvatar, LayoutHelper.createLinear(48, 48))
        headerCard.addView(infoCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(headerCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(tabLayoutAdmin, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val sectionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        listOf(
            headerContent, rowExams, rowQuestions, rowGeneratedTests, rowSyllabus, rowPolls,
            rowHomeBanner, rowFloatingLink, rowAnimatedEmoji, rowEditAbout, ShadowSectionCell(context),
            headerPeople, rowUsers, rowAdmins, rowPremium, ShadowSectionCell(context),
            headerCommunication, rowBroadcast, rowFeedback, rowCreateFeedbackPost, btnManageFeedbackPosts,
            rowFlaggedQuestions, ShadowSectionCell(context),
            headerInsights, rowAnalytics, rowApiUsage, rowRecentActions, rowExportData, ShadowSectionCell(context),
            headerSystem, cellMaintenance, rowAppConfig, rowSystemMonitor, rowVersionStatus
        ).forEach { view ->
            sectionsContainer.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        }
        scrollAdminHub.addView(sectionsContainer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        val centerHost = FrameLayout(context)
        centerHost.addView(scrollAdminHub, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(rvAdminSearch, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBarAdmin, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        root.addView(centerHost, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        applyTelegramTheme()
    }

    private fun createCell(context: Context, viewId: Int, label: String): TextCell {
        return TextCell(context).apply {
            id = viewId
            setText(label, true)
        }
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4)

        root.setBackgroundColor(bgWhite)
        headerCard.setBackgroundColor(bgWhite)
        tvAdminName.setTextColor(textPrimary)
        tvAdminEmail.setTextColor(textSecondary)
        tvAdminRole.setTextColor(accent)
        rvAdminSearch.setBackgroundColor(bgWhite)
        progressBarAdmin.setProgressColor(Theme.getColor(Theme.key_progressCircle))
    }
}

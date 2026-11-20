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
import android.widget.ScrollView
import android.widget.TextView
import com.eve.app.R
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramEditText
import com.eve.app.designsystem.TelegramOutlineEditText
import com.eve.app.ui.common.ErrorStateView
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for SendNotificationActivity (Pass 4b).
 * Replaces activity_send_notification.xml inflation using upstream RecyclerListView,
 * RadialProgressView, HeaderCell, Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class SendNotificationTelegramViewTree(context: Context) {

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

    private val topBarHost: FrameLayout = FrameLayout(context)
    private val topDivider: View = View(context)

    val normalTopBar: LinearLayout = LinearLayout(context).apply {
        id = R.id.normalTopBar
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4f), 0, dp(4f), 0)
    }

    val btnBack: ImageButton = createTopIcon(context, R.id.btnBack, R.drawable.ic_ab_back, "Back")
    private val tvTitle: TextView = TextView(context).apply {
        text = "Send Notification"
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

    val selectionTopBar: LinearLayout = LinearLayout(context).apply {
        id = R.id.selectionTopBar
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = View.GONE
        setPadding(dp(4f), 0, dp(4f), 0)
    }

    val btnCancelSelection: ImageButton = createTopIcon(context, R.id.btnCancelSelection, R.drawable.ic_ab_close, "Cancel selection")
    val tvSelectedCount: TextView = TextView(context).apply {
        id = R.id.tvSelectedCount
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        typeface = mediumFont()
    }
    val btnSelectAll: TextView = TextView(context).apply {
        id = R.id.btnSelectAll
        text = "Select All"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
        setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
        isClickable = true
        isFocusable = true
    }
    val btnDeleteSelected: ImageButton = createTopIcon(context, R.id.btnDeleteSelected, R.drawable.ic_delete, "Delete selected")

    val scrollNotificationContent: ScrollView = ScrollView(context).apply {
        id = R.id.scrollNotificationContent
        isFillViewport = true
    }

    val tilTitle: TelegramOutlineEditText = TelegramOutlineEditText(context).apply {
        id = R.id.tilTitle
    }
    val etTitle: TelegramEditText = TelegramEditText(context).apply {
        id = R.id.etTitle
        hint = "Notification title"
    }

    val tilMessage: TelegramOutlineEditText = TelegramOutlineEditText(context).apply {
        id = R.id.tilMessage
    }
    val etMessage: TelegramEditText = TelegramEditText(context).apply {
        id = R.id.etMessage
        hint = "Notification message"
        minLines = 3
    }

    val btnSend: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnSend
        text = "Send Notification"
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(32f))
        visibility = View.GONE
    }

    val compactErrorView: ErrorStateView = ErrorStateView(context).apply {
        id = R.id.compactErrorView
        visibility = View.GONE
    }

    val progressBarSent: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBarSent
        setSize(dp(28f))
        visibility = View.GONE
    }

    val tvNoSentBroadcasts: TextView = TextView(context).apply {
        id = R.id.tvNoSentBroadcasts
        text = "No broadcasts sent yet."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
        setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
        visibility = View.GONE
    }

    val rvSentBroadcasts: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvSentBroadcasts
        isNestedScrollingEnabled = false
    }

    private val headerCompose: HeaderCell = HeaderCell(context).apply { setText("Compose Broadcast") }
    private val headerHistory: HeaderCell = HeaderCell(context).apply { setText("Sent Broadcasts") }

    init {
        normalTopBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        normalTopBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        normalTopBar.addView(progressBarTop, LayoutHelper.createLinear(32, 32, 0f, 0f, 12f, 0f))

        selectionTopBar.addView(btnCancelSelection, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        selectionTopBar.addView(tvSelectedCount, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8f) })
        selectionTopBar.addView(btnSelectAll, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
        selectionTopBar.addView(btnDeleteSelected, LinearLayout.LayoutParams(dp(48f), dp(48f)))

        topBarHost.addView(normalTopBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        topBarHost.addView(selectionTopBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        root.addView(topBarHost, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(8f), dp(16f), dp(24f))
        }
        body.addView(headerCompose)
        tilTitle.addView(etTitle, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 10f))
        tilMessage.addView(etMessage, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        body.addView(tilMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 12f))
        body.addView(btnSend, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 0f, 0f, 0f, 8f))
        body.addView(progressBar, LayoutHelper.createLinear(36, 36, Gravity.CENTER_HORIZONTAL))
        body.addView(compactErrorView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 6f))

        body.addView(headerHistory, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 10f, 0f, 4f))
        body.addView(progressBarSent, LayoutHelper.createLinear(32, 32, Gravity.CENTER_HORIZONTAL))
        body.addView(tvNoSentBroadcasts, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        body.addView(rvSentBroadcasts, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        scrollNotificationContent.addView(body, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))
        root.addView(scrollNotificationContent, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

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
        topBarHost.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(Theme.getColor(Theme.key_divider))
        tvTitle.setTextColor(abTitle)
        tvSelectedCount.setTextColor(abTitle)
        btnSelectAll.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4))

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnCancelSelection).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }
        btnDeleteSelected.imageTintList = ColorStateList.valueOf(Theme.getColor(Theme.key_text_RedRegular))
        btnDeleteSelected.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)

        listOf(progressBarTop, progressBar, progressBarSent).forEach {
            it.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        }
        tvNoSentBroadcasts.setTextColor(textSecondary)
        rvSentBroadcasts.setBackgroundColor(bgWhite)
    }
}

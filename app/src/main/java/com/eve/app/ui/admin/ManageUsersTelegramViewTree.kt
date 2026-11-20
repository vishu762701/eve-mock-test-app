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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.eve.app.R
import com.eve.app.designsystem.TelegramButton
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ManageUsersActivity (Pass 4b).
 * Replaces activity_manage_users.xml inflation using upstream RecyclerListView,
 * RadialProgressView, Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class ManageUsersTelegramViewTree(context: Context) {

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
        text = "Manage Users"
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
    val btnRefresh: ImageButton = createTopIcon(context, R.id.btnRefresh, R.drawable.ic_refresh, "Refresh")

    val etSearchUser: EditText = EditText(context).apply {
        id = R.id.etSearchUser
        hint = "Search by name, email, or username..."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        setSingleLine(true)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = InputType.TYPE_CLASS_TEXT
        setPadding(dp(14f), dp(10f), dp(14f), dp(10f))
    }

    val swipeRefresh: SwipeRefreshLayout = SwipeRefreshLayout(context).apply {
        id = R.id.swipeRefresh
    }

    val rvUsers: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvUsers
        clipToPadding = false
        setPadding(0, dp(4f), 0, dp(16f))
    }

    val progressBar: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressBar
        setSize(dp(36f))
        visibility = View.GONE
    }

    val emptyGroup: LinearLayout = LinearLayout(context).apply {
        id = R.id.emptyGroup
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
        visibility = View.GONE
    }

    val tvEmptyTitle: TextView = TextView(context).apply {
        id = R.id.tvEmptyTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
    }

    val tvEmptyMessage: TextView = TextView(context).apply {
        id = R.id.tvEmptyMessage
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
    }

    val btnRetryUsers: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnRetryUsers
        text = "Retry"
    }

    // Pagination footer
    val layoutPagination: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutPagination
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12f), dp(8f), dp(12f), dp(10f))
        visibility = View.GONE
    }

    val tvPageSummary: TextView = TextView(context).apply {
        id = R.id.tvPageSummary
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        gravity = Gravity.CENTER
    }

    val layoutPageControls: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutPageControls
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val btnPagePrevious: TextView = TextView(context).apply {
        id = R.id.btnPagePrevious
        text = "Prev"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
        setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
        isClickable = true
        isFocusable = true
    }

    val layoutPageNumbers: TelegramPaginationStrip = TelegramPaginationStrip(context).apply {
        id = R.id.layoutPageNumbers
    }

    val btnPageNext: TextView = TextView(context).apply {
        id = R.id.btnPageNext
        text = "Next"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
        setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
        isClickable = true
        isFocusable = true
    }

    private val footerDivider: View = View(context)

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        topBar.addView(progressBarTop, LayoutHelper.createLinear(32, 32, 0f, 0f, 4f, 0f))
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        root.addView(etSearchUser, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 14f, 10f, 14f, 8f))

        val centerHost = FrameLayout(context)
        centerHost.addView(rvUsers, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerHost.addView(progressBar, LayoutHelper.createFrame(48, 48, Gravity.CENTER))

        emptyGroup.addView(tvEmptyTitle)
        emptyGroup.addView(tvEmptyMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 14f))
        emptyGroup.addView(btnRetryUsers, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 42, Gravity.CENTER_HORIZONTAL))
        centerHost.addView(emptyGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        swipeRefresh.addView(centerHost, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        root.addView(swipeRefresh, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(footerDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        layoutPageControls.addView(btnPagePrevious)
        layoutPageControls.addView(layoutPageNumbers, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        layoutPageControls.addView(btnPageNext)
        layoutPagination.addView(tvPageSummary, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 4f))
        layoutPagination.addView(layoutPageControls, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(layoutPagination, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

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
        val accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4)
        val divider = Theme.getColor(Theme.key_divider)
        val abBg = Theme.getColor(Theme.key_actionBarDefault)
        val abTitle = Theme.getColor(Theme.key_actionBarDefaultTitle)
        val abIcon = Theme.getColor(Theme.key_actionBarDefaultIcon)
        val abSelector = Theme.getColor(Theme.key_actionBarDefaultSelector)

        root.setBackgroundColor(bgWhite)
        topBar.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(divider)
        footerDivider.setBackgroundColor(divider)
        tvTitle.setTextColor(abTitle)

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnRefresh).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }

        etSearchUser.background = GradientDrawable().apply {
            cornerRadius = dp(10f).toFloat()
            setColor(bgGray)
        }
        etSearchUser.setTextColor(textPrimary)
        etSearchUser.setHintTextColor(textSecondary)

        rvUsers.setBackgroundColor(bgWhite)
        progressBarTop.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        progressBar.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        tvEmptyTitle.setTextColor(textPrimary)
        tvEmptyMessage.setTextColor(textSecondary)
        layoutPagination.setBackgroundColor(bgWhite)
        tvPageSummary.setTextColor(textSecondary)
        btnPagePrevious.setTextColor(accent)
        btnPageNext.setTextColor(accent)
    }
}

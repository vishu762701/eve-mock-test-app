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
import com.eve.app.designsystem.TelegramEditText
import com.eve.app.designsystem.TelegramOutlineEditText
import com.eve.app.designsystem.TelegramSwitch
import com.eve.app.designsystem.TelegramTabsView
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for ManagePremiumActivity (Pass 4c).
 * Replaces activity_manage_premium.xml inflation using upstream RecyclerListView,
 * RadialProgressView, HeaderCell, ShadowSectionCell, Theme.getColor(Theme.key_*),
 * and AndroidUtilities.
 */
class ManagePremiumTelegramViewTree(context: Context) {

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
        text = "Manage Premium"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    val btnRefresh: ImageButton = createTopIcon(context, R.id.btnRefresh, R.drawable.ic_refresh, "Refresh")

    val tabLayout: TelegramTabsView = TelegramTabsView(context).apply {
        id = R.id.tabLayout
    }

    val progressAdmin: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.progressAdmin
        setSize(dp(36f))
        visibility = View.GONE
    }

    // Tab 0: Settings
    val containerSettings: NestedScrollView = NestedScrollView(context).apply {
        id = R.id.containerSettings
        isFillViewport = true
    }

    val switchEnablePremium: TelegramSwitch = TelegramSwitch(context).apply { id = R.id.switchEnablePremium }
    val switchLifetime: TelegramSwitch = TelegramSwitch(context).apply { id = R.id.switchLifetime }
    val switchUpiEnabled: TelegramSwitch = TelegramSwitch(context).apply { id = R.id.switchUpiEnabled }
    val switchQrEnabled: TelegramSwitch = TelegramSwitch(context).apply { id = R.id.switchQrEnabled }

    val etPlanName: TelegramEditText = TelegramEditText(context).apply { id = R.id.etPlanName; hint = "Plan name" }
    val etPriceInr: TelegramEditText = TelegramEditText(context).apply { id = R.id.etPriceInr; hint = "Price (INR)"; inputType = InputType.TYPE_CLASS_NUMBER }
    val tilDurationDays: TelegramOutlineEditText = TelegramOutlineEditText(context).apply { id = R.id.tilDurationDays }
    val etDurationDays: TelegramEditText = TelegramEditText(context).apply { id = R.id.etDurationDays; hint = "Duration (days)"; inputType = InputType.TYPE_CLASS_NUMBER }
    val etDescription: TelegramEditText = TelegramEditText(context).apply { id = R.id.etDescription; hint = "Description"; minLines = 2 }
    val etBenefits: TelegramEditText = TelegramEditText(context).apply { id = R.id.etBenefits; hint = "Benefits (one per line)"; minLines = 3 }
    val etMerchantVpa: TelegramEditText = TelegramEditText(context).apply { id = R.id.etMerchantVpa; hint = "Merchant UPI VPA" }
    val etSessionExpiry: TelegramEditText = TelegramEditText(context).apply { id = R.id.etSessionExpiry; hint = "Session expiry (minutes)"; inputType = InputType.TYPE_CLASS_NUMBER }
    val btnSaveSettings: TelegramButton = TelegramButton(context).apply { id = R.id.btnSaveSettings; text = "Save Premium Settings" }

    // Tab 1: Premium Users
    val containerUsers: LinearLayout = LinearLayout(context).apply {
        id = R.id.containerUsers
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }
    val btnGrantManual: TelegramButton = TelegramButton(context).apply { id = R.id.btnGrantManual; text = "Grant Premium Manually" }
    val rvPremiumUsers: RecyclerListView = RecyclerListView(context).apply { id = R.id.rvPremiumUsers }
    val tvEmptyUsers: TextView = TextView(context).apply {
        id = R.id.tvEmptyUsers
        text = "No premium users yet."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        gravity = Gravity.CENTER
        visibility = View.GONE
    }

    // Tab 2: Transactions
    val containerTransactions: FrameLayout = FrameLayout(context).apply {
        id = R.id.containerTransactions
        visibility = View.GONE
    }
    val rvTransactions: RecyclerListView = RecyclerListView(context).apply { id = R.id.rvTransactions }
    val tvEmptyTransactions: TextView = TextView(context).apply {
        id = R.id.tvEmptyTransactions
        text = "No transactions yet."
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        gravity = Gravity.CENTER
        visibility = View.GONE
    }

    private val switchLabels = mutableListOf<TextView>()

    init {
        topBar.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        topBar.addView(tvTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(10f) })
        topBar.addView(btnRefresh, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        root.addView(tabLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48))

        // Settings tab content
        val settingsCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4f), 0, dp(24f))
        }
        settingsCol.addView(HeaderCell(context).apply { setText("Premium Plan") })
        settingsCol.addView(createSwitchRow(context, "Enable Premium", switchEnablePremium))
        settingsCol.addView(createSwitchRow(context, "Lifetime Access", switchLifetime))
        settingsCol.addView(etPlanName, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 6f))
        settingsCol.addView(etPriceInr, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 6f))
        tilDurationDays.addView(etDurationDays, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        settingsCol.addView(tilDurationDays, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 6f))
        settingsCol.addView(etDescription, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 6f))
        settingsCol.addView(etBenefits, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 10f))
        settingsCol.addView(ShadowSectionCell(context))
        settingsCol.addView(HeaderCell(context).apply { setText("Payment Settings") })
        settingsCol.addView(createSwitchRow(context, "Enable UPI Intent", switchUpiEnabled))
        settingsCol.addView(createSwitchRow(context, "Enable QR Payment", switchQrEnabled))
        settingsCol.addView(etMerchantVpa, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 6f))
        settingsCol.addView(etSessionExpiry, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 6f, 16f, 12f))
        settingsCol.addView(btnSaveSettings, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 46, 16f, 6f, 16f, 12f))
        containerSettings.addView(settingsCol, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        // Users tab content
        containerUsers.addView(btnGrantManual, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 16f, 12f, 16f, 8f))
        val usersFrame = FrameLayout(context)
        usersFrame.addView(rvPremiumUsers, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        usersFrame.addView(tvEmptyUsers, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER))
        containerUsers.addView(usersFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // Transactions tab content
        containerTransactions.addView(rvTransactions, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        containerTransactions.addView(tvEmptyTransactions, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER))

        val bodyHost = FrameLayout(context)
        bodyHost.addView(containerSettings, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        bodyHost.addView(containerUsers, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        bodyHost.addView(containerTransactions, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        bodyHost.addView(progressAdmin, LayoutHelper.createFrame(48, 48, Gravity.CENTER))

        root.addView(bodyHost, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
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

    private fun createSwitchRow(context: Context, label: String, sw: TelegramSwitch): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
        }
        val tv = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        }
        switchLabels.add(tv)
        row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(sw)
        return row
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
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
        switchLabels.forEach { it.setTextColor(textPrimary) }
        progressAdmin.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        rvPremiumUsers.setBackgroundColor(bgWhite)
        rvTransactions.setBackgroundColor(bgWhite)
        tvEmptyUsers.setTextColor(textSecondary)
        tvEmptyTransactions.setTextColor(textSecondary)
    }
}

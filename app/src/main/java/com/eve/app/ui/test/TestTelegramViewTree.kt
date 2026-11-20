package com.eve.app.ui.test

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.designsystem.TelegramButton
import com.eve.app.ui.common.CircularTimerView
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for TestActivity (Pass 3c).
 * Replaces activity_test.xml inflation using real upstream RadialProgressView,
 * RecyclerListView, LayoutHelper, Theme.getColor(Theme.key_*), and AndroidUtilities.
 */
class TestTelegramViewTree(context: Context) {

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
        setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
    }

    private val topDivider: View = View(context)

    val tvTestTitle: TextView = TextView(context).apply {
        id = R.id.tvTestTitle
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val circularTimerView: CircularTimerView = CircularTimerView(context).apply {
        id = R.id.circularTimerView
    }

    val tvTimer: TextView = TextView(context).apply {
        id = R.id.tvTimer
        text = "00:00"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
    }

    val tvOfflineBanner: TextView = TextView(context).apply {
        id = R.id.tvOfflineBanner
        text = "Offline mode — answers saved locally"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
        visibility = View.GONE
    }

    val rvQuestionPalette: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvQuestionPalette
        clipToPadding = false
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
    }

    private val paletteDivider: View = View(context)

    val viewPager: ViewPager2 = ViewPager2(context).apply {
        id = R.id.viewPager
    }

    val loadingTest: RadialProgressView = RadialProgressView(context).apply {
        id = R.id.loadingTest
        setSize(dp(36f))
        visibility = View.GONE
    }

    val progressGroup: LinearLayout = LinearLayout(context).apply {
        id = R.id.progressGroup
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        visibility = View.GONE
    }

    val messageGroup: LinearLayout = LinearLayout(context).apply {
        id = R.id.messageGroup
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
        visibility = View.GONE
    }

    val ivMessageIcon: LottieAnimationView = LottieAnimationView(context).apply {
        id = R.id.ivMessageIcon
    }

    val tvMessage: TextView = TextView(context).apply {
        id = R.id.tvMessage
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
    }

    val tvMessageSub: TextView = TextView(context).apply {
        id = R.id.tvMessageSub
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        gravity = Gravity.CENTER
    }

    val btnRetry: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnRetry
        text = "Retry"
    }

    // 2x2 action button grid
    val layoutBottomBar: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutBottomBar
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12f), dp(8f), dp(12f), dp(12f))
    }

    private val bottomDivider: View = View(context)

    val btnMarkReview: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnMarkReview
        text = "Mark Review"
    }

    val btnClear: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnClear
        text = "Clear"
    }

    val btnPrev: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnPrev
        text = "Previous"
    }

    val btnNext: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnNext
        text = "Next"
    }

    init {
        // 1. Top bar
        topBar.addView(tvTestTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        topBar.addView(circularTimerView, LayoutHelper.createLinear(28, 28, 8f, 0f, 6f, 0f))
        topBar.addView(tvTimer, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))

        root.addView(topBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        root.addView(tvOfflineBanner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(rvQuestionPalette, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 52))
        root.addView(paletteDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))

        // 2. Center content (ViewPager2 + loading/progress/message overlays)
        val centerFrame = FrameLayout(context)
        centerFrame.addView(viewPager, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        centerFrame.addView(loadingTest, LayoutHelper.createFrame(48, 48, Gravity.CENTER))

        progressGroup.addView(RadialProgressView(context), LayoutHelper.createLinear(44, 44))
        centerFrame.addView(progressGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        messageGroup.addView(ivMessageIcon, LayoutHelper.createLinear(112, 112, Gravity.CENTER_HORIZONTAL))
        messageGroup.addView(tvMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 12f, 0f, 4f))
        messageGroup.addView(tvMessageSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 14f))
        messageGroup.addView(btnRetry, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44, Gravity.CENTER_HORIZONTAL))
        centerFrame.addView(messageGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        root.addView(centerFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        // 3. Bottom 2x2 action grid
        root.addView(bottomDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        val row1 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(btnMarkReview, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginEnd = dp(6f) })
        row1.addView(btnClear, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginStart = dp(6f) })

        val row2 = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(btnPrev, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginEnd = dp(6f) })
        row2.addView(btnNext, LinearLayout.LayoutParams(0, dp(42f), 1f).apply { marginStart = dp(6f) })

        layoutBottomBar.addView(row1, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 8f))
        layoutBottomBar.addView(row2, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        root.addView(layoutBottomBar, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        applyTelegramTheme()
    }

    fun applyTelegramTheme() {
        val bgWhite = Theme.getColor(Theme.key_windowBackgroundWhite)
        val bgGray = Theme.getColor(Theme.key_windowBackgroundGray)
        val textPrimary = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
        val textSecondary = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
        val divider = Theme.getColor(Theme.key_divider)
        val abBg = Theme.getColor(Theme.key_actionBarDefault)
        val abTitle = Theme.getColor(Theme.key_actionBarDefaultTitle)

        root.setBackgroundColor(bgWhite)
        topBar.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(divider)
        tvTestTitle.setTextColor(abTitle)
        tvTimer.setTextColor(abTitle)
        tvOfflineBanner.setBackgroundColor(bgGray)
        tvOfflineBanner.setTextColor(textSecondary)
        rvQuestionPalette.setBackgroundColor(bgWhite)
        paletteDivider.setBackgroundColor(divider)
        loadingTest.setProgressColor(Theme.getColor(Theme.key_progressCircle))
        tvMessage.setTextColor(textPrimary)
        tvMessageSub.setTextColor(textSecondary)
        bottomDivider.setBackgroundColor(divider)
        layoutBottomBar.setBackgroundColor(bgWhite)
    }
}

package com.eve.app.ui.bookmarks

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.designsystem.TelegramChipGroup
import com.eve.app.designsystem.TelegramPullRefreshLayout
import com.eve.app.designsystem.TelegramSelectionBar
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for BookmarksActivity (Pass 3d).
 * Replaces activity_bookmarks.xml inflation using real upstream RecyclerListView,
 * RadialProgressView, Theme.getColor(Theme.key_*), Theme.createSelectorDrawable,
 * and AndroidUtilities.dp/getTypeface.
 */
class BookmarksTelegramViewTree(context: Context) {

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

    val toolbarNormal: LinearLayout = LinearLayout(context).apply {
        id = R.id.toolbarNormal
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4f), 0, dp(4f), 0)
    }

    val btnBack: ImageButton = createTopIcon(context, R.id.btnBack, R.drawable.ic_ab_back, "Back")

    private val tvTitle: TextView = TextView(context).apply {
        text = "Bookmarks"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val tvBookmarkCount: TextView = TextView(context).apply {
        id = R.id.tvBookmarkCount
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val btnSearch: ImageButton = createTopIcon(context, R.id.btnSearch, R.drawable.outline_header_search, "Search bookmarks")

    val toolbarSearch: LinearLayout = LinearLayout(context).apply {
        id = R.id.toolbarSearch
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = View.GONE
        setPadding(dp(4f), 0, dp(4f), 0)
    }

    val btnSearchBack: ImageButton = createTopIcon(context, R.id.btnSearchBack, R.drawable.ic_ab_back, "Close search")

    val etSearchBookmarks: EditText = EditText(context).apply {
        id = R.id.etSearchBookmarks
        hint = "Search saved questions..."
        background = null
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        setSingleLine(true)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = InputType.TYPE_CLASS_TEXT
    }

    val btnClearSearch: ImageButton = createTopIcon(context, R.id.btnClearSearch, R.drawable.ic_ab_close, "Clear search").apply {
        visibility = View.GONE
    }

    val selectionBar: TelegramSelectionBar = TelegramSelectionBar(context).apply {
        id = R.id.selectionBar
        visibility = View.GONE
    }

    val tvOfflineBanner: TextView = TextView(context).apply {
        id = R.id.tvOfflineBanner
        text = "Offline mode — showing cached bookmarks"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
        visibility = View.GONE
    }

    val filterScroll: HorizontalScrollView = HorizontalScrollView(context).apply {
        id = R.id.filterScroll
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
    }

    val chipGroupFilters: TelegramChipGroup = TelegramChipGroup(context).apply {
        id = R.id.chipGroupFilters
    }

    val pullRefreshLayout: TelegramPullRefreshLayout = TelegramPullRefreshLayout(context).apply {
        id = R.id.pullRefreshLayout
    }

    val rvBookmarks: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvBookmarks
        clipToPadding = false
        setPadding(0, dp(4f), 0, dp(24f))
    }

    val progressGroup: FrameLayout = FrameLayout(context).apply {
        id = R.id.progressGroup
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

    val btnRetry: AppCompatButton = AppCompatButton(context).apply {
        id = R.id.btnRetry
        text = "Retry"
        isAllCaps = false
        typeface = mediumFont()
    }

    init {
        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10f), 0, dp(8f), 0)
        }
        titleCol.addView(tvTitle)
        titleCol.addView(tvBookmarkCount)

        toolbarNormal.addView(btnBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        toolbarNormal.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        toolbarNormal.addView(btnSearch, LinearLayout.LayoutParams(dp(48f), dp(48f)))

        toolbarSearch.addView(btnSearchBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        toolbarSearch.addView(etSearchBookmarks, LinearLayout.LayoutParams(0, dp(40f), 1f))
        toolbarSearch.addView(btnClearSearch, LinearLayout.LayoutParams(dp(40f), dp(40f)))

        topBarHost.addView(toolbarNormal, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        topBarHost.addView(toolbarSearch, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        topBarHost.addView(selectionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))

        root.addView(topBarHost, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        root.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        root.addView(tvOfflineBanner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        filterScroll.addView(chipGroupFilters, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        root.addView(filterScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        val listFrame = FrameLayout(context)
        listFrame.addView(rvBookmarks, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        progressGroup.addView(RadialProgressView(context), LayoutHelper.createFrame(44, 44, Gravity.CENTER))
        listFrame.addView(progressGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        messageGroup.addView(ivMessageIcon, LayoutHelper.createLinear(112, 112, Gravity.CENTER_HORIZONTAL))
        messageGroup.addView(tvMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 12f, 0f, 4f))
        messageGroup.addView(tvMessageSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 14f))
        messageGroup.addView(btnRetry, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 42, Gravity.CENTER_HORIZONTAL))
        listFrame.addView(messageGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        pullRefreshLayout.addView(listFrame, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        root.addView(pullRefreshLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

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

        root.setBackgroundColor(bgWhite)
        topBarHost.setBackgroundColor(abBg)
        topDivider.setBackgroundColor(divider)
        tvTitle.setTextColor(abTitle)
        tvBookmarkCount.setTextColor(textSecondary)

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnBack, btnSearch, btnSearchBack, btnClearSearch).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }
        etSearchBookmarks.setTextColor(abTitle)
        etSearchBookmarks.setHintTextColor(textSecondary)
        tvOfflineBanner.setBackgroundColor(bgGray)
        tvOfflineBanner.setTextColor(textSecondary)
        rvBookmarks.setBackgroundColor(bgWhite)
        tvMessage.setTextColor(textPrimary)
        tvMessageSub.setTextColor(textSecondary)
        btnRetry.background = Theme.createSimpleSelectorRoundRectDrawable(
            dp(8f),
            Theme.getColor(Theme.key_featuredStickers_addButton),
            Theme.getColor(Theme.key_featuredStickers_addButtonPressed)
        )
        btnRetry.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText))
    }
}

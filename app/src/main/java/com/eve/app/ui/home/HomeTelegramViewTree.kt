package com.eve.app.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.eve.app.R
import com.eve.app.designsystem.TelegramActionBar
import com.eve.app.designsystem.TelegramAvatarImageView
import com.eve.app.designsystem.TelegramButton
import com.eve.app.designsystem.TelegramChipGroup
import com.eve.app.designsystem.TelegramFlickerSkeletonView
import com.eve.app.designsystem.TelegramPullRefreshLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.DividerCell
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RadialProgressView
import org.telegram.ui.Components.RecyclerListView

/**
 * Programmatic Telegram-native view hierarchy for MainActivity (Pass 3a).
 * Replaces activity_main.xml inflation with real upstream org.telegram.ui.*
 * primitives (RecyclerListView, DividerCell, RadialProgressView, Theme.getColor,
 * AndroidUtilities.dp/getTypeface, Theme.createSimpleSelectorDrawable).
 */
class HomeTelegramViewTree(context: Context) {

    private fun dp(v: Float): Int = AndroidUtilities.dp(v)
    private fun mediumFont(): Typeface =
        AndroidUtilities.getTypeface("fonts/rmedium.ttf") ?: Typeface.DEFAULT_BOLD

    val drawerLayout: DrawerLayout = DrawerLayout(context).apply {
        id = R.id.drawerLayout
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val root: DrawerLayout get() = drawerLayout

    // --- Main content host ---
    val mainContentContainer: LinearLayout = LinearLayout(context).apply {
        id = R.id.mainContentContainer
        orientation = LinearLayout.VERTICAL
        layoutParams = DrawerLayout.LayoutParams(
            DrawerLayout.LayoutParams.MATCH_PARENT,
            DrawerLayout.LayoutParams.MATCH_PARENT
        )
    }

    // --- Top bar container (Telegram ActionBar chrome + inline search bar) ---
    val topBarContainer: FrameLayout = FrameLayout(context).apply {
        id = R.id.topBarContainer
    }

    val homeGlassToolbar: TelegramActionBar = TelegramActionBar(context).apply {
        id = R.id.homeGlassToolbar
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dp(56f)
        )
    }

    val normalTopBar: LinearLayout = LinearLayout(context).apply {
        id = R.id.normalTopBar
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4f), 0, dp(4f), 0)
    }

    val btnMenu: ImageButton = createTopIconButton(context, R.id.btnMenu, R.drawable.ic_menu, "Menu")

    val titleGroup: LinearLayout = LinearLayout(context).apply {
        id = R.id.titleGroup
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10f), 0, dp(8f), 0)
    }

    val tvWelcome: TextView = TextView(context).apply {
        id = R.id.tvWelcome
        text = context.getString(R.string.app_name)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val btnLockApp: ImageButton = createTopIconButton(context, R.id.btnLockApp, R.drawable.ic_lock, "Lock app").apply {
        visibility = View.GONE
    }

    val notificationAction: FrameLayout = FrameLayout(context).apply {
        id = R.id.notificationAction
        isClickable = true
        isFocusable = true
        contentDescription = "Notifications"
    }

    val ivNotificationIdle: ImageView = ImageView(context).apply {
        id = R.id.ivNotificationIdle
        setImageResource(R.drawable.ic_home_notification)
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
    }

    val btnNotification: LottieAnimationView = LottieAnimationView(context).apply {
        id = R.id.btnNotification
        setAnimation(R.raw.home_notification_bell)
        visibility = View.INVISIBLE
        setPadding(dp(10f), dp(10f), dp(10f), dp(10f))
    }

    val dotUnread: View = View(context).apply {
        id = R.id.dotUnread
        visibility = View.GONE
    }

    val btnSearch: ImageButton = createTopIconButton(context, R.id.btnSearch, R.drawable.outline_header_search, "Search")
    val btnOverflow: ImageButton = createTopIconButton(context, R.id.btnOverflow, R.drawable.ic_ab_other, "More options")

    // --- Search bar ---
    val searchTopBar: LinearLayout = LinearLayout(context).apply {
        id = R.id.searchTopBar
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        visibility = View.GONE
        setPadding(dp(4f), 0, dp(4f), 0)
    }

    val btnSearchBack: ImageButton = createTopIconButton(context, R.id.btnSearchBack, R.drawable.ic_ab_back, "Close search")

    val layoutSearchFieldContainer: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutSearchFieldContainer
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8f), 0, dp(4f), 0)
    }

    val etSearch: EditText = EditText(context).apply {
        id = R.id.etSearch
        hint = "Search exams..."
        background = null
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        setSingleLine(true)
        imeOptions = EditorInfo.IME_ACTION_SEARCH
        inputType = InputType.TYPE_CLASS_TEXT
    }

    val btnClearSearch: ImageButton = createTopIconButton(context, R.id.btnClearSearch, R.drawable.ic_ab_close, "Clear search").apply {
        visibility = View.GONE
    }

    // --- Search filters & suggestions ---
    val searchFilterScroll: HorizontalScrollView = HorizontalScrollView(context).apply {
        id = R.id.searchFilterScroll
        isHorizontalScrollBarEnabled = false
        visibility = View.GONE
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
        clipToPadding = false
    }

    val searchFilterGroup: TelegramChipGroup = TelegramChipGroup(context).apply {
        id = R.id.searchFilterGroup
    }

    val searchSuggestionsList: LinearLayout = LinearLayout(context).apply {
        id = R.id.searchSuggestionsList
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }

    // --- Offline banner ---
    val tvOfflineBanner: TextView = TextView(context).apply {
        id = R.id.tvOfflineBanner
        text = "Offline mode — showing cached exams"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
        setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
        visibility = View.GONE
    }

    // --- Category filter strip ---
    val chipGroupCategory: HomeCategoryChipGroup = HomeCategoryChipGroup(context).apply {
        id = R.id.chipGroupCategory
    }

    // --- Home rotating banner ---
    val panelHomeBanner: FrameLayout = FrameLayout(context).apply {
        id = R.id.panelHomeBanner
        visibility = View.GONE
    }

    val blurHomeBannerMaterial: View = View(context).apply {
        id = R.id.blurHomeBannerMaterial
    }

    val vpHomeBanners: ViewPager2 = ViewPager2(context).apply {
        id = R.id.vpHomeBanners
    }

    val btnHomeBannerCta: AppCompatButton = AppCompatButton(context).apply {
        id = R.id.btnHomeBannerCta
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        typeface = mediumFont()
        visibility = View.GONE
        setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
    }

    // --- Home active poll card ---
    val cardPoll: FrameLayout = FrameLayout(context).apply {
        id = R.id.cardPoll
        visibility = View.GONE
    }

    val tvHomePollHeader: TextView = TextView(context).apply {
        id = R.id.tvHomePollHeader
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        typeface = mediumFont()
    }

    val tvHomePollQuestion: TextView = TextView(context).apply {
        id = R.id.tvHomePollQuestion
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = mediumFont()
    }

    val tvHomePollStatus: TextView = TextView(context).apply {
        id = R.id.tvHomePollStatus
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        visibility = View.GONE
    }

    val rgHomePollOptions: RadioGroup = RadioGroup(context).apply {
        id = R.id.rgHomePollOptions
        orientation = RadioGroup.VERTICAL
    }

    val btnSubmitHomeVote: AppCompatButton = AppCompatButton(context).apply {
        id = R.id.btnSubmitHomeVote
        text = "Vote"
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = mediumFont()
        visibility = View.GONE
    }

    val layoutHomePollResults: LinearLayout = LinearLayout(context).apply {
        id = R.id.layoutHomePollResults
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
    }

    val tvHomePollTotalVotes: TextView = TextView(context).apply {
        id = R.id.tvHomePollTotalVotes
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        visibility = View.GONE
    }

    // --- Pull refresh + real upstream RecyclerListView ---
    val pullRefreshLayout: TelegramPullRefreshLayout = TelegramPullRefreshLayout(context).apply {
        id = R.id.pullRefreshLayout
    }

    val rvExams: RecyclerListView = RecyclerListView(context).apply {
        id = R.id.rvExams
        clipToPadding = false
        setPadding(0, 0, 0, dp(88f))
    }

    val progressGroup: LinearLayout = LinearLayout(context).apply {
        id = R.id.progressGroup
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        visibility = View.GONE
    }

    val loadingHome: TelegramFlickerSkeletonView = TelegramFlickerSkeletonView(context).apply {
        id = R.id.loadingHome
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

    // --- Floating airplane CTA ---
    val cardFloatingAirplane: FrameLayout = FrameLayout(context).apply {
        id = R.id.cardFloatingAirplane
        visibility = View.GONE
        isClickable = true
        isFocusable = true
    }

    val lottieFloatingAirplane: LottieAnimationView = LottieAnimationView(context).apply {
        id = R.id.lottieFloatingAirplane
        setAnimation(R.raw.man_flying_on_paper_airplane)
    }

    // --- Availability / Maintenance / Force-update overlays ---
    val availabilityCheckingOverlay: FrameLayout = FrameLayout(context).apply {
        id = R.id.availabilityCheckingOverlay
        visibility = View.GONE
        isClickable = true
        isFocusable = true
    }

    val maintenanceOverlay: ScrollView = ScrollView(context).apply {
        id = R.id.maintenanceOverlay
        isFillViewport = true
        visibility = View.GONE
    }

    val tvMaintenanceTitle: TextView = TextView(context).apply {
        id = R.id.tvMaintenanceTitle
        text = "Under Maintenance"
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        typeface = mediumFont()
        gravity = Gravity.CENTER
    }

    val tvMaintenanceMessage: TextView = TextView(context).apply {
        id = R.id.tvMaintenanceMessage
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        gravity = Gravity.CENTER
    }

    val btnCheckMaintenanceAgain: AppCompatButton = AppCompatButton(context).apply {
        id = R.id.btnCheckMaintenanceAgain
        text = "Check Again"
        isAllCaps = false
        typeface = mediumFont()
    }

    val forceUpdateOverlay: FrameLayout = FrameLayout(context).apply {
        id = R.id.forceUpdateOverlay
        visibility = View.GONE
        isClickable = true
        isFocusable = true
    }

    val tvForceUpdateMessage: TextView = TextView(context).apply {
        id = R.id.tvForceUpdateMessage
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
        gravity = Gravity.CENTER
    }

    val btnForceUpdateNow: AppCompatButton = AppCompatButton(context).apply {
        id = R.id.btnForceUpdateNow
        text = "Update Now"
        isAllCaps = false
        typeface = mediumFont()
    }

    // --- Side navigation drawer ---
    val navDrawerPanel: FrameLayout = FrameLayout(context).apply {
        id = R.id.navDrawerPanel
        layoutParams = DrawerLayout.LayoutParams(
            dp(304f),
            DrawerLayout.LayoutParams.MATCH_PARENT,
            GravityCompat.START
        )
    }

    val ivDrawerGlassBlurBackground: ImageView = ImageView(context).apply {
        id = R.id.ivDrawerGlassBlurBackground
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val ivDrawerAvatar: TelegramAvatarImageView = TelegramAvatarImageView(context).apply {
        id = R.id.ivDrawerAvatar
    }

    val tvDrawerUserName: TextView = TextView(context).apply {
        id = R.id.tvDrawerUserName
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        typeface = mediumFont()
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val tvDrawerUserEmail: TextView = TextView(context).apply {
        id = R.id.tvDrawerUserEmail
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val btnDrawerQr: ImageButton = createTopIconButton(context, R.id.btnDrawerQr, R.drawable.telegram_profile_qr, "QR Code")

    val layoutDrawerProfile: LinearLayout
    val layoutDrawerPremium: LinearLayout
    val lottieDrawerPremiumStarFallback: LottieAnimationView = LottieAnimationView(context).apply {
        id = R.id.lottieDrawerPremiumStarFallback
        setAnimation(R.raw.premium_star)
    }
    val tvDrawerPremiumStatus: TextView = TextView(context).apply {
        id = R.id.tvDrawerPremiumStatus
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        visibility = View.GONE
    }
    val layoutDrawerPerformance: LinearLayout
    val layoutDrawerLeaderboard: LinearLayout
    val layoutDrawerSyllabus: LinearLayout
    val layoutDrawerTopic: LinearLayout
    val layoutDrawerPyq: LinearLayout
    val layoutDrawerMistakes: LinearLayout
    val layoutDrawerSettings: LinearLayout

    val btnAdmin: TelegramButton = TelegramButton(context).apply {
        id = R.id.btnAdmin
        text = "Admin Console"
        visibility = View.GONE
    }

    private val drawerRowTitles = mutableListOf<TextView>()
    private val drawerRowIcons = mutableListOf<ImageView>()
    private val drawerRowContainers = mutableListOf<LinearLayout>()
    private val drawerDividers = mutableListOf<DividerCell>()
    private val pollCardInner: LinearLayout
    private val topBarDivider: View

    init {
        // 1. Assemble Top Bar
        titleGroup.addView(tvWelcome, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        notificationAction.addView(ivNotificationIdle, FrameLayout.LayoutParams(dp(48f), dp(48f), Gravity.CENTER))
        notificationAction.addView(btnNotification, FrameLayout.LayoutParams(dp(48f), dp(48f), Gravity.CENTER))
        notificationAction.addView(dotUnread, FrameLayout.LayoutParams(dp(8f), dp(8f), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(11f)
            marginEnd = dp(11f)
        })

        normalTopBar.addView(btnMenu, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        normalTopBar.addView(titleGroup, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        normalTopBar.addView(btnLockApp, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        normalTopBar.addView(notificationAction, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        normalTopBar.addView(btnSearch, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        normalTopBar.addView(btnOverflow, LinearLayout.LayoutParams(dp(48f), dp(48f)))

        layoutSearchFieldContainer.addView(etSearch, LinearLayout.LayoutParams(0, dp(40f), 1f))
        layoutSearchFieldContainer.addView(btnClearSearch, LinearLayout.LayoutParams(dp(40f), dp(40f)))
        searchTopBar.addView(btnSearchBack, LinearLayout.LayoutParams(dp(48f), dp(48f)))
        searchTopBar.addView(layoutSearchFieldContainer, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        topBarContainer.addView(homeGlassToolbar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        topBarContainer.addView(normalTopBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))
        topBarContainer.addView(searchTopBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56f))

        topBarDivider = View(context)

        // 2. Assemble Search filter & suggestions
        searchFilterScroll.addView(searchFilterGroup, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))

        // 3. Assemble Home Banner
        panelHomeBanner.addView(blurHomeBannerMaterial, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        panelHomeBanner.addView(vpHomeBanners, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 152f))
        panelHomeBanner.addView(btnHomeBannerCta, LayoutHelper.createFrame(
            LayoutHelper.WRAP_CONTENT, 34f, Gravity.BOTTOM or Gravity.END, 0f, 0f, 12f, 10f
        ))

        // 4. Assemble Poll Card
        pollCardInner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        }
        pollCardInner.addView(tvHomePollHeader)
        pollCardInner.addView(tvHomePollQuestion, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 4f, 0f, 4f))
        pollCardInner.addView(tvHomePollStatus)
        pollCardInner.addView(rgHomePollOptions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 8f, 0f, 4f))
        pollCardInner.addView(btnSubmitHomeVote, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 42, 0f, 6f, 0f, 4f))
        pollCardInner.addView(layoutHomePollResults, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 4f))
        pollCardInner.addView(tvHomePollTotalVotes)
        cardPoll.addView(pollCardInner, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat()))

        // 5. Assemble Body + PullRefresh + Overlays
        val contentHost = FrameLayout(context)
        val bodyCol = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        bodyCol.addView(tvOfflineBanner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        bodyCol.addView(searchFilterScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        bodyCol.addView(searchSuggestionsList, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        bodyCol.addView(chipGroupCategory, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12f, 6f, 12f, 6f))
        bodyCol.addView(panelHomeBanner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 152, 16f, 6f, 16f, 8f))
        bodyCol.addView(cardPoll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16f, 4f, 16f, 8f))

        val listFrame = FrameLayout(context)
        listFrame.addView(rvExams, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        progressGroup.addView(RadialProgressView(context), LayoutHelper.createLinear(44, 44))
        listFrame.addView(progressGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        listFrame.addView(loadingHome, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        messageGroup.addView(ivMessageIcon, LayoutHelper.createLinear(112, 112, Gravity.CENTER_HORIZONTAL))
        messageGroup.addView(tvMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 12f, 0f, 4f))
        messageGroup.addView(tvMessageSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 14f))
        messageGroup.addView(btnRetry, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 42, Gravity.CENTER_HORIZONTAL))
        listFrame.addView(messageGroup, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        pullRefreshLayout.addView(listFrame, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        bodyCol.addView(pullRefreshLayout, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        contentHost.addView(bodyCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        // Floating airplane button
        cardFloatingAirplane.addView(lottieFloatingAirplane, LayoutHelper.createFrame(56, 56, Gravity.CENTER))
        contentHost.addView(cardFloatingAirplane, LayoutHelper.createFrame(
            64, 64f, Gravity.BOTTOM or Gravity.END, 0f, 0f, 16f, 20f
        ))

        // Availability overlay
        availabilityCheckingOverlay.addView(
            RadialProgressView(context),
            LayoutHelper.createFrame(44, 44, Gravity.CENTER)
        )
        contentHost.addView(availabilityCheckingOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        // Maintenance overlay
        val maintCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
        }
        maintCol.addView(tvMaintenanceTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        maintCol.addView(tvMaintenanceMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 10f, 0f, 20f))
        maintCol.addView(btnCheckMaintenanceAgain, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44, Gravity.CENTER_HORIZONTAL))
        maintenanceOverlay.addView(maintCol, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        contentHost.addView(maintenanceOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        // Force update overlay
        val forceCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
        }
        forceCol.addView(tvForceUpdateMessage, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 0f, 0f, 20f))
        forceCol.addView(btnForceUpdateNow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 44, Gravity.CENTER_HORIZONTAL))
        forceUpdateOverlay.addView(forceCol, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER))
        contentHost.addView(forceUpdateOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        mainContentContainer.addView(topBarContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56))
        mainContentContainer.addView(topBarDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1))
        mainContentContainer.addView(contentHost, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // 6. Assemble Side Navigation Drawer (Telegram Drawer Style)
        navDrawerPanel.addView(ivDrawerGlassBlurBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))
        val drawerScroll = ScrollView(context).apply { isVerticalScrollBarEnabled = false }
        val drawerCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8f), 0, dp(24f))
        }

        // Drawer header (Avatar + Name + Email + QR)
        val headerBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(20f), dp(12f), dp(14f))
        }
        val avatarRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        avatarRow.addView(ivDrawerAvatar, LayoutHelper.createLinear(64, 64))
        avatarRow.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        avatarRow.addView(btnDrawerQr, LayoutHelper.createLinear(44, 44))
        headerBox.addView(avatarRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        headerBox.addView(tvDrawerUserName, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 12f, 0f, 2f))
        headerBox.addView(tvDrawerUserEmail, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        drawerCol.addView(headerBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        fun addDivider() {
            val d = DividerCell(context)
            drawerDividers.add(d)
            drawerCol.addView(d, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))
        }

        addDivider()
        layoutDrawerProfile = createDrawerRow(context, R.id.layoutDrawerProfile, R.drawable.msg_openprofile, "My Profile")
        drawerCol.addView(layoutDrawerProfile)

        // Premium row with Lottie star + status badge
        layoutDrawerPremium = LinearLayout(context).apply {
            id = R.id.layoutDrawerPremium
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(19f), 0, dp(16f), 0)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f))
        }
        layoutDrawerPremium.addView(lottieDrawerPremiumStarFallback, LayoutHelper.createLinear(26, 26, 0f, 0f, 24f, 0f))
        val premTitle = TextView(context).apply {
            text = "EVE Premium"
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            typeface = mediumFont()
        }
        drawerRowTitles.add(premTitle)
        drawerRowContainers.add(layoutDrawerPremium)
        layoutDrawerPremium.addView(premTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        layoutDrawerPremium.addView(tvDrawerPremiumStatus)
        drawerCol.addView(layoutDrawerPremium)

        addDivider()
        layoutDrawerPerformance = createDrawerRow(context, R.id.layoutDrawerPerformance, R.drawable.ic_performance_increase, "Performance")
        layoutDrawerLeaderboard = createDrawerRow(context, R.id.layoutDrawerLeaderboard, R.drawable.ic_leaderboard_star, "Leaderboard")
        layoutDrawerSyllabus = createDrawerRow(context, R.id.layoutDrawerSyllabus, R.drawable.ic_syllabus, "Syllabus")
        layoutDrawerTopic = createDrawerRow(context, R.id.layoutDrawerTopic, R.drawable.ic_topic, "Topic Practice")
        layoutDrawerPyq = createDrawerRow(context, R.id.layoutDrawerPyq, R.drawable.ic_pyq, "Previous Year Papers")
        layoutDrawerMistakes = createDrawerRow(context, R.id.layoutDrawerMistakes, R.drawable.ic_warning, "Mistake Notebook")
        drawerCol.addView(layoutDrawerPerformance)
        drawerCol.addView(layoutDrawerLeaderboard)
        drawerCol.addView(layoutDrawerSyllabus)
        drawerCol.addView(layoutDrawerTopic)
        drawerCol.addView(layoutDrawerPyq)
        drawerCol.addView(layoutDrawerMistakes)

        addDivider()
        layoutDrawerSettings = createDrawerRow(context, R.id.layoutDrawerSettings, R.drawable.telegram_profile_settings, "Settings")
        drawerCol.addView(layoutDrawerSettings)
        drawerCol.addView(btnAdmin, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, 16f, 12f, 16f, 8f))

        drawerScroll.addView(drawerCol, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        navDrawerPanel.addView(drawerScroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat()))

        drawerLayout.addView(mainContentContainer)
        drawerLayout.addView(navDrawerPanel)

        applyTelegramTheme()
    }

    private fun createTopIconButton(context: Context, viewId: Int, iconRes: Int, desc: String): ImageButton {
        return ImageButton(context).apply {
            id = viewId
            setImageResource(iconRes)
            contentDescription = desc
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
        }
    }

    private fun createDrawerRow(context: Context, viewId: Int, iconRes: Int, label: String): LinearLayout {
        val row = LinearLayout(context).apply {
            id = viewId
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(19f), 0, dp(16f), 0)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48f))
        }
        val iv = ImageView(context).apply {
            setImageResource(iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val tv = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
            typeface = mediumFont()
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        drawerRowIcons.add(iv)
        drawerRowTitles.add(tv)
        drawerRowContainers.add(row)
        row.addView(iv, LayoutHelper.createLinear(24, 24, 0f, 0f, 26f, 0f))
        row.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        return row
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
        val listSelector = Theme.getColor(Theme.key_listSelector)
        val drawerBg = Theme.getColor(Theme.key_chats_menuBackground)
        val drawerIcon = Theme.getColor(Theme.key_chats_menuItemIcon)
        val drawerText = Theme.getColor(Theme.key_chats_menuItemText)
        val btnBg = Theme.getColor(Theme.key_featuredStickers_addButton)
        val btnText = Theme.getColor(Theme.key_featuredStickers_buttonText)

        drawerLayout.setBackgroundColor(bgWhite)
        mainContentContainer.setBackgroundColor(bgWhite)
        topBarContainer.setBackgroundColor(abBg)
        topBarDivider.setBackgroundColor(divider)
        tvWelcome.setTextColor(abTitle)

        val iconTint = ColorStateList.valueOf(abIcon)
        listOf(btnMenu, btnLockApp, btnSearch, btnOverflow, btnSearchBack, btnClearSearch).forEach { btn ->
            btn.imageTintList = iconTint
            btn.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        }
        ivNotificationIdle.imageTintList = iconTint
        notificationAction.background = Theme.createSelectorDrawable(abSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)
        dotUnread.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Theme.getColor(Theme.key_text_RedBold))
        }

        etSearch.setTextColor(abTitle)
        etSearch.setHintTextColor(textSecondary)
        tvOfflineBanner.setBackgroundColor(bgGray)
        tvOfflineBanner.setTextColor(textSecondary)
        searchSuggestionsList.setBackgroundColor(bgWhite)

        pollCardInner.background = GradientDrawable().apply {
            cornerRadius = dp(12f).toFloat()
            setColor(bgGray)
        }
        tvHomePollHeader.setTextColor(accent)
        tvHomePollQuestion.setTextColor(textPrimary)
        tvHomePollStatus.setTextColor(textSecondary)
        tvHomePollTotalVotes.setTextColor(textSecondary)

        listOf(btnHomeBannerCta, btnSubmitHomeVote, btnRetry, btnCheckMaintenanceAgain, btnForceUpdateNow).forEach { b ->
            b.background = Theme.createSimpleSelectorRoundRectDrawable(dp(8f), btnBg, Theme.getColor(Theme.key_featuredStickers_addButtonPressed))
            b.setTextColor(btnText)
        }

        rvExams.setBackgroundColor(bgWhite)
        tvMessage.setTextColor(textPrimary)
        tvMessageSub.setTextColor(textSecondary)
        availabilityCheckingOverlay.setBackgroundColor(bgWhite)
        maintenanceOverlay.setBackgroundColor(bgWhite)
        tvMaintenanceTitle.setTextColor(textPrimary)
        tvMaintenanceMessage.setTextColor(textSecondary)
        forceUpdateOverlay.setBackgroundColor(bgWhite)
        tvForceUpdateMessage.setTextColor(textPrimary)

        navDrawerPanel.setBackgroundColor(drawerBg)
        ivDrawerGlassBlurBackground.setBackgroundColor(drawerBg)
        tvDrawerUserName.setTextColor(drawerText)
        tvDrawerUserEmail.setTextColor(textSecondary)
        tvDrawerPremiumStatus.setTextColor(accent)
        btnDrawerQr.imageTintList = ColorStateList.valueOf(drawerIcon)
        btnDrawerQr.background = Theme.createSelectorDrawable(listSelector, Theme.RIPPLE_MASK_CIRCLE_20DP)

        val drawerIconTint = ColorStateList.valueOf(drawerIcon)
        drawerRowIcons.forEach { it.imageTintList = drawerIconTint }
        drawerRowTitles.forEach { it.setTextColor(drawerText) }
        drawerRowContainers.forEach { it.background = Theme.getSelectorDrawable(false) }
        drawerDividers.forEach { it.invalidate() }
    }
}

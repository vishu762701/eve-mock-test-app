package com.eve.app.ui.home

import com.eve.app.designsystem.UiArcMotion

import android.graphics.Outline
import android.view.ViewOutlineProvider

import com.eve.app.ui.common.EveBaseActivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.content.res.Configuration
import android.provider.Settings
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.airbnb.lottie.value.SimpleLottieValueCallback
import com.eve.app.util.AppBulletin
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.HomeBanner
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.admin.AdminActivity
import com.eve.app.ui.bookmarks.BookmarksActivity
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.notifications.NotificationsActivity
import com.eve.app.ui.performance.PerformanceActivity
import com.eve.app.ui.practice.PracticeActivity
import com.eve.app.ui.profile.ProfileActivity
import com.eve.app.ui.pyq.PyqActivity
import com.eve.app.ui.settings.SettingsActivity
import com.eve.app.ui.syllabus.SyllabusActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.BuildConfig
import com.eve.app.util.AppConfigManager
import com.eve.app.util.Constants
import com.eve.app.util.CrashlyticsHelper
import com.eve.app.util.NetworkUtil
import com.eve.app.util.NotificationHelper
import com.eve.app.util.NotificationStore
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.AvatarDrawable
import com.eve.app.data.repository.FloatingLinkData
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.ThemeManager
import com.eve.app.designsystem.ButtonBounce
import com.eve.app.util.ThemeSwitchAnimator
import com.eve.app.theme.TelegramThemeEngine
import com.eve.app.util.SessionManager
import com.eve.app.util.UiState
import com.eve.app.util.VibrationHelper
import com.eve.app.util.hasFreshWorkerAdminHint
import coil.load
import coil.transform.CircleCropTransformation
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import android.view.ViewConfiguration
import android.view.HapticFeedbackConstants
import org.telegram.ui.Components.CubicBezierInterpolator
import com.eve.app.util.HomeBannerLinkValidator
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : EveBaseActivity() {

    private val lifecycleExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val stackTrace = Log.getStackTraceString(throwable)
        Log.e("EVE_STARTUP", "Uncaught exception in MainActivity lifecycleScope:\n$stackTrace", throwable)
        Log.e("MainActivity", "Uncaught exception in lifecycleScope:\n$stackTrace", throwable)
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private var isAdminUser: Boolean = false

    // ActionBarMenuItem.java:779-782: a second tap toggles the SAME popup closed.
    // Do not stack separate focusable windows on repeated overflow taps.
    private var homeOverflowPopup: TelegramMenuPopup? = null
    private var isSearchActive = false
    private var searchTransitionAnimator: android.animation.AnimatorSet? = null
    private var currentSearchQuery = ""
    private var homeListPositionBeforeSearch: android.os.Parcelable? = null

    private var restoreThemeContent = false
    private var themeListState: android.os.Parcelable? = null
    private var lastLoadedItems: List<HomeListItem> = emptyList()
    private var lastSearchItems: List<HomeListItem> = emptyList()
    private var searchDebounceJob: Job? = null

    private var notifJob: Job? = null

    private val bannerAdapter = HomeBannerAdapter()
    private val bannerRepo = com.eve.app.data.repository.HomeBannerRepository()
    private var bannerObserverJob: Job? = null
    private var bannerAutoScrollJob: Job? = null
    private var currentBannerCount = 0
    private var isUserDraggingBanner = false
    private var homeBanners: List<HomeBanner> = emptyList()

    private val floatingLinkRepo = com.eve.app.data.repository.FloatingLinkRepository()
    private var activeFloatingLinkUrl: String? = null
    private var activeFloatingLinkData: FloatingLinkData? = null
    private var isFloatingAirplaneAdmin: Boolean = false
    // Prevent overlapping admin PUTs from persisting stale coordinates out of order.
    private var floatingLinkPositionSaveInFlight = false
    private var safeInsetTop = 0
    private var safeInsetBottom = 0
    private var safeInsetLeft = 0
    private var safeInsetRight = 0

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignored */ }

    private val foregroundNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctx = context ?: this@MainActivity
            VibrationHelper.vibrateNotification(ctx)
            animateNotificationBell()
            updateNotificationDot()
        }
    }

    private var isExamNavigating = false

    private val adapter = ExamAdapter(
        onClick = { exam, attempted, _ ->
            if (isExamNavigating) return@ExamAdapter
            isExamNavigating = true
            getSharedPreferences("eve_app_prefs", Context.MODE_PRIVATE).edit().putString("last_exam_id", exam.id).apply()

            // a) If the loaded exam list contains any exam with parentExamId == this exam.id -> open ExamTestsActivity
            if (viewModel.hasSubExams(exam.id)) {
                isExamNavigating = false
                val intent = Intent(this, ExamTestsActivity::class.java).apply {
                    putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                    putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                }
                startActivity(intent)
                return@ExamAdapter
            }

            // b) Else fetch ExamRepository.getLiveGeneratedTests(exam.id). While loading, use existing loading indicator pattern and ignore repeated taps.
            binding.progressGroup.visibility = View.VISIBLE
            lifecycleScope.launch(lifecycleExceptionHandler) {
                try {
                    val liveTests = com.eve.app.data.repository.ExamRepository().getLiveGeneratedTests(exam.id)
                    binding.progressGroup.visibility = View.GONE
                    isExamNavigating = false

                    // c) If the list is non-empty -> open ExamTestsActivity for this exam
                    if (liveTests.isNotEmpty()) {
                        val intent = Intent(this@MainActivity, ExamTestsActivity::class.java).apply {
                            putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                            putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                        }
                        startActivity(intent)
                    } else {
                        // d) If the list is empty -> run the EXISTING legacy logic exactly as it is today
                        val isSubmitted = attempted || HomeViewModel.isAttemptSubmitted(exam.id)
                        if (isSubmitted) {
                            ExamLaunchHelper.openPreviousAttemptResult(
                                context = this@MainActivity,
                                scope = lifecycleScope,
                                examId = exam.id,
                                examName = exam.examName,
                                exam = exam,
                                onLoading = { loading ->
                                    binding.progressGroup.visibility = if (loading) View.VISIBLE else View.GONE
                                }
                            )
                        } else {
                            startActivity(
                                Intent(this@MainActivity, TestActivity::class.java)
                                    .putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                                    .putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                                    .putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                                    .putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
                                    .putExtra(Constants.EXTRA_NEGATIVE_MARKING, exam.negativeMarkingValue)
                            )
                        }
                    }
                } catch (e: Exception) {
                    binding.progressGroup.visibility = View.GONE
                    isExamNavigating = false
                    AppBulletin.showError(this@MainActivity, "Couldn't load tests")
                }
            }
        },
        onLongClick = { exam: Exam, isPinned: Boolean, anchorView: View ->
            showPinPopupMenu(exam, isPinned, anchorView)
        },
        onFeedbackPostLongClick = { post, _ ->
            handleFeedbackPostLongClick(post)
        },
        onSendReply = { post, text, onComplete ->
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                com.eve.app.util.AppBulletin.show(this, "Please sign in to reply")
                onComplete(false)
            } else {
                lifecycleScope.launch(lifecycleExceptionHandler) {
                    val repo = com.eve.app.data.repository.FeedbackRepository()
                    val result = repo.submitPostReply(
                        postId = post.id,
                        uid = user.uid,
                        name = user.displayName ?: "Student",
                        email = user.email ?: "",
                        text = text
                    )
                    result.onSuccess {
                        com.eve.app.util.AppBulletin.showSuccess(this@MainActivity, "Reply sent")
                        onComplete(true)
                    }.onFailure { err ->
                        com.eve.app.util.AppBulletin.showError(this@MainActivity, "Failed to send: ${err.localizedMessage ?: "Unknown error"}")
                        onComplete(false)
                    }
                }
            }
        }
    )

    private var hasEmptyPlayed = false
    private var hasAnimatedWelcome = false
    private var greetingIntroAnimator: android.animation.Animator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreThemeContent = com.eve.app.util.ThemeManager.isThemeRecreation(this, savedInstanceState) &&
            viewModel.state.value is UiState.Success
        if (restoreThemeContent) {
            @Suppress("DEPRECATION")
            themeListState = savedInstanceState?.getParcelable("theme_home_list_state")
        }
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false
        hasAnimatedWelcome = savedInstanceState?.getBoolean(HomeGreetingMotion.KEY_HAS_ANIMATED_GREETING, false) ?: false

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            goToLogin()
            return
        }

        Log.e("EVE_STARTUP", "stage: inflate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        TelegramThemeEngine.addThemeChangeListener(categoryPaletteListener)
        setupHomeBannerSurface()
        setupTopBarDepth()

        com.eve.app.util.SystemBarHelper.syncSystemBars(this)

        Log.e("EVE_STARTUP", "stage: setupDrawer")
        setupDrawer(user)

        Log.e("EVE_STARTUP", "stage: setupBannerCarousel")
        setupBannerCarousel()

        binding.tvWelcome.text = "Hi, ${user.displayName ?: "Student"}"
        if (!hasAnimatedWelcome) {
            hasAnimatedWelcome = true
            greetingIntroAnimator = HomeGreetingMotion.animateIntro(binding.tvWelcome)
        } else {
            HomeGreetingMotion.settleNatural(binding.tvWelcome)
        }

        Log.e("EVE_STARTUP", "stage: setupNotificationBell")
        setupNotificationBell()

        Log.e("EVE_STARTUP", "stage: setupPushNotifications")
        setupPushNotifications()

        val notifFilter = IntentFilter("com.eve.app.NOTIFICATION_RECEIVED")
        ContextCompat.registerReceiver(
            this,
            foregroundNotificationReceiver,
            notifFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        com.eve.app.designsystem.DesignPress.bind(binding.btnMenu) {
            if (ThemeSwitchAnimator.isTransitioning || isFinishing || isDestroyed) return@bind
            prepareDrawerSurface()
            binding.drawerLayout.openDrawer(GravityCompat.START, UiArcMotion.isMotionEnabled(this))
        }
        loadProfilePhoto(user)
        lifecycleScope.launch(lifecycleExceptionHandler) {
            val admin = adminRepo.isAdmin(user.email)
            isFloatingAirplaneAdmin = admin
        }

        // Telegram-style Search setup
        Log.e("EVE_STARTUP", "stage: setupSearch")
        setupSearch()
        restoreSearchPresentation(savedInstanceState)

        // Telegram-style overflow menu setup
        binding.btnOverflow.setOnClickListener { anchor ->
            if (ThemeSwitchAnimator.isTransitioning || isFinishing || isDestroyed) return@setOnClickListener
            val openPopup = homeOverflowPopup
            if (openPopup?.isShowing == true) {
                openPopup.dismiss()
                return@setOnClickListener
            }
            homeOverflowPopup = TelegramMenuPopup(
                context = this,
                onThemeToggle = { cx, cy, iconWidth, iconHeight ->
                    try {
                        val goingDark = !ThemeSwitchAnimator.isDarkMode(this)
                        ThemeSwitchAnimator.animateAt(this, cx, cy, goingDark, iconWidth, iconHeight)
                    } catch (t: Throwable) {
                        android.util.Log.e("ThemeClickDiag", "EXCEPTION in MainActivity onThemeToggle", t)
                    }
                },
                onHistory = { com.eve.app.util.EveNavigationHelper.navigate(this, Intent(this, HistoryActivity::class.java), anchor) },
                onBookmarks = { com.eve.app.util.EveNavigationHelper.navigate(this, Intent(this, BookmarksActivity::class.java), anchor) }
            ).also { it.show(anchor) }
        }

        // System back button closes drawer first, then active search
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START, UiArcMotion.isMotionEnabled(this@MainActivity))
                } else if (isSearchActive) {
                    closeSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        Log.e("EVE_STARTUP", "stage: viewModel load")
        val initialAdmin = hasFreshWorkerAdminHint(user.email)
        isAdminUser = initialAdmin
        if (initialAdmin) {
            showAdminButton()
        }
        viewModel.loadForUser(user.uid, initialAdmin)

        lifecycleScope.launch(lifecycleExceptionHandler) {
            val admin = adminRepo.isAdmin(user.email)
            if (admin != isAdminUser) {
                isAdminUser = admin
                if (admin) showAdminButton() else binding.btnAdmin.visibility = View.GONE
                viewModel.loadForUser(user.uid, admin)
            }
        }

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter
        if (restoreThemeContent) {
            binding.rvExams.layoutAnimation = null
            render(viewModel.state.value)
        }

        Log.e("EVE_STARTUP", "stage: setupFloatingAirplane")
        setupFloatingAirplane()

        binding.btnRetry.setOnClickListener { viewModel.load() }

        updateNotificationDot()

        checkAppConfigAndMaintenance()

        lifecycleScope.launch(lifecycleExceptionHandler) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    NetworkUtil.observe(this@MainActivity).collect { online ->
                        binding.tvOfflineBanner.visibility = if (online) View.GONE else View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
        // Lifecycle can still report STARTED inside onResume. Run after the
        // resume dispatch so an actual incoming bell event is not dropped.
        binding.root.post {
            if (!isFinishing && !isDestroyed && ::binding.isInitialized &&
                pendingIncomingBellUntilResume &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                pendingIncomingBellUntilResume = false
                if (NotificationStore.hasUnread(this)) animateNotificationBell()
            }
        }
        hasEmptyPlayed = false
        isExamNavigating = false
        checkAppConfigAndMaintenance()
        val current = FirebaseAuth.getInstance().currentUser
        loadProfilePhoto(current)
        if (current != null) {
            updateDrawerHeader(current)
            lifecycleScope.launch(lifecycleExceptionHandler) {
                val admin = adminRepo.isAdmin(current.email)
                isFloatingAirplaneAdmin = admin
                viewModel.loadForUser(current.uid, admin)
            }
        }
        // On return from another screen, the admin may have moved the icon.
        // Skip the 10-minute in-memory URL/position cache and read the public
        // server-authoritative coordinates (no admin endpoint or privilege).
        refreshFloatingLink(forceRefresh = true)

        if (::binding.isInitialized && binding.cardFloatingAirplane.visibility == View.VISIBLE) {
            val animScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            if (animScale > 0f && !binding.lottieFloatingAirplane.isAnimating) {
                binding.lottieFloatingAirplane.resumeAnimation()
            }
        }
        if (::binding.isInitialized) {
            setupDrawerPremiumStar()
        }
        // Debug shows actual drawer/scrim and floating-pill geometry after
        // FLAG_ACTIVITY_REORDER_TO_FRONT resumes. The release implementation is a no-op.
        HomeLayoutDiagnostic.report(
            this, binding.drawerLayout, null,
            listOf(
                "maintenance" to binding.maintenanceOverlay,
                "forceUpdate" to binding.forceUpdateOverlay,
                "loadingHome" to binding.loadingHome,
                "progressGroup" to binding.progressGroup,
                "drawerBlur" to binding.ivDrawerGlassBlurBackground,
                "mainContent" to binding.mainContentContainer
            )
        )
    }

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized) {
            // Invalidate asynchronous composition callbacks when Home is no longer visible.
            bellAnimationGeneration++
            bellTapNavigationEpoch++
            bellTapNavigationPending = false
            pendingBellGeneration = null
            binding.btnNotification.cancelAnimation()
            restoreNotificationBellIdle()
            if (binding.cardFloatingAirplane.visibility == View.VISIBLE) {
                binding.lottieFloatingAirplane.pauseAnimation()
            }
        }
    }

    private fun checkAppConfigAndMaintenance() {
        lifecycleScope.launch(lifecycleExceptionHandler) {
            val config = AppConfigManager.fetchAppConfig()
            val email = FirebaseAuth.getInstance().currentUser?.email
            val isAdmin = email?.let { adminRepo.isAdmin(it) } ?: false

            // 1. Force update check (applies to all users)
            if (AppConfigManager.isUpdateRequired(config)) {
                binding.forceUpdateOverlay.visibility = View.VISIBLE
                binding.maintenanceOverlay.visibility = View.GONE
                binding.tvForceUpdateMessage.text =
                    "A newer version (v${config.minimum_supported_version_code}+) is required to continue. Your current version is ${BuildConfig.VERSION_CODE}."
                binding.btnForceUpdateNow.setOnClickListener {
                    AppConfigManager.openUpdateLink(this@MainActivity)
                }
                return@launch
            } else {
                binding.forceUpdateOverlay.visibility = View.GONE
            }

            // 2. Maintenance mode check (admins can bypass)
            if (AppConfigManager.isMaintenanceActive(config)) {
                if (!isAdmin) {
                    binding.maintenanceOverlay.visibility = View.VISIBLE
                    binding.tvMaintenanceMessage.text = config.maintenance_message
                    binding.btnCheckMaintenanceAgain.setOnClickListener {
                        checkAppConfigAndMaintenance()
                    }
                } else {
                    binding.maintenanceOverlay.visibility = View.GONE
                }
            } else {
                binding.maintenanceOverlay.visibility = View.GONE
            }
        }
    }

    /**
     * Recreate the active search surface without replaying entrance motion or forcing
     * the keyboard. Android owns restored IME/window focus; the query is immediately
     * available to the next Home state render rather than waiting for debounce.
     */
    private fun restoreSearchPresentation(savedInstanceState: Bundle?) {
        if (savedInstanceState?.getBoolean("home_search_active") != true) return
        isSearchActive = true
        homeListPositionBeforeSearch = savedInstanceState.getParcelable("home_search_list_position")
        val query = savedInstanceState.getString("home_search_query").orEmpty()
        binding.etSearch.setText(query)
        binding.etSearch.setSelection(binding.etSearch.text?.length ?: 0)
        searchDebounceJob?.cancel()
        currentSearchQuery = query.trim()
        binding.btnClearSearch.visibility = if (currentSearchQuery.isEmpty()) View.GONE else View.VISIBLE
        binding.normalTopBar.visibility = View.GONE
        binding.normalTopBar.alpha = 1f
        binding.homeGlassToolbar.alpha = 0f
        binding.titleGroup.visibility = View.GONE
        binding.searchTopBar.visibility = View.VISIBLE
        binding.searchTopBar.alpha = 1f
        binding.btnSearchBack.alpha = 1f
        binding.etSearch.alpha = 1f
        binding.chipGroupCategory.visibility = View.GONE
        refreshSearchChrome()
    }

    /** UI-only related suggestions from real EVE exams; never fabricated entries. */
    private fun showSearchSuggestions(query: String): Boolean {
        val list = binding.searchSuggestionsList
        list.removeAllViews()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val recent = HomeSearchHistory.load(this, uid)
        val phrases = if (query.isEmpty()) recent else lastSearchItems
            .filterIsInstance<HomeListItem.ExamRow>()
            .map { it.exam.examName }
            .distinct()
            .filter { HomeSearchText.matches(query, it) }
            .take(3)
        if (phrases.isEmpty()) {
            list.visibility = View.GONE
            return false
        }
        list.visibility = View.VISIBLE
        fun row(title: String, onClick: (() -> Unit)? = null): android.widget.TextView =
            android.widget.TextView(this).apply {
                text = title
                com.eve.app.designsystem.DesignType.apply(this,
                    if (onClick == null) com.eve.app.designsystem.DesignType.Role.SECONDARY
                    else com.eve.app.designsystem.DesignType.Role.BODY)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                val inset = (16 * resources.displayMetrics.density).toInt()
                setPadding(inset, 0, inset, 0)
                minHeight = (48 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.CENTER_VERTICAL
                if (onClick != null) {
                    isClickable = true
                    isFocusable = true
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener { onClick() }
                }
            }
        list.addView(row(if (query.isEmpty()) "Recent searches" else "Related exams"))
        phrases.forEach { phrase ->
            list.addView(row(phrase) {
                binding.etSearch.setText(phrase)
                binding.etSearch.setSelection(binding.etSearch.text?.length ?: 0)
                currentSearchQuery = phrase
                HomeSearchHistory.record(this, uid, phrase)
                applyCurrentList()
            })
        }
        if (query.isEmpty()) {
            list.addView(row("Clear All") {
                HomeSearchHistory.clear(this, uid)
                applyCurrentList()
            })
        }
        return true
    }

    private fun refreshSearchChrome() {
        binding.panelHomeBanner.visibility =
            if (!isSearchActive && homeBanners.isNotEmpty()) View.VISIBLE else View.GONE
        // The admin-only action is presentation-hidden in search, never revoked.
        // Reopen its original authorized state when closing or restoring search.
        binding.btnAdmin.visibility =
            if (!isSearchActive && isAdminUser) View.VISIBLE else View.GONE
    }

    private fun setupSearch() {
        binding.btnSearch.setOnClickListener {
            openSearch()
        }

        binding.btnSearchBack.setOnClickListener {
            closeSearch()
        }

        binding.btnClearSearch.setOnClickListener {
            if (binding.etSearch.text.isNullOrEmpty()) {
                closeSearch()
            } else {
                binding.etSearch.text?.clear()
            }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            val submitted = actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == android.view.KeyEvent.KEYCODE_ENTER
            if (!isSearchActive || !submitted) {
                false
            } else {
                val query = HomeSearchHistory.normalize(binding.etSearch.text?.toString().orEmpty())
                if (query.isNotEmpty()) {
                    HomeSearchHistory.record(this, FirebaseAuth.getInstance().currentUser?.uid.orEmpty(), query)
                    currentSearchQuery = query
                    applyCurrentList()
                }
                true
            }
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                searchDebounceJob?.cancel()
                searchDebounceJob = lifecycleScope.launch(lifecycleExceptionHandler) {
                    delay(250)
                    currentSearchQuery = query
                    applyCurrentList()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun cancelSearchVisualAnimations() {
        // Unauthenticated onCreate exits before binding inflation; onDestroy still runs.
        if (!::binding.isInitialized) return
        // An interrupted ViewPropertyAnimator must not commit a stale end action.
        binding.normalTopBar.animate().cancel()
        binding.homeGlassToolbar.animate().cancel()
        binding.btnSearchBack.animate().cancel()
        binding.etSearch.animate().cancel()
    }

    /**
     * Telegram ActionBarMenuItem.java:935-1041 (approved revision):
     * a 150ms AnimatorSet crossfades searchContainer against menu icons.
     * EVE's two toolbar groups are host substitutions, NOT a byte-exact port.
     */
    private fun startSearchAlphaTransition(opening: Boolean) {
        searchTransitionAnimator?.cancel()
        // Original ActionBarMenuItem.java:950-1031 crossfades ONLY menu icon
        // views against searchContainer. The EVE title is a separate Home row
        // and must not be faded as a giant parent (it overlaps search text and
        // changes the header height at the END of the old animation).
        val menuIcons = binding.homeGlassToolbar
        val search = binding.searchTopBar
        binding.normalTopBar.alpha = 1f
        if (opening) {
            binding.titleGroup.visibility = View.GONE
            binding.normalTopBar.visibility = View.VISIBLE
            if (search.visibility != View.VISIBLE) search.alpha = 0f
            search.visibility = View.VISIBLE
        } else {
            if (binding.normalTopBar.visibility != View.VISIBLE) menuIcons.alpha = 0f
            binding.normalTopBar.visibility = View.VISIBLE
            binding.titleGroup.visibility = View.VISIBLE
        }

        val animator = android.animation.AnimatorSet().apply {
            playTogether(
                android.animation.ObjectAnimator.ofFloat(
                    search, View.ALPHA, search.alpha, if (opening) 1f else 0f
                ),
                android.animation.ObjectAnimator.ofFloat(
                    menuIcons, View.ALPHA, menuIcons.alpha, if (opening) 0f else 1f
                )
            )
            duration = 150L
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (cancelled || isSearchActive != opening) return
                    if (opening) {
                        binding.normalTopBar.visibility = View.GONE
                        menuIcons.alpha = 0f
                        search.alpha = 1f
                    } else {
                        search.visibility = View.GONE
                        search.alpha = 0f
                        menuIcons.alpha = 1f
                    }
                    if (searchTransitionAnimator === animation) searchTransitionAnimator = null
                }
            })
        }
        searchTransitionAnimator = animator
        animator.start()
    }

    private fun openSearch() {
        if (isSearchActive) return
        homeListPositionBeforeSearch = binding.rvExams.layoutManager?.onSaveInstanceState()
        isSearchActive = true
        refreshSearchChrome()
        cancelSearchVisualAnimations()
        searchTransitionAnimator?.cancel()
        // Telegram's verified search toggle has no width spring. Keep weighted layout without redundant relayout.
        val lp = binding.layoutSearchFieldContainer.layoutParams as android.widget.LinearLayout.LayoutParams
        if (lp.width != 0 || lp.weight != 1f) {
            lp.width = 0
            lp.weight = 1f
            binding.layoutSearchFieldContainer.layoutParams = lp
        }
        binding.btnSearchBack.alpha = 1f
        binding.etSearch.alpha = 1f
        if (!UiArcMotion.isMotionEnabled(this)) {
            binding.titleGroup.visibility = View.GONE
            binding.normalTopBar.visibility = View.GONE
            binding.normalTopBar.alpha = 1f
            binding.homeGlassToolbar.alpha = 0f
            binding.searchTopBar.visibility = View.VISIBLE
            binding.searchTopBar.alpha = 1f
        } else {
            startSearchAlphaTransition(opening = true)
        }
        binding.etSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        binding.chipGroupCategory.visibility = View.GONE
        applyCurrentList()
    }

    private fun closeSearch() {
        if (!isSearchActive) return
        isSearchActive = false
        refreshSearchChrome()
        binding.searchSuggestionsList.visibility = View.GONE
        cancelSearchVisualAnimations()
        searchTransitionAnimator?.cancel()
        binding.etSearch.text?.clear()
        // Original Telegram ActionBarMenuItem.java:986 clears search focus on
        // collapse; hiding IME alone can leave focus on an invisible input.
        binding.etSearch.clearFocus()
        searchDebounceJob?.cancel()
        currentSearchQuery = ""
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        if (!UiArcMotion.isMotionEnabled(this)) {
            binding.searchTopBar.visibility = View.GONE
            binding.searchTopBar.alpha = 0f
            binding.normalTopBar.visibility = View.VISIBLE
            binding.normalTopBar.alpha = 1f
            binding.homeGlassToolbar.alpha = 1f
            binding.titleGroup.visibility = View.VISIBLE
        } else {
            startSearchAlphaTransition(opening = false)
        }
        val data = (viewModel.state.value as? UiState.Success)?.data
        if (data != null && data.categories.size > 1) {
            binding.chipGroupCategory.visibility = View.VISIBLE
        }
        applyCurrentList()
        val savedListPosition = homeListPositionBeforeSearch
        homeListPositionBeforeSearch = null
        if (savedListPosition != null) binding.rvExams.post {
            if (!isSearchActive) binding.rvExams.layoutManager?.onRestoreInstanceState(savedListPosition)
        }
    }

    private fun applyCurrentList() {
        refreshSearchChrome()
        if (isSearchActive && currentSearchQuery.isEmpty()) {
            adapter.submit(emptyList())
            binding.rvExams.visibility = View.GONE
            binding.btnRetry.visibility = View.GONE
            val hasRecent = showSearchSuggestions("")
            binding.messageGroup.visibility = if (hasRecent) View.GONE else View.VISIBLE
            binding.ivMessageIcon.visibility = View.GONE
            binding.tvMessage.text = "Search exams"
            binding.tvMessageSub.text = "Type an exam name or category to see results."
            return
        }
        binding.searchSuggestionsList.visibility = View.GONE
        binding.rvExams.visibility = View.VISIBLE
        binding.ivMessageIcon.visibility = View.VISIBLE
        if (isSearchActive && currentSearchQuery.isNotEmpty()) {
            showSearchSuggestions(currentSearchQuery)
            val q = currentSearchQuery
            val filtered = lastSearchItems.filter { item ->
                when (item) {
                    is HomeListItem.ExamRow ->
                        HomeSearchText.matches(q, item.exam.examName, item.exam.categoryOrOther)
                    is HomeListItem.FeedbackPostRow ->
                        HomeSearchText.matches(q, item.post.title, item.post.message)
                    is HomeListItem.Header -> false
                }
            }

            if (filtered.isEmpty()) {
                adapter.submit(emptyList())
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.ivMessageIcon, hasEmptyPlayed)
                binding.tvMessage.text = "No matching items"
                binding.tvMessageSub.text = "Try searching for a different keyword or category."
            } else {
                binding.messageGroup.visibility = View.GONE
                adapter.submit(filtered)
            }
        } else {
            val hasContent = lastLoadedItems.any { it is HomeListItem.ExamRow || it is HomeListItem.FeedbackPostRow }
            val currentState = viewModel.state.value
            if (!hasContent) {
                if (currentState is UiState.Loading) {
                    binding.messageGroup.visibility = View.GONE
                } else if (currentState is UiState.Success) {
                    adapter.submit(emptyList())
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.GONE
                    hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.ivMessageIcon, hasEmptyPlayed)
                    binding.tvMessage.text = "No exams available"
                    binding.tvMessageSub.text = "Check back soon for newly published mock tests."
                }
            } else {
                binding.messageGroup.visibility = View.GONE
                adapter.submit(lastLoadedItems)
            }
        }
    }

    private fun showPinPopupMenu(exam: Exam, isPinned: Boolean, anchorView: View) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val popup = PopupMenu(this, anchorView)
        val title = if (isPinned) "Unpin" else "Pin"
        popup.menu.add(title)
        popup.setOnMenuItemClickListener {
            viewModel.togglePin(user.uid, exam.id, isPinned)
            // Task B: Fire exactly one haptic event per pin and per unpin action
            com.eve.app.util.VibrationHelper.vibrateLightHaptic(this)
            val msg = if (isPinned) "Test unpinned" else "Test pinned to top"
            com.eve.app.util.AppBulletin.showSuccess(this, msg)
            true
        }
        com.eve.app.designsystem.DesignPopupGlass.around(anchorView) { popup.show() }
    }

    private fun openFeedbackForPost(post: com.eve.app.data.model.FeedbackPost) {
        val intent = Intent(this, com.eve.app.ui.feedback.FeedbackActivity::class.java).apply {
            putExtra("post_id", post.id)
            putExtra("post_title", post.title)
        }
        startActivity(intent)
    }

    private val adminDeleteFlow by lazy { com.eve.app.ui.admin.AdminDeleteFlow(this) }

    private fun handleFeedbackPostLongClick(post: com.eve.app.data.model.FeedbackPost) {
        val email = FirebaseAuth.getInstance().currentUser?.email

        lifecycleScope.launch(lifecycleExceptionHandler) {
            // Original AlertDialog.show() checks AndroidUtilities.isSafeToShow:
            // AndroidUtilities.java:6255-6267 => !isDestroyed && !isFinishing.
            // The admin lookup is asynchronous; never open a window during teardown.
            if (adminRepo.isAdmin(email) && !isDestroyed && !isFinishing) {
                com.eve.app.designsystem.TelegramNativeAlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Feedback Post?")
                    .setMessage("Are you sure you want to delete '${post.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        adminDeleteFlow.removeConfirmed("feedback:${post.id}", { viewModel.deleteFeedbackPostConfirmed(post.id); null })
                    }
                    .setNegativeButton("Cancel", null)
                    .makeRed(android.content.DialogInterface.BUTTON_POSITIVE)
                    .show()
            }
        }
    }

    private var isReturningFromStopped = false

    override fun onStart() {
        super.onStart()
        val returningFromStop = isReturningFromStopped
        if (returningFromStop) {
            isReturningFromStopped = false
            adapter.resetPlayedAnimations()
            if (::binding.isInitialized) {
                binding.rvExams.post {
                    adapter.replayVisible(binding.rvExams)
                }
            }
        }
        if (::binding.isInitialized) {
            FirebaseAuth.getInstance().currentUser?.let { current ->
                // onCreate already set the text and started the one-time tracking intro.
                // Never reset it during this Activity's FIRST onStart().
                if (returningFromStop) {
                    binding.tvWelcome.text = "Hi, ${current.displayName ?: "Student"}"
                    HomeGreetingMotion.settleNatural(binding.tvWelcome)
                }
                updateDrawerHeader(current)
                lifecycleScope.launch(lifecycleExceptionHandler) {
                    val admin = adminRepo.isAdmin(current.email)
                    viewModel.loadForUser(current.uid, admin)
                }
            }
            FirebaseAuth.getInstance().currentUser?.let { loadProfilePhoto(it) }

            updateNotificationDot()

            startListeningToNotifications()
            startListeningToHomeBanner()

            refreshFloatingLink()
        }
    }

    override fun onStop() {
        super.onStop()
        isReturningFromStopped = true
        greetingIntroAnimator?.cancel()
        greetingIntroAnimator = null
        if (::binding.isInitialized) binding.tvWelcome.animate().cancel()
        notifJob?.cancel()
        notifJob = null
        stopBannerAutoScroll()
        bannerObserverJob?.cancel()
        bannerObserverJob = null
        if (::binding.isInitialized && binding.cardFloatingAirplane.visibility == View.VISIBLE) {
            binding.lottieFloatingAirplane.pauseAnimation()
        }
    }

    private fun setupFloatingAirplane() {
        binding.cardFloatingAirplane.strokeWidth = 0
        binding.cardFloatingAirplane.strokeColor = Color.TRANSPARENT
        binding.cardFloatingAirplane.cardElevation = 0f
        binding.cardFloatingAirplane.maxCardElevation = 0f
        binding.cardFloatingAirplane.outlineProvider = null
        binding.cardFloatingAirplane.background = null
        binding.cardFloatingAirplane.foreground = null
        binding.cardFloatingAirplane.setCardBackgroundColor(
            ContextCompat.getColor(this, android.R.color.transparent)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.cardFloatingAirplane.outlineAmbientShadowColor = Color.TRANSPARENT
            binding.cardFloatingAirplane.outlineSpotShadowColor = Color.TRANSPARENT
        }
        applyFloatingAirplaneTheme()

        ViewCompat.setOnApplyWindowInsetsListener(binding.cardFloatingAirplane) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            safeInsetTop = bars.top
            safeInsetBottom = bars.bottom
            safeInsetLeft = bars.left
            safeInsetRight = bars.right

            val gestures = windowInsets.getInsets(WindowInsetsCompat.Type.systemGestures())
            val baseMarginPx = resources.getDimensionPixelSize(R.dimen.design_screen_margin)
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                val bottom = maxOf(bars.bottom, gestures.bottom) + baseMarginPx
                val end = maxOf(bars.right, baseMarginPx)
                if (lp.bottomMargin != bottom || lp.marginEnd != end) {
                    lp.bottomMargin = bottom
                    lp.marginEnd = end
                    view.layoutParams = lp
                }
            }
            val ime = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            val hasLink = com.eve.app.data.repository.FloatingLinkRepository.isValidHttpsUrl(
                activeFloatingLinkUrl.orEmpty()
            )
            view.visibility = when {
                !hasLink -> View.GONE
                ime -> View.INVISIBLE // Keep receiving insets when the keyboard closes.
                else -> View.VISIBLE
            }
            // INVISIBLE does not stop a Lottie animation. Avoid invisible work while typing;
            // resume the existing animation only when the window is active again.
            if (ime && binding.lottieFloatingAirplane.isAnimating) {
                binding.lottieFloatingAirplane.pauseAnimation()
            } else if (!ime && hasLink &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                com.eve.app.designsystem.DesignMotion.enabled(this) &&
                !binding.lottieFloatingAirplane.isAnimating
            ) {
                binding.lottieFloatingAirplane.resumeAnimation()
            }
            windowInsets
        }

        setupFloatingAirplaneInteraction()
    }

    private fun setupFloatingAirplaneInteraction() {
        val card = binding.cardFloatingAirplane
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        val bounce = ButtonBounce(card)
        val longPressHandler = Handler(Looper.getMainLooper())
        var isDragging = false
        var isLongPressScheduled = false
        var downRawX = 0f
        var downRawY = 0f
        var startTransX = 0f
        var startTransY = 0f
        var lastLinkLaunch = 0L

        // A real View click callback is required for TalkBack/keyboard ACTION_CLICK.
        // Both student and admin activate the same link without entering drag mode.
        card.setOnClickListener {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastLinkLaunch >= 800L) {
                lastLinkLaunch = now
                openFloatingLink()
            }
        }

        card.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val data = activeFloatingLinkData
            if (data?.x != null && data?.y != null) {
                applyFloatingAirplanePosition(data.x, data.y)
            }
        }

        val longPressRunnable = Runnable {
            if (!isFloatingAirplaneAdmin || floatingLinkPositionSaveInFlight) return@Runnable
            // Do not let a slower public GET started at Home resume undo drag.
            floatingLinkFetchGeneration++
            isDragging = true
            isLongPressScheduled = false
            bounce.setPressed(false)
            card.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            card.parent?.requestDisallowInterceptTouchEvent(true)
            card.animate()
                .scaleX(1.10f)
                .scaleY(1.10f)
                .alpha(0.92f)
                .setDuration(160)
                .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                .start()
        }

        card.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startTransX = view.translationX
                    startTransY = view.translationY
                    isDragging = false

                    bounce.setPressed(true)

                    if (isFloatingAirplaneAdmin && !floatingLinkPositionSaveInFlight) {
                        isLongPressScheduled = true
                        longPressHandler.postDelayed(longPressRunnable, 400L)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

                    if (!isDragging) {
                        if (distance > slop) {
                            if (isLongPressScheduled) {
                                longPressHandler.removeCallbacks(longPressRunnable)
                                isLongPressScheduled = false
                            }
                            bounce.setPressed(false)
                        }
                    } else {
                        val parent = view.parent as? ViewGroup
                        if (parent != null && parent.width > 0 && parent.height > 0) {
                            val marginPx = resources.getDimensionPixelSize(R.dimen.design_screen_margin)
                            val cardW = view.width.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.floating_link_icon_size)
                            val cardH = view.height.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.floating_link_icon_size)

                            val minX = (safeInsetLeft + marginPx).toFloat()
                            val maxX = (parent.width - safeInsetRight - cardW - marginPx).toFloat().coerceAtLeast(minX)
                            val minY = (safeInsetTop + marginPx).toFloat()
                            val maxY = (parent.height - safeInsetBottom - cardH - marginPx).toFloat().coerceAtLeast(minY)

                            val newX = (view.left + startTransX + dx).coerceIn(minX, maxX)
                            val newY = (view.top + startTransY + dy).coerceIn(minY, maxY)

                            view.translationX = newX - view.left
                            view.translationY = newY - view.top
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isLongPressScheduled) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                        isLongPressScheduled = false
                    }
                    if (isDragging) {
                        isDragging = false
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                        view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(180)
                            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                            .start()

                        val parent = view.parent as? ViewGroup
                        if (parent != null && parent.width > 0 && parent.height > 0) {
                            val marginPx = resources.getDimensionPixelSize(R.dimen.design_screen_margin)
                            val cardW = view.width.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.floating_link_icon_size)
                            val cardH = view.height.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.floating_link_icon_size)

                            val minX = (safeInsetLeft + marginPx).toFloat()
                            val maxX = (parent.width - safeInsetRight - cardW - marginPx).toFloat().coerceAtLeast(minX)
                            val minY = (safeInsetTop + marginPx).toFloat()
                            val maxY = (parent.height - safeInsetBottom - cardH - marginPx).toFloat().coerceAtLeast(minY)

                            val currentX = view.left + view.translationX
                            val currentY = view.top + view.translationY

                            val availableW = maxX - minX
                            val availableH = maxY - minY
                            val normX = if (availableW > 0) ((currentX - minX) / availableW).coerceIn(0f, 1f) else 0.5f
                            val normY = if (availableH > 0) ((currentY - minY) / availableH).coerceIn(0f, 1f) else 0.5f

                            // Presentation moves optimistically during drag, but a
                            // failed admin write must not leave local icon position
                            // diverged from the last server-confirmed coordinates.
                            val previous = activeFloatingLinkData
                            floatingLinkPositionSaveInFlight = true
                            lifecycleScope.launch(lifecycleExceptionHandler) {
                                try {
                                    val result = floatingLinkRepo.updateFloatingLinkPosition(normX, normY)
                                    if (result.isSuccess) {
                                        activeFloatingLinkData = FloatingLinkData(
                                            url = activeFloatingLinkUrl.orEmpty(),
                                            x = normX,
                                            y = normY
                                        )
                                    } else {
                                        applyFloatingAirplanePosition(previous?.x, previous?.y)
                                        AppBulletin.showError(this@MainActivity, "Couldn't save position")
                                    }
                                } finally {
                                    floatingLinkPositionSaveInFlight = false
                                }
                            }
                        }
                    } else {
                        // Regular tap (both admin and non-admin)
                        bounce.setPressed(false)
                        val dx = event.rawX - downRawX
                        val dy = event.rawY - downRawY
                        val distance = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        if (distance <= slop) {
                            // Dispatch the same click as accessibility and keyboard
                            // activation; never launch the link after an admin drag.
                            view.performClick()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (isLongPressScheduled) {
                        longPressHandler.removeCallbacks(longPressRunnable)
                        isLongPressScheduled = false
                    }
                    bounce.setPressed(false)
                    if (isDragging) {
                        isDragging = false
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                        view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(180)
                            .setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT)
                            .start()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun openFloatingLink() {
        val url = activeFloatingLinkUrl?.trim()
        if (url.isNullOrEmpty() || !com.eve.app.data.repository.FloatingLinkRepository.isValidHttpsUrl(url)) {
            AppBulletin.showError(this, "No valid link configured")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (_: android.content.ActivityNotFoundException) {
            AppBulletin.showError(this, "No app is available to open this link.")
        } catch (_: Exception) {
            AppBulletin.showError(this, "Couldn't open link")
        }
    }

    private fun applyFloatingAirplanePosition(normX: Float?, normY: Float?) {
        val card = binding.cardFloatingAirplane
        if (normX == null || normY == null) {
            card.translationX = 0f
            card.translationY = 0f
            return
        }
        val applyCoords = {
            val parent = card.parent as? ViewGroup
            if (parent != null && parent.width > 0 && parent.height > 0) {
                val marginPx = resources.getDimensionPixelSize(R.dimen.design_screen_margin)
                val cardW = card.width.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.floating_link_icon_size)
                val cardH = card.height.takeIf { it > 0 } ?: resources.getDimensionPixelSize(R.dimen.floating_link_icon_size)

                val minX = (safeInsetLeft + marginPx).toFloat()
                val maxX = (parent.width - safeInsetRight - cardW - marginPx).toFloat().coerceAtLeast(minX)
                val minY = (safeInsetTop + marginPx).toFloat()
                val maxY = (parent.height - safeInsetBottom - cardH - marginPx).toFloat().coerceAtLeast(minY)

                val targetX = minX + normX.coerceIn(0f, 1f) * (maxX - minX)
                val targetY = minY + normY.coerceIn(0f, 1f) * (maxY - minY)

                if (card.left > 0 || card.top > 0) {
                    card.translationX = targetX - card.left
                    card.translationY = targetY - card.top
                }
            }
        }
        if (card.isLaidOut && (card.left > 0 || card.top > 0)) {
            applyCoords()
        } else {
            card.post { applyCoords() }
        }
    }

    // Both onStart and onResume can initiate a request. Prevent an older,
    // slower response from overwriting the freshest position on Home.
    private var floatingLinkFetchGeneration = 0

    private fun refreshFloatingLink(forceRefresh: Boolean = false) {
        val generation = ++floatingLinkFetchGeneration
        lifecycleScope.launch(lifecycleExceptionHandler) {
            val data = floatingLinkRepo.getFloatingLinkData(forceRefresh = forceRefresh)
            if (generation != floatingLinkFetchGeneration ||
                isDestroyed || isFinishing || !::binding.isInitialized) return@launch
            activeFloatingLinkData = data
            val link = data?.url
            activeFloatingLinkUrl = link
            updateFloatingAirplaneState(link)
            applyFloatingAirplanePosition(data?.x, data?.y)
        }
    }

    private fun applyFloatingAirplaneTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val keyPathAll = KeyPath("**")

        // Preserve the existing artwork palette through shared lookup tokens.
        val targetColor = if (isDark) {
            ContextCompat.getColor(this, R.color.text_primary)
        } else {
            ContextCompat.getColor(this, R.color.hairline)
        }

        val callback = SimpleLottieValueCallback<Int> { frameInfo ->
            val orig = frameInfo?.startValue ?: return@SimpleLottieValueCallback 0
            com.eve.app.designsystem.DesignLottieTokens.airplaneColor(orig, isDark, targetColor)
        }

        binding.lottieFloatingAirplane.addValueCallback(keyPathAll, LottieProperty.COLOR, callback)
        binding.lottieFloatingAirplane.addValueCallback(keyPathAll, LottieProperty.STROKE_COLOR, callback)
    }

    private fun updateFloatingAirplaneState(link: String?) {
        val valid = com.eve.app.data.repository.FloatingLinkRepository.isValidHttpsUrl(link.orEmpty())
        val imeVisible = ViewCompat.getRootWindowInsets(binding.cardFloatingAirplane)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        binding.cardFloatingAirplane.visibility = when {
            !valid -> View.GONE
            imeVisible -> View.INVISIBLE
            else -> View.VISIBLE
        }
        when {
            !valid -> binding.lottieFloatingAirplane.cancelAnimation()
            imeVisible || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                if (binding.lottieFloatingAirplane.isAnimating) binding.lottieFloatingAirplane.pauseAnimation()
            }
            com.eve.app.designsystem.DesignMotion.enabled(this) &&
                !binding.lottieFloatingAirplane.isAnimating -> binding.lottieFloatingAirplane.resumeAnimation()
        }
    }

    private fun setupBannerCarousel() {
        binding.vpHomeBanners.clipChildren = false
        binding.vpHomeBanners.clipToPadding = false
        (binding.vpHomeBanners.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.apply {
            clipChildren = false
            clipToPadding = false
        }
        binding.vpHomeBanners.adapter = bannerAdapter
        binding.vpHomeBanners.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                renderHomeBannerCta(position)
            }
            override fun onPageScrollStateChanged(state: Int) {
                isUserDraggingBanner = (state == androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_DRAGGING)
            }
        })
    }

    private fun startListeningToHomeBanner() {
        bannerObserverJob?.cancel()
        bannerObserverJob = lifecycleScope.launch(lifecycleExceptionHandler) {
            bannerRepo.observeBanners().collect { banners ->
                homeBanners = banners
                currentBannerCount = banners.size
                if (banners.isEmpty()) {
                    binding.panelHomeBanner.visibility = View.GONE
                    binding.btnHomeBannerCta.visibility = View.GONE
                    bannerAdapter.submitList(emptyList())
                    stopBannerAutoScroll()
                } else {
                    binding.panelHomeBanner.visibility = if (isSearchActive) View.GONE else View.VISIBLE
                    bannerAdapter.submitList(banners)
                    val currentPage = binding.vpHomeBanners.currentItem.coerceIn(0, banners.lastIndex)
                    binding.vpHomeBanners.setCurrentItem(currentPage, false)
                    binding.vpHomeBanners.isUserInputEnabled = banners.size > 1
                    renderHomeBannerCta(currentPage)
                    startBannerAutoScroll(banners.size)
                }
            }
        }
    }

    private fun startBannerAutoScroll(count: Int) {
        stopBannerAutoScroll()
        if (count <= 1) return
        bannerAutoScrollJob = lifecycleScope.launch(lifecycleExceptionHandler) {
            while (isActive) {
                delay(4500L)
                if (!isUserDraggingBanner && currentBannerCount > 1) {
                    val nextItem = (binding.vpHomeBanners.currentItem + 1) % currentBannerCount
                    binding.vpHomeBanners.setCurrentItem(nextItem, true)
                }
            }
        }
    }

    private fun stopBannerAutoScroll() {
        bannerAutoScrollJob?.cancel()
        bannerAutoScrollJob = null
    }

    private fun startListeningToNotifications() {
        notifJob?.cancel()
        notifJob = lifecycleScope.launch(lifecycleExceptionHandler) {
            try {
                val res = com.eve.app.data.remote.ApiClient.apiService.getBroadcasts(50)
                if (res.success) {
                    // An empty successful response still establishes the
                    // initial sync baseline for subsequent live notifications.
                    // Merge all available new broadcasts; never replace FCM/reminder entries
                    // or mark the Home screen's unread notifications as acknowledged.
                    val hadBroadcastBaseline =
                        NotificationStore.hasEstablishedBroadcastBaseline(this@MainActivity)
                    val newCount = NotificationStore.importBroadcasts(this@MainActivity, res.data.orEmpty())
                    if (newCount > 0 && hadBroadcastBaseline) {
                        // First sync backfills old posts; only subsequent new IDs
                        // qualify for the incoming notification animation.
                        animateNotificationBell()
                    }
                }
                updateNotificationDot()
            } catch (_: Exception) {
                // Keep locally cached unread state when network retrieval fails.
                updateNotificationDot()
            }
        }
    }

    private fun updateNotificationDot() {
        binding.dotUnread.visibility = if (NotificationStore.hasUnread(this)) View.VISIBLE else View.GONE
    }

    private fun restoreNotificationBellIdle() {
        if (!::binding.isInitialized) return
        binding.btnNotification.alpha = 0f
        binding.ivNotificationIdle.visibility = View.VISIBLE
    }

    private var bellAnimationGeneration = 0
    private var pendingBellGeneration: Int? = null
    // A broadcast/remote import can arrive during STARTED, before ON_RESUME.
    // Queue only that real event; Home/inbox navigation never sets this flag.
    private var pendingIncomingBellUntilResume = false
    // A tap is navigation, not an incoming notification; keep its lifecycle state separate.
    private var bellTapNavigationPending = false
    private var bellTapNavigationEpoch = 0

    /** Original EVE asset: notification_bell.json (Git blob bff3b04887e06d0f144a641b8ec5c14f640c4dd4),
     * present since the historical e9bad22a commit. This is not an Apple asset or
     * a claim of official iOS animation provenance. Motion is triggered only by
     * incoming NotificationStore events, newly imported broadcasts, or a real user tap. */
    private fun animateNotificationBell() {
        if (!::binding.isInitialized || !com.eve.app.designsystem.DesignMotion.enabled(this)) {
            pendingIncomingBellUntilResume = false
            restoreNotificationBellIdle()
            return
        }
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            pendingIncomingBellUntilResume = true
            restoreNotificationBellIdle()
            return
        }
        pendingIncomingBellUntilResume = false
        val generation = ++bellAnimationGeneration
        if (binding.btnNotification.composition == null) {
            // One registered composition listener consumes only the latest event.
            // No unbounded listener per notification or duplicate playback.
            pendingBellGeneration = generation
        } else {
            startNotificationBellPlayback(generation)
        }
    }

    private fun startNotificationBellPlayback(generation: Int) {
        if (generation != bellAnimationGeneration || !::binding.isInitialized ||
            !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val bell = binding.btnNotification
        if (bell.composition == null) {
            pendingBellGeneration = generation
            return
        }
        try {
            bell.cancelAnimation()
            bell.repeatCount = 0
            bell.progress = 0f
            binding.ivNotificationIdle.visibility = View.INVISIBLE
            bell.alpha = 1f
            bell.playAnimation()
            if (!bell.isAnimating) {
                restoreNotificationBellIdle()
                return
            }
            // The original 140-frame/60fps Lottie lasts about 2333ms.
            // The old fixed 2200ms watchdog cut off its final frames.
            val safetyMs = ((bell.composition?.duration?.toLong() ?: 2400L) + 300L)
                .coerceIn(2500L, 5000L)
            bell.postDelayed({
                if (::binding.isInitialized && generation == bellAnimationGeneration) {
                    bell.cancelAnimation()
                    restoreNotificationBellIdle()
                }
            }, safetyMs)
        } catch (_: Exception) {
            restoreNotificationBellIdle()
        }
    }

    private fun setupNotificationBell() {
        HomeAppearance.tintNotificationBell(binding.btnNotification)
        binding.btnNotification.addLottieOnCompositionLoadedListener {
            val generation = pendingBellGeneration ?: return@addLottieOnCompositionLoadedListener
            pendingBellGeneration = null
            if (generation == bellAnimationGeneration &&
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                startNotificationBellPlayback(generation)
            }
        }
        restoreNotificationBellIdle()
        binding.btnNotification.addAnimatorListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                restoreNotificationBellIdle()
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {
                restoreNotificationBellIdle()
            }
        })
        binding.notificationAction.setOnClickListener {
            // A user tap must animate before navigation, without counting as new notification.
            // Reject double taps and invalidate delayed dispatch on Activity pause/recreation.
            if (bellTapNavigationPending || isFinishing || isDestroyed) return@setOnClickListener
            bellTapNavigationPending = true
            val tapEpoch = ++bellTapNavigationEpoch
            val openInbox = {
                if (tapEpoch == bellTapNavigationEpoch && !isFinishing && !isDestroyed &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    // Read flags are changed only inside NotificationsActivity.
                    com.eve.app.util.EveNavigationHelper.navigate(
                        this, Intent(this, NotificationsActivity::class.java), binding.notificationAction
                    )
                } else {
                    bellTapNavigationPending = false
                }
            }
            if (!com.eve.app.designsystem.DesignMotion.enabled(this) ||
                binding.btnNotification.composition == null ||
                !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                // Reduced motion, unloaded composition, or absent animation: never block the inbox.
                openInbox()
            } else {
                animateNotificationBell()
                if (binding.btnNotification.isAnimating) {
                    // Display a perceptible original-EVE animation, then enter inbox;
                    // onPause cancels the remainder, preventing an offscreen replay.
                    binding.notificationAction.postDelayed(openInbox, 850L)
                } else {
                    openInbox()
                }
            }
        }
    }

    private fun setupHomeBannerSurface() {
        com.eve.app.designsystem.EveDesignSystem.Components.applyHomeBannerSurface(
            binding.panelHomeBanner,
            binding.blurHomeBannerMaterial
        )
    }

    private fun setupTopBarDepth() {
        binding.topBarContainer.setBackgroundColor(getColor(R.color.transparent))
        binding.searchTopBar.setBackgroundColor(getColor(R.color.transparent))
        // Home only: scrolling enabled the blur and also drew a perimeter stroke.
        // Disable that unwanted hairline without touching shared toolbar behavior.
        binding.homeGlassToolbar.drawHairline = false
        // The screenshots show the scroll-activated #1F1F1F glass coating
        // against a pure-black Home canvas. The original source does not
        // authorize a conflicting extra Home-only strip; do not re-enable it
        // on every pre-draw or during the search alpha transition.
        binding.homeGlassToolbar.scrollBackdropEnabled = false
        binding.homeGlassToolbar.glassEnabled = false
        binding.homeGlassToolbar.setBackgroundColor(Color.TRANSPARENT)
        binding.homeGlassToolbar.invalidate()
    }

    private fun renderHomeBannerCta(position: Int) {
        val banner = homeBanners.getOrNull(position)
        val url = banner?.linkUrl?.trim().orEmpty()
        val label = banner?.linkLabel?.trim().orEmpty()
        if (banner == null || label.isBlank() || !HomeBannerLinkValidator.isValidUrl(url)) {
            binding.btnHomeBannerCta.visibility = View.GONE
            binding.btnHomeBannerCta.setOnClickListener(null)
            return
        }

        binding.btnHomeBannerCta.text = label
        binding.btnHomeBannerCta.visibility = View.VISIBLE
        binding.btnHomeBannerCta.setOnClickListener {
            if (!HomeBannerLinkValidator.isValidUrl(url)) return@setOnClickListener
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: android.content.ActivityNotFoundException) {
                AppBulletin.showError(this, "No app is available to open this link.")
            }
        }
    }

    override fun onDestroy() {
        // A stale popup window belongs to the old Activity. Its 150ms animated
        // dismiss must not outlive configuration/process teardown or run an action.
        homeOverflowPopup?.dismissForHostTeardown()
        homeOverflowPopup = null
        TelegramThemeEngine.removeThemeChangeListener(categoryPaletteListener)
        searchDebounceJob?.cancel()
        searchTransitionAnimator?.cancel()
        cancelSearchVisualAnimations()
        super.onDestroy()
        try {
            unregisterReceiver(foregroundNotificationReceiver)
        } catch (_: Exception) { }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
        outState.putBoolean(HomeGreetingMotion.KEY_HAS_ANIMATED_GREETING, hasAnimatedWelcome)
        if (::binding.isInitialized) {
            outState.putParcelable("theme_home_list_state", binding.rvExams.layoutManager?.onSaveInstanceState())
            outState.putBoolean("home_search_active", isSearchActive)
            if (isSearchActive) outState.putParcelable("home_search_list_position", homeListPositionBeforeSearch)
            if (isSearchActive) {
                // Save the actual input, not the last (possibly debounced) search result.
                outState.putString("home_search_query", binding.etSearch.text?.toString().orEmpty())
            }
        }
    }

    private fun loadProfilePhoto(user: com.google.firebase.auth.FirebaseUser?) {
        if (!::binding.isInitialized) return
        if (user == null) {
            binding.btnMenu.setImageResource(R.drawable.msg_openprofile)
            binding.btnMenu.imageTintList = ContextCompat.getColorStateList(this, R.color.telegram_type_primary)
            return
        }

        val effectiveName = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
        val effectiveId = user.uid.takeIf { it.isNotBlank() } ?: effectiveName
        val fallbackDrawable = AvatarDrawable.create(this, effectiveName, effectiveId)

        val localBmp = ProfilePhotoManager.loadBitmap(this)
        when {
            localBmp != null -> {
                binding.btnMenu.imageTintList = null
                val circularDrawable = RoundedBitmapDrawableFactory.create(resources, localBmp).apply {
                    isCircular = true
                }
                binding.btnMenu.setImageDrawable(circularDrawable)
            }
            !user.photoUrl?.toString().isNullOrBlank() -> {
                binding.btnMenu.imageTintList = null
                binding.btnMenu.load(user.photoUrl) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(fallbackDrawable)
                    error(fallbackDrawable)
                }
            }
            else -> {
                binding.btnMenu.imageTintList = null
                binding.btnMenu.setImageDrawable(fallbackDrawable)
            }
        }
    }

    private fun setupPushNotifications() {
        NotificationHelper.createChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            val prefs = getSharedPreferences("eve_app_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("notification_permission_requested", false)) {
                prefs.edit().putBoolean("notification_permission_requested", true).apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Subscribe to new exams topic AND all_users broadcast topic
        FirebaseMessaging.getInstance().subscribeToTopic(NotificationHelper.TOPIC_NEW_EXAMS)
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        ReminderScheduler.applySavedState(this)
    }

    private fun showAdminButton() {
        com.eve.app.designsystem.EveDesignSystem.Components.applyAdminButton(binding.btnAdmin)
        binding.btnAdmin.visibility = if (isSearchActive) View.GONE else View.VISIBLE
        binding.btnAdmin.setOnClickListener {
            com.eve.app.util.EveNavigationHelper.navigate(this, Intent(this, AdminActivity::class.java), binding.btnAdmin)
        }
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            CrashlyticsHelper.identify(uid, isAdmin = true)
        }
    }

    private fun render(state: UiState<HomeUiData>) {
        when (state) {
            is UiState.Loading -> {
                hasEmptyPlayed = false
                if (lastLoadedItems.isNotEmpty()) {
                    binding.loadingHome.visibility = View.GONE
                    binding.rvExams.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                } else {
                    binding.loadingHome.visibility = View.VISIBLE
                    binding.rvExams.visibility = View.GONE
                    binding.progressGroup.visibility = View.GONE
                    binding.messageGroup.visibility = View.GONE
                    binding.chipGroupCategory.visibility = View.GONE
                }
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                val data = state.data
                lastLoadedItems = data.items
                lastSearchItems = data.searchItems
                renderChips(data.categories, data.selectedCategory)
                applyCurrentList()
                if (restoreThemeContent) {
                    com.eve.app.designsystem.DesignLoading.showContent(binding.loadingHome, binding.rvExams)
                    binding.rvExams.layoutManager?.onRestoreInstanceState(themeListState)
                    themeListState = null
                    restoreThemeContent = false
                    // Keep the XML layout entrance suppressed for this restored content.
                    // Setting a controller again schedules another entrance on the next layout.
                } else {
                    com.eve.app.designsystem.DesignLoading.showContent(binding.loadingHome, binding.rvExams)
                }
                // Loading replacement can make an empty search list visible again.
                if (isSearchActive) applyCurrentList()
                binding.chipGroupCategory.visibility =
                    if (data.categories.size <= 1 || isSearchActive) View.GONE else View.VISIBLE

                if (!TargetExamsBottomSheet.isOnboardingDone(this@MainActivity)) {
                    // Onboarding must offer ALL published main exams, even if Home opens
                    // with a persisted category filter. Search index has no duplicate cards.
                    val mainExams = data.searchItems.filterIsInstance<HomeListItem.ExamRow>()
                        .map { it.exam }.filter { it.isMainExam }
                    if (mainExams.isNotEmpty()) {
                        TargetExamsBottomSheet.show(this@MainActivity, mainExams) {
                            viewModel.refreshTargetExams()
                        }
                    }
                }
            }
            is UiState.Error -> {
                hasEmptyPlayed = false
                if (lastLoadedItems.isNotEmpty()) {
                    binding.loadingHome.visibility = View.GONE
                    binding.rvExams.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                } else {
                    binding.loadingHome.visibility = View.GONE
                    binding.progressGroup.visibility = View.GONE
                    binding.messageGroup.visibility = View.VISIBLE
                    binding.btnRetry.visibility = View.VISIBLE
                    com.eve.app.util.EmptyStateAnimationHelper.showErrorState(binding.ivMessageIcon)
                    if (NetworkUtil.isOnline(this)) {
                        binding.tvMessage.text = "Something went wrong"
                        binding.tvMessageSub.text = state.message
                    } else {
                        binding.tvMessage.text = "No internet connection"
                        binding.tvMessageSub.text =
                            "Exams not cached yet. Please retry once back online."
                    }
                    binding.chipGroupCategory.visibility = View.GONE
                }
            }
        }
    }

    private val categoryPaletteListener: (TelegramThemeEngine.ResolvedPalette) -> Unit = {
        if (::binding.isInitialized && !isFinishing && !isDestroyed) {
            runOnUiThread { refreshInstantiatedCategoryChips() }
        }
    }

    private fun refreshInstantiatedCategoryChips() {
        if (!::binding.isInitialized) return
        val group = binding.chipGroupCategory
        for (index in 0 until group.childCount) {
            (group.getChildAt(index) as? Chip)?.let {
                com.eve.app.designsystem.EveDesignSystem.Components.refreshCategoryChipPalette(it)
            }
        }
    }

    private fun renderChips(categories: List<String>, selected: String) {
        if (binding.chipGroupCategory.childCount == categories.size) {
            val same = (0 until binding.chipGroupCategory.childCount).all { i ->
                (binding.chipGroupCategory.getChildAt(i) as? Chip)?.text?.toString() == categories[i]
            }
            if (same) {
                val group = binding.chipGroupCategory
                val selectedChip = (0 until group.childCount)
                    .mapNotNull { group.getChildAt(it) as? Chip }
                    .firstOrNull { it.text.toString() == selected }
                // A single group.check() transfers selection atomically. Per-chip unchecked
                // mutations can fight selectionRequired, especially when moving back to All.
                if (selectedChip != null && group.checkedChipId != selectedChip.id) {
                    group.check(selectedChip.id)
                }
                refreshInstantiatedCategoryChips()
                return
            }
        }
        binding.chipGroupCategory.removeAllViews()
        categories.forEach { category ->
            val chip = HomeAppearance.categoryChip(this, category, selected) {
                if (category != selected) {
                    viewModel.selectCategory(category)
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private var logoutPending = false

    private fun logout() {
        if (logoutPending) return
        logoutPending = true
        lifecycleScope.launch {
            com.eve.app.util.ProfilePresenceClient.revokeBeforeLogout()
            performLocalLogout()
        }
    }

    private fun performLocalLogout() {
        SessionManager.clear()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        com.eve.app.data.repository.PremiumRepository.clearCacheForLogout(uid, this)
        FirebaseAuth.getInstance().signOut()
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions
            .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut()
        goToLogin()
    }

    private fun prepareDrawerSurface() {
        // Drawer content is opaque; retain listener and animation ownership unchanged.
        binding.ivDrawerGlassBlurBackground.setImageDrawable(null)
        // Explicit owner Night override: keep this drawer black; do not change other surfaces.
        binding.ivDrawerGlassBlurBackground.setBackgroundColor(
            ContextCompat.getColor(this, R.color.home_drawer_background))
    }

    private var pendingDrawerDestination: Intent? = null

    private fun navigateFromDrawer(intent: Intent) {
        if (pendingDrawerDestination != null) return // Reject duplicate rapid taps.
        if (!binding.drawerLayout.isDrawerVisible(GravityCompat.START)) {
            com.eve.app.util.EveNavigationHelper.navigate(this, intent)
            return
        }
        pendingDrawerDestination = intent
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupDrawer(user: com.google.firebase.auth.FirebaseUser) {
        updateDrawerHeader(user)
        prepareDrawerSurface()

        // The existing outside scrim preserves the drawer interaction.
        val scrimColor = com.eve.app.designsystem.EveDesignSystem.Colors.drawerScrim(this)
        binding.drawerLayout.setScrimColor(scrimColor)

        // Drawer clipping follows the shared fixed-card shape.
        binding.navDrawerPanel.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = com.eve.app.designsystem.EveDesignSystem.Shapes.cardRadius(resources)
                outline.setRoundRect(-radius.toInt(), 0, view.width, view.height, radius)
            }
        }
        binding.navDrawerPanel.clipToOutline = true

        // Drawer animation ownership stays here; the drawer surface is opaque.
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                playDrawerPremiumStar()
                // Keep native DrawerLayout as the sole gesture and animation owner.
                // Transfer keyboard focus into the opened disclosure.
                binding.layoutDrawerProfile.requestFocus()
            }

            override fun onDrawerClosed(drawerView: View) {
                binding.ivDrawerGlassBlurBackground.setImageDrawable(null)
                // Commit navigation only after DrawerLayout has fully released its
                // scrim and window animation, avoiding overlapping transitions.
                val destination = pendingDrawerDestination
                pendingDrawerDestination = null
                if (destination != null && !isFinishing && !isDestroyed) {
                    com.eve.app.util.EveNavigationHelper.navigate(this@MainActivity, destination)
                } else if (binding.btnMenu.isAttachedToWindow) {
                    binding.btnMenu.requestFocus()
                }
            }
        })

        setupDrawerPremiumStar()

        com.eve.app.designsystem.DesignPress.bind(binding.ivDrawerAvatar)
        binding.ivDrawerAvatar.setOnClickListener {
            navigateFromDrawer(Intent(this, ProfileActivity::class.java))
        }

        val drawerRows = listOf(
            binding.layoutDrawerProfile,

            binding.layoutDrawerPremium,
            binding.layoutDrawerPerformance,
            binding.layoutDrawerLeaderboard,
            binding.layoutDrawerSyllabus,
            binding.layoutDrawerTopic,
            binding.layoutDrawerPyq,
            binding.layoutDrawerMistakes,

            binding.layoutDrawerSettings
        )
        drawerRows.forEach { row ->
            com.eve.app.designsystem.DesignPress.bind(row)
        }

        binding.layoutDrawerProfile.setOnClickListener {
            navigateFromDrawer(Intent(this, ProfileActivity::class.java))
        }
        binding.layoutDrawerPremium.setOnClickListener {
            navigateFromDrawer(Intent(this, com.eve.app.ui.premium.PremiumActivity::class.java))
        }
        binding.layoutDrawerPerformance.setOnClickListener {
            navigateFromDrawer(Intent(this, PerformanceActivity::class.java))
        }
        binding.layoutDrawerLeaderboard.setOnClickListener {
            navigateFromDrawer(Intent(this, LeaderboardActivity::class.java).apply {
                putExtra(Constants.EXTRA_EXAM_ID, "overall")
                putExtra(Constants.EXTRA_EXAM_NAME, "Overall Leaderboard")
            })
        }
        binding.layoutDrawerSyllabus.setOnClickListener {
            navigateFromDrawer(Intent(this, SyllabusActivity::class.java))
        }
        binding.layoutDrawerTopic.setOnClickListener {
            navigateFromDrawer(Intent(this, PracticeActivity::class.java))
        }
        binding.layoutDrawerPyq.setOnClickListener {
            navigateFromDrawer(Intent(this, PyqActivity::class.java))
        }
        binding.layoutDrawerMistakes.setOnClickListener {
            navigateFromDrawer(Intent(this, com.eve.app.ui.mistakes.MistakesActivity::class.java))
        }
        binding.layoutDrawerSettings.setOnClickListener {
            navigateFromDrawer(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateDrawerHeader(user: com.google.firebase.auth.FirebaseUser) {
        ProfilePhotoManager.applyTo(this, binding.ivDrawerAvatar, user.photoUrl?.toString(), R.drawable.design_avatar_surface)
        binding.tvDrawerUserName.text = user.displayName?.takeIf { it.isNotBlank() } ?: "Student"
        binding.tvDrawerUserEmail.text = user.email ?: ""

        val isPrem = com.eve.app.data.repository.PremiumRepository.isCurrentUserPremium(this)
        if (isPrem) {
            binding.tvDrawerPremiumStatus.visibility = View.VISIBLE
            binding.tvDrawerPremiumStatus.text = "★ Premium Active"
        } else {
            binding.tvDrawerPremiumStatus.visibility = View.GONE
        }
        lifecycleScope.launch(lifecycleExceptionHandler) {
            val res = com.eve.app.data.repository.PremiumRepository.refreshStatus(this@MainActivity)
            if (res.isSuccess) {
                val status = res.getOrNull()
                if (status?.isPremium == true) {
                    binding.tvDrawerPremiumStatus.visibility = View.VISIBLE
                    val exp = status.expiresAt
                    if (status.isLifetime) {
                        binding.tvDrawerPremiumStatus.text = "★ Premium • Lifetime"
                    } else if (exp != null && exp > 0) {
                        val dateStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(java.util.Date(exp))
                        binding.tvDrawerPremiumStatus.text = "★ Premium Active • Expires $dateStr"
                    } else {
                        binding.tvDrawerPremiumStatus.text = "★ Premium Active"
                    }
                } else {
                    binding.tvDrawerPremiumStatus.visibility = View.GONE
                }
            }
        }
    }

    private fun animateThemeChange(
        rootView: View,
        touchX: Int,
        touchY: Int,
        applyNewThemeAction: Runnable? = null
    ) {
        ThemeManager.animateThemeChange(rootView, touchX, touchY, applyNewThemeAction)
    }

    private fun setupDrawerPremiumStar() {
        val fallback = binding.lottieDrawerPremiumStarFallback
        fallback.speed = 1.5f

        val isDark = ThemeManager.isDarkMode(this)
        if (isDark) {
            fallback.setAnimation(R.raw.premium_star)
        } else {
            fallback.setAnimation(R.raw.premium_star_light)
        }

        fallback.repeatCount = 0
        fallback.addLottieOnCompositionLoadedListener {
            if (!fallback.isAnimating) {
                fallback.progress = 1f
            }
        }
        fallback.progress = 1f

        fallback.visibility = View.VISIBLE
    }

    private fun playDrawerPremiumStar() {
        val fallback = binding.lottieDrawerPremiumStarFallback

        if (fallback.visibility == View.VISIBLE) {
            val animScale = Settings.Global.getFloat(
                contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            if (animScale == 0f) {
                fallback.progress = 1f
            } else {
                fallback.progress = 0f
                fallback.playAnimation()
            }
        }
    }
}

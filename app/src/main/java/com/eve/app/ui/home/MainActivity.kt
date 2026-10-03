package com.eve.app.ui.home

import com.eve.app.ui.common.EveBaseActivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.os.Build
import com.eve.app.util.FastBlurHelper
import android.os.Bundle
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
import com.eve.app.ui.common.HomePanelWashDrawable
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
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.about.AboutActivity
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
import com.eve.app.util.RippleHelper
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.ThemeManager
import com.eve.app.util.ThemeSwitchAnimator
import com.eve.app.util.SessionManager
import com.eve.app.util.UiState
import com.eve.app.util.VibrationHelper
import com.eve.app.util.isHardcodedAdmin
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

    private var isSearchActive = false
    private var currentSearchQuery = ""
    private var lastLoadedItems: List<HomeListItem> = emptyList()
    private var searchDebounceJob: Job? = null

    private var notifJob: Job? = null

    private val bannerAdapter = HomeBannerAdapter()
    private val bannerRepo = com.eve.app.data.repository.HomeBannerRepository()
    private var bannerObserverJob: Job? = null
    private var bannerAutoScrollJob: Job? = null
    private var currentBannerCount = 0
    private var isUserDraggingBanner = false

    private val floatingLinkRepo = com.eve.app.data.repository.FloatingLinkRepository()
    private var activeFloatingLinkUrl: String? = null
    private var lastStreakFetchTime: Long = 0L

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignored */ }

    private val foregroundNotificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctx = context ?: this@MainActivity
            VibrationHelper.vibrateNotification(ctx)
            binding.btnNotification.repeatCount = 0
            binding.btnNotification.progress = 0f
            binding.btnNotification.playAnimation()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasEmptyPlayed = savedInstanceState?.getBoolean("key_empty_played", false) ?: false

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            goToLogin()
            return
        }

        Log.e("EVE_STARTUP", "stage: inflate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        applyFindTestPanelBackground()

        Log.e("EVE_STARTUP", "stage: setupDrawer")
        setupDrawer(user)

        Log.e("EVE_STARTUP", "stage: setupBannerCarousel")
        setupBannerCarousel()

        binding.tvWelcome.text = "Hi, ${user.displayName ?: "Student"}"

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

        binding.ivProfile.setOnClickListener {
            captureDrawerBlur()
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        loadProfilePhoto(user)

        // Telegram-style Search setup
        Log.e("EVE_STARTUP", "stage: setupSearch")
        setupSearch()

        // Telegram-style overflow menu setup
        binding.btnOverflow.setOnClickListener { anchor ->
            TelegramMenuPopup(
                context = this,
                onThemeToggle = { cx, cy, iconWidth, iconHeight ->
                    try {
                        val goingDark = !ThemeSwitchAnimator.isDarkMode(this)
                        ThemeSwitchAnimator.animateAt(this, cx, cy, goingDark, iconWidth, iconHeight)
                    } catch (t: Throwable) {
                        android.util.Log.e("ThemeClickDiag", "EXCEPTION in MainActivity onThemeToggle", t)
                    }
                },
                onHistory = { startActivity(Intent(this, HistoryActivity::class.java)) },
                onBookmarks = { startActivity(Intent(this, BookmarksActivity::class.java)) },
                onMistakes = { startActivity(Intent(this, com.eve.app.ui.mistakes.MistakesActivity::class.java)) },
                onTopic = { startActivity(Intent(this, PracticeActivity::class.java)) },
                onPyq = { startActivity(Intent(this, PyqActivity::class.java)) },
                onLogout = { logout() }
            ).show(anchor)
        }

        // System back button closes drawer first, then active search
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
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
        val initialAdmin = SessionManager.getCachedAdminStatus(user.email, allowStale = true)
            ?: isHardcodedAdmin(user.email)
        isAdminUser = initialAdmin
        if (initialAdmin) {
            showAdminButton()
        }
        viewModel.loadForUser(user.uid, initialAdmin)

        lifecycleScope.launch(lifecycleExceptionHandler) {
            val admin = adminRepo.isAdmin(user.email)
            if (admin != isAdminUser) {
                isAdminUser = admin
                if (admin) showAdminButton()
                viewModel.loadForUser(user.uid, admin)
            }
        }

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter

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
        applyFindTestPanelBackground()
        hasEmptyPlayed = false
        isExamNavigating = false
        checkAppConfigAndMaintenance()
        FirebaseAuth.getInstance().currentUser?.let { current ->
            lifecycleScope.launch(lifecycleExceptionHandler) {
                val admin = adminRepo.isAdmin(current.email)
                viewModel.loadForUser(current.uid, admin)
            }
        }

        val now = System.currentTimeMillis()
        if (now - lastStreakFetchTime > 60_000L) {
            lifecycleScope.launch(lifecycleExceptionHandler) {
                try {
                    val tzOffset = -java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000
                    val res = com.eve.app.data.remote.ApiClient.api.getStreak(tzOffset)
                    if (res.success && res.data != null) {
                        lastStreakFetchTime = System.currentTimeMillis()
                        val prefs = getSharedPreferences(com.eve.app.util.StreakHelper.PREFS_NAME, Context.MODE_PRIVATE)
                        val goal = prefs.getInt(com.eve.app.util.StreakHelper.KEY_DAILY_GOAL, com.eve.app.util.StreakHelper.DEFAULT_DAILY_GOAL)
                        binding.tvStreakSummary.text = com.eve.app.util.StreakHelper.formatStreakText(
                            res.data.currentStreak,
                            res.data.todayCount,
                            goal
                        )
                        binding.tvStreakSummary.visibility = View.VISIBLE
                    }
                } catch (_: Exception) {}
            }
        }

        if (::binding.isInitialized && binding.cardFloatingAirplane.visibility == View.VISIBLE) {
            val animScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            if (animScale > 0f && !binding.lottieFloatingAirplane.isAnimating) {
                binding.lottieFloatingAirplane.resumeAnimation()
            }
        }
        if (::binding.isInitialized) {
            setupDrawerPremiumStar()
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.lottieDrawerPremiumStar.setPaused(false)
                binding.lottieDrawerPremiumStar.updateTheme()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized) {
            binding.lottieDrawerPremiumStar.setPaused(true)
        }
        if (::binding.isInitialized && binding.cardFloatingAirplane.visibility == View.VISIBLE) {
            binding.lottieFloatingAirplane.pauseAnimation()
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

    private fun openSearch() {
        isSearchActive = true
        binding.searchTopBar.visibility = View.VISIBLE
        binding.searchTopBar.alpha = 0f
        binding.searchTopBar.translationX = 50f

        binding.normalTopBar.animate()
            .alpha(0f)
            .translationX(-50f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.normalTopBar.visibility = View.GONE
            }
            .start()

        binding.searchTopBar.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.etSearch.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        binding.chipGroupCategory.visibility = View.GONE
        applyCurrentList()
    }

    private fun closeSearch() {
        isSearchActive = false
        binding.etSearch.text?.clear()
        currentSearchQuery = ""
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        binding.normalTopBar.visibility = View.VISIBLE

        binding.searchTopBar.animate()
            .alpha(0f)
            .translationX(50f)
            .setDuration(160)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                binding.searchTopBar.visibility = View.GONE
            }
            .start()

        binding.normalTopBar.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(160)
            .setInterpolator(DecelerateInterpolator())
            .start()

        val data = (viewModel.state.value as? UiState.Success)?.data
        if (data != null && data.categories.size > 1) {
            binding.chipGroupCategory.visibility = View.VISIBLE
        }
        applyCurrentList()
    }

    private fun applyCurrentList() {
        if (isSearchActive && currentSearchQuery.isNotEmpty()) {
            val q = currentSearchQuery.lowercase()
            val filtered = lastLoadedItems.filter { item ->
                when (item) {
                    is HomeListItem.ExamRow ->
                        item.exam.examName.lowercase().contains(q) || item.exam.categoryOrOther.lowercase().contains(q)
                    is HomeListItem.FeedbackPostRow ->
                        item.post.title.lowercase().contains(q) || item.post.message.lowercase().contains(q)
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
        popup.show()
    }

    private fun openFeedbackForPost(post: com.eve.app.data.model.FeedbackPost) {
        val intent = Intent(this, com.eve.app.ui.feedback.FeedbackActivity::class.java).apply {
            putExtra("post_id", post.id)
            putExtra("post_title", post.title)
        }
        startActivity(intent)
    }

    private fun handleFeedbackPostLongClick(post: com.eve.app.data.model.FeedbackPost) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        lifecycleScope.launch(lifecycleExceptionHandler) {
            if (adminRepo.isAdmin(email)) {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Delete Feedback Post?")
                    .setMessage("Are you sure you want to delete '${post.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        com.eve.app.util.AppUndoBar.show(
                            context = this@MainActivity,
                            message = "Post '${post.title}' deleted",
                            timeLeftMs = com.eve.app.util.AppUndoBar.TIME_IMPORTANT,
                            onUndo = {
                                com.eve.app.util.AppBulletin.show(this@MainActivity, "Delete cancelled")
                            },
                            onExecuteDelete = {
                                viewModel.deleteFeedbackPost(post.id)
                            }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private var isReturningFromStopped = false

    override fun onStart() {
        super.onStart()
        if (isReturningFromStopped) {
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
                binding.tvWelcome.text = "Hi, ${current.displayName ?: "Student"}"
                updateDrawerHeader(current)
                lifecycleScope.launch(lifecycleExceptionHandler) {
                    val admin = adminRepo.isAdmin(current.email)
                    viewModel.loadForUser(current.uid, admin)
                }
            }
            FirebaseAuth.getInstance().currentUser?.let { loadProfilePhoto(it) }

            if (NotificationStore.hasNewUnseen(this)) {
                binding.dotUnread.visibility = View.VISIBLE
                binding.btnNotification.repeatCount = 0
                binding.btnNotification.progress = 0f
                binding.btnNotification.playAnimation()
                val latest = NotificationStore.getLatestNotificationTimestamp(this)
                if (latest > 0L) NotificationStore.setLastSeenTimestamp(this, latest)
            } else {
                updateNotificationDot()
                if (binding.btnNotification.isAnimating) {
                    binding.btnNotification.pauseAnimation()
                }
                binding.btnNotification.progress = 0f
            }

            startListeningToNotifications()
            startListeningToHomeBanner()

            lifecycleScope.launch(lifecycleExceptionHandler) {
                val link = floatingLinkRepo.getFloatingLink()
                activeFloatingLinkUrl = link
                updateFloatingAirplaneState(link)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        isReturningFromStopped = true
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
        binding.cardFloatingAirplane.setCardBackgroundColor(
            ContextCompat.getColor(this, android.R.color.transparent)
        )
        applyFloatingAirplaneTheme()

        ViewCompat.setOnApplyWindowInsetsListener(binding.cardFloatingAirplane) { view, windowInsets ->
            val navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val baseMarginPx = (16 * resources.displayMetrics.density).toInt()
            (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                lp.bottomMargin = baseMarginPx + navInsets.bottom
                lp.marginEnd = baseMarginPx + navInsets.right
                view.layoutParams = lp
            }
            windowInsets
        }

        binding.cardFloatingAirplane.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    v.performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
            }
            true
        }

        binding.cardFloatingAirplane.setOnClickListener {
            val url = activeFloatingLinkUrl ?: return@setOnClickListener
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (_: Exception) {
                AppBulletin.showError(this, "Couldn't open link")
            }
        }
    }

    private fun applyFloatingAirplaneTheme() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val keyPathAll = KeyPath("**")

        // Dark mode: remap #000000 and #231f20 (RGB) to ContextCompat.getColor(this, R.color.eve_text).
        // Light mode: remap #ffffff and #fcfcfc to ContextCompat.getColor(this, R.color.eve_stroke).
        val targetColor = if (isDark) {
            ContextCompat.getColor(this, R.color.eve_text)
        } else {
            ContextCompat.getColor(this, R.color.eve_stroke)
        }

        val callback = SimpleLottieValueCallback<Int> { frameInfo ->
            val orig = frameInfo?.startValue ?: return@SimpleLottieValueCallback 0
            val rgb = orig and 0x00FFFFFF
            val alpha = orig and 0xFF000000.toInt()

            if (isDark) {
                when (rgb) {
                    0x000000, 0x231F20 -> alpha or (targetColor and 0x00FFFFFF)
                    else -> orig
                }
            } else {
                when (rgb) {
                    0xFFFFFF, 0xFCFCFC -> alpha or (targetColor and 0x00FFFFFF)
                    else -> orig
                }
            }
        }

        binding.lottieFloatingAirplane.addValueCallback(keyPathAll, LottieProperty.COLOR, callback)
        binding.lottieFloatingAirplane.addValueCallback(keyPathAll, LottieProperty.STROKE_COLOR, callback)
    }

    private fun updateFloatingAirplaneState(link: String?) {
        if (!link.isNullOrBlank()) {
            val isCurrentlyGone = binding.cardFloatingAirplane.visibility != View.VISIBLE
            if (isCurrentlyGone) {
                binding.cardFloatingAirplane.scaleX = 0f
                binding.cardFloatingAirplane.scaleY = 0f
                binding.cardFloatingAirplane.alpha = 0f
                binding.cardFloatingAirplane.visibility = View.VISIBLE
                binding.cardFloatingAirplane.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(250)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }
            val animScale = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            if (animScale == 0f) {
                binding.lottieFloatingAirplane.progress = 1f
            } else {
                binding.lottieFloatingAirplane.repeatCount = com.airbnb.lottie.LottieDrawable.INFINITE
                if (!binding.lottieFloatingAirplane.isAnimating) {
                    binding.lottieFloatingAirplane.playAnimation()
                }
            }
        } else {
            binding.cardFloatingAirplane.visibility = View.GONE
            binding.lottieFloatingAirplane.cancelAnimation()
        }
    }

    private fun setupBannerCarousel() {
        binding.vpHomeBanners.adapter = bannerAdapter
        binding.vpHomeBanners.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateBannerDots(position)
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
                currentBannerCount = banners.size
                if (banners.isEmpty()) {
                    binding.cardHomeBanner.visibility = View.GONE
                    binding.layoutBannerDots.visibility = View.GONE
                    stopBannerAutoScroll()
                } else {
                    binding.cardHomeBanner.visibility = View.VISIBLE
                    bannerAdapter.submitList(banners)

                    if (banners.size == 1) {
                        binding.layoutBannerDots.visibility = View.GONE
                        binding.vpHomeBanners.isUserInputEnabled = false
                        stopBannerAutoScroll()
                    } else {
                        binding.layoutBannerDots.visibility = View.VISIBLE
                        binding.vpHomeBanners.isUserInputEnabled = true
                        setupBannerDots(banners.size, binding.vpHomeBanners.currentItem)
                        startBannerAutoScroll(banners.size)
                    }
                }
            }
        }
    }

    private fun setupBannerDots(count: Int, selectedIndex: Int) {
        binding.layoutBannerDots.removeAllViews()
        val density = resources.displayMetrics.density
        val margin = (3 * density).toInt()
        val h = (6 * density).toInt()
        for (i in 0 until count) {
            val isActive = (i == selectedIndex)
            val w = if (isActive) (16 * density).toInt() else (6 * density).toInt()
            val dot = View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(w, h).apply {
                    setMargins(margin, 0, margin, 0)
                }
                setBackgroundResource(if (isActive) R.drawable.bg_banner_dot_active else R.drawable.bg_banner_dot_inactive)
            }
            binding.layoutBannerDots.addView(dot)
        }
    }

    private fun updateBannerDots(selectedIndex: Int) {
        val density = resources.displayMetrics.density
        val margin = (3 * density).toInt()
        val h = (6 * density).toInt()
        for (i in 0 until binding.layoutBannerDots.childCount) {
            val dot = binding.layoutBannerDots.getChildAt(i) ?: continue
            val isActive = (i == selectedIndex)
            val w = if (isActive) (16 * density).toInt() else (6 * density).toInt()
            dot.layoutParams = android.widget.LinearLayout.LayoutParams(w, h).apply {
                setMargins(margin, 0, margin, 0)
            }
            dot.setBackgroundResource(if (isActive) R.drawable.bg_banner_dot_active else R.drawable.bg_banner_dot_inactive)
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
                val res = com.eve.app.data.remote.ApiClient.apiService.getBroadcasts(1)
                if (res.success && !res.data.isNullOrEmpty()) {
                    val doc = res.data.first()
                    val sentAt = doc.sentAt
                    val lastSeen = NotificationStore.getLastSeenTimestamp(this@MainActivity)
                    if (sentAt > lastSeen && lastSeen > 0L) {
                        NotificationStore.add(this@MainActivity, doc.title, doc.message)
                        VibrationHelper.vibrateNotification(this@MainActivity)
                        binding.btnNotification.repeatCount = 0
                        binding.btnNotification.progress = 0f
                        binding.btnNotification.playAnimation()
                        updateNotificationDot()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun updateNotificationDot() {
        binding.dotUnread.visibility = if (NotificationStore.hasUnread(this)) View.VISIBLE else View.GONE
    }

    private fun setupNotificationBell() {
        val iconColor = ContextCompat.getColor(this, R.color.eve_header_ink_icon)
        binding.btnNotification.addValueCallback(
            com.airbnb.lottie.model.KeyPath("**"),
            com.airbnb.lottie.LottieProperty.COLOR_FILTER
        ) {
            android.graphics.PorterDuffColorFilter(iconColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
        }
        binding.btnNotification.repeatCount = 0

        binding.btnNotification.setOnClickListener {
            binding.btnNotification.repeatCount = 0
            binding.btnNotification.progress = 0f
            binding.btnNotification.playAnimation()
            val latest = NotificationStore.getLatestNotificationTimestamp(this)
            if (latest > 0L) {
                NotificationStore.setLastSeenTimestamp(this, latest)
            }
            binding.dotUnread.visibility = View.GONE
            binding.btnNotification.postDelayed({
                startActivity(Intent(this, NotificationsActivity::class.java))
            }, 350)
        }
    }

    private fun applyFindTestPanelBackground() {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (!isNight) {
            val radiusPx = resources.getDimension(R.dimen.eve_radius_card)
            val strokePx = resources.displayMetrics.density * 1f
            binding.panelFindTest.background = HomePanelWashDrawable(radiusPx, strokePx)
        } else {
            binding.panelFindTest.setBackgroundResource(R.drawable.bg_panel_card)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                binding.mainContentContainer.setRenderEffect(null)
            } catch (_: Throwable) { }
        }
        try {
            unregisterReceiver(foregroundNotificationReceiver)
        } catch (_: Exception) { }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("key_empty_played", hasEmptyPlayed)
    }

    private fun loadProfilePhoto(user: com.google.firebase.auth.FirebaseUser) {
        ProfilePhotoManager.applyTo(
            this, binding.ivProfile, user.photoUrl?.toString(), R.drawable.bg_circle_translucent
        )
    }

    private fun setupPushNotifications() {
        NotificationHelper.createChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Subscribe to new exams topic AND all_users broadcast topic
        FirebaseMessaging.getInstance().subscribeToTopic(NotificationHelper.TOPIC_NEW_EXAMS)
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
        ReminderScheduler.applySavedState(this)
    }

    private fun showAdminButton() {
        binding.btnAdmin.visibility = View.VISIBLE
        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
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
                    binding.shimmerSkeletonHome.visibility = View.GONE
                    binding.rvExams.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                } else {
                    binding.shimmerSkeletonHome.visibility = View.VISIBLE
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
                renderChips(data.categories, data.selectedCategory)
                applyCurrentList()
                com.eve.app.util.ShimmerHelper.crossFade(binding.shimmerSkeletonHome, binding.rvExams)
                binding.chipGroupCategory.visibility =
                    if (data.categories.size <= 1 || isSearchActive) View.GONE else View.VISIBLE

                if (!TargetExamsBottomSheet.isOnboardingDone(this@MainActivity)) {
                    val mainExams = data.items.filterIsInstance<HomeListItem.ExamRow>().map { it.exam }.filter { it.isMainExam }
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
                    binding.shimmerSkeletonHome.visibility = View.GONE
                    binding.rvExams.visibility = View.VISIBLE
                    binding.messageGroup.visibility = View.GONE
                } else {
                    binding.shimmerSkeletonHome.visibility = View.GONE
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

    private fun renderChips(categories: List<String>, selected: String) {
        if (binding.chipGroupCategory.childCount == categories.size) {
            val same = (0 until binding.chipGroupCategory.childCount).all { i ->
                (binding.chipGroupCategory.getChildAt(i) as? Chip)?.text?.toString() == categories[i]
            }
            if (same) {
                (0 until binding.chipGroupCategory.childCount).forEach { i ->
                    val chip = binding.chipGroupCategory.getChildAt(i) as Chip
                    chip.isChecked = chip.text.toString() == selected
                }
                return
            }
        }
        binding.chipGroupCategory.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = category == selected
                chipCornerRadius = 100f * resources.displayMetrics.density
                chipStrokeWidth = 1f * resources.displayMetrics.density
                chipStrokeColor = androidx.core.content.ContextCompat.getColorStateList(this@MainActivity, R.color.selector_category_chip_stroke)
                chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(this@MainActivity, R.color.selector_category_chip_bg)
                setTextColor(androidx.core.content.ContextCompat.getColorStateList(this@MainActivity, R.color.selector_category_chip_text))
                isCheckedIconVisible = false
                setOnClickListener { viewModel.selectCategory(category) }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun logout() {
        SessionManager.clear()
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        com.eve.app.data.repository.PremiumRepository.clearCacheForLogout(uid, this)
        FirebaseAuth.getInstance().signOut()
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut()
        goToLogin()
    }

    private var drawerBlurBitmap: Bitmap? = null

    private fun captureDrawerBlur() {
        if (drawerBlurBitmap != null) return
        try {
            val container = binding.mainContentContainer
            if (container.width <= 0 || container.height <= 0) return

            val drawerW = binding.navDrawerPanel.width.takeIf { it > 0 }
                ?: (290 * resources.displayMetrics.density).toInt()
            val drawerH = container.height

            val scale = 4
            val sampleW = (drawerW / scale).coerceAtLeast(1)
            val sampleH = (drawerH / scale).coerceAtLeast(1)

            val bmp = Bitmap.createBitmap(sampleW, sampleH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.scale(1f / scale, 1f / scale)
            container.draw(canvas)

            val blurred = FastBlurHelper.blur(bmp, radius = 20, canReuseInBitmap = true)
            drawerBlurBitmap = blurred
            binding.ivDrawerGlassBlurBackground.setImageBitmap(blurred)
        } catch (_: Throwable) {
            // Fallback gracefully
        }
    }

    private fun setupDrawer(user: com.google.firebase.auth.FirebaseUser) {
        updateDrawerHeader(user)

        // Frosted glass translucent scrim allowing underlying content to remain sharp and visible outside drawer
        val isDark = ThemeManager.isDarkMode(this)
        val scrimColor = if (isDark) Color.argb(0x4D, 0, 0, 0) else Color.argb(0x26, 0, 0, 0)
        binding.drawerLayout.setScrimColor(scrimColor)

        // Clip drawer panel rounded right edge (24dp) so blur background and tint conform to rounded corners
        binding.navDrawerPanel.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val radius = 24 * resources.displayMetrics.density
                outline.setRoundRect(-radius.toInt(), 0, view.width, view.height, radius)
            }
        }
        binding.navDrawerPanel.clipToOutline = true

        // Scoped frosted-glass blur: blur ONLY the drawer panel, never the rest of the screen
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                if (slideOffset > 0f) {
                    if (drawerBlurBitmap == null) {
                        captureDrawerBlur()
                    }
                    binding.lottieDrawerPremiumStar.setPaused(false)
                }
            }

            override fun onDrawerStateChanged(newState: Int) {
                if ((newState == DrawerLayout.STATE_DRAGGING || newState == DrawerLayout.STATE_SETTLING) && drawerBlurBitmap == null) {
                    captureDrawerBlur()
                }
            }

            override fun onDrawerOpened(drawerView: View) {
                playDrawerPremiumStar()
            }

            override fun onDrawerClosed(drawerView: View) {
                binding.ivDrawerGlassBlurBackground.setImageDrawable(null)
                drawerBlurBitmap = null
                binding.lottieDrawerPremiumStar.setPaused(true)
            }
        })

        setupDrawerPremiumStar()

        // FIX 7: Telegram-style premium masked, bounded RippleDrawable on avatar and drawer rows
        binding.ivDrawerAvatar.foreground = RippleHelper.createPremiumRippleDrawable(this, cornerRadiusDp = -1f, isDark = isDark)
        binding.ivDrawerAvatar.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val drawerRowRadiusDp = 12f
        val drawerRows = listOf(
            binding.layoutDrawerProfile,
            binding.layoutDrawerPremium,
            binding.layoutDrawerPerformance,
            binding.layoutDrawerLeaderboard,
            binding.layoutDrawerSyllabus,
            binding.layoutDrawerAbout,
            binding.layoutDrawerSettings
        )
        drawerRows.forEach { row ->
            row.foreground = RippleHelper.createPremiumRippleDrawable(this, cornerRadiusDp = drawerRowRadiusDp, isDark = isDark)
        }

        binding.layoutDrawerProfile.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        binding.layoutDrawerPremium.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, com.eve.app.ui.premium.PremiumActivity::class.java))
        }
        binding.layoutDrawerPerformance.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, PerformanceActivity::class.java))
        }
        binding.layoutDrawerLeaderboard.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(
                Intent(this, LeaderboardActivity::class.java).apply {
                    putExtra(Constants.EXTRA_EXAM_ID, "overall")
                    putExtra(Constants.EXTRA_EXAM_NAME, "Overall Leaderboard")
                }
            )
        }
        binding.layoutDrawerSyllabus.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SyllabusActivity::class.java))
        }
        binding.layoutDrawerAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, AboutActivity::class.java))
        }
        binding.layoutDrawerSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun updateDrawerHeader(user: com.google.firebase.auth.FirebaseUser) {
        ProfilePhotoManager.applyTo(this, binding.ivDrawerAvatar, user.photoUrl?.toString(), R.drawable.bg_circle_translucent)
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
        val starView = binding.lottieDrawerPremiumStar
        val fallback = binding.lottieDrawerPremiumStarFallback
        starView.fallbackView = fallback
        starView.lazyMode = false
        starView.animationSpeedMultiplier = 1.5f
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

        if (starView.isFailed) {
            fallback.visibility = View.VISIBLE
            starView.visibility = View.GONE
        } else {
            fallback.visibility = View.VISIBLE
            starView.visibility = View.VISIBLE
            starView.updateTheme()
        }
    }

    private fun playDrawerPremiumStar() {
        val starView = binding.lottieDrawerPremiumStar
        val fallback = binding.lottieDrawerPremiumStarFallback

        if (starView.isFailed || starView.visibility != View.VISIBLE) {
            fallback.visibility = View.VISIBLE
            starView.visibility = View.GONE
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
            return
        }

        starView.setPaused(false)
        starView.updateTheme()
        starView.startEnterAnimation()

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

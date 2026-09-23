package com.eve.app.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.admin.AdminActivity
import com.eve.app.ui.daily.DailyQuizActivity
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.notifications.NotificationsActivity
import com.eve.app.ui.performance.PerformanceActivity
import com.eve.app.ui.practice.PracticeActivity
import com.eve.app.ui.profile.ProfileActivity
import com.eve.app.ui.pyq.PyqActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.CrashlyticsHelper
import com.eve.app.util.DateUtil
import com.eve.app.util.NetworkUtil
import com.eve.app.util.NotificationHelper
import com.eve.app.util.NotificationStore
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.StreakStore
import com.eve.app.util.ThemeManager
import com.eve.app.util.UiState
import com.eve.app.util.isHardcodedAdmin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val adminRepo = AdminRepository()

    private var isSearchActive = false
    private var currentSearchQuery = ""
    private var lastLoadedItems: List<HomeListItem> = emptyList()
    private var searchDebounceJob: Job? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignored */ }

    private val adapter = ExamAdapter { exam ->
        startActivity(
            Intent(this, TestActivity::class.java)
                .putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                .putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                .putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                .putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Hi, ${user.displayName ?: "Student"}"

        binding.btnNotification.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        setupPushNotifications()

        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        loadProfilePhoto(user)

        // Telegram-style Search setup
        setupSearch()

        // Telegram-style 2-card overflow menu setup
        binding.btnOverflow.setOnClickListener { anchor ->
            TelegramMenuPopup(
                context = this,
                onThemeToggle = { ThemeManager.toggleWithReveal(this, anchor) },
                onHistory = { startActivity(Intent(this, HistoryActivity::class.java)) },
                onPerformance = { startActivity(Intent(this, PerformanceActivity::class.java)) },
                onTopic = { startActivity(Intent(this, PracticeActivity::class.java)) },
                onPyq = { startActivity(Intent(this, PyqActivity::class.java)) },
                onLeaderboard = {
                    startActivity(
                        Intent(this, LeaderboardActivity::class.java).apply {
                            putExtra(Constants.EXTRA_EXAM_ID, "overall")
                            putExtra(Constants.EXTRA_EXAM_NAME, "Overall Leaderboard")
                        }
                    )
                },
                onLogout = { logout() }
            ).show(anchor)
        }

        // System back button closes active search first
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSearchActive) {
                    closeSearch()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        if (isHardcodedAdmin(user.email)) {
            showAdminButton()
            viewModel.loadForUser(user.uid, true)
        } else {
            lifecycleScope.launch {
                val admin = adminRepo.isAdmin(user.email)
                if (admin) showAdminButton()
                viewModel.loadForUser(user.uid, admin)
            }
        }

        binding.cardDailyGk.setOnClickListener {
            startActivity(Intent(this, DailyQuizActivity::class.java))
        }
        refreshDailyCard()

        binding.rvExams.layoutManager = LinearLayoutManager(this)
        binding.rvExams.adapter = adapter

        binding.btnRetry.setOnClickListener { viewModel.load() }

        updateNotificationDot()

        lifecycleScope.launch {
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
                searchDebounceJob = lifecycleScope.launch {
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

        applyCurrentList()
    }

    private fun applyCurrentList() {
        if (currentSearchQuery.isBlank()) {
            adapter.submit(lastLoadedItems)
            val empty = lastLoadedItems.isEmpty()
            binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvExams.visibility = if (empty) View.GONE else View.VISIBLE
            binding.chipGroupCategory.visibility = if (isSearchActive) View.GONE else View.VISIBLE
        } else {
            binding.chipGroupCategory.visibility = View.GONE
            val filtered = lastLoadedItems.filterIsInstance<HomeListItem.ExamRow>()
                .filter { it.exam.examName.contains(currentSearchQuery, ignoreCase = true) }
            adapter.submit(filtered)
            val empty = filtered.isEmpty()
            binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvExams.visibility = if (empty) View.GONE else View.VISIBLE
            if (empty) {
                binding.ivMessageIcon.setImageResource(R.drawable.ic_state_empty)
                binding.tvMessage.text = "No exams found"
                binding.tvMessageSub.text = "Try a different search query"
                binding.btnRetry.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().currentUser?.let { user ->
            lifecycleScope.launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(user.uid)
                        .update("lastActive", System.currentTimeMillis())
                } catch (_: Exception) {}
            }
        }
    }

    private fun logout() {
        CrashlyticsHelper.clearIdentity()
        FirebaseAuth.getInstance().signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::binding.isInitialized) {
            FirebaseAuth.getInstance().currentUser?.let { current ->
                lifecycleScope.launch {
                    val admin = isHardcodedAdmin(current.email) || adminRepo.isAdmin(current.email)
                    viewModel.loadForUser(current.uid, admin)
                }
            }
            FirebaseAuth.getInstance().currentUser?.let { loadProfilePhoto(it) }
            updateNotificationDot()
            refreshDailyCard()
        }
    }

    private fun refreshDailyCard() {
        if (!::binding.isInitialized) return
        val streak = StreakStore.streak(this)
        val streakText = if (streak > 0) "  •  🔥 $streak day streak" else ""
        binding.tvDailyTitle.text = "Daily GK  •  ${DateUtil.display(DateUtil.todayIso())}"
        binding.tvDailySub.text = if (StreakStore.attemptedToday(this)) {
            val score = StreakStore.lastScore(this)
            val total = StreakStore.lastTotal(this)
            "Attempted today • $score/$total$streakText"
        } else {
            "Today's current affairs quiz$streakText"
        }
    }

    private fun updateNotificationDot() {
        binding.dotUnread.visibility = if (NotificationStore.hasUnread(this)) View.VISIBLE else View.GONE
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
                binding.progressGroup.visibility = View.VISIBLE
                binding.messageGroup.visibility = View.GONE
                binding.chipGroupCategory.visibility = View.GONE
            }
            is UiState.Success -> {
                binding.progressGroup.visibility = View.GONE
                binding.btnRetry.visibility = View.GONE
                val data = state.data
                lastLoadedItems = data.items
                renderChips(data.categories, data.selectedCategory)
                applyCurrentList()
                binding.chipGroupCategory.visibility =
                    if (data.categories.size <= 1 || isSearchActive) View.GONE else View.VISIBLE
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setImageResource(R.drawable.ic_state_error)
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
                setOnClickListener { viewModel.selectCategory(category) }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

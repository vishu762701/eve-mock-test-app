package com.eve.app.ui.home

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.admin.AdminActivity
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.notifications.NotificationsActivity
import com.eve.app.ui.performance.PerformanceActivity
import com.eve.app.ui.practice.PracticeActivity
import com.eve.app.ui.profile.ProfileActivity
import com.eve.app.ui.pyq.PyqActivity
import com.eve.app.ui.syllabus.SyllabusActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.CrashlyticsHelper
import com.eve.app.util.NetworkUtil
import com.eve.app.util.NotificationHelper
import com.eve.app.util.NotificationStore
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.ThemeManager
import com.eve.app.util.UiState
import com.eve.app.util.VibrationHelper
import com.eve.app.util.isHardcodedAdmin
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val feedbackRepository = FeedbackRepository()

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val adminRepo = AdminRepository()

    private var isSearchActive = false
    private var currentSearchQuery = ""
    private var lastLoadedItems: List<HomeListItem> = emptyList()
    private var searchDebounceJob: Job? = null

    private var firestoreNotifRegistration: ListenerRegistration? = null

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

    private val adapter = ExamAdapter(
        onClick = { exam ->
            startActivity(
                Intent(this, TestActivity::class.java)
                    .putExtra(Constants.EXTRA_EXAM_ID, exam.id)
                    .putExtra(Constants.EXTRA_EXAM_NAME, exam.examName)
                    .putExtra(Constants.EXTRA_EXAM_CATEGORY, exam.categoryOrOther)
                    .putExtra(Constants.EXTRA_TIME_LIMIT, exam.timeLimitMinutes)
            )
        },
        onLongClick = { exam: Exam, isPinned: Boolean, anchorView: View ->
            showPinPopupMenu(exam, isPinned, anchorView)
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

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvWelcome.text = "Hi, ${user.displayName ?: "Student"}"

        setupNotificationBell()
        setupPushNotifications()
        setupFeedbackCard()

        val notifFilter = IntentFilter("com.eve.app.NOTIFICATION_RECEIVED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(foregroundNotificationReceiver, notifFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(foregroundNotificationReceiver, notifFilter)
        }

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
                onThemeToggle = { cx, cy -> ThemeManager.toggleWithCircularReveal(this, cx, cy) },
                onHistory = { startActivity(Intent(this, HistoryActivity::class.java)) },
                onPerformance = { startActivity(Intent(this, PerformanceActivity::class.java)) },
                onTopic = { startActivity(Intent(this, PracticeActivity::class.java)) },
                onPyq = { startActivity(Intent(this, PyqActivity::class.java)) },
                onSyllabus = { startActivity(Intent(this, SyllabusActivity::class.java)) },
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
            val filtered = lastLoadedItems.filterIsInstance<HomeListItem.ExamRow>()
                .filter { it.exam.examName.lowercase().contains(q) || it.exam.categoryOrOther.lowercase().contains(q) }

            if (filtered.isEmpty()) {
                adapter.submit(emptyList())
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.ivMessageIcon, hasEmptyPlayed)
                binding.tvMessage.text = "No matching exams"
                binding.tvMessageSub.text = "Try searching for a different keyword or category."
            } else {
                binding.messageGroup.visibility = View.GONE
                hasEmptyPlayed = false
                adapter.submit(filtered)
            }
        } else {
            val examRows = lastLoadedItems.filterIsInstance<HomeListItem.ExamRow>()
            if (examRows.isEmpty()) {
                adapter.submit(emptyList())
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.GONE
                hasEmptyPlayed = com.eve.app.util.EmptyStateAnimationHelper.showEmptyState(binding.ivMessageIcon, hasEmptyPlayed)
                binding.tvMessage.text = "No exams available"
                binding.tvMessageSub.text = "Check back soon for newly published mock tests."
            } else {
                binding.messageGroup.visibility = View.GONE
                hasEmptyPlayed = false
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
            val msg = if (isPinned) "Test unpinned" else "Test pinned to top"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            true
        }
        popup.show()
    }

    private fun setupFeedbackCard() {
        binding.btnSendHomeFeedback.setOnClickListener {
            val message = binding.etHomeFeedback.text?.toString()?.trim().orEmpty()
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter your message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (message.length > 1000) {
                Toast.makeText(this, "Message cannot exceed 1000 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val uid = user.uid
            val name = user.displayName ?: "Student"
            val email = user.email ?: ""

            binding.btnSendHomeFeedback.isEnabled = false
            lifecycleScope.launch {
                val result = feedbackRepository.sendFeedback(uid, name, email, message)
                binding.btnSendHomeFeedback.isEnabled = true
                result.onSuccess {
                    Toast.makeText(this@MainActivity, "Thank you! Your feedback has been sent.", Toast.LENGTH_LONG).show()
                    binding.etHomeFeedback.text = null
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.hideSoftInputFromWindow(binding.etHomeFeedback.windowToken, 0)
                }.onFailure { err ->
                    Toast.makeText(this@MainActivity, "Failed to send: ${err.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                }
            }
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
        }
    }

    override fun onStop() {
        super.onStop()
        firestoreNotifRegistration?.remove()
        firestoreNotifRegistration = null
    }

    private fun startListeningToNotifications() {
        firestoreNotifRegistration?.remove()
        firestoreNotifRegistration = FirebaseFirestore.getInstance()
            .collection("notifications")
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || snapshot.isEmpty) return@addSnapshotListener
                val doc = snapshot.documents.firstOrNull() ?: return@addSnapshotListener
                val sentAt = doc.getTimestamp("sentAt")?.toDate()?.time
                    ?: doc.getLong("sentAt")
                    ?: 0L
                val lastSeen = NotificationStore.getLastSeenTimestamp(this)
                if (sentAt > lastSeen && lastSeen > 0L) {
                    val title = doc.getString("title") ?: "New Notification"
                    val body = doc.getString("message") ?: ""
                    NotificationStore.add(this, title, body)
                    VibrationHelper.vibrateNotification(this)
                    binding.btnNotification.repeatCount = 0
                    binding.btnNotification.progress = 0f
                    binding.btnNotification.playAnimation()
                    updateNotificationDot()
                }
            }
    }

    private fun updateNotificationDot() {
        binding.dotUnread.visibility = if (NotificationStore.hasUnread(this)) View.VISIBLE else View.GONE
    }

    private fun setupNotificationBell() {
        val textColor = ContextCompat.getColor(this, R.color.eve_text)
        binding.btnNotification.addValueCallback(
            com.airbnb.lottie.model.KeyPath("**"),
            com.airbnb.lottie.LottieProperty.COLOR_FILTER
        ) {
            android.graphics.PorterDuffColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_ATOP)
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

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(foregroundNotificationReceiver)
        } catch (_: Exception) { }
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
                binding.ivMessageIcon.setAnimation(R.raw.error_404)
                binding.ivMessageIcon.playAnimation()
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

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut()
        goToLogin()
    }
}

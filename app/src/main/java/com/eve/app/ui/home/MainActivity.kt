package com.eve.app.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import com.eve.app.R
import com.eve.app.data.repository.AdminRepository
import com.eve.app.databinding.ActivityMainBinding
import com.eve.app.ui.admin.AdminActivity
import com.eve.app.ui.history.HistoryActivity
import com.eve.app.ui.leaderboard.LeaderboardActivity
import com.eve.app.ui.login.LoginActivity
import com.eve.app.ui.notifications.NotificationsActivity
import com.eve.app.ui.profile.ProfileActivity
import com.eve.app.ui.daily.DailyQuizActivity
import com.eve.app.ui.practice.PracticeActivity
import com.eve.app.ui.test.TestActivity
import com.eve.app.util.Constants
import com.eve.app.util.DateUtil
import com.eve.app.util.StreakStore
import com.eve.app.util.CrashlyticsHelper
import com.eve.app.util.NetworkUtil
import com.eve.app.util.NotificationHelper
import com.eve.app.util.NotificationStore
import com.eve.app.util.ProfilePhotoManager
import com.eve.app.util.ReminderScheduler
import com.eve.app.util.ThemeManager
import com.eve.app.util.UiState
import com.eve.app.util.isHardcodedAdmin
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HomeViewModel by viewModels()
    private val adminRepo = AdminRepository()

    // Phase 12: Android 13+ par POST_NOTIFICATIONS permission runtime me maangni padti hai.
    // User "Deny" bhi kar de to app normally chalti rahegi, sirf naya-exam/reminder alerts nahi dikhenge.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignore kar sakte hain */ }

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

        // Bug fix: bell icon ab daily-reminder toggle nahi, balki Notifications list kholta hai
        // (reminder ON/OFF ab Profile screen me shift kar diya gaya hai).
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        setupPushNotifications()
        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        loadProfilePhoto(user)

        // Hardcoded admin ho to turant dikhao (fast path, koi network wait nahi)
        if (isHardcodedAdmin(user.email)) {
            showAdminButton()
            viewModel.loadForUser(user.uid, true)
        } else {
            // Students ke liye attempted state bhi load hota hai. Dynamic admin hone par
            // restriction hata kar normal admin preview/retry behaviour preserve hota hai.
            lifecycleScope.launch {
                val admin = adminRepo.isAdmin(user.email)
                if (admin) showAdminButton()
                viewModel.loadForUser(user.uid, admin)
            }
        }

        binding.btnOverflow.setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                val isDark = ThemeManager.isDarkMode(this@MainActivity)
                val themeTitle = if (isDark) "Light Mode" else "Dark Mode"
                menu.add(0, 0, 0, themeTitle)
                menu.add(0, 1, 1, getString(com.eve.app.R.string.home_menu_history))
                menu.add(0, 2, 2, getString(com.eve.app.R.string.home_menu_performance))
                menu.add(0, 3, 3, getString(com.eve.app.R.string.home_menu_topic_test))
                menu.add(0, 4, 4, getString(com.eve.app.R.string.home_menu_pyq))
                menu.add(0, 5, 5, getString(R.string.home_menu_overall_leaderboard))

                val logoutStr = getString(R.string.home_menu_logout)
                val logoutTitle = SpannableString(logoutStr).apply {
                    setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(this@MainActivity, R.color.eve_red)),
                        0,
                        length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                menu.add(0, 6, 6, logoutTitle)

                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        0 -> ThemeManager.toggleWithReveal(this@MainActivity, anchor)
                        1 -> startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
                        2 -> startActivity(Intent(this@MainActivity, com.eve.app.ui.performance.PerformanceActivity::class.java))
                        3 -> startActivity(Intent(this@MainActivity, PracticeActivity::class.java))
                        4 -> startActivity(Intent(this@MainActivity, com.eve.app.ui.pyq.PyqActivity::class.java))
                        5 -> startActivity(
                            Intent(this@MainActivity, LeaderboardActivity::class.java).apply {
                                putExtra(Constants.EXTRA_EXAM_ID, "overall")
                                putExtra(Constants.EXTRA_EXAM_NAME, "Overall Leaderboard")
                            }
                        )
                        6 -> logout()
                    }
                    true
                }
                show()
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

    override fun onResume() {
        super.onResume()
        // Heartbeat: updates lastActive so Admin Dashboard "Online Now" counter stays accurate
        FirebaseAuth.getInstance().currentUser?.let { user ->
            lifecycleScope.launch {
                try {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(user.uid)
                        .update("lastActive", System.currentTimeMillis())
                } catch (_: Exception) {
                    // Ignore stats update errors (e.g. offline)
                }
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
        // Admin ne naya exam add kiya ho to list refresh ho jaye
        if (::binding.isInitialized) {
            FirebaseAuth.getInstance().currentUser?.let { current ->
                lifecycleScope.launch {
                    val admin = isHardcodedAdmin(current.email) || adminRepo.isAdmin(current.email)
                    viewModel.loadForUser(current.uid, admin)
                }
            }
            // Profile screen se photo badal ke wapas aaya ho to header par bhi turant update ho
            FirebaseAuth.getInstance().currentUser?.let { loadProfilePhoto(it) }
            // Notifications screen se wapas aaye ho (sab read ho chuke) to dot hat jaye
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
            "Aaj attempt ho chuka • $score/$total$streakText"
        } else {
            "Aaj ka current affairs quiz$streakText"
        }
    }

    private fun updateNotificationDot() {
        binding.dotUnread.visibility = if (NotificationStore.hasUnread(this)) View.VISIBLE else View.GONE
    }

    private fun loadProfilePhoto(user: com.google.firebase.auth.FirebaseUser) {
        ProfilePhotoManager.applyTo(
            this, binding.ivProfile, user.photoUrl?.toString(), com.eve.app.R.drawable.bg_circle_translucent
        )
    }

    /**
     * Phase 12 setup: notification channels banao, Android 13+ par permission maango, naye-exam
     * FCM topic subscribe karo, aur saved reminder preference ke hisaab se daily reminder schedule karo.
     */
    private fun setupPushNotifications() {
        NotificationHelper.createChannels(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        FirebaseMessaging.getInstance().subscribeToTopic(NotificationHelper.TOPIC_NEW_EXAMS)
        ReminderScheduler.applySavedState(this)
    }

    private fun showAdminButton() {
        binding.btnAdmin.visibility = View.VISIBLE
        binding.btnAdmin.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
        // Phase 15: ab crash reports me pata chalega ki crash admin ke saath hua ya student ke
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
                renderChips(data.categories, data.selectedCategory)
                adapter.submit(data.items)
                val empty = data.items.isEmpty()
                binding.messageGroup.visibility = if (empty) View.VISIBLE else View.GONE
                if (empty) {
                    binding.ivMessageIcon.setImageResource(com.eve.app.R.drawable.ic_state_empty)
                    if (data.categories.size <= 1) {
                        binding.tvMessage.text = "Abhi koi exam available nahi hai"
                        binding.tvMessageSub.text = "Admin ke naya exam add karte hi yahan dikhega"
                    } else {
                        binding.tvMessage.text = "Is category me abhi koi exam nahi hai"
                        binding.tvMessageSub.text = "Koi aur category try karo ya \"All\" par wapas jao"
                    }
                }
                binding.chipGroupCategory.visibility = if (data.categories.size <= 1) View.GONE else View.VISIBLE
            }
            is UiState.Error -> {
                binding.progressGroup.visibility = View.GONE
                binding.messageGroup.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.ivMessageIcon.setImageResource(com.eve.app.R.drawable.ic_state_error)
                if (NetworkUtil.isOnline(this)) {
                    binding.tvMessage.text = "Kuch gadbad ho gayi"
                    binding.tvMessageSub.text = state.message
                } else {
                    binding.tvMessage.text = "No internet connection"
                    binding.tvMessageSub.text =
                        "Exams abhi tak cache nahi hue. Network wapas aane par retry karo."
                }
                binding.chipGroupCategory.visibility = View.GONE
            }
        }
    }

    private fun renderChips(categories: List<String>, selected: String) {
        // Ek hi jaisi list dobara build na ho isliye simple guard
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

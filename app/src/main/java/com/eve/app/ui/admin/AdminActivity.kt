package com.eve.app.ui.admin

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.BuildConfig
import com.eve.app.R
import com.eve.app.data.model.AppConfig
import com.eve.app.data.model.Exam
import com.eve.app.data.model.FeedbackPost
import com.eve.app.data.model.FeedbackPostReply
import com.eve.app.data.model.HomeBanner
import com.eve.app.data.model.Question
import com.eve.app.data.remote.AdminMeData
import com.eve.app.data.remote.ApiClient
import com.eve.app.data.repository.AdminRepository
import com.eve.app.data.repository.ExamRepository
import com.eve.app.data.repository.FeedbackRepository
import com.eve.app.data.repository.FloatingLinkRepository
import com.eve.app.data.repository.HomeBannerRepository
import com.eve.app.databinding.ActivityAdminBinding
import com.eve.app.databinding.DialogCreateFeedbackPostBinding
import com.eve.app.databinding.DialogManageBannersBinding
import com.eve.app.databinding.DialogPostRepliesBinding
import com.eve.app.designsystem.TelegramNativeAlertDialog
import com.eve.app.ui.admin.tg.TextCell
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AdminAccessGate
import com.eve.app.util.AppBulletin
import com.eve.app.util.AvatarDrawable
import com.eve.app.util.ExamImageHelper
import com.eve.app.util.HomeBannerLinkValidator
import com.eve.app.util.QuestionImportHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminActivity : EveBaseActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()
    private val adminRepo = AdminRepository()
    private val bannerRepo = HomeBannerRepository()
    private val examRepo = ExamRepository()
    private val floatingLinkRepo = FloatingLinkRepository()
    private val feedbackRepo = FeedbackRepository()

    private var exams: List<Exam> = emptyList()
    private var selectedExamForImageUpdate: Exam? = null
    private var onBannerImageSelected: ((Uri) -> Unit)? = null
    private var currentConfig: AppConfig? = null

    private val pickBannerImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            onBannerImageSelected?.invoke(uri)
        }
    }

    private val pickEditExamImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val exam = selectedExamForImageUpdate ?: return@registerForActivityResult
        if (uri != null) {
            val base64 = ExamImageHelper.uriToBase64(this, uri)
            if (base64 != null) {
                viewModel.updateExamImage(exam.id, base64) {
                    selectedExamForImageUpdate = null
                }
            } else {
                AppBulletin.showError(this, "Failed to load image")
            }
        }
    }

    // Search system items
    data class SearchItem(
        val title: String,
        val section: String,
        val keywords: List<String>,
        val iconRes: Int? = null,
        val lottieRes: Int? = null,
        val requiredRight: String? = null,
        val action: () -> Unit
    )

    private val searchItems = mutableListOf<SearchItem>()
    private val searchAdapter = SearchResultsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AdminAccessGate.requireAdmin(this, binding.root, requireFreshServer = true) {
            binding.progressBarAdmin.visibility = View.GONE
            setupActionBar()
            setupHeaderBlock()
            setupHubSections()
            setupSearchIndex()
            setupObservers()
            loadData()
        }
    }

    private fun setupActionBar() {
        binding.telegramActionBar.setTitle("Admin Dashboard")
        binding.telegramActionBar.setSubtitle("EVE Control Center")
        binding.telegramActionBar.onBackPressed = { finish() }

        binding.telegramActionBar.onSearchTextChanged = { query ->
            performSearch(query)
        }
        binding.telegramActionBar.onSearchCollapsed = {
            binding.rvAdminSearch.visibility = View.GONE
            binding.scrollAdminHub.visibility = View.VISIBLE
        }

        binding.rvAdminSearch.layoutManager = LinearLayoutManager(this)
        binding.rvAdminSearch.adapter = searchAdapter
    }

    private fun setupHeaderBlock() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email.orEmpty().trim().lowercase(Locale.ROOT)
        val name = user?.displayName ?: if (email.contains("@")) email.substringBefore("@") else "Administrator"

        binding.tvAdminName.text = name
        binding.tvAdminEmail.text = email
        val isOwner = email == "own.keni@gmail.com" || email == "admin@eve.app"
        binding.tvAdminRole.text = if (isOwner) "Owner" else "Administrator"

        val avatar = AvatarDrawable.create(this, name, email)
        binding.ivAdminAvatar.setImageDrawable(avatar)
    }

    private fun setupHubSections() {
        // SECTION 1: CONTENT
        binding.headerContent.setText("CONTENT")

        binding.rowExams.setTextAndLottie("Exams", R.raw.tg_folder_in)
        binding.rowExams.setOnClickListener { showExamsOptionsDialog() }

        binding.rowQuestions.setTextAndIcon("Questions", R.drawable.ic_edit)
        binding.rowQuestions.setOnClickListener {
            startActivity(Intent(this, ManageQuestionsActivity::class.java))
        }

        binding.rowGeneratedTests.setTextAndLottie("AI Generated Tests", R.raw.tg_qr_code_logo)
        binding.rowGeneratedTests.setOnClickListener {
            startActivity(Intent(this, GeneratedTestsActivity::class.java))
        }

        binding.rowSyllabus.setTextAndLottie("Syllabus", R.raw.tg_saved_messages)
        binding.rowSyllabus.setOnClickListener {
            startActivity(Intent(this, ManageSyllabusActivity::class.java))
        }

        binding.rowHomeBanner.setTextAndIcon("Home Banner", R.drawable.ic_image)
        binding.rowHomeBanner.setOnClickListener { showHomeBannerOptionsDialog() }

        binding.rowFloatingLink.setTextAndIcon("Floating Link", R.drawable.ic_link)
        binding.rowFloatingLink.setOnClickListener { showFloatingLinkDialog() }

        binding.rowAnimatedEmoji.setTextAndLottie("Animated Emoji", R.raw.tg_fire_on, needDivider = false)
        binding.rowAnimatedEmoji.setOnClickListener {
            AppBulletin.showSuccess(this, "Animated Emoji Studio (Configured in Mission 03 F4)")
        }

        // SECTION 2: PEOPLE
        binding.headerPeople.setText("PEOPLE")

        binding.rowUsers.setTextAndLottie("Users", R.raw.tg_chats_archive)
        binding.rowUsers.setOnClickListener {
            startActivity(Intent(this, ManageUsersActivity::class.java))
        }

        binding.rowAdmins.setTextAndLottie("Admins and Rights", R.raw.tg_ic_admin)
        binding.rowAdmins.setOnClickListener {
            startActivity(Intent(this, AdminsAndRightsActivity::class.java))
        }

        binding.rowPremium.setTextAndLottie("Premium Management", R.raw.premium_star, needDivider = false)
        binding.rowPremium.setOnClickListener {
            startActivity(Intent(this, ManagePremiumActivity::class.java))
        }

        // SECTION 3: COMMUNICATION
        binding.headerCommunication.setText("COMMUNICATION")

        binding.rowBroadcast.setTextAndIcon("Broadcast Notification", R.drawable.ic_broadcast)
        binding.rowBroadcast.setOnClickListener {
            startActivity(Intent(this, SendNotificationActivity::class.java))
        }

        binding.rowFlaggedQuestions.setTextAndLottie("Flagged Questions Inbox", R.raw.tg_report_police)
        binding.rowFlaggedQuestions.setOnClickListener {
            startActivity(Intent(this, FlaggedQuestionsActivity::class.java))
        }

        binding.rowFeedback.setTextAndIcon("Feedback Messages", R.drawable.ic_message_dots)
        binding.rowFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackMessagesActivity::class.java))
        }

        binding.rowCreateFeedbackPost.setTextAndIcon("Create Feedback Post", R.drawable.ic_campaign)
        binding.rowCreateFeedbackPost.setOnClickListener { showCreateFeedbackPostDialog() }

        binding.btnManageFeedbackPosts.setTextAndIcon("Manage Feedback Posts", R.drawable.ic_edit)
        binding.btnManageFeedbackPosts.setOnClickListener { showManageFeedbackPostsDialog() }

        binding.rowPolls.setTextAndLottie("Polls", R.raw.tg_tab_poll, needDivider = false)
        binding.rowPolls.setOnClickListener {
            startActivity(Intent(this, ManagePollsActivity::class.java))
        }

        // SECTION 4: INSIGHTS
        binding.headerInsights.setText("INSIGHTS")

        binding.rowAnalytics.setTextAndLottie("Analytics & Crash Reports", R.raw.tg_statistic_preload)
        binding.rowAnalytics.setOnClickListener {
            startActivity(Intent(this, AdminAnalyticsActivity::class.java))
        }

        binding.rowRecentActions.setTextAndLottie("Recent Actions (Audit Log)", R.raw.tg_filters)
        binding.rowRecentActions.setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }

        binding.rowSystemMonitor.setTextAndIcon("System Health & Operations", R.drawable.ic_maintenance)
        binding.rowSystemMonitor.setOnClickListener {
            startActivity(Intent(this, SystemMonitorActivity::class.java))
        }

        binding.rowApiUsage.setTextAndLottie("API Usage & Cost Monitor", R.raw.tg_statistic_preload, needDivider = false)
        binding.rowApiUsage.setOnClickListener {
            startActivity(Intent(this, ApiUsageActivity::class.java))
        }

        // SECTION 5: SYSTEM
        binding.headerSystem.setText("SYSTEM")

        binding.cellMaintenance.setTextAndCheck("Maintenance Mode", checked = false)
        binding.cellMaintenance.setSubtitle("When active, students are blocked by notice")
        binding.cellMaintenance.setOnCheckedChangeListener { _, isChecked ->
            handleMaintenanceToggle(isChecked)
        }

        binding.rowAppConfig.setTextAndIcon("App Config & Maintenance", R.drawable.ic_maintenance)
        binding.rowAppConfig.setOnClickListener {
            startActivity(Intent(this, AppConfigActivity::class.java))
        }

        binding.rowVersionStatus.setTextAndIcon("Version Status", R.drawable.ic_info)
        binding.rowVersionStatus.setValue("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        binding.rowVersionStatus.setOnClickListener {
            startActivity(Intent(this, AppConfigActivity::class.java))
        }

        binding.rowExportData.setTextAndIcon("Export All Exam Data (JSON)", R.drawable.ic_download)
        binding.rowExportData.setOnClickListener { exportExamData() }

        binding.rowEditAbout.setTextAndIcon("Edit About / Privacy / Terms", R.drawable.ic_edit, needDivider = false)
        binding.rowEditAbout.setOnClickListener {
            startActivity(Intent(this, EditAboutActivity::class.java))
        }
    }

    private fun setupSearchIndex(adminMe: AdminMeData? = null) {
        searchItems.clear()
        val allCandidates = listOf(
            SearchItem("Exams", "CONTENT", listOf("exams", "परीक्षा", "mock tests", "manage exams"), lottieRes = R.raw.tg_folder_in, requiredRight = "manage_exams") { showExamsOptionsDialog() },
            SearchItem("Manage Existing Exams", "CONTENT", listOf("existing exams", "published", "edit exams"), iconRes = R.drawable.ic_exam_placeholder, requiredRight = "manage_exams") { startActivity(Intent(this, ManageExistingExamsActivity::class.java)) },
            SearchItem("Manage Exams (Hierarchy)", "CONTENT", listOf("create exam", "exam tree", "category"), iconRes = R.drawable.ic_edit, requiredRight = "manage_exams") { startActivity(Intent(this, ManageExamsActivity::class.java)) },
            SearchItem("Questions", "CONTENT", listOf("questions", "प्रश्न", "question bank", "mcq"), iconRes = R.drawable.ic_edit, requiredRight = "manage_questions") { startActivity(Intent(this, ManageQuestionsActivity::class.java)) },
            SearchItem("AI Generated Tests", "CONTENT", listOf("ai tests", "एआई टेस्ट", "automated", "generate test"), lottieRes = R.raw.tg_qr_code_logo, requiredRight = "manage_generated_tests") { startActivity(Intent(this, GeneratedTestsActivity::class.java)) },
            SearchItem("Syllabus", "CONTENT", listOf("syllabus", "पाठ्यक्रम", "pdf", "topics"), lottieRes = R.raw.tg_saved_messages, requiredRight = "manage_syllabus") { startActivity(Intent(this, ManageSyllabusActivity::class.java)) },
            SearchItem("Home Banner", "CONTENT", listOf("home banner", "बैनर", "promotions", "banner link"), iconRes = R.drawable.ic_image, requiredRight = "manage_content") { showHomeBannerOptionsDialog() },
            SearchItem("Floating Link", "CONTENT", listOf("floating link", "फ्लोटिंग लिंक", "quick link", "url"), iconRes = R.drawable.ic_link, requiredRight = "manage_content") { showFloatingLinkDialog() },
            SearchItem("Animated Emoji", "CONTENT", listOf("animated emoji", "इमोजी", "stickers", "studio"), lottieRes = R.raw.tg_fire_on, requiredRight = "manage_content") { AppBulletin.showSuccess(this, "Animated Emoji Studio (Configured in Mission 03 F4)") },
            SearchItem("Users", "PEOPLE", listOf("users", "उपयोगकर्ता", "students", "ban user", "signups"), lottieRes = R.raw.tg_chats_archive, requiredRight = "manage_users") { startActivity(Intent(this, ManageUsersActivity::class.java)) },
            SearchItem("Admins and Rights", "PEOPLE", listOf("admins", "एडमिन", "admin rights", "permissions"), lottieRes = R.raw.tg_ic_admin, requiredRight = "manage_admins_rights") { startActivity(Intent(this, AdminsAndRightsActivity::class.java)) },
            SearchItem("Premium Management", "PEOPLE", listOf("premium", "प्रीमियम", "subscription", "grant premium"), lottieRes = R.raw.premium_star, requiredRight = "manage_premium") { startActivity(Intent(this, ManagePremiumActivity::class.java)) },
            SearchItem("Broadcast Notification", "COMMUNICATION", listOf("broadcast", "प्रसारण", "notification", "push"), iconRes = R.drawable.ic_broadcast, requiredRight = "send_broadcasts") { startActivity(Intent(this, SendNotificationActivity::class.java)) },
            SearchItem("Flagged Questions Inbox", "COMMUNICATION", listOf("flagged questions", "फ्लैग्ड", "reports", "moderation"), lottieRes = R.raw.tg_report_police, requiredRight = "moderate_feedback") { startActivity(Intent(this, FlaggedQuestionsActivity::class.java)) },
            SearchItem("Feedback Messages", "COMMUNICATION", listOf("feedback", "प्रतिक्रिया", "replies", "student messages"), iconRes = R.drawable.ic_message_dots, requiredRight = "moderate_feedback") { startActivity(Intent(this, FeedbackMessagesActivity::class.java)) },
            SearchItem("Create Feedback Post", "COMMUNICATION", listOf("create feedback post", "नया पोस्ट", "survey"), iconRes = R.drawable.ic_campaign, requiredRight = "moderate_feedback") { showCreateFeedbackPostDialog() },
            SearchItem("Manage Feedback Posts", "COMMUNICATION", listOf("manage feedback posts", "पोस्ट प्रबंधन"), iconRes = R.drawable.ic_edit, requiredRight = "moderate_feedback") { showManageFeedbackPostsDialog() },
            SearchItem("Polls", "COMMUNICATION", listOf("polls", "सर्वेक्षण", "vote", "student poll"), lottieRes = R.raw.tg_tab_poll, requiredRight = "moderate_feedback") { startActivity(Intent(this, ManagePollsActivity::class.java)) },
            SearchItem("Analytics & Crash Reports", "INSIGHTS", listOf("analytics", "एनालिटिक्स", "reports", "stats"), lottieRes = R.raw.tg_statistic_preload, requiredRight = "view_analytics_logs") { startActivity(Intent(this, AdminAnalyticsActivity::class.java)) },
            SearchItem("Recent Actions (Audit Log)", "INSIGHTS", listOf("recent actions", "audit log", "गतिविधियां", "history"), lottieRes = R.raw.tg_filters, requiredRight = "view_analytics_logs") { startActivity(Intent(this, ActivityLogActivity::class.java)) },
            SearchItem("System Health & Operations", "INSIGHTS", listOf("system health", "operations", "health monitor"), iconRes = R.drawable.ic_maintenance, requiredRight = "view_analytics_logs") { startActivity(Intent(this, SystemMonitorActivity::class.java)) },
            SearchItem("API Usage & Cost Monitor", "INSIGHTS", listOf("api usage", "cost monitor", "usage tracker", "gemini calls"), lottieRes = R.raw.tg_statistic_preload, requiredRight = "view_analytics_logs") { startActivity(Intent(this, ApiUsageActivity::class.java)) },
            SearchItem("App Config & Maintenance", "SYSTEM", listOf("app config", "maintenance", "रखरखाव", "config"), iconRes = R.drawable.ic_maintenance, requiredRight = "manage_config_maintenance") { startActivity(Intent(this, AppConfigActivity::class.java)) },
            SearchItem("Version Status", "SYSTEM", listOf("version status", "वर्जन", "build info", "min version"), iconRes = R.drawable.ic_info) { startActivity(Intent(this, AppConfigActivity::class.java)) },
            SearchItem("Export All Exam Data", "SYSTEM", listOf("export data", "निर्यात", "backup json", "download exams"), iconRes = R.drawable.ic_download, requiredRight = "export_data") { exportExamData() },
            SearchItem("Edit About / Privacy / Terms", "SYSTEM", listOf("edit about", "privacy policy", "terms of service"), iconRes = R.drawable.ic_edit, requiredRight = "manage_content") { startActivity(Intent(this, EditAboutActivity::class.java)) }
        )

        val allowed = if (adminMe == null || adminMe.isOwner) {
            allCandidates
        } else {
            allCandidates.filter { it.requiredRight == null || adminMe.rights.contains(it.requiredRight) }
        }
        searchItems.addAll(allowed)
    }

    private fun applyAdminPermissions(adminMe: AdminMeData?) {
        if (adminMe == null) return

        if (!adminMe.customTitle.isNullOrBlank()) {
            binding.tvAdminRole.text = adminMe.customTitle
        } else {
            binding.tvAdminRole.text = if (adminMe.isOwner) "Owner" else "Administrator"
        }

        if (adminMe.isOwner) {
            setupSearchIndex(null)
            return
        }

        fun has(right: String): Boolean = adminMe.rights.contains(right)

        // SECTION 1: CONTENT
        binding.rowExams.visibility = if (has("manage_exams")) View.VISIBLE else View.GONE
        binding.rowQuestions.visibility = if (has("manage_questions")) View.VISIBLE else View.GONE
        binding.rowGeneratedTests.visibility = if (has("manage_generated_tests")) View.VISIBLE else View.GONE
        binding.rowSyllabus.visibility = if (has("manage_syllabus")) View.VISIBLE else View.GONE
        binding.rowHomeBanner.visibility = if (has("manage_content")) View.VISIBLE else View.GONE
        binding.rowFloatingLink.visibility = if (has("manage_content")) View.VISIBLE else View.GONE
        binding.rowAnimatedEmoji.visibility = if (has("manage_content")) View.VISIBLE else View.GONE

        val contentVisible = listOf(
            binding.rowExams, binding.rowQuestions, binding.rowGeneratedTests,
            binding.rowSyllabus, binding.rowHomeBanner, binding.rowFloatingLink,
            binding.rowAnimatedEmoji
        ).any { it.visibility == View.VISIBLE }
        binding.headerContent.visibility = if (contentVisible) View.VISIBLE else View.GONE

        // SECTION 2: PEOPLE
        binding.rowUsers.visibility = if (has("manage_users")) View.VISIBLE else View.GONE
        binding.rowAdmins.visibility = if (has("manage_admins_rights")) View.VISIBLE else View.GONE
        binding.rowPremium.visibility = if (has("manage_premium")) View.VISIBLE else View.GONE

        val peopleVisible = listOf(binding.rowUsers, binding.rowAdmins, binding.rowPremium).any { it.visibility == View.VISIBLE }
        binding.headerPeople.visibility = if (peopleVisible) View.VISIBLE else View.GONE

        // SECTION 3: COMMUNICATION
        binding.rowBroadcast.visibility = if (has("send_broadcasts")) View.VISIBLE else View.GONE
        binding.rowFlaggedQuestions.visibility = if (has("moderate_feedback")) View.VISIBLE else View.GONE
        binding.rowFeedback.visibility = if (has("moderate_feedback")) View.VISIBLE else View.GONE
        binding.rowCreateFeedbackPost.visibility = if (has("moderate_feedback")) View.VISIBLE else View.GONE
        binding.btnManageFeedbackPosts.visibility = if (has("moderate_feedback")) View.VISIBLE else View.GONE
        binding.rowPolls.visibility = if (has("moderate_feedback")) View.VISIBLE else View.GONE

        val commVisible = listOf(
            binding.rowBroadcast, binding.rowFlaggedQuestions, binding.rowFeedback,
            binding.rowCreateFeedbackPost, binding.btnManageFeedbackPosts, binding.rowPolls
        ).any { it.visibility == View.VISIBLE }
        binding.headerCommunication.visibility = if (commVisible) View.VISIBLE else View.GONE

        // SECTION 4: INSIGHTS
        binding.rowAnalytics.visibility = if (has("view_analytics_logs")) View.VISIBLE else View.GONE
        binding.rowRecentActions.visibility = if (has("view_analytics_logs")) View.VISIBLE else View.GONE
        binding.rowSystemMonitor.visibility = if (has("view_analytics_logs")) View.VISIBLE else View.GONE
        binding.rowApiUsage.visibility = if (has("view_analytics_logs")) View.VISIBLE else View.GONE

        val insightsVisible = listOf(
            binding.rowAnalytics, binding.rowRecentActions, binding.rowSystemMonitor, binding.rowApiUsage
        ).any { it.visibility == View.VISIBLE }
        binding.headerInsights.visibility = if (insightsVisible) View.VISIBLE else View.GONE

        // SECTION 5: SYSTEM
        binding.cellMaintenance.visibility = if (has("manage_config_maintenance")) View.VISIBLE else View.GONE
        binding.rowAppConfig.visibility = if (has("manage_config_maintenance")) View.VISIBLE else View.GONE
        binding.rowExportData.visibility = if (has("export_data")) View.VISIBLE else View.GONE
        binding.rowEditAbout.visibility = if (has("manage_content")) View.VISIBLE else View.GONE

        setupSearchIndex(adminMe)
    }

    private fun performSearch(query: String) {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) {
            binding.rvAdminSearch.visibility = View.GONE
            binding.scrollAdminHub.visibility = View.VISIBLE
            return
        }

        val filtered = searchItems.filter { item ->
            item.title.lowercase(Locale.ROOT).contains(trimmed) ||
            item.section.lowercase(Locale.ROOT).contains(trimmed) ||
            item.keywords.any { it.lowercase(Locale.ROOT).contains(trimmed) }
        }

        binding.scrollAdminHub.visibility = View.GONE
        binding.rvAdminSearch.visibility = View.VISIBLE
        searchAdapter.submit(filtered)
    }

    private inner class SearchResultsAdapter : RecyclerView.Adapter<SearchResultsAdapter.VH>() {
        private var items: List<SearchItem> = emptyList()

        fun submit(list: List<SearchItem>) {
            items = list
            notifyDataSetChanged()
        }

        inner class VH(val cell: TextCell) : RecyclerView.ViewHolder(cell)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val cell = TextCell(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return VH(cell)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            if (item.lottieRes != null) {
                holder.cell.setTextAndLottie(item.title, item.lottieRes)
            } else if (item.iconRes != null) {
                holder.cell.setTextAndIcon(item.title, item.iconRes)
            } else {
                holder.cell.setText(item.title)
            }
            holder.cell.setSubtitle(item.section)
            holder.cell.setOnClickListener {
                binding.telegramActionBar.closeSearch()
                item.action.invoke()
            }
        }

        override fun getItemCount(): Int = items.size
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.exams.collect { examList ->
                        exams = examList
                    }
                }
                launch {
                    viewModel.totalUsers.collect { total ->
                        if (total != null && total > 0) {
                            binding.rowUsers.setValue("$total users")
                        }
                    }
                }
            }
        }
    }

    private fun loadData() {
        viewModel.loadExams()
        viewModel.loadUserStats()
        viewModel.loadAdminMe { adminMe ->
            applyAdminPermissions(adminMe)
        }

        lifecycleScope.launch {
            try {
                val config = adminRepo.getAppConfig()
                currentConfig = config
                binding.cellMaintenance.isChecked = config.maintenance_mode
                binding.rowVersionStatus.setValue("v${BuildConfig.VERSION_NAME} (Min: ${config.minimum_supported_version_code})")
            } catch (_: Exception) {}
        }
    }

    private fun handleMaintenanceToggle(enable: Boolean) {
        val config = currentConfig ?: AppConfig(
            minimum_supported_version_code = 1,
            maintenance_mode = false,
            maintenance_message = "App is temporarily undergoing maintenance."
        )

        if (enable) {
            binding.cellMaintenance.isChecked = false
            TelegramNativeAlertDialog.Builder(this)
                .setTitle("Enable Maintenance Mode?")
                .setMessage("Are you sure? When maintenance mode is active, students opening the app will be blocked by a full-screen maintenance notice.")
                .setPositiveButton("Enable Maintenance") { _, _ ->
                    binding.cellMaintenance.isChecked = true
                    val updated = config.copy(maintenance_mode = true)
                    saveMaintenanceConfig(updated)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    binding.cellMaintenance.isChecked = false
                }
                .show()
        } else {
            val updated = config.copy(maintenance_mode = false)
            saveMaintenanceConfig(updated)
        }
    }

    private fun saveMaintenanceConfig(config: AppConfig) {
        lifecycleScope.launch {
            try {
                adminRepo.updateAppConfig(config)
                currentConfig = config
                AppBulletin.showSuccess(this@AdminActivity, "Maintenance settings saved")
            } catch (e: Exception) {
                binding.cellMaintenance.isChecked = currentConfig?.maintenance_mode ?: false
                AppBulletin.showError(this@AdminActivity, "Failed to update maintenance: ${e.message}")
            }
        }
    }

    private fun showExamsOptionsDialog() {
        val options = arrayOf("Manage Existing Exams (Published/Drafts)", "Manage Exams (Hierarchy & Details)")
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Exams Management")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, ManageExistingExamsActivity::class.java))
                    1 -> startActivity(Intent(this, ManageExamsActivity::class.java))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Retained question/exam helpers for contract verification
    private fun showAddQuestionDialog(selectedExam: Exam) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_question, null)
        val etQuestion = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etQuestionText)
        val etOptA = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionA)
        val etOptB = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionB)
        val etOptC = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionC)
        val etOptD = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etOptionD)
        val rgCorrect = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgCorrectAnswer)
        val etExplanation = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etExplanation)
        val etTopic = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTopic)

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Add Question to ${selectedExam.examName}")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val qText = etQuestion.text?.toString()?.trim().orEmpty()
                val a = etOptA.text?.toString()?.trim().orEmpty()
                val b = etOptB.text?.toString()?.trim().orEmpty()
                val c = etOptC.text?.toString()?.trim().orEmpty()
                val d = etOptD.text?.toString()?.trim().orEmpty()
                val exp = etExplanation.text?.toString()?.trim().orEmpty()
                val topic = etTopic.text?.toString()?.trim().orEmpty()

                if (qText.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                    AppBulletin.showError(this, "Question text and all 4 options are required")
                    return@setPositiveButton
                }

                val correct = when (rgCorrect.checkedRadioButtonId) {
                    R.id.rbA -> "A"
                    R.id.rbB -> "B"
                    R.id.rbC -> "C"
                    R.id.rbD -> "D"
                    else -> ""
                }
                if (correct.isBlank()) {
                    AppBulletin.showError(this, "Please select the correct option")
                    return@setPositiveButton
                }

                val newQuestion = Question(
                    id = "",
                    examId = selectedExam.id,
                    questionText = qText,
                    optionA = a,
                    optionB = b,
                    optionC = c,
                    optionD = d,
                    correctAnswer = correct,
                    explanation = exp,
                    topic = topic
                )

                viewModel.addQuestion(newQuestion) {
                    AppBulletin.showSuccess(this@AdminActivity, "Question added successfully!")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditExamImageDialog() {
        val exam = exams.firstOrNull() ?: return
        selectedExamForImageUpdate = exam
        val hasImage = exam.imageUrl.isNotBlank()
        val options = if (hasImage) arrayOf("Change Image", "Remove Image") else arrayOf("Choose Image")
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Exam Image: ${exam.examName}")
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Choose Image", "Change Image" -> pickEditExamImageLauncher.launch("image/*")
                    "Remove Image" -> viewModel.updateExamImage(exam.id, "") { selectedExamForImageUpdate = null }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptRenameExam() {
        val exam = exams.firstOrNull() ?: return
        val input = EditText(this).apply {
            setText(exam.examName)
            setSelection(text.length)
            hint = "Enter new exam name"
        }
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Rename Exam")
            .setMessage("Rename '${exam.examName}' (${exam.categoryOrOther})")
            .setView(input)
            .setPositiveButton("Rename") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.length >= 2) {
                    viewModel.renameExam(exam.id, newName) {
                        AppBulletin.showSuccess(this@AdminActivity, "Renamed exam to '$newName'")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteExam() {
        val exam = exams.firstOrNull() ?: return
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Delete ${exam.examName}?")
            .setMessage("Delete ${exam.examName}? This will permanently remove the exam and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExam(exam.id) {
                    AppBulletin.showSuccess(this@AdminActivity, "Exam '${exam.examName}' deleted")
                }
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
    }

    private fun confirmDeleteQuestion(q: Question) {
        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Delete Question?")
            .setMessage("Delete this question? This will permanently remove the question and cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteQuestion(q)
                AppBulletin.showSuccess(this@AdminActivity, "Question deleted")
            }
            .setNegativeButton("Cancel", null)
            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
    }

    private fun showCreateFeedbackPostDialog() {
        val dialogBinding = DialogCreateFeedbackPostBinding.inflate(layoutInflater)
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelPost.setOnClickListener { dialog.dismiss() }

        dialogBinding.btnPublishPost.setOnClickListener {
            val title = dialogBinding.etPostTitle.text?.toString()?.trim().orEmpty()
            val message = dialogBinding.etPostMessage.text?.toString()?.trim().orEmpty()

            if (title.isEmpty()) {
                dialogBinding.tilPostTitle.error = "Title cannot be empty"
                return@setOnClickListener
            }
            dialogBinding.tilPostTitle.error = null

            if (message.isEmpty()) {
                dialogBinding.tilPostMessage.error = "Message cannot be empty"
                return@setOnClickListener
            }
            dialogBinding.tilPostMessage.error = null

            val user = FirebaseAuth.getInstance().currentUser
            val authorId = user?.uid ?: ""
            val authorEmail = user?.email ?: ""

            dialogBinding.btnPublishPost.isEnabled = false
            lifecycleScope.launch {
                val result = feedbackRepo.createFeedbackPost(title, message, authorId, authorEmail)
                dialogBinding.btnPublishPost.isEnabled = true
                result.onSuccess {
                    AppBulletin.showSuccess(this@AdminActivity, "Feedback post published successfully!")
                    dialog.dismiss()
                }.onFailure { err ->
                    AppBulletin.showError(this@AdminActivity, "Failed to publish: ${err.localizedMessage ?: "Unknown error"}")
                }
            }
        }

        dialog.show()
    }

    private fun showHomeBannerOptionsDialog() {
        val dialogBinding = DialogManageBannersBinding.inflate(layoutInflater)
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        var pendingBannerUri: Uri? = null
        var editingBannerId: String? = null

        fun updateLinkLabelVisibility() {
            val hasUrl = !dialogBinding.etBannerLinkUrl.text.isNullOrBlank()
            dialogBinding.tilBannerLinkLabel.visibility = if (hasUrl) View.VISIBLE else View.GONE
            if (!hasUrl) {
                dialogBinding.etBannerLinkLabel.setText("")
                dialogBinding.tilBannerLinkLabel.error = null
            }
        }

        dialogBinding.etBannerLinkUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateLinkLabelVisibility()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        val bannerAdapter = AdminBannerAdapter(
            onMoveUp = { banner ->
                lifecycleScope.launch {
                    try {
                        bannerRepo.reorderBanner(banner.id, moveUp = true)
                        refreshBannerList(dialogBinding)
                    } catch (e: Exception) {
                        AppBulletin.showError(this@AdminActivity, "Error reordering: ${e.localizedMessage}")
                    }
                }
            },
            onMoveDown = { banner ->
                lifecycleScope.launch {
                    try {
                        bannerRepo.reorderBanner(banner.id, moveUp = false)
                        refreshBannerList(dialogBinding)
                    } catch (e: Exception) {
                        AppBulletin.showError(this@AdminActivity, "Error reordering: ${e.localizedMessage}")
                    }
                }
            },
            onEditLink = { banner ->
                pendingBannerUri = null
                editingBannerId = banner.id
                dialogBinding.cardPreviewBanner.visibility = View.GONE
                dialogBinding.tvBannerUploadError.visibility = View.GONE
                dialogBinding.etBannerLinkUrl.setText(banner.linkUrl)
                dialogBinding.etBannerLinkLabel.setText(banner.linkLabel)
                updateLinkLabelVisibility()
                dialogBinding.btnSaveBanner.text = "Save Banner Link"
                dialogBinding.btnSaveBanner.isEnabled = true
                dialogBinding.btnSaveBanner.visibility = View.VISIBLE
            },
            onDelete = { banner ->
                TelegramNativeAlertDialog.Builder(this@AdminActivity)
                    .setTitle("Delete Banner?")
                    .setMessage("Are you sure you want to delete this banner?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            try {
                                bannerRepo.deleteBanner(banner.id)
                                AppBulletin.showSuccess(this@AdminActivity, "Banner deleted")
                                refreshBannerList(dialogBinding)
                            } catch (e: Exception) {
                                AppBulletin.showError(this@AdminActivity, "Failed to delete: ${e.localizedMessage}")
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .makeRed(DialogInterface.BUTTON_POSITIVE).show()
            }
        )

        dialogBinding.rvAdminBanners.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvAdminBanners.adapter = bannerAdapter
        refreshBannerList(dialogBinding)

        dialogBinding.btnPickBannerImage.setOnClickListener {
            onBannerImageSelected = { uri ->
                pendingBannerUri = uri
                editingBannerId = null
                dialogBinding.ivPreviewBanner.setImageURI(uri)
                dialogBinding.cardPreviewBanner.visibility = View.VISIBLE
                dialogBinding.tvBannerUploadError.visibility = View.GONE
                dialogBinding.btnSaveBanner.visibility = View.VISIBLE
                dialogBinding.btnSaveBanner.isEnabled = true
                dialogBinding.btnSaveBanner.text = "Save & Publish Banner"
            }
            pickBannerImageLauncher.launch("image/*")
        }

        dialogBinding.btnSaveBanner.setOnClickListener {
            val uri = pendingBannerUri
            val bannerId = editingBannerId
            val rawUrl = dialogBinding.etBannerLinkUrl.text?.toString()?.trim().orEmpty()
            val rawLabel = dialogBinding.etBannerLinkLabel.text?.toString()?.trim().orEmpty()

            val validationError = HomeBannerLinkValidator.validate(rawUrl, rawLabel)
            if (validationError != null) {
                dialogBinding.tilBannerLinkUrl.error = validationError
                return@setOnClickListener
            }
            dialogBinding.tilBannerLinkUrl.error = null

            if (bannerId == null && uri == null) return@setOnClickListener

            dialogBinding.btnSaveBanner.isEnabled = false
            dialogBinding.btnSaveBanner.text = if (bannerId == null) "Uploading..." else "Saving..."

            lifecycleScope.launch {
                try {
                    if (bannerId != null) {
                        bannerRepo.updateBannerLink(bannerId, rawUrl, rawLabel)
                        AppBulletin.showSuccess(this@AdminActivity, "Banner link updated")
                    } else if (uri != null) {
                        bannerRepo.uploadBanner(this@AdminActivity, uri, rawUrl, rawLabel)
                        AppBulletin.showSuccess(this@AdminActivity, "Banner uploaded successfully")
                    }
                    dialog.dismiss()
                } catch (e: Exception) {
                    dialogBinding.btnSaveBanner.isEnabled = true
                    dialogBinding.btnSaveBanner.text = "Save & Publish Banner"
                    dialogBinding.tvBannerUploadError.text = e.localizedMessage ?: "Failed to save banner"
                    dialogBinding.tvBannerUploadError.visibility = View.VISIBLE
                }
            }
        }

        dialogBinding.btnCloseBannerDialog.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun refreshBannerList(dialogBinding: DialogManageBannersBinding) {
        lifecycleScope.launch {
            try {
                val banners = bannerRepo.getBanners()
                (dialogBinding.rvAdminBanners.adapter as? AdminBannerAdapter)?.submitList(banners)
            } catch (_: Exception) {}
        }
    }

    private fun showFloatingLinkDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = resources.getDimensionPixelSize(R.dimen.design_screen_margin)
            setPadding(pad, pad, pad, pad)
        }

        val etUrl = EditText(this).apply {
            hint = "https://example.com/target"
            inputType = InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val etLabel = EditText(this).apply {
            hint = "Join Discussion / View Channel"
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.design_card_gap)
            }
        }

        layout.addView(etUrl)
        layout.addView(etLabel)

        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Floating Link")
            .setMessage("Configure a persistent floating link on student home screens.")
            .setView(layout)
            .setPositiveButton("Save Link", null)
            .setNeutralButton("Clear Link", null)
            .setNegativeButton("Cancel", null)
            .show()

        requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_POSITIVE)).setOnClickListener {
            val rawUrl = etUrl.text?.toString()?.trim().orEmpty()
            if (rawUrl.isEmpty()) {
                lifecycleScope.launch {
                    try {
                        floatingLinkRepo.clearFloatingLink()
                        AppBulletin.showSuccess(this@AdminActivity, "Floating link cleared")
                        dialog.dismiss()
                    } catch (e: Exception) {
                        AppBulletin.showError(this@AdminActivity, "Clear failed: ${e.message}")
                    }
                }
                return@setOnClickListener
            }

            val finalUrl = if (!rawUrl.startsWith("http://", ignoreCase = true) &&
                !rawUrl.startsWith("https://", ignoreCase = true)
            ) {
                "https://$rawUrl"
            } else {
                rawUrl
            }

            if (!FloatingLinkRepository.isValidHttpsUrl(finalUrl)) {
                AppBulletin.showError(this, "URL must be valid HTTPS")
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    floatingLinkRepo.updateFloatingLink(finalUrl)
                    AppBulletin.showSuccess(this@AdminActivity, "Floating link saved")
                    dialog.dismiss()
                } catch (e: Exception) {
                    AppBulletin.showError(this@AdminActivity, "Save failed: ${e.message}")
                }
            }
        }

        requireNotNull(dialog.getActionButton(DialogInterface.BUTTON_NEUTRAL)).setOnClickListener {
            TelegramNativeAlertDialog.Builder(this)
                .setTitle("Clear Floating Link?")
                .setMessage("Remove the floating link for all students?")
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            floatingLinkRepo.clearFloatingLink()
                            AppBulletin.showSuccess(this@AdminActivity, "Floating link cleared")
                            dialog.dismiss()
                        } catch (e: Exception) {
                            AppBulletin.showError(this@AdminActivity, "Clear failed: ${e.message}")
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .makeRed(DialogInterface.BUTTON_POSITIVE).show()
        }
    }

    private fun showManageFeedbackPostsDialog() {
        lifecycleScope.launch {
            val posts = feedbackRepo.getFeedbackPosts()
            if (posts.isEmpty()) {
                TelegramNativeAlertDialog.Builder(this@AdminActivity)
                    .setTitle("Feedback Posts")
                    .setMessage("No feedback posts published yet.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val titles = posts.map { "${it.title} (${it.message.take(30)}...)" }.toTypedArray()
            TelegramNativeAlertDialog.Builder(this@AdminActivity)
                .setTitle("Select Feedback Post to View Replies")
                .setItems(titles) { _, which ->
                    showPostOptionsDialog(posts[which])
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showPostOptionsDialog(post: FeedbackPost) {
        val options = arrayOf("✏️ Edit Post", "👁️ View Replies", "🗑️ Delete Post")
        TelegramNativeAlertDialog.Builder(this)
            .setTitle(post.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditFeedbackPostDialog(post)
                    1 -> showPostRepliesDialog(post)
                    2 -> {
                        TelegramNativeAlertDialog.Builder(this)
                            .setTitle("Delete Post?")
                            .setMessage("Delete '${post.title}' and all its replies? This cannot be undone.")
                            .setPositiveButton("Delete") { _, _ ->
                                lifecycleScope.launch {
                                    feedbackRepo.deleteFeedbackPost(post.id).onSuccess {
                                        AppBulletin.showSuccess(this@AdminActivity, "Post deleted successfully")
                                    }.onFailure { err ->
                                        AppBulletin.showError(this@AdminActivity, "Failed to delete: ${err.message}")
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .makeRed(DialogInterface.BUTTON_POSITIVE).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditFeedbackPostDialog(post: FeedbackPost) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = resources.getDimensionPixelSize(R.dimen.design_screen_margin)
            setPadding(pad, pad, pad, pad)
        }
        val etTitle = EditText(this).apply {
            setText(post.title)
            hint = "Post Title"
        }
        val etMessage = EditText(this).apply {
            setText(post.message)
            hint = "Post Message"
        }
        layout.addView(etTitle)
        layout.addView(etMessage)

        TelegramNativeAlertDialog.Builder(this)
            .setTitle("Edit Feedback Post")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = etTitle.text?.toString()?.trim().orEmpty()
                val newMsg = etMessage.text?.toString()?.trim().orEmpty()
                if (newTitle.isNotEmpty() && newMsg.isNotEmpty()) {
                    lifecycleScope.launch {
                        feedbackRepo.updateFeedbackPost(post.id, newTitle, newMsg).onSuccess {
                            AppBulletin.showSuccess(this@AdminActivity, "Post updated")
                        }.onFailure { err ->
                            AppBulletin.showError(this@AdminActivity, "Failed: ${err.message}")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPostRepliesDialog(post: FeedbackPost) {
        val dialogBinding = DialogPostRepliesBinding.inflate(layoutInflater)
        val dialog = TelegramNativeAlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.tvPostRepliesTitle.text = "Replies: ${post.title}"
        dialogBinding.tvPostRepliesSnippet.text = post.message

        var currentReplies: List<FeedbackPostReply> = emptyList()

        lateinit var repliesAdapter: PostRepliesAdapter
        repliesAdapter = PostRepliesAdapter(
            onMarkRead = { reply ->
                lifecycleScope.launch {
                    feedbackRepo.markReplyAsRead(post.id, reply.id).onSuccess {
                        currentReplies = currentReplies.map { if (it.id == reply.id) it.copy(read = true) else it }
                        repliesAdapter.submitList(currentReplies)
                        AppBulletin.showSuccess(this@AdminActivity, "Marked as read")
                    }
                }
            },
            onDelete = { reply ->
                TelegramNativeAlertDialog.Builder(this)
                    .setTitle("Delete Reply?")
                    .setMessage("Delete reply from ${reply.name}?")
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            feedbackRepo.deleteSingleReply(post.id, reply.id).onSuccess {
                                currentReplies = currentReplies.filter { it.id != reply.id }
                                repliesAdapter.submitList(currentReplies)
                                dialogBinding.tvNoReplies.visibility = if (currentReplies.isEmpty()) View.VISIBLE else View.GONE
                                AppBulletin.showSuccess(this@AdminActivity, "Reply deleted")
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .makeRed(DialogInterface.BUTTON_POSITIVE).show()
            }
        )

        dialogBinding.rvReplies.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvReplies.adapter = repliesAdapter

        dialogBinding.btnDeletePostFromReplies.setOnClickListener {
            TelegramNativeAlertDialog.Builder(this)
                .setTitle("Delete Post?")
                .setMessage("Delete '${post.title}' from Home screen? All its student replies will also be permanently deleted.")
                .setPositiveButton("Delete") { _, _ ->
                    dialog.dismiss()
                    lifecycleScope.launch {
                        feedbackRepo.deleteFeedbackPost(post.id).onSuccess {
                            AppBulletin.showSuccess(this@AdminActivity, "Post deleted")
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .makeRed(DialogInterface.BUTTON_POSITIVE).show()
        }

        dialogBinding.btnCloseReplies.setOnClickListener { dialog.dismiss() }

        dialogBinding.progressBarReplies.visibility = View.VISIBLE
        lifecycleScope.launch {
            currentReplies = feedbackRepo.getRepliesForPost(post.id)
            dialogBinding.progressBarReplies.visibility = View.GONE
            repliesAdapter.submitList(currentReplies)
            dialogBinding.tvNoReplies.visibility = if (currentReplies.isEmpty()) View.VISIBLE else View.GONE
        }

        dialog.show()
    }

    private fun exportExamData() {
        val progressDialog = TelegramNativeAlertDialog.Builder(this)
            .setTitle("Exporting Exam Data")
            .setMessage("Fetching all exams and questions from server... Please wait.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        lifecycleScope.launch {
            try {
                val exportList = mutableListOf<Map<String, Any?>>()
                var offset = 0
                val limit = 50
                var hasMore = true
                var totalServerExams = 0
                var totalServerQuestions = 0

                while (hasMore) {
                    val response = com.eve.app.data.remote.ApiClient.apiService.exportExams(limit, offset)
                    if (!response.success || response.data == null) {
                        throw Exception(response.error ?: "Failed to fetch exam export data from server")
                    }
                    val data = response.data
                    exportList.addAll(data.exams)
                    totalServerExams = data.pagination.totalExams
                    totalServerQuestions = data.pagination.totalQuestions
                    hasMore = data.pagination.hasMore && data.exams.isNotEmpty()
                    offset += limit
                }

                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val fileName = "eve_backup_$dateStr.json"
                val tempFileName = "$fileName.tmp"
                val jsonString = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(exportList)

                val cacheFile = File(cacheDir, fileName)
                val tempCacheFile = File(cacheDir, tempFileName)
                tempCacheFile.writeText(jsonString)
                if (cacheFile.exists()) cacheFile.delete()
                if (!tempCacheFile.renameTo(cacheFile)) {
                    cacheFile.writeText(jsonString)
                    tempCacheFile.delete()
                }

                progressDialog.dismiss()

                val contentUri = FileProvider.getUriForFile(this@AdminActivity, "${packageName}.fileprovider", cacheFile)
                TelegramNativeAlertDialog.Builder(this@AdminActivity)
                    .setTitle("Export Complete")
                    .setMessage("Successfully exported $totalServerExams exams ($totalServerQuestions questions). Share or save the file:")
                    .setPositiveButton("Share JSON") { _, _ ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Exam Backup"))
                    }
                    .setNegativeButton("Done", null)
                    .show()

            } catch (e: Exception) {
                progressDialog.dismiss()
                AppBulletin.showError(this@AdminActivity, "Export failed: ${e.message}")
            }
        }
    }
}

package com.eve.app.ui.syllabus

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.ExamRepository
import com.eve.app.ui.common.EveBaseActivity
import com.eve.app.util.AppBulletin
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Components.EmptyTextProgressView
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.Stories.recorder.ButtonWithCounterView

/**
 * Exam Syllabus screen powered by real ported Telegram UI components:
 * ActionBar (with ActionBarMenuItem search), RecyclerListView, EmptyTextProgressView,
 * HeaderCell, TextDetailSettingsCell, ShadowSectionCell, TextInfoPrivacyCell, and ButtonWithCounterView.
 */
class SyllabusActivity : EveBaseActivity() {

    companion object {
        private const val MENU_SEARCH = 1
        private const val MENU_REFRESH = 2
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EXAM = 1
        private const val VIEW_TYPE_SHADOW = 2
        private const val VIEW_TYPE_INFO = 3
    }

    private val examRepository = ExamRepository()
    private val adapter = TelegramSyllabusAdapter()

    private val syllabusAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
    private var syllabusOwnerUid: String? = null

    private fun hasSyllabusOwner(): Boolean =
        com.eve.app.util.SubmissionRetryPolicy.ownsSubmission(syllabusOwnerUid, syllabusAuth.currentUser?.uid)

    private val syllabusAuthListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener {
        if (syllabusOwnerUid != null && !hasSyllabusOwner()) finish()
    }

    private var allExams = listOf<Exam>()
    private var currentQuery = ""
    private var pendingDownloadExam: Exam? = null
    private val downloadIdToExam = mutableMapOf<Long, Exam>()
    private val examIdToUri = mutableMapOf<String, Uri>()
    private val downloadingIds = mutableSetOf<String>()
    private val downloadedIds = mutableSetOf<String>()

    private lateinit var actionBar: ActionBar
    private lateinit var listView: RecyclerListView
    private lateinit var emptyView: EmptyTextProgressView
    private lateinit var retryButton: ButtonWithCounterView

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingDownloadExam?.let { startDownload(it) }
        } else {
            AppBulletin.showError(this, "Storage permission is required to save downloads")
        }
        pendingDownloadExam = null
    }

    private val onDownloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!hasSyllabusOwner()) return
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                val exam = downloadIdToExam[downloadId] ?: return

                downloadingIds.remove(exam.id)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val fileUri = dm.getUriForDownloadedFile(downloadId)

                if (fileUri != null) {
                    examIdToUri[exam.id] = fileUri
                    downloadedIds.add(exam.id)
                    adapter.notifyDataSetChanged()
                    AppBulletin.show(
                        context = this@SyllabusActivity,
                        message = "Syllabus downloaded: ${exam.examName}",
                        durationMs = AppBulletin.DURATION_PROLONG,
                        actionText = "OPEN",
                        onAction = { openPdf(fileUri) }
                    )
                } else {
                    adapter.notifyDataSetChanged()
                    AppBulletin.showError(this@SyllabusActivity, "Failed to download syllabus")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val liveUid = syllabusAuth.currentUser?.uid
        syllabusOwnerUid = if (savedInstanceState == null) liveUid
            else savedInstanceState.getString("syllabus_owner_uid")
        if (!hasSyllabusOwner()) { finish(); return }

        Theme.syncFromEvePalette(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray))
        }
        setContentView(root)

        // 1. Real Telegram ActionBar with live Search + Refresh
        actionBar = ActionBar(this).apply {
            setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault))
            setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSelector), false)
            setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon), false)
            setTitleColor(Theme.getColor(Theme.key_actionBarDefaultTitle))
            setSubtitleColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle))
            setBackButtonImage(R.drawable.ic_ab_back)
            setTitle("Exam Syllabus")
            val menu = createMenu()
            val searchItem = menu.addItem(MENU_SEARCH, R.drawable.msg_search)
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(object : ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    override fun onSearchCollapse() {
                        currentQuery = ""
                        filterExams("")
                    }

                    override fun onTextChanged(editText: EditText?) {
                        currentQuery = editText?.text?.toString().orEmpty()
                        filterExams(currentQuery)
                    }
                })
            searchItem.setSearchFieldHint("Search exam or category…")
            menu.addItem(MENU_REFRESH, R.drawable.msg_retry)
            setActionBarMenuOnItemClick(object : ActionBar.ActionBarMenuOnItemClick() {
                override fun onItemClick(id: Int) {
                    when (id) {
                        -1 -> com.eve.app.util.EveNavigationHelper.finishWithTransition(this@SyllabusActivity)
                        MENU_REFRESH -> loadExams(userTriggered = true)
                    }
                }
            })
        }
        root.addView(
            actionBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // 2. Content container with RecyclerListView + EmptyTextProgressView + Retry CTA
        val contentFrame = FrameLayout(this)
        root.addView(
            contentFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        listView = RecyclerListView(this).apply {
            layoutManager = LinearLayoutManager(this@SyllabusActivity)
            adapter = this@SyllabusActivity.adapter
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
            setScrollbarFadingEnabled(true)
            setScrollBarDefaultDelayBeforeFade(900)
            clipToPadding = false
            setPadding(0, 0, 0, AndroidUtilities.dp(24f))
        }
        contentFrame.addView(
            listView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        emptyView = EmptyTextProgressView(this).apply {
            setShowAtCenter(true)
            setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder))
            setProgressBarColor(Theme.getColor(Theme.key_progressCircle))
            setText("No Syllabi Available\nThere are no exam syllabi published yet.")
            showProgress()
        }
        contentFrame.addView(
            emptyView,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT.toFloat())
        )

        retryButton = ButtonWithCounterView(this, null).apply {
            setRound()
            setText("Retry", false)
            visibility = View.GONE
            setOnClickListener { loadExams(userTriggered = false) }
        }
        contentFrame.addView(
            retryButton,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                48f,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                24f,
                0f,
                24f,
                32f
            )
        )

        loadExams(userTriggered = false)

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            this,
            onDownloadCompleteReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onStart() {
        super.onStart()
        if (!hasSyllabusOwner()) { finish(); return }
        syllabusAuth.addAuthStateListener(syllabusAuthListener)
    }

    override fun onStop() {
        syllabusAuth.removeAuthStateListener(syllabusAuthListener)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (!hasSyllabusOwner()) { finish(); return }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("syllabus_owner_uid", syllabusOwnerUid)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(onDownloadCompleteReceiver)
        } catch (_: Exception) { }
    }

    private fun loadExams(userTriggered: Boolean) {
        val hasCachedItems = allExams.isNotEmpty()
        retryButton.visibility = View.GONE
        if (!hasCachedItems) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            emptyView.showProgress()
        }

        lifecycleScope.launch {
            try {
                allExams = examRepository.getExams()
                filterExams(currentQuery)
                if (userTriggered) {
                    AppBulletin.show(this@SyllabusActivity, "Syllabi refreshed")
                }
            } catch (e: Exception) {
                if (hasCachedItems) {
                    listView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                    AppBulletin.showError(
                        this@SyllabusActivity,
                        "Failed to refresh syllabi: ${e.localizedMessage.orEmpty()}"
                    )
                } else {
                    listView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                    emptyView.setText("Failed to load syllabi\n${e.localizedMessage.orEmpty()}")
                    emptyView.showTextView()
                    retryButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun filterExams(query: String) {
        val trimmed = query.trim().lowercase()
        val filtered = allExams.filter { exam ->
            exam.examName.lowercase().contains(trimmed) ||
                exam.category.lowercase().contains(trimmed)
        }

        adapter.submit(filtered)
        retryButton.visibility = View.GONE

        if (filtered.isEmpty()) {
            listView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            if (trimmed.isNotBlank()) {
                emptyView.setText("No Results\nNo exam syllabus matches \"$query\".")
            } else {
                emptyView.setText("No Syllabi Available\nThere are no exam syllabi published yet.")
            }
            emptyView.showTextView()
            actionBar.setSubtitle(null)
        } else {
            emptyView.visibility = View.GONE
            listView.visibility = View.VISIBLE
            actionBar.setSubtitle("${filtered.size} ${if (filtered.size == 1) "exam" else "exams"}")
        }
    }

    private fun checkPermissionAndDownload(exam: Exam) {
        if (!hasSyllabusOwner()) { finish(); return }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloadExam = exam
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startDownload(exam)
        }
    }

    private fun startDownload(exam: Exam) {
        if (!hasSyllabusOwner()) { finish(); return }
        if (exam.syllabusUrl.isBlank()) {
            AppBulletin.showError(this, "Syllabus not available for this exam")
            return
        }

        try {
            val fileName = exam.syllabusFileName.ifBlank {
                "${exam.examName.replace("\\s+".toRegex(), "_")}_syllabus.pdf"
            }.let { if (!it.endsWith(".pdf", ignoreCase = true)) "$it.pdf" else it }

            val request = DownloadManager.Request(Uri.parse(exam.syllabusUrl))
                .setTitle(fileName)
                .setDescription("Downloading ${exam.examName} syllabus")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            downloadIdToExam[downloadId] = exam
            downloadingIds.add(exam.id)
            adapter.notifyDataSetChanged()

            AppBulletin.show(this, "Downloading $fileName...")
        } catch (e: Exception) {
            AppBulletin.showError(this, "Download failed: ${e.localizedMessage}")
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(Intent.createChooser(intent, "Open Syllabus PDF"))
        } catch (_: Exception) {
            AppBulletin.showError(this, "No PDF viewer app found on device")
        }
    }

    private inner class TelegramSyllabusAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val items = mutableListOf<Exam>()

        fun submit(newItems: List<Exam>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else items.size + 3

        override fun getItemViewType(position: Int): Int = when {
            position == 0 -> VIEW_TYPE_HEADER
            position <= items.size -> VIEW_TYPE_EXAM
            position == items.size + 1 -> VIEW_TYPE_SHADOW
            else -> VIEW_TYPE_INFO
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view: View = when (viewType) {
                VIEW_TYPE_HEADER -> HeaderCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                }
                VIEW_TYPE_EXAM -> TextDetailSettingsCell(parent.context).apply {
                    setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite))
                    foreground = Theme.getSelectorDrawable(false)
                    setMultilineDetail(true)
                }
                VIEW_TYPE_SHADOW -> ShadowSectionCell(parent.context)
                else -> TextInfoPrivacyCell(parent.context)
            }
            view.layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                VIEW_TYPE_HEADER -> {
                    (holder.itemView as HeaderCell).setText("Available Syllabi")
                }
                VIEW_TYPE_EXAM -> {
                    val exam = items[position - 1]
                    val divider = position < items.size
                    val cell = holder.itemView as TextDetailSettingsCell
                    val statusText = when {
                        downloadingIds.contains(exam.id) -> "Downloading PDF…"
                        downloadedIds.contains(exam.id) && examIdToUri.containsKey(exam.id) -> "Downloaded • Tap to open PDF"
                        exam.syllabusUrl.isNotBlank() -> "Tap to download syllabus PDF"
                        else -> "Syllabus PDF not uploaded yet"
                    }
                    val categoryPart = exam.category.ifBlank { "General" }
                    cell.setTextAndValueAndIcon(
                        exam.examName,
                        "$categoryPart • $statusText",
                        R.drawable.msg_download,
                        divider
                    )
                    cell.setOnClickListener {
                        val existingUri = examIdToUri[exam.id]
                        if (existingUri != null && downloadedIds.contains(exam.id)) {
                            openPdf(existingUri)
                        } else if (!downloadingIds.contains(exam.id)) {
                            checkPermissionAndDownload(exam)
                        }
                    }
                }
                VIEW_TYPE_INFO -> {
                    (holder.itemView as TextInfoPrivacyCell).setText(
                        "Downloaded PDFs are saved to your device Downloads folder."
                    )
                }
            }
        }
    }
}

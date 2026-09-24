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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.eve.app.data.model.Exam
import com.eve.app.data.repository.ExamRepository
import com.eve.app.databinding.ActivitySyllabusBinding
import com.eve.app.ui.common.ErrorStateView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class SyllabusActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySyllabusBinding
    private val examRepository = ExamRepository()
    private lateinit var adapter: SyllabusAdapter

    private var allExams = listOf<Exam>()
    private var pendingDownloadExam: Exam? = null
    private val downloadIdToExam = mutableMapOf<Long, Exam>()
    private val examIdToUri = mutableMapOf<String, Uri>()

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingDownloadExam?.let { startDownload(it) }
        } else {
            Toast.makeText(this, "Storage permission is required to save downloads", Toast.LENGTH_SHORT).show()
        }
        pendingDownloadExam = null
    }

    private val onDownloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                val exam = downloadIdToExam[downloadId] ?: return

                adapter.setDownloading(exam.id, false)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val fileUri = dm.getUriForDownloadedFile(downloadId)

                if (fileUri != null) {
                    examIdToUri[exam.id] = fileUri
                    adapter.setDownloaded(exam.id)
                    Snackbar.make(binding.root, "Syllabus downloaded: ${exam.examName}", Snackbar.LENGTH_LONG)
                        .setAction("OPEN") {
                            openPdf(fileUri)
                        }
                        .show()
                } else {
                    Toast.makeText(this@SyllabusActivity, "Failed to download syllabus", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyllabusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        loadExams()

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadCompleteReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadCompleteReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(onDownloadCompleteReceiver)
        } catch (_: Exception) { }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.swipeRefresh.setOnRefreshListener { loadExams() }
    }

    private fun setupRecyclerView() {
        adapter = SyllabusAdapter(
            onDownloadClick = { exam, _ -> checkPermissionAndDownload(exam) },
            onOpenClick = { exam ->
                examIdToUri[exam.id]?.let { openPdf(it) }
            }
        )
        binding.rvSyllabus.layoutManager = LinearLayoutManager(this)
        binding.rvSyllabus.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterExams(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadExams() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorStateView.hide()
        binding.emptyStateView.hide()

        lifecycleScope.launch {
            try {
                allExams = examRepository.getExams()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                filterExams(binding.etSearch.text?.toString().orEmpty())
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                binding.emptyStateView.hide()
                binding.errorStateView.show(
                    type = ErrorStateView.ErrorType.SERVER_ERROR,
                    customMessage = "Failed to load syllabi. ${e.localizedMessage.orEmpty()}",
                    onRetry = { loadExams() }
                )
            }
        }
    }

    private fun filterExams(query: String) {
        val trimmed = query.trim().lowercase()
        val filtered = allExams.filter { exam ->
            exam.examName.lowercase().contains(trimmed) ||
            exam.category.lowercase().contains(trimmed)
        }

        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.errorStateView.hide()
            if (trimmed.isNotBlank()) {
                binding.emptyStateView.show(
                    title = "No Results",
                    message = "No exam syllabus matches \"$query\"."
                )
            } else {
                binding.emptyStateView.show(
                    title = "No Syllabi Available",
                    message = "There are no exam syllabi published yet."
                )
            }
        } else {
            binding.errorStateView.hide()
            binding.emptyStateView.hide()
        }
    }

    private fun checkPermissionAndDownload(exam: Exam) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            pendingDownloadExam = exam
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            startDownload(exam)
        }
    }

    private fun startDownload(exam: Exam) {
        if (exam.syllabusUrl.isBlank()) {
            Toast.makeText(this, "Syllabus not available for this exam", Toast.LENGTH_SHORT).show()
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
            adapter.setDownloading(exam.id, true)

            Toast.makeText(this, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "No PDF viewer app found on device", Toast.LENGTH_SHORT).show()
        }
    }
}

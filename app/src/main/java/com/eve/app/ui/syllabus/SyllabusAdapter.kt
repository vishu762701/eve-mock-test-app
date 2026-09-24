package com.eve.app.ui.syllabus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemSyllabusBinding

class SyllabusAdapter(
    private val onDownloadClick: (Exam, Int) -> Unit,
    private val onOpenClick: (Exam) -> Unit
) : ListAdapter<Exam, SyllabusAdapter.SyllabusViewHolder>(ExamDiffCallback()) {

    private val downloadingIds = mutableSetOf<String>()
    private val downloadedIds = mutableSetOf<String>()

    fun setDownloading(examId: String, isDownloading: Boolean) {
        if (isDownloading) {
            downloadingIds.add(examId)
        } else {
            downloadingIds.remove(examId)
        }
        val pos = currentList.indexOfFirst { it.id == examId }
        if (pos != -1) notifyItemChanged(pos)
    }

    fun setDownloaded(examId: String) {
        downloadingIds.remove(examId)
        downloadedIds.add(examId)
        val pos = currentList.indexOfFirst { it.id == examId }
        if (pos != -1) notifyItemChanged(pos)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyllabusViewHolder {
        val binding = ItemSyllabusBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SyllabusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SyllabusViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SyllabusViewHolder(
        private val binding: ItemSyllabusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exam: Exam) {
            binding.tvCategory.text = exam.categoryOrOther
            binding.tvExamName.text = exam.examName

            val hasSyllabus = exam.syllabusUrl.isNotBlank()
            val isDownloading = downloadingIds.contains(exam.id)
            val isDownloaded = downloadedIds.contains(exam.id)

            if (hasSyllabus) {
                binding.tvFileName.text = exam.syllabusFileName.ifBlank { "syllabus.pdf" }
                binding.btnDownload.isEnabled = !isDownloading
                binding.btnDownload.alpha = 1.0f
                binding.btnDownload.text = if (isDownloaded) "Re-download" else "Download PDF"

                binding.pbDownloading.visibility = if (isDownloading) View.VISIBLE else View.GONE
                binding.btnOpen.visibility = if (isDownloaded) View.VISIBLE else View.GONE
                binding.btnOpen.setOnClickListener { onOpenClick(exam) }

                binding.btnDownload.setOnClickListener {
                    onDownloadClick(exam, bindingAdapterPosition)
                }
            } else {
                binding.tvFileName.text = "Syllabus coming soon"
                binding.btnDownload.isEnabled = false
                binding.btnDownload.alpha = 0.5f
                binding.btnDownload.text = "Coming Soon"
                binding.pbDownloading.visibility = View.GONE
                binding.btnOpen.visibility = View.GONE
                binding.btnDownload.setOnClickListener(null)
            }
        }
    }

    private class ExamDiffCallback : DiffUtil.ItemCallback<Exam>() {
        override fun areItemsTheSame(oldItem: Exam, newItem: Exam): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Exam, newItem: Exam): Boolean =
            oldItem == newItem
    }
}

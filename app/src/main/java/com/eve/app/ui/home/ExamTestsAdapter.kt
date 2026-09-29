package com.eve.app.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.R
import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.databinding.ItemExamTestBinding

sealed class ExamTestsListItem {
    data class SubExamItem(val exam: Exam) : ExamTestsListItem()
    data class TestItem(
        val test: GeneratedTest,
        val title: String,
        val subtitle: String,
        val isCompleted: Boolean,
        val isResume: Boolean = false,
        val isLocked: Boolean = false,
        val opensText: String = ""
    ) : ExamTestsListItem()
}

class ExamTestsAdapter(
    private val onSubExamClick: (Exam) -> Unit,
    private val onTestClick: (GeneratedTest, Boolean) -> Unit,
    private val onLockedClick: (GeneratedTest, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ExamTestsAdapter.ExamTestViewHolder>() {

    private val items = mutableListOf<ExamTestsListItem>()

    fun submitList(newItems: List<ExamTestsListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamTestViewHolder {
        val binding = ItemExamTestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExamTestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExamTestViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ExamTestViewHolder(private val binding: ItemExamTestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ExamTestsListItem) {
            when (item) {
                is ExamTestsListItem.SubExamItem -> {
                    binding.tvTitle.text = item.exam.examName
                    binding.tvSubtitle.visibility = View.GONE
                    binding.tvStatus.visibility = View.GONE
                    binding.cardContainer.setOnClickListener {
                        onSubExamClick(item.exam)
                    }
                }
                is ExamTestsListItem.TestItem -> {
                    binding.tvTitle.text = item.title
                    binding.tvSubtitle.visibility = View.VISIBLE
                    binding.tvStatus.visibility = View.VISIBLE

                    val context = binding.root.context
                    when {
                        item.isLocked -> {
                            binding.tvSubtitle.text = item.opensText
                            binding.tvStatus.text = "Locked"
                            binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_text_secondary))
                            binding.cardContainer.setOnClickListener {
                                onLockedClick(item.test, item.opensText)
                            }
                        }
                        item.isCompleted -> {
                            binding.tvSubtitle.text = item.subtitle
                            binding.tvStatus.text = "Completed"
                            binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_status_success))
                            binding.cardContainer.setOnClickListener {
                                onTestClick(item.test, item.isCompleted)
                            }
                        }
                        item.isResume -> {
                            binding.tvSubtitle.text = item.subtitle
                            binding.tvStatus.text = "Resume"
                            binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_primary))
                            binding.cardContainer.setOnClickListener {
                                onTestClick(item.test, item.isCompleted)
                            }
                        }
                        else -> {
                            binding.tvSubtitle.text = item.subtitle
                            binding.tvStatus.text = "Start"
                            binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_primary))
                            binding.cardContainer.setOnClickListener {
                                onTestClick(item.test, item.isCompleted)
                            }
                        }
                    }
                }
            }
        }
    }
}

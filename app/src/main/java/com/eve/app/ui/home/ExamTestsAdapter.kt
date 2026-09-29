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
        val isCompleted: Boolean
    ) : ExamTestsListItem()
}

class ExamTestsAdapter(
    private val onSubExamClick: (Exam) -> Unit,
    private val onTestClick: (GeneratedTest, Boolean) -> Unit
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
                    binding.tvSubtitle.text = item.subtitle
                    binding.tvStatus.visibility = View.VISIBLE

                    val context = binding.root.context
                    if (item.isCompleted) {
                        binding.tvStatus.text = "Completed"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_status_success))
                    } else {
                        binding.tvStatus.text = "Start"
                        binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.eve_primary))
                    }

                    binding.cardContainer.setOnClickListener {
                        onTestClick(item.test, item.isCompleted)
                    }
                }
            }
        }
    }
}

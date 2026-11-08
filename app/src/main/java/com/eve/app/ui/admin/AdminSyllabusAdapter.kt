package com.eve.app.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eve.app.data.model.Exam
import com.eve.app.databinding.ItemAdminSyllabusBinding
import com.eve.app.designsystem.TelegramThemeSelectors
import com.eve.app.theme.TelegramThemeEngine
import java.util.Date

class AdminSyllabusAdapter(
    private val onReplace: (Exam) -> Unit,
    private val onReassign: (Exam) -> Unit,
    private val onDelete: (Exam) -> Unit
) : RecyclerView.Adapter<AdminSyllabusAdapter.VH>() {

    private val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    private var items: List<Exam> = emptyList()

    fun submit(newItems: List<Exam>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAdminSyllabusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        b.root.rippleColor = android.content.res.ColorStateList.valueOf(
            TelegramThemeEngine.resolveCurrentPalette(parent.context).actionSelector
        )
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemAdminSyllabusBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(exam: Exam) {
            val palette = TelegramThemeEngine.resolveCurrentPalette(b.root.context)
            val density = b.root.resources.displayMetrics.density

            b.tvExamName.text = exam.examName
            b.tvExamName.setTextColor(palette.textPrimary)
            b.tvExamCategory.text = "Category: ${exam.categoryOrOther}"
            b.tvExamCategory.setTextColor(palette.textSecondary)
            b.tvFileName.text = exam.syllabusFileName.ifBlank { "syllabus.pdf" }
            b.tvFileName.setTextColor(palette.textPrimary)

            b.tvUploadDate.text = if (exam.syllabusUploadedAt > 0) {
                "Uploaded: ${dateFormat.format(Date(exam.syllabusUploadedAt))}"
            } else {
                "Active Syllabus"
            }
            b.tvUploadDate.setTextColor(palette.textSecondary)

            b.btnChangeExam.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )
            b.btnReplacePdf.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )

            b.btnDeletePdf.background = TelegramThemeSelectors.getRoundRectSelectorDrawable(
                16f * density,
                palette.actionSelector
            )
            b.btnReplacePdf.setOnClickListener { onReplace(exam) }
            b.btnChangeExam.setOnClickListener { onReassign(exam) }
            b.btnDeletePdf.setOnClickListener { onDelete(exam) }
        }
    }
}

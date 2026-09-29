package com.eve.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.eve.app.data.model.Exam
import com.eve.app.databinding.BottomSheetTargetExamsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip

object TargetExamsBottomSheet {

    const val PREFS_NAME = "target_exams_prefs"
    const val KEY_TARGET_EXAMS = "target_exam_ids"
    const val KEY_ONBOARDING_DONE = "onboarding_done"

    fun getTargetExamIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_TARGET_EXAMS, emptySet()) ?: emptySet()
    }

    fun isOnboardingDone(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun show(
        activity: AppCompatActivity,
        exams: List<Exam>,
        onSaved: (Set<String>) -> Unit = {}
    ) {
        val dialog = BottomSheetDialog(activity)
        val binding = BottomSheetTargetExamsBinding.inflate(LayoutInflater.from(activity))
        dialog.setContentView(binding.root)

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTargets = prefs.getStringSet(KEY_TARGET_EXAMS, emptySet()) ?: emptySet()

        val mainExams = exams.filter { it.isMainExam }.sortedBy { it.examName }
        val chipMap = mutableMapOf<Int, String>()

        mainExams.forEach { exam ->
            val chip = Chip(activity).apply {
                text = exam.examName
                isCheckable = true
                isChecked = currentTargets.contains(exam.id)
            }
            binding.chipGroupExams.addView(chip)
            chipMap[chip.id] = exam.id
        }

        binding.btnSkip.setOnClickListener {
            prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
            dialog.dismiss()
        }

        binding.btnContinue.setOnClickListener {
            val selected = mutableSetOf<String>()
            for (i in 0 until binding.chipGroupExams.childCount) {
                val chip = binding.chipGroupExams.getChildAt(i) as? Chip
                if (chip != null && chip.isChecked) {
                    chipMap[chip.id]?.let { selected.add(it) }
                }
            }
            prefs.edit()
                .putStringSet(KEY_TARGET_EXAMS, selected)
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .apply()
            onSaved(selected)
            dialog.dismiss()
        }

        dialog.show()
    }
}

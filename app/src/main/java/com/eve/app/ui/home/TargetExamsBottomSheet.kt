package com.eve.app.ui.home

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.eve.app.data.model.Exam
import com.eve.app.databinding.BottomSheetTargetExamsBinding
import com.eve.app.designsystem.UiArcMotion
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

    /** Shared production chip creation: readable at large text, bounded label, full accessible name. */
    fun examChip(context: Context, name: String, checked: Boolean): Chip =
        Chip(context, null, com.google.android.material.R.attr.chipStyle).apply {
            id = android.view.View.generateViewId()
            text = name
            contentDescription = name
            isCheckable = true
            isChecked = checked
            setCheckedIconResource(com.eve.app.R.drawable.ic_target_exam_checked)
            checkedIconTint = androidx.core.content.ContextCompat.getColorStateList(context, com.eve.app.R.color.on_accent)
            isCheckedIconVisible = true
            com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.CHIP)
            val density = resources.displayMetrics.density
            chipMinHeight = resources.getDimension(com.eve.app.R.dimen.design_pill_height)
            shapeAppearanceModel = shapeAppearanceModel.withCornerSize(com.google.android.material.shape.RelativeCornerSize(.5f))
            chipStrokeWidth = 0f
            chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(context, com.eve.app.R.color.accent)
            setTextColor(androidx.core.content.ContextCompat.getColor(context, com.eve.app.R.color.on_accent))
            chipStartPadding = resources.getDimension(com.eve.app.R.dimen.design_pill_padding)
            chipEndPadding = chipStartPadding
            setEnsureMinTouchTargetSize(true)
            maxWidth = (Math.round(minOf(resources.configuration.screenWidthDp, 640) * density) -
                2 * resources.getDimensionPixelSize(com.eve.app.R.dimen.design_screen_margin)).coerceAtLeast(resources.getDimensionPixelSize(com.eve.app.R.dimen.design_touch_min))
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

    fun show(
        activity: AppCompatActivity,
        exams: List<Exam>,
        onSaved: (Set<String>) -> Unit = {}
    ): BottomSheetDialog {
        val dialog = BottomSheetDialog(activity, com.eve.app.R.style.Theme_Eve_BottomSheetDialog)
        val binding = BottomSheetTargetExamsBinding.inflate(LayoutInflater.from(activity))
        dialog.setContentView(binding.root)

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTargets = prefs.getStringSet(KEY_TARGET_EXAMS, emptySet()) ?: emptySet()

        val mainExams = exams.filter { it.isMainExam }.sortedBy { it.examName }
        val chipMap = mutableMapOf<Int, String>()

        mainExams.forEach { exam ->
            val chip = examChip(activity, exam.examName, currentTargets.contains(exam.id))
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

        val displayMetrics = activity.resources.displayMetrics
        val vh = displayMetrics.heightPixels
        val density = displayMetrics.density

        dialog.behavior.maxWidth = Math.round(640f * density)
        // UIArc detent specification: [0.45, 0.92] with initialDetent: 0 (0.45 peek)
        dialog.behavior.isFitToContents = false
        dialog.behavior.halfExpandedRatio = 0.45f
        dialog.behavior.peekHeight = Math.round(vh * 0.45f)
        dialog.behavior.expandedOffset = Math.round(vh * 0.08f)
        dialog.behavior.skipCollapsed = false

        dialog.show()
        com.eve.app.designsystem.DesignSurfaces.dialog(dialog)
        com.eve.app.designsystem.DesignSurfaces.sheet(binding.root)

        val motionEnabled = UiArcMotion.isMotionEnabled(activity)
        dialog.behavior.state = if (motionEnabled) {
            BottomSheetBehavior.STATE_HALF_EXPANDED
        } else {
            BottomSheetBehavior.STATE_EXPANDED
        }
        return dialog
    }
}

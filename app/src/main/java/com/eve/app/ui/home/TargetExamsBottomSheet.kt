package com.eve.app.ui.home

import android.content.Context
import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import org.telegram.ui.Components.CubicBezierInterpolator
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
            com.eve.app.designsystem.DesignType.apply(this, com.eve.app.designsystem.DesignType.Role.CHIP)
            val density = resources.displayMetrics.density
            chipMinHeight = resources.getDimension(com.eve.app.R.dimen.design_pill_height)
            shapeAppearanceModel = shapeAppearanceModel.withCornerSize(com.google.android.material.shape.RelativeCornerSize(.5f))
            chipStartPadding = resources.getDimension(com.eve.app.R.dimen.design_pill_padding)
            chipEndPadding = chipStartPadding
            setEnsureMinTouchTargetSize(true)
            maxWidth = (Math.round(minOf(resources.configuration.screenWidthDp, 640) * density) -
                2 * resources.getDimensionPixelSize(com.eve.app.R.dimen.design_screen_margin)).coerceAtLeast(resources.getDimensionPixelSize(com.eve.app.R.dimen.design_touch_min))
            ellipsize = android.text.TextUtils.TruncateAt.END

            applyChipVisualState(this, checked)

            setOnCheckedChangeListener { chipView, isNowChecked ->
                applyChipVisualState(this, isNowChecked)
                if (UiArcMotion.isMotionEnabled(context)) {
                    chipView.scaleX = 0.90f
                    chipView.scaleY = 0.90f
                    chipView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(UiArcMotion.Spring.SNAPPY.settlingDurationMs)
                        .setInterpolator(UiArcMotion.Spring.SNAPPY)
                        .start()
                }
            }
        }

    fun applyChipVisualState(chip: Chip, isChecked: Boolean) {
        val context = chip.context
        chip.chipStrokeWidth = 0f
        if (isChecked) {
            chip.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(context, com.eve.app.R.color.accent)
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.eve.app.R.color.on_accent))
            chip.isCheckedIconVisible = true
        } else {
            chip.chipBackgroundColor = androidx.core.content.ContextCompat.getColorStateList(context, com.eve.app.R.color.surface_inset)
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(context, com.eve.app.R.color.text_primary))
            chip.isCheckedIconVisible = false
        }
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
        dialog.behavior.maxHeight = Math.round(vh * 0.92f)
        // UIArc dual detents: [0.45, 0.92] with 0.92 expanded height and 0.45 resting peek detent
        dialog.behavior.isFitToContents = false
        dialog.behavior.halfExpandedRatio = 0.45f
        dialog.behavior.peekHeight = Math.round(vh * 0.45f)
        dialog.behavior.expandedOffset = Math.round(vh * 0.08f)
        dialog.behavior.skipCollapsed = false

        dialog.show()
        com.eve.app.designsystem.DesignSurfaces.dialog(dialog)
        com.eve.app.designsystem.DesignSurfaces.sheet(binding.root)

        // Source: BottomSheet.java:135-192,213-222,1766-1778. Telegram does NOT
        // use Window FLAG_DIM_BEHIND here; its SheetBackDrawable owns alpha=51.
        // Apply only to this EVE Target Exams sheet, leaving Material sheet
        // content, detents, scroll, outside-dismiss and state handling unchanged.
        dialog.findViewById<View>(com.google.android.material.R.id.coordinator)?.let { host ->
            val scrim = TelegramBottomSheetSourceScrim().apply { alpha = 0 }
            host.background = scrim
            dialog.window?.let { window ->
                window.setDimAmount(0f)
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
            if (!UiArcMotion.isMotionEnabled(activity)) {
                scrim.alpha = 51
            } else {
                val fade = ValueAnimator.ofInt(0, 51).apply {
                    duration = 400L
                    startDelay = 20L
                    interpolator = CubicBezierInterpolator.EASE_OUT_QUINT
                    addUpdateListener { scrim.alpha = it.animatedValue as Int }
                }
                host.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) = Unit
                    override fun onViewDetachedFromWindow(v: View) {
                        fade.cancel()
                        v.removeOnAttachStateChangeListener(this)
                    }
                })
                fade.start()
            }
        }

        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }
}

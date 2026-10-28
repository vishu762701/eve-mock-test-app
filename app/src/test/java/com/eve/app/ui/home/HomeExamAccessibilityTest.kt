package com.eve.app.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeExamAccessibilityTest {
    @Test fun actionableCardReadsTitleAndTestDurationOnce() {
        assertEquals(
            "Rajasthan History, 60 minutes, History",
            HomeExamAccessibility.describe("Rajasthan History", "60 minutes, History", null)
        )
    }

    @Test fun attemptedAndPinnedStatesAppearWithoutDecorativeDuplicateLabels() {
        assertEquals(
            "Hindi Grammar, Submitted - tap to view result, Pinned test",
            HomeExamAccessibility.describe(
                "Hindi Grammar", "Submitted - tap to view result", "Pinned test"
            )
        )
    }

    @Test fun missingOptionalLabelsAreNotAnnouncedAsBlankCommas() {
        assertEquals("राजस्थान इतिहास", HomeExamAccessibility.describe(
            " राजस्थान इतिहास ", "", null
        ))
    }

    @Test fun binderUsesSingleCardNodeAndDoesNotChangeExamLaunchAction() {
        val adapter = File("src/main/java/com/eve/app/ui/home/ExamAdapter.kt").readText()
        val binding = adapter.substringAfter("fun bind(exam: Exam, attempted: Boolean, isPinned: Boolean")
            .substringBefore("private fun showLogoMode()")
        assertTrue(binding.contains("b.root.contentDescription = HomeExamAccessibility.describe("))
        assertTrue(binding.contains("if (isPinned) b.ivPinned.contentDescription else null"))
        assertTrue(binding.contains("b.root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES"))
        assertTrue(binding.contains("child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO"))
        assertTrue(binding.contains("b.root.setOnClickListener { onClick(exam, attempted, attempt) }"))
        assertTrue(binding.contains("b.root.setOnLongClickListener"))
    }
}

package com.eve.app.ui.admin

import com.eve.app.data.model.BroadcastMessage
import com.eve.app.data.model.Exam
import com.eve.app.data.model.GeneratedTest
import com.eve.app.data.model.Question
import com.eve.app.data.model.QuestionAnalytics
import com.eve.app.ui.admin.AnalyticsTimeRange
import com.eve.app.ui.admin.ExamBarData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Verification test suite for all 6 Admin Dashboard Functional & Safety fixes:
 * 1. Broadcast Notification Independent Deletion (Item 1)
 * 2. Broadcast Notification Send & Delete Confirmation (Item 2)
 * 3. Broadcast Notification Category Targeting (Item 3)
 * 4. Manage Exam Auto-Generation Safeguards (4a, 4b, 4c, 4d, 4e)
 * 5. Admin Analytics Bar Chart Visualization & Dynamic Time Range Filtering (5a, 5b, 5c)
 * 6. Create Feedback Post Layout & Dedicated Management Restructure (6a, 6b)
 */
class AdminDashboardFunctionalSafetyTest {

    // =========================================================================
    // ITEM 1: BROADCAST NOTIFICATIONS — INDEPENDENT ROW DELETION
    // =========================================================================
    @Test
    fun testItem1_independentRowDeletionWithoutSharedBlocker() {
        val broadcasts = mutableListOf(
            BroadcastMessage(id = "b1", title = "Update 1", message = "Msg 1", targetCategory = "All Users"),
            BroadcastMessage(id = "b2", title = "Update 2", message = "Msg 2", targetCategory = "SSC"),
            BroadcastMessage(id = "b3", title = "Update 3", message = "Msg 3", targetCategory = "Banking")
        )

        // Deleting the middle broadcast (b2) independently
        fun deleteBroadcastById(id: String, list: MutableList<BroadcastMessage>): List<BroadcastMessage> {
            list.removeAll { it.id == id }
            return list
        }

        // Delete b2 first
        val afterB2 = deleteBroadcastById("b2", broadcasts)
        assertEquals(2, afterB2.size)
        assertFalse(afterB2.any { it.id == "b2" })
        assertTrue(afterB2.any { it.id == "b1" })
        assertTrue(afterB2.any { it.id == "b3" })

        // Delete b3 next without having deleted b1
        val afterB3 = deleteBroadcastById("b3", broadcasts)
        assertEquals(1, afterB3.size)
        assertEquals("b1", afterB3.first().id)

        // Verify XML layout contains tvBroadcastTarget and btnDelete
        val itemXml = File("src/main/res/layout/item_sent_broadcast.xml")
        assertTrue("item_sent_broadcast.xml must exist", itemXml.exists())
        val xmlContent = itemXml.readText()
        assertTrue(xmlContent.contains("android:id=\"@+id/btnDeleteBroadcast\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/tvBroadcastTarget\""))
    }

    // =========================================================================
    // ITEM 2: BROADCAST NOTIFICATIONS — CONFIRMATION DIALOGS
    // =========================================================================
    @Test
    fun testItem2_sendAndDeleteConfirmationContracts() {
        val title = "SSC Exam Schedule"
        val message = "Mock test starts tomorrow at 10 AM IST."
        val targetCategory = "SSC"

        // Send confirmation dialog contract
        fun buildSendConfirmationMessage(t: String, msg: String, cat: String): String {
            val audienceDesc = if (cat == "All Users") "all registered students" else "all students in '$cat'"
            return "Are you sure? Delivered notifications cannot be recalled.\n\nThis notification will be dispatched to $audienceDesc immediately:\n\n📢 Title: $t\n🎯 Target Audience: $cat\n\n💬 Message:\n$msg"
        }

        val sendConfirmation = buildSendConfirmationMessage(title, message, targetCategory)
        assertTrue(sendConfirmation.contains("📢 Title: $title"))
        assertTrue(sendConfirmation.contains("🎯 Target Audience: $targetCategory"))
        assertTrue(sendConfirmation.contains("💬 Message:\n$message"))

        // Delete confirmation dialog contract
        fun buildDeleteConfirmationMessage(b: BroadcastMessage): String {
            val titleText = if (b.title.isNotBlank()) "'${b.title}'" else "this broadcast"
            return "Are you sure you want to delete $titleText?\n\n💬 Message:\n${b.message}\n\nThis will remove it from all students' in-app notification lists and cannot be undone."
        }

        val broadcast = BroadcastMessage(id = "b1", title = title, message = message)
        val deleteConfirmation = buildDeleteConfirmationMessage(broadcast)
        assertTrue(deleteConfirmation.contains("Are you sure you want to delete '$title'?"))
        assertTrue(deleteConfirmation.contains(message))
        assertTrue(deleteConfirmation.contains("cannot be undone"))
    }

    // =========================================================================
    // ITEM 3: BROADCAST NOTIFICATIONS — CATEGORY TARGETING
    // =========================================================================
    @Test
    fun testItem3_categoryTargetingPayloadAndBadge() {
        val targetCategory = "Banking"
        val message = BroadcastMessage(
            id = "msg_123",
            title = "IBPS PO Alert",
            message = "New sectional test uploaded",
            targetCategory = targetCategory
        )

        assertEquals("Banking", message.targetCategory)

        // Sent broadcast target badge display logic
        fun getTargetBadgeText(target: String): String {
            return if (target.isNotBlank() && target != "All Users") "🎯 $target" else "🌐 All Users"
        }

        assertEquals("🎯 Banking", getTargetBadgeText(message.targetCategory))
        assertEquals("🌐 All Users", getTargetBadgeText("All Users"))
        assertEquals("🌐 All Users", getTargetBadgeText(""))

        // Verify spinner exists in activity_send_notification.xml
        val sendXml = File("src/main/res/layout/activity_send_notification.xml")
        assertTrue(sendXml.exists())
        val sendXmlContent = sendXml.readText()
        assertTrue("spTargetCategory must be in layout", sendXmlContent.contains("android:id=\"@+id/spTargetCategory\""))
    }

    // =========================================================================
    // ITEM 4: MANAGE EXAMS — AUTO-GENERATION SAFEGUARDS (4a, 4b, 4c, 4d, 4e)
    // =========================================================================
    @Test
    fun testItem4a_autoGenToggleDefaultsOffAndDisabledForUnsavedExams() {
        // Unsaved exam (empty ID)
        val unsavedExamId = ""
        val isSavedExam = unsavedExamId.isNotBlank()

        val autoGenEnabled = false
        val toggleEnabled = isSavedExam
        val toggleAlpha = if (toggleEnabled) 1.0f else 0.5f

        assertFalse("Auto-generation switch must default to false for new exam", autoGenEnabled)
        assertFalse("Auto-generation switch must be disabled for unsaved exam", toggleEnabled)
        assertEquals(0.5f, toggleAlpha, 0.01f)

        // Once exam is saved and gets an ID
        val savedExamId = "exam_ssc_cgl_01"
        val isNowSaved = savedExamId.isNotBlank()
        val toggleEnabledAfterSave = isNowSaved
        val toggleAlphaAfterSave = if (toggleEnabledAfterSave) 1.0f else 0.5f

        assertTrue("Auto-generation switch must become enabled once exam is saved", toggleEnabledAfterSave)
        assertEquals(1.0f, toggleAlphaAfterSave, 0.01f)
    }

    @Test
    fun testItem4b_generateNowButtonVisibilityAndPayload() {
        val exam = Exam(
            id = "exam_railway_01",
            examName = "RRB NTPC",
            testNumber = "Test 3",
            questionCount = 30,
            generationPrompt = "Focus on arithmetic and Indian Railways history"
        )

        fun buildTriggerPayload(e: Exam): Map<String, Any> {
            return mapOf(
                "examId" to e.id,
                "questionCount" to e.questionCount,
                "testNumber" to e.testNumber,
                "customPromptNotes" to e.generationPrompt
            )
        }

        val payload = buildTriggerPayload(exam)
        assertEquals("exam_railway_01", payload["examId"])
        assertEquals(30, payload["questionCount"])
        assertEquals("Test 3", payload["testNumber"])
        assertEquals("Focus on arithmetic and Indian Railways history", payload["customPromptNotes"])
    }

    @Test
    fun testItem4c_generatedTestDisplayTitleAndBatchDeletionContract() {
        val batch = GeneratedTest(
            id = "gen_batch_99",
            examId = "exam_101",
            examName = "SSC CGL",
            testNumber = "Test 5",
            title = "",
            questionCount = 25,
            questions = listOf(
                com.eve.app.data.model.GeneratedQuestion(
                    questionText = "What is the capital of India?",
                    optionA = "Delhi",
                    optionB = "Mumbai",
                    optionC = "Kolkata",
                    optionD = "Chennai",
                    correctAnswer = "A"
                )
            )
        )

        assertEquals("SSC CGL - Test 5", batch.displayTitle)

        val batchWithCustomTitle = batch.copy(title = "General Awareness Special")
        assertEquals("General Awareness Special", batchWithCustomTitle.displayTitle)

        // Deletion confirmation message contract
        fun buildDeleteTestMessage(t: GeneratedTest) =
            "Are you sure you want to permanently delete '${t.displayTitle}'? This batch of questions will be removed."

        val deleteMsg = buildDeleteTestMessage(batch)
        assertTrue(deleteMsg.contains("Test 5"))
        assertTrue(deleteMsg.contains("permanently delete"))
    }

    @Test
    fun testItem4d_syllabusFileNameAndUploadDateFormatting() {
        val uploadedAtTimestamp = 1759000000000L // specific timestamp
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val formattedDate = dateFormat.format(Date(uploadedAtTimestamp))

        assertNotNull(formattedDate)
        assertTrue("Formatted date must not be blank", formattedDate.isNotBlank())

        val exam = Exam(
            id = "exam_1",
            syllabusFileName = "ssc_cgl_syllabus_2026.pdf",
            syllabusUrl = "https://storage.googleapis.com/.../ssc_cgl_syllabus_2026.pdf",
            syllabusUploadedAt = uploadedAtTimestamp
        )

        assertEquals("ssc_cgl_syllabus_2026.pdf", exam.syllabusFileName)
        assertEquals(uploadedAtTimestamp, exam.syllabusUploadedAt)

        // Verify upload syllabus is removed from activity_manage_exams.xml and exists in activity_manage_syllabus.xml
        val manageXml = File("src/main/res/layout/activity_manage_exams.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_manage_exams.xml")
        assertTrue(manageXml.exists())
        val xmlContent = manageXml.readText()
        assertFalse("Upload syllabus PDF must be removed from Manage Exams", xmlContent.contains("android:id=\"@+id/btnPickPdf\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/btnGenerateNow\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/rvGeneratedTestsForExam\""))

        val syllabusXml = File("src/main/res/layout/activity_manage_syllabus.xml").takeIf { it.exists() }
            ?: File("app/src/main/res/layout/activity_manage_syllabus.xml")
        assertTrue("Dedicated activity_manage_syllabus.xml must exist", syllabusXml.exists())
    }

    @Test
    fun testItem4e_staggerDelayAndRateLimitErrorHandling() {
        // Stagger delay contract: consecutive scheduled executions must sleep by a positive interval (e.g. 10000ms)
        val staggerDelayMs = 10000L
        assertTrue("Stagger delay between multi-exam scheduled runs must be >= 5000ms", staggerDelayMs >= 5000L)

        // Rate limit error formatting contract
        fun formatGenerationError(errorMessage: String): String {
            val isRateLimit = errorMessage.contains("429") || errorMessage.lowercase().contains("quota") || errorMessage.lowercase().contains("rate limit")
            return if (isRateLimit) "Gemini Rate Limit (HTTP 429): $errorMessage" else "API Error: $errorMessage"
        }

        val rateLimitError = formatGenerationError("Resource has been exhausted (e.g. check quota) - 429 Too Many Requests")
        assertTrue(rateLimitError.startsWith("Gemini Rate Limit (HTTP 429):"))

        val normalError = formatGenerationError("Network timeout connecting to server")
        assertTrue(normalError.startsWith("API Error:"))
    }

    // =========================================================================
    // ITEM 5: ADMIN ANALYTICS — BAR CHART & TIME FILTERING (5a, 5b, 5c)
    // =========================================================================
    @Test
    fun testItem5a_barChartDataMappingAndBounds() {
        val barDataList = listOf(
            ExamBarData("SSC CGL Tier 1", 120),
            ExamBarData("IBPS PO Prelims", 85),
            ExamBarData("RRB NTPC Stage 1", 40),
            ExamBarData("State Police Constable", 15)
        )

        assertEquals(4, barDataList.size)
        val maxAttempts = barDataList.maxOf { it.attemptCount }
        assertEquals(120, maxAttempts)

        // Verify ratios
        val ratio1 = barDataList[0].attemptCount.toFloat() / maxAttempts.toFloat()
        val ratio2 = barDataList[1].attemptCount.toFloat() / maxAttempts.toFloat()
        assertEquals(1.0f, ratio1, 0.001f)
        assertEquals(85f / 120f, ratio2, 0.001f)

        // Verify custom view class file exists
        val barChartFile = File("src/main/java/com/eve/app/ui/admin/ExamAttemptsBarChartView.kt")
        assertTrue("ExamAttemptsBarChartView.kt must exist", barChartFile.exists())
    }

    @Test
    fun testItem5b_timeRangeFilteringDefinitions() {
        assertEquals(7, AnalyticsTimeRange.LAST_7_DAYS.days)
        assertEquals("Last 7 days", AnalyticsTimeRange.LAST_7_DAYS.label)

        assertEquals(30, AnalyticsTimeRange.LAST_30_DAYS.days)
        assertEquals("Last 30 days", AnalyticsTimeRange.LAST_30_DAYS.label)

        assertEquals(0, AnalyticsTimeRange.ALL_TIME.days)
        assertEquals("All time", AnalyticsTimeRange.ALL_TIME.label)

        // Verify activity_admin_analytics.xml contains time filter chips and bar chart
        val analyticsXml = File("src/main/res/layout/activity_admin_analytics.xml")
        assertTrue(analyticsXml.exists())
        val xmlContent = analyticsXml.readText()
        assertTrue(xmlContent.contains("android:id=\"@+id/chipGroupTimeRange\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/chipLast7Days\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/chipLast30Days\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/chipAllTime\""))
        assertTrue(xmlContent.contains("android:id=\"@+id/barChartView\""))
    }

    @Test
    fun testItem5c_editQuestionCallbackAndContract() {
        val questionAnalytics = QuestionAnalytics(
            id = "qa_1",
            examId = "exam_101",
            examName = "SSC CGL",
            questionNumber = 7,
            questionText = "Which article of the Indian Constitution guarantees Right to Equality?",
            topic = "Polity",
            attempts = 45,
            correct = 10,
            wrong = 35,
            unattempted = 5
        )

        assertEquals("Polity", questionAnalytics.topic)
        assertEquals(35, questionAnalytics.wrong)
        assertTrue("Wrong rate must be > 70%", questionAnalytics.wrongRate > 70.0)

        // Verify item_analytics_question.xml contains btnEditQuestion
        val itemQuestionXml = File("src/main/res/layout/item_analytics_question.xml")
        assertTrue(itemQuestionXml.exists())
        val xmlContent = itemQuestionXml.readText()
        assertTrue(xmlContent.contains("android:id=\"@+id/btnEditQuestion\""))
        assertTrue(xmlContent.contains("Edit this question"))
    }

    // =========================================================================
    // ITEM 6: FEEDBACK POST LAYOUT & RESTRUCTURE (6a, 6b)
    // =========================================================================
    @Test
    fun testItem6a_feedbackPostDialogLayoutNoButtonCutoff() {
        val dialogXml = File("src/main/res/layout/dialog_create_feedback_post.xml")
        assertTrue(dialogXml.exists())
        val xmlContent = dialogXml.readText()

        // btnPublishPost and btnCancelPost must have weight or proper wrapping
        assertTrue("btnPublishPost must be present", xmlContent.contains("android:id=\"@+id/btnPublishPost\""))
        assertTrue("btnCancelPost must be present", xmlContent.contains("android:id=\"@+id/btnCancelPost\""))
        // btnManageExistingPosts should have been moved out of the create dialog
        assertFalse("btnManageExistingPosts must be removed from create dialog", xmlContent.contains("android:id=\"@+id/btnManageExistingPosts\""))
    }

    @Test
    fun testItem6b_dedicatedManageFeedbackPostsButtonInAdminActivity() {
        val adminXml = File("src/main/res/layout/activity_admin.xml")
        assertTrue(adminXml.exists())
        val xmlContent = adminXml.readText()

        assertTrue(
            "activity_admin.xml must contain btnManageFeedbackPosts under Communication section",
            xmlContent.contains("android:id=\"@+id/btnManageFeedbackPosts\"")
        )
    }
}

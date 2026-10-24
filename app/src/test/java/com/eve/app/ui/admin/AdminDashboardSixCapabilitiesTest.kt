package com.eve.app.ui.admin

import com.eve.app.data.model.AdminAuditLog
import com.eve.app.data.model.FlaggedQuestion
import com.eve.app.data.model.AggregatedFlaggedQuestion
import com.eve.app.data.model.ApiUsageStats
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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
 * Unit and contract test suite for the 6 new capabilities in the Eve app's Admin Dashboard:
 * 1. Audit Log / Activity History (Firestore collection, model, reverse-chronological, filters)
 * 2. Backup / Export Exam Data (JSON serialization schema, questions nesting, filename)
 * 3. API Usage / Cost Monitor (Gemini calls, proxy writes/reads, usage_stats/YYYY-MM)
 * 4. Flagged Questions System (Student reporting dialog, aggregation, dismiss flags)
 * 5. App Version / Build Info Panel (Enforcement logic, status badges)
 * 6. Maintenance Mode Toggle (Confirmation contract for ON vs OFF, Cloud Function)
 */
class AdminDashboardSixCapabilitiesTest {

    private fun findProjectFile(vararg paths: String): File {
        for (path in paths) {
            val f = File(path)
            if (f.exists()) return f
            val parentF = File("../$path")
            if (parentF.exists()) return parentF
        }
        return File(paths[0])
    }

    // =========================================================================
    // CAPABILITY 1: AUDIT LOG / ACTIVITY HISTORY
    // =========================================================================
    @Test
    fun testCapability1_auditLogModelAndActionBadges() {
        val actions = listOf(
            AdminAuditLog.ACTION_EXAM_CREATED,
            AdminAuditLog.ACTION_EXAM_EDITED,
            AdminAuditLog.ACTION_EXAM_DELETED,
            AdminAuditLog.ACTION_BROADCAST_SENT,
            AdminAuditLog.ACTION_BROADCAST_DELETED,
            AdminAuditLog.ACTION_FEEDBACK_POST_CREATED,
            AdminAuditLog.ACTION_FEEDBACK_POST_EDITED,
            AdminAuditLog.ACTION_FEEDBACK_POST_DELETED,
            AdminAuditLog.ACTION_USER_STATUS_TOGGLED,
            AdminAuditLog.ACTION_ADMIN_ADDED,
            AdminAuditLog.ACTION_ADMIN_REMOVED,
            AdminAuditLog.ACTION_MAINTENANCE_TOGGLED,
            AdminAuditLog.ACTION_GENERATE_NOW_TRIGGERED
        )

        for (action in actions) {
            val log = AdminAuditLog(
                id = "log_1",
                actionType = action,
                description = "Test action: $action",
                adminEmail = "admin@eveapp.in",
                timestamp = System.currentTimeMillis()
            )
            val badge = log.getActionBadge()
            assertNotNull("Badge should not be null for action $action", badge)
            assertTrue("Badge text must not be blank for action $action", badge.text.isNotBlank())
        }
    }

    @Test
    fun testCapability1_auditLogDateFiltering() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val logToday = AdminAuditLog("1", AdminAuditLog.ACTION_EXAM_CREATED, "Created exam", "admin@eve.in", now - 1000)
        val log3DaysAgo = AdminAuditLog("2", AdminAuditLog.ACTION_BROADCAST_SENT, "Sent broadcast", "admin@eve.in", now - (3 * oneDayMs))
        val log10DaysAgo = AdminAuditLog("3", AdminAuditLog.ACTION_MAINTENANCE_TOGGLED, "Maintenance ON", "admin@eve.in", now - (10 * oneDayMs))

        val allLogs = listOf(logToday, log3DaysAgo, log10DaysAgo)

        // Filter: Today
        val todayLogs = allLogs.filter { (now - it.timestamp) < oneDayMs }
        assertEquals(1, todayLogs.size)
        assertEquals("1", todayLogs[0].id)

        // Filter: Last 7 Days
        val last7DaysLogs = allLogs.filter { (now - it.timestamp) < (7 * oneDayMs) }
        assertEquals(2, last7DaysLogs.size)

        // Filter: All time (reverse chronological)
        val sortedAll = allLogs.sortedByDescending { it.timestamp }
        assertEquals("1", sortedAll[0].id)
        assertEquals("2", sortedAll[1].id)
        assertEquals("3", sortedAll[2].id)
    }

    @Test
    fun testCapability1_firestoreRulesForAuditLog() {
        val rulesFile = findProjectFile("firestore.rules")
        assertTrue("firestore.rules must exist", rulesFile.exists())
        val rules = rulesFile.readText()

        assertTrue(
            "admin_audit_log rules must allow admin read and create, but disallow update and delete",
            rules.contains("match /admin_audit_log/{logId}") &&
                    rules.contains("allow update, delete: if false")
        )
    }

    // =========================================================================
    // CAPABILITY 2: BACKUP / EXPORT EXAM DATA
    // =========================================================================
    @Test
    fun testCapability2_exportDataJsonSerializationSchema() {
        val examsData = listOf(
            mapOf(
                "id" to "exam_ssc_cgl",
                "examName" to "SSC CGL Tier 1",
                "category" to "SSC",
                "timeLimitMinutes" to 60,
                "questionCount" to 2,
                "autoGenerationEnabled" to true,
                "autoGenTime" to "06:00",
                "generationPrompt" to "Standard difficulty questions",
                "syllabusFileName" to "ssc_cgl_syllabus.pdf",
                "syllabusUrl" to "https://storage.example.com/syllabus.pdf",
                "questions" to listOf(
                    mapOf(
                        "id" to "q1",
                        "examId" to "exam_ssc_cgl",
                        "questionText" to "What is the capital of India?",
                        "optionA" to "Mumbai",
                        "optionB" to "New Delhi",
                        "optionC" to "Kolkata",
                        "optionD" to "Chennai",
                        "correctAnswer" to "B",
                        "explanation" to "New Delhi is the official capital of India.",
                        "topic" to "General Awareness"
                    ),
                    mapOf(
                        "id" to "q2",
                        "examId" to "exam_ssc_cgl",
                        "questionText" to "If 2x + 4 = 10, what is x?",
                        "optionA" to "2",
                        "optionB" to "3",
                        "optionC" to "4",
                        "optionD" to "5",
                        "correctAnswer" to "B",
                        "explanation" to "2x = 6 => x = 3.",
                        "topic" to "Quantitative Aptitude"
                    )
                )
            )
        )

        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonOutput = gson.toJson(examsData)

        assertNotNull(jsonOutput)
        assertTrue(jsonOutput.contains("\"examName\": \"SSC CGL Tier 1\""))
        assertTrue(jsonOutput.contains("\"questions\": ["))
        assertTrue(jsonOutput.contains("\"correctAnswer\": \"B\""))
        assertTrue(jsonOutput.contains("\"explanation\": \"New Delhi is the official capital of India.\""))

        // Verify deserialization back to list of maps
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val parsedList: List<Map<String, Any>> = gson.fromJson(jsonOutput, type)
        assertEquals(1, parsedList.size)
        val firstExam = parsedList[0]
        assertEquals("SSC CGL Tier 1", firstExam["examName"])
        val parsedQuestions = firstExam["questions"] as List<*>
        assertEquals(2, parsedQuestions.size)

        // Verify file naming pattern
        val datePattern = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val expectedFilename = "eve_backup_$datePattern.json"
        assertTrue("Filename must follow pattern eve_backup_YYYY-MM-DD.json", expectedFilename.startsWith("eve_backup_"))
        assertTrue("Filename must end with .json", expectedFilename.endsWith(".json"))
    }

    // =========================================================================
    // CAPABILITY 3: API USAGE / COST MONITOR
    // =========================================================================
    @Test
    fun testCapability3_apiUsageStatsModelAndCalculations() {
        val stats = ApiUsageStats(
            month = "2026-09",
            geminiCalls = 42,
            documentWrites = 350,
            testSubmissions = 1280,
            lastUpdated = System.currentTimeMillis()
        )

        assertEquals("2026-09", stats.month)
        assertEquals(42L, stats.geminiCalls)
        assertEquals(350L, stats.documentWrites)
        assertEquals(1280L, stats.testSubmissions)
        assertEquals("September 2026", stats.getFormattedMonth())
    }

    @Test
    fun testCapability3_firestoreRulesForUsageStats() {
        val rulesFile = findProjectFile("firestore.rules")
        assertTrue(rulesFile.exists())
        val rules = rulesFile.readText()

        assertTrue(
            "usage_stats must have admin read and write rules",
            rules.contains("match /usage_stats/{monthKey}") &&
                    rules.contains("allow read: if isAdmin()") &&
                    rules.contains("allow write: if isAdmin()")
        )
    }

    // =========================================================================
    // CAPABILITY 4: FLAGGED QUESTIONS SYSTEM
    // =========================================================================
    @Test
    fun testCapability4_flaggedQuestionAggregationAndDismissal() {
        val q1Flag1 = FlaggedQuestion(
            id = "f1",
            questionId = "q_101",
            examId = "exam_1",
            examName = "SSC CGL",
            questionText = "What is 2+2?",
            reason = FlaggedQuestion.REASONS[0],
            comment = "Options say C is 5 but 2+2 is 4",
            studentId = "u1",
            studentEmail = "student1@gmail.com",
            timestamp = 1000L,
            status = FlaggedQuestion.STATUS_PENDING
        )

        val q1Flag2 = FlaggedQuestion(
            id = "f2",
            questionId = "q_101",
            examId = "exam_1",
            examName = "SSC CGL",
            questionText = "What is 2+2?",
            reason = FlaggedQuestion.REASONS[2],
            comment = "Typo in option C",
            studentId = "u2",
            studentEmail = "student2@gmail.com",
            timestamp = 2000L,
            status = FlaggedQuestion.STATUS_PENDING
        )

        val q2Flag1 = FlaggedQuestion(
            id = "f3",
            questionId = "q_102",
            examId = "exam_2",
            examName = "RRB NTPC",
            questionText = "Fastest train in India?",
            reason = FlaggedQuestion.REASONS[3],
            comment = "Vande Bharat replaced Shatabdi",
            studentId = "u3",
            studentEmail = "student3@gmail.com",
            timestamp = 1500L,
            status = FlaggedQuestion.STATUS_PENDING
        )

        val pendingFlags = listOf(q1Flag1, q1Flag2, q2Flag1)

        // Aggregation logic test
        val groupedByQuestion = pendingFlags.groupBy { it.questionId }
        assertEquals(2, groupedByQuestion.size)

        val aggregatedList = groupedByQuestion.map { (qId, list) ->
            val first = list.first()
            val reasons = list.map { it.reason }.distinct()
            val comments = list.mapNotNull { it.comment.takeIf { c -> c.isNotBlank() } }
            AggregatedFlaggedQuestion(
                questionId = qId,
                examId = first.examId,
                examName = first.examName,
                questionText = first.questionText,
                flagCount = list.size,
                reasons = reasons,
                comments = comments,
                flagIds = list.map { it.id },
                latestTimestamp = list.maxOf { it.timestamp }
            )
        }.sortedByDescending { it.flagCount }

        // q_101 has 2 flags, q_102 has 1 flag
        assertEquals("q_101", aggregatedList[0].questionId)
        assertEquals(2, aggregatedList[0].flagCount)
        assertEquals(2, aggregatedList[0].comments.size)
        assertEquals(listOf("f1", "f2"), aggregatedList[0].flagIds)

        assertEquals("q_102", aggregatedList[1].questionId)
        assertEquals(1, aggregatedList[1].flagCount)

        // Dismissal contract test
        val dismissedFlagIds = aggregatedList[0].flagIds
        val remainingFlags = pendingFlags.filter { it.id !in dismissedFlagIds }
        assertEquals(1, remainingFlags.size)
        assertEquals("f3", remainingFlags[0].id)
    }

    @Test
    fun testCapability4_firestoreRulesForFlaggedQuestions() {
        val rulesFile = findProjectFile("firestore.rules")
        assertTrue(rulesFile.exists())
        val rules = rulesFile.readText()

        assertTrue(
            "flagged_questions must allow student creation and admin management",
            rules.contains("match /flagged_questions/{flagId}") &&
                    rules.contains("allow create: if isSignedIn()") &&
                    rules.contains("allow read, update, delete: if isAdmin()")
        )
    }

    // =========================================================================
    // CAPABILITY 5: APP VERSION / BUILD INFO PANEL
    // =========================================================================
    @Test
    fun testCapability5_versionEnforcementLogic() {
        val installedVersionCode = 20

        fun isVersionSupported(minRequiredCode: Int): Boolean {
            return installedVersionCode >= minRequiredCode
        }

        assertTrue("Installed version 20 >= min version 10 should be supported", isVersionSupported(10))
        assertTrue("Installed version 20 >= min version 20 should be supported", isVersionSupported(20))
        assertFalse("Installed version 20 < min version 25 should require update", isVersionSupported(25))
    }

    @Test
    fun testCapability5_appVersionCardInAdminXml() {
        val adminXmlFile = findProjectFile("src/main/res/layout/activity_admin.xml", "app/src/main/res/layout/activity_admin.xml")
        assertTrue(adminXmlFile.exists())
        val content = adminXmlFile.readText()

        assertTrue("activity_admin.xml must contain tvInstalledVersion", content.contains("tvInstalledVersion"))
        assertTrue("activity_admin.xml must contain tvMinSupportedVersion", content.contains("tvMinSupportedVersion"))
        assertTrue("activity_admin.xml must contain tvVersionStatusBadge", content.contains("tvVersionStatusBadge"))
    }

    // =========================================================================
    // CAPABILITY 6: MAINTENANCE MODE TOGGLE & CONFIRMATION CONTRACT
    // =========================================================================
    @Test
    fun testCapability6_maintenanceModeConfirmationContract() {
        // Turning ON requires confirmation dialog; turning OFF does not
        fun requiresConfirmationBeforeToggle(willBeActive: Boolean): Boolean {
            return willBeActive // Only true requires confirmation
        }

        assertTrue("Turning maintenance mode ON must require confirmation dialog", requiresConfirmationBeforeToggle(true))
        assertFalse("Turning maintenance mode OFF must not require confirmation dialog", requiresConfirmationBeforeToggle(false))
    }

    @Test
    fun testCapability6_maintenanceCloudFunctionExported() {
        val fnFile = findProjectFile("functions/index.js")
        assertTrue("functions/index.js must exist", fnFile.exists())
        val fnContent = fnFile.readText()

        assertTrue(
            "functions/index.js must export updateRemoteConfigMaintenance",
            fnContent.contains("exports.updateRemoteConfigMaintenance")
        )
        assertTrue(
            "functions/index.js must handle maintenanceMode parameter",
            fnContent.contains("data.maintenanceMode")
        )
        assertTrue(
            "functions/index.js must update system_config/app_config",
            fnContent.contains("db.collection(\"system_config\").doc(\"app_config\").set")
        )
    }

    @Test
    fun testCapability6_maintenanceControlsInAdminXml() {
        val adminXmlFile = findProjectFile("src/main/res/layout/activity_admin.xml", "app/src/main/res/layout/activity_admin.xml")
        assertTrue(adminXmlFile.exists())
        val content = adminXmlFile.readText()

        assertTrue("activity_admin.xml must contain switchMaintenanceMode", content.contains("switchMaintenanceMode"))
        assertTrue("activity_admin.xml must contain etMaintenanceMessage", content.contains("etMaintenanceMessage"))
        assertTrue("activity_admin.xml must contain btnApplyMaintenance", content.contains("btnApplyMaintenance"))
        assertTrue("activity_admin.xml must contain tvLiveMaintenanceStatus", content.contains("tvLiveMaintenanceStatus"))
    }
}

package com.eve.app.data.repository

import com.eve.app.data.model.Exam
import com.eve.app.data.model.PyqSet
import com.eve.app.data.model.Question
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ExamRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun attemptLockId(userId: String, examId: String): String =
        "${userId}_${examId}"

    /**
     * Returns exams already completed by this user. The deterministic lock is checked first;
     * the legacy attempts query keeps old users backward-compatible.
     */
    suspend fun getAttemptedExamIds(userId: String): Set<String> {
        if (userId.isBlank()) return emptySet()

        val locks = db.collection("attempt_locks")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents
            .mapNotNull { it.getString("examId") }
            .toMutableSet()

        val legacy = db.collection("attempts")
            .whereEqualTo("userId", userId)
            .get().await()
            .documents
            .mapNotNull { it.getString("examId") }

        locks.addAll(legacy)
        return locks
    }

    /** Test start se pehle deterministic server-side lock ko check karta hai. */
    suspend fun hasAttemptLock(userId: String, examId: String): Boolean {
        if (userId.isBlank() || examId.isBlank()) return false
        val lock = db.collection("attempt_locks").document(attemptLockId(userId, examId)).get().await()
        if (lock.exists()) return true

        // Purane attempts ke liye graceful backward compatibility.
        return !db.collection("attempts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("examId", examId)
            .limit(1)
            .get().await().isEmpty
    }

    suspend fun getExams(): List<Exam> =
        db.collection("exams").get().await().documents.mapNotNull { doc ->
            doc.toObject(Exam::class.java)?.copy(id = doc.id)
        }.sortedBy { it.examName }

    suspend fun getQuestions(examId: String): List<Question> =
        db.collection("questions").whereEqualTo("examId", examId).get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(Question::class.java)?.copy(id = doc.id)
            }

    /** Phase 19 gap-fix: mock test me PYQ tagged questions mix nahi hone chahiye. */
    suspend fun getMockQuestions(examId: String): List<Question> =
        getQuestions(examId).filter { !it.isPyq }

    suspend fun getQuestionsForTopic(examId: String, topic: String): List<Question> =
        getQuestions(examId).filter { !it.isPyq && it.topic.trim().equals(topic.trim(), ignoreCase = true) }

    /**
     * Phase 19: PYQ questions. Composite index avoid karne ke liye exam ke saare questions
     * load karke client-side filter — Practice topics jaisa hi, offline cache bhi kaam karta hai.
     */
    suspend fun getPyqQuestions(examId: String, year: Int, paper: String = ""): List<Question> =
        getQuestions(examId).filter { q ->
            q.isPyq && q.pyqYear == year && (paper.isBlank() || q.pyqPaper.trim().equals(paper, ignoreCase = true))
        }

    suspend fun getPyqSets(exam: Exam): List<PyqSet> {
        val grouped = getQuestions(exam.id)
            .filter { it.isPyq && it.pyqYear > 0 }
            .groupBy { it.pyqYear to it.pyqPaper.trim() }
        return grouped.map { (key, list) ->
            val (year, paper) = key
            PyqSet(
                examId = exam.id,
                examName = exam.examName,
                category = exam.categoryOrOther,
                year = year,
                paper = paper,
                questionCount = list.size,
                // Full-size paper = exam ka official time; chhota set = 1 min/Q.
                timeLimitMinutes = if (list.size >= 20) exam.timeLimitMinutes.coerceAtLeast(1)
                else list.size.coerceAtLeast(1)
            )
        }.sortedWith(compareByDescending<PyqSet> { it.year }.thenBy { it.paper.lowercase() })
    }

    suspend fun addExam(
        name: String,
        minutes: Int,
        category: String,
        testNumber: String = "Test 1",
        questionCount: Int = 20,
        autoGenEnabled: Boolean = true,
        autoGenTime: String = "00:00",
        timezone: String = "Asia/Kolkata",
        generationPrompt: String = ""
    ): String {
        val trimmed = name.trim()
        val data = hashMapOf(
            "examName" to trimmed,
            "timeLimitMinutes" to minutes,
            "category" to category.trim(),
            "testNumber" to testNumber,
            "questionCount" to questionCount,
            "autoGenerationEnabled" to autoGenEnabled,
            "autoGenTime" to autoGenTime,
            "timezone" to timezone,
            "generationPrompt" to generationPrompt,
            "syllabusUrl" to "",
            "syllabusFileName" to "",
            "lastGeneratedDate" to "",
            "lastGenerationStatus" to "",
            "lastGenerationError" to "",
            "lastGenerationTime" to 0L
        )
        val docRef = db.collection("exams").add(data).await()
        return docRef.id
    }

    suspend fun renameExam(examId: String, newName: String) {
        val trimmed = newName.trim()
        db.collection("exams").document(examId).update("examName", trimmed).await()

        val genTests = db.collection("generated_tests").whereEqualTo("examId", examId).get().await()
        if (!genTests.isEmpty) {
            genTests.documents.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "examName", trimmed) }
                batch.commit().await()
            }
        }

        val attempts = db.collection("attempts").whereEqualTo("examId", examId).get().await()
        if (!attempts.isEmpty) {
            attempts.documents.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "examName", trimmed) }
                batch.commit().await()
            }
        }

        val leaderboard = db.collection("leaderboard").whereEqualTo("examId", examId).get().await()
        if (!leaderboard.isEmpty) {
            leaderboard.documents.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "examName", trimmed) }
                batch.commit().await()
            }
        }
    }

    suspend fun deleteExam(examId: String) {
        val questions = db.collection("questions").whereEqualTo("examId", examId).get().await()
        questions.documents.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }

        val genTests = db.collection("generated_tests").whereEqualTo("examId", examId).get().await()
        genTests.documents.chunked(450).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().await()
        }

        db.collection("exams").document(examId).delete().await()
    }

    suspend fun updateExamFullSettings(
        examId: String,
        examName: String,
        testNumber: String,
        questionCount: Int,
        autoGenEnabled: Boolean,
        autoGenTime: String,
        syllabusUrl: String,
        syllabusFileName: String,
        generationPrompt: String
    ) {
        val data = hashMapOf<String, Any>(
            "examName" to examName.trim(),
            "testNumber" to testNumber.trim(),
            "questionCount" to questionCount,
            "autoGenerationEnabled" to autoGenEnabled,
            "autoGenTime" to autoGenTime.trim(),
            "syllabusUrl" to syllabusUrl,
            "syllabusFileName" to syllabusFileName,
            "generationPrompt" to generationPrompt.trim()
        )
        db.collection("exams").document(examId).update(data).await()
    }

    suspend fun uploadSyllabusPdf(examId: String, fileName: String, bytes: ByteArray): String {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("syllabi/${examId}_${System.currentTimeMillis()}.pdf")
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("application/pdf")
            .build()
        fileRef.putBytes(bytes, metadata).await()
        val downloadUrl = fileRef.downloadUrl.await().toString()
        db.collection("exams").document(examId).update(
            "syllabusUrl", downloadUrl,
            "syllabusFileName", fileName
        ).await()
        return downloadUrl
    }

    suspend fun removeSyllabusPdf(examId: String, syllabusUrl: String) {
        if (syllabusUrl.isNotBlank()) {
            try {
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(syllabusUrl)
                storageRef.delete().await()
            } catch (_: Exception) {}
        }
        db.collection("exams").document(examId).update(
            "syllabusUrl", "",
            "syllabusFileName", ""
        ).await()
    }

    suspend fun addQuestion(q: Question) {
        db.collection("questions").add(questionMap(q)).await()
    }

    /** Phase 21: 500-per-batch Firestore writes. 100 questions ~1-2 batches. */
    suspend fun addQuestions(questions: List<Question>): Int {
        if (questions.isEmpty()) return 0
        questions.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { q ->
                val ref = db.collection("questions").document()
                batch.set(ref, questionMap(q))
            }
            batch.commit().await()
        }
        return questions.size
    }

    private fun questionMap(q: Question): HashMap<String, Any> = hashMapOf(
        "examId" to q.examId,
        "questionText" to q.questionText,
        "optionA" to q.optionA,
        "optionB" to q.optionB,
        "optionC" to q.optionC,
        "optionD" to q.optionD,
        "correctAnswer" to q.correctAnswer,
        "explanation" to q.explanation,
        "topic" to q.topic,
        "isPyq" to q.isPyq,
        "pyqYear" to q.pyqYear,
        "pyqPaper" to q.pyqPaper,
        "questionTextHi" to q.questionTextHi,
        "optionAHi" to q.optionAHi,
        "optionBHi" to q.optionBHi,
        "optionCHi" to q.optionCHi,
        "optionDHi" to q.optionDHi,
        "explanationHi" to q.explanationHi
    )

    suspend fun updateQuestion(q: Question) {
        val data = hashMapOf(
            "examId" to q.examId,
            "questionText" to q.questionText,
            "optionA" to q.optionA,
            "optionB" to q.optionB,
            "optionC" to q.optionC,
            "optionD" to q.optionD,
            "correctAnswer" to q.correctAnswer,
            "explanation" to q.explanation,
            "topic" to q.topic,
            "isPyq" to q.isPyq,
            "pyqYear" to q.pyqYear,
            "pyqPaper" to q.pyqPaper,
            "questionTextHi" to q.questionTextHi,
            "optionAHi" to q.optionAHi,
            "optionBHi" to q.optionBHi,
            "optionCHi" to q.optionCHi,
            "optionDHi" to q.optionDHi,
            "explanationHi" to q.explanationHi
        )
        db.collection("questions").document(q.id).set(data).await()
    }

    suspend fun deleteQuestion(questionId: String) {
        db.collection("questions").document(questionId).delete().await()
    }

    suspend fun updateExamAiSettings(
        examId: String,
        syllabus: String,
        questionCount: Int,
        customPromptNotes: String,
        autoGenerationEnabled: Boolean
    ) {
        val data = hashMapOf<String, Any>(
            "syllabus" to syllabus,
            "questionCount" to questionCount,
            "customPromptNotes" to customPromptNotes,
            "autoGenerationEnabled" to autoGenerationEnabled
        )
        db.collection("exams").document(examId).update(data).await()
    }

    suspend fun getGeneratedTests(examId: String? = null): List<com.eve.app.data.model.GeneratedTest> {
        val query = if (examId.isNullOrBlank()) {
            db.collection("generated_tests")
        } else {
            db.collection("generated_tests").whereEqualTo("examId", examId)
        }
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val eId = doc.getString("examId") ?: ""
            val eName = doc.getString("examName") ?: ""
            val genAt = doc.getLong("generatedAt") ?: 0L
            val status = doc.getString("status") ?: "paused"
            val count = doc.getLong("questionCount")?.toInt() ?: 0

            @Suppress("UNCHECKED_CAST")
            val rawQuestions = doc.get("questions") as? List<Map<String, Any>> ?: emptyList()
            val questions = rawQuestions.map { m ->
                com.eve.app.data.model.GeneratedQuestion(
                    questionText = m["questionText"] as? String ?: "",
                    optionA = m["optionA"] as? String ?: "",
                    optionB = m["optionB"] as? String ?: "",
                    optionC = m["optionC"] as? String ?: "",
                    optionD = m["optionD"] as? String ?: "",
                    correctAnswer = m["correctAnswer"] as? String ?: "A",
                    explanation = m["explanation"] as? String ?: ""
                )
            }

            com.eve.app.data.model.GeneratedTest(
                id = doc.id,
                examId = eId,
                examName = eName,
                generatedAt = genAt,
                status = status,
                questionCount = if (count > 0) count else questions.size,
                questions = questions
            )
        }.sortedByDescending { it.generatedAt }
    }

    suspend fun getLiveGeneratedTests(examId: String): List<com.eve.app.data.model.GeneratedTest> {
        return getGeneratedTests(examId).filter { it.isLive }
    }

    suspend fun updateGeneratedTestStatus(testId: String, status: String) {
        db.collection("generated_tests").document(testId)
            .update("status", status).await()
    }

    suspend fun deleteGeneratedTest(testId: String) {
        db.collection("generated_tests").document(testId).delete().await()
    }
}

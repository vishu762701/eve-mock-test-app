package com.eve.app.data.remote

import com.eve.app.data.model.*
import okhttp3.RequestBody
import retrofit2.http.*

data class AiHealthSlot(
    val slot: String = "",
    val ok: Boolean = false,
    val latencyMs: Long = 0L,
    val providerStatus: String = "",
    val message: String = ""
)

data class AiHealthResponse(
    val primaryModel: String? = null,
    val workingModel: String? = null,
    val slots: List<AiHealthSlot> = emptyList()
)

data class AiGenerationErrorDetails(
    val providerStatus: String? = null,
    val providerMessage: String? = null,
    val model: String? = null,
    val correlationId: String? = null
)

data class AiGenerationErrorResponse(
    val success: Boolean = false,
    val error: String? = null,
    val code: String? = null,
    val details: AiGenerationErrorDetails? = null
)

data class AttemptSubmitResult(
    val attemptId: String,
    val score: Double,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val unattempted: Int,
    val timeTakenSeconds: Long = 0L,
    val counted: Int = 1,
    val answers: List<AnswerItem> = emptyList()
)

data class StartAttemptResponse(
    val startedAt: Long,
    val serverNow: Long,
    val timeLimitSeconds: Long,
    val remainingSeconds: Long? = null,
    val activeSeconds: Long? = null,
    val questions: List<Question>? = null
)

data class QuestionStatsDto(
    val attempts: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
    val unattempted: Int = 0,
    val avgTimeSeconds: Int = 0
)

data class LeaderboardStatsResponse(
    val participants: Int = 0,
    val topperScore: Double = 0.0,
    val averageScore: Double = 0.0,
    val myRank: Int? = null,
    val myScore: Double? = null,
    val myTimeSeconds: Long? = null,
    val myPercentile: Double? = null
)

data class MistakeItem(
    val questionId: String = "",
    val questionText: String = "",
    val questionTextHi: String = "",
    val selected: String = "",
    val selectedText: String = "",
    val selectedTextHi: String = "",
    val correct: String = "",
    val correctText: String = "",
    val correctTextHi: String = "",
    val explanation: String = "",
    val explanationHi: String = "",
    val topic: String = "",
    val examId: String = "",
    val examName: String = "",
    val timestamp: Long = 0L
)

data class StreakResponse(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayCount: Int = 0,
    val lastActiveDate: String? = null
)

data class UserStatsResponse(
    val totalUsers: Long,
    val onlineUsers: Long
)

data class UserVoteResponse(
    val optionIndex: Int?
)

data class AttemptLockResponse(
    val hasLock: Boolean,
    val timestamp: Long? = null
)

data class FloatingLinkResponse(
    val url: String? = null
)

data class SyllabusUploadResponse(
    val syllabusUrl: String,
    val syllabusFileName: String
)

data class UserProfileResponse(
    val id: String,
    val eveId: String? = null,
    val email: String,
    val displayName: String,
    val dob: String,
    val category: String
)

data class AuthUserResponse(
    val uid: String,
    val email: String,
    val displayName: String,
    val isAdmin: Boolean
)

data class SubmitReportRequest(
    val questionId: String,
    val examId: String,
    val examName: String,
    val questionText: String,
    val reason: String,
    val comment: String
)

data class QuestionReportDto(
    val id: String = "",
    val questionId: String = "",
    val examId: String = "",
    val examName: String = "",
    val questionText: String = "",
    val reason: String = "",
    val comment: String = "",
    val studentId: String = "",
    val studentEmail: String = "",
    val timestamp: Long = 0L,
    val status: String = "pending",
    val reportType: String = "content"
)

data class CreateAuditLogRequest(
    val actionType: String,
    val description: String,
    val adminEmail: String? = null
)

data class AdminAuditLogDto(
    val id: String = "",
    val actionType: String = "",
    val description: String = "",
    val adminEmail: String = "",
    val timestamp: Long = 0L
)

interface EveApiService {

    // --- Auth & Profile ---
    @GET("api/auth/me")
    suspend fun getMe(): ApiResponse<AuthUserResponse>

    @POST("api/users/sync")
    suspend fun syncUser(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    @GET("api/users/profile")
    suspend fun getProfile(): ApiResponse<UserProfileResponse>

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    @POST("api/users/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): ApiResponse<Unit>

    @DELETE("api/users/account")
    suspend fun deleteAccount(): ApiResponse<Map<String, Any>>

    // --- Exams ---
    @GET("api/exams")
    suspend fun getExams(): ApiResponse<List<Exam>>

    @GET("api/exams/{id}")
    suspend fun getExam(@Path("id") id: String): ApiResponse<Exam>

    @POST("api/exams")
    suspend fun createExam(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    @PUT("api/exams/{id}")
    suspend fun updateExam(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    @PUT("api/exams/{id}/image")
    suspend fun updateExamImage(@Path("id") id: String, @Body body: Map<String, String>): ApiResponse<Unit>

    @PUT("api/exams/{id}/rename")
    suspend fun renameExam(@Path("id") id: String, @Body body: Map<String, String>): ApiResponse<Unit>

    @DELETE("api/exams/{id}")
    suspend fun deleteExam(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/exams/{id}/syllabus")
    suspend fun uploadSyllabus(
        @Path("id") id: String,
        @Header("x-file-name") fileName: String,
        @Header("Content-Type") contentType: String = "application/pdf",
        @Body body: RequestBody
    ): ApiResponse<SyllabusUploadResponse>

    @DELETE("api/exams/{id}/syllabus")
    suspend fun removeSyllabus(@Path("id") id: String): ApiResponse<Unit>

    // --- Questions ---
    @GET("api/questions/exam/{examId}")
    suspend fun getQuestions(
        @Path("examId") examId: String,
        @Query("isPyq") isPyq: Boolean? = null,
        @Query("topic") topic: String? = null,
        @Query("pyqYear") pyqYear: Int? = null,
        @Query("pyqPaper") pyqPaper: String? = null
    ): ApiResponse<List<Question>>

    @POST("api/questions")
    suspend fun addQuestion(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    @POST("api/questions/batch")
    suspend fun addQuestionsBatch(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, Any>>

    @PUT("api/questions/{id}")
    suspend fun updateQuestion(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    @DELETE("api/questions/{id}")
    suspend fun deleteQuestion(@Path("id") id: String): ApiResponse<Unit>

    @GET("api/questions/stats")
    suspend fun getQuestionStats(@Query("ids") ids: String): ApiResponse<Map<String, QuestionStatsDto>>

    // --- Attempts & Grading ---
    @POST("api/attempts/start")
    suspend fun startAttempt(@Body body: Map<String, String>): ApiResponse<StartAttemptResponse>

    @POST("api/attempts/pause")
    suspend fun pauseAttempt(@Body body: Map<String, String>): ApiResponse<Map<String, @JvmSuppressWildcards Any>>

    @POST("api/attempts/resume")
    suspend fun resumeAttempt(@Body body: Map<String, String>): ApiResponse<Map<String, @JvmSuppressWildcards Any>>

    @POST("api/attempts/submit")
    suspend fun submitAttempt(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<AttemptSubmitResult>

    @GET("api/attempts")
    suspend fun getAttempts(): ApiResponse<List<TestAttempt>>

    @GET("api/attempts/mistakes")
    suspend fun getMistakes(
        @Query("limit") limit: Int = 100,
        @Query("filter") filter: String = "all"
    ): ApiResponse<List<MistakeItem>>

    @GET("api/attempts/streak")
    suspend fun getStreak(@Query("tzOffsetMinutes") tzOffsetMinutes: Int): ApiResponse<StreakResponse>

    @GET("api/attempts/locks")
    suspend fun getAttemptLocks(): ApiResponse<List<String>>

    @GET("api/attempts/locks/{examId}")
    suspend fun checkAttemptLock(@Path("examId") examId: String): ApiResponse<AttemptLockResponse>

    @POST("api/attempts/reset")
    suspend fun resetAttemptPost(@Body body: Map<String, String>): ApiResponse<Map<String, @JvmSuppressWildcards Any>>

    @DELETE("api/attempts/exam/{examId}")
    suspend fun resetAttempt(@Path("examId") examId: String): ApiResponse<Map<String, @JvmSuppressWildcards Any>>

    // --- Leaderboard ---
    @GET("api/leaderboard")
    suspend fun getLeaderboard(
        @Query("examId") examId: String,
        @Query("limit") limit: Int = 50
    ): ApiResponse<List<LeaderboardEntry>>

    @GET("api/leaderboard/rank")
    suspend fun getUserRank(@Query("examId") examId: String): ApiResponse<RankInfo?>

    @GET("api/leaderboard/stats")
    suspend fun getLeaderboardStats(@Query("examId") examId: String): ApiResponse<LeaderboardStatsResponse>

    // --- Broadcasts ---
    @GET("api/broadcasts")
    suspend fun getBroadcasts(@Query("limit") limit: Int = 100): ApiResponse<List<BroadcastMessage>>

    @POST("api/broadcasts")
    suspend fun sendBroadcast(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<BroadcastMessage>

    @DELETE("api/broadcasts/{id}")
    suspend fun deleteBroadcast(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/broadcasts/bulk-delete")
    suspend fun bulkDeleteBroadcasts(@Body body: Map<String, List<String>>): ApiResponse<Map<String, Any>>

    // --- Feedback Messages ---
    @POST("api/feedback/messages")
    suspend fun sendFeedbackMessage(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    @GET("api/feedback/messages")
    suspend fun getFeedbackMessages(): ApiResponse<List<FeedbackMessage>>

    @PUT("api/feedback/messages/{id}/read")
    suspend fun markFeedbackRead(@Path("id") id: String): ApiResponse<Unit>

    @DELETE("api/feedback/messages/{id}")
    suspend fun deleteFeedbackMessage(@Path("id") id: String): ApiResponse<Unit>

    // --- Feedback Posts & Replies ---
    @GET("api/feedback/posts")
    suspend fun getFeedbackPosts(): ApiResponse<List<FeedbackPost>>

    @POST("api/feedback/posts")
    suspend fun createFeedbackPost(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    @PUT("api/feedback/posts/{id}")
    suspend fun updateFeedbackPost(@Path("id") id: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    @DELETE("api/feedback/posts/{id}")
    suspend fun deleteFeedbackPost(@Path("id") id: String): ApiResponse<Unit>

    @GET("api/feedback/posts/{postId}/replies")
    suspend fun getPostReplies(@Path("postId") postId: String): ApiResponse<List<FeedbackPostReply>>

    @POST("api/feedback/posts/{postId}/replies")
    suspend fun submitPostReply(@Path("postId") postId: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    @PUT("api/feedback/posts/{postId}/replies/{replyId}/read")
    suspend fun markReplyRead(@Path("postId") postId: String, @Path("replyId") replyId: String): ApiResponse<Unit>

    @DELETE("api/feedback/posts/{postId}/replies/{replyId}")
    suspend fun deleteReply(@Path("postId") postId: String, @Path("replyId") replyId: String): ApiResponse<Unit>

    // --- Banners ---
    @GET("api/banners")
    suspend fun getBanners(): ApiResponse<List<HomeBanner>>

    @POST("api/banners")
    suspend fun uploadBanner(
        @Header("Content-Type") contentType: String = "image/jpeg",
        @Body body: RequestBody
    ): ApiResponse<HomeBanner>

    @DELETE("api/banners/{id}")
    suspend fun deleteBanner(@Path("id") id: String): ApiResponse<Unit>

    @PUT("api/banners/{id}/reorder")
    suspend fun reorderBanner(@Path("id") id: String, @Body body: Map<String, Boolean>): ApiResponse<Unit>

    // --- Pinned Exams ---
    @GET("api/pins")
    suspend fun getPinnedExams(): ApiResponse<List<String>>

    @POST("api/pins/{examId}")
    suspend fun pinExam(@Path("examId") examId: String): ApiResponse<Unit>

    @DELETE("api/pins/{examId}")
    suspend fun unpinExam(@Path("examId") examId: String): ApiResponse<Unit>

    @POST("api/pins/{examId}/toggle")
    suspend fun togglePin(@Path("examId") examId: String): ApiResponse<Map<String, Boolean>>

    // --- Bookmarks ---
    @GET("api/bookmarks/ids")
    suspend fun getBookmarkIds(): ApiResponse<List<String>>

    @GET("api/bookmarks")
    suspend fun getBookmarks(): ApiResponse<List<BookmarkedQuestion>>

    @POST("api/bookmarks")
    suspend fun addBookmark(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    @DELETE("api/bookmarks/{id}")
    suspend fun deleteBookmark(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/bookmarks/toggle")
    suspend fun toggleBookmark(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, Boolean>>

    // --- Polls ---
    @GET("api/polls")
    suspend fun getPolls(): ApiResponse<List<Poll>>

    @GET("api/polls/active")
    suspend fun getActivePoll(): ApiResponse<Poll?>

    @POST("api/polls")
    suspend fun createPoll(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, String>>

    @PUT("api/polls/{id}/status")
    suspend fun updatePollStatus(@Path("id") id: String, @Body body: Map<String, Boolean>): ApiResponse<Unit>

    @DELETE("api/polls/{id}")
    suspend fun deletePoll(@Path("id") id: String): ApiResponse<Unit>

    @GET("api/polls/{id}/vote")
    suspend fun getUserVote(@Path("id") id: String): ApiResponse<UserVoteResponse>

    @POST("api/polls/{id}/vote")
    suspend fun submitVote(@Path("id") id: String, @Body body: Map<String, Int>): ApiResponse<Unit>

    // --- App Content ---
    @GET("api/app-content/{type}")
    suspend fun getAppContent(@Path("type") type: String): ApiResponse<AppContent>

    @GET("api/app-content/home_hero/admin")
    suspend fun getAdminHomeHero(): ApiResponse<AppContent>

    @PUT("api/app-content/{type}")
    suspend fun updateAppContent(@Path("type") type: String, @Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Unit>

    // --- Admin Management ---
    @GET("api/admin/admins")
    suspend fun getDynamicAdmins(): ApiResponse<List<String>>

    @POST("api/admin/admins")
    suspend fun addAdmin(@Body body: Map<String, String>): ApiResponse<Map<String, String>>

    @DELETE("api/admin/admins/{email}")
    suspend fun removeAdmin(@Path("email") email: String): ApiResponse<Unit>

    @GET("api/admin/analytics/exams")
    suspend fun getExamAnalytics(@Query("days") days: Int? = null): ApiResponse<List<ExamAnalytics>>

    @GET("api/admin/analytics/questions")
    suspend fun getQuestionAnalytics(@Query("examId") examId: String? = null): ApiResponse<List<QuestionAnalytics>>

    @GET("api/admin/stats/users")
    suspend fun getUserStats(): ApiResponse<UserStatsResponse>

    @GET("api/admin/users")
    suspend fun getUsers(@Query("q") query: String? = null): ApiResponse<List<AdminUser>>

    @POST("api/admin/users/{id}/status")
    suspend fun toggleUserStatus(@Path("id") id: String, @Body body: Map<String, Boolean>): ApiResponse<Map<String, Any>>

    @GET("api/admin/users/{id}/attempts")
    suspend fun getUserAttempts(@Path("id") id: String): ApiResponse<List<TestAttempt>>

    @POST("api/feedback/messages/{id}/reply")
    suspend fun replyFeedbackMessage(@Path("id") id: String, @Body body: Map<String, String>): ApiResponse<Map<String, Any>>

    @GET("api/app-config")
    suspend fun getAppConfig(): ApiResponse<AppConfig>

    @PUT("api/admin/config")
    suspend fun updateAppConfig(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<AppConfig>

    // --- Generated Tests ---
    @GET("api/generated-tests")
    suspend fun getGeneratedTests(@Query("examId") examId: String? = null): ApiResponse<List<GeneratedTest>>

    @GET("api/generated-tests/{id}")
    suspend fun getGeneratedTest(@Path("id") id: String): ApiResponse<GeneratedTest>

    @PUT("api/generated-tests/{id}/status")
    suspend fun updateGeneratedTestStatus(@Path("id") id: String, @Body body: Map<String, String>): ApiResponse<Unit>

    @PUT("api/generated-tests/{id}/schedule")
    suspend fun scheduleGeneratedTest(
        @Path("id") id: String,
        @Body body: Map<String, Long>
    ): ApiResponse<Map<String, Any>>

    @DELETE("api/generated-tests/{id}")
    suspend fun deleteGeneratedTest(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/generated-tests/generate-now")
    suspend fun triggerAiTestGeneration(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, Any>>

    @POST("api/admin/ai-health")
    suspend fun checkAiHealth(): ApiResponse<AiHealthResponse>

    // --- Floating Community Link ---
    @GET("api/floating-link")
    suspend fun getFloatingLink(): ApiResponse<FloatingLinkResponse>

    @GET("api/admin/floating-link")
    suspend fun getAdminFloatingLink(): ApiResponse<FloatingLinkResponse>

    @PUT("api/admin/floating-link")
    suspend fun updateAdminFloatingLink(@Body body: Map<String, String>): ApiResponse<FloatingLinkResponse>

    // --- Premium & Payment Endpoints ---
    @GET("api/premium/plan")
    suspend fun getPremiumPlan(): ApiResponse<PremiumPlanDto>

    @GET("api/premium/status")
    suspend fun getPremiumStatus(): ApiResponse<PremiumStatusDto>

    @POST("api/premium/orders/create")
    suspend fun createPremiumOrder(@Body body: CreateOrderRequest): ApiResponse<CreateOrderResponse>

    @GET("api/premium/orders/{orderId}/status")
    suspend fun getOrderStatus(@Path("orderId") orderId: String): ApiResponse<OrderStatusDto>

    @POST("api/premium/orders/{orderId}/simulate-sandbox-payment")
    suspend fun simulateSandboxPayment(@Path("orderId") orderId: String): ApiResponse<Any>

    // --- Admin Premium Management Endpoints ---
    @GET("api/admin/premium/config")
    suspend fun getAdminPremiumConfig(): ApiResponse<AdminPremiumConfigDto>

    @PUT("api/admin/premium/config")
    suspend fun updateAdminPremiumConfig(@Body body: AdminPremiumConfigDto): ApiResponse<Any>

    @GET("api/admin/premium/transactions")
    suspend fun getAdminTransactions(
        @Query("limit") limit: Int = 100,
        @Query("status") status: String = ""
    ): ApiResponse<List<AdminTransactionDto>>

    @GET("api/admin/premium/users")
    suspend fun getAdminPremiumUsers(): ApiResponse<List<AdminPremiumUserDto>>

    @POST("api/admin/premium/users/grant")
    suspend fun grantPremium(@Body body: AdminGrantRequest): ApiResponse<Any>

    @POST("api/admin/premium/users/extend")
    suspend fun extendPremium(@Body body: AdminExtendRequest): ApiResponse<Any>

    @POST("api/admin/premium/users/revoke")
    suspend fun revokePremium(@Body body: AdminRevokeRequest): ApiResponse<Any>

    // --- Question Reports ---
    @POST("api/reports")
    suspend fun submitReport(@Body body: SubmitReportRequest): ApiResponse<Map<String, Any>>

    @GET("api/admin/reports")
    suspend fun getAdminReports(@Query("type") type: String? = null): ApiResponse<List<QuestionReportDto>>

    @PUT("api/admin/reports/{id}/dismiss")
    suspend fun dismissReport(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/admin/reports/dismiss-batch")
    suspend fun dismissReportBatch(@Body body: Map<String, List<String>>): ApiResponse<Unit>

    @DELETE("api/admin/reports/{id}")
    suspend fun deleteReport(@Path("id") id: String): ApiResponse<Unit>

    // --- Admin Audit Log ---
    @POST("api/admin/audit-log")
    suspend fun createAuditLog(@Body body: CreateAuditLogRequest): ApiResponse<Unit>

    @GET("api/admin/audit-log")
    suspend fun getAdminAuditLogs(@Query("range") range: String = "all"): ApiResponse<List<AdminAuditLogDto>>
}

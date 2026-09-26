package com.eve.app.data.remote

import com.eve.app.data.model.*
import okhttp3.RequestBody
import retrofit2.http.*

data class AttemptSubmitResult(
    val attemptId: String,
    val score: Double,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val unattempted: Int
)

data class UserStatsResponse(
    val totalUsers: Long,
    val onlineUsers: Long
)

data class UserVoteResponse(
    val optionIndex: Int?
)

data class AttemptLockResponse(
    val hasLock: Boolean
)

data class SyllabusUploadResponse(
    val syllabusUrl: String,
    val syllabusFileName: String
)

data class UserProfileResponse(
    val id: String,
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

    // --- Attempts & Grading ---
    @POST("api/attempts/submit")
    suspend fun submitAttempt(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<AttemptSubmitResult>

    @GET("api/attempts")
    suspend fun getAttempts(): ApiResponse<List<TestAttempt>>

    @GET("api/attempts/locks")
    suspend fun getAttemptLocks(): ApiResponse<List<String>>

    @GET("api/attempts/locks/{examId}")
    suspend fun checkAttemptLock(@Path("examId") examId: String): ApiResponse<AttemptLockResponse>

    // --- Leaderboard ---
    @GET("api/leaderboard")
    suspend fun getLeaderboard(
        @Query("examId") examId: String,
        @Query("limit") limit: Int = 50
    ): ApiResponse<List<LeaderboardEntry>>

    @GET("api/leaderboard/rank")
    suspend fun getUserRank(@Query("examId") examId: String): ApiResponse<RankInfo?>

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
    suspend fun getExamAnalytics(): ApiResponse<List<ExamAnalytics>>

    @GET("api/admin/analytics/questions")
    suspend fun getQuestionAnalytics(@Query("examId") examId: String? = null): ApiResponse<List<QuestionAnalytics>>

    @GET("api/admin/stats/users")
    suspend fun getUserStats(): ApiResponse<UserStatsResponse>

    // --- Generated Tests ---
    @GET("api/generated-tests")
    suspend fun getGeneratedTests(@Query("examId") examId: String? = null): ApiResponse<List<GeneratedTest>>

    @PUT("api/generated-tests/{id}/status")
    suspend fun updateGeneratedTestStatus(@Path("id") id: String, @Body body: Map<String, String>): ApiResponse<Unit>

    @DELETE("api/generated-tests/{id}")
    suspend fun deleteGeneratedTest(@Path("id") id: String): ApiResponse<Unit>

    @POST("api/generated-tests/generate-now")
    suspend fun triggerAiTestGeneration(@Body body: Map<String, @JvmSuppressWildcards Any>): ApiResponse<Map<String, Any>>
}

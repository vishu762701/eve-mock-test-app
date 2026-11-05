package com.eve.app.ui.admin;

import com.eve.app.data.repository.AdminAnalyticsRepository;
import com.eve.app.data.repository.FlaggedQuestionRepository;
import java.util.List;
import kotlin.coroutines.Continuation;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Synthetic analytics/report transport boundaries for the real gated Activities. */
public final class QuestionReviewBoundaryFixture {
    public static Object exams, analytics, reports;
    public static boolean dismissSuccess=true;
    public static int dismissCalls;
    public static boolean holdReportReads;
    public static final java.util.List<Integer> analyticsDays = new java.util.ArrayList<>();
    public static final java.util.List<ReportRead> reportReads = new java.util.ArrayList<>();
    public static final class ReportRead {
        public final boolean technical;
        public final Continuation<Object> reply;
        @SuppressWarnings("unchecked")
        ReportRead(boolean technical, Continuation<?> reply) {
            this.technical=technical; this.reply=(Continuation<Object>) reply;
        }
    }
    @Implements(value=AdminAnalyticsRepository.class,isInAndroidSdk=false)
    public static class AnalyticsBoundary {
        @Implementation public Object getExamAnalytics(int days,Continuation<?> continuation) { analyticsDays.add(days); return exams; }
        @Implementation public Object getQuestionAnalytics(String examId,Continuation<?> continuation) { return analytics; }
    }
    @Implements(value=FlaggedQuestionRepository.class,isInAndroidSdk=false)
    public static class ReportsBoundary {
        @Implementation public Object getPendingFlaggedQuestions(boolean technical,Continuation<?> continuation) {
            if (holdReportReads) {
                reportReads.add(new ReportRead(technical,continuation));
                return kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
            }
            return reports;
        }
        @Implementation public Object dismissFlags(List<String> ids,boolean technical,Continuation<?> continuation) {
            dismissCalls++; return dismissSuccess;
        }
    }
    @Implements(value=com.eve.app.data.repository.AuditLogRepository.class,isInAndroidSdk=false)
    public static class AuditBoundary {
        @Implementation public Object getLogs(
            com.eve.app.data.repository.AuditLogTimeRange timeRange,
            Long before,
            String adminEmail,
            String actionType,
            String searchQuery,
            int limit,
            Continuation<?> continuation
        ) {
            return java.util.Collections.emptyList();
        }
    }
}

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
    @Implements(value=AdminAnalyticsRepository.class,isInAndroidSdk=false)
    public static class AnalyticsBoundary {
        @Implementation public Object getExamAnalytics(int days,Continuation<?> continuation) { return exams; }
        @Implementation public Object getQuestionAnalytics(String examId,Continuation<?> continuation) { return analytics; }
    }
    @Implements(value=FlaggedQuestionRepository.class,isInAndroidSdk=false)
    public static class ReportsBoundary {
        @Implementation public Object getPendingFlaggedQuestions(boolean technical,Continuation<?> continuation) { return reports; }
        @Implementation public Object dismissFlags(List<String> ids,boolean technical,Continuation<?> continuation) {
            dismissCalls++; return dismissSuccess;
        }
    }
}

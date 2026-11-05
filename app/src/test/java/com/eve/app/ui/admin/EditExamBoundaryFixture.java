package com.eve.app.ui.admin;

import com.eve.app.data.model.Question;
import com.eve.app.data.model.Exam;
import com.eve.app.data.repository.AdminRepository;
import com.eve.app.data.repository.ExamRepository;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/** Test-only auth/data boundary; real Activity/gate/widgets/callbacks remain in use. */
public final class EditExamBoundaryFixture {
    public static Object permissions, exams, questions;
    public static int freshReads, dataReads, writes, questionReads;
    public static Question lastUpdatedQuestion;
    public static Continuation<? super Unit> pendingWrite;
    public static int batchWrites;
    public static java.util.List<Question> lastBatch;
    public static Continuation<? super Integer> pendingBatch;
    public static boolean holdPermissions;
    public static Continuation<Object> pendingPermissions;
    public static boolean holdMembership;
    public static Continuation<? super Boolean> pendingMembership;
    public static boolean holdExamRead;
    public static Continuation<Object> pendingExamRead;
    public static Exam lastUpdatedExam;
    public static int examWrites;
    public static Continuation<? super Unit> pendingExamWrite;
    public static void reset() { freshReads=0; dataReads=0; writes=0; questionReads=0; pendingWrite=null; lastUpdatedQuestion=null; batchWrites=0; lastBatch=null; pendingBatch=null; holdMembership=false; pendingMembership=null; holdPermissions=false; pendingPermissions=null; holdExamRead=false; pendingExamRead=null; lastUpdatedExam=null; examWrites=0; pendingExamWrite=null; }
    @Implements(value=AdminRepository.class,isInAndroidSdk=false)
    public static class AdminBoundary {
        @Implementation public Object isAdminFresh(String uid,String email,Continuation<? super Boolean> continuation) {
            freshReads++;
            if (holdMembership) { pendingMembership=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED(); }
            return true;
        }
        @Implementation public Object isAdmin(String email,Continuation<? super Boolean> continuation) {
            throw new AssertionError("Privileged Activity must use fresh server membership");
        }
        @SuppressWarnings("unchecked")
        @Implementation public Object getAdminMe(Continuation<?> continuation) {
            if (holdPermissions) { pendingPermissions=(Continuation<Object>)continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED(); }
            return permissions;
        }
        @Implementation public Object getAppConfig(Continuation<?> continuation) {
            return new com.eve.app.data.model.AppConfig(1, false, "", "live");
        }
    }
    @Implements(value=ExamRepository.class,isInAndroidSdk=false)
    public static class ExamBoundary {
        @SuppressWarnings("unchecked")
        @Implementation public Object getExams(Continuation<?> continuation) {
            dataReads++;
            if (holdExamRead) { pendingExamRead=(Continuation<Object>)continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED(); }
            return exams != null ? exams : java.util.Collections.emptyList();
        }
        @Implementation public Object updateExam(Exam exam,Continuation<? super Unit> continuation) {
            examWrites++; lastUpdatedExam=exam; pendingExamWrite=continuation;
            return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
        @Implementation public Object getQuestions(String id,Continuation<?> continuation) { dataReads++; questionReads++; return questions; }
        @Implementation public Object updateQuestion(Question question,Continuation<? super Unit> continuation) {
            writes++; lastUpdatedQuestion=question; pendingWrite=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
        @Implementation public Object addQuestions(java.util.List<Question> questions,Continuation<? super Integer> continuation) {
            batchWrites++; lastBatch=questions; pendingBatch=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
        @Implementation public Object deleteQuestion(String id,Continuation<? super Unit> continuation) {
            writes++; pendingWrite=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
        @Implementation public Object addQuestion(Question question,Continuation<? super Unit> continuation) {
            writes++; pendingWrite=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
        @Implementation public Object getGeneratedTests(String examId, Continuation<?> continuation) {
            return java.util.Collections.emptyList();
        }
        @Implementation public Object getGeneratedTests(Continuation<?> continuation) {
            return java.util.Collections.emptyList();
        }
    }
}

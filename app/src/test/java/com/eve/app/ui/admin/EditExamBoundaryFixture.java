package com.eve.app.ui.admin;

import com.eve.app.data.model.Question;
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
    public static void reset() { freshReads=0; dataReads=0; writes=0; questionReads=0; pendingWrite=null; lastUpdatedQuestion=null; }
    @Implements(value=AdminRepository.class,isInAndroidSdk=false)
    public static class AdminBoundary {
        @Implementation public Object isAdminFresh(String uid,String email,Continuation<? super Boolean> continuation) {
            freshReads++; return true;
        }
        @Implementation public Object isAdmin(String email,Continuation<? super Boolean> continuation) {
            throw new AssertionError("Privileged Activity must use fresh server membership");
        }
        @Implementation public Object getAdminMe(Continuation<?> continuation) { return permissions; }
    }
    @Implements(value=ExamRepository.class,isInAndroidSdk=false)
    public static class ExamBoundary {
        @Implementation public Object getExams(Continuation<?> continuation) { dataReads++; return exams; }
        @Implementation public Object getQuestions(String id,Continuation<?> continuation) { dataReads++; questionReads++; return questions; }
        @Implementation public Object updateQuestion(Question question,Continuation<? super Unit> continuation) {
            writes++; lastUpdatedQuestion=question; pendingWrite=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
        @Implementation public Object addQuestion(Question question,Continuation<? super Unit> continuation) {
            writes++; pendingWrite=continuation; return IntrinsicsKt.getCOROUTINE_SUSPENDED();
        }
    }
}

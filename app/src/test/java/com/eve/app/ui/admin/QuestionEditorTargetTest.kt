package com.eve.app.ui.admin

import com.eve.app.data.model.Question
import org.junit.Assert.*
import org.junit.Test

class QuestionEditorTargetTest {
    private val first=Question(id="first",examId="exam",questionText="Same")
    private val target=first.copy(id="target")
    @Test fun knownIdWinsDuplicateTextRegardlessOfOrder() {
        assertSame(target,resolveQuestionEditorTarget(listOf(first,target),"exam","target","Same"))
        assertSame(target,resolveQuestionEditorTarget(listOf(target,first),"exam","target","Same"))
    }
    @Test fun missingKnownIdCannotFallBackToAnotherQuestion() {
        assertNull(resolveQuestionEditorTarget(listOf(first),"exam","missing","Same"))
    }
    @Test fun legacyRowIdAndUniqueTextFallbackAreExamScoped() {
        val other=first.copy(id="other",examId="other")
        assertSame(first,resolveQuestionEditorTarget(listOf(other,first),"exam","","Same",legacyId="row-id"))
        assertSame(target,resolveQuestionEditorTarget(listOf(first,target),"exam","","Same",legacyId="target"))
        assertNull(resolveQuestionEditorTarget(listOf(other),"exam","","Same"))
    }
    @Test fun ambiguousTextOrDuplicateIdsNeverChooseAnArbitraryRecord() {
        assertNull(resolveQuestionEditorTarget(listOf(first,target),"exam","","Same"))
        assertNull(resolveQuestionEditorTarget(listOf(target,target.copy(optionA="Different")),"exam","target","Same"))
    }
    @Test fun incompleteScopeOrUnidentifiedRowsNeverCreateAnEditTarget() {
        assertNull(resolveQuestionEditorTarget(listOf(first),null,"first","Same"))
        assertNull(resolveQuestionEditorTarget(listOf(first),"exam",null,null))
        assertNull(resolveQuestionEditorTarget(listOf(first.copy(id="")),"exam","","Same"))
    }
}

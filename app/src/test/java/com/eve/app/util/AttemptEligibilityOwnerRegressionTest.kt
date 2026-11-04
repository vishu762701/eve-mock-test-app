package com.eve.app.util

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AttemptEligibilityOwnerRegressionTest {
    @Test fun adminPremiumAndHistoryEligibilityUseTheSameOriginalUid() {
        val src = File("src/main/java/com/eve/app/util/AttemptLimitManager.kt").readText()
        val gate = src.substringAfter("suspend fun canAttempt(").substringBefore("fun canAttemptLocal(")
        val admin = gate.indexOf("val adminAllowed = isAdmin(")
        val afterAdmin = gate.indexOf("if (!isOriginalAccount(originalUid)) return false", admin)
        val premium = gate.indexOf("PremiumRepository.isCurrentUserPremium(", afterAdmin)
        val premiumGuard = gate.indexOf("return isOriginalAccount(originalUid)", premium)
        val history = gate.indexOf("val count = getAttemptCount(", premiumGuard)
        val finalGuard = gate.indexOf("return isOriginalAccount(originalUid) && count < MAX_ATTEMPTS", history)
        assertTrue(admin >= 0 && afterAdmin > admin && premium > afterAdmin &&
            premiumGuard > premium && history > premiumGuard && finalGuard > history)
        val count = src.substringAfter("suspend fun getAttemptCount(").substringBefore("suspend fun canAttempt(")
        val fetch = count.indexOf("HistoryRepository().getAttempts(uid)")
        val guard = count.indexOf("if (!isOriginalAccount(uid)) return localCount", fetch)
        val save = count.indexOf("setLocalAttemptCount(context, examId, uid, highest)", guard)
        assertTrue(fetch >= 0 && guard > fetch && save > guard)
    }
}

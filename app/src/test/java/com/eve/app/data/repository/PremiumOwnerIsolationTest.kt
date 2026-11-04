package com.eve.app.data.repository

import com.eve.app.data.model.PremiumStatusDto
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UID ownership and exact payment-response guards without live payment or Firebase calls. */
class PremiumOwnerIsolationTest {
    @Test fun memoryCacheNeverGrantsAnotherAccount() {
        val premium = PremiumRepository.OwnerBoundStatus(
            "account-A", PremiumStatusDto(isPremium = true, isLifetime = true)
        )
        assertTrue(premium.activeFor("account-A", 1000L))
        assertFalse(premium.activeFor("account-B", 1000L))
        assertFalse(premium.activeFor(null, 1000L))
        assertFalse(PremiumRepository.OwnerBoundStatus(
            "account-A", PremiumStatusDto(isPremium = false, isLifetime = true)
        ).activeFor("account-A", 1000L))
    }

    @Test fun ownerAndExpiryMustBothMatch() {
        val limited = PremiumRepository.OwnerBoundStatus(
            "account-A", PremiumStatusDto(isPremium = true, expiresAt = 1500L)
        )
        assertTrue(limited.activeFor("account-A", 1499L))
        assertFalse(limited.activeFor("account-A", 1500L))
        assertFalse(limited.activeFor("account-B", 1499L))
        assertTrue(PremiumRepository.OwnerBoundStatus(
            "account-A", PremiumStatusDto(isPremium = true, expiresAt = null)
        ).activeFor("account-A", 1500L))
    }

    @Test fun staleAndReorderedResponsesCannotPublish() {
        assertTrue(PremiumRepository.acceptsResult("account-A", "account-A", 5L, 5L))
        assertFalse(PremiumRepository.acceptsResult("account-A", "account-B", 5L, 5L))
        assertFalse(PremiumRepository.acceptsResult("account-A", null, 5L, 5L))
        assertFalse(PremiumRepository.acceptsResult("account-A", "account-A", 4L, 5L))
    }

    @Test fun actualPaymentCallersRetainOriginalAccountGuards() {
        val repo = File("src/main/java/com/eve/app/data/repository/PremiumRepository.kt").readText()
        val premium = File("src/main/java/com/eve/app/ui/premium/PremiumActivity.kt").readText()
        val checkout = File("src/main/java/com/eve/app/ui/premium/PaymentCheckoutActivity.kt").readText()
        val home = File("src/main/java/com/eve/app/ui/home/MainActivity.kt").readText()
        val profile = File("src/main/java/com/eve/app/ui/profile/ProfileActivity.kt").readText()
        val refresh = repo.substringAfter("suspend fun refreshStatus(").substringBefore("suspend fun createOrder(")
        assertTrue(repo.contains("cachedStatus?.activeFor(uid, System.currentTimeMillis())"))
        assertTrue(refresh.indexOf("if (!canAcceptResponse(uid, requestEpoch))") >= 0)
        assertTrue(refresh.indexOf("if (!canAcceptResponse(uid, requestEpoch))") <
            refresh.indexOf("cachedStatus = OwnerBoundStatus(uid, status)"))
        assertTrue(repo.contains("if (cachedStatus?.ownerUid == uid || isSameOwner(uid))"))
        assertTrue(repo.contains("if (data.orderId != orderId)"))
        assertTrue(premium.contains("putExtra(PaymentCheckoutActivity.EXTRA_OWNER_UID, requestUid)"))
        assertTrue(checkout.contains("launchedOwner != FirebaseAuth.getInstance().currentUser?.uid"))
        assertTrue(checkout.contains("if (!hasOriginalAccount()) { finish(); break }"))
        assertTrue(checkout.contains("if (!hasOriginalAccount()) { finish(); return@launch }"))
        assertTrue(home.contains("if (!searchOwnerCurrent || FirebaseAuth.getInstance().currentUser?.uid != premiumOwnerUid) return@launch"))
        assertTrue(profile.contains("if (!isProfileOwner()) return@launch"))
    }
}

package com.eve.app.data.repository

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.model.HomeBanner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.lang.reflect.Proxy

class HomeBannerResponseContractTest {
    private fun fake(response: Any): HomeBannerRepository {
        val api = Proxy.newProxyInstance(EveApiService::class.java.classLoader,
            arrayOf(EveApiService::class.java)) { _, _, _ ->
            if (response is Throwable) throw response
            response
        } as EveApiService
        return HomeBannerRepository(api)
    }

    @Test fun deniedAndMalformedBannerListsDoNotLookEmpty() = runBlocking {
        for (answer in listOf(
            ApiResponse<List<HomeBanner>>(false, null, "Denied"),
            ApiResponse<List<HomeBanner>>(true, null),
            ApiResponse<List<HomeBanner>>(false, listOf(HomeBanner(id="stale")), "Denied")
        )) {
            try {
                fake(answer).getBanners()
                fail("Unconfirmed banner list became valid")
            } catch (_: IllegalStateException) { }
        }
        assertTrue(fake(ApiResponse(true, emptyList<HomeBanner>())).getBanners().isEmpty())
    }

    @Test fun serverDeniedBannerMutationsMustStayResultFailures() = runBlocking {
        val denied = fake(ApiResponse<Any>(success=false, error="No permissions"))
        assertTrue(denied.deleteBanner("b").isFailure)
        assertTrue(denied.updateBannerLink("b", "https://example.org", "Action").isFailure)
        assertTrue(denied.reorderBanner("b", moveUp=true).isFailure)
        for (result in listOf(
            denied.deleteBanner("b"),
            denied.updateBannerLink("b", "https://example.org", "Action"),
            denied.reorderBanner("b", true)
        )) {
            try {
                result.getOrThrow()
                fail("Unconfirmed mutation reached a success continuation")
            } catch (_: IllegalStateException) { }
        }
    }

    @Test fun cancellationPropagatesRatherThanBecomingAnOrdinaryMutationFailure() = runBlocking {
        val cancelled = fake(kotlinx.coroutines.CancellationException("Activity stopped"))
        try {
            cancelled.deleteBanner("b")
            fail("Cancellation should propagate to the caller")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        try {
            cancelled.updateBannerLink("b", "https://example.org", "Label")
            fail("Cancellation should propagate to the caller")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        try {
            cancelled.reorderBanner("b", true)
            fail("Cancellation should propagate to the caller")
        } catch (_: kotlinx.coroutines.CancellationException) { }
    }

    @Test fun successfulServerResponsesRemainUsable() = runBlocking {
        val ok = fake(ApiResponse<Any>(success = true))
        assertTrue(ok.deleteBanner("b").isSuccess)
        assertTrue(ok.updateBannerLink("b", "https://example.org", "Label").isSuccess)
        assertTrue(ok.reorderBanner("b", true).isSuccess)
    }

    @Test fun boundedImageReadRejectsOverflowAndEmptyButKeepsExactLimit() {
        val max = 5*1024*1024
        assertEquals(max, readBoundedBannerBytes(ByteArrayInputStream(ByteArray(max))).size)
        try {
            readBoundedBannerBytes(ByteArrayInputStream(ByteArray(max+1)))
            fail("Oversize file must not reach multipart upload")
        } catch (_: IllegalArgumentException) { }
        try {
            readBoundedBannerBytes(ByteArrayInputStream(ByteArray(0)))
            fail("Empty image must not reach multipart upload")
        } catch (_: IllegalArgumentException) { }
    }
}

package com.eve.app.ui.admin.emoji

import com.eve.app.data.remote.ApiResponse
import com.eve.app.data.remote.EmojiItemDto
import com.eve.app.data.remote.EveApiService
import com.eve.app.data.repository.EmojiRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EmojiWorkflowTest {
    private fun api(handler: (String, Array<out Any?>) -> Any): EveApiService =
        Proxy.newProxyInstance(EveApiService::class.java.classLoader, arrayOf(EveApiService::class.java)) { _, method, args ->
            handler(method.name, args ?: emptyArray())
        } as EveApiService

    @Test fun readsAllPagesAndKeepsServerMetadata() = runBlocking {
        val offsets = mutableListOf<Int>()
        val repo = EmojiRepository(api { name, args ->
            check(name == "getEmojiItems")
            val offset = args[2] as Int
            offsets += offset
            ApiResponse(true, (offset until minOf(offset + 100, 250)).map { EmojiItemDto(id = "item$it", byteSize = it) })
        })
        val result = repo.getAllItems().getOrThrow()
        assertEquals(listOf(0,100,200), offsets)
        assertEquals(250,result.size)
        assertEquals(249,result.last().byteSize)
    }

    @Test fun pageFailureOrRepeatedPageCannotBecomeAnEmptySuccessfulCatalogue() = runBlocking {
        val repeated = List(100) { EmojiItemDto(id = "$it") }
        val repeatRepo = EmojiRepository(api { _, _ -> ApiResponse(true,repeated) })
        assertTrue(repeatRepo.getAllItems().isFailure)
        val failedRepo = EmojiRepository(api { _, args ->
            if (args[2] == 0) ApiResponse(true,repeated) else ApiResponse<List<EmojiItemDto>>(false,null,error="Store unavailable")
        })
        assertTrue(failedRepo.getAllItems().isFailure)
    }

    @Test fun incompleteDeletionIsDifferentFromFailureAndOlderSuccessfulServer() = runBlocking {
        for ((data, expected) in listOf(mapOf<String,Any>("mediaCleanupComplete" to false) to false, emptyMap<String,Any>() to true)) {
            val repo = EmojiRepository(api { name, _ -> check(name == "deleteEmojiPack"); ApiResponse(true,data) })
            assertEquals(expected, repo.deletePack("pack").getOrThrow())
        }
    }

    @Test fun nativeFileReadBoundsMemoryAndRejectsEmptyFiles() {
        val exact = ByteArray(EmojiUploadReader.MAX_BYTES) { 42 }
        assertArrayEquals(exact,EmojiUploadReader.read(exact.inputStream()))
        for (bytes in listOf(byteArrayOf(), ByteArray(EmojiUploadReader.MAX_BYTES + 1))) {
            try { EmojiUploadReader.read(bytes.inputStream()); fail("Invalid file accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}

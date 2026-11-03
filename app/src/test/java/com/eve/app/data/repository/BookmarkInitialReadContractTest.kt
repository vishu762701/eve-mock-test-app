package com.eve.app.data.repository

import com.eve.app.data.model.BookmarkedQuestion
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Malformed/failed initial read is an error, not a confirmed empty list. */
class BookmarkInitialReadContractTest {
    @Test fun genuineEmptyServerPageIsValid() {
        assertTrue(confirmedBookmarkRows(true, emptyList(), null).isEmpty())
    }

    @Test fun deniedOrMalformedServerPageMustNotBecomeEmptySuccess() {
        assertThrows(IllegalStateException::class.java) {
            confirmedBookmarkRows(false, emptyList(), "Server denied")
        }
        assertThrows(IllegalStateException::class.java) {
            confirmedBookmarkRows(true, null, null)
        }
        assertThrows(IllegalStateException::class.java) {
            confirmedBookmarkRows(true, listOf(BookmarkedQuestion()), null)
        }
    }

    @Test fun validPayloadPreservesQuestionAndOrder() {
        val one = BookmarkedQuestion(questionId = "q-1")
        val two = BookmarkedQuestion(questionId = "q-2")
        assertEquals(listOf(one, two), confirmedBookmarkRows(true, listOf(one, two), null))
    }

    @Test fun actualObserverPropagatesFailuresIntoExistingViewModelCatch() {
        val repo = File("src/main/java/com/eve/app/data/repository/BookmarkRepository.kt").readText()
        val vm = File("src/main/java/com/eve/app/ui/bookmarks/BookmarksViewModel.kt").readText()
        val read = repo.substringAfter("fun observeBookmarkedQuestions(")
            .substringBefore("suspend fun bookmarkQuestion(")
        val request = read.indexOf("val response = api.getBookmarks()")
        val owner = read.indexOf("if (!accepts(userId)) return@flow", request)
        val confirmed = read.indexOf("confirmedBookmarkRows(", owner)
        val emit = read.indexOf("emitAll(_bookmarksFlow)", confirmed)
        assertTrue(request >= 0 && owner > request && confirmed > owner && emit > confirmed)
        assertTrue(vm.contains("repository.observeBookmarkedQuestions(uid)"))
        assertTrue(vm.contains(".catch { e ->"))
        assertTrue(vm.contains("_state.value = UiState.Error(e.message ?: \"Failed to load bookmarks\")"))
        assertTrue("Do not swallow network exceptions in initial bookmark reads",
            !read.contains("catch (_: Exception)"))
    }
}

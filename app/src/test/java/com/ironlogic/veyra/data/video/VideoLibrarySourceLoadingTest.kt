package com.ironlogic.veyra.data.video

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoLibrarySourceLoadingTest {
    @Test
    fun returnsSuccessfulSourceResults() = runBlocking {
        val result = loadVideoSourceSafely<List<String>>(
            onFailure = { throw AssertionError("Successful source reported a failure.") }
        ) {
            listOf("video-one", "video-two")
        }

        assertEquals(listOf("video-one", "video-two"), result)
    }

    @Test
    fun reportsOrdinaryProviderFailureAndKeepsBatchRecoverable() = runBlocking {
        var reportedMessage: String? = null
        val result = loadVideoSourceSafely<List<String>>(
            onFailure = { reportedMessage = it.message }
        ) {
            throw IllegalStateException("provider unavailable")
        }

        assertNull(result)
        assertEquals("provider unavailable", reportedMessage)
    }

    @Test(expected = CancellationException::class)
    fun propagatesCancellationInsteadOfTreatingItAsProviderFailure() {
        runBlocking {
            loadVideoSourceSafely<List<String>>(
                onFailure = { throw AssertionError("Cancellation must not be reported as a provider failure.") }
            ) {
                throw CancellationException("library scan cancelled")
            }
        }
    }
}

package app.discair.data

import app.discair.model.InboxTrack
import app.discair.model.RadioMix
import app.discair.model.RadioMixRequest
import app.discair.player.QueueItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RadioMixActionsTest {
    private val existing = listOf(QueueItem(songId = "existing", title = "Current song", artist = "A", album = "B"))
    private val ready = RadioMix("ready", "preview", "artist", tracks = listOf(
        InboxTrack(0, "One", "A", "B", 1000, "owned", "one"),
        InboxTrack(1, "Missing", "A", "B", 1000, "missing", null),
        InboxTrack(2, "One", "A", "B", 1000, "owned", "one"),
    ))

    @Test fun artistSuccessReplacesQueueOnceWithOwnedOccurrencesInOrder() = runTest {
        var queue = existing
        var calls = 0
        val result = generateRadioMix(RadioMixRequest("artist", "artist"), { ready }, { queue = it; calls++ }, StandardTestDispatcher(testScheduler))
        assertEquals(ready, result)
        assertEquals(listOf("one", "one"), queue.map { it.songId })
        assertEquals(1, calls)
    }

    @Test fun discoveryPreviewNeverStartsPlayback() = runTest {
        var queue = existing
        generateRadioMix(RadioMixRequest("discovery", tag = "jazz"), { ready }, { queue = it }, StandardTestDispatcher(testScheduler))
        assertEquals(existing, queue)
    }

    @Test fun nonReadyEmptyOrIncompletePreviewPreservesCurrentQueue() = runTest {
        for (mix in listOf(ready.copy(state = "empty"), ready.copy(state = "unconfigured"),
            ready.copy(state = "index_unavailable"), ready.copy(tracks = emptyList()), ready.copy(previewId = null))) {
            var queue = existing
            generateRadioMix(RadioMixRequest("artist", "artist"), { mix }, { queue = it }, StandardTestDispatcher(testScheduler))
            assertEquals(existing, queue)
        }
    }

    @Test fun failureAndCancellationNeverReplaceTheQueue() = runTest {
        for (error in listOf(IllegalStateException("Provider unavailable"), CancellationException("Left the page"))) {
            var queue = existing
            try {
                generateRadioMix(RadioMixRequest("artist", "artist"), { throw error }, { queue = it }, StandardTestDispatcher(testScheduler))
                fail("Expected error")
            } catch (caught: Exception) {
                assertEquals(error, caught)
            }
            assertEquals(existing, queue)
        }
        var queue = existing
        val job = launch {
            generateRadioMix(RadioMixRequest("artist", "artist"), { ready }, { queue = it }, StandardTestDispatcher(testScheduler))
        }
        job.cancel()
        job.join()
        assertEquals(existing, queue)
    }
}

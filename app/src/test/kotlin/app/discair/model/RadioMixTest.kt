package app.discair.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioMixTest {
    private val mixed = """{"state":"ready","kind":"discovery","preview_id":"0123456789abcdef0123456789abcdef",
        "title":"Discair · Discovery Mix · jazz","total_count":5,"owned_count":2,"tracks":[
        {"position":0,"title":"First","artist":"A","album":"B","duration_ms":180500,"match_state":"owned","song_id":"one"},
        {"position":1,"title":"Missing","artist":"A","album":null,"duration_ms":null,"match_state":"missing","song_id":null},
        {"position":2,"title":"Uncertain","artist":"A","album":"B","duration_ms":180000,"match_state":"ambiguous","song_id":"unsafe"},
        {"position":3,"title":"First","artist":"A","album":"B","duration_ms":180500,"match_state":"owned","song_id":"one"},
        {"position":4,"title":"Invalid","artist":"A","album":"B","duration_ms":180000,"match_state":"owned","song_id":"radio:12"}]}"""

    @Test fun fullRowsKeepOrderAndOnlyConfirmedPlayableOccurrencesReachTheQueue() {
        val mix = ApiJson.decodeFromString<RadioMix>(mixed)
        assertEquals((0..4).toList(), mix.tracks.map { it.position })
        assertEquals(listOf("one", "one"), mix.playbackItems().map { it.songId })
        assertEquals(listOf(180, 180), mix.playbackItems().map { it.durationSec })
        assertEquals(5, mix.edition().totalCount)
    }

    @Test fun emptyUnknownAndNonReadyResponsesNeverOfferAReplacementQueue() {
        for (state in listOf("empty", "unconfigured", "index_unavailable", "unavailable")) {
            val mix = ApiJson.decodeFromString<RadioMix>("""{"state":"$state","tracks":[]}""")
            assertTrue(mix.playbackItems().isEmpty())
            assertTrue(mix.explanation.isNotBlank())
            // Defensive against an unsuccessful response carrying old tracks.
            assertTrue(ApiJson.decodeFromString<RadioMix>(mixed).copy(state = state).playbackItems().isEmpty())
        }
        val mix = ApiJson.decodeFromString<RadioMix>(mixed)
        assertTrue(mix.copy(ownedCount = null, tracks = mix.tracks.map { it.copy(matchState = null) }).playbackItems().isEmpty())
    }

    @Test fun importCompletionIsNotOwnershipAndRefreshedOwnershipKeepsSourceOrder() {
        val mix = ApiJson.decodeFromString<RadioMix>(mixed)
        val imported = mix.copy(tracks = mix.tracks.map {
            if (it.position == 1) it.copy(acquisitionStatus = "imported") else it
        })
        assertEquals(listOf("one", "one"), imported.playbackItems().map { it.songId })
        val indexed = imported.copy(tracks = imported.tracks.map {
            if (it.position == 1) it.copy(matchState = "owned", songId = "new") else it
        })
        assertEquals(listOf("one", "new", "one"), indexed.playbackItems().map { it.songId })
    }

    @Test fun artistPreviewAcceptsAdditionalServerMetadata() {
        val mix = ApiJson.decodeFromString<RadioMix>("""{"state":"ready","kind":"artist","preview_id":"abc",
            "owned_count":1,"total_count":1,"tracks":[{"position":0,"title":"Song","artist":"A","album":"B",
            "album_id":"album","duration":120.9,"duration_ms":120900,"match_state":"owned","song_id":"song"}]}""")
        assertEquals("song", mix.playbackItems().single().songId)
        assertEquals(120, mix.playbackItems().single().durationSec)
    }
}

package app.discair

import app.discair.data.CacheMiss
import app.discair.data.LibrarianRepository
import app.discair.data.ReadMode
import app.discair.model.ApiJson
import app.discair.model.RadioMixRequest
import app.discair.net.ConnectionException
import app.discair.net.ConnectionFailure
import app.discair.net.LibrarianClient
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class RadioMixRepositoryTest {
    private val requests = mutableListOf<Request>()
    private val bodies = mutableListOf<String>()
    private var code = 200
    private var body = """{"state":"ready","preview_id":"0123456789abcdef0123456789abcdef","tracks":[]}"""
    private val http = OkHttpClient.Builder().addInterceptor { chain ->
        val request = chain.request()
        requests += request
        bodies += request.body?.let { value -> okio.Buffer().also { value.writeTo(it) }.readUtf8() }.orEmpty()
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build()
    private val repo = LibrarianRepository(LibrarianClient(http, "https://fixture.test", "u", "p"), "https://fixture.test")

    @Test fun previewUsesStructuredSeedAndSelectedControlsWithoutClientTrackMetadata() {
        repo.previewRadioMix(RadioMixRequest("artist", "01234567-89ab-cdef-0123-456789abcdef", familiarity = "deep_cuts"))
        assertEquals("POST", requests.single().method)
        assertEquals("/api/radio-mixes/preview", requests.single().url.encodedPath)
        val sent = ApiJson.parseToJsonElement(bodies.single()).jsonObject
        assertEquals("deep_cuts", sent.getValue("familiarity").jsonPrimitive.content)
        assertEquals("artist", sent.getValue("kind").jsonPrimitive.content)
        assertFalse(sent.containsKey("tracks"))
        assertFalse(sent.containsKey("tag"))
        repo.previewRadioMix(RadioMixRequest("discovery", tag = "jazz", mode = "hard"))
        val tag = ApiJson.parseToJsonElement(bodies.last()).jsonObject
        assertEquals("jazz", tag.getValue("tag").jsonPrimitive.content)
        assertEquals("hard", tag.getValue("mode").jsonPrimitive.content)
        assertFalse(tag.containsKey("artist_mbid"))
    }

    @Test fun readsAreLiveAndDownloadUsesOnlyStoredPreviewAndOriginalPosition() {
        val id = "0123456789abcdef0123456789abcdef"
        repeat(2) { repo.radioMix(id) }
        assertEquals(2, requests.size)
        assertEquals("GET", requests.last().method)
        assertEquals("/api/radio-mixes/$id", requests.last().url.encodedPath)
        body = """{"ok":false,"status":"already_in_progress","artist":"A","job_album":"B [Song]"}"""
        val reply = repo.downloadRadioMixTrack(id, 3)
        assertEquals("/api/radio-mixes/$id/tracks/3/download", requests.last().url.encodedPath)
        assertEquals("POST", requests.last().method)
        assertEquals("", bodies.last())
        assertEquals("B [Song]", reply.acquisitionKey?.jobAlbum)
    }

    @Test fun safeServerRefusalsAndThrottlingAreSurfacedWithoutAutomaticRetry() {
        for (status in listOf(409, 429, 503)) {
            requests.clear()
            code = status
            body = """{"detail":"Cannot confirm recording identity."}"""
            try {
                repo.downloadRadioMixTrack("preview", 1)
                fail("Expected refusal")
            } catch (error: ConnectionException) {
                if (status == 429) assertTrue(error.failure is ConnectionFailure.RateLimited)
                else assertEquals(status, (error.failure as ConnectionFailure.Server).code)
                assertTrue(error.failure.message.contains("Cannot confirm recording identity"))
            }
            assertEquals(1, requests.size)
        }
    }

    @Test fun mixReadsAndGenerationNeverUseAnOfflineCachedPreview() {
        for (read in listOf<() -> Any>({ repo.radioMix("preview") }, { repo.previewRadioMix(RadioMixRequest("discovery", tag = "jazz")) })) {
            try {
                ReadMode.only(true) { read() }
                fail("Expected a live-only read")
            } catch (_: CacheMiss) { }
        }
        assertTrue(requests.isEmpty())
    }
}

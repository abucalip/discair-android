package app.discair.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RadioMixRequest(
    val kind: String,
    @SerialName("artist_mbid") val artistMbid: String? = null,
    val tag: String? = null,
    val familiarity: String = "familiar",
    val mode: String = "easy",
)

@Serializable
data class RadioMix(
    val state: String,
    @SerialName("preview_id") val previewId: String? = null,
    val kind: String = "discovery",
    val title: String = "Discovery Mix",
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("owned_count") val ownedCount: Int? = null,
    val tracks: List<InboxTrack> = emptyList(),
) {
    // Reuse the ordered, confirmed-ownership playback projection; source positions retain repeats.
    fun edition() = InboxEdition(0, title, if (kind == "artist") "artist_radio" else "discovery_mix", null, totalCount, ownedCount, tracks)

    fun playbackItems() = if (state == "ready") edition().playbackItems() else emptyList()

    val explanation: String get() = when (state) {
        "unconfigured" -> "Connect ListenBrainz in Settings to generate mixes."
        "index_unavailable" -> "Library ownership is unavailable. Try again after the library finishes indexing."
        "empty" -> "No tracks matched this mix. Try another seed or setting."
        else -> "This mix is unavailable. Try again later."
    }
}

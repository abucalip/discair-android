package app.discair.data

import app.discair.model.RadioMix
import app.discair.model.RadioMixRequest
import app.discair.player.QueueItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Only a completed, playable Artist Radio preview replaces the native queue. */
internal suspend fun generateRadioMix(
    request: RadioMixRequest,
    preview: (RadioMixRequest) -> RadioMix,
    onPlay: (List<QueueItem>) -> Unit,
    io: CoroutineDispatcher = Dispatchers.IO,
): RadioMix {
    val mix = withContext(io) { preview(request) }
    if (request.kind == "artist" && mix.previewId != null) {
        val items = mix.playbackItems()
        if (items.isNotEmpty()) onPlay(items)
    }
    return mix
}

package app.discair.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.discair.model.RadioMixRequest
import app.discair.net.classifyException
import app.discair.player.QueueItem
import app.discair.ui.LocalBottomClearance
import app.discair.ui.common.BackBar
import app.discair.ui.common.Button
import app.discair.ui.common.FilterChip
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.OutlinedButton
import app.discair.ui.common.OutlinedTextField
import app.discair.ui.common.PageHeading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal val LocalOpenRadioMix = compositionLocalOf<(String, String?) -> Unit> { { _, _ -> } }

@Composable
internal fun RadioMixEntry() {
    val open = LocalOpenRadioMix.current
    OutlinedButton(onClick = { open("discovery", null) },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().heightIn(min = 48.dp)) {
        Text("Discover a mix")
    }
}

@Composable
internal fun ArtistRadioActions(mbid: String?) {
    if (mbid.isNullOrBlank()) return
    val open = LocalOpenRadioMix.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { open("artist", mbid) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Start Artist Radio")
        }
        OutlinedButton(onClick = { open("discovery", mbid) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Discover a mix")
        }
    }
}

@Composable
internal fun RadioMixScreen(kind: String, artistMbid: String?, onBack: () -> Unit, onPlay: (List<QueueItem>) -> Unit) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var tag by rememberSaveable(kind, artistMbid) { mutableStateOf("") }
    var familiarity by rememberSaveable(kind, artistMbid) { mutableStateOf("familiar") }
    var mode by rememberSaveable(kind, artistMbid) { mutableStateOf("easy") }
    var previewId by rememberSaveable(kind, artistMbid) { mutableStateOf<String?>(null) }
    // In-flight work belongs to this composition; restored screens never replay an old request.
    var busy by androidx.compose.runtime.remember(repo, kind, artistMbid) { mutableStateOf(false) }
    var message by rememberSaveable(kind, artistMbid) { mutableStateOf<String?>(null) }
    val currentId = previewId
    if (currentId != null) {
        androidx.activity.compose.BackHandler { previewId = null }
        RadioMixDetailScreen(currentId, onBack = { previewId = null }, onPlay = onPlay)
        return
    }
    val artist = kind == "artist"
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        BackBar(onBack, if (artist) "Artist Radio" else "Discovery Mix")
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
            bottom = 24.dp + LocalBottomClearance.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { PageHeading(if (artist) "Artist Radio" else "Discover a mix", large = true) }
            item { Text(if (artist) "Play a mix of owned tracks from this artist and similar artists."
                else "Browse a mix, play owned tracks, and download discoveries.") }
            if (artistMbid == null) item {
                OutlinedTextField(tag, { tag = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    label = { Text("Tag") }, placeholder = { Text("e.g. jazz") }, singleLine = true,
                    supportingText = { Text("For an artist seed, open Discover a mix on an artist page.") })
            }
            item {
                if (artist) MixChoices("Familiarity", familiarity,
                    listOf("familiar" to "Familiar", "explore" to "Explore", "deep_cuts" to "Deep cuts"), !busy) { familiarity = it }
                else MixChoices("Mode", mode, listOf("easy" to "Easy", "medium" to "Medium", "hard" to "Hard"), !busy) { mode = it }
            }
            item {
                Button(onClick = {
                    if (!busy) {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                val mix = app.discair.data.generateRadioMix(
                                    RadioMixRequest(kind, artistMbid, tag.trim().takeIf { artistMbid == null }, familiarity, mode),
                                    repo::previewRadioMix, onPlay,
                                )
                                val id = mix.previewId
                                if (mix.state == "ready" && id != null) {
                                    previewId = id
                                } else message = mix.explanation
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                message = classifyException(e, networkAvailable = true).message
                            } finally { busy = false }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    enabled = !busy && (artistMbid != null || tag.trim().length in 1..80)) {
                    Text(if (busy) "Generating mix…" else if (artist) "Start Artist Radio" else "Generate mix")
                }
            }
            item { message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        }
    }
}

@Composable
private fun MixChoices(label: String, selected: String, options: List<Pair<String, String>>, enabled: Boolean, choose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options, key = { it.first }) { (value, title) ->
                FilterChip(selected = selected == value, onClick = { choose(value) }, enabled = enabled,
                    label = { Text(title) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

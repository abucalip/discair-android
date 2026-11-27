package app.discair.ui.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.model.PathKind
import app.discair.model.PathLink
import app.discair.model.PathPane
import app.discair.model.PathStop
import app.discair.model.PathsResponse
import app.discair.model.PathsView
import app.discair.model.heading
import app.discair.model.pane
import app.discair.model.view
import app.discair.ui.common.ArtistPhoto
import app.discair.ui.common.BentoBanner
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSkeleton
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Load
import app.discair.ui.common.RequestUpdateButton
import app.discair.ui.common.bentoTones
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Artist Paths sheet body (BENTO_THEME.md step B3, paths frames 3 & 4).
 * Preserves the exact state, lazy fetching, and Back behaviour of [ArtistPathsSheet].
 * Steps render as rotating pastel tone tiles with round artist photos; an opened step renders a `deep` musician/step tile,
 * the path link details in Bento tiles, and Open / Next step actions.
 */
@Composable
internal fun BentoArtistPathList(
    artistName: String,
    mbid: String,
    kind: PathKind,
    state: Load<PathsResponse>,
    step: Int,
    list: LazyListState,
    go: (PathLink) -> Unit,
    onKind: (PathKind) -> Unit,
    onStep: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Side, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtistPhoto(artistName, null, Modifier.size(52.dp), circle = true)
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                BentoEyebrow("Paths from")
                Text(artistName, style = BentoType.tileName(), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(6.dp))
        BentoTabs(
            labels = PathKind.entries.map { it.label },
            selectedIndex = PathKind.entries.indexOf(kind).coerceAtLeast(0),
            onSelect = { onKind(PathKind.entries[it]) },
            modifier = Modifier.padding(horizontal = Side),
        )
        Text(
            kindDescription(kind, artistName, (state as? Load.Ready)?.value?.startType.orEmpty()),
            style = BentoType.small(),
            color = p.textMuted,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Side, vertical = 8.dp),
        )
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (state) {
                Load.Loading -> Column(
                    Modifier.fillMaxSize().padding(horizontal = Side).semantics { contentDescription = "Loading paths" },
                    verticalArrangement = Arrangement.spacedBy(Gap),
                ) {
                    repeat(3) { BentoSkeleton(Modifier.fillMaxWidth().height(92.dp), small = true) }
                }
                is Load.Failed -> Column(
                    Modifier.fillMaxSize().padding(horizontal = Side),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoBanner("Paths unavailable", state.failure.message, onRetry = onRetry)
                    if ((state.failure as? app.discair.net.ConnectionFailure.Server)?.code == 404) {
                        RequestUpdateButton("artist", mbid, byMbid = true, onDone = onRetry)
                    }
                }
                is Load.Ready -> when (val view = state.value.view()) {
                    is PathsView.Stops -> {
                        val stops = view.response.stops
                        val notes = state.value.truncated?.let {
                            listOf("The server stopped this path early (${truncatedWords(it)}), so later steps may be missing.")
                        }.orEmpty()
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            state = list,
                            contentPadding = PaddingValues(start = Side, end = Side, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(Gap),
                        ) {
                            itemsIndexed(stops, key = { idx, s -> "bento-artist-step:$idx:${s.name}" }) { idx, stop ->
                                BentoArtistStepCard(
                                    index = idx,
                                    total = stops.size,
                                    kind = kind,
                                    stop = stop,
                                    startName = artistName,
                                    open = step == idx,
                                    go = go,
                                    onOpen = { onStep(idx) },
                                    onClose = { onStep(-1) },
                                    onNext = if (idx + 1 < stops.size) ({ onStep(idx + 1) }) else null,
                                )
                            }
                            notes.forEach { note ->
                                item {
                                    Text(note, style = BentoType.small(), color = p.textMuted, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                    PathsView.Empty -> BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth(), small = true) {
                        Text(
                            "No ${kind.label.lowercase()} path from $artistName yet. A path appears only when every step has evidence.",
                            style = BentoType.body(), color = p.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BentoArtistStepCard(
    index: Int,
    total: Int,
    kind: PathKind,
    stop: PathStop,
    startName: String,
    open: Boolean,
    go: (PathLink) -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onNext: (() -> Unit)?,
) {
    val t = bentoTones()
    var cover by remember { mutableIntStateOf(0) }
    val tone = BentoTone.rotate(index)

    if (!open) {
        val h = stop.heading(kind)
        val detail = rowDetail(kind, stop)
        BentoTile(
            Modifier.fillMaxWidth()
                .combinedClickable(
                    onClickLabel = "Open step",
                    role = Role.Button,
                    onClick = onOpen,
                    onLongClickLabel = "Show next cover",
                    onLongClick = { cover++ },
                )
                .semantics { stateDescription = "Collapsed" },
            tone = tone,
            small = true,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(14.dp))) { PathThumb(kind, stop, 52, cover) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(h.title, style = BentoType.tileName(), color = t.onTone, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (detail.isNotBlank()) {
                        Text(detail, style = BentoType.small(), color = t.onToneMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(BentoIcons.Down, null, tint = t.onToneMuted, modifier = Modifier.size(20.dp))
            }
        }
        return
    }

    val pane = remember(stop, kind) { stop.pane(kind, startName) }
    val library = if (stop.owned) "In your library" else "Not in your library"
    val heading = stop.heading(kind)
    val (title, lines) = when (pane) {
        is PathPane.Song -> pane.title to listOfNotNull(pane.album?.let { "on $it" }, pane.artist?.let { "by $it" }, library)
        is PathPane.Leader -> pane.name to listOf(pane.typeLabel, library)
        is PathPane.LabelAlbum -> pane.album to listOfNotNull(pane.artist, library)
        is PathPane.Recording -> heading.title to listOfNotNull(heading.detail)
        is PathPane.Place -> pane.name to listOf(pane.typeLabel, library)
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Gap)) {
        BentoTile(
            Modifier.fillMaxWidth().semantics { stateDescription = "Expanded" },
            tone = BentoTone.Deep,
            onClick = onClose,
            clickLabel = "Close step",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(16.dp))) { PathThumb(kind, stop, 68, cover) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    BentoEyebrow("Step ${index + 1} of $total", tone = BentoTone.Deep)
                    Text(title, style = BentoType.tileTitle(), color = t.onDeep, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    lines.filter { it.isNotBlank() }.forEach { line ->
                        Text(line, style = BentoType.small(), color = t.onDeep.copy(alpha = 0.78f))
                    }
                }
                Icon(BentoIcons.Up, null, tint = t.onDeep, modifier = Modifier.size(20.dp))
            }
        }

        BentoTile(Modifier.fillMaxWidth(), tone = tone, small = true) {
            PaneBody(pane, stop, startName, go)
        }

        if (onNext != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BentoButton("Next step", onNext, kind = BentoButtonKind.Outline, small = true, icon = BentoIcons.Chevron)
            }
        }
    }
}

package app.discair.ui.album

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
import app.discair.model.AlbumAnswer
import app.discair.model.AlbumStep
import app.discair.model.AlbumStepReason
import app.discair.model.AlbumTab
import app.discair.model.PathAlbum
import app.discair.model.PathLink
import app.discair.model.albumPathsEmpty
import app.discair.model.albumTabDescription
import app.discair.model.isArtist
import app.discair.model.reasonEyebrow
import app.discair.ui.common.ArtistPhoto
import app.discair.ui.common.BentoBanner
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPairRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSkeleton
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Load
import app.discair.ui.common.PathAlbumCover
import app.discair.ui.common.PathRowCover
import app.discair.ui.common.RequestUpdateButton
import app.discair.ui.common.SameRoomPhotos
import app.discair.ui.common.bentoTones
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Album Paths sheet body (BENTO_THEME.md step B3, paths frames 1 & 2).
 * Keeps the exact sheet state, lazy fetching, and Back behaviour owned by [AlbumPathsSheet].
 * Steps render as rotating pastel tone tiles; an opened step renders the album in a `deep` tile, one tone tile per reason,
 * and tiles to open the person and the album.
 */
@Composable
internal fun BentoAlbumPathList(
    albumName: String,
    artistName: String,
    albumId: String,
    tab: AlbumTab,
    tabs: List<AlbumTab>,
    state: Load<AlbumAnswer>,
    seeds: Int,
    step: Int,
    list: LazyListState,
    go: (PathLink) -> Unit,
    onTab: (AlbumTab) -> Unit,
    onStep: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Side, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PathAlbumCover(artistName, albumName, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                BentoEyebrow("Paths from")
                Text(albumName, style = BentoType.tileName(), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(6.dp))
        BentoTabs(
            labels = tabs.map { it.label },
            selectedIndex = tabs.indexOf(tab).coerceAtLeast(0),
            onSelect = { onTab(tabs[it]) },
            modifier = Modifier.padding(horizontal = Side),
        )
        Text(
            albumTabDescription(tab, albumName, seeds),
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
                        RequestUpdateButton("album", albumId, byMbid = false, onDone = onRetry)
                    }
                }
                is Load.Ready -> {
                    val answer = state.value
                    if (answer.steps.isEmpty()) {
                        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth(), small = true) {
                            Text(answer.empty ?: albumPathsEmpty(tab, albumName), style = BentoType.body(), color = p.textMuted)
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            state = list,
                            contentPadding = PaddingValues(start = Side, end = Side, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(Gap),
                        ) {
                            itemsIndexed(answer.steps, key = { idx, s -> "bento-album-step:$idx:${s.album}" }) { idx, item ->
                                BentoAlbumStepCard(
                                    index = idx,
                                    tab = tab,
                                    step = item,
                                    startName = albumName,
                                    startArtist = artistName,
                                    open = step == idx,
                                    go = go,
                                    onOpen = { onStep(idx) },
                                    onClose = { onStep(-1) },
                                )
                            }
                            answer.notes.forEach { note ->
                                item {
                                    Text(note, style = BentoType.small(), color = p.textMuted, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BentoAlbumStepCard(
    index: Int,
    tab: AlbumTab,
    step: AlbumStep,
    startName: String,
    startArtist: String,
    open: Boolean,
    go: (PathLink) -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
) {
    val t = bentoTones()
    var cover by remember { mutableIntStateOf(0) }
    val tone = BentoTone.rotate(index)

    if (!open) {
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
                PathRowCover(step.artist, step.album, cover, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(step.album, style = BentoType.tileName(), color = t.onTone, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = listOfNotNull(step.artist.takeIf { it.isNotBlank() }, step.year?.toString()).joinToString(" · ")
                    if (meta.isNotBlank()) Text(meta, style = BentoType.small(), color = t.onToneMuted, maxLines = 1)
                    if (step.detail.isNotBlank()) {
                        Text(step.detail, style = BentoType.body(), color = t.onTone, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(BentoIcons.Down, null, tint = t.onToneMuted, modifier = Modifier.size(20.dp))
            }
        }
        return
    }

    // Opened step: album in a `deep` tile, one tone tile per reason, then person & album tiles
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Gap)) {
        BentoTile(
            Modifier.fillMaxWidth().semantics { stateDescription = "Expanded" },
            tone = BentoTone.Deep,
            onClick = onClose,
            clickLabel = "Close step",
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PathRowCover(step.artist, step.album, cover, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BentoEyebrow(if (step.owned) "In your library" else "Not in your library", tone = BentoTone.Deep)
                    Text(step.album, style = BentoType.tileTitle(), color = t.onDeep, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    val meta = listOfNotNull(step.artist.takeIf { it.isNotBlank() }, step.year?.toString()).joinToString(" · ")
                    if (meta.isNotBlank()) Text(meta, style = BentoType.small(), color = t.onDeep.copy(alpha = 0.78f))
                }
                Icon(BentoIcons.Up, null, tint = t.onDeep, modifier = Modifier.size(20.dp))
            }
        }

        step.play?.let { play ->
            BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Sun, small = true) {
                BentoEyebrow("Your listening", tone = BentoTone.Sun)
                Spacer(Modifier.height(4.dp))
                Text(play, style = BentoType.tileName(), color = t.onTone)
            }
        }

        if (tab == AlbumTab.SameRoom) {
            SameRoomPhotos(startArtist, startName, step.artist, step.album)
        }

        val reasons = step.reasons.take(3)
        reasons.forEachIndexed { rIdx, reason ->
            BentoAlbumReasonTile(reason, BentoTone.rotate(index + rIdx + 1))
        }

        val person = step.reasons.firstNotNullOfOrNull { it.via?.takeIf { v -> v.isArtist } }
        val albumLink = step.artist.takeIf { it.isNotBlank() }?.let { by -> PathLink(true, step.album, by) }
        BentoPairRow(
            left = {
                if (person != null) {
                    BentoTile(
                        Modifier.weight(1f),
                        tone = BentoTone.Surface,
                        small = true,
                        onClick = { go(PathLink(false, person.name, null, person.mbid)) },
                        clickLabel = "Open ${person.name}",
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ArtistPhoto(person.name, null, Modifier.size(40.dp), circle = true)
                            Column(Modifier.weight(1f)) {
                                BentoEyebrow("Person")
                                Text(person.name, style = BentoType.rowTitle(), color = LocalPalette.current.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            right = {
                if (albumLink != null) {
                    BentoTile(
                        Modifier.weight(1f),
                        tone = BentoTone.Surface,
                        small = true,
                        onClick = { go(albumLink) },
                        clickLabel = "Open ${step.album}",
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PathAlbumCover(step.artist, step.album, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                            Column(Modifier.weight(1f)) {
                                BentoEyebrow("Open album")
                                Text(step.album, style = BentoType.rowTitle(), color = LocalPalette.current.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun BentoAlbumReasonTile(reason: AlbumStepReason, tone: BentoTone) {
    val t = bentoTones()
    BentoTile(Modifier.fillMaxWidth(), tone = tone, small = true) {
        BentoEyebrow(reasonEyebrow(reason), tone = tone)
        reason.via?.name?.let { who ->
            Spacer(Modifier.height(4.dp))
            Text(who, style = BentoType.tileName(), color = t.onTone)
        }
        Spacer(Modifier.height(4.dp))
        Text(reason.text, style = BentoType.body(), color = t.onTone)
    }
}

package app.discair.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.discair.model.SearchResult
import app.discair.player.QueueItem
import app.discair.ui.LocalBottomClearance
import app.discair.ui.Opener
import app.discair.ui.common.BentoGetButton
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSearchField
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Cover
import app.discair.ui.common.IndexListsRules
import app.discair.ui.common.LocalAcquisitions
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.pageMetadata
import app.discair.ui.common.routeKey
import app.discair.ui.player.LocalPlayActions
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Search screen (BENTO_THEME.md step B4, library-search frame 3).
 * Pill search field with inset focus ring, status line, then grouped white tiles for Artists, In your library, and
 * Not in your library (with morphing BentoGetButton).
 */
@Composable
internal fun BentoSearch(
    text: String,
    onText: (String) -> Unit,
    state: SearchState,
    status: String?,
    focus: FocusRequester,
    onSearchKey: () -> Unit,
    open: Opener,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.ink)) {
        BentoSearchField(
            value = text,
            onValueChange = { onText(it.take(200)) },
            placeholder = "Artists, albums, songs, genres",
            onClear = { onText("") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchKey() }),
            modifier = Modifier
                .padding(horizontal = Side)
                .padding(bottom = Gap)
                .focusRequester(focus)
                .semantics { contentDescription = "Search artists, albums, songs, genres" },
        )
        if (text.isBlank()) {
            BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth(), tone = BentoTone.Sky, small = true) {
                Text("In your library first, then everything else.", style = BentoType.body(), color = app.discair.ui.common.bentoTones().onTone)
            }
        }
        status?.let {
            Text(
                it,
                style = BentoType.small(),
                color = if (state.error != null) p.chartRedText else p.textMuted,
                modifier = Modifier
                    .padding(horizontal = Side + 4.dp, vertical = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        val groups = IndexListsRules.searchGroups(state.results)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Side, end = Side, top = 4.dp, bottom = 24.dp + LocalBottomClearance.current),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            groups.forEach { (group, rows) ->
                item(key = "bento-search-group:${group.name}") {
                    BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Surface) {
                        BentoTileHeader(group.title(rows.size))
                        Spacer(Modifier.height(6.dp))
                        rows.forEachIndexed { idx, r ->
                            BentoSearchResultRow(r, open, showDivider = idx < rows.lastIndex)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoSearchResultRow(r: SearchResult, open: Opener, showDivider: Boolean) {
    val p = LocalPalette.current
    val repo = LocalRepository.current
    val play = LocalPlayActions.current
    val hub = LocalAcquisitions.current
    val ownedId = r.id.takeIf { r.owned }
    val artist = r.artist
    val album = r.album
    val ownedSong = if (r.type == "song" && r.owned && !r.albumId.isNullOrBlank()) {
        QueueItem(
            songId = r.id,
            title = r.name,
            artist = r.artist.orEmpty(),
            album = r.album.orEmpty(),
            albumId = r.albumId,
            coverId = r.albumId,
            coverUrl = repo.absolute("/api/owned/artwork/${r.albumId}"),
            durationSec = r.duration ?: 0,
            year = r.year,
            bitRateKbps = r.bitRate,
            suffix = r.suffix.orEmpty(),
            bitDepth = r.bitDepth,
            samplingRateHz = r.samplingRate,
        )
    } else null

    val go: (() -> Unit)? = when {
        ownedSong != null -> ({ play.playSong(ownedSong) })
        r.type == "artist" -> ({ open.artist(r.name, r.key, ownedId, r.mbid) })
        r.type == "album" && artist != null -> ({ open.album(r.key, artist, r.name, ownedId, r.year) })
        r.type == "song" && artist != null && album != null -> ({ open.album(routeKey(artist, album), artist, album, null, null) })
        r.type == "genre" -> ({ open.genre(r.tag ?: r.key, r.name) })
        else -> null
    }

    BentoRow(
        title = r.name,
        subtitle = IndexListsRules.searchMeta(r).ifBlank { null },
        showDivider = showDivider,
        onClick = go,
        lead = when {
            ownedSong != null -> ({
                BentoIconButton(BentoIcons.Play, "Play ${r.name}", { play.playSong(ownedSong) }, solid = true, small = true)
            })
            r.type == "genre" -> null
            r.type == "artist" -> ({ Cover(r.artworkUrl?.let(repo::absolute), Modifier.size(46.dp), circle = true) })
            else -> ({
                Cover(
                    r.artworkUrl?.let(repo::absolute),
                    Modifier.size(46.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)),
                    rect = true,
                )
            })
        },
        trail = {
            when {
                ownedSong != null -> BentoIconButton(
                    BentoIcons.Queue,
                    "Add ${r.name} to queue",
                    { play.queueSong(ownedSong, next = false) },
                    small = true,
                )
                r.type == "album" && !r.owned && artist != null -> BentoGetButton(
                    state = hub.getState(artist, r.name),
                    onGet = { hub.getAlbum(artist, r.name, pageMetadata(artist, r.name, r.year)) },
                    small = true,
                )
                r.type == "album" && r.owned -> BentoTag("In library", tone = BentoTone.Leaf)
                r.type == "artist" || r.type == "genre" -> Icon(BentoIcons.Chevron, null, tint = p.textMuted, modifier = Modifier.size(18.dp))
                r.type != "song" && r.owned -> BentoTag("In library", tone = BentoTone.Leaf)
                else -> Unit
            }
        },
    )
}

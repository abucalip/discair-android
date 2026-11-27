package app.discair.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.data.AlbumSort
import app.discair.data.ArtistSort
import app.discair.ui.LocalBottomClearance
import app.discair.ui.Opener
import app.discair.ui.album.BentoActionSheet
import app.discair.ui.common.BentoBanner
import app.discair.ui.common.BentoChip
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPairRow
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSearchField
import app.discair.ui.common.BentoSkeleton
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Cover
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.plural
import app.discair.ui.common.sharedCover
import app.discair.ui.player.LocalPlayActions
import app.discair.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Library screen (BENTO_THEME.md step B4, library-search frames 1 & 2).
 * Rounded search field, Albums / Artists view tiles with counts, sort chips (with direction flip), 2-column album cover
 * tiles, and round-photo artist rows with a Bento action sheet.
 */
@Composable
internal fun BentoLibrary(
    state: LibraryState,
    model: LibraryViewModel,
    open: Opener,
    list: LazyListState,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val repo = LocalRepository.current

    val totals = remember { mutableStateMapOf<LibraryTab, Int>() }
    LaunchedEffect(state.tab, state.loading, state.total, state.failure, state.query) {
        if (!state.loading && state.failure == null && state.query.isBlank()) {
            state.total?.let { totals[state.tab] = it }
        }
    }
    LaunchedEffect(Unit) {
        val other = if (state.tab == LibraryTab.Artists) LibraryTab.Albums else LibraryTab.Artists
        if (totals[other] == null) {
            try {
                val total = withContext(Dispatchers.IO) {
                    if (other == LibraryTab.Artists) repo.artists("", ArtistSort.Name, null, null).total
                    else repo.albums("", AlbumSort.Name, null, null).total
                }
                if (total != null) totals[other] = total
            } catch (_: Exception) {
                // Keep count hidden if request fails.
            }
        }
    }

    Column(Modifier.fillMaxSize().background(p.ink)) {
        // Search field
        BentoSearchField(
            value = state.query,
            onValueChange = { model.search(it.take(120)) },
            placeholder = if (state.tab == LibraryTab.Albums) "Filter albums" else "Filter artists",
            onClear = { model.search("") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.padding(horizontal = Side).padding(bottom = Gap),
        )

        // Two view tiles: Artists (deep when active, warm when idle) and Albums (deep when active, sun when idle)
        BentoPairRow(
            Modifier.padding(horizontal = Side).padding(bottom = Gap),
            left = {
                val active = state.tab == LibraryTab.Artists
                val tone = if (active) BentoTone.Deep else BentoTone.Warm
                BentoTile(
                    Modifier.weight(1f),
                    tone = tone,
                    small = true,
                    onClick = { model.selectTab(LibraryTab.Artists) },
                    clickLabel = "Show artists",
                ) {
                    Text("Artists", style = BentoType.tileName(), color = tone.content(p, t), maxLines = 1)
                    totals[LibraryTab.Artists]?.let { cnt ->
                        Text("$cnt", style = BentoType.small(), color = tone.muted(p, t))
                    }
                }
            },
            right = {
                val active = state.tab == LibraryTab.Albums
                val tone = if (active) BentoTone.Deep else BentoTone.Sun
                BentoTile(
                    Modifier.weight(1f),
                    tone = tone,
                    small = true,
                    onClick = { model.selectTab(LibraryTab.Albums) },
                    clickLabel = "Show albums",
                ) {
                    Text("Albums", style = BentoType.tileName(), color = tone.content(p, t), maxLines = 1)
                    totals[LibraryTab.Albums]?.let { cnt ->
                        Text("$cnt", style = BentoType.small(), color = tone.muted(p, t))
                    }
                }
            },
        )

        // Sort chips + direction flip chip
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Side).padding(bottom = Gap),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.tab == LibraryTab.Albums) {
                AlbumSort.entries.forEach { sort ->
                    BentoChip(
                        label = sort.label,
                        selected = state.albumSort == sort,
                        onClick = { if (state.albumSort == sort) model.flipDirection() else model.sortAlbums(sort) },
                        small = true,
                    )
                }
            } else {
                ArtistSort.entries.forEach { sort ->
                    BentoChip(
                        label = sort.label,
                        selected = state.artistSort == sort,
                        onClick = { if (state.artistSort == sort) model.flipDirection() else model.sortArtists(sort) },
                        small = true,
                    )
                }
            }
            BentoChip(
                label = directionLabel(state),
                selected = false,
                onClick = model::flipDirection,
                small = true,
            )
        }

        // Content area
        val count = if (state.tab == LibraryTab.Artists) state.artists.size else state.albums.size
        Box(Modifier.weight(1f)) {
            when {
                state.failure != null && count == 0 -> Box(Modifier.padding(horizontal = Side)) {
                    BentoBanner("Library unavailable", state.failure.message, onRetry = model::retry)
                }
                state.loading && count == 0 -> Column(
                    Modifier.fillMaxSize().padding(horizontal = Side),
                    verticalArrangement = Arrangement.spacedBy(Gap),
                ) {
                    repeat(3) { BentoSkeleton(Modifier.fillMaxWidth().height(92.dp)) }
                }
                state.indexState == "building" && count == 0 -> BentoLibraryNote("Your library is still being indexed. Try again in a moment.")
                state.indexState == "unavailable" -> BentoLibraryNote("The library index is unavailable.")
                !state.loading && count == 0 -> BentoLibraryNote(
                    if (state.query.isBlank()) "Your library is empty." else "Nothing in your library matches “${state.query}”.",
                )
                state.tab == LibraryTab.Albums -> BentoAlbumGrid(state, model, open)
                else -> BentoArtistRows(state, model, open, list)
            }
        }
    }
}

@Composable
private fun BentoLibraryNote(text: String) {
    BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth()) {
        Text(text, style = BentoType.body(), color = LocalPalette.current.textMuted)
    }
}

@Composable
private fun BentoAlbumGrid(state: LibraryState, model: LibraryViewModel, open: Opener) {
    val p = LocalPalette.current
    val t = bentoTones()
    val repo = LocalRepository.current
    val grid = rememberLazyGridState()
    val rotation = remember { listOf(BentoTone.Sun, BentoTone.Bloom, BentoTone.Warm, BentoTone.Leaf, BentoTone.Sky) }
    LaunchedEffect(grid, state.nextCursor) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= grid.layoutInfo.totalItemsCount - 6) model.loadMore() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Side, end = Side, bottom = 24.dp + LocalBottomClearance.current),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        items(state.albums.size, key = { state.albums[it].id }) { i ->
            val album = state.albums[i]
            val tone = rotation[i % rotation.size]
            BentoTile(
                Modifier.animateItem().bentoTileEnter(i, "bento-lib-albums").semantics(mergeDescendants = true) {},
                tone = tone,
                small = true,
                padding = 10.dp,
                onClick = { open.album(album.key, album.artist, album.name, album.id, album.year) },
                clickLabel = "Open ${album.name}",
            ) {
                Cover(
                    album.artworkUrl?.let { repo.absolute(it) },
                    Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp))
                        .sharedCover(album.key),
                    rect = true,
                )
                Spacer(Modifier.height(8.dp))
                Text(album.name, style = BentoType.rowTitle(), color = tone.content(p, t), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(album.artist.ifBlank { null }, album.year?.toString()).joinToString(" · "),
                    style = BentoType.small(), color = tone.muted(p, t), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BentoArtistRows(state: LibraryState, model: LibraryViewModel, open: Opener, list: LazyListState) {
    val p = LocalPalette.current
    val t = bentoTones()
    val repo = LocalRepository.current
    val grid = rememberLazyGridState()
    val rotation = remember { listOf(BentoTone.Warm, BentoTone.Bloom, BentoTone.Leaf, BentoTone.Sun, BentoTone.Sky) }

    LaunchedEffect(grid, state.nextCursor) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= grid.layoutInfo.totalItemsCount - 9) model.loadMore() }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = grid,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Side, end = Side, bottom = 24.dp + LocalBottomClearance.current),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.artists.size, key = { state.artists[it].id }) { i ->
            val artist = state.artists[i]
            val artUrl = artist.artworkUrl?.let { repo.absolute(it) }
            val tone = rotation[i % rotation.size]
            val meta = artist.albumCount.takeIf { it > 0 }?.let { plural(it, "album") } ?: ""
            BentoTile(
                Modifier.animateItem().bentoTileEnter(i, "bento-lib-artists").semantics(mergeDescendants = true) {},
                tone = tone,
                small = true,
                padding = 8.dp,
                onClick = { open.artist(artist.name, artist.key, artist.id, null) },
                clickLabel = "Open ${artist.name}",
            ) {
                Cover(
                    artUrl,
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)),
                    rect = true,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    artist.name,
                    style = BentoType.small().copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = tone.content(p, t),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (meta.isNotEmpty()) {
                    Text(meta, style = BentoType.tileLabel(), color = tone.muted(p, t), maxLines = 1)
                }
            }
        }
    }
}

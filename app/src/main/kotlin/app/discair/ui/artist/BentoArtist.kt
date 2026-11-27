package app.discair.ui.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.model.TopSong
import app.discair.ui.LocalBottomClearance
import app.discair.ui.Opener
import app.discair.ui.album.BentoActionSheet
import app.discair.ui.common.AlbumCover
import app.discair.ui.common.ArtistPhoto
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoChip
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPageBar
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.GetSongButton
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.plural
import app.discair.ui.player.LocalPlayActions
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

private class BentoArtistAlbum(
    val name: String,
    val year: Int?,
    val key: String,
    val id: String?,
    val artworkUrl: String?,
    val owned: Boolean,
)

/**
 * Bento's Artist page (Layout.Bento). Spec: BENTO_THEME.md step B3 (album-artist-genre, frame 3).
 * Photo tile (20dp radius), name, genre chips, Play / Shuffle / Paths pills, top songs tile, owned & missing albums with
 * "Get" tags, biography tile, photo strip, and similar artists tile.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BentoArtistContent(
    d: CoverStackArtistData,
    open: Opener,
    list: LazyListState,
    showAllSongs: Boolean,
    onAllSongs: () -> Unit,
    showAllMissing: Boolean,
    onAllMissing: () -> Unit,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val play = LocalPlayActions.current
    val artistId = d.artistId
    val info = d.sections.artist.artist
    val pageKey = "bento-artist:${d.route.key}"
    var showMenu by remember(d.route) { mutableStateOf(false) }
    var songMenu by remember(d.route) { mutableStateOf<Pair<TopSong, String>?>(null) }

    val genres = d.sections.tags.items.map { it.name }.ifEmpty { listOfNotNull(info?.genre) }.filter { it.isNotBlank() }.take(3)
    val missing = d.sections.missingAlbums.items
    val allAlbums = remember(d.owned, missing, showAllMissing) {
        val own = d.owned.map { BentoArtistAlbum(it.name, it.year, it.key, it.id, it.artworkUrl, true) }
            .sortedWith(compareBy(nullsLast()) { it.year })
        val miss = (if (showAllMissing) missing else missing.take(8))
            .map { BentoArtistAlbum(it.name, it.year, it.key, null, null, false) }
            .sortedWith(compareBy(nullsLast()) { it.year })
        own + miss
    }

    if (showMenu && artistId != null) {
        BentoActionSheet(
            eyebrow = "Artist",
            title = d.name,
            cover = { ArtistPhoto(d.name, d.owned.firstOrNull()?.artworkUrl, Modifier.size(54.dp), circle = true) },
            items = listOf(
                Triple("Play next", BentoIcons.Play) { play.queueArtist(artistId, next = true) },
                Triple("Add to queue", BentoIcons.Queue) { play.queueArtist(artistId, next = false) },
            ),
            onDismiss = { showMenu = false },
        )
    }

    songMenu?.let { (song, albumId) ->
        BentoActionSheet(
            eyebrow = song.album ?: d.name,
            title = song.name,
            cover = {
                val ownedAlbum = song.album?.let { a -> d.owned.firstOrNull { it.name.equals(a, ignoreCase = true) } }
                AlbumCover(d.name, song.album ?: song.name, ownedAlbum?.artworkUrl, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), rect = true)
            },
            items = listOf(
                Triple("Play next", BentoIcons.Play) { play.queueTrack(albumId, song.name, null, next = true) },
                Triple("Add to queue", BentoIcons.Queue) { play.queueTrack(albumId, song.name, null, next = false) },
                Triple("Download to device", BentoIcons.Download) { play.downloadTrack(albumId, song.name, null) },
            ),
            onDismiss = { songMenu = null },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().background(p.ink),
        state = list,
        contentPadding = PaddingValues(bottom = 32.dp + LocalBottomClearance.current),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        item(key = "bar") {
            BentoPageBar(
                onBack = open.back,
                title = "Artist",
                action = if (artistId != null) ({
                    BentoIconButton(BentoIcons.More, "More for ${d.name}", { showMenu = true }, inHeader = true)
                }) else null,
            )
        }

        // Hero artist tile (warm tone, 20dp rounded photo, name, summary, actions)
        item(key = "hero") {
            BentoTile(
                Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(0, pageKey),
                tone = BentoTone.Warm,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtistPhoto(
                        d.name,
                        d.owned.firstOrNull()?.artworkUrl,
                        Modifier.size(116.dp).clip(RoundedCornerShape(BentoRules.ARTIST_TILE_RADIUS.dp)),
                        rect = true,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            d.name,
                            style = BentoType.heroTitle(),
                            color = t.onTone,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        val ownedCount = d.sections.albums.ownedCount ?: d.ownedAlbumCount ?: d.owned.size
                        val totalCount = d.sections.albums.albumCount
                        val summary = buildList {
                            if (d.inLibrary) {
                                add(if (totalCount != null && totalCount > ownedCount) "$ownedCount of $totalCount albums" else plural(ownedCount, "album"))
                            } else {
                                add("Not in your library")
                            }
                            if (missing.isNotEmpty()) add("${missing.size} to get")
                            info?.playCount?.takeIf { it > 0 }?.let { add(plural(it, "play")) }
                        }.joinToString(" · ")
                        Text(summary, style = BentoType.small(), color = t.onToneMuted, maxLines = 2)
                    }
                }
                if (genres.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        genres.forEach { g ->
                            BentoChip(g, selected = false, onClick = { open.genre(g.lowercase(), g) }, small = true, tone = BentoTone.Warm)
                        }
                    }
                }
                if (artistId != null || d.canPaths) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (artistId != null) {
                            BentoButton("Play", { play.playArtist(artistId, shuffle = false) }, icon = BentoIcons.Play, tone = BentoTone.Warm)
                            BentoButton("Shuffle", { play.playArtist(artistId, shuffle = true) }, kind = BentoButtonKind.Outline, icon = BentoIcons.Shuffle, tone = BentoTone.Warm)
                        }
                        if (d.canPaths) {
                            BentoButton("Paths", d.onPaths, kind = BentoButtonKind.Outline, icon = BentoIcons.Paths, tone = BentoTone.Warm)
                        }
                    }
                }
            }
        }

        // Top songs tile
        val allSongs = d.sections.topSongs.items
        if (allSongs.isNotEmpty()) {
            item(key = "top-songs") {
                val shown = if (showAllSongs) allSongs else allSongs.take(5)
                BentoTile(
                    Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(1, pageKey),
                    tone = BentoTone.Surface,
                ) {
                    BentoTileHeader(
                        "Top songs",
                        actionLabel = if (!showAllSongs && allSongs.size > 5) "See all" else null,
                        onAction = onAllSongs,
                    )
                    Spacer(Modifier.height(6.dp))
                    shown.forEachIndexed { i, song ->
                        val onAlbum = song.album?.takeIf { it.isNotBlank() }
                        val ownedAlbum = onAlbum?.let { a -> d.owned.firstOrNull { it.name.equals(a, ignoreCase = true) } }
                        val albumId = ownedAlbum?.id?.takeIf { it.isNotBlank() }
                        BentoRow(
                            title = song.name,
                            subtitle = onAlbum,
                            showDivider = i < shown.lastIndex,
                            onClick = if (song.owned && albumId != null) ({ play.playAlbum(albumId, song.name, null) }) else null,
                            lead = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("${i + 1}", style = BentoType.rank(), color = p.control)
                                    AlbumCover(
                                        d.name, onAlbum ?: song.name, ownedAlbum?.artworkUrl,
                                        Modifier.size(44.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)),
                                        rect = true,
                                    )
                                }
                            },
                            trail = {
                                if (!song.owned && onAlbum != null) {
                                    GetSongButton(d.name, onAlbum, song.name, null, null)
                                } else if (song.owned && albumId != null) {
                                    BentoIconButton(
                                        BentoIcons.More, "More ways to play ${song.name}",
                                        { songMenu = song to albumId }, small = true,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        // Albums tile (owned + missing with Get tag)
        if (allAlbums.isNotEmpty()) {
            item(key = "albums") {
                BentoTile(
                    Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(2, pageKey),
                    tone = BentoTone.Surface,
                ) {
                    BentoTileHeader(
                        "Albums",
                        actionLabel = if (!showAllMissing && missing.size > 8) "All (${d.owned.size + missing.size})" else null,
                        onAction = onAllMissing,
                    )
                    Spacer(Modifier.height(10.dp))
                    allAlbums.chunked(2).forEachIndexed { rIdx, pair ->
                        if (rIdx > 0) Spacer(Modifier.height(Gap))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            pair.forEach { a ->
                                val said = listOfNotNull(a.name, a.year?.toString(), if (a.owned) "in your library" else "not in your library").joinToString(", ")
                                Column(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(role = Role.Button) { open.album(a.key, d.name, a.name, a.id, a.year) }
                                        .semantics(mergeDescendants = true) { contentDescription = said },
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box {
                                        AlbumCover(
                                            d.name, a.name, a.artworkUrl,
                                            Modifier.fillMaxWidth().aspectRatio(1f)
                                                .clip(RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp))
                                                .alpha(if (a.owned) 1f else 0.62f),
                                            rect = true,
                                        )
                                        if (!a.owned) {
                                            BentoTag("Get", Modifier.align(Alignment.BottomStart).padding(6.dp), tone = BentoTone.Warm)
                                        }
                                    }
                                    Text(a.name, style = BentoType.rowTitle(), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(a.year?.toString() ?: "—", style = BentoType.small(), color = p.textMuted, maxLines = 1)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Biography tile
        val bio = d.sections.bio.bio?.text.orEmpty()
        if (bio.isNotBlank()) {
            item(key = "bio") {
                var expanded by remember(bio) { mutableStateOf(false) }
                BentoTile(
                    Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(3, pageKey),
                    tone = BentoTone.Sky,
                ) {
                    BentoTileHeader("About", tone = BentoTone.Sky)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        bio,
                        style = BentoType.body(),
                        color = t.onTone,
                        maxLines = if (expanded) Int.MAX_VALUE else 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!expanded) {
                        Spacer(Modifier.height(8.dp))
                        BentoButton("Read more", { expanded = true }, kind = BentoButtonKind.Outline, small = true, tone = BentoTone.Sky)
                    }
                }
            }
        }

        if (d.collage.isNotEmpty()) {
            item(key = "collage") {
                BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(4, pageKey)) {
                    ArtistPhotoStrip(d.collage, d.name, open)
                }
            }
        }

        // Similar artists tile
        if (d.similar.isNotEmpty()) {
            item(key = "similar") {
                BentoTile(
                    Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(5, pageKey),
                    tone = BentoTone.Leaf,
                ) {
                    BentoTileHeader("Similar artists", tone = BentoTone.Leaf)
                    Spacer(Modifier.height(6.dp))
                    d.similar.forEachIndexed { idx, a ->
                        BentoRow(
                            title = a.name,
                            subtitle = if (a.owned) "In your library" else "Not in your library",
                            tone = BentoTone.Leaf,
                            showDivider = idx < d.similar.lastIndex,
                            onClick = { open.artist(a.name, a.key, a.id, a.mbid) },
                            lead = { ArtistPhoto(a.name, a.artworkUrl, Modifier.size(44.dp), circle = true) },
                            trail = { Icon(BentoIcons.Chevron, null, tint = t.onToneMuted, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
        }
    }
}

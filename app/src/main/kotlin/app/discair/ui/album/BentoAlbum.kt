package app.discair.ui.album

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.model.FeedbackAction
import app.discair.ui.LocalBottomClearance
import app.discair.ui.Opener
import app.discair.ui.common.AlbumCover
import app.discair.ui.common.ArtistPhoto
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoGetButton
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPageBar
import app.discair.ui.common.BentoPairRow
import app.discair.ui.common.BentoPlayingBars
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.GalleryPagesRules
import app.discair.ui.common.GetSongButton
import app.discair.ui.common.IndexPagesRules
import app.discair.ui.common.Load
import app.discair.ui.common.LocalAcquisitions
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.ModalBottomSheet
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.pageMetadata
import app.discair.ui.common.plural
import app.discair.ui.common.rememberLoad
import app.discair.ui.common.sharedCover
import app.discair.ui.player.LocalPlayActions
import app.discair.ui.player.LocalPlayingNow
import app.discair.ui.theme.LocalPalette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Album page (Layout.Bento). Spec: BENTO_THEME.md step B3 (album-artist-genre, sheets).
 * Owned album: cover tile with edition picker & leaf quality tag, info tone tiles, Play / Shuffle / Paths / Save pills,
 * Songs · Info · Reviews · Booklet chips, track tile with playing bars & Bento action sheet. Missing album: cover tile with
 * warm "Not in your library" tag, Get album morph button, Save button, track list & similar albums/artists.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BentoAlbumContent(d: CoverStackAlbumData, open: Opener, list: LazyListState) {
    val p = LocalPalette.current
    val t = bentoTones()
    val play = LocalPlayActions.current
    val hub = LocalAcquisitions.current
    val albumId = d.albumId
    val pageKey = "bento-album:${d.route.key}"
    val serviceMenu = rememberServiceMenu(d.artist, d.title, d.info?.releaseGroupMbid)
    val albumMenu = buildList<Triple<String, ImageVector, () -> Unit>> {
        if (albumId != null) {
            add(Triple("Play next", BentoIcons.Play) { play.queueAlbum(albumId, next = true) })
            add(Triple("Add to queue", BentoIcons.Queue) { play.queueAlbum(albumId, next = false) })
        }
        if (albumId != null && d.pathsOn) {
            add(Triple("Paths from this album", BentoIcons.Paths, d.onPaths))
        }
        if (d.canPickEdition) {
            add(Triple("Choose cover", BentoIcons.Grid, d.onPickEdition))
        }
        serviceMenu.forEach { (label, action) -> add(Triple(label, BentoIcons.Info, action)) }
    }
    var showAlbumMenu by remember(d.route) { mutableStateOf(false) }
    var songMenuIndex by remember(d.route) { mutableStateOf<Int?>(null) }

    val tabs = GalleryPagesRules.albumTabs(d.tracks.isNotEmpty(), d.booklet != null)
    var chosen by remember(d.route) { mutableStateOf<String?>(null) }
    val tab = chosen?.let { tabs.indexOf(it) }?.takeIf { it >= 0 }
        ?: GalleryPagesRules.defaultTab(tabs, d.descriptionFinished, d.description?.paragraphs.isNullOrEmpty())
    val metadata = pageMetadata(d.artist, d.title, d.year, d.info?.releaseGroupMbid)
    val openArtist = { open.artist(d.artist, d.info?.artistKey?.ifBlank { null }, d.info?.artistId, null) }
    val genre = d.genre?.takeIf { it.isNotBlank() }
    val totalSecs = d.tracks.sumOf { it.durationSeconds ?: 0 }
    val playingNow = LocalPlayingNow.current
    val playingIdx = d.tracks.indices.firstOrNull { i ->
        val tr = d.tracks[i]
        tr.owned && playingNow?.matches(albumId, tr.title, tr.number, tr.disc ?: 1) == true
    }

    if (showAlbumMenu && albumMenu.isNotEmpty()) {
        BentoActionSheet(
            eyebrow = if (d.owned) "In your library" else "Not in your library",
            title = d.title,
            cover = {
                AlbumCover(d.artist, d.title, d.coverUrl, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), rect = true)
            },
            items = albumMenu,
            onDismiss = { showAlbumMenu = false },
        )
    }

    songMenuIndex?.let { idx ->
        val tr = d.tracks.getOrNull(idx)
        if (tr != null && albumId != null) {
            val items = buildList<Triple<String, ImageVector, () -> Unit>> {
                add(Triple("Play next", BentoIcons.Play) { play.queueTrack(albumId, tr.title, tr.number, next = true, disc = tr.disc) })
                add(Triple("Add to queue", BentoIcons.Queue) { play.queueTrack(albumId, tr.title, tr.number, next = false, disc = tr.disc) })
                if (d.pathsOn) add(Triple("Paths from this album", BentoIcons.Paths, d.onPaths))
                add(Triple("Download to device", BentoIcons.Download) { play.downloadTrack(albumId, tr.title, tr.number, disc = tr.disc) })
            }
            BentoActionSheet(
                eyebrow = "Track ${trackNumberShown(d.tracks, idx)} of ${d.tracks.size} · ${d.artist}",
                title = tr.title,
                cover = {
                    AlbumCover(d.artist, d.title, d.coverUrl, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), rect = true)
                },
                items = items,
                onDismiss = { songMenuIndex = null },
            )
        }
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
                title = if (d.owned) "Album" else "Not in library",
                action = if (albumMenu.isNotEmpty()) ({
                    BentoIconButton(BentoIcons.More, "More for ${d.title}", { showAlbumMenu = true }, inHeader = true)
                }) else null,
            )
        }

        // Hero cover + title + primary action tile
        item(key = "hero") {
            BentoTile(
                Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(0, pageKey),
                tone = BentoTone.Surface,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val coverSize = if (d.owned) 224.dp else 188.dp
                    Box(Modifier.size(coverSize)) {
                        FlipCover(d.backs, coverSize) {
                            Box(Modifier.size(coverSize)) {
                                AlbumCover(
                                    d.artist, d.title, d.coverUrl,
                                    Modifier.size(coverSize).clip(RoundedCornerShape(BentoRules.COVER_LARGE_RADIUS.dp)).sharedCover(d.route.key),
                                    rect = true,
                                )
                                if (d.canPickEdition) {
                                    EditionStack(d.editionCount, d.onPickEdition, Modifier.align(Alignment.BottomStart).padding(8.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!d.owned) {
                        BentoTag("Not in your library", tone = BentoTone.Warm)
                    } else {
                        d.info?.quality?.takeIf { it.isNotBlank() }?.let { q ->
                            BentoTag(q, tone = BentoTone.Leaf)
                        }
                        if (playingIdx != null) {
                            BentoTag("Playing track ${playingIdx + 1}", tone = BentoTone.Sun)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    d.title,
                    style = BentoType.heroTitle(),
                    color = p.text,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    d.artist,
                    style = BentoType.rowTitle(),
                    color = p.control,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 36.dp).clickable(role = Role.Button, onClick = openArtist).padding(vertical = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    if (albumId != null) {
                        BentoButton("Play", { play.playAlbum(albumId) }, icon = BentoIcons.Play)
                        BentoButton("Shuffle", { play.playAlbum(albumId, shuffle = true) }, kind = BentoButtonKind.Outline, icon = BentoIcons.Shuffle)
                        if (d.pathsOn) {
                            BentoButton("Paths", d.onPaths, kind = BentoButtonKind.Outline, icon = BentoIcons.Paths)
                        }
                    }
                    if (!d.owned || d.tracks.any { !it.owned }) {
                        BentoGetButton(
                            state = hub.getState(d.artist, d.title),
                            onGet = { hub.getAlbum(d.artist, d.title, metadata) },
                            small = d.owned,
                            idleLabel = "Get album",
                        )
                    }
                    if (!d.owned) {
                        BentoSaveButton(d.artist, d.title, metadata)
                    }
                }
            }
        }

        // Info pair tiles (Year / Length + Genre / Tracks)
        val hasMeta = d.year != null || totalSecs > 0 || genre != null || d.tracks.isNotEmpty()
        if (hasMeta) {
            item(key = "info-pair") {
                BentoPairRow(
                    Modifier.padding(horizontal = Side).bentoTileEnter(1, pageKey),
                    left = {
                        BentoTile(Modifier.weight(1f), tone = BentoTone.Sky, small = true) {
                            BentoTileLabel(if (d.year != null) "Released" else "Tracks", tone = BentoTone.Sky)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                d.year?.toString() ?: plural(d.tracks.size, "track"),
                                style = BentoType.tileTitle(),
                                color = t.onTone,
                                maxLines = 1,
                            )
                            val sub = listOfNotNull(
                                if (d.year != null && d.tracks.isNotEmpty()) plural(d.tracks.size, "track") else null,
                                if (totalSecs > 0) "${totalSecs / 60} min" else null,
                            ).joinToString(" · ")
                            if (sub.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Text(sub, style = BentoType.small(), color = t.onToneMuted, maxLines = 1)
                            }
                        }
                    },
                    right = {
                        BentoTile(
                            Modifier.weight(1f),
                            tone = BentoTone.Bloom,
                            small = true,
                            onClick = genre?.let { g -> { open.genre(g.lowercase(), g) } },
                            clickLabel = genre?.let { "Open $it genre" },
                        ) {
                            BentoTileLabel(if (genre != null) "Genre" else "Library", tone = BentoTone.Bloom)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                genre ?: if (d.owned) "In library" else "Not owned",
                                style = BentoType.tileTitle(),
                                color = t.onTone,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (genre != null) "Open genre" else if (d.owned) "Ready to play" else "Available to get",
                                style = BentoType.small(),
                                color = t.onToneMuted,
                                maxLines = 1,
                            )
                        }
                    },
                )
            }
        }

        // Tabs strip (Songs / Info / Reviews / Booklet)
        item(key = "tabs") {
            BentoTabs(
                labels = tabs,
                selectedIndex = tab,
                onSelect = { chosen = tabs[it] },
                modifier = Modifier.padding(horizontal = Side).bentoTileEnter(2, pageKey),
            )
        }

        // Active tab body
        when (tabs[tab]) {
            "Songs" -> {
                item(key = "tracks") {
                    BentoTile(
                        Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(3, pageKey),
                        tone = BentoTone.Surface,
                    ) {
                        BentoTileHeader("Tracks")
                        Spacer(Modifier.height(6.dp))
                        d.tracks.forEachIndexed { i, tr ->
                            if (startsDisc(d.tracks, i)) {
                                BentoEyebrow(
                                    "Disc ${tr.disc ?: 1}",
                                    Modifier.padding(top = if (i == 0) 4.dp else 12.dp, bottom = 4.dp),
                                )
                            }
                            val current = tr.owned && playingNow?.matches(albumId, tr.title, tr.number, tr.disc ?: 1) == true
                            BentoRow(
                                title = tr.title,
                                subtitle = tr.durationSeconds?.let { IndexPagesRules.clock(it) },
                                active = current,
                                showDivider = i < d.tracks.lastIndex,
                                onClick = if (tr.owned && albumId != null) ({
                                    play.playAlbum(albumId, tr.title, tr.number, startDisc = tr.disc)
                                }) else null,
                                lead = {
                                    Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                                        if (current) {
                                            BentoPlayingBars(playingNow?.isPlaying == true)
                                        } else {
                                            Text(
                                                "%02d".format(trackNumberShown(d.tracks, i)),
                                                style = BentoType.time(),
                                                color = p.textMuted,
                                            )
                                        }
                                    }
                                },
                                trail = {
                                    if (!tr.owned) {
                                        GetSongButton(d.artist, d.title, tr.title, tr.number, d.year)
                                    } else if (albumId != null) {
                                        BentoIconButton(
                                            icon = BentoIcons.More,
                                            description = "More ways to play ${tr.title}",
                                            onClick = { songMenuIndex = i },
                                            small = true,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
            "Info" -> {
                item(key = "info") {
                    BentoAlbumInfoTile(d, Modifier.padding(horizontal = Side).bentoTileEnter(3, pageKey))
                }
            }
            "Reviews" -> {
                item(key = "reviews") {
                    BentoAlbumReviewsTile(
                        d.artist, d.title, d.info?.releaseGroupMbid,
                        Modifier.padding(horizontal = Side).bentoTileEnter(3, pageKey),
                    )
                }
            }
            "Booklet" -> {
                d.booklet?.let { b ->
                    item(key = "booklet") {
                        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(3, pageKey)) {
                            BookletTab(b)
                        }
                    }
                }
            }
        }

        // Similar albums tile
        if (d.similarAlbums.isNotEmpty()) {
            item(key = "similar-albums") {
                BentoTile(
                    Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(4, pageKey),
                    tone = BentoTone.Surface,
                ) {
                    BentoTileHeader("Similar albums")
                    Spacer(Modifier.height(10.dp))
                    d.similarAlbums.take(4).chunked(2).forEachIndexed { rIdx, pair ->
                        if (rIdx > 0) Spacer(Modifier.height(Gap))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                            pair.forEach { album ->
                                Column(
                                    Modifier.weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable(role = Role.Button) { open.album(album.key, album.artist, album.name, null, album.year) },
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    AlbumCover(
                                        album.artist, album.name, null,
                                        Modifier.fillMaxWidth().aspectRatio(1f)
                                            .clip(RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp))
                                            .then(if (album.key != d.route.key) Modifier.sharedCover(album.key) else Modifier),
                                        rect = true,
                                    )
                                    Text(album.name, style = BentoType.rowTitle(), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        listOfNotNull(album.artist.ifBlank { null }, album.year?.toString()).joinToString(" · "),
                                        style = BentoType.small(), color = p.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Similar artists tile
        if (d.similarArtists.isNotEmpty()) {
            item(key = "similar-artists") {
                BentoTile(
                    Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(5, pageKey),
                    tone = BentoTone.Warm,
                ) {
                    BentoTileHeader("Similar artists", tone = BentoTone.Warm)
                    Spacer(Modifier.height(6.dp))
                    d.similarArtists.take(4).forEachIndexed { idx, a ->
                        BentoRow(
                            title = a.name,
                            subtitle = if (a.owned) "In your library" else "Not in your library",
                            tone = BentoTone.Warm,
                            showDivider = idx < minOf(4, d.similarArtists.size) - 1,
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

@Composable
private fun BentoSaveButton(artist: String, album: String, metadata: kotlinx.serialization.json.JsonObject) {
    val repo = LocalRepository.current
    val scope = rememberCoroutineScope()
    var state by remember(artist, album) { mutableStateOf(0) } // 0 idle, 1 pending, 2 saved, 3 failed
    BentoButton(
        label = when (state) {
            1 -> "Saving…"
            2 -> "Saved"
            3 -> "Try again"
            else -> "Save"
        },
        onClick = {
            state = 1
            scope.launch {
                state = try {
                    withContext(Dispatchers.IO) { repo.feedback(FeedbackAction.Save, artist, album, metadata) }
                    2
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    3
                }
            }
        },
        kind = BentoButtonKind.Outline,
        icon = if (state == 2) BentoIcons.Check else BentoIcons.Bookmark,
        enabled = state == 0 || state == 3,
    )
}

@Composable
private fun BentoAlbumInfoTile(d: CoverStackAlbumData, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val uri = LocalUriHandler.current
    val paragraphs = d.description?.paragraphs.orEmpty()
    var all by remember(d.route) { mutableStateOf(false) }
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader("About this album")
        Spacer(Modifier.height(8.dp))
        if (paragraphs.isEmpty()) {
            Text(
                if (d.descriptionFinished) "No description found for this album." else "Loading description…",
                style = BentoType.body(), color = p.textMuted,
            )
            return@BentoTile
        }
        Text(
            paragraphs.joinToString("\n\n"),
            style = BentoType.body(),
            color = p.text,
            maxLines = if (all) Int.MAX_VALUE else 8,
            overflow = TextOverflow.Ellipsis,
        )
        if (!all) {
            Spacer(Modifier.height(8.dp))
            BentoButton("Read more", { all = true }, kind = BentoButtonKind.Outline, small = true)
        }
        val credit = listOfNotNull(d.description?.source ?: "Apple Music", d.description?.license).joinToString(" · ")
        val link = d.description?.url
        Spacer(Modifier.height(8.dp))
        Text(
            credit,
            style = BentoType.small(),
            color = if (link != null) p.control else p.textMuted,
            modifier = if (link != null) Modifier.clickable(role = Role.Button) { uri.openUri(link) } else Modifier,
        )
    }
}

@Composable
private fun BentoAlbumReviewsTile(artist: String, album: String, mbid: String?, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val repo = LocalRepository.current
    val uri = LocalUriHandler.current
    val loaded = rememberLoad(artist, album, mbid) { repo.albumReviews(artist, album, mbid) }
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader("Reviews")
        Spacer(Modifier.height(10.dp))
        when (val state = loaded.state) {
            Load.Loading -> Text("Loading reviews…", style = BentoType.body(), color = p.textMuted)
            is Load.Failed -> {
                Text("Reviews could not be loaded.", style = BentoType.body(), color = p.textMuted)
                Spacer(Modifier.height(8.dp))
                BentoButton("Try again", loaded.reload, kind = BentoButtonKind.Outline, small = true)
            }
            is Load.Ready -> {
                val data = state.value
                CritiqueBrainzReviewBlock(data.critiquebrainz, hasPressReviews = data.reviews.isNotEmpty())
                data.reviews.forEachIndexed { idx, review ->
                    if (idx > 0) Spacer(Modifier.height(12.dp))
                    BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.rotate(idx), small = true) {
                        BentoEyebrow(review.source, tone = BentoTone.rotate(idx))
                        Spacer(Modifier.height(4.dp))
                        Text("“${review.quote}”", style = BentoType.body(), color = bentoTones().onTone)
                        Spacer(Modifier.height(8.dp))
                        BentoButton(
                            "Read full review",
                            { uri.openUri(review.url) },
                            kind = BentoButtonKind.Outline,
                            small = true,
                            tone = BentoTone.rotate(idx),
                        )
                    }
                }
                val hasAny = data.critiquebrainz.isNotEmpty() || data.reviews.isNotEmpty()
                val note = when {
                    hasAny -> if (data.retryable) "Some sources did not answer." else ""
                    data.retryable -> "No reviews found. Some sources did not answer."
                    else -> "No reviews found for this album."
                }
                if (note.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(note, style = BentoType.small(), color = p.textMuted)
                }
                if (data.retryable) {
                    Spacer(Modifier.height(8.dp))
                    BentoButton("Try again", loaded.reload, kind = BentoButtonKind.Outline, small = true)
                }
            }
        }
    }
}

/** Shared Bento option sheet (BENTO_THEME.md 7.3 & sheets): 54dp cover, eyebrow, title, 60dp rows with 44dp icon circles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BentoActionSheet(
    eyebrow: String,
    title: String,
    cover: @Composable () -> Unit,
    items: List<Triple<String, ImageVector, () -> Unit>>,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = Side, end = Side, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                cover()
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BentoEyebrow(eyebrow)
                    Text(title, style = BentoType.tileName(), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            items.forEachIndexed { idx, (label, icon, action) ->
                BentoRow(
                    title = label,
                    showDivider = idx < items.lastIndex,
                    onClick = { onDismiss(); action() },
                    lead = {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(p.ink),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, null, tint = p.control, modifier = Modifier.size(20.dp))
                        }
                    },
                )
            }
        }
    }
}
